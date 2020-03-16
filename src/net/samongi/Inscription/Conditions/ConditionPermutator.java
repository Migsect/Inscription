package net.samongi.Inscription.Conditions;

import java.lang.reflect.Array;
import java.util.*;

public class ConditionPermutator {

    // ---------------------------------------------------------------------------------------------------------------//
    List<List<Condition>> m_conditions = new ArrayList<>();
    List<Integer> m_iterator = new ArrayList<>();
    // ---------------------------------------------------------------------------------------------------------------//
    public ConditionPermutator() {
    }

    // ---------------------------------------------------------------------------------------------------------------//
    public void addConditionGroup(Collection<Condition> conditions) {
        m_conditions.add(new ArrayList<>(conditions));
    }

    // ---------------------------------------------------------------------------------------------------------------//
    private void initializeIterator() {
        for (int index = 0; index < m_conditions.size(); index++) {
            m_iterator.add(0);
        }
    }
    private void incrementIterator(int index) {
        if (index >= m_iterator.size()) {
            return;
        }
        int iteratorIndex = m_iterator.get(index);
        int iteratorIndexMax = m_conditions.get(index).size() - 1;
        if (iteratorIndex >= iteratorIndexMax) {
            m_iterator.set(index, 0);
            incrementIterator(index + 1);
        } else {
            m_iterator.set(index, iteratorIndex + 1);
        }
    }

    private void incrementIterator() {
        incrementIterator(0);
    }

    // ---------------------------------------------------------------------------------------------------------------//
    private boolean hasNext(int index) {
        if (m_iterator.isEmpty()) {
            initializeIterator();
        }
        if (index >= m_iterator.size()) {
            return false;
        }

        int iteratorIndex = m_iterator.get(index);
        int iteratorIndexMax = m_conditions.get(index).size() - 1;
        if (iteratorIndex >= iteratorIndexMax) {
            return hasNext(index + 1);
        } else {
            return true;
        }
    }

    public boolean hasNext() {
        return hasNext(0);
    }

    public Set<Condition> next() {
        if (m_iterator.isEmpty()) {
            initializeIterator();
        }
        Set<Condition> conditions = get();
        incrementIterator();
        return conditions;
    }

    public Set<Condition> get() {
        if (m_iterator.isEmpty()) {
            initializeIterator();
        }
        Set<Condition> conditions = new HashSet<>();
        for (int index = 0; index < m_iterator.size(); index++) {
            int iteratorIndex = m_iterator.get(index);
            List<Condition> iteratorConditions = m_conditions.get(index);

            if (iteratorIndex >= m_conditions.size()) {
                continue;
            }
            conditions.add(iteratorConditions.get(iteratorIndex));
        }
        return conditions;
    }

    // ---------------------------------------------------------------------------------------------------------------//
}
