package com.revolsys.jdbc.io;

import java.sql.ResultSetMetaData;

import com.revolsys.gis.io.Statistics;
import com.revolsys.jdbc.JdbcConnection;
import com.revolsys.record.io.RecordStoreQueryReader;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.record.schema.RecordStore;

public interface JdbcRecordStore extends RecordStore {

  RecordStoreQueryReader createReader();

  @Override
  JdbcWriter createWriter();

  String getDatabaseQualifiedTableName(final String typePath);

  String getDatabaseSchemaName(final String schemaName);

  String getDatabaseTableName(final String typePath);

  String getGeneratePrimaryKeySql(RecordDefinition recordDefinition);

  JdbcConnection getJdbcConnection();

  JdbcConnection getJdbcConnection(boolean autoCommit);

  @Override
  String getLabel();

  Object getNextPrimaryKey(RecordDefinition recordDefinition);

  Object getNextPrimaryKey(String typePath);

  RecordDefinition getRecordDefinition(String tableName,
    ResultSetMetaData resultSetRecordDefinition);

  @Override
  Statistics getStatistics(String name);

  @Override
  void initialize();

  @Override
  void setLabel(String label);
}
