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


public class Zap_Stats extends BaseShipSystemScript {
        public boolean isActive = false;
	public static final float ROF_BONUS = 3f;
	public static final float FLUX_REDUCTION = 50f;
	public static float SPEED_BONUS = 75f;
	public static float TURN_BONUS = 20f;
	
	private Color color = new Color(100,255,100,255);
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		
		float mult = 1f + ROF_BONUS * effectLevel;
		stats.getBallisticRoFMult().modifyMult(id, mult);
		stats.getBallisticWeaponFluxCostMod().modifyPercent(id, -FLUX_REDUCTION);
		
		if (state == ShipSystemStatsScript.State.OUT) {
			stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
			//stats.getMaxTurnRate().unmodify(id);
		} else {
			stats.getMaxSpeed().modifyFlat(id, SPEED_BONUS);
			stats.getAcceleration().modifyPercent(id, SPEED_BONUS * 3f * effectLevel);
			stats.getDeceleration().modifyPercent(id, SPEED_BONUS * 3f * effectLevel);
			stats.getTurnAcceleration().modifyFlat(id, TURN_BONUS * effectLevel);
			stats.getTurnAcceleration().modifyPercent(id, TURN_BONUS * 5f * effectLevel);
			stats.getMaxTurnRate().modifyFlat(id, 15f);
			stats.getMaxTurnRate().modifyPercent(id, 100f);
		}
		
		if (stats.getEntity() instanceof ShipAPI) {
			ShipAPI ship = (ShipAPI) stats.getEntity();
			
			ship.getEngineController().fadeToOtherColor(this, color, new Color(0,0,0,0), effectLevel, 0.67f);
			//ship.getEngineController().fadeToOtherColor(this, Color.white, new Color(0,0,0,0), effectLevel, 0.67f);
			ship.getEngineController().extendFlame(this, 2f * effectLevel, 0f * effectLevel, 0f * effectLevel);
			
                                if (state == State.OUT) {
                                //once
                                if (!isActive) {
                            ShipAPI empTarget = ship;
                            for (CombatEntityAPI EmpTarget :AIUtils.getNearbyEnemies(ship,900)) {
                                for (int x = 0; x < 2; x++) {
                                
                                Global.getCombatEngine().spawnEmpArc(ship, ship.getLocation(),
                                                   EmpTarget,
                                                   EmpTarget, DamageType.ENERGY, 900, 1200,
                                                   2000, null, 30f, new Color(230,40,40,255),
                                                   new Color(255,255,255,255));

                                }
                            }
                            
                                  isActive = true;
                                }//end once
                                }
                        
			
//			String key = ship.getId() + "_" + id;
//			Object test = Global.getCombatEngine().getCustomData().get(key);
//			if (state == State.IN) {
//				if (test == null && effectLevel > 0.2f) {
//					Global.getCombatEngine().getCustomData().put(key, new Object());
//					ship.getEngineController().getExtendLengthFraction().advance(1f);
//					for (ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
//						if (engine.isSystemActivated()) {
//							ship.getEngineController().setFlameLevel(engine.getEngineSlot(), 1f);
//						}
//					}
//				}
//			} else {
//				Global.getCombatEngine().getCustomData().remove(key);
//			}
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
                isActive = false;
	}
	
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		float mult = 1f + ROF_BONUS * effectLevel;
		float bonusPercent = (int) ((mult - 1f) * 100f);
		if (index == 0) {
			return new StatusData("ballistic rate of fire +" + (int) bonusPercent + "%", false);
		}
		if (index == 1) {
			return new StatusData("ballistic flux use -" + (int) FLUX_REDUCTION + "%", false);
		}
		if (index == 0) {
			return new StatusData("improved maneuverability", false);
		} else if (index == 1) {
			return new StatusData("+" + (int)SPEED_BONUS + " top speed", false);
		}
		return null;
	}
}
