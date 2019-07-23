package vmcleaner;

import java.math.BigDecimal;
import java.util.Date;

import io.kubernetes.client.models.V1PodList;
import resourceprofiler.ResourceProfiler;
import resourceprofiler.WorkerNode;
import util.NodeUtil;
import util.SchedulingUtil;

public class EmptyVMCleaner extends VMCleaner {

	public EmptyVMCleaner() throws Exception {
		super();
	}

	@Override
	public void clean(Date date) throws Exception {
		// check if there are empty nodes, tainted or untainted

		for (WorkerNode workerNode : ResourceProfiler.getUnderutilizedNodes(date)) {
			// if node is underutilized
			WorkerNode node = ResourceProfiler.findNodeByName(workerNode.getName(), workerNode.getEvent_time());

			if (node.getType().startsWith("batch")
					&& node.getRamavailable().divide(node.getCpucapacity()).compareTo(new BigDecimal(0.5)) > 0) {
				// job migration for each node

				V1PodList pods = NodeUtil.getRunningPods(SchedulingUtil.api, node.getName());

				if (pods.getItems().size() == 0) {
					removeNode(node.getName(), date);
					System.out.println("Remove empty node: " + node.getName());
				}

			}
		}

	}

}