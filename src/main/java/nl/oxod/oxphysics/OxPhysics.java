package nl.oxod.oxphysics;

import net.fabricmc.api.ModInitializer;
import nl.oxod.oxphysics.bullet.natives.NativeLoader;
import nl.oxod.oxphysics.event.ServerEventHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OxPhysics implements ModInitializer {
	public static final String MOD_ID = "oxphysics";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// prevent annoying libbulletjme spam
		java.util.logging.LogManager.getLogManager().reset();

		NativeLoader.load();
		ServerEventHandler.register();
	}
}
