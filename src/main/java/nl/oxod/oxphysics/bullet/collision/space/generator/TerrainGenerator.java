package nl.oxod.oxphysics.bullet.collision.space.generator;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import nl.oxod.oxphysics.bullet.collision.body.ElementRigidBody;
import nl.oxod.oxphysics.bullet.collision.body.TerrainRigidBody;
import nl.oxod.oxphysics.bullet.collision.space.MinecraftSpace;

/**
 * Used for loading blocks into the simulation so that rigid bodies can interact
 * with them.
 * 
 * @see MinecraftSpace
 */
public class TerrainGenerator {
  public static void step(MinecraftSpace space) {
    final var chunkCache = space.getChunkCache();
    final Map<TerrainRigidBody, Boolean> keep = new IdentityHashMap<>();

    for (var rigidBody : space.getRigidBodiesByClass(ElementRigidBody.class)) {
      if (!rigidBody.terrainLoadingEnabled() || !rigidBody.isActive()) {
        continue;
      }

      final var aabb = rigidBody.getCurrentMinecraftBoundingBox().inflate(0.5f);

      BlockPos.betweenClosedStream(aabb).forEach(blockPos -> {
        chunkCache.getBlockData(blockPos).ifPresent(blockData -> {
          space.getTerrainObjectAt(blockPos).ifPresentOrElse(terrain -> {
            if (blockData.blockState() != terrain.getBlockState()) {
              space.removeCollisionObject(terrain);

              final var terrain2 = TerrainRigidBody.from(blockData);
              space.addCollisionObject(terrain2);
              keep.put(terrain2, Boolean.TRUE);
            } else {
              keep.put(terrain, Boolean.TRUE);
            }
          }, () -> {
            final var terrain = TerrainRigidBody.from(blockData);
            space.addCollisionObject(terrain);
            keep.put(terrain, Boolean.TRUE);
          });
        });
      });
    }

    space.getTerrainMap().forEach((blockPos, terrain) -> {
      if (!keep.containsKey(terrain)) {
        space.removeTerrainObjectAt(blockPos);
      }
    });
  }
}
