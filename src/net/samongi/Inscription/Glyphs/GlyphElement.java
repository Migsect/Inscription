package net.samongi.Inscription.Glyphs;

import net.md_5.bungee.api.ChatColor;

public enum GlyphElement
{
  
  FIRE ("Fire", ChatColor.RED),
  WATER ("Water", ChatColor.AQUA),
  AIR ("Air", ChatColor.LIGHT_PURPLE),
  EARTH ("Earth", ChatColor.GREEN),
  SHADOW ("Shadow", ChatColor.DARK_GRAY),
  LIGHT ("Light", ChatColor.YELLOW),
  TIME ("Time", ChatColor.BLUE),
  SPACE ("Space", ChatColor.DARK_PURPLE);
  
  private final String display;
  private final ChatColor color;
  
  GlyphElement(String display, ChatColor color)
  {
    this.display = display;
    this.color = color;
  }
  
  public String getDisplay(){return this.display;}
  public ChatColor getColor(){return this.color;}
  
}
