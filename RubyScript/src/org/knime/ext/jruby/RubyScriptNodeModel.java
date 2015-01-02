/**
 * This is the model implementation of RubyScript.
 * 
 * This source code based on PythonScriptNodeModel.java from org.knime.ext.jython.source_2.9.0.0040102 by Tripos
 * 
 * @author rss
 * 
 */

package org.knime.ext.jruby;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.ext.jruby.preferences.PreferenceConstants;
import org.osgi.framework.Bundle;

import org.jruby.embed.ScriptingContainer;
import org.jruby.javasupport.JavaEmbedUtils.EvalUnit;
import org.jruby.CompatVersion;
import org.jruby.embed.LocalContextScope;
import org.jruby.RubyInstanceConfig.CompileMode;

public class RubyScriptNodeModel extends NodeModel {

    public static final String SCRIPT = "script";
    public static final String APPEND_COLS = "append_columns";
    public static final String COLUMN_NAMES = "new_column_names";
    public static final String COLUMN_TYPES = "new_column_types";

    protected int m_numInputs = 0;
    protected int m_numOutputs = 0;

    /**
     * our logger instance.
     */
    private static NodeLogger m_logger = NodeLogger
            .getLogger(RubyScriptNodeModel.class);

    protected String m_scriptHeader = "";
    protected String m_scriptFooter = "";
    protected String m_script = "";
    protected int m_scriptFirstLineNumber;

    protected boolean m_appendCols = true;
    protected String[] m_columnNames;
    protected String[] m_columnTypes;
    private ArrayList<DataColumnSpec> m_inputColumnList = null;

    private static String m_javaExtDirsExtensionsPath;
    private static String m_javaClasspathExtensionsPath;

    private boolean m_snippetMode;

    private static Object m_ScriptingContainerLock = new Object();
    
    public class ScriptError {
        public int lineNum;
        public int columnNum;
        public String type;
        public String text;
        public String trace;
        public String msg;        

        public ScriptError() {
          clear();
        }
        
        public void clear() {
            lineNum = -1;
            columnNum = -1;
            type = "--UnKnown--";
            text = "--UnKnown--";
            trace = "";
            msg = "";
        }
    }
    
    private ScriptError m_script_error;
    
    public ScriptError getErrorData() {
        return m_script_error;
    }

    protected RubyScriptNodeModel(int inNumInputs, int inNumOutputs, boolean snippetMode) {
        super(inNumInputs, inNumOutputs);

        m_numInputs = inNumInputs;
        m_numOutputs = inNumOutputs;
        m_snippetMode = snippetMode;

        m_script_error = new ScriptError();

        // define the common imports string
        StringBuffer buffer = new StringBuffer();
        buffer.append("require PLUGIN_PATH+'/rb/knime.rb'\n");
        m_scriptFirstLineNumber = 1;

        if (m_snippetMode == true ) {
            buffer.append("func = ->(row) do \n");
            m_scriptFirstLineNumber += 1;
        }

        m_scriptHeader = buffer.toString();

        buffer = new StringBuffer();
        buffer.append("# Available scripting variables:\n");
        for (int i = 0; i < m_numInputs; i++) {
            buffer.append(String.format(
                    "#     inData%d - input DataTable %d\n", i, i + 1));
        }
        buffer.append("#     outContainer - container housing output DataTable"
                + " (the same as outContainer0)\n");

        for (int i = 0; i < m_numOutputs; i++) {
            buffer.append(String
                    .format("#     outContainer%d - container housing output DataTable %d\n", i, i+1));
        }
        buffer.append("#\n");

        if (m_snippetMode) {
            buffer.append("# Snippet intended for operations with one row.\n"
                    + "# This code places in the special lambda function with argument named row.\n"
                    + "# The lambda function must return the row by any available for Ruby ways.\n"
                    + "#\n"
                    + "# Example script. "
                    + "Add new two columns with String and Int types from current row:\n"
                    + "#   row << (Cells.new.string('Hi!').int(row.getCell(0).to_s.length))\n"
                    + "#\n");

            buffer.append("# Default snippet (copy existing row):\n");
            buffer.append("#\n\n");

            buffer.append("  row");

        } else {
            if (m_numInputs > 0) {
                buffer.append("# Example starter script. "
                        + "Add values for new two columns with String and Int types:\n"
                        + "#\n"
                        + "# count = $in_data_0.length\n"
                        + "# $in_data_0.each_with_index do |row, i|\n"
                        + "#   $out_data_0 << "
                        + "row << (Cells.new.string('Hi!').int(row.getCell(0).to_s.length))\n"
                        + "#   setProgress \"#{i*100/count}%\" if i%100 != 0\n"
                        + "# end\n" + "#\n");
               buffer.append("# Default script:\n");
                buffer.append("#\n\n");

                buffer.append("$in_data_0.each do |row|\n");
                buffer.append("    $out_data_0 << row\n");
                buffer.append("end");
            } else {
                buffer.append("# Example starter script. "
                        + "Add values for new two columns with String and Int types:\n");
                buffer.append("#\n");
                buffer.append("# count = 100000\n");
                buffer.append("# count.times do |i|\n");
                buffer.append("#   $out_data_0 << Cells.new.string('Hi!').int(rand i))\n");
                buffer.append("#   setProgress \"#{i*100/count}%\" if i%100 != 0\n");
                buffer.append("# end\n");
                buffer.append("#\n");
                buffer.append("# Default script:\n");
                buffer.append("#\n\n");

                buffer.append("10.times do |i|\n");
                buffer.append("    $outContainer << Cells.new.int(i)\n");
                buffer.append("end");
            }
        }
        m_script = buffer.toString();

        if (m_snippetMode) {
            buffer = new StringBuffer();
            buffer.append("end\n");
            buffer.append("snippet_runner &func\n");
            m_scriptFooter = buffer.toString();
        }        
    }

