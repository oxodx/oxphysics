package nl.oxod.oxphysics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Display;

@Mixin(Display.class)
public interface DisplayAccessor {
  @Accessor("DATA_POS_ROT_INTERPOLATION_DURATION_ID")
  EntityDataAccessor<Integer> getDataPosRotInterpolationDurationId();
}
