package nl.oxod.oxphysics.event;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import nl.oxod.oxphysics.command.OxPhysicsCommands;
import nl.oxod.oxphysics.api.BlockDisplayPhysicsAccessor;
import nl.oxod.oxphysics.api.EntityPhysicsElement;
import nl.oxod.oxphysics.api.event.ServerEvents;
import nl.oxod.oxphysics.api.event.collision.PhysicsSpaceEvents;
import nl.oxod.oxphysics.bullet.collision.body.ElementRigidBody;
import nl.oxod.oxphysics.bullet.collision.body.EntityRigidBody;
import nl.oxod.oxphysics.bullet.collision.space.MinecraftSpace;
import nl.oxod.oxphysics.bullet.collision.space.generator.EntityCollisionGenerator;
import nl.oxod.oxphysics.bullet.collision.space.generator.PressureGenerator;
import nl.oxod.oxphysics.bullet.collision.space.generator.TerrainGenerator;
import nl.oxod.oxphysics.bullet.collision.space.storage.SpaceStorage;
import nl.oxod.oxphysics.bullet.collision.space.supplier.entity.ServerEntitySupplier;
import nl.oxod.oxphysics.bullet.collision.space.supplier.level.ServerLevelSupplier;
import nl.oxod.oxphysics.bullet.math.Convert;
import nl.oxod.oxphysics.bullet.thread.PhysicsThread;

public final class ServerEventHandler {
  private static PhysicsThread thread;

  public static PhysicsThread getThread() {
    return thread;
  }

  public static void register() {
    // Rayon Events
    PhysicsSpaceEvents.STEP.register(PressureGenerator::step);
    PhysicsSpaceEvents.STEP.register(TerrainGenerator::step);
    PhysicsSpaceEvents.ELEMENT_ADDED.register(ServerEventHandler::onElementAddedToSpace);

    // Commands
    CommandRegistrationCallback.EVENT.register(OxPhysicsCommands::register);

    // Server Events
    ServerLifecycleEvents.SERVER_STARTING.register(ServerEventHandler::onServerStart);
    ServerLifecycleEvents.SERVER_STOPPING.register(ServerEventHandler::onServerStop);
    ServerTickEvents.END_SERVER_TICK.register(ServerEventHandler::onServerTick);

    // Level Events
    ServerLevelEvents.LOAD.register(ServerEventHandler::onLevelLoad);
    ServerTickEvents.START_LEVEL_TICK.register(ServerEventHandler::onStartLevelTick);
    ServerTickEvents.START_LEVEL_TICK.register(ServerEventHandler::onEntityStartLevelTick);
    ServerEvents.Block.BLOCK_UPDATE.register(ServerEventHandler::onBlockUpdate);

    // Entity Events
    ServerEntityEvents.ENTITY_LOAD.register(ServerEventHandler::onEntityLoad);
    EntityTrackingEvents.START_TRACKING.register(ServerEventHandler::onStartTrackingEntity);
    EntityTrackingEvents.STOP_TRACKING.register(ServerEventHandler::onStopTrackingEntity);
  }

  public static void onBlockUpdate(Level level, BlockState blockState, BlockPos blockPos) {
    MinecraftSpace.getOptional(level).ifPresent(space -> space.doBlockUpdate(blockPos));
  }

  public static void onServerStart(MinecraftServer server) {
    thread = new PhysicsThread(server, Thread.currentThread(), new ServerLevelSupplier(server),
        new ServerEntitySupplier(), "Server Physics Thread");
  }

  public static void onServerStop(MinecraftServer server) {
    thread.destroy();
  }

  public static void onServerTick(MinecraftServer server) {
    if (thread.throwable != null) {
      throw new RuntimeException(thread.throwable);
    }
  }

  public static void onStartLevelTick(Level level) {
    MinecraftSpace.get(level).step();
  }

  public static void onLevelLoad(MinecraftServer server, ServerLevel level) {
    final var space = new MinecraftSpace(thread, level);
    ((SpaceStorage) level).setSpace(space);
    PhysicsSpaceEvents.INIT.invoker().onInit(space);
  }

