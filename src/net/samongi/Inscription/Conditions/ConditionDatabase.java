package net.samongi.Inscription.Conditions;

import net.samongi.Inscription.Inscription;
import net.samongi.SamongiLib.DataStructures.PartialKeyMap;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Extends the partial key map to make allow for conditions that are comparative.
 * What needs to be changed is how getInclusiveKeys operates in which it needs to do a search over all the key caches
 * instead just grabbing the first one.  Since the inclusive keys are based on comparisons.
 */
public class ConditionDatabase extends PartialKeyMap<Condition, Double> {

    //----------------------------------------------------------------------------------------------------------------//

    @Override public Set<Set<Condition>> getInclusiveKeys(Condition key) {
        if (!(key instanceof ComparativeCondition)) {
            return getInclusiveKeysDirect(key);
        }

        ComparativeCondition comparativeKey = (ComparativeCondition) key;
        Set<Condition> cachedKeys = getCachedKeys();
        Set<Set<Condition>> conditionKeys = new HashSet<Set<Condition>>();
        for (Condition condition : cachedKeys) {
            // We need to ensure that our classes match while we are doing this search.
            if (!key.getClass().isInstance(condition)) {
                continue;
            }

            ComparativeCondition otherCondition = (ComparativeCondition) condition;
            Inscription.logger.finest("  Comparison " + otherCondition.compare(comparativeKey));
            if (otherCondition.compare(comparativeKey)) {
                conditionKeys.addAll(getInclusiveKeysDirect(condition));
                Inscription.logger.finest("  Keys Added " + getInclusiveKeysDirect(condition).size());
            }
        }

        return conditionKeys;
    }

    @Override public Set<Set<Condition>> getValidKeys(@Nonnull Collection<Condition> keys) {
        Set<Set<Condition>> validKeys = new HashSet<>();
        for (Condition key : keys) {
            // Finding all the key subsets.
            Set<Set<Condition>> involvedKeySets = getInclusiveKeys(key);
            for (Set<Condition> keySet : involvedKeySets) {
                boolean containsAll = true;
                for (Condition keySetKey : keySet) {
                    if (!ComparativeCondition.contains(keySet, keySetKey)) {
                        containsAll = false;
                        break;
                    }
                }
                if (containsAll) {
                    validKeys.add(keySet);
                }
            }
        }
        return validKeys;
    }
    //----------------------------------------------------------------------------------------------------------------//
}
