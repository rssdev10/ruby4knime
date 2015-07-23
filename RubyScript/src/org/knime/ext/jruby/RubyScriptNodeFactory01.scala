package org.knime.ext.jruby

class RubyScriptNodeFactory01 extends RubyScriptNodeFactory {

  override def createNodeModel(): RubyScriptNodeModel = {
    setModel(new RubyScriptNodeModel(0, 1, false))
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
public class RubyScriptNodeFactory01 extends RubyScriptNodeFactory {

    |**
     * {@inheritDoc}
     *|
    @Override
    public final RubyScriptNodeModel createNodeModel() {
        return setModel(new RubyScriptNodeModel(0, 1, false));
    }
}

*/
}