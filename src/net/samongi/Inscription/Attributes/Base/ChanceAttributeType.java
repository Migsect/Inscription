package net.samongi.Inscription.Attributes.Base;

import net.samongi.Inscription.Attributes.AttributeTypeConstructor;
import net.samongi.Inscription.Attributes.GeneralAttributeParser;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Attributes.AttributeType;

/**
 * This is an attribute type which relies on possibility.
 * It generally has a maximum chance and a minimum chance to do something on
 * some event.
 *
 * @author Migsect
 */
public abstract class ChanceAttributeType extends AttributeType {

    protected double min_chance;
    protected double max_chance;

    protected ChanceAttributeType(GeneralAttributeParser parser) {
        super(parser);
    }

    public void setMin(double chance) {
        this.min_chance = chance;
    }

    public void setMax(double chance) {
        this.max_chance = chance;
    }

    public double getChance(Glyph glyph) {
        int glyph_level = glyph.getLevel();
        int rarity_level = glyph.getRarity().getRank();

        double rarity_multiplier = 1 + this.m_rarityMultiplier * rarity_level;
        double base_chance =
            this.min_chance + (this.max_chance - this.min_chance) * (glyph_level - 1) / (Glyph.MAX_LEVEL - 1);
        return rarity_multiplier * base_chance;
    }

    public String getChanceString(Glyph glyph) {
        return String.format("%.1f", this.getChance(glyph) * 100);
    }

}