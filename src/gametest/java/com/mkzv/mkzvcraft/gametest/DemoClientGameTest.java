package com.mkzv.mkzvcraft.gametest;

import com.mkzv.mkzvcraft.ModItems;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Walks through the mod in a real client: item in hand, inventory tooltip, crafting recipe.
 */
public class DemoClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();

			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("give @a mkzvcraft:test_gem 16");
			world.getServer().runCommand("execute as @a at @s run summon item ^ ^1 ^2 {Item:{id:\"mkzvcraft:test_gem\",count:1}}");
			world.getServer().runCommand("execute as @a at @s run summon item ^1 ^1 ^3 {Item:{id:\"mkzvcraft:test_gem\",count:1}}");
			world.getServer().runCommand("execute as @a at @s run summon item ^-1 ^1 ^3 {Item:{id:\"mkzvcraft:test_gem\",count:1}}");
			context.getInput().lookAt(0, 20);
			context.waitTicks(40);
			context.takeScreenshot("1_gem_in_hand");

			// Inventory with the cursor over hotbar slot 1 to show the tooltip.
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);
			hoverGuiPoint(context, 8 + 8, 142 + 8);
			context.waitTicks(5);
			context.takeScreenshot("2_inventory_tooltip");
			context.setScreen(() -> null);

			// Crafting table: lay out the recipe server-side and hover the result.
			BlockPos table = context.computeOnClient(mc -> mc.player.blockPosition().north(2));
			world.getServer().runCommand("setblock " + table.getX() + " " + table.getY() + " " + table.getZ() + " minecraft:crafting_table");
			context.waitTicks(5);
			context.getInput().lookAt(table);
			context.waitTick();
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(CraftingScreen.class);

			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();

				if (player.containerMenu instanceof CraftingMenu menu) {
					// Slot 0 is the result, 1-9 are the 3x3 grid (row-major).
					menu.getSlot(2).set(new ItemStack(Items.AMETHYST_SHARD));
					menu.getSlot(4).set(new ItemStack(Items.AMETHYST_SHARD));
					menu.getSlot(5).set(new ItemStack(Items.DIAMOND));
					menu.getSlot(6).set(new ItemStack(Items.AMETHYST_SHARD));
					menu.getSlot(8).set(new ItemStack(Items.AMETHYST_SHARD));
					menu.broadcastChanges();
				}
			});
			context.waitTicks(10);

			boolean crafted = context.computeOnClient(mc ->
					mc.player.containerMenu.getSlot(0).getItem().is(ModItems.TEST_GEM));
			System.out.println("[mkzvcraft-demo] recipe result is test_gem: " + crafted);

			hoverGuiPoint(context, 124 + 8, 35 + 8);
			context.waitTicks(5);
			context.takeScreenshot("3_crafting_recipe");
			context.setScreen(() -> null);

			if (!crafted) {
				throw new AssertionError("Crafting recipe did not produce mkzvcraft:test_gem");
			}
		}
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
