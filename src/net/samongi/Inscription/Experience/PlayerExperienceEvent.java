package net.samongi.Inscription.Experience;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerExperienceEvent extends PlayerEvent {
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    final private String m_experienceType;
    private int m_amount;

    public PlayerExperienceEvent(Player player, String experienceType, int amount)
    {
        super(player);
        m_experienceType = experienceType;
        m_amount = amount;
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
