package net.samongi.Inscription.Ability;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Inscription;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import java.util.*;

public class AbilityManager implements Listener {

    //----------------------------------------------------------------------------------------------------------------//
    private Map<String, AbilityHandler> m_idToHandlers = new HashMap<>();
    private Map<String, AbilityHandler> m_displayToHandlers = new HashMap<>();

    //----------------------------------------------------------------------------------------------------------------//
    public AbilityManager() {

    }

    //----------------------------------------------------------------------------------------------------------------//
    public void registerActionHandler(AbilityHandler handler) {
        m_idToHandlers.put(handler.getId(), handler);
        m_displayToHandlers.put(handler.getDisplay(), handler);
        Inscription.logger.fine("ActionHandler Registered: " + handler.getId() + "[" + handler.getDisplay() + "]");
    }

    public List<AbilityHandler> getActionHandlers() {
        return new ArrayList<>(m_idToHandlers.values());
    }

    //----------------------------------------------------------------------------------------------------------------//
    private AbilityHandler getActionHandlerFromItem(@Nonnull ItemStack item) {

        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return null;
        }

        List<String> lore = itemMeta.getLore();
        if (lore == null || lore.size() < 1) {
            return null;
        }

        String tomeLine = lore.get(0);
        tomeLine = ChatColor.stripColor(tomeLine).replace("Tome ", "");
        return m_displayToHandlers.get(tomeLine);
    }

    //----------------------------------------------------------------------------------------------------------------//
    @EventHandler(priority = EventPriority.HIGH) public void onPlayerInteract(PlayerInteractEvent event) {

        EquipmentSlot slot = event.getHand();
        Player player = event.getPlayer();
        Action action = event.getAction();
        Block block = event.getClickedBlock();

        // These are the cases where we aren't going to trigger an interact.
        // Note that this means tomes must be main hand to use.
        boolean blockInteract = action == Action.RIGHT_CLICK_BLOCK && block != null && block.getType().isInteractable();
        if (blockInteract || event.useItemInHand() == Event.Result.DENY || slot == EquipmentSlot.OFF_HAND) {
            return;
        }

        // We only care if it is a right click on air or on a block.
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }

        AbilityHandler handler = getActionHandlerFromItem(item);
        if (handler == null) {
            return;
        }

        // We will always trigger an action.
        handler.onAction(event);

        // If there was a block targeted, then we will pass that along as further information.
        if (action == Action.RIGHT_CLICK_BLOCK) {
            handler.onActionTargetBlock(event);
        }

    }

    @EventHandler public void onPlayerInteractAtEntityEvent(PlayerInteractAtEntityEvent event) {
        // TODO This needs to be implemented when functionality for entities exists.
    }

    //----------------------------------------------------------------------------------------------------------------//
}
