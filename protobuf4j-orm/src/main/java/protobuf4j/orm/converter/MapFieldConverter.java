package protobuf4j.orm.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.Descriptors;
import com.google.protobuf.MapEntry;
import protobuf4j.core.ProtoMessageHelper;
import protobuf4j.core.ProtobufObjectMapper;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

@SuppressWarnings({"unchecked", "rawtypes"})
public class MapFieldConverter implements IFieldValueConverter {
  private static final ObjectMapper OBJECT_MAPPER = ProtobufObjectMapper.DEFAULT;

  private final ProtoMessageHelper<?> messageHelper;
  private final Descriptors.FieldDescriptor fieldDescriptor;
  private final Descriptors.FieldDescriptor keyFd;
  private final Descriptors.FieldDescriptor valFd;
  private final IFieldResolver basicTypeFieldResolver;

  public MapFieldConverter(ProtoMessageHelper<?> protoMessageHelper,
      Descriptors.FieldDescriptor fieldDescriptor, IFieldResolver basicTypeFieldResolver) {
    this.messageHelper = protoMessageHelper;
    this.fieldDescriptor = fieldDescriptor;
    this.keyFd = fieldDescriptor.getMessageType().findFieldByName("key");
    this.valFd = fieldDescriptor.getMessageType().findFieldByName("value");
    this.basicTypeFieldResolver = basicTypeFieldResolver;
    if (valFd.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
      throw new FieldConversionException(
          "not support map field with message value type, field=" + fieldDescriptor +
              ", javaType=" + fieldDescriptor.getJavaType());
    }
  }

  @Override
  public Class<?> getSqlValueType() {
    return String.class;
  }

  @Override
  public Object toSqlValue(Object fieldValue) {
    Map<Object, Object> map;
    if (fieldValue instanceof Collection) {
      map = collectionToMap((Collection<? extends MapEntry>) fieldValue);
    } else if (fieldValue instanceof Map) {
      map = mapToMap((Map) fieldValue);
    } else {
      throw new FieldConversionException(
          "fail to convert map field, field=" + fieldDescriptor + ", keyType=" +
              keyFd.getJavaType() + ", valueType=" + valFd.getJavaType() + ", fieldValue=" +
              FieldConversionException.toString(fieldValue));
    }
    try {
      return OBJECT_MAPPER.writeValueAsString(map);
    } catch (IOException e) {
      throw new FieldConversionException(
          "fail to convert map field, field=" + fieldDescriptor + ", keyType=" +
              keyFd.getJavaType() + ", valueType=" + valFd.getJavaType() + ", fieldValue=" +
              FieldConversionException.toString(fieldValue), e);
    }
  }

  private Map<Object, Object> mapToMap(Map<?, ?> map) {
    Map<Object, Object> newMap = Maps.newLinkedHashMapWithExpectedSize(map.size());
    for (Map.Entry entry : map.entrySet()) {
      Object key = basicTypeFieldResolver.toSqlValue(keyFd, entry.getKey());
      Object value = basicTypeFieldResolver.toSqlValue(valFd, entry.getValue());
      newMap.put(key, value);
    }
    return newMap;
  }

  private Map<Object, Object> collectionToMap(Collection<? extends MapEntry> valueCollection) {
    Map<Object, Object> map = Maps.newLinkedHashMapWithExpectedSize(valueCollection.size());
    for (MapEntry entry : valueCollection) {
      Object key = basicTypeFieldResolver.toSqlValue(keyFd, entry.getKey());
      Object value = basicTypeFieldResolver.toSqlValue(valFd, entry.getValue());
      map.put(key, value);
    }
    return map;
  }

  @Override
  public Object fromSqlValue(Object sqlValue) {
    if (sqlValue == null) {
      return Collections.emptyList();
    }
    if (sqlValue instanceof String) {
      try {
        LinkedHashMap<String, Object> map =
            OBJECT_MAPPER.readValue((String) sqlValue, LinkedHashMap.class);
        List<MapEntry<?, ?>> mapEntries = Lists.newArrayList();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
          Object k = lookupTransform(keyFd).apply(entry.getKey());
          Object v = lookupTransform(valFd).apply(String.valueOf(entry.getValue()));
          k = basicTypeFieldResolver.fromSqlValue(keyFd, k);
          v = basicTypeFieldResolver.fromSqlValue(valFd, v);
          MapEntry.Builder<?, ?> entryBuilder =
              (MapEntry.Builder<?, ?>) messageHelper.newBuilderForField(fieldDescriptor);
          entryBuilder.setField(keyFd, k).setField(valFd, v);
          mapEntries.add(entryBuilder.build());
        }
        return mapEntries;
      } catch (IOException e) {
        throw new FieldConversionException(
            "fail to parse map field, field=" + fieldDescriptor + ", keyType=" +
                keyFd.getJavaType() + ", valueType=" + valFd.getJavaType() + ", sqlValue=" +
                FieldConversionException.toString(sqlValue), e);
      }
    }
    throw new FieldConversionException(
        "fail to parse map field, field=" + fieldDescriptor + ", keyType=" + keyFd.getJavaType() +
            ", valueType=" + valFd.getJavaType() + ", sqlValue=" +
            FieldConversionException.toString(sqlValue));
  }

  private static Function<String, Object> lookupTransform(Descriptors.FieldDescriptor fd) {
    switch (fd.getJavaType()) {
      case BOOLEAN:
        return text -> (Integer.parseInt(Objects.requireNonNull(text)) != 0);
      case ENUM:
        return text -> fd.getEnumType()
            .findValueByNumber(Integer.parseInt(Objects.requireNonNull(text)));
      case INT:
        return Integer::parseInt;
      case LONG:
        return Long::parseLong;
      case FLOAT:
        return Float::parseFloat;
      case DOUBLE:
        return Double::parseDouble;
      case STRING:
        return text -> text;
      default:
        throw new FieldConversionException(
            "not support java type: " + fd.getJavaType() + ", field=" + fd.getFullName());
    }
  }

}
