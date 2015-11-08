package net.samongi.Inscription.Commands;

import java.util.List;

import org.bukkit.command.CommandSender;

import net.md_5.bungee.api.ChatColor;
import net.samongi.SamongiLib.CommandHandling.ArgumentType;
import net.samongi.SamongiLib.CommandHandling.BaseCommand;
import net.samongi.SamongiLib.CommandHandling.CommandHandler;
import net.samongi.SamongiLib.CommandHandling.SenderType;

public class CommandHelp extends BaseCommand
{
  private CommandHandler handler;
  public CommandHelp(String command_path, CommandHandler handler)
  {
    super(command_path);
    this.permission = "inscription.help";
    this.allowed_senders.add(SenderType.PLAYER);
    this.allowed_senders.add(SenderType.CONSOLE);
    this.allowed_arguments.add(new ArgumentType[0]);
    this.handler = handler;
  }
  
  @Override
  public boolean run(CommandSender sender, String[] args)
  {
    List<BaseCommand> commands = handler.getCommands();
    sender.sendMessage(ChatColor.GRAY + "<<< <<< <<< " + ChatColor.YELLOW + "Inscription Command Usage" + ChatColor.GRAY + " >>> >>> >>>");
    for(BaseCommand b : commands)
    {
      List<String> usage = b.getUsage();
      if(usage.size() == 0) sender.sendMessage(ChatColor.YELLOW + b.getCommandPath() + ChatColor.GRAY + " : No usage definition set.");
      for(int i = 0; i < usage.size(); i++)
      {
        if(i == 0) sender.sendMessage(ChatColor.YELLOW + b.getCommandPath() + " : " + ChatColor.GRAY  + usage.get(i));
        else sender.sendMessage(ChatColor.YELLOW + "  > " + ChatColor.GRAY  + usage.get(i));
      }
    }
    return true;
  }
}
