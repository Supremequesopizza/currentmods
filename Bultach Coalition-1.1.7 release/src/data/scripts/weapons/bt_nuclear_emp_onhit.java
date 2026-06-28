package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;

import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.magiclib.util.MagicRender;
import data.scripts.utilities.bt_yoinked_graphicLibEffects;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;

import static com.fs.starfarer.api.util.Misc.ZERO;


public class bt_nuclear_emp_onhit implements OnHitEffectPlugin {

    static final Color CORE_EXPLOSION_COLOR = new Color(178, 0, 0, 255);
    static final Color CORE_GLOW_COLOR = new Color(255, 200, 200, 150);
    static final Color EXPLOSION_COLOR = new Color(255, 176, 176, 10);
    static final Color FLASH_GLOW_COLOR = new Color(241, 215, 215, 200);
    static final Color GLOW_COLOR = new Color(255, 172, 172, 50);
    static final Vector2f ZERO = new Vector2f();

    static final int NUM_PARTICLES = 50;

    static final DamagingExplosionSpec explosion = new DamagingExplosionSpec(0.05f,
            400,
            400f,
            250,
            125f,
            CollisionClass.PROJECTILE_FF,
            CollisionClass.PROJECTILE_FIGHTER,
            0,
            0,
            0,
            0,
            new Color(178, 0, 0, 255),
            new Color(255, 150, 49, 255)
    );

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (target instanceof MissileAPI) return;

