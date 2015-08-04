package org.knime.ext.jruby

import org.eclipse.ui.plugin.AbstractUIPlugin
import org.osgi.framework.BundleContext
import RubyScriptNodePlugin._

/**
 * This is the eclipse bundle activator. Note: KNIME node developers probably
 * won't have to do anything in here, as this class is only needed by the
 * eclipse platform/plugin mechanism. If you want to move/rename this file, make
 * sure to change the plugin.xml file in the project root directory accordingly.
 * 
 * @author rss
 */
object RubyScriptNodePlugin {

  private var plugin: RubyScriptNodePlugin = _

  def getDefault(): RubyScriptNodePlugin = plugin
}

class RubyScriptNodePlugin extends AbstractUIPlugin() {

  plugin = this

  /* (non-Javadoc)
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
   */
  override def start(context: BundleContext) {
    super.start(context)
    plugin = this
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
   */
  override def stop(context: BundleContext) {
    super.stop(context)
    plugin = null
  }

/*
Original Java:
|* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 *
 *|
package org.knime.ext.jruby;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

|**
 * This is the eclipse bundle activator. Note: KNIME node developers probably
 * won't have to do anything in here, as this class is only needed by the
 * eclipse platform/plugin mechanism. If you want to move/rename this file, make
 * sure to change the plugin.xml file in the project root directory accordingly.
 * 
 * @author
 *|
public class RubyScriptNodePlugin extends AbstractUIPlugin {
    // The shared instance.
    private static RubyScriptNodePlugin plugin;

    |**
     * The constructor.
     *|
    public RubyScriptNodePlugin() {
        super();
        plugin = this;
    }

    |**
     * This method is called upon plug-in activation.
     * 
     * @param context
     *            The OSGI bundle context
     * @throws Exception
     *             If this plugin could not be started
     *|
    @Override
    public final void start(final BundleContext context) throws Exception {
        super.start(context);
    }

    |**
     * This method is called when the plug-in is stopped.
     * 
     * @param context
     *            The OSGI bundle context
     * @throws Exception
     *             If this plugin could not be stopped
     *|
    @Override
    public final void stop(final BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
    }

    |**
     * Returns the shared instance.
     * 
     * @return Singleton instance of the Plugin
     *|
    public static RubyScriptNodePlugin getDefault() {
        return plugin;
    }
}

*/
}