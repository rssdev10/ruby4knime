package org.knime.ext.jruby

import org.knime.core.node.NodeView

/**
 * <code>NodeView</code> for the "RubyScript" Node.
 * 
 * @author rss
 *
 */
class RubyScriptNodeView protected (nodeModel: RubyScriptNodeModel) extends NodeView[RubyScriptNodeModel](nodeModel) {

  /* (non-Javadoc)
   * @see org.knime.core.node.AbstractNodeView#modelChanged()
   */
  protected override def modelChanged() {
    assert(getNodeModel.asInstanceOf[RubyScriptNodeModel] != null)
  }

  /* (non-Javadoc)
   * @see org.knime.core.node.NodeView#onClose()
   */
  protected override def onClose() {
  }

  /* (non-Javadoc)
   * @see org.knime.core.node.NodeView#onOpen()
   */
  protected override def onOpen() {
  }
}
