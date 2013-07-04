package com.revolsys.swing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import org.apache.commons.beanutils.MethodUtils;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.revolsys.beans.InvokeMethodCallable;

public class InvokeMethodSwingWorker<T, V> extends SwingWorker<T, V> {
  private Callable<T> backgroundTask;

  private Object object;

  private String doneMethodName;

  private String description;

  private Collection<? extends Object> doneMethodParameters;

  public InvokeMethodSwingWorker(final String description, final Object object,
    final String backgroundMethodName) {
    this(description, object, backgroundMethodName, Collections.emptyList(),
      null, Collections.emptyList());
  }

  public InvokeMethodSwingWorker(final String description, final Object object,
    final String backgroundMethodName,
    final Collection<? extends Object> backgroundMethodParameters,
    final String doneMethodName,
    final Collection<? extends Object> doneMethodParameters) {
    this.description = description;
    this.object = object;
    if (StringUtils.hasText(backgroundMethodName)) {
      this.backgroundTask = new InvokeMethodCallable<T>(object,
        backgroundMethodName, backgroundMethodParameters.toArray());
    }
    if (StringUtils.hasText(doneMethodName)) {
      this.doneMethodName = doneMethodName;
      this.doneMethodParameters = new ArrayList<Object>(doneMethodParameters);
    }
  }

  public InvokeMethodSwingWorker(final String description, final Object object,
    final String backgroundMethodName, final Collection<Object> parameters) {
    this(description, object, backgroundMethodName, parameters, null,
      Collections.emptyList());
  }

  public InvokeMethodSwingWorker(final String description, final Object object,
    final String backgroundMethodName, final String doneMethodName) {
    this(description, object, backgroundMethodName, Collections.emptyList(),
      doneMethodName, Collections.emptyList());
  }

  @Override
  protected T doInBackground() throws Exception {
    if (backgroundTask != null) {
      return backgroundTask.call();
    } else {
      return null;
    }
  }

  @Override
  protected void done() {
    T result = null;
    try {
      if (!isCancelled()) {
        result = get();
      }
    } catch (final InterruptedException e) {
      return;
    } catch (final ExecutionException e) {
      LoggerFactory.getLogger(getClass()).error("Unable to get result", e);
      return;
    }
    if (doneMethodName != null) {
      try {
        final List<Object> parameters = new ArrayList<Object>(
          doneMethodParameters);
        if (result != null) {
          parameters.add(result);
        }
        if (object == null) {
          throw new RuntimeException("Object cannot be null " + this);
        } else if (object instanceof Class<?>) {
          final Class<?> clazz = (Class<?>)object;
          MethodUtils.invokeStaticMethod(clazz, doneMethodName,
            parameters.toArray());
        } else {
          MethodUtils.invokeMethod(object, doneMethodName, parameters.toArray());
        }
      } catch (final Throwable e) {
        LoggerFactory.getLogger(getClass()).error(
          "Unable to invoke done method " + this, e);
      }
    }
  }

  public String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return description;
  }
}
