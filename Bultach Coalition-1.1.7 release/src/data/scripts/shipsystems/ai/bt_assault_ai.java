package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.characters.PersonalityAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.combat.ShipAIConfig;

import static com.fs.starfarer.api.impl.campaign.ids.Personalities.AGGRESSIVE;
import static com.fs.starfarer.api.impl.campaign.ids.Personalities.RECKLESS;

public class bt_assault_ai implements ShipSystemAIScript {

	private ShipAPI ship;
	private ShipSystemAPI system;
	private IntervalUtil tracker = new IntervalUtil(0.5f, 1.0f);
	private ShipAIConfig savedConfig = new ShipAIConfig(); // Unbased AI
	private ShipwideAIFlags flags;

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.system = system;
		this.flags = flags;

		if (ship.getShipAI() != null) {
			this.savedConfig = ship.getShipAI().getConfig(); // Save the original, likely unbased AI
		} else {
			this.savedConfig = new ShipAIConfig();
		}
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		if (ship == null || system == null || !ship.isAlive()) {
			return;
		}

		tracker.advance(amount);  // Ensure the system checks aren't happening too frequently idk
		if (!tracker.intervalElapsed()) {
			return;
		}


		// If our CR is low, we really don't wanna use this lol
		float cr = ship.getCurrentCR();
		if (cr < 0.2f) {
			if (system.isActive()) {
				deactivateSystem();
			}
			return;
		}

		ShipAPI closestEnemy = findClosestEnemy();
		float distanceToEnemy = closestEnemy != null ? MathUtils.getDistance(ship, closestEnemy) : Float.MAX_VALUE;

		if (distanceToEnemy > 2000f) {
			if (system.isActive()) {
				deactivateSystem(); // Turn off the system if we got no enemies around or we're backing off to vent
			}
			return;
		}

		// If all conditions are met, we're moving in to fight. No reason not to use it.
		if (!system.isActive() && system.getState() == ShipSystemAPI.SystemState.IDLE && !ship.getFluxTracker().isOverloadedOrVenting()) {
			activateSystem();
		}

		// Check if the ship is backing off and is decently far away
		if (flags.hasFlag(ShipwideAIFlags.AIFlags.BACKING_OFF) && distanceToEnemy > 1400f) {
			deactivateSystem();
		}
	}

	private void activateSystem() {
		ship.useSystem();
		// Become based when active
		savedConfig.personalityOverride = ship.getShipAI().getConfig().personalityOverride = AGGRESSIVE;
	}


	private void deactivateSystem() {
		if (system.isActive()) {
			ship.useSystem();
		}
		// Become unbased when off
		if (ship.getShipAI().getConfig() != null) {
			ship.getShipAI().getConfig().personalityOverride = savedConfig.personalityOverride;
		}
	}

	private ShipAPI findClosestEnemy() {
		float minDistance = Float.MAX_VALUE;
		ShipAPI closestEnemy = null;

		for (ShipAPI enemy : AIUtils.getEnemiesOnMap(ship)) {
			float distance = MathUtils.getDistance(ship, enemy);
			if (distance < minDistance) {
				minDistance = distance;
				closestEnemy = enemy;
			}
		}

		return closestEnemy;
	}
}
