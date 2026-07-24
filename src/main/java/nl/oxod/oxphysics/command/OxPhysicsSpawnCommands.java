package nl.oxod.oxphysics.command;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.oxod.oxphysics.OxPhysics;
import nl.oxod.oxphysics.api.BlockDisplayPhysicsAccessor;
import nl.oxod.oxphysics.api.EntityPhysicsElement;
import nl.oxod.oxphysics.api.PhysicsInteractionAccessor;
import nl.oxod.oxphysics.bullet.collision.body.EntityRigidBody;
import nl.oxod.oxphysics.bullet.collision.body.shape.MinecraftShape;
import nl.oxod.oxphysics.bullet.collision.space.MinecraftSpace;
import nl.oxod.oxphysics.mixin.BlockDisplayMixin;
import nl.oxod.oxphysics.mixin.DisplayAccessor;
import nl.oxod.oxphysics.mixin.InteractionAccessor;

public class OxPhysicsSpawnCommands {
  private static final BlockState DEFAULT_SPAWN_BLOCKSTATE = Blocks.STONE.defaultBlockState();
  private static final int DISPLAY_INTERPOLATION_TICKS = 2;
  private static final double GRID_SPACING = 2.1;
  private static final float SHAPE_MARGIN = 1E-4f;
  private static final double FULL_CUBE_EPSILON = 1E-5;

  public static void register(
      final LiteralArgumentBuilder<CommandSourceStack> root,
      final CommandBuildContext buildContext) {

    var blockArgument = Commands.argument("block", BlockStateArgument.block(buildContext))
        .executes(ctx -> executeSpawnBlock(ctx))
        .then(Commands.argument("name", StringArgumentType.string())
            .executes(ctx -> executeSpawnBlock(ctx)));

    var gridArgument = Commands.argument("sideLength", IntegerArgumentType.integer(1, 32))
        .executes(ctx -> executeSpawnGrid(ctx))
        .then(Commands.argument("block", BlockStateArgument.block(buildContext))
            .executes(ctx -> executeSpawnGrid(ctx))
            .then(Commands.argument("name", StringArgumentType.string())
                .executes(ctx -> executeSpawnGrid(ctx))));

    root.then(Commands.literal("spawn")
        .then(Commands.literal("block")
            .executes(ctx -> executeSpawnBlock(ctx))
            .then(blockArgument))
        .then(Commands.literal("grid")
            .then(gridArgument)));
  }

  private static int executeSpawnBlock(CommandContext<CommandSourceStack> ctx) {
    CommandSourceStack source = ctx.getSource();
    BlockState state = getArgumentOrDefault(ctx, "block", DEFAULT_SPAWN_BLOCKSTATE);
    String name = getArgumentOrNull(ctx, "name", String.class);

    if (!spawnPhysicsBlock(source.getPosition(), state, source.getLevel(), name)) {
      source.sendFailure(Component.literal("§cOxPhysics Error: Could not add entity to world."));
      return 0;
    }

    source.sendSuccess(() -> Component.literal("Spawned block"), false);
    return 1;
  }

  private static int executeSpawnGrid(CommandContext<CommandSourceStack> ctx) {
    CommandSourceStack source = ctx.getSource();
    Vec3 basePos = source.getPosition();
    Level level = source.getLevel();

    int sideLength = IntegerArgumentType.getInteger(ctx, "sideLength");
    BlockState state = getArgumentOrDefault(ctx, "block", DEFAULT_SPAWN_BLOCKSTATE);
    String name = getArgumentOrNull(ctx, "name", String.class);

    int spawnedCount = 0;

    for (int x = 0; x < sideLength; x++) {
      for (int y = 0; y < sideLength; y++) {
        for (int z = 0; z < sideLength; z++) {
          Vec3 pos = new Vec3(x, y, z).scale(GRID_SPACING).add(basePos);
          if (spawnPhysicsBlock(pos, state, level, name)) {
            spawnedCount++;
          }
        }
      }
    }

    if (spawnedCount == 0) {
      source.sendFailure(Component.literal("§cOxPhysics Error: Could not add entities to world."));
      return 0;
    }

    final int total = spawnedCount;
    source.sendSuccess(() -> Component.literal("Spawned grid of " + total + " blocks"), false);
    return total;
  }

