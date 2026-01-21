package com.maxim.tacionian.blocks.cable;

import com.maxim.tacionian.register.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class TachyonCableBlock extends Block implements EntityBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final IntegerProperty LIGHT_LEVEL = IntegerProperty.create("light_level", 0, 3);

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private static final VoxelShape CENTER_SHAPE = Block.box(6, 6, 6, 10, 10, 10);
    // Кешуємо шейпи для сторін
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.NORTH, Block.box(6, 6, 0, 10, 10, 6),
            Direction.SOUTH, Block.box(6, 6, 10, 10, 10, 16),
            Direction.WEST, Block.box(0, 6, 6, 6, 10, 10),
            Direction.EAST, Block.box(10, 6, 6, 16, 10, 10),
            Direction.UP, Block.box(6, 10, 6, 10, 16, 10),
            Direction.DOWN, Block.box(6, 0, 6, 10, 6, 10)
    );

    public TachyonCableBlock(Properties props) {
        super(props.lightLevel(state -> state.getValue(LIGHT_LEVEL) * 4));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(POWERED, false).setValue(LIGHT_LEVEL, 0)
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CENTER_SHAPE;
        if (state.getValue(NORTH)) shape = Shapes.or(shape, SHAPES.get(Direction.NORTH));
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, SHAPES.get(Direction.SOUTH));
        if (state.getValue(EAST)) shape = Shapes.or(shape, SHAPES.get(Direction.EAST));
        if (state.getValue(WEST)) shape = Shapes.or(shape, SHAPES.get(Direction.WEST));
        if (state.getValue(UP)) shape = Shapes.or(shape, SHAPES.get(Direction.UP));
        if (state.getValue(DOWN)) shape = Shapes.or(shape, SHAPES.get(Direction.DOWN));
        return shape;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return makeConnections(context.getLevel(), context.getClickedPos(), 0);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return makeConnections(level, pos, state.getValue(LIGHT_LEVEL));
    }

    private BlockState makeConnections(LevelAccessor level, BlockPos pos, int light) {
        return this.defaultBlockState()
                .setValue(LIGHT_LEVEL, light)
                .setValue(POWERED, light > 0)
                .setValue(NORTH, canConnectTo(level, pos.north(), Direction.SOUTH))
                .setValue(SOUTH, canConnectTo(level, pos.south(), Direction.NORTH))
                .setValue(EAST, canConnectTo(level, pos.east(), Direction.WEST))
                .setValue(WEST, canConnectTo(level, pos.west(), Direction.EAST))
                .setValue(UP, canConnectTo(level, pos.above(), Direction.DOWN))
                .setValue(DOWN, canConnectTo(level, pos.below(), Direction.UP));
    }

    private boolean canConnectTo(LevelAccessor level, BlockPos neighborPos, Direction side) {
        BlockEntity be = level.getBlockEntity(neighborPos);
        if (be == null) return false;
        // СУВОРА ПЕРЕВІРКА: Тільки Тахіонна капабіліті. Жодного RF.
        return be.getCapability(ModCapabilities.TACHYON_STORAGE, side).isPresent();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, LIGHT_LEVEL, NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TachyonCableBlockEntity(pos, state);
    }

    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof TachyonCableBlockEntity cable) TachyonCableBlockEntity.tick(lvl, pos, st, cable);
        };
    }
}