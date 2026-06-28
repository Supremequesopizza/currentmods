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

public class supportships_jammerpod_stats extends BaseShipSystemScript {

	private CombatEngineAPI engine;
	
	private float pulseDelay = 0.1f;

	private static final float ARC = 25f;
	private static final float SCAN_RANGE = 1500f;
	private boolean VALID = false;

	private float particleCount = 60f;
	private float gradient = 2.145f; // this is the gradient to match the angle of the arc, so we can have the particles spawned in the correct area

	public static float DAM_MULT = 0.5f;
	public static float SPD_MULT = -0.5f;
	public static float DRONE_DAM_MULT = 0.75f;
	public static float DRONE_SPD_MULT = -0.75f;	
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
                if (target_ship.isHulk() || target_ship.isFrigate() || target_ship.isDestroyer() || target_ship.isCruiser() || target_ship.isCapital() || target_ship.getOwner() == ship.getOwner()) {
                    continue;
                }
                
                	// check if the ship is in the frontal arc, in range and is a fighter, if yes to all, then it's valid
			    float angle = VectorUtils.getAngle(ship.getLocation(), target_ship.getLocation());

				if ((Math.abs(MathUtils.getShortestRotation(angle, ship.getFacing())) <= ARC) && (MathUtils.getDistance(ship, target_ship) <= (range))) {
					VALID = true;
				} else {
					VALID = false;
				}

