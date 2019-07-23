package taskpacker;

import java.math.BigDecimal;

import io.kubernetes.client.models.V1Pod;

public class Pod {
	private V1Pod pod;
	private BigDecimal cpu;
	private BigDecimal ram;
	
	public Pod(V1Pod pod, BigDecimal cpu, BigDecimal ram) {
		super();
		this.pod = pod;
		this.cpu = cpu;
		this.ram = ram;
	}
	public V1Pod getPod() {
		return pod;
	}
	public void setPod(V1Pod pod) {
		this.pod = pod;
	}
	public BigDecimal getCpu() {
		return cpu;
	}
	public void setCpu(BigDecimal cpu) {
		this.cpu = cpu;
	}
	public BigDecimal getRam() {
		return ram;
	}
	public void setRam(BigDecimal ram) {
		this.ram = ram;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((pod == null) ? 0 : pod.hashCode());
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
		Pod other = (Pod) obj;
		if (pod == null) {
			if (other.pod != null)
				return false;
		} else if (!pod.equals(other.pod))
			return false;
		return true;
	}
	
}
