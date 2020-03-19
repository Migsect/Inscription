package net.samongi.Inscription;

import java.io.File;
import java.util.logging.Level;

import net.samongi.Inscription.Commands.CommandExperience;
import net.samongi.Inscription.Commands.CommandGenerate;
import net.samongi.Inscription.Commands.CommandHelp;
import net.samongi.Inscription.Commands.CommandInventory;
import net.samongi.Inscription.Commands.CommandReload;
import net.samongi.Inscription.Conditions.Types.*;
import net.samongi.Inscription.Experience.BlockTracker;
import net.samongi.Inscription.Experience.ExperienceManager;
import net.samongi.Inscription.Attributes.AttributeManager;
import net.samongi.Inscription.Attributes.Types.*;
import net.samongi.Inscription.Experience.ExperienceSource.*;
import net.samongi.Inscription.Glyphs.Types.GlyphTypesManager;
import net.samongi.Inscription.Loot.Generator.GeneratorManager;
import net.samongi.Inscription.Listeners.PlayerListener;
import net.samongi.Inscription.Loot.LootManager;
import net.samongi.Inscription.Player.PlayerManager;
import net.samongi.Inscription.Recipe.RecipeManager;
import net.samongi.Inscription.TypeClass.TypeClasses.*;
import net.samongi.Inscription.TypeClass.TypeClassManager;
import net.samongi.Inscription.Waypoints.WaypointManager;
import net.samongi.SamongiLib.CommandHandling.CommandHandler;
import net.samongi.SamongiLib.Logger.BetterLogger;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Inscription extends JavaPlugin {

    //----------------------------------------------------------------------------------------------------------------//

    private static final String ATTRIBUTE_DIRECTORY = "attributes";
    private static final String EXPERIENCE_DIRECTORY = "experience";
    private static final String GENERATORS_DIRECTORY = "generators";
    private static final String LOOT_DIRECTORY = "loot";
    private static final String PLAYER_DATA_DIRECTORY = "player-data";
    private static final String TYPE_CLASS_DIRECTORY = "type-classes";
    private static final String GLYPH_TYPES_DIRECTORY = "glyph-types";

    private static final String TRACKER_DATA_FOLDER = "block-tracker";

    //----------------------------------------------------------------------------------------------------------------//
    private static Inscription instance;
    public static Inscription getInstance() {
        return Inscription.instance;
    }

    public static BetterLogger logger;

    private static int s_maxLevel = 100;
    public static int getMaxLevel() {
        return s_maxLevel;
    }

    //----------------------------------------------------------------------------------------------------------------//
    private CommandHandler m_commandHandler;
    private BlockTracker m_blockTracker;

    private TypeClassManager m_typeClassManager = null;
    private ExperienceManager m_experienceManager = null;
    private AttributeManager m_attributeManager = null;
    private LootManager m_lootManager = null;
    private PlayerManager m_playerManager = null;
    private GeneratorManager m_generatorManager = null;
    private GlyphTypesManager m_glyphTypesManager = null;
    private RecipeManager m_recipeManager = null;
    private WaypointManager m_waypointManager = null;

    //----------------------------------------------------------------------------------------------------------------//
    public Inscription() {
        Inscription.instance = this;
        Inscription.logger = new BetterLogger(this);
    }
    //----------------------------------------------------------------------------------------------------------------//
    private void setupAttributeManager() {
        m_attributeManager = new AttributeManager();
        createAttributeConstructor();
        createConditionParsers();
        m_attributeManager.parse(new File(this.getDataFolder(), ATTRIBUTE_DIRECTORY));
    }

    private void setupExperienceManager() {
        m_experienceManager = new ExperienceManager();
        m_experienceManager.registerExperienceSource(DamageDoneExperienceSource.CONFIG_KEY, new DamageDoneExperienceSource());
        m_experienceManager.registerExperienceSource(KillExperienceSource.CONFIG_KEY, new KillExperienceSource());
        m_experienceManager.registerExperienceSource(MaterialCraftExperienceSource.CONFIG_KEY, new MaterialCraftExperienceSource());
        m_experienceManager.registerExperienceSource(MaterialIngredientExperienceSource.CONFIG_KEY, new MaterialIngredientExperienceSource());
        m_experienceManager.registerExperienceSource(MaterialPlaceExperienceSource.CONFIG_KEY, new MaterialPlaceExperienceSource());
        m_experienceManager.registerExperienceSource(MaterialBreakExperienceSource.CONFIG_KEY, new MaterialBreakExperienceSource());
        m_experienceManager.registerExperienceSource(ChunkExploreExperienceSource.CONFIG_KEY, new ChunkExploreExperienceSource());
        m_experienceManager.parse(new File(this.getDataFolder(), EXPERIENCE_DIRECTORY));

        getServer().getPluginManager().registerEvents(m_experienceManager, this);
    }

    private void setupGeneratorManager() {
        if (m_typeClassManager == null || m_attributeManager == null) {
            logger.severe("LootManager was setup out of order!");
        }
        this.m_generatorManager = new GeneratorManager();
        this.m_generatorManager.parse(new File(this.getDataFolder(), Inscription.GENERATORS_DIRECTORY));
    }

    private void setupLootManager() {
        if (m_typeClassManager == null || m_attributeManager == null) {
            logger.severe("LootManager was setup out of order!");
        }
        m_lootManager = new LootManager();
        m_lootManager.parse(new File(getDataFolder(), LOOT_DIRECTORY));

        boolean dropConsumables = getConfig().getBoolean("drop-consumables", false);
        m_lootManager.setDropConsumables(dropConsumables);

        getServer().getPluginManager().registerEvents(m_lootManager, this);
    }

    private void setupPlayerManager() {
        File playerDataLocation = new File(this.getDataFolder(), PLAYER_DATA_DIRECTORY);
        this.m_playerManager = new PlayerManager(playerDataLocation);

        for (Player player : Bukkit.getOnlinePlayers()) {
            this.getPlayerManager().loadPlayer(player);
        }

        getServer().getPluginManager().registerEvents(m_playerManager, this);
    }

    public void setupTypeClassManager() {
        m_typeClassManager = new TypeClassManager();
        m_typeClassManager.registerTypeClassHandler(BiomeClass.handler);
        m_typeClassManager.registerTypeClassHandler(BlockClass.handler);
        m_typeClassManager.registerTypeClassHandler(DamageClass.handler);
        m_typeClassManager.registerTypeClassHandler(EntityClass.handler);
        m_typeClassManager.registerTypeClassHandler(MaterialClass.handler);
        m_typeClassManager.parse(new File(getDataFolder(), TYPE_CLASS_DIRECTORY));
        m_typeClassManager.updateAllInvertedClasses();
    }

    public void setupGlyphTypesManager() {
        m_glyphTypesManager = new GlyphTypesManager();
        m_glyphTypesManager.parse(new File(getDataFolder(), GLYPH_TYPES_DIRECTORY));
    }

    private void setupRecipeManager() {
        m_recipeManager = new RecipeManager();
        m_recipeManager.registerRecipes();
        getServer().getPluginManager().registerEvents(m_recipeManager, this);
    }

    private void setupWaypointManager() {
        m_waypointManager = new WaypointManager();
        getServer().getPluginManager().registerEvents(m_waypointManager, this);
    }
    //----------------------------------------------------------------------------------------------------------------//
    @Override public void onEnable() {
        /* Configuration handling */
        File configFile = new File(this.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            logger.info("Found no config file, copying over defaults...");
            this.getConfig().options().copyDefaults(true);
            this.saveConfig();
        }

        readConfig();
        setupBlockTracker();

        setupGlyphTypesManager();
        setupTypeClassManager();
        setupExperienceManager();
        setupAttributeManager();
        setupGeneratorManager();
        setupLootManager();  // Should always occur after attribute and type class manager
        setupPlayerManager();
        setupRecipeManager();
        setupWaypointManager();

        createListeners();
        createCommands();
    }

    @Override public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            getPlayerManager().unloadPlayer(p);
        }
        tearDownBlockTracker();
    }

    //----------------------------------------------------------------------------------------------------------------//

    private void readConfig() {

        Level logLevel = Level.parse(this.getConfig().getString("loggingLevel", Level.INFO.toString()).toUpperCase());
        logger.setLevel(logLevel);
        logger.info("Logger set to: " + logger.getLevel().toString());

        s_maxLevel = getConfig().getInt("max-glyph-level", 100);
    }

    private void setupBlockTracker() {
        m_blockTracker = new BlockTracker(new File(this.getDataFolder(), TRACKER_DATA_FOLDER));
        m_blockTracker.configureTracker(this.getConfig());
        for (World world : Bukkit.getWorlds()) {
            m_blockTracker.loadWorldChunks(world);
        }

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(m_blockTracker, this);
    }
    private void tearDownBlockTracker() {
        m_blockTracker.saveAllChunkRegions();
    }

    private void createCommands() {
        m_commandHandler = new CommandHandler(this);
        m_commandHandler.registerCommand(new CommandHelp("inscription", m_commandHandler));
        m_commandHandler.registerCommand(new CommandExperience("inscription experience"));
        m_commandHandler.registerCommand(new CommandInventory("inscription inventory"));
        m_commandHandler.registerCommand(new CommandReload("inscription reload"));
        m_commandHandler.registerCommand(new CommandGenerate("inscription generate"));
    }

    private void createListeners() {
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new PlayerListener(), this);
    }

    private void createAttributeConstructor() {
        m_attributeManager.registerConstructor(new DamageDoneAttributeType.Factory());
        m_attributeManager.registerConstructor(new BlockBonusAttributeType.Factory());
        m_attributeManager.registerConstructor(new DurabilityAttributeType.Factory());
        m_attributeManager.registerConstructor(new ChainBreakAttributeType.Factory());
        m_attributeManager.registerConstructor(new ExperienceBonusAttributeType.Factory());
        m_attributeManager.registerConstructor(new DamageReductionAttributeType.Factory());
        m_attributeManager.registerConstructor(new LifeStealAttributeType.Factory());
        m_attributeManager.registerConstructor(new WaypointAttributeType.Factory());
    }

    private void createConditionParsers() {
        m_attributeManager.registerConditionParser("from-entity", FromEntityCondition::new);
        m_attributeManager.registerConditionParser("to-damage_taken", ToDamageTakenCondition::new);
        m_attributeManager.registerConditionParser("to-biome", ToBiomeCondition::new);
        m_attributeManager.registerConditionParser("for-block", ForBlockCondition::new);
        m_attributeManager.registerConditionParser("player-while-level", PlayerWhileLevelCondition::new);
        m_attributeManager.registerConditionParser("player-while-life", PlayerWhileLifeCondition::new);
        m_attributeManager.registerConditionParser("player-using-material", PlayerUsingMaterialCondition::new);
        m_attributeManager.registerConditionParser("player-while-wearing-material", PlayerWhileWearingMaterialCondition::new);
        m_attributeManager.registerConditionParser("player-in-biome", PlayerInBiomeCondition::new);
        m_attributeManager.registerConditionParser("target-entity", TargetEntityCondition::new);
        m_attributeManager.registerConditionParser("target-in-biome", TargetInBiomeCondition::new);
        m_attributeManager.registerConditionParser("target-using-material", TargetUsingMaterialCondition::new);
        m_attributeManager.registerConditionParser("from-biome", FromBiomeCondition::new);
        m_attributeManager.registerConditionParser("to-biome", ToBiomeCondition::new);
    }

    //----------------------------------------------------------------------------------------------------------------//
    public BlockTracker getBlockTracker() {
        return m_blockTracker;
    }
    public LootManager getLootManager() {
        return m_lootManager;
    }
    public GeneratorManager getGeneratorManager() {
        return m_generatorManager;
    }
    public PlayerManager getPlayerManager() {
        return m_playerManager;
    }
    public ExperienceManager getExperienceManager() {
        return m_experienceManager;
    }
    public AttributeManager getAttributeManager() {
        return m_attributeManager;
    }
    public TypeClassManager getTypeClassManager() {
        return m_typeClassManager;
    }
    public GlyphTypesManager getGlyphTypesManager() {
        return m_glyphTypesManager;
    }
    public RecipeManager getRecipeManager() {
        return m_recipeManager;
    }
    public WaypointManager getWaypointManager() {
        return m_waypointManager;
    }
}
