package nl.oxod.oxphysics.command;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.oxod.oxphysics.OxPhysics;
import nl.oxod.oxphysics.api.BlockDisplayPhysicsAccessor;
import nl.oxod.oxphysics.api.EntityPhysicsElement;
import nl.oxod.oxphysics.bullet.collision.body.EntityRigidBody;
import nl.oxod.oxphysics.bullet.collision.body.shape.MinecraftShape;
import nl.oxod.oxphysics.bullet.collision.space.MinecraftSpace;
import nl.oxod.oxphysics.mixin.BlockDisplayMixin;
import nl.oxod.oxphysics.mixin.DisplayAccessor;

public class OxPhysicsSpawnCommands {
  private static final BlockState DEFAULT_SPAWN_BLOCKSTATE = Blocks.STONE.defaultBlockState();

  public static void register(final LiteralArgumentBuilder<CommandSourceStack> oxPhysicsBuilder,
      final CommandBuildContext buildContext) {

    oxPhysicsBuilder.then(
        Commands.literal("spawn")
            .then(Commands.literal("block")
                .executes(ctx -> spawnBlock(ctx, DEFAULT_SPAWN_BLOCKSTATE, null))
                .then(Commands.argument("block", BlockStateArgument.block(buildContext))
                    .executes(ctx -> {
                      BlockState state = BlockStateArgument.getBlock(ctx, "block").getState();
                      return spawnBlock(ctx, state, null);
                    })
                    .then(Commands.argument("name", StringArgumentType.string())
                        .executes(ctx -> {
                          BlockState state = BlockStateArgument.getBlock(ctx, "block").getState();
                          String name = StringArgumentType.getString(ctx, "name");
                          return spawnBlock(ctx, state, name);
                        }))))

            .then(Commands.literal("grid")
                .then(Commands.argument("sideLength", IntegerArgumentType.integer(1, 32))
                    .executes(ctx -> spawnGrid(ctx, DEFAULT_SPAWN_BLOCKSTATE, null))
                    .then(Commands.argument("block", BlockStateArgument.block(buildContext))
                        .executes(ctx -> {
                          BlockState state = BlockStateArgument.getBlock(ctx, "block").getState();
                          return spawnGrid(ctx, state, null);
                        })
                        .then(Commands.argument("name", StringArgumentType.string())
                            .executes(ctx -> {
                              BlockState state = BlockStateArgument.getBlock(ctx, "block").getState();
                              String name = StringArgumentType.getString(ctx, "name");
                              return spawnGrid(ctx, state, name);
                            }))))));
  }

  private static boolean spawnPhysicsBlock(Vec3 pos, BlockState blockState, Level level,
      String name) {
    var display = new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, level);

    double targetX = pos.x() + 0.5;
    double targetY = pos.y() + 0.5;
    double targetZ = pos.z() + 0.5;
    display.setPos(targetX, targetY, targetZ);
    display.setOldPosAndRot();

    var blockAccessor = (BlockDisplayMixin) (Object) display;
    var displayAccessor = (DisplayAccessor) (Object) display;

    display.getEntityData().set(blockAccessor.getDataBlockStateId(), blockState);
    display.getEntityData().set(displayAccessor.getDataPosRotInterpolationDurationId(), 1);

    BlockPos blockPos = BlockPos.containing(targetX, targetY, targetZ);
    var collisionShape = blockState.getCollisionShape(level, blockPos);
    MinecraftShape shape;

    if (collisionShape.isEmpty()) {
      shape = MinecraftShape.convex(display.getBoundingBox());
    } else {
      var aabb = collisionShape.bounds();
      boolean isFullCube = Math.abs(aabb.minX) < 1E-5 && Math.abs(aabb.minY) < 1E-5 && Math.abs(aabb.minZ) < 1E-5
          && Math.abs(aabb.maxX - 1.0) < 1E-5 && Math.abs(aabb.maxY - 1.0) < 1E-5 && Math.abs(aabb.maxZ - 1.0) < 1E-5;

      shape = isFullCube ? MinecraftShape.box(aabb) : MinecraftShape.convex(collisionShape);
    }

    shape.setMargin(1E-4f);

    var space = MinecraftSpace.get(level);
    if (space == null) {
      OxPhysics.LOGGER.error("Physics Error: Physics space for level is null.");
      return false;
    }

    var rigidBody = new EntityRigidBody((EntityPhysicsElement) display, space, shape);
    rigidBody.setCcdMotionThreshold(0.01f);
    rigidBody.setCcdSweptSphereRadius(0.4f);
    rigidBody.setContactProcessingThreshold(0.0f);

    var physicsAccessor = (BlockDisplayPhysicsAccessor) (Object) display;
    physicsAccessor.physics$setActive(true);
    physicsAccessor.physics$setRigidBody(rigidBody);

    if (name != null && !name.isEmpty()) {
      display.setCustomName(Component.literal(name));
      display.setCustomNameVisible(false);
    }

    return level.addFreshEntity(display);
  }

  private static int spawnBlock(final CommandContext<CommandSourceStack> ctx,
      final BlockState material, final @Nullable String name) throws CommandSyntaxException {
    final CommandSourceStack source = ctx.getSource();
    final Vec3 playerPos = source.getPosition();
    final Level level = source.getLevel();

    boolean success = spawnPhysicsBlock(playerPos, material, level, name);
    if (!success) {
      source.sendFailure(Component.literal("§cOxPhysics Error: Could not add entity to world."));
      return 0;
    }

    source.sendSuccess(() -> Component.literal("Spawned block"), false);
    return 1;
  }

  private static int spawnGrid(final CommandContext<CommandSourceStack> ctx,
      final BlockState material, final @Nullable String name) throws CommandSyntaxException {
    final CommandSourceStack source = ctx.getSource();
    final Vec3 playerPos = source.getPosition();
    final Level level = source.getLevel();

    final int sideLength = IntegerArgumentType.getInteger(ctx, "sideLength");
    int spawnedCount = 0;

    for (int x = 0; x < sideLength; x++) {
      for (int z = 0; z < sideLength; z++) {
        for (int y = 0; y < sideLength; y++) {
          // Calculate grid offset position
          Vec3 pos = new Vec3(x, y, z).scale(2.1).add(playerPos);

          if (spawnPhysicsBlock(pos, material, level, name)) {
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
}