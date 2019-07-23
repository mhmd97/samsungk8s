package taskpacker;

import java.util.Date;
import java.util.List;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.models.V1Pod;

public abstract class Autoscaler {
	
	public abstract void pack(List<V1Pod> pods, String namespace, Date date) throws ApiException;

}
