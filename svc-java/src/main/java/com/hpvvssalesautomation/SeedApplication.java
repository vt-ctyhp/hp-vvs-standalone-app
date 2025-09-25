package com.hpvvssalesautomation;

import com.hpvvssalesautomation.seed.SeedRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SeedApplication {

    public static void main(String[] args) throws Exception {
        String fixturesDirProp = System.getProperty("fixtures.dir");
        Path fixturesDir = fixturesDirProp != null ? Paths.get(fixturesDirProp) : Paths.get("..", "fixtures").toAbsolutePath().normalize();

        ConfigurableApplicationContext context = new SpringApplicationBuilder(Application.class)
                .web(WebApplicationType.NONE)
                .run(args);

        try (context) {
            SeedRunner runner = context.getBean(SeedRunner.class);
            runner.run(fixturesDir);
        }
    }
}
