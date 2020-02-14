package net.samongi.Inscription.Glyphs.Attributes.Base;

import net.samongi.Inscription.Glyphs.Attributes.AttributeType;
import net.samongi.Inscription.Glyphs.Glyph;

public abstract class MultiplierAttributeType extends AttributeType {

    protected double m_minMultiplier;
    protected double m_maxMultiplier;

    protected MultiplierAttributeType(String typeName, String description)
    {
        super(typeName, description);
    }

    public void setMin(double chance)
    {
        this.m_minMultiplier = chance;
    }

    public void setMax(double chance)
    {
        this.m_maxMultiplier = chance;
    }

    public double getMultiplier(Glyph glyph)
    {
        int glyph_level = glyph.getLevel();
        int rarity_level = glyph.getRarity().getRank();

        double rarity_multiplier = 1 + this.rarityMultiplier * rarity_level;
        double base_chance = this.m_minMultiplier + (this.m_maxMultiplier - this.m_minMultiplier) * (glyph_level - 1)
            / (Glyph.MAX_LEVEL - 1);
        return rarity_multiplier * base_chance;
    }

    public String getMultiplierString(Glyph glyph)
    {
        return String.format("%.1f", this.getMultiplier(glyph));
    }
    public String getMultiplierPercentageString(Glyph glyph) {
        return String.format("%.1f", this.getMultiplier(glyph) * 100);
    }
}
