package com.revolsys.gis.oracle.esri;

import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.LoggerFactory;

import com.revolsys.data.record.schema.FieldDefinition;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.record.schema.RecordStore;
import com.revolsys.data.record.schema.RecordStoreSchema;
import com.revolsys.gis.data.io.DataObjectStoreExtension;
import com.revolsys.gis.oracle.io.OracleSdoGeometryJdbcAttribute;
import com.revolsys.jdbc.field.JdbcFieldAdder;
import com.revolsys.jdbc.io.AbstractJdbcRecordStore;

public class ArcSdeBinaryGeometryDataStoreExtension implements DataObjectStoreExtension {

  private Object sdeUtil;

  public ArcSdeBinaryGeometryDataStoreExtension() {
  }

  @Override
  public void initialize(final RecordStore dataStore, final Map<String, Object> connectionProperties) {
    try {
      this.sdeUtil = new ArcSdeBinaryGeometryDataStoreUtil(dataStore, connectionProperties);
    } catch (final NoClassDefFoundError e) {

    }
  }

  @Override
  public boolean isEnabled(final RecordStore dataStore) {
    return ArcSdeConstants.isSdeAvailable(dataStore) && this.sdeUtil != null;
  }

  @Override
  public void postProcess(final RecordStoreSchema schema) {
    final AbstractJdbcRecordStore dataStore = (AbstractJdbcRecordStore)schema.getRecordStore();
    for (final RecordDefinition metaData : schema.getTypes()) {
      final String typePath = metaData.getPath();
      final Map<String, Map<String, Object>> typeColumnProperties = JdbcFieldAdder.getTypeColumnProperties(
        schema, typePath);
      for (final Entry<String, Map<String, Object>> columnEntry : typeColumnProperties.entrySet()) {
        final String columnName = columnEntry.getKey();
        final Map<String, Object> columnProperties = columnEntry.getValue();
        if (ArcSdeConstants.SDEBINARY.equals(columnProperties.get(ArcSdeConstants.GEOMETRY_COLUMN_TYPE))) {
          final FieldDefinition attribute = metaData.getField(columnName);
          if (!(attribute instanceof OracleSdoGeometryJdbcAttribute)) {
            if (this.sdeUtil == null) {
              LoggerFactory.getLogger(getClass()).error(
                  "SDE Binary columns not supported without the ArcSDE Java API jars");
            } else {
              ((ArcSdeBinaryGeometryDataStoreUtil)this.sdeUtil).createGeometryColumn(dataStore,
                schema, metaData, typePath, columnName, columnName.toUpperCase(), columnProperties);
            }
          }
        }
      }
    }
  }

  @Override
  public void preProcess(final RecordStoreSchema schema) {
  }

}
