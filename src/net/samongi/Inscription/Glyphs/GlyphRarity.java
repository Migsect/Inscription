package net.samongi.Inscription.Glyphs;

import org.bukkit.ChatColor;

public enum GlyphRarity {

    COMMON("Common", ChatColor.WHITE, 0, 0),
    MAGICAL("Magical", ChatColor.GREEN, 1, 0),
    RARE("Rare", ChatColor.BLUE, 2, 0),
    MYTHIC("Mythic", ChatColor.GOLD, 3, 0),
    LEGENDARY("Legendary", ChatColor.RED, 4, 0);

    private final String m_display;
    private final ChatColor m_color;
    private final int m_rank;
    private final int m_modelIncrement;

    GlyphRarity(String display, ChatColor color, int rank, int modelIncrement) {
        m_display = display;
        m_color = color;
        m_rank = rank;
        m_modelIncrement = modelIncrement;
    }

    public String getDisplay() {
        return this.m_display;
    }
    public ChatColor getColor() {
        return this.m_color;
    }
    public int getRank() {
        return this.m_rank;
    }
    public int getModelIncrement() {
        return this.m_modelIncrement;
    }

}
