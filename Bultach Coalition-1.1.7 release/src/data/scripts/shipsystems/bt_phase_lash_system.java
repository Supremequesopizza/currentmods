package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.EmpArcEntityAPI.EmpArcParams;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class bt_phase_lash_system extends BaseShipSystemScript {

    public static final float MAX_RANGE = 1700f;
    public static final float EMP_DAMAGE = 1500f;
    public static final float PHASE_OVERLOAD_DUR = 2f;
    public static final float LASH_SPEED = 7000f;
    public static final float RENDER_FALLBACK_RANGE = 2000f;

    public static final Color LASH_COLOR = new Color(255, 81, 81, 255);
    public static final Color LASH_CORE_COLOR = new Color(255, 215, 215, 255);
    public static final Color PHASE_ARC_COLOR = new Color(255, 100, 100, 255);
    public static final Color PHASE_CORE_COLOR = new Color(255, 215, 215, 255);

    public static final String TARGET_KEY = "bt_phase_lash_target";
    private boolean fired = false;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        String targetDataKey = ship.getId() + TARGET_KEY;

        if (state == State.IN) {
            if (Global.getCombatEngine().getCustomData().get(targetDataKey) == null) {
                ShipAPI target = findTarget(ship);
                Global.getCombatEngine().getCustomData().put(targetDataKey, target);
            }
        } else if (effectLevel >= 1 && !fired) {
            fired = true;
            Object targetObject = Global.getCombatEngine().getCustomData().get(targetDataKey);
            if (targetObject instanceof ShipAPI) {
                ShipAPI target = (ShipAPI) targetObject;
                if (target != ship) {
                    fireLash(ship, target);
                }
            }
        } else if (state == State.OUT) {
            Global.getCombatEngine().getCustomData().remove(targetDataKey);
        }
    }

    private void fireLash(ShipAPI ship, ShipAPI target) {
        List<WeaponSlotAPI> systemSlots = new ArrayList<>();
        for (WeaponSlotAPI potentialSlot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
            if (potentialSlot.isSystemSlot()) {
                systemSlots.add(potentialSlot);
            }
        }
        if (systemSlots.isEmpty()) return;

        int numArcs;
        switch (target.getHullSize()) {
            case FRIGATE:
                numArcs = 1;
                break;
            case DESTROYER:
                numArcs = 2;
                break;
            case CRUISER:
                numArcs = 3;
                break;
            case CAPITAL_SHIP:
                numArcs = 4;
                break;
            default:
                numArcs = 1;
        }

        CombatEngineAPI engine = Global.getCombatEngine();

        for (int i = 0; i < numArcs; i++) {
            WeaponSlotAPI slot = systemSlots.get(i % systemSlots.size());
            Vector2f from = slot.computePosition(ship);
            Vector2f to = target.getLocation();
            float distance = MathUtils.getDistance(from, to);

            EmpArcParams params = new EmpArcParams();
            params.fadeOutDist = 50f;
            params.minFadeOutMult = 10f;
            params.flickerRateMult = 0.3f;

            Color arcColor = target.isPhased() ? PHASE_ARC_COLOR : LASH_COLOR;
            Color coreColor = target.isPhased() ? PHASE_CORE_COLOR : LASH_CORE_COLOR;

            EmpArcEntityAPI arc = engine.spawnEmpArc(ship, from, ship, target,
                    DamageType.ENERGY,
                    0f,
                    EMP_DAMAGE / numArcs,
                    MAX_RANGE,
                    "tachyon_lance_emp_impact",
                    50f,
                    arcColor,
                    coreColor,
                    params);


            if (distance > RENDER_FALLBACK_RANGE) {
                arc.setCoreWidthOverride(25f);
                arc.setSingleFlickerMode(true);

            } else {
                params.movementDurOverride = Math.max(0.05f, distance / LASH_SPEED);
                arc.setCoreWidthOverride(25f);
                arc.setRenderGlowAtStart(false);
                arc.setFadedOutAtStart(true);
                arc.setSingleFlickerMode(true);
            }
        }

        Global.getSoundPlayer().playSound("energy_lash_fire_at_enemy", 1f, 1f, ship.getLocation(), ship.getVelocity());

        if (target.isPhased()) {
            target.getFluxTracker().beginOverloadWithTotalBaseDuration(PHASE_OVERLOAD_DUR);
            if (target.getFluxTracker().showFloaty() || ship == Global.getCombatEngine().getPlayerShip() || target == Global.getCombatEngine().getPlayerShip()) {
                target.getFluxTracker().showOverloadFloatyIfNeeded("Phase Interdiction!", PHASE_ARC_COLOR, 4f, true);
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        fired = false;
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;
        String targetDataKey = ship.getId() + TARGET_KEY;
        Global.getCombatEngine().getCustomData().remove(targetDataKey);
    }

    private ShipAPI findTarget(ShipAPI ship) {
        if (ship == null) return ship;

        ShipAPI target = ship.getShipTarget();
        if (isValidTarget(ship, target) && MathUtils.getDistance(ship, target) <= MAX_RANGE) {
            return target;
        }

        if (ship == Global.getCombatEngine().getPlayerShip()) {
            target = Misc.findClosestShipEnemyOf(ship, ship.getMouseTarget(), ShipAPI.HullSize.FRIGATE, MAX_RANGE, true);
            if (isValidTarget(ship, target)) {
                return target;
            }
        }

        return ship;
    }

    public static boolean isValidTarget(ShipAPI source, ShipAPI target) {
        if (target == null || target.isFighter() || target.isDrone() || target.isHulk() || !target.isAlive()) {
            return false;
        }
        return source != null && source.getOwner() != target.getOwner();
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (ship == null || system == null) return null;
        if (system.getState() != ShipSystemAPI.SystemState.IDLE) return null;
        if (findTarget(ship) != ship) {
            return "READY";
        }
        if (ship.getShipTarget() != null) {
            return "OUT OF RANGE";
        }
        return "NO TARGET";
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        if (ship == null || system == null) return false;
        if (system.getState() != ShipSystemAPI.SystemState.IDLE) return false;
        return findTarget(ship) != ship;
    }
}