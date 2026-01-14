/*
 * Copyright 2021 the original author or authors.
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

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeLiteral;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

public class BeanPropertyCapitalizationStrategy extends Recipe {

    @Getter
    final String displayName = "De-capitalize `BeanIntrospection` `getProperty(..)` and `getRequiredProperty(..)` name arguments";

    @Getter
    final String description = "As of Micronaut 3.x property names for getters like `getXForwarded()` are de-capitalized from `XForwarded` to `xForwarded`.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("io.micronaut.core.beans.BeanIntrospection", false), new BeanPropertyCapitalizationStrategyVisitor());
    }

    private static class BeanPropertyCapitalizationStrategyVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final MethodMatcher BEAN_PROPERTY_METHOD = new MethodMatcher("io.micronaut.core.beans.BeanIntrospection getProperty(..)");
        private static final MethodMatcher REQUIRED_BEAN_PROPERTY_METHOD = new MethodMatcher("io.micronaut.core.beans.BeanIntrospection getRequiredProperty(..)");

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
            if ((BEAN_PROPERTY_METHOD.matches(mi) || REQUIRED_BEAN_PROPERTY_METHOD.matches(mi)) && mi.getArguments().get(0) instanceof J.Literal) {
                J.Literal propertyNameArg = (J.Literal) mi.getArguments().get(0);
                String sVal = String.valueOf(propertyNameArg.getValue());
                final String newValue = sVal.substring(0, 1).toLowerCase() + sVal.substring(1);
                if (!sVal.equals(newValue)) {
                    doAfterVisit(new ChangeLiteral<>(propertyNameArg, p -> newValue));
                }
            }
            return mi;
        }
    }
}
