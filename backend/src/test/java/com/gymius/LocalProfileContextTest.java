package com.gymius;

import com.gymius.config.DevAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("local")
@AutoConfigureMockMvc
class LocalProfileContextTest {

    @Autowired
    private DevAuthenticationFilter devAuthenticationFilter;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void startsWithoutExternalServicesAndProvidesLocalAuthentication() throws Exception {
        assertThat(devAuthenticationFilter).isNotNull();
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("local@gymius.dev"));

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.database").value("ready"));
    }
}
