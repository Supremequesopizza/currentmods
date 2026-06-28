package data.scripts.weapons;

import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import java.awt.Color;

public class GigaRailEveryFrameEffect implements EveryFrameWeaponEffectPlugin {
	
    private static final Color MUZZLE_FLASH_COLOR = new Color(255, 80, 30, 135);
	float MAX_DAMAGE_MULT = 1.2f;

	private IntervalUtil steamInterval = new IntervalUtil(0.05f, 0.05f);
    private boolean FIRE = false;
    
    private float emissionAngle = 4f; // the angle the smoke is emitted at, weapon facing +/- this value 
    private float smokeVel = 40f; // maximum speed the smoke is emitted at
    private float spacingDist = 30f; // maximum distance the smoke can be spawned forwards of the barrel 
    private float spawnSpread = 6f; // random spread around the point of the muzzle for the smoke to spread out to make it more natural looking
    
    
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		weapon.getSpec().setUnaffectedByProjectileSpeedBonuses(true);
		
        if (engine.isPaused())
            return;
		
		for (DamagingProjectileAPI p : engine.getProjectiles()){
			
			if(p.getWeapon() == weapon){
				float Damage = p.getProjectileSpec().getDamage().getBaseDamage();
				
				if (p.getElapsed() < 0.4f)
					p.setDamageAmount(Damage + (Damage * p.getElapsed()/0.8f));
				else
					p.setDamageAmount(Damage * MAX_DAMAGE_MULT);
			}
		}
		
		if (Global.getSettings().getCurrentState() == GameState.COMBAT){
			float chargeLevel = weapon.getChargeLevel();
			float shipFacing = weapon.getCurrAngle();
			Vector2f weaponLocation = weapon.getLocation();
			Vector2f shipVelocity = weapon.getShip().getVelocity();
			Vector2f muzzleLocation = MathUtils.getPointOnCircumference(weaponLocation, 185f, shipFacing);
				
			if (chargeLevel == 1f){
					
				RippleDistortion ripple = new RippleDistortion(muzzleLocation, shipVelocity); //I FUCKING LOVE RIPPLES
				ripple.setSize(600f);
				ripple.setIntensity(80f);
				ripple.setFrameRate(30f);
				ripple.fadeInSize(0.6f);
				ripple.fadeOutIntensity(0.7f);
				DistortionShader.addDistortion(ripple);
				
				engine.spawnExplosion(muzzleLocation, shipVelocity, MUZZLE_FLASH_COLOR, 240f, 0.4f); //flash time
				Global.getSoundPlayer().playUISound("gigarail_shot", 1f, 0.5f);
			}
		}
		
		
		
		if (weapon.getChargeLevel() < 0.95) {
            FIRE = true;
        }
        
        if (weapon.getCooldownRemaining() < 0.5f) {
            FIRE = false;
        }
        
        if (weapon.isFiring() && !FIRE) {
            steamInterval.advance(amount);
            if (steamInterval.intervalElapsed()) {
                
                for (int i=0; i < 3; i++) {
                    

                    float angle1 = weapon.getCurrAngle() + MathUtils.getRandomNumberInRange(-emissionAngle, emissionAngle);
                    Vector2f offsetVel1 = MathUtils.getPointOnCircumference(weapon.getShip().getVelocity(), MathUtils.getRandomNumberInRange(1f, smokeVel), angle1);
                    
                    Vector2f point1 = MathUtils.getPointOnCircumference(weapon.getFirePoint(0), MathUtils.getRandomNumberInRange(1f, spacingDist), angle1);
                    
                    engine.addNebulaSmokeParticle(MathUtils.getRandomPointInCircle(point1, spawnSpread),
                            offsetVel1,
                            30f, //size
                            1.7f, //end mult
                            0.4f, //ramp fraction
                            0.5f, //full bright fraction
                            0.6f, //duration
                            new Color(110,
                                    100,
                                    100,
                                    70));
                    
                }
            }
        }
		
	}
}
