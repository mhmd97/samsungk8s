package resourceprofiler;

import java.math.BigDecimal;
import java.util.Date;

public class WorkerNode {
	private Date event_time;
	private String name;
	private String type;
	private BigDecimal ramcapacity;
	private BigDecimal cpucapacity;
	private BigDecimal ramavailable;
	private BigDecimal cpuavailable;

	public WorkerNode(String name, String type, Date event_time, BigDecimal ramcapacity,
			BigDecimal cpucapacity, BigDecimal ramavailable, BigDecimal cpuavailable) {
		super();
		this.name = name;
		this.type = type;
		this.event_time = event_time;
		this.ramcapacity = ramcapacity;
		this.cpucapacity = cpucapacity;
		this.ramavailable = ramavailable;
		this.cpuavailable = cpuavailable;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Date getEvent_time() {
		return event_time;
	}

	public void setEvent_time(Date event_time) {
		this.event_time = event_time;
	}

	public BigDecimal getRamcapacity() {
		return ramcapacity;
	}

	public void setRamcapacity(BigDecimal ramcapacity) {
		this.ramcapacity = ramcapacity;
	}

	public BigDecimal getCpucapacity() {
		return cpucapacity;
	}

	public void setCpucapacity(BigDecimal cpucapacity) {
		this.cpucapacity = cpucapacity;
	}

	public BigDecimal getRamavailable() {
		return ramavailable;
	}

	public void setRamavailable(BigDecimal ramavailable) {
		this.ramavailable = ramavailable;
	}

	public BigDecimal getCpuavailable() {
		return cpuavailable;
	}

	public void setCpuavailable(BigDecimal cpuavailable) {
		this.cpuavailable = cpuavailable;
	}

	@Override
	public String toString() {
		return "WorkerNode [name=" + name + ", type=" + type + ", event_time=" + event_time + ", ramcapacity="
				+ ramcapacity + ", cpucapacity=" + cpucapacity + ", ramavailable=" + ramavailable + ", cpuavailable="
				+ cpuavailable + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		WorkerNode other = (WorkerNode) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
