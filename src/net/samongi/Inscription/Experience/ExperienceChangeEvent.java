package net.samongi.Inscription.Experience;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ExperienceChangeEvent extends Event implements Cancellable
{
  private static final HandlerList handlers = new HandlerList();
  
  private boolean cancelled = false;
  private final String type;
  private final int old_value;
  private int new_value;
  
  public ExperienceChangeEvent(String type, int old_value, int new_value)
  {
    this.type = type;
    this.old_value = old_value;
    this.new_value = new_value;
  }
  
  public int getOldValue(){return this.old_value;}
  public int getNewValue(){return this.new_value;}
  public void setNewValue(int new_value){this.new_value = new_value;}
  public String getType(){return this.type;}
  
  @Override
  public boolean isCancelled(){return this.cancelled;}

  @Override
  public void setCancelled(boolean arg0){this.cancelled = arg0;}
  
  @Override
  public HandlerList getHandlers()
  {
    return ExperienceChangeEvent.handlers;
  }

}
