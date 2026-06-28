package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.combat.DisintegratorEffect;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.List;

public class bt_dotOnHit_energy implements OnHitEffectPlugin {

    float dotDuration = 3f;
    float totalDamage = 350f;
    float fractionPerSecond = 1 / dotDuration;
    float breachDamage = 350;
    private static final float ARMOR_THRESHOLD_FOR_BREACH = 3000f;

    @Override
    public void onHit(final DamagingProjectileAPI projectile, final CombatEntityAPI target, final Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, final CombatEngineAPI engine) {
        if (!shieldHit && target instanceof ShipAPI) {
            final ShipAPI shipTarget = (ShipAPI) target;
            final boolean attemptBreachEffect = shipTarget.getHullSpec().getArmorRating() > ARMOR_THRESHOLD_FOR_BREACH;

            engine.addPlugin(new EveryFrameCombatPlugin() {

                float dotTotalDuration = 0;
                float breachDamageDealtThisHit = 0;
                boolean breachEffectProcessed = false;

                final float initialFacing = target.getFacing();
                final Vector2f shipRefHitLoc = new Vector2f(point.x - target.getLocation().x, point.y - target.getLocation().y);
                final IntervalUtil damageTimer = new IntervalUtil(0.25f, 0.25f);
                final IntervalUtil FXTimer = new IntervalUtil(0.1f, 0.1f);

                @Override
                public void advance(float amount, List<InputEventAPI> events) {
                    if (engine.isPaused()) return;

                    dotTotalDuration += amount;
                    damageTimer.advance(amount);
                    FXTimer.advance(amount);
                    Vector2f hitLoc = new Vector2f();
                    if (damageTimer.intervalElapsed() || FXTimer.intervalElapsed()) {
                        hitLoc = VectorUtils.rotate(new Vector2f(shipRefHitLoc), target.getFacing() - initialFacing);
                        hitLoc = new Vector2f(hitLoc.x + target.getLocation().x, hitLoc.y + target.getLocation().y);
                    }
                    if (damageTimer.intervalElapsed()) {
                        engine.applyDamage(target, hitLoc, totalDamage * fractionPerSecond * damageTimer.getIntervalDuration(), DamageType.ENERGY, 0, true, true, projectile.getSource());
                    }
                    if (FXTimer.intervalElapsed()) {
                        engine.addSwirlyNebulaParticle(hitLoc, target.getVelocity(), MathUtils.getRandomNumberInRange(10, 30), MathUtils.getRandomNumberInRange(1, 2), MathUtils.getRandomNumberInRange(0.8f, 1.2f), MathUtils.getRandomNumberInRange(0.3f, 0.7f), MathUtils.getRandomNumberInRange(0.4f, 0.6f), new Color(MathUtils.getRandomNumberInRange(200, 255), MathUtils.getRandomNumberInRange(100, 160), MathUtils.getRandomNumberInRange(0, 60), MathUtils.getRandomNumberInRange(100, 200)), true);
                        engine.addNebulaParticle(hitLoc, target.getVelocity(), MathUtils.getRandomNumberInRange(10, 30), MathUtils.getRandomNumberInRange(1, 2), MathUtils.getRandomNumberInRange(0.8f, 1.2f), MathUtils.getRandomNumberInRange(0.3f, 0.7f), MathUtils.getRandomNumberInRange(0.4f, 0.6f), new Color(MathUtils.getRandomNumberInRange(200, 255), MathUtils.getRandomNumberInRange(100, 160), MathUtils.getRandomNumberInRange(0, 60), MathUtils.getRandomNumberInRange(100, 200)), true);

                        engine.addSwirlyNebulaParticle(hitLoc, target.getVelocity(), MathUtils.getRandomNumberInRange(15, 45), MathUtils.getRandomNumberInRange(3, 4), MathUtils.getRandomNumberInRange(0.8f, 1.2f), MathUtils.getRandomNumberInRange(0.3f, 0.7f), MathUtils.getRandomNumberInRange(0.4f, 0.6f), new Color(MathUtils.getRandomNumberInRange(20, 40), MathUtils.getRandomNumberInRange(20, 40), MathUtils.getRandomNumberInRange(20, 40), MathUtils.getRandomNumberInRange(50, 150)), true);
                        engine.addNebulaParticle(hitLoc, target.getVelocity(), MathUtils.getRandomNumberInRange(15, 45), MathUtils.getRandomNumberInRange(3, 4), MathUtils.getRandomNumberInRange(0.8f, 1.2f), MathUtils.getRandomNumberInRange(0.3f, 0.7f), MathUtils.getRandomNumberInRange(0.4f, 0.6f), new Color(MathUtils.getRandomNumberInRange(20, 40), MathUtils.getRandomNumberInRange(20, 40), MathUtils.getRandomNumberInRange(20, 40), MathUtils.getRandomNumberInRange(50, 150)), true);
                    }
                    if (dotTotalDuration >= dotDuration) engine.removePlugin(this);

                    if (attemptBreachEffect && !breachEffectProcessed) {
                        breachDamageDealtThisHit = dealArmorDamage(projectile, shipTarget, point, bt_dotOnHit_energy.this.breachDamage);
                        breachEffectProcessed = true;
                    }
                }

                private float dealArmorDamage(DamagingProjectileAPI projectile, ShipAPI targetShip, Vector2f point, float armorDamage) {
                    float damageDealt = 0f;

                    ArmorGridAPI grid = targetShip.getArmorGrid();
                    int[] cell = grid.getCellAtLocation(point);
                    if (cell == null) return damageDealt;

                    int gridWidth = grid.getGrid().length;
                    int gridHeight = grid.getGrid()[0].length;

                    float damageTypeMult = DisintegratorEffect.getDamageTypeMult(projectile.getSource(), targetShip);

                    for (int i = -2; i <= 2; i++) {
                        for (int j = -2; j <= 2; j++) {
                            if ((i == 2 || i == -2) && (j == 2 || j == -2)) continue;

                            int cx = cell[0] + i;
                            int cy = cell[1] + j;

                            if (cx < 0 || cx >= gridWidth || cy < 0 || cy >= gridHeight) continue;

                            float damMult = 1/30f;
                            if (i == 0 && j == 0) {
                                damMult = 1/15f;
                            } else if (i <= 1 && i >= -1 && j <= 1 && j >= -1) {
                                damMult = 1/15f;
                            } else {
                                damMult = 1/30f;
                            }

                            float armorInCell = grid.getArmorValue(cx, cy);
                            float damage = armorDamage * damMult * damageTypeMult;
                            damage = Math.min(damage, armorInCell);
                            if (damage <= 0) continue;

                            targetShip.getArmorGrid().setArmorValue(cx, cy, Math.max(0, armorInCell - damage));
                            damageDealt += damage;
                        }
                    }

                    if (damageDealt > 0) {
                        if (Misc.shouldShowDamageFloaty(projectile.getSource(), targetShip)) {
                            engine.addFloatingDamageText(point, damageDealt, Misc.FLOATY_ARMOR_DAMAGE_COLOR, targetShip, projectile.getSource());
                        }
                        targetShip.syncWithArmorGridState();
                    }
                    return damageDealt;
                }

                @Override
                public void init(CombatEngineAPI engine) {
                }

                @Override
                public void renderInWorldCoords(ViewportAPI viewport) {
                }

                @Override
                public void renderInUICoords(ViewportAPI viewport) {
                }

                @Override
                public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
                }
            });
        }
        if (shieldHit){
            target.getShield().setActiveArc(target.getShield().getActiveArc() - 10f);
        }
    }
}