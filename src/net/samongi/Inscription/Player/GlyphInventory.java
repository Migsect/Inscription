package net.samongi.Inscription.Player;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Glyphs.Attributes.Attribute;

public class GlyphInventory implements Serializable
{
  // Debug methods for class
  private static void log(String message){Inscription.log("[G.Inventory] " + message);;}
  @SuppressWarnings("unused")
  private static void logDebug(String message){if(Inscription.debug()) GlyphInventory.log("[DEBUG] " + message );}
  @SuppressWarnings("unused")
  private static boolean debug(){return Inscription.debug();}
  
  // Serialization UID
  private static final long serialVersionUID = 7926951459348801465L;
  
  // Constants and accessors
  private static final int ROW_LENGTH = 9;
  private static final int ROW_NUNMBER = 5;
  private static int getMaxGlyphSlots(){return ROW_LENGTH * ROW_NUNMBER;}
  
  // Storing glyph inventories for listener referencing
  private static Map<Inventory, GlyphInventory> glyph_inventories = new HashMap<>();
  
  public static boolean isGlyphInventory(Inventory inventory){return glyph_inventories.containsKey(inventory);}
  public static GlyphInventory getGlyphInventory(Inventory inventory){return glyph_inventories.get(inventory);}
  public static void onInventoryClose(InventoryCloseEvent event)
  {
    // Getting the needed information
    Player player = (Player) event.getPlayer();
    Inventory inventory = event.getInventory();
    // Checking if the closed inventory is an inventory.
    if(!GlyphInventory.isGlyphInventory(inventory)) return;
    GlyphInventory g_inventory = GlyphInventory.getGlyphInventory(inventory);
    // Syncing the inventory on the close.
    g_inventory.sync(inventory, player);
    
    // Removing the inventory from the listing if it doesn't have any viewers.
    if(inventory.getViewers().size() < 1) GlyphInventory.glyph_inventories.remove(inventory);
  }
  public static void onInventoryClick(InventoryClickEvent event)
  {
    if(event.isCancelled()) return;
    
    Player player = (Player)event.getWhoClicked();
    Inventory inventory = event.getClickedInventory();
    
    if(!GlyphInventory.isGlyphInventory(inventory)) return;
    GlyphInventory g_inventory = GlyphInventory.getGlyphInventory(inventory);
    
    int slot = event.getSlot(); // the clicked slot.
    
    // Canceling the event if the slot is locked
    if(g_inventory.isLocked(slot)) event.setCancelled(true);
    
    // Paying for the slot if the player who clicked it has enough experience.
    if(event.getClick().isLeftClick() && event.getClick().isShiftClick())
    {
      if(player.getLevel() >= g_inventory.unlocked_slots)
      {
        player.setLevel(player.getLevel() - g_inventory.unlocked_slots);
        g_inventory.setLocked(slot, false);
        g_inventory.populateLockedSlots(inventory);
      }
    }
  }
  
  // <--- Start Class Members --->
  
  // Inventory caching
  private transient Inventory inventory = null;
  
  // Indexing of glyphs
  private HashMap<Integer, Glyph> glyphs = new HashMap<>();
  private UUID owner = null;
  
  // Unlocked Slots - true means its locked :3
  private int unlocked_slots = 0;
  private Boolean[] locked_slots = new Boolean[getMaxGlyphSlots()];
  
  public GlyphInventory(Player owner)
  {
    this.owner = owner.getUniqueId();
    for(int i = 0; i < getMaxGlyphSlots(); i++) locked_slots[i] = true;
  }
  public GlyphInventory(UUID owner)
  {
    this.owner = owner;
    for(int i = 0; i < getMaxGlyphSlots(); i++) locked_slots[i] = true;
  }
  
  public void setLocked(int slot, boolean is_locked)
  {
    this.locked_slots[slot] = is_locked;
    this.unlocked_slots = 0;
    for(int i = 0; i < getMaxGlyphSlots(); i++) {if(locked_slots[i] == false) this.unlocked_slots++;}
  }
  public boolean isLocked(int slot){return this.locked_slots[slot];}
  
