/*
 * Copyright 2017-2023 the original author or authors.
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
package org.springframework.data.relational.core.mapping;

import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Meta data a repository might need for implementing persistence operations for instances of type {@code T}
 *
 * @author Jens Schauder
 * @author Greg Turnquist
 * @author Bastian Wilhelm
 * @author Mikhail Polivakha
 */
class RelationalPersistentEntityImpl<T> extends BasicPersistentEntity<T, RelationalPersistentProperty>
        implements RelationalPersistentEntity<T> {

    // 增加spel的支持
    private final SpelExpressionParser parser = new SpelExpressionParser();

    private final NamingStrategy namingStrategy;
    private final Supplier<Optional<SqlIdentifier>> tableName;
    private final Lazy<Optional<SqlIdentifier>> schemaName;
    private boolean forceQuote = true;

    /**
     * Creates a new {@link RelationalPersistentEntityImpl} for the given {@link TypeInformation}.
     *
     * @param information must not be {@literal null}.
     */
    RelationalPersistentEntityImpl(TypeInformation<T> information, NamingStrategy namingStrategy) {

        super(information);

        this.namingStrategy = namingStrategy;

        this.tableName = () -> Optional.ofNullable(findAnnotation(Table.class)) //
                .map(Table::value) //
                .filter(StringUtils::hasText) //
                .map(value -> { // 增加spel的支持
                    if (value.contains(ParserContext.TEMPLATE_EXPRESSION.getExpressionPrefix())) {
                        Expression expression = parser.parseExpression(value, ParserContext.TEMPLATE_EXPRESSION);
                        return expression.getValue(this.getEvaluationContext(null), String.class);
                    } else {
                        return value;
                    }
                })
                .map(this::createSqlIdentifier);

        this.schemaName = Lazy.of(() -> Optional.ofNullable(findAnnotation(Table.class)) //
                .map(Table::schema) //
                .filter(StringUtils::hasText) //
                .map(s -> { // 增加spel的支持
                    Expression expression = parser.parseExpression(s, ParserContext.TEMPLATE_EXPRESSION);
                    return expression.getValue(this.getEvaluationContext(null), String.class);
                })
                .map(this::createSqlIdentifier));
    }

    private SqlIdentifier createSqlIdentifier(String name) {
        return isForceQuote() ? SqlIdentifier.quoted(name) : SqlIdentifier.unquoted(name);
    }

    private SqlIdentifier createDerivedSqlIdentifier(String name) {
        return new DerivedSqlIdentifier(name, isForceQuote());
    }

    public boolean isForceQuote() {
        return forceQuote;
    }

    public void setForceQuote(boolean forceQuote) {
        this.forceQuote = forceQuote;
    }

    @Override
    public SqlIdentifier getTableName() {

        Optional<SqlIdentifier> explicitlySpecifiedTableName = tableName.get();
        SqlIdentifier schemalessTableIdentifier = createDerivedSqlIdentifier(namingStrategy.getTableName(getType()));

        return explicitlySpecifiedTableName.orElse(schemalessTableIdentifier);
    }

    @Override
    public SqlIdentifier getQualifiedTableName() {

        SqlIdentifier schema = determineCurrentEntitySchema();
        Optional<SqlIdentifier> explicitlySpecifiedTableName = tableName.get();

        SqlIdentifier schemalessTableIdentifier = createDerivedSqlIdentifier(namingStrategy.getTableName(getType()));

        if (schema == null) {
            return explicitlySpecifiedTableName.orElse(schemalessTableIdentifier);
        }

        return explicitlySpecifiedTableName.map(sqlIdentifier -> SqlIdentifier.from(schema, sqlIdentifier))
                .orElse(SqlIdentifier.from(schema, schemalessTableIdentifier));
    }

    /**
     * @return {@link SqlIdentifier} representing the current entity schema. If the schema is not specified, neither
     * explicitly, nor via {@link NamingStrategy}, then return {@link null}
     */
    @Nullable
    private SqlIdentifier determineCurrentEntitySchema() {

        Optional<SqlIdentifier> explicitlySpecifiedSchema = schemaName.get();
        return explicitlySpecifiedSchema.orElseGet(
                () -> StringUtils.hasText(namingStrategy.getSchema()) ? createDerivedSqlIdentifier(namingStrategy.getSchema())
                        : null);
    }

    @Override
    public SqlIdentifier getIdColumn() {
        return getRequiredIdProperty().getColumnName();
    }

    @Override
    public String toString() {
        return String.format("RelationalPersistentEntityImpl<%s>", getType());
    }
}
