package org.knime.ext.jruby.preferences

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer
import org.eclipse.jface.preference.IPreferenceStore
import org.knime.ext.jruby.RubyScriptNodePlugin

/**
 * Class is used to initialize default preference values.
 * 
 * @author rss
 *
 */
class PreferenceInitializer extends AbstractPreferenceInitializer {

  /* (non-Javadoc)
   * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
   */
  def initializeDefaultPreferences() {
    val store = RubyScriptNodePlugin.getDefault.getPreferenceStore
    store.setDefault(PreferenceConstants.JRUBY_USE_EXTERNAL_GEMS, false)
    store.setDefault(PreferenceConstants.JRUBY_PATH, "")
  }
}
