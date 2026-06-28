package data.hullmods;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.utils.bultach_utils;


public class bt_gestalt_drone_afterimages extends BaseHullMod {

	public static final Color AFTERIMAGE_COLOR = new Color(255, 251, 197, 50);
	public static final float AFTERIMAGE_DURATION = 0.15f;
	private IntervalUtil afterImageTimer = new IntervalUtil(0.05f, 0.05f);




	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		super.advanceInCombat(ship, amount);

		if (Global.getCombatEngine() == null || Global.getCombatEngine().isPaused()) {
			return;
		}
		if (!ship.isAlive()) {
			return;
		}


		afterImageTimer.advance(amount);
		if (afterImageTimer.intervalElapsed()) {
			bultach_utils.afterimage(ship, AFTERIMAGE_COLOR, AFTERIMAGE_DURATION);
		}

	}
}


