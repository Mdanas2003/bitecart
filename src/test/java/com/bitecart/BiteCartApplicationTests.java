package com.bitecart;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BiteCartApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Phase 0 application context starts successfully.
    }
}
