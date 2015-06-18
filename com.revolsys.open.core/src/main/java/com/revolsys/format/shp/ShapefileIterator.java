package com.revolsys.format.shp;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel.MapMode;
import java.util.NoSuchElementException;

import org.springframework.core.io.Resource;

import com.revolsys.collection.iterator.AbstractIterator;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordFactory;
import com.revolsys.data.record.io.RecordIterator;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.record.schema.RecordDefinitionImpl;
import com.revolsys.data.types.DataType;
import com.revolsys.data.types.DataTypes;
import com.revolsys.format.xbase.XbaseIterator;
import com.revolsys.gis.cs.CoordinateSystem;
import com.revolsys.gis.cs.esri.EsriCoordinateSystems;
import com.revolsys.gis.data.model.DataObjectUtil;
import com.revolsys.gis.io.EndianInputStream;
import com.revolsys.gis.io.EndianMappedByteBuffer;
import com.revolsys.gis.io.LittleEndianRandomAccessFile;
import com.revolsys.io.EndianInput;
import com.revolsys.io.FileUtil;
import com.revolsys.io.IoConstants;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.parallel.process.InvokeMethodRunnable;
import com.revolsys.spring.SpringUtil;
import com.revolsys.util.Property;
import com.vividsolutions.jts.geom.Geometry;

public class ShapefileIterator extends AbstractIterator<Record> implements RecordIterator {

  private boolean closeFile = true;

  private RecordFactory dataObjectFactory;

  private GeometryFactory geometryFactory;

  private EndianInput in;

  private EndianMappedByteBuffer indexIn;

  private boolean mappedFile;

  private RecordDefinition metaData;

  private final String name;

  private int position;

  private Resource resource;

  private int shapeType;

  private XbaseIterator xbaseIterator;

  private String typeName;

  private RecordDefinition returnMetaData;

  public ShapefileIterator(final Resource resource, final RecordFactory factory) throws IOException {
    this.dataObjectFactory = factory;
    final String baseName = FileUtil.getBaseName(resource.getFilename());
    this.name = baseName;
    this.typeName = "/" + this.name;
    this.resource = resource;
  }

  @Override
  protected void doClose() {
    if (this.closeFile) {
      forceClose();
    }
  }

  @Override
  protected synchronized void doInit() {
    if (this.in == null) {
      try {
        final Boolean memoryMapped = getProperty("memoryMapped");
        try {
          final File file = SpringUtil.getFile(this.resource);
          final File indexFile = new File(file.getParentFile(), this.name + ".shx");
          if (Boolean.TRUE == memoryMapped) {
            this.in = new EndianMappedByteBuffer(file, MapMode.READ_ONLY);
            this.indexIn = new EndianMappedByteBuffer(indexFile, MapMode.READ_ONLY);
            this.mappedFile = true;
          } else {
            this.in = new LittleEndianRandomAccessFile(file, "r");
          }
        } catch (final IllegalArgumentException e) {
          this.in = new EndianInputStream(this.resource.getInputStream());
        } catch (final FileNotFoundException e) {
          this.in = new EndianInputStream(this.resource.getInputStream());
        }

        final Resource xbaseResource = this.resource.createRelative(this.name + ".dbf");
        if (xbaseResource.exists()) {
          this.xbaseIterator = new XbaseIterator(xbaseResource, this.dataObjectFactory,
            new InvokeMethodRunnable(this, "updateMetaData"));
          this.xbaseIterator.setTypeName(this.typeName);
          this.xbaseIterator.setProperty("memoryMapped", memoryMapped);
          this.xbaseIterator.setCloseFile(this.closeFile);
        }
        loadHeader();
        int numAxis = 3;
        int srid = 0;
        if (this.shapeType < 10) {
          numAxis = 2;
        } else if (this.shapeType < 20) {
          numAxis = 3;
        } else if (this.shapeType < 30) {
          numAxis = 4;
        } else {
          numAxis = 4;
        }
        this.geometryFactory = getProperty(IoConstants.GEOMETRY_FACTORY);
        final Resource projResource = this.resource.createRelative(this.name + ".prj");
        if (projResource.exists()) {
          try {
            final CoordinateSystem coordinateSystem = EsriCoordinateSystems.getCoordinateSystem(projResource);
            srid = EsriCoordinateSystems.getCrsId(coordinateSystem);
            setProperty(IoConstants.GEOMETRY_FACTORY, this.geometryFactory);
          } catch (final Exception e) {
            e.printStackTrace();
          }
        }
        if (this.geometryFactory == null) {
          if (srid < 1) {
            srid = 4326;
          }
          this.geometryFactory = GeometryFactory.floating(srid, numAxis);
        }

        if (this.xbaseIterator != null) {
          this.xbaseIterator.hasNext();
        }
        if (this.metaData == null) {
          this.metaData = DataObjectUtil.createGeometryMetaData();
        }
        this.metaData.setGeometryFactory(this.geometryFactory);
      } catch (final IOException e) {
        throw new RuntimeException("Error initializing mappedFile " + this.resource, e);
      }
    }
  }

  public void forceClose() {
    FileUtil.closeSilent(this.in, this.indexIn);
    if (this.xbaseIterator != null) {
      this.xbaseIterator.forceClose();
    }
    this.dataObjectFactory = null;
    this.geometryFactory = null;
    this.in = null;
    this.indexIn = null;
    this.metaData = null;
    this.resource = null;
    this.xbaseIterator = null;
  }

