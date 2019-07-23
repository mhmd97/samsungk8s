package taskpacker;

public class Flavor {
	private String name;
	private long vcpu;
	private long memory;
	
	public Flavor(String name, long vcpu, long memory) {
		super();
		this.name = name;
		this.vcpu = vcpu;
		this.memory = memory;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getVcpu() {
		return vcpu;
	}
	public void setVcpu(long vcpu) {
		this.vcpu = vcpu;
	}
	public long getMemory() {
		return memory;
	}
	public void setMemory(long memory) {
		this.memory = memory;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (memory ^ (memory >>> 32));
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (int) (vcpu ^ (vcpu >>> 32));
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Flavor other = (Flavor) obj;
		if (memory != other.memory)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (vcpu != other.vcpu)
			return false;
		return true;
	}

}