    protected final BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {

        int i;

        // construct the output data table specs and the output containers
        DataTableSpec[] outSpecs = configure(m_numInputs > 0 ? 
                new DataTableSpec[] { inData[0].getDataTableSpec() } : 
                    null);

        DataContainer[] outContainer = new DataContainer[m_numOutputs];
        for (i = 0; i < m_numOutputs; i++) {
            outContainer[i] = new DataContainer(outSpecs[i]);
        }

        String fileSep = System.getProperty("file.separator");

        // construct all necessary paths
        Bundle core = Platform.getBundle("org.knime.core");
        String coreClassPath = core.getHeaders().get("Bundle-Classpath")
                .toString();
        String corePluginPath = FileLocator.resolve(
                FileLocator.find(core, new Path("."), null)).getPath();

        Bundle base = Platform.getBundle("org.knime.base");
        String baseClassPath = base.getHeaders().get("Bundle-Classpath")
                .toString();
        String basePluginPath = FileLocator.resolve(
                FileLocator.find(base, new Path("."), null)).getPath();

        Bundle ruby = Platform.getBundle("org.knime.ext.jruby");
        String rubyPluginPath = FileLocator.resolve(
                FileLocator.find(ruby, new Path("."), null)).getPath();

        // set up ext dirs
        StringBuffer ext = new StringBuffer();
        ext.append(basePluginPath + fileSep + "lib");
        ext.append(corePluginPath + fileSep + "lib");
        ext.append(getJavaExtDirsExtensionPath());

        // set up the classpath
        List<String> classpath = new ArrayList<String>();
        for (String s : coreClassPath.split(",")) {
            URL u = FileLocator.find(core, new Path(s), null);
            if (u != null) {
                classpath.add(FileLocator.resolve(u).getFile());
            }
        }
        // this entry is necessary if KNIME is started from Eclipse SDK
        classpath.add(corePluginPath + fileSep + "bin");

        for (String s : baseClassPath.split(",")) {
            URL u = FileLocator.find(base, new Path(s), null);
            if (u != null) {
                classpath.add(FileLocator.resolve(u).getFile());
            }
        }
        // this entry is necessary if KNIME is started from Eclipse SDK
        classpath.add(basePluginPath + fileSep + "bin");

        classpath.add(getJavaClasspathExtensionPath());

        if (RubyScriptNodePlugin.getDefault().getPreferenceStore()
                .getBoolean(PreferenceConstants.JRUBY_USE_EXTERNAL_GEMS)) {
            String str = RubyScriptNodePlugin.getDefault().getPreferenceStore()
                    .getString(PreferenceConstants.JRUBY_PATH);
            System.setProperty("jruby.home", str);
        }

        // File(RubyScriptNodeModel.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();

        ScriptingContainer container;

        // **** JRuby 1.7.13 workaround ******
        // Container creation is failed for the first of two parallel executed.
        // Fails only first time!
        // ***********************************
        synchronized(m_ScriptingContainerLock){
        container = new ScriptingContainer(
                LocalContextScope.THREADSAFE);
        container.setCompatVersion(CompatVersion.RUBY2_0);
        container.setCompileMode(CompileMode.JIT);

        // Code for classpath inherited from jythonscript. It`s possible
        // redundant paths.
        container.setLoadPaths(classpath);

        container.setOutput(new LoggerOutputStream(m_logger,
                NodeLogger.LEVEL.WARN));
        container.setError(new LoggerOutputStream(m_logger,
                NodeLogger.LEVEL.ERROR));

        // ********** Configuring of global variables ***************
        container.put("$num_inputs", m_numInputs);
        container.put("$input_datatable_arr", inData);

        for (i = 0; i < m_numInputs; i++) {
            container.put(String.format("$inData%d", i), inData[i]);
            container.put(String.format("$in_data_%d", i), inData[i]);
        }

        container.put("$output_datatable_arr", outContainer);
        for (i = 0; i < m_numOutputs; i++) {
            container.put(String.format("$outContainer%d", i), outContainer[i]);
            container.put(String.format("$out_data_%d", i), outContainer[i]);
        }
        container.put("$outContainer", outContainer[0]);

        container.put("$outColumnNames", m_columnNames);
        container.put("$outColumnTypes", m_columnTypes);
        container.put("$num_outputs", m_numOutputs);

        container.put("$exec", exec);
        container.put("$node", this);
        container.put("PLUGIN_PATH", rubyPluginPath);
        }

        // ********** Script execution ***************
        String script_fn = "node_script.rb";
        try {
            m_script_error.clear();
            container.setScriptFilename(script_fn);
            EvalUnit unit = container.parse(m_scriptHeader + m_script
                    + m_scriptFooter,
                    -m_scriptFirstLineNumber // fix first string number
                    );
            unit.run();
        } catch (Exception e) {
            Pattern p = Pattern.compile("SystemExit: ([0-9]+)");
            Matcher matcher = p.matcher(e.toString());
            if (matcher.find()) {
                int exitCode = Integer.parseInt(matcher.group(1));
                m_logger.debug("Exit code: " + exitCode);
            } else {
                findErrorSource(e, script_fn);
                m_logger.error("Script error in line: "
                        + m_script_error.lineNum);
            }
            throw new CanceledExecutionException(e.getMessage());
        }

        // Output result preparing
        BufferedDataTable[] result = new BufferedDataTable[m_numOutputs];
        for (i = 0; i < m_numOutputs; i++) {
            outContainer[i].close();
            result[i] = exec.createBufferedDataTable(
                    outContainer[i].getTable(), exec);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    protected final DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

        m_appendCols &= m_numInputs > 0;
        // append the property columns to the data table spec
        DataTableSpec newSpec = m_appendCols ? inSpecs[0] : new DataTableSpec();

        if (m_numInputs > 0) {
            m_inputColumnList = new ArrayList<DataColumnSpec>();
            for (Iterator<DataColumnSpec> item = inSpecs[0].iterator(); item
                    .hasNext();) {
                m_inputColumnList.add(item.next());
            }
        }

        if (m_columnNames == null) {
            return new DataTableSpec[] { newSpec };
        }

        for (int i = 0; i < m_columnNames.length; i++) {
            DataType type = StringCell.TYPE;
            String columnType = m_columnTypes[i];

            // convert short classes names
            if ("String".equals(columnType)) {
                columnType = StringCell.class.getName();
            } else if ("Integer".equals(columnType)) {
                columnType = IntCell.class.getName();
            } else if ("Double".equals(columnType)) {
                columnType = DoubleCell.class.getName();
            }

            try {
                Class cls = Class.forName(columnType);
                if (org.knime.core.data.DataCell.class.isAssignableFrom(cls))
                    type = DataType.getType(cls);
                else
                    throw new InvalidSettingsException (columnType
                            + " does not extend org.knime.core.data.DataCell class.");


            } catch (ClassNotFoundException e) {
                // e.printStackTrace();
                throw new InvalidSettingsException (columnType
                        + " is an incorrect Java class name. "
                        + "Please check it and specify a fully qualified class name.");

                //columnType = "StringCell";
            }

            if (!m_columnTypes[i].equals(columnType))
                m_columnTypes[i] = columnType;

            DataColumnSpec newColumn = new DataColumnSpecCreator(
                    m_columnNames[i], type).createSpec();

            newSpec = AppendedColumnTable.getTableSpec(newSpec, newColumn);
        }

        if (m_script == null) {
            m_script = "";
        }

        DataTableSpec[] result = new DataTableSpec[m_numOutputs];
        for (int i = 0; i < m_numOutputs; i++) {
            result[i] = newSpec;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Nothing to load.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals to save
    }

    /**
     * {@inheritDoc}
     */
    protected final void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(SCRIPT, m_script);
        settings.addBoolean(APPEND_COLS, m_appendCols);
        settings.addStringArray(COLUMN_NAMES, m_columnNames);
        settings.addStringArray(COLUMN_TYPES, m_columnTypes);
    }

    /**
     * {@inheritDoc}
     */
    protected final void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_script = settings.getString(SCRIPT);
        // since 1.3
        m_appendCols = settings.getBoolean(APPEND_COLS, true);
        m_columnNames = settings.getStringArray(COLUMN_NAMES);
        m_columnTypes = settings.getStringArray(COLUMN_TYPES);

        m_script_error.clear();
    }

