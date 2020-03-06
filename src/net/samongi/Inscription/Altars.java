package net.samongi.Inscription;

import net.samongi.SamongiLib.Blocks.Altar;
import org.bukkit.Material;

import javax.annotation.Nonnull;

public class Altars {

    private static Altar s_inscriptionAltar = null;
    public static @Nonnull Altar getInscriptionAltar() {
        if (s_inscriptionAltar != null) {
            return s_inscriptionAltar;
        }
        s_inscriptionAltar = new Altar();
        s_inscriptionAltar.addPossibleFocusBlock(Material.LECTERN.createBlockData());
        s_inscriptionAltar.addTranslation('B', Material.BOOKSHELF.createBlockData());

        // @formatter:off
        s_inscriptionAltar.addLayer(0, new String[]{
            "BB   BB",
            "B     B",
            "       ",
            "       ",
            "       ",
            "B     B",
            "BB   BB",
        });
        s_inscriptionAltar.addLayer(1, new String[]{
            "BB   BB",
            "B     B",
            "       ",
            "       ",
            "       ",
            "B     B",
            "BB   BB",
        });
        // @formatter:on

        return s_inscriptionAltar;
    }

    private static Altar s_waypointAltar = null;
    public static @Nonnull Altar getWaypointAltar()
    {
        if (s_waypointAltar != null) {
            return s_waypointAltar;
        }

        s_waypointAltar = new Altar();

        s_waypointAltar.addPossibleFocusBlock(Material.BLACK_BANNER.createBlockData());
        s_waypointAltar.addPossibleFocusBlock(Material.GRAY_BANNER.createBlockData());
        s_waypointAltar.addPossibleFocusBlock(Material.LIGHT_GRAY_BANNER.createBlockData());
        s_waypointAltar.addPossibleFocusBlock(Material.WHITE_BANNER.createBlockData());
        s_waypointAltar.addPossibleFocusBlock(Material.RED_BANNER.createBlockData());
        s_waypointAltar.addPossibleFocusBlock(Material.PINK_BANNER.createBlockData());
        s_waypointAltar.addPossibleFocusBlock(Material.GREEN_BANNER.createBlockData());
        s_waypointAltar.addPossibleFocusBlock(Material.LIME_BANNER.createBlockData());
        s_waypointAltar.addPossibleFocusBlock(Material.BLUE_BANNER.createBlockData());
        s_waypointAltar.addPossibleFocusBlock(Material.LIGHT_BLUE_BANNER.createBlockData());
        s_waypointAltar.addPossibleFocusBlock(Material.PURPLE_BANNER.createBlockData());
        s_waypointAltar.addPossibleFocusBlock(Material.MAGENTA_BANNER.createBlockData());
        s_waypointAltar.addPossibleFocusBlock(Material.YELLOW_BANNER.createBlockData());
        s_waypointAltar.addPossibleFocusBlock(Material.ORANGE_BANNER.createBlockData());
        s_waypointAltar.addPossibleFocusBlock(Material.BROWN_BANNER.createBlockData());
        s_waypointAltar.addPossibleFocusBlock(Material.CYAN_BANNER.createBlockData());

        s_waypointAltar.addTranslation('O', Material.OBSIDIAN.createBlockData());
        s_waypointAltar.addTranslation('L', Material.LAPIS_BLOCK.createBlockData());

        // @formatter:off
        s_waypointAltar.addLayer(-1, new String[]{
            "       ",
            "       ",
            "       ",
            "   O   ",
            "       ",
            "       ",
            "       ",
        });
        s_waypointAltar.addLayer(0, new String[]{
            "O     O",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "O     O",
        });
        s_waypointAltar.addLayer(1, new String[]{
            "O     O",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "O     O",
        });
        s_waypointAltar.addLayer(2, new String[]{
            "L     L",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "L     L",
        });
        // @formatter:on

        return s_waypointAltar;
    }
}
