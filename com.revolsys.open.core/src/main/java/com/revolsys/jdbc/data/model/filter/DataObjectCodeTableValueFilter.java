package com.revolsys.jdbc.data.model.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.revolsys.data.codes.CodeTable;
import com.revolsys.data.identifier.SingleIdentifier;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.filter.Filter;

/**
 * Filter DataObjects by the value of the attributeName.
 *
 * @author Paul Austin
 */
public class DataObjectCodeTableValueFilter implements Filter<Record> {
  /** The attributeName name, or path to match. */
  private String attributeName;

  private String name;

  /** The value to match. */
  private final List<Object> values = new ArrayList<Object>();

  public DataObjectCodeTableValueFilter() {
  }

  public DataObjectCodeTableValueFilter(final String attributeName,
    final List<Object> values) {
    this.attributeName = attributeName;
    this.values.addAll(values);
  }

  public DataObjectCodeTableValueFilter(final String attributeName,
    final Object... values) {
    this(attributeName, Arrays.asList(values));
  }

  /**
   * Match the attributeName on the data object with the required value.
   *
   * @param object The object.
   * @return True if the object matched the filter, false otherwise.
   */
  @Override
  public boolean accept(final Record object) {
    final Object propertyValue = object.getValue(this.attributeName);
    if (this.values.contains(propertyValue)) {
      return true;
    } else {
      final RecordDefinition metaData = object.getMetaData();
      final CodeTable codeTable = metaData.getCodeTableByColumn(this.attributeName);
      if (codeTable != null) {
        final Object codeValue = codeTable.getValue(SingleIdentifier.create(
          propertyValue));
        if (this.values.contains(codeValue)) {
          this.values.add(propertyValue);
          return true;
        } else {
          return false;
        }
      } else {
        return false;
      }

    }
  }

  /**
   * Get the attributeName name, or path to match.
   *
   * @return The attributeName name, or path to match.
   */
  public String getAttributeName() {
    return this.attributeName;
  }

  /**
   * @return the values
   */
  public List<Object> getValues() {
    return this.values;
  }

  public void setAttributeName(final String attributeName) {
    this.attributeName = attributeName;
  }

  /**
   * @param name the name to set
   */
  public void setName(final String name) {
    this.name = name;
  }

  public void setValue(final Object value) {
    setValues(Collections.singletonList(value));
  }

  /**
   * @param values the values to set
   */
  public void setValues(final List<Object> values) {
    this.values.clear();
    this.values.addAll(values);
  }

  /**
   * @return the name
   */
  @Override
  public String toString() {
    if (this.name == null) {
      return this.attributeName + " in " + this.values;
    } else {
      return this.name;
    }
  }

}
