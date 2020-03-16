package net.samongi.Inscription.Attributes.Types;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Attributes.Attribute;
import net.samongi.Inscription.Attributes.AttributeType;
import net.samongi.Inscription.Attributes.AttributeTypeFactory;
import net.samongi.Inscription.Attributes.Base.NumericalAttributeType;
import net.samongi.Inscription.Attributes.GeneralAttributeParser;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.PlayerData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;

import javax.annotation.Nonnull;
import java.util.Random;

public class ExperienceBonusAttributeType extends NumericalAttributeType {

    private static final String TYPE_IDENTIFIER = "EXPERIENCE_BONUS";

    //--------------------------------------------------------------------------------------------------------------------//
    protected ExperienceBonusAttributeType(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
        super(section);
    }
    //--------------------------------------------------------------------------------------------------------------------//
    @Override public Attribute generate() {
        return new Attribute(this) {

            @Override public void cache(PlayerData playerData) {
                CacheData cached_data = playerData.getData(ExperienceBonusAttributeType.TYPE_IDENTIFIER);
                if (cached_data == null) {
                    cached_data = new ExperienceBonusAttributeType.Data();
                }
                if (!(cached_data instanceof ExperienceBonusAttributeType.Data)) {
                    return;
                }

                Inscription.logger.finer("Caching attribute for " + m_displayName);
                ExperienceBonusAttributeType.Data data = (ExperienceBonusAttributeType.Data) cached_data;

                double multiplier = getNumber(this.getGlyph());
                double currentValue = data.get();
                double newValue = currentValue + multiplier;

                data.set(newValue > 1 ? 1 : newValue);
                Inscription.logger.finer("  +C Added '" + multiplier + "' bonus " + currentValue + "->" + newValue);

                playerData.setData(data);
                Inscription.logger.finer("Finished caching for " + m_displayName);
            }

            @Override public String getLoreLine() {
                String chanceString = getDisplayString(this.getGlyph(), "+", "x");

                String infoLine = chanceString + ChatColor.YELLOW + " extra experience.";
                return this.getType().getLoreLine() + infoLine;
            }
        };
    }

    //--------------------------------------------------------------------------------------------------------------------//
    public static class Data implements CacheData {

        /* Data members of the the data */
        private double m_globalExperienceBonus = 0.0;

        /* *** Setters *** */
        public void set(double amount) {
            this.m_globalExperienceBonus = amount;
        }

        /* *** Getters *** */
        public double get() {
            return this.m_globalExperienceBonus;
        }

        @Override public void clear() {
            this.m_globalExperienceBonus = 0.0;
        }

        @Override public String getType() {
            return TYPE_IDENTIFIER;
        }

        @Override public String getData() {
            // TODO This returns the data as a string
            return "";
        }
    }

    //--------------------------------------------------------------------------------------------------------------------//
    public static class Factory extends AttributeTypeFactory {

        @Nonnull @Override public String getAttributeTypeId() {
            return TYPE_IDENTIFIER;
        }

        @Nonnull @Override public AttributeType construct(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
            return new ExperienceBonusAttributeType(section);
        }
        @Override public Listener getListener() {
            return new Listener() {

                @EventHandler public void onExperienceChange(PlayerExpChangeEvent event) {
                    Player player = event.getPlayer();
                    PlayerData playerData = Inscription.getInstance().getPlayerManager().getData(player);
                    CacheData cacheData = playerData.getData(ExperienceBonusAttributeType.TYPE_IDENTIFIER);
                    if (!(cacheData instanceof ExperienceBonusAttributeType.Data)) {
                        return;
                    }
                    ExperienceBonusAttributeType.Data data = (ExperienceBonusAttributeType.Data) cacheData;

                    double experienceMultiplier = 1 + data.get();
                    int experience = event.getAmount();
                    double multipliedExperience = experience * experienceMultiplier;
                    double fractionalExperience = multipliedExperience - Math.floor(multipliedExperience);

                    Random rand = new Random();
                    if (rand.nextDouble() < fractionalExperience) {
                        multipliedExperience += 1;
                    }
                    event.setAmount((int) multipliedExperience);
                    Inscription.logger.finest(
                        "" + "[PlayerExpChangeEvent] Extra Experience: " + experienceMultiplier + " " + experience
                            + " -> " + multipliedExperience);
                }
            };
        }
    }

}
