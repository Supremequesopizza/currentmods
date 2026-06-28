package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.magiclib.util.MagicLensFlare;
import org.magiclib.util.MagicRender;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class bt_pd_arching_onfire implements OnFireEffectPlugin {

	public static float ARC_ANGLE = 30f;
	private static final int MAX_SECONDARY_ARCS = 7;
	private static final float SECONDARY_ARC_RANGE = 500f;
	private static final float SECONDARY_ARC_DAMAGE = 60f;
	private static final float SECONDARY_ARC_EMP = 0f;
	private static final float SECONDARY_ARC_THICKNESS = 10f;
	private static final String SECONDARY_ARC_SOUND = "bt_gestalt_arc_quiet";
	private static final Color SECONDARY_ARC_CORE = new Color(255, 214, 160, 166);
	private static final Color SECONDARY_ARC_FRINGE = new Color(255, 191, 145, 115);
	private static final Color FLARE_COLOR = new Color(255, 230, 196, 152);


	@Override
	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		float emp = projectile.getEmpAmount();
		float dam = projectile.getDamageAmount();
		ShipAPI ship = weapon.getShip();
		if (ship == null) return;

		CombatEntityAPI target = findTarget(projectile, weapon, engine);
		float thickness = 20f;
		float coreWidthMult = 0.67f;
		Color color = weapon.getSpec().getGlowColor();

		if (target != null) {
			EmpArcEntityAPI primaryArc = engine.spawnEmpArc(
					ship, projectile.getLocation(), ship, target,
					DamageType.ENERGY, dam, emp, 100000f, "bt_gestalt_arc_quiet",
					thickness, color, new Color(255, 183, 140, 140)
			);
			primaryArc.setCoreWidthOverride(thickness * coreWidthMult);
			primaryArc.setSingleFlickerMode();

			Vector2f primaryHitLocation = target.getLocation();

			List<CombatEntityAPI> validTargets = new ArrayList<>();
			List<MissileAPI> missiles = CombatUtils.getMissilesWithinRange(primaryHitLocation, SECONDARY_ARC_RANGE);
			List<ShipAPI> ships = CombatUtils.getShipsWithinRange(primaryHitLocation, SECONDARY_ARC_RANGE);

			for (MissileAPI missile : missiles) {
				if (missile != target && missile.getOwner() != ship.getOwner()) {
					validTargets.add(missile);
				}
			}
			for (ShipAPI nearbyShip : ships) {
				if (nearbyShip.isFighter() && nearbyShip.isAlive() && !nearbyShip.isPhased() && nearbyShip.getOwner() != ship.getOwner() && nearbyShip != target) {
					validTargets.add(nearbyShip);
				}
			}

			int arcsSpawned = 0;
			for (CombatEntityAPI secondaryTarget : validTargets) {
				if (arcsSpawned >= MAX_SECONDARY_ARCS) {
					break;
				}
				spawnSecondaryArc(engine, ship, primaryHitLocation, target, secondaryTarget);
				arcsSpawned++;
			}

			if (MagicRender.screenCheck(0.5f, primaryHitLocation)) {
				MagicLensFlare.createSharpFlare(engine, ship, primaryHitLocation, 2, 50, 0, FLARE_COLOR, FLARE_COLOR);
			}

		} else {

			Vector2f from = new Vector2f(projectile.getLocation());
			Vector2f to = pickNoTargetDest(projectile, weapon, engine);
			EmpArcEntityAPI visualArc = engine.spawnEmpArcVisual(from, ship, to, ship, thickness, color, FLARE_COLOR);
			visualArc.setCoreWidthOverride(thickness * coreWidthMult);
			visualArc.setSingleFlickerMode();
		}
	}

	private void spawnSecondaryArc(CombatEngineAPI engine, ShipAPI source, Vector2f from, CombatEntityAPI anchor, CombatEntityAPI target) {
		engine.spawnEmpArcPierceShields(
				source, from, anchor, target,
				DamageType.ENERGY, SECONDARY_ARC_DAMAGE, SECONDARY_ARC_EMP, 10000f,
				SECONDARY_ARC_SOUND, SECONDARY_ARC_THICKNESS, SECONDARY_ARC_FRINGE, SECONDARY_ARC_CORE
		);

		if (MagicRender.screenCheck(0.5f, target.getLocation())) {
			MagicLensFlare.createSharpFlare(engine, source, target.getLocation(), 5, 250, 0, SECONDARY_ARC_CORE, Color.white);
		}
	}

	public CombatEntityAPI findTarget(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		float range = weapon.getRange();
		Vector2f from = projectile.getLocation();
		ShipAPI ship = weapon.getShip();
		if (ship == null) return null;

		Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(from,
				range * 1.5f, range * 1.5f);
		int owner = ship.getOwner();
		CombatEntityAPI best = null;
		float minScore = Float.MAX_VALUE;

		boolean ignoreFlares = ship.getMutableStats().getDynamic().getValue(Stats.PD_IGNORES_FLARES, 0) >= 1;
		ignoreFlares |= weapon.hasAIHint(WeaponAPI.AIHints.IGNORES_FLARES);

		while (iter.hasNext()) {
			Object o = iter.next();
			if (!(o instanceof MissileAPI) && !(o instanceof ShipAPI)) continue;

			CombatEntityAPI other = (CombatEntityAPI) o;
			if (other.getOwner() == owner) continue;
			if (other.getCollisionClass() == CollisionClass.NONE) continue;

			if (other instanceof ShipAPI) {
				ShipAPI otherShip = (ShipAPI) other;
				if (otherShip.isHulk()) continue;
				if (otherShip.isPhased()) continue;
			}

			if (ignoreFlares && other instanceof MissileAPI) {
				MissileAPI missile = (MissileAPI) other;
				if (missile.isFlare()) continue;
			}

			float radius = Misc.getTargetingRadius(from, other, false);
			float dist = Misc.getDistance(from, other.getLocation()) - radius;

			if (dist > range) continue;
			if (!Misc.isInArc(weapon.getCurrAngle(), ARC_ANGLE, from, other.getLocation())) continue;

			float score = dist;
			if (score < minScore) {
				minScore = score;
				best = other;
			}
		}
		return best;
	}

	public Vector2f pickNoTargetDest(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		float spread = 50f;
		float range = weapon.getRange() - spread;
		Vector2f from = projectile.getLocation();
		Vector2f dir = Misc.getUnitVectorAtDegreeAngle(weapon.getCurrAngle());
		dir.scale(range);
		Vector2f.add(from, dir, dir);
		dir = Misc.getPointWithinRadius(dir, spread);
		return dir;
	}
}