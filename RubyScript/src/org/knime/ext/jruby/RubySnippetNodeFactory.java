package org.knime.ext.jruby;

/**
 * <code>NodeFactory</code> for the "RubyScript" Node.
 * 
 * 
 * @author
 */
public class RubySnippetNodeFactory extends RubyScriptNodeFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public final RubyScriptNodeModel createNodeModel() {
        return setModel(new RubyScriptNodeModel(1, 1, true));
    }
}
