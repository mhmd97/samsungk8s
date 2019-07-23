package scheduler;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.kubernetes.client.models.V1Node;
import io.kubernetes.client.models.V1Pod;
import resourceprofiler.ResourceProfiler;
import util.SchedulingUtil;

public class BFDScheduler extends CustomScheduler{

	@Override
	public List<V1Pod> schedule(List<V1Pod> pods, String namespace, Date date) throws Exception {
		
		List<V1Pod> pendingPods = new ArrayList<V1Pod>();
		for(V1Pod pod : pods) {
			String server = ResourceProfiler.bestFitSchedule(pod, date);
			if(server != null) {
				V1Node node = SchedulingUtil.findNodeByName(server);
				SchedulingUtil.createBinding(node, pod, namespace);
				System.out.println("Create binding between " + server + " and " + pod.getMetadata().getName());
				
				ResourceProfiler.update(server, pod, date, true);
			}
			else {
				pendingPods.add(pod);
			}
		}
		return pendingPods;
	}

}
