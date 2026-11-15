package com.bytehealers.healverse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

/**
 * Health probes must work without a token (load balancers, uptime monitors), expose no internals,
 * and nothing else under /actuator may be reachable anonymously.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HealthEndpointTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthIsPublicAndReportsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void healthDoesNotExposeComponentDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(content().string(not(containsString("jdbc"))))
                .andExpect(content().string(not(containsString("diskSpace"))));
    }

    @Test
    void livenessAndReadinessProbesArePublic() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void otherActuatorEndpointsAreNotAnonymouslyReachable() throws Exception {
        for (String path : new String[]{"/actuator", "/actuator/env", "/actuator/beans", "/actuator/heapdump"}) {
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }
}
