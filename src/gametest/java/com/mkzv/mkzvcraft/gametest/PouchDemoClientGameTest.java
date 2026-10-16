package com.mkzv.mkzvcraft.gametest;

import java.util.ArrayList;
import java.util.List;

import com.mkzv.mkzvcraft.ModItems;
import com.mkzv.mkzvcraft.entity.Grogu;
import com.mkzv.mkzvcraft.pouch.GroguPouch;
import com.mkzv.mkzvcraft.pouch.GroguPouchItem;
import com.mkzv.mkzvcraft.pouch.PouchState;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;

/**
 * Pouch walkthrough with real clicks: Grogu climbs into the pouch (icon changes), rides on the
 * player's body, pushes a husk, heals the player at half health, naps, and climbs back out.
 * Then: throwing the pouch lets him out, and creative doesn't duplicate anything.
 */
public class PouchDemoClientGameTest implements FabricClientGameTest {
	private static final String AHEAD = "execute as @a at @s rotated 180 0 run ";

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			TestServerContext server = world.getServer();
			List<String> problems = new ArrayList<>();

			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("difficulty easy");
			server.runCommand("gamemode survival @a");
			server.runCommand("gamerule show_advancement_messages false");
			server.runCommand("item replace entity @a weapon.mainhand with mkzvcraft:grogu_pouch");
			summonTamedGrogu(context, server, "{Health:7f,CustomName:\"Baby\",Rotation:[0f,0f]}");

			// 1. Right-click Grogu with the pouch: he climbs in, the pouch in hand now holds him.
			clickGrogu(context, server);
			context.waitTicks(5);
			String packed = server.computeOnServer(PouchDemoClientGameTest::describe);
			System.out.println("[pouch-demo] packed: " + packed);
			check(problems, packed.equals("grogus=0 carrying=true pouches=1 full=1 inHand=full"), "packed " + packed);

