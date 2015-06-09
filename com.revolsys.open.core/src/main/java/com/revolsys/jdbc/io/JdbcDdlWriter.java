package com.revolsys.jdbc.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.revolsys.util.Property;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.property.ShortNameProperty;
import com.revolsys.data.record.schema.FieldDefinition;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.io.Path;
import com.revolsys.util.CollectionUtil;
import com.revolsys.util.MathUtil;

public abstract class JdbcDdlWriter implements Cloneable {
  private PrintWriter out;

  public JdbcDdlWriter() {
  }

  public JdbcDdlWriter(final PrintWriter out) {
    this.out = out;
  }

  @Override
  public JdbcDdlWriter clone() {
    try {
      return (JdbcDdlWriter)super.clone();
    } catch (final CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  public JdbcDdlWriter clone(final File file) {
    final JdbcDdlWriter clone = clone();
    clone.setOut(file);
    return clone;
  }

  public void close() {
    this.out.flush();
    this.out.close();
  }

  public PrintWriter getOut() {
    return this.out;
  }

  public String getSequenceName(final RecordDefinition metaData) {
    throw new UnsupportedOperationException();
  }

  public String getTableAlias(final RecordDefinition metaData) {
    final String shortName = ShortNameProperty.getShortName(metaData);
    if (shortName == null) {
      final String path = metaData.getPath();
      return Path.getName(path);
    } else {
      return shortName;
    }
  }

  public void println() {
    this.out.println();
  }

  public void setOut(final File file) {
    try {
      final FileWriter writer = new FileWriter(file);
      this.out = new PrintWriter(writer);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void setOut(final PrintWriter out) {
    this.out = out;
  }

  public void writeAddForeignKeyConstraint(final RecordDefinition metaData,
    final String attributeName, final RecordDefinition referencedMetaData) {
    final String typePath = metaData.getPath();
    final String referencedTypeName = referencedMetaData.getPath();
    final String referencedAttributeName = referencedMetaData.getIdFieldName();
    final String constraintName = getTableAlias(metaData) + "_" + getTableAlias(referencedMetaData)
      + "_FK";
    writeAddForeignKeyConstraint(typePath, constraintName, attributeName, referencedTypeName,
      referencedAttributeName);
  }

  public void writeAddForeignKeyConstraint(final RecordDefinition metaData,
    final String attributeName, final String referenceTablePrefix,
    final RecordDefinition referencedMetaData) {
    final String typePath = metaData.getPath();
    final String referencedTypeName = referencedMetaData.getPath();
    final String referencedAttributeName = referencedMetaData.getIdFieldName();
    final String constraintName = getTableAlias(metaData) + "_" + referenceTablePrefix + "_"
      + getTableAlias(referencedMetaData) + "_FK";
    writeAddForeignKeyConstraint(typePath, constraintName, attributeName, referencedTypeName,
      referencedAttributeName);
  }

  public void writeAddForeignKeyConstraint(final String typePath, final String constraintName,
    final String attributeName, final String referencedTypeName,
    final String referencedAttributeName) {
    this.out.print("ALTER TABLE ");
    writeTableName(typePath);
    this.out.print(" ADD CONSTRAINT ");
    this.out.print(constraintName);
    this.out.print(" FOREIGN KEY (");
    this.out.print(attributeName);
    this.out.print(") REFERENCES ");
    writeTableName(referencedTypeName);
    this.out.print(" (");
    this.out.print(referencedAttributeName);
    this.out.println(");");
  }

  public void writeAddPrimaryKeyConstraint(final RecordDefinition metaData) {
    final String idAttributeName = metaData.getIdFieldName();
    if (idAttributeName != null) {
      final String typePath = metaData.getPath();
      final String constraintName = getTableAlias(metaData) + "_PK";
      writeAddPrimaryKeyConstraint(typePath, constraintName, idAttributeName);
    }
  }

  public void writeAddPrimaryKeyConstraint(final String typePath, final String constraintName,
    final String columnName) {
    this.out.print("ALTER TABLE ");
    writeTableName(typePath);
    this.out.print(" ADD CONSTRAINT ");
    this.out.print(constraintName);
    this.out.print(" PRIMARY KEY (");
    this.out.print(columnName);
    this.out.println(");");
  }

  public abstract void writeColumnDataType(final FieldDefinition attribute);

  public void writeCreateSchema(final String schemaName) {
  }

  public String writeCreateSequence(final RecordDefinition metaData) {
    final String sequenceName = getSequenceName(metaData);
    writeCreateSequence(sequenceName);
    return sequenceName;
  }

  public void writeCreateSequence(final String sequenceName) {
    this.out.print("CREATE SEQUENCE ");
    this.out.print(sequenceName);
    this.out.println(";");
  }

  public void writeCreateTable(final RecordDefinition metaData) {
    final String typePath = metaData.getPath();
    this.out.println();
    this.out.print("CREATE TABLE ");
    writeTableName(typePath);
    this.out.println(" (");
    for (int i = 0; i < metaData.getFieldCount(); i++) {
      final FieldDefinition attribute = metaData.getField(i);
      if (i > 0) {
        this.out.println(",");
      }
      final String name = attribute.getName();
      this.out.print("  ");
      this.out.print(name);
      for (int j = name.length(); j < 32; j++) {
        this.out.print(' ');
      }
      writeColumnDataType(attribute);
      if (attribute.isRequired()) {
        this.out.print(" NOT NULL");
      }
    }
    this.out.println();
    this.out.println(");");

    writeAddPrimaryKeyConstraint(metaData);

    writeGeometryMetaData(metaData);

    final FieldDefinition idAttribute = metaData.getIdField();
    if (idAttribute != null) {
      if (Number.class.isAssignableFrom(idAttribute.getType().getJavaClass())) {
        writeCreateSequence(metaData);
      }
    }
  }

  public void writeCreateView(final String typePath, final String queryTypeName,
    final List<String> columnNames) {
    this.out.println();
    this.out.print("CREATE VIEW ");
    writeTableName(typePath);
    this.out.println(" AS ( ");
    this.out.println("  SELECT ");
    this.out.print("  ");
    this.out.println(CollectionUtil.toString(",\n  ", columnNames));
    this.out.print("  FROM ");
    writeTableName(queryTypeName);
    this.out.println();
    this.out.println(");");
  }

  public abstract void writeGeometryMetaData(final RecordDefinition metaData);

  public void writeGrant(final String typePath, final String username, final boolean select,
    final boolean insert, final boolean update, final boolean delete) {

    this.out.print("GRANT ");
    final List<String> perms = new ArrayList<String>();
    if (select) {
      perms.add("SELECT");
    }
    if (insert) {
      perms.add("INSERT");
    }
    if (update) {
      perms.add("UPDATE");
    }
    if (delete) {
      perms.add("DELETE");
    }
    this.out.print(CollectionUtil.toString(", ", perms));
    this.out.print(" ON ");
    writeTableName(typePath);
    this.out.print(" TO ");
    this.out.print(username);
    this.out.println(";");

  }

  public void writeInsert(final Record row) {
    final RecordDefinition metaData = row.getRecordDefinition();
    final String typePath = metaData.getPath();
    this.out.print("INSERT INTO ");
    writeTableName(typePath);
    this.out.print(" (");
    for (int i = 0; i < metaData.getFieldCount(); i++) {
      if (i > 0) {
        this.out.print(", ");
      }
      this.out.print(metaData.getFieldName(i));
    }
    this.out.print(" ) VALUES (");
    for (int i = 0; i < metaData.getFieldCount(); i++) {
      if (i > 0) {
        this.out.print(", ");
      }
      final Object value = row.getValue(i);
      if (value == null) {
        this.out.print("NULL");
      } else if (value instanceof Number) {
        final Number number = (Number)value;
        this.out.print(MathUtil.toString(number));
      } else {
        this.out.print("'");
        this.out.print(value.toString().replaceAll("'", "''"));
        this.out.print("'");
      }
    }
    this.out.println(");");

  }

  public void writeInserts(final List<Record> rows) {
    for (final Record row : rows) {
      writeInsert(row);
    }

  }

  public void writeResetSequence(final RecordDefinition metaData, final List<Record> values) {
    throw new UnsupportedOperationException();
  }

  public void writeTableName(final String typePath) {
    final String schemaName = Path.getPath(typePath).substring(1);
    final String tableName = Path.getName(typePath);
    writeTableName(schemaName, tableName);
  }

  public void writeTableName(final String schemaName, final String tableName) {
    if (Property.hasValue(schemaName)) {
      this.out.print(schemaName);
      this.out.print('.');
    }
    this.out.print(tableName);
  }
}
