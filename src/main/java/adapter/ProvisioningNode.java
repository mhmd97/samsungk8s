package adapter;

public class ProvisioningNode {

	private String name;
	private String id;
	private double RAMCapacity;
	private double CPUCapacity;
	
	public ProvisioningNode(String name, String id, double rAMCapacity, double cPUCapacity) {
		super();
		this.id = id;
		this.name = name;
		RAMCapacity = rAMCapacity;
		CPUCapacity = cPUCapacity;
	}

	public String getName() {
		return name;
	}

	public double getRAMCapacity() {
		return RAMCapacity;
	}

	public double getCPUCapacity() {
		return CPUCapacity;
	}
	
	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return "ProvisioningNode [name=" + name + ", id=" + id + ", RAMCapacity=" + RAMCapacity + ", CPUCapacity="
				+ CPUCapacity + "]";
	}
	
	
}
