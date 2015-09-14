package com.revolsys.gis.graph.filter;

import java.util.function.Predicate;

import com.revolsys.equals.EqualsInstance;
import com.revolsys.gis.graph.AttributedObject;

public class AttributeFilter<T extends AttributedObject> implements Predicate<T> {
  private final String fieldName;

  private final boolean inverse;

  private final Object value;

  public AttributeFilter(final String fieldName, final Object value) {
    this.fieldName = fieldName;
    this.value = value;
    this.inverse = false;
  }

  public AttributeFilter(final String fieldName, final Object value, final boolean inverse) {
    this.fieldName = fieldName;
    this.value = value;
    this.inverse = inverse;
  }

  @Override
  public boolean test(final T object) {
    final Object value = object.getAttribute(this.fieldName);
    final boolean equal = EqualsInstance.INSTANCE.equals(this.value, value);
    if (this.inverse) {
      return !equal;
    } else {
      return equal;
    }
  }
}
