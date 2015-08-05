package org.knime.ext.jruby

import javax.swing.table._

import scala.collection.mutable.ArrayBuffer

/**
 * This class realizes a model for the table 
 *   which presents an output columns list    
 * 
 * @author rss
 *
 */
class ScriptNodeOutputColumnsTableModel extends AbstractTableModel {

  private var data = ArrayBuffer[ArrayBuffer[Any]]()

  private var columnNames = ArrayBuffer[String]()

  private var m_readOnly: Boolean = false

  override def getColumnName(col: Int): String = columnNames(col)

  def getRowCount(): Int = data.size

  def getColumnCount(): Int = columnNames.size

  /* (non-Javadoc)
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  def getValueAt(row: Int, col: Int): AnyRef = {
    data(row)(col).asInstanceOf[AnyRef]
  }

  /* (non-Javadoc)
   * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
   */
  override def isCellEditable(row: Int, col: Int): Boolean = !m_readOnly

  def setReadOnly(readOnly: Boolean) {
    m_readOnly = readOnly
  }

  /* (non-Javadoc)
   * @see javax.swing.table.AbstractTableModel#setValueAt(java.lang.Object, int, int)
   */
  override def setValueAt(value: AnyRef, row: Int, col: Int) {
    data(row)(col) = value
    fireTableCellUpdated(row, col)
  }

  def addRow(dataTableColumnName: AnyRef, dataTableColumnType: AnyRef) {
    data += ArrayBuffer[Any](dataTableColumnName, dataTableColumnType)
    val rowNum = data.size - 1
    fireTableRowsInserted(rowNum, rowNum)
  }

  def removeRow(row: Int) {
    data.remove(row)
    fireTableRowsDeleted(row, row)
  }

  def addColumn(columnName: String) {
    columnNames += columnName
  }

  def getDataTableColumnNames(): Array[String] = getDataTableValues(0)

  def getDataTableColumnTypes(): Array[String] = getDataTableValues(1)

  private def getDataTableValues(colIndex: Int): Array[String] = {
    data.map(_(colIndex).asInstanceOf[String]).toArray
  }

  def clearRows() {
    data.clear()
  }

  private def swap[T](s:ArrayBuffer[T], i: Int, j: Int) {
    val v = s(i);
    s(i) = s(j);
    s(j) = v
  }

  def moveRowsUp(rows: Array[Int]) {
    for (j <- 0 until rows.length if rows(j) != 0)
      swap(data, rows(j), rows(j) - 1)
    fireTableDataChanged()
  }

  def moveRowsDown(rows: Array[Int]) {
    for (j <- rows.length - 1 to 0 if rows(j) != data.size - 1)
      swap(data, rows(j), rows(j) + 1)
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