package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;

public class ork_shotgunfire implements OnFireEffectPlugin {

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        Global.getSoundPlayer().playSound("shotgun_shot", 1, 1f, weapon.getLocation(), weapon.getShip().getVelocity());
        projectile.getVelocity().scale(MathUtils.getRandomNumberInRange(0.7f, 1.1f));
    }
}