package nl.oxod.oxphysics.command;

import com.jme3.math.Vector3f;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.state.BlockState;
import nl.oxod.oxphysics.bullet.collision.space.MinecraftSpace;
import nl.oxod.oxphysics.bullet.math.Convert;
import nl.oxod.oxphysics.entity.PhysicsBlockEntity;

public final class OxPhysicsCommands {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context,
      Commands.CommandSelection selection) {
    dispatcher.register(Commands.literal("oxphysics")
        .then(Commands.literal("spawn")
            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                .then(Commands.argument("block", BlockStateArgument.block(context))
                    .executes(ctx -> {
                      var pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                      var blockInput = BlockStateArgument.getBlock(ctx, "block");
                      var blockState = blockInput.getState();
                      var level = ctx.getSource().getLevel();

                      spawnPhysicsBlock(level, pos, blockState);
                      ctx.getSource().sendSuccess(
                          () -> Component.literal("Spawned physics block at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()),
                          true);
                      return 1;
                    })))));
  }

  private static void spawnPhysicsBlock(ServerLevel level, BlockPos pos, BlockState blockState) {
    var physicsEntity = new PhysicsBlockEntity(level, pos, blockState);
    level.addFreshEntity(physicsEntity);

    var display = new Display.BlockDisplay(net.minecraft.world.entity.EntityTypes.BLOCK_DISPLAY, level);
    display.setPos(pos.getX(), pos.getY(), pos.getZ());
    physicsEntity.setBlockDisplay(display);
    level.addFreshEntity(display);

    var space = MinecraftSpace.get(level);
    space.getWorkerThread().execute(() -> {
      space.addCollisionObject(physicsEntity.getRigidBody());
    });
  }
}
