package data.scripts.shipsystems.convergence;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;
import data.scripts.shipsystems.convergence.bt_divine_convergence_SystemScript;

public class ConvergenceSwarmSystemAI implements ShipSystemAIScript {

	public static float MIN_DP_ADVANTAGE_FOR_CONSTRUCTION = 35;

	protected ShipAPI ship;
	protected CombatEngineAPI engine;
	protected ShipwideAIFlags flags;
	protected ShipSystemAPI system;
	protected bt_divine_convergence_SystemScript convergenceScript;

	protected IntervalUtil tracker = new IntervalUtil(0.5f, 1f);
	protected float considerUsingSystemTimer = 0f;

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.system = system;
		this.engine = engine;
		this.flags = flags;

		try {
			this.convergenceScript = (bt_divine_convergence_SystemScript) system.getScript();
		} catch (ClassCastException e) {
			Global.getLogger(ConvergenceSwarmSystemAI.class).error(
					"Failed to cast system script to bt_divine_convergence_SystemScript for ship: " +
							(ship != null ? ship.getHullSpec().getHullId() : "null") +
							". Ensure the correct AI script is assigned to the system.", e);
			this.convergenceScript = null;
		}
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		if (engine.isPaused() || convergenceScript == null || ship == null || !ship.isAlive()) {
			return;
		}

		tracker.advance(amount);
		if (considerUsingSystemTimer > 0) {
			considerUsingSystemTimer -= amount;
		}


		if (tracker.intervalElapsed()) {
			if (system.getCooldownRemaining() > 0 || system.isOutOfAmmo() || system.isActive()) {
				return;
			}

			if (!convergenceScript.isUsable(system, ship)) {
				considerUsingSystemTimer = 0f;
				return;
			}

			CombatFleetManagerAPI manager = Global.getCombatEngine().getFleetManager(ship.getOwner());
			int dpLeft = 0;
			if (manager != null) {
				dpLeft = manager.getMaxStrength() - manager.getCurrStrength();
			}

			boolean goodFluxSituation = ship.getFluxLevel() < 0.7f && ship.getHardFluxLevel() < 0.5f;


			boolean notInImmediatePeril = (flags.getCustom(ShipwideAIFlags.AIFlags.NEEDS_HELP) == null &&
					flags.getCustom(ShipwideAIFlags.AIFlags.IN_CRITICAL_DPS_DANGER) == null &&
					flags.getCustom(ShipwideAIFlags.AIFlags.BACKING_OFF) == null &&
					(collisionDangerDir == null || collisionDangerDir.lengthSquared() < 0.01f));


			if (goodFluxSituation && notInImmediatePeril && dpLeft > MIN_DP_ADVANTAGE_FOR_CONSTRUCTION) {
				if (considerUsingSystemTimer <= 0f) {
					considerUsingSystemTimer = 2.5f + (float) Math.random() * 2.5f;
				}
			} else {

				if (considerUsingSystemTimer > 1f) {
					considerUsingSystemTimer = 0f;
				}
			}

			if (considerUsingSystemTimer > 0f && considerUsingSystemTimer <= tracker.getIntervalDuration() * 1.1f ) {
				ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
				considerUsingSystemTimer = 0f;
			}
		}
	}
}