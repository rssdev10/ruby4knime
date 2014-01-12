package org.knime.ext.jruby;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "RubyScript" Node.
 * 
 *
 * @author 
 */
public class RubyScriptNodeFactory 
        extends NodeFactory<RubyScriptNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public RubyScriptNodeModel createNodeModel() {
        return new RubyScriptNodeModel(1,1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<RubyScriptNodeModel> createNodeView(final int viewIndex,
            final RubyScriptNodeModel nodeModel) {
        return new RubyScriptNodeView(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new RubyScriptNodeDialog();
    }

}
