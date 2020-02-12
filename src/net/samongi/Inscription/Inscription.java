package net.samongi.Inscription;

import java.io.File;
import java.util.logging.Level;

import net.samongi.Inscription.Commands.CommandExperience;
import net.samongi.Inscription.Commands.CommandGenerate;
import net.samongi.Inscription.Commands.CommandHelp;
import net.samongi.Inscription.Commands.CommandInventory;
import net.samongi.Inscription.Commands.CommandReload;
import net.samongi.Inscription.Experience.ExperienceManager;
import net.samongi.Inscription.Glyphs.Attributes.AttributeManager;
import net.samongi.Inscription.Glyphs.Attributes.Types.*;
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

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Inscription extends JavaPlugin {

    private static final String player_data_directory = "player-data";
    private static final String type_class_directory = "type-classes";
    private static final String attribute_directory = "attributes";
    private static final String generators_directory = "generators";

    private static final String experience_config_file = "experience.yml";
    private static final String drops_config_file = "drops.yml";

    private static Inscription instance;

    public static Inscription getInstance()
    {
        return Inscription.instance;
    }

    public static BetterLogger logger;

    private CommandHandler m_commandHandler;

    private LootManager m_lootManager;
    private PlayerManager m_playerManager;
    private ExperienceManager m_experienceManager;
    private AttributeManager m_attributeManager;
    private TypeClassManager m_typeClassManager;

    public Inscription()
    {
        Inscription.instance = this;
        Inscription.logger = new BetterLogger(this);
    }

    @Override
    public void onEnable()
    {
        /* Configuration handling */
        File config_file = new File(this.getDataFolder(), "config.yml");
        if (!config_file.exists()) {
            logger.info("Found no config file, copying over defaults...");
            this.getConfig().options().copyDefaults(true);
            this.saveConfig();
        }

        /* Setting up the log level */
        Level logLevel = Level.parse(this.getConfig().getString("loggingLevel", Level.INFO.toString()).toUpperCase());
        logger.setLevel(logLevel);
        logger.info("Logger set to: " + logger.getLevel().toString());

        /* Creating the type class manager */
        this.m_typeClassManager = new TypeClassManager();
        this.m_typeClassManager.registerEntityClass(EntityClass.getGlobal("GLOBAL"));
        this.m_typeClassManager.registerEntityClass(EntityClass.getGlobalLiving("GLOBAL_LIVING"));
        this.m_typeClassManager.registerMaterialClass(MaterialClass.getGlobal("GLOBAL"));
        this.m_typeClassManager.parse(new File(this.getDataFolder(), type_class_directory));

        /* Creating the experience handler */
        this.m_experienceManager = new ExperienceManager();
        ConfigFile experience_config = new ConfigFile(new File(this.getDataFolder(), experience_config_file));
        this.m_experienceManager.parse(experience_config);
        this.m_experienceManager.loadTracker(new File(this.getDataFolder(), "tracker.dat"));
        this.m_experienceManager.configureTracker(this.getConfig());

        /* Creating the Attribute manager */
        this.m_attributeManager = new AttributeManager();
        this.createAttributeConstructor();
        this.m_attributeManager.parse(new File(this.getDataFolder(), attribute_directory));

        /* Creating the loot manager */
        // CAN ONLY BE MADE AFTER ATTRIBUTE MANAGER AND TYPE_CLASS
        this.m_lootManager = new LootManager();
        this.m_lootManager.parseGenerators(new File(this.getDataFolder(), generators_directory));
        this.m_lootManager.parseDrops(new File(this.getDataFolder(), drops_config_file));

        /* Creating the player manager */
        File player_data_location = new File(this.getDataFolder(), player_data_directory);
        this.m_playerManager = new PlayerManager(player_data_location);

        for (Player p : Bukkit.getOnlinePlayers())
            this.getPlayerManager().loadPlayer(p);

        /* Creating all the listenrs */
        this.createListeners();
        /* Creating all the commands */
        this.createCommands();

        // Loading all the player profiles again
    }
    @Override
    public void onDisable()
    {
        for (Player p : Bukkit.getOnlinePlayers())
            this.getPlayerManager().unloadPlayer(p);
        this.getExperienceManager().saveTracker(new File(this.getDataFolder(), "tracker.dat"));
    }

    private void createCommands()
    {
        this.m_commandHandler = new CommandHandler(this);
        this.m_commandHandler.registerCommand(new CommandHelp("inscription", this.m_commandHandler));
        this.m_commandHandler.registerCommand(new CommandExperience("inscription experience"));
        this.m_commandHandler.registerCommand(new CommandInventory("inscription inventory"));
        this.m_commandHandler.registerCommand(new CommandReload("inscription reload"));
        this.m_commandHandler.registerCommand(new CommandGenerate("inscription generate"));
    }
    private void createListeners()
    {
        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(new BlockListener(), this);
        pm.registerEvents(new EntityListener(), this);
        pm.registerEvents(new PlayerListener(), this);
    }

    public void createAttributeConstructor()
    {
        this.m_attributeManager.registerConstructor(new DamageAttributeType.Constructor());
        this.m_attributeManager.registerConstructor(new BlockBonusAttributeType.Constructor());
        this.m_attributeManager.registerConstructor(new DurabilityAttributeType.Constructor());
        this.m_attributeManager.registerConstructor(new ChainBreakAttributeType.Constructor());
        this.m_attributeManager.registerConstructor(new ExperienceBonusAttributeType.Constructor());
    }

    public LootManager getLootHandler()
    {
        return this.m_lootManager;
    }
    public PlayerManager getPlayerManager()
    {
        return this.m_playerManager;
    }
    public ExperienceManager getExperienceManager()
    {
        return this.m_experienceManager;
    }
    public AttributeManager getAttributeManager()
    {
        return this.m_attributeManager;
    }
    public TypeClassManager getTypeClassManager()
    {
        return this.m_typeClassManager;
    }
}