                	// if the target ship is classed as valid, apply debuffs, otherwise clear debuffs
                if (VALID){
					if(target_ship.isFighter()){
                	target_ship.getMutableStats().getHullDamageTakenMult().modifyMult(id + ship.getId(), 1f + (effectLevel * DAM_MULT));
                	target_ship.getMutableStats().getArmorDamageTakenMult().modifyMult(id + ship.getId(), 1f + (effectLevel * DAM_MULT));
                	target_ship.getMutableStats().getShieldDamageTakenMult().modifyMult(id + ship.getId(), 1f + (effectLevel * DAM_MULT));
                	target_ship.getMutableStats().getEmpDamageTakenMult().modifyMult(id + ship.getId(), 1f + (effectLevel * DAM_MULT));
					target_ship.getMutableStats().getMaxSpeed().modifyMult(id + ship.getId(), 1f + (effectLevel * SPD_MULT));
					target_ship.getMutableStats().getMaxTurnRate().modifyMult(id + ship.getId(), 1f + (effectLevel * SPD_MULT));
					}
					if(target_ship.isDrone()){
						target_ship.getMutableStats().getHullDamageTakenMult().modifyMult(id + ship.getId(), 1f + (effectLevel * DRONE_DAM_MULT));
						target_ship.getMutableStats().getArmorDamageTakenMult().modifyMult(id + ship.getId(), 1f + (effectLevel * DRONE_DAM_MULT));
						target_ship.getMutableStats().getShieldDamageTakenMult().modifyMult(id + ship.getId(), 1f + (effectLevel * DRONE_DAM_MULT));
						target_ship.getMutableStats().getEmpDamageTakenMult().modifyMult(id + ship.getId(), 1f + (effectLevel * DRONE_DAM_MULT));
						target_ship.getMutableStats().getMaxSpeed().modifyMult(id + ship.getId(), 1f + (effectLevel * DRONE_SPD_MULT));
						target_ship.getMutableStats().getMaxTurnRate().modifyMult(id + ship.getId(), 1f + (effectLevel * DRONE_SPD_MULT));
					}
					target_ship.setJitter(id + ship.getId(), JITTER_COLOR, Math.min(effectLevel, 0.8f), 3, 0f, 5f);
                }else{
                	target_ship.getMutableStats().getHullDamageTakenMult().unmodify(id + ship.getId());
                	target_ship.getMutableStats().getArmorDamageTakenMult().unmodify(id + ship.getId());
                	target_ship.getMutableStats().getShieldDamageTakenMult().unmodify(id + ship.getId());
                	target_ship.getMutableStats().getEmpDamageTakenMult().unmodify(id + ship.getId());
                	target_ship.getMutableStats().getMaxSpeed().unmodify(id + ship.getId());
                	target_ship.getMutableStats().getMaxTurnRate().unmodify(id + ship.getId());
					
                	target_ship.setJitter(id, null, 0f, 0, 0f, 0f);
                }
        	}
        	
        	
        	Vector2f emitterPos = new Vector2f();
        	for (WeaponSlotAPI weapon : ship.getHullSpec().getAllWeaponSlotsCopy()) {
				float slotOrder = weapon.getRenderOrderMod();
		    	  if (weapon.isSystemSlot() && slotOrder == 2 ) {
		    		  emitterPos = weapon.computePosition(ship);
		    	  }
        	}

        	
        	
			// particle spawning script, spawns a cone of particles
    		pulseDelay -= timer;
			while (pulseDelay <= 0f) {
				pulseDelay += 0.1f;
				
				// add a glow over the "emitter"
				engine.addSmoothParticle(emitterPos, ship.getVelocity(), 40f, effectLevel, 0.29f, new Color(204,204,255,160));
				
				while (particleCount >= 0f) {
					particleCount -= 1f;
	    			  
					for (WeaponSlotAPI weapon : ship.getHullSpec().getAllWeaponSlotsCopy()) {
						float slotOrder2 = weapon.getRenderOrderMod();
						if (weapon.isSystemSlot() && slotOrder2 == 1) {
	    					  
							float randomRange = MathUtils.getRandomNumberInRange(1f, 17f);
							randomRange *= effectLevel;
				    		
							float particleRange = ship.getMutableStats().getSystemRangeBonus().computeEffective(randomRange); // this is so we get correctly scaling area with SysEx
							
							Vector2f posZero = weapon.computePosition(ship);
							Vector2f shipPos = ship.getLocation();
							posZero.x -= shipPos.x;
							posZero.y -= shipPos.y;
							
							posZero.scale(particleRange);
							
							float radius0 = particleRange * 70;
							float radius = radius0 / gradient;
							
							Vector2f randomPos = MathUtils.getRandomPointInCircle(null, radius);
							posZero.x += randomPos.x;
							posZero.y += randomPos.y;
							
							posZero.x += shipPos.x;
							posZero.y += shipPos.y;
							
							//Vector2f randomVel = MathUtils.getRandomPointOnCircumference(null, MathUtils.getRandomNumberInRange(1f, 13f));
							//float randomSize = MathUtils.getRandomNumberInRange(27f, 51f);
							// the old randoms, for big ugly "functional" effect, from before the sexo sprites
							Vector2f randomVel = MathUtils.getRandomPointOnCircumference(null, MathUtils.getRandomNumberInRange(5f, 31f));
							float randomSize = MathUtils.getRandomNumberInRange(7f, 13f);
							
							engine.addSmoothParticle(posZero, //location 
									randomVel, //speed
									randomSize, //size
									1.0f, //brightness
									0.45f, //duration
									new Color(204,204,255,120));
				    		  	
							float randomRange2 = MathUtils.getRandomNumberInRange(11f, 17f);
							randomRange2 *= effectLevel;
							
							float particleRange2 = ship.getMutableStats().getSystemRangeBonus().computeEffective(randomRange2); // this is so we get correctly scaling area with SysEx
							
							Vector2f posZero2 = weapon.computePosition(ship);
							posZero2.x -= shipPos.x;
							posZero2.y -= shipPos.y;
							
							posZero2.scale(particleRange2);
							
							float radius02 = particleRange2 * 70;
							float radius2 = radius02 / gradient;
							
							Vector2f randomPos2 = MathUtils.getRandomPointInCircle(null, radius2);
							posZero2.x += randomPos2.x;
							posZero2.y += randomPos2.y;
							
							posZero2.x += shipPos.x;
							posZero2.y += shipPos.y;
							
							engine.addSmoothParticle(posZero2, //location 
									randomVel, //speed
									randomSize, //size
									1.0f, //brightness
									0.45f, //duration
									new Color(204,204,255,120));
						}
					}
				}
				particleCount = ship.getMutableStats().getSystemRangeBonus().computeEffective(120f * effectLevel); // this is so we get correctly scaling particle count for the area with SysEx
			}
			// particle script ends here
		}
	}
	
	public void unapply(MutableShipStatsAPI stats, String id) {
		pulseDelay = 0f;
		
    	VALID = false;
		

    	
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (engine != Global.getCombatEngine()) {
            engine = Global.getCombatEngine();

        }
        for (ShipAPI target_ship : engine.getShips()) {
            if (target_ship.isHulk() || target_ship.isFrigate() || target_ship.isDestroyer() || target_ship.isCruiser() || target_ship.isCapital() || target_ship.getOwner() == ship.getOwner()) {
                continue;
            }
        	target_ship.getMutableStats().getHullDamageTakenMult().unmodify(id + ship.getId());
        	target_ship.getMutableStats().getArmorDamageTakenMult().unmodify(id + ship.getId());
        	target_ship.getMutableStats().getShieldDamageTakenMult().unmodify(id + ship.getId());
        	target_ship.getMutableStats().getEmpDamageTakenMult().unmodify(id + ship.getId());
			target_ship.getMutableStats().getMaxSpeed().unmodify(id + ship.getId());
            target_ship.getMutableStats().getMaxTurnRate().unmodify(id + ship.getId());
        	
        	target_ship.setJitter(id + ship.getId(), null, 0f, 0, 0f, 0f);
        }
	}

	public static float getMaxRange(ShipAPI ship) {
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(SCAN_RANGE);
		//return RANGE;
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("Jamming Enemy Fighters", false);
		}
		return null;
	}
}


