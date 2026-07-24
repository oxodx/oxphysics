package nl.oxod.oxphysics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Interaction;

@Mixin(Interaction.class)
public interface InteractionAccessor {
  @Accessor("DATA_WIDTH_ID")
  EntityDataAccessor<Float> getDataWidthId();

  @Accessor("DATA_HEIGHT_ID")
  EntityDataAccessor<Float> getDataHeightId();
}
