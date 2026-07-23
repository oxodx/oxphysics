package nl.oxod.oxphysics.api.event.collision;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import nl.oxod.oxphysics.bullet.collision.body.ElementRigidBody;
import nl.oxod.oxphysics.bullet.collision.space.MinecraftSpace;

public final class PhysicsSpaceEvents {
  public static final Event<Init> INIT = EventFactory.createArrayBacked(Init.class, (events) -> (space) -> {
    for (var e : events) {
      e.onInit(space);
    }
  });
  public static final Event<Step> STEP = EventFactory.createArrayBacked(Step.class, (events) -> (space) -> {
    for (var e : events) {
      e.onStep(space);
    }
  });
  public static final Event<PostStep> POST_STEP = EventFactory.createArrayBacked(PostStep.class, (events) -> (space) -> {
    for (var e : events) {
      e.onPostStep(space);
    }
  });
  public static final Event<ElementAdded> ELEMENT_ADDED = EventFactory.createArrayBacked(ElementAdded.class,
      (events) -> (space, body) -> {
        for (var e : events) {
          e.onElementAdded(space, body);
        }
      });
  public static final Event<ElementRemoved> ELEMENT_REMOVED = EventFactory.createArrayBacked(ElementRemoved.class,
      (events) -> (space, body) -> {
        for (var e : events) {
          e.onElementRemoved(space, body);
        }
      });

  private PhysicsSpaceEvents() {
  }

  @FunctionalInterface
  public interface Init {
    /**
     * Invoked each time a new {@link MinecraftSpace} is created.
     * 
     * @param space the minecraft space
     */
    void onInit(MinecraftSpace space);
  }

  @FunctionalInterface
  public interface Step {
    /**
     * Invoked each time the {@link MinecraftSpace} is stepped.
     * 
     * @param space the minecraft space
     */
    void onStep(MinecraftSpace space);
  }

  @FunctionalInterface
  public interface PostStep {
    /**
     * Invoked after each time the {@link MinecraftSpace} is stepped.
     * Use for position correction, corner collision checks, etc.
     * 
     * @param space the minecraft space
     */
    void onPostStep(MinecraftSpace space);
  }

  @FunctionalInterface
  public interface ElementAdded {
    void onElementAdded(MinecraftSpace space, ElementRigidBody rigidBody);
  }

  @FunctionalInterface
  public interface ElementRemoved {
    void onElementRemoved(MinecraftSpace space, ElementRigidBody rigidBody);
  }
}
