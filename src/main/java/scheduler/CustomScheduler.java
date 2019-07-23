package scheduler;

import java.util.Date;
import java.util.List;

import io.kubernetes.client.models.V1Pod;

public abstract class CustomScheduler {
	

	public abstract List<V1Pod> schedule(List<V1Pod> pods, String namespace, Date date) throws Exception ;

}
