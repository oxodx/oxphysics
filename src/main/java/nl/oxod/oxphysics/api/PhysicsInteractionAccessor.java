package nl.oxod.oxphysics.api;

import org.jetbrains.annotations.Nullable;

import nl.oxod.oxphysics.bullet.collision.body.EntityRigidBody;

public interface PhysicsInteractionAccessor {
  @Nullable
  EntityRigidBody physics$getRigidBody();

  void physics$setRigidBody(@Nullable EntityRigidBody rigidBody);
}
