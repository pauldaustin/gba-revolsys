package com.revolsys.gis.data.model.filter;

import java.util.Arrays;
import java.util.Collection;

import com.revolsys.data.equals.EqualsInstance;
import com.revolsys.data.record.Record;
import com.revolsys.filter.Filter;
import com.revolsys.gis.data.model.DataObjectUtil;

public class AttributesEqualFilter implements Filter<Record> {
  public static boolean accept(final Record object1, final Record object2,
    final boolean nullEqualsEmptyString, final Collection<String> attributeNames) {
    for (final String attributeName : attributeNames) {
      final Object value1 = DataObjectUtil.getAttributeByPath(object1, attributeName);
      final Object value2 = DataObjectUtil.getAttributeByPath(object2, attributeName);
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

  public static boolean accept(final Record object1, final Record object2,
    final boolean nullEqualsEmptyString, final String... attributeNames) {
    return accept(object1, object2, nullEqualsEmptyString, Arrays.asList(attributeNames));
  }

  public static boolean accept(final Record object1, final Record object2,
    final String... attributeNames) {
    return accept(object1, object2, false, Arrays.asList(attributeNames));
  }

  private final Collection<String> attributeNames;

  private final Record object;

  private boolean nullEqualsEmptyString;

  public AttributesEqualFilter(final Record object, final Collection<String> attributeNames) {
    this.attributeNames = attributeNames;
    this.object = object;
  }

  public AttributesEqualFilter(final Record object, final String... attributeNames) {
    this(object, Arrays.asList(attributeNames));
  }

  @Override
  public boolean accept(final Record object) {
    return accept(this.object, object, this.nullEqualsEmptyString, this.attributeNames);
  }

  public boolean isNullEqualsEmptyString() {
    return this.nullEqualsEmptyString;
  }

  public void setNullEqualsEmptyString(final boolean nullEqualsEmptyString) {
    this.nullEqualsEmptyString = nullEqualsEmptyString;
  }

  @Override
  public String toString() {
    return "AttributeEquals" + this.attributeNames;
  }

}
