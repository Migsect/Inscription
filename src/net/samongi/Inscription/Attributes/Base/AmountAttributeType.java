package net.samongi.Inscription.Attributes.Base;

import net.samongi.Inscription.Attributes.AttributeType;
import net.samongi.Inscription.Attributes.GeneralAttributeParser;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Inscription;
import org.bukkit.ChatColor;

public abstract class AmountAttributeType extends AttributeType {

    protected double m_minAmount;
    protected double m_maxAmount;

    protected AmountAttributeType(GeneralAttributeParser parser) {
        super(parser);
    }

    public void setMin(double chance) {
        m_minAmount = chance;
    }

    public void setMax(double chance) {
        m_maxAmount = chance;
    }

    public int getAmount(Glyph glyph) {
        int glyph_level = glyph.getLevel();
        int rarity_level = glyph.getRarity().getRank();

        double rarity_multiplier = 1 + this.m_rarityMultiplier * rarity_level;
        double baseAmount = this.m_minAmount + (this.m_maxAmount - this.m_minAmount) * (glyph_level - 1) / (Inscription.getMaxLevel() - 1);
        return (int)(rarity_multiplier * baseAmount);
    }

    public String getAmountString(Glyph glyph) {

        return String.format("%d", this.getAmount(glyph) );
    }
    public String getMinAmountString(Glyph glyph) {
        return String.format("%d", (int)(this.m_minAmount  * calculateRarityMultiplier(glyph)));
    }
    public String getMaxAmountString(Glyph glyph) {
        return String.format("%d", (int)(this.m_maxAmount  * calculateRarityMultiplier(glyph)));
    }

    public String getDisplayString(Glyph glyph) {
        return getDisplayString(glyph, "", "");
    }
    public String getDisplayString(Glyph glyph, String prefix, String suffix) {
        String chanceString = prefix + getAmountString(glyph) + suffix;
        String minAmountString = prefix + getMinAmountString(glyph) + suffix;
        String maxAmountString = prefix + getMaxAmountString(glyph) + suffix;

        return ChatColor.BLUE + chanceString + ChatColor.DARK_GRAY + "[" + minAmountString + "," + maxAmountString + "]";
    }
}
