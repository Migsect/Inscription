package net.samongi.Inscription.Altars;

import net.samongi.SamongiLib.Items.Comparators.BlockDataComparator;
import net.samongi.SamongiLib.Items.Comparators.BlockDataMaterialComparator;
import net.samongi.SamongiLib.Items.MaskedBlockData;
import net.samongi.SamongiLib.Vector.SamIntVector;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import javax.annotation.Nonnull;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Altar {

    private static MaskedBlockData.Mask[] DEFAULT_BLOCK_DATA_COMPARATORS = new MaskedBlockData.Mask[]{MaskedBlockData.Mask.MATERIAL};

    //--------------------------------------------------------------------------------------------------------------------//
    private static class RelativeBlock {

        private final SamIntVector m_displacement;
        private final MaskedBlockData m_blockData;

        public RelativeBlock(MaskedBlockData blockData, SamIntVector displacement) {
            m_blockData = blockData;
            m_displacement = displacement;
        }

        public MaskedBlockData getBlockData() {
            return m_blockData;
        }

        public SamIntVector getDisplacement() {
            return m_displacement;
        }
    }

    //--------------------------------------------------------------------------------------------------------------------//
    private List<RelativeBlock> m_blocks;
    private MaskedBlockData m_focusBlock;

    private Map<Character, MaskedBlockData> m_blockDataTranslation = new HashMap<>();

    //--------------------------------------------------------------------------------------------------------------------//

    public Altar() {
        addTranslation('_', Material.AIR.createBlockData());
    }

    //--------------------------------------------------------------------------------------------------------------------//
    public void setFocusBlock(@Nonnull BlockData blockData) {
        m_focusBlock = new MaskedBlockData(blockData, DEFAULT_BLOCK_DATA_COMPARATORS);
    }
    public void setFocusBlock(@Nonnull MaskedBlockData blockData) {
        m_focusBlock = blockData;
    }

    public void addRelativeBlock(@Nonnull BlockData blockData, @Nonnull SamIntVector displacement) {
        addRelativeBlock(new MaskedBlockData(blockData, DEFAULT_BLOCK_DATA_COMPARATORS), displacement);
    }
    public void addRelativeBlock(@Nonnull MaskedBlockData blockData, @Nonnull SamIntVector displacement) {
        RelativeBlock relativeBlock = new RelativeBlock(blockData, displacement);
        m_blocks.add(relativeBlock);
    }

    public void addTranslation(char character, @Nonnull BlockData blockData) {
        m_blockDataTranslation.put(character, new MaskedBlockData(blockData, DEFAULT_BLOCK_DATA_COMPARATORS));
    }
    public void addTranslation(char character, @Nonnull MaskedBlockData blockData) {
        m_blockDataTranslation.put(character, blockData);
    }

    public MaskedBlockData getTranslation(char character) {
        return m_blockDataTranslation.get(character);
    }

    public void addLayer(int verticalDisplacement, @Nonnull String[] pattern) {
        // validating the rows to make sure they are uniform.
        int rowLength = -1;
        for (String row : pattern) {
            if (row.length() % 2 != 1) {
                throw new IllegalArgumentException();
            }
            if (row.length() != rowLength && rowLength > 0) {
                throw new IllegalArgumentException();
            }
            rowLength = row.length();
        }

        int colLength = pattern.length;
        if (pattern.length % 2 != 1) {
            throw new IllegalArgumentException();
        }

        int colMidPoint = colLength / 2;
        int rowMidPoint = rowLength / 2;

        for (int col = 0; col < rowLength; col++) {
            for (int row = 0; row < colLength; row++) {
                int rowDisplacement = row - rowMidPoint;
                int colDisplacement = col - colMidPoint;
                char character = pattern[row].charAt(col);
                MaskedBlockData blockData = getTranslation(character);
                addRelativeBlock(blockData, new SamIntVector(rowDisplacement, verticalDisplacement, colDisplacement));
            }
        }
    }
    //--------------------------------------------------------------------------------------------------------------------//
    public boolean checkPattern(@Nonnull Location location) {
        Block centerBlock = location.getBlock();
        if (centerBlock == null) {
            return false;
        }
        BlockData centerBlockData = centerBlock.getBlockData();
        return false;
    }

}