  private static boolean spawnPhysicsBlock(Vec3 pos, BlockState blockState, Level level, @Nullable String name) {
    var space = MinecraftSpace.get(level);
    if (space == null) {
      OxPhysics.LOGGER.error("Physics Error: Physics space for level is null.");
      return false;
    }

    Display.BlockDisplay display = createBlockDisplay(pos, blockState, level, name);
    MinecraftShape shape = createCollisionShape(level, pos, blockState, display);
    shape.setMargin(SHAPE_MARGIN);

    // Rigidbody setup
    var rigidBody = new EntityRigidBody((EntityPhysicsElement) display, space, shape);
    rigidBody.setCcdMotionThreshold(0.01f);
    rigidBody.setCcdSweptSphereRadius(0.4f);
    rigidBody.setContactProcessingThreshold(0.0f);

    var physicsAccessor = (BlockDisplayPhysicsAccessor) display;
    physicsAccessor.physics$setActive(true);
    physicsAccessor.physics$setRigidBody(rigidBody);

    if (!level.addFreshEntity(display)) {
      return false;
    }

    Interaction interaction = createInteractionEntity(pos, level, rigidBody);
    physicsAccessor.physics$setInteractionEntity(interaction);

    if (!level.addFreshEntity(interaction)) {
      display.discard();
      return false;
    }

    // Add to async physics thread
    space.getWorkerThread().execute(() -> space.addCollisionObject(rigidBody));
    return true;
  }

  private static Display.BlockDisplay createBlockDisplay(Vec3 pos, BlockState state, Level level,
      @Nullable String name) {
    var display = new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, level);
    display.setPos(pos.x(), pos.y(), pos.z());
    display.setOldPosAndRot();

    var blockAccessor = (BlockDisplayMixin) (Object) display;
    var displayAccessor = (DisplayAccessor) (Object) display;

    display.getEntityData().set(blockAccessor.getDataBlockStateId(), state);
    display.getEntityData().set(displayAccessor.getDataPosRotInterpolationDurationId(), DISPLAY_INTERPOLATION_TICKS);
    display.getEntityData().set(displayAccessor.getDataTransformationInterpolationDurationId(),
        DISPLAY_INTERPOLATION_TICKS);

    if (name != null && !name.isEmpty()) {
      display.setCustomName(Component.literal(name));
      display.setCustomNameVisible(false);
    }

    return display;
  }

  private static Interaction createInteractionEntity(Vec3 pos, Level level, EntityRigidBody rigidBody) {
    var interaction = new Interaction(EntityTypes.INTERACTION, level);
    var accessor = (InteractionAccessor) (Object) interaction;

    interaction.getEntityData().set(accessor.getDataWidthId(), 1.0f);
    interaction.getEntityData().set(accessor.getDataHeightId(), 1.0f);
    interaction.setPos(pos.x() + 0.5, pos.y(), pos.z() + 0.5);

    ((PhysicsInteractionAccessor) interaction).physics$setRigidBody(rigidBody);
    return interaction;
  }

  private static MinecraftShape createCollisionShape(Level level, Vec3 pos, BlockState state,
      Display.BlockDisplay display) {
    BlockPos blockPos = BlockPos.containing(pos);
    var collisionShape = state.getCollisionShape(level, blockPos);

    if (collisionShape.isEmpty()) {
      return MinecraftShape.convex(display.getBoundingBox());
    }

    AABB aabb = collisionShape.bounds();
    boolean isFullCube = Math.abs(aabb.minX) < FULL_CUBE_EPSILON &&
        Math.abs(aabb.minY) < FULL_CUBE_EPSILON &&
        Math.abs(aabb.minZ) < FULL_CUBE_EPSILON &&
        Math.abs(aabb.maxX - 1.0) < FULL_CUBE_EPSILON &&
        Math.abs(aabb.maxY - 1.0) < FULL_CUBE_EPSILON &&
        Math.abs(aabb.maxZ - 1.0) < FULL_CUBE_EPSILON;

    return isFullCube ? MinecraftShape.box(aabb) : MinecraftShape.convex(collisionShape);
  }

  private static <T> @Nullable T getArgumentOrNull(CommandContext<CommandSourceStack> ctx, String name,
      Class<T> clazz) {
    try {
      return ctx.getArgument(name, clazz);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static BlockState getArgumentOrDefault(CommandContext<CommandSourceStack> ctx, String name,
      BlockState fallback) {
    try {
      return BlockStateArgument.getBlock(ctx, name).getState();
    } catch (IllegalArgumentException e) {
      return fallback;
    }
  }
}