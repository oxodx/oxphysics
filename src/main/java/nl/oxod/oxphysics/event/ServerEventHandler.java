package nl.oxod.oxphysics.event;

import org.jetbrains.annotations.Nullable;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display.BlockDisplay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import nl.oxod.oxphysics.api.BlockDisplayPhysicsAccessor;
import nl.oxod.oxphysics.api.EntityPhysicsElement;
import nl.oxod.oxphysics.api.event.ServerEvents;
import nl.oxod.oxphysics.api.event.collision.PhysicsSpaceEvents;
import nl.oxod.oxphysics.bullet.collision.body.ElementRigidBody;
import nl.oxod.oxphysics.bullet.collision.body.EntityRigidBody;
import nl.oxod.oxphysics.bullet.collision.body.shape.MinecraftShape;
import nl.oxod.oxphysics.bullet.collision.space.MinecraftSpace;
import nl.oxod.oxphysics.bullet.collision.space.generator.EntityCollisionGenerator;
import nl.oxod.oxphysics.bullet.collision.space.generator.PressureGenerator;
import nl.oxod.oxphysics.bullet.collision.space.generator.TerrainGenerator;
import nl.oxod.oxphysics.bullet.collision.space.storage.SpaceStorage;
import nl.oxod.oxphysics.bullet.collision.space.supplier.entity.ServerEntitySupplier;
import nl.oxod.oxphysics.bullet.collision.space.supplier.level.ServerLevelSupplier;
import nl.oxod.oxphysics.bullet.math.Convert;
import nl.oxod.oxphysics.bullet.thread.PhysicsThread;
import nl.oxod.oxphysics.mixin.BlockDisplayMixin;
import nl.oxod.oxphysics.mixin.DisplayAccessor;
import nl.oxod.oxphysics.mixin.InteractionAccessor;

public final class ServerEventHandler {
  private static final Vector3f BLOCK_DISPLAY_CENTER_OFFSET = new Vector3f(0.5f, 0.5f, 0.5f);
  private static final float BLOCK_DISPLAY_FLOOR_CLEARANCE = 0.005f;
  private static final float SHAPE_MARGIN = 1E-4f;
  private static final double FULL_CUBE_EPSILON = 1E-5;

  private static PhysicsThread thread;

  public static PhysicsThread getThread() {
    return thread;
  }

