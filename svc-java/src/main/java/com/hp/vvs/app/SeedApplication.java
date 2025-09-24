package com.hp.vvs.app;

import com.hp.vvs.app.seed.FixtureSeeder;
import java.nio.file.Path;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class SeedApplication {

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(Application.class)
                .web(WebApplicationType.NONE)
                .run(args);
        try {
            FixtureSeeder seeder = context.getBean(FixtureSeeder.class);
            seeder.seed(
                    Path.of("fixtures/master.sample.csv"),
                    Path.of("fixtures/ledger.sample.csv"));
        } finally {
            context.close();
        }
    }
}
