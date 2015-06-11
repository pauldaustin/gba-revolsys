package com.revolsys.jdbc.io;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import com.revolsys.collection.iterator.AbstractIterator;
import com.revolsys.converter.string.BooleanStringConverter;
import com.revolsys.data.query.Query;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordFactory;
import com.revolsys.data.record.RecordState;
import com.revolsys.data.record.io.RecordIterator;
import com.revolsys.data.record.schema.FieldDefinition;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.record.schema.RecordDefinitionImpl;
import com.revolsys.gis.io.Statistics;
import com.revolsys.jdbc.JdbcUtils;
import com.revolsys.jdbc.field.JdbcFieldDefinition;

public class JdbcQueryIterator extends AbstractIterator<Record> implements RecordIterator {

  public static Record getNextObject(final JdbcRecordStore dataStore,
    final RecordDefinition metaData, final List<FieldDefinition> attributes,
    final RecordFactory dataObjectFactory, final ResultSet resultSet) {
    final Record object = dataObjectFactory.createRecord(metaData);
    if (object != null) {
      object.setState(RecordState.Initalizing);
      int columnIndex = 1;

      for (final FieldDefinition attribute : attributes) {
        final JdbcFieldDefinition jdbcAttribute = (JdbcFieldDefinition)attribute;
        try {
          columnIndex = jdbcAttribute.setFieldValueFromResultSet(resultSet, columnIndex, object);
        } catch (final SQLException e) {
          throw new RuntimeException("Unable to get value " + (columnIndex + 1)
            + " from result set", e);
        }
      }
      object.setState(RecordState.Persisted);
      dataStore.addStatistic("query", object);
    }
    return object;
  }

  public static ResultSet getResultSet(final RecordDefinition metaData,
    final PreparedStatement statement, final Query query) throws SQLException {
    JdbcUtils.setPreparedStatementParameters(statement, query);

    return statement.executeQuery();
  }

  private Connection connection;

  private final int currentQueryIndex = -1;

  private RecordFactory dataObjectFactory;

  private DataSource dataSource;

  private JdbcRecordStore dataStore;

  private final int fetchSize = 10;

  private RecordDefinition metaData;

  private List<Query> queries;

  private ResultSet resultSet;

  private PreparedStatement statement;

  private List<FieldDefinition> attributes = new ArrayList<FieldDefinition>();

  private Query query;

  private Statistics statistics;

  public JdbcQueryIterator(final JdbcRecordStore dataStore, final Query query,
    final Map<String, Object> properties) {
    super();
    this.connection = dataStore.getJdbcConnection();
    this.dataSource = dataStore.getDataSource();

    if (this.dataSource != null) {
      try {
        this.connection = JdbcUtils.getConnection(this.dataSource);
        boolean autoCommit = false;
        if (BooleanStringConverter.getBoolean(properties.get("autoCommit"))) {
          autoCommit = true;
        }
        this.connection.setAutoCommit(autoCommit);
      } catch (final SQLException e) {
        throw new IllegalArgumentException("Unable to create connection", e);
      }
    }
    this.dataObjectFactory = query.getProperty("recordFactory");
    if (this.dataObjectFactory == null) {
      this.dataObjectFactory = dataStore.getRecordFactory();
    }
    this.dataStore = dataStore;
    this.query = query;
    this.statistics = (Statistics)properties.get(Statistics.class.getName());
  }

  @Override
  @PreDestroy
  public void doClose() {
    JdbcUtils.close(this.statement, this.resultSet);
    JdbcUtils.release(this.connection, this.dataSource);
    this.attributes = null;
    this.connection = null;
    this.dataObjectFactory = null;
    this.dataSource = null;
    this.dataStore = null;
    this.metaData = null;
    this.queries = null;
    this.query = null;
    this.resultSet = null;
    this.statement = null;
    this.statistics = null;
  }

  @Override
  protected void doInit() {
    this.resultSet = getResultSet();
  }

  public JdbcRecordStore getDataStore() {
    return this.dataStore;
  }

  protected String getErrorMessage() {
    if (this.queries == null) {
      return null;
    } else {
      return this.queries.get(this.currentQueryIndex).getSql();
    }
  }

  @Override
  protected Record getNext() throws NoSuchElementException {
    try {
      if (this.resultSet != null && this.resultSet.next()) {
        final Record object = getNextObject(this.dataStore, this.metaData, this.attributes,
          this.dataObjectFactory, this.resultSet);
        if (this.statistics != null) {
          this.statistics.add(object);
        }
        return object;
      } else {
        close();
        throw new NoSuchElementException();
      }
    } catch (final SQLException e) {
      close();
      throw new RuntimeException(getErrorMessage(), e);
    } catch (final RuntimeException e) {
      close();
      throw e;
    } catch (final Error e) {
      close();
      throw e;
    }
  }

  @Override
  public RecordDefinition getRecordDefinition() {
    if (this.metaData == null) {
      hasNext();
    }
    return this.metaData;
  }

  protected ResultSet getResultSet() {
    final String tableName = this.query.getTypeName();
    this.metaData = this.query.getRecordDefinition();
    if (this.metaData == null) {
      if (tableName != null) {
        this.metaData = this.dataStore.getRecordDefinition(tableName);
        this.query.setMetaData(this.metaData);
      }
    }
    final String sql = getSql(this.query);
    try {
      this.statement = this.connection.prepareStatement(sql);
      this.statement.setFetchSize(this.fetchSize);

      this.resultSet = getResultSet(this.metaData, this.statement, this.query);
      final ResultSetMetaData resultSetMetaData = this.resultSet.getMetaData();

      if (this.metaData == null) {
        this.metaData = this.dataStore.getMetaData(tableName, resultSetMetaData);
      }
      final List<String> attributeNames = new ArrayList<String>(this.query.getFieldNames());
      if (attributeNames.isEmpty()) {
        this.attributes.addAll(this.metaData.getFields());
      } else {
        for (final String attributeName : attributeNames) {
          if (attributeName.equals("*")) {
            this.attributes.addAll(this.metaData.getFields());
          } else {
            final FieldDefinition attribute = this.metaData.getField(attributeName);
            if (attribute != null) {
              this.attributes.add(attribute);
            }
          }
        }
      }

      final String typePath = this.query.getTypeNameAlias();
      if (typePath != null) {
        final RecordDefinitionImpl newRecordDefinition = ((RecordDefinitionImpl)this.metaData).rename(typePath);
        this.metaData = newRecordDefinition;
      }
    } catch (final SQLException e) {
      JdbcUtils.close(this.statement, this.resultSet);
      throw JdbcUtils.getException(this.dataSource, this.connection, "Execute Query", sql, e);
    }
    return this.resultSet;
  }

  protected String getSql(final Query query) {
    return JdbcUtils.getSelectSql(query);
  }

  protected void setQuery(final Query query) {
    this.query = query;
  }

}
