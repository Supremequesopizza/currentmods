package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ProximityExplosionEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.DamageType;

import org.lwjgl.util.vector.Vector2f;
import java.util.List;
import org.lazywizard.lazylib.combat.CombatUtils;

public class bt_seilch_charge_onhit implements ProximityExplosionEffect {

    private static final float EFFECT_RADIUS = 300f;
    private static final float FLUX_TO_CONVERT = 300f;
    private static final float KINETIC_DAMAGE_AMOUNT = 150f;

    @Override
    public void onExplosion(DamagingProjectileAPI explosion, DamagingProjectileAPI originalProjectile) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || explosion == null || originalProjectile == null) {
            return;
        }

        Vector2f explosionLocation = explosion.getLocation();
        List<ShipAPI> shipsInRange = CombatUtils.getShipsWithinRange(explosionLocation, EFFECT_RADIUS);

        for (ShipAPI targetShip : shipsInRange) {
            if (!targetShip.isAlive() || targetShip.isPhased()) {
                continue;
            }

            FluxTrackerAPI fluxTracker = targetShip.getFluxTracker();
            if (fluxTracker != null) {
                float currentSoftFlux = fluxTracker.getCurrFlux();
                float currentHardFlux = fluxTracker.getHardFlux();
                float amountToConvert = Math.min(FLUX_TO_CONVERT, currentSoftFlux);

                if (amountToConvert > 0f) {
                    fluxTracker.decreaseFlux(amountToConvert);
                    fluxTracker.setHardFlux(currentHardFlux + amountToConvert);
                }
            }

            ShieldAPI shield = targetShip.getShield();
            if (shield != null && shield.isOn() && shield.isWithinArc(explosionLocation)) {
                engine.applyDamage(
                        targetShip,
                        explosionLocation,
                        KINETIC_DAMAGE_AMOUNT,
                        DamageType.KINETIC,
                        0f,
                        false,
                        false,
                        originalProjectile
                );
            }
        }
    }
}