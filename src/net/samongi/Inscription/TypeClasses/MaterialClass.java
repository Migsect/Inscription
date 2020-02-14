package net.samongi.Inscription.TypeClasses;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.samongi.Inscription.Inscription;

import net.samongi.SamongiLib.Items.Comparators.BlockDataAgeableComparator;
import net.samongi.SamongiLib.Items.Comparators.BlockDataComparator;
import net.samongi.SamongiLib.Items.Comparators.BlockDataGroupComparator;
import net.samongi.SamongiLib.Items.Comparators.BlockDataMaterialComparator;
import net.samongi.SamongiLib.Items.MaskedBlockData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.libs.jline.internal.Nullable;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

/**
 * Referred to by attributes and othe rplugin systems
 * to group together different material types.
 */
public class MaterialClass implements Serializable {

    private static final long serialVersionUID = -4115095882218966162L;

    /**
     * Returns a material class with all the materials within it.
     *
     * @param name The name of the class to be set
     * @return A material class with all the materials in it.
     */
    public static MaterialClass getGlobal(String name)
    {
        MaterialClass materialClass = new MaterialClass(name);
        for (Material material : Material.values()) {
            materialClass.addMaterial(material);
        }
        materialClass.m_isGlobal = true;
        return materialClass;
    }

    /**
     * The name of the class
     */
    private final String m_name;

    private final Set<Material> m_materialData = new HashSet<>();

    private final static MaskedBlockData.Mask[] BLOCKDATA_MASKS = new MaskedBlockData.Mask[]{
        MaskedBlockData.Mask.MATERIAL,
        MaskedBlockData.Mask.AGEABLE
    };
    private final Set<MaskedBlockData> m_blockData = new HashSet<>();
    private final Set<String> m_inherited = new HashSet<>();

    /**
     * Determines if the class is global.
     */
    private boolean m_isGlobal = false;

    public MaterialClass(@Nonnull String name)
    {
        this.m_name = name;
    }

    /**
     * Returns the name of this entity class.
     * This name will generally be reader-friendly
     *
     * @return The name of the class
     */
    @Nonnull
    public String getName()
    {
        return this.m_name;
    }
    /**
     * Returns the type name format of this class
     * This name is not user friendly but will make it instantly identifiable
     * as an entity class reference. Used for debugging as well as storing within
     * Maps.
     *
     * @return The type name of this class
     */
    @Nonnull
    public String getTypeName()
    {
        return this.m_name.replace(" ", "_").toUpperCase();
    }
    /**
     * Returns true if the passed in type is contained within this class
     * Otherwise it will return false
     *
     * @param type The type of the material to check
     * @return True if the class contains the material type
     */
    public boolean containsMaterial(@Nonnull Material type)
    {
        return this.m_materialData.contains(type);
    }
    public boolean containsBlockData(@Nonnull BlockData type)
    {
        MaskedBlockData key = new MaskedBlockData(type, BLOCKDATA_MASKS);
        return this.m_blockData.contains(key);
    }
    public boolean matchesItemStack(@Nonnull ItemStack itemStack)
    {
        return this.containsMaterial(itemStack.getType());
    }
    public boolean matchesBlock(@Nonnull Block block)
    {
        return this.containsMaterial(block.getType()) || this.containsBlockData(block.getBlockData());
    }

