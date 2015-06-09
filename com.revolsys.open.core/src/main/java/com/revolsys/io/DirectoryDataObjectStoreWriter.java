package com.revolsys.io;

import javax.annotation.PreDestroy;

import com.revolsys.data.record.Record;

public class DirectoryDataObjectStoreWriter extends AbstractWriter<Record> {

  private DirectoryDataObjectStore dataStore;

  public DirectoryDataObjectStoreWriter(final DirectoryDataObjectStore dataStore) {
    this.dataStore = dataStore;
  }

  @PreDestroy
  @Override
  public void close() {
    super.close();
    this.dataStore = null;
  }

  @Override
  public void write(final Record object) {
    if (object != null) {
      try {
        final boolean currentDataStore = object.getRecordDefinition().getRecordStore() == this.dataStore;
        switch (object.getState()) {
          case New:
            this.dataStore.insert(object);
            break;
          case Modified:
            if (currentDataStore) {
              throw new UnsupportedOperationException();
            } else {
              this.dataStore.insert(object);
            }
            break;
          case Persisted:
            if (currentDataStore) {
              throw new UnsupportedOperationException();
            } else {
              this.dataStore.insert(object);
            }
          case Deleted:
            throw new UnsupportedOperationException();
          default:
            throw new IllegalStateException("State not known");
        }
      } catch (final RuntimeException e) {
        throw e;
      } catch (final Error e) {
        throw e;
      } catch (final Exception e) {
        throw new RuntimeException("Unable to write", e);
      }
    }
  }
}
