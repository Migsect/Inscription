package net.samongi.Inscription.Player;

import net.samongi.Inscription.Experience.ExperienceMap;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Loot.Generator.GlyphGenerator;
import net.samongi.Inscription.Loot.LootManager;
import net.samongi.SamongiLib.Menu.ButtomAction.ButtonAction;
import net.samongi.SamongiLib.Menu.InventoryMenu;
import net.samongi.SamongiLib.Utilities.TextUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InscriptionRootMenu {

    //----------------------------------------------------------------------------------------------------------------//
    private static final String TITLE = ChatColor.DARK_PURPLE + "Inscription Menu";
    private static final int ROWS = 3;

    //----------------------------------------------------------------------------------------------------------------//
    private final Player m_viewingPlayer;
    private final PlayerData m_subjectData;

    //----------------------------------------------------------------------------------------------------------------//
    public InscriptionRootMenu(@Nonnull Player viewer, @Nonnull PlayerData subjectData) {
        m_viewingPlayer = viewer;
        m_subjectData = subjectData;
    }

    //----------------------------------------------------------------------------------------------------------------//

    private ItemStack getOverflowExperienceItem() {
        ItemStack item = new ItemStack(Material.HOPPER);

        ItemMeta itemMeta = item.getItemMeta();
        assert itemMeta != null;

        itemMeta.setDisplayName(ChatColor.DARK_GREEN + "Overflow Experience");

        List<String> lore = new ArrayList<>();
        lore.add("");
        Map<String, Integer> overflowExperience = m_subjectData.getExperience();
        for (String key : overflowExperience.keySet()) {
            String line = ChatColor.YELLOW + key + ": " + ChatColor.BLUE + overflowExperience.get(key);
            lore.add(line);
        }
        lore.add("");

        String helpLine =
            "" + ChatColor.ITALIC + ChatColor.DARK_GRAY + "Overflow experience is gathered when one does not have a valid glyph equipped for that experience. "
                + "This experience can be used to acquire undiscovered glyphs.";
        lore.addAll(TextUtil.wrapText(helpLine, 60, 0));

        itemMeta.setLore(lore);
        item.setItemMeta(itemMeta);
        return item;
    }

    private ItemStack getGlyphInventoryItem() {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta itemMeta = item.getItemMeta();
        assert itemMeta != null;

        itemMeta.setDisplayName(ChatColor.GREEN + "Glyph Inventory");

        List<String> lore = new ArrayList<>();
        lore.add("");
        String actionLine = ChatColor.YELLOW + "Click to open the glyph inventory.";
        lore.add(actionLine);
        lore.add("");
        String helpLine = "" + ChatColor.ITALIC + ChatColor.DARK_GRAY
            + "Glyphs added into the glyph inventory become activated and will benefit you in addition to being eligible to receive experience from actions you performed.";
        lore.addAll(TextUtil.wrapText(helpLine, 60, 0));

        itemMeta.setLore(lore);
        item.setItemMeta(itemMeta);
        return item;
    }

    private ItemStack getGlyphShopItem() {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta itemMeta = item.getItemMeta();
        assert itemMeta != null;

        itemMeta.setDisplayName(ChatColor.GREEN + "Glyph Discovery");
        List<String> lore = new ArrayList<>();
        lore.add("");
        String actionLine = ChatColor.YELLOW + "Click to open the glyph discovery menu.";
        lore.add(actionLine);
        lore.add("");
        String helpLine = "" + ChatColor.ITALIC + ChatColor.DARK_GRAY
            + "Overflow experience can be used to make undiscovered glyphs which can then be used to create random glyphs.";
        lore.addAll(TextUtil.wrapText(helpLine, 60, 0));

        itemMeta.setLore(lore);
        item.setItemMeta(itemMeta);
        return item;
    }

    private InventoryMenu getGlyphShopMenu() {
        String title = ChatColor.DARK_PURPLE + "Undiscovered Glyph Generation";
        InventoryMenu menu = new InventoryMenu(m_viewingPlayer, ROWS, title);

        LootManager lootManager = Inscription.getInstance().getLootManager();
        List<LootManager.GeneratorExperiencePair> generators = lootManager.getOverflowGeneratorPairs();
        int slot = 0;
        for (LootManager.GeneratorExperiencePair pair : generators) {
            GlyphGenerator generator = pair.generator;
            ExperienceMap experience = pair.experience;

            ItemStack generatorItem = lootManager.createGeneratorConsumable(generator);

            ItemMeta itemMeta = generatorItem.getItemMeta();
            assert itemMeta != null;
            List<String> lore = itemMeta.getLore();
            assert lore != null;
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "---- ---- ---- ---- ---- ----");
            lore.add("");
            lore.add(ChatColor.GREEN + "Click to purchase");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Experience Cost:");

            boolean canPurchase = true;
            for (String type : experience.experienceTypes()) {
                boolean meetsRequirement = m_subjectData.getExperience(type) > experience.get(type);
                ChatColor balanceColor = meetsRequirement ? ChatColor.DARK_GREEN : ChatColor.DARK_RED;
                lore.add("  " + ChatColor.YELLOW + type + ": " + ChatColor.BLUE + experience.get(type) + ChatColor.DARK_GRAY + " (currently have " + balanceColor
                    + m_subjectData.getExperience(type) + ChatColor.DARK_GRAY + ")");
                if (!meetsRequirement) {
                    canPurchase = false;
                }
            }

            if (canPurchase) {
                menu.addClickAction(slot, () -> {
                    ItemStack generatorConsumable = lootManager.createGeneratorConsumable(generator);
                    lootManager.dropGlyph(generator, m_viewingPlayer.getLocation());
                    for (String experienceType : experience.experienceTypes()) {
                        m_subjectData.addExperience(experienceType, -experience.get(experienceType));
                    }
                    getGlyphShopMenu().openMenu();
                });
            } else {
                lore.add("");
                lore.add(ChatColor.DARK_RED + "You don't have enough experience to create this!");
            }

            itemMeta.setLore(lore);
            generatorItem.setItemMeta(itemMeta);

            // Setting up the menu. We won't add an action when the player can't purchase the item.
            menu.setItem(slot, generatorItem);
            slot++;

        }

        return menu;
    }

    public InventoryMenu getMenu() {
        String title = ChatColor.LIGHT_PURPLE + m_subjectData.getPlayerName() + "'s " + TITLE;
        InventoryMenu rootMenu = new InventoryMenu(m_viewingPlayer, ROWS, title);

        rootMenu.setItem(26, getOverflowExperienceItem());
        rootMenu.setItem(0, getGlyphInventoryItem());
        rootMenu.addClickAction(0, () -> m_viewingPlayer.openInventory(m_subjectData.getGlyphInventory().getInventory()));
        rootMenu.setItem(1, getGlyphShopItem());
        rootMenu.addClickAction(1, () -> getGlyphShopMenu().openMenu());

        return rootMenu;
    }

    public void open() {
        getMenu().openMenu();
    }
    //----------------------------------------------------------------------------------------------------------------//

}
