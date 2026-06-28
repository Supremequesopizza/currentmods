package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;

import java.util.HashMap;
import java.util.Map;

public class bt_arm_everyframe implements EveryFrameWeaponEffectPlugin {


    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        for (DamagingProjectileAPI p : engine.getProjectiles()){
            if (p.getWeapon() == weapon)
                engine.removeEntity(p);
        }
    }

}
