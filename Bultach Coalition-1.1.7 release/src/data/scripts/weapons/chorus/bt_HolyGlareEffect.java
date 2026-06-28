package data.scripts.weapons.chorus;

import java.awt.Color;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.dweller.RiftLightningEffect;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.EmpArcEntityAPI.EmpArcParams;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual.NEParams;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

public class bt_HolyGlareEffect implements EveryFrameWeaponEffectPlugin {


	public static final Color HOLY_COLOR = new Color(255, 235, 200, 255);
	public static final Color HOLY_GLOW_COLOR = new Color(255, 255, 240, 255);
	private boolean hasPlayedChargeStartSound =false;
	private boolean hasFiredThisCharge =false;
	public static float RIFT_DAMAGE = 100f;
	protected IntervalUtil fireInterval = new IntervalUtil(0.1f, 0.2f);
	protected boolean hadDamageTargetPrev = false;
	protected boolean lengthChangedPrev = false;
	protected float sinceRiftSpawn = 0f;
	protected Vector2f prevTo = null;
	protected Vector2f prevFrom = null;
	private final String CHARGE="bt_chorus_charge";
	public bt_HolyGlareEffect() {
		fireInterval.randomize();
	}

	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (engine.isPaused()) return;

		BeamAPI beam = null;
		if (!weapon.getBeams().isEmpty()) {
			beam = weapon.getBeams().get(0);
		}

		float chargeLevel = weapon.getChargeLevel();
		String sequenceState = "READY";

		// frankencode
		if (chargeLevel >= 1f && weapon.isFiring()) {
			sequenceState = "FIRING";
			if (!hasFiredThisCharge) {
				hasFiredThisCharge = true;

			}
		} else if (chargeLevel > 0 && !hasFiredThisCharge) {

			sequenceState = "CHARGEUP";
		} else if (chargeLevel > 0 && hasFiredThisCharge) {

			sequenceState = "CHARGEDOWN";
		} else if (weapon.getCooldownRemaining() > 0) {
			sequenceState = "COOLDOWN";
			if (hasFiredThisCharge) {
				hasFiredThisCharge = false;
			}
		} else {
			sequenceState = "READY";

			if (hasFiredThisCharge) {
				hasFiredThisCharge = false;
			}
			if (hasPlayedChargeStartSound) {
				hasPlayedChargeStartSound = false;
			}
		}

		if (!weapon.isFiring() && chargeLevel <=0f && weapon.getCooldownRemaining() <= 0f) {
			if (hasFiredThisCharge) {
				hasFiredThisCharge = false;
			}
		}


		if (sequenceState.equals("CHARGEUP") && !hasPlayedChargeStartSound) {
			Global.getSoundPlayer().playSound(CHARGE, 1f, 1.5f, weapon.getLocation(), weapon.getShip().getVelocity());
			hasPlayedChargeStartSound = true;
		}

		if (!sequenceState.equals("CHARGEUP")) {
			hasPlayedChargeStartSound = false;
		}

		if (beam == null || beam.getSource() == null) return;

		sinceRiftSpawn += amount;

		float maxRange = weapon.getRange();
		Vector2f from = beam.getFrom();
		Vector2f to = beam.getRayEndPrevFrame();
		Vector2f to2 = beam.getTo();
		float dist = Misc.getDistance(from, to);
		float dist2 = Misc.getDistance(from, to2);
		if (dist2 < dist) {
			to = to2;
			dist = dist2;
		}

		boolean hasDamageTarget = beam.getDamageTarget() != null;
		boolean lengthChanged = prevTo == null ||
				Math.abs(Misc.getDistance(prevFrom, prevTo) - Misc.getDistance(from, to)) > 2f;

		boolean forceRiftSpawn = (hasDamageTarget && !hadDamageTargetPrev) ||
				(!lengthChanged && lengthChangedPrev);

		lengthChangedPrev = lengthChanged;
		hadDamageTargetPrev = hasDamageTarget;
		prevFrom = new Vector2f(from);
		prevTo = new Vector2f(to);

