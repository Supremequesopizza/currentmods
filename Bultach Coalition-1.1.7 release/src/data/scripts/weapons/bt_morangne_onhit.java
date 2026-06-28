package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;

public class bt_morangne_onhit implements OnHitEffectPlugin {

    private static final float EMP_DAMAGE_ON_MAX_ROF = 25f;
    private static final float EMP_ARC_THICKNESS = 10f;
    private static final String EMP_IMPACT_SOUND = "tachyon_lance_emp_impact";
    private static final Color EMP_FRINGE_COLOR = new Color(255, 100, 100, 0);
    private static final Color EMP_CORE_COLOR = new Color(255, 220, 220, 0);

    private String getUniqueMemoryKeyForWeapon(WeaponAPI weapon) {
        if (weapon == null || weapon.getSlot() == null) {
            return null;
        }
        return bt_ramping_rotary.ROF_BONUS_MEM_KEY_PREFIX + weapon.getSlot().getId();
    }

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (projectile == null || target == null || engine == null) {
            return;
        }

        WeaponAPI weapon = projectile.getWeapon();
        if (weapon == null) {
            return;
        }

        ShipAPI firingShip = projectile.getSource();
        if (firingShip == null) {
            return;
        }

        Float currentRoFBonus = null;
        String uniqueKey = getUniqueMemoryKeyForWeapon(weapon);

        if (uniqueKey != null && firingShip.getCustomData().containsKey(uniqueKey)) {
            Object bonusObj = firingShip.getCustomData().get(uniqueKey);
            if (bonusObj instanceof Float) {
                currentRoFBonus = (Float) bonusObj;
            }
        }

        if (currentRoFBonus == null) {
            return;
        }

        boolean atMaxRoF = (currentRoFBonus >= bt_ramping_rotary.MAX_ROF_BONUS * 0.99f);

        if (atMaxRoF) {
            engine.spawnEmpArc(
                    projectile.getSource(),
                    point,
                    target,
                    target,
                    DamageType.ENERGY,
                    0f,
                    EMP_DAMAGE_ON_MAX_ROF,
                    300f,
                    EMP_IMPACT_SOUND,
                    EMP_ARC_THICKNESS,
                    EMP_FRINGE_COLOR,
                    EMP_CORE_COLOR
            );
        }
    }
}