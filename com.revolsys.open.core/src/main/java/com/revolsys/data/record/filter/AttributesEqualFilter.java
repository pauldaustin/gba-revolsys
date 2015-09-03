package com.revolsys.data.record.filter;

import java.util.Arrays;
import java.util.Collection;

import com.revolsys.data.equals.EqualsInstance;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.Records;
import java.util.function.Predicate;

public class AttributesEqualFilter implements Predicate<Record> {
  public static boolean test(final Record object1, final Record object2,
    final boolean nullEqualsEmptyString, final Collection<String> attributeNames) {
    for (final String attributeName : attributeNames) {
      final Object value1 = Records.getFieldByPath(object1, attributeName);
      final Object value2 = Records.getFieldByPath(object2, attributeName);
      if (nullEqualsEmptyString) {
        if (value1 == null) {
          if (value2 != null && !"".equals(value2)) {
            return false;
          }
        } else if (value2 == null) {
          if (value1 != null && !"".equals(value1)) {
            return false;
          }
        } else if (!EqualsInstance.INSTANCE.equals(value1, value2)) {
          return false;
        }
      } else {
        if (!EqualsInstance.INSTANCE.equals(value1, value2)) {
          return false;
        }
      }
    }
    return true;
  }

  public static boolean test(final Record object1, final Record object2,
    final boolean nullEqualsEmptyString, final String... attributeNames) {
    return test(object1, object2, nullEqualsEmptyString, Arrays.asList(attributeNames));
  }

  public static boolean test(final Record object1, final Record object2,
    final String... attributeNames) {
    return test(object1, object2, false, Arrays.asList(attributeNames));
  }

  private final Collection<String> attributeNames;

  private boolean nullEqualsEmptyString;

  private final Record object;

  public AttributesEqualFilter(final Record object, final Collection<String> attributeNames) {
    this.attributeNames = attributeNames;
    this.object = object;
  }

  public AttributesEqualFilter(final Record object, final String... attributeNames) {
    this(object, Arrays.asList(attributeNames));
  }

  public boolean isNullEqualsEmptyString() {
    return this.nullEqualsEmptyString;
  }

  public void setNullEqualsEmptyString(final boolean nullEqualsEmptyString) {
    this.nullEqualsEmptyString = nullEqualsEmptyString;
  }

  @Override
  public boolean test(final Record object) {
    return test(this.object, object, this.nullEqualsEmptyString, this.attributeNames);
  }

  @Override
  public String toString() {
    return "AttributeEquals" + this.attributeNames;
  }

}
