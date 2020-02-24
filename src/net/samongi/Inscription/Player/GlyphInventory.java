package net.samongi.Inscription.Player;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Attributes.Attribute;
import net.samongi.SamongiLib.Exceptions.InvalidConfigurationException;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GlyphInventory implements Serializable {

    // Serialization UID
    private static final long serialVersionUID = 7926951459348801465L;

    // Constants and accessors
    private static final int ROW_LENGTH = 9;
    private static final int ROW_NUNMBER = 5;

    public static int getMaxGlyphSlots() {
        return ROW_LENGTH * ROW_NUNMBER;
    }

    // Storing glyph inventories for listener referencing
    private static Map<Inventory, GlyphInventory> glyph_inventories = new HashMap<>();

    public static boolean isGlyphInventory(Inventory inventory) {

        return glyph_inventories.containsKey(inventory);
    }

    public static GlyphInventory getGlyphInventory(Inventory inventory) {

        return glyph_inventories.get(inventory);
    }

    public static void onInventoryClose(InventoryCloseEvent event) {
        // Getting the needed information
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();
        // Checking if the closed inventory is an inventory.
        if (!GlyphInventory.isGlyphInventory(inventory))
            return;
        GlyphInventory glyphInventory = GlyphInventory.getGlyphInventory(inventory);
        // Syncing the inventory on the close.
        glyphInventory.sync(inventory, player);

        // Removing the inventory from the listing if it doesn't have any viewers.
        if (inventory.getViewers().size() < 1) {
            GlyphInventory.glyph_inventories.remove(inventory);
        }
    }
    public static void onInventoryClick(InventoryClickEvent event) {
        if (event.isCancelled())
            return;

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getClickedInventory();

        if (!GlyphInventory.isGlyphInventory(inventory))
            return;
        GlyphInventory glyphInventory = GlyphInventory.getGlyphInventory(inventory);

        int slot = event.getSlot(); // the clicked slot.

        // Canceling the event if the slot is locked
        if (glyphInventory.isLocked(slot))
            event.setCancelled(true);

        // Paying for the slot if the player who clicked it has enough experience.
        if (event.getClick().isLeftClick() && event.getClick().isShiftClick() && glyphInventory.isLocked(slot)) {
            if (player.getLevel() >= glyphInventory.m_unlockedSlots) {
                player.setLevel(player.getLevel() - glyphInventory.m_unlockedSlots);
                glyphInventory.setLocked(slot, false);
                glyphInventory.populateLockedSlots(inventory);
            }
        }
    }

    // <--- Start Class Members --->

    // Inventory caching
    private transient Inventory inventory = null;

    // Indexing of glyphs
    private HashMap<Integer, Glyph> g_glyphs = new HashMap<>();
    private UUID g_owner = null;

    private int m_unlockedSlots = 0;
    private Boolean[] m_lockedSlots = new Boolean[getMaxGlyphSlots()];

    public GlyphInventory(Player owner) {
        this.g_owner = owner.getUniqueId();
        for (int i = 0; i < getMaxGlyphSlots(); i++)
            m_lockedSlots[i] = true;
    }
    public GlyphInventory(UUID owner) {
        this.g_owner = owner;
        for (int i = 0; i < getMaxGlyphSlots(); i++) {
            m_lockedSlots[i] = true;
        }
    }
    public GlyphInventory(UUID owner, ConfigurationSection section) throws InvalidConfigurationException {
        this.g_owner = owner;

        /* Setting the locked slots */
        for (int i = 0; i < getMaxGlyphSlots(); i++) {
            m_lockedSlots[i] = true;
        }
        List<Integer> unlockedSlotsSection = section.getIntegerList("unlocked-slots");
        if (unlockedSlotsSection == null)
            throw new InvalidConfigurationException("No 'unlocked-slots' key");
        for (int i : unlockedSlotsSection) {
            this.m_lockedSlots[i] = false;
            this.m_unlockedSlots++;
        }

        /* Setting the glyphs */
        ConfigurationSection glyphsSection = section.getConfigurationSection("glyphs");
        if (glyphsSection == null)
            throw new InvalidConfigurationException("No 'glyphs' key");
        Set<String> keys = glyphsSection.getKeys(false);
        for (String k : keys) {

            Integer slot = -1;
            try {
                slot = Integer.parseInt(k);
            }
            catch (NumberFormatException error) {
                throw new InvalidConfigurationException("Invalid key: 'glyphs." + k + "'");
            }

            ConfigurationSection glyphSection = glyphsSection.getConfigurationSection(k);
            if (glyphSection == null)
                throw new InvalidConfigurationException("No 'glyphs." + k + "' key");
            Glyph glyph = Glyph.getGlyph(glyphSection);
            if (glyph == null) {
                Inscription.logger.severe("Glyph was null while constructing GlyphInventory for '" + owner + "'");
                continue;
            }
            this.g_glyphs.put(slot, glyph);

        }

    }
    public void setLocked(int slot, boolean is_locked) {
        this.m_lockedSlots[slot] = is_locked;
        this.m_unlockedSlots = 0;
        for (int i = 0; i < getMaxGlyphSlots(); i++) {
            if (m_lockedSlots[i] == false)
                this.m_unlockedSlots++;
        }
    }
    public boolean isLocked(int slot) {
        return this.m_lockedSlots[slot];
    }

    /**
     * Retrieves the UUID of the owner
     * If there was not an owner set for this inventory, then this will return
     * null
     *
     * @return UUID of the owner, otherwise null
     */
    public UUID getOwner() {
        return this.g_owner;
    }
    /**
     * Sets the owner of this glyph inventory
     *
     * @param owner
     */
    public void setOwner(UUID owner) {
        this.g_owner = owner;
    }
    /**
     * Sets the owner of this glyph inventory
     *
     * @param owner
     */
    public void setOwner(Player owner) {
        this.g_owner = owner.getUniqueId();
    }

    /**
     * Retrieves the inventory for the glyph inventory
     * This will generate an inventory if it isn't already being accessed by
     * another player
     * This will generate a new inventory if the cached copy doesn't exist or the
     * current cached copy has no viewers.
     *
     * @return An inventory object
     */
    public Inventory getInventory() {
        if (this.inventory == null || this.inventory.getViewers().size() == 0) {
            this.inventory = Bukkit.getServer()
                .createInventory(null, GlyphInventory.getMaxGlyphSlots(), ChatColor.BLUE + "Glyph Inventory");
            Inscription.logger.finest("Glyphs lazy: " + g_glyphs);
            for (int index : g_glyphs.keySet()) {
                Glyph glyph = g_glyphs.get(index);
                if (glyph == null) {
                    Inscription.logger.severe("Found null glyph during getInventory for '" + g_owner + "'");
                    continue;
                }
                this.inventory.setItem(index, glyph.getItemStack());
            }
            GlyphInventory.glyph_inventories.put(inventory, this);
        }
        this.populateLockedSlots(inventory);
        return this.inventory;
    }
    private void populateLockedSlots(Inventory inventory) {
        for (int i = 0; i < getMaxGlyphSlots(); i++) {
            boolean lock_state = this.m_lockedSlots[i];
            if (!lock_state) // if lock_state is false
            {
                ItemStack item = inventory.getItem(i);
                if (item == null)
                    continue;
                if (!item.getType().equals(Material.GRAY_STAINED_GLASS))
                    continue;
                inventory.clear(i);
                continue;
            }
            ItemStack lock_item = new ItemStack(Material.GRAY_STAINED_GLASS, 1);
            lock_item.setDurability((short) 15);

            ItemMeta im = lock_item.getItemMeta();
            im.setDisplayName(ChatColor.BOLD + "" + ChatColor.GREEN + "Unlock Glyph Slot");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + "Use " + ChatColor.YELLOW + this.m_unlockedSlots + " Levels" + ChatColor.WHITE
                + " to unlock this slot.");
            lore.add(ChatColor.AQUA + "Shift-Left Click" + ChatColor.WHITE + " to pay the Levels");
            im.setLore(lore);

            lock_item.setItemMeta(im);
            inventory.setItem(i, lock_item);

        }
    }

    public ConfigurationSection getAsConfigurationSection() {
        ConfigurationSection section = new YamlConfiguration();

        /* Getting all the slots that are locked */
        List<Integer> unlockedSlots = new ArrayList<>();
        for (int i = 0; i < this.m_lockedSlots.length; i++) {
            if (!this.m_lockedSlots[i])
                unlockedSlots.add(i);
        }
        section.set("unlocked-slots", unlockedSlots);

        /* Setting all the glyphs */
        ConfigurationSection glyphs = new YamlConfiguration();
        for (Integer key : this.g_glyphs.keySet()) {
            Glyph glyph = this.g_glyphs.get(key);
            glyphs.set("" + key, glyph.getAsConfigurationSection());
        }
        section.set("glyphs", glyphs);

        return section;
    }

    /**
     * Called when the glyph inventory needs to be synced with a corresponding
     * inventory
     * May be called at any time to sync with the provided inventory. This will
     * override any
     * existing data supported by the inventory.
     * <p>
     * The player provided if the inventory has any errors and as such will drop
     * any items it must
     * throw out at the player.
     *
     * @param inventory The inventory to sync with
     * @param player    The player that caused the sync
     */
    public void sync(Inventory inventory, Player player) {
        if (!inventory.equals(this.inventory))
            return;

        // Parsing all the glyphs
        for (int index = 0; index < GlyphInventory.getMaxGlyphSlots(); index++) {
            ItemStack item = inventory.getItem(index);
            Glyph glyph = Glyph.getGlyph(item);

            if (glyph != null) {
                this.g_glyphs.put(index, glyph);
                if (item.getAmount() > 1) {
                    int drop_amount = item.getAmount() - 1;
                    ItemStack drop_item = item.clone();
                    drop_item.setAmount(drop_amount);
                    player.getWorld().dropItem(player.getLocation(), drop_item);
                    item.setAmount(1);
                }
            } else if (!this.isLocked(index)) {
                this.g_glyphs.remove(index);
                if (item != null) {
                    player.getWorld().dropItem(player.getLocation(), item);
                    inventory.clear(index);
                }
            }
        }

        PlayerData data = Inscription.getInstance().getPlayerManager().getData(this.g_owner);
        if (data == null)
            return;
        Inscription.logger.fine("Caching Glyphs for Glyph Inventory of: " + data.getPlayerName());
        this.cacheGlyphs(data);
    }

    /**
     * Calls the cache method on each attribute of the glyphs
     * This is only done if the glyphs have an implemented cache method.
     *
     * @param data The player data to cache these glyphs with.
     */
    public void cacheGlyphs(PlayerData data) {
        data.clearCachedData();
        for (Glyph glyph : this.g_glyphs.values()) {
            if (glyph == null) {
                Inscription.logger
                    .severe("Found glyph data to be null when caching player data: '" + data.getPlayerName() + "'");
                continue;
            }
            for (Attribute attribute : glyph.getAttributes()) {
                if (attribute == null) {
                    Inscription.logger
                        .severe("Found attribute to be null when caching player data: '" + data.getPlayerName() + "'");
                    continue;

                }
                attribute.cache(data);
            }
        }
    }

    /**
     * Returns a list of all glyphs in this inventory. This is only the most
     * recent
     * snapshot of glyphs until the player closes their glyph inventory.
     *
     * @return A list of glyphs
     */
    public List<Glyph> getGlyphs() {
        return new ArrayList<>(this.g_glyphs.values());
    }

    /**
     * Distributes the experience throughout the inventory to the different glyphs
     * The minimum increment will be 1 experience. As such, glyphs are random
     * selected for receiving the experience.
     * <p>
     * Glyphs are only distributed to if they need experience. This will also
     * attempt to levelup the glyph
     * given it is able to levelup. If it doesn't levelup and has extra
     * experience, that experience will be returned to the pool.
     *
     * @param type   The type
     * @param amount
     * @return false if the experience wasn't used.
     */
    public boolean distributeExperience(String type, int amount) {
        Inscription.logger.fine("Distributing Experience: " + type + ", amount:" + amount);

        // We are going to get a list of all the glyphs
        List<Glyph> glyphGroup = new ArrayList<>();
        for (Glyph glyph : this.g_glyphs.values()) {
            // First we need to get the experience that the glyph needs
            int glyphExperience = glyph.remainingExperience(type);
            // If it doesn't need any experience or is max level we will ignore it
            if (glyphExperience <= 0 || glyph.isMaxLevel()) {
                continue;
            }
            glyphGroup.add(glyph); // Adding the glyph to the group
        }

        // If we don't have any glyphs that can receive this experience, then we'll cache
        // the experience.
        if (glyphGroup.size() == 0) {
            Inscription.logger.fine("Could not find any glyphs to distribute to");
            return false;
        }

        // Getting the size of the glyph group
        int validGlyphs = glyphGroup.size();

        // This will truncate any decimals, some glyphs won't get experience
        int experienceIncrement = amount / validGlyphs;
        // The excess will be added to the first-come glyphs during the distribution.
        int experienceExcessPool = amount % validGlyphs;

        Random rand = new Random(); // creating a random number generator.
        int experiencePool = amount - experienceExcessPool; // creating an experiencePool to pull from for each glyph
        List<Glyph> glyphPool = new ArrayList<>(glyphGroup);

        /* if we run out of experience or if we're out of glyphs */
        while (experiencePool > 0 && glyphPool.size() > 0) {
            /* getting the index of one of the glyphs from the group */
            int randomIndex = rand.nextInt(glyphPool.size());
            Glyph glyph = glyphPool.get(randomIndex);

            int availableExperience = 0;
            if (experienceIncrement > experiencePool) // if the pool doesn't have enough
            {
                availableExperience = experiencePool;
                experiencePool = 0;
            } else {
                // if the pool has enough experience
                availableExperience = experienceIncrement;
                experiencePool -= experienceIncrement;
            }

            // The excess pool will contribute 1 experience to the first glyphs that get experience.
            if (experienceExcessPool > 0) {
                availableExperience += 1;
                experienceExcessPool -= 1;
            }

            if (glyph.getLevel() < Glyph.MAX_LEVEL) {
                // Adding the experience and attempting to levelup the glyph
                glyph.addExperience(type, availableExperience);
                int prior_level = glyph.getLevel();
                boolean didLevel = glyph.attemptLevelup();
                boolean displayLevelMessage = didLevel;
                // Leveling up the glyph if it can.
                while (didLevel) {
                    didLevel = glyph.attemptLevelup();
                }

                if (displayLevelMessage) {
                    Player owner = Bukkit.getPlayer(this.g_owner);
                    owner.sendMessage(ChatColor.YELLOW + "Congratulations!");
                    owner.sendMessage(
                        ChatColor.WHITE + "[" + glyph.getItemStack().getItemMeta().getDisplayName() + ChatColor.WHITE
                            + "] " + ChatColor.YELLOW + "has leveled up to " + ChatColor.GREEN + "Level " + glyph
                            .getLevel() + ChatColor.WHITE + " from " + ChatColor.GREEN + "Level " + prior_level);
                    owner.playSound(owner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                }
            }
            glyphPool.remove(randomIndex); // removing the glyph from the list
        }
        // Cleaning up the rest of experience pool to make sure there is no waste
        if (experiencePool > 0) {
            glyphGroup.get(rand.nextInt(glyphGroup.size())).addExperience(type, experiencePool);
        }

        return true;
    }
}
