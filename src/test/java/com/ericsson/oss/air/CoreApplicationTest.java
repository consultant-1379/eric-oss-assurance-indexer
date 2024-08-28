/*******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.air;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {CoreApplication.class, CoreApplicationTest.class})
class CoreApplicationTest {
    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mvc;

    @Value("${info.app.description}")
    private String description;

    @BeforeEach
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @AfterEach
    public void tearDown() {
        webApplicationContext.getBean(RecordConsumer.class).stop();
    }

    @Test
    void metrics_available() throws Exception {
        final MvcResult result = mvc.perform(get("/actuator/prometheus").contentType(MediaType.TEXT_PLAIN)).andExpect(status().isOk())
                .andReturn();
        System.out.println("***>Prometheus Endpoint Availability Test Result: \n" + result.getResponse().getContentAsString());
        // Test for the presence of only the Indexer's StartUp metric since that's the only one that is
        // reported at runtime when the ApplicationReadyEvent has been raised.
        Assertions.assertTrue(result.getResponse().getContentAsString().contains("ais_startup_time_seconds"));
        // Test for presence of some other expected metrics.
        Assertions.assertTrue(result.getResponse().getContentAsString().contains("jvm_threads_states_threads"));
        Assertions.assertTrue(result.getResponse().getContentAsString().contains("jvm_memory_used_bytes"));
        Assertions.assertTrue(result.getResponse().getContentAsString().contains("process_uptime_seconds"));
        final MvcResult resultMetrics = mvc.perform(get ("/actuator/metrics").contentType(MediaType.TEXT_PLAIN))
            .andExpect(status().isOk()).andReturn();
        System.out.println("***>Metrics Endpoint Availability Test Result: \n" + resultMetrics.getResponse().getContentAsString());
        Assertions.assertTrue(resultMetrics.getResponse().getContentAsString().contains("jvm.threads.states"));
        Assertions.assertTrue(resultMetrics.getResponse().getContentAsString().contains("jvm.memory.used"));
        Assertions.assertTrue(resultMetrics.getResponse().getContentAsString().contains("process.uptime"));

    }

    @Test
    void info_available() throws Exception {
        final MvcResult result = mvc.perform(get("/actuator/info").contentType(MediaType.TEXT_PLAIN)).andExpect(status().isOk())
                .andReturn();
        Assertions.assertTrue(result.getResponse().getContentAsString().contains(this.description));
    }
}
