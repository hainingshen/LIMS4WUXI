package com.njxs.lims;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
    "lims.auth.enabled=false",
    "lims.persistence.enabled=false"
})
class LimsApplicationTests {

  @Test
  void contextLoads() {
  }
}
