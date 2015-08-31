package com.revolsys.spring.resource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Path;

import com.revolsys.io.FileNames;
import com.revolsys.io.FileUtil;
import com.revolsys.util.Property;
import com.revolsys.util.WrappedException;

public interface Resource extends org.springframework.core.io.Resource {

  static String CLASSPATH_URL_PREFIX = "classpath:";

  static boolean exists(final Resource resource) {
    if (resource == null) {
      return false;
    } else {
      return resource.exists();
    }
  }

  static org.springframework.core.io.Resource getResource(final Object source) {
    org.springframework.core.io.Resource resource;
    if (source instanceof org.springframework.core.io.Resource) {
      resource = (org.springframework.core.io.Resource)source;
    } else if (source instanceof Path) {
      resource = new PathResource((Path)source);
    } else if (source instanceof File) {
      resource = new FileSystemResource((File)source);
    } else if (source instanceof URL) {
      resource = new UrlResource((URL)source);
    } else if (source instanceof URI) {
      resource = new UrlResource((URI)source);
    } else if (source instanceof String) {
      return SpringUtil.getResource((String)source);
    } else {
      throw new IllegalArgumentException(source.getClass() + " is not supported");
    }
    return resource;
  }

  static Resource getResource(final String location) {
    if (Property.hasValue(location)) {
      if (location.charAt(0) == '/' || location.length() > 1 && location.charAt(1) == ':') {
        return new PathResource(location);
      } else if (location.startsWith(CLASSPATH_URL_PREFIX)) {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final String path = location.substring(CLASSPATH_URL_PREFIX.length());
        return new ClassPathResource(path, classLoader);
      } else {
        return new UrlResource(location);
      }
    }
    return null;
  }

  default String contentsAsString() {
    final Reader reader = newReader();
    return FileUtil.getString(reader);
  }

  default Resource createAddExtension(final String extension) {
    final String fileName = getFilename();
    final String newFileName = fileName + "." + extension;
    final Resource parent = getParent();
    if (parent == null) {
      return null;
    } else {
      return parent.createChild(newFileName);
    }
  }

  default Resource createChangeExtension(final String extension) {
    final String baseName = getBaseName();
    final String newFileName = baseName + "." + extension;
    final Resource parent = getParent();
    if (parent == null) {
      return null;
    } else {
      return parent.createChild(newFileName);
    }
  }

  default Resource createChild(final CharSequence childPath) {
    return createRelative(childPath.toString());
  }

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

  @Override
  InputStream getInputStream();

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

  default InputStream newBufferedInputStream() {
    final InputStream in = newInputStream();
    return new BufferedInputStream(in);
  }

  default OutputStream newBufferedOutputStream() {
    final OutputStream out = newOutputStream();
    return new BufferedOutputStream(out);
  }

  default BufferedReader newBufferedReader() {
    final Reader in = newReader();
    return new BufferedReader(in);
  }

  default InputStream newInputStream() {
    return getInputStream();
  }

  default OutputStream newOutputStream() {
    try {
      final URL url = getURL();
      final String protocol = url.getProtocol();
      if (protocol.equals("file") || protocol.equals("folderconnection")) {
        final File file = getFile();
        return new FileOutputStream(file);
      } else {
        final URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        return connection.getOutputStream();
      }
    } catch (final IOException e) {
      throw new WrappedException(e);
    }
  }

  default PrintWriter newPrintWriter() {
    final Writer writer = newWriter();
    return new PrintWriter(writer);
  }

  default Reader newReader() {
    final InputStream in = getInputStream();
    return FileUtil.createUtf8Reader(in);
  }

  default Writer newWriter() {
    final OutputStream stream = newOutputStream();
    return FileUtil.createUtf8Writer(stream);
  }

  default Writer newWriter(final Charset charset) {
    final OutputStream stream = newOutputStream();
    return new OutputStreamWriter(stream, charset);
  }
}
