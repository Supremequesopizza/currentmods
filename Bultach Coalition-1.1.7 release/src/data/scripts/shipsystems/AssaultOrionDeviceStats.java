package data.scripts.shipsystems;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.Misc;

/**
 *  The way to provide custom params is to have a derived class that sets p = <params> in its constructor. 
 *  
 * @author Alex
 *
 * Copyright 2022 Fractal Softworks, LLC
 */
public class AssaultOrionDeviceStats extends BaseShipSystemScript {
	
	
	public static class OrionDeviceParams {
		public Color shapedExplosionColor = new Color(255, 125, 25, 155);
		public Color jitterColor = new Color(255, 125, 25, 55);
	}

	boolean runonce = true;

	boolean fired = false;
	
	
	protected OrionDeviceParams p = new OrionDeviceParams();
	

	protected Color orig = null;
	protected void recolor(ShipAPI ship) {
		if (ship == null) return;
		
		if (orig == null) orig = p.shapedExplosionColor;
		
		Color curr = ship.getEngineController().getFlameColorShifter().getCurr();
		
		p.shapedExplosionColor = Misc.interpolateColor(orig, curr, 0.75f);
		p.shapedExplosionColor = Misc.setAlpha(p.shapedExplosionColor, orig.getAlpha());
	}
	
	protected boolean wasIdle = false;
	protected boolean deployedBomb = false;
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;
		//boolean player = false;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else {
			return;
		}
		
		recolor(ship);

		WeaponAPI bomblauncher = null;

		for (WeaponAPI w : ship.getAllWeapons()) {
			if (Objects.equals(w.getSlot().getId(), "WS 018")) {
				bomblauncher = w;
			}
		}

		fired = false;
		
		if (effectLevel >= 1) {
			ship.getEngineController().forceShowAccelerating();
			ship.getEngineController().getExtendLengthFraction().shift(this,ship.getEngineController().getExtendLengthFraction().getBase()*3f,1f,0f,1f);
			Vector2f direction = MathUtils.getPointOnCircumference(new Vector2f(0.0F, 0.0F), 1.0F, ship.getFacing());
			if (!VectorUtils.isZeroVector(direction)) {
				Vector2f dir = new Vector2f();
				direction.normalise(dir);
				dir.scale(1.5f);
				Vector2f.add(dir, ship.getVelocity(), ship.getVelocity());
			}

			if(runonce) {
				bomblauncher.setAmmo(1);
				runonce = false;
			}
		}
	}
	
	protected void advanceImpl(float amount, ShipAPI ship, State state, float effectLevel) {
		
	}
	
	
	public void unapply(MutableShipStatsAPI stats, String id) {

		ShipAPI ship = null;
		//boolean player = false;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else {
			return;
		}

		runonce = true;
		for (WeaponAPI w : ship.getAllWeapons()) {
			if (Objects.equals(w.getSlot().getId(), "WS 018")) {
				if(!fired){
					w.setForceFireOneFrame(true);
					fired=true;
				}
			}
		}
	}
	
}








