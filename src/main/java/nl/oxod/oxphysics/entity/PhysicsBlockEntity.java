package nl.oxod.oxphysics.entity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.oxod.oxphysics.api.EntityPhysicsElement;
import nl.oxod.oxphysics.bullet.collision.body.EntityRigidBody;
import nl.oxod.oxphysics.bullet.collision.body.shape.MinecraftShape;
import nl.oxod.oxphysics.mixin.BlockDisplayMixin;

public class PhysicsBlockEntity extends Entity implements EntityPhysicsElement {
  private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE = SynchedEntityData.defineId(
      PhysicsBlockEntity.class, EntityDataSerializers.BLOCK_STATE);

  @Nullable
  private EntityRigidBody rigidBody;

  @Nullable
  private Display.BlockDisplay blockDisplay;

  public PhysicsBlockEntity(EntityType<PhysicsBlockEntity> entityType, Level level) {
    super(entityType, level);
  }

  public PhysicsBlockEntity(Level level, BlockPos pos, BlockState blockState) {
    this(OxPhysicsEntities.PHYSICS_BLOCK, level);
    this.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    this.entityData.set(DATA_BLOCK_STATE, blockState);
    this.rigidBody = new EntityRigidBody(this);
  }

  public void setBlockDisplay(Display.BlockDisplay blockDisplay) {
    this.blockDisplay = blockDisplay;
    var accessor = (BlockDisplayMixin) (Object) blockDisplay;
    blockDisplay.getEntityData().set(accessor.getDataBlockStateId(), this.getBlockState());
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {
    builder.define(DATA_BLOCK_STATE, Blocks.STONE.defaultBlockState());
  }

  @Override
  @Nullable
  public EntityRigidBody getRigidBody() {
    return this.rigidBody;
  }

  @Override
  public MinecraftShape.Convex createShape() {
    BlockState blockState = this.getBlockState();
    if (blockState.isAir()) {
      return MinecraftShape.convex(this.getBoundingBox());
    }
    return MinecraftShape.convex(blockState.getCollisionShape(this.level(), this.blockPosition()));
  }

  public BlockState getBlockState() {
    return this.entityData.get(DATA_BLOCK_STATE);
  }

  @Override
  public void tick() {
    super.tick();
    if (this.blockDisplay != null && !this.isRemoved()) {
      this.blockDisplay.setPos(this.getX() - 0.5, this.getY() - 0.5, this.getZ() - 0.5);
    }
  }

  @Override
  protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
    this.entityData.set(DATA_BLOCK_STATE,
        input.read("BlockState", BlockState.CODEC).orElse(Blocks.STONE.defaultBlockState()));
  }

  @Override
  protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
    output.store("BlockState", BlockState.CODEC, this.getBlockState());
  }

  @Override
  public boolean isPickable() {
    return true;
  }

  @Override
  public boolean isAttackable() {
    return true;
  }

  @Override
  public boolean hurtServer(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount) {
    if (this.isInvulnerableToBase(source)) {
      return false;
    }
    this.discard();
    return true;
  }

  @Override
  public void onRemoval(net.minecraft.world.entity.Entity.RemovalReason reason) {
    super.onRemoval(reason);
    if (this.blockDisplay != null && !this.blockDisplay.isRemoved()) {
      this.blockDisplay.discard();
    }
  }

  @Override
  protected net.minecraft.world.entity.Entity.MovementEmission getMovementEmission() {
    return Entity.MovementEmission.NONE;
  }
}
