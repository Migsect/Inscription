package net.samongi.Inscription.Loot.Generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class WeightMap<T> {

    private HashMap<T, Integer> item_weights = new HashMap<>();

    public Set<T> keySet() {
        return this.item_weights.keySet();
    }
    public void put(T item, int weight) {
        this.item_weights.put(item, weight);
    }
    public void remove(T item) {
        this.item_weights.remove(item);
    }
    public boolean containsKey(T item) {
        return this.item_weights.containsKey(item);
    }
    public int getWeight(T item) {
        return this.item_weights.get(item);
    }

    public T getRandom() {
        int weight_sum = this.getWeightSum();
        if (weight_sum <= 0)
            return null;
        List<T> items = this.randomizedList();

        Random rand = new Random();

        // getting a number from 0 to the sum - 1
        int selected = rand.nextInt(weight_sum);

        for (T i : items) {
            // Getting the weight of the current item.
            int i_weight = this.item_weights.get(i);
            if (selected <= i_weight)
                return i;
            else
                selected -= i_weight;
        }
        return null;
    }

    private List<T> randomizedList() {
        List<T> tmp_list = new ArrayList<>(item_weights.keySet());
        List<T> ret_list = new ArrayList<>();
        Random rand = new Random();
        while (!tmp_list.isEmpty()) {
            int i = rand.nextInt(tmp_list.size());
            ret_list.add(tmp_list.get(i));
            tmp_list.remove(i);
        }
        return ret_list;
    }

    private int getWeightSum() {
        int sum = 0;
        for (int i : item_weights.values())
            sum += i;
        return sum;
    }
}
