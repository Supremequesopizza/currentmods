package data.scripts.shipsystems;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;

import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.AIUtils;

public class FluxBoosterStats extends BaseShipSystemScript {

        public boolean isActive = false;
	public static final float ROF_BONUS = 300f;
	public static final float FLUX_REDUCTION = 0f;
        public static final float HARD_FLUX_DISSIPATION_PERCENT = 50f;
        public static final float FLUX_DISSIPATION_PERCENT = 200f;
	public static float SPEED_BONUS = 200f;
	public static float TURN_BONUS = 350f;

	//private Color color = new Color(220, 50, 0,255);

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		float mult = 1f + ROF_BONUS * effectLevel;
		//stats.getBallisticRoFMult().modifyMult(id, mult);
                //stats.getEnergyRoFMult().modifyMult(id, mult);
                //stats.getMissileRoFMult().modifyMult(id, mult);
		stats.getBallisticWeaponFluxCostMod().modifyPercent(id, -FLUX_REDUCTION);
                stats.getHardFluxDissipationFraction().modifyFlat(id, (float)HARD_FLUX_DISSIPATION_PERCENT * 0.01f);
                stats.getFluxDissipation().modifyPercent(id, FLUX_DISSIPATION_PERCENT);

		if (state == ShipSystemStatsScript.State.OUT) {
			stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
		} else {
			stats.getMaxSpeed().modifyFlat(id, SPEED_BONUS * effectLevel);
			stats.getAcceleration().modifyFlat(id, SPEED_BONUS * effectLevel);
                        stats.getDeceleration().modifyFlat(id, SPEED_BONUS * effectLevel);
			//stats.getAcceleration().modifyPercent(id, 200f * effectLevel);
		}

		if (stats.getEntity() instanceof ShipAPI) {
			ShipAPI ship = (ShipAPI) stats.getEntity();

			//ship.getEngineController().fadeToOtherColor(this, color, new Color(0,0,0,0), effectLevel, 0.67f);
			//ship.getEngineController().fadeToOtherColor(this, Color.white, new Color(0,0,0,0), effectLevel, 0.67f);
			ship.getEngineController().extendFlame(this, 2f * effectLevel, 0f * effectLevel, 0f * effectLevel);

                                if (state == State.OUT) {
                                //once
                                if (!isActive) {
                            //ShipAPI empTarget = ship;
                            //for (int x = 0; x < 30; x++) {
                                //Global.getCombatEngine().spawnEmpArc(ship, ship.getLocation(),
                                                   //empTarget,
                                                   //empTarget, DamageType.ENERGY, 0, 200,
                                                   //2000, null, 30f, new Color(230,40,40,0),
                                                   //new Color(255,255,255,0));
                            //}

                            }

                                  isActive = true;
                                }//end once
                                }
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getBallisticRoFMult().unmodify(id);
		stats.getBallisticWeaponFluxCostMod().unmodify(id);
		stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
                stats.getHardFluxDissipationFraction().unmodify(id);
                stats.getFluxDissipation().unmodify(id);
                isActive = false;
	}

	public StatusData getStatusData(int index, State state, float effectLevel) {
		float mult = 1f + ROF_BONUS * effectLevel;
		float bonusPercent = (int) ((mult - 1f) * 100f);
		if (index == 0) {
			return new StatusData("increased engine power", false);
		}
		if (index == 2) {
			return new StatusData("hard flux dissipation +" + (int) HARD_FLUX_DISSIPATION_PERCENT + "%", false);
		}
                if (index == 3) {
			return new StatusData("flux dissipation +" + (int) FLUX_DISSIPATION_PERCENT + "%", false);
		}
		return null;
	}
}
