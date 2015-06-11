package com.revolsys.gis.oracle.io;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.slf4j.LoggerFactory;

import com.revolsys.collection.ResultPager;
import com.revolsys.collection.iterator.AbstractIterator;
import com.revolsys.collection.map.IntHashMap;
import com.revolsys.data.query.Query;
import com.revolsys.data.query.SqlCondition;
import com.revolsys.data.record.ArrayRecordFactory;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordFactory;
import com.revolsys.data.record.property.FieldProperties;
import com.revolsys.data.record.property.ShortNameProperty;
import com.revolsys.data.record.schema.FieldDefinition;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.types.DataTypes;
import com.revolsys.gis.cs.CoordinateSystem;
import com.revolsys.gis.cs.WktCsParser;
import com.revolsys.gis.cs.epsg.EpsgCoordinateSystems;
import com.revolsys.gis.oracle.esri.ArcSdeBinaryGeometryDataStoreExtension;
import com.revolsys.gis.oracle.esri.ArcSdeStGeometryAttribute;
import com.revolsys.gis.oracle.esri.ArcSdeStGeometryRecordStoreExtension;
import com.revolsys.io.Path;
import com.revolsys.jdbc.JdbcUtils;
import com.revolsys.jdbc.field.JdbcFieldAdder;
import com.revolsys.jdbc.io.AbstractJdbcRecordStore;
import com.revolsys.jdbc.io.RecordStoreIteratorFactory;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.util.Property;

public class OracleRecordStore extends AbstractJdbcRecordStore {
  public static final List<String> ORACLE_INTERNAL_SCHEMAS = Arrays.asList("ANONYMOUS",
    "APEX_030200", "AURORA$JIS$UTILITY$", "AURORA$ORB$UNAUTHENTICATED", "AWR_STAGE", "CSMIG",
    "CTXSYS", "DBSNMP", "DEMO", "DIP", "DMSYS", "DSSYS", "EXFSYS", "LBACSYS", "MDSYS", "OLAPSYS",
    "ORACLE_OCM", "ORDDATA", "ORDPLUGINS", "ORDSYS", "OSE$HTTP$ADMIN", "OUTLN", "PERFSTAT", "SDE",
    "SYS", "SYSTEM", "TRACESVR", "TSMSYS", "WMSYS", "XDB");

  private boolean initialized;

  private boolean useSchemaSequencePrefix = true;

  private final IntHashMap<CoordinateSystem> oracleCoordinateSystems = new IntHashMap<>();

  public OracleRecordStore() {
    this(new ArrayRecordFactory());
  }

  public OracleRecordStore(final DataSource dataSource) {
    super(dataSource);
    initSettings();
  }

  public OracleRecordStore(final OracleDatabaseFactory databaseFactory,
    final Map<String, ? extends Object> connectionProperties) {
    super(databaseFactory);
    setConnectionProperties(connectionProperties);
    final DataSource dataSource = databaseFactory.createDataSource(connectionProperties);
    setDataSource(dataSource);
    initSettings();

  }

  public OracleRecordStore(final RecordFactory recordFactory) {
    super(recordFactory);
    initSettings();
  }

  public OracleRecordStore(final RecordFactory recordFactory, final DataSource dataSource) {
    this(recordFactory);
    setDataSource(dataSource);
  }

