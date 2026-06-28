package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

public class bt_shieldCracker implements OnHitEffectPlugin {

    final float minDamage = 100f,
            maxDamage = 500f,
            lowEff = 0.6f,
            maxEff = 1.1f;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (shieldHit) {
            float shieldEff = Math.max(lowEff, Math.min(maxEff, target.getShield().getFluxPerPointOfDamage()));
            float effectLevel =  1 - ((shieldEff - lowEff) / (maxEff - lowEff));
            float damage = minDamage + ((maxDamage - minDamage) * effectLevel);
            engine.applyDamage(target, point,damage,DamageType.ENERGY,0,false,false,projectile.getSource(),false);
        }
    }
}
