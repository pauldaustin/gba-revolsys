package com.revolsys.jump.oracle.driver;

import java.sql.SQLException;

import javax.xml.namespace.QName;

import oracle.jdbc.pool.OracleDataSource;

import com.revolsys.gis.jdbc.io.JdbcDataObjectStore;
import com.revolsys.gis.oracle.io.OracleDataObjectStore;
import com.revolsys.jump.model.FeatureDataObjectFactory;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.datastore.AdhocQuery;
import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.DataStoreException;
import com.vividsolutions.jump.datastore.DataStoreMetadata;
import com.vividsolutions.jump.datastore.FilterQuery;
import com.vividsolutions.jump.datastore.Query;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.io.FeatureInputStream;
import com.vividsolutions.jump.parameter.ParameterList;

public class OracleDataStoreConnection implements DataStoreConnection {

  private JdbcDataStoreMetaData metaData;

  private JdbcDataObjectStore dataStore;

  private OracleDataSource dataSource;

  private String schema;

  public OracleDataStoreConnection(
    final ParameterList params)
    throws DataStoreException {
    try {
      String url = (String)params.getParameter(OracleDataStoreDriver.URL);
      if (url == null) {
        String host = (String)params.getParameter(OracleDataStoreDriver.HOST);
        String db = (String)params.getParameter(OracleDataStoreDriver.DB);
        String port = (String)params.getParameter(OracleDataStoreDriver.PORT);
        url = "jdbc:oracle:thin:@" + host + ":" + port + ":" + db;
      }
      schema = params.getParameter(OracleDataStoreDriver.SCHEMA)
        .toString()
        .toUpperCase();

      String user = (String)params.getParameter(OracleDataStoreDriver.USER);
      String password = (String)params.getParameter(OracleDataStoreDriver.PASSWORD);
      if (schema == null) {
        schema = user;
      }
      // TODO delete

      dataSource = new OracleDataSource();
      dataSource.setURL(url);
      dataSource.setUser(user);
      dataSource.setPassword(password);
      dataSource.setConnectionCachingEnabled(true);
      dataStore = new OracleDataObjectStore(new FeatureDataObjectFactory(), dataSource);
      dataStore.initialize();
      metaData = new JdbcDataStoreMetaData(dataStore, schema);
    } catch (SQLException e) {
      throw new DataStoreException(e.getMessage(), e);
    }
  }

  public JdbcDataObjectStore getDataStore() {
    return dataStore;
  }

  public void close()
    throws DataStoreException {
    try {
      dataSource.close();
      dataSource = null;
      dataStore = null;
      metaData = null;
    } catch (SQLException e) {
      throw new DataStoreException(e.getMessage(), e);
    }
  }

  public FeatureInputStream execute(
    final Query query) {
    if (query instanceof FilterQuery) {
      return executeFilterQuery((FilterQuery)query);
    }
    if (query instanceof AdhocQuery) {
      return executeAdhocQuery((AdhocQuery)query);
    }
    throw new IllegalArgumentException("Unsupported Query type");
  }

  /**
   * Executes a filter query. The SRID is optional for queries - it will be
   * determined automatically from the table metadata if not supplied.
   * 
   * @param query the query to execute
   * @return the results of the query
   */
  public FeatureInputStream executeFilterQuery(
    final FilterQuery query) {
    String datasetName = query.getDatasetName();
    FeatureSchema featureSchema = metaData.getFeatureSchema(datasetName,
      query.getGeometryAttributeName());
    QName typeName = new QName(schema, datasetName);
    Geometry filterGeometry = query.getFilterGeometry();
    String condition = query.getCondition();
    JdbcDataObjectStoreInputStream ifs = new JdbcDataObjectStoreInputStream(
      dataStore, typeName, featureSchema, filterGeometry, condition);
    return ifs;
  }

  public FeatureInputStream executeAdhocQuery(
    final AdhocQuery query) {
    query.getQuery();

    return null;
  }

  public DataStoreMetadata getMetadata() {
    return metaData;
  }

  public boolean isClosed()
    throws DataStoreException {
    return dataSource == null;
  }
}
