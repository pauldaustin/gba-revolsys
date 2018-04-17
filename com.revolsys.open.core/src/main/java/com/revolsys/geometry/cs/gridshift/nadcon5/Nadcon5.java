package com.revolsys.geometry.cs.gridshift.nadcon5;

import com.revolsys.geometry.cs.gridshift.GridHorizontalShiftOperation;

public interface Nadcon5 {

  static String NAD27 = "NAD27";

  static String NAD83_CURRENT = "NAD83(2011)";

  static GridHorizontalShiftOperation NAD_27_TO_83 = newGridShiftOperation(NAD27, NAD83_CURRENT);

  static GridHorizontalShiftOperation NAD_83_TO_27 = newGridShiftOperation(NAD83_CURRENT, NAD27);

  static GridHorizontalShiftOperation newGridShiftOperation(final String sourceDatumName,
    final String targetDatumName) {
    return new Nadcon5GridShiftOperation(sourceDatumName, targetDatumName);
  }
}
