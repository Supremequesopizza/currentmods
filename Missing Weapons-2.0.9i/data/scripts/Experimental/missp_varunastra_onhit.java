package data.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

public class missp_varunastra_onhit implements OnHitEffectPlugin {

    private static final float DAMAGE = 250f;

    @Override
    public void onHit(DamagingProjectileAPI proj,
                      CombatEntityAPI target,
                      Vector2f point,
                      boolean shieldHit,
                      ApplyDamageResultAPI damageResult,
                      CombatEngineAPI engine) {

        if (proj == null || target == null) return;

        Vector2f start = (Vector2f) proj.getCustomData().get("varunastra_start");
        Float half     = (Float) proj.getCustomData().get("varunastra_half");

        if (start == null || half == null) return;

        float dist = Vector2f.sub(point, start, null).length();
        DamageType type = dist >= half ? DamageType.KINETIC : DamageType.HIGH_EXPLOSIVE;

        engine.applyDamage(
                target,
                point,
                DAMAGE,
                type,
                0f,
                false,
                false,
                proj.getSource()
        );
    }
}