        explosion(point, engine, projectile);
    }

    static void explosion(Vector2f point, CombatEngineAPI engine, DamagingProjectileAPI projectile) {
        // Blast visuals
        float CoreExplosionRadius = 200f;
        float CoreExplosionDuration = 3f;
        float ExplosionRadius = 500f;
        float ExplosionDuration = 6f;
        float CoreGlowRadius = 1200f;
        float CoreGlowDuration = 6f;
        float GlowRadius = 2850f;
        float GlowDuration = 9f;
        float FlashGlowRadius = 1400f;
        float FlashGlowDuration = 1f;

        engine.spawnExplosion(point, ZERO, CORE_EXPLOSION_COLOR, CoreExplosionRadius, CoreExplosionDuration);
        engine.spawnExplosion(point, ZERO, EXPLOSION_COLOR, ExplosionRadius, ExplosionDuration);
        engine.addHitParticle(point, ZERO, CoreGlowRadius, 1f, CoreGlowDuration, CORE_GLOW_COLOR);
        engine.addSmoothParticle(point, ZERO, GlowRadius, 1f, GlowDuration, GLOW_COLOR);
        engine.addHitParticle(point, ZERO, FlashGlowRadius, 1f, FlashGlowDuration, FLASH_GLOW_COLOR);

        for (int x = 0; x < NUM_PARTICLES; x++) {
            engine.addHitParticle(point,
                    MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(50f, 150f), (float) Math.random() * 360f),
                    MathUtils.getRandomNumberInRange(4, 8), 1f, MathUtils.getRandomNumberInRange(0.4f, 0.9f), CORE_EXPLOSION_COLOR);
        }

        explosion.setDamageType(DamageType.FRAGMENTATION);
        explosion.setShowGraphic(false);
        explosion.setMaxDamage(projectile.getDamageAmount());
        explosion.setMinDamage(explosion.getMaxDamage() * 0.5f);
        engine.spawnDamagingExplosion(explosion, projectile.getSource(), point);

        Global.getSoundPlayer().playSound("emp_nuclear_detonation", 1f + MathUtils.getRandomNumberInRange(-0.1f, 0.1f), 1f, point, ZERO);



        MagicRender.battlespace(
                Global.getSettings().getSprite("fx", "bultach_siege_impact_shockwave"),
                point,
                new Vector2f(),
                new Vector2f(50, 50),
                new Vector2f(500, 500),
                //angle,
                360 * (float) Math.random(),
                0,
                new Color(222, 49, 49, 255),
                true,
                0,
                1f,
                0.9f
        );

        MagicRender.battlespace(
                Global.getSettings().getSprite("fx", "bultach_siege_impact_shockwave"),
                point,
                new Vector2f(),
                new Vector2f(50, 50),
                new Vector2f(1500, 1500),
                //angle,
                360 * (float) Math.random(),
                0,
                new Color(222, 49, 49, 255),
                true,
                0,
                1f,
                0.9f
        );

        MagicRender.battlespace(
                Global.getSettings().getSprite("fx", "bultach_nuke_shockwave"),
                point,
                new Vector2f(),
                new Vector2f(196, 196),
                new Vector2f(1500, 1500),
                //angle,
                360 * (float) Math.random(),
                0,
                new Color(220, 49, 49, 255),
                true,
                0,
                1f,
                1f
        );

        MagicRender.battlespace(
                Global.getSettings().getSprite("fx", "bultach_siege_explosion"),
                point,
                new Vector2f(),
                new Vector2f(128, 128),
                new Vector2f(1000, 1000),
                //angle,
                360 * (float) Math.random(),
                0,
                new Color(255, 135, 170, 255),
                true,
                0.2f,
                0.5f,
                1f
        );

        MagicRender.battlespace(
                Global.getSettings().getSprite("fx", "bultach_siege_explosion"),
                point,
                new Vector2f(),
                new Vector2f(250, 250),
                new Vector2f(350, 350),
                //angle,
                360 * (float) Math.random(),
                0,
                new Color(149, 35, 35, 200),
                true,
                0.35f,
                1.0f,
                1.2f
        );

        MagicRender.battlespace(
                Global.getSettings().getSprite("fx", "bultach_nuke_risidual"),
                point,
                new Vector2f(),
                new Vector2f(200, 200),
                new Vector2f(425, 425),
                //angle,
                360 * (float) Math.random(),
                0,
                new Color(255, 153, 230, 100),
                true,
                0.35f,
                2f,
                3f
        );

        WaveDistortion wave = new WaveDistortion(point, ZERO);
        wave.setIntensity(1.5f);
        wave.setSize(225f);
        wave.flip(true);
        wave.setLifetime(0f);
        wave.fadeOutIntensity(1f);
        wave.setLocation(projectile.getLocation());
        DistortionShader.addDistortion(wave);

        boolean light = false;
        if (Global.getSettings().getModManager().isModEnabled("shaderLib")) {
            light = true;
        }

        if (light) {
            bt_yoinked_graphicLibEffects.CustomRippleDistortion(
                    point,
                    ZERO,
                    1225,
                    5,
                    false,
                    0,
                    360,
                    1f,
                    0.1f,
                    0.55f,
                    0.75f,
                    0.5f,
                    0f
            );

            MagicRender.battlespace(
                    Global.getSettings().getSprite("campaignEntities", "fusion_lamp_glow"),
                    point,
                    new Vector2f(),
                    new Vector2f(2000 * MathUtils.getRandomNumberInRange(0.8f, 1.2f), 3500 * MathUtils.getRandomNumberInRange(0.8f, 1.2f)),
                    new Vector2f(),
                    360 * (float) Math.random(),
                    0,
                    new Color(255, 64, 64, 255),
                    true,
                    0,
                    0,
                    0.7f,
                    0.20f,
                    MathUtils.getRandomNumberInRange(0.05f, 0.2f),
                    0,
                    MathUtils.getRandomNumberInRange(0.4f, 0.6f),
                    MathUtils.getRandomNumberInRange(0.1f, 0.3f),
                    CombatEngineLayers.CONTRAILS_LAYER
            );
        }
        if(projectile.getWeapon()!=null && projectile.getSource()!=null){
            ShipAPI ship = projectile.getSource();

            AdvanceableListener nukeListener = new nukeListener(ship, projectile, point);
            ship.addListener(nukeListener);

        }
    }

    public static class nukeListener implements AdvanceableListener {

        //this determines how fast the damage will tick and how quickly the shockwave moves. lower is faster
        IntervalUtil interval = new IntervalUtil(0.01f, 0.01f);
        //this determines the radius of the explosion
        public static float nukeRad = 1450f;
        //this determines the speed of the explosion, recommend that you change interval instead since making this too big could make it choppy and inconsistent
        public static float nukeSpeed = 15f;
        //energy damage that the nuke deals
        public float nukeDMG = 1500f;
        //emp damage that the nuke deals
        public float nukeEMP = 3000f;
        //percentage of damage a ship takes per tick after taking the initial damage instance
        public static float secondaryDamage = 0.85f;
        //damage and emp of the shockwave will be multiplied by this every time the interval passes. make sure this is close to 1.0 since it happens very frequently.
        public static float nukeDecayRate = 0.995f;

        CombatEngineAPI engine = Global.getCombatEngine();

        float currNukeRad = 0f;
        ArrayList<ShipAPI> hitShips = new ArrayList<>();
        public Vector2f point;
        public DamagingProjectileAPI projectile;
        public ShipAPI ship;

        public nukeListener(ShipAPI ship, DamagingProjectileAPI projectile, Vector2f point) {
            this.projectile = projectile;
            this.point = point;
            this.ship = ship;
        }

        @Override
        public void advance(float amount) {


            interval.advance(amount);

            if (interval.intervalElapsed()) {

                currNukeRad += nukeSpeed;

                for (CombatEntityAPI e:AIUtils.getEnemiesOnMap(projectile)){

                    Vector2f wavepoint = MathUtils.getPointOnCircumference(point,currNukeRad,VectorUtils.getAngle(point,e.getLocation()));
                    if(CollisionUtils.isPointWithinBounds(wavepoint,e)){
                        if(e instanceof ShipAPI) {
                            if(hitShips.contains(e)){
                                engine.applyDamage(e, wavepoint, nukeDMG*secondaryDamage, DamageType.ENERGY, nukeEMP*secondaryDamage, true, false, ship);
                            }else {
                                hitShips.add((ShipAPI) e);
                                engine.applyDamage(e, wavepoint, nukeDMG, DamageType.ENERGY, nukeEMP, true, false, ship);
                            }
                        }
                    }
                }

                nukeDMG = nukeDMG*nukeDecayRate;
                nukeEMP = nukeEMP*nukeDecayRate;
            }

            if(currNukeRad>=nukeRad){
                ship.removeListener(this);
            }
        }

    }
}



























