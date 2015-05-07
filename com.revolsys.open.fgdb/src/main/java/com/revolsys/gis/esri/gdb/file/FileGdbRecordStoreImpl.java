package com.revolsys.gis.esri.gdb.file;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.revolsys.collection.AbstractIterator;
import com.revolsys.collection.map.Maps;
import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordState;
import com.revolsys.data.record.schema.AbstractRecordStore;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.record.schema.RecordDefinitionImpl;
import com.revolsys.data.record.schema.RecordStoreSchema;
import com.revolsys.data.types.DataType;
import com.revolsys.format.esri.gdb.xml.EsriGeodatabaseXmlConstants;
import com.revolsys.format.esri.gdb.xml.model.CodedValueDomain;
import com.revolsys.format.esri.gdb.xml.model.DEFeatureClass;
import com.revolsys.format.esri.gdb.xml.model.DEFeatureDataset;
import com.revolsys.format.esri.gdb.xml.model.DETable;
import com.revolsys.format.esri.gdb.xml.model.Domain;
import com.revolsys.format.esri.gdb.xml.model.EsriGdbXmlParser;
import com.revolsys.format.esri.gdb.xml.model.EsriGdbXmlSerializer;
import com.revolsys.format.esri.gdb.xml.model.EsriXmlRecordDefinitionUtil;
import com.revolsys.format.esri.gdb.xml.model.Field;
import com.revolsys.format.esri.gdb.xml.model.Index;
import com.revolsys.format.esri.gdb.xml.model.SpatialReference;
import com.revolsys.format.esri.gdb.xml.model.enums.FieldType;
import com.revolsys.gis.cs.BoundingBox;
import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.gis.data.model.codes.CodeTable;
import com.revolsys.gis.data.query.AbstractMultiCondition;
import com.revolsys.gis.data.query.BinaryCondition;
import com.revolsys.gis.data.query.CollectionValue;
import com.revolsys.gis.data.query.Column;
import com.revolsys.gis.data.query.Condition;
import com.revolsys.gis.data.query.ILike;
import com.revolsys.gis.data.query.LeftUnaryCondition;
import com.revolsys.gis.data.query.Like;
import com.revolsys.gis.data.query.Query;
import com.revolsys.gis.data.query.QueryValue;
import com.revolsys.gis.data.query.RightUnaryCondition;
import com.revolsys.gis.data.query.SqlCondition;
import com.revolsys.gis.data.query.Value;
import com.revolsys.gis.esri.gdb.file.capi.FileGdbDomainCodeTable;
import com.revolsys.gis.esri.gdb.file.capi.swig.EnumRows;
import com.revolsys.gis.esri.gdb.file.capi.swig.Envelope;
import com.revolsys.gis.esri.gdb.file.capi.swig.EsriFileGdb;
import com.revolsys.gis.esri.gdb.file.capi.swig.Geodatabase;
import com.revolsys.gis.esri.gdb.file.capi.swig.Row;
import com.revolsys.gis.esri.gdb.file.capi.swig.Table;
import com.revolsys.gis.esri.gdb.file.capi.swig.VectorOfWString;
import com.revolsys.gis.esri.gdb.file.capi.type.AbstractFileGdbFieldDefinition;
import com.revolsys.gis.esri.gdb.file.capi.type.BinaryAttribute;
import com.revolsys.gis.esri.gdb.file.capi.type.DateAttribute;
import com.revolsys.gis.esri.gdb.file.capi.type.DoubleAttribute;
import com.revolsys.gis.esri.gdb.file.capi.type.FloatAttribute;
import com.revolsys.gis.esri.gdb.file.capi.type.GeometryAttribute;
import com.revolsys.gis.esri.gdb.file.capi.type.GlobalIdAttribute;
import com.revolsys.gis.esri.gdb.file.capi.type.GuidAttribute;
import com.revolsys.gis.esri.gdb.file.capi.type.IntegerAttribute;
import com.revolsys.gis.esri.gdb.file.capi.type.OidFieldDefinition;
import com.revolsys.gis.esri.gdb.file.capi.type.ShortAttribute;
import com.revolsys.gis.esri.gdb.file.capi.type.StringAttribute;
import com.revolsys.gis.esri.gdb.file.capi.type.XmlAttribute;
import com.revolsys.io.FileUtil;
import com.revolsys.io.PathUtil;
import com.revolsys.io.Writer;
import com.revolsys.io.xml.XmlProcessor;
import com.revolsys.jdbc.JdbcUtils;
import com.revolsys.util.CollectionUtil;
import com.revolsys.util.DateUtil;
import com.revolsys.util.ExceptionUtil;
import com.revolsys.util.JavaBeanUtil;
import com.vividsolutions.jts.geom.Geometry;

