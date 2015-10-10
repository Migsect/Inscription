package net.samongi.Inscription.Glyphs;

import net.md_5.bungee.api.ChatColor;

public enum GlyphElement
{
  
  FIRE ("Fire", ChatColor.RED),
  WATER ("Fire", ChatColor.RED),
  AIR ("Fire", ChatColor.RED),
  EARTH ("Fire", ChatColor.RED),
  SHADOW ("Fire", ChatColor.RED),
  LIGHT ("Fire", ChatColor.RED),
  TIME ("Fire", ChatColor.RED),
  SPACE ("Fire", ChatColor.RED);
  
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
