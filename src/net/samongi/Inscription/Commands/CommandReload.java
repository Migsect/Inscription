package net.samongi.Inscription.Commands;

import net.samongi.Inscription.Inscription;
import net.samongi.SamongiLib.CommandHandling.ArgumentType;
import net.samongi.SamongiLib.CommandHandling.BaseCommand;
import net.samongi.SamongiLib.CommandHandling.SenderType;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * 
 * This command will reload the plugin's configuration files and as such will
 * prevent the need for the entire server to be reloaded.
 * 
 * @author Migsect
 *
 */
public class CommandReload extends BaseCommand
{

  public CommandReload(String command_path)
  {
    super(command_path);
    this.permission = "inscription.reload";
    this.allowed_senders.add(SenderType.PLAYER);
    this.allowed_senders.add(SenderType.CONSOLE);
    this.allowed_arguments.add(new ArgumentType[0]);
  }

  @Override
  public boolean run(CommandSender sender, String[] args)
  {
    Inscription.getInstance().onDisable();
    Inscription.getInstance().onEnable();
    sender.sendMessage(ChatColor.GOLD + "Plugin Reloaded");
    return true;
  }
}
