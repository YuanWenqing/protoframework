package protobuf4j.orm.dao;

import com.google.protobuf.Descriptors;

/**
 * author: yuanwq
 * date: 2018/7/20
 */
public interface IProtoMessageSqlHandler extends IMessageSqlHandler {
  /**
   * 将{@code fd}字段的值{@code value}转换为对应sql类型的值
   *
   * @see #resolveSqlValueType(Descriptors.FieldDescriptor)
   */
  Object toSqlValue(Descriptors.FieldDescriptor fd, Object value);

  /**
   * 将sql类型的值{@code sqlValue}转换为{@code fd}字段的类型的值
   */
  Object fromSqlValue(Descriptors.FieldDescriptor fd, Object sqlValue);

  /**
   * 寻找{@code fd}的字段值对应的sql类型
   *
   * @see #toSqlValue(Descriptors.FieldDescriptor, Object)
   */
  Class<?> resolveSqlValueType(Descriptors.FieldDescriptor fd);
}
