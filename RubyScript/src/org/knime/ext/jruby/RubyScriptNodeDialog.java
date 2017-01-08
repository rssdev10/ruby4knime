/**
 * <code>NodeDialog</code> for the "JRuby Script" Node.
 *
 * This source code based on PythonScriptNodeDialog.java 
 * from org.knime.ext.jython.source_2.9.0.0040102 by Tripos
 *
 */

package org.knime.ext.jruby;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import java.util.function.*;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.table.TableCellEditor;
import javax.swing.text.BadLocationException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.*;
import org.knime.core.node.workflow.FlowVariable;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Font;

import org.fife.ui.rtextarea.*;
import org.fife.ui.rsyntaxtextarea.*;


public class RubyScriptNodeDialog extends NodeDialogPane {

    private final static String TEMPLATE_FLOW_VAR = "FlowVariableList['%s'] ";
    // rules from knime.rb:
    //   name = str.gsub(/[^[[:word:]]]/, '_').gsub(/\_+/, '_').chomp('_')
    //   define_method("i#{i}_#{name}") where #{i} - index of the input port
    private final static String TEMPLATE_COLUMN_NAME = "i%d_%s ";

    private static NodeLogger logger = NodeLogger
            .getLogger(RubyScriptNodeDialog.class);

    private JPanel m_scriptPanel;
    //private JTextArea scriptTextArea = new JTextArea();
    private RSyntaxTextArea m_scriptTextArea;

    private JTextArea m_errorMessage;
    private JScrollPane m_sp_errorMessage;

    private JTable m_table;
    private int m_counter = 1;
    private JCheckBox m_appendColsCB;
    private RubyScriptNodeFactory m_factory;

    private JTable[] m_columnTables;

    /**
     * New pane for configuring ScriptedNode node dialog.
     * 
     */
    protected RubyScriptNodeDialog(RubyScriptNodeFactory factory) {
        super();

        m_factory = factory;

        createColumnSelectionTab();
        createScriptTab();
    }

