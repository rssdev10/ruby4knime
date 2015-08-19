package org.knime.ext.jruby

import java.io.File
import java.util.ArrayList
import java.util.regex.Pattern
import org.eclipse.core.runtime.FileLocator
import org.eclipse.core.runtime.Path
import org.eclipse.core.runtime.Platform
import org.knime.base.data.append.column.AppendedColumnTable
import org.knime.core.data.DataColumnSpecCreator
import org.knime.core.data.DataTableSpec
import org.knime.core.data.DataType
import org.knime.core.data.container.DataContainer
import org.knime.core.data.`def`.DoubleCell
import org.knime.core.data.`def`.IntCell
import org.knime.core.data.`def`.StringCell
import org.knime.core.node.BufferedDataTable
import org.knime.core.node.CanceledExecutionException
import org.knime.core.node.ExecutionContext
import org.knime.core.node.ExecutionMonitor
import org.knime.core.node.InvalidSettingsException
import org.knime.core.node.NodeLogger
import org.knime.core.node.NodeModel
import org.knime.core.node.NodeSettingsRO
import org.knime.core.node.NodeSettingsWO
import org.jruby.embed.ScriptingContainer
import org.jruby.CompatVersion
import org.jruby.embed.LocalContextScope
import org.jruby.RubyInstanceConfig.CompileMode
import RubyScriptNodeModel._
import scala.collection.JavaConversions._
import org.knime.core.data.DataCell
import org.knime.core.data.`def`.DoubleCell
import org.knime.core.data.`def`.IntCell
import org.knime.ext.jruby.preferences.PreferenceConstants
import scala.collection.JavaConversions._
import scala.util.matching.Regex
import scala.collection.mutable.ArrayBuffer

/**
 * This is the model implementation of RubyScript.
 * 
 * This source code based on PythonScriptNodeModel.java from org.knime.ext.jython.source_2.9.0.0040102 by Tripos
 * 
 * @author rss
 * 
 */
object RubyScriptNodeModel {

  val SCRIPT = "script"
  val APPEND_COLS = "append_columns"
  val COLUMN_NAMES = "new_column_names"
  val COLUMN_TYPES = "new_column_types"

  private var logger: NodeLogger = 
    NodeLogger.getLogger(classOf[RubyScriptNodeModel])

  private var javaExtDirsExtensionsPath: String = _

  private var javaClasspathExtensionsPath: String = _

  def setJavaExtDirsExtensionPath(path: String) {
    javaExtDirsExtensionsPath = path
  }

  def getJavaExtDirsExtensionPath(): String = javaExtDirsExtensionsPath

  def setJavaClasspathExtensionPath(path: String) {
    javaClasspathExtensionsPath = path
  }

  def getJavaClasspathExtensionPath(): String = javaClasspathExtensionsPath
}

