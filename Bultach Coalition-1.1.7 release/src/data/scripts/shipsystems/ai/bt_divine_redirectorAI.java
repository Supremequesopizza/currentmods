package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class bt_divine_redirectorAI implements ShipSystemAIScript {

	private ShipAPI ship;
	private CombatEngineAPI engine;
	private ShipSystemAPI system;

	private final IntervalUtil checkInterval = new IntervalUtil(0.2f, 0.4f);
	private static final float CONE_ANGLE = 60f;
	private static final float RANGE_CHECK_MULT = 3.0f;
	private static final float PROJECTILE_DAMAGE_THRESHOLD = 1000f;
	private static final float FRIENDLY_PROJECTILE_THRESHOLD = 500f;
	private static final float PRESSURE_FLUX_THRESHOLD = 0.45f;
	private static final float MAX_FLUX_LEVEL = 0.99f;


	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.system = system;
		this.engine = engine;
		checkInterval.forceIntervalElapsed();
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		if (engine == null || engine.isPaused() || ship == null) {
			return;
		}

		checkInterval.advance(amount);

		if (checkInterval.intervalElapsed()) {
			if (system.getState() != ShipSystemAPI.SystemState.IDLE) {
				return;
			}
			if (!ship.isAlive() || ship.isPhased()) {
				return;
			}

			float currentFlux = ship.getFluxTracker().getFluxLevel();
			if (currentFlux > MAX_FLUX_LEVEL) {
				return;
			}

			boolean underPressure = currentFlux > PRESSURE_FLUX_THRESHOLD;
			float potentialAbsorbedEnemyDamage = 0f;
			float potentialAbsorbedFriendlyDamage = 0f;
			float checkRange = ship.getCollisionRadius() * RANGE_CHECK_MULT;

			List<DamagingProjectileAPI> projectiles = engine.getProjectiles();
			for (DamagingProjectileAPI proj : projectiles) {
				if (proj.didDamage() || proj.isFading()) {
					continue;
				}

				boolean isFriendly = proj.getOwner() == ship.getOwner();

				if (isFriendly && !underPressure) {
					continue;
				}

				if (MathUtils.isWithinRange(ship.getLocation(), proj.getLocation(), checkRange)) {
					float angleToProj = VectorUtils.getAngle(ship.getLocation(), proj.getLocation());
					float angleDiff = MathUtils.getShortestRotation(ship.getFacing(), angleToProj);

					if (Math.abs(angleDiff) <= CONE_ANGLE) {
						float aimAngleDiff = MathUtils.getShortestRotation(
								VectorUtils.getFacing(proj.getVelocity()),
								VectorUtils.getAngle(proj.getLocation(), ship.getLocation())
						);

						if (Math.abs(aimAngleDiff) <= 60f) {
							if (isFriendly) {
								potentialAbsorbedFriendlyDamage += proj.getDamageAmount();
							} else {
								potentialAbsorbedEnemyDamage += proj.getDamageAmount();
							}
						}
					}
				}
			}

			boolean triggerBasedOnEnemy = potentialAbsorbedEnemyDamage >= PROJECTILE_DAMAGE_THRESHOLD;
			boolean triggerBasedOnFriendly = underPressure && potentialAbsorbedFriendlyDamage >= FRIENDLY_PROJECTILE_THRESHOLD;

			if (triggerBasedOnEnemy || triggerBasedOnFriendly) {
				ship.useSystem();
			}
		}
	}
}