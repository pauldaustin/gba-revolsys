package com.revolsys.io;

import java.util.List;
import java.util.Set;

public interface IoFactory {
  static <C extends IoFactory> C factory(final Class<C> factoryClass, final Object source) {
    final IoFactoryRegistry registry = IoFactoryRegistry.getInstance();
    return registry.getFactory(factoryClass, source);
  }

  static <C extends IoFactory> boolean hasFactory(final Class<C> factoryClass,
    final Object source) {
    final C factory = factory(factoryClass, source);
    return factory != null;
  }

  String getFileExtension(String mediaType);

  List<String> getFileExtensions();

  String getMediaType(String fileExtension);

  Set<String> getMediaTypes();

  String getName();

  void init();

  boolean isAvailable();
}