  public static void register() {
    // Physics Space Events
    PhysicsSpaceEvents.STEP.register(PressureGenerator::step);
    PhysicsSpaceEvents.STEP.register(TerrainGenerator::step);
    PhysicsSpaceEvents.ELEMENT_ADDED.register(ServerEventHandler::onElementAddedToSpace);

    // Server Lifecycle Events
    ServerLifecycleEvents.SERVER_STARTING.register(ServerEventHandler::onServerStart);
    ServerLifecycleEvents.SERVER_STOPPING.register(ServerEventHandler::onServerStop);
    ServerTickEvents.END_SERVER_TICK.register(ServerEventHandler::onServerTick);

    // Level Lifecycle Events
    ServerLevelEvents.LOAD.register(ServerEventHandler::onLevelLoad);
    ServerTickEvents.START_LEVEL_TICK.register(ServerEventHandler::onStartLevelTick);
    ServerTickEvents.START_LEVEL_TICK.register(ServerEventHandler::onEntityStartLevelTick);
    ServerEvents.Block.BLOCK_UPDATE.register(ServerEventHandler::onBlockUpdate);

    // Entity Lifecycle Events
    ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> handleEntityTrackingStart(entity));
    EntityTrackingEvents.START_TRACKING.register((entity, player) -> handleEntityTrackingStart(entity));
    EntityTrackingEvents.STOP_TRACKING.register(ServerEventHandler::onStopTrackingEntity);
  }

  public static void onServerStart(MinecraftServer server) {
    thread = new PhysicsThread(
        server,
        Thread.currentThread(),
        new ServerLevelSupplier(server),
        new ServerEntitySupplier(),
        "Server Physics Thread");
  }

  public static void onServerStop(MinecraftServer server) {
    if (thread != null) {
      thread.destroy();
    }
  }

  public static void onServerTick(MinecraftServer server) {
    if (thread != null && thread.throwable != null) {
      throw new RuntimeException(thread.throwable);
    }
  }

  public static void onLevelLoad(MinecraftServer server, ServerLevel level) {
    var space = new MinecraftSpace(thread, level);
    ((SpaceStorage) level).setSpace(space);
    PhysicsSpaceEvents.INIT.invoker().onInit(space);
  }

  public static void onStartLevelTick(Level level) {
    MinecraftSpace.get(level).step();
  }

  public static void onBlockUpdate(Level level, BlockState blockState, BlockPos blockPos) {
    MinecraftSpace.getOptional(level).ifPresent(space -> space.doBlockUpdate(blockPos));
  }

  public static void onElementAddedToSpace(MinecraftSpace space, ElementRigidBody rigidBody) {
    if (rigidBody instanceof EntityRigidBody entityBody) {
      var pos = entityBody.getElement().cast().position();
      var location = Convert.toBullet(pos);

      // Block displays render from lower corner; Bullet shapes center on body
      // location
      if (entityBody.getElement().cast() instanceof BlockDisplay) {
        location.add(BLOCK_DISPLAY_CENTER_OFFSET);
      }

      entityBody.setPhysicsLocation(location);
    }
  }

  private static void handleEntityTrackingStart(Entity entity) {
    if (entity instanceof BlockDisplayPhysicsAccessor accessor
        && accessor.physics$isActive()
        && !EntityPhysicsElement.is(entity)) {
      setupPhysicsForBlockDisplay(entity);
    }

    if (EntityPhysicsElement.is(entity) && !PlayerLookup.tracking(entity).isEmpty()) {
      var space = MinecraftSpace.get(entity.level());
      space.getWorkerThread().execute(() -> space.addCollisionObject(EntityPhysicsElement.get(entity).getRigidBody()));
    }
  }

  public static void onStopTrackingEntity(Entity entity, ServerPlayer player) {
    if (EntityPhysicsElement.is(entity) && PlayerLookup.tracking(entity).isEmpty()) {
      var space = MinecraftSpace.get(entity.level());
      space.getWorkerThread()
          .execute(() -> space.removeCollisionObject(EntityPhysicsElement.get(entity).getRigidBody()));
    }
  }

  public static void onEntityStartLevelTick(Level level) {
    var space = MinecraftSpace.get(level);
    EntityCollisionGenerator.step(space);

    var tempVector = new Vector3f();
    var tempRotation = new Quaternion();

    for (var rigidBody : space.getRigidBodiesByClass(EntityRigidBody.class)) {
      var location = rigidBody.getFrame().getLocation(tempVector, 1.0f);
      var rotation = rigidBody.getFrame().getRotation(tempRotation, 1.0f);
      Entity element = rigidBody.getElement().cast();

      if (element instanceof BlockDisplay display) {
        // Keep the display's lower corner aligned with the center-based collision shape
        var offset = rotation.toRotationMatrix().mult(BLOCK_DISPLAY_CENTER_OFFSET, new Vector3f());
        element.absSnapTo(
            location.x - offset.x,
            location.y - offset.y + BLOCK_DISPLAY_FLOOR_CLEARANCE,
            location.z - offset.z);

        var displayAccessor = (DisplayAccessor) (Object) display;
        display.getEntityData().set(displayAccessor.getDataLeftRotationId(), Convert.toMinecraft(rotation));

        var physicsAccessor = (BlockDisplayPhysicsAccessor) display;
        updateInteraction(physicsAccessor.physics$getInteractionEntity(), rigidBody.getCurrentMinecraftBoundingBox());
      } else {
        element.absSnapTo(location.x, location.y, location.z);
      }
    }
  }

  private static void setupPhysicsForBlockDisplay(Entity entity) {
    var accessor = (BlockDisplayPhysicsAccessor) entity;
    var display = (BlockDisplay) entity;
    var blockAccessor = (BlockDisplayMixin) (Object) display;
    var blockState = display.getEntityData().get(blockAccessor.getDataBlockStateId());

    MinecraftShape shape = createCollisionShape(entity.level(), entity.blockPosition(), blockState, display);
    shape.setMargin(SHAPE_MARGIN);

    var space = MinecraftSpace.get(entity.level());
    var rigidBody = new EntityRigidBody((EntityPhysicsElement) entity, space, shape);
    rigidBody.setCcdMotionThreshold(0.01f);
    rigidBody.setCcdSweptSphereRadius(0.4f);
    rigidBody.setContactProcessingThreshold(0.0f);

    accessor.physics$setRigidBody(rigidBody);
  }

  private static MinecraftShape createCollisionShape(Level level, BlockPos pos, BlockState state,
      BlockDisplay display) {
    var collisionShape = state.getCollisionShape(level, pos);
    if (collisionShape.isEmpty()) {
      return MinecraftShape.convex(display.getBoundingBox());
    }

    AABB aabb = collisionShape.bounds();
    boolean isFullCube = Math.abs(aabb.minX) < FULL_CUBE_EPSILON &&
        Math.abs(aabb.minY) < FULL_CUBE_EPSILON &&
        Math.abs(aabb.minZ) < FULL_CUBE_EPSILON &&
        Math.abs(aabb.maxX - 1.0) < FULL_CUBE_EPSILON &&
        Math.abs(aabb.maxY - 1.0) < FULL_CUBE_EPSILON &&
        Math.abs(aabb.maxZ - 1.0) < FULL_CUBE_EPSILON;

    return isFullCube ? MinecraftShape.box(aabb) : MinecraftShape.convex(collisionShape);
  }

  private static void updateInteraction(@Nullable Interaction interaction, AABB bounds) {
    if (interaction == null || interaction.isRemoved()) {
      return;
    }

    var interactionAccessor = (InteractionAccessor) (Object) interaction;
    interaction.getEntityData().set(interactionAccessor.getDataWidthId(),
        (float) Math.max(bounds.getXsize(), bounds.getZsize()));
    interaction.getEntityData().set(interactionAccessor.getDataHeightId(), (float) bounds.getYsize());
    interaction.absSnapTo(bounds.getCenter().x, bounds.minY, bounds.getCenter().z);
  }
}
