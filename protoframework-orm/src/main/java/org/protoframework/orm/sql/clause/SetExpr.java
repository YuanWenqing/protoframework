package org.protoframework.orm.sql.clause;

import org.protoframework.orm.sql.AbstractSqlObject;
import org.protoframework.orm.sql.IExpression;
import org.protoframework.orm.sql.ISqlValue;
import org.protoframework.orm.sql.expr.Column;

import javax.annotation.Nonnull;
import java.util.List;

import static com.google.common.base.Preconditions.*;

/**
 * @author: yuanwq
 * @date: 2018/7/14
 */
public class SetExpr extends AbstractSqlObject {
  private final Column column;
  private final IExpression valueExpr;

  public SetExpr(@Nonnull Column column, @Nonnull IExpression valueExpr) {
    checkNotNull(column);
    checkNotNull(valueExpr);
    this.column = column;
    this.valueExpr = valueExpr;
  }

  public Column getColumn() {
    return column;
  }

  public IExpression getValueExpr() {
    return valueExpr;
  }

  @Override
  public StringBuilder toSqlTemplate(@Nonnull StringBuilder sb) {
    column.toSqlTemplate(sb);
    sb.append("=");
    valueExpr.toSqlTemplate(sb);
    return sb;
  }

  @Override
  public StringBuilder toSolidSql(@Nonnull StringBuilder sb) {
    column.toSolidSql(sb);
    sb.append("=");
    valueExpr.toSolidSql(sb);
    return sb;
  }

  @Override
  public List<ISqlValue> collectSqlValue(@Nonnull List<ISqlValue> sqlValues) {
    column.collectSqlValue(sqlValues);
    valueExpr.collectSqlValue(sqlValues);
    return sqlValues;
  }
}