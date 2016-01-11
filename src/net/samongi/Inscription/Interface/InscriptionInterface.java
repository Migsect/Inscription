package net.samongi.Inscription.Interface;

import net.samongi.Inscription.Inscription;

/**Used by external plugins to access the plugin.
 * This can be seen as a means for external 
 * 
 * @author Alex
 */
public class InscriptionInterface
{
  public static void reload()
  {
    Inscription.getInstance().onDisable();
    Inscription.getInstance().onEnable();
  }
}
