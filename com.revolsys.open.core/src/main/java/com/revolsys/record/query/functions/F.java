package com.revolsys.record.query.functions;

import com.revolsys.record.query.Column;
import com.revolsys.record.query.QueryValue;

public class F {
  public static Lower lower(final QueryValue value) {

    return new Lower(value);
  }

  public static RegexpReplace regexpReplace(final QueryValue value, final String pattern,
    final String replace) {
    return new RegexpReplace(value, pattern, replace);
  }

  public static RegexpReplace regexpReplace(final QueryValue value, final String pattern,
    final String replace, final String flags) {
    return new RegexpReplace(value, pattern, replace, flags);
  }

  public static Upper upper(final QueryValue value) {
    return new Upper(value);
  }

  public static Upper upper(final String name) {
    return upper(new Column(name));
  }
}
