package nl.oxod.oxphysics.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import nl.oxod.oxphysics.OxPhysics;

public final class OxPhysicsEntities {
  public static EntityType<PhysicsBlockEntity> PHYSICS_BLOCK;

  public static void register() {
    ResourceKey<EntityType<?>> key = ResourceKey.create(
        Registries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath(OxPhysics.MOD_ID, "physics_block"));

    PHYSICS_BLOCK = Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
        EntityType.Builder.<PhysicsBlockEntity>of(PhysicsBlockEntity::new, MobCategory.MISC)
            .sized(1.0f, 1.0f)
            .noSave()
            .clientTrackingRange(0)
            .build(key));

    OxPhysics.LOGGER.info("Registered OxPhysics entity types");
  }
}
