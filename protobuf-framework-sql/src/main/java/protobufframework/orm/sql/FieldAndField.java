package protobufframework.orm.sql;

import protobufframework.orm.sql.expr.ArithmeticExpr;
import protobufframework.orm.sql.expr.Column;
import protobufframework.orm.sql.expr.LogicalExpr;
import protobufframework.orm.sql.expr.RelationalExpr;

/**
 * a util to build relational expression for two specified fields
 *
 * author: yuanwq
 * date: 2018/7/16
 */
public interface FieldAndField {

  static RelationalExpr eq(String left, String right) {
    return RelationalExpr.eq(Column.of(left), Column.of(right));
  }

  static RelationalExpr ne(String left, String right) {
    return RelationalExpr.ne(Column.of(left), Column.of(right));
  }

  static RelationalExpr lt(String left, String right) {
    return RelationalExpr.lt(Column.of(left), Column.of(right));
  }

  static RelationalExpr lte(String left, String right) {
    return RelationalExpr.lte(Column.of(left), Column.of(right));
  }

  static RelationalExpr gt(String left, String right) {
    return RelationalExpr.gt(Column.of(left), Column.of(right));
  }

  static RelationalExpr gte(String left, String right) {
    return RelationalExpr.gte(Column.of(left), Column.of(right));
  }

  static ArithmeticExpr add(String left, String right) {
    return ArithmeticExpr.add(Column.of(left), Column.of(right));
  }

  static ArithmeticExpr subtract(String left, String right) {
    return ArithmeticExpr.subtract(Column.of(left), Column.of(right));
  }

  static ArithmeticExpr multiply(String left, String right) {
    return ArithmeticExpr.multiply(Column.of(left), Column.of(right));
  }

  static ArithmeticExpr divide(String left, String right) {
    return ArithmeticExpr.divide(Column.of(left), Column.of(right));
  }

  static ArithmeticExpr divRound(String left, String right) {
    return ArithmeticExpr.divRound(Column.of(left), Column.of(right));
  }

  static ArithmeticExpr mod(String left, String right) {
    return ArithmeticExpr.mod(Column.of(left), Column.of(right));
  }

  static LogicalExpr and(String left, String right) {
    return LogicalExpr.and(Column.of(left), Column.of(right));
  }

  static LogicalExpr or(String left, String right) {
    return LogicalExpr.or(Column.of(left), Column.of(right));
  }

  static LogicalExpr xor(String left, String right) {
    return LogicalExpr.xor(Column.of(left), Column.of(right));
  }

  static LogicalExpr not(String field) {
    return LogicalExpr.not(Column.of(field));
  }
}
