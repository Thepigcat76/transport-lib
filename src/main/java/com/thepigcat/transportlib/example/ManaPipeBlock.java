package com.thepigcat.transportlib.example;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ManaPipeBlock extends Block {
    public static final BooleanProperty[] CONNECTION = new BooleanProperty[6];
    public final int border;
    public final VoxelShape shapeCenter;
    public final VoxelShape shapeD;
    public final VoxelShape shapeU;
    public final VoxelShape shapeN;
    public final VoxelShape shapeS;
    public final VoxelShape shapeW;
    public final VoxelShape shapeE;
    public final VoxelShape[] shapes;

    static {
        for (Direction dir : Direction.values()) {
            CONNECTION[dir.get3DDataValue()] = BooleanProperty.create(dir.getSerializedName());
        }
    }

    public ManaPipeBlock(Properties properties, int width) {
        super(properties.noOcclusion());
        registerDefaultState(getStateDefinition().any()
                .setValue(CONNECTION[0], false)
                .setValue(CONNECTION[1], false)
                .setValue(CONNECTION[2], false)
                .setValue(CONNECTION[3], false)
                .setValue(CONNECTION[4], false)
                .setValue(CONNECTION[5], false)
        );
        border = (16 - width) / 2;
        int B0 = border;
        int B1 = 16 - border;
        shapeCenter = box(B0, B0, B0, B1, B1, B1);
        shapeD = box(B0, 0, B0, B1, B0, B1);
        shapeU = box(B0, B1, B0, B1, 16, B1);
        shapeN = box(B0, B0, 0, B1, B1, B0);
        shapeS = box(B0, B0, B1, B1, B1, 16);
        shapeW = box(0, B0, B0, B0, B1, B1);
        shapeE = box(B1, B0, B0, 16, B1, B1);
        shapes = new VoxelShape[64];
    }

    @Override
    public VoxelShape getShape(BlockState blockState, BlockGetter p_60556_, BlockPos p_60557_, CollisionContext p_60558_) {
        int index = 0;

        for (Direction direction : Direction.values()) {
            if (blockState.getValue(CONNECTION[direction.ordinal()])) {
                index |= 1 << direction.ordinal();
            }
        }

        return getShape(index);
    }

    public VoxelShape getShape(int i) {
        if (shapes[i] == null) {
            shapes[i] = shapeCenter;

            if (((i >> 0) & 1) != 0) {
                shapes[i] = Shapes.or(shapes[i], shapeD);
            }

            if (((i >> 1) & 1) != 0) {
                shapes[i] = Shapes.or(shapes[i], shapeU);
            }

            if (((i >> 2) & 1) != 0) {
                shapes[i] = Shapes.or(shapes[i], shapeN);
            }

            if (((i >> 3) & 1) != 0) {
                shapes[i] = Shapes.or(shapes[i], shapeS);
            }

            if (((i >> 4) & 1) != 0) {
                shapes[i] = Shapes.or(shapes[i], shapeW);
            }

            if (((i >> 5) & 1) != 0) {
                shapes[i] = Shapes.or(shapes[i], shapeE);
            }
        }

        return shapes[i];
    }

    // Check for newly added blocks
    @Override
    public @NotNull BlockState updateShape(BlockState blockState, Direction facingDirection, BlockState facingBlockState, LevelAccessor level, BlockPos blockPos, BlockPos facingBlockPos) {
        updatePipeBlock(blockState, facingDirection, level, blockPos, facingBlockPos);

        int connectionIndex = facingDirection.ordinal();
        BlockEntity blockEntity = level.getBlockEntity(facingBlockPos);
        if (canConnectToPipe(facingBlockState) || (blockEntity != null && canConnectTo(blockEntity))) {
            return blockState.setValue(CONNECTION[connectionIndex], true);
        } else if (facingBlockState.isEmpty()) {
            return blockState.setValue(CONNECTION[connectionIndex], false);
        }

        return blockState;
    }


    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        BlockState blockState = defaultBlockState();

        for (Direction direction : Direction.values()) {
            int connectionIndex = direction.ordinal();
            BlockPos facingBlockPos = blockPos.relative(direction);
            BlockEntity blockEntity = level.getBlockEntity(facingBlockPos);

            if (blockEntity != null && canConnectTo(blockEntity)) {
                blockState = blockState.setValue(CONNECTION[connectionIndex], true);
            }
        }

        return blockState;
    }

    @Override
    public @NotNull RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTION[0], CONNECTION[1], CONNECTION[2], CONNECTION[3], CONNECTION[4], CONNECTION[5]);
    }

    public boolean canConnectToPipe(BlockState connectTo) {
        return connectTo.is(this);
    }

    public boolean canConnectTo(@Nullable BlockEntity connectTo) {
        return connectTo instanceof ManaBatteryBlockEntity;
    }

    // TODO: Create nodes for connections to interactors
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (level instanceof ServerLevel serverLevel) {
            int connectionsAmount = 0;
            boolean[] connections = new boolean[6];
            Direction[] directions = new Direction[6];
            BlockPos interactor = null;
            Direction interactorConnection = null;

            for (Direction dir : Direction.values()) {
                boolean value = state.getValue(CONNECTION[dir.get3DDataValue()]);
                connections[dir.get3DDataValue()] = value;
                if (value) {
                    directions[dir.get3DDataValue()] = dir;
                    connectionsAmount++;
                    if (ExampleNetworkRegistry.MANA_NETWORK.get().checkForInteractorAt(serverLevel, pos, dir)) {
                        interactor = pos.relative(dir);
                        interactorConnection = dir;
                    }
                } else {
                    directions[dir.get3DDataValue()] = null;
                }
            }

            if ((connectionsAmount == 2
                    && ((connections[0] && connections[1])
                    || (connections[2] && connections[3])
                    || (connections[4] && connections[5]))) || connectionsAmount == 0) {
                if (ExampleNetworkRegistry.MANA_NETWORK.get().hasNodeAt(serverLevel, pos)) {
                    ExampleNetworkRegistry.MANA_NETWORK.get().removeNodeAndUpdate(serverLevel, pos);
                }

                Direction direction0 = null;
                Direction direction1 = null;
                for (Direction direction : directions) {
                    if (direction != null) {
                        if (direction0 == null) {
                            direction0 = direction;
                        } else {
                            direction1 = direction;
                        }
                    }
                }

                if (interactor != null) {
                    ExampleNetworkRegistry.MANA_NETWORK.get().addNodeAndUpdate(serverLevel, pos, directions, false, interactor, interactorConnection);
                } else if (direction0 != null && direction1 != null) {
                    ExampleNetworkRegistry.MANA_NETWORK.get().addConnection(serverLevel, pos, direction0, direction1);
                }
            } else {
                ExampleNetworkRegistry.MANA_NETWORK.get().addNodeAndUpdate(serverLevel, pos, directions, connectionsAmount == 1, interactor, interactorConnection);
            }

        }

    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);

        if (level instanceof ServerLevel serverLevel) {
            if (!state.is(newState.getBlock())) {
                if (ExampleNetworkRegistry.MANA_NETWORK.get().hasNodeAt(serverLevel, pos)) {
                    ExampleNetworkRegistry.MANA_NETWORK.get().removeNodeAndUpdate(serverLevel, pos);
                } else {
                    List<Direction> directions = getDirections(state);
                    if (directions.size() == 2) {
                        Direction direction0 = directions.getFirst();
                        Direction direction1 = directions.get(1);
                        if (direction0 == direction1.getOpposite()) {
                            ExampleNetworkRegistry.MANA_NETWORK.get().removeConnection(serverLevel, pos, direction0, direction1);
                        }
                    }
                }

            }
        }

    }

    private static void updatePipeBlock(BlockState blockState, Direction facingDirection, LevelAccessor level, BlockPos blockPos, BlockPos facingBlockPos) {
        if (level instanceof ServerLevel serverLevel) {
            if (ExampleNetworkRegistry.MANA_NETWORK.get().checkForInteractorAt(serverLevel, blockPos, facingDirection)) {
                int connectionsAmount = 0;
                Direction[] directions = new Direction[6];
                for (Direction direction : Direction.values()) {
                    boolean value = blockState.getValue(CONNECTION[direction.get3DDataValue()]);
                    if (value) {
                        directions[direction.get3DDataValue()] = direction;
                        connectionsAmount++;
                    }
                }

                ExampleNetworkRegistry.MANA_NETWORK.get().addNodeAndUpdate(serverLevel, blockPos, directions, connectionsAmount == 1, facingBlockPos, facingDirection);
            }
        }
    }

    private static List<Direction> getDirections(BlockState state) {
        List<Direction> directions = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (state.getValue(CONNECTION[direction.get3DDataValue()])) {
                directions.add(direction);
            }
        }
        return directions;
    }

}