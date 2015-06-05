package com.revolsys.format.csv;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.revolsys.io.FileUtil;
import com.revolsys.util.ExceptionUtil;

public class CsvIterator implements Iterator<List<String>>, Iterable<List<String>> {

  private static final int BUFFER_SIZE = 8096;

  /** The current record. */
  private List<String> currentRecord;

  /** Flag indicating if there are more records to be read. */
  private boolean hasNext = true;

  /** The reader to */
  private final Reader in;

  private final char[] buffer = new char[BUFFER_SIZE];

  private int readCount;

  private int index = 0;

  private final StringBuilder sb = new StringBuilder();

  /**
   * Constructs CSVReader with supplied separator and quote char.
   *
   * @param reader The reader to the CSV file.
   * @throws IOException
   */
  public CsvIterator(final Reader in) {
    this.in = in;
    readNextRecord();
  }

  /**
   * Closes the underlying reader.
   *
   * @throws IOException if the close fails
   */
  public void close() {
    FileUtil.closeSilent(in);
  }

  /**
   * Returns <tt>true</tt> if the iteration has more elements.
   *
   * @return <tt>true</tt> if the iterator has more elements.
   */
  @Override
  public boolean hasNext() {
    return hasNext;
  }

  @Override
  public Iterator<List<String>> iterator() {
    return this;
  }

  /**
   * Return the next record from the iterator.
   *
   * @return The record
   */
  @Override
  public List<String> next() {
    if (!hasNext) {
      throw new NoSuchElementException("No more elements");
    } else {
      final List<String> object = currentRecord;
      readNextRecord();
      return object;
    }
  }

  private List<String> parseRecord() throws IOException {
    final StringBuilder sb = this.sb;
    final Reader in = this.in;
    sb.delete(0, sb.length());
    final List<String> fields = new ArrayList<>();
    boolean inQuotes = false;
    boolean hadQuotes = false;
    while (readCount != -1) {
      if (index >= readCount) {
        index = 0;
        readCount = in.read(buffer, 0, BUFFER_SIZE);
        if (readCount < 0) {
          if (fields.isEmpty()) {
            hasNext = false;
            return null;
          } else {
            return fields;
          }
        }
      }
      final char c = buffer[index++];
      switch (c) {
        case '"':
          hadQuotes = true;
          final char nextChar = previewNextChar();
          if (inQuotes && nextChar == '"') {
            sb.append('"');
            index++;
          } else {
            inQuotes = !inQuotes;
            if (sb.length() > 0 && nextChar != ',' && nextChar != '\n' && nextChar != 0) {
              sb.append(c);
            }
          }
        break;
        case ',':
          if (inQuotes) {
            sb.append(c);
          } else {
            if (hadQuotes || sb.length() > 0) {
              fields.add(sb.toString());
              sb.delete(0, sb.length());
            } else {
              fields.add(null);
            }
            hadQuotes = false;
          }
        break;
        case '\r':
          if (previewNextChar() == '\n') {
          } else {
            if (inQuotes) {
              sb.append('\n');
            } else {
              if (hadQuotes || sb.length() > 0) {
                fields.add(sb.toString());
                sb.delete(0, sb.length());
              } else {
                fields.add(null);
              }
              return fields;
            }
          }
        break;
        case '\n':
          if (previewNextChar() == '\r') {
            index++;
          }
          if (inQuotes) {
            sb.append(c);
          } else {
            if (hadQuotes || sb.length() > 0) {
              fields.add(sb.toString());
              sb.delete(0, sb.length());
            } else {
              fields.add(null);
            }
            return fields;
          }
        break;
        default:
          sb.append(c);
        break;
      }
    }
    hasNext = false;
    return null;
  }

  private char previewNextChar() throws IOException {
    if (index >= readCount) {
      index = 0;
      readCount = in.read(buffer, 0, BUFFER_SIZE);
      if (readCount < 0) {
        return 0;
      }
    }
    return buffer[index];
  }

  /**
   * Reads the next line from the buffer and converts to a string array.
   *
   * @return a string array with each comma-separated element as a separate
   *         entry.
   * @throws IOException if bad things happen during the read
   */
  private List<String> readNextRecord() {
    if (hasNext) {
      try {
        currentRecord = parseRecord();
      } catch (final IOException e) {
        ExceptionUtil.throwUncheckedException(e);
      }
      return currentRecord;
    } else {
      return null;
    }
  }

  /**
   * Removing items from the iterator is not supported.
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
