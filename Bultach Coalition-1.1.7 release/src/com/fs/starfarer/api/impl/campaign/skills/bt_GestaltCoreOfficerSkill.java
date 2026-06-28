package com.fs.starfarer.api.impl.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import java.util.Random;
import java.util.Iterator;
import java.util.LinkedList;

public class bt_GestaltCoreOfficerSkill {


    public static final float ARMOR_HEAL_FRACTION = 0.15f;
    public static final float MAX_REGEN_ARMOR_FRACTION = 0.40f;


    public static final float SHIELD_REFUND_FRACTION = 0.03f;
    public static final float HULL_ARMOR_REFUND_FRACTION = 0.08f;
    public static final float FLUX_REFUND_DURATION = 1f;


    private static final Random random = new Random();

    public static class Level1 implements ShipSkillEffect {
        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            if (stats.getEntity() instanceof ShipAPI) {
                ShipAPI ship = (ShipAPI) stats.getEntity();
                if (!ship.hasListenerOfClass(GestaltFluxRefundListener.class)) {
                    ship.addListener(new GestaltFluxRefundListener(ship));
                }
            }
        }

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            if (stats.getEntity() instanceof ShipAPI) {
                ShipAPI ship = (ShipAPI) stats.getEntity();
                ship.removeListenerOfClass(GestaltFluxRefundListener.class);
            }
        }

        @Override
        public String getEffectDescription(float level) {
            return String.format(
                    "Refunds %d%% flux for shield hits and %d%% flux for armor/hull hits, spread over %.1f seconds.",
                    (int)(SHIELD_REFUND_FRACTION * 100f),
                    (int)(HULL_ARMOR_REFUND_FRACTION * 100f),
                    FLUX_REFUND_DURATION
            );
        }

        @Override
        public String getEffectPerLevelDescription() { return null; }
        @Override
        public ScopeDescription getScopeDescription() { return ScopeDescription.PILOTED_SHIP; }
    }


    public static class FluxRefundInstance {
        float amount;
        float timeRemaining;

        public FluxRefundInstance(float amount, float timeRemaining) {
            this.amount = amount;
            this.timeRemaining = timeRemaining;
        }
    }

    public static class GestaltFluxRefundListener implements DamageDealtModifier, AdvanceableListener {
        private final ShipAPI ship;
        private final LinkedList<FluxRefundInstance> refunds = new LinkedList<>();

        public GestaltFluxRefundListener(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage,
                                        Vector2f point, boolean shieldHit) {
            if (ship == null || !ship.isAlive() || target == null) return null;
            if (damage == null || damage.isDps()) return null;

            if (!(target instanceof ShipAPI)) return null;
            ShipAPI targetShip = (ShipAPI) target;
            if (targetShip.isFighter()) return null;
            if (target.getOwner() == ship.getOwner()) return null;

            float refundFraction = shieldHit ? SHIELD_REFUND_FRACTION : HULL_ARMOR_REFUND_FRACTION;
            float fluxRefund = damage.getDamage() * refundFraction;

            if (fluxRefund > 0f) {
                refunds.add(new FluxRefundInstance(fluxRefund, FLUX_REFUND_DURATION));
            }

            return null;
        }

        @Override
        public void advance(float amount) {
            if (!ship.isAlive()) return;

            Iterator<FluxRefundInstance> iter = refunds.iterator();
            while (iter.hasNext()) {
                FluxRefundInstance ref = iter.next();
                float step = (ref.amount / FLUX_REFUND_DURATION) * amount;
                ship.getFluxTracker().decreaseFlux(step);
                ref.timeRemaining -= amount;
                if (ref.timeRemaining <= 0f) {
                    iter.remove();
                }
            }
        }
    }

    public static class Level2 implements ShipSkillEffect {
        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            if (stats.getEntity() instanceof ShipAPI) {
                ShipAPI ship = (ShipAPI) stats.getEntity();
                CombatEngineAPI engine = Global.getCombatEngine();

                if (engine == null || (engine != null && engine.isSimulation())) {
                    return;
                }

                if (!ship.hasListenerOfClass(GestaltArmorHealingListener.class)) {
                    ship.addListener(new GestaltArmorHealingListener(ship));
                }
            }
        }

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            if (stats.getEntity() instanceof ShipAPI) {
                ShipAPI ship = (ShipAPI) stats.getEntity();
                ship.removeListenerOfClass(GestaltArmorHealingListener.class);
            }
        }

        @Override
        public String getEffectDescription(float level) {
            return String.format(
                    "Repairs armor equal to %.0f%% of damage dealt, up to %.0f%% of max armor.",
                    ARMOR_HEAL_FRACTION * 100f,
                    MAX_REGEN_ARMOR_FRACTION * 100f
            );
        }

        @Override
        public String getEffectPerLevelDescription() { return null; }
        @Override
        public ScopeDescription getScopeDescription() { return ScopeDescription.PILOTED_SHIP; }

        public void createCustomDescription(MutableCharacterStatsAPI charStats, SkillSpecAPI skill, TooltipMakerAPI info, float width) {
            info.addPara("Repairs armor equal to %s of damage dealt, up to %s of max armor.",
                    0f, Misc.getHighlightColor(),
                    (int)(ARMOR_HEAL_FRACTION * 100f) + "%",
                    (int)(MAX_REGEN_ARMOR_FRACTION * 100f) + "%");
        }
    }

    public static class GestaltArmorHealingListener implements DamageDealtModifier {
        protected final ShipAPI ship;
        protected final ArmorGridAPI grid;
        protected final int gridWidth;
        protected final int gridHeight;
        protected float healedAmount = 0f;
        protected final float healLimit;
        protected final float maxArmorPerCell;

        public GestaltArmorHealingListener(ShipAPI ship) {
            this.ship = ship;
            this.grid = ship.getArmorGrid();

            if (this.grid != null) {
                this.maxArmorPerCell = grid.getMaxArmorInCell();
                float[][] gridArray = grid.getGrid();
                this.gridWidth = gridArray.length;
                this.gridHeight = (gridWidth > 0) ? gridArray[0].length : 0;

                float totalMaxArmor = 0f;
                if (gridWidth > 0 && gridHeight > 0) {
                    totalMaxArmor = maxArmorPerCell * gridWidth * gridHeight;
                }
                this.healLimit = totalMaxArmor * MAX_REGEN_ARMOR_FRACTION;
            } else {
                this.maxArmorPerCell = 0f;
                this.gridWidth = 0;
                this.gridHeight = 0;
                this.healLimit = 0f;
            }
        }

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage,
                                        Vector2f point, boolean shieldHit) {
            if (grid == null) return null;
            if (ship == null || !ship.isAlive() || ship.isHulk()) return null;
            if (damage == null || shieldHit || !(target instanceof ShipAPI) ||
                    ((ShipAPI)target).isFighter() || ((ShipAPI)target).isHulk() ||
                    target.getOwner() == ship.getOwner()) return null;
            if (gridWidth <= 0 || gridHeight <= 0) return null;
            if (healedAmount >= healLimit) return null;

            float rawDamageDealt = damage.getDamage();
            float healAmountForThisHit = rawDamageDealt * ARMOR_HEAL_FRACTION;
            healAmountForThisHit = Math.min(healAmountForThisHit, healLimit - healedAmount);
            if (healAmountForThisHit <= 0f) return null;

            float appliedHealThisHit = 0f;
            int attempts = 0;
            int maxAttempts = gridWidth * gridHeight;

            while (appliedHealThisHit < healAmountForThisHit && attempts < maxAttempts) {
                attempts++;
                int x = random.nextInt(gridWidth);
                int y = random.nextInt(gridHeight);

                float currentArmor = grid.getArmorValue(x, y);
                float missingArmor = maxArmorPerCell - currentArmor;

                if (missingArmor > 0) {
                    float healThisCell = Math.min(missingArmor, healAmountForThisHit - appliedHealThisHit);
                    if (healThisCell > 0.01f) {
                        grid.setArmorValue(x, y, currentArmor + healThisCell);
                        healedAmount += healThisCell;
                        appliedHealThisHit += healThisCell;
                    }
                }
                if (healedAmount >= healLimit) break;
            }

            if (appliedHealThisHit > 0) {
                ship.syncWithArmorGridState();
            }
            return null;
        }
    }
}
