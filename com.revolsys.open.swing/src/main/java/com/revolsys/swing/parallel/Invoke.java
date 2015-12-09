package com.revolsys.swing.parallel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import com.revolsys.beans.MethodInvoker;
import com.revolsys.collection.list.Lists;
import com.revolsys.collection.map.Maps;
import com.revolsys.parallel.ThreadInterruptedException;
import com.revolsys.parallel.process.InvokeMethodRunnable;
import com.revolsys.transaction.Propagation;
import com.revolsys.transaction.Transaction;
import com.revolsys.util.ExceptionUtil;
import com.revolsys.util.Property;

public class Invoke {

  private static PropertyChangeListener PROPERTY_CHANGE_LISTENER = new PropertyChangeListener() {
    @Override
    public synchronized void propertyChange(final PropertyChangeEvent event) {
      final SwingWorker<?, ?> worker = (SwingWorker<?, ?>)event.getSource();
      if (worker.isCancelled() || worker.isDone()) {
        try {
          final List<SwingWorker<?, ?>> oldWorkers;
          List<SwingWorker<?, ?>> newWorkers;
          synchronized (WORKERS) {
            oldWorkers = getWorkers();
            WORKERS.remove(worker);
            if (worker instanceof MaxThreadsSwingWorker) {
              final MaxThreadsSwingWorker maxThreadsWorker = (MaxThreadsSwingWorker)worker;
              final String workerKey = maxThreadsWorker.getWorkerKey();
              final int maxThreads = maxThreadsWorker.getMaxThreads();
              int threads = Maps.decrementCount(WORKER_COUNTS, workerKey);
              final List<SwingWorker<?, ?>> waitingWorkers = WAITING_WORKERS.get(workerKey);
              while (Property.hasValue(waitingWorkers) && threads < maxThreads) {
                final SwingWorker<?, ?> nextWorker = waitingWorkers.remove(0);
                Maps.addCount(WORKER_COUNTS, workerKey);
                nextWorker.execute();
                threads++;
              }
            }
            for (final Iterator<SwingWorker<?, ?>> iterator = WORKERS.iterator(); iterator
              .hasNext();) {
              final SwingWorker<?, ?> swingWorker = iterator.next();
              if (swingWorker.isDone()) {
                iterator.remove();
              }
            }
            newWorkers = getWorkers();
          }
          PROPERTY_CHANGE_SUPPORT.firePropertyChange("workers", oldWorkers, newWorkers);
        } finally {
          worker.removePropertyChangeListener(this);
        }
      }
    }
  };

  private static final PropertyChangeSupport PROPERTY_CHANGE_SUPPORT = new PropertyChangeSupport(
    Invoke.class);

  private static final List<SwingWorker<?, ?>> WORKERS = new LinkedList<>();

  private static final Map<String, List<SwingWorker<?, ?>>> WAITING_WORKERS = new HashMap<>();

  private static final Map<String, Integer> WORKER_COUNTS = new HashMap<>();

  public static void andWait(final Runnable runnable) {
    if (SwingUtilities.isEventDispatchThread()) {
      runnable.run();
    } else {
      try {
        SwingUtilities.invokeAndWait(runnable);
      } catch (final InterruptedException e) {
        throw new ThreadInterruptedException(e);
      } catch (final InvocationTargetException e) {
        ExceptionUtil.throwCauseException(e);
      }
    }
  }

  public static void background(final Runnable backgroundTask) {
    worker(new RunnableSwingWorker(backgroundTask));
  }

  public static void background(final String description, final Object object, final Method method,
    final Object... parameters) {
    final MethodInvoker backgroundTask = new MethodInvoker(method, object, parameters);
    background(description, backgroundTask);
  }

  public static SwingWorker<?, ?> background(final String description, final Object object,
    final String backgroundMethodName, final List<Object> parameters) {
    final SwingWorker<?, ?> worker = new InvokeMethodSwingWorker<Object, Object>(description,
      object, backgroundMethodName, parameters);
    worker(worker);
    return worker;
  }

