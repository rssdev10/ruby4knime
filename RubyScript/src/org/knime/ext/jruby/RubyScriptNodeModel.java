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
import java.util.List;
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
    protected int numInputs = 0;
    protected int numOutputs = 0;

    /**
     * our logger instance.
     */
    private static NodeLogger logger = NodeLogger
            .getLogger(RubyScriptNodeModel.class);
    protected String scriptHeader = "";
    protected String scriptFooter = "";
    protected String script = "";
    protected boolean appendCols = true;
    protected String[] columnNames;
    protected String[] columnTypes;
    private static String javaExtDirsExtensionsPath;
    private static String javaClasspathExtensionsPath;

    protected RubyScriptNodeModel(int inNumInputs, int inNumOutputs) {
        super(inNumInputs, inNumOutputs);

        this.numInputs = inNumInputs;
        this.numOutputs = inNumOutputs;

        // define the common imports string
        StringBuffer buffer = new StringBuffer();
        buffer.append("require PLUGIN_PATH+'/rb/knime.rb'\n");
        scriptHeader = buffer.toString();

        buffer = new StringBuffer();
        buffer.append("# Available scripting variables:\n");
        for (int i = 0; i < numInputs; i++) {
            buffer.append(String.format(
                    "#     inData%d - input DataTable %d\n", i, i + 1));
        }

        if (numInputs > 0) {
            buffer.append("#     outContainer - container housing output DataTable\n");
            buffer.append("#\n");
            buffer.append("# Example starter script. Add values for new two columns with String and Int types:\n");
            buffer.append("#\n");
            buffer.append("# count = $inData0.length\n");
            buffer.append("# $inData0.each_with_index do |row, i|\n");
            buffer.append("#     $outContainer << row << (Cells.new.string('Hi!').int(row.getCell(0).to_s.length))\n");
            buffer.append("#     setProgress \"#{i*100/count}%\" if i%100 != 0\n");
            buffer.append("# end\n");
            buffer.append("#\n");
            buffer.append("# Default script:\n");
            buffer.append("#\n\n");
            buffer.append("$inData0.each do |row|\n");
            buffer.append("    $outContainer << row\n");
            buffer.append("end");
        } else {
            buffer.append("#     outContainer - container housing output DataTable\n");
            buffer.append("#\n");
            buffer.append("# Example starter script. Add values for new two columns with String and Int types:\n");
            buffer.append("#\n");
            buffer.append("# count = 100000\n");
            buffer.append("# count.times do |i|\n");
            buffer.append("#     $outContainer << Cells.new.string('Hi!').int(rand i))\n");
            buffer.append("#     setProgress \"#{i*100/count}%\" if i%100 != 0\n");
            buffer.append("# end\n");
            buffer.append("#\n");
            buffer.append("# Default script:\n");
            buffer.append("#\n\n");

            buffer.append("10.times do |i|\n");
            buffer.append("    $outContainer << Cells.new.int(i)\n");
            buffer.append("end");
        }
        script = buffer.toString();
    }

    protected final BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {

        int i;
        BufferedDataTable[] in = (numInputs > 0 ? new BufferedDataTable[numInputs]
                : null);

        for (i = 0; i < numInputs; i++) {
            in[i] = inData[i];
        }

        // construct the output data table specs and the output containers
        DataTableSpec[] outSpecs = configure(in != null ? new DataTableSpec[] { in[0]
                .getDataTableSpec() } : null);

        DataContainer[] outContainer = new DataContainer[numOutputs];
        for (i = 0; i < numOutputs; i++) {
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

        ScriptingContainer container = new ScriptingContainer(
                LocalContextScope.THREADSAFE);
        container.setCompatVersion(CompatVersion.RUBY2_0);
        container.setCompileMode(CompileMode.JIT);

        // Code for classpath inherited from jythonscript. It`s possible
        // redundant paths.
        container.setLoadPaths(classpath);

        container.setOutput(new LoggerOutputStream(logger,
                NodeLogger.LEVEL.INFO));
        container.setError(new LoggerOutputStream(logger,
                NodeLogger.LEVEL.ERROR));

        for (i = 0; i < numInputs; i++) {
            container.put(String.format("$inData%d", i), in[i]);
        }

        for (i = 0; i < numOutputs; i++) {
            container.put(String.format("$outContainer%d", i), outContainer[i]);
        }
        container.put("$outContainer", outContainer[0]);

        container.put("$outColumnNames", columnNames);
        container.put("$outColumnTypes", columnTypes);
        container.put("$exec", exec);
        container.put("PLUGIN_PATH", rubyPluginPath);

        EvalUnit unit = container
                .parse(scriptHeader + script + scriptFooter, 1);
        unit.run();

        BufferedDataTable[] result = new BufferedDataTable[numOutputs];
        for (i = 0; i < numOutputs; i++) {
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
        appendCols &= numInputs > 0;
        // append the property columns to the data table spec
        DataTableSpec newSpec = appendCols ? inSpecs[0] : new DataTableSpec();

        if (columnNames == null) {
            return new DataTableSpec[] { newSpec };
        }

        for (int i = 0; i < columnNames.length; i++) {
            DataType type = StringCell.TYPE;
            String columnType = columnTypes[i];

            if ("String".equals(columnType)) {
                type = StringCell.TYPE;
            } else if ("Integer".equals(columnType)) {
                type = IntCell.TYPE;
            } else if ("Double".equals(columnType)) {
                type = DoubleCell.TYPE;
            }
            DataColumnSpec newColumn = new DataColumnSpecCreator(
                    columnNames[i], type).createSpec();

            newSpec = AppendedColumnTable.getTableSpec(newSpec, newColumn);
        }

        if (script == null) {
            script = "";
        }

        DataTableSpec[] result = new DataTableSpec[numOutputs];
        for (int i = 0; i < numOutputs; i++) {
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
        settings.addString(SCRIPT, script);
        settings.addBoolean(APPEND_COLS, appendCols);
        settings.addStringArray(COLUMN_NAMES, columnNames);
        settings.addStringArray(COLUMN_TYPES, columnTypes);
    }

    /**
     * {@inheritDoc}
     */
    protected final void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        script = settings.getString(SCRIPT);
        // since 1.3
        appendCols = settings.getBoolean(APPEND_COLS, true);
        columnNames = settings.getStringArray(COLUMN_NAMES);
        columnTypes = settings.getStringArray(COLUMN_TYPES);
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
        javaExtDirsExtensionsPath = path;
    }

    public static String getJavaExtDirsExtensionPath() {
        return javaExtDirsExtensionsPath;
    }

    public static void setJavaClasspathExtensionPath(String path) {
        javaClasspathExtensionsPath = path;
    }

    public static String getJavaClasspathExtensionPath() {
        return javaClasspathExtensionsPath;
    }
}
