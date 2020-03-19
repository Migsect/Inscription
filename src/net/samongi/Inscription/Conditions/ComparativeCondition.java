package net.samongi.Inscription.Conditions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;

public abstract class ComparativeCondition implements Condition {

    //----------------------------------------------------------------------------------------------------------------//
    private static interface Comparison {

        boolean compare(Double a, Double b);
    }

    public static boolean contains(Collection<Condition> conditions, Condition condition) {
        if (condition instanceof ComparativeCondition) {
            for (Condition collectionCondition : conditions) {
                if (!condition.getClass().isInstance(collectionCondition)) {
                    continue;
                }
                ComparativeCondition groupCompareCondition = (ComparativeCondition) collectionCondition;
                ComparativeCondition compareCondition = (ComparativeCondition) condition;
                if (!compareCondition.compare(groupCompareCondition)) {
                    return false;
                }
            }
        } else {
            return conditions.contains(condition);
        }
        return true;
    }

    //----------------------------------------------------------------------------------------------------------------//
    public enum Mode {
        NULL("SHOULD NOT BE SEEN", (a, b) -> false),
        EQUALS("is", Object::equals),
        NOT_EQUALS("is not", (a, b) -> !a.equals(b)),
        AT_MOST("is at most", (a, b) -> a <= b),
        AT_LEAST("is at least", (a, b) -> a >= b),
        LESS_THAN("is more than", (a, b) -> a < b),
        MORE_THAN("is less than", (a, b) -> a > b);

        private final String m_display;
        private final Comparison m_comparison;
        private Mode(String display, Comparison comparison) {
            m_display = display;
            m_comparison = comparison;
        }

        public @Nonnull String getDisplay() {
            return m_display;
        }

        public boolean compare(@Nonnull Double a, @Nonnull Double b) {
            return m_comparison.compare(a, b);
        }
    }

    //----------------------------------------------------------------------------------------------------------------//
    private final Double m_value;
    private final Mode m_mode;

    //----------------------------------------------------------------------------------------------------------------//
    public ComparativeCondition(@Nonnull Double value, @Nonnull Mode mode) {
        m_value = value;
        m_mode = mode;
    }
    public ComparativeCondition(ConfigurationSection section) throws InvalidConfigurationException {
        String modeString = section.getString("mode");
        if (modeString == null) {
            throw new InvalidConfigurationException("'mode' is not defined");
        }

        try {
            m_mode = Mode.valueOf(modeString);
        }
        catch (IllegalArgumentException exception) {
            throw new InvalidConfigurationException("'" + modeString + "' is not a valid mode");
        }

        if (section.isDouble("value")) {
            throw new InvalidConfigurationException("'value' is not defined");
        }
        m_value = section.getDouble("value");
    }

    //----------------------------------------------------------------------------------------------------------------//
    public @Nonnull Mode getMode() {
        return m_mode;
    }
    public @Nonnull Double getValue() {
        return m_value;
    }
    public boolean compare(ComparativeCondition other) {
        if (m_mode == Mode.NULL) {
            return other.m_mode.compare(m_value, other.getValue());
        }
        return m_mode.compare(other.getValue(), m_value);
    }
    //----------------------------------------------------------------------------------------------------------------//

    public boolean equals(ComparativeCondition other) {
        return m_mode.equals(other.m_mode) && m_value.equals(other.m_value);
    }

    @Override public boolean equals(Object obj) {
        if (obj instanceof ComparativeCondition) {
            return equals((ComparativeCondition) obj);
        }
        return false;
    }
    @Override public int hashCode() {
        return Arrays.hashCode(new Object[]{m_value, m_mode});
    }
    @Override public String toString() {
        return "{" + this.getClass().getSimpleName() + "," + getMode().toString() + "," + (Math.round(1000 * getValue()) / 1000) + "}";
    }

    //----------------------------------------------------------------------------------------------------------------//
}
