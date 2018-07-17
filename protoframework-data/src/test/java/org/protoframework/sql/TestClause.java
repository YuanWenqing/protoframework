package org.protoframework.sql;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.protoframework.sql.clause.*;
import org.protoframework.sql.expr.TableColumn;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author: yuanwq
 * @date: 2018/7/17
 */
public class TestClause {
  @Test
  public void testSelect() {
    SelectClause clause;

    clause = QueryCreator.select();
    System.out.println(clause);
    assertTrue(clause.isEmpty());
    assertEquals(0, clause.getSelectExprs().size());
    try {
      clause.toSqlTemplate(new StringBuilder());
      fail();
    } catch (IllegalArgumentException e) {
      System.out.println(e.getMessage());
    }
    assertEquals("SELECT ", clause.toSolidSql(new StringBuilder()).toString());

    clause.select(TableColumn.of("a"));
    System.out.println(clause);
    assertEquals("SELECT a", clause.toSqlTemplate(new StringBuilder()).toString());
    assertEquals("SELECT a", clause.toSolidSql(new StringBuilder()).toString());
    assertFalse(clause.isEmpty());

    clause.select("b");
    System.out.println(clause);
    assertTrue(clause.getSelectExprs().get(1).getExpression() instanceof TableColumn);
    assertNull(clause.getSelectExprs().get(1).getAlias());
    assertEquals("SELECT a,b", clause.toSqlTemplate(new StringBuilder()).toString());
    assertEquals("SELECT a,b", clause.toSolidSql(new StringBuilder()).toString());
    assertFalse(clause.isEmpty());

    clause.select(new SelectExpr(FieldValues.add("a", 1), "b"));
    System.out.println(clause);
    assertEquals("b", clause.getSelectExprs().get(2).getAlias());
    assertEquals("SELECT a,b,a+? AS b", clause.toSqlTemplate(new StringBuilder()).toString());
    assertEquals("SELECT a,b,a+1 AS b", clause.toSolidSql(new StringBuilder()).toString());
    List<ISqlValue> sqlValues = clause.collectSqlValue(Lists.newArrayList());
    assertEquals(1, sqlValues.size());
    assertEquals(1, sqlValues.get(0).getValue());
  }

  @Test
  public void testFrom() {
    FromClause clause = QueryCreator.from("t");

    assertEquals("t", clause.getTableRef().getTableName());
    assertEquals("FROM t", clause.toSqlTemplate(new StringBuilder()).toString());
    assertEquals("FROM t", clause.toSolidSql(new StringBuilder()).toString());
    assertTrue(clause.collectSqlValue(Lists.newArrayList()).isEmpty());
  }

  @Test
  public void testWhere() {
    WhereClause clause;

    clause = QueryCreator.where();
    System.out.println(clause);
    assertNull(clause.getCond());
    assertNull(clause.getOrderBy());
    assertNull(clause.getGroupBy());
    assertNull(clause.getPagination());
    assertEquals("", clause.toSqlTemplate(new StringBuilder()).toString());
    assertEquals("", clause.toSolidSql(new StringBuilder()).toString());
    assertTrue(clause.collectSqlValue(Lists.newArrayList()).isEmpty());

    clause.setCond(FieldValues.eq("a", 1));
    System.out.println(clause);
    assertNotNull(clause.getCond());
    assertEquals("WHERE a=?", clause.toSqlTemplate(new StringBuilder()).toString());
    assertEquals("WHERE a=1", clause.toSolidSql(new StringBuilder()).toString());

    clause.orderBy();
    System.out.println(clause);
    assertNotNull(clause.getOrderBy());
    assertEquals("WHERE a=?", clause.toSqlTemplate(new StringBuilder()).toString());
    assertEquals("WHERE a=1", clause.toSolidSql(new StringBuilder()).toString());

    clause.orderBy().asc("b");
    System.out.println(clause);
    assertEquals("WHERE a=? ORDER BY b ASC", clause.toSqlTemplate(new StringBuilder()).toString());
    assertEquals("WHERE a=1 ORDER BY b ASC", clause.toSolidSql(new StringBuilder()).toString());

    clause.groupBy();
    System.out.println(clause);
    assertNotNull(clause.getGroupBy());
    assertEquals("WHERE a=? ORDER BY b ASC", clause.toSqlTemplate(new StringBuilder()).toString());
    assertEquals("WHERE a=1 ORDER BY b ASC", clause.toSolidSql(new StringBuilder()).toString());

    clause.groupBy().by("c");
    System.out.println(clause);
    assertEquals("WHERE a=? ORDER BY b ASC GROUP BY c",
        clause.toSqlTemplate(new StringBuilder()).toString());
    assertEquals("WHERE a=1 ORDER BY b ASC GROUP BY c",
        clause.toSolidSql(new StringBuilder()).toString());

    clause.limit(10);
    System.out.println(clause);
    assertEquals("WHERE a=? ORDER BY b ASC GROUP BY c LIMIT 10 OFFSET 0",
        clause.toSqlTemplate(new StringBuilder()).toString());
    assertEquals("WHERE a=1 ORDER BY b ASC GROUP BY c LIMIT 10 OFFSET 0",
        clause.toSolidSql(new StringBuilder()).toString());

    clause.setCond(null);
    System.out.println(clause);
    assertEquals("ORDER BY b ASC GROUP BY c LIMIT 10 OFFSET 0",
        clause.toSqlTemplate(new StringBuilder()).toString());
    assertEquals("ORDER BY b ASC GROUP BY c LIMIT 10 OFFSET 0",
        clause.toSolidSql(new StringBuilder()).toString());

    clause.setGroupBy(null);
    System.out.println(clause);
    assertEquals("ORDER BY b ASC LIMIT 10 OFFSET 0",
        clause.toSqlTemplate(new StringBuilder()).toString());
    assertEquals("ORDER BY b ASC LIMIT 10 OFFSET 0",
        clause.toSolidSql(new StringBuilder()).toString());

    clause.setOrderBy(null);
    System.out.println(clause);
    assertEquals("LIMIT 10 OFFSET 0", clause.toSqlTemplate(new StringBuilder()).toString());
    assertEquals("LIMIT 10 OFFSET 0", clause.toSolidSql(new StringBuilder()).toString());

    clause.setPagination(null);
    clause.groupBy().by("c");
    System.out.println(clause);
    assertEquals("GROUP BY c", clause.toSqlTemplate(new StringBuilder()).toString());
    assertEquals("GROUP BY c", clause.toSolidSql(new StringBuilder()).toString());
  }

