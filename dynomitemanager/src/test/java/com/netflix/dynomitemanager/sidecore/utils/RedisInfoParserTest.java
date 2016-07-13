package com.netflix.dynomitemanager.sidecore.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Iterator;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * Unitests for RedisInfoParser
 * 
 * @author diegopacheco
 *
 */
public class RedisInfoParserTest {

	@Test
	public void testParserOk() throws FileNotFoundException, Exception {
		File file = new File(new File(".").getCanonicalPath() + "/src/test/resources/redis_info.txt");

		RedisInfoParser parser = new RedisInfoParser();
		Map<String, Long> metrics = parser.parse(new FileReader(file));

		System.out.println("All Parserd Metrics");
		Iterator<String> iter = metrics.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			Long value = metrics.get(key);
			System.out.println("Key: " + key + ", value: " + value);
		}

		Assert.assertEquals("18803", metrics.get("Redis_Server_uptime_in_seconds").toString());
		Assert.assertEquals("1", metrics.get("Redis_Clients_connected_clients").toString());
		Assert.assertEquals("0", metrics.get("Redis_Clients_client_longest_output_list").toString());
		Assert.assertEquals("0", metrics.get("Redis_Clients_client_biggest_input_buf").toString());
		Assert.assertEquals("0", metrics.get("Redis_Clients_blocked_clients").toString());
		Assert.assertEquals("2504768", metrics.get("Redis_Memory_used_memory").toString());
		Assert.assertEquals("360",metrics.get("Redis_Memory_mem_fragmentation_ratio").toString());
		Assert.assertEquals("1468428979",metrics.get("Redis_Persistence_rdb_last_save_time").toString());
		Assert.assertEquals("0",metrics.get("Redis_Persistence_aof_enabled").toString());
		Assert.assertEquals("0",metrics.get("Redis_Persistence_aof_rewrite_in_progress").toString());
		Assert.assertEquals("1",metrics.get("Redis_Stats_total_connections_received").toString());
		Assert.assertEquals("0",metrics.get("Redis_Stats_total_commands_processed").toString());
		Assert.assertEquals("0",metrics.get("Redis_Stats_instantaneous_ops_per_sec").toString());
		Assert.assertEquals("0",metrics.get("Redis_Stats_rejected_connections").toString());
		Assert.assertEquals("0",metrics.get("Redis_Stats_expired_keys").toString());
		Assert.assertEquals("0",metrics.get("Redis_Stats_evicted_keys").toString());
		Assert.assertEquals("0",metrics.get("Redis_Stats_keyspace_hits").toString());
		Assert.assertEquals("0",metrics.get("Redis_Stats_keyspace_misses").toString());
		Assert.assertEquals("14",metrics.get("Redis_CPU_used_cpu_sys").toString());
		Assert.assertEquals("5",metrics.get("Redis_CPU_used_cpu_user").toString());
		Assert.assertEquals("16850",metrics.get("Redis_Keyspace_db0_keys").toString());
		Assert.assertEquals("0",metrics.get("Redis_Keyspace_db0_expires").toString());
		Assert.assertEquals("0",metrics.get("Redis_Keyspace_db0_avg_ttl").toString());
	}
	
	@Test
	public void testParserGetWrongKey() throws FileNotFoundException, Exception {
		File file = new File(new File(".").getCanonicalPath() + "/src/test/resources/redis_info.txt");

		RedisInfoParser parser = new RedisInfoParser();
		Map<String, Long> metrics = parser.parse(new FileReader(file));

		Assert.assertEquals(null,metrics.get("Redis_key_does_not_exists"));
	}

}