    /**
     * Gets a set of the material types within this class. This is a set that when
     * mutated does not
     * mutate the class itself.
     *
     * @return A set of materials contained within this class.
     */
    @Nonnull
    public Set<Material> getMaterials()
    {
        TypeClassManager manager = Inscription.getInstance().getTypeClassManager();
        HashSet<Material> returnSet = new HashSet(this.m_materialData);
        if (manager == null) {
            return returnSet;
        }

        // We are going to downgrade the block data into a material for the cases
        // that can sue the block data.
        for (MaskedBlockData data : this.m_blockData) {
            returnSet.add(data.getBlockData().getMaterial());
        }

        for (String type : this.m_inherited) {
            MaterialClass materialClass = manager.getMaterialClass(type);
            if (materialClass == null) {
                continue;
            }
            returnSet.addAll(materialClass.getMaterials());
        }
        return returnSet;
    }
    @Nonnull public Set<BlockData> getBlockData()
    {
        TypeClassManager manager = Inscription.getInstance().getTypeClassManager();
        HashSet<BlockData> returnSet = new HashSet();
        if (manager == null) {
            return returnSet;
        }

        for (MaskedBlockData blockData : m_blockData) {
            returnSet.add(blockData.getBlockData());
        }

        // We are going to try to upgrade the material into block data if it's possible.
        // Not if the material isn't valid for blocks, it'll throw an exception.
        for (Material data : this.m_materialData) {
            try {
                BlockData blockData = Bukkit.createBlockData(data);
                returnSet.add(blockData);
            }
            catch (IllegalArgumentException exception) {
            }
        }

        for (String materialClassKey : this.m_inherited) {
            MaterialClass materialClass = manager.getMaterialClass(materialClassKey);
            if (materialClass == null) {
                continue;
            }
            returnSet.addAll(materialClass.getBlockData());
        }
        return returnSet;
    }

    /**
     * Adds the material type to the material class.
     *
     * @param material
     */
    public void addMaterial(@Nonnull Material material)
    {
        this.m_materialData.add(material);
    }

    public void addMaterial(@Nonnull BlockData blockData)
    {
        MaskedBlockData key = new MaskedBlockData(blockData, BLOCKDATA_MASKS);
        this.m_blockData.add(key);
    }

    /**
     * Adds the material type parsed from the string to this class
     * Will return false if it could not successfully parse the string
     * for a material
     *
     * @param type
     * @return False if the material was not added.
     */
    public boolean addMaterial(@Nonnull String type)
    {
        try {
            Material materialType = Material.valueOf(type);
            this.addMaterial(materialType);
            return true;
        }
        catch (IllegalArgumentException error) {
        }

        try {
            BlockData blockData = Bukkit.createBlockData(type);
            this.addMaterial(blockData);
            return true;
        }
        catch (IllegalArgumentException error) {
        }

        Inscription.logger.warning("Could not find type for: " + type);
        return false;
    }

    /**
     * Adds a class to be inherited by this class.
     *
     * @param className
     */
    public void addInherited(@Nonnull String className)
    {
        this.m_inherited.add(className);
    }

    /**
     * Returns true if the class is contructed through the global methods.
     * Global signifies optimizaitons in data storage.
     *
     * @return True if the class is a global global.
     */
    public boolean isGlobal()
    {
        if (this.m_isGlobal) return true;

        TypeClassManager manager = Inscription.getInstance().getTypeClassManager();
        for (String inherited : this.m_inherited) {
            if (manager.getMaterialClass(inherited) != null && manager.getMaterialClass(inherited).isGlobal()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Will parse the configuration section for an material class
     * returns an material class based off the section passed in
     *
     * @param section
     * @return
     */
    @Nullable
    public static MaterialClass parse(@Nonnull ConfigurationSection section)
    {
        String name = section.getString("name");
        if (name == null) {
            Inscription.logger.warning("Material class does not have a name defined.");
            return null;
        }
        Inscription.logger.fine("Found name to be: '" + name + "'");

        MaterialClass materialClass = new MaterialClass(name);
        List<String> materials = section.getStringList("materials");
        if (materials != null) {
            Inscription.logger.fine("Found Materials:");
            for (String material : materials) {
                boolean valid = materialClass.addMaterial(material);
                if (!valid) {
                    Inscription.logger.warning("'" + material + " is not a valid type for MaterialClass: '" + name + "'");
                    continue;
                }
                Inscription.logger.fine(" - '" + material + "'");
            }
        }
        List<String> inherited = section.getStringList("inherited");
        if (inherited != null) {
            Inscription.logger.fine("Found Inherited:");
            for (String i : inherited) {
                Inscription.logger.fine(" - '" + i + "'");
                materialClass.addInherited(i);
            }

        }
        return materialClass;
    }
}
