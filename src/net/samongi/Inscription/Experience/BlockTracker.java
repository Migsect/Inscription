package net.samongi.Inscription.Experience;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;

/**Keeps tabs on blocks that have bee placed.
 * 
 * @author Alex
 *
 */
public class BlockTracker implements Serializable
{
  private static final long serialVersionUID = -2972844610158182014L;
  
  private Set<Location> placed_locations = new HashSet<>();
  private Set<Material> tracked_materials = new HashSet<>();
  
  public boolean isPlaced(Location loc){return this.placed_locations.contains(loc);}
  public void addPlaced(Location loc){this.placed_locations.add(loc);}
  public void removePlaced(Location loc){this.placed_locations.remove(loc);}
  
  public boolean isTracked(Material mat){return this.tracked_materials.contains(mat);}
  public void addTracked(Material mat){this.tracked_materials.add(mat);}
  public void removeTracked(Material mat){this.tracked_materials.remove(mat);}
}
