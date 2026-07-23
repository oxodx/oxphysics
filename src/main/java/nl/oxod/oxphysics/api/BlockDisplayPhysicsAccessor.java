package nl.oxod.oxphysics.api;

import org.jetbrains.annotations.Nullable;

import nl.oxod.oxphysics.bullet.collision.body.EntityRigidBody;

public interface BlockDisplayPhysicsAccessor {
  void physics$setRigidBody(@Nullable EntityRigidBody rigidBody);
}
