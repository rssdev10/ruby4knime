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

/*
Original Java:
package org.knime.ext.jruby;

import org.knime.core.node.NodeView;

|**
 * <code>NodeView</code> for the "RubyScript" Node.
 * 
 * 
 * @author
 *|
public class RubyScriptNodeView extends NodeView<RubyScriptNodeModel> {

    |**
     * Creates a new view.
     * 
     * @param nodeModel
     *            The model (class: {@link RubyScriptNodeModel})
     *|
    protected RubyScriptNodeView(final RubyScriptNodeModel nodeModel) {
        super(nodeModel);

        // TODO instantiate the components of the view here.

    }

    |**
     * {@inheritDoc}
     *|
    @Override
    protected final void modelChanged() {

        // TODO retrieve the new model from your nodemodel and
        // update the view.
        RubyScriptNodeModel nodeModel = (RubyScriptNodeModel) getNodeModel();
        assert nodeModel != null;

        // be aware of a possibly not executed nodeModel! The data you retrieve
        // from your nodemodel could be null, emtpy, or invalid in any kind.

    }

    |**
     * {@inheritDoc}
     *|
    @Override
    protected void onClose() {

        // TODO things to do when closing the view
    }

    |**
     * {@inheritDoc}
     *|
    @Override
    protected void onOpen() {

        // TODO things to do when opening the view
    }

}

*/
}