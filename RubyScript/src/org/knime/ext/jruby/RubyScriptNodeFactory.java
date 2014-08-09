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
/**
 * @author rss
 *
 */
public class RubyScriptNodeFactory extends NodeFactory<RubyScriptNodeModel> {

    private RubyScriptNodeModel m_model;
    private RubyScriptNodeDialog m_dialog;

    protected RubyScriptNodeModel setModel(RubyScriptNodeModel model) {
        m_model = model;
        return model;
    }
    
    public RubyScriptNodeModel getModel() {
        return m_model;
    }

    public RubyScriptNodeDialog getDialog() {
        return m_dialog;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public RubyScriptNodeModel createNodeModel() {
        return setModel(new RubyScriptNodeModel(1, 1, false)); 
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getNrNodeViews() {
        return 1;
    }

//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public final NodeView<RubyScriptNodeModel> createNodeView(final int viewIndex,
//            final RubyScriptNodeModel nodeModel) {
//        return new RubyScriptNodeView(nodeModel);
//    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final NodeDialogPane createNodeDialogPane() {
        m_dialog = new RubyScriptNodeDialog(this); 
        return m_dialog;
    }
}
