package com.hbsoo.message.queue;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageQueueAutoConfigurationTest {

    @Test
    void autoConfigurationImports_registersMessageQueue() throws Exception {
        String path = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, "AutoConfiguration.imports must exist");
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(content.contains("com.hbsoo.message.queue.config.MessageQueueAutoConfiguration"));
        }
    }

}
