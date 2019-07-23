package evaluation;

import scheduler.BFDScheduler;
import scheduler.CustomScheduler;
import taskpacker.Autoscaler;
import taskpacker.TaskPacker;
import taskpacker.ThresholdAutoscaler;
import vmcleaner.HybridVMCleaner;
import vmcleaner.VMCleaner;

public class KubernetesScheduler {

	public static void main(String[] args) {
		runWorkload();
	}
	
	public static void runWorkload() {
		try {
				
//			VMCleaner vc = new RedeployVMCleaner();
//			Autoscaler as = new RunTimeAutoscaler();
			CustomScheduler cs = new BFDScheduler();
			
			
			VMCleaner vc = new HybridVMCleaner();
			Autoscaler as = new ThresholdAutoscaler();
//			CustomScheduler cs = new DefaultScheduler();
//			CustomScheduler cs = new LRPScheduler();
			TaskPacker tp = new TaskPacker(cs, as ,vc);
			
			tp.schedule();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
