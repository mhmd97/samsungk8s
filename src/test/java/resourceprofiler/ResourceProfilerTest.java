package resourceprofiler;

import static adapter.ClusterInfo.IP;
import static adapter.ClusterInfo.TOKEN;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.BatchV1Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1DeleteOptions;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1JobList;
import io.kubernetes.client.util.Config;

public class ResourceProfilerTest {
	private static ApiClient client = Config.fromToken(IP, TOKEN, false);
	private static CoreV1Api api;
	private static BatchV1Api batch;
	private static Date date = new Date();

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		client.getHttpClient().setReadTimeout(60, TimeUnit.SECONDS);
		Configuration.setDefaultApiClient(client);
		api = new CoreV1Api();
		batch = new BatchV1Api();
		client.setDebugging(true);
		ResourceProfiler.clusterSnapshot(date);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		ResourceProfiler.close();
	}

//	@Test
//	public void testListJobs()    {
//		V1JobList jobs = batch.listJobForAllNamespaces(null, null, false, null, null, null, null, null, null);
//		for (V1Job job : jobs.getItems()) {
//
//			if (job.getMetadata().getName().startsWith("sleeplarge")) {
//				V1DeleteOptions body = new V1DeleteOptions();
//				try {
//					batch.deleteNamespacedJob(job.getMetadata().getName(), "default", "true", body);
//				} catch (ApiException e) {
//					e.printStackTrace();
//					System.out.println("finish deletion");
//				}
//				System.out.println("start to create");
//				
//					job.getSpec().getTemplate().getMetadata().setLabels(null);
//					job.getSpec().setSelector(null);
//					job.getMetadata().setResourceVersion(null);
//					
//					batch.createNamespacedJob("default", job,  pretty);
//			}
//
//			
//
//		}
//
//		System.out.println("fin");
//	}

//	@Test
//	public void testSchedulePods() throws ApiException {
//		V1PodList podList = api.listPodForAllNamespaces(null, null, false, null, null, null, null, null, null);
//		for (V1Pod pod : podList.getItems()) {
//			ResourceProfiler.bestFitSchedule(pod, date);
////			assertEquals(ResourceProfiler.schedulePod(pod, date));
//		}
//	}

//	@Test
//	public void deprovision() throws ApiException {
//		V1PodList podList = api.listPodForAllNamespaces(null, null, false, null, null, null, null, null, null);
//		for (V1Node node : api.listNode("true", null, null, false, null, null, null, null, null).getItems()) {
//			for (V1Pod pod : podList.getItems()) {
//				ResourceProfiler.update(node.getMetadata().getName(), pod, date, false);
//			}
//
//		}
//	}
//	
//	@Test
//	public void provision() throws ApiException {
//		V1PodList podList = api.listPodForAllNamespaces(null, null, false, null, null, null, null, null, null);
//		for (V1Node node : api.listNode("true", null, null, false, null, null, null, null, null).getItems()) {
//			for (V1Pod pod : podList.getItems()) {
//				ResourceProfiler.update(node.getMetadata().getName(), pod, date, true);
//			}
//
//		}
//	}

}
