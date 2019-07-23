package taskpacker;

public class PackedServer {
	private String serverName;
	private String flavorName;

	public PackedServer(String serverName, String flavorName) {
		this.serverName = serverName;
		this.flavorName = flavorName;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getFlavorName() {
		return flavorName;
	}

	public void setFlavorName(String flavorName) {
		this.flavorName = flavorName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((flavorName == null) ? 0 : flavorName.hashCode());
		result = prime * result + ((serverName == null) ? 0 : serverName.hashCode());
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
		PackedServer other = (PackedServer) obj;
		if (flavorName == null) {
			if (other.flavorName != null)
				return false;
		} else if (!flavorName.equals(other.flavorName))
			return false;
		if (serverName == null) {
			if (other.serverName != null)
				return false;
		} else if (!serverName.equals(other.serverName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PackedServer [serverName=" + serverName + ", flavorName=" + flavorName + "]";
	}
	
	
	
	

}
