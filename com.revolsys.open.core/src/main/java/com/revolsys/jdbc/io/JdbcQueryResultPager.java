package com.revolsys.jdbc.io;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;

import com.revolsys.collection.ResultPager;
import com.revolsys.converter.string.BooleanStringConverter;
import com.revolsys.data.query.Query;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordFactory;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.io.FileUtil;
import com.revolsys.jdbc.JdbcConnection;
import com.revolsys.jdbc.JdbcUtils;

public class JdbcQueryResultPager implements ResultPager<Record> {
  /** The objects in the current page. */
  private List<Record> results;

  /** The number of objects in a page. */
  private int pageSize = 10;

  /** The current page number. */
  private int pageNumber = -1;

  /** The total number of results. */
  private int numResults;

  /** The number of pages. */
  private int numPages;

  private JdbcConnection connection;

  private RecordFactory recordFactory;

  private JdbcRecordStore recordStore;

  private RecordDefinition recordDefinition;

  private PreparedStatement statement;

  private ResultSet resultSet;

  private final Query query;

  private final String sql;

  public JdbcQueryResultPager(final JdbcRecordStore recordStore,
    final Map<String, Object> properties, final Query query) {
    final boolean autoCommit = BooleanStringConverter.getBoolean(properties.get("autoCommit"));
    this.connection = recordStore.getJdbcConnection(autoCommit);
    this.recordFactory = recordStore.getRecordFactory();
    this.recordStore = recordStore;

    this.query = query;

    final String tableName = query.getTypeName();
    this.recordDefinition = query.getRecordDefinition();
    if (this.recordDefinition == null) {
      this.recordDefinition = recordStore.getRecordDefinition(tableName);
      query.setRecordDefinition(this.recordDefinition);
    }

    this.sql = JdbcUtils.getSelectSql(query);
  }

  @Override
  @PreDestroy
  public void close() {
    JdbcUtils.close(this.statement, this.resultSet);
    FileUtil.closeSilent(this.connection);
    this.connection = null;
    this.recordFactory = null;
    this.recordStore = null;
    this.recordDefinition = null;
    this.results = null;
    this.resultSet = null;
    this.statement = null;
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    close();
  }

  /**
   * Get the index of the last object in the current page.
   *
   * @return The index of the last object in the current page.
   */
  @Override
  public int getEndIndex() {
    if (this.pageNumber == getNumPages()) {
      return getNumResults();
    } else {
      return (this.pageNumber + 1) * this.pageSize;
    }
  }

  /**
   * Get the list of objects in the current page.
   *
   * @return The list of objects in the current page.
   */
  @Override
  public List<Record> getList() {
    if (this.results == null) {
      throw new IllegalStateException("The page number must be set using setPageNumber");
    }
    return this.results;
  }

  /**
   * Get the page number of the next page.
   *
   * @return Thepage number of the next page.
   */
  @Override
  public int getNextPageNumber() {
    return this.pageNumber + 2;
  }

  /**
   * Get the number of pages.
   *
   * @return The number of pages.
   */
  @Override
  public int getNumPages() {
    return this.numPages + 1;
  }

  /**
   * Get the total number of results returned.
   *
   * @return The total number of results returned.
   */
  @Override
  public int getNumResults() {
    return this.numResults;
  }

  /**
   * Get the page number of the current page. Index starts at 1.
   *
   * @return The page number of the current page.
   */
  @Override
  public int getPageNumber() {
    return this.pageNumber + 1;
  }

  /**
   * Get the number of objects to display in a page.
   *
   * @return The number of objects to display in a page.
   */
  @Override
  public int getPageSize() {
    return this.pageSize;
  }

  /**
   * Get the page number of the previous page.
   *
   * @return Thepage number of the previous page.
   */
  @Override
  public int getPreviousPageNumber() {
    return this.pageNumber;
  }

  public Query getQuery() {
    return this.query;
  }

  public RecordDefinition getRecordDefinition() {
    return this.recordDefinition;
  }

