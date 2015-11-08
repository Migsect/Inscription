package net.samongi.Inscription.Glyphs;

import org.bukkit.ChatColor;

public enum GlyphRarity
{
  
  COMMON ("Common", ChatColor.WHITE, 0),
  MAGICAL ("Magical", ChatColor.GREEN, 1),
  RARE ("Rare", ChatColor.BLUE, 2),
  MYTHIC ("Mythic", ChatColor.GOLD, 3),
  LEGENDARY ("Legendary", ChatColor.RED, 4);
  
  private final String display;
  private final ChatColor color;
  private final int rank;
  
  GlyphRarity(String display, ChatColor color, int rank)
  {
    this.display = display;
    this.color = color;
    this.rank = rank;
  }
  
  public String getDisplay(){return this.display;}
  public ChatColor getColor(){return this.color;}
  public int getRank(){return this.rank;}
  
}
