package resourceprofiler;

import static adapter.ClusterInfo.IP;
import static adapter.ClusterInfo.TOKEN;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1Node;
import io.kubernetes.client.models.V1NodeCondition;
import io.kubernetes.client.models.V1NodeList;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodList;
import io.kubernetes.client.models.V1ResourceRequirements;
import io.kubernetes.client.util.Config;
import taskpacker.Flavor;
import util.NodeUtil;
import util.ServerConfig;

public class ResourceProfiler {
	private static List<BigDecimal> memoryUti = new ArrayList<BigDecimal>();
	private static List<BigDecimal> cpuUti = new ArrayList<BigDecimal>();
	private static List<Integer> nodeR = new ArrayList<Integer>();
	private static List<Integer> podR = new ArrayList<Integer>();
	private static ApiClient client;
	private static CoreV1Api api;
	private static CassandraDao dao;

	static {
		CassandraCluster cluster = new CassandraCluster("127.0.0.1");
		dao = new CassandraDao(cluster, "k1");
		client = Config.fromToken(IP, TOKEN, false);
		client.getHttpClient().setReadTimeout(60, TimeUnit.SECONDS);
		Configuration.setDefaultApiClient(client);

		api = new CoreV1Api();
	}

	public static void clusterSnapshot(Date date) throws ApiException {
		String fieldSelector = "status.phase!=Failed,status.phase!=Succeeded";
		V1PodList podList = api.listPodForAllNamespaces(null, fieldSelector, false, null, null, null, null, null, null);
		Map<String, List<V1Pod>> nodeMap = new HashMap<String, List<V1Pod>>();
		for (V1Pod pod : podList.getItems()) {
			String name = pod.getSpec().getNodeName();
			if (nodeMap.get(name) == null) {
				nodeMap.put(name, new ArrayList<V1Pod>());
			}

			nodeMap.get(name).add(pod);
		}

		V1NodeList nodeList = api.listNode("true", null, null, false, null, null, null, null, null);
		List<WorkerNode> nodes = new ArrayList<WorkerNode>();
		for (V1Node node : nodeList.getItems()) {

			boolean flag = false;
			for (V1NodeCondition condition : node.getStatus().getConditions()) {
				if (condition.getType().equals("Ready") && !condition.getStatus().equals("True")) {
					flag = true;
					break;
				}
			}

			if (flag) {
				continue;
			}

			String name = node.getMetadata().getName();
			List<V1Pod> pods = nodeMap.get(name);

			BigDecimal cpuCapacity = NodeUtil.getCPUCapacity(node);
			BigDecimal memoryCapacity = NodeUtil.getMemoryCapacity(node);
			BigDecimal cpuAvailable = cpuCapacity;
			BigDecimal memoryAvailable = memoryCapacity;
			System.out.println("cpu: " + cpuAvailable);
			System.out.println("memory: " + memoryAvailable);
			for (V1Pod pod : pods) {
				for (V1Container container : pod.getSpec().getContainers()) {

					V1ResourceRequirements resources = container.getResources();
					Map<String, Quantity> requests = resources.getRequests();
					if (requests != null && requests.get("memory") != null && requests.get("cpu") != null) {

						cpuAvailable = cpuAvailable.subtract(requests.get("cpu").getNumber());
						memoryAvailable = memoryAvailable.subtract(requests.get("memory").getNumber());

					}
				}

			}

			WorkerNode workerNode = new WorkerNode(name, name.substring(0, name.lastIndexOf("-")), date, memoryCapacity,
					cpuCapacity, memoryAvailable, cpuAvailable.subtract(new BigDecimal(0.25)));

			nodes.add(workerNode);
		}

		dao.batchInsert(nodes);
	}

