package net.samongi.Inscription.Experience;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class ExperienceMap {

    // ---------------------------------------------------------------------------------------------------------------//
    public static @Nonnull ExperienceMap parse(@Nullable ConfigurationSection section) {
        if (section == null) {
            return new ExperienceMap();
        }

        return new ExperienceMap(section);
    }

    // ---------------------------------------------------------------------------------------------------------------//
    private final Map<String, Integer> m_experiences;

    // ---------------------------------------------------------------------------------------------------------------//
    public ExperienceMap() {
        m_experiences = new HashMap<>();
    }
    public ExperienceMap(@Nonnull Map<String, Integer> initializer) {
        m_experiences = new HashMap<>(initializer);
    }
    public ExperienceMap(@Nonnull ConfigurationSection section) {
        this();
        for (String key : section.getKeys(false)) {
            set(key, section.getInt(key, 0));
        }
    }

    public ExperienceMap clone() {
        return new ExperienceMap(m_experiences);

    }
    // ---------------------------------------------------------------------------------------------------------------//
    public boolean has(@Nonnull String experienceType) {
        return m_experiences.containsKey(experienceType);
    }

    public int get(@Nonnull String experienceType) {
        return m_experiences.getOrDefault(experienceType, 0);
    }

    public Map<String, Integer> get() {
        return new HashMap<>(m_experiences);
    }

    public int getTotal() {
        int total = 0;
        for (Integer value : m_experiences.values()) {
            total += value;
        }
        return total;
    }

    public void set(@Nonnull String experienceType, int amount) {
        m_experiences.put(experienceType, amount);
    }

    // ---------------------------------------------------------------------------------------------------------------//

    public void addInplace(int amount) {
        for (String experienceType : m_experiences.keySet()) {
            addInplace(experienceType, amount);
        }
    }
    public void addInplace(@Nonnull String experienceType, int amount) {
        int current = get(experienceType);
        set(experienceType, current + amount);
    }
    public void addInplace(@Nonnull ExperienceMap other) {
        for (String experienceType : other.m_experiences.keySet()) {
            addInplace(experienceType, other.get(experienceType));
        }
    }
    public @Nonnull ExperienceMap add(int amount) {
        ExperienceMap experienceMap = clone();
        experienceMap.add(amount);
        return experienceMap;
    }
    public @Nonnull ExperienceMap add(@Nonnull String experienceType, int amount) {
        ExperienceMap experienceMap = clone();
        experienceMap.addInplace(experienceType, amount);
        return experienceMap;
    }
    public @Nonnull ExperienceMap add(@Nonnull ExperienceMap other) {
        ExperienceMap experienceMap = clone();
        experienceMap.addInplace(other);
        return experienceMap;
    }
    public @Nonnull ExperienceMap subtract(@Nonnull String experienceType, int amount) {
        return add(experienceType, -amount);
    }
    public @Nonnull ExperienceMap subtract(@Nonnull ExperienceMap other) {
        return add(other.negate());
    }
    public void incrementInplace() {
        addInplace(1);
    }
    public @Nonnull ExperienceMap increment() {
        return add(1);
    }
    public void decrementInplace() {
        addInplace(-1);
    }
    public @Nonnull ExperienceMap decrement() {
        return add(-1);
    }

    // ---------------------------------------------------------------------------------------------------------------//
    public void multiplyInplace(@Nonnull String experienceType, double scalar) {
        int current = get(experienceType);
        set(experienceType, (int) Math.floor(current * scalar));
    }
    public void multiplyInplace(double scalar) {
        for (String experienceType : m_experiences.keySet()) {
            multiplyInplace(experienceType, scalar);
        }
    }
    public @Nonnull ExperienceMap multiply(@Nonnull String experienceType, double scalar) {
        ExperienceMap experienceMap = clone();
        experienceMap.multiplyInplace(experienceType, scalar);
        return experienceMap;
    }
    public @Nonnull ExperienceMap multiply(double scalar) {
        ExperienceMap experienceMap = clone();
        experienceMap.multiplyInplace(scalar);
        return experienceMap;
    }
    public @Nonnull ExperienceMap negate() {
        return multiply(-1);
    }

    /**
     * Does not handle remainder, all remainders are simply rounded down.
     */
    public @Nonnull ExperienceMap divide(double scalar) {
        return multiply(1 / scalar);
    }
    public @Nonnull ExperienceMap remainder(int divisor) {
        ExperienceMap experienceMap = new ExperienceMap();
        for (String experienceType : experienceTypes()) {
            experienceMap.set(experienceType, get(experienceType) % divisor);
        }
        return experienceMap;
    }
    // ---------------------------------------------------------------------------------------------------------------//
    public @Nonnull void minInplace(@Nonnull String experienceType, int amount) {
        int current = get(experienceType);
        set(experienceType, Math.min(current, amount));
    }

    public @Nonnull void minInplace(int amount) {
        for (String experienceType : m_experiences.keySet()) {
            minInplace(experienceType, amount);
        }
    }

    public @Nonnull ExperienceMap min(int amount) {
        ExperienceMap experienceMap = clone();
        experienceMap.minInplace(amount);
        return experienceMap;
    }

    public @Nonnull void maxInplace(@Nonnull String experienceType, int amount) {
        int current = get(experienceType);
        set(experienceType, Math.max(current, amount));
    }

    public @Nonnull void maxInplace(int amount) {
        for (String experienceType : m_experiences.keySet()) {
            maxInplace(experienceType, amount);
        }
    }

    public @Nonnull ExperienceMap max(int amount) {
        ExperienceMap experienceMap = clone();
        experienceMap.maxInplace(amount);
        return experienceMap;
    }

    // ---------------------------------------------------------------------------------------------------------------//
    public @Nonnull ExperienceMap ones() {
        ExperienceMap experienceMap = new ExperienceMap();
        for (String experienceType : experienceTypes()) {
            experienceMap.set(experienceType, 1);
        }
        return experienceMap;
    }

    public @Nonnull ExperienceMap zeros() {
        return ones().multiply(0);
    }

    // ---------------------------------------------------------------------------------------------------------------//
    /**
     * Retrieves a random experience from the map and returns it in its own map.
     *
     * @return
     */
    public @Nonnull ExperienceMap randomSingle() {
        ExperienceMap experienceMap = new ExperienceMap();
        Random rand = new Random();
        List<String> types = new ArrayList<>(experienceTypes());
        String experienceType = types.get(rand.nextInt(types.size()));
        experienceMap.set(experienceType, get(experienceType));
        return experienceMap;
    }

    public void distributeExperience(int amount) {
        addInplace(amount / size());

        int remainder = amount % size();

        List<String> experienceTypes = new ArrayList<>(experienceTypes());
        Collections.shuffle(experienceTypes);
        for (String experienceType : experienceTypes) {
            addInplace(experienceType, 1);
            remainder -= 1;
            if (remainder == 0) {
                break;
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------//
    public @Nonnull Set<String> experienceTypes() {
        return m_experiences.keySet();
    }

    public int size() {
        return m_experiences.size();
    }

    public void clear() {
        m_experiences.clear();
    }

    /**
     * Cleans the experience map of any 0 values.
     */
    public void clean() {
        for (String experienceType : experienceTypes()) {
            if (get(experienceType) == 0) {
                m_experiences.remove(experienceType);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------//
    public ConfigurationSection toConfigurationSection() {
        ConfigurationSection section = new YamlConfiguration();
        for (String key : m_experiences.keySet()) {
            section.set(key, m_experiences.get(key));
        }
        return section;
    }

    // ---------------------------------------------------------------------------------------------------------------//
}
