package com.revolsys.parallel.process;

import com.revolsys.parallel.channel.Channel;
import java.util.function.Predicate;

public class FilterProcess<T> extends BaseInOutProcess<T, T> {
  private Predicate<T> predicate;

  private boolean invert = false;

  public Predicate<T> getFilter() {
    return this.predicate;
  }

  public boolean isInvert() {
    return this.invert;
  }

  protected void postAccept(final T object) {
  }

  protected void postReject(final T object) {
  }

  @Override
  protected void process(final Channel<T> in, final Channel<T> out, final T object) {
    boolean accept = this.predicate.test(object);
    if (this.invert) {
      accept = !accept;
    }
    if (accept) {
      out.write(object);
      postAccept(object);
    } else {
      postReject(object);
    }
  }

  public void setFilter(final Predicate<T> filter) {
    this.predicate = filter;
  }

  public void setInvert(final boolean invert) {
    this.invert = invert;
  }

}
