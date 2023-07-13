package org.openrewrite.java.micronaut;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AddHttpRequestTypeParameter extends Recipe {

    @Override
    public String getDisplayName() {
        return "Add HttpRequest type parameter for implemented interfaces";
    }

    @Override
    public String getDescription() {
        return "This recipe adds an HttpRequest type parameter to a class implements statement for interfaces that have been generically parameterized " +
          "where they previously specified HttpRequest explicitly.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                if (classDecl.getImplements() != null) {
                    List<TypeTree> newInterfaceTypes = classDecl.getImplements().stream()
                            //.map(typeTree -> Optional.ofNullable(typeTree.getType() != null && typeTree.getType() instanceof JavaType.FullyQualified
                            //        ? (JavaType.FullyQualified) typeTree.getType() : null))
                            //.filter(interfaceType -> interfaceType.isPresent()
                            //        && !(interfaceType.get() instanceof JavaType.Parameterized)
                            //        && "io.micronaut.security.authentication.AuthenticationProvider".equals(interfaceType.get().getFullyQualifiedName()))
                            .map(interfaceType -> {
                                JavaType.FullyQualified fqInterfaceType = (JavaType.FullyQualified) interfaceType.getType();
                                JavaType.Class testType = (JavaType.Class) JavaType.buildType(fqInterfaceType.getFullyQualifiedName());
                                testType = testType.withTypeParameters(Collections.singletonList(JavaType.buildType("io.micronaut.http.HttpRequest")));
                                TypeTree testTree = interfaceType.withType(testType);
                                return testTree;
                            }).collect(Collectors.toList());
                    return classDecl.withImplements(newInterfaceTypes);
                }
                return super.visitClassDeclaration(classDecl, executionContext);
            }
        };
    }
}
