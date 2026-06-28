package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.magiclib.util.MagicLensFlare;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class BurnerDriveStats extends BaseShipSystemScript {

	private static Map mag = new HashMap();
	static {
		mag.put(ShipAPI.HullSize.FIGHTER, 40f);
		mag.put(ShipAPI.HullSize.FRIGATE, 150f);
		mag.put(ShipAPI.HullSize.DESTROYER, 255f);
		mag.put(ShipAPI.HullSize.CRUISER, 420f);
		mag.put(ShipAPI.HullSize.CAPITAL_SHIP, 420f);
	}

	private Color color_smoke = new Color(255,215,215,155);
	private Color color = new Color(45,20,25,155);
	private Color color_flare = new Color(245,120,25,255);

	private boolean kabloey = false;
	private float kabloeyTimer = 0;
	private int prevSmoke = 1;

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		}
		stats.getMaxSpeed().modifyFlat(id, 500f * effectLevel);
		stats.getAcceleration().modifyPercent(id, 10000f * effectLevel);
		stats.getDeceleration().modifyMult(id, 0 * effectLevel);
		stats.getTurnAcceleration().modifyFlat(id, 90f * effectLevel);
		stats.getTurnAcceleration().modifyPercent(id, 200f * effectLevel);
		stats.getMaxTurnRate().modifyFlat(id, 90f);
		stats.getMaxTurnRate().modifyPercent(id, 100f * effectLevel);

		if(!kabloey){//I hate vfx
			float size = (float) mag.get(ship.getHullSize());
			Vector2f offset = new Vector2f(ship.getHullSpec().getWeaponSlotAPI("WS_ORIONSLOT").getLocation().x,0);
			VectorUtils.rotate(offset,ship.getFacing());
			Vector2f loc = null;
			loc = Vector2f.add(offset,ship.getLocation(),loc);
			//Vector2f loc = ship.getHullSpec().getWeaponSlotAPI("WS_ORIONSLOT").getLocation();
			Vector2f vel = new Vector2f(0,0);

			if(!ship.getHullSize().equals(ShipAPI.HullSize.FIGHTER)){
				DamagingExplosionSpec spec = new DamagingExplosionSpec(1, 50, 10, 500,
						100, CollisionClass.PROJECTILE_NO_FF, CollisionClass.PROJECTILE_NO_FF,
						1, 20, 0.5f, 50, color_flare, color_flare);
				Global.getCombatEngine().spawnDamagingExplosion(spec,ship,loc);
			}
			MagicLensFlare.createSharpFlare(Global.getCombatEngine(),ship,loc,5,size,0,color_flare,color_smoke);
			Global.getCombatEngine().spawnExplosion(loc,vel,color,size*1.5f,1.4f);
			kabloey = true;
		}
		if(!ship.getHullSize().equals(ShipAPI.HullSize.FIGHTER)) {
			if (kabloeyTimer > 0.1f) {
				float size = (float) mag.get(ship.getHullSize());
				Vector2f offset = new Vector2f(ship.getHullSpec().getWeaponSlotAPI("WS_ORIONSLOT").getLocation().x, 0);
				VectorUtils.rotate(offset, ship.getFacing());
				Vector2f loc = null;
				loc = Vector2f.add(offset, ship.getLocation(), loc);
				Vector2f vel = new Vector2f(0, 0);
				//ship.addAfterimage(color, 0, 0, 0, 0, 1, 0f, 0.1f, 0.2f, false, false, false);
				Global.getCombatEngine().addSmokeParticle(loc, vel, size * 0.5f, 0.1f * prevSmoke, 1f, color);
				prevSmoke++;
				kabloeyTimer = 0;
			} else {
				kabloeyTimer += Global.getCombatEngine().getElapsedInLastFrame();
			}
		}

	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		prevSmoke = 1;
		kabloeyTimer = 0;
		kabloey = false;
	}

	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("Inertial dampers disabled - personal mag-stab mandatory", true);
		}
		return null;
	}
}
