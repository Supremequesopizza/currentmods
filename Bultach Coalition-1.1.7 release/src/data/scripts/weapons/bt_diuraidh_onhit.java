package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

public class bt_diuraidh_onhit implements OnHitEffectPlugin {

    private static final float HARD_FLUX_ON_SHIELD_HIT = 100f;
    private static final float BREACH_DAMAGE_AMOUNT = 200f;
    private static final float SIMPLE_HE_DAMAGE_ON_HIT = 100f;
    private static final float ARMOR_THRESHOLD_FOR_BREACH = 3000f;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

        if (!(target instanceof ShipAPI)) {
            return;
        }
        ShipAPI targetShip = (ShipAPI) target;
        if (!targetShip.isAlive()) {
            return;
        }

        if (shieldHit) {
            if (targetShip.getFluxTracker() != null) {
                targetShip.getFluxTracker().increaseFlux(HARD_FLUX_ON_SHIELD_HIT, true);
            }
        } else {

            float shipBaseArmor = targetShip.getHullSpec().getArmorRating();

            if (shipBaseArmor > ARMOR_THRESHOLD_FOR_BREACH) {
                dealArmorDamage(projectile, targetShip, point, BREACH_DAMAGE_AMOUNT);
            } else {
                engine.applyDamage(
                        targetShip,
                        point,
                        SIMPLE_HE_DAMAGE_ON_HIT,
                        DamageType.HIGH_EXPLOSIVE,
                        0f,
                        false,
                        false,
                        projectile.getSource()
                );
            }
        }
    }

    private float dealArmorDamage(DamagingProjectileAPI projectile, ShipAPI target, Vector2f point, float breachDamage) {
        ArmorGridAPI armorGrid = target.getArmorGrid();
        float[][] armorGridValues = armorGrid.getGrid();

        int[] cell = armorGrid.getCellAtLocation(point);
        if (cell == null) return 0f;

        int gridX = cell[0];
        int gridY = cell[1];

        float armorValueInCell = armorGridValues[gridX][gridY];
        float actualArmorDamageDealt = Math.min(breachDamage, armorValueInCell);

        armorGrid.setArmorValue(gridX, gridY, armorValueInCell - actualArmorDamageDealt);

        float remainingDamageToHull = breachDamage - actualArmorDamageDealt;
        if (remainingDamageToHull > 0) {
            Global.getCombatEngine().applyDamage(
                    target,
                    point,
                    remainingDamageToHull,
                    DamageType.HIGH_EXPLOSIVE,
                    0f,
                    true,
                    false,
                    projectile.getSource()
            );
        }
        return breachDamage;
    }
}