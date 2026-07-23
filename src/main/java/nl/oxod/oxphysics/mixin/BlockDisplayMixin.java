package nl.oxod.oxphysics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(Display.BlockDisplay.class)
public interface BlockDisplayMixin {
  @Accessor("DATA_BLOCK_STATE_ID")
  EntityDataAccessor<BlockState> getDataBlockStateId();
}
