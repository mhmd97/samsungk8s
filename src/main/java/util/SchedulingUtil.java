package util;

import static adapter.ClusterInfo.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import com.google.gson.reflect.TypeToken;

import adapter.OpenStackAdapter;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1Binding;
import io.kubernetes.client.models.V1DeleteOptions;
import io.kubernetes.client.models.V1Node;
import io.kubernetes.client.models.V1NodeList;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1ObjectReference;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodCondition;
import io.kubernetes.client.models.V1PodList;
import io.kubernetes.client.models.V1Taint;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Watch;

public class SchedulingUtil {

	private static ApiClient client;
	public static CoreV1Api api;

	static {
		client = Config.fromToken(IP, TOKEN, false);
		client.getHttpClient().setReadTimeout(60, TimeUnit.SECONDS);
		Configuration.setDefaultApiClient(client);

		api = new CoreV1Api();
	}

	public static Watch<V1Pod> createWatch(String namespace) throws Exception {
		Watch<V1Pod> watch = Watch.createWatch(client, api.listNamespacedPodCall(namespace, "true", null, null, null,
				null, null, null, null, true, null, null), new TypeToken<Watch.Response<V1Pod>>() {
				}.getType());
		return watch;
	}

	public static void redeployJob(V1Pod pod) {
		System.out.println("Start to redeploy pod: " + pod.getMetadata().getName());
		int time = Integer.parseInt(pod.getSpec().getContainers().get(0).getCommand().get(2).split("sleep ")[1])
				- Seconds.secondsBetween(
						pod.getStatus().getContainerStatuses().get(0).getState().getRunning().getStartedAt(),
						DateTime.now()).getSeconds();

		String job = pod.getMetadata().getOwnerReferences().get(0).getName();

		String file = "sleep";
		String name = pod.getMetadata().getGenerateName();

		if (name.contains("small")) {
			file += "Small";
		} else if (name.contains("med")) {
			file += "Med";
		} else {
			file += "Large";
		}
		file += ".yaml";
		
		

		String command = "ssh -i /home/davidzhong/Documents/davidzhong.pem ubuntu@115.146.92.248 sudo /home/ubuntu/workloads/migrate/redeploy.sh " + job + " " + file + " " + time;
		System.out.println(command);
		OpenStackAdapter.cmd(command);
	}

	public static void migrateJob(V1Pod pod, String from, String to) throws Exception {
//		V1Node node1 = findNodeByName(from);
		V1Node node2 = findNodeByName(to);
//		String address1 = node1.getStatus().getAddresses().get(0).getAddress();
//		String address2 = node2.getStatus().getAddresses().get(0).getAddress();
		String podName = getContainerName(pod);

//		String command = "ssh -i /home/davidzhong/Documents/davidzhong.pem ubuntu@" + address1
//				+ " sudo /home/ubuntu/checkpoint.sh " + podName + " " + podName;
//		System.out.println(command);
//		OpenStackAdapter.cmd(command);
//
//		Thread.sleep(10000);

		unschedulePod(pod, NAMESPACE);
		List<V1Pod> list = null;
		while (list == null || list.isEmpty() || getContainerName(list.get(0)).equals(podName)) {
			Thread.sleep(20000);
			list = SchedulingUtil.getAllPendingPods(SCHEDULER).parallelStream()
					.filter(p -> p.getMetadata().getName().startsWith(pod.getMetadata().getGenerateName()))
					.collect(Collectors.toList());

		}

		SchedulingUtil.createBinding(node2, list.get(0), NAMESPACE);
		Thread.sleep(30000);
//		String newPod = getContainerName(list.get(0));
//
//		command = "ssh -i /home/davidzhong/Documents/davidzhong.pem ubuntu@" + address2 + " sudo /home/ubuntu/restore.sh "
//				+ podName + " " + newPod + " " + address1;
//		System.out.println(command);
//		Thread.sleep(30000);
//		OpenStackAdapter.cmd(command);
//		Thread.sleep(1000000);

	}

	private static String getContainerName(V1Pod pod) {
		return "k8s_" + pod.getSpec().getContainers().get(0).getName() + "_" + pod.getMetadata().getName() + "_"
				+ pod.getMetadata().getNamespace() + "_" + pod.getMetadata().getUid() + "_0";
	}

