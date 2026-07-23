package nl.oxod.oxphysics.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ServerEvents {
  public static class Block {
    public static final Event<BlockUpdate> BLOCK_UPDATE = EventFactory.createArrayBacked(BlockUpdate.class,
        (events) -> (world, blockState, pos) -> {
          for (var e : events) {
            e.onBlockUpdate(world, blockState, pos);
          }
        });

    @FunctionalInterface
    public interface BlockUpdate {
      void onBlockUpdate(Level level, BlockState blockState, BlockPos blockPos);
    }
  }
}
