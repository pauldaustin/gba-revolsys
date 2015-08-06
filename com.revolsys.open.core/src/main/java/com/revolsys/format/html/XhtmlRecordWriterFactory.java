package com.revolsys.format.html;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import com.revolsys.data.record.io.AbstractRecordAndGeometryWriterFactory;
import com.revolsys.data.record.io.RecordWriter;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.io.FileUtil;

public class XhtmlRecordWriterFactory extends AbstractRecordAndGeometryWriterFactory {
  public XhtmlRecordWriterFactory() {
    super("XHMTL", true, true);
    addMediaTypeAndFileExtension("text/html", "html");
    addMediaTypeAndFileExtension("application/xhtml+xml", "xhtml");
    addMediaTypeAndFileExtension("application/xhtml+xml", "html");
  }

  @Override
  public RecordWriter createRecordWriter(final String baseName, final RecordDefinition metaData,
    final OutputStream outputStream, final Charset charset) {
    final OutputStreamWriter writer = FileUtil.createUtf8Writer(outputStream);
    return new XhtmlRecordWriter(metaData, writer);
  }
}
