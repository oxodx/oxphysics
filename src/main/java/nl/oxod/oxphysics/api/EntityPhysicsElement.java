package nl.oxod.oxphysics.api;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import nl.oxod.oxphysics.bullet.collision.body.EntityRigidBody;
import nl.oxod.oxphysics.bullet.collision.body.shape.MinecraftShape;

/**
 * Use this interface to create a physics entity.
 * 
 * @see PhysicsElement
 */
public interface EntityPhysicsElement extends PhysicsElement<Entity> {
  static boolean is(Entity entity) {
    return entity instanceof EntityPhysicsElement element && element.getRigidBody() != null;
  }

  static EntityPhysicsElement get(Entity entity) {
    return (EntityPhysicsElement) entity;
  }

  @Override
  @Nullable
  EntityRigidBody getRigidBody();

  @Override
  default MinecraftShape.Convex createShape() {
    final var box = cast().getBoundingBox();
    return MinecraftShape.convex(box.contract(box.getXsize() * 0.25, box.getYsize() * 0.25, box.getZsize() * 0.25));
  }

  default boolean skipVanillaEntityCollisions() {
    return false;
  }
}
