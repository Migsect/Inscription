package net.samongi.Inscription.Conditions;

import net.samongi.Inscription.TypeClass.TypeClass;

public abstract class TypeClassCondition<TClass extends TypeClass> implements Condition {

    // ---------------------------------------------------------------------------------------------------------------//
    public interface TypeClassToConditionConverter {
        public Condition convert(TypeClass tClass);
    }
    // ---------------------------------------------------------------------------------------------------------------//
    protected TClass m_typeClass;

    // ---------------------------------------------------------------------------------------------------------------//
    public TypeClassCondition(TClass typeClass) {
        m_typeClass = typeClass;
    }
    public TypeClassCondition() {
    }

    // ---------------------------------------------------------------------------------------------------------------//
    public TypeClass getTypeClass() {
        return m_typeClass;
    }

    // ---------------------------------------------------------------------------------------------------------------//
    @Override public int hashCode() {
        return 31 * m_typeClass.hashCode();
    }

    // ---------------------------------------------------------------------------------------------------------------//
}
