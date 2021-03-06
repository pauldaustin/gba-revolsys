/* *****************************************************************************
 The Open Java Unified Mapping Platform (OpenJUMP) is an extensible, interactive
 GUI for visualizing and manipulating spatial features with geometry and
 attributes.

 Copyright (C) 2007  Revolution Systems Inc.

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 For more information see:

 http://openjump.org/

 ******************************************************************************/
package com.revolsys.swing.listener;

import java.util.EventObject;

public class ValueChangeEvent extends EventObject {

  /**
   *
   */
  private static final long serialVersionUID = -4574568906845813198L;

  private final Object value;

  public ValueChangeEvent(final Object source, final Object value) {
    super(source);
    this.value = value;
  }

  /**
   * @return the value
   */
  public Object getValue() {
    return this.value;
  }

}
