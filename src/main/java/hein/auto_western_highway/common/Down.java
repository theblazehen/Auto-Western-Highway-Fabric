package hein.auto_western_highway.common;

import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import hein.auto_western_highway.common.types.StepHeight;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

import static hein.auto_western_highway.common.AutoHighwaySchematic.*;
import static hein.auto_western_highway.common.Baritone.build;
import static hein.auto_western_highway.common.Baritone.resetSettings;
import static hein.auto_western_highway.common.Blocks.*;
import static hein.auto_western_highway.common.Globals.globalHudRenderer;
import static hein.auto_western_highway.common.InventoryManagement.replenishItemsIfNeeded;
import static hein.auto_western_highway.common.InventoryManagement.setHotbarToInventoryLoadout;
import static net.minecraft.util.math.Direction.Axis.X;
import static net.minecraft.util.math.Direction.Axis.Y;

public class Down {
    public static StepHeight getStepDownHeight(BlockPos standingBlock) {
        List<String> rayDownBlocks = getBlocksNameFromBlockPositions(getRayDownBlockPositions(standingBlock));
        StepHeight stepHeight = new StepHeight();
        stepHeight.containsScaffoldBlockingBlocks = rayDownBlocks.stream().anyMatch(Blocks::isScaffoldBlockingBlock);
        stepHeight.height = 0;
        for (int step = 0; step < Constants.MAX_RAY_STEPS; step++) {
            List<String> blocks = List.of(
                    rayDownBlocks.get(step * 3),
                    rayDownBlocks.get(step * 3 + 1),
                    rayDownBlocks.get(step * 3 + 2)
            );
            // on the 0-th step, ignore the block we are standing on
            if (step == 0 && isNonTerrainBlock(blocks.get(1)) && isNonTerrainBlock(blocks.get(2))) {
                stepHeight.height += 1;
            } else if (blocks.stream().allMatch(Blocks::isNonTerrainBlock)) {
                stepHeight.height += 1;
            } else {
                break;
            }
        }
        return stepHeight;
    }

    private static List<BlockPos> getRayDownBlockPositions(BlockPos standingBlock) {
        List<BlockPos> blocks = new ArrayList<>(List.of(copyBlock(standingBlock, 0, 1, 0)));
        for (int step = 0; step < Constants.MAX_RAY_STEPS; step++) {
            BlockPos stepStartPos = offsetBlock(blocks.get(blocks.size() - 1), 0, -1, 0);
            for (int blockStep = 0; blockStep < 3; blockStep++) {
                blocks.add(copyBlock(stepStartPos, -blockStep, 0, 0));
            }
        }
        return blocks.subList(1, blocks.size());
    }

    public static BlockPos stepDown(int count, BlockPos buildOrigin) {
        buildOrigin = buildOrigin.offset(Y, 1);
        for (int i = 0; i < count; i++) {
            replenishItemsIfNeeded();
            setHotbarToInventoryLoadout();
            globalHudRenderer.message = String.format("Stepping down %d step%s", count - i, count - i > 1 ? "s" : "");
            build(STEP_DOWN, copyBlock(buildOrigin, -1, -1, -1));
            build(STEP, copyBlock(buildOrigin, -2, -2, -1));
            buildOrigin = offsetBlock(buildOrigin, -2, -1, 0);
        }
        return buildOrigin.offset(Y, -1);
    }

    public static void downwardScaffold(StepHeight stepDownHeight, BlockPos standingBlock) {
        BlockPos buildOrigin = copyBlock(standingBlock);
        Settings settings = BaritoneAPI.getSettings();
        settings.buildIgnoreExisting.value = !stepDownHeight.containsScaffoldBlockingBlocks;
        for (int i = 0; i < stepDownHeight.height; i++) {
            replenishItemsIfNeeded();
            setHotbarToInventoryLoadout();
            globalHudRenderer.message = String.format("Scaffolding down %d step%s", stepDownHeight.height - i, stepDownHeight.height - i > 1 ? "s" : "");
            build(STEP_SCAFFOLD, copyBlock(buildOrigin, -2 * stepDownHeight.height, -stepDownHeight.height, 0));
            buildOrigin = offsetBlock(buildOrigin, 2, 1, 0);
        }
        resetSettings();
    }

    public static int getFutureStepDownLength(BlockPos standingBlock, int stepUpHeight) {
        BlockPos stepUpBlock = copyBlock(standingBlock, -2 * stepUpHeight, stepUpHeight, 0);
        for (int futureStep = 0; futureStep < Constants.FUTURE_STEPS; futureStep++) {
            BlockPos futureBlock = stepUpBlock.offset(X, -futureStep);
            StepHeight futureStepDownHeight = getStepDownHeight(futureBlock);
            if (futureStepDownHeight.height >= stepUpHeight) {
                return 4 * stepUpHeight + futureStep;
            }
        }
        return 0;
    }

}