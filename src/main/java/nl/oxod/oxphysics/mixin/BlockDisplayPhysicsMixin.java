package nl.oxod.oxphysics.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.oxod.oxphysics.api.BlockDisplayPhysicsAccessor;
import nl.oxod.oxphysics.api.EntityPhysicsElement;
import nl.oxod.oxphysics.bullet.collision.body.EntityRigidBody;

@Mixin(Display.BlockDisplay.class)
public abstract class BlockDisplayPhysicsMixin extends Entity implements EntityPhysicsElement, BlockDisplayPhysicsAccessor {
  private BlockDisplayPhysicsMixin(EntityType<?> type, Level level) {
    super(type, level);
  }

  @Unique
  @Nullable
  private EntityRigidBody physics$rigidBody;

  @Unique
  @Nullable
  private Interaction physics$interactionEntity;

  @Unique
  private boolean physics$active;

  @Override
  @Nullable
  public EntityRigidBody getRigidBody() {
    return this.physics$rigidBody;
  }

  public void physics$setRigidBody(@Nullable EntityRigidBody rigidBody) {
    this.physics$rigidBody = rigidBody;
  }

  @Override
  @Nullable
  public Interaction physics$getInteractionEntity() {
    return this.physics$interactionEntity;
  }

  @Override
  public void physics$setInteractionEntity(@Nullable Interaction interaction) {
    this.physics$interactionEntity = interaction;
  }

  @Override
  public boolean physics$isActive() {
    return this.physics$active;
  }

  @Override
  public void physics$setActive(boolean active) {
    this.physics$active = active;
  }

  @Override
  public boolean skipVanillaEntityCollisions() {
    return true;
  }

  @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
  private void oxphysics$saveData(ValueOutput output, CallbackInfo ci) {
    if (this.physics$active) {
      output.putBoolean("oxphysics:physics_active", true);
    }
  }

  @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
  private void oxphysics$loadData(ValueInput input, CallbackInfo ci) {
    this.physics$active = input.getBooleanOr("oxphysics:physics_active", false);
  }
}
