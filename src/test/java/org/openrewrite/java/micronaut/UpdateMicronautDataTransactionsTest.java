package org.openrewrite.java.micronaut;


import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

public class UpdateMicronautDataTransactionsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(),
            "javax.transaction-api-1.*", "jakarta.transaction-api-2.*", "micronaut-data-jdbc-4.*"))
          .recipes(Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.micronaut")
            .build()
            .activateRecipes(
              "org.openrewrite.java.micronaut.UpdateMicronautPlatformBom",
              "org.openrewrite.java.micronaut.UpdateMicronautData"));
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
                    <version>4.0.0-RC1</version>
                </parent>
                <dependencies>
                    <dependency>
                        <groupId>io.micronaut.data</groupId>
                        <artifactId>micronaut-data-jdbc</artifactId>
                        <scope>compile</scope>
                    </dependency>
                </dependencies>
                <build>
                    <plugins>
                        <plugin>
                            <groupId>io.micronaut.build</groupId>
                            <artifactId>micronaut-maven-plugin</artifactId>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """)));
    }
}
