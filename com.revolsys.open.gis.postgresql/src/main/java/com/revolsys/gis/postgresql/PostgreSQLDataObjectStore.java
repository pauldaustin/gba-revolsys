package com.revolsys.gis.postgresql;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.util.StringUtils;

import com.revolsys.collection.AbstractIterator;
import com.revolsys.collection.ResultPager;
import com.revolsys.data.query.Query;
import com.revolsys.data.query.QueryValue;
import com.revolsys.data.query.functions.EnvelopeIntersects;
import com.revolsys.data.record.ArrayRecordFactory;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordFactory;
import com.revolsys.data.record.property.ShortNameProperty;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.record.schema.RecordDefinitionImpl;
import com.revolsys.data.types.DataTypes;
import com.revolsys.io.PathUtil;
import com.revolsys.jdbc.JdbcUtils;
import com.revolsys.jdbc.attribute.JdbcAttribute;
import com.revolsys.jdbc.attribute.JdbcAttributeAdder;
import com.revolsys.jdbc.io.AbstractJdbcDataObjectStore;
import com.revolsys.jdbc.io.DataStoreIteratorFactory;

public class PostgreSQLDataObjectStore extends AbstractJdbcDataObjectStore {

  public static final AbstractIterator<Record> createPostgreSQLIterator(
    final PostgreSQLDataObjectStore dataStore, final Query query,
    final Map<String, Object> properties) {
    return new PostgreSQLJdbcQueryIterator(dataStore, query, properties);
  }

  public static final List<String> POSTGRESQL_INTERNAL_SCHEMAS = Arrays.asList(
    "information_schema", "pg_catalog", "pg_toast_temp_1");

  private boolean useSchemaSequencePrefix = true;

  public PostgreSQLDataObjectStore() {
    this(new ArrayRecordFactory());
  }

  public PostgreSQLDataObjectStore(final RecordFactory dataObjectFactory) {
    super(dataObjectFactory);
    initSettings();
  }

  public PostgreSQLDataObjectStore(final RecordFactory dataObjectFactory,
    final DataSource dataSource) {
    this(dataObjectFactory);
    setDataSource(dataSource);
  }

  public PostgreSQLDataObjectStore(final DataSource dataSource) {
    super(dataSource);
    initSettings();
  }

  public PostgreSQLDataObjectStore(
    final PostgreSQLDatabaseFactory databaseFactory,
    final Map<String, ? extends Object> connectionProperties) {
    super(databaseFactory);
    final DataSource dataSource = databaseFactory.createDataSource(connectionProperties);
    setDataSource(dataSource);
    initSettings();
    setConnectionProperties(connectionProperties);
  }

  @Override
  protected JdbcAttribute addAttribute(final RecordDefinitionImpl metaData,
    final String dbColumnName, final String name, final String dataType,
    final int sqlType, final int length, final int scale,
    final boolean required, final String description) {
    final JdbcAttribute attribute = super.addAttribute(metaData, dbColumnName,
      name, dataType, sqlType, length, scale, required, description);
    if (!dbColumnName.matches("[a-z_]")) {
      attribute.setQuoteName(true);
    }
    return attribute;
  }

  @Override
  public void appendQueryValue(final Query query, final StringBuffer sql,
    final QueryValue queryValue) {
    if (queryValue instanceof EnvelopeIntersects) {
      final EnvelopeIntersects envelopeIntersects = (EnvelopeIntersects)queryValue;
      final QueryValue boundingBox1Value = envelopeIntersects.getBoundingBox1Value();
      if (boundingBox1Value == null) {
        sql.append("NULL");
      } else {
        boundingBox1Value.appendSql(query, this, sql);
      }
      sql.append(" && ");
      final QueryValue boundingBox2Value = envelopeIntersects.getBoundingBox2Value();
      if (boundingBox2Value == null) {
        sql.append("NULL");
      } else {
        boundingBox2Value.appendSql(query, this, sql);
      }
    } else {
      super.appendQueryValue(query, sql, queryValue);
    }
  }

  @Override
  public String getGeneratePrimaryKeySql(final RecordDefinition metaData) {
    final String sequenceName = getSequenceName(metaData);
    return "nextval('" + sequenceName + "')";
  }

  @Override
  public Object getNextPrimaryKey(final RecordDefinition metaData) {
    final String sequenceName = getSequenceName(metaData);
    return getNextPrimaryKey(sequenceName);
  }

  @Override
  public Object getNextPrimaryKey(final String sequenceName) {
    final String sql = "SELECT nextval(?)";
    try {
      return JdbcUtils.selectLong(getDataSource(), getConnection(), sql,
        sequenceName);
    } catch (final SQLException e) {
      throw new IllegalArgumentException(
        "Cannot create ID for " + sequenceName, e);
    }
  }

