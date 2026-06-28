package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;

public class ReactorEffect implements EveryFrameWeaponEffectPlugin {

    public static float normalizeAngle(float angleDeg) {
        return (angleDeg % 360f + 360f) % 360f;
    }

    private final float currDir = Math.signum((float) Math.random() - 0.5f);

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        weapon.getSprite().setAdditiveBlend();
        if (engine.isPaused()) {
            return;
        }
        if (weapon.getShip().isHulk()) {
            return;
        }

        float curr = weapon.getCurrAngle();
        curr += currDir * amount * 36f;
        weapon.setCurrAngle(curr);
    }

}