  public RecordFactory getDataObjectFactory() {
    return this.dataObjectFactory;
  }

  @Override
  protected Record getNext() {
    Record record;
    try {
      if (this.xbaseIterator != null) {
        if (this.xbaseIterator.hasNext()) {
          record = this.xbaseIterator.next();
          for (int i = 0; i < this.xbaseIterator.getDeletedCount(); i++) {
            this.position++;
            readGeometry();
          }
        } else {
          throw new NoSuchElementException();
        }
      } else {
        record = this.dataObjectFactory.createRecord(this.metaData);
      }

      final Geometry geometry = readGeometry();
      record.setGeometryValue(geometry);
    } catch (final EOFException e) {
      throw new NoSuchElementException();
    } catch (final IOException e) {
      throw new RuntimeException("Error reading geometry " + this.resource, e);
    }
    if (this.returnMetaData == null) {
      return record;
    } else {
      final Record copy = this.dataObjectFactory.createRecord(this.returnMetaData);
      copy.setValues(record);
      return copy;
    }
  }

  public int getPosition() {
    return this.position;
  }

  @Override
  public RecordDefinition getRecordDefinition() {
    return this.metaData;
  }

  public String getTypeName() {
    return this.typeName;
  }

  public boolean isCloseFile() {
    return this.closeFile;
  }

  /**
   * Load the header record from the shape mappedFile.
   *
   * @throws IOException If an I/O error occurs.
   */
  @SuppressWarnings("unused")
  private void loadHeader() throws IOException {
    this.in.readInt();
    this.in.skipBytes(20);
    final int fileLength = this.in.readInt();
    final int version = this.in.readLEInt();
    this.shapeType = this.in.readLEInt();
    final double minX = this.in.readLEDouble();
    final double minY = this.in.readLEDouble();
    final double maxX = this.in.readLEDouble();
    final double maxY = this.in.readLEDouble();
    final double minZ = this.in.readLEDouble();
    final double maxZ = this.in.readLEDouble();
    final double minM = this.in.readLEDouble();
    final double maxM = this.in.readLEDouble();
  }

  @SuppressWarnings("unused")
  private Geometry readGeometry() throws IOException {
    final int recordNumber = this.in.readInt();
    final int recordLength = this.in.readInt();
    final int shapeType = this.in.readLEInt();
    return ShapefileGeometryUtil.SHP_INSTANCE.read(this.geometryFactory, this.in, shapeType);
  }

  public void setCloseFile(final boolean closeFile) {
    this.closeFile = closeFile;
    if (this.xbaseIterator != null) {
      this.xbaseIterator.setCloseFile(closeFile);
    }
  }

  public void setMetaData(final RecordDefinition metaData) {
    this.returnMetaData = metaData;
  }

  public void setPosition(final int position) {
    if (this.mappedFile) {
      final EndianMappedByteBuffer file = (EndianMappedByteBuffer)this.in;
      this.position = position;
      try {
        this.indexIn.seek(100 + 8 * position);
        final int offset = this.indexIn.readInt();
        file.seek(offset * 2);
        setLoadNext(true);
      } catch (final IOException e) {
        throw new RuntimeException("Unable to find record " + position, e);
      }
      if (this.xbaseIterator != null) {
        this.xbaseIterator.setPosition(position);
      }
    } else {
      throw new UnsupportedOperationException("The position can only be set on files");
    }
  }

  public void setTypeName(final String typeName) {
    if (Property.hasValue(typeName)) {
      this.typeName = typeName;
    }
  }

  @Override
  public String toString() {
    return ShapefileConstants.DESCRIPTION + " " + this.resource;
  }

  public void updateMetaData() {
    assert this.metaData == null : "Cannot override metaData when set";
    if (this.xbaseIterator != null) {
      final RecordDefinitionImpl metaData = this.xbaseIterator.getRecordDefinition();
      this.metaData = metaData;
      if (metaData.getGeometryFieldIndex() == -1) {
        DataType geometryType = DataTypes.GEOMETRY;
        switch (this.shapeType) {
          case ShapefileConstants.POINT_SHAPE:
          case ShapefileConstants.POINT_Z_SHAPE:
          case ShapefileConstants.POINT_M_SHAPE:
          case ShapefileConstants.POINT_ZM_SHAPE:
            geometryType = DataTypes.POINT;
          break;

          case ShapefileConstants.POLYLINE_SHAPE:
          case ShapefileConstants.POLYLINE_Z_SHAPE:
          case ShapefileConstants.POLYLINE_M_SHAPE:
          case ShapefileConstants.POLYLINE_ZM_SHAPE:
            geometryType = DataTypes.MULTI_LINE_STRING;
          break;

          case ShapefileConstants.POLYGON_SHAPE:
          case ShapefileConstants.POLYGON_Z_SHAPE:
          case ShapefileConstants.POLYGON_M_SHAPE:
          case ShapefileConstants.POLYGON_ZM_SHAPE:
            geometryType = DataTypes.MULTI_POLYGON;
          break;

          case ShapefileConstants.MULTI_POINT_SHAPE:
          case ShapefileConstants.MULTI_POINT_Z_SHAPE:
          case ShapefileConstants.MULTI_POINT_M_SHAPE:
          case ShapefileConstants.MULTI_POINT_ZM_SHAPE:
            geometryType = DataTypes.MULTI_POINT;
          break;

          default:
          break;
        }
        metaData.addField("geometry", geometryType, true);
      }
    }
  }

}
