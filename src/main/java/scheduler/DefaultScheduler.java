package scheduler;

import java.util.Date;
import java.util.List;

import io.kubernetes.client.models.V1Pod;

public class DefaultScheduler extends CustomScheduler {

	@Override
	public List<V1Pod> schedule(List<V1Pod> pods, String namespace, Date date) throws Exception {
		return null;
	}

}
