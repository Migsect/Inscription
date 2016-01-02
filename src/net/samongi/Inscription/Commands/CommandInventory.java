package net.samongi.Inscription.Commands;

import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.GlyphInventory;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.SamongiLib.CommandHandling.ArgumentType;
import net.samongi.SamongiLib.CommandHandling.BaseCommand;
import net.samongi.SamongiLib.CommandHandling.SenderType;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandInventory extends BaseCommand
{
  public CommandInventory(String command_path)
  {
    super(command_path);
    this.permission = "inscription.inventory";
    this.allowed_senders.add(SenderType.PLAYER);
    this.allowed_arguments.add(new ArgumentType[0]);
  }
  
  @Override
  public boolean run(CommandSender sender, String[] args)
  {
    Player player = (Player) sender;
    PlayerData data = Inscription.getInstance().getPlayerManager().getData(player);
    
    GlyphInventory inventory = data.getGlyphInventory();
    player.openInventory(inventory.getInventory());
    
    return true;
  }
}
