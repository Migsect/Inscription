package net.samongi.Inscription.Experience.ExperienceSource;

import net.samongi.Inscription.Experience.ExperienceReward;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.SamongiLib.Chunks.ChunkUtil;
import net.samongi.SamongiLib.Items.ItemUtil;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChunkExploreExperienceSource implements ExperienceSource {

    // ---------------------------------------------------------------------------------------------------------------//
    public static final String CONFIG_KEY = "chunk-explore";

    private static final String CONFIG_EXPLORE_RADIUS_PATH = "experience-sources.chunk-explore.explore-radius";
    private static final String CONFIG_MAX_INHABITED_TIME_PATH = "experience-sources.chunk-explore.max-inhabited-time";
    private static final String CONFIG_MIN_INHABITED_REDUCTION_PATH = "experience-sources.chunk-explore.max-inhabited-reduction";

    // ---------------------------------------------------------------------------------------------------------------//
    private Map<Biome, ExperienceReward> m_experiencePerExplore = new HashMap<>();
    private int m_exploreRadius = 0;
    private long m_maxInhabitedTime = 72000; // One Hour
    private double m_maxInhabitedReduction = 0.9;

    private ExperienceReward m_defaultReward = null;

    // ---------------------------------------------------------------------------------------------------------------//
    public ChunkExploreExperienceSource() {
        loadConfig();
    }

    private void loadConfig() {
        m_exploreRadius = Inscription.getInstance().getConfig().getInt(CONFIG_EXPLORE_RADIUS_PATH, m_exploreRadius);
        m_maxInhabitedTime = Inscription.getInstance().getConfig().getLong(CONFIG_MAX_INHABITED_TIME_PATH, m_maxInhabitedTime);
        m_maxInhabitedReduction = Inscription.getInstance().getConfig().getDouble(CONFIG_MIN_INHABITED_REDUCTION_PATH, m_maxInhabitedReduction);
    }

    // ---------------------------------------------------------------------------------------------------------------//
    public void setExpPerExplore(Biome biome, ExperienceReward reward) {
        m_experiencePerExplore.put(biome, reward);
    }
    public ExperienceReward getExpPerExplore(Biome biome) {
        return this.m_experiencePerExplore.getOrDefault(biome, m_defaultReward);
    }

    // ---------------------------------------------------------------------------------------------------------------//
    @EventHandler void onChunkEnter(PlayerMoveEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Location fromLocation = event.getFrom();
        Location toLocation = event.getTo();

        // We want to check if the movement is to a new chunk.
        Chunk fromChunk = fromLocation.getChunk();
        Chunk toChunk = toLocation.getChunk();
        if (fromChunk.equals(toChunk)) {
            return;
        }
        Inscription.logger.finest("[ChunkEnter] " + toChunk.getX() + " " + toChunk.getZ() + " " + toChunk.getWorld().getName());

        Player player = event.getPlayer();
        PlayerData playerData = Inscription.getInstance().getPlayerManager().getData(player);

        World world = toChunk.getWorld();
        Set<Chunk> explorableChunks = new HashSet<>();

        int negativeRadius = -m_exploreRadius;
        int positiveRadius = m_exploreRadius;
        for (int indexX = negativeRadius; indexX < positiveRadius; indexX++) {
            for (int indexZ = negativeRadius; indexZ < positiveRadius; indexZ++) {
                int chunkX = indexX + toChunk.getX();
                int chunkZ = indexZ + toChunk.getZ();

                Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                if (playerData.isChunkExplored(chunk)) {
                    continue;
                }
                explorableChunks.add(chunk);
            }
        }
        for (Chunk chunk : explorableChunks) {
            playerData.addExploredChunk(chunk);
            long inhabitedTime = chunk.getInhabitedTime();
            double inhabitedReduction = (Math.min(inhabitedTime, m_maxInhabitedTime) / (double)m_maxInhabitedTime) * m_maxInhabitedReduction;

            Map<Biome, Double> biomeRatios = ChunkUtil.calculateChunkBiomeRatio(chunk);
            for (Biome biome : biomeRatios.keySet()) {
                double ratio = biomeRatios.get(biome);
                Inscription.logger.finest("[ExploreEXP] ratio " + ratio + " inhabitedReduction " + inhabitedReduction);

                ExperienceReward reward = getExpPerExplore(biome);
                reward.reward(player, ratio * (1 - inhabitedReduction));
            }
        }

    }

    // ---------------------------------------------------------------------------------------------------------------//
    @Override public boolean parseRewards(ConfigurationSection section) {
        if (section == null) {
            return false;
        }

        Inscription.logger.fine("Parsing chunk explore experience rewards");

        Set<String> biomeKeys = section.getKeys(false);
        for (String key : biomeKeys) {
            if (!section.isConfigurationSection(key)) {
                Inscription.logger.warning(String.format("'%s is not a configuration section'", key));
                continue;
            }

            Biome biome = null;
            try {
                biome = Biome.valueOf(key);
            }
            catch (IllegalArgumentException exception) {
            }
            if (biome == null && !key.equals("DEFAULT")){
                Inscription.logger.warning("  " + key + " is not a valid biome type.");
                continue;
            }

            ConfigurationSection experienceRewardSection = section.getConfigurationSection(key);
            if (experienceRewardSection == null) {
                Inscription.logger.warning("  " + key + "'s configuration section is null.");
                continue;
            }

            ExperienceReward reward = ExperienceReward.parse(experienceRewardSection);
            if (biome != null) {
                setExpPerExplore(biome, reward);
            } else {
                m_defaultReward = reward;
            }

            Inscription.logger.fine("  Parsed: " + key + " registered: " + key);
        }

        return true;
    }
    // ---------------------------------------------------------------------------------------------------------------//
}
