package nl.oxod.oxphysics.bullet.natives;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;

import org.apache.commons.io.FileUtils;

import com.jme3.system.JmeSystem;
import com.jme3.system.NativeLibraryLoader;

import net.fabricmc.loader.api.FabricLoader;
import nl.oxod.oxphysics.OxPhysics;

public class NativeLoader {
  public static void load() {
    final var fileName = getPlatformSpecficName();
    final var nativesFolder = getGameDir().resolve("natives/");
    final var url = NativeLoader.class.getResource("/assets/natives/" + fileName);

    try {
      if (!Files.exists(nativesFolder)) {
        Files.createDirectories(nativesFolder);
      }

      final var destination = nativesFolder.resolve(fileName);
      final var destinationFile = destination.toFile();

      if (Files.exists(destination)) {
        if (!destinationFile.delete()) {
          OxPhysics.LOGGER.warn("Failed to remove old bullet natives.");
        }
      }

      try {
        FileUtils.copyURLToFile(url, destinationFile);
      } catch (IOException e) {
        OxPhysics.LOGGER.warn("Unable to copy natives.");
      }

      NativeLibraryLoader.loadLibbulletjme(true, nativesFolder.toFile(), "Release", getVariant());
    } catch (IOException | NoSuchElementException e) {
      e.printStackTrace();
      throw new RuntimeException("Unable to load bullet natives.");
    }
  }

  static Path getGameDir() {
    return FabricLoader.getInstance().getGameDir();
  }

  static String getVariant() {
    //final var platform = JmeSystem.getPlatform();
    //if (platform == Platform.Windows64 || platform == Platform.Linux64) {
    //  return "SpMt";
    //}

    return "Sp";
  }

  static String getPlatformSpecficName() {
    final var platform = JmeSystem.getPlatform();

    final var name = switch (platform) {
      case Windows32, Windows64 -> "bulletjme.dll";
      case Android_ARM7, Android_ARM8, Linux_ARM32, Linux_ARM64, Linux32, Linux64 -> "libbulletjme.so";
      case MacOSX32, MacOSX64, MacOSX_ARM64 -> "libbulletjme.dylib";
      default -> throw new RuntimeException("Invalid platform " + platform);
    };

    return platform + "Release" + getVariant() + "_" + name;
  }
}
