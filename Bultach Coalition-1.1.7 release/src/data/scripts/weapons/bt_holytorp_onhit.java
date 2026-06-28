package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.ProximityExplosionEffect;
import com.fs.starfarer.api.input.InputEventAPI;
import data.campaign.ids.bultachazoid_IDS;

import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import data.scripts.utilities.bt_yoinked_graphicLibEffects;

import java.awt.Color;
import java.util.Collection;
import java.util.List;

public class bt_holytorp_onhit implements ProximityExplosionEffect {

    public static final float AOE_RADIUS = 1000f;
    public static final float EFFECT_DURATION_SECONDS = 12f;

    public static final String GESTALT_TAG = bultachazoid_IDS.GESTALT;

    public static final String ENEMY_DEBUFF_ID = "custom_aoe_vulnerability";
    public static final float ENEMY_VULNERABILITY_PER_STACK = 0.15f;
    public static final int ENEMY_MAX_STACKS = 3;
    public static final Color ENEMY_JITTER_COLOR = new Color(255, 0, 0, 75);

    public static final String ALLY_BUFF_ID = "custom_aoe_gestalt_boost";
    public static final float ALLY_FLUX_DISSIPATION_PER_STACK = 0.10f;
    public static final int ALLY_MAX_STACKS = 3;
    public static final Color ALLY_JITTER_COLOR = new Color(255, 247, 214, 178);

    private static final Color CORE_EXPLOSION_COLOR_VIS = new Color(255, 232, 152, 255);
    private static final Color CORE_GLOW_COLOR_VIS = new Color(255, 246, 200, 150);
    private static final Color EXPLOSION_COLOR_VIS = new Color(255, 242, 176, 10);
    private static final Color FLASH_GLOW_COLOR_VIS = new Color(241, 237, 215, 200);
    private static final Color GLOW_COLOR_VIS = new Color(255, 244, 172, 50);
    private static final Vector2f ZERO_VEL = new Vector2f();
    private static final int NUM_PARTICLES_VIS = 50;

    public void triggerAoECenteredOnProjectile(DamagingProjectileAPI projectileThatExploded) {
        if (projectileThatExploded == null || projectileThatExploded.getSource() == null) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        ShipAPI sourceShip = projectileThatExploded.getSource();
        Vector2f location = projectileThatExploded.getLocation();

        spawnVisualsAndSoundFX(sourceShip, location, engine);

        List<ShipAPI> shipsInRange = CombatUtils.getShipsWithinRange(location, AOE_RADIUS);

        for (ShipAPI target : shipsInRange) {
            if (target == sourceShip) continue;

            if (!target.isAlive() || target.isFighter()) {
                continue;
            }
            if (target.getHullSpec() == null || target.getHullSpec().getHullId() == null || target.getHullSpec().getHullId().isEmpty()) {
                continue;
            }

            boolean targetIsGestalt = target.getHullSpec().hasTag(GESTALT_TAG) || target.hasTag(GESTALT_TAG);

            if (targetIsGestalt) {
                applyEffectToShip(target, true);
            } else {
                applyEffectToShip(target, false);
            }
        }
    }

    private void applyEffectToShip(ShipAPI target, boolean isGestaltEffect) {
        AoeEffectListener listener = null;
        String targetEffectId = isGestaltEffect ? ALLY_BUFF_ID : ENEMY_DEBUFF_ID;

        for (AdvanceableListener l : target.getListeners(AoeEffectListener.class)) {
            AoeEffectListener potentialListener = (AoeEffectListener) l;
            if (potentialListener.getEffectId().equals(targetEffectId)) {
                listener = potentialListener;
                break;
            }
        }

        if (listener != null) {
            listener.refreshEffect();
        } else {
            String oppositeEffectId = isGestaltEffect ? ENEMY_DEBUFF_ID : ALLY_BUFF_ID;
            AoeEffectListener oldListenerOfType = null;
            for (AdvanceableListener l : target.getListeners(AoeEffectListener.class)) {
                AoeEffectListener potentialOldListener = (AoeEffectListener) l;
                if (potentialOldListener.getEffectId().equals(oppositeEffectId)) {
                    oldListenerOfType = potentialOldListener;
                    break;
                }
            }
            if (oldListenerOfType != null) {
                oldListenerOfType.cleanup();
                target.removeListener(oldListenerOfType);
            }

            AoeEffectListener newListener = new AoeEffectListener(target, isGestaltEffect);
            target.addListener(newListener);
        }
    }

