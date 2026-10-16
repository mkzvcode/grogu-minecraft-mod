package com.mkzv.mkzvcraft.gametest;

import java.util.List;

import com.mkzv.mkzvcraft.entity.Grogu;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.CameraType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.Husk;

/**
 * Grogu showcase: portrait, Force push against a husk, Force heal of a hurt owner and the nap after it.
 */
public class GroguDemoClientGameTest implements FabricClientGameTest {
	private static final String AHEAD = "execute as @a at @s rotated 180 0 run ";

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			TestServerContext server = world.getServer();

			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("difficulty easy");
			server.runCommand("gamemode survival @a");
			server.runCommand("gamerule show_advancement_messages false");
			server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 180 18");
			context.waitTicks(5);

			// 0. Close-up, three-quarter view.
			server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 180 30");
			server.runCommand(AHEAD + "summon mkzvcraft:grogu ^ ^ ^1.6 {NoAI:1b,Rotation:[35f,0f]}");
			context.waitTicks(30);
			screenshot(context, "grogu_0_closeup");
			server.runOnServer(s -> nearbyGrogus(s).forEach(Entity::discard));
			server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 180 18");
			context.waitTicks(5);

			// 1. Portrait: one standing, one sitting.
			server.runCommand(AHEAD + "summon mkzvcraft:grogu ^0.8 ^ ^2.2 {NoAI:1b,Rotation:[25f,0f]}");
			server.runCommand(AHEAD + "summon mkzvcraft:grogu ^-0.8 ^ ^2.2 {NoAI:1b,Sitting:1b,Rotation:[-25f,0f]}");
			context.waitTicks(30);
			screenshot(context, "grogu_1_portrait");
			server.runOnServer(s -> nearbyGrogus(s).forEach(Entity::discard));
			context.waitTicks(5);

			// 2. Force push: a tamed, sitting Grogu guards the player from a husk.
			server.runCommand(AHEAD + "summon mkzvcraft:grogu ^-0.9 ^ ^2.6 {Rotation:[-60f,0f]}");
			context.waitTicks(2);
			server.runOnServer(GroguDemoClientGameTest::tameNearbyGrogus);
			context.waitTicks(10);
			server.runCommand(AHEAD + "summon minecraft:husk ^2.2 ^ ^3.8 {Rotation:[90f,0f]}");

			for (int shot = 1; shot <= 6; shot++) {
				context.waitTicks(3);
				screenshot(context, "grogu_2_push_" + shot);
			}

			context.waitTicks(10);
			double huskDistance = server.computeOnServer(GroguDemoClientGameTest::huskDistanceFromGrogu);
			System.out.println("[grogu-demo] husk distance from Grogu after push: " + huskDistance);
			server.runOnServer(s -> firstPlayer(s).level().getEntitiesOfClass(Husk.class, firstPlayer(s).getBoundingBox().inflate(48.0)).forEach(Entity::discard));
			context.waitTicks(20);

			// 3. Force heal, filmed from behind the player: owner drops to 5 HP, Grogu heals and then naps.
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			server.runOnServer(s -> firstPlayer(s).setHealth(5.0F));

			for (int shot = 1; shot <= 4; shot++) {
				context.waitTicks(5);
				screenshot(context, "grogu_3_heal_" + shot);
			}

			float healthAfter = server.computeOnServer(s -> firstPlayer(s).getHealth());
			System.out.println("[grogu-demo] owner health after Force heal: " + healthAfter);

			context.waitTicks(40);
			boolean napping = server.computeOnServer(s -> nearbyGrogus(s).stream().anyMatch(Grogu::isNapping));
			System.out.println("[grogu-demo] Grogu napping after heal: " + napping);
			screenshot(context, "grogu_4_nap");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));

			if (huskDistance < 4.0 || healthAfter <= 5.0F || !napping) {
				throw new AssertionError("Grogu demo failed: husk=" + huskDistance + " health=" + healthAfter + " napping=" + napping);
			}
		}
	}

	/** Clears advancement toasts so they don't cover the shot. */
	private static void screenshot(ClientGameTestContext context, String name) {
		context.runOnClient(mc -> mc.gui.toastManager().clear());
		context.takeScreenshot(name);
	}

	private static ServerPlayer firstPlayer(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static List<Grogu> nearbyGrogus(MinecraftServer server) {
		ServerPlayer player = firstPlayer(server);
		return player.level().getEntitiesOfClass(Grogu.class, player.getBoundingBox().inflate(16.0));
	}

	private static void tameNearbyGrogus(MinecraftServer server) {
		ServerPlayer player = firstPlayer(server);

		for (Grogu grogu : nearbyGrogus(server)) {
			grogu.tame(player);
			grogu.setOrderedToSit(true);
			grogu.setInSittingPose(true);
		}
	}

	private static double huskDistanceFromGrogu(MinecraftServer server) {
		ServerPlayer player = firstPlayer(server);
		Grogu grogu = nearbyGrogus(server).getFirst();
		return player.level().getEntitiesOfClass(Husk.class, player.getBoundingBox().inflate(32.0)).stream()
				.mapToDouble(husk -> husk.distanceTo(grogu))
				.min()
				.orElse(-1.0);
	}
}
