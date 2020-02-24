package net.samongi.Inscription.Glyphs.Types;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Inscription;
import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GlyphElement {

    private final String m_typeName;
    private final String m_display;
    private final ChatColor m_color;
    private final int m_modelIncrement;

    public GlyphElement(String typeName, String display, ChatColor color, int modelIncrement) {
        m_typeName = typeName;
        m_display = display;
        m_color = color;
        m_modelIncrement = modelIncrement;
    }

    public String getDisplay() {

        return this.m_display;
    }
    public String getType() {

        return m_typeName;
    }
    public ChatColor getColor() {

        return this.m_color;
    }
    public int getModelIncrement() {

        return this.m_modelIncrement;
    }

    public static @Nullable GlyphElement parse(@Nonnull String typeName, @Nonnull ConfigurationSection section) {
        if (!section.isString("display") || !section.isString("chat-color") || !section.isInt("model-increment")) {
            return null;
        }

        String display = section.getString("display");
        String chatColorString = section.getString("chat-color");
        ChatColor chatColor;
        try {
            chatColor = ChatColor.valueOf(chatColorString);
        }
        catch (IllegalArgumentException exception) {
            Inscription.logger.warning("'" + chatColorString + "' is not a valid ChatColor string.");
            return null;
        }

        int modelIncrement = section.getInt("model-increment");

        return new GlyphElement(typeName, display, chatColor, modelIncrement);
    }
}
