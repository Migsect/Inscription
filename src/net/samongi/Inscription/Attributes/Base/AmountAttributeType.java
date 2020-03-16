package net.samongi.Inscription.Attributes.Base;

import net.samongi.Inscription.Attributes.AttributeType;
import net.samongi.Inscription.Attributes.GeneralAttributeParser;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Inscription;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

import javax.annotation.Nonnull;

public abstract class AmountAttributeType extends NumericalAttributeType {

    //----------------------------------------------------------------------------------------------------------------//
    protected AmountAttributeType(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
        super(section);
    }

    //----------------------------------------------------------------------------------------------------------------//
    public int getAmount(Glyph glyph) {
        return (int) getNumber(glyph);
    }

    private String getNumberString(Glyph glyph, double multiplier) {
        return String.format("%d", multiplier * this.getNumber(glyph));
    }
    private String getMinNumberString(Glyph glyph, double multiplier) {
        return String.format("%d", (int)(multiplier * getMin() * calculateEffectRarityMultiplier(glyph)));
    }
    private String getMaxNumberString(Glyph glyph, double multiplier) {
        return String.format("%d", multiplier * getMax() * calculateEffectRarityMultiplier(glyph));
    }

    //----------------------------------------------------------------------------------------------------------------//
}