class RubyScriptNodeModel (
    var numInputs: Int,
    var numOutputs: Int,
    var snippetMode: Boolean
    ) extends NodeModel(numInputs, numOutputs) {

  protected var scriptHeader: String = ""
  protected var script: String = ""
  protected var scriptFooter: String = ""

  protected var scriptFirstLineNumber: Int = 1

  protected var appendCols: Boolean = true

  protected var columnNames = Array[String]()
  protected var columnTypes = Array[String]()

  class ScriptError {
    var lineNum: Int = -1
    var columnNum: Int = -1
    var errType: String = "--UnKnown--"
    var text: String = "--UnKnown--"
    var trace: String = ""
    var msg: String = ""
  }

  private var script_error: ScriptError = new ScriptError()

  def getErrorData(): ScriptError = script_error

  protected val templateFlowVar =
"""#
#  Flow variables:
#     puts FlowVariableList['knime.workspace'] # reading
#     FlowVariableList['filename'] = '1.txt'   # writing
#
"""

  protected val templateSnippet =
"""#
# Snippet intended for operations with one row.
# This code places in the special lambda function with argument named row.
# The lambda function must return the row by any available for Ruby ways.
#
# Example script. Add new two columns with String and Int types from current row:
#   row << (Cells.new.string('Hi!').int(row.getCell(0).to_s.length))
#
# Default snippet (copy existing row):
#

row
"""

  protected val templateScriptMultiInput =
"""# Example starter script.
# Add values for new two columns with String and Int types:
#
# count = $in_data_0.length
# $in_data_0.each_with_index do |row, i|
#   $out_data_0 << row << (Cells.new.string('Hi!').int(row.getCell(0).to_s.length))
#   setProgress "#{i*100/count}%" if i%100 != 0
# end
#
# Default script:
#


$in_data_0.each do |row|
    $out_data_0 << row
end
"""

  protected val templateScript =
"""# Example starter script.
# Add values for new two columns with String and Int types:
#
# count = 100000
# count.times do |i|
#   $out_data_0 << Cells.new.string('Hi!').int(rand i))
#   setProgress "#{i*100/count}%" if i%100 != 0
# end
#
# Default script:
#


10.times do |i|
    $outContainer << Cells.new.int(i)
end
"""

  var buffer = new StringBuilder()

  buffer ++= "require PLUGIN_PATH+'/rb/knime.rb'\n"

  if (snippetMode == true) {
    buffer ++= "func = ->(row) do \n"
    scriptFirstLineNumber += 1
  }
  scriptHeader = buffer.toString()

  buffer = new StringBuilder()
  buffer ++= "# Available scripting variables:\n"
  for (i <- 0 until numInputs) {
    buffer ++= "#     inData%d - input DataTable %d\n".format(i, i + 1)
  }
  buffer ++= "#     outContainer - container housing output DataTable (the same as outContainer0)\n"
  for (i <- 0 until numOutputs) {
    buffer ++= "#     outContainer%d - container housing output DataTable %d\n".format(i, i + 1)
  }

  buffer ++= templateFlowVar

  buffer ++= (snippetMode match {
    case true => templateSnippet
    case _ if (numInputs > 0) => templateScriptMultiInput
    case _ => templateScript
  })

  script = buffer.toString()

  if (snippetMode) {
    scriptFooter =
      "end\n" +
        "snippet_runner &func\n"
  }

  /* (non-Javadoc)
   * @see org.knime.core.node.NodeModel#execute(org.knime.core.node.BufferedDataTable[], org.knime.core.node.ExecutionContext)
   */
  override protected def execute(inData: Array[BufferedDataTable], exec: ExecutionContext): Array[BufferedDataTable] = {
    var i: Int = 0
    val outSpecs = configure(if (numInputs > 0) Array(inData(0).getDataTableSpec) else null)
    val outContainer = Array.tabulate(numOutputs) { i => new DataContainer(outSpecs(i)) }

    val fileSep = System.getProperty("file.separator")
    val core = Platform.getBundle("org.knime.core")
    val coreClassPath = core.getHeaders.get("Bundle-Classpath").toString
    val corePluginPath = FileLocator.resolve(FileLocator.find(core, new Path("."), null)).getPath
    val base = Platform.getBundle("org.knime.base")
    val baseClassPath = base.getHeaders.get("Bundle-Classpath").toString
    val basePluginPath = FileLocator.resolve(FileLocator.find(base, new Path("."), null)).getPath
    val ruby = Platform.getBundle("org.knime.ext.jruby")
    val rubyPluginPath = FileLocator.resolve(FileLocator.find(ruby, new Path("."), null)).getPath
    val ext = new StringBuffer()
    ext.append(basePluginPath + fileSep + "lib")
    ext.append(corePluginPath + fileSep + "lib")
    ext.append(getJavaExtDirsExtensionPath)

    val classpath = ArrayBuffer[String]()
    classpath ++= coreClassPath.split(",").view
      .map(s => FileLocator.find(core, new Path(s), null)).filter(_ != null)
      .map(FileLocator.resolve(_).getFile)

    classpath += corePluginPath + fileSep + "bin"
    baseClassPath.split(",").view
      .map(s => FileLocator.find(base, new Path(s), null)).filter(_ != null)
      .foreach { u => classpath.add(FileLocator.resolve(u).getFile) }

    classpath += basePluginPath + fileSep + "bin"
    classpath += getJavaClasspathExtensionPath
    if (RubyScriptNodePlugin.getDefault.getPreferenceStore.getBoolean(PreferenceConstants.JRUBY_USE_EXTERNAL_GEMS)) {
      val str = RubyScriptNodePlugin.getDefault.getPreferenceStore.getString(PreferenceConstants.JRUBY_PATH)
      System.setProperty("jruby.home", str)
    }
    var container = new ScriptingContainer(LocalContextScope.THREADSAFE)
    //container.setCompatVersion(CompatVersion.RUBY2_0)
    container.setCompileMode(CompileMode.JIT)
    container.setLoadPaths(classpath)
    container.setOutput(new LoggerOutputStream(logger, NodeLogger.LEVEL.WARN))
    container.setError(new LoggerOutputStream(logger, NodeLogger.LEVEL.ERROR))
    container.put("$num_inputs", numInputs)
    container.put("$input_datatable_arr", inData)
    for (i <- 0 until numInputs) {
      container.put("$inData%d".format(i), inData(i))
      container.put("$in_data_%d".format(i), inData(i))
    }
    container.put("$output_datatable_arr", outContainer)
    for (i <- 0 until numOutputs) {
      container.put("$outContainer%d".format(i), outContainer(i))
      container.put("$out_data_%d".format(i), outContainer(i))
    }
    container.put("$outContainer", outContainer(0))
    container.put("$outColumnNames", columnNames)
    container.put("$outColumnTypes", columnTypes)
    container.put("$num_outputs", numOutputs)
    container.put("$exec", exec)
    container.put("$node", this)
    container.put("PLUGIN_PATH", rubyPluginPath)
    val script_fn = "node_script.rb"
    try {
      script_error = new ScriptError()
      container.setScriptFilename(script_fn)
      val unit = container.parse(scriptHeader + script + scriptFooter, -scriptFirstLineNumber)
      unit.run()
    } catch {
      case e: Exception => {
        val p = Pattern.compile("SystemExit: ([0-9]+)")
        val matcher = p.matcher(e.toString)
        if (matcher.find()) {
          val exitCode = java.lang.Integer.parseInt(matcher.group(1))
          logger.debug("Exit code: " + exitCode)
        } else {
          findErrorSource(e, script_fn)
          logger.error("Script error in line: " + script_error.lineNum)
        }
        throw new CanceledExecutionException(e.getMessage)
      }
    }
    val result = Array.tabulate(numOutputs) { i =>
      outContainer(i).close()
      exec.createBufferedDataTable(outContainer(i).getTable, exec)
    }

    result
  }

  override protected def configure(inSpecs: Array[DataTableSpec]): Array[DataTableSpec] = {
    appendCols &= numInputs > 0
    var newSpec = if (appendCols) inSpecs(0) else new DataTableSpec()
    if (columnNames == null) {
      return Array(newSpec)
    }
    for (i <- 0 until columnNames.length) {
      var newColumnType = StringCell.TYPE
      var columnType:String = columnTypes(i) match {
        case "String" => classOf[StringCell].getName
        case "Integer" => classOf[IntCell].getName
        case "Double" => classOf[DoubleCell].getName
        case _ => columnTypes(i)
      }

      try {
        val cls = Class.forName(columnType)
        if (classOf[org.knime.core.data.DataCell].isAssignableFrom(cls))
            newColumnType = DataType.getType(cls.asInstanceOf[Class[_ <: org.knime.core.data.DataCell]])
        else
          throw new InvalidSettingsException(columnType + " does not extends org.knime.core.data.DataCell class.")
      } catch {
        case e: java.lang.ClassNotFoundException =>
          throw new InvalidSettingsException("The class " + columnType + " not found.")
      }

      if (columnTypes(i) != columnType) columnTypes(i) = columnType
      val newColumn = new DataColumnSpecCreator(columnNames(i), newColumnType).createSpec()
      newSpec = AppendedColumnTable.getTableSpec(newSpec, newColumn)
    }

    val result = for (i <- 0 until numOutputs) yield { newSpec }
    result.toArray
  }

  /* (non-Javadoc)
   * @see org.knime.core.node.NodeModel#loadInternals(java.io.File, org.knime.core.node.ExecutionMonitor)
   */
  protected override def loadInternals(nodeInternDir: File, exec: ExecutionMonitor) {
  }

  /* (non-Javadoc)
   * @see org.knime.core.node.NodeModel#saveInternals(java.io.File, org.knime.core.node.ExecutionMonitor)
   */
  protected override def saveInternals(nodeInternDir: File, exec: ExecutionMonitor) {
  }

  /* (non-Javadoc)
   * @see org.knime.core.node.NodeModel#saveSettingsTo(org.knime.core.node.NodeSettingsWO)
   */
  protected def saveSettingsTo(settings: NodeSettingsWO) {
    settings.addString(SCRIPT, script)
    settings.addBoolean(APPEND_COLS, appendCols)
    settings.addStringArray(COLUMN_NAMES, columnNames:_*)
    settings.addStringArray(COLUMN_TYPES, columnTypes:_*)
  }

  /* (non-Javadoc)
   * @see org.knime.core.node.NodeModel#loadValidatedSettingsFrom(org.knime.core.node.NodeSettingsRO)
   */
  protected def loadValidatedSettingsFrom(settings: NodeSettingsRO) {
    script = settings.getString(SCRIPT)
    appendCols = settings.getBoolean(APPEND_COLS, true)
    columnNames = Option(settings.getStringArray(COLUMN_NAMES)).getOrElse(Array())
    columnTypes = Option(settings.getStringArray(COLUMN_TYPES)).getOrElse(Array())
    script_error = new ScriptError()
  }

  /* (non-Javadoc)
   * @see org.knime.core.node.NodeModel#validateSettings(org.knime.core.node.NodeSettingsRO)
   */
  protected def validateSettings(settings: NodeSettingsRO) {
    settings.getString(SCRIPT)
    settings.getStringArray(COLUMN_NAMES)
    settings.getStringArray(COLUMN_TYPES)
  }

  /**
   *  Process an exception stack from JRuby.
   *  This methods searches a message at top of stack by any code
   *  from a filename with the value of filename parameter.
   *
   */
  private def findErrorSource(thr: Throwable, filename: String): Int = {
    val err = thr.getMessage
    if (err.startsWith("(SyntaxError)")) {
      """(?<=:)(\d+):(.*)""".r.findFirstMatchIn(err).foreach { m =>
            val line = m.group(1)
            val text = m.group(2)
            logger.debug("SyntaxError error line: " + line)
            if (text != null) script_error.text = text
            logger.debug("SyntaxError: " + script_error.text)
            script_error.lineNum = line.toInt
            script_error.columnNum = -1
            script_error.errType = "SyntaxError"
      }
    } else {
      script_error.errType = """(?<=\()(\w*)""".r
        .findFirstMatchIn(err).map(_ group 1).getOrElse(script_error.errType)

      val cause = thr.getCause
      cause.getStackTrace.find(line => line.getFileName == filename)
        .foreach { line =>
                script_error.text = cause.getMessage
                script_error.columnNum = -1
                script_error.lineNum = line.getLineNumber
                script_error.text = thr.getMessage
                script_error.errType = """(?<=org.knime.)(.*)(?=:)""".r
                  .findFirstMatchIn(err).map(_ group 1).getOrElse("RuntimeError")
          }
    }
    script_error.msg = "script" + (script_error.lineNum match {
      case -1 => "] stopped with error at line --unknown--"
      case _ =>
        " stopped with error in line " + script_error.lineNum +
          (if (script_error.columnNum != -1) " at column " + script_error.columnNum)
    })

    if (script_error.errType == "RuntimeError") {
      logger.error(script_error.msg + "\n" + script_error.errType + " ( " + script_error.text + " )")
      val cause = thr.getCause
      val stack = cause.getStackTrace
      val builder = new StringBuilder()
      for (line <- stack) {
        builder.append(line.getLineNumber)
        builder.append(":\t")
        builder.append(line.getClassName)
        builder.append(" ( ")
        builder.append(line.getMethodName)
        builder.append(" )\t")
        builder.append(line.getFileName)
        builder.append('\n')
      }
      script_error.trace = builder.toString
      if (script_error.trace.length > 0) {
        logger.error(
            "\n--- Traceback --- error source first\n" +
            "line:   class ( method )    file \n" +
            script_error.trace +
            "--- Traceback --- end --------------")
      }
    } else if (script_error.errType != "SyntaxError") {
      logger.error(script_error.msg)
      logger.error("Could not evaluate error source nor reason. Analyze StackTrace!")
      logger.error(err)
    }
    script_error.lineNum
  }

  /* (non-Javadoc)
   * @see org.knime.core.node.NodeModel#reset()
   */
  protected def reset() {
  }
}
