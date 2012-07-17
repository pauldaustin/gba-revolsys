package com.revolsys.ui.model;

import java.util.Locale;

public class DocInfo {
  private Locale locale;

  private String title;

  private String description;

  private boolean html = false;

  public void setHtml(boolean html) {
    this.html = html;
  }

  public boolean isHtml() {
    return html;
  }

  public DocInfo() {
  }

  public DocInfo(final DocInfo docInfo) {
    this.locale = docInfo.getLocale();
    this.title = docInfo.getTitle();
    this.description = docInfo.getDescription();
  }

  public DocInfo(final Locale locale, final String title,
    final String description) {
    this.locale = locale;
    this.title = title;
    this.description = description;
  }

  public DocInfo(final String title) {
    this.title = title;
  }

  public DocInfo(final String title, final String description) {
    this.title = title;
    this.description = description;
  }

  @Override
  public DocInfo clone() {
    return new DocInfo(this);
  }

  public String getDescription() {
    return description;
  }

  public Locale getLocale() {
    return locale;
  }

  public String getTitle() {
    return title;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public void setTitle(final String title) {
    this.title = title;
  }
}
