package vmcleaner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodList;
import resourceprofiler.ResourceProfiler;
import resourceprofiler.WorkerNode;
import util.NodeUtil;
import util.SchedulingUtil;

public class RedeployVMCleaner extends VMCleaner {

	public RedeployVMCleaner() throws Exception {
		super();
	}

	@Override
	public void clean(Date date) throws Exception {
		// check if there are empty nodes, tainted or untainted

		Map<String, WorkerNode> batches = ResourceProfiler.getBatchNodes(date);
		
		List<WorkerNode> underUtilizedNodes = new ArrayList<WorkerNode>();

		for (WorkerNode node : ResourceProfiler.getUnderutilizedNodes(date)) {

			// Clean empty nodes
			V1PodList pods = NodeUtil.getRunningPods(SchedulingUtil.api, node.getName());
			
			WorkerNode batch = batches.get(node.getType());
			
			if (pods.getItems().size() == 0) {
				System.out.println("No running pods remain in node " + node.getName() );
				System.out.println("Start cleaning emoty node " + node.getName());
				if(node.getType().startsWith("batch")) {
					batch.setCpuavailable(batch.getCpuavailable().subtract(node.getCpuavailable()));
					batch.setRamavailable(batch.getRamavailable().subtract(node.getRamcapacity()));
				}
				removeNode(node.getName(), date);
			}
			else if (!node.getType().startsWith("long")) {
				underUtilizedNodes.add(node);
			}



		}
		
		
//		for(WorkerNode node :  underUtilizedNodes) {
//			//Reschedule underutilized nodes
//			
//			System.out.println("Start migrating underutilized node " + node.getName());
//
//			V1PodList pods = NodeUtil.getRunningPods(SchedulingUtil.api, node.getName());
//			WorkerNode batch = batches.get(node.getType());
//			BigDecimal cpu = node.getCpucapacity().subtract(node.getCpuavailable());
//			BigDecimal ram = node.getRamcapacity().subtract(node.getRamavailable());
//
//			
//			if	(batch.getCpuavailable().subtract(node.getCpuavailable()).compareTo(cpu) > 0
//					&& batch.getRamavailable().subtract(node.getRamavailable()).compareTo(ram) > 0) {
//				
//				for (V1Pod pod : pods.getItems()) {
//						SchedulingUtil.redeployJob(pod);
//						ResourceProfiler.update(node.getName(), pod, date, false);
//				}
//				batch.setCpuavailable(batch.getCpuavailable().subtract(node.getCpuavailable()));
//				batch.setRamavailable(batch.getRamavailable().subtract(node.getRamcapacity()));
//				Thread.sleep(10000);
//				removeNode(node.getName(), date);
//			}
//			
//			else {
//				System.out.println("Rescheduling failed, due to lack of resources");
//			}
//		}

	}


}