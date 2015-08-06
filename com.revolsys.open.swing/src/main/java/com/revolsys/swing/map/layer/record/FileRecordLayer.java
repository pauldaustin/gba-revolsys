package com.revolsys.swing.map.layer.record;

import java.util.Map;

import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.io.RecordIo;
import com.revolsys.data.record.io.RecordReader;
import com.revolsys.data.record.io.RecordReaderFactory;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.io.FileNames;
import com.revolsys.io.IoFactoryRegistry;
import com.revolsys.io.map.InvokeMethodMapObjectFactory;
import com.revolsys.io.map.MapObjectFactory;
import com.revolsys.io.map.MapSerializerUtil;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.spring.resource.SpringUtil;
import com.revolsys.swing.SwingUtil;
import com.revolsys.swing.component.BasePanel;
import com.revolsys.swing.component.ValueField;
import com.revolsys.swing.layout.GroupLayoutUtil;
import com.revolsys.util.ExceptionUtil;
import com.revolsys.util.Property;
import com.vividsolutions.jts.geom.Geometry;

public class FileRecordLayer extends ListRecordLayer {
  public static final String DATA_OBJECT_FILE = "dataObjectFile";

  public static final MapObjectFactory FACTORY = new InvokeMethodMapObjectFactory(DATA_OBJECT_FILE,
    "File", FileRecordLayer.class, "create");

  public static FileRecordLayer create(final Map<String, Object> properties) {
    return new FileRecordLayer(properties);
  }

  private String url;

  private Resource resource;

  public FileRecordLayer(final Map<String, ? extends Object> properties) {
    super(properties);
    setType(DATA_OBJECT_FILE);
  }

  @Override
  protected ValueField addPropertiesTabGeneralPanelSource(final BasePanel parent) {
    final ValueField panel = super.addPropertiesTabGeneralPanelSource(parent);

    final String url = getUrl();
    if (url.startsWith("file:")) {
      final String fileName = url.replaceFirst("file:(//)?", "");
      SwingUtil.addReadOnlyTextField(panel, "File", fileName);
    } else {
      SwingUtil.addReadOnlyTextField(panel, "URL", url);
    }
    final String fileNameExtension = FileNames.getFileNameExtension(url);
    if (Property.hasValue(fileNameExtension)) {
      SwingUtil.addReadOnlyTextField(panel, "File Extension", fileNameExtension);
      final RecordReaderFactory factory = IoFactoryRegistry.getInstance()
        .getFactoryByFileExtension(RecordReaderFactory.class, fileNameExtension);
      if (factory != null) {
        SwingUtil.addReadOnlyTextField(panel, "File Type", factory.getName());
      }
    }
    GroupLayoutUtil.makeColumns(panel, 2, true);
    return panel;
  }

  @Override
  protected boolean doInitialize() {
    this.url = getProperty("url");
    if (Property.hasValue(this.url)) {
      this.resource = SpringUtil.getResource(this.url);
      return revert();
    } else {
      LoggerFactory.getLogger(getClass())
        .error("Layer definition does not contain a 'url' property: " + getName());
      return false;
    }

  }

  public String getUrl() {
    return this.url;
  }

  public boolean revert() {
    if (this.resource == null) {
      return false;
    } else {
      if (this.resource.exists()) {
        final Resource resource1 = this.resource;
        final RecordReader reader = RecordIo.recordReader(resource1);
        if (reader == null) {
          LoggerFactory.getLogger(getClass()).error("Cannot find reader for: " + this.resource);
          return false;
        } else {
          try {
            final RecordDefinition metaData = reader.getRecordDefinition();
            setRecordDefinition(metaData);
            final GeometryFactory geometryFactory = metaData.getGeometryFactory();
            BoundingBox boundingBox = new BoundingBox(geometryFactory);
            for (final Record record : reader) {
              final Geometry geometry = record.getGeometry();
              boundingBox = boundingBox.expandToInclude(geometry);

              createRecordInternal(record);
            }
            setBoundingBox(boundingBox);
            return true;
          } catch (final Throwable e) {
            ExceptionUtil.log(getClass(), "Error reading: " + this.resource, e);
          } finally {
            fireRecordsChanged();
            reader.close();
          }
        }
      } else {
        LoggerFactory.getLogger(getClass()).error("Cannot find: " + this.url);
      }
    }
    return false;
  }

  @Override
  public Map<String, Object> toMap() {
    final Map<String, Object> map = super.toMap();
    MapSerializerUtil.add(map, "url", this.url);
    return map;
  }
}