	public static String bestFitSchedule(V1Pod pod, Date date) {

		BigDecimal cpu = new BigDecimal(0);
		BigDecimal ram = new BigDecimal(0);
		for (V1Container container : pod.getSpec().getContainers()) {

			V1ResourceRequirements resources = container.getResources();
			Map<String, Quantity> requests = resources.getRequests();
			if (requests != null && requests.get("memory") != null && requests.get("cpu") != null) {
				cpu = cpu.add(requests.get("cpu").getNumber());
				ram = ram.add(requests.get("memory").getNumber());
			}
		}

		return dao.filterNodeByCapacity(ram, cpu, getType(pod), date);

	}

	public static String leastRequestedSchedule(V1Pod pod, Date date) {

		BigDecimal cpu = new BigDecimal(0);
		BigDecimal ram = new BigDecimal(0);
		for (V1Container container : pod.getSpec().getContainers()) {

			V1ResourceRequirements resources = container.getResources();
			Map<String, Quantity> requests = resources.getRequests();
			if (requests != null && requests.get("memory") != null && requests.get("cpu") != null) {
				cpu = cpu.add(requests.get("cpu").getNumber());
				ram = ram.add(requests.get("memory").getNumber());
			}
		}

		return dao.filterLRPNodeByCapacity(ram, cpu, getType(pod), date);

	}

	public static String reschedulePod(V1Pod pod, Date date, String node) {

		BigDecimal cpu = new BigDecimal(0);
		BigDecimal ram = new BigDecimal(0);
		for (V1Container container : pod.getSpec().getContainers()) {

			V1ResourceRequirements resources = container.getResources();
			Map<String, Quantity> requests = resources.getRequests();
			if (requests != null && requests.get("memory") != null && requests.get("cpu") != null) {
				cpu = cpu.add(requests.get("cpu").getNumber());
				ram = ram.add(requests.get("memory").getNumber());
			}
		}

		String target = dao.filterNodeByCapacity(ram, cpu, getType(pod), date);
		if (node.equals(target)) {
			return null;
		}

		return target;
	}

	public static List<WorkerNode> getUnderutilizedNodes(Date date) {
		return dao.filterNodeByDate(date).parallelStream()
				.filter(node -> !node.getName().startsWith("master") && node.getRamavailable()
						.divide(node.getRamcapacity(), 2, RoundingMode.HALF_UP).compareTo(new BigDecimal(0.5)) > 0)
				.collect(Collectors.toList());
	}

	public static Map<String, WorkerNode> getBatchNodes(Date date) {
		Map<String, WorkerNode> batches = new HashMap<String, WorkerNode>();
		for (WorkerNode node : dao.filterNodeByDate(date)) {
			if (node.getType().startsWith("batch") && !batches.containsKey(node.getType())) {
				batches.put(node.getType(), node);
			} else if (batches.containsKey(node.getType())) {
				WorkerNode batch = batches.get(node.getType());
				batch.setCpuavailable(batch.getCpuavailable().add(node.getCpuavailable()));
				batch.setCpucapacity(batch.getCpucapacity().add(node.getCpucapacity()));
				batch.setRamavailable(batch.getRamavailable().add(node.getRamavailable()));
				batch.setRamcapacity(batch.getRamcapacity().add(node.getRamcapacity()));
			}

		}
		return batches;
	}

	public static void addNode(WorkerNode node) throws ApiException {
		dao.insert(node);
	}

	public static WorkerNode findNodeByName(String name, Date date) {
		return dao.findNodeByName(name, date);
	}

	public static void update(String node, V1Pod pod, Date date, boolean isProvision) throws ApiException {
		WorkerNode workerNode = dao.findNodeByName(node, date);
		dao.delete(workerNode.getRamavailable(), workerNode.getCpuavailable(), date, node);
		BigDecimal cpu = new BigDecimal(0);
		BigDecimal ram = new BigDecimal(0);
		for (V1Container container : pod.getSpec().getContainers()) {

			V1ResourceRequirements resources = container.getResources();
			Map<String, Quantity> requests = resources.getRequests();
			if (requests != null && requests.get("memory") != null && requests.get("cpu") != null) {
				cpu = cpu.add(requests.get("cpu").getNumber());
				ram = ram.add(requests.get("memory").getNumber());
			}
		}

		if (isProvision) {
			workerNode.setCpuavailable(workerNode.getCpuavailable().subtract(cpu));
			workerNode.setRamavailable(workerNode.getRamavailable().subtract(ram));
		} else {
			workerNode.setCpuavailable(workerNode.getCpuavailable().add(cpu));
			workerNode.setRamavailable(workerNode.getRamavailable().add(ram));
		}

		dao.insert(workerNode);
	}

