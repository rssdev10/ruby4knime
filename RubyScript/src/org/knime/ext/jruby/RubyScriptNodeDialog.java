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
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import javax.swing.AbstractAction;
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

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.*;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Font;

import org.fife.ui.rtextarea.*;
import org.fife.ui.rsyntaxtextarea.*;


public class RubyScriptNodeDialog extends NodeDialogPane {
    private static NodeLogger logger = NodeLogger
            .getLogger(RubyScriptNodeDialog.class);
    //private JTextArea scriptTextArea = new JTextArea();
    private RSyntaxTextArea m_scriptTextArea = new RSyntaxTextArea();

    private JTextArea m_errorMessage = new JTextArea();
    private JScrollPane m_sp_errorMessage = new JScrollPane(m_errorMessage);

    private JTable table;
    private int counter = 1;
    private JCheckBox m_appendColsCB;
    private RubyScriptNodeFactory m_factory;

    /**
     * New pane for configuring ScriptedNode node dialog.
     * 
     */
    protected RubyScriptNodeDialog(RubyScriptNodeFactory factory) {
        super();

        m_factory = factory;

        //scriptTextArea.setAutoscrolls(true);
        //Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);
        //scriptTextArea.setFont(font);

        m_scriptTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_RUBY);
        m_scriptTextArea.setCodeFoldingEnabled(true);
        m_scriptTextArea.setAntiAliasingEnabled(true);
        RTextScrollPane spScript = new RTextScrollPane(m_scriptTextArea);
        spScript.setFoldIndicatorEnabled(true);

        Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);
        m_errorMessage.setFont(font);
        m_errorMessage.setForeground(Color.RED);
        m_errorMessage.setEditable(false);

        // construct the output column selection panel
        JPanel outputPanel = new JPanel();
        outputPanel.setLayout(new BoxLayout(outputPanel, BoxLayout.Y_AXIS));
        JPanel outputButtonPanel = new JPanel();
        JPanel outputMainPanel = new JPanel(new BorderLayout());
        JPanel newtableCBPanel = new JPanel();
        m_appendColsCB = new JCheckBox("Append columns to input table spec");
        newtableCBPanel.add(m_appendColsCB, BorderLayout.WEST);
        JButton addButton = new JButton(new AbstractAction() {

            private static final long serialVersionUID = -743704737927962277L;

            public void actionPerformed(final ActionEvent e) {
                ((ScriptNodeOutputColumnsTableModel) table.getModel()).addRow(
                        "script output " + counter, "String");
                counter++;
            }
        });
        addButton.setText("Add Output Column");

        JButton removeButton = new JButton(new AbstractAction() {

            private static final long serialVersionUID = 743704737927962277L;

            public void actionPerformed(final ActionEvent e) {
                int[] selectedRows = table.getSelectedRows();
                logger.debug("selectedRows = " + selectedRows);

                if (selectedRows.length == 0) {
                    return;
                }

                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    logger.debug("   removal " + i + ": removing row "
                            + selectedRows[i]);
                    ((ScriptNodeOutputColumnsTableModel) table.getModel())
                            .removeRow(selectedRows[i]);
                }
            }
        });
        removeButton.setText("Remove Output Column");

        outputButtonPanel.add(addButton);
        outputButtonPanel.add(removeButton);

        table = new JTable();
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        table.setAutoscrolls(true);
        ScriptNodeOutputColumnsTableModel model = new ScriptNodeOutputColumnsTableModel();
        model.addColumn("Column name");
        model.addColumn("Column type");
        model.addRow("script output " + counter, "String");
        counter++;
        table.setModel(model);

        outputMainPanel.add(table.getTableHeader(), BorderLayout.PAGE_START);
        outputMainPanel.add(table, BorderLayout.CENTER);
        outputPanel.add(newtableCBPanel);
        outputPanel.add(outputButtonPanel);
        outputPanel.add(outputMainPanel);

        TableColumn typeColumn = table.getColumnModel().getColumn(1);
        JComboBox<String> typeSelector = new JComboBox<String>();
        typeSelector.addItem("String");
        typeSelector.addItem("Integer");
        typeSelector.addItem("Double");
        typeSelector.setEditable(true);

        typeColumn.setCellEditor(new DefaultCellEditor(typeSelector));

        // construct the panel for script loading/authoring
        JPanel scriptPanel = new JPanel(new BorderLayout());

        JPanel scriptButtonPanel = new JPanel();
        JButton scriptButton = new JButton(new AbstractAction() {

            private static final long serialVersionUID = 6097485154386131768L;
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

                m_scriptTextArea.removeAllLineHighlights();
                m_sp_errorMessage.setVisible(false);
            }
        });
        scriptButton.setText("Load Script from File");

        scriptButtonPanel.add(scriptButton);

        JPanel scriptMainPanel = new JPanel(new BorderLayout());
        scriptMainPanel.add(new JLabel("Script: "), BorderLayout.NORTH);

        //scriptMainPanel.add(new JScrollPane(scriptTextArea),
        //        BorderLayout.CENTER);

        //scriptMainPanel.add(spScript, BorderLayout.CENTER);        

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                spScript, m_sp_errorMessage);
        scriptMainPanel.add(splitPane, BorderLayout.CENTER);

        scriptPanel.add(scriptButtonPanel, BorderLayout.PAGE_START);
        scriptPanel.add(scriptMainPanel, BorderLayout.CENTER);

        addTab("Script Output", outputPanel);
        addTab("Script", scriptPanel);
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

        m_scriptTextArea.removeAllLineHighlights();
        m_sp_errorMessage.setVisible(false);
        m_errorMessage.setText("");
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
        }

        boolean appendCols = settings.getBoolean(
                RubyScriptNodeModel.APPEND_COLS, true);
        m_appendColsCB.setSelected(appendCols);

        String[] dataTableColumnNames = settings.getStringArray(
                RubyScriptNodeModel.COLUMN_NAMES, new String[0]);
        String[] dataTableColumnTypes = settings.getStringArray(
                RubyScriptNodeModel.COLUMN_TYPES, new String[0]);

        ((ScriptNodeOutputColumnsTableModel) table.getModel()).clearRows();

        if (dataTableColumnNames == null) {
            return;
        }

        for (int i = 0; i < dataTableColumnNames.length; i++) {
            ((ScriptNodeOutputColumnsTableModel) table.getModel()).addRow(
                    dataTableColumnNames[i], dataTableColumnTypes[i]);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected final void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        // work around a jtable cell value persistence problem
        // by explicitly stopping editing if a cell is currently in edit mode
        int editingRow = table.getEditingRow();
        int editingColumn = table.getEditingColumn();

        if (editingRow != -1 && editingColumn != -1) {
            TableCellEditor editor = table.getCellEditor(editingRow,
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
        String[] columnNames = ((ScriptNodeOutputColumnsTableModel) table
                .getModel()).getDataTableColumnNames();
        settings.addStringArray(RubyScriptNodeModel.COLUMN_NAMES, columnNames);

        String[] columnTypes = ((ScriptNodeOutputColumnsTableModel) table
                .getModel()).getDataTableColumnTypes();
        settings.addStringArray(RubyScriptNodeModel.COLUMN_TYPES, columnTypes);
    }

}
