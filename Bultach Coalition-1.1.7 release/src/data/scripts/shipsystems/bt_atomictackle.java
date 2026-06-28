package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import data.scripts.utils.bt_atomicheader;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class bt_atomictackle extends BaseShipSystemScript {


	public static final float SPEED_BOOST = 500f;
	public static final float RESIST_MULT = 0.5f;
	public static final float MASS_SCALAR = 2f;
	public static final float MASS_UNSCALAR = 0.5f;
	public static final float TURN_BOOST = 1f;
	public static final float TURN_CLAMP = 0f;
	public static final float SELF_DAMAGE = 0f;

	public static Vector2f calculateHeader(float facing){
		double angle = Math.toRadians(facing);
		Vector2f dir = new Vector2f((float)Math.cos(angle),(float)Math.sin(angle));
		if (dir.lengthSquared() > 0f) dir.normalise();
		return dir;
	}

	private boolean primed = false;
	private boolean upMassed = false;

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		Color explosionColor = new Color(255,220,135,255);
		Color jitterColor = new Color(255,220,135,64);
		if (state == State.OUT) {

			stats.getMaxSpeed().unmodify(id);
			stats.getAcceleration().unmodify(id);
			if (ship.getVelocity().length() > stats.getMaxSpeed().getModifiedValue())
				ship.getVelocity().scale(0.95f);

		} else if (state == State.ACTIVE) {
			Vector2f header = bt_atomicheader.calculateHeader(ship.getFacing());
			if (primed) {
				primed = false;
				Vector2f behindShip = new Vector2f(header);
				behindShip.scale(-180f);
				Vector2f explosionLocation = Vector2f.add(ship.getLocation(),behindShip,new Vector2f());
				behindShip.normalise().scale(165f);
				Vector2f damageLocation = Vector2f.add(ship.getLocation(),behindShip,new Vector2f());
				Global.getCombatEngine().spawnExplosion(explosionLocation,ship.getVelocity(),explosionColor,450f,2f);

				stats.getMaxTurnRate().unmodify(id);
				stats.getTurnAcceleration().unmodify(id);

				ship.setMass(ship.getMass() * MASS_SCALAR);
				for (ShipAPI s : ship.getChildModulesCopy()){
					s.setMass(s.getMass() * MASS_SCALAR);
				}
				ship.getVelocity().set(header);
				ship.getVelocity().scale(SPEED_BOOST);
				Global.getSoundPlayer().playSound("bt_atomictackle",1f,5f,explosionLocation,ship.getVelocity());

				// Self damage. Prevent engines from taking damage and remove DR for this step, then undo those changes.
				stats.getEngineDamageTakenMult().modifyMult(id,0f);
				stats.getArmorDamageTakenMult().unmodify(id);
                stats.getHullDamageTakenMult().unmodify(id);
				Global.getCombatEngine().applyDamage(ship,damageLocation, SELF_DAMAGE, DamageType.HIGH_EXPLOSIVE,0f,true,false,null);
                stats.getEngineDamageTakenMult().unmodify(id);
                stats.getArmorDamageTakenMult().modifyMult(id, RESIST_MULT);
                stats.getHullDamageTakenMult().modifyMult(id, RESIST_MULT);

				upMassed = true;
			}
			stats.getMaxTurnRate().modifyMult(id,TURN_CLAMP);
			stats.getTurnAcceleration().modifyMult(id,TURN_CLAMP);

			stats.getMaxSpeed().modifyFlat(id,SPEED_BOOST);
			stats.getAcceleration().modifyFlat(id,SPEED_BOOST);




		} else if (state == State.IN) {
			if (!primed)
				Global.getSoundPlayer().playSound("bt_atomictackle_charge",1f,1f,ship.getLocation(),ship.getVelocity());
			primed = true;
			stats.getMaxTurnRate().modifyMult(id,TURN_BOOST);
			stats.getTurnAcceleration().modifyMult(id,TURN_BOOST);
			ship.getEngineController().setFlameLevel(ship.getEngineController().getShipEngines().get(10).getEngineSlot(),0f);
			stats.getArmorDamageTakenMult().modifyMult(id, RESIST_MULT,"damage resistance");
			stats.getHullDamageTakenMult().modifyMult(id, RESIST_MULT);
			ship.setJitter(null,jitterColor,effectLevel,5,50f);
			for (ShipAPI s : ship.getChildModulesCopy()){
				s.setJitter(null,jitterColor,effectLevel,5,50f);
			}
		}
		if (state != State.IN)
			stats.getMissileMaxSpeedBonus().modifyFlat(id,ship.getVelocity().length());
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		stats.getMaxSpeed().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getArmorDamageTakenMult().unmodify(id);
		stats.getHullDamageTakenMult().unmodify(id);
		stats.getMissileMaxSpeedBonus().unmodify(id);
		if (upMassed){
			ship.setMass(ship.getMass() * MASS_UNSCALAR);
			for (ShipAPI s : ship.getChildModulesCopy()){
				s.setMass(s.getMass() * MASS_UNSCALAR);
			}
			upMassed = false;
		}
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("" + (int)(100f - (RESIST_MULT * 100f)) + "% damage reduction", false);
		}
		if (index == 1 && state != State.IN) {
			return new StatusData("mass +" + (int)((MASS_SCALAR - 1f) * 100f) + "%", false);
		}
		if (index == 2 && state != State.IN) {
			return new StatusData("YOU CAN'T ESCAPE", false);
		}
		return null;
	}
}
