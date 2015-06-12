package com.revolsys.util;

public class Strings {

  public static boolean contains(final String text, final String matchText) {
    if (text == null || matchText == null) {
      return false;
    } else {
      return text.contains(matchText);
    }
  }

  public static boolean endsWith(final String text, final String suffix) {
    if (text != null && Property.hasValue(suffix)) {
      return text.endsWith(suffix);
    } else {
      return false;
    }
  }

  public static boolean equalExceptOneCharacter(final String string1, final String string2) {
    final int length1 = string1.length();
    if (length1 != string2.length()) {
      return false;
    } else {
      boolean equal = true;
      for (int i = 0; i < length1; ++i) {
        if (string1.charAt(i) != string2.charAt(i)) {
          if (equal) {
            equal = false;
          } else {
            return false;
          }
        }
      }
      return true;
    }
  }

  public static boolean equalExceptOneExtraCharacter(final String string1, final String string2) {
    final int length1 = string1.length();
    final int length2 = string2.length();
    if (length1 == length2) {
      return string1.equals(string2);
    } else {
      if (length1 == length2 + 1) {
        return equalExceptOneExtraCharacter(string2, string1);
      }
      if (length2 == length1 + 1) {
        int matchCount = 0;
        for (int i = 0; i < length1; i++) {
          if (string1.charAt(i) == string2.charAt(i)) {
            matchCount++;
          } else {
            break;
          }
        }
        for (int i = 1; i <= length1; i++) {
          final char c1 = string1.charAt(length1 - i);
          final char c2 = string2.charAt(length2 - i);
          if (c1 == c2) {
            matchCount++;
          } else {
            break;
          }
        }
        return matchCount == length1;
      } else {
        return false;
      }
    }
  }

  public static String firstPart(final String text, final char character) {
    final int index = text.indexOf(character);
    if (index == -1) {
      return "";
    } else {
      return text.substring(0, index);
    }
  }

  public static String lastPart(final String text, final char character) {
    final int index = text.lastIndexOf(character);
    if (index == -1) {
      return "";
    } else {
      return text.substring(0, index);
    }
  }

  public static String lowerCase(final String text) {
    if (text == null) {
      return null;
    } else {
      return text.toLowerCase();
    }
  }

  public static boolean matches(final String text, final String regex) {
    if (text == null || regex == null) {
      return false;
    } else {
      return text.matches(regex);
    }
  }

  public static String substring(final String text, final char character, final int toIndex) {
    int startIndex = 0;
    for (int i = 0; i < toIndex && startIndex != -1; i++) {
      final int index = text.indexOf(character, startIndex);
      if (index == -1) {
        return "";
      }
      startIndex = index + 1;
    }
    if (startIndex == -1) {
      return text;
    } else {
      return text.substring(startIndex);
    }
  }

  public static String substring(final String text, final char character, final int fromIndex,
    final int toIndex) {
    if (fromIndex < 0) {
      throw new StringIndexOutOfBoundsException(fromIndex);
    } else if (toIndex < 0) {
      throw new StringIndexOutOfBoundsException(toIndex);
    }
    int startIndex = 0;
    for (int i = 0; i < fromIndex && startIndex != -1; i++) {
      final int index = text.indexOf(character, startIndex);
      if (index == -1) {
        return "";
      }
      startIndex = index + 1;
    }
    int endIndex = startIndex;
    for (int i = fromIndex; i < toIndex && endIndex != -1; i++) {
      if (i > fromIndex) {
        endIndex++;
      }
      final int index = text.indexOf(character, endIndex);
      if (index == -1) {
        return text.substring(startIndex);
      } else {
        endIndex = index;
      }
    }
    if (endIndex == -1) {
      return "";
    } else {
      return text.substring(startIndex, endIndex);
    }
  }

  public static String trim(final String text) {
    if (text == null) {
      return null;
    } else {
      return text.trim();
    }
  }

  public static String upperCase(final String text) {
    if (text == null) {
      return null;
    } else {
      return text.toUpperCase();
    }
  }
}