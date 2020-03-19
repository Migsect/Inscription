package net.samongi.Inscription.Attributes.Base;

import net.samongi.Inscription.Attributes.AttributeType;
import net.samongi.Inscription.Attributes.GeneralAttributeParser;
import net.samongi.Inscription.Conditions.Condition;
import net.samongi.Inscription.Conditions.ConditionPermutator;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.CacheTypes.CompositeCacheData;
import net.samongi.Inscription.Player.CacheTypes.NumericCacheData;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

public abstract class NumericalAttributeType extends AttributeType {

    //----------------------------------------------------------------------------------------------------------------//
    public static enum ReduceType {

        PRE_ADDITIVE(Double::sum, 0),
        PRE_MULTIPLICATIVE((Double lhs, Double rhs) -> lhs * rhs, 1),
        PRE_MINIMUM(Double::min, Double.MAX_VALUE),
        PRE_MAXIMUM(Double::max, Double.MIN_VALUE),

        ADDITIVE(Double::sum, 0),
        MULTIPLICATIVE((Double lhs, Double rhs) -> lhs * rhs, 1),
        MINIMUM(Double::min, Double.MAX_VALUE),
        MAXIMUM(Double::max, Double.MIN_VALUE);

        //----------------------------------------------------------------------------------------------------------------//
        private final BinaryOperator<Double> m_operator;
        private final double m_initialAggregator;
        //----------------------------------------------------------------------------------------------------------------//

        ReduceType(BinaryOperator<Double> operator, double initialAggregator) {
            m_operator = operator;
            m_initialAggregator = initialAggregator;
        }

        public double exectute(double lhs, double rhs) {
            return m_operator.apply(lhs, rhs);
        }
        public BinaryOperator<Double> getReduceOperator() {
            return m_operator;
        }
        public double getInitialAggregator() {
            return m_initialAggregator;
        }
        //----------------------------------------------------------------------------------------------------------------//
    }

    public static double calculateConditionAggregate(Collection<Set<Condition>> conditionGroups, CompositeCacheData<ReduceType, NumericCacheData> cacheData) {
        Double aggregate = null;
        for (ReduceType reduceType : ReduceType.values()) {
            NumericCacheData numericData = cacheData.getCacheData(reduceType);
            if (numericData == null) {
                continue;
            }

            ConditionPermutator permutator = new ConditionPermutator();
            for (Set<Condition> conditions : conditionGroups) {
                Inscription.logger.finest("conditions " + conditions.stream().map(Condition::toString).collect(Collectors.toList()));
                permutator.addConditionGroup(conditions);
            }

            double subAggregate = numericData.get();
            Set<Set<Condition>> validConditionSets = new HashSet<>();
            while (permutator.hasNext()) {
                Set<Condition> conditionSet = permutator.next();
                validConditionSets.addAll(numericData.getValidConditionKeys(conditionSet));
            }
            for (Set<Condition> conditionSet : validConditionSets) {
                double amount = numericData.get(conditionSet);
                Inscription.logger.finest(conditionSet + " : " + amount);
                subAggregate = reduceType.exectute(subAggregate, amount);
            }

            if (aggregate == null) {
                aggregate = subAggregate;
            } else {
                aggregate = reduceType.exectute(aggregate, subAggregate);
            }

        }
        return aggregate;
    }

    //----------------------------------------------------------------------------------------------------------------//
    private double m_minNumber;
    private double m_maxNumber;

    private ReduceType m_reduceType = ReduceType.ADDITIVE;

    //----------------------------------------------------------------------------------------------------------------//
    protected NumericalAttributeType(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
        super(section);

        if (!section.contains("effect-min")) {
            throw new InvalidConfigurationException("'effect-min' is not defined");
        }

        if (!section.contains("effect-max")) {
            throw new InvalidConfigurationException("'effect-max' is not defined");
        }

        m_minNumber = section.getDouble("effect-min");
        m_maxNumber = section.getDouble("effect-max");

        String reduceTypeString = section.getString("reduce-type");
        if (reduceTypeString != null) {
            try {
                m_reduceType = ReduceType.valueOf(reduceTypeString);
            }
            catch (IllegalArgumentException exception) {
                throw new InvalidConfigurationException("Invalid value for 'reduce-type'");
            }
        }
    }

    //----------------------------------------------------------------------------------------------------------------//
    public void setMin(double number) {
        this.m_minNumber = number;
    }

    public void setMax(double number) {
        this.m_maxNumber = number;
    }

    public double getMin() {
        return m_minNumber;
    }
    public double getMax() {
        return m_maxNumber;
    }

    public double getNumber(Glyph glyph) {
        int glyph_level = glyph.getLevel();
        int rarity_level = glyph.getRarity().getRank();

        double rarityMultiplier = calculateEffectRarityMultiplier(glyph);
        double base_chance = this.m_minNumber + (this.m_maxNumber - this.m_minNumber) * (glyph_level - 1) / (Inscription.getMaxLevel() - 1);
        return rarityMultiplier * base_chance;
    }

    public ReduceType getReduceType() {
        return m_reduceType;
    }

    public boolean isPositive(Glyph glyph) {
        return getNumber(glyph) >= 0;
    }

    //----------------------------------------------------------------------------------------------------------------//

    private String getNumberString(Glyph glyph, double multiplier) {
        return String.format("%.1f", multiplier * this.getNumber(glyph));
    }
    private String getMinNumberString(Glyph glyph, double multiplier) {
        return String.format("%.1f", multiplier * this.m_minNumber * calculateEffectRarityMultiplier(glyph));
    }
    private String getMaxNumberString(Glyph glyph, double multiplier) {
        return String.format("%.1f", multiplier * this.m_maxNumber * calculateEffectRarityMultiplier(glyph));
    }
    public String getDisplayString(Glyph glyph, double multiplier) {
        return getDisplayString(glyph, multiplier, "", "");
    }
    public String getDisplayString(Glyph glyph, String prefix, String suffix) {
        return getDisplayString(glyph, 1, prefix, suffix);
    }
    public String getDisplayString(Glyph glyph, double multiplier, String prefix, String suffix) {
        String numberString = prefix + getNumberString(glyph, multiplier) + suffix;
        String minNumberString = prefix + getMinNumberString(glyph, multiplier) + suffix;
        String maxNumberString = prefix + getMaxNumberString(glyph, multiplier) + suffix;

        return ChatColor.BLUE + numberString + ChatColor.DARK_GRAY + "[" + minNumberString + "," + maxNumberString + "]";
    }

    //----------------------------------------------------------------------------------------------------------------//
}
