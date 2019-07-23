package resourceprofiler;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.Session;

public class CassandraCluster {

	private Cluster cluster;
	 
    public CassandraCluster(String node) {
        Builder b = Cluster.builder().addContactPoint(node);
        cluster = b.build();
        
    }
 
    public Session getSession(String namespace) {
        return cluster.connect(namespace);
    }
 
    public void close() {
        cluster.close();
    }


}
