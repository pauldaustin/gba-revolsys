package com.revolsys.gis.data.query;

import java.sql.PreparedStatement;

import com.revolsys.gis.model.data.equals.EqualsRegistry;

public class Column extends AbstractCondition {

  private final String name;

  public Column(final String name) {
    this.name = name;
  }

  @Override
  public int appendParameters(final int index, final PreparedStatement statement) {
    return index;
  }

  @Override
  public void appendSql(final StringBuffer buffer) {
    buffer.append(name);
  }

  @Override
  public Column clone() {
    return new Column(name);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof Column) {
      final Column value = (Column)obj;
      return EqualsRegistry.equal(value.getName(), this.getName());
    } else {
      return false;
    }
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }
}