  protected Query addBoundingBoxFilter(Query query) {
    final BoundingBox boundingBox = query.getBoundingBox();
    if (boundingBox != null) {
      final String typePath = query.getTypeName();
      final RecordDefinition recordDefinition = getRecordDefinition(typePath);
      if (recordDefinition == null) {
        throw new IllegalArgumentException("Unable to  find table " + typePath);
      } else if (recordDefinition.getGeometryField() == null) {
        return query;
      } else {
        query = query.clone();
        final FieldDefinition geometryAttribute = recordDefinition.getGeometryField();
        final String geometryColumnName = geometryAttribute.getName();
        final GeometryFactory geometryFactory = geometryAttribute.getProperty(FieldProperties.GEOMETRY_FACTORY);

        final BoundingBox projectedBoundingBox = boundingBox.convert(geometryFactory);

        final double x1 = projectedBoundingBox.getMinX();
        final double y1 = projectedBoundingBox.getMinY();
        final double x2 = projectedBoundingBox.getMaxX();
        final double y2 = projectedBoundingBox.getMaxY();

        if (geometryAttribute instanceof OracleSdoGeometryJdbcAttribute) {
          final String where = " SDO_RELATE("
            + geometryColumnName
            + ","
            + "MDSYS.SDO_GEOMETRY(2003,?,NULL,MDSYS.SDO_ELEM_INFO_ARRAY(1,1003,3),MDSYS.SDO_ORDINATE_ARRAY(?,?,?,?)),'mask=ANYINTERACT querytype=WINDOW') = 'TRUE'";
          query.and(new SqlCondition(where, geometryFactory.getSRID(), x1, y1, x2, y2));
        } else if (geometryAttribute instanceof ArcSdeStGeometryAttribute) {
          final String where = " SDE.ST_ENVINTERSECTS(" + geometryColumnName + ", ?, ?, ?, ?) = 1";
          query.and(new SqlCondition(where, x1, y1, x2, y2));
        } else {
          throw new IllegalArgumentException("Unknown geometry attribute " + geometryAttribute);
        }
      }
    }
    return query;
  }

  public AbstractIterator<Record> createOracleIterator(final OracleRecordStore recordStore,
    final Query query, final Map<String, Object> properties) {
    return new OracleJdbcQueryIterator(recordStore, query, properties);
  }

  public synchronized CoordinateSystem getCoordinateSystem(final int oracleSrid) {
    CoordinateSystem coordinateSystem = this.oracleCoordinateSystems.get(oracleSrid);
    if (coordinateSystem == null) {
      try {
        final Map<String, Object> result = JdbcUtils.selectMap(getDataSource(),
          "SELECT * FROM MDSYS.SDO_CS_SRS WHERE SRID = ?", oracleSrid);
        if (result == null) {
          coordinateSystem = EpsgCoordinateSystems.getCoordinateSystem(oracleSrid);
        } else {
          final String wkt = (String)result.get("WKTEXT");
          coordinateSystem = WktCsParser.read(wkt);
          coordinateSystem = EpsgCoordinateSystems.getCoordinateSystem(coordinateSystem);
        }
      } catch (final Throwable e) {
        LoggerFactory.getLogger(getClass()).error(
          "Unable to load coordinate system: " + oracleSrid, e);
        return null;
      }
      this.oracleCoordinateSystems.put(oracleSrid, coordinateSystem);
    }
    return coordinateSystem;
  }

  @Override
  public String getGeneratePrimaryKeySql(final RecordDefinition recordDefinition) {
    final String sequenceName = getSequenceName(recordDefinition);
    return sequenceName + ".NEXTVAL";
  }

  @Override
  public Object getNextPrimaryKey(final RecordDefinition recordDefinition) {
    final String sequenceName = getSequenceName(recordDefinition);
    return getNextPrimaryKey(sequenceName);
  }

  @Override
  public Object getNextPrimaryKey(final String sequenceName) {
    final String sql = "SELECT " + sequenceName + ".NEXTVAL FROM SYS.DUAL";
    try {
      return JdbcUtils.selectLong(getDataSource(), (Connection)null, sql);
    } catch (final SQLException e) {
      throw new IllegalArgumentException("Cannot create ID for " + sequenceName, e);
    }
  }

  @Override
  public int getRowCount(final Query query) {
    final Query bboxQuery = addBoundingBoxFilter(query);
    if (bboxQuery != query) {
      query.setFieldNames("count(*))");
    }
    return super.getRowCount(query);
  }

  public String getSequenceName(final RecordDefinition recordDefinition) {
    if (recordDefinition == null) {
      return null;
    } else {
      final String typePath = recordDefinition.getPath();
      final String schema = getDatabaseSchemaName(Path.getPath(typePath));
      final String shortName = ShortNameProperty.getShortName(recordDefinition);
      final String sequenceName;
      if (Property.hasValue(shortName)) {
        if (this.useSchemaSequencePrefix) {
          sequenceName = schema + "." + shortName.toLowerCase() + "_SEQ";
        } else {
          sequenceName = shortName.toLowerCase() + "_SEQ";
        }
      } else {
        final String tableName = getDatabaseTableName(typePath);
        if (this.useSchemaSequencePrefix) {
          sequenceName = schema + "." + tableName + "_SEQ";
        } else {
          sequenceName = tableName + "_SEQ";
        }
      }
      return sequenceName;
    }
  }

