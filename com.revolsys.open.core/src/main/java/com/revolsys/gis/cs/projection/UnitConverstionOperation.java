package com.revolsys.gis.cs.projection;

import javax.measure.converter.UnitConverter;
import javax.measure.unit.Unit;

import com.revolsys.gis.model.coordinates.Coordinates;

public class UnitConverstionOperation implements CoordinatesOperation {
  private final UnitConverter converter;

  private int numAxis = 0;

  private final Unit sourceUnit;

  private final Unit targetUnit;

  public UnitConverstionOperation(final Unit sourceUnit, final Unit targetUnit) {
    this.sourceUnit = sourceUnit;
    this.targetUnit = targetUnit;
    this.converter = sourceUnit.getConverterTo(targetUnit);
  }

  public UnitConverstionOperation(final Unit sourceUnit, final Unit targetUnit, final int numAxis) {
    this.sourceUnit = sourceUnit;
    this.targetUnit = targetUnit;
    this.numAxis = numAxis;
    this.converter = sourceUnit.getConverterTo(targetUnit);
  }

  @Override
  public void perform(final Coordinates from, final Coordinates to) {
    final int numAxis = Math.min(from.getNumAxis(), to.getNumAxis());

    for (int i = 0; i < numAxis; i++) {
      final double value = from.getValue(i);
      if (i < this.numAxis) {
        final double convertedValue = this.converter.convert(value);
        to.setValue(i, convertedValue);
      } else {
        to.setValue(i, value);
      }
    }

  }

  @Override
  public String toString() {
    return this.sourceUnit + "->" + this.targetUnit;
  }
}
