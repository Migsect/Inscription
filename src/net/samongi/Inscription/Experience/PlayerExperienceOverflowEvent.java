package net.samongi.Inscription.Experience;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerExperienceOverflowEvent extends Event {

    private static final HandlerList HANDLERS_LIST = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    final private Player m_player;
    final private String m_experienceType;
    private int m_amount;

    public PlayerExperienceOverflowEvent(Player player, String experienceType, int amount)
    {
        m_player = player;
        m_experienceType = experienceType;
        m_amount = amount;
    }

    public Player getPlayer()
    {
        return m_player;
    }

    public String getExperienceType()
    {
        return m_experienceType;
    }

    public int getAmount()
    {
        return m_amount;
    }

    public void setAmount(int amount)
    {
        m_amount = amount;
    }

    @Override public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }
}
