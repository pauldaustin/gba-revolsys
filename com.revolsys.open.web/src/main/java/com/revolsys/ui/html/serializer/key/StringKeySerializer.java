package com.revolsys.ui.html.serializer.key;

import com.revolsys.format.xml.XmlWriter;
import com.revolsys.util.JavaBeanUtil;

public class StringKeySerializer extends AbstractKeySerializer {
  public StringKeySerializer() {
  }

  public StringKeySerializer(final String name) {
    super(name);
  }

  public StringKeySerializer(final String name, final String label) {
    super(name);
    setLabel(label);
  }

  /**
   * Serialize the value to the XML writer.
   *
   * @param out The XML writer to serialize to.
   * @param object The object to get the value from.
   */
  @Override
  public void serialize(final XmlWriter out, final Object object) {
    final Object value = JavaBeanUtil.getProperty(object, getName());
    if (value == null) {
      out.text("-");
    } else {
      out.text(value);
    }
  }
}
