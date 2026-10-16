package com.mkzv.grogu.pouch;

import com.mkzv.grogu.ModComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Grogu's pouch. Empty: right-click your tamed Grogu to put him in. With Grogu: right-click a block
 * to let him out. While it is in your inventory he rides on your body and uses the Force.
 */
public class GroguPouchItem extends Item {
	public GroguPouchItem(Item.Properties properties) {
		super(properties);
	}

	public static boolean hasGrogu(ItemStack stack) {
		return stack.has(ModComponents.GROGU);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		ItemStack pouch = context.getItemInHand();

		if (!hasGrogu(pouch)) {
			return InteractionResult.PASS;
		}

		Level level = context.getLevel();

		if (level instanceof ServerLevel serverLevel) {
			BlockPos clicked = context.getClickedPos();
			BlockPos pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty()
					? clicked
					: clicked.relative(context.getClickedFace());
			Player player = context.getPlayer();
			float yaw = player != null ? player.getYRot() + 180.0F : 0.0F;
			GroguPouch.letOut(serverLevel, pouch, pos, yaw);
		}

		return InteractionResult.SUCCESS;
	}
}
