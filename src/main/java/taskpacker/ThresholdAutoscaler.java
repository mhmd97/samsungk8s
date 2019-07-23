package taskpacker;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import adapter.OpenStackAdapter;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.models.V1Pod;
import resourceprofiler.ResourceProfiler;
import resourceprofiler.WorkerNode;

public class ThresholdAutoscaler extends Autoscaler {
	private static int index = 5;
	private boolean isAutoscaling = false;

	@Override
	public void pack(List<V1Pod> pods, String namespace, Date date) throws ApiException {
		
		for (Entry<String, WorkerNode> entry : ResourceProfiler.getBatchNodes(date).entrySet()) {
			String type = entry.getKey();
			WorkerNode batch = entry.getValue();
			System.out.println("Start autoscaling");
			System.out.println(batch.getCpuavailable().divide(batch.getCpucapacity(), 3, RoundingMode.CEILING));
			System.out.println(batch.getRamavailable().divide(batch.getRamcapacity(), 3, RoundingMode.CEILING));
			if ((batch.getCpuavailable().divide(batch.getCpucapacity(), 3, RoundingMode.CEILING)
					.compareTo(new BigDecimal(0.2)) < 0
					|| batch.getRamavailable().divide(batch.getRamcapacity(), 3, RoundingMode.CEILING)
							.compareTo(new BigDecimal(0.2)) < 0) && !isAutoscaling){
				System.out.println("adding new server");
				isAutoscaling = true;
				String serverName = type + "-" + index;

				new Thread(new Runnable() {

					@Override
					public void run() {
							new OpenStackAdapter().addNodeByName(serverName, "m2.medium");
							isAutoscaling = false;
					}

				}).start();

				index++;

			}
			
			else {
				System.out.println("Out of autoscaling cycle");
			}

		}

	}

}
