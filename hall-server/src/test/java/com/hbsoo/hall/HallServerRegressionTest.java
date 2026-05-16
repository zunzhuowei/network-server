package com.hbsoo.hall;

import com.hbsoo.server.NetworkServer;
import com.hbsoo.server.config.ServerInfoProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = HallServerApplication.class)
@ActiveProfiles("regression")
class HallServerRegressionTest {

    @Autowired
    private ServerInfoProperties serverInfoProperties;

    @Autowired
    private NetworkServer insideServer;

    @Test
    void contextLoads_andInsideServerBindsExpectedPort() {
        assertEquals(2000, serverInfoProperties.getId());
        assertNotNull(insideServer);
        assertEquals(26003, serverInfoProperties.getInsideServers().stream()
                .filter(s -> s.getId().equals(2000))
                .findFirst()
                .orElseThrow()
                .getPort());
    }

}
