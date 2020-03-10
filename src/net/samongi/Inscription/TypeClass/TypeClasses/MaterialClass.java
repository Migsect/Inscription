package net.samongi.Inscription.TypeClass.TypeClasses;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.samongi.Inscription.Inscription;

import net.samongi.Inscription.TypeClass.TypeClass;
import net.samongi.Inscription.TypeClass.TypeClassHandler;
import net.samongi.Inscription.TypeClass.TypeClassManager;
import net.samongi.SamongiLib.Items.MaskedBlockData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.craftbukkit.libs.jline.internal.Nullable;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

/**
 * Referred to by attributes and othe rplugin systems
 * to group together different material types.
 */
public class MaterialClass extends TypeClass {

    //----------------------------------------------------------------------------------------------------------------//
    public static final TypeClassHandler<MaterialClass> handler = new TypeClassHandler<>("material-classes",
        MaterialClass::new, new MaterialClass("GLOBAL", true));

    //----------------------------------------------------------------------------------------------------------------//
    private final Set<Material> m_materials = new HashSet<>();

    //----------------------------------------------------------------------------------------------------------------//
    public MaterialClass(String name) {
        super(name);
    }
    private MaterialClass(String name, boolean isGlobal) {
        super(name, isGlobal);
    }
    private MaterialClass(ConfigurationSection section) throws InvalidConfigurationException {
        super(section);
    }
    //----------------------------------------------------------------------------------------------------------------//
    @Nonnull public Set<Material> getMaterials() {
        return getClassMembers().stream().map((Object obj) -> (Material) obj).collect(Collectors.toSet());
    }

    public boolean containsMaterial(@Nonnull Material type) {
        return this.m_materials.contains(type);
    }

    public boolean matchesItemStack(@Nonnull ItemStack itemStack) {
        return this.containsMaterial(itemStack.getType());
    }

    public void addMaterial(@Nonnull Material material) {
        this.m_materials.add(material);
    }

    /**
     * Adds the material type parsed from the string to this class
     * Will return false if it could not successfully parse the string
     * for a material
     *
     * @param type
     * @return False if the material was not added.
     */
    public boolean addMaterial(@Nonnull String type) {
        try {
            Material materialType = Material.valueOf(type);
            this.addMaterial(materialType);

        }
        catch (IllegalArgumentException error) {
            return false;
        }

        return true;
    }
    //----------------------------------------------------------------------------------------------------------------//
    @Override protected TypeClassHandler<?> getTypeClassManager() {
        return handler;
    }

    @Override protected void addGlobalClassMembers() {
        for (Material material : Material.values()) {
            addMaterial(material);
        }
    }

    @Override public Set<Object> getDirectClassMembers() {
        return new HashSet<>(m_materials);
    }

    @Override protected void parse(ConfigurationSection section) {
        List<String> materialStrings = section.getStringList("materials");
        if (materialStrings != null) {
            Inscription.logger.fine("Found Materials:");
            for (String materialString : materialStrings) {
                boolean valid = addMaterial(materialString);
                if (!valid) {
                    Inscription.logger.warning("'" + materialString + "' is not a valid type for MaterialClass '" + getName() + "'");
                    continue;
                }
                Inscription.logger.fine(" - '" + materialString + "'");
            }
        }
    }

    //----------------------------------------------------------------------------------------------------------------//
}
