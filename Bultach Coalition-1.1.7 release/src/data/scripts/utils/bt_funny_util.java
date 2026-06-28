package data.scripts.utils;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

public class bt_funny_util {

    public static final String ABLATIVE_ARMOR_CHECK = "ablative_armor";
    public static final int FP_THRESHOLD = 100;
    public static final float SHIELD_EFFICIENCY_THRESHOLD = 0.4f;
    public static final float PUNISHMENT_EXPONENT = 1.5f;
    public static final float MAX_DAMAGE_PER_HIT = 100000f;
    public static final float MAX_FACTOR_PER_CATEGORY = 10.0f;

    private static class PunishmentProfile {
        float fluxThreshold;
        float armorThreshold;
        float hullThreshold;
        int sModCountThreshold;

        PunishmentProfile(float flux, float armor, float hull, int sModThreshold) {
            this.fluxThreshold = flux;
            this.armorThreshold = armor;
            this.hullThreshold = hull;
            this.sModCountThreshold = sModThreshold;
        }
    }

    private static final PunishmentProfile PROFILE_FRIGATE = new PunishmentProfile(
            4000f, 750f, 3100f, 4);
    private static final PunishmentProfile PROFILE_DESTROYER = new PunishmentProfile(
            6000f, 1000f, 5500f, 4);
    private static final PunishmentProfile PROFILE_CRUISER = new PunishmentProfile(
            13000f, 1700f, 15000f, 4);
    private static final PunishmentProfile PROFILE_CAPITAL = new PunishmentProfile(
            28000f, 2100f, 30000f, 4);
    private static final PunishmentProfile PROFILE_DEFAULT = PROFILE_CRUISER;

    private static PunishmentProfile getProfileForShip(ShipAPI ship) {
        if (ship == null || ship.getHullSpec() == null) return PROFILE_DEFAULT;
        switch (ship.getHullSize()) {
            case FRIGATE: return PROFILE_FRIGATE;
            case DESTROYER: return PROFILE_DESTROYER;
            case CRUISER: return PROFILE_CRUISER;
            case CAPITAL_SHIP: return PROFILE_CAPITAL;
            default: return PROFILE_DEFAULT;
        }
    }

    public static boolean hasAblativeArmor(ShipAPI targetShip) {
        if (targetShip == null || targetShip.getVariant() == null) return false;
        return targetShip.getVariant().hasHullMod(ABLATIVE_ARMOR_CHECK);
    }

    public static boolean isHighFleetPointTarget(ShipAPI targetShip) {
        if (targetShip == null) return false;
        FleetMemberAPI member = targetShip.getFleetMember();
        if (member != null) return member.getFleetPointCost() > FP_THRESHOLD;
        if (targetShip.getHullSpec() != null) return targetShip.getHullSpec().getFleetPoints() > FP_THRESHOLD;
        return false;
    }

    public static boolean hasExtremeShieldEfficiency(ShipAPI targetShip) {
        if (targetShip == null || targetShip.getShield() == null || !targetShip.getShield().isOn() || targetShip.isPhased()) {
            return false;
        }
        ShieldAPI shield = targetShip.getShield();
        return shield.getFluxPerPointOfDamage() <= SHIELD_EFFICIENCY_THRESHOLD;
    }

    public static float getGestaltPunishmentDamageMultiplier(ShipAPI targetShip) {
        if (targetShip == null || !targetShip.isAlive() || targetShip.isPhased() || targetShip.isFighter()) {
            return 1.0f;
        }
        if (targetShip.getFluxTracker() == null || targetShip.getArmorGrid() == null ||
                targetShip.getMutableStats() == null || targetShip.getVariant() == null ) {
            return 1.0f;
        }

        PunishmentProfile profile = getProfileForShip(targetShip);
        float totalScalingMultiplier = 1.0f;

        float targetMaxFlux = targetShip.getFluxTracker().getMaxFlux();
        float fluxRatio = targetMaxFlux / profile.fluxThreshold;
        float fluxFactor = 1.0f;
        if (fluxRatio > 1.0f) {
            fluxFactor = (float) Math.pow(fluxRatio, PUNISHMENT_EXPONENT);
        }
        fluxFactor = Math.min(fluxFactor, MAX_FACTOR_PER_CATEGORY);
        totalScalingMultiplier *= fluxFactor;

        float armorFactor = 1.0f;
        if (!hasAblativeArmor(targetShip)) {
            float currentArmorRating = targetShip.getArmorGrid().getArmorRating();
            float armorRatio = currentArmorRating / profile.armorThreshold;
            if (armorRatio > 1.0f) {
                armorFactor = (float) Math.pow(armorRatio, PUNISHMENT_EXPONENT);
                if (targetShip.getMutableStats().getEffectiveArmorBonus() != null) {
                    armorFactor *= targetShip.getMutableStats().getEffectiveArmorBonus().getMult();
                }
            }
        }
        armorFactor = Math.min(armorFactor, MAX_FACTOR_PER_CATEGORY);
        totalScalingMultiplier *= armorFactor;

        ShipVariantAPI variant = targetShip.getVariant();
        int smodCount = variant.getSMods().size();
        float smodFactor = 1.0f;
        if (smodCount > profile.sModCountThreshold) {

            float punishableSModRatio = (float) smodCount / profile.sModCountThreshold;
            smodFactor = (float) Math.pow(punishableSModRatio, PUNISHMENT_EXPONENT);
        }
        smodFactor = Math.min(smodFactor, MAX_FACTOR_PER_CATEGORY);
        totalScalingMultiplier *= smodFactor;


        float targetMaxHull = targetShip.getMaxHitpoints();
        float hullRatio = targetMaxHull / profile.hullThreshold;
        float hullFactor = 1.0f;
        if (hullRatio > 1.0f) {
            hullFactor = (float) Math.pow(hullRatio, PUNISHMENT_EXPONENT);
        }
        hullFactor = Math.min(hullFactor, MAX_FACTOR_PER_CATEGORY);
        totalScalingMultiplier *= hullFactor;

        return Math.max(1.0f, totalScalingMultiplier);
    }

    public static float capFinalDamage(float damageAmount) {
        return Math.min(damageAmount, MAX_DAMAGE_PER_HIT);
    }

    public static float getScaledAndCappedDamage(float baseDamage, ShipAPI targetShip) {
        if (targetShip == null) return baseDamage;
        float scalingMultiplier = getGestaltPunishmentDamageMultiplier(targetShip);
        float scaledDamage = baseDamage * scalingMultiplier;
        return capFinalDamage(scaledDamage);
    }
}