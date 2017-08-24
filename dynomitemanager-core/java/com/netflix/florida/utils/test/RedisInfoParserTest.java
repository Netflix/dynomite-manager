package com.netflix.florida.utils.test;

import java.io.File;
import java.io.FileReader;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.netflix.florida.sidecore.storage.RedisInfoParser;

public class RedisInfoParserTest {

    @Test
    public void test() throws Exception {

        File file = new File("./src/test/resources/redis_info.txt");

        RedisInfoParser parser = new RedisInfoParser();
        Map<String, Long> metrics = parser.parse(new FileReader(file));

        Assert.assertTrue(metrics.get("Redis_Server_uptime_in_seconds") == 1234L);
        Assert.assertTrue(metrics.get("Redis_Clients_connected_clients") == 4L);
        Assert.assertTrue(metrics.get("Redis_Clients_client_longest_output_list") == 0L);
        Assert.assertTrue(metrics.get("Redis_Clients_client_biggest_input_buf") == 0L);
        Assert.assertTrue(metrics.get("Redis_Clients_blocked_clients") == 0L);
        Assert.assertTrue(metrics.get("Redis_Memory_used_memory") == 314569968L);
        // Assert.assertTrue(metrics.get("Redis_Memory_used_memory_human") ==
        // 300L);
        // Assert.assertTrue(metrics.get("Redis_Memory_used_memory_rss") ==
        // 328806400L);
        // Assert.assertTrue(metrics.get("Redis_Memory_used_memory_peak") ==
        // 314569968L);
        // Assert.assertTrue(metrics.get("Redis_Memory_used_memory_peak_human")
        // == 300L);
        // Assert.assertTrue(metrics.get("Redis_Memory_used_memory_lua") ==
        // 33792L);
        Assert.assertTrue(metrics.get("Redis_Memory_mem_fragmentation_ratio") == 105L);
        // Assert.assertTrue(metrics.get("Redis_Persistence_loading") == 0L);
        // Assert.assertTrue(metrics.get("Redis_Persistence_rdb_changes_since_last_save")
        // == 53046299L);
        // Assert.assertTrue(metrics.get("Redis_Persistence_rdb_bgsave_in_progress")
        // == 0L);
        // Assert.assertTrue(metrics.get("Redis_Persistence_rdb_last_save_time")
        // == 1411544331L);
        // Assert.assertTrue(metrics.get("Redis_Persistence_rdb_last_bgsave_status")
        // == 1L);
        // Assert.assertTrue(metrics.get("Redis_Persistence_rdb_last_bgsave_time_sec")
        // == -1L);
        // Assert.assertTrue(metrics.get("Redis_Persistence_rdb_current_bgsave_time_sec")
        // == -1L);
        // Assert.assertTrue(metrics.get("Redis_Persistence_aof_enabled") ==
        // 0L);
        // Assert.assertTrue(metrics.get("Redis_Persistence_aof_rewrite_in_progress")
        // == 0L);
        // Assert.assertTrue(metrics.get("Redis_Persistence_aof_rewrite_scheduled")
        // == 0L);
        // Assert.assertTrue(metrics.get("Redis_Persistence_aof_last_rewrite_time_sec")
        // == -1L);
        // Assert.assertTrue(metrics.get("Redis_Persistence_aof_current_rewrite_time_sec")
        // == -1L);
        // Assert.assertTrue(metrics.get("Redis_Persistence_aof_last_bgrewrite_status")
        // == 1L);
        // Assert.assertTrue(metrics.get("Redis_Persistence_aof_last_write_status")
        // == 1L);
        Assert.assertTrue(metrics.get("Redis_Stats_total_connections_received") == 3995L);
        Assert.assertTrue(metrics.get("Redis_Stats_total_commands_processed") == 94308679L);
        Assert.assertTrue(metrics.get("Redis_Stats_instantaneous_ops_per_sec") == 6321L);
        Assert.assertTrue(metrics.get("Redis_Stats_rejected_connections") == 0L);
        // Assert.assertTrue(metrics.get("Redis_Stats_sync_full") == 0L);
        // Assert.assertTrue(metrics.get("Redis_Stats_sync_partial_ok") == 0L);
        // Assert.assertTrue(metrics.get("Redis_Stats_sync_partial_err") == 0L);
        Assert.assertTrue(metrics.get("Redis_Stats_expired_keys") == 0L);
        Assert.assertTrue(metrics.get("Redis_Stats_evicted_keys") == 0L);
        Assert.assertTrue(metrics.get("Redis_Stats_keyspace_hits") == 41254397L);
        Assert.assertTrue(metrics.get("Redis_Stats_keyspace_misses") == 0L);
        // Assert.assertTrue(metrics.get("Redis_Stats_pubsub_channels") == 0L);
        // Assert.assertTrue(metrics.get("Redis_Stats_pubsub_patterns") == 0L);
        // Assert.assertTrue(metrics.get("Redis_Stats_latest_fork_usec") == 0L);
        // Assert.assertTrue(metrics.get("Redis_Replication_connected_slaves")
        // == 0L);
        // Assert.assertTrue(metrics.get("Redis_Replication_master_repl_offset")
        // == 0L);
        // Assert.assertTrue(metrics.get("Redis_Replication_repl_backlog_active")
        // == 0L);
        // Assert.assertTrue(metrics.get("Redis_Replication_repl_backlog_size")
        // == 1048576L);
        // Assert.assertTrue(metrics.get("Redis_Replication_repl_backlog_first_byte_offset")
        // == 0L);
        // Assert.assertTrue(metrics.get("Redis_Replication_repl_backlog_histlen")
        // == 0L);
        Assert.assertTrue(metrics.get("Redis_CPU_used_cpu_sys") == 2052L);
        Assert.assertTrue(metrics.get("Redis_CPU_used_cpu_user") == 793L);
        // Assert.assertTrue(metrics.get("Redis_CPU_used_cpu_sys_children") ==
        // 0L);
        // Assert.assertTrue(metrics.get("Redis_CPU_used_cpu_user_children") ==
        // 0L);
        Assert.assertTrue(metrics.get("Redis_Keyspace_db0_keys") == 2499968L);
        Assert.assertTrue(metrics.get("Redis_Keyspace_db0_expires") == 0L);
        Assert.assertTrue(metrics.get("Redis_Keyspace_db0_avg_ttl") == 0L);
    }

}