  public RecordFactory getRecordFactory() {
    return this.recordFactory;
  }

  public JdbcRecordStore getRecordStore() {
    return this.recordStore;
  }

  protected String getSql() {
    return this.sql;
  }

  /**
   * Get the index of the first object in the current page. Index starts at 1.
   *
   * @return The index of the first object in the current page.
   */
  @Override
  public int getStartIndex() {
    if (getNumResults() == 0) {
      return 0;
    } else {
      return this.pageNumber * this.pageSize + 1;
    }
  }

  /**
   * Check to see if there is a next page.
   *
   * @return True if there is a next page.
   */
  @Override
  public boolean hasNextPage() {
    return this.pageNumber < getNumPages();
  }

  /**
   * Check to see if there is a previous page.
   *
   * @return True if there is a previous page.
   */
  @Override
  public boolean hasPreviousPage() {
    return this.pageNumber > 0;
  }

  private void initResultSet() {
    if (this.resultSet == null) {
      try {
        this.statement = this.connection.prepareStatement(this.sql,
          ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
        this.statement.setFetchSize(this.pageSize);

        this.resultSet = JdbcQueryIterator.getResultSet(this.recordDefinition, this.statement,
          this.query);
        this.resultSet.last();
        this.numResults = this.resultSet.getRow();
      } catch (final SQLException e) {
        JdbcUtils.close(this.statement, this.resultSet);
        throw new RuntimeException("Error executing query:" + this.sql, e);
      }
    }
  }

  public boolean isClosed() {
    return this.recordStore == null;
  }

  /**
   * Check to see if this is the first page.
   *
   * @return True if this is the first page.
   */
  @Override
  public boolean isFirstPage() {
    return this.pageNumber == 0;
  }

  /**
   * Check to see if this is the last page.
   *
   * @return True if this is the last page.
   */
  @Override
  public boolean isLastPage() {
    return this.pageNumber == getNumPages();
  }

  protected void setNumResults(final int numResults) {
    this.numResults = numResults;
  }

  /**
   * Set the current page number.
   *
   * @param pageNumber The current page number.
   */
  @Override
  public void setPageNumber(final int pageNumber) {
    if (pageNumber > getNumPages()) {
      this.pageNumber = getNumPages() - 1;
    } else if (pageNumber <= 0) {
      this.pageNumber = 0;
    } else {
      this.pageNumber = pageNumber - 1;
    }
    updateResults();
  }

  @Override
  public void setPageNumberAndSize(final int pageSize, final int pageNumber) {
    if (pageNumber <= 0) {
      this.pageNumber = 0;
    } else {
      this.pageNumber = pageNumber - 1;
    }
    this.pageSize = pageSize;
    updateNumPages();
    updateResults();
  }

  /**
   * Set the number of objects per page.
   *
   * @param pageSize The number of objects per page.
   */
  @Override
  public void setPageSize(final int pageSize) {
    this.pageSize = pageSize;
    updateNumPages();
    updateResults();
  }

  protected void setResults(final List<Record> results) {
    this.results = results;
  }

  protected void updateNumPages() {
    this.numPages = Math.max(0, (getNumResults() - 1) / getPageSize());
  }

  /**
   * Update the cached results for the current page.
   */
  protected void updateResults() {
    this.results = new ArrayList<Record>();
    try {
      initResultSet();
      if (this.pageNumber != -1 && this.resultSet != null) {
        if (this.resultSet.absolute(this.pageNumber * this.pageSize + 1)) {
          int i = 0;
          do {
            final Record object = JdbcQueryIterator.getNextRecord(this.recordStore,
              this.recordDefinition, this.recordDefinition.getFields(), this.recordFactory,
              this.resultSet);
            this.results.add(object);
            i++;
          } while (this.resultSet.next() && i < this.pageSize);
        }
      }
    } catch (final SQLException e) {
      this.connection.getException("updateResults", this.sql, e);
    } catch (final RuntimeException e) {
      close();
    } catch (final Error e) {
      close();
    }
  }
}
