package com.pastline.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.pastline.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DatabaseSetupTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void flywayAppliedFirstMigration() {
        Integer applied = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success", Integer.class);
        assertThat(applied).isEqualTo(1);
    }

    @Test
    void vectorExtensionIsInstalled() {
        String version =
                jdbc.queryForObject("SELECT extversion FROM pg_extension WHERE extname = 'vector'", String.class);
        assertThat(version).isNotBlank();
    }
}
