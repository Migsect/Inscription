package net.samongi.Inscription.Glyphs;

import org.bukkit.ChatColor;

@Deprecated public enum GlyphRarity_OLD {

    COMMON("Common", ChatColor.WHITE, 0, 100),
    MAGICAL("Magical", ChatColor.GREEN, 1, 600),
    RARE("Rare", ChatColor.BLUE, 2, 1000),
    MYTHIC("Mythic", ChatColor.GOLD, 3, 400),
    LEGENDARY("Legendary", ChatColor.RED, 4, 300);

    private final String m_display;
    private final ChatColor m_color;
    private final int m_rank;
    private final int m_modelIncrement;

    GlyphRarity_OLD(String display, ChatColor color, int rank, int modelIncrement) {
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