  public static SwingWorker<?, ?> background(final String description, final Object object,
    final String backgroundMethodName, final Object... parameters) {
    final SwingWorker<?, ?> worker = new InvokeMethodSwingWorker<Object, Object>(description,
      object, backgroundMethodName, Arrays.asList(parameters));
    worker(worker);
    return worker;
  }

  public static void background(final String description, final Runnable backgroundTask) {
    worker(new RunnableSwingWorker(description, backgroundTask));
  }

  public static void backgroundTransaction(final String description,
    final PlatformTransactionManager transactionManager, final Propagation propagation,
    final Runnable runnable) {
    background(description, Transaction.runnable(runnable, transactionManager, propagation));
  }

  public static PropertyChangeSupport getPropertyChangeSupport() {
    return PROPERTY_CHANGE_SUPPORT;
  }

  public static List<SwingWorker<?, ?>> getWorkers() {
    return Lists.array(WORKERS);
  }

  public static void later(final Object object, final Method method, final Object... parameters) {
    later(new Runnable() {

      @Override
      public void run() {
        try {
          method.invoke(object, parameters);
        } catch (final InvocationTargetException e) {
          LoggerFactory.getLogger(getClass()).error(
            "Error invoking method " + method + " " + Arrays.toString(parameters),
            e.getTargetException());
        } catch (final Throwable e) {
          LoggerFactory.getLogger(getClass())
            .error("Error invoking method " + method + " " + Arrays.toString(parameters), e);
        }
      }
    });
  }

  public static void later(final Object object, final String methodName,
    final Object... parameters) {
    final InvokeMethodRunnable runnable = new InvokeMethodRunnable(object, methodName, parameters);
    later(runnable);
  }

  public static void later(final Runnable runnable) {
    if (SwingUtilities.isEventDispatchThread()) {
      runnable.run();
    } else {
      SwingUtilities.invokeLater(runnable);
    }
  }

  public static SwingWorker<?, ?> worker(final String description, final Object object,
    final String backgroundMethodName, final Collection<? extends Object> backgrounMethodParameters,
    final String doneMethodName, final Collection<? extends Object> doneMethodParameters) {
    final SwingWorker<?, ?> worker = new InvokeMethodSwingWorker<Object, Object>(description,
      object, backgroundMethodName, backgrounMethodParameters, doneMethodName,
      doneMethodParameters);
    worker(worker);
    return worker;
  }

  public static void worker(final SwingWorker<? extends Object, ? extends Object> worker) {
    boolean execute = true;
    final List<SwingWorker<?, ?>> oldWorkers;
    final List<SwingWorker<?, ?>> newWorkers;
    synchronized (WORKERS) {
      if (WORKERS.contains(worker)) {
        return;
      }
      oldWorkers = getWorkers();
      WORKERS.add(worker);
      if (worker instanceof MaxThreadsSwingWorker) {
        final MaxThreadsSwingWorker maxThreadsWorker = (MaxThreadsSwingWorker)worker;
        final String workerKey = maxThreadsWorker.getWorkerKey();
        final int maxThreads = maxThreadsWorker.getMaxThreads();
        final int threads = Maps.getCount(WORKER_COUNTS, workerKey);
        if (threads >= maxThreads) {
          execute = false;
          Maps.addToList(WAITING_WORKERS, workerKey, worker);
        } else {
          Maps.addCount(WORKER_COUNTS, workerKey);
        }
      }
      newWorkers = getWorkers();
    }
    worker.addPropertyChangeListener(PROPERTY_CHANGE_LISTENER);
    PROPERTY_CHANGE_SUPPORT.firePropertyChange("workers", oldWorkers, newWorkers);
    if (execute) {
      worker.execute();
    }
  }

  private Invoke() {
  }

}
