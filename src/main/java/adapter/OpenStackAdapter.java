package adapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.api.compute.ComputeService;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.openstack.OSFactory;

public class OpenStackAdapter {

	private OSClientV3 os;
	private static final String endpoint = "https://keystone.rc.nectar.org.au:5000/v3/";
	private static final String username = "your user name";
	private static final String password = "your password";
	private static final String domain = "Default";
	private static final String projectId = "you pj id";
	private static final String k8sWorkerImgId = "your img id";
	private static final String defaultFlavorName = "m2.small";
	private String defaultFlavorId = null;
	private static final String KNOWNHOST = "/home/davidzhong/.ssh/known_hosts";
	private ComputeService cs;
	
	public OpenStackAdapter() {
		os = OSFactory.builderV3().endpoint(endpoint).credentials(username, password, Identifier.byName(domain))
				.scopeToProject(Identifier.byId(projectId)).authenticate();
		cs = os.compute();

		// Get default flavor id
		defaultFlavorId = getFlavorId(defaultFlavorName);
	}

	private String getFlavorId(String flavorName) {
		@SuppressWarnings("unchecked")
		List<Flavor> flavors = (List<Flavor>) cs.flavors().list();

		for (Flavor f : flavors) {
			// System.out.println(f.getName());
			if (f.getName().equals(flavorName)) {
				return f.getId();
			}
		}
		return null;
	}

	public static void main(String[] args) throws IOException {
		OpenStackAdapter os = new OpenStackAdapter();
		os.addNodeByName("worker_migration", "m3.xsmall");
	}

	private void joinCluster(String serverName) {

		try {
			String address = null;
			while (address == null || address.isEmpty()) {
				Thread.sleep(60000);

				for (Server server : os.compute().servers().list()) {
					if (server.getName().equals(serverName)) {
						address = server.getAccessIPv4();
						break;
					}
				}
			}
			String command = "ssh -i /home/davidzhong/Documents/davidzhong.pem ubuntu@" + address
					+ " sudo /home/ubuntu/join.sh";

			System.out.println(command);
			
			Process cmdProc;
			String line;
			boolean flag = true;

			while (flag) {
				Thread.sleep(120000);
				System.out.println("ssh-keyscan " + address + " >> " + KNOWNHOST);
				cmd("ssh-keyscan " + address + " >> " + KNOWNHOST);
				cmdProc = Runtime.getRuntime().exec(command);

				BufferedReader is = new BufferedReader(new InputStreamReader(cmdProc.getErrorStream()));
				flag = false;
				while ((line = is.readLine()) != null) {
					System.out.println(line);
					if (line.contains("Connection refused")) {
						flag = true;
						System.out.println("ssh-keygen -R " + address);
						cmd("ssh-keygen -R " + address);
						break;
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public ProvisioningNode addNodeByName(String serverName, String flavorName) {

		String flavorId = defaultFlavorId;
		if (flavorName != null) {
			flavorId = getFlavorId(flavorName);
		}

		// Create a Server Model Object
		ServerCreate sc = Builders.server()
				.name(serverName)
				.flavor(flavorId)
				.image(k8sWorkerImgId)
				.addSecurityGroup("k8s_base")
				.addSecurityGroup("k8s_worker")
				.addSecurityGroup("http")
				.addSecurityGroup("icmp")
				.addSecurityGroup("ssh")
				.availabilityZone("melbourne-qh2")
				.keypairName("davidzhong")
				.build();

		// Boot the Server
		Server server = os.compute().servers().boot(sc);
		Flavor flavor = os.compute().flavors().get(flavorId);
		// server.getFlavor().getRam() server.getFlavor returns null for some reason

		double ramBytes = flavor.getRam() * 1048576.0;
		// Must account for control plane kubernetes pods
		ramBytes -= 151397597.184;

		joinCluster(serverName);

		return new ProvisioningNode(serverName, server.getId(), ramBytes, flavor.getVcpus());

	}

	/*
	 * public void addNodeAndWait(String flavorName) {
	 * 
	 * String flavorId = defaultFlavorId; if(flavorName != null) { flavorId =
	 * getFlavorId(flavorName); }
	 * 
	 * //String flavorId = getFlavorId(flavorName);
	 * 
	 * // Create a Server Model Object String serverName = serverNamePrefix +
	 * serverCounter; ServerCreate sc = Builders.server().name(serverName)
	 * .flavor(flavorId) .image(k8sWorkerImgId) .addSecurityGroup("k8s_base")
	 * .addSecurityGroup("http") .addSecurityGroup("icmp") .addSecurityGroup("ssh")
	 * .availabilityZone("melbourne-np") .keypairName("nectar") .build();
	 * 
	 * // Boot the Server os.compute().servers().bootAndWaitActive(sc,
	 * bootAndWaitInterval );
	 * 
	 * serverCounter++;
	 * AccountingManager.getInstance().registerNodeStart(serverName, true); }
	 */

	public void deleteNode(String serverName, boolean scaleIn) {
		String serverId = getServerId(serverName);
		os.compute().servers().delete(serverId);
	}

	private String getServerId(String serverName) {
		for (Server server : os.compute().servers().list()) {
			if (server.getName().equals(serverName)) {
				return server.getId();
			}
		}
		return null;
	}

	public synchronized boolean isServerActive(String serverId) {
		OSClientV3 osThread = OSFactory.clientFromToken(os.getToken());
		Server server = osThread.compute().servers().get(serverId);
		if (server.getStatus().toString().equals("ACTIVE")) {
			return true;
		}
		return false;
	}

	public static void cmd(String command) {
		try {
			ProcessBuilder builder = new ProcessBuilder();
			builder.command("bash", "-c", command);

			Process p = builder.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
