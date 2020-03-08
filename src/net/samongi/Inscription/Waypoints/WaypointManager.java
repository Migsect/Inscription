package net.samongi.Inscription.Waypoints;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Altars;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.SamongiLib.Blocks.Altar;
import net.samongi.SamongiLib.Menu.InventoryMenu;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import javax.annotation.Nonnull;

public class WaypointManager implements Listener {

    // ---------------------------------------------------------------------------------------------------------------//
    void openWaypointMenu(@Nonnull Player player, @Nonnull Location location) {
        Waypoint waypoint = new Waypoint(location);
        InventoryMenu menu = waypoint.getInventoryMenu(player);
        menu.openMenu();
    }

    void registerWaypointLocation(@Nonnull Player player, @Nonnull Location location) {
        PlayerData playerData = Inscription.getInstance().getPlayerManager().getData(player);
        boolean added = playerData.addWaypoint(location);
        if (added) {
            player.sendMessage(ChatColor.YELLOW + "Waypoint Added");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Waypoint already added");
        }
    }

    // ---------------------------------------------------------------------------------------------------------------//

    @EventHandler void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK
            || event.useInteractedBlock() == Event.Result.DENY)
        {
            return;
        }
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Location location = block.getLocation();

        Altar waypointAltar = Altars.getWaypointAltar();
        if (!waypointAltar.checkPattern(block)) {
            return;
        }
        Inscription.logger.finer("Waypoint clicked at " + block.getLocation());

        if (player.isSneaking()) {
            registerWaypointLocation(player, location);
        } else {
            openWaypointMenu(player, location);
        }

    }
}
