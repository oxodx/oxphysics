package nl.oxod.oxphysics;

import net.fabricmc.api.ModInitializer;
import nl.oxod.oxphysics.bullet.natives.NativeLoader;
import nl.oxod.oxphysics.event.ServerEventHandler;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class OxPhysics implements ModInitializer {
	public static final String MOD_NAME = "OxPhysics";
	public static final String MOD_ID = "oxphysics";

	public static final String ISSUE_TRACKER_URL = "https://github.com/oxodx/oxphysics/issues";

	public static final Logger LOGGER = LogUtils.getLogger();

	@Override
	public void onInitialize() {
		// prevent annoying libbulletjme spam
		java.util.logging.LogManager.getLogManager().reset();

		NativeLoader.load();
		ServerEventHandler.register();

		LOGGER.info("{} loaded!", MOD_NAME);
	}
}
