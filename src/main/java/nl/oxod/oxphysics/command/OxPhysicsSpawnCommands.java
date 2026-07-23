package nl.oxod.oxphysics.command;

import org.jetbrains.annotations.Nullable;

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

    // Explicit tree building without intermediate helper methods
    oxPhysicsBuilder.then(
        Commands.literal("spawn")
            .then(Commands.literal("block")
                // 1. /oxphysics spawn block (default block, no name)
                .executes(ctx -> safeExecute(ctx, DEFAULT_SPAWN_BLOCKSTATE, null))

                // 2. /oxphysics spawn block <block>
                .then(Commands.argument("block", BlockStateArgument.block(buildContext))
                    .executes(ctx -> {
                      BlockState state = BlockStateArgument.getBlock(ctx, "block").getState();
                      return safeExecute(ctx, state, null);
                    })

                    // 3. /oxphysics spawn block <block> <name>
                    .then(Commands.argument("name", StringArgumentType.string())
                        .executes(ctx -> {
                          BlockState state = BlockStateArgument.getBlock(ctx, "block").getState();
                          String name = StringArgumentType.getString(ctx, "name");
                          return safeExecute(ctx, state, name);
                        })))));
  }

  /**
   * Safe execution wrapper to guarantee any runtime/parsing exceptions are
   * trapped.
   */
  private static int safeExecute(final CommandContext<CommandSourceStack> ctx,
      final BlockState material, final @Nullable String name) {
    try {
      return spawnBlock(ctx, material, name);
    } catch (Throwable t) {
      t.printStackTrace();
      ctx.getSource().sendFailure(Component.literal("§cOxPhysics Command Execution Failed: " + t.getMessage()));
      return 0;
    }
  }

  private static int spawnBlock(final CommandContext<CommandSourceStack> ctx,
      final BlockState material, final @Nullable String name) throws CommandSyntaxException {
    final CommandSourceStack source = ctx.getSource();
    final Vec3 playerPos = source.getPosition();
    final Level level = source.getLevel();

    var display = new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, level);

    double targetX = playerPos.x() + 0.5;
    double targetY = playerPos.y() + 0.5;
    double targetZ = playerPos.z() + 0.5;
    display.setPos(targetX, targetY, targetZ);
    display.setOldPosAndRot();

    var blockAccessor = (BlockDisplayMixin) (Object) display;
    var displayAccessor = (DisplayAccessor) (Object) display;

    display.getEntityData().set(blockAccessor.getDataBlockStateId(), material);
    display.getEntityData().set(displayAccessor.getDataPosRotInterpolationDurationId(), 1);

    BlockPos blockPos = BlockPos.containing(targetX, targetY, targetZ);
    var collisionShape = material.getCollisionShape(level, blockPos);
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
      source.sendFailure(Component.literal("§cOxPhysics Error: Physics space for level is null."));
      return 0;
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

    boolean success = level.addFreshEntity(display);
    if (!success) {
      source.sendFailure(Component.literal("§cOxPhysics Error: Could not add entity to world."));
      return 0;
    }

    source.sendSuccess(() -> Component.literal("Spawned block"), false);
    return 1;
  }
}
