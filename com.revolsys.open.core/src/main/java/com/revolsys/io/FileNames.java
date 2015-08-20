package com.revolsys.io;

import java.util.ArrayList;
import java.util.List;

public class FileNames {

  public static String getBaseName(final String fileName) {
    int endIndex = fileName.length();
    int slashIndex;
    for (slashIndex = fileName.lastIndexOf("/", endIndex - 1); slashIndex != -1
      && slashIndex == endIndex - 1; slashIndex = fileName.lastIndexOf("/", endIndex - 1)) {
      endIndex--;
    }
    final int dotIndex = fileName.lastIndexOf('.', endIndex - 1);
    if (dotIndex == -1) {
      if (slashIndex == -1) {
        return fileName.substring(0, endIndex);
      } else if (slashIndex == fileName.length() - 1) {
        return "";
      } else {
        return fileName.substring(slashIndex + 1, endIndex);
      }
    } else {
      if (slashIndex == -1) {
        return fileName.substring(0, dotIndex);
      } else if (slashIndex > dotIndex) {
        if (slashIndex == fileName.length() - 1) {
          return "";
        } else {
          return fileName.substring(slashIndex + 1, endIndex);
        }
      } else {
        return fileName.substring(slashIndex + 1, dotIndex);
      }
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
