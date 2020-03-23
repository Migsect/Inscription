package net.samongi.Inscription.Player.CacheTypes;

import net.samongi.Inscription.Attributes.Base.NumericalAttributeType;
import net.samongi.Inscription.Conditions.Condition;
import net.samongi.Inscription.Conditions.ConditionDatabase;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.SamongiLib.DataStructures.PartialKeyMap;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BinaryOperator;

public abstract class NumericCacheData implements CacheData {

    //----------------------------------------------------------------------------------------------------------------//
    private double m_dataGlobal = 0;
    private ConditionDatabase m_data;

    private NumericalAttributeType.ReduceType m_reduceType = NumericalAttributeType.ReduceType.ADDITIVE;

    //----------------------------------------------------------------------------------------------------------------//
    public NumericCacheData() {
        m_data = new ConditionDatabase();
    }
    public NumericCacheData(NumericalAttributeType.ReduceType reduceType) {
        m_data = new ConditionDatabase();
        m_reduceType = reduceType;
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
        set(conditions, m_reduceType.getReduceOperator().apply(currentValue, value));
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
        //Inscription.logger.finest("getReduce/getValidConditionKeys: " + getValidConditionKeys(conditions));
        List<Double> values = m_data.getSubsets(conditions);
        //Inscription.logger.finest("getReduce values: " + values);
        return m_reduceType.getReduceOperator().apply(m_dataGlobal, values.stream().reduce(0.0, m_reduceType.getReduceOperator()));
    }

    public Set<Set<Condition>> getValidConditionKeys(Collection<Condition> conditions) {
        return m_data.getValidKeys(conditions);
    }

    public Set<Set<Condition>> keySet() {
        return m_data.keySet();
    }

    //----------------------------------------------------------------------------------------------------------------//
    @Override public void clear() {
        m_dataGlobal = 0;
        m_data.clear();
    }

}
