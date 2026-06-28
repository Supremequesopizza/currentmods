package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;

public class bt_repair_missile_plugin implements OnFireEffectPlugin {

    public bt_repair_missile_plugin() {
    }

    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        float delay = 0.25f + 0.75f * (float) Math.random();
        weapon.setRefireDelay(delay);
    }
}