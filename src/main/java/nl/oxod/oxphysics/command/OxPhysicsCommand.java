package nl.oxod.oxphysics.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import nl.oxod.oxphysics.OxPhysics;

public final class OxPhysicsCommand {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
      final CommandBuildContext buildContext) {
    final LiteralArgumentBuilder<CommandSourceStack> oxPhysicsBuilder = Commands.literal(OxPhysics.MOD_ID)
      .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));

    OxPhysicsSpawnCommands.register(oxPhysicsBuilder, buildContext);

    dispatcher.register(oxPhysicsBuilder);
  }
}
