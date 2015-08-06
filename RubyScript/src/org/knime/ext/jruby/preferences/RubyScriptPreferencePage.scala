package org.knime.ext.jruby.preferences

import org.eclipse.jface.preference._
import org.eclipse.ui.IWorkbenchPreferencePage
import org.eclipse.ui.IWorkbench
import org.knime.ext.jruby.RubyScriptNodePlugin

/**
 * @author rss
 *
 */
class RubyScriptPreferencePage extends FieldEditorPreferencePage with IWorkbenchPreferencePage {

  setPreferenceStore(RubyScriptNodePlugin.getDefault.getPreferenceStore)

  setDescription("Ruby Scripting preferences")

  /* (non-Javadoc)
   * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
   */
  def createFieldEditors() {
    addField(
        new BooleanFieldEditor(PreferenceConstants.JRUBY_USE_EXTERNAL_GEMS,
            "&Use external jRuby gems", getFieldEditorParent))
    addField(
        new DirectoryFieldEditor(PreferenceConstants.JRUBY_PATH,
            "&Root path of external jRuby installation:", getFieldEditorParent))
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
   */
  def init(workbench: IWorkbench) {
  }
}