  @Override
  @PostConstruct
  public void initialize() {
    super.initialize();
    if (!this.initialized) {
      this.initialized = true;
      final JdbcFieldAdder attributeAdder = new JdbcFieldAdder();
      addFieldAdder("NUMBER", attributeAdder);

      addFieldAdder("CHAR", attributeAdder);
      addFieldAdder("NCHAR", attributeAdder);
      addFieldAdder("VARCHAR", attributeAdder);
      addFieldAdder("VARCHAR2", attributeAdder);
      addFieldAdder("NVARCHAR2", new JdbcFieldAdder(DataTypes.STRING));
      addFieldAdder("LONG", attributeAdder);
      addFieldAdder("CLOB", attributeAdder);
      addFieldAdder("NCLOB", attributeAdder);

      addFieldAdder("DATE", attributeAdder);
      addFieldAdder("TIMESTAMP", attributeAdder);

      final OracleSdoGeometryAttributeAdder sdoGeometryAttributeAdder = new OracleSdoGeometryAttributeAdder(
        this, getDataSource());
      addFieldAdder("SDO_GEOMETRY", sdoGeometryAttributeAdder);
      addFieldAdder("MDSYS.SDO_GEOMETRY", sdoGeometryAttributeAdder);

      final OracleBlobFieldAdder blobAdder = new OracleBlobFieldAdder();
      addFieldAdder("BLOB", blobAdder);

      final OracleClobFieldAdder clobAdder = new OracleClobFieldAdder();
      addFieldAdder("CLOB", clobAdder);

      setPrimaryKeySql("SELECT distinct cols.table_name, cols.column_name FROM all_constraints cons, all_cons_columns cols WHERE cons.constraint_type = 'P' AND cons.constraint_name = cols.constraint_name AND cons.owner = cols.owner AND cons.owner =?");
      setPrimaryKeyTableCondition(" AND cols.table_name = ?");

      setSchemaPermissionsSql("select distinct p.owner \"SCHEMA_NAME\" "
          + "from ALL_TAB_PRIVS_RECD P "
          + "where p.privilege in ('SELECT', 'INSERT', 'UPDATE', 'DELETE') union all select USER \"SCHEMA_NAME\" from DUAL");
      setSchemaTablePermissionsSql("select distinct p.owner \"SCHEMA_NAME\", p.table_name, p.privilege, comments \"REMARKS\" "
          + "from ALL_TAB_PRIVS_RECD P "
          + "join all_tab_comments C on (p.owner = c.owner and p.table_name = c.table_name) "
          + "where p.owner = ? and c.table_type in ('TABLE', 'VIEW') and p.privilege in ('SELECT', 'INSERT', 'UPDATE', 'DELETE') "
          + " union all "
          + "select user \"SCHEMA_NAME\", t.table_name, 'ALL', comments from user_tables t join user_tab_comments c on (t.table_name = c.table_name) and c.table_type in ('TABLE', 'VIEW')");

      addRecordStoreExtension(new ArcSdeStGeometryRecordStoreExtension());
      addRecordStoreExtension(new ArcSdeBinaryGeometryDataStoreExtension());
    }
  }

  private void initSettings() {
    setExcludeTablePatterns(".*\\$");
    setSqlPrefix("BEGIN ");
    setSqlSuffix(";END;");
    setIteratorFactory(new RecordStoreIteratorFactory(this, "createOracleIterator"));
  }

  @Override
  public boolean isSchemaExcluded(final String schemaName) {
    return ORACLE_INTERNAL_SCHEMAS.contains(schemaName);
  }

  public boolean isUseSchemaSequencePrefix() {
    return this.useSchemaSequencePrefix;
  }

  @Override
  public ResultPager<Record> page(final Query query) {
    return new OracleJdbcQueryResultPager(this, getProperties(), query);
  }

  public void setUseSchemaSequencePrefix(final boolean useSchemaSequencePrefix) {
    this.useSchemaSequencePrefix = useSchemaSequencePrefix;
  }

}
