package com.alrex.parcool.common.action.impl;

import com.alrex.parcool.api.SoundEvents;
import com.alrex.parcool.client.animation.impl.CrawlAnimator;
import com.alrex.parcool.client.animation.impl.SlidingAnimator;
import com.alrex.parcool.client.input.KeyRecorder;
import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.action.StaminaConsumeTiming;
import com.alrex.parcool.common.capability.Animation;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import com.alrex.parcool.config.ParCoolConfig;
import com.alrex.parcool.utilities.VectorUtil;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.TickEvent;

import java.nio.ByteBuffer;

public class Slide extends Action {
	private Vector3d slidingVec = null;

	@Override
	public boolean canStart(PlayerEntity player, Parkourability parkourability, IStamina stamina, ByteBuffer startInfo) {
		return (!stamina.isExhausted()
				&& parkourability.getActionInfo().can(Slide.class)
				&& KeyRecorder.keyCrawlState.isPressed()
				&& player.isOnGround()
				&& !parkourability.get(Roll.class).isDoing()
				&& !parkourability.get(Tap.class).isDoing()
				&& parkourability.get(Crawl.class).isDoing()
				&& !player.isInWaterOrBubble()
				&& parkourability.get(FastRun.class).getDashTick(parkourability.getAdditionalProperties()) > 5
		);
	}

	@Override
	public boolean canContinue(PlayerEntity player, Parkourability parkourability, IStamina stamina) {
        int maxSlidingTick = Math.min(
                parkourability.getActionInfo().getClientSetting().get(ParCoolConfig.Client.Integers.SlidingContinuableTick),
                parkourability.getActionInfo().getServerLimitation().get(ParCoolConfig.Server.Integers.MaxSlidingContinuableTick)
        );
		return getDoingTick() < maxSlidingTick
				&& parkourability.get(Crawl.class).isDoing();
	}

	@Override
	public void onStartInLocalClient(PlayerEntity player, Parkourability parkourability, IStamina stamina, ByteBuffer startData) {
		slidingVec = player.getLookAngle().multiply(1, 0, 1).normalize();
		if (ParCoolConfig.Client.Booleans.EnableActionSounds.get())
            player.playSound(SoundEvents.SLIDE.get(), 1f, 1f);
		Animation animation = Animation.get(player);
		if (animation != null) {
			animation.setAnimator(new SlidingAnimator());
		}
		parkourability.getCancelMarks().addMarkerCancellingJump(this::isDoing);
	}

	@Override
	public void onStartInOtherClient(PlayerEntity player, Parkourability parkourability, ByteBuffer startData) {
		Animation animation = Animation.get(player);
		if (animation != null) {
			animation.setAnimator(new SlidingAnimator());
		}
	}

	@Override
	public void onWorkingTickInLocalClient(PlayerEntity player, Parkourability parkourability, IStamina stamina) {
		if (slidingVec != null) {
			ModifiableAttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
			double speedScale = 0.45;
			if (attr != null) {
				speedScale = attr.getValue() * 4.5;
			}
			Vector3d vec = slidingVec.scale(speedScale);
			player.setDeltaMovement((player.isOnGround() ? vec : vec.scale(0.6)).add(0, player.getDeltaMovement().y(), 0));
		}
	}

	@Override
	public void onStopInLocalClient(PlayerEntity player) {
		Animation animation = Animation.get(player);
		if (animation != null && !animation.hasAnimator()) {
			animation.setAnimator(new CrawlAnimator());
		}
	}

	@Override
	public void onStopInOtherClient(PlayerEntity player) {
		Animation animation = Animation.get(player);
		if (animation != null && !animation.hasAnimator()) {
			animation.setAnimator(new CrawlAnimator());
		}
	}

	@Override
	public void onRenderTick(TickEvent.RenderTickEvent event, PlayerEntity player, Parkourability parkourability) {
		if (slidingVec == null || !isDoing()) return;
		player.yRot = (float) VectorUtil.toYawDegree(slidingVec);
	}

	@Override
	public StaminaConsumeTiming getStaminaConsumeTiming() {
		return StaminaConsumeTiming.None;
	}
}
