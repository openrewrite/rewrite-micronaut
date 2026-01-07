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


import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class UpdateMicronautDataTest extends Micronaut4RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(),
            "javax.transaction-api-1.*",
            "jakarta.transaction-api-2.*",
            "micronaut-data-jdbc-3.*",
            "micronaut-data-jdbc-4.*",
            "micronaut-data-model-4.*",
            "micronaut-data-tx-3.*",
            "micronaut-data-tx-4.*"))
          .recipeFromResources(
            "org.openrewrite.java.micronaut.UpdateMicronautPlatformBom",
            "org.openrewrite.java.micronaut.UpdateMicronautData");
    }

    @Language("java")
    private final String annotatedJavaxTxRepository = """
      import io.micronaut.data.jdbc.annotation.JdbcRepository;
      import io.micronaut.data.model.query.builder.sql.Dialect;
      import io.micronaut.data.repository.PageableRepository;

      import java.util.Map;

      import javax.transaction.Transactional;

      @JdbcRepository(dialect = Dialect.MYSQL)
      public interface GenreRepository extends PageableRepository<Map<String, Object>, Long> {

          @Transactional
          Map<String, Object> save(String name);

      }
      """;

    @Language("java")
    private final String annotatedJakartaTxRepository = """
      import io.micronaut.data.jdbc.annotation.JdbcRepository;
      import io.micronaut.data.model.query.builder.sql.Dialect;
      import io.micronaut.data.repository.PageableRepository;

      import java.util.Map;

      import jakarta.transaction.Transactional;

      @JdbcRepository(dialect = Dialect.MYSQL)
      public interface GenreRepository extends PageableRepository<Map<String, Object>, Long> {

          @Transactional
          Map<String, Object> save(String name);

      }
      """;

    @DocumentExample
    @Test
    void updateSQLAnnotations() {
        rewriteRun(mavenProject("project",
          //language=java
          srcMainJava(java("""
            import io.micronaut.data.jdbc.annotation.ColumnTransformer;
            import io.micronaut.data.jdbc.annotation.JoinColumn;
            import io.micronaut.data.jdbc.annotation.JoinColumns;
            import io.micronaut.data.jdbc.annotation.JoinTable;

            public class MyEntity {

                @JoinTable(
                            name = "m2m_address_association",
                            joinColumns = @JoinColumns({
                                                  @JoinColumn(name="ADDR_ID", referencedColumnName="ID"),
                                                  @JoinColumn(name="ADDR_ZIP", referencedColumnName="ZIP")
                                              }))
                List<String> addresses;

                @ColumnTransformer(read = "UPPER(org)")
                private String name;

            }
            """, """
            import io.micronaut.data.annotation.sql.ColumnTransformer;
            import io.micronaut.data.annotation.sql.JoinColumn;
            import io.micronaut.data.annotation.sql.JoinColumns;
            import io.micronaut.data.annotation.sql.JoinTable;

            public class MyEntity {

                @JoinTable(
                            name = "m2m_address_association",
                            joinColumns = @JoinColumns({
                                                  @JoinColumn(name="ADDR_ID", referencedColumnName="ID"),
                                                  @JoinColumn(name="ADDR_ZIP", referencedColumnName="ZIP")
                                              }))
                List<String> addresses;

                @ColumnTransformer(read = "UPPER(org)")
                private String name;

            }
            """))));
    }

    @Test
    void updateJavaCodeAndUnmodifiedGradleDependencies() {
        rewriteRun(mavenProject("project", srcMainJava(java(annotatedJavaxTxRepository, annotatedJakartaTxRepository)),
          //language=groovy
          buildGradle("""
            dependencies {
                annotationProcessor("io.micronaut.data:micronaut-data-processor")
                implementation("io.micronaut.data:micronaut-data-jdbc")
            }
            """)));
    }

    @Test
    void updateJavaCodeAndUnmodifiedMavenDependencies() {
        rewriteRun(mavenProject("project", srcMainJava(java(annotatedJavaxTxRepository, annotatedJakartaTxRepository)),
          //language=xml
          pomXml("""
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <parent>
                    <groupId>io.micronaut.platform</groupId>
                    <artifactId>micronaut-parent</artifactId>
                    <version>%s</version>
                </parent>
                <properties>
                    <micronaut.version>%1$s</micronaut.version>
                </properties>
                <dependencies>
                    <dependency>
                        <groupId>io.micronaut.data</groupId>
                        <artifactId>micronaut-data-jdbc</artifactId>
                        <scope>compile</scope>
                    </dependency>
                </dependencies>
            </project>
            """.formatted(latestMicronautVersion))));
    }

    @Test
    void updateJavaCodeAndMavenDataVersion() {
        rewriteRun(mavenProject("project", srcMainJava(java(annotatedJavaxTxRepository, annotatedJakartaTxRepository)),
          //language=xml
          pomXml("""
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <parent>
                      <groupId>io.micronaut.platform</groupId>
                      <artifactId>micronaut-parent</artifactId>
                      <version>%1$s</version>
                  </parent>
                  <properties>
                      <micronaut.version>%1$s</micronaut.version>
                      <micronaut.data.version>%2$s</micronaut.data.version>
                  </properties>
                  <dependencies>
                      <dependency>
                          <groupId>io.micronaut.data</groupId>
                          <artifactId>micronaut-data-jdbc</artifactId>
                          <scope>compile</scope>
                      </dependency>
                  </dependencies>
              </project>
              """.formatted(latestMicronautVersion, "3.10.0"),
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <parent>
                      <groupId>io.micronaut.platform</groupId>
                      <artifactId>micronaut-parent</artifactId>
                      <version>%1$s</version>
                  </parent>
                  <properties>
                      <micronaut.version>%1$s</micronaut.version>
                  </properties>
                  <dependencies>
                      <dependency>
                          <groupId>io.micronaut.data</groupId>
                          <artifactId>micronaut-data-jdbc</artifactId>
                          <scope>compile</scope>
                      </dependency>
                  </dependencies>
              </project>
              """.formatted(latestMicronautVersion))));
    }

    @Test
    void updateTransactionalAdvice() {
        rewriteRun(mavenProject("project",
          //language=java
          srcMainJava(java("""
            import io.micronaut.transaction.annotation.TransactionalAdvice;

            public class MyTxService {

                @TransactionalAdvice
                public void doSomethingTransactional() {

                }
            }
            """, """
            import io.micronaut.transaction.annotation.Transactional;

            public class MyTxService {

                @Transactional
                public void doSomethingTransactional() {

                }
            }
            """))));

    }
}
