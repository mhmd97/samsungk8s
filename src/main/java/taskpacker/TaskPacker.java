package taskpacker;

import static adapter.ClusterInfo.NAMESPACE;
import static adapter.ClusterInfo.SCHEDULER;

import java.util.Date;
import java.util.List;

import io.kubernetes.client.models.V1Pod;
import resourceprofiler.ResourceProfiler;
import scheduler.CustomScheduler;
import util.SchedulingUtil;
import vmcleaner.VMCleaner;

public class TaskPacker {

	private CustomScheduler cs;
	private Autoscaler as;
	private VMCleaner vmCleaner;

	public TaskPacker(CustomScheduler cs, Autoscaler as, VMCleaner vmCleaner) {
		super();
		this.cs = cs;
		this.as = as;
		this.vmCleaner = vmCleaner;
	}

	// Manually get pending pods instead of using watch (this also avoids socket
	// exception)
	public void schedule() throws Exception {
		Date date = null;
		int i = 0;

		while (true) {

			date = new Date();
//			System.out.println("Scheduling pending pods" + date);
			ResourceProfiler.clusterSnapshot(date);
			// Get all pods
			List<V1Pod> pendingPodList = SchedulingUtil.getAllPendingPods(SCHEDULER);
			
//			
//			if(pendingPodList.isEmpty()) {
//				break;
//			}

			List<V1Pod> unscheduledPods = cs.schedule(pendingPodList, NAMESPACE, date);
			if (unscheduledPods != null && !unscheduledPods.isEmpty()) {
				as.pack(unscheduledPods, NAMESPACE, date);
			}

			// clean underutilized instances
//			Thread.sleep(30000);
			vmCleaner.clean(date);

			ResourceProfiler.show(date);
			System.out.println("Finish scheduling for all nodes at " + date);

			if (i > 20 && SchedulingUtil.getAllPendingPods(SCHEDULER).isEmpty()) {
				System.out.println("Finish scheduling all pods" + date);
			}

			if (i > 20 && SchedulingUtil.isFinished(SCHEDULER)) {
				System.out.println("All batches finished" + date);
				break;
			}

			// Wait for ten seconds before trying to schedule pending pods again
			i++;
			Thread.sleep(60000);

		}

//		System.out.println("All pods scheduled, terminating...");
//		
//		//Wait until all batch jobs complete
//		System.out.println("Waiting until batch jobs complete...");
//		List<V1Pod> activeJobs = SchedulingUtil.getActivePodsWithMatchingLabel("type", "batch", schedulerName);
//		while(!activeJobs.isEmpty()) {
//			Thread.sleep(10000);
//
//			activeJobs = SchedulingUtil.getActivePodsWithMatchingLabel("type", "batch", schedulerName);
//
//		}
//
//		//Update accounting info for all remaining nodes
//		vmCleaner.clean(date);
//		System.out.println("Finalizing accounting...");
	}

}
