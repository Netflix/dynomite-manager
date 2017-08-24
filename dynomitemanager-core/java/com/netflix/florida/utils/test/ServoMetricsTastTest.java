package com.netflix.florida.utils.test;

import java.util.concurrent.ConcurrentHashMap;

import junit.framework.Assert;

import org.junit.Test;

import com.netflix.florida.monitoring.ServoMetricsTask;
import com.netflix.servo.monitor.NumericMonitor;

public class ServoMetricsTastTest {

    @Test
    public void test() throws Exception {

        String json = "{\"service\":\"dynomite\", \"source\":\"florida-i-16ca1846\", \"version\":\"0.3.1\", \"uptime\":40439, \"timestamp\":1399064677, \"datacenter\":\"DC1\","
                + "\"dyn_o_mite\":"
                + "{\"client_eof\":0, \"client_err\":0, \"client_connections\":3, \"server_ejects\":0, \"forward_error\":0, \"fragments\":0, \"stats_count\":22,"
                + "\"127.0.0.1\":"
                + "{\"server_eof\":0, \"server_err\":0, \"server_timedout\":11, \"server_connections\":3, \"server_ejected_at\":0, \"requests\":20000,"
                + "\"request_bytes\":0, \"responses\":5, \"response_bytes\":0, \"in_queue\":0, \"in_queue_bytes\":0, \"out_queue\":0,"
                + "\"out_queue_bytes\":0" + "}" + "}" + "}";

        ServoMetricsTask impl = new ServoMetricsTask(null);
        impl.processJsonResponse(json);

        ConcurrentHashMap<String, NumericMonitor<Number>> metricMap = impl.getMetricsMap();

        testCounterValue("dynomite__client_eof", 0, metricMap);
        testCounterValue("dynomite__client_err", 0, metricMap);
        testCounterValue("dynomite__client_connections", 3, metricMap);
        testCounterValue("dynomite__server_ejects", 0, metricMap);
        testCounterValue("dynomite__forward_error", 0, metricMap);
        testCounterValue("dynomite__fragments", 0, metricMap);
        testCounterValue("dynomite__stats_count", 22, metricMap);
        testCounterValue("dynomite__127.0.0.1__server_eof", 0, metricMap);
        testCounterValue("dynomite__127.0.0.1__server_err", 0, metricMap);
        testCounterValue("dynomite__127.0.0.1__server_timedout", 11, metricMap);
        testCounterValue("dynomite__127.0.0.1__server_connections", 3, metricMap);
        testCounterValue("dynomite__127.0.0.1__server_ejected_at", 0, metricMap);
        testCounterValue("dynomite__127.0.0.1__requests", 20000, metricMap);
        testCounterValue("dynomite__127.0.0.1__request_bytes", 0, metricMap);
        testCounterValue("dynomite__127.0.0.1__responses", 5, metricMap);
        testCounterValue("dynomite__127.0.0.1__response_bytes", 0, metricMap);
        testCounterValue("dynomite__127.0.0.1__in_queue", 0, metricMap);
        testCounterValue("dynomite__127.0.0.1__in_queue_bytes", 0, metricMap);
        testCounterValue("dynomite__127.0.0.1__out_queue", 0, metricMap);
        testCounterValue("dynomite__127.0.0.1__out_queue_bytes", 0, metricMap);
    }

    private void testCounterValue(String name, int expectedValue,
            ConcurrentHashMap<String, NumericMonitor<Number>> metricMap) throws Exception {

        NumericMonitor<Number> metric = metricMap.get(name);
        Assert.assertNotNull(metric);
        Assert.assertEquals(expectedValue, metric.getValue().intValue());
    }

}
