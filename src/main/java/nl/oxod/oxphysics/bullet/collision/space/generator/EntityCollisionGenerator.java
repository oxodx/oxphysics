package nl.oxod.oxphysics.bullet.collision.space.generator;

import com.jme3.math.Vector3f;

import nl.oxod.oxphysics.bullet.collision.body.EntityRigidBody;
import nl.oxod.oxphysics.bullet.collision.space.MinecraftSpace;
import nl.oxod.oxphysics.bullet.math.Convert;

public class EntityCollisionGenerator {
  public static void step(MinecraftSpace space) {
    for (var rigidBody : space.getRigidBodiesByClass(EntityRigidBody.class)) {
      if (rigidBody.getElement().skipVanillaEntityCollisions()) {
        continue;
      }

      final var box = rigidBody.getCurrentBoundingBox();
      final var location = rigidBody.getPhysicsLocation(new Vector3f()).subtract(new Vector3f(0, -box.getYExtent(), 0));
      final var mass = rigidBody.getMass();

      final var vanillaBox = rigidBody.getCurrentMinecraftBoundingBox();

      for (var entity : space.getWorkerThread().getEntitySupplier().getInsideOf(rigidBody, vanillaBox)) {
        final var entityPos = Convert.toBullet(entity.position().add(0, entity.getBoundingBox().getYsize(), 0));
        final var normal = location.subtract(entityPos).multLocal(new Vector3f(1, 0, 1)).normalize();

        final var intersection = entity.getBoundingBox().intersect(vanillaBox);
        final var force = normal.clone()
            .multLocal((float) intersection.getSize() / (float) vanillaBox.getSize())
            .multLocal(mass)
            .multLocal(new Vector3f(1, 0, 1));
        rigidBody.applyCentralImpulse(force);
      }
    }
  }
}
