package net.samongi.Inscription.Player;

public interface CacheData
{
  /**Will clear the data
   */
  public void clear();
  
  /**Will return the cachedata type.
   * @return
   */
  public String getType();
  
  /**Will return the data as a stirng that can be printed out and is readable
   * @return
   */
  public String getData();
}
