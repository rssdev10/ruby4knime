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
}
