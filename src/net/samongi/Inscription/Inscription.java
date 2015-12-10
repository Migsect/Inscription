package net.samongi.Inscription;

import java.io.File;
import java.util.logging.Logger;

import net.samongi.Inscription.Commands.CommandExperience;
import net.samongi.Inscription.Commands.CommandHelp;
import net.samongi.Inscription.Experience.ExperienceManager;
import net.samongi.Inscription.Glyphs.Attributes.AttributeManager;
import net.samongi.Inscription.Glyphs.Attributes.Types.DamageAttributeType;
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

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Inscription extends JavaPlugin
{
  private static final String player_data_directory = "player-data";
  private static final String type_class_directory = "type-classes";
  private static final String attribute_directory = "attributes";
  private static final String generators_directory = "generators";
  
  private static final String experience_config_file = "experience.yml";
  private static final String drops_config_file = "drops.yml";
  
  private static Inscription instance;
  private static boolean debug = true;
  public static Inscription getInstance(){return Inscription.instance;}
  
  private static Logger logger;
  
  public static final String debug_tag = "###";
  public static void log(String message){Inscription.logger.info(message);}
  public static void logDebug(String message){if(Inscription.debug) Inscription.logger.info("[DEBUG] " + message);}
  public static boolean debug(){return Inscription.debug;}
  
  private CommandHandler command_handler;
  
  private LootManager loot_manager;
  private PlayerManager player_manager;
  private ExperienceManager experience_manager;
  private AttributeManager attribute_manager;
  private TypeClassManager type_class_manager;
  
  public Inscription()
  {
    Inscription.instance = this;
    Inscription.logger = this.getLogger();
  }
  
  @Override
  public void onEnable()
  {
    // Configuration handling
    File config_file = new File(this.getDataFolder(), "config.yml");
    if(!config_file.exists())
    {
      Inscription.log("Found no config file, copying over defaults...");
      this.getConfig().options().copyDefaults(true);
      this.saveConfig();
    }
    // Setting up debug boolean
    Inscription.debug = this.getConfig().getBoolean("debug", true);
    Inscription.log("Debug set to: " + debug);
    
    // Creating the type class manager
    this.type_class_manager = new TypeClassManager();
    this.type_class_manager.registerEntityClass(EntityClass.getGlobal("GLOBAL"));
    this.type_class_manager.registerEntityClass(EntityClass.getGlobalLiving("GLOVAL_LIVING"));
    this.type_class_manager.registerMaterialClass(MaterialClass.getGlobal("GLOBAL"));
    this.type_class_manager.parse(new File(this.getDataFolder(), type_class_directory));

    // Creating the Attribute manager
    this.attribute_manager = new AttributeManager();
    this.attribute_manager.registerConstructor(new DamageAttributeType.Constructor());
    
    this.attribute_manager.parse(new File(this.getDataFolder(), attribute_directory));

    // Creating the experience handler
    this.experience_manager = new ExperienceManager();
    ConfigFile experience_config = new ConfigFile(new File(this.getDataFolder(), experience_config_file));
    this.experience_manager.parse(experience_config);

    // Creating the loot manager
    //  CAN ONLY BE MADE AFTER ATTRIBUTE MANAGER AND TYPE_CLASS
    this.loot_manager = new LootManager();
    this.loot_manager.parseGenerators(new File(this.getDataFolder(), generators_directory));
    this.loot_manager.parseDrops(new File(this.getDataFolder(), drops_config_file));
    
    // Creating the player manager
    File player_data_location = new File(this.getDataFolder(), player_data_directory);
    this.player_manager = new PlayerManager(player_data_location);
    for(Player p : Bukkit.getOnlinePlayers()) this.getPlayerManager().loadPlayer(p);
    
    
    /*
    GlyphGenerator tmp_gen = new GlyphGenerator("TEST");
    tmp_gen.setMaxLevel(100);
    tmp_gen.setMinLevel(1);
    tmp_gen.addElement(GlyphElement.AIR, 1);
    tmp_gen.addElement(GlyphElement.FIRE, 1);
    tmp_gen.addElement(GlyphElement.WATER, 1);
    tmp_gen.addElement(GlyphElement.EARTH, 1);
    tmp_gen.addRarity(GlyphRarity.COMMON, 100);
    tmp_gen.addRarity(GlyphRarity.MAGICAL, 60);
    tmp_gen.addRarity(GlyphRarity.RARE, 25);
    tmp_gen.addRarity(GlyphRarity.MYTHIC, 10);
    tmp_gen.addRarity(GlyphRarity.LEGENDARY, 5);
    tmp_gen.addAttributeCount(1, 1);
    tmp_gen.addAttributeType(new DamageAttributeType("DAMAGE", "Savage", 1, 10, 0.5), 10);
    tmp_gen.addAttributeType(new DamageAttributeType("SUPER_DAMAGE", "Deadly", 1, 100, 0.5), 10);
    tmp_gen.addAttributeType(new DamageAttributeType("HYPER_DAMAGE", "Destructive", 1, 1000, 0.5), 10);
    this.loot_manager.registerGeneratorToMaterial(Material.STONE, tmp_gen, 0.5);
    */
    
    // Creating all the listenrs
    this.createListeners();
    // Creating all the commands
    this.createCommands();
    
    
    // Loading all the player profiles again
  }
  
  @Override
  public void onDisable()
  {
    for(Player p : Bukkit.getOnlinePlayers()) this.getPlayerManager().unloadPlayer(p);
  }
  
  private void createCommands()
  {
    this.command_handler = new CommandHandler(this);
    this.command_handler.registerCommand(new CommandHelp("inscription", this.command_handler));
    this.command_handler.registerCommand(new CommandExperience("inscription experience"));
  }
  
  private void createListeners()
  {
    PluginManager pm = this.getServer().getPluginManager();
    pm.registerEvents(new BlockListener(), this);
    pm.registerEvents(new EntityListener(), this);
    pm.registerEvents(new PlayerListener(), this);
  }
  
  public LootManager getLootHandler(){return this.loot_manager;}
  public PlayerManager getPlayerManager(){return this.player_manager;}
  public ExperienceManager getExperienceManager(){return this.experience_manager;}
  public AttributeManager getAttributeManager(){return this.attribute_manager;}
  public TypeClassManager getTypeClassManager(){return this.type_class_manager;}
}
