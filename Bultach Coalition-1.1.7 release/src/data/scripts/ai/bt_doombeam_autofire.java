package data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class bt_doombeam_autofire implements AutofireAIPlugin {

    private static final float MIN_FIRING_TIME = 6f;
    private static final float MAX_OWN_FLUX_LEVEL = 0.7f;
    private static final float MAX_SWITCH_ANGLE = 20f;

    private final WeaponAPI weapon;
    private final ShipAPI ship;

    private ShipAPI target;
    private float timeSpentFiring = 0f;
    private final IntervalUtil targetFindInterval = new IntervalUtil(0.1f, 0.2f);


    public bt_doombeam_autofire(WeaponAPI weapon) {
        this.weapon = weapon;
        this.ship = weapon.getShip();
    }

    @Override
    public void advance(float amount) {
        if (Global.getCombatEngine().isPaused() || ship == null) {
            return;
        }

        if (weapon.isFiring()) {
            timeSpentFiring += amount;
        } else {
            timeSpentFiring = 0f;
        }

        targetFindInterval.advance(amount);
        if (targetFindInterval.intervalElapsed()) {
            if (weapon.isFiring() && timeSpentFiring < MIN_FIRING_TIME) {
                return;
            }

            if (ship.getFluxTracker().getFluxLevel() >= MAX_OWN_FLUX_LEVEL) {
                target = null;
                return;
            }


            CombatEntityAPI potentialTarget = AIUtils.getNearestEnemy(ship);
            ShipAPI newTarget = null;
            if (potentialTarget instanceof ShipAPI) {
                ShipAPI potentialShip = (ShipAPI) potentialTarget;

                if (!potentialShip.isFighter() && !potentialShip.isDrone() && !potentialShip.isPhased() && MathUtils.getDistance(ship, potentialTarget) <= weapon.getRange()) {
                    float angleToNewTarget = VectorUtils.getAngle(weapon.getLocation(), potentialShip.getLocation());
                    if (Math.abs(MathUtils.getShortestRotation(weapon.getCurrAngle(), angleToNewTarget)) < MAX_SWITCH_ANGLE) {
                        newTarget = potentialShip;
                    }
                }
            }
            target = newTarget;
        }
    }

    @Override
    public boolean shouldFire() {
        if (weapon.isFiring() && timeSpentFiring < MIN_FIRING_TIME) {
            return true;
        }

        if (target == null || !target.isAlive() || target.isPhased()) {
            return false;
        }

        if (weapon.isFiring()) {
            return true;
        }

        float angleToTarget = VectorUtils.getAngle(weapon.getLocation(), target.getLocation());
        return Math.abs(MathUtils.getShortestRotation(weapon.getCurrAngle(), angleToTarget)) < 5f;
    }

    @Override
    public void forceOff() {
        timeSpentFiring = 0f;
        target = null;
    }

    @Override
    public Vector2f getTarget() {
        if (target != null) {
            return target.getLocation();
        }
        return null;
    }

    @Override
    public ShipAPI getTargetShip() {
        return target;
    }

    @Override
    public WeaponAPI getWeapon() {
        return weapon;
    }

    @Override
    public MissileAPI getTargetMissile() {
        return null;
    }
}