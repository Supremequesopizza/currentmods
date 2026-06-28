package data.scripts.weapons.doom;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.utils.bultach_utils;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static com.fs.starfarer.api.util.Misc.ZERO;
import static data.scripts.utils.bultach_utils.lerp;


public class bt_doombeam_behavior implements EveryFrameWeaponEffectPlugin, BeamEffectPlugin
{
    private static final String GLOW_SPRITE_PATH = "graphics/weapons/ork_tuiltean_laser_glow.png";
    private static final float GLOW_WIDTH = 88f;
    private static final float GLOW_HEIGHT = 128f;


    static final float VFX_OFFSET = 75f;
    static final float VFX_OFFSET2 = 125f;

    static final boolean DO_DISTORTION = true;
    static final float DISTORTION_SIZE = 15f;
    static final boolean DISTORTION_WHILE_FIRING_ONLY = true;
    static final boolean DO_DISTORTION_FLASH = false;
    static final Color DISTORTION_FLASH_COLOR = new Color(178, 0, 0, 255);
    private final IntervalUtil particle_interval = new IntervalUtil(0.05f, 0.1f);

    static final float FLARE_THICCNESS = 20f;
    static final float FLARE_LENGTH = 500f;
    static final float FLARE_SPAWN_RADIUS = 0f;
    private final IntervalUtil flare_interval = new IntervalUtil(0.2f, 0.25f);
    static final Color FLARE_COLOR = new Color(213, 10, 10, 189);
    static final boolean FLARE_ONLY_WHILE_FIRING = true;

    static final float TIER_TIME = 1.25f;

    static final float T1_WIDTH = 20f;
    static final float T2_WIDTH = 55f;
    static final float T3_WIDTH = 100f;

    static final float T2_DAMAGE_MULT = 2.0f;
    static final float T3_DAMAGE_MULT = 4.0f;

    static final IntervalUtil t2_arc_interval = new IntervalUtil(0.25f, 0.25f);
    static final IntervalUtil t3_arc_interval = new IntervalUtil(0.10f, 0.10f);

    static final float ARC_DAMAGE = 0f;
    static final float ARC_EMP = 200f;

    private boolean did_t2 = false;
    private boolean did_t3 = false;
    private boolean hasReachedT3 = false;

    float time_firing = 0f;
    boolean wasZero = false;

    private enum State {
        IDLE,
        FIRING,
        COOLDOWN
    }
    private State state = State.IDLE;
    private static final float FADE_OUT_DURATION = 0.25f;
    private static final float ACTUAL_COOLDOWN = 20.0f;
    private static final float SHORT_COOLDOWN = 2.0f;
    private float customCooldownTimer = 0f;
    private float lastFrameWidth = 0f;
    private static final Color GLOW_COLOR = new Color(213, 10, 10);
    private float glowPulseTimer = 0f;
    private static final float GLOW_FREQUENCY = 3f;


