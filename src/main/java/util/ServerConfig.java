package util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1ResourceRequirements;
import taskpacker.Flavor;
import taskpacker.PackedServer;
import taskpacker.Pod;

public class ServerConfig {
	private static int index = 5;

	public static final Flavor[] flavors = { 
			new Flavor("m3.xsmall", 1, 2000000000L),
			new Flavor("m2.small", 1, 4000000000L),
			new Flavor("m3.small", 2, 4000000000L),
			new Flavor("m2.medium", 2, 6000000000L), 
			new Flavor("m3.medium", 4, 8000000000L),
			new Flavor("m2.large", 4, 12000000000L),
			new Flavor("m3.large", 8, 16000000000L)
			};

	public static Map<PackedServer, List<V1Pod>> selectServer(Map<String, List<V1Pod>> sortedPods) throws ApiException {
		Map<PackedServer, List<V1Pod>> config = new HashMap<PackedServer, List<V1Pod>>();

		for(Entry<String, List<V1Pod>> entry : sortedPods.entrySet()) {
			BigDecimal cpu = new BigDecimal(0);
			BigDecimal memory = new BigDecimal(0);
			List<V1Pod> pods = entry.getValue();
			Set<Pod> podEntries = new HashSet<Pod>();
			for(V1Pod pod : pods) {
				BigDecimal podCpu = new BigDecimal(0);
				BigDecimal podMemory = new BigDecimal(0);
				
				for (V1Container container : pod.getSpec().getContainers()) {

					V1ResourceRequirements resources = container.getResources();
					Map<String, Quantity> requests = resources.getRequests();
					if (requests != null && requests.get("memory") != null && requests.get("cpu") != null) {
						podCpu = podCpu.add(requests.get("cpu").getNumber());
						podMemory = podMemory.add(requests.get("memory").getNumber());
					}
				}
				podEntries.add(new Pod(pod, podCpu, podMemory));
				cpu = cpu.add(podCpu);
				memory = memory.add(podMemory);
				
			}
			
			System.out.println(entry.getKey() + " total cpu " + cpu + " total memory : " + memory);
			
			Flavor flavor = selectFlavor(cpu,memory);
			BigDecimal cpuLeft = new BigDecimal(flavor.getVcpu());
			BigDecimal memoryLeft = new BigDecimal(flavor.getMemory());
			PackedServer server = new PackedServer(entry.getKey() + index , flavor.getName());
			config.put(server, new ArrayList<V1Pod>());
			
			for(Pod pe : podEntries) {
				V1Pod pod = pe.getPod();
				BigDecimal podCpu = pe.getCpu();
				BigDecimal podMemory = pe.getRam();
				
				if(cpuLeft.compareTo(podCpu) >= 0 && memoryLeft.compareTo(podMemory) >=0) {
					config.get(server).add(pod);
					cpu = cpu.subtract(podCpu);
					memory = memory.subtract(podMemory);
					cpuLeft = cpuLeft.subtract(podCpu);
					memoryLeft = memoryLeft.subtract(podMemory);
					System.out.println("cpuleft: " + cpuLeft + " memoryLeft: " + memoryLeft);
				}
				else {
					flavor = selectFlavor(cpu,memory);
					cpuLeft = new BigDecimal(flavor.getVcpu());
					memoryLeft = new BigDecimal(flavor.getMemory());
					index++;
					server = new PackedServer(entry.getKey() + index , flavor.getName());
					config.put(server, new ArrayList<V1Pod>());
					config.get(server).add(pod);
					cpu = cpu.subtract(podCpu);
					memory = memory.subtract(podMemory);
					cpuLeft = cpuLeft.subtract(podCpu);
					memoryLeft = memoryLeft.subtract(podMemory);
					
				}
			}
			index++;
			
			
		}
		return config;

	}
	
	private static Flavor selectFlavor(BigDecimal cpu, BigDecimal memory) {
		Flavor flavor = flavors[0];
		BigDecimal cpuGap = cpu.subtract(new BigDecimal(flavor.getVcpu())).abs();
		BigDecimal memoryGap = memory.subtract(new BigDecimal(flavor.getMemory())).abs();
		
		for(int i = 1; i< flavors.length; i++) {
			BigDecimal cpuCapacity = new BigDecimal(flavors[i].getVcpu()-0.25);
			BigDecimal memCapacity = new BigDecimal(flavors[i].getMemory());
			BigDecimal cpuGapI = cpu.subtract(cpuCapacity).abs();
			BigDecimal memoryGapI = memory.subtract(memCapacity).abs();
			if(memoryGapI.compareTo(memoryGap) < 0) {
				flavor = flavors[i];
				cpuGap = cpuGapI;
				memoryGap = memoryGapI;
			}
			else if (memoryGapI.compareTo(memoryGap) == 0 && cpuGapI.compareTo(cpuGap) < 0) {
				flavor = flavors[i];
				cpuGap = cpuGapI;
				memoryGap = memoryGapI;
			}
		}
		
		return flavor;
		
	}

}
