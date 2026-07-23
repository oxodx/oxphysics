package nl.oxod.oxphysics.mixin.entity;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import nl.oxod.oxphysics.api.EntityPhysicsElement;
import nl.oxod.oxphysics.bullet.math.Convert;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Allows {@link PhysicsElement} objects to be affected by explosions.
 */
@Mixin(ServerExplosion.class)
public class ExplosionMixin {
  @ModifyArg(method = "hurtEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(Lnet/minecraft/world/phys/Vec3;)V"))
  public Vec3 setVelocity(Vec3 velocity, @Local Entity entity) {
    if (EntityPhysicsElement.is(entity)) {
      var element = EntityPhysicsElement.get(entity);
      element.getRigidBody()
          .applyCentralImpulse(Convert.toBullet(velocity).multLocal(element.getRigidBody().getMass() * 100f));
    }

    return velocity;
  }
}
