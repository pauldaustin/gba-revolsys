package com.revolsys.gis.postgresql;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.postgis.PGbox2d;
import org.postgis.Point;

import com.revolsys.collection.ResultPager;
import com.revolsys.collection.iterator.AbstractIterator;
import com.revolsys.datatype.DataTypes;
import com.revolsys.io.Path;
import com.revolsys.jdbc.JdbcConnection;
import com.revolsys.jdbc.JdbcUtils;
import com.revolsys.jdbc.field.JdbcFieldAdder;
import com.revolsys.jdbc.field.JdbcFieldDefinition;
import com.revolsys.jdbc.io.AbstractJdbcRecordStore;
import com.revolsys.jdbc.io.RecordStoreIteratorFactory;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.record.ArrayRecordFactory;
import com.revolsys.record.Record;
import com.revolsys.record.RecordFactory;
import com.revolsys.record.property.ShortNameProperty;
import com.revolsys.record.query.BinaryCondition;
import com.revolsys.record.query.Query;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.record.schema.RecordDefinitionImpl;
import com.revolsys.util.Property;

public class PostgreSQLRecordStore extends AbstractJdbcRecordStore {

  public static final List<String> POSTGRESQL_INTERNAL_SCHEMAS = Arrays.asList("information_schema",
    "pg_catalog", "pg_toast_temp_1");

  public static final AbstractIterator<Record> createPostgreSQLIterator(
    final PostgreSQLRecordStore recordStore, final Query query,
    final Map<String, Object> properties) {
    return new PostgreSQLJdbcQueryIterator(recordStore, query, properties);
  }

  private boolean useSchemaSequencePrefix = true;

  public PostgreSQLRecordStore() {
    this(new ArrayRecordFactory());
  }

  public PostgreSQLRecordStore(final DataSource dataSource) {
    super(dataSource);
    initSettings();
  }

  public PostgreSQLRecordStore(final PostgreSQLDatabaseFactory databaseFactory,
    final Map<String, ? extends Object> connectionProperties) {
    super(databaseFactory);
    final DataSource dataSource = databaseFactory.createDataSource(connectionProperties);
    setDataSource(dataSource);
    initSettings();
    setConnectionProperties(connectionProperties);
  }

  public PostgreSQLRecordStore(final RecordFactory recordFactory) {
    super(recordFactory);
    initSettings();
  }

  public PostgreSQLRecordStore(final RecordFactory recordFactory, final DataSource dataSource) {
    this(recordFactory);
    setDataSource(dataSource);
  }

  @Override
  protected JdbcFieldDefinition addField(final RecordDefinitionImpl recordDefinition,
    final String dbColumnName, final String name, final String dataType, final int sqlType,
    final int length, final int scale, final boolean required, final String description) {
    final JdbcFieldDefinition fieldDefinition = super.addField(recordDefinition, dbColumnName, name,
      dataType, sqlType, length, scale, required, description);
    if (!dbColumnName.matches("[a-z_]")) {
      fieldDefinition.setQuoteName(true);
    }
    return fieldDefinition;
  }

  @Override
  public String getGeneratePrimaryKeySql(final RecordDefinition recordDefinition) {
    final String sequenceName = getSequenceName(recordDefinition);
    return "nextval('" + sequenceName + "')";
  }

  @Override
  public Object getNextPrimaryKey(final RecordDefinition recordDefinition) {
    final String sequenceName = getSequenceName(recordDefinition);
    return getNextPrimaryKey(sequenceName);
  }

  @Override
  public Object getNextPrimaryKey(final String sequenceName) {
    final String sql = "SELECT nextval(?)";
    try (
      JdbcConnection connection = getJdbcConnection()) {
      return JdbcUtils.selectLong(null, connection, sql, sequenceName);
    } catch (final SQLException e) {
      throw new IllegalArgumentException("Cannot create ID for " + sequenceName, e);
    }
  }

  @Override
  public int getRowCount(Query query) {
    BoundingBox boundingBox = query.getBoundingBox();
    if (boundingBox != null) {
      final String typePath = query.getTypeName();
      final RecordDefinition recordDefinition = getRecordDefinition(typePath);
      if (recordDefinition == null) {
        throw new IllegalArgumentException("Unable to  find table " + typePath);
      } else {
        query = query.clone();
        query.setFieldNames("count(*))");
        final String geometryFieldName = recordDefinition.getGeometryFieldName();
        final GeometryFactory geometryFactory = recordDefinition.getGeometryFactory();
        boundingBox = boundingBox.convert(geometryFactory);
        final double x1 = boundingBox.getMinX();
        final double y1 = boundingBox.getMinY();
        final double x2 = boundingBox.getMaxX();
        final double y2 = boundingBox.getMaxY();

        final PGbox2d box = new PGbox2d(new Point(x1, y1), new Point(x2, y2));
        query.and(new BinaryCondition(geometryFieldName, "&&", box));
      }
    }

    return super.getRowCount(query);
  }