  @Test
  public void testGroupBy() {
    GroupByClause clause;

    clause = QueryCreator.groupBy();
    System.out.println(clause);
    assertTrue(clause.isEmpty());
    assertEquals(0, clause.getGroupByExprs().size());
    assertEquals("", clause.toSqlTemplate(new StringBuilder()).toString());
    assertEquals("", clause.toSolidSql(new StringBuilder()).toString());

    clause.asc("a");
    System.out.println(clause);
    assertEquals("GROUP BY a ASC", clause.toSqlTemplate(new StringBuilder()).toString());
    assertEquals("GROUP BY a ASC", clause.toSolidSql(new StringBuilder()).toString());
    assertFalse(clause.isEmpty());

    clause.desc("b");
    System.out.println(clause);
    assertTrue(clause.getGroupByExprs().get(1).getExpression() instanceof TableColumn);
    assertEquals(Direction.DESC, clause.getGroupByExprs().get(1).getDirection());
    assertEquals("GROUP BY a ASC,b DESC", clause.toSqlTemplate(new StringBuilder()).toString());
    assertEquals("GROUP BY a ASC,b DESC", clause.toSolidSql(new StringBuilder()).toString());
    assertFalse(clause.isEmpty());

    clause.by("c");
    System.out.println(clause);
    assertEquals("GROUP BY a ASC,b DESC,c", clause.toSqlTemplate(new StringBuilder()).toString());
    assertEquals("GROUP BY a ASC,b DESC,c", clause.toSolidSql(new StringBuilder()).toString());
  }

  @Test
  public void testOrderBy() {
    OrderByClause clause;

    clause = QueryCreator.orderBy();
    System.out.println(clause);
    assertTrue(clause.isEmpty());
    assertEquals(0, clause.getOrderByExprs().size());
    assertEquals("", clause.toSqlTemplate(new StringBuilder()).toString());
    assertEquals("", clause.toSolidSql(new StringBuilder()).toString());

    clause.asc("a");
    System.out.println(clause);
    assertEquals("ORDER BY a ASC", clause.toSqlTemplate(new StringBuilder()).toString());
    assertEquals("ORDER BY a ASC", clause.toSolidSql(new StringBuilder()).toString());
    assertFalse(clause.isEmpty());

    clause.desc("b");
    System.out.println(clause);
    assertTrue(clause.getOrderByExprs().get(1).getExpression() instanceof TableColumn);
    assertEquals(Direction.DESC, clause.getOrderByExprs().get(1).getDirection());
    assertEquals("ORDER BY a ASC,b DESC", clause.toSqlTemplate(new StringBuilder()).toString());
    assertEquals("ORDER BY a ASC,b DESC", clause.toSolidSql(new StringBuilder()).toString());
    assertFalse(clause.isEmpty());

    clause.by(new OrderByExpr(FieldValues.add("a", 1), Direction.DESC));
    System.out.println(clause);
    assertEquals("ORDER BY a ASC,b DESC,a+? DESC",
        clause.toSqlTemplate(new StringBuilder()).toString());
    assertEquals("ORDER BY a ASC,b DESC,a+1 DESC",
        clause.toSolidSql(new StringBuilder()).toString());
    List<ISqlValue> sqlValues = clause.collectSqlValue(Lists.newArrayList());
    assertEquals(1, sqlValues.size());
    assertEquals(1, sqlValues.get(0).getValue());
  }

  @Test
  public void testPagination() {
    try {
      QueryCreator.pagination(-1).buildByOffset(10);
      fail();
    } catch (IllegalArgumentException e) {
      System.out.println(e.getMessage());
    }
    try {
      QueryCreator.pagination(-1).buildByPageNo(10);
      fail();
    } catch (IllegalArgumentException e) {
      System.out.println(e.getMessage());
    }
    try {
      QueryCreator.pagination(-1).build();
      fail();
    } catch (IllegalArgumentException e) {
      System.out.println(e.getMessage());
    }
    try {
      QueryCreator.pagination(10).buildByOffset(-1);
      fail();
    } catch (IllegalArgumentException e) {
      System.out.println(e.getMessage());
    }
    try {
      QueryCreator.pagination(10).buildByPageNo(0);
      fail();
    } catch (IllegalArgumentException e) {
      System.out.println(e.getMessage());
    }

    PaginationClause clause;

    clause = QueryCreator.pagination(-1).
        setDefaultLimit(10).build();
    assertEquals(10, clause.getLimit());
    assertEquals(0, clause.getOffset());

    clause = QueryCreator.pagination(20).
        setDefaultLimit(12).
        setDefaultPageNo(10).buildByPageNo(0);
    assertEquals(20, clause.getLimit());
    assertEquals(180, clause.getOffset());

    clause = QueryCreator.pagination(10).
        setDefaultOffset(10).buildByOffset(-1);
    assertEquals(10, clause.getLimit());
    assertEquals(10, clause.getOffset());
  }
}