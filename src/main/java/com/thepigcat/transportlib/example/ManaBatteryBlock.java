package com.thepigcat.transportlib.example;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ManaBatteryBlock extends BaseEntityBlock {
    public static final EnumProperty<BatteryType> BATTERY_TYPE = EnumProperty.create("battery_type", BatteryType.class);

    public ManaBatteryBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(BATTERY_TYPE, BatteryType.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(BATTERY_TYPE));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(Items.STICK)) {
            if (level.getBlockEntity(pos) instanceof ManaBatteryBlockEntity be){
                be.fillMana(100);
            }
        } else if (stack.is(Items.DIAMOND)) {
            if (level.getBlockEntity(pos) instanceof ManaBatteryBlockEntity be && level instanceof ServerLevel serverLevel) {
                int remainder = ExampleNetworkRegistry.MANA_NETWORK.get().transport(serverLevel, pos, be.manaStored);
                be.drainMana(be.manaStored - remainder);
            }
        } else if (stack.isEmpty()) {
            BatteryType type = state.getValue(BATTERY_TYPE);
            if (type == BatteryType.OUTPUT) {
                type = BatteryType.NONE;
            } else {
                type = BatteryType.values()[type.ordinal() + 1];
            }
            level.setBlockAndUpdate(pos, state.setValue(BATTERY_TYPE, type));
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(ManaBatteryBlock::new);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new ManaBatteryBlockEntity(blockPos, blockState);
    }

    public enum BatteryType implements StringRepresentable {
        NONE("none"),
        INPUT("input"),
        OUTPUT("output");

        private final String name;

        BatteryType(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.name;
        }
    }
}
