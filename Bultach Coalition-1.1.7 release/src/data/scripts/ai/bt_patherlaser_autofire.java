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

public class bt_patherlaser_autofire implements AutofireAIPlugin {

    private static final float MIN_FIRING_TIME = 0.5f;
    private static final float AIM_ACCURACY_DEG = 1f;

    private final WeaponAPI weapon;
    private ShipAPI target;
    private float timeSpentFiring = 0f;
    private final IntervalUtil targetFindInterval = new IntervalUtil(0.2f, 0.3f);

    public bt_patherlaser_autofire(WeaponAPI weapon) {
        this.weapon = weapon;
    }

    @Override
    public void advance(float amount) {
        if (Global.getCombatEngine().isPaused()) {
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

            CombatEntityAPI potentialTarget = AIUtils.getNearestEnemy(weapon.getShip());
            if (potentialTarget instanceof ShipAPI) {
                if (MathUtils.getDistance(weapon.getShip(), potentialTarget) <= weapon.getRange()) {
                    target = (ShipAPI) potentialTarget;
                } else {
                    target = null;
                }
            } else {
                target = null;
            }
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
        return Math.abs(MathUtils.getShortestRotation(weapon.getCurrAngle(), angleToTarget)) < AIM_ACCURACY_DEG;
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