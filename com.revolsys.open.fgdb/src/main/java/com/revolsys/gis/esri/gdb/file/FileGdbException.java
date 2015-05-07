package com.revolsys.gis.esri.gdb.file;

import java.util.ArrayList;
import java.util.List;

import com.revolsys.gis.esri.gdb.file.capi.swig.EsriFileGdb;
import com.revolsys.gis.esri.gdb.file.capi.swig.VectorOfWString;

public class FileGdbException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final List<String> errors = new ArrayList<>();

  public FileGdbException() {
  }

  public FileGdbException(final String message) {
    super(message);
    initErrors();
  }

  public FileGdbException(final String message, final Throwable cause) {
    super(message, cause);
    initErrors();
  }

  public FileGdbException(final Throwable cause) {
    super(cause);
    initErrors();
  }

  public List<String> getErrors() {
    return this.errors;
  }

  private void initErrors() {
    synchronized (FileGdbRecordStoreImpl.API_SYNC) {
      final VectorOfWString errors = EsriFileGdb.getErrors();
      long errorCount = errors.size();
      for (int i = 0; i < errorCount; i++) {
        final String error = errors.get(i);
        this.errors.add(error);
      }
    }
  }
}
