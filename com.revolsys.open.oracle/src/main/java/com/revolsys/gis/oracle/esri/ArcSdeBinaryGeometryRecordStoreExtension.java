package com.revolsys.gis.oracle.esri;

import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.LoggerFactory;

import com.revolsys.gis.oracle.io.OracleSdoGeometryJdbcAttribute;
import com.revolsys.jdbc.field.JdbcFieldAdder;
import com.revolsys.jdbc.io.AbstractJdbcRecordStore;
import com.revolsys.record.io.RecordStoreExtension;
import com.revolsys.record.schema.FieldDefinition;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.record.schema.RecordStore;
import com.revolsys.record.schema.RecordStoreSchema;

public class ArcSdeBinaryGeometryRecordStoreExtension implements RecordStoreExtension {

  private Object sdeUtil;

  public ArcSdeBinaryGeometryRecordStoreExtension() {
  }

  @Override
  public void initialize(final RecordStore recordStore,
    final Map<String, Object> connectionProperties) {
    try {
      this.sdeUtil = new ArcSdeBinaryGeometryRecordUtil(recordStore, connectionProperties);
    } catch (final NoClassDefFoundError e) {

    }
  }

  @Override
  public boolean isEnabled(final RecordStore recordStore) {
    return ArcSdeConstants.isSdeAvailable(recordStore) && this.sdeUtil != null;
  }

  @Override
  public void postProcess(final RecordStoreSchema schema) {
    final AbstractJdbcRecordStore recordStore = (AbstractJdbcRecordStore)schema.getRecordStore();
    for (final RecordDefinition recordDefinition : schema.getRecordDefinitions()) {
      final String typePath = recordDefinition.getPath();
      final Map<String, Map<String, Object>> typeColumnProperties = JdbcFieldAdder
        .getTypeColumnProperties(schema, typePath);
      for (final Entry<String, Map<String, Object>> columnEntry : typeColumnProperties.entrySet()) {
        final String columnName = columnEntry.getKey();
        final Map<String, Object> columnProperties = columnEntry.getValue();
        if (ArcSdeConstants.SDEBINARY
          .equals(columnProperties.get(ArcSdeConstants.GEOMETRY_COLUMN_TYPE))) {
          final FieldDefinition attribute = recordDefinition.getField(columnName);
          if (!(attribute instanceof OracleSdoGeometryJdbcAttribute)) {
            if (this.sdeUtil == null) {
              LoggerFactory.getLogger(getClass())
                .error("SDE Binary columns not supported without the ArcSDE Java API jars");
            } else {
              ((ArcSdeBinaryGeometryRecordUtil)this.sdeUtil).createGeometryColumn(recordStore,
                schema, recordDefinition, typePath, columnName, columnName.toUpperCase(),
                columnProperties);
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