package net.samongi.Inscription;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import net.samongi.Inscription.Commands.CommandExperience;
import net.samongi.Inscription.Commands.CommandGenerate;
import net.samongi.Inscription.Commands.CommandHelp;
import net.samongi.Inscription.Commands.CommandInventory;
import net.samongi.Inscription.Commands.CommandReload;
import net.samongi.Inscription.Experience.ExperienceManager;
import net.samongi.Inscription.Glyphs.Attributes.AttributeManager;
import net.samongi.Inscription.Glyphs.Attributes.Types.*;
import net.samongi.Inscription.Glyphs.Generator.GeneratorManager;
import net.samongi.Inscription.Listeners.BlockListener;
import net.samongi.Inscription.Listeners.EntityListener;
import net.samongi.Inscription.Listeners.PlayerListener;
import net.samongi.Inscription.Loot.LootManager;
import net.samongi.Inscription.Player.PlayerManager;
import net.samongi.Inscription.TypeClasses.EntityClass;
import net.samongi.Inscription.TypeClasses.MaterialClass;
import net.samongi.Inscription.TypeClasses.TypeClassManager;
import net.samongi.SamongiLib.CommandHandling.CommandHandler;
import net.samongi.SamongiLib.Configuration.ConfigFile;
import net.samongi.SamongiLib.Logger.BetterLogger;

