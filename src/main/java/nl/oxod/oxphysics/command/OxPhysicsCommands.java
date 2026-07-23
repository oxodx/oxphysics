package nl.oxod.oxphysics.command;

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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import nl.oxod.oxphysics.api.BlockDisplayPhysicsAccessor;
import nl.oxod.oxphysics.api.EntityPhysicsElement;
import nl.oxod.oxphysics.bullet.collision.body.EntityRigidBody;
import nl.oxod.oxphysics.mixin.BlockDisplayMixin;

public final class OxPhysicsCommands {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context,
      Commands.CommandSelection selection) {
    dispatcher.register(Commands.literal("oxphysics")
        .then(Commands.literal("spawn")
            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                .then(Commands.argument("block", BlockStateArgument.block(context))
                    .executes(ctx -> {
                      try {
                        var pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                        var blockInput = BlockStateArgument.getBlock(ctx, "block");
                        var blockState = blockInput.getState();
                        var level = ctx.getSource().getLevel();

                        spawnPhysicsBlock(level, pos, blockState);
                        ctx.getSource().sendSuccess(
                            () -> Component.literal("Spawned physics block at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()),
                            true);
                        return 1;
                      } catch (Exception e) {
                        nl.oxod.oxphysics.OxPhysics.LOGGER.error("Failed to spawn physics block", e);
                        throw new RuntimeException(e);
                      }
                    })))));
  }

  private static void spawnPhysicsBlock(ServerLevel level, BlockPos pos, BlockState blockState) {
    var display = new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, level);
    display.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

    var physicsAccessor = (BlockDisplayPhysicsAccessor) (Object) display;
    physicsAccessor.physics$setBlockState(blockState);

    var rigidBody = new EntityRigidBody((EntityPhysicsElement) display);
    physicsAccessor.physics$setRigidBody(rigidBody);

    var blockAccessor = (BlockDisplayMixin) (Object) display;
    display.getEntityData().set(blockAccessor.getDataBlockStateId(), blockState);

    level.addFreshEntity(display);
  }
}