	public static V1Node findNodeByName(String name) throws Exception {
		for (V1Node node : getAllNodes().getItems()) {
			if (node.getMetadata().getName().equals(name)) {
				return node;
			}
		}
		return null;
	}

	public static V1NodeList getAllNodes() throws Exception {
		return api.listNode("true", null, null, false, null, null, null, null, null);
	}

	public static void createBinding(V1Node node, V1Pod pod, String namespace) throws Exception {
		
		String podName = pod.getMetadata().getName();
		String nodeName = node.getMetadata().getName();
		V1ObjectReference target = new V1ObjectReference();
		target.kind("Node");
		target.apiVersion("v1");
		target.name(nodeName);

		V1Binding body = new V1Binding();
		body.kind("Binding");
		body.apiVersion("v1");

		V1ObjectMeta meta = new V1ObjectMeta();
		meta.setName(podName);
		body.setMetadata(meta);
		body.target(target);

		System.out.println("Scheduling pod " + podName + " to node " + nodeName);

		api.createNamespacedBinding(namespace, body, null);
	}

	public static List<V1Pod> getPendingPodsWithMatchingLabel(String key, String value, String schedulerName)
			throws Exception {
		List<V1Pod> pods = new ArrayList<V1Pod>();
		V1PodList podList = api.listPodForAllNamespaces(null, null, false, null, null, null, null, null, null);

		for (V1Pod pod : podList.getItems()) {

			// Filter by namespace and status
			boolean schedulePod = true;
			if (pod.getStatus().getPhase().equals("Pending")
					&& pod.getSpec().getSchedulerName().equals(schedulerName)) {
				if (pod.getStatus().getConditions() != null) {
					for (V1PodCondition condition : pod.getStatus().getConditions()) {
						if (condition.getType().equals("PodScheduled")) {
							if (condition.getStatus().equals("True")) {
								// Pod status is still pending but it has been scheduled already
								schedulePod = false;
							}
						}
					}
				}

				if (pod.getMetadata().getLabels().containsKey(key)
						&& pod.getMetadata().getLabels().get(key).equals(value) && schedulePod) {
					pods.add(pod);
				}
			}
		}

		return pods;
	}

	public static List<V1Pod> getPendingPodsWithSameGenerateName(String generateName, String schedulerName)
			throws Exception {
		List<V1Pod> pods = new ArrayList<V1Pod>();
		V1PodList podList = api.listPodForAllNamespaces(null, null, false, null, null, null, null, null, null);

		for (V1Pod pod : podList.getItems()) {

			// Filter by namespace and status
			boolean schedulePod = true;
			if (pod.getStatus().getPhase().equals("Pending")
					&& pod.getSpec().getSchedulerName().equals(schedulerName)) {
				if (pod.getStatus().getConditions() != null) {
					for (V1PodCondition condition : pod.getStatus().getConditions()) {
						if (condition.getType().equals("PodScheduled")) {
							if (condition.getStatus().equals("True")) {
								// Pod status is still pending but it has been scheduled already
								schedulePod = false;
							}
						}
					}
				}
				if (pod.getMetadata().getGenerateName().equals(generateName) && schedulePod) {
					pods.add(pod);
				}
			}
		}

		return pods;
	}

	public static List<V1Pod> getActivePodsWithMatchingLabel(String key, String value, String schedulerName)
			throws Exception {
		List<V1Pod> pods = new ArrayList<V1Pod>();
		V1PodList podList = api.listPodForAllNamespaces(null, null, false, null, null, null, null, null, null);

		for (V1Pod pod : podList.getItems()) {

			// Filter by namespace and status
			if (!pod.getStatus().getPhase().equals("Succeeded")
					&& pod.getSpec().getSchedulerName().equals(schedulerName)) {
				if (pod.getMetadata().getLabels().containsKey(key)
						&& pod.getMetadata().getLabels().get(key).equals(value)) {
					pods.add(pod);
				}
			}
		}
		System.out.println(pods.size() + " batch jobs still running");
		return pods;
	}

	public static void unschedulePod(V1Pod pod, String namespace) {

		try {
			V1DeleteOptions body = new V1DeleteOptions();
			api.deleteNamespacedPod(pod.getMetadata().getName(), namespace, body, "true", 0, true, "Foreground");
		} catch (Exception e) {
			System.out.println("Deleted pod: " + pod.getMetadata().getName());
			e.printStackTrace();
		}
	}

