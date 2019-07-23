package vmcleaner;

import java.util.Date;

import adapter.OpenStackAdapter;
import resourceprofiler.ResourceProfiler;
import util.NodeUtil;
import util.SchedulingUtil;

public abstract class VMCleaner {

	protected OpenStackAdapter openStack;

	public VMCleaner() {
		openStack = new OpenStackAdapter();
	}

	public abstract void clean(Date date) throws Exception;
	
	
	protected void removeNode(String node, Date date) throws Exception {
		System.out.println("Removing node " + node);
		// shut down node if all its jobs could be migrated
		NodeUtil.deleteNode(SchedulingUtil.api, node);
		// shutdown VM and record with accounting
		openStack.deleteNode(node, true);
		ResourceProfiler.shutDown(node, date);
	}

}
