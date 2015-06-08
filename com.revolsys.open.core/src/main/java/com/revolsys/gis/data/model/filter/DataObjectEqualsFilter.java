package com.revolsys.gis.data.model.filter;

import java.util.Collection;
import java.util.HashSet;

import com.revolsys.data.record.Record;
import com.revolsys.filter.Filter;
import com.revolsys.gis.model.data.equals.EqualsInstance;
import com.revolsys.gis.model.data.equals.EqualsRegistry;
import com.vividsolutions.jts.geom.Geometry;

public class DataObjectEqualsFilter implements Filter<Record> {
  private EqualsRegistry equalsRegistry = EqualsInstance.INSTANCE;

  private final Collection<String> equalExclude = new HashSet<String>();

  private final Record searchObject;

  public DataObjectEqualsFilter(final EqualsRegistry equalsRegistry, final Record searchObject) {
    this(null, searchObject, null);
  }

  public DataObjectEqualsFilter(final EqualsRegistry equalsRegistry, final Record searchObject,
    final Collection<String> equalExclude) {
    if (equalsRegistry != null) {
      this.equalsRegistry = equalsRegistry;
    }
    this.searchObject = searchObject;
    if (equalExclude != null) {
      this.equalExclude.addAll(equalExclude);
    }
  }

  public DataObjectEqualsFilter(final Record searchObject) {
    this(null, searchObject, null);
  }

  public DataObjectEqualsFilter(final Record searchObject, final Collection<String> equalExclude) {
    this(null, searchObject, equalExclude);
  }

  @Override
  public boolean accept(final Record object) {
    final Geometry serachGeometry = this.searchObject.getGeometryValue();
    final Geometry geometry = object.getGeometryValue();

    if (this.equalsRegistry.equals(serachGeometry, geometry, this.equalExclude)) {
      if (this.equalsRegistry.equals(this.searchObject, object, this.equalExclude)) {
        return true;
      }
    }
    return false;
  }

}
