package nl.oxod.oxphysics.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import nl.oxod.oxphysics.api.EntityPhysicsElement;
import nl.oxod.oxphysics.bullet.collision.body.EntityRigidBody;
import nl.oxod.oxphysics.bullet.collision.body.shape.MinecraftShape;

@Mixin(Display.BlockDisplay.class)
public abstract class BlockDisplayPhysicsMixin extends Entity implements EntityPhysicsElement {
  private BlockDisplayPhysicsMixin(EntityType<?> type, Level level) {
    super(type, level);
  }

  @Unique
  @Nullable
  private EntityRigidBody physics$rigidBody;

  @Unique
  @Nullable
  private BlockState physics$blockState;

  @Override
  @Nullable
  public EntityRigidBody getRigidBody() {
    return this.physics$rigidBody;
  }

  public void physics$setRigidBody(@Nullable EntityRigidBody rigidBody) {
    this.physics$rigidBody = rigidBody;
  }

  public void physics$setBlockState(BlockState blockState) {
    this.physics$blockState = blockState;
  }

  @Override
  public MinecraftShape.Convex createShape() {
    if (this.physics$blockState != null && !this.physics$blockState.isAir()) {
      return MinecraftShape.convex(this.physics$blockState.getCollisionShape(this.level(), this.blockPosition()));
    }
    return MinecraftShape.convex(this.getBoundingBox());
  }

  @Override
  public boolean skipVanillaEntityCollisions() {
    return true;
  }
}
