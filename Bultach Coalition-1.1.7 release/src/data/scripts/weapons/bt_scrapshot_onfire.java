package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

public class bt_scrapshot_onfire implements OnFireEffectPlugin {

    float count = 0;
    String smallID = "ork_scrapshot_frag";

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {

        projectile.getVelocity().scale(1f + MathUtils.getRandomNumberInRange(-0.15f, 0.15f));


        float randomNum = (float) Math.random();
        if (randomNum <= 0.55f){
            DamagingProjectileAPI spawnedProjSmall = (DamagingProjectileAPI) engine.spawnProjectile(projectile.getSource(), weapon, smallID, projectile.getLocation(), projectile.getFacing(), weapon.getShip().getVelocity());
            spawnedProjSmall.getVelocity().scale(1f + MathUtils.getRandomNumberInRange(-0.15f, 0.15f));
            engine.removeEntity(projectile);
        }


        if (count == 0){
            Global.getSoundPlayer().playSound("bt_scrapshot_fire", 1, 1f, weapon.getLocation(), weapon.getShip().getVelocity());
        }
        if (count == 19){
            count = 0;
        } else {
            count++;
        }

    }
}