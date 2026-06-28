package com.fs.starfarer.api.impl.campaign;

import java.awt.Color;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

public class bt_sirencall_effect implements EveryFrameScript {

	protected float moteSpawnRate = 1f;
	protected SectorEntityToken entity;
	protected IntervalUtil moteSpawn = new IntervalUtil(0.01f, 0.1f);
	protected String soundId = "bt_sirencall_loop";
	protected boolean soundPlaying = false;

	public
	bt_sirencall_effect(SectorEntityToken entity, float moteSpawnRate) {
		super();
		this.entity = entity;
		this.moteSpawnRate = moteSpawnRate;
	}

	public void advance(float amount) {
		float days = Misc.getDays(amount);
		moteSpawn.advance(days * moteSpawnRate);
		if (moteSpawn.intervalElapsed()) {
			spawnMote(entity);
		}

		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
		if (playerFleet != null) {
			float distance = Misc.getDistance(playerFleet.getLocation(), entity.getLocation());
			float maxSoundRange = 2000f;
			float minSoundRange = 200f;

			float volume = 0f;
			if (distance <= maxSoundRange) {
				if (distance <= minSoundRange) {
					volume = 1f;
				} else {
					volume = 1f - (distance - minSoundRange) / (maxSoundRange - minSoundRange);
				}
			}

			if (volume > 0f) {
				if (!soundPlaying) {
					soundPlaying = true;
				}
				Global.getSoundPlayer().playLoop(soundId, entity, 1f, volume, entity.getLocation(), entity.getVelocity(), 1f, 1f);
			} else {
				if (soundPlaying) {
					soundPlaying = false;
				}
			}
		}
	}


	public static void spawnMote(SectorEntityToken from) {
		if (!from.isInCurrentLocation()) return;
		float dur = 1f + 2f * (float) Math.random();
		dur *= 2f;
		float size = 3f + (float) Math.random() * 5f;
		size *= 3f;
		Color color = new Color(255, 232, 176,175);

		Vector2f loc = Misc.getPointWithinRadius(from.getLocation(), from.getRadius());
		Vector2f vel = Misc.getUnitVectorAtDegreeAngle((float) Math.random() * 360f);
		vel.scale(5f + (float) Math.random() * 10f);
		vel.scale(0.25f);
		Vector2f.add(vel, from.getVelocity(), vel);
		Misc.addGlowyParticle(from.getContainingLocation(), loc, vel, size, 0.5f, dur, color);
	}

	public boolean isDone() {
		return false;
	}

	public boolean runWhilePaused() {
		return false;
	}
}