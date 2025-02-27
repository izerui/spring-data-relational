package org.springframework.data.relational.core.query;

import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.Nullable;

import java.util.List;

public class JustCriteria implements CriteriaDefinition {

    private final @Nullable String sql;

    JustCriteria(@Nullable String sql) {
        this.sql = sql;
    }

    public static JustCriteria just(String sql) {
        return new JustCriteria(sql);
    }


    @Override
    public boolean isGroup() {
        return false;
    }

    @Override
    public List<CriteriaDefinition> getGroup() {
        return null;
    }

    @Override
    public SqlIdentifier getColumn() {
        return SqlIdentifier.EMPTY;
    }

    @Override
    public Comparator getComparator() {
        return Comparator.JUST;
    }

    @Override
    public Object getValue() {
        return sql;
    }

    @Override
    public boolean isIgnoreCase() {
        return false;
    }

    @Override
    public CriteriaDefinition getPrevious() {
        return null;
    }

    @Override
    public boolean hasPrevious() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Combinator getCombinator() {
        return Combinator.INITIAL;
    }
}