  /**Retrieves the UUID of the owner
   * If there was not an owner set for this inventory, then this will return null
   * @return UUID of the owner, otherwise null
   */
  public UUID getOwner(){return this.owner;}
  /**Sets the owner of this glyph inventory
   * @param owner
   */
  public void setOwner(UUID owner){this.owner = owner;}
  /**Sets the owner of this glyph inventory
   * @param owner
   */
  public void setOwner(Player owner){this.owner = owner.getUniqueId();}
  
  /**Retrieves the inventory for the glyph inventory
   * This will generate an inventory if it isn't already being accessed by another player
   * This will generate a new inventory if the cached copy doesn't exist or the current cached copy has no viewers.
   * 
   * @return An inventory object
   */
  public Inventory getInventory()
  {
    if(this.inventory == null || this.inventory.getViewers().size() == 0) 
    {
      this.inventory = Bukkit.getServer().createInventory(null, GlyphInventory.getMaxGlyphSlots(), ChatColor.BLUE + "Glyph Inventory");
      for(int i : glyphs.keySet()) this.inventory.setItem(i, glyphs.get(i).getItemStack());
      GlyphInventory.glyph_inventories.put(inventory, this);
    }
    this.populateLockedSlots(inventory);
    return this.inventory;
  }
  private void populateLockedSlots(Inventory inventory)
  {
    for(int i = 0; i < getMaxGlyphSlots(); i++)
    {
      boolean lock_state = this.locked_slots[i];
      if(!lock_state) // if lock_state is false
      {
        ItemStack item = inventory.getItem(i);
        if(item == null) continue;
        if(!item.getType().equals(Material.STAINED_GLASS_PANE)) continue;
        inventory.clear(i);
        continue;
      }
      ItemStack lock_item = new ItemStack(Material.STAINED_GLASS_PANE, 1);
      lock_item.setDurability((short) 15);
      
      ItemMeta im = lock_item.getItemMeta();
      im.setDisplayName(ChatColor.BOLD + "" + ChatColor.GREEN + "Unlock Glyph Slot");
      List<String> lore = new ArrayList<>();
      lore.add(ChatColor.WHITE + "Use " + ChatColor.YELLOW + this.unlocked_slots + " Levels" + ChatColor.WHITE + " to unlock this slot.");
      lore.add(ChatColor.AQUA + "Shift-Left Click" + ChatColor.WHITE + " to pay the Levels");
      im.setLore(lore);
      
      lock_item.setItemMeta(im);
      inventory.setItem(i, lock_item);
      
    }
  }
  
  /**Called when the glyph inventory needs to be synced with a corresponding inventory
   * May be called at any time to sync with the provided inventory.  This will override any
   * existing data supported by the inventory.
   * 
   * The player provided if the inventory has any errors and as such will drop any items it must
   * throw out at the player.
   * 
   * @param inventory The inventory to sync with
   * @param player The player that caused the sync
   */
  public void sync(Inventory inventory, Player player)
  {
    if(!inventory.equals(this.inventory)) return;

    // Parsing all the glyphs
    for(int i = 0 ; i < GlyphInventory.getMaxGlyphSlots() ; i++)
    {
      ItemStack item = inventory.getItem(i);
      Glyph glyph = Glyph.getGlyph(item);
      
      if(glyph != null) 
      {
        this.glyphs.put(i, glyph);
        if(item.getAmount() > 1)
        {
          int drop_amount = item.getAmount() - 1; 
          ItemStack drop_item = item.clone();
          drop_item.setAmount(drop_amount);
          player.getWorld().dropItem(player.getLocation(), drop_item);
          item.setAmount(1);
        }
      }
      else if(!this.isLocked(i))
      {
        this.glyphs.remove(i);
        if(item != null)
        {
          player.getWorld().dropItem(player.getLocation(), item);
          inventory.clear(i);
        }
      }
    }
    
    PlayerData data = Inscription.getInstance().getPlayerManager().getData(this.owner);
    if(data == null) return;
    Inscription.logDebug("Caching Glyphs for Glyph Inventory of: " + data.getPlayerName());
    this.cacheGlyphs(data);
  }
  
