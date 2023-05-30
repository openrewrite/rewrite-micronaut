package org.openrewrite.java.micronaut;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

public class UpdateMicronautValidationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("validation-api", "jakarta.validation-api", "jakarta.inject-api"));
        spec.recipeFromResource("/META-INF/rewrite/micronaut3-to-4.yml", "org.openrewrite.java.micronaut.UpdateMicronautPlatformBom", "org.openrewrite.java.micronaut.UpdateMicronautValidation");
    }

    @Language("java")
    private final String annotatedJavaxClass = """
            import jakarta.inject.Singleton;
            import javax.validation.constraints.NotBlank;
            
            @Singleton
            public class PersonService {
                public void sayHello(@NotBlank String name) {
                    System.out.println("Hello " + name);
                }
            }
        """;

    @Language("java")
    private final String annotatedJakartaClass = """
            import jakarta.inject.Singleton;
            import jakarta.validation.constraints.NotBlank;
            
            @Singleton
            public class PersonService {
                public void sayHello(@NotBlank String name) {
                    System.out.println("Hello " + name);
                }
            }
        """;

    @Language("groovy")
    private final String buildGradleInitial = """
            dependencies {
                annotationProcessor("io.micronaut:micronaut-http-validation")
                implementation("io.micronaut:micronaut-http-client")
                implementation("io.micronaut:micronaut-jackson-databind")
                implementation("io.micronaut:micronaut-validation")
                runtimeOnly("ch.qos.logback:logback-classic")
            }
        """;

    @Language("groovy")
    private final String buildGradleExpected = """
            dependencies {
                annotationProcessor("io.micronaut.validation:micronaut-validation-processor")
                implementation("io.micronaut:micronaut-http-client")
                implementation("io.micronaut:micronaut-jackson-databind")
                implementation("io.micronaut.validation:micronaut-validation")
                runtimeOnly("ch.qos.logback:logback-classic")
            }
        """;

    @Language("xml")
    private final String pomInitial = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <parent>
                    <groupId>io.micronaut</groupId>
                    <artifactId>micronaut-parent</artifactId>
                    <version>3.9.1</version>
                </parent>
                <dependencies>
                    <dependency>
                        <groupId>io.micronaut</groupId>
                        <artifactId>micronaut-http-client</artifactId>
                        <scope>compile</scope>
                    </dependency>
                    <dependency>
                        <groupId>io.micronaut</groupId>
                        <artifactId>micronaut-http-server-netty</artifactId>
                        <scope>compile</scope>
                    </dependency>
                    <dependency>
                        <groupId>io.micronaut</groupId>
                        <artifactId>micronaut-jackson-databind</artifactId>
                        <scope>compile</scope>
                    </dependency>
                    <dependency>
                        <groupId>io.micronaut</groupId>
                        <artifactId>micronaut-validation</artifactId>
                        <scope>compile</scope>
                    </dependency>
                    <dependency>
                        <groupId>ch.qos.logback</groupId>
                        <artifactId>logback-classic</artifactId>
                        <scope>runtime</scope>
                    </dependency>
                </dependencies>
                <build>
                    <plugins>
                        <plugin>
                            <groupId>io.micronaut.build</groupId>
                            <artifactId>micronaut-maven-plugin</artifactId>
                        </plugin>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                            <configuration>
                                <annotationProcessorPaths combine.children="append">
                                    <path>
                                        <groupId>io.micronaut</groupId>
                                        <artifactId>micronaut-http-validation</artifactId>
                                        <version>${micronaut.version}</version>
                                    </path>
                                </annotationProcessorPaths>
                                <compilerArgs>
                                    <arg>-Amicronaut.processing.group=com.example</arg>
                                    <arg>-Amicronaut.processing.module=demo</arg>
                                </compilerArgs>
                            </configuration>
                        </plugin>
                    </plugins>
                </build>
            </project>
        """;

    @Language("xml")
    private final String pomExpected = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <parent>
                    <groupId>io.micronaut.platform</groupId>
                    <artifactId>micronaut-parent</artifactId>
                    <version>4.0.0-M2</version>
                </parent>
                <dependencies>
                    <dependency>
                        <groupId>io.micronaut</groupId>
                        <artifactId>micronaut-http-client</artifactId>
                        <scope>compile</scope>
                    </dependency>
                    <dependency>
                        <groupId>io.micronaut</groupId>
                        <artifactId>micronaut-http-server-netty</artifactId>
                        <scope>compile</scope>
                    </dependency>
                    <dependency>
                        <groupId>io.micronaut</groupId>
                        <artifactId>micronaut-jackson-databind</artifactId>
                        <scope>compile</scope>
                    </dependency>
                    <dependency>
                        <groupId>io.micronaut.validation</groupId>
                        <artifactId>micronaut-validation</artifactId>
                        <scope>compile</scope>
                    </dependency>
                    <dependency>
                        <groupId>ch.qos.logback</groupId>
                        <artifactId>logback-classic</artifactId>
                        <scope>runtime</scope>
                    </dependency>
                </dependencies>
                <build>
                    <plugins>
                        <plugin>
                            <groupId>io.micronaut.build</groupId>
                            <artifactId>micronaut-maven-plugin</artifactId>
                        </plugin>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                            <configuration>
                                <annotationProcessorPaths combine.children="append">
                                    <path>
                                        <groupId>io.micronaut.validation</groupId>
                                        <artifactId>micronaut-validation-processor</artifactId>
                                        <version>${micronaut.validation.version}</version>
                                        <exclusions>
                                            <exclusion>
                                                <groupId>io.micronaut</groupId>
                                                <artifactId>micronaut-inject</artifactId>
                                            </exclusion>
                                        </exclusions>
                                    </path>
                                </annotationProcessorPaths>
                                <compilerArgs>
                                    <arg>-Amicronaut.processing.group=com.example</arg>
                                    <arg>-Amicronaut.processing.module=demo</arg>
                                </compilerArgs>
                            </configuration>
                        </plugin>
                    </plugins>
                </build>
            </project>
        """;


    @Test
    void updateJavaCodeAndModifyGradleDependencies() {
        rewriteRun(mavenProject("project", srcMainJava(java(annotatedJavaxClass, annotatedJakartaClass)),
                buildGradle(buildGradleInitial, buildGradleExpected)));
    }

    @Test
    void updateJavaCodeAndModifyMavenDependencies() {
        rewriteRun(mavenProject("project", srcMainJava(java(annotatedJavaxClass, annotatedJakartaClass)),
                pomXml(pomInitial, pomExpected)));
    }
}