    private void spawnVisualsAndSoundFX(ShipAPI explosionSource, Vector2f point, CombatEngineAPI engine) {
        float CoreExplosionRadius = 200f;
        float CoreExplosionDuration = 1f;
        float ExplosionRadius = 500f;
        float ExplosionDuration = 2f;
        float CoreGlowRadius = 500f;
        float CoreGlowDuration = 1f;
        float GlowRadius = 750f;
        float GlowDuration = 1f;
        float FlashGlowRadius = 700f;
        float FlashGlowDuration = 0.05f;

        engine.spawnExplosion(point, ZERO_VEL, CORE_EXPLOSION_COLOR_VIS, CoreExplosionRadius, CoreExplosionDuration);
        engine.spawnExplosion(point, ZERO_VEL, EXPLOSION_COLOR_VIS, ExplosionRadius, ExplosionDuration);
        engine.addHitParticle(point, ZERO_VEL, CoreGlowRadius, 1f, CoreGlowDuration, CORE_GLOW_COLOR_VIS);
        engine.addSmoothParticle(point, ZERO_VEL, GlowRadius, 1f, GlowDuration, GLOW_COLOR_VIS);
        engine.addHitParticle(point, ZERO_VEL, FlashGlowRadius, 1f, FlashGlowDuration, FLASH_GLOW_COLOR_VIS);

        for (int x = 0; x < NUM_PARTICLES_VIS; x++) {
            engine.addHitParticle(point,
                    MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(50f, 150f), (float) Math.random() * 360f),
                    MathUtils.getRandomNumberInRange(4, 8), 1f, MathUtils.getRandomNumberInRange(0.4f, 0.9f), CORE_EXPLOSION_COLOR_VIS);
        }
        Global.getSoundPlayer().playSound("bt_holy_torp_impact", 1f + MathUtils.getRandomNumberInRange(-0.1f, 0.1f), 1f, point, ZERO_VEL);

        MagicRender.battlespace(Global.getSettings().getSprite("fx", "bt_holy_explosion"), point, ZERO_VEL, new Vector2f(50, 50), new Vector2f(1800, 1800), 360 * (float) Math.random(), 150, new Color(255, 239, 166, 255), true, 0, 0f, 0.8f);
        MagicRender.battlespace(Global.getSettings().getSprite("fx", "bultach_holy_explosion_shockwave"), point, ZERO_VEL, new Vector2f(96, 96), new Vector2f(1800, 1800), 360 * (float) Math.random(), 50, new Color(255, 242, 182, 255), true, 0, 0f, 0.9f);
        MagicRender.battlespace(Global.getSettings().getSprite("fx", "bultach_siege_explosion"), point, ZERO_VEL, new Vector2f(128, 128), new Vector2f(1000, 1000), 360 * (float) Math.random(), 0, new Color(255, 239, 135, 255), true, 0.2f, 0.0f, 0.4f);
        MagicRender.battlespace(Global.getSettings().getSprite("fx", "bultach_siege_explosion"), point, ZERO_VEL, new Vector2f(250, 250), new Vector2f(350, 350), 360 * (float) Math.random(), 0, new Color(149, 126, 35, 200), true, 0.35f, 0.0f, 1f);
        MagicRender.battlespace(Global.getSettings().getSprite("fx", "bultach_siege_explosion"), point, ZERO_VEL, new Vector2f(200, 200), new Vector2f(225, 225), 360 * (float) Math.random(), 0, new Color(255, 235, 153, 100), true, 0.35f, 0.0f, 1.5f);

        WaveDistortion wave = new WaveDistortion(point, ZERO_VEL);
        wave.setIntensity(1.5f); wave.setSize(225f); wave.flip(true); wave.setLifetime(0f); wave.fadeOutIntensity(1f);
        DistortionShader.addDistortion(wave);

