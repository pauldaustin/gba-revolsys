package com.revolsys.gis.parallel;

import java.util.ArrayList;
import java.util.List;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.filter.AttributesEqualFilter;
import com.revolsys.data.record.filter.AttributesEqualOrNullFilter;
import com.revolsys.predicate.AndFilter;
import com.revolsys.predicate.Factory;
import java.util.function.Predicate;

public class CompareFilterFactory implements Factory<Predicate<Record>, Record> {
  private List<String> equalAttributeNames = new ArrayList<String>();

  private List<String> equalOrNullAttributeNames = new ArrayList<String>();

  @Override
  public Predicate<Record> create(final Record object) {
    final AndFilter<Record> filters = new AndFilter<Record>();
    if (!this.equalAttributeNames.isEmpty()) {
      final Predicate<Record> valuesFilter = new AttributesEqualFilter(object,
        this.equalAttributeNames);
      filters.addFilter(valuesFilter);
    }
    if (!this.equalOrNullAttributeNames.isEmpty()) {
      final Predicate<Record> valuesFilter = new AttributesEqualOrNullFilter(object,
        this.equalOrNullAttributeNames);
      filters.addFilter(valuesFilter);
    }

    return filters;
  }

  public List<String> getEqualAttributeNames() {
    return this.equalAttributeNames;
  }

  public List<String> getEqualOrNullAttributeNames() {
    return this.equalOrNullAttributeNames;
  }

  public void setEqualAttributeNames(final List<String> equalAttributeNames) {
    this.equalAttributeNames = equalAttributeNames;
  }

  public void setEqualOrNullAttributeNames(final List<String> equalOrNullAttributeNames) {
    this.equalOrNullAttributeNames = equalOrNullAttributeNames;
  }

}
