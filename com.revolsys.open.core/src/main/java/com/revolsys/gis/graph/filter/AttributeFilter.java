package com.revolsys.gis.graph.filter;

import com.revolsys.data.equals.EqualsInstance;
import com.revolsys.gis.graph.AttributedObject;
import java.util.function.Predicate;

public class AttributeFilter<T extends AttributedObject> implements Predicate<T> {
  private final String attributeName;

  private final boolean inverse;

  private final Object value;

  public AttributeFilter(final String attributeName, final Object value) {
    this.attributeName = attributeName;
    this.value = value;
    this.inverse = false;
  }

  public AttributeFilter(final String attributeName, final Object value, final boolean inverse) {
    this.attributeName = attributeName;
    this.value = value;
    this.inverse = inverse;
  }

  @Override
  public boolean test(final T object) {
    final Object value = object.getAttribute(this.attributeName);
    final boolean equal = EqualsInstance.INSTANCE.equals(this.value, value);
    if (this.inverse) {
      return !equal;
    } else {
      return equal;
    }
  }
}
