package org.knime.ext.jruby

class RubySnippetNodeFactory extends RubyScriptNodeFactory {

  override def createNodeModel(): RubyScriptNodeModel = {
    setModel(new RubyScriptNodeModel(1, 1, true))
  }

/*
Original Java:
package org.knime.ext.jruby;

|**
 * <code>NodeFactory</code> for the "RubyScript" Node.
 * 
 * 
 * @author
 *|
public class RubySnippetNodeFactory extends RubyScriptNodeFactory {

    |**
     * {@inheritDoc}
     *|
    @Override
    public final RubyScriptNodeModel createNodeModel() {
        return setModel(new RubyScriptNodeModel(1, 1, true));
    }
}

*/
}