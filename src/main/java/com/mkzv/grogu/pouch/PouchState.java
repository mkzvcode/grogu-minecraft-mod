package com.mkzv.grogu.pouch;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What the carried Grogu is doing, synced to clients for rendering and the pouch tooltip.
 * {@code healCooldownSeconds} is 0 when he is ready to heal.
 */
public record PouchState(int forceTicks, int forceType, boolean napping, int healCooldownSeconds) {
	public static final StreamCodec<ByteBuf, PouchState> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, PouchState::forceTicks,
			ByteBufCodecs.VAR_INT, PouchState::forceType,
			ByteBufCodecs.BOOL, PouchState::napping,
			ByteBufCodecs.VAR_INT, PouchState::healCooldownSeconds,
			PouchState::new);
}