import net.samongi.SamongiLib.Recipes.RecipeGraph;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Inscription extends JavaPlugin {

    //----------------------------------------------------------------------------------------------------------------//

    private static final String s_attributeDirectory = "attributes";
    private static final String s_experienceDirectory = "experience";
    private static final String s_generatorsDirectory = "generators";
    private static final String s_lootDirectory = "loot";
    private static final String s_playerDataDirectory = "player-data";
    private static final String s_typeClassDirectory = "type-classes";

    @Deprecated private static final String experience_config_file = "experience.yml";
    @Deprecated private static final String drops_config_file = "drops.yml";
    private static final String trackerDataFile = "tracker.dat";

    private static Inscription instance;
    public static Inscription getInstance() {
        return Inscription.instance;
    }

    public static BetterLogger logger;

    //----------------------------------------------------------------------------------------------------------------//
    private CommandHandler m_commandHandler;

    private TypeClassManager m_typeClassManager = null;
    private ExperienceManager m_experienceManager = null;
    private AttributeManager m_attributeManager = null;
    private LootManager m_lootManager = null;
    private PlayerManager m_playerManager = null;
    private GeneratorManager m_generatorManager = null;

    //----------------------------------------------------------------------------------------------------------------//
    public Inscription() {
        Inscription.instance = this;
        Inscription.logger = new BetterLogger(this);
    }
    //----------------------------------------------------------------------------------------------------------------//
    public void setupLogger() {
        Level logLevel = Level.parse(this.getConfig().getString("loggingLevel", Level.INFO.toString()).toUpperCase());
        logger.setLevel(logLevel);
        logger.info("Logger set to: " + logger.getLevel().toString());
    }

    //----------------------------------------------------------------------------------------------------------------//
    private void setupAttributeManager() {
        this.m_attributeManager = new AttributeManager();
        this.createAttributeConstructor();
        this.m_attributeManager.parse(new File(this.getDataFolder(), s_attributeDirectory));
    }

    private void setupExperienceManager() {
        m_experienceManager = new ExperienceManager();
        // ConfigFile experience_config = new ConfigFile();
        m_experienceManager.parse(new File(this.getDataFolder(), s_experienceDirectory));
        m_experienceManager.loadTracker(new File(this.getDataFolder(), trackerDataFile));
        m_experienceManager.configureTracker(this.getConfig());

        getServer().getPluginManager().registerEvents(m_experienceManager, this);
    }

    private void setupGeneratorManager() {
        if (m_typeClassManager == null || m_attributeManager == null) {
            logger.severe("LootManager was setup out of order!");
        }
        this.m_generatorManager = new GeneratorManager();
        this.m_generatorManager.parse(new File(this.getDataFolder(), Inscription.s_generatorsDirectory));
    }

    private void setupLootManager() {
        if (m_typeClassManager == null || m_attributeManager == null) {
            logger.severe("LootManager was setup out of order!");
        }
        m_lootManager = new LootManager();
        // m_lootManager.parseGenerators(new File(getDataFolder(), Inscription.s_generatorsDirectoryirectory));
        m_lootManager.parse(new File(getDataFolder(), s_lootDirectory));

        boolean dropConsumables = getConfig().getBoolean("drop-consumables", false);
        m_lootManager.setDropConsumables(dropConsumables);

        getServer().getPluginManager().registerEvents(m_lootManager, this);
    }

    private void setupPlayerManager() {
        File playerDataLocation = new File(this.getDataFolder(), s_playerDataDirectory);
        this.m_playerManager = new PlayerManager(playerDataLocation);

        for (Player player : Bukkit.getOnlinePlayers()) {
            this.getPlayerManager().loadPlayer(player);
        }

        getServer().getPluginManager().registerEvents(m_playerManager, this);
    }

    public void setupTypeClassManager() {
        this.m_typeClassManager = new TypeClassManager();
        this.m_typeClassManager.registerEntityClass(EntityClass.getGlobal("GLOBAL"));
        this.m_typeClassManager.registerEntityClass(EntityClass.getGlobalLiving("GLOBAL_LIVING"));
        this.m_typeClassManager.registerMaterialClass(MaterialClass.getGlobal("GLOBAL"));
        this.m_typeClassManager.parse(new File(this.getDataFolder(), s_typeClassDirectory));
    }

    //----------------------------------------------------------------------------------------------------------------//
    @Override public void onEnable() {
        /* Configuration handling */
        File config_file = new File(this.getDataFolder(), "config.yml");
        if (!config_file.exists()) {
            logger.info("Found no config file, copying over defaults...");
            this.getConfig().options().copyDefaults(true);
            this.saveConfig();
        }

        setupLogger();

        setupTypeClassManager();
        setupExperienceManager();
        setupAttributeManager();
        setupGeneratorManager();
        setupLootManager();  // Should always occur after attribute and type class manager
        setupPlayerManager();

        this.createListeners();
        this.createCommands();

        // Loading all the player profiles again
    }

    @Override public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers())
            this.getPlayerManager().unloadPlayer(p);
        this.getExperienceManager().saveTracker(new File(this.getDataFolder(), "tracker.dat"));
    }

    //----------------------------------------------------------------------------------------------------------------//

    private void createCommands() {
        this.m_commandHandler = new CommandHandler(this);
        this.m_commandHandler.registerCommand(new CommandHelp("inscription", this.m_commandHandler));
        this.m_commandHandler.registerCommand(new CommandExperience("inscription experience"));
        this.m_commandHandler.registerCommand(new CommandInventory("inscription inventory"));
        this.m_commandHandler.registerCommand(new CommandReload("inscription reload"));
        this.m_commandHandler.registerCommand(new CommandGenerate("inscription generate"));
    }
    private void createListeners() {
        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(new BlockListener(), this);
        pm.registerEvents(new EntityListener(), this);
        pm.registerEvents(new PlayerListener(), this);
    }

    public void createAttributeConstructor() {
        this.m_attributeManager.registerConstructor(new DamageAttributeType.Constructor());
        this.m_attributeManager.registerConstructor(new BlockBonusAttributeType.Constructor());
        this.m_attributeManager.registerConstructor(new DurabilityAttributeType.Constructor());
        this.m_attributeManager.registerConstructor(new ChainBreakAttributeType.Constructor());
        this.m_attributeManager.registerConstructor(new ExperienceBonusAttributeType.Constructor());
        this.m_attributeManager.registerConstructor(new DamageReductionAttributeType.Constructor());
    }

    //----------------------------------------------------------------------------------------------------------------//
    public LootManager getLootManager() {
        return this.m_lootManager;
    }
    public GeneratorManager getGeneratorManager() {
        return this.m_generatorManager;
    }
    public PlayerManager getPlayerManager() {
        return this.m_playerManager;
    }
    public ExperienceManager getExperienceManager() {
        return this.m_experienceManager;
    }
    public AttributeManager getAttributeManager() {
        return this.m_attributeManager;
    }
    public TypeClassManager getTypeClassManager() {
        return this.m_typeClassManager;
    }
}