public class FileGdbRecordStoreImpl extends AbstractRecordStore implements
FileGdbRecordStore {
  static final Object API_SYNC = new Object();

  private static final String CATALOG_PATH_PROPERTY = FileGdbRecordStoreImpl.class
      + ".CatalogPath";

  private static final Map<FieldType, Constructor<? extends AbstractFileGdbFieldDefinition>> ESRI_FIELD_TYPE_ATTRIBUTE_MAP = new HashMap<FieldType, Constructor<? extends AbstractFileGdbFieldDefinition>>();

  private static final Logger LOG = LoggerFactory.getLogger(FileGdbRecordStoreImpl.class);

  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\?");

  static {
    addFieldTypeAttributeConstructor(FieldType.esriFieldTypeInteger,
      IntegerAttribute.class);
    addFieldTypeAttributeConstructor(FieldType.esriFieldTypeSmallInteger,
      ShortAttribute.class);
    addFieldTypeAttributeConstructor(FieldType.esriFieldTypeDouble,
      DoubleAttribute.class);
    addFieldTypeAttributeConstructor(FieldType.esriFieldTypeSingle,
      FloatAttribute.class);
    addFieldTypeAttributeConstructor(FieldType.esriFieldTypeString,
      StringAttribute.class);
    addFieldTypeAttributeConstructor(FieldType.esriFieldTypeDate,
      DateAttribute.class);
    addFieldTypeAttributeConstructor(FieldType.esriFieldTypeGeometry,
      GeometryAttribute.class);
    addFieldTypeAttributeConstructor(FieldType.esriFieldTypeOID,
      OidFieldDefinition.class);
    addFieldTypeAttributeConstructor(FieldType.esriFieldTypeBlob,
      BinaryAttribute.class);
    addFieldTypeAttributeConstructor(FieldType.esriFieldTypeGlobalID,
      GlobalIdAttribute.class);
    addFieldTypeAttributeConstructor(FieldType.esriFieldTypeGUID,
      GuidAttribute.class);
    addFieldTypeAttributeConstructor(FieldType.esriFieldTypeXML,
      XmlAttribute.class);

  }

  private static void addFieldTypeAttributeConstructor(
    final FieldType fieldType,
    final Class<? extends AbstractFileGdbFieldDefinition> fieldClass) {
    try {
      final Constructor<? extends AbstractFileGdbFieldDefinition> constructor = fieldClass.getConstructor(Field.class);
      ESRI_FIELD_TYPE_ATTRIBUTE_MAP.put(fieldType, constructor);
    } catch (final SecurityException e) {
      LOG.error("No public constructor for ESRI type " + fieldType, e);
    } catch (final NoSuchMethodException e) {
      LOG.error("No public constructor for ESRI type " + fieldType, e);
    }

  }

  public static SpatialReference getSpatialReference(
    final GeometryFactory geometryFactory) {
    if (geometryFactory == null || geometryFactory.getSRID() == 0) {
      return null;
    } else {
      final String wkt;
      synchronized (API_SYNC) {
        wkt = EsriFileGdb.getSpatialReferenceWkt(geometryFactory.getSRID());
      }
      final SpatialReference spatialReference = SpatialReference.get(
        geometryFactory, wkt);
      return spatialReference;
    }
  }

  private final Object apiSync = new Object();

  private boolean closed = false;

  private boolean createMissingRecordStore = true;

  private boolean createMissingTables = true;

  private String defaultSchema = "/";

  private Map<String, List<String>> domainFieldNames = new HashMap<String, List<String>>();

  private final Set<EnumRows> enumRowsToClose = new HashSet<>();

  private boolean exists = false;

  private String fileName;

  private Geodatabase geodatabase;

  private int geodatabaseReferenceCount;

  private final Map<String, AtomicLong> idGenerators = new HashMap<String, AtomicLong>();

  private boolean initialized;

  private final Map<String, Integer> tableReferenceCountsByTypePath = new HashMap<>();

  private final Map<String, Table> tablesToClose = new HashMap<>();

  private final Map<String, Integer> writeLockCountsByTypePath = new HashMap<>();

  protected FileGdbRecordStoreImpl(final File file) {
    this.fileName = file.getAbsolutePath();
    setConnectionProperties(Collections.singletonMap("url",
      FileUtil.toUrl(file).toString()));
  }

  public void addChildSchema(final String path) {
    synchronized (this.apiSync) {
      synchronized (API_SYNC) {
        final Geodatabase geodatabase = getGeodatabase();
        if (geodatabase != null) {
          try {
            final VectorOfWString childDatasets = geodatabase.getChildDatasets(
              path, "Feature Dataset");
            for (int i = 0; i < childDatasets.size(); i++) {
              final String childPath = childDatasets.get(i);
              addFeatureDatasetSchema(childPath);
            }
          } finally {
            releaseGeodatabase();
          }
        }
      }
    }
  }

  @Override
  public void addCodeTable(final CodeTable codeTable) {
    super.addCodeTable(codeTable);
    synchronized (this.apiSync) {
      synchronized (API_SYNC) {
        if (codeTable instanceof Domain) {
          final Domain domain = (Domain)codeTable;
          createDomain(domain);
        }
      }
    }
  }

  private RecordStoreSchema addFeatureDatasetSchema(final String path) {
    final String schemaName = path.replaceAll("\\\\", "/");
    final RecordStoreSchema schema = new RecordStoreSchema(this, schemaName);
    schema.setProperty(CATALOG_PATH_PROPERTY, path);
    addSchema(schema);
    addChildSchema(path);
    return schema;
  }

  private void addTableRecordDefinition(final String schemaName,
    final String path) {
    synchronized (this.apiSync) {
      synchronized (API_SYNC) {
        final Geodatabase geodatabase = getGeodatabase();
        if (geodatabase != null) {
          try {
            final String tableDefinition = geodatabase.getTableDefinition(path);
            final RecordDefinition recordDefinition = getRecordDefinition(
              schemaName, path, tableDefinition);
            addRecordDefinition(recordDefinition);
          } finally {
            releaseGeodatabase();
          }
        }
      }
    }
  }

  public void alterDomain(final CodedValueDomain domain) {
    final String domainDefinition = EsriGdbXmlSerializer.toString(domain);
    synchronized (this.apiSync) {
      synchronized (API_SYNC) {
        final Geodatabase geodatabase = getGeodatabase();
        if (geodatabase != null) {
          try {
            geodatabase.alterDomain(domainDefinition);
          } finally {
            releaseGeodatabase();
          }
        }
      }
    }
  }

  private void appendQueryValue(final StringBuffer buffer,
    final QueryValue condition) {
    if (condition instanceof Like || condition instanceof ILike) {
      final BinaryCondition like = (BinaryCondition)condition;
      final QueryValue left = like.getLeft();
      final QueryValue right = like.getRight();
      buffer.append("UPPER(CAST(");
      appendQueryValue(buffer, left);
      buffer.append(" AS VARCHAR(4000))) LIKE ");
      if (right instanceof Value) {
        final Value valueCondition = (Value)right;
        final Object value = valueCondition.getValue();
        buffer.append("'");
        if (value != null) {
          final String string = StringConverterRegistry.toString(value);
          buffer.append(string.toUpperCase());
        }
        buffer.append("'");
      } else {
        appendQueryValue(buffer, right);
      }
    } else if (condition instanceof LeftUnaryCondition) {
      final LeftUnaryCondition unaryCondition = (LeftUnaryCondition)condition;
      final String operator = unaryCondition.getOperator();
      final QueryValue right = unaryCondition.getQueryValue();
      buffer.append(operator);
      buffer.append(" ");
      appendQueryValue(buffer, right);
    } else if (condition instanceof RightUnaryCondition) {
      final RightUnaryCondition unaryCondition = (RightUnaryCondition)condition;
      final QueryValue left = unaryCondition.getValue();
      final String operator = unaryCondition.getOperator();
      appendQueryValue(buffer, left);
      buffer.append(" ");
      buffer.append(operator);
    } else if (condition instanceof BinaryCondition) {
      final BinaryCondition binaryCondition = (BinaryCondition)condition;
      final QueryValue left = binaryCondition.getLeft();
      final String operator = binaryCondition.getOperator();
      final QueryValue right = binaryCondition.getRight();
      appendQueryValue(buffer, left);
      buffer.append(" ");
      buffer.append(operator);
      buffer.append(" ");
      appendQueryValue(buffer, right);
    } else if (condition instanceof AbstractMultiCondition) {
      final AbstractMultiCondition multipleCondition = (AbstractMultiCondition)condition;
      buffer.append("(");
      boolean first = true;
      final String operator = multipleCondition.getOperator();
      for (final QueryValue subCondition : multipleCondition.getQueryValues()) {
        if (first) {
          first = false;
        } else {
          buffer.append(" ");
          buffer.append(operator);
          buffer.append(" ");
        }
        appendQueryValue(buffer, subCondition);
      }
      buffer.append(")");
    } else if (condition instanceof Value) {
      final Value valueCondition = (Value)condition;
      final Object value = valueCondition.getValue();
      appendValue(buffer, value);
    } else if (condition instanceof CollectionValue) {
      final CollectionValue collectionValue = (CollectionValue)condition;
      final List<Object> values = collectionValue.getValues();
      boolean first = true;
      for (final Object value : values) {
        if (first) {
          first = false;
        } else {
          buffer.append(", ");
        }
        appendValue(buffer, value);
      }
    } else if (condition instanceof Column) {
      final Column column = (Column)condition;
      final Object name = column.getName();
      buffer.append(name);
    } else if (condition instanceof SqlCondition) {
      final SqlCondition sqlCondition = (SqlCondition)condition;
      final String where = sqlCondition.getSql();
      final List<Object> parameters = sqlCondition.getParameterValues();
      if (parameters.isEmpty()) {
        if (where.indexOf('?') > -1) {
          throw new IllegalArgumentException(
            "No arguments specified for a where clause with placeholders: "
                + where);
        } else {
          buffer.append(where);
        }
      } else {
        final Matcher matcher = PLACEHOLDER_PATTERN.matcher(where);
        int i = 0;
        while (matcher.find()) {
          if (i >= parameters.size()) {
            throw new IllegalArgumentException(
              "Not enough arguments for where clause with placeholders: "
                  + where);
          }
          final Object argument = parameters.get(i);
          matcher.appendReplacement(buffer,
            StringConverterRegistry.toString(argument));
          appendValue(buffer, argument);
          i++;
        }
        matcher.appendTail(buffer);
      }

    } else {
      condition.appendSql(buffer);
    }
  }

  public void appendValue(final StringBuffer buffer, final Object value) {
    if (value == null) {
      buffer.append("''");
    } else if (value instanceof Number) {
      buffer.append(value);
    } else if (value instanceof java.util.Date) {
      final String stringValue = DateUtil.format("yyyy-MM-dd",
        (java.util.Date)value);
      buffer.append("DATE '" + stringValue + "'");
    } else {
      final String stringValue = StringConverterRegistry.toString(value);
      buffer.append("'");
      buffer.append(stringValue.replaceAll("'", "''"));
      buffer.append("'");
    }
  }

  @Override
  @PreDestroy
  public void close() {
    FileGdbRecordStoreFactory.release(this.fileName);
  }

  protected void closeEnumRows() {
    synchronized (this.apiSync) {
      for (final Iterator<EnumRows> iterator = this.enumRowsToClose.iterator(); iterator.hasNext();) {
        final EnumRows rows = iterator.next();
        try {
          rows.Close();
        } catch (final Throwable e) {
        } finally {
          rows.delete();
        }
        iterator.remove();
      }
      this.enumRowsToClose.clear();
    }
  }

  public void closeEnumRows(final EnumRows rows) {
    synchronized (this.apiSync) {
      if (isOpen(rows)) {
        try {
          rows.Close();
        } catch (final Throwable e) {
        } finally {
          try {
            rows.delete();
          } catch (final Throwable t) {
          }
        }
        this.enumRowsToClose.remove(rows);
      }
    }
  }

  private void closeGeodatabase(final Geodatabase geodatabase) {
    if (geodatabase != null) {
      synchronized (API_SYNC) {
        EsriFileGdb.CloseGeodatabase(geodatabase);
      }
    }
  }

  protected void closeRow(final Row row) {
    if (row != null) {
      synchronized (this.apiSync) {
        row.delete();
      }
    }
  }

  private void closeTables() {
    synchronized (this.apiSync) {
      if (!this.tablesToClose.isEmpty()) {
        final Geodatabase geodatabase = getGeodatabase();
        if (geodatabase != null) {
          try {
            for (final Entry<String, Table> entry : this.tablesToClose.entrySet()) {
              final Table table = entry.getValue();
              try {
                table.setLoadOnlyMode(false);
                table.freeWriteLock();
                geodatabase.closeTable(table);
              } catch (final Throwable e) {
              } finally {
                try {
                  table.delete();
                } catch (final Throwable t) {
                }
              }
            }
            this.tablesToClose.clear();
            this.tableReferenceCountsByTypePath.clear();
            this.writeLockCountsByTypePath.clear();
          } finally {
            releaseGeodatabase();
          }
        }
      }
    }
  }

  public synchronized void createDomain(final Domain domain) {
    synchronized (this.apiSync) {
      final Geodatabase geodatabase = getGeodatabase();
      if (geodatabase != null) {
        try {
          final String domainName = domain.getDomainName();
          if (!this.domainFieldNames.containsKey(domainName)) {
            synchronized (API_SYNC) {
              final String domainDef = EsriGdbXmlSerializer.toString(domain);
              try {
                geodatabase.createDomain(domainDef);
              } catch (final Exception e) {
                LOG.debug(domainDef);
                LOG.error("Unable to create domain", e);
              }
              loadDomain(geodatabase, domain.getDomainName());
            }
          }
        } finally {
          releaseGeodatabase();
        }
      }
    }
  }

  private Geodatabase createGeodatabase() {
    Geodatabase geodatabase;
    synchronized (API_SYNC) {
      geodatabase = EsriFileGdb.createGeodatabase(this.fileName);
    }
    return geodatabase;
  }

  @Override
  public AbstractIterator<Record> createIterator(final Query query,
    final Map<String, Object> properties) {
    String typePath = query.getTypeName();
    RecordDefinition recordDefinition = query.getRecordDefinition();
    if (recordDefinition == null) {
      typePath = query.getTypeName();
      recordDefinition = getRecordDefinition(typePath);
      if (recordDefinition == null) {
        throw new IllegalArgumentException("Type name does not exist "
            + typePath);
      }
    } else {
      typePath = recordDefinition.getPath();
    }
    final BoundingBox boundingBox = query.getBoundingBox();
    final Map<String, Boolean> orderBy = query.getOrderBy();
    final StringBuffer whereClause = getWhereClause(query);
    StringBuffer sql = new StringBuffer();
    if (orderBy.isEmpty() || boundingBox != null) {
      if (!orderBy.isEmpty()) {
        LoggerFactory.getLogger(getClass()).error(
          "Unable to sort on " + recordDefinition.getPath() + " "
              + orderBy.keySet()
              + " as the ESRI library can't sort with a bounding box query");
      }
      sql = whereClause;
    } else {
      sql.append("SELECT ");

      final List<String> fieldNames = query.getFieldNames();
      if (fieldNames.isEmpty()) {
        CollectionUtil.append(sql, recordDefinition.getFieldNames());
      } else {
        CollectionUtil.append(sql, fieldNames);
      }
      sql.append(" FROM ");
      sql.append(JdbcUtils.getTableName(typePath));
      if (whereClause.length() > 0) {
        sql.append(" WHERE ");
        sql.append(whereClause);
      }
      boolean first = true;
      for (final Entry<String, Boolean> entry : orderBy.entrySet()) {
        final String fieldName = entry.getKey();
        final DataType dataType = recordDefinition.getFieldType(fieldName);
        // TODO at the moment only numbers are supported
        if (dataType != null
            && Number.class.isAssignableFrom(dataType.getJavaClass())) {
          if (first) {
            sql.append(" ORDER BY ");
            first = false;
          } else {
            sql.append(", ");
          }
          sql.append(fieldName);
          final Boolean ascending = entry.getValue();
          if (!ascending) {
            sql.append(" DESC");
          }

        } else {
          LoggerFactory.getLogger(getClass()).error(
            "Unable to sort on " + recordDefinition.getPath() + "." + fieldName
            + " as the ESRI library can't sort on " + dataType + " fields");
        }
      }
    }

    final FileGdbQueryIterator iterator = new FileGdbQueryIterator(this,
      typePath, sql.toString(), boundingBox, query, query.getOffset(),
      query.getLimit());
    return iterator;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T createPrimaryIdValue(final String typePath) {
    synchronized (this.apiSync) {
      final RecordDefinition recordDefinition = getRecordDefinition(typePath);
      if (recordDefinition == null) {
        return null;
      } else {
        final String idAttributeName = recordDefinition.getIdFieldName();
        if (idAttributeName == null) {
          return null;
        } else if (!idAttributeName.equals("OBJECTID")) {
          AtomicLong idGenerator = this.idGenerators.get(typePath);
          if (idGenerator == null) {
            long maxId = 0;
            for (final Record record : query(typePath)) {
              final Object id = record.getIdValue();
              if (id instanceof Number) {
                final Number number = (Number)id;
                if (number.longValue() > maxId) {
                  maxId = number.longValue();
                }
              }
            }
            idGenerator = new AtomicLong(maxId);
            this.idGenerators.put(typePath, idGenerator);
          }
          return (T)(Object)idGenerator.incrementAndGet();
        } else {
          return null;
        }
      }
    }
  }

  protected Row createRowObject(final Table table) {
    synchronized (this.apiSync) {
      if (isOpen(table)) {
        return table.createRowObject();
      } else {
        return null;
      }
    }
  }

  private RecordStoreSchema createSchema(final DETable table) {
    synchronized (this.apiSync) {
      synchronized (API_SYNC) {
        final Geodatabase geodatabase = getGeodatabase();
        if (geodatabase == null) {
          return null;
        } else {
          try {
            final String catalogPath = table.getParentCatalogPath();
            final List<DEFeatureDataset> datasets = EsriXmlRecordDefinitionUtil.createDEFeatureDatasets(table);
            for (final DEFeatureDataset dataset : datasets) {
              final String path = dataset.getCatalogPath();
              final String datasetDefinition = EsriGdbXmlSerializer.toString(dataset);
              try {
                geodatabase.createFeatureDataset(datasetDefinition);
                addFeatureDatasetSchema(path);
              } catch (final Throwable t) {
                if (LOG.isDebugEnabled()) {
                  LOG.debug(datasetDefinition);
                }
                throw new RuntimeException("Unable to create feature dataset "
                    + path, t);
              }
            }
            return getSchema(catalogPath.replaceAll("\\\\", "/"));
          } finally {
            releaseGeodatabase();
          }
        }
      }
    }
  }

  public void createSchema(final String schemaName,
    final GeometryFactory geometryFactory) {
    synchronized (this.apiSync) {
      synchronized (API_SYNC) {
        final Geodatabase geodatabase = getGeodatabase();
        if (geodatabase != null) {
          try {
            final SpatialReference spatialReference = getSpatialReference(geometryFactory);
            final List<DEFeatureDataset> datasets = EsriXmlRecordDefinitionUtil.createDEFeatureDatasets(
              schemaName.replaceAll("/", ""), spatialReference);
            for (final DEFeatureDataset dataset : datasets) {
              final String path = dataset.getCatalogPath();
              final String datasetDefinition = EsriGdbXmlSerializer.toString(dataset);
              try {
                geodatabase.createFeatureDataset(datasetDefinition);
                addFeatureDatasetSchema(path);
              } catch (final Throwable t) {
                if (LOG.isDebugEnabled()) {
                  LOG.debug(datasetDefinition);
                }
                throw new RuntimeException("Unable to create feature dataset "
                    + path, t);
              }
            }
          } finally {
            releaseGeodatabase();
          }
        }
      }
    }
  }

  protected RecordDefinitionImpl createTable(final DETable deTable) {
    synchronized (this.apiSync) {
      synchronized (API_SYNC) {
        final Geodatabase geodatabase = getGeodatabase();
        if (geodatabase == null) {
          return null;
        } else {
          try {
            String schemaPath = deTable.getParentCatalogPath();
            String schemaName = schemaPath.replaceAll("\\\\", "/");
            RecordStoreSchema schema = getSchema(schemaName);
            if (schema == null) {
              if (schemaName.length() > 1 && deTable instanceof DEFeatureClass) {
                schema = createSchema(deTable);
              } else {
                schema = new RecordStoreSchema(this, schemaName);
                addSchema(schema);
              }
            } else if (schema.getProperty(CATALOG_PATH_PROPERTY) == null) {
              if (schemaName.length() > 1 && deTable instanceof DEFeatureClass) {
                createSchema(deTable);
              }
            }
            if (schemaName.equals(this.defaultSchema)) {
              if (!(deTable instanceof DEFeatureClass)) {
                schemaPath = "\\";
                // @TODO clone
                deTable.setCatalogPath("\\" + deTable.getName());

              }
            } else if (schemaName.equals("")) {
              schemaName = this.defaultSchema;
            }
            for (final Field field : deTable.getFields()) {
              final String fieldName = field.getName();
              final CodeTable codeTable = getCodeTableByFieldName(fieldName);
              if (codeTable instanceof FileGdbDomainCodeTable) {
                final FileGdbDomainCodeTable domainCodeTable = (FileGdbDomainCodeTable)codeTable;
                field.setDomain(domainCodeTable.getDomain());
              }
            }
            final String tableDefinition = EsriGdbXmlSerializer.toString(deTable);
            try {
              final Table table = geodatabase.createTable(tableDefinition,
                schemaPath);
              geodatabase.closeTable(table);
              table.delete();
              final RecordDefinitionImpl recordDefinition = getRecordDefinition(
                schemaName, schemaPath, tableDefinition);
              addRecordDefinition(recordDefinition);
              return recordDefinition;

            } catch (final Throwable t) {
              if (LOG.isDebugEnabled()) {
                LOG.debug(tableDefinition);
              }
              throw new RuntimeException("Unable to create table "
                  + deTable.getCatalogPath(), t);
            }
          } finally {
            releaseGeodatabase();
          }
        }
      }
    }
  }

  private RecordDefinition createTable(final RecordDefinition recordDefinition) {
    synchronized (this.apiSync) {
      synchronized (API_SYNC) {
        final GeometryFactory geometryFactory = recordDefinition.getGeometryFactory();
        final SpatialReference spatialReference = getSpatialReference(geometryFactory);

        final DETable deTable = EsriXmlRecordDefinitionUtil.getDETable(
          recordDefinition, spatialReference);
        final RecordDefinitionImpl tableRecordDefinition = createTable(deTable);
        final String idFieldName = recordDefinition.getIdFieldName();
        if (idFieldName != null) {
          tableRecordDefinition.setIdFieldName(idFieldName);
        }
        return tableRecordDefinition;
      }
    }
  }

  @Override
  public Writer<Record> createWriter() {
    return new FileGdbWriter(this);
  }

  @Override
  public void delete(final Record record) {
    // Don't synchronize to avoid deadlock as that is done lower down in the
    // methods
    if (record.getState() == RecordState.Persisted
        || record.getState() == RecordState.Modified) {
      record.setState(RecordState.Deleted);
      final Writer<Record> writer = getWriter();
      writer.write(record);
    }
  }

  @Override
  public void deleteGeodatabase() {
    synchronized (this.apiSync) {
      final String fileName = this.fileName;
      try {
        doClose();
      } finally {
        if (new File(fileName).exists()) {
          synchronized (API_SYNC) {
            EsriFileGdb.DeleteGeodatabase(fileName);
          }
        }
      }
    }
  }

  protected void deleteRow(final String typePath, final Table table,
    final Row row) {
    synchronized (this.apiSync) {
      if (isOpen(table)) {
        final boolean loadOnly = this.writeLockCountsByTypePath.containsKey(typePath);
        if (loadOnly) {
          table.setLoadOnlyMode(false);
        }
        table.deleteRow(row);
        if (loadOnly) {
          table.setLoadOnlyMode(true);
        }
      }
    }
  }

  public void doClose() {
    this.exists = false;
    this.closed = true;
    synchronized (this.apiSync) {
      synchronized (API_SYNC) {
        try {
          final Geodatabase geodatabase = this.geodatabase;
          if (geodatabase != null) {
            closeEnumRows();
            closeTables();
            try {
              closeGeodatabase(geodatabase);
            } finally {
              this.geodatabase = null;
            }
          }
        } finally {
          super.close();
        }
      }
    }
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
  }

  protected void freeWriteLock(final String typePath, final Table table) {
    synchronized (this.apiSync) {
      if (isOpen(table)) {
        if (Maps.decrementCount(this.writeLockCountsByTypePath, typePath) == 0) {
          table.setLoadOnlyMode(false);
          table.freeWriteLock();
        }
      }
    }
  }

  public String getDefaultSchema() {
    return this.defaultSchema;
  }

  public Map<String, List<String>> getDomainColumNames() {
    return this.domainFieldNames;
  }

  public String getFileName() {
    return this.fileName;
  }

  private Geodatabase getGeodatabase() {
    synchronized (this.apiSync) {
      if (this.exists) {
        this.geodatabaseReferenceCount++;
        if (this.geodatabase == null) {
          this.geodatabase = openGeodatabase();
        }
        return this.geodatabase;
      } else {
        return null;
      }
    }
  }

  @Override
  public RecordDefinition getRecordDefinition(
    final RecordDefinition sourceRecordDefinition) {
    synchronized (this.apiSync) {
      RecordDefinition recordDefinition = super.getRecordDefinition(sourceRecordDefinition);
      if (this.createMissingTables && recordDefinition == null) {
        recordDefinition = createTable(sourceRecordDefinition);
      }
      return recordDefinition;
    }
  }

  public RecordDefinitionImpl getRecordDefinition(final String schemaName,
    final String path, final String tableDefinition) {
    synchronized (this.apiSync) {
      synchronized (API_SYNC) {
        try {
          final XmlProcessor parser = new EsriGdbXmlParser();
          final DETable deTable = parser.process(tableDefinition);
          final String tableName = deTable.getName();
          final String typePath = PathUtil.toPath(schemaName, tableName);
          final RecordStoreSchema schema = getSchema(schemaName);
          final RecordDefinitionImpl recordDefinition = new RecordDefinitionImpl(
            this, schema, typePath);
          for (final Field field : deTable.getFields()) {
            final String fieldName = field.getName();
            final FieldType type = field.getType();
            final Constructor<? extends AbstractFileGdbFieldDefinition> fieldConstructor = ESRI_FIELD_TYPE_ATTRIBUTE_MAP.get(type);
            if (fieldConstructor != null) {
              try {
                final AbstractFileGdbFieldDefinition fieldDefinition = JavaBeanUtil.invokeConstructor(
                  fieldConstructor, field);
                fieldDefinition.setDataStore(this);
                recordDefinition.addField(fieldDefinition);
                if (fieldDefinition instanceof GlobalIdAttribute) {
                  recordDefinition.setIdFieldName(fieldName);
                }
              } catch (final Throwable e) {
                LOG.error(tableDefinition);
                throw new RuntimeException("Error creating field for "
                    + typePath + "." + field.getName() + " : " + field.getType(),
                    e);
              }
            } else {
              LOG.error("Unsupported field type " + fieldName + ":" + type);
            }
          }
          final String oidFieldName = deTable.getOIDFieldName();
          recordDefinition.setProperty(
            EsriGeodatabaseXmlConstants.ESRI_OBJECT_ID_FIELD_NAME, oidFieldName);
          if (deTable instanceof DEFeatureClass) {
            final DEFeatureClass featureClass = (DEFeatureClass)deTable;
            final String shapeFieldName = featureClass.getShapeFieldName();
            recordDefinition.setGeometryFieldName(shapeFieldName);
          }
          recordDefinition.setProperty(CATALOG_PATH_PROPERTY, path);
          for (final Index index : deTable.getIndexes()) {
            if (index.getName().endsWith("_PK")) {
              for (final Field field : index.getFields()) {
                final String fieldName = field.getName();
                recordDefinition.setIdFieldName(fieldName);
              }
            }
          }
          addRecordDefinitionProperties(recordDefinition);
          if (recordDefinition.getIdFieldIndex() == -1) {
            recordDefinition.setIdFieldName(deTable.getOIDFieldName());
          }

          return recordDefinition;
        } catch (final RuntimeException e) {
          if (LOG.isDebugEnabled()) {
            LOG.debug(tableDefinition);
          }
          throw e;
        }
      }
    }
  }

  @Override
  public int getRowCount(final Query query) {
    if (query == null) {
      return 0;
    } else {
      synchronized (this.apiSync) {
        final Geodatabase geodatabase = getGeodatabase();
        if (geodatabase == null) {
          return 0;
        } else {
          try {
            String typePath = query.getTypeName();
            RecordDefinition recordDefinition = query.getRecordDefinition();
            if (recordDefinition == null) {
              typePath = query.getTypeName();
              recordDefinition = getRecordDefinition(typePath);
              if (recordDefinition == null) {
                return 0;
              }
            } else {
              typePath = recordDefinition.getPath();
            }
            final StringBuffer whereClause = getWhereClause(query);
            final BoundingBox boundingBox = query.getBoundingBox();

            if (boundingBox == null) {
              final StringBuffer sql = new StringBuffer();
              sql.append("SELECT OBJECTID FROM ");
              sql.append(JdbcUtils.getTableName(typePath));
              if (whereClause.length() > 0) {
                sql.append(" WHERE ");
                sql.append(whereClause);
              }

              final EnumRows rows = query(sql.toString(), false);
              if (rows == null) {
                return 0;
              } else {
                try {
                  int count = 0;
                  for (Row row = rows.next(); row != null; row = rows.next()) {
                    count++;
                    row.delete();
                  }
                  return count;
                } finally {
                  closeEnumRows(rows);
                }
              }
            } else {
              final GeometryAttribute geometryAttribute = (GeometryAttribute)recordDefinition.getGeometryField();
              if (geometryAttribute == null || boundingBox.isEmpty()) {
                return 0;
              } else {
                final StringBuffer sql = new StringBuffer();
                sql.append("SELECT " + geometryAttribute.getName() + " FROM ");
                sql.append(JdbcUtils.getTableName(typePath));
                if (whereClause.length() > 0) {
                  sql.append(" WHERE ");
                  sql.append(whereClause);
                }

                final EnumRows rows = query(sql.toString(), false);
                try {
                  int count = 0;
                  for (Row row = rows.next(); row != null; row = rows.next()) {
                    final Geometry geometry = (Geometry)geometryAttribute.getValue(row);
                    final BoundingBox geometryBoundingBox = BoundingBox.getBoundingBox(geometry);
                    if (geometryBoundingBox.intersects(boundingBox)) {
                      count++;
                    }
                    row.delete();
                  }
                  return count;
                } finally {
                  closeEnumRows(rows);
                }
              }
            }
          } finally {
            releaseGeodatabase();
          }
        }
      }
    }
  }

  protected Table getTable(final String typePath) {
    synchronized (this.apiSync) {
      synchronized (API_SYNC) {
        if (!isExists() || getRecordDefinition(typePath) == null) {
          return null;
        } else {
          final String path = typePath.replaceAll("/", "\\\\");
          try {
            final Geodatabase geodatabase = getGeodatabase();
            if (geodatabase == null) {
              return null;
            } else {
              try {
                Table table = this.tablesToClose.get(typePath);
                if (table == null) {
                  table = this.geodatabase.openTable(path);
                  if (table != null) {
                    if (this.tablesToClose.isEmpty()) {
                      this.geodatabaseReferenceCount++;
                    }
                    this.tablesToClose.put(typePath, table);
                  }
                }
                if (table != null) {
                  Maps.addCount(this.tableReferenceCountsByTypePath, typePath);
                }
                return table;
              } catch (final RuntimeException e) {
                throw new RuntimeException("Unable to open table " + typePath,
                  e);
              }
            }
          } finally {
            releaseGeodatabase();
          }
        }
      }
    }
  }

  protected Table getTableWithWriteLock(final String typePath) {
    synchronized (this.apiSync) {
      final Table table = getTable(typePath);
      if (table != null) {
        if (Maps.addCount(this.writeLockCountsByTypePath, typePath) == 1) {
          table.setWriteLock();
          table.setLoadOnlyMode(true);
        }
      }
      return table;
    }
  }

  protected StringBuffer getWhereClause(final Query query) {
    final StringBuffer whereClause = new StringBuffer();
    final Condition whereCondition = query.getWhereCondition();
    if (whereCondition != null) {
      appendQueryValue(whereClause, whereCondition);
    }
    return whereClause;
  }

  @Override
  public Writer<Record> getWriter() {
    synchronized (this.apiSync) {
      Writer<Record> writer = getSharedAttribute("writer");
      if (writer == null) {
        writer = createWriter();
        setSharedAttribute("writer", writer);
      }
      return writer;
    }
  }

  @Override
  @PostConstruct
  public void initialize() {
    synchronized (this.apiSync) {
      if (!this.initialized) {
        Geodatabase geodatabase = null;
        this.initialized = true;
        try {
          super.initialize();
          final File file = new File(this.fileName);
          if (file.exists()) {
            if (file.isDirectory()) {
              if (!new File(this.fileName, "gdb").exists()) {
                throw new IllegalArgumentException(
                  FileUtil.getCanonicalPath(file)
                    + " is not a valid ESRI File Geodatabase");
              }
              geodatabase = openGeodatabase();
            } else {
              throw new IllegalArgumentException(
                FileUtil.getCanonicalPath(file)
                  + " ESRI File Geodatabase must be a directory");
            }
          } else if (this.createMissingRecordStore) {
            geodatabase = createGeodatabase();
          } else {
            throw new IllegalArgumentException(
              "ESRI file geodatabase not found " + this.fileName);
          }
          final VectorOfWString domainNames = geodatabase.getDomains();
          for (int i = 0; i < domainNames.size(); i++) {
            final String domainName = domainNames.get(i);
            loadDomain(geodatabase, domainName);
          }
          this.exists = true;
        } catch (final Throwable e) {
          ExceptionUtil.throwUncheckedException(e);
        } finally {
          if (geodatabase != null) {
            closeGeodatabase(geodatabase);
          }
        }
      }
    }
  }

  @Override
  public void insert(final Record record) {
    // Don't synchronize to avoid deadlock as that is done lower down in the
    // methods
    getWriter().write(record);
  }

  protected void insertRow(final Table table, final Row row) {
    synchronized (this.apiSync) {
      if (isOpen(table)) {
        table.insertRow(row);
      }
    }
  }

  public boolean isClosed() {
    return this.closed;
  }

  public boolean isCreateMissingDataStore() {
    return this.createMissingRecordStore;
  }

  public boolean isCreateMissingTables() {
    return this.createMissingTables;
  }

  public boolean isExists() {
    return this.exists;
  }

  public boolean isNull(final Row row, final String name) {
    synchronized (this.apiSync) {
      return row.isNull(name);
    }
  }

  public boolean isOpen(final EnumRows enumRows) {
    synchronized (this.apiSync) {
      if (enumRows == null) {
        return false;
      } else {
        return this.enumRowsToClose.contains(enumRows);
      }
    }
  }

  public boolean isOpen(final Table table) {
    synchronized (this.apiSync) {
      if (table == null) {
        return false;
      } else {
        return this.tablesToClose.containsValue(table);
      }
    }
  }

  private boolean isPathExists(final Geodatabase geodatabase, String path) {
    if (path == null) {
      return false;
    } else if ("\\".equals(path)) {
      return true;
    } else {
      final boolean pathExists = true;

      path = path.replaceAll("[\\/]+", "\\");
      path = path.replaceAll("\\$", "");
      int index = 0;
      while (index != -1) {
        final String parentPath = path.substring(0, index + 1);
        final int nextIndex = path.indexOf(index + 1, '\\');
        String element;
        if (nextIndex == -1) {
          element = path.substring(index + 1);
        } else {
          element = path.substring(index + 1, nextIndex);
        }
        boolean found = false;
        final VectorOfWString children = geodatabase.getChildDatasets(
          parentPath, "Feature Dataset");
        for (int i = 0; i < children.size(); i++) {
          final String childPath = children.get(i);
          if (childPath.equals(element)) {
            found = true;
          }
        }
        if (!found) {
          return false;
        }
        index = nextIndex;
      }
      return pathExists;
    }
  }

  @Override
  public Record load(final String typePath, final Object... id) {
    synchronized (this.apiSync) {
      final RecordDefinition recordDefinition = getRecordDefinition(typePath);
      if (recordDefinition == null) {
        throw new IllegalArgumentException("Unknown type " + typePath);
      } else {
        final FileGdbQueryIterator iterator = new FileGdbQueryIterator(this,
          typePath, recordDefinition.getIdFieldName() + " = " + id[0]);
        try {
          if (iterator.hasNext()) {
            return iterator.next();
          } else {
            return null;
          }
        } finally {
          iterator.close();
        }
      }
    }
  }

  protected void loadDomain(final Geodatabase geodatabase,
    final String domainName) {
    final String domainDef = geodatabase.getDomainDefinition(domainName);
    final Domain domain = EsriGdbXmlParser.parse(domainDef);
    if (domain instanceof CodedValueDomain) {
      final CodedValueDomain codedValueDomain = (CodedValueDomain)domain;
      final FileGdbDomainCodeTable codeTable = new FileGdbDomainCodeTable(this,
        codedValueDomain);
      super.addCodeTable(codeTable);
      final List<String> fieldNames = this.domainFieldNames.get(domainName);
      if (fieldNames != null) {
        for (final String fieldName : fieldNames) {
          addCodeTable(fieldName, codeTable);
        }
      }
    }
  }

  @Override
  protected void loadSchemaDataObjectMetaData(final RecordStoreSchema schema,
    final Map<String, RecordDefinition> recordDefinitionMap) {
    synchronized (this.apiSync) {
      synchronized (API_SYNC) {
        final Geodatabase geodatabase = getGeodatabase();
        if (geodatabase != null) {
          try {
            final String schemaName = schema.getPath();
            if (schemaName.equals(this.defaultSchema)) {
              loadSchemaRecordDefinition(recordDefinitionMap, schemaName, "\\",
                  "Feature Class");
              loadSchemaRecordDefinition(recordDefinitionMap, schemaName, "\\",
                  "Table");
            }
            final String path = schemaName.replaceAll("/", "\\\\");
            loadSchemaRecordDefinition(recordDefinitionMap, schemaName, path,
                "Feature Class");
            loadSchemaRecordDefinition(recordDefinitionMap, schemaName, path,
                "Table");
          } finally {
            releaseGeodatabase();
          }
        }
      }
    }
  }

  public void loadSchemaRecordDefinition(
    final Map<String, RecordDefinition> recordDefinitionMap,
    final String schemaName, final String path, final String datasetType) {
    synchronized (this.apiSync) {
      synchronized (API_SYNC) {
        final Geodatabase geodatabase = getGeodatabase();
        if (geodatabase != null) {
          try {
            final boolean pathExists = isPathExists(geodatabase, path);
            if (pathExists) {
              final VectorOfWString childFeatureClasses = geodatabase.getChildDatasets(
                path, datasetType);
              for (int i = 0; i < childFeatureClasses.size(); i++) {
                final String childPath = childFeatureClasses.get(i);
                addTableRecordDefinition(schemaName, childPath);
              }
            }
          } finally {
            releaseGeodatabase();
          }
        }
      }
    }
  }

  @Override
  protected void loadSchemas(final Map<String, RecordStoreSchema> schemaMap) {
    synchronized (this.apiSync) {
      final Geodatabase geodatabase = getGeodatabase();
      if (geodatabase != null) {
        try {
          addSchema(new RecordStoreSchema(this, this.defaultSchema));
          addChildSchema("\\");
        } finally {
          releaseGeodatabase();
        }
      }
    }
  }

  protected Row nextRow(final EnumRows rows) {
    synchronized (this.apiSync) {
      if (isOpen(rows)) {
        return rows.next();
      } else {
        return null;
      }
    }
  }

  @Override
  protected void obtainConnected() {
    getGeodatabase();
  }

  private Geodatabase openGeodatabase() {
    synchronized (API_SYNC) {
      return EsriFileGdb.openGeodatabase(this.fileName);
    }
  }

  public EnumRows query(final String sql, final boolean recycling) {
    synchronized (this.apiSync) {
      final Geodatabase geodatabase = getGeodatabase();
      if (geodatabase == null) {
        return null;
      } else {
        try {
          final EnumRows enumRows = geodatabase.query(sql, recycling);
          this.enumRowsToClose.add(enumRows);
          return enumRows;
        } catch (final Throwable t) {
          throw new RuntimeException("Error running sql: " + sql, t);
        } finally {
          releaseGeodatabase();
        }
      }
    }
  }

  @Override
  protected void releaseConnected() {
    releaseGeodatabase();
  }

  private void releaseGeodatabase() {
    synchronized (this.apiSync) {
      if (this.geodatabase != null) {
        this.geodatabaseReferenceCount--;
        if (this.geodatabaseReferenceCount <= 0) {
          this.geodatabaseReferenceCount = 0;
          try {
            closeGeodatabase(this.geodatabase);
          } finally {
            this.geodatabase = null;
          }
        }
      }
    }
  }

  protected void releaseTable(final String typePath) {
    synchronized (this.apiSync) {
      final Geodatabase geodatabase = getGeodatabase();
      if (geodatabase != null) {
        try {
          final Table table = this.tablesToClose.get(typePath);
          if (table != null) {
            if (Maps.decrementCount(this.tableReferenceCountsByTypePath,
              typePath) == 0) {
              try {
                this.tablesToClose.remove(typePath);
                this.writeLockCountsByTypePath.remove(typePath);
                geodatabase.closeTable(table);
              } catch (final Exception e) {
                LoggerFactory.getLogger(getClass()).error(
                  "Unable to close table: " + typePath, e);
              } finally {
                if (this.tablesToClose.isEmpty()) {
                  this.geodatabaseReferenceCount--;
                }
                table.delete();
              }
            }
          }
        } finally {
          releaseGeodatabase();
        }
      }
    }
  }

  protected void releaseTableAndWriteLock(final String typePath) {
    synchronized (this.apiSync) {
      final Geodatabase geodatabase = getGeodatabase();
      if (geodatabase != null) {
        try {
          final Table table = this.tablesToClose.get(typePath);
          if (table != null) {
            if (Maps.decrementCount(this.writeLockCountsByTypePath, typePath) == 0) {
              try {
                table.setLoadOnlyMode(false);
                table.freeWriteLock();
              } catch (final Exception e) {
                LoggerFactory.getLogger(getClass()).error(
                  "Unable to free write lock for table: " + typePath, e);
              }
            }
          }
          releaseTable(typePath);
        } finally {
          releaseGeodatabase();
        }
      }
    }
  }

  public EnumRows search(final String typePath, final Table table,
    final String fields, final String whereClause, final boolean recycling) {
    synchronized (this.apiSync) {
      if (isOpen(table)) {
        try {
          final EnumRows rows = table.search(fields, whereClause, recycling);
          this.enumRowsToClose.add(rows);
          return rows;
        } catch (final Throwable t) {
          LoggerFactory.getLogger(getClass()).error(
            "Unable to execute query SELECT " + fields + " FROM " + typePath
            + " WHERE " + whereClause, t);

        }
      }
      return null;
    }
  }

  public EnumRows search(final String typePath, final Table table,
    final String fields, final String whereClause, final Envelope boundingBox,
    final boolean recycling) {
    synchronized (this.apiSync) {
      if (isOpen(table)) {
        try {
          final EnumRows rows = table.search(fields, whereClause, boundingBox,
            recycling);
          this.enumRowsToClose.add(rows);
          return rows;
        } catch (final Exception e) {
          LOG.error("ERROR executing query SELECT " + fields + " FROM "
              + typePath + " WHERE " + whereClause + " AND " + boundingBox, e);
        }
      }
      return null;
    }
  }

  @Override
  public void setCreateMissingRecordStore(final boolean createMissingRecordStore) {
    this.createMissingRecordStore = createMissingRecordStore;
  }

  @Override
  public void setCreateMissingTables(final boolean createMissingTables) {
    this.createMissingTables = createMissingTables;
  }

  @Override
  public void setDefaultSchema(final String defaultSchema) {
    synchronized (this.apiSync) {
      if (StringUtils.hasText(defaultSchema)) {
        if (!defaultSchema.startsWith("/")) {
          this.defaultSchema = "/" + defaultSchema;
        } else {
          this.defaultSchema = defaultSchema;
        }
      } else {
        this.defaultSchema = "/";
      }
      refreshSchema();
    }
  }

  public void setDomainColumNames(
    final Map<String, List<String>> domainColumNames) {
    this.domainFieldNames = domainColumNames;
  }

  public void setFileName(final String fileName) {
    this.fileName = fileName;
  }

  public void setNull(final Row row, final String name) {
    synchronized (this.apiSync) {
      row.setNull(name);
    }
  }

  @Override
  public String toString() {
    return this.fileName;
  }

  @Override
  public void update(final Record record) {
    // Don't synchronize to avoid deadlock as that is done lower down in the
    // methods
    getWriter().write(record);
  }

  protected void updateRow(final String typePath, final Table table,
    final Row row) {
    synchronized (this.apiSync) {
      if (isOpen(table)) {
        final boolean loadOnly = this.writeLockCountsByTypePath.containsKey(typePath);
        if (loadOnly) {
          table.setLoadOnlyMode(false);
        }
        table.updateRow(row);
        if (loadOnly) {
          table.setLoadOnlyMode(true);
        }
      }
    }
  }
}
