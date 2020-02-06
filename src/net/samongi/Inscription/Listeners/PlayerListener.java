package net.samongi.Inscription.Listeners;

import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.GlyphInventory;
import net.samongi.Inscription.Player.PlayerData;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerListener implements Listener
{

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event)
  {
    Inscription.getInstance().getPlayerManager().onPlayerJoin(event);
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event)
  {
    Inscription.getInstance().getPlayerManager().onPlayerQuit(event);
  }

  @EventHandler
  public void onCraftItem(CraftItemEvent event)
  {
    if (event.isCancelled()) return;
    Inscription.getInstance().getExperienceManager().onCraftItem(event);
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event)
  {
    GlyphInventory.onInventoryClose(event);
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event)
  {
    if (event.isCancelled()) return;
    GlyphInventory.onInventoryClick(event);
  }

  @EventHandler
  public void onBlockRightClick(PlayerInteractEvent event)
  {
    if (event.isCancelled()) return;
    Player player = event.getPlayer();

    ItemStack hand_item = event.getItem();
    if (hand_item == null) return;
    Block clicked_block = event.getClickedBlock();
    if (clicked_block == null) return;

    Material clicked_material = clicked_block.getType();
    Material hand_material = hand_item.getType();

    if (clicked_material.equals(Material.ENCHANTING_TABLE) && hand_material.equals(Material.PAPER))
    {
      PlayerData data = Inscription.getInstance().getPlayerManager().getData(player);
      GlyphInventory inventory = data.getGlyphInventory();
      BukkitRunnable task = new BukkitRunnable()
      {

        @Override
        public void run()
        {
          player.openInventory(inventory.getInventory());
        }
      };
      task.runTask(Inscription.getInstance());
      Inscription.logger.finest("Player Attempted to open Glyph Inventory through Enchanting table");
    }
  }

}
