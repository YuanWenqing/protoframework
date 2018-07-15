package org.protoframework.dao;

/**
 * Created by tuqc on 15-3-17.
 */

import com.google.common.collect.Sets;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import org.apache.commons.lang3.StringUtils;
import org.protoframework.core.ProtoMessageHelper;
import org.protoframework.util.ThreadLocalTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * We make a convention that methods to search multiple beans in the inherited DAO Classes should be
 * named starting with 'list', like listXXXsByYYY(..), instead of 'find' naming in this abstract DAO
 * Class. We reserve the word 'find' for naming more general methods.<br>
 * 所有子类必须有一个public static final String TABLE_NAME = "...";
 *
 * @param <T> 访问的数据表的数据元素类型
 * @author yuanwq
 */
public class ProtoMessageDao<T extends Message> implements IMessageDao<T> {
  protected static final ThreadLocalTimer timer = new ThreadLocalTimer();

  /**
   * 访问的数据表的数据元素类型
   */
  protected final Class<T> messageType;
  /**
   * 访问的数据表名
   */
  protected final String tableName;
  protected final ProtoMessageHelper<T> messageHelper;
  protected final RowMapper<T> messageMapper;

  /**
   * 记录dao日志的logger
   */
  protected final Logger daoLogger;
  /**
   * 记录执行的sql的logger
   */
  protected final DaoSqlLogger sqlLogger;

  protected JdbcTemplate jdbcTemplate;

  protected ISqlConvention sqlConvention;

  protected static final String SQL_SELECT_TEMPLATE = "SELECT %s FROM %s %s";
  protected static final String SQL_INSERT_TEMPLATE = "INSERT INTO %s (%s) VALUES (%s);";
  protected static final String SQL_INSERT_IGNORE_TEMPLATE =
      "INSERT IGNORE INTO %s (%s) VALUES (%s);";
  protected static final String SQL_UPDATE_TEMPLATE = "UPDATE %s %s %s;";
  protected static final String SQL_DELETE_TEMPLATE = "DELETE FROM %s %s";

  public ProtoMessageDao(Class<T> messageType) {
    this(messageType, ProtoSqls.tableName(messageType));
  }

  private ProtoMessageDao(Class<T> messageType, String tableName) {
    this.messageType = messageType;
    this.tableName = tableName;
    this.messageHelper = ProtoMessageHelper.getHelper(messageType);
    this.messageMapper = new ProtoMessageRowMapper<>(messageType);

    this.daoLogger = LoggerFactory
        .getLogger(getClass().getName() + "#" + messageHelper.getDescriptor().getFullName());
    this.sqlLogger = new DaoSqlLogger(messageHelper.getDescriptor().getFullName());
  }

  public Class<T> getMessageType() {
    return messageType;
  }

  public String getTableName() {
    return tableName;
  }

  public ProtoMessageHelper<T> getMessageHelper() {
    return messageHelper;
  }

  public RowMapper<T> getMessageMapper() {
    return messageMapper;
  }

  public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public JdbcTemplate getJdbcTemplate() {
    return jdbcTemplate;
  }

  ////////////////////////////// insert //////////////////////////////

  /**
   * 插入记录
   */
  @Override
  public boolean insert(final T message) {
    int rows = doInsert(SQL_INSERT_TEMPLATE, message, null);
    return rows > 0;
  }

  @Override
  public Number insertReturnKey(final T message) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    int rows = doInsert(SQL_INSERT_TEMPLATE, message, keyHolder);
    if (rows == 0) {
      throw new RuntimeException(
          "fail to insert into " + tableName + ": " + messageHelper.toString(message));
    }
    return keyHolder.getKey();
  }

  /**
   * 插入记录。如果记录不存在，插入；如果记录已经存在，什么也不做，直接返回。
   */
  @Override
  public boolean insertIgnore(final T message) {
    int rows = doInsert(SQL_INSERT_IGNORE_TEMPLATE, message, null);
    return rows > 0;
  }

  /**
   * Warn: 实例化的dao中的方法不建议直接使用该方法
   */
  protected int doInsert(String sqlTemplate, T message, KeyHolder keyHolder) {
    Set<String> fields = getInsertFields(message);
    if (fields.isEmpty()) {
      throw new RuntimeException("empty message to insert into " + tableName);
    }
    final String sql = String.format(sqlTemplate, this.tableName, StringUtils.join(fields, ","),
        StringUtils.repeat("?", ",", fields.size()));
    PreparedStatementCreator creator = makeInsertCreator(sql, fields, message);
    timer.restart();
    try {
      if (keyHolder == null) {
        return getJdbcTemplate().update(creator);
      } else {
        return getJdbcTemplate().update(creator, keyHolder);
      }
    } finally {
      sqlLogger.insert()
          .info("Insert SQL(cost={}): {}, values: {}", timer.stop(TimeUnit.MILLISECONDS), sql,
              messageHelper.toString(message));
    }
  }

  private PreparedStatementCreator makeInsertCreator(String sql, Collection<String> fields,
      T message) {
    return new PreparedStatementCreator() {
      @Override
      public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        setValuesInner(ps, message, fields);
        return ps;
      }
    };
  }

  @Override
  public int[] insertMulti(List<T> messages) {
    return doInsertMulti(SQL_INSERT_TEMPLATE, messages);
  }

  @Override
  public int[] insertIgnoreMulti(List<T> messages) {
    return doInsertMulti(SQL_INSERT_IGNORE_TEMPLATE, messages);
  }

  /**
   * Warn: 实例化的dao中的方法不建议直接使用该方法
   */
  protected int[] doInsertMulti(String sqlTemplate, List<T> messages) {
    if (messages.isEmpty()) return new int[0];
    Set<String> used = getInsertFields(messages);
    final String sql = String.format(sqlTemplate, this.tableName, StringUtils.join(used, ","),
        StringUtils.repeat("?", ",", used.size()));
    int[] rows = this.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {
      @Override
      public void setValues(PreparedStatement ps, int i) throws SQLException {
        T message = messages.get(i);
        setValuesInner(ps, message, used);
      }

      @Override
      public int getBatchSize() {
        return messages.size();
      }
    });
    return rows;
  }

  private LinkedHashSet<String> getInsertFields(T message) {
    return getInsertFields(Collections.singletonList(message));
  }

  private LinkedHashSet<String> getInsertFields(Collection<T> messages) {
    LinkedHashSet<String> fields = Sets.newLinkedHashSet();
    for (T message : messages) {
      for (FieldDescriptor fd : messageHelper.getFieldDescriptorList()) {
        if (messageHelper.isFieldSet(message, fd.getName())) {
          fields.add(fd.getName());
        }
      }
    }
    return fields;
  }

  private void setValuesInner(PreparedStatement ps, T message, Collection<String> fields)
      throws SQLException {
    int j = 1;
    for (String name : fields) {
      FieldDescriptor fd = messageHelper.getFieldDescriptor(name);
      Object value = messageHelper.getFieldValue(message, name);
      // TODO: 转换value
      value = ProtoSqls.sqlValue(fd, value);
      ps.setObject(j++, value);
    }
  }

}