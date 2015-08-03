package org.knime.ext.jruby

import javax.swing.table._
import java.util._
//remove if not needed
import scala.collection.JavaConversions._

@SerialVersionUID(3748218863796706007L)
class ScriptNodeOutputColumnsTableModel extends AbstractTableModel {

  private var data: ArrayList[ArrayList[Any]] = new ArrayList[ArrayList[Any]]()

  private var columnNames: ArrayList[String] = new ArrayList[String]()

  private var m_readOnly: Boolean = false

  override def getColumnName(col: Int): String = columnNames.get(col).toString

  def getRowCount(): Int = data.size

  def getColumnCount(): Int = columnNames.size

  def getValueAt(row: Int, col: Int): AnyRef = {
    val rowList = data.get(row)
    rowList.get(col).asInstanceOf[AnyRef]
  }

  override def isCellEditable(row: Int, col: Int): Boolean = !m_readOnly

  def setReadOnly(readOnly: Boolean) {
    m_readOnly = readOnly
  }

  override def setValueAt(value: AnyRef, row: Int, col: Int) {
    val rowList = data.get(row)
    rowList.set(col, value)
    fireTableCellUpdated(row, col)
  }

  def addRow(dataTableColumnName: AnyRef, dataTableColumnType: AnyRef) {
    val row = new ArrayList[Any]()
    row.add(dataTableColumnName)
    row.add(dataTableColumnType)
    data.add(row)
    val rowNum = data.size - 1
    fireTableRowsInserted(rowNum, rowNum)
  }

  def removeRow(row: Int) {
    data.remove(row)
    fireTableRowsDeleted(row, row)
  }

  def addColumn(columnName: String) {
    columnNames.add(columnName)
  }

  def getDataTableColumnNames(): Array[String] = getDataTableValues(0)

  def getDataTableColumnTypes(): Array[String] = getDataTableValues(1)

  private def getDataTableValues(colIndex: Int): Array[String] = {
//    val dataTableColumnValues = Array.ofDim[String](data.size)
//    var rowNum = 0
//    for (row <- data) {
//      dataTableColumnValues(rowNum) = row.get(colIndex).asInstanceOf[String]
//      rowNum += 1
//    }
//    dataTableColumnValues
    
     data.map(_.get(colIndex).asInstanceOf[String]).toArray
  }

  def clearRows() {
    data = new ArrayList[ArrayList[Any]]()
  }

  def moveRowsUp(rows: Array[Int]) {
    for (j <- 0 until rows.length if rows(j) != 0)
      Collections.swap(data, rows(j), rows(j) - 1)
    fireTableDataChanged()
  }

  def moveRowsDown(rows: Array[Int]) {
    for (j <- rows.length - 1 to 0) {
      if (rows(j) != data.size - 1) Collections.swap(data, rows(j), rows(j) + 1)
    }
    fireTableDataChanged()
  }

/*
Original Java:
package org.knime.ext.jruby;

import javax.swing.table.*;
import java.util.*;

public class ScriptNodeOutputColumnsTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 3748218863796706007L;
    private ArrayList<ArrayList<Object>> data = new ArrayList<ArrayList<Object>>();
    private ArrayList<String> columnNames = new ArrayList<String>();
    private boolean m_readOnly = false;

    public final String getColumnName(int col) {
        return columnNames.get(col).toString();
    }

    public final int getRowCount() {
        return data.size();
    }

    public final int getColumnCount() {
        return columnNames.size();
    }

    public final Object getValueAt(int row, int col) {

        ArrayList<Object> rowList = data.get(row);
        return rowList.get(col);
    }

    public final boolean isCellEditable(int row, int col) {
        return !m_readOnly;
    }

    public final void setReadOnly(boolean readOnly) {
        m_readOnly = readOnly;
    }

    public final void setValueAt(Object value, int row, int col) {
        ArrayList<Object> rowList = data.get(row);
        rowList.set(col, value);
        fireTableCellUpdated(row, col);
    }

    public final void addRow(final Object dataTableColumnName,
            final Object dataTableColumnType) {
        ArrayList<Object> row = new ArrayList<Object>();
        row.add(dataTableColumnName);
        row.add(dataTableColumnType);

        data.add(row);

        int rowNum = data.size() - 1;
        fireTableRowsInserted(rowNum, rowNum);
    }

    public final void removeRow(final int row) {
        data.remove(row);
        fireTableRowsDeleted(row, row);
    }

    public final void addColumn(final String columnName) {
        columnNames.add(columnName);
    }

    public final String[] getDataTableColumnNames() {
        return getDataTableValues(0);
    }

    public final String[] getDataTableColumnTypes() {
        return getDataTableValues(1);
    }

    private String[] getDataTableValues(final int colIndex) {
        String[] dataTableColumnValues = new String[data.size()];

        Iterator<ArrayList<Object>> i = data.iterator();
        int rowNum = 0;
        while (i.hasNext()) {
            ArrayList<Object> row = i.next();
            dataTableColumnValues[rowNum] = (String) row.get(colIndex);
            rowNum++;
        }
        return dataTableColumnValues;
    }

    public final void clearRows() {
        data = new ArrayList<ArrayList<Object>>();
    }

    public final void moveRowsUp(int[] rows) {
        for (int j = 0; j < rows.length; j++) {
            if (rows[j] != 0)
                Collections.swap(data, rows[j], rows[j] - 1);
        }
        fireTableDataChanged();
    }

    public final void moveRowsDown(int[] rows) {
        for (int j = rows.length - 1; j >= 0; j--) {
            if (rows[j] != data.size() - 1)
                Collections.swap(data, rows[j], rows[j] + 1);
        }
        fireTableDataChanged();
    }
}

*/
}