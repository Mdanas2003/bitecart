package com.bitecart.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHardeningIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void malformedPublicRouteUsesSafeBadRequestPageAndSecurityHeaders() throws Exception {
        mockMvc.perform(get("/products/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/bad-request"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotAccessAdminAndCsrfProtectsStateChanges() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-Frame-Options", "DENY"));
        mockMvc.perform(post("/cart/clear"))
                .andExpect(status().isForbidden());
    }
}
