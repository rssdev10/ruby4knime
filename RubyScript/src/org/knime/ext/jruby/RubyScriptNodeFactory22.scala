package org.knime.ext.jruby

/**
 * <code>NodeFactory</code> for the "RubyScript" Node.
 * 
 * @author rss
 *
 */
class RubyScriptNodeFactory22 extends RubyScriptNodeFactory {

  /* (non-Javadoc)
   * @see org.knime.ext.jruby.RubyScriptNodeFactory#createNodeModel()
   */
  override def createNodeModel(): RubyScriptNodeModel = {
    setModel(new RubyScriptNodeModel(2, 2, false))
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
public class RubyScriptNodeFactory22 extends RubyScriptNodeFactory {

    |**
     * {@inheritDoc}
     *|
    @Override
    public final RubyScriptNodeModel createNodeModel() {
        return setModel(new RubyScriptNodeModel(2, 2, false));
    }
}

*/
}