package nl.oxod.oxphysics.api;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.block.state.BlockState;
import nl.oxod.oxphysics.bullet.collision.body.EntityRigidBody;

public interface BlockDisplayPhysicsAccessor {
  void physics$setRigidBody(@Nullable EntityRigidBody rigidBody);

  void physics$setBlockState(BlockState blockState);
}