	public static void shutDown(String node, Date date) throws ApiException {
		WorkerNode workerNode = dao.findNodeByName(node, date);
		dao.delete(workerNode.getRamavailable(), workerNode.getCpuavailable(), date, node);
		workerNode.setType("Down");
		dao.insert(workerNode);

	}

	public static void close() {
		dao.close();
	}

	private static String getType(V1Pod pod) {
		String type = "longrunning";
		if (pod.getMetadata().getLabels().containsKey("type")
				&& pod.getMetadata().getLabels().get("type").equals("batch")) {
			String name = pod.getMetadata().getGenerateName();
			type = "batch" + "-";

			if (name.contains("small")) {
				type += "small";
			} else if (name.contains("med")) {
				type += "med";
			} else {
				type += "large";
			}

		}
		return type;
	}

	private static double getPrice(WorkerNode node) {

		HashMap<String, Double> b2 = new HashMap<String, Double>();
		b2.put("m3.xsmall", 0.0143);
		b2.put("m2.small", 0.0286);
		b2.put("m3.small", 0.0572);
		b2.put("m2.medium", 0.0858);
		b2.put("m3.medium", 0.152);
		b2.put("m2.large", 0.228);
		b2.put("m3.large", 0.4573);

		HashMap<String, Double> a2 = new HashMap<String, Double>();

		a2.put("m3.xsmall", 0.0495);
		a2.put("m2.small", 0.0893);
		a2.put("m3.small", 0.1044);
		a2.put("m2.medium", 0.1360);
		a2.put("m3.medium", 0.2184);
		a2.put("m2.large", 0.2856);
		a2.put("m3.large", 0.4573);

		for (Flavor f : ServerConfig.flavors) {
			if (node.getCpucapacity().longValue() == f.getVcpu()
					&& (node.getRamcapacity().longValue() - f.getMemory()) / 1000000000 == 0) {
				return node.getName().startsWith("batch") ? b2.get(f.getName()) : b2.get(f.getName());
			}
		}
		return 0;
	}

	public static void main(String[] args) {

		List<WorkerNode> nodes = dao.fetchAll();
		Map<String, List<WorkerNode>> map = new HashMap<String, List<WorkerNode>>();
		Date bound = new Date(0);

		for (WorkerNode node : nodes) {
			if (node.getType().contains("batch") && node.getEvent_time().after(bound)) {
				bound = node.getEvent_time();
			}

			if (!map.containsKey(node.getName())) {
				map.put(node.getName(), Arrays.asList(node, node));
			} else {
				List<WorkerNode> edges = map.get(node.getName());
				if (edges.get(0).getEvent_time().after(node.getEvent_time())) {
					edges.set(0, node);
				}
				if (edges.get(1).getEvent_time().before(node.getEvent_time())) {
					edges.set(1, node);
				}
			}
		}

		System.out.println(bound);

		double totalPrice = 0;
		for (Entry<String, List<WorkerNode>> e : map.entrySet()) {
			long minute = 0;
			if (!e.getValue().get(0).getType().contains("batch") && e.getValue().get(1).getEvent_time().after(bound)) {
				minute = (bound.getTime() - e.getValue().get(0).getEvent_time().getTime()) / 60000;
			} else {
				minute = (e.getValue().get(1).getEvent_time().getTime() - e.getValue().get(0).getEvent_time().getTime())
						/ 60000;
			}

			System.out.println(e.getKey() + " : " + e.getValue().get(0).getEvent_time() + " " + e.getValue().get(1).getEvent_time());
			double price = minute * getPrice(e.getValue().get(0)) / 60;
			totalPrice += price;
			System.out.println(e.getKey() + " " + price);
		}

		System.out.println(totalPrice);

	}
	
