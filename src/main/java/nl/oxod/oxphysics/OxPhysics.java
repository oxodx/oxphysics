package nl.oxod.oxphysics;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OxPhysics implements ModInitializer {
	public static final String MOD_ID = "oxphysics";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// prevent annoying libbulletjme spam
		java.util.logging.LogManager.getLogManager().reset();

		LOGGER.info("Hello Fabric world!");
	}
}