        if (Global.getSettings().getModManager().isModEnabled("shaderLib")) {
            bt_yoinked_graphicLibEffects.CustomRippleDistortion(point, ZERO_VEL, 225, 3, false, 0, 360, 1f, 0.1f, 0.25f, 0.5f, 0.5f, 0f);
            MagicRender.battlespace(
                    Global.getSettings().getSprite("campaignEntities", "fusion_lamp_glow"), point, new Vector2f(),
                    new Vector2f(100 * MathUtils.getRandomNumberInRange(0.8f, 1.2f), 2500 * MathUtils.getRandomNumberInRange(0.8f, 1.2f)),
                    new Vector2f(), 360 * (float) Math.random(), 0, new Color(255, 241, 167, 255), true,
                    0f, 0f, 0.7f, 0.20f, MathUtils.getRandomNumberInRange(0.05f, 0.2f), 0f,
                    MathUtils.getRandomNumberInRange(0.4f, 0.6f), MathUtils.getRandomNumberInRange(0.1f, 0.3f),
                    CombatEngineLayers.CONTRAILS_LAYER);
        }
    }

    public static class AoeEffectListener implements AdvanceableListener {
        private final ShipAPI targetShip;
        private final boolean isGestaltEffect;
        private float timer;
        private int currentStacks;
        private final String effectId;
        private final Color jitterColor;
        private final String jitterKey;

        public AoeEffectListener(ShipAPI targetShip, boolean isGestaltEffect) {
            this.targetShip = targetShip;
            this.isGestaltEffect = isGestaltEffect;
            this.timer = EFFECT_DURATION_SECONDS;
            this.currentStacks = 0;

            if (isGestaltEffect) {
                this.effectId = ALLY_BUFF_ID;
                this.jitterColor = ALLY_JITTER_COLOR;
            } else {
                this.effectId = ENEMY_DEBUFF_ID;
                this.jitterColor = ENEMY_JITTER_COLOR;
            }
            this.jitterKey = "custom_aoe_listener_jitter_" + targetShip.getId() + "_" + effectId;
            refreshEffect();
        }

        public String getEffectId() { return effectId; }
        public boolean isAllyEffect() { return isGestaltEffect; }

        public void refreshEffect() {
            this.timer = EFFECT_DURATION_SECONDS;
            if (isGestaltEffect) {
                currentStacks = Math.min(currentStacks + 1, ALLY_MAX_STACKS);
            } else {
                currentStacks = Math.min(currentStacks + 1, ENEMY_MAX_STACKS);
            }
            applyStatMods();
        }
        private void applyStatMods() {
            removeStatMods();
            if (isGestaltEffect) {
                float bonus = currentStacks * ALLY_FLUX_DISSIPATION_PER_STACK;
                if (bonus > 0) targetShip.getMutableStats().getFluxDissipation().modifyMult(effectId, 1f + bonus);
            } else {
                float vulnerabilityPercent = currentStacks * ENEMY_VULNERABILITY_PER_STACK * 100f;
                if (vulnerabilityPercent > 0) {
                    targetShip.getMutableStats().getHullDamageTakenMult().modifyPercent(effectId, vulnerabilityPercent);
                    targetShip.getMutableStats().getArmorDamageTakenMult().modifyPercent(effectId, vulnerabilityPercent);
                    targetShip.getMutableStats().getShieldDamageTakenMult().modifyPercent(effectId, vulnerabilityPercent);
                }
            }
        }
        private void removeStatMods() {
            if (isGestaltEffect) targetShip.getMutableStats().getFluxDissipation().unmodify(effectId);
            else {
                targetShip.getMutableStats().getHullDamageTakenMult().unmodify(effectId);
                targetShip.getMutableStats().getArmorDamageTakenMult().unmodify(effectId);
                targetShip.getMutableStats().getShieldDamageTakenMult().unmodify(effectId);
            }
        }
        public void cleanup() {
            removeStatMods();
            if (targetShip != null && targetShip.isAlive()) targetShip.setJitterUnder(jitterKey, jitterColor, 0, 0, 0, 0);
        }
        @Override
        public void advance(float amount) {
            if (Global.getCombatEngine().isPaused()) return;
            if (!targetShip.isAlive() || !targetShip.getListeners(AoeEffectListener.class).contains(this)) {
                return;
            }

            timer -= amount;
            if (timer <= 0) {
                cleanup();
                if (targetShip.getListeners(AoeEffectListener.class).contains(this)) {
                    targetShip.removeListener(this);
                }
                return;
            }
            float effectIntensity = Math.max(0.1f, (float) currentStacks / (isGestaltEffect ? ALLY_MAX_STACKS : ENEMY_MAX_STACKS));
            float jitterAlphaMult = (float)jitterColor.getAlpha() / 255f;
            if (timer < 1f) jitterAlphaMult *= timer;
            Color finalJitterColor = new Color((float)jitterColor.getRed() / 255f, (float)jitterColor.getGreen() / 255f, (float)jitterColor.getBlue() / 255f, jitterAlphaMult);
            targetShip.setJitterUnder(jitterKey, finalJitterColor, effectIntensity, 5 + currentStacks, 0.5f, 3f + (effectIntensity * 12f));
        }
    }

    @Override
    public void onExplosion(DamagingProjectileAPI explosion, DamagingProjectileAPI originalProjectile) {
        if (originalProjectile != null) {
            this.triggerAoECenteredOnProjectile(originalProjectile);
        } else if (explosion != null) {
            this.triggerAoECenteredOnProjectile(explosion);
        }
    }
}