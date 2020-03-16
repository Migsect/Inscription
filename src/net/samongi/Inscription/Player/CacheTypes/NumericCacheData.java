package net.samongi.Inscription.Player.CacheTypes;

import net.samongi.Inscription.Attributes.Base.NumericalAttributeType;
import net.samongi.Inscription.Conditions.Condition;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.SamongiLib.DataStructures.PartialKeyMap;

import java.util.Collection;
import java.util.Set;
import java.util.function.BinaryOperator;

public abstract class NumericCacheData implements CacheData {

    //----------------------------------------------------------------------------------------------------------------//
    private double m_dataGlobal = 0;
    private PartialKeyMap<Condition, Double> m_data;

    private BinaryOperator<Double> m_reduceOperator = NumericalAttributeType.ReduceType.ADDITIVE.getReduceOperator();

    //----------------------------------------------------------------------------------------------------------------//
    public NumericCacheData() {
        m_data = new PartialKeyMap<>();
    }
    public NumericCacheData(BinaryOperator<Double> reduceOperator) {
        m_data = new PartialKeyMap<>();
        m_reduceOperator = reduceOperator;
    }

    //----------------------------------------------------------------------------------------------------------------//
    public void set(double value) {
        m_dataGlobal = value;
    }
    public void set(Set<Condition> conditions, double value) {
        if (conditions.isEmpty()) {
            m_dataGlobal = value;
        }
        m_data.put(conditions, value);
    }
    public void add(Set<Condition> conditions, double value) {
        double currentValue = get(conditions);
        set(conditions, m_reduceOperator.apply(currentValue, value));
    }

    public double get() {
        return m_dataGlobal;
    }
    public double get(Set<Condition> conditions) {
        if (conditions.isEmpty()) {
            return m_dataGlobal;
        }
        return m_data.getOrDefault(conditions, 0.0);
    }

    public double getReduce(Set<Condition> conditions) {
        double valueSum = m_dataGlobal;
        Collection<Double> values = m_data.getSubsets(conditions);
        return m_reduceOperator.apply(m_dataGlobal, values.stream().reduce(0.0, m_reduceOperator));
    }

    public Set<Set<Condition>> getValidConditionKeys(Collection<Condition> conditions) {
        return m_data.getValidKeys(conditions);
    }

    //----------------------------------------------------------------------------------------------------------------//
    @Override public void clear() {
        m_dataGlobal = 0;
        m_data.clear();
    }

}
