package nl.oxod.oxphysics.mixin.entity;

import com.jme3.math.Vector3f;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.player.Player;
import nl.oxod.oxphysics.api.PhysicsInteractionAccessor;
import nl.oxod.oxphysics.bullet.math.Convert;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerPhysicsMixin {
  private static final float PUNCH_IMPULSE = 0.8f;

  @Inject(method = "attack", at = @At("HEAD"))
  private void oxphysics$punchPhysicsBlock(Entity target, CallbackInfo ci) {
    if (!(target instanceof Interaction interaction)) {
      return;
    }

    var rigidBody = ((PhysicsInteractionAccessor) interaction).physics$getRigidBody();
    if (rigidBody == null) {
      return;
    }

    var player = (Player) (Object) this;
    var impulse = Convert.toBullet(player.getLookAngle()).normalize()
        .multLocal(rigidBody.getMass() * PUNCH_IMPULSE);
    rigidBody.getSpace().getWorkerThread().execute(() -> {
      if (rigidBody.isInWorld()) {
        rigidBody.activate();
        rigidBody.applyCentralImpulse(impulse);
      }
    });
  }
}
