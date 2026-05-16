package com.hbsoo.server.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AutoConfigurationImportsRegressionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NetworkServerAutoConfiguration.class));

    @Test
    void networkServerAutoConfiguration_isLoadable() {
        contextRunner
                .withPropertyValues(
                        "hbsoo.server.id=9999",
                        "hbsoo.server.insideServers[0].host=127.0.0.1",
                        "hbsoo.server.insideServers[0].port=29999",
                        "hbsoo.server.insideServers[0].type=test",
                        "hbsoo.server.insideServers[0].id=9999"
                )
                .run(context -> assertThat(context).hasSingleBean(NetworkServerAutoConfiguration.class));
    }

}
