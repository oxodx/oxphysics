package nl.oxod.oxphysics.bullet.collision.space.storage;

import nl.oxod.oxphysics.bullet.collision.space.MinecraftSpace;
import net.minecraft.world.level.Level;

/**
 * Used for storing a {@link MinecraftSpace} within any
 * {@link Level} object.
 */
public interface SpaceStorage {
  void setSpace(MinecraftSpace space);

  MinecraftSpace getSpace();
}
