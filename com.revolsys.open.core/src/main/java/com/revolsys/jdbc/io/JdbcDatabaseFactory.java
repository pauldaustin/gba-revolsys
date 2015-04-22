package com.revolsys.jdbc.io;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.revolsys.data.record.schema.RecordStore;
import com.revolsys.gis.data.io.RecordStoreFactory;

public interface JdbcDatabaseFactory extends RecordStoreFactory {

  boolean canHandleUrl(String url);

  void closeDataSource(DataSource dataSource);

  JdbcDataObjectStore createDataObjectStore(DataSource dataSource);

  @Override
  JdbcDataObjectStore createRecordStore(
    Map<String, ? extends Object> connectionProperties);

  DataSource createDataSource(Map<String, ? extends Object> connectionProperties);

  @Override
  Class<? extends RecordStore> getRecordStoreInterfaceClass(
    Map<String, ? extends Object> connectionProperties);

  List<String> getProductNames();

  @Override
  List<String> getUrlPatterns();
}