    /**
     * Creates column selection tab panel
     */
    private final void createColumnSelectionTab() {
        // construct the output column selection panel
        JPanel outputPanel = new JPanel();
        outputPanel.setLayout(new BoxLayout(outputPanel, BoxLayout.Y_AXIS));
        JPanel outputButtonPanel = new JPanel();
        JPanel outputMainPanel = new JPanel(new BorderLayout());
        JPanel newtableCBPanel = new JPanel();
        m_appendColsCB = new JCheckBox("Append columns to input table spec");
        newtableCBPanel.add(m_appendColsCB, BorderLayout.WEST);

        JButton addButton = new JButton("Add Output Column");
        addButton.addActionListener( new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                String name;
                ScriptNodeOutputColumnsTableModel model = ((ScriptNodeOutputColumnsTableModel) m_table
                        .getModel());
                String[] columns = model.getDataTableColumnNames();
                boolean found;

                do {
                    found = false;
                    name = "script output " + m_counter;
                    m_counter++;
                    for (String s : columns) {
                        if (name.equals(s)) {
                            found = true;
                            break;
                        }
                    }
                } while (found);

                model.addRow(name, "String");
            }
        });

        JButton removeButton = new JButton("Remove Output Column");
        removeButton.addActionListener( new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                int[] selectedRows = m_table.getSelectedRows();
                logger.debug("selectedRows = " + selectedRows);

                if (selectedRows.length == 0) {
                    return;
                }

                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    logger.debug("   removal " + i + ": removing row "
                            + selectedRows[i]);
                    ((ScriptNodeOutputColumnsTableModel) m_table.getModel())
                            .removeRow(selectedRows[i]);
                }
            }
        });

        m_table = new JTable();
        m_table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        m_table.setAutoscrolls(true);
        ScriptNodeOutputColumnsTableModel model = new ScriptNodeOutputColumnsTableModel();
        model.addColumn("Column name");
        model.addColumn("Column type");
        model.addRow("script output " + m_counter, "String");
        m_counter++;
        m_table.setModel(model);

        JButton upButton = createButtonForRowsMoving("Up",
                selectedRows -> model.moveRowsUp(selectedRows));
        JButton downButton = createButtonForRowsMoving("Down",
                selectedRows -> model.moveRowsDown(selectedRows));

		outputButtonPanel.add(addButton);
		outputButtonPanel.add(removeButton);
		outputButtonPanel.add(Box.createHorizontalStrut(40));
		outputButtonPanel.add(upButton);
		outputButtonPanel.add(downButton);

        outputMainPanel.add(m_table.getTableHeader(), BorderLayout.PAGE_START);
        outputMainPanel.add(m_table, BorderLayout.CENTER);
        outputPanel.add(newtableCBPanel);
        outputPanel.add(outputButtonPanel);
        outputPanel.add(outputMainPanel);

        TableColumn typeColumn = m_table.getColumnModel().getColumn(1);
        JComboBox<String> typeSelector = new JComboBox<String>();
        typeSelector.addItem("String");
        typeSelector.addItem("Integer");
        typeSelector.addItem("Double");
        typeSelector.setEditable(true);

        typeColumn.setCellEditor(new DefaultCellEditor(typeSelector));
        addTab("Script Output", outputPanel);
    }

    /**
     * Creates script tab panel which contains lists of columns, flow variables,
     * Ruby script etc.
     */
    private final void createScriptTab() {
        // scriptTextArea.setAutoscrolls(true);
        // Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);
        // scriptTextArea.setFont(font);

        m_errorMessage = new JTextArea();
        m_sp_errorMessage = new JScrollPane(m_errorMessage);

        m_scriptTextArea = new RSyntaxTextArea();
        m_scriptTextArea
                .setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_RUBY);
        m_scriptTextArea.setCodeFoldingEnabled(true);
        m_scriptTextArea.setAntiAliasingEnabled(true);
        RTextScrollPane spScript = new RTextScrollPane(m_scriptTextArea);
        spScript.setFoldIndicatorEnabled(true);

        Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);
        m_errorMessage.setFont(font);
        m_errorMessage.setForeground(Color.RED);
        m_errorMessage.setEditable(false);

        m_scriptPanel = new JPanel(new BorderLayout());

        JPanel scriptButtonPanel = new JPanel();

        // script load button
        JButton scriptButton = new JButton("Load Script from File");
        scriptButton.addActionListener( new ActionListener() {
            private JFileChooser fileChooser = new JFileChooser();

            public void actionPerformed(final ActionEvent e) {

                // open the file dialog
                int returnVal = fileChooser.showOpenDialog((Component) e
                        .getSource());

                if (returnVal != JFileChooser.APPROVE_OPTION) {
                    return;
                }

                // check for file existence
                File file = fileChooser.getSelectedFile();
                if (!file.exists()) {
                    return;
                }

                // read the contents and put them in the script textarea
                StringBuffer buffer = new StringBuffer();
                BufferedReader reader;
                try {
                    reader = new BufferedReader(new FileReader(file));
                    while (reader.ready()) {
                        String line = reader.readLine();
                        buffer.append(line + "\n");
                    }
                    reader.close();

                } catch (IOException exc) {
                    exc.printStackTrace();
                }

                m_scriptTextArea.setText(buffer.toString());

                clearErrorHighlight();
            }
        });
        scriptButtonPanel.add(scriptButton);

        JPanel scriptMainPanel = new JPanel(new BorderLayout());
        scriptMainPanel.add(new JLabel("Script: "), BorderLayout.NORTH);

        // scriptMainPanel.add(new JScrollPane(scriptTextArea),
        // BorderLayout.CENTER);

        // scriptMainPanel.add(spScript, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                spScript, m_sp_errorMessage);
        scriptMainPanel.add(splitPane, BorderLayout.CENTER);

        // add output column list
        int num = m_factory.getModel().getInputPortRoles().length;
        m_columnTables = new JTable[num];

        JPanel inputColumnsPanel = new JPanel();
        inputColumnsPanel.setLayout(new BoxLayout(inputColumnsPanel, BoxLayout.PAGE_AXIS));

        if (num > 0)
            inputColumnsPanel.setMinimumSize(new Dimension(20, 150));

        for (int i = 0; i < num; i++) {
            inputColumnsPanel.add(addColumnPane(
                    String.format("Input[%d] columns: ", i), i));
        }

        // add flow variables
        JPanel flowVariablesPanel = addFlowVariablesPane("Flow variables: ");

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                inputColumnsPanel, flowVariablesPanel);

        splitPane.setDividerLocation(splitPane.getSize().height
                - splitPane.getInsets().bottom - splitPane.getDividerSize()
                - 50);

        // scriptMainPanel.add(splitPane, BorderLayout.WEST);
        // scriptMainPanel.add(new JLabel("Script: "), BorderLayout.NORTH);

        m_scriptPanel.add(scriptButtonPanel, BorderLayout.PAGE_START);
        m_scriptPanel.add(scriptMainPanel, BorderLayout.CENTER);

        JSplitPane config_and_sript = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, splitPane, m_scriptPanel);

        config_and_sript.setDividerLocation(200);

        // addTab("Script", m_scriptPanel, false);
        addTab("Script", config_and_sript, false);
    }

    /**
     * Creates a panel of of input columns
     * @param label
     * @param list of columns
     * @return JPanel
     */
    private final JPanel addColumnPane(String label, int index) {
        JPanel panel = new JPanel(new BorderLayout());
        JTable table = new JTable();
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        table.setAutoscrolls(true);
        //table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        ScriptNodeOutputColumnsTableModel model = new ScriptNodeOutputColumnsTableModel();
        model.addColumn("Column name");
        model.addColumn("Column type");
        model.setReadOnly(true);
        table.setModel(model);

        // dblclick on a table's row
        table.addMouseListener(new MouseAdapter() {
            private int m_index;
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    JTable table = (JTable) event.getSource();
                    Point p = event.getPoint();
                    int row = table.rowAtPoint(p);
                    if (row >= 0) {
                        String name = table.getModel().getValueAt(row, 0)
                                .toString();
                        if (name.length() > 0) {
                            // see knime.rb rules
                            name = name.replaceAll("[^\\p{Alnum}]", "_")
                                    .replaceAll("\\_+", "_");
                            if (name.charAt(name.length() - 1) == '_')
                                name = name.substring(0, name.length() - 1);

                            m_scriptTextArea.insert(String.format(
                                    TEMPLATE_COLUMN_NAME, m_index, name),
                                    m_scriptTextArea.getCaretPosition());
                        }
                    }
                }
            }
            private MouseAdapter init(int index){
                m_index = index;
                return this;
            }
        }.init(index));

        JScrollPane scrollPane = new JScrollPane(table);
        table.setFillsViewportHeight(true);

        panel.add(new JLabel(label), BorderLayout.NORTH);
        // inputColumnsPanel.add(m_inpputColumnsTable.getTableHeader(),
        // BorderLayout.PAGE_START);
        panel.add(scrollPane, BorderLayout.CENTER);
        m_columnTables[index] = table;
        return panel;
    }

    private final void updateColumnTable(final DataTableSpec[] specs) {
        if (specs != null) {
            for (int i = 0; i < specs.length; i++) {
                ScriptNodeOutputColumnsTableModel model = 
                        (ScriptNodeOutputColumnsTableModel) (m_columnTables[i].getModel());
                model.clearRows();
                for (Iterator<DataColumnSpec> item = specs[i].iterator(); item
                        .hasNext();) {
                    DataColumnSpec spec = item.next();
                    model.addRow(spec.getName(), spec.getType().toString());
                }
            }
        }
    }

    /**
     * Creates a panel with flow variable list
     * @param label
     * @return JPanel
     */
    private final JPanel addFlowVariablesPane(String label) {
        JPanel flowVariablesPanel = new JPanel(new BorderLayout());
        JTable table = new JTable();
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        table.setAutoscrolls(true);
        ScriptNodeOutputColumnsTableModel model = new ScriptNodeOutputColumnsTableModel();
        model.addColumn("Name");
        model.addColumn("Value");
        model.setReadOnly(true);
        table.setModel(model);
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    JTable table = (JTable) event.getSource();
                    Point p = event.getPoint();
                    int row = table.rowAtPoint(p);
                    if (row >= 0) {
                        m_scriptTextArea.insert(
                                String.format(TEMPLATE_FLOW_VAR, table
                                        .getModel().getValueAt(row, 0)
                                        .toString()),
                                m_scriptTextArea.getCaretPosition());
                    }
                }
            }
        });

        Map<String, FlowVariable> flow_variables = m_factory.getModel()
                .getAvailableFlowVariables();
        for (Iterator<FlowVariable> i = flow_variables.values().iterator(); i
                .hasNext();) {
            FlowVariable var = i.next();
            ((ScriptNodeOutputColumnsTableModel) (table.getModel())).addRow(
                    var.getName(), var.getStringValue());
        }

        JScrollPane scrollPane = new JScrollPane(table);
        table.setFillsViewportHeight(true);

        flowVariablesPanel.add(new JLabel(label),
                BorderLayout.NORTH);
        // flowVariablesPanel.add(m_flowVariableTable.getTableHeader(),
        // BorderLayout.PAGE_START);
        // flowVariablesPanel.add(m_flowVariableTable, BorderLayout.CENTER);
        flowVariablesPanel.add(scrollPane, BorderLayout.CENTER);
        return flowVariablesPanel;
    }

    /**
     * {@inheritDoc}
     */
    protected final void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) {
        String script = settings.getString(RubyScriptNodeModel.SCRIPT, null);
        if (script == null) {
            script = "";
        }
        m_scriptTextArea.setText(script);

        clearErrorHighlight();
        RubyScriptNodeModel.ScriptError error = m_factory.getModel().getErrorData();
        if ( error.lineNum != -1 ) {
            try {
                m_scriptTextArea.addLineHighlight(error.lineNum - 1, Color.red);
            } catch (BadLocationException e1) {
                // nothing to do
                // e1.printStackTrace();
            }
            StringBuilder outstr = new StringBuilder();
            outstr.append(error.text);
            outstr.append("\nline:\t class ( method )\t file\n");
            outstr.append(error.trace);
            m_errorMessage.setText(outstr.toString());

            m_sp_errorMessage.setVisible(true);

            setSelected("Script");
        }

        boolean appendCols = settings.getBoolean(
                RubyScriptNodeModel.APPEND_COLS, true);
        m_appendColsCB.setSelected(appendCols);

        String[] dataTableColumnNames = settings.getStringArray(
                RubyScriptNodeModel.COLUMN_NAMES, new String[0]);
        String[] dataTableColumnTypes = settings.getStringArray(
                RubyScriptNodeModel.COLUMN_TYPES, new String[0]);

        ((ScriptNodeOutputColumnsTableModel) m_table.getModel()).clearRows();

        if (dataTableColumnNames == null) {
            return;
        }

        for (int i = 0; i < dataTableColumnNames.length; i++) {
            ((ScriptNodeOutputColumnsTableModel) m_table.getModel()).addRow(
                    dataTableColumnNames[i], dataTableColumnTypes[i]);
        }

        updateColumnTable(specs);
    }

    /**
     * {@inheritDoc}
     */
    protected final void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        // work around a jtable cell value persistence problem
        // by explicitly stopping editing if a cell is currently in edit mode
        int editingRow = m_table.getEditingRow();
        int editingColumn = m_table.getEditingColumn();

        if (editingRow != -1 && editingColumn != -1) {
            TableCellEditor editor = m_table.getCellEditor(editingRow,
                    editingColumn);
            editor.stopCellEditing();
        }

        // save the settings
        String scriptSetting = m_scriptTextArea.getText();
        if (scriptSetting == null || "".equals(scriptSetting)) {
            throw new InvalidSettingsException(
                    "Please specify a script to be run.");
        }
        settings.addString(RubyScriptNodeModel.SCRIPT, m_scriptTextArea.getText());

        settings.addBoolean(RubyScriptNodeModel.APPEND_COLS,
                m_appendColsCB.isSelected());
        String[] columnNames = ((ScriptNodeOutputColumnsTableModel) m_table
                .getModel()).getDataTableColumnNames();
        settings.addStringArray(RubyScriptNodeModel.COLUMN_NAMES, columnNames);

        String[] columnTypes = ((ScriptNodeOutputColumnsTableModel) m_table
                .getModel()).getDataTableColumnTypes();
        settings.addStringArray(RubyScriptNodeModel.COLUMN_TYPES, columnTypes);
    }

    /**
     * Delete highlight in the script pane and hide error window
     */
    protected final void clearErrorHighlight() {
        m_scriptTextArea.removeAllLineHighlights();
        m_sp_errorMessage.setVisible(false);
        m_errorMessage.setText("");
        m_scriptPanel.revalidate();
        m_scriptPanel.repaint();
    }

	protected JButton createButtonForRowsMoving(String title,
			Function<int[], int[]> mover) {
		JButton button = new JButton(title);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				int[] selectedRows = m_table.getSelectedRows();
				logger.debug("selectedRows = " + selectedRows);

				if (selectedRows.length == 0) {
					return;
				}

				int[] selection = mover.apply(selectedRows);

				m_table.addRowSelectionInterval(
						selection[0],
						selection[selection.length - 1]);
			}
		});

		return button;
	}
}
