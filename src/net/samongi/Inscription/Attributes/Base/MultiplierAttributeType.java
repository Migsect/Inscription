package net.samongi.Inscription.Attributes.Base;

import net.samongi.Inscription.Attributes.AttributeType;
import net.samongi.Inscription.Attributes.GeneralAttributeParser;
import net.samongi.Inscription.Glyphs.Glyph;
import org.bukkit.ChatColor;

public abstract class MultiplierAttributeType extends AttributeType {

    protected double m_minMultiplier;
    protected double m_maxMultiplier;

    protected MultiplierAttributeType(GeneralAttributeParser parser) {
        super(parser);
    }

    public void setMin(double chance) {
        this.m_minMultiplier = chance;
    }

    public void setMax(double chance) {
        this.m_maxMultiplier = chance;
    }

    public double getMultiplier(Glyph glyph) {
        int glyph_level = glyph.getLevel_LEGACY();
        int rarity_level = glyph.getRarity().getRank();

        double rarity_multiplier = calculateRarityMultiplier(glyph);
        double base_chance = this.m_minMultiplier + (this.m_maxMultiplier - this.m_minMultiplier) * (glyph_level - 1) / (Glyph.MAX_LEVEL - 1);
        return rarity_multiplier * base_chance;
    }

    public String getMultiplierString(Glyph glyph, double multiplier) {
        return String.format("%.1f", multiplier * this.getMultiplier(glyph));
    }
    public String getMinMultiplierString(Glyph glyph, double multiplier) {
        return String.format("%.1f", multiplier * this.m_minMultiplier * calculateRarityMultiplier(glyph));
    }
    public String getMaxMultiplierString(Glyph glyph, double multiplier) {
        return String.format("%.1f", multiplier * this.m_maxMultiplier * calculateRarityMultiplier(glyph));
    }
    public String getDisplayString(Glyph glyph, double multiplier) {
        return getDisplayString(glyph, multiplier, "", "");
    }
    public String getDisplayString(Glyph glyph, String prefix, String suffix) {
        return getDisplayString(glyph, 1, prefix, suffix);
    }
    public String getDisplayString(Glyph glyph, double multiplier, String prefix, String suffix) {
        String multiplierString = prefix + getMultiplierString(glyph, multiplier) + suffix;
        String minMultiplierString = prefix + getMinMultiplierString(glyph, multiplier) + suffix;
        String maxMultiplierString = prefix + getMaxMultiplierString(glyph, multiplier) + suffix;

        return ChatColor.BLUE + multiplierString + ChatColor.DARK_GRAY + "[" + minMultiplierString + "," + maxMultiplierString + "]";
    }
}
