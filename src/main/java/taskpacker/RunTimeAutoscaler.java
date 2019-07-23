package taskpacker;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

import adapter.OpenStackAdapter;
import adapter.ProvisioningNode;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.models.V1Node;
import io.kubernetes.client.models.V1NodeCondition;
import io.kubernetes.client.models.V1Pod;
import util.SchedulingUtil;
import util.ServerConfig;

public class RunTimeAutoscaler extends Autoscaler {
	private boolean isAutoscaling = false;
	
	private long timestamp = 0;

	@Override
	public void pack(List<V1Pod> pods, String namespace, Date date) throws ApiException {
//		if(System.currentTimeMillis() - timestamp < 480000) {
//			System.out.println("Out of autoscaling cycle");
//			return;
//		}
//		else {
//			timestamp = System.currentTimeMillis();
//		}
		
		if(isAutoscaling) {
			System.out.println("Out of autoscaling cycle");
			return;
		}
		else {
			isAutoscaling = true;
			
		}
		
		Map<String, List<V1Pod>> sortedPods = new HashMap<String, List<V1Pod>>();

		for (V1Pod pod : pods) {
			String type = "longrunning-";
			if (pod.getMetadata().getLabels().containsKey("type")
					&& pod.getMetadata().getLabels().get("type").equals("batch")) {
				type = "batch" + "-" + getRunTimeScale(pod.getMetadata().getGenerateName()) + "-";
			}

			if (!sortedPods.containsKey(type)) {
				sortedPods.put(type, new ArrayList<V1Pod>());
			}
			sortedPods.get(type).add(pod);
		}

		ExecutorService executor = Executors.newFixedThreadPool(8);
		executor.submit(() -> {

			try {
				List<Entry<PackedServer, List<V1Pod>>> list = 
				new ArrayList<Entry<PackedServer, List<V1Pod>>>(ServerConfig.selectServer(sortedPods).entrySet());
						list.parallelStream().forEach(entry -> {

							try {
								System.out.println(
										entry.getKey().toString() + " number of pods: " + entry.getValue().size());
										String serverName = entry.getKey().getServerName();
										ProvisioningNode pNode = new OpenStackAdapter().addNodeByName(serverName,
												entry.getKey().getFlavorName());
										System.out.println("Finish starting node: " + pNode.getName());

							} catch (Exception e) {
								e.printStackTrace();
							}
						});
						isAutoscaling = false;
			} catch (ApiException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

//		ForkJoinPool forkJoinPool = new ForkJoinPool(8);
//		try {
//			forkJoinPool.submit(() -> {
//				try {
//					new ArrayList<Entry<PackedServer, List<V1Pod>>>(ServerConfig.selectServer(sortedPods).entrySet())
//							.parallelStream().forEach(entry -> {
//
//								try {
//									System.out.println(
//											entry.getKey().toString() + " number of pods: " + entry.getValue().size());
//											String serverName = entry.getKey().getServerName();
//											ProvisioningNode pNode = new OpenStackAdapter().addNodeByName(serverName,
//													entry.getKey().getFlavorName());
//											boolean flag = true;
//											while (flag) {
//												Thread.sleep(60000);
//												V1Node node = SchedulingUtil.findNodeByName(pNode.getName());
////												BigDecimal cpuCapacity = NodeUtil.getCPUCapacity(node);
////												BigDecimal memoryCapacity = NodeUtil.getMemoryCapacity(node);
//
//												for (V1NodeCondition condition : node.getStatus().getConditions()) {
////											System.out.println("condition: " + condition.getType() + " : " + condition.getStatus());
//													if (condition.getType().equals("Ready") && condition.getStatus().equals("True")) {
////														ResourceProfiler.addNode(new WorkerNode(serverName, serverName.substring(0, serverName.lastIndexOf("-")), date,
////																memoryCapacity, cpuCapacity, memoryCapacity, cpuCapacity));
////														for (V1Pod pod : entry.getValue()) {
////															SchedulingUtil.createBinding(node, pod, namespace);
////															ResourceProfiler.update(node.getMetadata().getName(), pod, date, true);
////														}
//														flag = false;
//													}
//												}
//											}
//											System.out.println("Finish scheduling for node: " + pNode.getName());
//
//								} catch (Exception e) {
//									e.printStackTrace();
//								}
//							});
//				} catch (ApiException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}).get();
//		} catch (ExecutionException | InterruptedException e) {
//			e.printStackTrace();
//		}

	}

	private String getRunTimeScale(String name) {
		if (name.contains("small")) {
			return "small";
		} else if (name.contains("med")) {
			return "med";
		} else {
			return "large";
		}
	}

}
