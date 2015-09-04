package com.revolsys.gis.converter;

import java.util.function.Predicate;

import org.springframework.core.convert.converter.Converter;

import com.revolsys.data.record.Record;

public class FilterRecordConverter {
  private Converter<Record, Record> converter;

  private Predicate<Record> predicate;

  public FilterRecordConverter() {
  }

  public FilterRecordConverter(final Predicate<Record> filter,
    final Converter<Record, Record> converter) {
    this.predicate = filter;
    this.converter = converter;
  }

  public Converter<Record, Record> getConverter() {
    return this.converter;
  }

  public Predicate<Record> getFilter() {
    return this.predicate;
  }

  @Override
  public String toString() {
    return "filter=" + this.predicate + "\nconverter=" + this.converter;
  }
}
