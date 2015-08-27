package org.knime.ext.jruby

import java.awt.BorderLayout
import java.awt.Color
import java.awt.event._

import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.DefaultCellEditor
import javax.swing.JComboBox
import javax.swing.table.TableColumn
import javax.swing.table.TableCellEditor
import javax.swing.text.BadLocationException

import org.knime.core.data.DataColumnSpec
import org.knime.core.data.DataTableSpec
import org.knime.core.node._
import org.knime.core.node.workflow.FlowVariable

import java.awt.Font

import org.fife.ui.rtextarea._
import org.fife.ui.rsyntaxtextarea._

import RubyScriptNodeDialog._
//remove if not needed

import scala.collection.JavaConversions._
import scala.collection.convert.WrapAsScala.enumerationAsScalaIterator

import scala.swing._
import scala.swing.event._
import scala.swing.Table
import scala.swing.Container
import scala.swing.FileChooser

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

  private var table: Table = _

  private var columnCounter: Int = 1

  private var doAppendInputColumns: CheckBox = _

  private var columnTables: Array[Table] = _

  private val fileChooser = new FileChooser()

  createColumnSelectionTab()

  createScriptTab()

  implicit private def toTableModel(table: Table):ScriptNodeOutputColumnsTableModel = {
    table.model.asInstanceOf[ScriptNodeOutputColumnsTableModel]
  }
  /**
   * Create column selection tab panel
   */
  private def createColumnSelectionTab() {
    table = new Table() {
      model = new ScriptNodeOutputColumnsTableModel() {
        addColumn("Column name")
        addColumn("Column type")
        addRow("script output " + columnCounter, "String")
      }
      columnCounter += 1

      val typeSelector =
        new ComboBox[String](Seq(
            "String",
            "Integer",
            "Double" )) {
          makeEditable
        }

      override def editor(row: Int, column: Int) = {
        column match {
          case 1 => new DefaultCellEditor(
              typeSelector.peer.asInstanceOf[JComboBox[String]])
          case _ => null
        }
      }
    }

    val outputPanel = new BoxPanel(Orientation.Vertical) {
      contents += new BorderPanel() {
        doAppendInputColumns = new CheckBox("Append columns to input table spec")
        layout(doAppendInputColumns) = BorderPanel.Position.West
        maximumSize = minimumSize
      }

      contents += new BoxPanel(Orientation.Horizontal) {
        contents += new Button("Add Output Column") {
          reactions += {
            case ButtonClicked(b) =>
              var name: String = null
              val model: ScriptNodeOutputColumnsTableModel = table
              val columns = model.getDataTableColumnNames
              do {
                name = "script output " + columnCounter
                columnCounter += 1
              } while (columns.indexWhere(_ == name) >= 0);
              model.addRow(name, "String")
          }
        }
        contents += new Button("Remove Output Column") {
          reactions += {
            case ButtonClicked(b) =>
              val selectedRows = table.selection.rows
              logger.debug("selectedRows = " + selectedRows)
              selectedRows.view.toSeq.reverse.foreach(el => {
                logger.debug("   removing row " + el)
                table.removeRow(el)
              })
          }
        }
        contents += Swing.HStrut(40)
        contents += createButtonForRowsMoving("Up", table.moveRowsUp)
        contents += createButtonForRowsMoving("Down", table.moveRowsDown)
        maximumSize = minimumSize
      }

      contents += new BorderPanel() {
        peer.add(table.peer.getTableHeader, BorderLayout.PAGE_START)
        layout(table) = BorderPanel.Position.Center
      }
    }
    addTab("Script Output", outputPanel.peer)
  }

  /**
   * Create script tab panel which contains lists of columns, flow variables,
   * Ruby script etc.
   */
  private def createScriptTab() {
    errorMessage = new TextArea() {
      font = new Font(Font.MONOSPACED, Font.PLAIN, 12)
      foreground = Color.RED
      editable = false
    }
    spErrorMessage = new ScrollPane(errorMessage)
    scriptTextArea = new RSyntaxTextArea() {
      setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_RUBY)
      setCodeFoldingEnabled(true)
      setAntiAliasingEnabled(true)
    }

    val spScript = new RTextScrollPane(scriptTextArea) {
      setFoldIndicatorEnabled(true)
    }

    scriptPanel = new BorderPanel() {
      val scriptButtonPanel = new BorderPanel() {
        layout(new Button("Load Script from File") {
          reactions += {
            case ButtonClicked(b) =>
              val returnVal = fileChooser.showOpenDialog(b)
              if (returnVal == FileChooser.Result.Approve) {
                val file = fileChooser.selectedFile
                if (file.exists()) {
                  val file_content = scala.io.Source.fromFile(file, "utf-8").mkString
                  scriptTextArea.setText(file_content)
                  clearErrorHighlight()
                }
              }
          }
        }) = BorderPanel.Position.East

        layout(new Label("Ruby Script")) = BorderPanel.Position.Center
      }
      layout(scriptButtonPanel) = BorderPanel.Position.North

      layout(new BorderPanel() {
        layout(new SplitPane(Orientation.Horizontal,
          swing.Component.wrap(spScript), spErrorMessage)) =
            BorderPanel.Position.Center
      }) = BorderPanel.Position.Center
    }

    val num = factory.getModel.getInputPortRoles.length
    columnTables = Array.ofDim[Table](num)
    val inputColumnsPanel = new BorderPanel() {
      peer.setLayout(
        new BoxLayout(peer, BoxLayout.PAGE_AXIS))
      if (num > 0) minimumSize = new Dimension(20, 150)
      for (i <- 0 until num) {
        peer.add(addColumnPane("Input[%d] columns: ".format(i), i).peer)
      }
      visible = true
    }
    val flowVariablesPanel = addFlowVariablesPane("Flow variables: ")
    val splitPane = new SplitPane(Orientation.Horizontal,
      inputColumnsPanel, flowVariablesPanel)

    val config_and_sript = 
      new SplitPane(Orientation.Vertical, splitPane, scriptPanel) {
        dividerLocation = 200
    }
    addTab("Script", config_and_sript.peer, false)
  }

  /**
   * Create a panel of input columns
   * @param label
   * @param list of columns
   * @return JPanel
   */
  private def addColumnPane(label: String, index: Int): Panel = {
    new BorderPanel() {
      val table = new Table() {
        model = new ScriptNodeOutputColumnsTableModel() {
          addColumn("Column name")
          addColumn("Column type")
          setReadOnly(true)
        }
        autoResizeMode = (Table.AutoResizeMode.LastColumn)
        listenTo(mouse.clicks)
        reactions += {
          case e: MousePressed =>
            if (e.clicks == 2) {
              val table = e.source.asInstanceOf[Table]
              val row = table.peer.rowAtPoint(e.point)
              if (row >= 0) {
                var name = table.model.getValueAt(row, 0).toString
                if (name.length > 0) {
                  name = name.replaceAll("[^\\p{Alnum}]", "_")
                    .replaceAll("\\_+", "_")
                  if (name.charAt(name.length - 1) == '_')
                    name = name.substring(0, name.length - 1)
                  scriptTextArea.insert(
                    TEMPLATE_COLUMN_NAME.format(index, name),
                    scriptTextArea.getCaretPosition)
                }
              }
            }
        }
      }

      //table.setFillsViewportHeight(true)
      layout(new Label(label)) = BorderPanel.Position.North
      layout(new ScrollPane(table)) = BorderPanel.Position.Center
      columnTables(index) = table
    }
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
    new BorderPanel() {
      val table = new Table() {
        model = new ScriptNodeOutputColumnsTableModel() {
          addColumn("Name")
          addColumn("Value")
          setReadOnly(true)
        }
        listenTo(mouse.clicks)
        reactions += {
          case event: MouseClicked =>
            if (event.clicks == 2) {
              val table = event.source.asInstanceOf[Table]
              val row = table.peer.rowAtPoint(event.point)
              if (row >= 0) {
                scriptTextArea.insert(String.format(TEMPLATE_FLOW_VAR,
                  table.model.getValueAt(row, 0).toString),
                  scriptTextArea.getCaretPosition)
              }
            }
        }
      }
      factory.getModel.getAvailableFlowVariables
        .values.foreach(
          varDescr => table.addRow(varDescr.getName, varDescr.getStringValue)
        )

      //table.setFillsViewportHeight(true)
      layout(new Label(label)) = BorderPanel.Position.North
      layout(new ScrollPane(table)) = BorderPanel.Position.Center
    }
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
    val editingRow = table.peer.getEditingRow
    val editingColumn = table.peer.getEditingColumn
    if (editingRow != -1 && editingColumn != -1) {
      val editor = table.peer.getCellEditor(editingRow, editingColumn)
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

  private def createButtonForRowsMoving(title: String, func: (Seq[Int]) => Seq[Int]): Button = {
    new Button(title) {
      reactions += {
        case ButtonClicked(b) =>
          val selectedRows = table.selection.rows
          logger.debug("selectedRows = " + selectedRows)
          if (selectedRows.size > 0) {
            table.selection.rows ++= func(selectedRows.toSeq)
          }
      }
    }
  }
}