  public String getSequenceName(final RecordDefinition recordDefinition) {
    final String typePath = recordDefinition.getPath();
    final String schema = getDatabaseSchemaName(Path.getPath(typePath));
    final String shortName = ShortNameProperty.getShortName(recordDefinition);
    final String sequenceName;
    if (Property.hasValue(shortName)) {
      if (this.useSchemaSequencePrefix) {
        sequenceName = schema + "." + shortName.toLowerCase() + "_seq";
      } else {
        sequenceName = shortName.toLowerCase() + "_seq";
      }
    } else {
      final String tableName = getDatabaseTableName(typePath);
      final String idFieldName = recordDefinition.getIdFieldName().toLowerCase();
      if (this.useSchemaSequencePrefix) {
        sequenceName = schema + "." + tableName + "_" + idFieldName + "_seq";
      } else {
        sequenceName = tableName + "_" + idFieldName + "_seq";
      }
    }
    return sequenceName;

  }

  @Override
  @PostConstruct
  public void initialize() {
    super.initialize();
    final JdbcFieldAdder numberFieldAdder = new JdbcFieldAdder(DataTypes.DECIMAL);
    addFieldAdder("numeric", numberFieldAdder);

    final JdbcFieldAdder stringFieldAdder = new JdbcFieldAdder(DataTypes.STRING);
    addFieldAdder("varchar", stringFieldAdder);
    addFieldAdder("text", stringFieldAdder);
    addFieldAdder("name", stringFieldAdder);
    addFieldAdder("bpchar", stringFieldAdder);

    final JdbcFieldAdder longFieldAdder = new JdbcFieldAdder(DataTypes.LONG);
    addFieldAdder("int8", longFieldAdder);
    addFieldAdder("bigint", longFieldAdder);
    addFieldAdder("bigserial", longFieldAdder);
    addFieldAdder("serial8", longFieldAdder);

    final JdbcFieldAdder intFieldAdder = new JdbcFieldAdder(DataTypes.INT);
    addFieldAdder("int4", intFieldAdder);
    addFieldAdder("integer", intFieldAdder);
    addFieldAdder("serial", intFieldAdder);
    addFieldAdder("serial4", intFieldAdder);

    final JdbcFieldAdder shortFieldAdder = new JdbcFieldAdder(DataTypes.SHORT);
    addFieldAdder("int2", shortFieldAdder);
    addFieldAdder("smallint", shortFieldAdder);

    final JdbcFieldAdder floatFieldAdder = new JdbcFieldAdder(DataTypes.FLOAT);
    addFieldAdder("float4", floatFieldAdder);

    final JdbcFieldAdder doubleFieldAdder = new JdbcFieldAdder(DataTypes.DOUBLE);
    addFieldAdder("float8", doubleFieldAdder);
    addFieldAdder("double precision", doubleFieldAdder);

    addFieldAdder("date", new JdbcFieldAdder(DataTypes.DATE_TIME));

    addFieldAdder("bool", new JdbcFieldAdder(DataTypes.BOOLEAN));

    final JdbcFieldAdder geometryFieldAdder = new PostgreSQLGeometryFieldAdder(this,
      getDataSource());
    addFieldAdder("geometry", geometryFieldAdder);
    setPrimaryKeySql("SELECT t.relname \"TABLE_NAME\", c.attname \"COLUMN_NAME\"" //
      + " FROM pg_namespace s" //
      + " join pg_class t on t.relnamespace = s.oid" //
      + " join pg_index i on i.indrelid = t.oid " //
      + " join pg_attribute c on c.attrelid = t.oid" //
      + " WHERE s.nspname = ? AND c.attnum = any(i.indkey) AND i.indisprimary");
    setPrimaryKeyTableCondition(" AND r.relname = ?");
    setSchemaPermissionsSql("select distinct t.table_schema as \"SCHEMA_NAME\" "
      + "from information_schema.role_table_grants t  "
      + "where (t.grantee  in (current_user, 'PUBLIC') or "
      + "t.grantee in (select role_name from information_schema.applicable_roles r where r.grantee = current_user)) and "
      + "privilege_type IN ('SELECT', 'INSERT','UPDATE','DELETE') ");
    setSchemaTablePermissionsSql(
      "select distinct t.table_schema as \"SCHEMA_NAME\", t.table_name, t.privilege_type as \"PRIVILEGE\", d.description as \"REMARKS\" from information_schema.role_table_grants t join pg_namespace n on t.table_schema = n.nspname join pg_class c on (n.oid = c.relnamespace AND t.table_name = c.relname) left join pg_description d on d.objoid = c.oid "
        + "where t.table_schema = ? and "
        + "(t.grantee  in (current_user, 'PUBLIC') or t.grantee in (select role_name from information_schema.applicable_roles r where r.grantee = current_user)) AND "
        + "privilege_type IN ('SELECT', 'INSERT','UPDATE','DELETE') "
        + "order by t.table_schema, t.table_name, t.privilege_type");
  }

  protected void initSettings() {
    setIteratorFactory(
      new RecordStoreIteratorFactory(PostgreSQLRecordStore.class, "createPostgreSQLIterator"));
  }

  @Override
  public boolean isSchemaExcluded(final String schemaName) {
    return POSTGRESQL_INTERNAL_SCHEMAS.contains(schemaName);
  }

  public boolean isUseSchemaSequencePrefix() {
    return this.useSchemaSequencePrefix;
  }

  @Override
  public ResultPager<Record> page(final Query query) {
    return new PostgreSQLJdbcQueryResultPager(this, getProperties(), query);
  }

  public void setUseSchemaSequencePrefix(final boolean useSchemaSequencePrefix) {
    this.useSchemaSequencePrefix = useSchemaSequencePrefix;
  }
}
