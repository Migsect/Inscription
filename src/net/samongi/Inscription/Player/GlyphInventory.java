package net.samongi.Inscription.Player;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.samongi.Inscription.Experience.ExperienceMap;
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

public class GlyphInventory {

    // ---------------------------------------------------------------------------------------------------------------//
    private static final int ROW_LENGTH = 9;
    private static final int ROW_NUNMBER = 5;

    // ---------------------------------------------------------------------------------------------------------------//

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
        if (!GlyphInventory.isGlyphInventory(inventory)) {
            return;
        }
        GlyphInventory glyphInventory = GlyphInventory.getGlyphInventory(inventory);
        // Syncing the inventory on the close.
        glyphInventory.sync(inventory, player);

        // Removing the inventory from the listing if it doesn't have any viewers.
        if (inventory.getViewers().size() < 1) {
            GlyphInventory.glyph_inventories.remove(inventory);
        }
    }
    public static void onInventoryClick(InventoryClickEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getClickedInventory();

        if (!GlyphInventory.isGlyphInventory(inventory)) {
            return;
        }
        GlyphInventory glyphInventory = GlyphInventory.getGlyphInventory(inventory);

        int slot = event.getSlot(); // the clicked slot.

        // Canceling the event if the slot is locked
        if (!glyphInventory.isUnlocked(slot)) {
            event.setCancelled(true);
        }

        // Paying for the slot if the player who clicked it has enough experience.
        if (event.getClick().isLeftClick() && event.getClick().isShiftClick() && !glyphInventory.isUnlocked(slot)) {
            PlayerData playerData = Inscription.getInstance().getPlayerManager().getData(player);
            assert playerData != null;

            ExperienceMap playerExperience = playerData.getExperience();
            ExperienceMap slotExperienceCost = glyphInventory.getSlotCost(slot);
            if (playerExperience.greaterThanEquals(slotExperienceCost)) {
                playerData.addExperience(slotExperienceCost.negate());
            }

            glyphInventory.setUnlocked(slot, true);
            glyphInventory.populateLockedSlots(inventory);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------//

    private HashMap<Integer, Glyph> m_glyphs = new HashMap<>();

    private final Player m_owner;

    private Set<Integer> m_unlockedSlots = new HashSet<>();

    // A caching of the slot costs such that they don't need to be recalculated often.
    private List<ExperienceMap> m_slotCosts = null;

    // Inventory caching
    private Inventory inventory = null;

    private int m_columns = ROW_LENGTH;
    private int m_rows = ROW_NUNMBER;

    // ---------------------------------------------------------------------------------------------------------------//
    public GlyphInventory(Player owner) {
        m_owner = owner;
        m_rows = Inscription.getInstance().getConfig().getInt("glyph-inventory.rows", ROW_NUNMBER);
    }

    public GlyphInventory(Player owner, ConfigurationSection section) throws InvalidConfigurationException {
        this(owner);

        List<Integer> unlockedSlotsSection = section.getIntegerList("unlocked-slots");
        m_unlockedSlots.addAll(unlockedSlotsSection);
        Inscription.logger.finest("Found unlocked slots: " + m_unlockedSlots);

        /* Setting the glyphs */
        ConfigurationSection glyphsSection = section.getConfigurationSection("glyphs");
        if (glyphsSection == null) {
            throw new InvalidConfigurationException("No 'glyphs' key");
        }
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
            if (glyphSection == null) {
                throw new InvalidConfigurationException("No 'glyphs." + k + "' key");
            }
            Glyph glyph = Glyph.getGlyph(glyphSection);
            if (glyph == null) {
                Inscription.logger.severe("Glyph was null while constructing GlyphInventory for '" + owner + "'");
                continue;
            }
            m_glyphs.put(slot, glyph);
            Inscription.logger.finest("Loaded glyph with slot: " + slot);

        }

    }
    // ---------------------------------------------------------------------------------------------------------------//

    public void setUnlocked(int slot, boolean unlockedState) {
        if (unlockedState) {
            m_unlockedSlots.add(slot);
        } else {
            m_unlockedSlots.remove(slot);
        }
    }

    public boolean isUnlocked(int slot) {
        return m_unlockedSlots.contains(slot);
    }

    // ---------------------------------------------------------------------------------------------------------------//
    /**
     * Retrieves the UUID of the owner
     * If there was not an owner set for this inventory, then this will return
     * null
     *
     * @return UUID of the owner, otherwise null
     */
    public Player getOwner() {
        return m_owner;
    }

    /**
     * Returns a seed for unique per-player randomness.
     *
     * @return A number to act as a random number generator seed.
     */
    public int getSeed() {
        return m_owner.getUniqueId().hashCode();
    }

    public int getMaxGlyphSlots() {
        return m_rows * m_columns;
    }

    // ---------------------------------------------------------------------------------------------------------------//
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
        if (inventory == null || inventory.getViewers().size() == 0) {
            inventory = Bukkit.getServer().createInventory(null, getMaxGlyphSlots(), ChatColor.BLUE + "Glyph Inventory");
            Inscription.logger.finest("Glyphs lazy: " + m_glyphs);
            for (int index : m_glyphs.keySet()) {
                Glyph glyph = m_glyphs.get(index);
                if (glyph == null) {
                    Inscription.logger.severe("Found null glyph during getInventory for '" + m_owner + "'");
                    continue;
                }
                inventory.setItem(index, glyph.getItemStack());
            }
            GlyphInventory.glyph_inventories.put(inventory, this);
        }
        populateLockedSlots(inventory);
        return inventory;
    }

    private void populateSlotCosts() {
        if (m_slotCosts != null) {
            return;
        }

        Random random = new Random(getSeed());

        m_slotCosts = new ArrayList<>();

        List<String> experienceTypes = Inscription.getInstance().getConfig().getStringList("glyph-inventory.experience-types");
        int unlockCostBase = Inscription.getInstance().getConfig().getInt("glyph-inventory.unlock-cost-base");
        double unlockCostMultiplier = Inscription.getInstance().getConfig().getDouble("glyph-inventory.unlock-cost-increase");

        // Shuffle the experience types to ensure they are added randomly.
        Collections.shuffle(experienceTypes, random);

        int experienceStep = 0;
        int totalCosts = getMaxGlyphSlots();
        while (m_slotCosts.size() < totalCosts) {
            int unlockCost = (int) Math.floor(unlockCostBase * Math.pow(unlockCostMultiplier, experienceStep));
            for (String experienceType : experienceTypes) {
                ExperienceMap experienceMap = new ExperienceMap();
                experienceMap.set(experienceType, unlockCost);

                m_slotCosts.add(experienceMap);
                if (m_slotCosts.size() >= totalCosts) {
                    break;
                }
            }
            experienceStep++;
        }

        // Shuffle the slot costs such that they are arranged randomly.
        Collections.shuffle(m_slotCosts, random);

    }

    private ExperienceMap getSlotCost(int slot) {
        populateSlotCosts();
        return m_slotCosts.get(slot);
    }

    private void populateLockedSlots(Inventory inventory) {
        for (int slot = 0; slot < getMaxGlyphSlots(); slot++) {
            if (isUnlocked(slot)) {
                ItemStack item = inventory.getItem(slot);
                if (item == null || !item.getType().equals(Material.GRAY_STAINED_GLASS_PANE)) {
                    continue;
                }
                inventory.clear(slot);
                continue;
            }
            populateSlotCosts();
            ExperienceMap experienceCost = m_slotCosts.get(slot);
            PlayerData playerData = Inscription.getInstance().getPlayerManager().getData(m_owner);

            ItemStack lockedSlotItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);
            ItemMeta itemMeta = lockedSlotItem.getItemMeta();
            if (itemMeta == null) {
                continue;
            }

            itemMeta.setDisplayName(ChatColor.BOLD + "" + ChatColor.GREEN + "Locked Glyph Slot");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.YELLOW + "Experience Cost:");
            lore.addAll(experienceCost.toCostLore(playerData, 2));
            lore.add("");
            lore.add(ChatColor.AQUA + "Shift-Left Click" + ChatColor.YELLOW + " to pay the cost to unlock.");
            itemMeta.setLore(lore);

            lockedSlotItem.setItemMeta(itemMeta);
            inventory.setItem(slot, lockedSlotItem);

        }
    }

    // ---------------------------------------------------------------------------------------------------------------//
    public ConfigurationSection getAsConfigurationSection() {
        ConfigurationSection section = new YamlConfiguration();

        section.set("unlocked-slots", new ArrayList<>(m_unlockedSlots));

        /* Setting all the glyphs */
        ConfigurationSection glyphs = new YamlConfiguration();
        for (Integer key : m_glyphs.keySet()) {
            Glyph glyph = m_glyphs.get(key);
            glyphs.set("" + key, glyph.getAsConfigurationSection());
        }
        section.set("glyphs", glyphs);

        return section;
    }

    // ---------------------------------------------------------------------------------------------------------------//
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
        if (!inventory.equals(inventory)) {
            return;
        }

        // Parsing all the glyphs
        for (int slot = 0; slot < getMaxGlyphSlots(); slot++) {
            ItemStack item = inventory.getItem(slot);
            Glyph glyph = Glyph.getGlyph(item);

            if (glyph != null) {
                m_glyphs.put(slot, glyph);
                if (item.getAmount() > 1) {
                    int drop_amount = item.getAmount() - 1;
                    ItemStack drop_item = item.clone();
                    drop_item.setAmount(drop_amount);
                    player.getWorld().dropItem(player.getLocation(), drop_item);
                    item.setAmount(1);
                }
            } else if (isUnlocked(slot)) {
                m_glyphs.remove(slot);
                if (item != null) {
                    player.getWorld().dropItem(player.getLocation(), item);
                    inventory.clear(slot);
                }
            }
        }

        PlayerData data = Inscription.getInstance().getPlayerManager().getData(m_owner);
        if (data == null) {
            return;
        }
        Inscription.logger.fine("Caching Glyphs for Glyph Inventory of: " + data.getPlayerName());
        cacheGlyphs(data);
    }

    /**
     * Calls the cache method on each attribute of the glyphs
     * This is only done if the glyphs have an implemented cache method.
     *
     * @param data The player data to cache these glyphs with.
     */
    public void cacheGlyphs(PlayerData data) {
        data.clearCachedData();
        for (Glyph glyph : m_glyphs.values()) {
            if (glyph == null) {
                Inscription.logger.severe("Found glyph data to be null when caching player data: '" + data.getPlayerName() + "'");
                continue;
            }
            for (Attribute attribute : glyph.getAttributes()) {
                if (attribute == null) {
                    Inscription.logger.severe("Found attribute to be null when caching player data: '" + data.getPlayerName() + "'");
                    continue;

                }
                attribute.cache(data);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------//
    /**
     * Returns a list of all glyphs in this inventory. This is only the most
     * recent
     * snapshot of glyphs until the player closes their glyph inventory.
     *
     * @return A list of glyphs
     */
    public List<Glyph> getGlyphs() {
        return new ArrayList<>(m_glyphs.values());
    }

    // ---------------------------------------------------------------------------------------------------------------//
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
     * @return The amount of experience that wasn't used.
     */
    public int distributeExperience(String experienceType, int amount) {
        Inscription.logger.fine("Distributing Experience: " + experienceType + ", amount:" + amount);

        // We are going to get a list of all the glyphs
        List<Glyph> glyphGroup = new ArrayList<>();
        for (Glyph glyph : m_glyphs.values()) {
            if (!glyph.getRelevantExperienceTypes().contains(experienceType) || glyph.isMaxLevel()) {
                continue;
            }
            glyphGroup.add(glyph);
        }

        if (glyphGroup.size() == 0) {
            Inscription.logger.finer("Could not find any glyphs to distribute to");
            return amount;
        }

        List<Glyph> glyphCandidates = new ArrayList<>(glyphGroup);
        List<Integer> priorLevels = glyphCandidates.stream().map((glyph) -> glyph.getLevel()).collect(Collectors.toList());


        /* if we run out of experience or if we're out of glyphs */
        List<Glyph> glyphPool = new ArrayList<>(glyphCandidates);
        int experiencePool = amount;
        while (glyphPool.size() > 0 && experiencePool > 0) {
            Collections.shuffle(glyphPool);

            ExperienceMap experienceAddition = new ExperienceMap();

            int distributionAmount = experiencePool / glyphPool.size();
            if (experiencePool / glyphPool.size() <= 0) {
                distributionAmount = 1;
            }
            experienceAddition.set(experienceType, distributionAmount);

            Set<Glyph> maxedGlyphs = new HashSet<>();
            for (Glyph glyph : glyphPool) {
                ExperienceMap overflowExperience = glyph.addExperience(experienceAddition);
                experiencePool -= experienceAddition.subtract(overflowExperience).getTotal();
                if (overflowExperience.getTotal() >= 0) {
                    maxedGlyphs.add(glyph);
                }
            }
            glyphPool.removeAll(maxedGlyphs);
        }
        for (int index = 0; index < glyphCandidates.size(); index++) {
            Glyph glyph = glyphCandidates.get(index);
            int priorLevel = priorLevels.get(index);
            if (glyph.getLevel() == priorLevel) {
                continue;
            }
            m_owner.sendMessage(ChatColor.YELLOW + "Congratulations!");

            TextComponent glyphMessage = glyph.getTextComponent();
            glyphMessage.addExtra(
                ChatColor.YELLOW + " has leveled up to " + ChatColor.GREEN + "Level " + glyph.getLevel() + ChatColor.WHITE + " from " + ChatColor.GREEN
                    + "Level " + priorLevel);
            m_owner.spigot().sendMessage(glyphMessage);
            m_owner.playSound(m_owner.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, (float) 0.5, 1);
        }
        // If we have any left over, then we need to return that.
        return experiencePool;
    }
}
