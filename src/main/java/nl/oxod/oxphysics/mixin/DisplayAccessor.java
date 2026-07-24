package nl.oxod.oxphysics.mixin;

import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Display;

@Mixin(Display.class)
public interface DisplayAccessor {
  @Accessor("DATA_POS_ROT_INTERPOLATION_DURATION_ID")
  EntityDataAccessor<Integer> getDataPosRotInterpolationDurationId();

  @Accessor("DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID")
  EntityDataAccessor<Integer> getDataTransformationInterpolationDurationId();

  @Accessor("DATA_LEFT_ROTATION_ID")
  EntityDataAccessor<Quaternionfc> getDataLeftRotationId();
}
