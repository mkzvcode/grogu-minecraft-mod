package com.mkzv.grogu.gametest;

import java.util.List;

import com.mkzv.grogu.entity.Grogu;
import com.mkzv.grogu.pouch.GroguPouch;
import com.mojang.datafixers.util.Pair;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Store-page screenshots: a real swamp at sunset, HUD hidden, 1920x1080.
 * Saved to build/run/clientGameTest/screenshots as showcase_*.png.
 */
public class ShowcaseClientGameTest implements FabricClientGameTest {
	private static final String AHEAD = "execute as @a at @s rotated 180 0 run ";

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder()
				.setUseConsistentSettings(false)
				.adjustSettings(settings -> settings.setSeed("grogu"))
				.create()) {
			TestServerContext server = world.getServer();
			world.getConnection().waitForChunksRender();

			server.runCommand("gamerule show_advancement_messages false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("weather clear");
			server.runCommand("difficulty easy");
			server.runCommand("gamemode creative @a");

			// Fly over the nearest swamp, let it load, then land on a dry patch.
			BlockPos swamp = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(biome -> biome.is(Biomes.SWAMP), firstPlayer(s).blockPosition(), 6400, 32, 64);
				return found == null ? firstPlayer(s).blockPosition() : found.getFirst();
			});
			server.runCommand("tp @a " + swamp.getX() + " 140 " + swamp.getZ() + " 180 20");
			context.waitTicks(40);
			world.getConnection().waitForChunksRender();
			BlockPos spot = server.computeOnServer(s -> findDrySpot(s.overworld(), swamp));
			System.out.println("[grogu-showcase] swamp " + swamp + " spot " + spot);
			server.runCommand("tp @a " + spot.getX() + ".5 " + spot.getY() + " " + spot.getZ() + ".5 180 18");
			server.runCommand("gamemode survival @a");
			server.runCommand("kill @e[type=!minecraft:player]");
			context.waitTicks(20);
			world.getConnection().waitForChunksRender();
			setHudHidden(context, true);

			// 1. Hero shot at sunset.
			server.runCommand("time set 11200");
			server.runCommand(AHEAD + "summon grogu:grogu ^ ^ ^2.4 {NoAI:1b,Rotation:[20f,0f]}");
			context.getInput().lookAt(180.0F, 22.0F);
			context.waitTicks(40);
			shot(context, "showcase_1_grogu");

			// 2. Cosy evening: one sitting by a campfire, one standing.
			server.runOnServer(ShowcaseClientGameTest::removeGrogus);
			server.runCommand("time set 12300");
			server.runCommand(AHEAD + "setblock ^ ^ ^3.2 minecraft:campfire");
			server.runCommand(AHEAD + "summon grogu:grogu ^-1.2 ^ ^3 {NoAI:1b,Sitting:1b,Rotation:[-60f,0f]}");
			server.runCommand(AHEAD + "summon grogu:grogu ^1.3 ^ ^3.4 {NoAI:1b,Rotation:[50f,0f]}");
			context.getInput().lookAt(180.0F, 20.0F);
			context.waitTicks(40);
			shot(context, "showcase_2_campfire");
			server.runOnServer(ShowcaseClientGameTest::removeGrogus);
			server.runCommand(AHEAD + "setblock ^ ^ ^3.2 minecraft:air");

			// 3. The pouch, seen from the front.
			server.runCommand("time set 11200");
			server.runCommand(AHEAD + "summon grogu:grogu ^ ^ ^2 {Rotation:[0f,0f]}");
			context.waitTicks(2);
			server.runOnServer(ShowcaseClientGameTest::tameAll);
			server.runCommand("item replace entity @a weapon.mainhand with grogu:grogu_pouch");
			context.waitTicks(5);
			server.runOnServer(s -> {
				ServerPlayer player = firstPlayer(s);
				Grogu grogu = grogus(s).getFirst();
				GroguPouch.putIn(grogu, player, player.getMainHandItem());
				// Keep the hands free for the photo: move the pouch to the last hotbar slot.
				player.getInventory().setItem(8, player.getMainHandItem().copy());
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			});
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.getInput().lookAt(160.0F, 10.0F);
			context.waitTicks(30);
			shot(context, "showcase_3_pouch");

			// 4. Force push from the pouch at dusk.
			server.runCommand("time set 11200");
			context.getInput().lookAt(180.0F, 8.0F);
			server.runCommand(AHEAD + "summon minecraft:husk ^-2.5 ^ ^1 {Rotation:[90f,0f]}");

			for (int i = 1; i <= 6; i++) {
				context.waitTicks(3);
				shot(context, "showcase_4_push_" + i);
			}

			server.runOnServer(s -> s.overworld().getEntitiesOfClass(Husk.class, firstPlayer(s).getBoundingBox().inflate(48.0)).forEach(Entity::discard));

			// 5. Force heal.
			server.runCommand("time set 11200");
			context.waitTicks(10);
			server.runOnServer(s -> firstPlayer(s).setHealth(8.0F));

			for (int i = 1; i <= 8; i++) {
				context.waitTicks(3);
				shot(context, "showcase_5_heal_" + i);
			}

			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			setHudHidden(context, false);
		}
	}

	/** A grassy clearing near {@code center}: flat, dry and under open sky around the player and in front of him. */
	private static BlockPos findDrySpot(ServerLevel level, BlockPos center) {
		for (int radius = 0; radius <= 96; radius += 3) {
			for (int dx = -radius; dx <= radius; dx += 3) {
				for (int dz = -radius; dz <= radius; dz += 3) {
					BlockPos here = surface(level, center.getX() + dx, center.getZ() + dz);

					if (here != null && isClearing(level, here)) {
						return here;
					}
				}
			}
		}

		BlockPos fallback = surface(level, center.getX(), center.getZ());
		return fallback != null ? fallback : center;
	}

	private static boolean isClearing(ServerLevel level, BlockPos origin) {
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -6; dz <= 2; dz++) {
				BlockPos other = surface(level, origin.getX() + dx, origin.getZ() + dz);

				if (other == null || Math.abs(other.getY() - origin.getY()) > 1) {
					return false;
				}

				if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, other.getX(), other.getZ()) > other.getY()) {
					return false;
				}
			}
		}

		return true;
	}

	private static void removeGrogus(MinecraftServer server) {
		grogus(server).forEach(Entity::discard);
	}

	private static void setHudHidden(ClientGameTestContext context, boolean hidden) {
		context.runOnClient(mc -> {
			if (mc.gui.hud.isHidden() != hidden) {
				mc.gui.hud.toggle();
			}
		});
	}
	private static BlockPos surface(ServerLevel level, int x, int z) {
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		BlockPos pos = new BlockPos(x, y, z);
		boolean grass = level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK);
		boolean open = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
		return grass && open && level.getFluidState(pos).isEmpty() ? pos : null;
	}

	private static void tameAll(MinecraftServer server) {
		ServerPlayer player = firstPlayer(server);

		for (Grogu grogu : grogus(server)) {
			grogu.tame(player);
		}
	}

	private static List<Grogu> grogus(MinecraftServer server) {
		ServerPlayer player = firstPlayer(server);
		return player.level().getEntitiesOfClass(Grogu.class, player.getBoundingBox().inflate(16.0));
	}

	private static ServerPlayer firstPlayer(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(mc -> mc.gui.toastManager().clear());
		context.takeScreenshot(TestScreenshotOptions.of(name).withSize(1920, 1080));
	}
}
