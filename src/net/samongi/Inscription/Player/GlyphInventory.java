package net.samongi.Inscription.Player;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Glyphs.Attributes.Attribute;

public class GlyphInventory implements Serializable
{
  private static final long serialVersionUID = 7926951459348801465L;
  
  private static final int ROW_LENGTH = 9;
  private static final int ROW_NUNMBER = 5;
  private static int getMaxGlyphSlots(){return ROW_LENGTH * ROW_NUNMBER;}
  
  Inventory inventory = null;
  
  HashMap<Integer, Glyph> glyphs = new HashMap<>();
  
  public Inventory getInventory()
  {
    if(this.inventory == null || this.inventory.getViewers().size() == 0) 
    {
      this.inventory = Bukkit.getServer().createInventory(null, GlyphInventory.getMaxGlyphSlots(), ChatColor.GOLD + "Glyph Inventory");
      for(int i : glyphs.keySet()) this.inventory.setItem(i, glyphs.get(i).getItemStack());
    }
    return this.inventory;
  }
  
  /**Method to be called by the player listener when inventories are closed.
   * 
   * @param event The event that is called.
   */
  public void onInventoryClose(InventoryCloseEvent event)
  {
    Inventory inventory = event.getInventory();
    if(inventory != this.inventory) return;
    
    // Parsing all the glyphs
    for(int i = 0 ; i < getMaxGlyphSlots() ; i++)
    {
      Glyph glyph = Glyph.getGlyph(inventory.getItem(i));
      if(glyph != null) this.glyphs.put(i, glyph);
    }
  }
  
  /**Calls the cache method on each attribute of the glyphs
   * This is only done if the glyphs have an implemented cache method.
   * 
   * @param data The player data to cache these glyphs with.
   */
  public void cacheGlyphs(PlayerData data)
  {
    for(Glyph g : this.glyphs.values())
    {
      for(Attribute a : g.getAttributes()) a.cache(data);
    }
  }
  
  /**Returns a list of all glyphs in this inventory. This is only the most recent
   * snapshot of glyphs until the player closes their glyph inventory.
   * 
   * @return A list of glyphs
   */
  public List<Glyph> getGlyphs(){return new ArrayList<>(this.glyphs.values());}
}
