package net.samongi.Inscription.Waypoints;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Altars;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.SamongiLib.Blocks.Altar;
import net.samongi.SamongiLib.Menu.InventoryMenu;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
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
    private static final String BASE_DISTANCE_PATH = "waypoints.base-distance";
    private static final String BASE_INTERWORLD_DISTANCE_PATH = "waypoints.base-interworld-distance";

    private static final String MIN_FAILURE_CHANCE_PATH = "waypoints.base-waypoint-distance";
    private static final String MAX_FAILURE_CHANCE_PATH = "waypoints.max-failure-chance";
    private static final String FAILURE_EXPONENT_BASE_PATH = "waypoints.failure-exponent-base";

    private static final String CURRENCY_PATH = "waypoints.currency";
    private static final String CURRENCY_POWER_PATH = "waypoints.currency-power";

    // ---------------------------------------------------------------------------------------------------------------//
    private int m_baseInterworldDistance = 10000;
    private int m_baseDistance = 128;

    private double m_minimumFailureChance = 0.05;
    private double m_maximumFailureChance = 0.95;
    private double m_failureExponentBase = 2;

    private Material m_currency = null;
    private int m_currencyPower = 0;

    // ---------------------------------------------------------------------------------------------------------------//
    public WaypointManager() {
        getConfig();
    }

    private void getConfig() {
        ConfigurationSection config = Inscription.getInstance().getConfig();

        m_baseDistance = config.getInt(BASE_DISTANCE_PATH, 64);
        m_baseInterworldDistance = config.getInt(BASE_INTERWORLD_DISTANCE_PATH, 10000);

        m_minimumFailureChance = config.getDouble(MIN_FAILURE_CHANCE_PATH, 0.0);
        m_maximumFailureChance = config.getDouble(MAX_FAILURE_CHANCE_PATH, 1.0);
        m_failureExponentBase = config.getDouble(FAILURE_EXPONENT_BASE_PATH, 2.0);

        m_currencyPower = config.getInt(CURRENCY_POWER_PATH, 0);
        String currencyString = config.getString(CURRENCY_PATH);
        try {
            m_currency = Material.valueOf(currencyString);
        } catch (IllegalArgumentException exception)
        {
            Inscription.logger.warning("Currency material '" + currencyString + "' is not a valid material.");
        }
    }
    // ---------------------------------------------------------------------------------------------------------------//
    public int getBaseDistance() {
        return m_baseDistance;
    }
    public int getBaseInterworldDistance() {
        return m_baseInterworldDistance;
    }

    public double getMinimumFailureChance() {
        return m_minimumFailureChance;
    }
    public double getMaximumFailureChance() {
        return m_maximumFailureChance;
    }
    public double getFailureExponentBase() {
        return m_failureExponentBase;
    }

    public Material getCurrency() {
        return m_currency;
    }
    public int getCurrencyPower() {
        return m_currencyPower;
    }

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
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK || event.useInteractedBlock() == Event.Result.DENY) {
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