	public static V1NodeList getAllSchedulableNodes() throws Exception {
		V1NodeList schedNodes = new V1NodeList();
		V1NodeList allNodes = getAllNodes();
		for (V1Node node : allNodes.getItems()) {
			boolean suitable = true;
			if (node.getSpec().getTaints() != null) {
				for (V1Taint taint : node.getSpec().getTaints()) {
					if (taint.getEffect().equals("NoSchedule")) {
						suitable = false;
					}
				}
			}
			if (suitable) {
				schedNodes.addItemsItem(node);
			}

		}
		return schedNodes;
	}

	public static List<V1Pod> getMoveablePods(V1PodList pods) {
		List<V1Pod> moveable = new ArrayList<V1Pod>();
		for (V1Pod pod : pods.getItems()) {
			if (pod.getMetadata().getLabels().containsKey("rescheduling")) {
				if (pod.getMetadata().getLabels().get("rescheduling").equals("moveable")) {
					moveable.add(pod);
				}
			}
		}
		return moveable;
	}

	public static List<V1Pod> getBatchPods(V1PodList pods) {
		List<V1Pod> batch = new ArrayList<V1Pod>();
		for (V1Pod pod : pods.getItems()) {
			if (pod.getMetadata().getLabels().containsKey("type")) {
				if (pod.getMetadata().getLabels().get("type").equals("batch")) {
					batch.add(pod);
				}
			}
		}
		return batch;
	}

	public static V1NodeList getAllUnschedulableNodes() throws Exception {
		V1NodeList schedNodes = new V1NodeList();
		V1NodeList allNodes = getAllNodes();
		for (V1Node node : allNodes.getItems()) {
			boolean suitable = false;
			if (node.getSpec().getTaints() != null) {
				for (V1Taint taint : node.getSpec().getTaints()) {
					if (taint.getEffect().equals("NoSchedule")) {
						suitable = true;
						if (node.getMetadata().getLabels().containsKey("node-role.kubernetes.io/master")) {
							suitable = false;

						}
					}
				}
			}
			if (suitable) {
				schedNodes.addItemsItem(node);
			}

		}
		return schedNodes;
	}

	public static void removeTaintFromNodes(List<V1Node> updatedNodes) throws Exception {
		for (V1Node node : updatedNodes) {
			for (V1Taint taint : node.getSpec().getTaints()) {
				if (taint.getEffect().equals("NoSchedule")) {
					if (!node.getMetadata().getLabels().containsKey("node-role.kubernetes.io/master")) {
						// not master, remove taint
						NodeUtil.removeNoSchedulingTaint(api, node);
					}
				}
			}
		}

	}

	public static List<V1Pod> getAllPendingPods(String schedulerName) throws Exception {
		V1PodList podList = SchedulingUtil.api.listPodForAllNamespaces(null, null, false, null, null, null, null, null,
				null);
		List<V1Pod> pendingPodList = new ArrayList<>();

		for (V1Pod pod : podList.getItems()) {
			// Filter by namespace and status
			boolean pending = true;
			if (pod.getStatus().getPhase().equals("Pending")
					&& pod.getSpec().getSchedulerName().equals(schedulerName)
					) {
				System.out.printf("%s : %s%n", "Pending pod: ", pod.getMetadata().getName());
				if (pod.getStatus().getConditions() != null) {
					for (V1PodCondition condition : pod.getStatus().getConditions()) {
						if (condition.getType().equals("PodScheduled")) {
							if (condition.getStatus().equals("True")) {
								// Pod status is still pending but it has been scheduled already
								pending = false;
							}
						}
					}
				}

				if (pending) {
					pendingPodList.add(pod);
				}
			}
		}

		return pendingPodList;
	}
	
	public static boolean isFinished(String schedulerName) throws Exception {
		V1PodList podList = SchedulingUtil.api.listPodForAllNamespaces(null, null, false, null, null, null, null, null,
				null);

		for (V1Pod pod : podList.getItems()) {
			// Filter by namespace and status
			
			if (pod.getStatus().getPhase().equals("Running")
					&& pod.getSpec().getSchedulerName().equals(schedulerName)
					) {
//				System.out.printf("%s : %s%n", "Running pod: ", pod.getMetadata().getName());
				if(pod.getMetadata().getName().startsWith("sleep")) {
					return false;
				}

			}
		}

		return true;
	}

}
