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

    public final void setReadOnly(boolean readOnly){
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
}
