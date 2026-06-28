package data.shipsystems.scripts;

import java.awt.Color;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.util.MagicRender;

import java.util.HashMap;
import java.util.Map;

public class supportships_lifesaver_stats extends BaseShipSystemScript {

	private CombatEngineAPI engine;
	
	private float pulseDelay = 0.1f;

	private static final float ARC = 360f;
	private static final float SCAN_RANGE = 1000f;
	private boolean VALID = false;


	public static float CR_MULT = 0.5f;
	public static float CREW_MULT = 0.5f;
	public static Color JITTER_COLOR = new Color(204,204,255,75);
	



	@Override
	public void apply(MutableShipStatsAPI stats, final String id, State state, float effectLevel) {
        if (engine != Global.getCombatEngine()) {
            engine = Global.getCombatEngine();

        }
        
        ShipAPI ship = (ShipAPI)stats.getEntity();
		float range = getMaxRange(ship);
        float timer = engine.getElapsedInLastFrame();
	  
        if (effectLevel <= 0.01f) {
        	return;
        } else {
        	
        	
        	for (ShipAPI target_ship : engine.getShips()) {
        			// check if the ship is a valid target
                if (target_ship.isHulk() || target_ship.isDrone() ||target_ship.isFighter() || target_ship.getOwner() != ship.getOwner()) {
                    continue;
                }
                
			    float angle = VectorUtils.getAngle(ship.getLocation(), target_ship.getLocation());

				if ((Math.abs(MathUtils.getShortestRotation(angle, ship.getFacing())) <= ARC) && (MathUtils.getDistance(ship, target_ship) <= (range))) {
					VALID = true;
				} else {
					VALID = false;
				}

                	// if the target ship is classed as valid, apply debuffs, otherwise clear debuffs
                if (VALID){
                	target_ship.getMutableStats().getCRLossPerSecondPercent().modifyMult(id + ship.getId(), 1f + (effectLevel * CR_MULT));
					target_ship.getMutableStats().getCrewLossMult().modifyMult(id + ship.getId(), 1f + (effectLevel * CREW_MULT));
					target_ship.setJitter(id + ship.getId(), JITTER_COLOR, Math.min(effectLevel, 0.8f), 3, 0f, 5f);
                }else{
                	target_ship.getMutableStats().getCRLossPerSecondPercent().unmodify(id + ship.getId());
					target_ship.getMutableStats().getCrewLossMult().unmodify(id + ship.getId());
					target_ship.setJitter(id, null, 0f, 0, 0f, 0f);
                }
        	}
		}
	}
	

	public static float getMaxRange(ShipAPI ship) {
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(SCAN_RANGE);
		//return RANGE;
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("Providing Support", false);
		}
		return null;
	}
}