    @SuppressWarnings("unchecked")
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon)
    {
        if (engine == null || weapon == null || engine.isPaused() || weapon.getShip() == null) return;

        glowPulseTimer += amount;

        if (!weapon.getShip().hasListenerOfClass(bultach_orbbeam_listener.class))
            weapon.getShip().addListener(new bultach_orbbeam_listener());

        Vector2f vfx_loc = MathUtils.getPointOnCircumference(weapon.getLocation(), VFX_OFFSET, weapon.getCurrAngle());
        Vector2f vfx_loc2 = MathUtils.getPointOnCircumference(weapon.getLocation(), VFX_OFFSET2, weapon.getCurrAngle());

        flare_interval.advance(amount);
        if (flare_interval.intervalElapsed() && (!FLARE_ONLY_WHILE_FIRING || state == State.FIRING))
            bultach_utils.createSharpFlare(engine, weapon.getShip(), weapon.getShip(), MathUtils.getRandomPointInCircle(vfx_loc, FLARE_SPAWN_RADIUS), FLARE_THICCNESS, FLARE_LENGTH, weapon.getCurrAngle() + 90f, FLARE_COLOR, Color.WHITE);

        if (DO_DISTORTION && (!DISTORTION_WHILE_FIRING_ONLY || state == State.FIRING)) {
            WaveDistortion wave = new WaveDistortion();
            wave.setLocation(vfx_loc);
            if (DISTORTION_WHILE_FIRING_ONLY) wave.setIntensity(10f * weapon.getChargeLevel());
            else wave.setIntensity(10f);
            wave.setSize(DISTORTION_SIZE);
            wave.setLifetime(0.02f);
            DistortionShader.addDistortion(wave);
        }

        switch (state) {
            case IDLE:
                renderGlow(weapon, 0.3f, 0.5f, GLOW_FREQUENCY);
                if (weapon.isFiring() && !weapon.getBeams().isEmpty()) {
                    state = State.FIRING;
                }
                break;
            case FIRING:
                renderGlow(weapon, 1f);
                if (!weapon.isFiring() || weapon.getBeams().isEmpty()) {
                    if (DO_DISTORTION_FLASH) {
                        MagicRender.battlespace(
                                Global.getSettings().getSprite("fx", "bultach_siegelaser_shockwave"),
                                vfx_loc,
                                ZERO,
                                new Vector2f(160f, 390),
                                new Vector2f(160, 390),
                                weapon.getCurrAngle() + 0f,
                                0f,
                                new Color(255, 36, 36, 208),
                                true,
                                0.0f,
                                0f,
                                0.5f
                        );
                    }
                    state = State.COOLDOWN;
                    if (hasReachedT3) {
                        customCooldownTimer = ACTUAL_COOLDOWN;
                    } else {
                        customCooldownTimer = SHORT_COOLDOWN;
                    }
                    time_firing = 0f;
                    did_t2 = false;
                    did_t3 = false;
                    hasReachedT3 = false;
                    break;
                }

                time_firing += amount;
                if (!weapon.getBeams().isEmpty()) {
                    BeamAPI beam = weapon.getBeams().get(0);
                    if (time_firing < TIER_TIME - 0.25f) {
                        beam.setWidth(T1_WIDTH);
                    } else if (time_firing > (TIER_TIME - 0.25f) && time_firing < (TIER_TIME + 0.25f)) {
                        beam.setWidth(lerp(T1_WIDTH, T2_WIDTH, (time_firing - TIER_TIME + 0.25f) * 2f));
                    } else if (time_firing > (TIER_TIME * 2f - 0.25f) && time_firing < (TIER_TIME * 2f + 0.25f)) {
                        beam.setWidth(lerp(T2_WIDTH, T3_WIDTH, (time_firing - TIER_TIME * 2f + 0.25f) * 2f));
                    }
                    lastFrameWidth = beam.getWidth();
                }

                if (DO_DISTORTION_FLASH) {
                    if (particle_interval.intervalElapsed())
                        addRadialParticles(engine, vfx_loc, weapon.getShip().getVelocity(), DISTORTION_FLASH_COLOR, 50, 300, 0.2f, 10f, 2);
                }

                if (time_firing > TIER_TIME && !did_t2) {
                    did_t2 = true;
                    MagicRender.battlespace(
                            Global.getSettings().getSprite("fx", "bultach_siegelaser_shockwave"),
                            vfx_loc,
                            ZERO,
                            new Vector2f(85f, 185),
                            new Vector2f(85, 185),
                            weapon.getCurrAngle() + 0f,
                            0f,
                            new Color(255, 157, 232, 223),
                            true,
                            0.0f,
                            0f,
                            0.5f
                    );
                    Global.getSoundPlayer().playSound("bt_doomlas_t2", 1.4f, 1f, weapon.getLocation(), weapon.getShip().getVelocity());
                } else if (time_firing > 2 * TIER_TIME && !did_t3) {
                    did_t3 = true;
                    hasReachedT3 = true;
                    MagicRender.battlespace(
                            Global.getSettings().getSprite("fx", "bultach_siegelaser_shockwave"),
                            vfx_loc,
                            ZERO,
                            new Vector2f(120f, 310),
                            new Vector2f(120, 310),
                            weapon.getCurrAngle() + 0f,
                            0f,
                            new Color(255, 64, 64, 249),
                            true,
                            0.0f,
                            0f,
                            0.7f
                    );
                    MagicRender.battlespace(
                            Global.getSettings().getSprite("fx", "bultach_siegelaser_shockwave"),
                            vfx_loc2,
                            ZERO,
                            new Vector2f(85f, 185),
                            new Vector2f(85, 185),
                            weapon.getCurrAngle() + 0f,
                            0f,
                            new Color(255, 194, 229, 246),
                            true,
                            0.0f,
                            0f,
                            0.5f
                    );
                    Global.getSoundPlayer().playSound("bt_doomlas_t3", 1.1f, 1f, weapon.getLocation(), weapon.getShip().getVelocity());
                }
                break;
            case COOLDOWN:
                if (customCooldownTimer > 0) {
                    customCooldownTimer -= amount;
                    weapon.setForceNoFireOneFrame(true);

                    float fadeProgress = (customCooldownTimer - (customCooldownTimer > SHORT_COOLDOWN ? (ACTUAL_COOLDOWN - FADE_OUT_DURATION) : (SHORT_COOLDOWN - FADE_OUT_DURATION))) / FADE_OUT_DURATION;
                    renderGlow(weapon, Math.max(0f, fadeProgress));

                    if (!weapon.getBeams().isEmpty()) {
                        if (fadeProgress > 0f) {
                            weapon.getBeams().get(0).setWidth(lastFrameWidth * Math.max(0f, fadeProgress));
                        } else {
                            weapon.getBeams().get(0).setWidth(0f);
                        }
                    }
                } else {
                    state = State.IDLE;
                }
                break;
        }
    }

    private void renderGlow(WeaponAPI weapon, float alpha) {
        if (alpha <= 0f) return;
        Color renderColor = new Color(GLOW_COLOR.getRed(), GLOW_COLOR.getGreen(), GLOW_COLOR.getBlue(), (int) (255 * alpha));
        MagicRender.singleframe(
                Global.getSettings().getSprite(GLOW_SPRITE_PATH),
                weapon.getLocation(),
                new Vector2f(GLOW_WIDTH, GLOW_HEIGHT),
                weapon.getCurrAngle(),
                renderColor,
                true
        );
    }

    private void renderGlow(WeaponAPI weapon, float minAlpha, float maxAlpha, float frequency) {
        float sine = 0.5f + 0.5f * (float) Math.sin(glowPulseTimer * frequency);
        float alpha = minAlpha + (maxAlpha - minAlpha) * sine;
        renderGlow(weapon, alpha);
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam)
    {
        CombatEntityAPI target = beam.getDamageTarget();
        if (target instanceof ShipAPI && beam.getBrightness() >= 1f && state == State.FIRING) {
            float dur = beam.getDamage().getDpsDuration();

            if (!wasZero) dur = 0;
            wasZero = beam.getDamage().getDpsDuration() <= 0;
            IntervalUtil fireInterval;
            if (beam.getWidth() >= T3_WIDTH - 0.1f)
                fireInterval = t3_arc_interval;
            else if (beam.getWidth() >= T2_WIDTH)
                fireInterval = t2_arc_interval;
            else
                return;

            fireInterval.advance(dur);
            if (fireInterval.intervalElapsed()) {
                ShipAPI ship = (ShipAPI) target;
                boolean hitShield = target.getShield() != null && target.getShield().isWithinArc(beam.getTo());
                float pierceChance = ((ShipAPI) target).getHardFluxLevel() - 0.1f;
                pierceChance *= ship.getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT);

                boolean piercedShield = hitShield && (float) Math.random() < pierceChance;

                if (!hitShield || piercedShield) {
                    Vector2f point = beam.getRayEndPrevFrame();
                    engine.spawnEmpArcPierceShields(
                            beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
                            DamageType.ENERGY,
                            ARC_DAMAGE,
                            ARC_EMP,
                            100000f,
                            "tachyon_lance_emp_impact",
                            beam.getWidth() + 5f,
                            beam.getFringeColor(),
                            beam.getCoreColor()
                    );
                }
            }
        }
    }

    private static class bultach_orbbeam_listener implements DamageDealtModifier
    {

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit)
        {
            if (param instanceof BeamAPI && ((BeamAPI) param).getWeapon() != null)
            {
                BeamAPI beam = (BeamAPI) param;
                WeaponAPI wep = ((BeamAPI) param).getWeapon();
                if (wep.getSpec().getWeaponId().equals("ork_sathar_doom_laser"))
                {
                    if (beam.getWidth() > T1_WIDTH * 1.5f && beam.getWidth() <= T2_WIDTH) {
                        damage.getModifier().modifyMult("orbbeam", T2_DAMAGE_MULT);
                        return "orbbeam";
                    }
                    else if (beam.getWidth() > T2_WIDTH * 1.5f) {
                        damage.getModifier().modifyMult("orbbeam", T3_DAMAGE_MULT);
                        return "orbbeam";
                    }
                }
            }
            return null;
        }
    }

    private static void addRadialParticles(CombatEngineAPI engine, Vector2f loc, Vector2f baseVel, Color color, float minvel, float maxvel, float dur, float size, int num)
    {
        for (int i = 0; i < num; i++)
        {
            Vector2f vel = MathUtils.getRandomPointOnCircumference(baseVel, lerp(minvel, maxvel, Misc.random.nextFloat()));
            engine.addHitParticle(
                    loc,
                    vel,
                    size,
                    1f,
                    dur,
                    color
            );
        }
    }
}