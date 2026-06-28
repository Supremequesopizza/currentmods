package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

public class missp_varunastra_onhit implements OnHitEffectPlugin {

    private static final float DAMAGE = 300f;

    @Override
    public void onHit(DamagingProjectileAPI proj,
                      CombatEntityAPI target,
                      Vector2f point,
                      boolean shieldHit,
                      ApplyDamageResultAPI damageResult,
                      CombatEngineAPI engine) {

        if (proj == null || target == null || point == null || engine == null) return;

        // ===== ORIGINAL DAMAGE CODE (UNCHANGED) =====
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
        // ===== END ORIGINAL DAMAGE CODE =====


        // ===== SOUND CODE ONLY =====
        boolean shieldVisualHit = false;

        if (target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
            ShieldAPI shield = ship.getShield();
            if (shield != null && shield.isOn() && shield.isWithinArc(point)) {
                shieldVisualHit = true;
            }
        }

        String soundId;

        if (shieldVisualHit) {
            soundId = "varunastra_shield_heavy";
        } else if (damageResult != null && damageResult.getDamageToHull() > 0f) {
            soundId = "varunastra_hit_solid";
        } else {
            soundId = "varunastra_hit_heavy";
        }

        Global.getSoundPlayer().playSound(
                soundId,
                1.0f,
                1.2f,
                point,
                proj.getVelocity()
        );
        // ===== END SOUND CODE =====
    }
}