  public String getSequenceName(final RecordDefinition metaData) {
    final String typePath = metaData.getPath();
    final String schema = getDatabaseSchemaName(PathUtil.getPath(typePath));
    final String shortName = ShortNameProperty.getShortName(metaData);
    final String sequenceName;
    if (StringUtils.hasText(shortName)) {
      if (this.useSchemaSequencePrefix) {
        sequenceName = schema + "." + shortName.toLowerCase() + "_seq";
      } else {
        sequenceName = shortName.toLowerCase() + "_seq";
      }
    } else {
      final String tableName = getDatabaseTableName(typePath);
      final String idAttributeName = metaData.getIdAttributeName()
          .toLowerCase();
      if (this.useSchemaSequencePrefix) {
        sequenceName = schema + "." + tableName + "_" + idAttributeName
            + "_seq";
      } else {
        sequenceName = tableName + "_" + idAttributeName + "_seq";
      }
    }
    return sequenceName;

  }

  @Override
  @PostConstruct
  public void initialize() {
    super.initialize();
    final JdbcAttributeAdder numberAttributeAdder = new JdbcAttributeAdder(
      DataTypes.DECIMAL);
    addAttributeAdder("numeric", numberAttributeAdder);

    final JdbcAttributeAdder stringAttributeAdder = new JdbcAttributeAdder(
      DataTypes.STRING);
    addAttributeAdder("varchar", stringAttributeAdder);
    addAttributeAdder("text", stringAttributeAdder);
    addAttributeAdder("name", stringAttributeAdder);
    addAttributeAdder("bpchar", stringAttributeAdder);

    final JdbcAttributeAdder longAttributeAdder = new JdbcAttributeAdder(
      DataTypes.LONG);
    addAttributeAdder("int8", longAttributeAdder);
    addAttributeAdder("bigint", longAttributeAdder);
    addAttributeAdder("bigserial", longAttributeAdder);
    addAttributeAdder("serial8", longAttributeAdder);

    final JdbcAttributeAdder intAttributeAdder = new JdbcAttributeAdder(
      DataTypes.INT);
    addAttributeAdder("int4", intAttributeAdder);
    addAttributeAdder("integer", intAttributeAdder);
    addAttributeAdder("serial", intAttributeAdder);
    addAttributeAdder("serial4", intAttributeAdder);

    final JdbcAttributeAdder shortAttributeAdder = new JdbcAttributeAdder(
      DataTypes.SHORT);
    addAttributeAdder("int2", shortAttributeAdder);
    addAttributeAdder("smallint", shortAttributeAdder);

    final JdbcAttributeAdder floatAttributeAdder = new JdbcAttributeAdder(
      DataTypes.FLOAT);
    addAttributeAdder("float4", floatAttributeAdder);

    final JdbcAttributeAdder doubleAttributeAdder = new JdbcAttributeAdder(
      DataTypes.DOUBLE);
    addAttributeAdder("float8", doubleAttributeAdder);
    addAttributeAdder("double precision", doubleAttributeAdder);

    addAttributeAdder("date", new JdbcAttributeAdder(DataTypes.DATE_TIME));

    addAttributeAdder("bool", new JdbcAttributeAdder(DataTypes.BOOLEAN));

    final JdbcAttributeAdder geometryAttributeAdder = new PostgreSQLGeometryAttributeAdder(
      this, getDataSource());
    addAttributeAdder("geometry", geometryAttributeAdder);
    setPrimaryKeySql("SELECT t.relname \"TABLE_NAME\", c.attname \"COLUMN_NAME\"" //
      + " FROM pg_namespace s" //
      + " join pg_class t on t.relnamespace = s.oid" //
      + " join pg_index i on i.indrelid = t.oid " //
      + " join pg_attribute c on c.attrelid = t.oid" //
      + " WHERE s.nspname = ? AND c.attnum = any(i.indkey) AND i.indisprimary");
    setSchemaPermissionsSql("select distinct t.table_schema as \"SCHEMA_NAME\" "
        + "from information_schema.role_table_grants t  "
        + "where (t.grantee  in (current_user, 'PUBLIC') or "
        + "t.grantee in (select role_name from information_schema.applicable_roles r where r.grantee = current_user)) and "
        + "privilege_type IN ('SELECT', 'INSERT','UPDATE','DELETE') ");
    setTablePermissionsSql("select distinct t.table_schema as \"SCHEMA_NAME\", t.table_name, t.privilege_type as \"PRIVILEGE\", d.description as \"REMARKS\" from information_schema.role_table_grants t join pg_namespace n on t.table_schema = n.nspname join pg_class c on (n.oid = c.relnamespace AND t.table_name = c.relname) left join pg_description d on d.objoid = c.oid "
        + "where t.table_schema = ? and "
        + "(t.grantee  in (current_user, 'PUBLIC') or t.grantee in (select role_name from information_schema.applicable_roles r where r.grantee = current_user)) AND "
        + "privilege_type IN ('SELECT', 'INSERT','UPDATE','DELETE') "
        + "order by t.table_schema, t.table_name, t.privilege_type");
  }

  protected void initSettings() {
    setIteratorFactory(new DataStoreIteratorFactory(
      PostgreSQLDataObjectStore.class, "createPostgreSQLIterator"));
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
