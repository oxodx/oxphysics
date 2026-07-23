package nl.oxod.oxphysics.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.Level;
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

  @Override
  @Nullable
  public EntityRigidBody getRigidBody() {
    return this.physics$rigidBody;
  }

  public void physics$setRigidBody(@Nullable EntityRigidBody rigidBody) {
    this.physics$rigidBody = rigidBody;
  }

  @Override
  public boolean skipVanillaEntityCollisions() {
    return true;
  }
}
