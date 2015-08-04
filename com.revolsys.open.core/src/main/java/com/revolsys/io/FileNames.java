package com.revolsys.io;

import java.util.ArrayList;
import java.util.List;

public class FileNames {

  public static String getBaseName(final String fileName) {
    final int slashIndex = fileName.lastIndexOf("/");
    final int dotIndex = fileName.lastIndexOf('.');
    if (slashIndex > dotIndex) {
      return fileName.substring(slashIndex + 1);
    }
    if (dotIndex != -1) {
      return fileName.substring(slashIndex + 1, dotIndex);
    } else {
      return fileName;
    }
  }

  public static String getFileNameExtension(final String fileName) {
    int slashIndex = fileName.lastIndexOf("/");
    final int dotIndex = fileName.lastIndexOf('.');
    if (slashIndex == -1) {
      slashIndex = 0;
    } else if (slashIndex > dotIndex) {
      return "";
    }
    if (dotIndex != -1) {
      return fileName.substring(dotIndex + 1);
    } else {
      return "";
    }
  }

  public static List<String> getFileNameExtensions(final String fileName) {
    final List<String> extensions = new ArrayList<>();
    int startIndex = fileName.lastIndexOf("/");
    if (startIndex == -1) {
      startIndex = 0;
    }
    for (int dotIndex = fileName.indexOf('.', startIndex); dotIndex > 0; dotIndex = fileName
      .indexOf('.', startIndex)) {
      dotIndex++;
      final String extension = fileName.substring(dotIndex);
      extensions.add(extension);
      startIndex = dotIndex;
    }
    return extensions;
  }

}
