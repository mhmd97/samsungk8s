	CREATE TABLE servers (
	event_time timestamp,
	name text,
	type text,	
	ramcapacity decimal,
	cpucapacity decimal,
	ramavailable decimal,
	cpuavailable decimal,
	PRIMARY KEY ((event_time), ramavailable, cpuavailable, name)
	)WITH CLUSTERING ORDER BY (ramavailable ASC);
	create index name_idx on servers(name);
	create index type_idx on servers(type);
	