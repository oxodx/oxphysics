package nl.oxod.oxphysics.mixin;

import net.minecraft.world.level.Level;
import nl.oxod.oxphysics.bullet.collision.space.MinecraftSpace;
import nl.oxod.oxphysics.bullet.collision.space.storage.SpaceStorage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Level.class)
public class LevelMixin implements SpaceStorage {
	@Unique
	private MinecraftSpace space;

	@Override
	public void setSpace(MinecraftSpace space) {
		this.space = space;
	}

	@Override
	public MinecraftSpace getSpace() {
		return this.space;
	}
}
