/*
 * Copyright 2013-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jdbc.repository.query;

import org.springframework.data.relational.repository.query.RelationalParameters;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;

class ExpressionStringQueryWrapper {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private final QueryMethodEvaluationContextProvider evaluationContextProvider;
    private final RelationalParameters parameters;
    private final Object[] values;
    private final EvaluationContext evaluationContext;

    public ExpressionStringQueryWrapper(QueryMethodEvaluationContextProvider evaluationContextProvider,
                                        RelationalParameters parameters,
                                        Object[] values) {

        this.evaluationContextProvider = evaluationContextProvider;
        this.parameters = parameters;
        this.values = values;
        this.evaluationContext = evaluationContextProvider.getEvaluationContext(parameters, values);
    }


    public String renderQueryIfExpressionOrReturnQuery(String query) {
        Assert.notNull(query, "query must not be null");

        Expression expr = PARSER.parseExpression(query, ParserContext.TEMPLATE_EXPRESSION);

        String result = expr.getValue(this.evaluationContext, String.class);

        if (result == null) {
            return query;
        }

        return result;
    }

}
