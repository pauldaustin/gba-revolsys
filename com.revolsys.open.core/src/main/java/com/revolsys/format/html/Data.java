package com.revolsys.format.html;

import com.revolsys.format.xml.XmlWriter;

public class Data {
  public static void parent(final XmlWriter out, final String value) {
    out.attribute("data-parent", value);
  }

  public static void toggle(final XmlWriter out, final String value) {
    out.attribute("data-toggle", value);
  }

}
