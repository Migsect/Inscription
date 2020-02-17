package net.samongi.Inscription.Glyphs;

import net.md_5.bungee.api.ChatColor;

public enum GlyphElement {

    FIRE("Fire", ChatColor.RED, 2),
    WATER("Water", ChatColor.AQUA, 7),
    AIR("Air", ChatColor.LIGHT_PURPLE, 11),
    EARTH("Earth", ChatColor.GREEN, 6),
    SHADOW("Shadow", ChatColor.DARK_GRAY, 15),
    LIGHT("Light", ChatColor.YELLOW, 4),
    TIME("Time", ChatColor.BLUE, 10),
    SPACE("Space", ChatColor.DARK_PURPLE, 12);

    private final String m_display;
    private final ChatColor m_color;
    private final int m_modelIncrement;

    GlyphElement(String display, ChatColor color, int modelIncrement) {
        this.m_display = display;
        this.m_color = color;
        this.m_modelIncrement = modelIncrement;
    }

    public String getDisplay() {

        return this.m_display;
    }
    public ChatColor getColor() {

        return this.m_color;
    }
    public int getModelIncrement() {

        return this.m_modelIncrement;
    }

}
