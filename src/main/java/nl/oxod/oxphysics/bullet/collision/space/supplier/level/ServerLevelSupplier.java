package nl.oxod.oxphysics.bullet.collision.space.supplier.level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * This {@link LevelSupplier} provides a list of all
 * {@link ServerLevel} objects running on the {@link MinecraftServer}.
 */
public record ServerLevelSupplier(MinecraftServer server) implements LevelSupplier {
  @Override
  public List<Level> getAll() {
    return new ArrayList<>((Collection<? extends Level>)server.getAllLevels());
  }

  @Override
  public Level get(@NonNull ResourceKey<Level> key) {
    return server.getLevel(key);
  }

  @Override
  public Optional<Level> getOptional(@NonNull ResourceKey<Level> key) {
    return Optional.ofNullable(get(key));
  }
}