			// 2. How it looks on the player, from the front and from behind.
			context.getInput().lookAt(180.0F, 15.0F);
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(20);
			screenshot(context, "pouch_1_front");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(5);
			screenshot(context, "pouch_2_back");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));

			// 3. The pouch icon and tooltip in the inventory.
			showTooltip(context, "pouch_3_tooltip_ready");

			// 4. Force push from the pouch: a husk walks up to the player's side.
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			server.runCommand(AHEAD + "summon minecraft:husk ^-2.5 ^ ^0.5 {Rotation:[90f,0f]}");

			for (int shot = 1; shot <= 5; shot++) {
				context.waitTicks(3);
				screenshot(context, "pouch_4_push_" + shot);
			}

			context.waitTicks(10);
			double huskDistance = server.computeOnServer(s -> firstPlayer(s).level()
					.getEntitiesOfClass(Husk.class, firstPlayer(s).getBoundingBox().inflate(32.0)).stream()
					.mapToDouble(husk -> husk.distanceTo(firstPlayer(s))).min().orElse(-1.0));
			System.out.println("[pouch-demo] husk distance after push: " + huskDistance);
			check(problems, huskDistance > 4.0, "push husk=" + huskDistance);
			server.runOnServer(s -> firstPlayer(s).level()
					.getEntitiesOfClass(Husk.class, firstPlayer(s).getBoundingBox().inflate(48.0)).forEach(Entity::discard));
			context.waitTicks(20);

			// 5. Force heal at exactly half health, then a nap in the pouch.
			server.runOnServer(s -> firstPlayer(s).setHealth(10.0F));

			for (int shot = 1; shot <= 3; shot++) {
				context.waitTicks(5);
				screenshot(context, "pouch_5_heal_" + shot);
			}

			float health = server.computeOnServer(s -> firstPlayer(s).getHealth());
			System.out.println("[pouch-demo] health after heal: " + health);
			check(problems, health > 10.0F, "heal health=" + health);
			context.waitTicks(30);
			boolean napping = server.computeOnServer(s -> {
				PouchState state = firstPlayer(s).getAttached(GroguPouch.STATE);
				return state != null && state.napping();
			});
			check(problems, napping, "napping");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			showTooltip(context, "pouch_6_tooltip_napping");

			// 6. Right-click the ground with the pouch: he climbs out.
			context.getInput().lookAt(groundAhead(server));
			context.waitTick();
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(15);
			String released = server.computeOnServer(PouchDemoClientGameTest::describe);
			System.out.println("[pouch-demo] released: " + released);
			check(problems, released.startsWith("grogus=1 name=Baby owner=true sitting=false") && released.endsWith("carrying=false pouches=1 full=0 inHand=empty"), "released " + released);
			context.getInput().lookAt(180.0F, 20.0F);
			context.waitTicks(10);
			screenshot(context, "pouch_7_released");

			// 7. Put him back and throw the pouch away (Q): he jumps out by himself.
			server.runOnServer(PouchDemoClientGameTest::tameAndSit);
			context.waitTicks(5);
			clickGrogu(context, server);
			context.getInput().pressKey(options -> options.keyDrop);
			context.waitTicks(20);
			String thrown = server.computeOnServer(s -> describe(s) + " droppedFull=" + droppedFullPouches(s));
			System.out.println("[pouch-demo] thrown: " + thrown);
			check(problems, thrown.startsWith("grogus=1 ") && thrown.endsWith("carrying=false pouches=0 full=0 inHand=none droppedFull=0"), "thrown " + thrown);

			// 8. Creative: in and out must not duplicate Grogu or the pouch.
			server.runCommand("kill @e[type=minecraft:item]");
			server.runCommand("gamemode creative @a");
			server.runCommand("item replace entity @a weapon.mainhand with mkzvcraft:grogu_pouch");
			server.runOnServer(PouchDemoClientGameTest::tameAndSit);
			context.waitTicks(10);
			clickGrogu(context, server);
			String creativeIn = server.computeOnServer(PouchDemoClientGameTest::describe);
			System.out.println("[pouch-demo] creative in: " + creativeIn);
			check(problems, creativeIn.equals("grogus=0 carrying=true pouches=1 full=1 inHand=full"), "creative in " + creativeIn);
			context.getInput().lookAt(groundAhead(server));
			context.waitTick();
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(15);
			String creativeOut = server.computeOnServer(PouchDemoClientGameTest::describe);
			System.out.println("[pouch-demo] creative out: " + creativeOut);
			check(problems, creativeOut.startsWith("grogus=1 ") && creativeOut.endsWith("carrying=false pouches=1 full=0 inHand=empty"), "creative out " + creativeOut);

			if (!problems.isEmpty()) {
				throw new AssertionError("Pouch demo failed: " + problems);
			}
		}
	}

	private static void check(List<String> problems, boolean ok, String what) {
		if (!ok) {
			problems.add(what);
		}
	}

	private static void summonTamedGrogu(ClientGameTestContext context, TestServerContext server, String nbt) {
		server.runCommand(AHEAD + "summon mkzvcraft:grogu ^ ^ ^2 " + nbt);
		context.waitTicks(2);
		server.runOnServer(PouchDemoClientGameTest::tameAndSit);
		context.waitTicks(10);
	}

	private static void clickGrogu(ClientGameTestContext context, TestServerContext server) {
		BlockPos groguPos = server.computeOnServer(s -> nearbyGrogus(s).getFirst().blockPosition());
		context.getInput().lookAt(groguPos);
		context.waitTick();
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(10);
	}

	private static BlockPos groundAhead(TestServerContext server) {
		return server.computeOnServer(s -> firstPlayer(s).blockPosition().north(2).below());
	}

	private static void showTooltip(ClientGameTestContext context, String name) {
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitForScreen(InventoryScreen.class);
		hoverGuiPoint(context, 8 + 8, 142 + 8);
		context.waitTicks(5);
		screenshot(context, name);
		context.setScreen(() -> null);
		context.waitTicks(2);
	}

	private static String describe(MinecraftServer server) {
		ServerPlayer player = firstPlayer(server);
		List<Grogu> grogus = nearbyGrogus(server);
		StringBuilder out = new StringBuilder("grogus=").append(grogus.size());

		if (grogus.size() == 1) {
			Grogu grogu = grogus.getFirst();
			out.append(" name=").append(grogu.getCustomName() == null ? null : grogu.getCustomName().getString())
					.append(" owner=").append(grogu.isOwnedBy(player))
					.append(" sitting=").append(grogu.isOrderedToSit())
					.append(" health=").append(grogu.getHealth());
		}

		int pouches = 0;
		int full = 0;

		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);

			if (stack.is(ModItems.GROGU_POUCH)) {
				pouches += stack.getCount();
				full += GroguPouchItem.hasGrogu(stack) ? stack.getCount() : 0;
			}
		}

		ItemStack hand = player.getMainHandItem();
		String inHand = !hand.is(ModItems.GROGU_POUCH) ? "none" : GroguPouchItem.hasGrogu(hand) ? "full" : "empty";
		return out.append(" carrying=").append(GroguPouch.isCarrying(player))
				.append(" pouches=").append(pouches)
				.append(" full=").append(full)
				.append(" inHand=").append(inHand)
				.toString();
	}

	private static long droppedFullPouches(MinecraftServer server) {
		ServerPlayer player = firstPlayer(server);
		return player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(16.0)).stream()
				.filter(item -> GroguPouchItem.hasGrogu(item.getItem()))
				.count();
	}

	private static void tameAndSit(MinecraftServer server) {
		ServerPlayer player = firstPlayer(server);

		for (Grogu grogu : nearbyGrogus(server)) {
			grogu.tame(player);
			grogu.setOrderedToSit(true);
			grogu.setInSittingPose(true);
		}
	}

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

	/** Moves the cursor to a point given in container-GUI coordinates (176x166 background, centered). */
	private static void hoverGuiPoint(ClientGameTestContext context, int guiX, int guiY) {
		double[] pos = context.computeOnClient(mc -> {
			var window = mc.getWindow();
			double scale = (double) window.getScreenWidth() / window.getGuiScaledWidth();
			int left = (window.getGuiScaledWidth() - 176) / 2;
			int top = (window.getGuiScaledHeight() - 166) / 2;
			return new double[] {(left + guiX) * scale, (top + guiY) * scale};
		});
		context.getInput().setCursorPos(pos[0], pos[1]);
	}
}