  /**Calls the cache method on each attribute of the glyphs
   * This is only done if the glyphs have an implemented cache method.
   * 
   * @param data The player data to cache these glyphs with.
   */
  public void cacheGlyphs(PlayerData data)
  {
    data.clearCachedData();
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
  
  /**Distributes the experience throughout the inventory to the different glyphs
   * The minimum increment will be 1 experience.  As such, glyphs are random selected for receiving the experience.
   * 
   * Glyphs are only distributed to if they need experience. This will also attempt to levelup the glyph
   * given it is able to levelup.  If it doesn't levelup and has extra experience, that experience will be returned to the pool.
   * 
   * @param type The type 
   * @param amount
   */
  public void distributeExperience(String type, int amount)
  {
    // We are going to get a list of all the glyphs
    List<Glyph> glyph_group = new ArrayList<>();
    for(Glyph g : this.glyphs.values())
    {
      // First we need to get the experience that the glyph needs
      int g_experience = g.remainingExperience(type);
      if(g_experience <= 0) continue; // If it doesn't need any experience, we will ignore it
      glyph_group.add(g); // Adding the glyph to the group
    }
    // If it is zero, we'll grab a group that can accept the experience (but doesn't really need it)
    if(glyph_group.size() == 0) for(Glyph g : this.glyphs.values())
    {
      int exp_to_level = g.getExperienceToLevel(type);
      if(exp_to_level > 0) glyph_group.add(g);
    }
    
    // Getting the size of the glyph group
    int g_amount = glyph_group.size();
    if(g_amount == 0) return; // No divide by 0s here!
    int increment = amount / g_amount; // This will truncate any decimals, some glyphs won't get experience
    if(amount % g_amount > 0) increment++; // If we do have remainders, we will round up.
    
    Random rand = new Random(); // creating a random number generator.
    int exp_pool = amount; // creating an exp_pool to pull from for each glyph
    List<Glyph> glyph_pool = new ArrayList<>(glyph_group);
    while(exp_pool > 0 && glyph_pool.size() > 0) // if we run out of experience or if we're out of glyphs
    {
      int rand_int = rand.nextInt(glyph_pool.size()); // getting the index of one of the glyphs from the group
      Glyph g = glyph_pool.get(rand_int);
      
      int available_exp = 0;
      if(increment > exp_pool) // if the pool doesn't have enough
      {
        available_exp = exp_pool;
        exp_pool = 0;
      }
      else // if the pool has enough experience
      {
        available_exp = increment;
        exp_pool -= increment;
      }
      
      // Adding the experience and attempting to levelup the glyph
      g.addExperience(type, available_exp);
      int prior_level = g.getLevel();
      boolean did_level = g.attemptLevelup();
      boolean do_text = did_level;
      while(did_level) did_level = g.attemptLevelup(); // levelup attempt
      if(do_text)
      {
        Player owner = Bukkit.getPlayer(this.owner);
        owner.sendMessage(ChatColor.YELLOW + "Congratulations!");
        owner.sendMessage( ChatColor.WHITE + "[" + g.getItemStack().getItemMeta().getDisplayName() + ChatColor.WHITE + "] " + ChatColor.YELLOW +
            "has leveled up to " + ChatColor.GREEN + "Level " + g.getLevel() + ChatColor.WHITE + " from " + ChatColor.GREEN + "Level " + prior_level);
      }
      glyph_pool.remove(rand_int); // removing the glyph from the list
    }
    // Cleaning up the rest of experience pool to make sure there is no waste
    if(exp_pool > 0) glyph_group.get(rand.nextInt(glyph_group.size())).addExperience(type, exp_pool);
  }
}
