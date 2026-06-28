//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package data.shipsystems.scripts;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

public class anvil_king extends BaseShipSystemScript {
	public static String RD_NO_EXTRA_CRAFT = "rd_no_extra_craft";
	public static String RD_FORCE_EXTRA_CRAFT = "rd_force_extra_craft";
	public static float EXTRA_FIGHTER_DURATION = 25.0F;
	public static final Object KEY_JITTER = new Object();
	public static final float DAMAGE_INCREASE_PERCENT = 60.0F;
	public static final Color JITTER_UNDER_COLOR = new Color(105, 150, 250, 225);
	public static final Color JITTER_COLOR = new Color(105, 150, 255, 255);

	public anvil_king() {
	}

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI)stats.getEntity();
			if (effectLevel == 1.0F) {
				Iterator var7 = ship.getLaunchBaysCopy().iterator();
					ship = (ShipAPI)stats.getEntity();
					if (effectLevel > 0.0F) {
						float jitterLevel = effectLevel;
						float maxRangeBonus = 5.0F;
						float jitterRangeBonus = effectLevel * maxRangeBonus;
						Iterator var10 = this.getFighters(ship).iterator();

						while (var7.hasNext()) {
							FighterLaunchBayAPI bay = (FighterLaunchBayAPI) var7.next();
							if (bay.getWing() != null) {
								bay.makeCurrentIntervalFast();
								FighterWingSpecAPI spec = bay.getWing().getSpec();
								int addForWing = getAdditionalFor(spec);
								int maxTotal = spec.getNumFighters() + addForWing;
								int actualAdd = maxTotal - bay.getWing().getWingMembers().size();
								actualAdd = Math.min(spec.getNumFighters(), actualAdd);
								if (actualAdd > 0) {
									bay.setFastReplacements(bay.getFastReplacements() + addForWing);
									bay.setExtraDeployments(actualAdd);
									bay.setExtraDeploymentLimit(maxTotal);
									bay.setExtraDuration(EXTRA_FIGHTER_DURATION);
								}
							}
						}
						while (var10.hasNext()) {
							ShipAPI fighter = (ShipAPI) var10.next();
							if (!fighter.isHulk()) {
								MutableShipStatsAPI fStats = fighter.getMutableStats();
								fStats.getBallisticWeaponDamageMult().modifyMult(id, 1.0F + 0.5F * effectLevel);
								fStats.getEnergyWeaponDamageMult().modifyMult(id, 1.0F + 0.5F * effectLevel);
								fStats.getMissileWeaponDamageMult().modifyMult(id, 1.0F + 0.5F * effectLevel);
								if (jitterLevel > 0.0F) {
									fighter.setWeaponGlow(effectLevel, Misc.setAlpha(JITTER_UNDER_COLOR, 255), EnumSet.allOf(WeaponAPI.WeaponType.class));
									fighter.setJitterUnder(KEY_JITTER, JITTER_COLOR, jitterLevel, 5, 0.0F, jitterRangeBonus);
									fighter.setJitter(KEY_JITTER, JITTER_UNDER_COLOR, jitterLevel, 2, 0.0F, 0.0F + jitterRangeBonus * 1.0F);
									Global.getSoundPlayer().playLoop("system_targeting_feed_loop", ship, 1.0F, 1.0F, fighter.getLocation(), fighter.getVelocity());
								}
							}
						}
					}
			}

		}
	}

	public static int getAdditionalFor(FighterWingSpecAPI spec) {
		if (spec.isBomber() && !spec.hasTag(RD_FORCE_EXTRA_CRAFT)) {
			return 0;
		} else if (spec.hasTag(RD_NO_EXTRA_CRAFT)) {
			return 0;
		} else {
			int size = spec.getNumFighters();
			return size <= 3 ? 1 : 2;
		}
	}
	private java.util.List<ShipAPI> getFighters(ShipAPI carrier) {
		List<ShipAPI> result = new ArrayList();
		Iterator var4 = Global.getCombatEngine().getShips().iterator();

		while(var4.hasNext()) {
			ShipAPI ship = (ShipAPI)var4.next();
			if (ship.isFighter() && ship.getWing() != null && ship.getWing().getSourceShip() == carrier) {
				result.add(ship);
			}
		}

		return result;
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI)stats.getEntity();
			Iterator var5 = this.getFighters(ship).iterator();

			while(var5.hasNext()) {
				ShipAPI fighter = (ShipAPI)var5.next();
				if (!fighter.isHulk()) {
					MutableShipStatsAPI fStats = fighter.getMutableStats();
					fStats.getBallisticWeaponDamageMult().unmodify(id);
					fStats.getEnergyWeaponDamageMult().unmodify(id);
					fStats.getMissileWeaponDamageMult().unmodify(id);
				}
			}

		}
	}

	public StatusData getStatusData(int index, State state, float effectLevel) {
		float percent = 50.0F * effectLevel;
		return index == 0 ? new StatusData(Misc.getRoundedValueMaxOneAfterDecimal(1.0F + 50.0F * effectLevel * 0.01F) + "x fighter damage", false) : null;
	}

	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		return true;
	}
}