    /**
     * {@inheritDoc}
     */
    protected final void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getString(SCRIPT);
        settings.getStringArray(COLUMN_NAMES);
        settings.getStringArray(COLUMN_TYPES);
    }

    public static void setJavaExtDirsExtensionPath(String path) {
        m_javaExtDirsExtensionsPath = path;
    }

    public static String getJavaExtDirsExtensionPath() {
        return m_javaExtDirsExtensionsPath;
    }

    public static void setJavaClasspathExtensionPath(String path) {
        m_javaClasspathExtensionsPath = path;
    }

    public static String getJavaClasspathExtensionPath() {
        return m_javaClasspathExtensionsPath;
    }

    /**
     *  Process exception stack from JRuby.
     *  This methods searches a message at top of stack by any code 
     *  from a filename with the value of filename parameter.  
     *
    */
    private int findErrorSource(Throwable thr, String filename) {
        String err = thr.getMessage();

        if (err.startsWith("(SyntaxError)")) {
            // org.jruby.parser.ParserSyntaxException
            // (SyntaxError) script.rb:2: syntax error, unexpected tRCURLY

            Pattern pLineS = Pattern.compile("(?<=:)(\\d+):(.*)");
            Matcher mLine = pLineS.matcher(err);
            if (mLine.find()) {
                m_logger.debug("SyntaxError error line: " + mLine.group(1));
                m_script_error.text = mLine.group(2) == null ? m_script_error.text
                        : mLine.group(2);
                m_logger.debug("SyntaxError: " + m_script_error.text);
                m_script_error.lineNum = Integer.parseInt(mLine.group(1));
                m_script_error.columnNum = -1;
                m_script_error.type = "SyntaxError";
            }
        } else {
            // if (err.startsWith("(NameError)")) {
            // org.jruby.embed.EvalFailedException
            // (NameError) undefined local variable or method `asdf' for
            // main:Object

            Pattern type = Pattern.compile("(?<=\\()(\\w*)");
            Matcher mLine = type.matcher(err);
            if (mLine.find()) {
                m_script_error.type = mLine.group(1);
            }
            Throwable cause = thr.getCause();
            // cause.printStackTrace();
            for (StackTraceElement line : cause.getStackTrace()) {
                if (line.getFileName().equals(filename)) {
                    m_script_error.text = cause.getMessage();
                    m_script_error.columnNum = -1;
                    m_script_error.lineNum = line.getLineNumber();
                    m_script_error.text = thr.getMessage();

                    Pattern knimeType = Pattern
                            .compile("(?<=org.knime.)(.*)(?=:)");
                    Matcher mKnimeType = knimeType
                            .matcher(m_script_error.text);

                    if (mKnimeType.find()) {
                        m_script_error.type = mKnimeType.group(1);
                    }

                    m_script_error.type = "RuntimeError";

                    break;
                }
            }
        }

        m_script_error.msg = "script";
        if (m_script_error.lineNum != -1) {
            m_script_error.msg += " stopped with error in line "
                    + m_script_error.lineNum;
            if (m_script_error.columnNum != -1) {
                m_script_error.msg += " at column "
                        + m_script_error.columnNum;
            }
        } else {
            m_script_error.msg += "] stopped with error at line --unknown--";
        }

        if (m_script_error.type == "RuntimeError") {
            m_logger.error(m_script_error.msg + "\n" + m_script_error.type
                    + " ( " + m_script_error.text + " )");

            Throwable cause = thr.getCause();
            // cause.printStackTrace();
            StackTraceElement[] stack = cause.getStackTrace();
            /*
             * StringWriter writer = new StringWriter(); PrintWriter out = new
             * PrintWriter(writer); cause.printStackTrace(out); errorTrace =
             * writer.toString();
             */
            StringBuilder builder = new StringBuilder();
            for (StackTraceElement line : stack) {
                builder.append(line.getLineNumber());
                builder.append(":\t");
                builder.append(line.getClassName());
                builder.append(" ( ");
                builder.append(line.getMethodName());
                builder.append(" )\t");
                builder.append(line.getFileName());
                builder.append('\n');
            }

            m_script_error.trace = builder.toString();
            if (m_script_error.trace.length() > 0) {
                m_logger.error("\n--- Traceback --- error source first\n"
                        + "line:   class ( method )    file \n"
                        + m_script_error.trace
                        + "--- Traceback --- end --------------");
            }

        } else if (m_script_error.type != "SyntaxError") {
            m_logger.error(m_script_error.msg);
            m_logger.error("Could not evaluate error source nor reason. Analyze StackTrace!");
            m_logger.error(err);
        }
        return m_script_error.lineNum;
    }

    public ArrayList<DataColumnSpec> getInputColumnList() {
        return m_inputColumnList;
    }
}
