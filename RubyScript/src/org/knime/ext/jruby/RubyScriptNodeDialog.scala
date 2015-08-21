package org.knime.ext.jruby

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Point
import java.awt.event._
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.Iterator
import java.util.Map
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.DefaultCellEditor
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTable
import javax.swing.table.TableColumn
import javax.swing.table.TableCellEditor
import javax.swing.text.BadLocationException
import org.knime.core.data.DataColumnSpec
import org.knime.core.data.DataTableSpec
import org.knime.core.node._
import org.knime.core.node.workflow.FlowVariable
import javax.swing.JScrollPane
import javax.swing.JTextArea
import java.awt.Font
import org.fife.ui.rtextarea._
import org.fife.ui.rsyntaxtextarea._
import RubyScriptNodeDialog._
//remove if not needed
import scala.collection.JavaConversions._
import scala.collection.convert.WrapAsScala.enumerationAsScalaIterator
import scala.swing._

/**
 * <code>NodeDialog</code> for the "JRuby Script" Node.
 * 
 * @author rss
 *
 */
object RubyScriptNodeDialog {

  private val TEMPLATE_FLOW_VAR = "FlowVariableList['%s'] "

  private val TEMPLATE_COLUMN_NAME = "i%d_%s "

  private var logger: NodeLogger = 
    NodeLogger.getLogger(classOf[RubyScriptNodeDialog])
}

