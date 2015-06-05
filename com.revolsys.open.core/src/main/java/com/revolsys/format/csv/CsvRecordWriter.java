package com.revolsys.format.csv;

import java.io.PrintWriter;
import java.io.Writer;

import com.revolsys.converter.string.StringConverter;
import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.types.DataType;
import com.revolsys.io.AbstractWriter;
import com.revolsys.io.FileUtil;

public class CsvRecordWriter extends AbstractWriter<Record> {
  /** The writer */
  private final PrintWriter out;

  private final RecordDefinition metaData;

  /**
   * Constructs CSVReader with supplied separator and quote char.
   * 
   * @param reader The reader to the CSV file.
   */
  public CsvRecordWriter(final RecordDefinition metaData, final Writer out) {
    this.metaData = metaData;
    this.out = new PrintWriter(out);
    for (int i = 0; i < metaData.getFieldCount(); i++) {
      if (i > 0) {
        this.out.print(',');
      }
      final String name = metaData.getFieldName(i);
      string(name);
    }
    this.out.println();
  }

  /**
   * Closes the underlying reader.
   */
  @Override
  public void close() {
    FileUtil.closeSilent(out);
  }

  @Override
  public void flush() {
    out.flush();
  }

  private void string(final Object value) {
    final String string = value.toString().replaceAll("\"", "\"\"");
    out.print('"');
    out.print(string);
    out.print('"');
  }

  @Override
  public void write(final Record object) {
    for (int i = 0; i < metaData.getFieldCount(); i++) {
      if (i > 0) {
        out.print(',');
      }
      final Object value = object.getValue(i);
      if (value != null) {
        final String name = metaData.getFieldName(i);
        final DataType dataType = metaData.getFieldType(name);

        @SuppressWarnings("unchecked")
        final Class<Object> dataTypeClass = (Class<Object>)dataType.getJavaClass();
        final StringConverter<Object> converter = StringConverterRegistry.getInstance()
          .getConverter(dataTypeClass);
        if (converter == null) {
          string(value);
        } else {
          final String stringValue = converter.toString(value);
          if (converter.requiresQuotes()) {
            string(stringValue);
          } else {
            out.print(stringValue);
          }
        }
      }
    }
    out.println();
  }

}
