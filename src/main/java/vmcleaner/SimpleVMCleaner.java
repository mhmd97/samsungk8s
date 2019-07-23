package vmcleaner;

import java.math.BigDecimal;
import java.util.Date;

import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodList;
import resourceprofiler.ResourceProfiler;
import resourceprofiler.WorkerNode;
import util.NodeUtil;
import util.SchedulingUtil;

public class SimpleVMCleaner extends VMCleaner {

	public SimpleVMCleaner() throws Exception {
		super();
	}

	@Override
	public void clean(Date date) throws Exception {
		// check if there are empty nodes, tainted or untainted

		for (WorkerNode workerNode : ResourceProfiler.getUnderutilizedNodes(date)) {
			// if node is underutilized
			WorkerNode node = ResourceProfiler.findNodeByName(workerNode.getName(), workerNode.getEvent_time());

			if (node.getType().startsWith("batch") && node.getRamavailable().divide(node.getCpucapacity()).compareTo(new BigDecimal(0.5)) > 0) {
				// job migration for each node

				V1PodList pods = NodeUtil.getRunningPods(SchedulingUtil.api, node.getName());

				if (pods.getItems().size() == 0) {
					removeNode(node.getName(), date);
				}

				else if (node.getName().startsWith("batch-small")) {
					boolean isAllMigrated = true;
					for (V1Pod pod : pods.getItems()) {
						String rescheduledNode = ResourceProfiler.reschedulePod(pod, node.getEvent_time(),
								node.getName());
						System.out.println("Existing pod:" + node.getName() + ", target node: " + rescheduledNode);
						if (null == rescheduledNode) {
							isAllMigrated = false;
							break;
						} else {
							SchedulingUtil.migrateJob(pod, node.getName(), rescheduledNode);
							ResourceProfiler.update(workerNode.getName(), pod, date, false);
							ResourceProfiler.update(rescheduledNode, pod, date, true);
						}
					}

					if (isAllMigrated) {
						removeNode(node.getName(), date);

					} else {
						System.out.println("Rescheduling failed with Node suitable node found");
					}
				}

			}
		}

	}

	

}
