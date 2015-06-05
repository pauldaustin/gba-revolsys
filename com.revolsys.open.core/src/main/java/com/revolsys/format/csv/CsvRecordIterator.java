package com.revolsys.format.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.core.io.Resource;

import com.revolsys.collection.iterator.AbstractIterator;
import com.revolsys.collection.map.Maps;
import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.data.record.ArrayRecordFactory;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordFactory;
import com.revolsys.data.record.io.RecordIterator;
import com.revolsys.data.record.property.FieldProperties;
import com.revolsys.data.record.schema.FieldDefinition;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.record.schema.RecordDefinitionImpl;
import com.revolsys.data.record.schema.RecordStoreSchema;
import com.revolsys.data.types.DataType;
import com.revolsys.data.types.DataTypes;
import com.revolsys.gis.cs.esri.EsriCoordinateSystems;
import com.revolsys.io.FileUtil;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.util.ExceptionUtil;
import com.revolsys.util.Property;
import com.vividsolutions.jts.geom.Geometry;

public class CsvRecordIterator extends AbstractIterator<Record> implements RecordIterator {

  private final char fieldSeparator;

  private String pointXFieldName;

  private String pointYFieldName;

  private String geometryColumnName;

  private DataType geometryType = DataTypes.GEOMETRY;

  private GeometryFactory geometryFactory = GeometryFactory.floating3();

  private RecordFactory recordFactory;

  private BufferedReader in;

  private RecordDefinition recordDefinition;

  private Resource resource;

  private boolean hasPointFields;

  public CsvRecordIterator(final Resource resource) {
    this(resource, new ArrayRecordFactory(), CsvConstants.FIELD_SEPARATOR);
  }

  public CsvRecordIterator(final Resource resource, final char fieldSeparator) {
    this(resource, new ArrayRecordFactory(), fieldSeparator);
  }

  public CsvRecordIterator(final Resource resource, final RecordFactory recordFactory) {
    this(resource, recordFactory, CsvConstants.FIELD_SEPARATOR);
  }

  public CsvRecordIterator(final Resource resource, final RecordFactory recordFactory,
    final char fieldSeparator) {
    this.resource = resource;
    this.recordFactory = recordFactory;
    this.fieldSeparator = fieldSeparator;
  }

  private void createRecordDefinition(final String[] fieldNames) throws IOException {
    hasPointFields = Property.hasValue(pointXFieldName) && Property.hasValue(pointYFieldName);
    if (hasPointFields) {
      geometryType = DataTypes.POINT;
    } else {
      pointXFieldName = null;
      pointYFieldName = null;
    }
    final List<FieldDefinition> fields = new ArrayList<>();
    FieldDefinition geometryField = null;
    for (final String fieldName : fieldNames) {
      DataType type = DataTypes.STRING;
      boolean isGeometryField = false;
      if (fieldName != null) {
        if (fieldName.equalsIgnoreCase(geometryColumnName)) {
          type = geometryType;
          isGeometryField = true;
        } else if ("GEOMETRY".equalsIgnoreCase(fieldName)) {
          type = DataTypes.GEOMETRY;
          isGeometryField = true;
        } else if ("GEOMETRYCOLLECTION".equalsIgnoreCase(fieldName)
          || "GEOMETRY_COLLECTION".equalsIgnoreCase(fieldName)) {
          type = DataTypes.GEOMETRY_COLLECTION;
          isGeometryField = true;
        } else if ("POINT".equalsIgnoreCase(fieldName)) {
          type = DataTypes.POINT;
          isGeometryField = true;
        } else if ("MULTI_POINT".equalsIgnoreCase(fieldName)
          || "MULTIPOINT".equalsIgnoreCase(fieldName)) {
          type = DataTypes.MULTI_POINT;
          isGeometryField = true;
        } else if ("LINE_STRING".equalsIgnoreCase(fieldName)
          || "LINESTRING".equalsIgnoreCase(fieldName) || "LINE".equalsIgnoreCase(fieldName)) {
          type = DataTypes.LINE_STRING;
          isGeometryField = true;
        } else if ("MULTI_LINESTRING".equalsIgnoreCase(fieldName)
          || "MULTILINESTRING".equalsIgnoreCase(fieldName)
          || "MULTILINE".equalsIgnoreCase(fieldName) || "MULTI_LINE".equalsIgnoreCase(fieldName)) {
          type = DataTypes.MULTI_LINE_STRING;
          isGeometryField = true;
        } else if ("POLYGON".equalsIgnoreCase(fieldName)) {
          type = DataTypes.POLYGON;
          isGeometryField = true;
        } else if ("MULTI_POLYGON".equalsIgnoreCase(fieldName)
          || "MULTIPOLYGON".equalsIgnoreCase(fieldName)) {
          type = DataTypes.MULTI_POLYGON;
          isGeometryField = true;
        }
      }
      final FieldDefinition field = new FieldDefinition(fieldName, type, false);
      if (isGeometryField) {
        geometryField = field;
      }
      fields.add(field);
    }
    if (hasPointFields) {
      if (geometryField == null) {
        geometryField = new FieldDefinition(geometryColumnName, geometryType, true);
        fields.add(geometryField);
      }
    }
    if (geometryField != null) {
      geometryField.setProperty(FieldProperties.GEOMETRY_FACTORY, geometryFactory);
    }
    final RecordStoreSchema schema = getProperty("schema");
    String typePath = getProperty("typePath");
    if (!Property.hasValue(typePath)) {
      typePath = "/" + FileUtil.getBaseName(resource.getFilename());
      String schemaPath = getProperty("schemaPath");
      if (Property.hasValue(schemaPath)) {
        if (!schemaPath.startsWith("/")) {
          schemaPath = "/" + schemaPath;
        }
        typePath = schemaPath + typePath;
      }
    }
    recordDefinition = new RecordDefinitionImpl(schema, typePath, getProperties(), fields);
  }

