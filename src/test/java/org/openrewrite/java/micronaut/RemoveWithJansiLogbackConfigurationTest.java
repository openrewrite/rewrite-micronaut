/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.micronaut;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.java.Assertions.srcMainResources;
import static org.openrewrite.xml.Assertions.xml;

public class RemoveWithJansiLogbackConfigurationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java.micronaut")
          .build()
          .activateRecipes("org.openrewrite.java.micronaut.RemoveWithJansiLogbackConfiguration"));
    }

    @Test
    void removeWithJansi() {
        rewriteRun(mavenProject("project", srcMainResources(
          xml(//language=xml
            """
              <configuration>  
                  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
                      <withJansi>true</withJansi>
                      <!-- encoders are assigned the type
                           ch.qos.logback.classic.encoder.PatternLayoutEncoder by default -->
                      <encoder>
                          <pattern>%cyan(%d{HH:mm:ss.SSS}) %gray([%thread]) %highlight(%-5level) %magenta(%logger{36}) - %msg%n</pattern>
                      </encoder>
                  </appender>
                  <root level="info">
                      <appender-ref ref="STDOUT" />
                  </root>
              </configuration>
              """,
            //language=xml
            """
              <configuration>  
                  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
                      <!-- encoders are assigned the type
                           ch.qos.logback.classic.encoder.PatternLayoutEncoder by default -->
                      <encoder>
                          <pattern>%cyan(%d{HH:mm:ss.SSS}) %gray([%thread]) %highlight(%-5level) %magenta(%logger{36}) - %msg%n</pattern>
                      </encoder>
                  </appender>
                  <root level="info">
                      <appender-ref ref="STDOUT" />
                  </root>
              </configuration>
              """, s -> s.path("logback.xml")))));
    }
}
