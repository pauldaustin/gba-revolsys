package com.revolsys.gis.data.io;

import java.io.File;
import java.util.Set;

import org.springframework.core.io.Resource;

import com.revolsys.data.record.Record;
import com.revolsys.gis.cs.CoordinateSystem;
import com.revolsys.gis.data.model.DataObjectFactory;
import com.revolsys.io.IoFactory;
import com.revolsys.io.Reader;

public interface DataObjectReaderFactory extends IoFactory {

  DataObjectReader createDataObjectReader(Resource resource);

  DataObjectReader createDataObjectReader(Resource resource,
    DataObjectFactory factory);

  Reader<Record> createDirectoryDataObjectReader();

  Reader<Record> createDirectoryDataObjectReader(File file);

  Reader<Record> createDirectoryDataObjectReader(File file,
    DataObjectFactory factory);

  Set<CoordinateSystem> getCoordinateSystems();

  boolean isBinary();

  boolean isCoordinateSystemSupported(CoordinateSystem coordinateSystem);
}
