package net.samongi.Inscription.Ability;

import net.samongi.SamongiLib.Utilities.TextUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public abstract class AbilityHandler {

    //----------------------------------------------------------------------------------------------------------------//
    private final String m_id;
    private final String m_display;
    private final String m_description;

    //----------------------------------------------------------------------------------------------------------------//
    public AbilityHandler(String id, String display, String description) {
        m_id = id;
        m_display = display;
        m_description = description;
    }

    //----------------------------------------------------------------------------------------------------------------//
    public String getId() {
        return m_id;
    }
    public String getDisplay() {
        return m_display;
    }
    public String getDescription() {
        return m_description;
    }

    //----------------------------------------------------------------------------------------------------------------//
    public ItemStack getItemstack() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta itemMeta = item.getItemMeta();
        assert itemMeta != null;

        itemMeta.setDisplayName(ChatColor.GOLD + getDisplay() + " Tome");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Tome " + ChatColor.BLUE + getDisplay());
        lore.add("");
        lore.addAll(TextUtil.wrapText("" + ChatColor.ITALIC + ChatColor.GRAY + getDescription(), 60, 0));

        itemMeta.setLore(lore);

        item.setItemMeta(itemMeta);

        return item;
    }

    //----------------------------------------------------------------------------------------------------------------//
    public void onAction(PlayerInteractEvent event) {
        // Empty
    }
    public void onActionTargetBlock(PlayerInteractEvent event) {
        // Empty
    }
    public void onActionTargetEntity(PlayerInteractAtEntityEvent event) {
        // Empty
    }

    //----------------------------------------------------------------------------------------------------------------//
}
