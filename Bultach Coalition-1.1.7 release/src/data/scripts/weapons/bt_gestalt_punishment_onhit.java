package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

public class bt_gestalt_punishment_onhit implements OnHitEffectPlugin {
    public static final float MAX_FLUX_SCALING = 35000f;
    public static final float MAX_ARMOR_SCALING = 2300f;
    public static final float ARMOR_EXPONENT = 1.5f;
    public static final float SMOD_EXPONENT_BASE = 1.5f;
    public static final float ARMOR_BREACH_THRESHOLD = 2700f;
    float breachDamage = 400f;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (target instanceof ShipAPI && !shieldHit) {
            ShipAPI targetShip = (ShipAPI) target;

            float totalDamageMult = 2f;

            // Flux-based scaling

            float fluxMult = Math.max(targetShip.getFluxTracker().getMaxFlux(), MAX_FLUX_SCALING) / MAX_FLUX_SCALING;
            totalDamageMult *= fluxMult;

            // Armor-based scaling

            float armorRating = targetShip.getArmorGrid().getArmorRating();
            float armorMult = (float) Math.pow(armorRating / MAX_ARMOR_SCALING, ARMOR_EXPONENT);
            armorMult *= targetShip.getMutableStats().getEffectiveArmorBonus().getMult();
            totalDamageMult *= armorMult;

            // Breach onhit if you're TOO good at armormaxxing

            if (armorRating > ARMOR_BREACH_THRESHOLD) {
                float breachDamageDealt = dealArmorDamage(projectile, targetShip, point, breachDamage);

                if (breachDamageDealt > 0) {
                  //  engine.addFloatingText(point, "lol!", 20f,
                           // Global.getSettings().getColor("damageTextColor"), target, 0.5f, 2f);

                   // Global.getLogger(this.getClass()).info("Breach damage dealt: " + breachDamageDealt);
                }
            }

            // S-mod scaling, only applies if you're a barcoder lol, only way to keep the difficulty

            int smodCount = targetShip.getVariant().getSMods().size();
            if (smodCount >= 4) {
                float smodMult = (float) Math.pow(SMOD_EXPONENT_BASE, smodCount - 3);
                totalDamageMult *= smodMult;
            }

            // Apply total damage multiplier. They add up because lol
            projectile.getDamage().getModifier().modifyMult("bultach_fuck_smods", totalDamageMult);

            // Debug logging
           // engine.addFloatingText(point, "Damage Mult: " + totalDamageMult, 20f,
           //         Global.getSettings().getColor("standardTextColor"), target, 0.5f, 2f);

         //   Global.getLogger(this.getClass()).info("Damage Multiplier Applied: " + totalDamageMult);
          //  Global.getLogger(this.getClass()).info("Armor Multiplier: " + armorMult);
         //   Global.getLogger(this.getClass()).info("Flux Multiplier: " + fluxMult);
         //   Global.getLogger(this.getClass()).info("S-Mod Multiplier: " + (smodCount >= 4 ? Math.pow(SMOD_EXPONENT_BASE, smodCount - 3) : 1f));
        }
    }

    private float dealArmorDamage(DamagingProjectileAPI projectile, ShipAPI target, Vector2f point, float breachDamage) {
        ArmorGridAPI armorGrid = target.getArmorGrid();
        float[][] armorGridValues = armorGrid.getGrid();
        float cellSize = armorGrid.getCellSize();

        int[] cell = armorGrid.getCellAtLocation(point);
        if (cell == null) return 0f;

        int gridX = cell[0];
        int gridY = cell[1];

        float armorValue = armorGridValues[gridX][gridY];
        float armorDamageDealt = Math.min(breachDamage, armorValue);

        armorGridValues[gridX][gridY] -= armorDamageDealt;

        if (armorGridValues[gridX][gridY] < 0) armorGridValues[gridX][gridY] = 0;

        float remainingDamage = breachDamage - armorDamageDealt;
        if (remainingDamage > 0) {
            Global.getCombatEngine().applyDamage(
                    target,
                    point,
                    remainingDamage,
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
