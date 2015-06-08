package com.revolsys.jdbc.io;

import java.sql.Connection;
import java.sql.ResultSetMetaData;

import javax.sql.DataSource;

import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.record.schema.RecordStore;
import com.revolsys.gis.data.io.DataObjectStoreQueryReader;
import com.revolsys.gis.io.Statistics;

public interface JdbcDataObjectStore extends RecordStore {

  DataObjectStoreQueryReader createReader();

  @Override
  JdbcWriter createWriter();

  Connection getConnection();

  String getDatabaseQualifiedTableName(final String typePath);

  String getDatabaseSchemaName(final String schemaName);

  String getDatabaseTableName(final String typePath);

  DataSource getDataSource();

  String getGeneratePrimaryKeySql(RecordDefinition metaData);

  @Override
  String getLabel();

  RecordDefinition getMetaData(String tableName, ResultSetMetaData resultSetMetaData);

  Object getNextPrimaryKey(RecordDefinition metaData);

  Object getNextPrimaryKey(String typePath);

  @Override
  Statistics getStatistics(String name);

  @Override
  void initialize();

  void setDataSource(DataSource dataSource);

  @Override
  void setLabel(String label);
}