class RubyScriptNodeDialog(private var factory: RubyScriptNodeFactory)
  extends NodeDialogPane() {

  private var scriptPanel: BorderPanel = _

  private var scriptTextArea: RSyntaxTextArea = _

  private var errorMessage: TextArea = _

  private var spErrorMessage: ScrollPane = _

  private var table: JTable = _

  private var columnCounter: Int = 1

  private var doAppendInputColumns: CheckBox = _

  private var columnTables: Array[JTable] = _

  private val fileChooser = new JFileChooser()

  createColumnSelectionTab()

  createScriptTab()

  implicit private def toActionListener(f: (ActionEvent) => Unit) = new ActionListener {
    def actionPerformed(e: ActionEvent) { f(e) }
  }

  implicit private def toTableModel(table: JTable):ScriptNodeOutputColumnsTableModel = {
    table.getModel.asInstanceOf[ScriptNodeOutputColumnsTableModel]
  }

  /**
   * Create column selection tab panel
   */
  private def createColumnSelectionTab() {
    val outputPanel = new JPanel()
    outputPanel.setLayout(new BoxLayout(outputPanel, BoxLayout.Y_AXIS))
    val outputButtonPanel = new JPanel()
    val outputMainPanel = new JPanel(new BorderLayout())
    val newtableCBPanel = new JPanel()
    doAppendInputColumns = new CheckBox("Append columns to input table spec")
    newtableCBPanel.add(doAppendInputColumns.peer, BorderLayout.WEST)
    val addButton = new JButton("Add Output Column")
    addButton.addActionListener((_: ActionEvent) => {
        var name: String = null
        val model:ScriptNodeOutputColumnsTableModel = table
        val columns = model.getDataTableColumnNames
        do {
          name = "script output " + columnCounter
          columnCounter += 1
        } while (columns.indexWhere(_ == name) >= 0);
        model.addRow(name, "String")
    })
    val removeButton = new JButton("Remove Output Column")
    removeButton.addActionListener((_: ActionEvent) => {
        val selectedRows = table.getSelectedRows
        logger.debug("selectedRows = " + selectedRows)
        if (selectedRows.length == 0) {
          return
        }
        for (i <- selectedRows.length - 1 to 0) {
          logger.debug("   removal " + i + ": removing row " + selectedRows(i))
          table.removeRow(selectedRows(i))
        }
    })

    table = new JTable()
    table.putClientProperty("terminateEditOnFocusLost", true)
    table.setAutoscrolls(true)
    val model = new ScriptNodeOutputColumnsTableModel()
    model.addColumn("Column name")
    model.addColumn("Column type")
    model.addRow("script output " + columnCounter, "String")
    columnCounter += 1
    table.setModel(model)

    def createButtonForRowsMoving(title: String, func: (Array[Int]) => (Int, Int)): JButton = {
      val result = new JButton(title)
      result.addActionListener((_: ActionEvent) => {
          val selectedRows = table.getSelectedRows
          logger.debug("selectedRows = " + selectedRows)
          if (selectedRows.length > 0) {
            val position = func(selectedRows)
            table.setRowSelectionInterval(position._1, position._2)
          }
      })
      result
    }

    val upButton = createButtonForRowsMoving(
        "Up",
        table.moveRowsUp)
    val downButton = createButtonForRowsMoving(
        "Down",
        table.moveRowsDown)

    Array(addButton, removeButton, Box.createHorizontalStrut(40),
      upButton, downButton).foreach(outputButtonPanel.add)

//    outputButtonPanel.add(addButton)
//    outputButtonPanel.add(removeButton)
//    outputButtonPanel.add(Box.createHorizontalStrut(40))
//    outputButtonPanel.add(upButton)
//    outputButtonPanel.add(downButton)

    outputMainPanel.add(table.getTableHeader, BorderLayout.PAGE_START)
    outputMainPanel.add(table, BorderLayout.CENTER)
    outputPanel.add(newtableCBPanel)
    outputPanel.add(outputButtonPanel)
    outputPanel.add(outputMainPanel)
    val typeColumn = table.getColumnModel.getColumn(1)
    val typeSelector = new JComboBox[String]()
    typeSelector.addItem("String")
    typeSelector.addItem("Integer")
    typeSelector.addItem("Double")
    typeSelector.setEditable(true)
    typeColumn.setCellEditor(new DefaultCellEditor(typeSelector))
    addTab("Script Output", outputPanel)
  }

  /**
   * Create script tab panel which contains lists of columns, flow variables,
   * Ruby script etc.
   */
  private def createScriptTab() {
    errorMessage = new TextArea()
    spErrorMessage = new ScrollPane(errorMessage)
    scriptTextArea = new RSyntaxTextArea()
    scriptTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_RUBY)
    scriptTextArea.setCodeFoldingEnabled(true)
    scriptTextArea.setAntiAliasingEnabled(true)
    val spScript = new RTextScrollPane(scriptTextArea)
    spScript.setFoldIndicatorEnabled(true)
    val font = new Font(Font.MONOSPACED, Font.PLAIN, 12)
    errorMessage.font = font
    errorMessage.foreground = Color.RED
    errorMessage.editable  = false
    scriptPanel = new BorderPanel()
    val scriptButtonPanel = new BorderPanel()
    val scriptButton = new JButton("Load Script from File")
    scriptButton.addActionListener((e: ActionEvent) => {
        val returnVal = fileChooser.showOpenDialog(
          e.getSource.asInstanceOf[Component])
        if (returnVal != JFileChooser.APPROVE_OPTION) {
          return
        }
        val file = fileChooser.getSelectedFile
        if (!file.exists()) {
          return
        }

        val file_content = scala.io.Source.fromFile(file, "utf-8").mkString
        scriptTextArea.setText(file_content)
        clearErrorHighlight()
    })
    scriptButtonPanel.peer.add(scriptButton)
    val scriptMainPanel = new BorderPanel()
    scriptMainPanel.peer.add(new Label("Script: ").peer, BorderLayout.NORTH)
    var splitPane = new SplitPane(Orientation.Horizontal,
        swing.Component.wrap(spScript), spErrorMessage)
    scriptMainPanel.peer.add(splitPane.peer, BorderLayout.CENTER)
    val num = factory.getModel.getInputPortRoles.length
    columnTables = Array.ofDim[JTable](num)
    val inputColumnsPanel = new BorderPanel()
    inputColumnsPanel.peer.setLayout(
      new BoxLayout(inputColumnsPanel.peer, BoxLayout.PAGE_AXIS))
    if (num > 0) inputColumnsPanel.minimumSize = new Dimension(20, 150)
    for (i <- 0 until num) {
      inputColumnsPanel.peer.add(addColumnPane("Input[%d] columns: ".format(i), i).peer)
    }
    val flowVariablesPanel = addFlowVariablesPane("Flow variables: ")
    splitPane = new SplitPane(Orientation.Horizontal,
      inputColumnsPanel, flowVariablesPanel)
    splitPane.dividerLocation  = splitPane.size.height
                               - splitPane.peer.getInsets().bottom
                               - splitPane.dividerSize - 50

    scriptPanel.peer.add(scriptButtonPanel.peer, BorderLayout.PAGE_START)
    scriptPanel.peer.add(scriptMainPanel.peer, BorderLayout.CENTER)
    val config_and_sript = new SplitPane(Orientation.Vertical, splitPane, scriptPanel)
    config_and_sript.dividerLocation = 200
    addTab("Script", config_and_sript.peer, false)
  }

  /**
   * Create a panel of input columns
   * @param label
   * @param list of columns
   * @return JPanel
   */
  private def addColumnPane(label: String, index: Int): Panel = {
    val panel = new BorderPanel()
    val table = new JTable()
    table.putClientProperty("terminateEditOnFocusLost", true)
    table.setAutoscrolls(true)
    table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN)
    val model = new ScriptNodeOutputColumnsTableModel()
    model.addColumn("Column name")
    model.addColumn("Column type")
    model.setReadOnly(true)
    table.setModel(model)
    table.addMouseListener(new MouseAdapter(){
      private var m_index: Int = _

      override def mouseClicked(event: MouseEvent) {
        if (event.getClickCount == 2) {
          val table = event.getSource.asInstanceOf[JTable]
          val p = event.getPoint
          val row = table.rowAtPoint(p)
          if (row >= 0) {
            var name = table.getModel.getValueAt(row, 0).toString
            if (name.length > 0) {
              name = name.replaceAll("[^\\p{Alnum}]", "_")
                .replaceAll("\\_+", "_")
              if (name.charAt(name.length - 1) == '_')
                name = name.substring(0, name.length - 1)
              scriptTextArea.insert(
                TEMPLATE_COLUMN_NAME.format(m_index, name),
                scriptTextArea.getCaretPosition)
            }
          }
        }
      }

      def init(index: Int): MouseAdapter = {
        m_index = index
        return this
      }
    }.init(index))
    val scrollPane = new JScrollPane(table)
    table.setFillsViewportHeight(true)
    panel.peer.add(new JLabel(label), BorderLayout.NORTH)
    panel.peer.add(scrollPane, BorderLayout.CENTER)
    columnTables(index) = table
    panel
  }

  private def updateColumnTable(specs: Array[DataTableSpec]) {
    if (specs != null) {
      for (i <- 0 until specs.length) {
        val model:ScriptNodeOutputColumnsTableModel = columnTables(i)
        model.clearRows()
        for (spec <- specs(i)) {
          model.addRow(spec.getName, spec.getType.toString)
        }
      }
    }
  }

  /**
   * Create a panel with flow variable list
   * @param label
   * @return JPanel
   */
  private def addFlowVariablesPane(label: String): Panel = {
    val flowVariablesPanel = new BorderPanel()
    val table = new JTable()
    table.putClientProperty("terminateEditOnFocusLost", true)
    table.setAutoscrolls(true)
    val model = new ScriptNodeOutputColumnsTableModel()
    model.addColumn("Name")
    model.addColumn("Value")
    model.setReadOnly(true)
    table.setModel(model)
    table.addMouseListener(new MouseAdapter() {

      override def mouseClicked(event: MouseEvent) {
        if (event.getClickCount == 2) {
          val table = event.getSource.asInstanceOf[JTable]
          val p = event.getPoint
          val row = table.rowAtPoint(p)
          if (row >= 0) {
            scriptTextArea.insert(String.format(TEMPLATE_FLOW_VAR,
                table.getModel.getValueAt(row, 0).toString),
                scriptTextArea.getCaretPosition)
          }
        }
      }
    })
    factory.getModel.getAvailableFlowVariables
      .values.foreach(
        varDescr => table.addRow(varDescr.getName, varDescr.getStringValue)
    )
    val scrollPane = new JScrollPane(table)
    table.setFillsViewportHeight(true)
    flowVariablesPanel.peer.add(new JLabel(label), BorderLayout.NORTH)
    flowVariablesPanel.peer.add(scrollPane, BorderLayout.CENTER)
    flowVariablesPanel
  }

  /* (non-Javadoc)
   * @see org.knime.core.node.NodeDialogPane#loadSettingsFrom(org.knime.core.node.NodeSettingsRO, org.knime.core.data.DataTableSpec[])
   */
  override protected def loadSettingsFrom(settings: NodeSettingsRO,
    specs: Array[DataTableSpec]) {
    var script = Option(settings.getString(RubyScriptNodeModel.SCRIPT, null))
      .getOrElse("")
    scriptTextArea.setText(script)
    clearErrorHighlight()
    val error = factory.getModel.getErrorData
    if (error.lineNum != -1) {
      try {
        scriptTextArea.addLineHighlight(error.lineNum - 1, Color.red)
      } catch {
        case e1: BadLocationException => 
      }
      val outstr = new StringBuilder()
      outstr ++= error.text
      outstr ++= "\nline:\t class ( method )\t file\n"
      outstr ++= error.trace
      errorMessage.text = outstr.toString
      spErrorMessage.visible = true
      setSelected("Script")
    }
    val appendCols = settings.getBoolean(RubyScriptNodeModel.APPEND_COLS, true)
    doAppendInputColumns.selected = appendCols
    val dataTableColumnNames =
      settings.getStringArray(RubyScriptNodeModel.COLUMN_NAMES,
        Array[String]():_*)
    val dataTableColumnTypes =
      settings.getStringArray(RubyScriptNodeModel.COLUMN_TYPES,
        Array[String](): _*)
    table.clearRows()
    if (dataTableColumnNames == null) {
      return
    }
    for (i <- 0 until dataTableColumnNames.length) {
      table.addRow(dataTableColumnNames(i), dataTableColumnTypes(i))
    }
    updateColumnTable(specs)
  }

  /* (non-Javadoc)
   * @see org.knime.core.node.NodeDialogPane#saveSettingsTo(org.knime.core.node.NodeSettingsWO)
   */
  protected def saveSettingsTo(settings: NodeSettingsWO) {
    val editingRow = table.getEditingRow
    val editingColumn = table.getEditingColumn
    if (editingRow != -1 && editingColumn != -1) {
      val editor = table.getCellEditor(editingRow, editingColumn)
      editor.stopCellEditing()
    }
    val scriptSetting = scriptTextArea.getText
    if (scriptSetting == null || scriptSetting.isEmpty()) {
      throw new InvalidSettingsException("Please specify a script to be run.")
    }
    settings.addString(RubyScriptNodeModel.SCRIPT, scriptTextArea.getText)
    settings.addBoolean(RubyScriptNodeModel.APPEND_COLS, doAppendInputColumns.selected)
    val columnNames = table.getDataTableColumnNames
    settings.addStringArray(RubyScriptNodeModel.COLUMN_NAMES, columnNames: _*)
    val columnTypes = table.getDataTableColumnTypes
    settings.addStringArray(RubyScriptNodeModel.COLUMN_TYPES, columnTypes: _*)
  }

  /**
   * Delete highlight in the script pane and hide error window
   */
  protected def clearErrorHighlight() {
    scriptTextArea.removeAllLineHighlights()
    spErrorMessage.visible = false
    errorMessage.text = ""
    scriptPanel.revalidate()
    scriptPanel.repaint()
  }
}
