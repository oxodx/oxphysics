package nl.oxod.oxphysics.bullet.collision.space.generator;

import com.jme3.math.Vector3f;

import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import nl.oxod.oxphysics.bullet.collision.body.EntityRigidBody;
import nl.oxod.oxphysics.bullet.collision.space.MinecraftSpace;
import nl.oxod.oxphysics.bullet.math.Convert;

public class EntityCollisionGenerator {
  private static final float WALKING_PUSH_IMPULSE = 0.05f;
  private static final double PLAYER_SEPARATION_STRENGTH = 0.5;

  public static void step(MinecraftSpace space) {
    for (var rigidBody : space.getRigidBodiesByClass(EntityRigidBody.class)) {
      if (rigidBody.getElement().cast() instanceof Display.BlockDisplay) {
        collideWithPlayers(rigidBody, space);
        continue;
      }

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

  private static void collideWithPlayers(EntityRigidBody rigidBody, MinecraftSpace space) {
    final var blockBox = rigidBody.getCurrentMinecraftBoundingBox();
    if (blockBox.getXsize() <= 0.0 || blockBox.getYsize() <= 0.0 || blockBox.getZsize() <= 0.0) {
      return;
    }

    // A small skin makes the response begin just before the player visually
    // enters the block, instead of waiting for a deep overlap.
    final var contactBox = blockBox.inflate(0.02);
    for (Entity entity : space.getWorkerThread().getEntitySupplier().getInsideOf(rigidBody, contactBox)) {
      if (!(entity instanceof Player player)) {
        continue;
      }

      final AABB playerBox = player.getBoundingBox();
      if (!playerBox.intersects(contactBox)) {
        continue;
      }

      final double overlapX = Math.min(playerBox.maxX, contactBox.maxX) - Math.max(playerBox.minX, contactBox.minX);
      final double overlapZ = Math.min(playerBox.maxZ, contactBox.maxZ) - Math.max(playerBox.minZ, contactBox.minZ);
      if (overlapX <= 0.0 || overlapZ <= 0.0) {
        continue;
      }

      final double playerCenterX = (playerBox.minX + playerBox.maxX) * 0.5;
      final double playerCenterZ = (playerBox.minZ + playerBox.maxZ) * 0.5;
      final double blockCenterX = (contactBox.minX + contactBox.maxX) * 0.5;
      final double blockCenterZ = (contactBox.minZ + contactBox.maxZ) * 0.5;
      final Vector3f direction;

      if (overlapX < overlapZ) {
        direction = new Vector3f(playerCenterX >= blockCenterX ? 1.0f : -1.0f, 0.0f, 0.0f);
      } else {
        direction = new Vector3f(0.0f, 0.0f, playerCenterZ >= blockCenterZ ? 1.0f : -1.0f);
      }

      player.push(direction.x * PLAYER_SEPARATION_STRENGTH, 0.0, direction.z * PLAYER_SEPARATION_STRENGTH);
      rigidBody.applyCentralImpulse(direction.mult(-rigidBody.getMass() * WALKING_PUSH_IMPULSE));
    }
  }
}
