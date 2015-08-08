package org.knime.ext.jruby

/**
 * <code>NodeFactory</code> for the "RubyScript" Node.
 * 
 * @author rss
 *
 */
class RubyScriptNodeFactory01 extends RubyScriptNodeFactory {

  /* (non-Javadoc)
   * @see org.knime.ext.jruby.RubyScriptNodeFactory#createNodeModel()
   */
  override def createNodeModel(): RubyScriptNodeModel = {
    setModel(new RubyScriptNodeModel(0, 1, false))
  }
}
