package com.revolsys.swing.map.layer.dataobject;

import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.Map;

import com.revolsys.gis.data.model.ArrayDataObject;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.gis.data.model.DataObjectState;
import com.revolsys.gis.model.data.equals.EqualsRegistry;

public class LayerDataObject extends ArrayDataObject {
  private static final long serialVersionUID = 1L;

  private DataObjectLayer layer;

  private Map<String, Object> originalValues;

  public LayerDataObject(DataObjectLayer layer) {
    super(layer.getMetaData());
    this.layer = layer;
  }

  public DataObjectLayer getLayer() {
    return layer;
  }

  @SuppressWarnings("unchecked")
  public <T> T getOriginalValue(String name) {
    if (originalValues == null) {
      return (T)getValue(name);
    } else {
      return (T)originalValues.get(name);
    }
  }

  public boolean isModified(String name) {
    if (originalValues == null) {
      return false;
    } else {
      return originalValues.containsKey(name);
    }
  }

  @Override
  public synchronized void setValue(int index, Object value) {
    DataObjectMetaData metaData = getMetaData();
    String attributeName = metaData.getAttributeName(index);

    final Object oldValue = getValue(index);
    if (!EqualsRegistry.INSTANCE.equals(oldValue, value)) {
      DataObjectLayer layer = getLayer();
      DataObjectState state = getState();
      if (state == DataObjectState.Initalizing) {
        // Allow modification on initialization
      } else if (state == DataObjectState.New) {
        if (!layer.isCanAddObjects()) {
          throw new IllegalStateException(
            "Adding new objects is not supported for layer " + layer);
        }
      } else if (state == DataObjectState.Deleted) {
        throw new IllegalStateException(
          "Cannot edit a deleted object for layer " + layer);
      } else {
        if (layer.isCanEditObjects()) {
          final Object originalValue = getOriginalValue(attributeName);
          if (EqualsRegistry.equal(value, originalValue)) {
            if (originalValues != null) {
              originalValues.remove(attributeName);
              if (originalValues.isEmpty()) {
                originalValues = null;
                setState(DataObjectState.Persisted);
              }
            }
          } else {
            if (originalValues == null) {
              originalValues = new HashMap<String, Object>();
            }
            originalValues.put(attributeName, originalValue);
          }
        } else {
          throw new IllegalStateException(
            "Editing objects is not supported for layer " + layer);
        }
      }
      super.setValue(index, value);
      PropertyChangeEvent event = new PropertyChangeEvent(this, attributeName,
        oldValue, value);
      layer.propertyChange(event);
    }

  }

}
