package org.knime.ext.jruby;

/**
 * <code>NodeFactory</code> for the "RubyScript" Node.
 * 
 * 
 * @author
 */
public class RubyScriptNodeFactory22 extends RubyScriptNodeFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public final RubyScriptNodeModel createNodeModel() {
        return setModel(new RubyScriptNodeModel(2, 2, false));
    }
}
