package com.revolsys.gis.postgresql;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.revolsys.data.record.property.FieldProperties;
import com.revolsys.data.record.schema.FieldDefinition;
import com.revolsys.data.record.schema.RecordDefinitionImpl;
import com.revolsys.data.types.DataType;
import com.revolsys.data.types.DataTypes;
import com.revolsys.io.Path;
import com.revolsys.jdbc.JdbcUtils;
import com.revolsys.jdbc.field.JdbcFieldAdder;
import com.revolsys.jdbc.io.AbstractJdbcRecordStore;
import com.revolsys.jdbc.io.JdbcConstants;
import com.revolsys.jdbc.io.SqlFunction;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.util.Property;

public class PostgreSQLGeometryFieldAdder extends JdbcFieldAdder {

  private static final Logger LOG = LoggerFactory.getLogger(PostgreSQLGeometryFieldAdder.class);

  private static final Map<String, DataType> DATA_TYPE_MAP = new HashMap<String, DataType>();

  static {
    DATA_TYPE_MAP.put("GEOMETRY", DataTypes.GEOMETRY);
    DATA_TYPE_MAP.put("POINT", DataTypes.POINT);
    DATA_TYPE_MAP.put("LINESTRING", DataTypes.LINE_STRING);
    DATA_TYPE_MAP.put("POLYGON", DataTypes.POLYGON);
    DATA_TYPE_MAP.put("MULTIPOINT", DataTypes.MULTI_POINT);
    DATA_TYPE_MAP.put("MULTILINESTRING", DataTypes.MULTI_LINE_STRING);
    DATA_TYPE_MAP.put("MULTIPOLYGON", DataTypes.MULTI_POLYGON);
  }

  private final DataSource dataSource;

  private final PostgreSQLRecordStore recordStore;

  public PostgreSQLGeometryFieldAdder(final PostgreSQLRecordStore recordStore,
    final DataSource dataSource) {
    this.recordStore = recordStore;
    this.dataSource = dataSource;
  }

  @Override
  public FieldDefinition addField(final AbstractJdbcRecordStore recordStore,
    final RecordDefinitionImpl metaData, final String dbName, final String name,
    final String dataTypeName, final int sqlType, final int length, final int scale,
    final boolean required, final String description) {
    final String typePath = metaData.getPath();
    String owner = this.recordStore.getDatabaseSchemaName(Path.getPath(typePath));
    if (!Property.hasValue(owner)) {
      owner = "public";
    }
    final String tableName = this.recordStore.getDatabaseTableName(typePath);
    final String columnName = name.toLowerCase();
    try {
      int srid = 0;
      String type = "geometry";
      int numAxis = 3;
      try {
        final String sql = "select SRID, TYPE, COORD_DIMENSION from GEOMETRY_COLUMNS where UPPER(F_TABLE_SCHEMA) = UPPER(?) AND UPPER(F_TABLE_NAME) = UPPER(?) AND UPPER(F_GEOMETRY_COLUMN) = UPPER(?)";
        final Map<String, Object> values = JdbcUtils.selectMap(this.dataSource, sql, owner,
          tableName, columnName);
        srid = (Integer)values.get("srid");
        type = (String)values.get("type");
        numAxis = (Integer)values.get("coord_dimension");
      } catch (final IllegalArgumentException e) {
        LOG.warn("Cannot get geometry column metadata for " + typePath + "." + columnName);
      }

      final DataType dataType = DATA_TYPE_MAP.get(type);
      final GeometryFactory storeGeometryFactory = this.recordStore.getGeometryFactory();
      final GeometryFactory geometryFactory;
      if (storeGeometryFactory == null) {
        geometryFactory = GeometryFactory.getFactory(srid, numAxis, 0, 0);
      } else {
        geometryFactory = GeometryFactory.getFactory(srid, numAxis,
          storeGeometryFactory.getScaleXY(), storeGeometryFactory.getScaleZ());
      }
      final FieldDefinition attribute = new PostgreSQLGeometryJdbcAttribute(dbName, name, dataType,
        required, description, null, srid, numAxis, geometryFactory);
      metaData.addField(attribute);
      attribute.setProperty(JdbcConstants.FUNCTION_INTERSECTS,
        new SqlFunction("st_intersects(", ")"));
      attribute.setProperty(JdbcConstants.FUNCTION_BUFFER, new SqlFunction("st_buffer(", ")"));
      attribute.setProperty(JdbcConstants.FUNCTION_EQUAL, new SqlFunction("st_equals(", ")"));
      attribute.setProperty(FieldProperties.GEOMETRY_FACTORY, geometryFactory);
      return attribute;
    } catch (final SQLException e) {
      LOG.error(
        "Attribute not registered in GEOMETRY_COLUMN table " + owner + "." + tableName + "." + name,
        e);
      return null;
    } catch (final Throwable e) {
      LOG.error("Error registering attribute " + name, e);
      return null;
    }
  }
}
