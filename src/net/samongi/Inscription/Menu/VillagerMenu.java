package net.samongi.Inscription.Menu;

import net.samongi.SamongiLib.Menu.InventoryMenu;
import net.samongi.SamongiLib.Utilities.TextUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.sqlite.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class VillagerMenu {

    //----------------------------------------------------------------------------------------------------------------//
    private static final String TITLE = ChatColor.DARK_PURPLE + "Villager Scan";
    private static final int ROWS = 6;

    //----------------------------------------------------------------------------------------------------------------//
    private List<Villager> m_villagers = new ArrayList<>();
    private final int m_range;

    //----------------------------------------------------------------------------------------------------------------//
    public VillagerMenu(Entity centerEntity, int range) {
        m_range = range;
        populateVillagers(centerEntity, range);
    }

    //----------------------------------------------------------------------------------------------------------------//

    private void populateVillagers(Entity centerEntity, int range) {
        List<Entity> entities = centerEntity.getNearbyEntities(range, range, range);
        for (Entity entity : entities) {
            if (entity instanceof Villager) {
                m_villagers.add((Villager) entity);
            }
        }
    }
    //----------------------------------------------------------------------------------------------------------------//

    private static Material getLevelMaterial(int level) {
        switch (level) {
            case 1:
                return Material.STONE;
            case 2:
                return Material.IRON_BLOCK;
            case 3:
                return Material.GOLD_BLOCK;
            case 4:
                return Material.EMERALD_BLOCK;
            case 5:
                return Material.DIAMOND_BLOCK;
            default:
                return Material.BARRIER;
        }
    }
    private static String getLevelname(int level) {
        switch (level) {
            case 1:
                return "Novice";
            case 2:
                return "Apprentice";
            case 3:
                return "Journeyman";
            case 4:
                return "Expert";
            case 5:
                return "Master";
            default:
                return "Undefined";
        }
    }

    private List<String> getVillagerTradeStrings(Villager villager) {
        List<String> tradeStrings = new ArrayList<>();
        for (int index = 0; index < villager.getRecipeCount(); index++) {
            MerchantRecipe recipe = villager.getRecipe(index);
            List<ItemStack> ingredients = recipe.getIngredients();
            ItemStack result = recipe.getResult();

            List<String> ingredientStrings = new ArrayList<>();
            for (ItemStack ingredient : ingredients) {
                if (ingredient == null || ingredient.getType() == Material.AIR) {
                    continue;
                }

                int ingredientAmount = ingredient.getAmount();
                String ingredientMaterialString = TextUtil.capitalize(ingredient.getType().toString().replace('_', ' ').toLowerCase(), " ");
                ChatColor ingredientColor = ingredient.getType() == Material.EMERALD ? ChatColor.GREEN : ChatColor.GOLD;
                ingredientStrings.add(ChatColor.BLUE + "" + ingredientAmount + ChatColor.YELLOW + " x " + ingredientColor + ingredientMaterialString);
            }
            String ingredientsString = StringUtils.join(ingredientStrings, ", ");

            int resultAmount = result.getAmount();
            String resultMaterialString = TextUtil.capitalize(result.getType().toString().replace('_', ' ').toLowerCase(), " ");
            ChatColor resultColor = result.getType() == Material.EMERALD ? ChatColor.GREEN : ChatColor.GOLD;

            String resultString = ChatColor.BLUE + "" + resultAmount + ChatColor.YELLOW + " x " + resultColor + resultMaterialString;
            tradeStrings.add("  " + ingredientsString + ChatColor.YELLOW + " -> " + resultString);
        }
        return tradeStrings;
    }

    private ItemStack getVillagerItem(Villager villager) {
        ItemStack item = new ItemStack(getLevelMaterial(villager.getVillagerLevel()));
        ItemMeta itemMeta = item.getItemMeta();
        assert itemMeta != null;

        String levelName = getLevelname(villager.getVillagerLevel());
        String professionName = getProfessionName(villager.getProfession());
        String name = villager.getCustomName();
        if (name == null || name.isEmpty()) {
            name = ChatColor.GREEN + levelName + " " + professionName;
        }
        itemMeta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.YELLOW + "Profession: " + ChatColor.BLUE + professionName);
        lore.add(ChatColor.YELLOW + "Level: " + ChatColor.BLUE + levelName + ChatColor.DARK_GRAY + "[" + villager.getVillagerLevel() + "]");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Trades:");
        lore.addAll(getVillagerTradeStrings(villager));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to highlight the villager in the world.");

        itemMeta.setLore(lore);

        item.setItemMeta(itemMeta);

        return item;
    }
    //----------------------------------------------------------------------------------------------------------------//

    private static Material getProfessionMaterial(Villager.Profession profession) {
        switch (profession) {
            case ARMORER:
                return Material.BLAST_FURNACE;
            case BUTCHER:
                return Material.SMOKER;
            case CARTOGRAPHER:
                return Material.CARTOGRAPHY_TABLE;
            case CLERIC:
                return Material.BREWING_STAND;
            case FARMER:
                return Material.COMPOSTER;
            case FISHERMAN:
                return Material.BARREL;
            case FLETCHER:
                return Material.FLETCHING_TABLE;
            case LEATHERWORKER:
                return Material.CAULDRON;
            case LIBRARIAN:
                return Material.LECTERN;
            case MASON:
                return Material.STONECUTTER;
            case NITWIT:
                return Material.DIRT;
            case SHEPHERD:
                return Material.LOOM;
            case TOOLSMITH:
                return Material.SMITHING_TABLE;
            case WEAPONSMITH:
                return Material.GRINDSTONE;
            case NONE:
                return Material.GLASS;
            default:
                return Material.BARRIER;
        }
    }

    private static String getProfessionName(Villager.Profession profession) {

        String professionName = profession.toString().toLowerCase();
        if (profession == Villager.Profession.NONE) {
            professionName = "unemployed";
        }
        professionName = professionName.substring(0, 1).toUpperCase() + professionName.substring(1);
        return professionName;
    }

    private List<Villager> getVillagersOfProfession(Villager.Profession profession) {
        return m_villagers.stream().filter((villager) -> villager.getProfession() == profession).collect(Collectors.toList());
    }

    private ItemStack getProfessionItem(Villager.Profession profession) {
        List<Villager> professionVillagers = getVillagersOfProfession(profession);

        ItemStack item = new ItemStack(getProfessionMaterial(profession));
        item.setAmount(professionVillagers.size());

        ItemMeta itemMeta = item.getItemMeta();
        assert itemMeta != null;

        String professionName = getProfessionName(profession);
        itemMeta.setDisplayName(ChatColor.GREEN + professionName + " Villagers");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.YELLOW + "There are " + ChatColor.BLUE + professionVillagers.size() + ChatColor.YELLOW + " within " + ChatColor.BLUE + m_range
            + ChatColor.YELLOW + " meters.");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to open up a listing of all " + professionName.toLowerCase() + " villagers in your vicinity.");

        itemMeta.setLore(lore);

        item.setItemMeta(itemMeta);

        return item;
    }

    private InventoryMenu getProfessionMenu(Player player, Villager.Profession profession) {
        InventoryMenu menu = new InventoryMenu(player, ROWS, TITLE + " - " + getProfessionName(profession));

        List<Villager> professionVillagers = getVillagersOfProfession(profession);
        professionVillagers.sort((a, b) -> b.getVillagerLevel() - a.getVillagerLevel());

        int slot = 0;
        for (Villager villager : professionVillagers) {
            ItemStack villagerItem = getVillagerItem(villager);

            menu.setItem(slot, villagerItem);
            menu.addLeftClickAction(slot, () -> {
                villager.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 1200, 0));
            }, false);
            slot++;
            if (slot >= menu.getInventory().getSize()) {
                break;
            }
        }

        return menu;
    }

    //----------------------------------------------------------------------------------------------------------------//
    private class ProfessionGroup {

        public final Villager.Profession profession;
        public final List<Villager> villagers;

        public ProfessionGroup(Villager.Profession profession) {
            this.profession = profession;
            this.villagers = getVillagersOfProfession(profession);
        }
    }

    public InventoryMenu getMenu(Player player) {
        InventoryMenu menu = new InventoryMenu(player, ROWS, TITLE);

        List<ProfessionGroup> villagerGroups = Arrays.stream(Villager.Profession.values()).map(ProfessionGroup::new).collect(Collectors.toList());
        villagerGroups.sort((a, b) -> b.villagers.size() - a.villagers.size());

        int slot = 0;
        for (ProfessionGroup group : villagerGroups) {
            ItemStack professionItem = getProfessionItem(group.profession);

            // Skipping the item if there are none of these villagers.
            if (professionItem.getAmount() < 1) {
                continue;
            }

            InventoryMenu professionMenu = getProfessionMenu(player, group.profession);
            menu.setItem(slot, professionItem);
            menu.addClickAction(slot, professionMenu::openMenu);
            slot++;
            if (slot >= menu.getInventory().getSize()) {
                break;
            }
        }

        return menu;
    }

    //----------------------------------------------------------------------------------------------------------------//
}
