package com.revolsys.io;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Supplier;

public class CloseableResourceProxy<R extends BaseCloseable> implements BaseCloseable {

  private class CloseableResourceHandler implements InvocationHandler {

    private int referenceCount = 1;

    private R resource;

    private CloseableResourceHandler(final R resource) {
      this.resource = resource;
    }

    private void disconnect() {
      final R resourceToClose;
      synchronized (CloseableResourceHandler.this) {
        this.referenceCount--;
        if (this.referenceCount <= 0) {
          CloseableResourceProxy.this.resourceHandler = null;
          CloseableResourceProxy.this.resourceProxy = null;
          resourceToClose = this.resource;
          synchronized (this) {
            this.resource = null;
          }
          this.referenceCount = 0;
        } else {
          resourceToClose = null;
        }
      }
      if (resourceToClose != null) {
        resourceToClose.close();
      }
    }

    private void increment() {
      this.referenceCount++;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args)
      throws Throwable {
      final R resource;
      synchronized (this) {
        resource = this.resource;
      }
      if (resource == null) {
        throw new IllegalStateException("Resource is closed");
      } else if (args == null && "close".equals(method.getName())) {
        disconnect();
        return null;
      } else {
        return method.invoke(resource, args);
      }
    }

    @SuppressWarnings("unchecked")
    private R newProxy() {
      final Class<?> resourceClass = this.resource.getClass();
      final ClassLoader classLoader = resourceClass.getClassLoader();
      final Class<?>[] resourceInterfaces = resourceClass.getInterfaces();
      return (R)Proxy.newProxyInstance(classLoader, resourceInterfaces, this);
    }

    private void resourceClose() {
      final R resourceToClose;
      synchronized (CloseableResourceHandler.this) {
        resourceToClose = this.resource;
        this.resource = null;
        this.referenceCount = 0;
      }
      if (resourceToClose != null) {
        resourceToClose.close();
      }
    }
  }

  public static <RS extends BaseCloseable> CloseableResourceProxy<RS> newProxy(
    final Supplier<RS> resourceFactory) {
    return new CloseableResourceProxy<>(resourceFactory);
  }

  private Supplier<R> resourceFactory;

  private CloseableResourceHandler resourceHandler;

  private R resourceProxy;

  public CloseableResourceProxy(final Supplier<R> resourceFactory) {
    this.resourceFactory = resourceFactory;
  }

  @Override
  public void close() {
    final CloseableResourceHandler handler;
    synchronized (this) {
      handler = this.resourceHandler;
      this.resourceFactory = null;
      this.resourceHandler = null;
    }
    if (handler != null) {
      handler.resourceClose();
    }
  }

  @Override
  protected void finalize() throws Throwable {
    close();
  }

  public synchronized R getResource() {
    if (this.resourceFactory == null) {
      throw new IllegalStateException("Resource closed");
    } else {
      if (this.resourceHandler == null) {
        final R resource = this.resourceFactory.get();
        if (resource == null) {
          return null;
        } else {
          this.resourceHandler = new CloseableResourceHandler(resource);
          this.resourceProxy = this.resourceHandler.newProxy();
        }
      } else {
        this.resourceHandler.increment();
      }
    }
    return this.resourceProxy;
  }

}