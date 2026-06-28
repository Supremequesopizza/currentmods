package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A modified Burndrive script which transfers most visual changes to a sub-module, now bultach'd
 * @author Nicke535
 */
public class bt_satharthrusters extends BaseShipSystemScript {
    //The hull ID of any modules we want to affect must contain this string
    private static final String HULL_ID_TO_AFFECT = "sgr_engine_";
    public static final float ACCELERATION_MULT = 5f;
    public static final float MAX_SPEED_INCREASE = 100f;

    //Visual config: change some visual parameters
    public static final Color ENGINE_COLOR = new Color(255, 88, 50);
    public static final float ENGINE_WIDTH_MULT = 1.5f;
    public static final float ENGINE_LENGTH_MULT = 2f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        //Burndrive-related code, for our stat bonuses
        if (state == State.OUT) {
            stats.getMaxSpeed().unmodify(id);
        } else {
            stats.getMaxSpeed().modifyFlat(id, MAX_SPEED_INCREASE * effectLevel);
        }
        stats.getAcceleration().modifyMult(id,1f + (ACCELERATION_MULT-1f)*effectLevel);
        stats.getDeceleration().modifyMult(id,1f + (ACCELERATION_MULT-1f)*effectLevel);

        //Finds our OTHER ships [our modules] to apply visuals to, and applies all visuals to those
        List<ShipAPI> engineModules = new ArrayList<>();
        for (ShipAPI module : ship.getChildModulesCopy()) {
            //Dead or non-engine modules don't count
            if (!module.getHullSpec().getHullId().contains(HULL_ID_TO_AFFECT) || !module.isAlive()) {
                continue;
            }

            //Also, if the module is flamed out, it should also not count
            if (module.getEngineController().isFlamedOut()) {
                continue;
            }

            engineModules.add(module);
        }
        for (ShipAPI module : engineModules) {
            //Change engine size and color
            module.getEngineController().fadeToOtherColor(this, ENGINE_COLOR, ENGINE_COLOR, effectLevel, 1.0f);
            module.getEngineController().extendFlame(this.getClass().getName(),
                    1f+((ENGINE_LENGTH_MULT-1f)*effectLevel),
                    1f+((ENGINE_WIDTH_MULT-1f)*effectLevel),
                    1f+((ENGINE_WIDTH_MULT-1f)*effectLevel));

            }
        }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        //Finds our engine modules, and make sure the system can't be activated if they're gone/dead
        List<ShipAPI> engineModules = new ArrayList<>();
        for (ShipAPI module : ship.getChildModulesCopy()) {
            //Dead or non-engine modules don't count
            if (!module.getHullSpec().getHullId().contains(HULL_ID_TO_AFFECT) || !module.isAlive()) {
                continue;
            }

            //Also, if the module is flamed out, it should also not count
            if (module.getEngineController().isFlamedOut()) {
                continue;
            }

            engineModules.add(module);
        }
        if (engineModules.isEmpty()) {
            return false;
        }
        return super.isUsable(system, ship);
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("all power to thrusters", false);
        }
        return null;
    }
}