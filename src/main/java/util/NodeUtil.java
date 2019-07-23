package util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1DeleteOptions;
import io.kubernetes.client.models.V1Node;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodList;
import io.kubernetes.client.models.V1ResourceRequirements;

public class NodeUtil {

	//In number of cores
	public static BigDecimal getCPUCapacity(V1Node node) throws ApiException {	
		Map<String, Quantity> capacity = node.getStatus().getCapacity();
		Quantity cpu = capacity.get("cpu");
		return cpu.getNumber();
	}
	
	//In bytes
	public static BigDecimal getMemoryCapacity(V1Node node) throws ApiException {	
		Map<String, Quantity> capacity = node.getStatus().getCapacity();
		Quantity memory = capacity.get("memory");
		BigDecimal conversion = new BigDecimal("0.9746937799");
		return memory.getNumber().multiply(conversion);
	}
	
	public static BigDecimal getPodsCapacity(V1Node node) throws ApiException {	
		Map<String, Quantity> capacity = node.getStatus().getCapacity();
		Quantity pods = capacity.get("pods");
		return pods.getNumber();
	}
	
	public static BigDecimal getCPURequests(CoreV1Api api, V1Node node) throws ApiException {
		String nodeName = node.getMetadata().getName();
		String fieldSelector = "spec.nodeName=" + nodeName + 
				",status.phase!=Failed,status.phase!=Succeeded";
		
		V1PodList podList = api.listPodForAllNamespaces(null, fieldSelector, false, null, null, null, null, null, null);
		BigDecimal CPUrequests = new BigDecimal(0);
		for(V1Pod pod : podList.getItems()) {
			for (V1Container container : pod.getSpec().getContainers()) {
				V1ResourceRequirements resources = container.getResources();
				Map<String, Quantity> requests = resources.getRequests();
				if(requests != null && requests.get("cpu") != null) {
						CPUrequests = CPUrequests.add(requests.get("cpu").getNumber());
				}
			}
		}
		return CPUrequests;
	}
	
	public static BigDecimal getMemoryRequests(CoreV1Api api, V1Node node) throws ApiException {
		String nodeName = node.getMetadata().getName();
		String fieldSelector = "spec.nodeName=" + nodeName + 
				",status.phase!=Failed,status.phase!=Succeeded";
		V1PodList podList = api.listPodForAllNamespaces(null, fieldSelector, false, null, null, null, null, null, null);
		BigDecimal memoryRequests = new BigDecimal(0);
		for(V1Pod pod : podList.getItems()) {
			for (V1Container container : pod.getSpec().getContainers()) {
				V1ResourceRequirements resources = container.getResources();
				Map<String, Quantity> requests = resources.getRequests();
				if(requests != null && requests.get("memory") != null) {
					memoryRequests = memoryRequests.add(requests.get("memory").getNumber());
				}
			}
		}
		return memoryRequests;
	}

	public static int getNumPods(CoreV1Api api, V1Node node, String namespace) throws Exception {
		int numPods = 0;
		String nodeName = node.getMetadata().getName();
		String fieldSelector = "spec.nodeName=" + nodeName + 
				",status.phase!=Failed,status.phase!=Succeeded";
		V1PodList podList = api.listPodForAllNamespaces(null, fieldSelector, false, null, null, null, null, null, null);
		for(V1Pod pod : podList.getItems()) {
			if (pod.getMetadata().getNamespace().equals("default")) {
				numPods++;
			}
		}
		return numPods;
	}
	
	public static V1PodList getRunningPods(CoreV1Api api, String name) throws Exception {
		String fieldSelector = "spec.nodeName=" + name + 
				",status.phase!=Failed,status.phase!=Succeeded";
		return api.listNamespacedPod("default", null, null, fieldSelector, false, null, null, null, null, null);
		//return api.listPodForAllNamespaces(null, fieldSelector, false, null, null, null, null, null, null);
		
	}
	
	public static void deleteNode(CoreV1Api api, String name) throws Exception {
		V1DeleteOptions deleteOptions = new V1DeleteOptions();
		api.deleteNode(name, deleteOptions, null, null, null, null);
	}
	
	public static void addNoSchedulingTaint(CoreV1Api api, V1Node node) throws Exception {
		String jsonPatchString= "{\"op\":\"add\",\"path\":\"/spec/taints\",\"value\":[{\"effect\":\"NoSchedule\",\"key\":\"autoscaler\",\"value\":\"shutdown\"}]}";
		List<JsonObject> arr = new ArrayList<>();
		JsonObject json = new JsonObject();
		JsonParser parser = new JsonParser();
		json = parser.parse(jsonPatchString).getAsJsonObject();
		arr.add(json);
		
		api.patchNode(node.getMetadata().getName(), arr, null);
	}

	public static void removeNoSchedulingTaint(CoreV1Api api, V1Node node) throws Exception {
		String jsonPatchString= "{\"op\":\"remove\",\"path\":\"/spec/taints\",\"value\":[{\"effect\":\"NoSchedule\",\"key\":\"autoscaler\",\"value\":\"shutdown\"}]}";
		List<JsonObject> arr = new ArrayList<>();
		JsonObject json = new JsonObject();
		JsonParser parser = new JsonParser();
		json = parser.parse(jsonPatchString).getAsJsonObject();
		arr.add(json);
		
		api.patchNode(node.getMetadata().getName(), arr, null);
	}
	
}