		fireInterval.advance(amount);
		if (fireInterval.intervalElapsed() || forceRiftSpawn) {

			if (beam.getDamageTarget() == null && dist < maxRange * 0.9f) {
				return;
			}
			if (beam.getBrightness() < 1) {
				return;
			}

			Color riftColor = HOLY_COLOR;

			float maxTimeWithoutExplosion = 1f;

			if ((float) Math.random() > 0.8f || forceRiftSpawn || sinceRiftSpawn > maxTimeWithoutExplosion) {
				DamagingProjectileAPI explosion = engine.spawnDamagingExplosion(
						createExplosionSpec(1f),
						beam.getSource(), to);

				float distFactor = 0f;
				if (dist > 500f) {
					distFactor = (dist - 500f) / 1500f;
					distFactor = Math.max(0f, Math.min(1f, distFactor));
				}
				float sizeAdd = 5f * distFactor;
				float baseSize = 15f;

				NEParams p = RiftCascadeMineExplosion.createStandardRiftParams(
						riftColor, baseSize + sizeAdd);
				p.noiseMult = 6f;
				p.thickness = 25f;
				p.fadeOut = 0.5f;
				p.spawnHitGlowAt = 1f;
				p.additiveBlend = true;
				p.blackColor = riftColor;
				p.underglow = null;
				p.withNegativeParticles = false;
				p.withHitGlow = false;
				p.fadeIn = 0f;

				RiftCascadeMineExplosion.spawnStandardRift(explosion, p);

				sinceRiftSpawn = 0f;
			}

			if (dist > 100f && (float) Math.random() > 0.5f) {
				EmpArcParams params = new EmpArcParams();
				params.segmentLengthMult = 8f;
				params.zigZagReductionFactor = 0.15f;
				params.fadeOutDist = 50f;
				params.minFadeOutMult = 10f;
				params.flickerRateMult = 0.3f;

				float fraction = Math.min(0.33f, 300f / dist);
				params.brightSpotFullFraction = fraction;
				params.brightSpotFadeFraction = fraction;

				float arcSpeed = RiftLightningEffect.RIFT_LIGHTNING_SPEED;
				params.movementDurOverride = Math.max(0.05f, dist / arcSpeed);

				ShipAPI ship = beam.getSource();
				EmpArcEntityAPI arc = (EmpArcEntityAPI) engine.spawnEmpArcVisual(from, ship, to, ship,
						80f,
						HOLY_COLOR,
						HOLY_GLOW_COLOR,
						params
				);
				arc.setCoreWidthOverride(40f);
				arc.setRenderGlowAtStart(false);
				arc.setFadedOutAtStart(true);
				arc.setSingleFlickerMode(true);

				Vector2f soundLoc = Vector2f.add(from, to, new Vector2f());
				soundLoc.scale(0.5f);
				Global.getSoundPlayer().playSound("bt_gestalt_arc", 1.1f, 0.2f, soundLoc, new Vector2f());
			}
		}

		Vector2f soundLoopLoc = Vector2f.add(from, to, new Vector2f());
		soundLoopLoc.scale(0.9f);
		Global.getSoundPlayer().playLoop("bt_chorus_loop",
				beam.getSource(), 1f, beam.getBrightness(),
				soundLoopLoc, beam.getSource().getVelocity());
	}

	public DamagingExplosionSpec createExplosionSpec(float damageMult) {
		float damage = RIFT_DAMAGE * damageMult;
		Color explosionColor = new Color(HOLY_COLOR.getRed(), HOLY_COLOR.getGreen(), HOLY_COLOR.getBlue(), 0); // Keep alpha 0
		Color particleColor = new Color(255, 255, 255, 0);

		DamagingExplosionSpec spec = new DamagingExplosionSpec(
				0.2f, 300f, 300f, damage, damage / 2f,
				CollisionClass.PROJECTILE_FF, CollisionClass.PROJECTILE_FIGHTER,
				3f, 3f, 0.5f, 0,
				particleColor, explosionColor
		);

		spec.setDamageType(DamageType.ENERGY);
		spec.setUseDetailedExplosion(false);
		spec.setSoundSetId("abyssal_glare_explosion");
		spec.setSoundVolume(damageMult);
		return spec;
	}
}