package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.Global;

public class bt_holytorp_onfire implements OnFireEffectPlugin {

    private static final String HOLY_TORP_PROJECTILE_ID = "ork_holy_torp_proj";

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (projectile != null && projectile.getProjectileSpecId() != null &&
                projectile.getProjectileSpecId().equals(HOLY_TORP_PROJECTILE_ID)) {

            if (!projectile.isFading() && engine != null) {
                engine.addPlugin(new bt_holytorp_aura(projectile));
            }
        }
    }
}