  public static void onElementAddedToSpace(MinecraftSpace space, ElementRigidBody rigidBody) {
    if (rigidBody instanceof EntityRigidBody entityBody) {
      final var pos = entityBody.getElement().cast().position();
      entityBody.setPhysicsLocation(Convert.toBullet(pos));
    }
  }

  public static void onEntityLoad(Entity entity, Level world) {
    if (entity instanceof BlockDisplayPhysicsAccessor accessor && accessor.physics$isActive()
        && !EntityPhysicsElement.is(entity)) {
      setupPhysicsForBlockDisplay(entity);
    }
    if (EntityPhysicsElement.is(entity) && !PlayerLookup.tracking(entity).isEmpty()) {
      var space = MinecraftSpace.get(entity.level());
      space.getWorkerThread().execute(() -> space.addCollisionObject(EntityPhysicsElement.get(entity).getRigidBody()));
    }
  }

  public static void onStartTrackingEntity(Entity entity, ServerPlayer player) {
    if (entity instanceof BlockDisplayPhysicsAccessor accessor && accessor.physics$isActive()
        && !EntityPhysicsElement.is(entity)) {
      setupPhysicsForBlockDisplay(entity);
    }
    if (EntityPhysicsElement.is(entity)) {
      var space = MinecraftSpace.get(entity.level());
      space.getWorkerThread().execute(() -> space.addCollisionObject(EntityPhysicsElement.get(entity).getRigidBody()));
    }
  }

  private static void setupPhysicsForBlockDisplay(Entity entity) {
    var accessor = (BlockDisplayPhysicsAccessor) entity;
    var display = (net.minecraft.world.entity.Display.BlockDisplay) entity;
    var blockAccessor = (nl.oxod.oxphysics.mixin.BlockDisplayMixin) (Object) display;
    var blockState = display.getEntityData().get(blockAccessor.getDataBlockStateId());

    var collisionShape = blockState.getCollisionShape(entity.level(), entity.blockPosition());
    nl.oxod.oxphysics.bullet.collision.body.shape.MinecraftShape shape;
    var aabb = collisionShape.bounds();
    boolean isFullCube = !collisionShape.isEmpty()
        && Math.abs(aabb.minX) < 1E-5 && Math.abs(aabb.minY) < 1E-5 && Math.abs(aabb.minZ) < 1E-5
        && Math.abs(aabb.maxX - 1.0) < 1E-5 && Math.abs(aabb.maxY - 1.0) < 1E-5 && Math.abs(aabb.maxZ - 1.0) < 1E-5;

    if (collisionShape.isEmpty()) {
      shape = nl.oxod.oxphysics.bullet.collision.body.shape.MinecraftShape.convex(display.getBoundingBox());
    } else if (isFullCube) {
      shape = nl.oxod.oxphysics.bullet.collision.body.shape.MinecraftShape.box(aabb);
    } else {
      shape = nl.oxod.oxphysics.bullet.collision.body.shape.MinecraftShape.convex(collisionShape);
    }
    shape.setMargin(1E-4f);

    var space = MinecraftSpace.get(entity.level());
    var rigidBody = new nl.oxod.oxphysics.bullet.collision.body.EntityRigidBody(
        (EntityPhysicsElement) entity, space, shape);
    rigidBody.setCcdMotionThreshold(0.01f);
    rigidBody.setCcdSweptSphereRadius(0.4f);
    rigidBody.setContactProcessingThreshold(0.0f);
    accessor.physics$setRigidBody(rigidBody);
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

    for (var rigidBody : space.getRigidBodiesByClass(EntityRigidBody.class)) {
      var location = rigidBody.getFrame().getLocation(new Vector3f(), 1.0f);
      var rotation = rigidBody.getFrame().getRotation(new Quaternion(), 1.0f);
      var element = rigidBody.getElement().cast();
      if (element instanceof net.minecraft.world.entity.Display display) {
        var bb = rigidBody.getCollisionShape().boundingBox(new Vector3f(), rotation, new BoundingBox());
        element.absSnapTo(location.x, location.y, location.z);
        var displayAccessor = (nl.oxod.oxphysics.mixin.DisplayAccessor) (Object) display;
        display.getEntityData().set(displayAccessor.getDataLeftRotationId(), Convert.toMinecraft(rotation));
      } else {
        element.absSnapTo(location.x, location.y, location.z);
      }
    }
  }
}
