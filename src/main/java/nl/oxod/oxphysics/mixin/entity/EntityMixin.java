package nl.oxod.oxphysics.mixin.entity;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.oxod.oxphysics.api.EntityPhysicsElement;
import nl.oxod.oxphysics.bullet.math.Convert;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Basic changes for {@link EntityPhysicsElement}s.
 * ({@link CallbackInfo#cancel()} go brrr)
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
  @Shadow
  protected abstract void onBelowWorld();

  @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
  public void pushAwayFrom(Entity entity, CallbackInfo info) {
    if (EntityPhysicsElement.is((Entity) (Object) this) && EntityPhysicsElement.is(entity)) {
      info.cancel();
    }
  }

  @Inject(method = "move", at = @At("HEAD"), cancellable = true)
  public void move(CallbackInfo info) {
    if (EntityPhysicsElement.is((Entity) (Object) this)) {
      info.cancel();
    }
  }

  @Inject(method = "saveWithoutId", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;addAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueOutput;)V"))
  public void saveWithoutId(ValueOutput view, CallbackInfo ci) {
    if (EntityPhysicsElement.is((Entity) (Object) this)) {
      var rigidBody = EntityPhysicsElement.get((Entity) (Object) this).getRigidBody();
      view.store("orientation", ExtraCodecs.QUATERNIONF_COMPONENTS,
          Convert.toMinecraft(rigidBody.getPhysicsRotation(new Quaternion())));
      view.store("linearVelocity", ExtraCodecs.VECTOR3F,
          Convert.toMinecraft(rigidBody.getLinearVelocity(new Vector3f())));
      view.store("angularVelocity", ExtraCodecs.VECTOR3F,
          Convert.toMinecraft(rigidBody.getAngularVelocity(new Vector3f())));
      view.putFloat("mass", rigidBody.getMass());
      view.putFloat("dragCoefficient", rigidBody.getDragCoefficient());
      view.putFloat("friction", rigidBody.getFriction());
      view.putFloat("restitution", rigidBody.getRestitution());
      view.putBoolean("terrainLoadingEnabled", rigidBody.terrainLoadingEnabled());
      view.putInt("buoyancyType", rigidBody.getBuoyancyType().ordinal());
      view.putInt("dragType", rigidBody.getDragType().ordinal());
    }
  }

  @Inject(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V"))
  public void load(ValueInput view, CallbackInfo ci) {
    if (EntityPhysicsElement.is((Entity) (Object) this)) {
      EntityPhysicsElement.get((Entity) (Object) this).getRigidBody().readTagInfo(view);
    }
  }
}
