package nl.oxod.oxphysics.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.world.entity.Interaction;
import nl.oxod.oxphysics.api.PhysicsInteractionAccessor;
import nl.oxod.oxphysics.bullet.collision.body.EntityRigidBody;

@Mixin(Interaction.class)
public class InteractionPhysicsMixin implements PhysicsInteractionAccessor {
  @Unique
  @Nullable
  private EntityRigidBody physics$rigidBody;

  @Override
  @Nullable
  public EntityRigidBody physics$getRigidBody() {
    return this.physics$rigidBody;
  }

  @Override
  public void physics$setRigidBody(@Nullable EntityRigidBody rigidBody) {
    this.physics$rigidBody = rigidBody;
  }
}
