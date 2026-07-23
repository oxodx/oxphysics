package nl.oxod.oxphysics.bullet.collision.space.supplier.level;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface LevelSupplier {
  /**
   * Provides the complete list of {@link Level}s If
   * there aren't any, it will return an empty list.
   * @return the list of {@link Level}s
   */
  List<Level> getAll();

  /**
   * Provides a specific {@link Level} based on the gives {@link ResourceKey}.
   * @param key the {@link ResourceKey} to identify the level with
   * @return a {@link Level}
   */
  Level get(@NonNull ResourceKey<Level> key);

  /**
    * Provides a specific {@link Level} based on the given {@link ResourceKey}.
    * @param key the {@link ResourceKey} to identify the Level with
    * @return an optional {@link Level}
    */
  Optional<Level> getOptional(@NonNull ResourceKey<Level> key);
}
