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

	// our logger instance
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
		buffer.append("#     inData0 - input DataTable 0\n");
		if (numInputs == 2) {
			buffer.append("#     inData1 - input DataTable 1\n");
		}
		buffer.append("#     outContainer - container housing output DataTable\n");
		buffer.append("#\n");
		buffer.append("# Example starter script. Add values for new two columns with String and Int types:\n");
		buffer.append("#\n");
		buffer.append("# count = $inData0.length\n");
		buffer.append("# $outContainer << (row << Cells.new.string('Hi!').int(row.getCell(0).to_s.length))");
		buffer.append("#     $outContainer << row.string('Hi!').int(row.getCell(0).to_s.length).append\n");
		buffer.append("#     setProgress \"#{i*100/count}%\" if i%100 != 0\n");
		buffer.append("# end\n");
		buffer.append("#\n");
		buffer.append("# Default script:\n");
		buffer.append("#\n");
		buffer.append("$inData0.each do |row|\n");
		buffer.append("    $outContainer << row\n");
		buffer.append("end");		

		script = buffer.toString();
	}

	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws CanceledExecutionException,
			Exception {
		BufferedDataTable in = inData[0];
		BufferedDataTable in2 = null;
		if (numInputs == 2) {
			in2 = inData[1];
		}

		// construct the output data table specs and the output containers
		DataTableSpec[] outSpecs = configure(new DataTableSpec[] { in
				.getDataTableSpec() });
		DataContainer outContainer = new DataContainer(outSpecs[0]);
		DataContainer outContainer2 = null;
		if (numOutputs == 2) {
			outContainer2 = new DataContainer(outSpecs[1]);
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
				LocalContextScope.CONCURRENT);
		container.setCompatVersion(CompatVersion.RUBY2_0);
		container.setCompileMode(CompileMode.JIT);

		// Code for classpath inherited from jythonscript. It`s possible redundant paths.   
		container.setLoadPaths(classpath);

		container.setOutput(new LoggerOutputStream(logger,
				NodeLogger.LEVEL.INFO));
		container.setError(new LoggerOutputStream(logger,
				NodeLogger.LEVEL.ERROR));

		container.put("$inData0", in);
		if (numInputs == 2) {
			container.put("$inData1", in2);
		}
		container.put("$outContainer", outContainer);
		container.put("$outColumnNames", columnNames);
		container.put("$outColumnTypes", columnTypes);
		container.put("$exec", exec);
		container.put("PLUGIN_PATH", rubyPluginPath);

		EvalUnit unit = container
				.parse(scriptHeader + script + scriptFooter, 1);
		unit.run();

		outContainer.close();
		if (outContainer2 != null) {
			outContainer2.close();
		}
		if (numOutputs == 2) {
			return new BufferedDataTable[] {
					exec.createBufferedDataTable(outContainer.getTable(), exec),
					exec.createBufferedDataTable(outContainer.getTable(), exec) };
		}
		return new BufferedDataTable[] { exec.createBufferedDataTable(
				outContainer.getTable(), exec) };
	}

	/**
	 * {@inheritDoc}
	 */
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
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

		if (numOutputs == 2) {
			return new DataTableSpec[] { newSpec, newSpec };
		}
		return new DataTableSpec[] { newSpec };
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
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		settings.addString(SCRIPT, script);
		settings.addBoolean(APPEND_COLS, appendCols);
		settings.addStringArray(COLUMN_NAMES, columnNames);
		settings.addStringArray(COLUMN_TYPES, columnTypes);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
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
	protected void validateSettings(final NodeSettingsRO settings)
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
