package net.samongi.Inscription.Listeners;

import net.samongi.Inscription.Experience.PlayerExperienceOverflowEvent;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.GlyphInventory;
import net.samongi.Inscription.Player.PlayerData;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerListener implements Listener {

    @EventHandler public void onPlayerJoin(PlayerJoinEvent event) {

        //Inscription.getInstance().getPlayerManager().onPlayerJoin(event);
    }

    @EventHandler public void onPlayerQuit(PlayerQuitEvent event) {

        //Inscription.getInstance().getPlayerManager().onPlayerQuit(event);
    }

    @EventHandler public void onCraftItem(CraftItemEvent event) {
        if (event.isCancelled()) {
            return;
        }
        // Inscription.getInstance().getExperienceManager().onCraftItem(event);
    }

    @EventHandler public void onInventoryClose(InventoryCloseEvent event) {

        GlyphInventory.onInventoryClose(event);
    }

    @EventHandler public void onInventoryClick(InventoryClickEvent event) {
        if (event.isCancelled()) {
            return;
        }
        GlyphInventory.onInventoryClick(event);
    }

    @EventHandler public void onPlayerInteractEvent(PlayerInteractEvent event) {
        onPlayerInteractEnchantingBlock(event);
        //Inscription.getInstance().getLootManager().onPlayerInteractEvent(event);
    }

    @EventHandler public void onPlayerExperienceOverflowEvent(PlayerExperienceOverflowEvent event) {
        //Inscription.getInstance().getLootManager().onPlayerExperienceOverflowEvent(event);

    }

    private void onPlayerInteractEnchantingBlock(PlayerInteractEvent event) {
        if (event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }
        Player player = event.getPlayer();

        ItemStack handItem = event.getItem();
        if (handItem == null) {
            return;
        }
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        Material clickedMaterial = clickedBlock.getType();
        Material handMaterial = handItem.getType();

        if ((clickedMaterial.equals(Material.ENCHANTING_TABLE) || clickedMaterial.equals(Material.LECTERN)) && handMaterial.equals(Material.PAPER)) {
            PlayerData data = Inscription.getInstance().getPlayerManager().getData(player);
            GlyphInventory inventory = data.getGlyphInventory();
            BukkitRunnable task = new BukkitRunnable() {

                @Override public void run() {
                    player.openInventory(inventory.getInventory());
                }
            };
            task.runTask(Inscription.getInstance());
            Inscription.logger.finest("Player Attempted to open Glyph Inventory through Enchanting table");
        }
    }

    @EventHandler private void onInventoryOpen(InventoryOpenEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Inventory inventory = event.getInventory();
        ItemStack[] items = inventory.getContents();
        for (ItemStack item : items) {
            if (item == null) {
                continue;
            }

            // We want to make sure that we check things simply before we do costly parsing.
            if (item.getType() != Material.PAPER) {
                continue;
            }
            ItemMeta itemMeta = item.getItemMeta();
            if (itemMeta.getLore() == null || itemMeta.getLore().size() <= 0) {
                continue;
            }

            Glyph glyph = Glyph.getGlyph(item);
            if (glyph == null) {
                continue;
            }

            int modelData = glyph.getCustomModelData();
            itemMeta.setCustomModelData(modelData);
            item.setItemMeta(itemMeta);
        }
    }
}
