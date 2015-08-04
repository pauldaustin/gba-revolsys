package com.revolsys.spring.resource;

import com.revolsys.io.FileNames;

public interface Resource extends org.springframework.core.io.Resource {

  @Override
  Resource createRelative(String relativePath);

  default String getBaseName() {
    final String filename = getFilename();
    return FileNames.getBaseName(filename);
  }

  default String getFileNameExtension(final Resource resource) {
    final String filename = resource.getFilename();
    return FileNames.getFileNameExtension(filename);
  }

  default Resource getParent() {
    return null;
  }

  default Resource getResourceWithExtension(final String extension) {
    final String baseName = getBaseName();
    final String newFileName = baseName + "." + extension;
    final Resource parent = getParent();
    if (parent == null) {
      return null;
    } else {
      return parent.createRelative(newFileName);
    }
  }
}
