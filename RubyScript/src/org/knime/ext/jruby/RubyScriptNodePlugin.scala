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
}