	public static void show(Date date) throws ApiException {
		String fieldSelector = "status.phase!=Failed,status.phase!=Succeeded";
		V1PodList podList = api.listPodForAllNamespaces(null, fieldSelector, false, null, null, null, null, null, null);
		
		V1NodeList nodeList = api.listNode("true", null, null, false, null, null, null, null, null);
		nodeR.add(nodeList.getItems().size() );
		
		
		Map<String, List<V1Pod>> nodeMap = new HashMap<String, List<V1Pod>>();
		int pn = 0;
		for (V1Pod pod : podList.getItems()) {
			String name = pod.getSpec().getNodeName();
			if (nodeMap.get(name) == null) {
				nodeMap.put(name, new ArrayList<V1Pod>());
			}

			nodeMap.get(name).add(pod);
			if(!pod.getMetadata().getName().startsWith("sleep") && !pod.getMetadata().getName().startsWith("nginx") ) {
				continue;
			}
				
			pn++;
		}
		podR.add(pn);
		List<WorkerNode> nodes = new ArrayList<WorkerNode>();
		for (V1Node node : nodeList.getItems()) {

			boolean flag = false;
			for (V1NodeCondition condition : node.getStatus().getConditions()) {
				if (condition.getType().equals("Ready") && !condition.getStatus().equals("True")) {
					flag = true;
					break;
				}
			}

			if (flag) {
				continue;
			}

			String name = node.getMetadata().getName();
			List<V1Pod> pods = nodeMap.get(name);

			BigDecimal cpuCapacity = NodeUtil.getCPUCapacity(node);
			BigDecimal memoryCapacity = NodeUtil.getMemoryCapacity(node);
			BigDecimal cpuAvailable = cpuCapacity;
			BigDecimal memoryAvailable = memoryCapacity;
			System.out.println("cpu: " + cpuAvailable);
			System.out.println("memory: " + memoryAvailable);
			for (V1Pod pod : pods) {
				for (V1Container container : pod.getSpec().getContainers()) {

					V1ResourceRequirements resources = container.getResources();
					Map<String, Quantity> requests = resources.getRequests();
					if (requests != null && requests.get("memory") != null && requests.get("cpu") != null) {

						cpuAvailable = cpuAvailable.subtract(requests.get("cpu").getNumber());
						memoryAvailable = memoryAvailable.subtract(requests.get("memory").getNumber());

					}
				}

			}

			WorkerNode workerNode = new WorkerNode(name, name.substring(0, name.lastIndexOf("-")), date, memoryCapacity,
					cpuCapacity, memoryAvailable, cpuAvailable.subtract(new BigDecimal(0.25)));

			nodes.add(workerNode);
		}

		BigDecimal cpuTotal = new BigDecimal(0);
		BigDecimal memoryTotal = new BigDecimal(0);
		BigDecimal cpuA = new BigDecimal(0);
		BigDecimal memoryA = new BigDecimal(0);
		if(nodes.size()>1) {
			for (WorkerNode node : nodes) {
				if (node.getName().startsWith("master")) {
					continue;
				}
				cpuTotal = cpuTotal.add(node.getCpucapacity());
				memoryTotal = memoryTotal.add(node.getRamcapacity());
				cpuA = cpuA.add(node.getCpuavailable());
				memoryA = memoryA.add(node.getRamavailable());
			}
			memoryUti.add(memoryTotal.subtract(memoryA).divide(memoryTotal, 2, RoundingMode.HALF_UP).multiply(new BigDecimal(100)));
			cpuUti.add(cpuTotal.subtract(cpuA).divide(cpuTotal, 2, RoundingMode.HALF_UP).multiply(new BigDecimal(100)));

			System.out.println(memoryUti);
			System.out.println(cpuUti);
			System.out.println(nodeR);
			System.out.println(podR);
		}
	}

}
