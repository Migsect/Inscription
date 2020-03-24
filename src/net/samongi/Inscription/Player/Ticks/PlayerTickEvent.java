package net.samongi.Inscription.Player.Ticks;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerTickEvent extends PlayerEvent {

    //----------------------------------------------------------------------------------------------------------------//
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    //----------------------------------------------------------------------------------------------------------------//

    int m_tickCount = -1;

    //----------------------------------------------------------------------------------------------------------------//

    public PlayerTickEvent(Player who, int tickCount) {
        super(who);
        m_tickCount = tickCount;
    }

    //----------------------------------------------------------------------------------------------------------------//

    public int getTickCount() {
        return m_tickCount;
    }

    @Override public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }
    //----------------------------------------------------------------------------------------------------------------//
}
