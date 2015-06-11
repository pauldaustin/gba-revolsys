package com.revolsys.jdbc.io;

import java.sql.ResultSetMetaData;

import javax.sql.DataSource;

import com.revolsys.data.record.io.RecordStoreQueryReader;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.record.schema.RecordStore;
import com.revolsys.gis.io.Statistics;
import com.revolsys.jdbc.JdbcConnection;

public interface JdbcRecordStore extends RecordStore {

  RecordStoreQueryReader createReader();

  @Override
  JdbcWriter createWriter();

  String getDatabaseQualifiedTableName(final String typePath);

  String getDatabaseSchemaName(final String schemaName);

  String getDatabaseTableName(final String typePath);

  DataSource getDataSource();

  String getGeneratePrimaryKeySql(RecordDefinition metaData);

  JdbcConnection getJdbcConnection();

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
