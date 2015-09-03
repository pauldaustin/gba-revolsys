package com.revolsys.data.record.filter;

import java.util.Arrays;
import java.util.Collection;

import com.revolsys.data.equals.EqualsInstance;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.Records;
import java.util.function.Predicate;

public class AttributesEqualOrSourceNullFilter implements Predicate<Record> {
  public static boolean test(final Record object1, final Record object2,
    final Collection<String> attributeNames) {
    for (final String attributeName : attributeNames) {
      final Object value1 = Records.getFieldByPath(object1, attributeName);
      final Object value2 = Records.getFieldByPath(object2, attributeName);

      if (value1 != null && !EqualsInstance.INSTANCE.equals(value1, value2)) {
        return false;
      }
    }
    return true;
  }

  public static boolean test(final Record object1, final Record object2,
    final String... attributeNames) {
    return test(object1, object2, Arrays.asList(attributeNames));
  }

  private final Collection<String> attributeNames;

  private final Record object;

  public AttributesEqualOrSourceNullFilter(final Record object,
    final Collection<String> attributeNames) {
    this.attributeNames = attributeNames;
    this.object = object;
  }

  public AttributesEqualOrSourceNullFilter(final Record object, final String... attributeNames) {
    this(object, Arrays.asList(attributeNames));
  }

  @Override
  public boolean test(final Record object) {
    return test(this.object, object, this.attributeNames);
  }

  @Override
  public String toString() {
    return "AttributeEquals" + this.attributeNames;
  }

}
