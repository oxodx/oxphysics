package nl.oxod.oxphysics.api;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Interaction;
import net.minecraft.world.level.block.state.BlockState;
import nl.oxod.oxphysics.bullet.collision.body.EntityRigidBody;

public interface BlockDisplayPhysicsAccessor {
  void physics$setRigidBody(@Nullable EntityRigidBody rigidBody);

  @Nullable
  Interaction physics$getInteractionEntity();

  void physics$setInteractionEntity(@Nullable Interaction interaction);

  boolean physics$isActive();

  void physics$setActive(boolean active);
}
