package resourceprofiler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import io.kubernetes.client.ApiException;

public class CassandraDao {
	private CassandraCluster cluster;
	private String namespace;
	private PreparedStatement insert;
	private PreparedStatement filterByCapacity;
	private PreparedStatement delete;
	private PreparedStatement filterByName;
	private PreparedStatement filterByDate;
	private PreparedStatement fetchAll;
	private PreparedStatement LRP;

	public CassandraDao(CassandraCluster cluster, String namespace) {
		this.cluster = cluster;
		this.namespace = namespace;
		Session session = cluster.getSession(namespace);
		insert = session.prepare(
				"insert into servers (name, type, ramcapacity, cpucapacity, ramavailable, cpuavailable, event_time) values (?, ?, ?, ?, ?, ?, ?)");

		filterByCapacity = session.prepare(
//				"select name, ramavailable, cpuavailable from servers where event_time = ? and (ramavailable,cpuavailable) > (?,?) and type = ? ");
	"select name, ramavailable, cpuavailable from servers where event_time = ? and (ramavailable,cpuavailable) > (?,?) ");
		
		LRP = session.prepare(
				"select name, ramavailable, cpuavailable from servers where event_time = ? and (ramavailable,cpuavailable) > (?,?)");

		delete = session.prepare(
				"delete from servers where event_time = ? and ramavailable = ? and cpuavailable = ? and name = ?");

		filterByName = session.prepare("select * from servers where event_time= ? and name = ? ");

		filterByDate = session
				.prepare("select * from servers where event_time = ?");
		
		fetchAll = session.prepare("select * from servers");

//		session.execute("truncate servers");
		session.close();
	}

	public void batchInsert(List<WorkerNode> nodes) throws ApiException {
		Session session = cluster.getSession(namespace);
		for (WorkerNode node : nodes) {
			System.out.println("Inserting node: " + node.toString());
			BoundStatement bs = insert.bind(node.getName(), node.getType(), node.getRamcapacity(),
					node.getCpucapacity(), node.getRamavailable(), node.getCpuavailable(), node.getEvent_time());
			session.execute(bs);

		}
		session.close();
	}

	public void insert(WorkerNode node) throws ApiException {
		Session session = cluster.getSession(namespace);
		BoundStatement bs = insert.bind(node.getName(), node.getType(), node.getRamcapacity(),
				node.getCpucapacity(), node.getRamavailable(), node.getCpuavailable(), node.getEvent_time());
		session.execute(bs);
		System.out.println("insert node: " + node.toString());
		session.close();
	}

	public List<WorkerNode> filterNodeByDate(Date date) {
		Session session = cluster.getSession(namespace);
		BoundStatement bs = filterByDate.bind(date);
		ResultSet rs = session.execute(bs);
		List<WorkerNode> nodes = new ArrayList<WorkerNode>();

		rs.forEach(row -> nodes.add(new WorkerNode(row.getString("name"), row.getString("type"), date, row.getDecimal("ramcapacity"), row.getDecimal("cpucapacity"),
				row.getDecimal("ramavailable"), row.getDecimal("cpuavailable"))));

		session.close();
		return nodes;
	}
	
	public List<WorkerNode> fetchAll() {
		Session session = cluster.getSession(namespace);
		BoundStatement bs = fetchAll.bind();
		ResultSet rs = session.execute(bs);
		List<WorkerNode> nodes = new ArrayList<WorkerNode>();

		rs.forEach(row -> nodes.add(new WorkerNode(row.getString("name"), row.getString("type"), row.getTimestamp("event_time"), row.getDecimal("ramcapacity"), row.getDecimal("cpucapacity"),
				row.getDecimal("ramavailable"), row.getDecimal("cpuavailable"))));

		session.close();
		return nodes;
	}

	public String filterNodeByCapacity(BigDecimal ram, BigDecimal cpu, String type, Date date) {
		Session session = cluster.getSession(namespace);
//		BoundStatement bs = filterByCapacity.bind(date, ram, cpu, type);
		BoundStatement bs = filterByCapacity.bind(date, ram, cpu);
		ResultSet rs = session.execute(bs);
		
		if (!rs.isExhausted()) {
			for(Row row : rs.all()) {
//				if(row.getDecimal("ramavailable").compareTo(ram) >= 0 && row.getDecimal("cpuavailable").compareTo(cpu) >=0 ) {
				if(!row.getString(0).contains("master") && row.getDecimal("ramavailable").compareTo(ram) >= 0 && row.getDecimal("cpuavailable").compareTo(cpu) >=0 ) {
					return row.getString(0);
				}
			}
			
		}
		session.close();
		return null;
	}
	
	public String filterLRPNodeByCapacity(BigDecimal ram, BigDecimal cpu, String type, Date date) {
		Session session = cluster.getSession(namespace);
		BoundStatement bs = LRP.bind(date, ram, cpu);
		ResultSet rs = session.execute(bs);
		if (!rs.isExhausted()) {
			List<Row> rows = rs.all();
			for(int i = rows.size()-1 ; i>=0; i--) {
				
				if(!rows.get(i).getString(0).contains("master") && rows.get(i).getDecimal("ramavailable").compareTo(ram) >= 0 && rows.get(i).getDecimal("cpuavailable").compareTo(cpu) >=0 ) {
					return rows.get(i).getString(0);
				}
			}
		}
		session.close();
		return null;
	}

//	public String filterRescheduledNode(BigDecimal ram, BigDecimal cpu, String type, Date date, String node) {
//		Session session = cluster.getSession(namespace);
//		BoundStatement bs = filterByCapacity.bind(date, ram, cpu, type);
//		ResultSet rs = session.execute(bs);
//		Iterator<Row> it = rs.iterator();
//
//		while (it.hasNext()) {
//			String target = it.next().getString(0);
//			if (!node.equals(target)) {
//				return target;
//			}
//		}
//
//		session.close();
//		return null;
//	}

	public void delete(BigDecimal ram, BigDecimal cpu, Date date, String name) {
		Session session = cluster.getSession(namespace);
		BoundStatement bs = delete.bind(date, ram, cpu, name);
		session.execute(bs);

		session.close();

	}

	public WorkerNode findNodeByName(String name, Date date) {
		Session session = cluster.getSession(namespace);
		BoundStatement bs = filterByName.bind(date, name);
		ResultSet rs = session.execute(bs);
		WorkerNode node = null;
		if (!rs.isExhausted()) {
			Row row = rs.one();
			node = new WorkerNode(name, row.getString("type"), date,
					row.getDecimal("ramcapacity"), row.getDecimal("cpucapacity"), row.getDecimal("ramavailable"),
					row.getDecimal("cpuavailable"));
		}

		session.close();
		return node;
	}
	


	public void close() {
		cluster.close();
	}
}