  /**
   * Closes the underlying reader.
   */
  @Override
  protected void doClose() {
    FileUtil.closeSilent(in);
    recordFactory = null;
    geometryFactory = null;
    in = null;
    resource = null;
  }

  @Override
  protected void doInit() {
    try {
      pointXFieldName = getProperty("pointXFieldName");
      pointYFieldName = getProperty("pointYFieldName");
      geometryColumnName = getProperty("geometryColumnName", "GEOMETRY");

      geometryFactory = GeometryFactory.get(getProperty("geometryFactory"));
      if (geometryFactory == null) {
        final Integer geometrySrid = Property.getInteger(this, "geometrySrid");
        if (geometrySrid == null) {
          geometryFactory = EsriCoordinateSystems.getGeometryFactory(resource);
        } else {
          geometryFactory = GeometryFactory.floating3(geometrySrid);
        }
      }
      if (geometryFactory == null) {
        geometryFactory = GeometryFactory.floating3();
      }
      final DataType geometryType = DataTypes.getType((String)getProperty("geometryType"));
      if (Geometry.class.isAssignableFrom(geometryType.getJavaClass())) {
        this.geometryType = geometryType;
      }

      in = new BufferedReader(FileUtil.createUtf8Reader(resource.getInputStream()));
      final String[] line = readNextRecord();
      createRecordDefinition(line);
    } catch (final IOException e) {
      ExceptionUtil.log(getClass(), "Unable to open " + resource, e);
    } catch (final NoSuchElementException e) {
    }
  }

  @Override
  protected Record getNext() {
    try {
      final String[] record = readNextRecord();
      if (record != null && record.length > 0) {
        return parseRecord(record);
      } else {
        throw new NoSuchElementException();
      }
    } catch (final IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * Reads the next line from the file.
   *
   * @return the next line from the file without trailing newline
   * @throws IOException if bad things happen during the read
   */
  private String getNextLine() throws IOException {
    final BufferedReader in = this.in;
    if (in == null) {
      throw new NoSuchElementException();
    } else {
      final String nextLine = this.in.readLine();
      if (nextLine == null) {
        throw new NoSuchElementException();
      }
      return nextLine;
    }
  }

  @Override
  public RecordDefinition getRecordDefinition() {
    return recordDefinition;
  }

  /**
   * Parses an incoming String and returns an array of elements.
   *
   * @param nextLine the string to parse
   * @return the comma-tokenized list of elements, or null if nextLine is null
   * @throws IOException if bad things happen during the read
   */
  private String[] parseLine(final String nextLine, final boolean readLine) throws IOException {
    String line = nextLine;
    if (line.length() == 0) {
      return new String[0];
    } else {

      final List<String> fields = new ArrayList<String>();
      StringBuilder sb = new StringBuilder();
      boolean inQuotes = false;
      boolean hadQuotes = false;
      do {
        if (inQuotes && readLine) {
          sb.append("\n");
          line = getNextLine();
          if (line == null) {
            break;
          }
        }
        for (int i = 0; i < line.length(); i++) {
          final char c = line.charAt(i);
          if (c == CsvConstants.QUOTE_CHARACTER) {
            hadQuotes = true;
            if (inQuotes && line.length() > i + 1
              && line.charAt(i + 1) == CsvConstants.QUOTE_CHARACTER) {
              sb.append(line.charAt(i + 1));
              i++;
            } else {
              inQuotes = !inQuotes;
              if (i > 2 && line.charAt(i - 1) != fieldSeparator && line.length() > i + 1
                && line.charAt(i + 1) != fieldSeparator) {
                sb.append(c);
              }
            }
          } else if (c == fieldSeparator && !inQuotes) {
            hadQuotes = false;
            if (hadQuotes || sb.length() > 0) {
              fields.add(sb.toString());
            } else {
              fields.add(null);
            }
            sb = new StringBuilder();
          } else {
            sb.append(c);
          }
        }
      } while (inQuotes);
      if (sb.length() > 0 || fields.size() > 0) {
        if (hadQuotes || sb.length() > 0) {
          fields.add(sb.toString());
        } else {
          fields.add(null);
        }
      }
      return fields.toArray(new String[0]);
    }
  }

  /**
   * Parse a record containing an array of String values into a Record with
   * the strings converted to the objects based on the attribute data type.
   *
   * @param record The record.
   * @return The Record.
   */
  private Record parseRecord(final String[] record) {
    final Record object = recordFactory.createRecord(recordDefinition);
    for (int i = 0; i < recordDefinition.getFieldCount(); i++) {
      String value = null;
      if (i < record.length) {
        value = record[i];
        if (value != null) {
          final DataType dataType = recordDefinition.getFieldType(i);
          final Object convertedValue = StringConverterRegistry.toObject(dataType, value);
          object.setValue(i, convertedValue);
        }
      }
    }
    if (hasPointFields) {
      final Double x = Maps.getDouble(object, pointXFieldName);
      final Double y = Maps.getDouble(object, pointYFieldName);
      if (x != null && y != null) {
        final Geometry geometry = geometryFactory.point(x, y);
        object.setGeometryValue(geometry);
      }
    }
    return object;
  }

  /**
   * Reads the next line from the buffer and converts to a string array.
   *
   * @return a string array with each comma-separated element as a separate
   *         entry.
   * @throws IOException if bad things happen during the read
   */
  private String[] readNextRecord() throws IOException {
    final String nextLine = getNextLine();
    return parseLine(nextLine, true);
  }
}
