package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.List;
import java.util.*;

public class bt_feregether_shipExplosion extends BaseEveryFrameCombatPlugin {

    private static final Set<String> APPLICABLE_SHIPS = new HashSet<>(1);

    private static final Color COLOR_ATTACHED_LIGHT = new Color(255, 229, 185);
    private static final Color COLOR_EMP_CORE = new Color(255, 240, 207, 150);
    private static final Color COLOR_EMP_FRINGE = new Color(227, 202, 169, 100);
    private static final Color COLOR_SUPERBRITE = new Color(255, 231, 200);

    private static final Map<String, Float> CORE_OFFSET = new HashMap<>(1);

    private static final String DATA_KEY = "bt_feregether_shipExplosion";

    private static final Map<HullSize, Float> EXPLOSION_AREA_INCREASE = new HashMap<>(5);
    private static final Map<HullSize, Float> EXPLOSION_INTENSITY = new HashMap<>(8);
    private static final Map<HullSize, Float> EXPLOSION_LENGTH = new HashMap<>(8);
    private static final Map<HullSize, Float> PITCH_BEND = new HashMap<>(4);
    private static final float FORCE_VS_ASTEROID = 400f;
    private static final float FORCE_VS_CAPITAL = 55f;
    private static final float FORCE_VS_CRUISER = 105f;
    private static final float FORCE_VS_DESTROYER = 195f;
    private static final float FORCE_VS_FIGHTER = 400f;
    private static final float FORCE_VS_FRIGATE = 300f;
    private static final float EXPLOSION_PUSH_RADIUS = 1200f;
    private static final float EXPLOSION_FORCE_VS_ALLIES_MODIFIER = .7f;

    private static final Vector2f ZERO = new Vector2f();

    private static final float CHARGE_UP_DURATION = 7.3f;
    private static final float MAX_JITTER_INTENSITY = 10f;
    private static final Color JITTER_COLOR = new Color(201, 169, 149, 89);
    private static final float ANGULAR_JITTER_MAX_SPEED = 35f;

    private static final List<String> FX_BEAM_SPRITE_PATHS = Arrays.asList(
            "graphics/fx/light_rays/bt_lightray_01.png",
            "graphics/fx/light_rays/bt_lightray_02.png",
            "graphics/fx/light_rays/bt_lightray_03.png"
    );
    private static final String DEFAULT_FX_BEAM_SPRITE = "graphics/fx/bt_nothing.png";

    private static final float FX_INITIAL_SPAWN_INTERVAL = 1.3f;
    private static final float FX_FINAL_SPAWN_INTERVAL = 0.05f;
    private static final float MIN_BEAM_LENGTH = 750f;
    private static final float MAX_BEAM_LENGTH = 1250f;
    private static final float MIN_BEAM_WIDTH = 165f;
    private static final float MAX_BEAM_WIDTH = 350f;
    private static final Color BEAM_COLOR = new Color(255, 229, 199, 60);

    // New constants for jitter and overlay
    private static final float BEAM_JITTER_MAX_RADIUS = 4f;
    private static final float OVERLAY_BEAM_JITTER_MAX_RADIUS = 9f;
    private static final Color OVERLAY_BEAM_COLOR = new Color(255, 255, 255, 85);

    static {
        APPLICABLE_SHIPS.add("ork_feregether");
        CORE_OFFSET.put("ork_feregether", 0f);
    }

    static {
        EXPLOSION_LENGTH.put(HullSize.FIGHTER, 1.5f);
        EXPLOSION_INTENSITY.put(HullSize.FIGHTER, 0.5f);
        EXPLOSION_AREA_INCREASE.put(HullSize.FIGHTER, 50f);
        PITCH_BEND.put(HullSize.FIGHTER, 1.2f);

        EXPLOSION_LENGTH.put(HullSize.FRIGATE, 3f);
        EXPLOSION_INTENSITY.put(HullSize.FRIGATE, 1.1f);
        EXPLOSION_AREA_INCREASE.put(HullSize.FRIGATE, 300f);
        PITCH_BEND.put(HullSize.FRIGATE, 1.07f);

        EXPLOSION_LENGTH.put(HullSize.DESTROYER, 4.5f);
        EXPLOSION_INTENSITY.put(HullSize.DESTROYER, 1.225f);
        EXPLOSION_AREA_INCREASE.put(HullSize.DESTROYER, 400f);
        PITCH_BEND.put(HullSize.DESTROYER, 1f);

        EXPLOSION_LENGTH.put(HullSize.CRUISER, 6f);
        EXPLOSION_INTENSITY.put(HullSize.CRUISER, 1.25f);
        EXPLOSION_AREA_INCREASE.put(HullSize.CRUISER, 500f);
        PITCH_BEND.put(HullSize.CRUISER, 0.92f);

        EXPLOSION_LENGTH.put(HullSize.CAPITAL_SHIP, 5f);
        EXPLOSION_INTENSITY.put(HullSize.CAPITAL_SHIP, 1.5f);
        EXPLOSION_AREA_INCREASE.put(HullSize.CAPITAL_SHIP, 650f);
        PITCH_BEND.put(HullSize.CAPITAL_SHIP, 0.85f);

        EXPLOSION_LENGTH.put(HullSize.DEFAULT, 5f);
        EXPLOSION_INTENSITY.put(HullSize.DEFAULT, 1.5f);
        EXPLOSION_AREA_INCREASE.put(HullSize.DEFAULT, 650f);
        PITCH_BEND.put(HullSize.DEFAULT, 1f);
    }

    private CombatEngineAPI engine;

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null) {
            return;
        }

        if (engine.isPaused()) {
            return;
        }

        final LocalData localData = (LocalData) engine.getCustomData().get(DATA_KEY);
        final Set<ShipAPI> deadShips = localData.deadShips;

        List<ShipAPI> ships = engine.getShips();
        for (ShipAPI ship : ships) {
            if (ship == null) {
                continue;
            }

            if (ship.isHulk() && !ship.isPiece()) {
                if (!APPLICABLE_SHIPS.contains(ship.getHullSpec().getHullId())) {
                    continue;
                }

                if (!deadShips.contains(ship)) {
                    deadShips.add(ship);
                    Global.getCombatEngine().addPlugin(createChargeAndExplodePlugin(ship));
                }
            }
        }
    }

    protected EveryFrameCombatPlugin createChargeAndExplodePlugin(final ShipAPI ship) {
        return new BaseEveryFrameCombatPlugin() {
            float elapsed = 0f;
            float fxSpawnTimer = 0f;
            Vector2f shipCoreLocationCache = new Vector2f(ship.getLocation());
            boolean hasPlayedStartSound = false;

            private static final float FADE_OUT_DURATION = 0.8f;
            private final List<LightBeam> beams = new ArrayList<>();
            private boolean explosionStarted = false;
            private float fadeOutAlpha = 1f;

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                if (engine.isPaused()) return;
                
                if (!engine.isEntityInPlay(ship)) {
                    Global.getCombatEngine().removePlugin(this);
                    return;
                }

                if (!hasPlayedStartSound) {
                    Global.getSoundPlayer().playSound("bt_feregether_death_riser", 0.9f, 1.4f, ship.getLocation(), ship.getVelocity());
                    hasPlayedStartSound = true;
                }

                elapsed += amount;

                shipCoreLocationCache = MathUtils.getPointOnCircumference(
                        ship.getLocation(),
                        CORE_OFFSET.getOrDefault(ship.getHullSpec().getHullId(), 0f),
                        ship.getFacing()
                );

                if (elapsed < CHARGE_UP_DURATION) {
                    float chargeProgress = elapsed / CHARGE_UP_DURATION;

                    float jitterIntensity = MAX_JITTER_INTENSITY * chargeProgress;
                    ship.setJitterUnder(this, JITTER_COLOR, jitterIntensity, 15, 0f, 5f + jitterIntensity * 0.2f);
                    ship.setJitter(this, JITTER_COLOR, jitterIntensity, 4, 0f, 2f + jitterIntensity * 0.2f);

                    float currentMaxErraticAngularSpeed = ANGULAR_JITTER_MAX_SPEED * chargeProgress;
                    float targetAngularVelocity = MathUtils.getRandomNumberInRange(-currentMaxErraticAngularSpeed, currentMaxErraticAngularSpeed);
                    ship.setAngularVelocity(targetAngularVelocity);

                    fxSpawnTimer -= amount;
                    if (fxSpawnTimer <= 0f) {
                        float currentSpawnInterval = FX_INITIAL_SPAWN_INTERVAL + chargeProgress * (FX_FINAL_SPAWN_INTERVAL - FX_INITIAL_SPAWN_INTERVAL);
                        fxSpawnTimer = currentSpawnInterval * (MathUtils.getRandomNumberInRange(0.75f, 1.25f));

                        float beamAngle = MathUtils.getRandomNumberInRange(0f, 360f);
                        float beamLength = MathUtils.getRandomNumberInRange(MIN_BEAM_LENGTH, MAX_BEAM_LENGTH) * (0.5f + 0.5f * chargeProgress);
                        float beamWidth = MathUtils.getRandomNumberInRange(MIN_BEAM_WIDTH, MAX_BEAM_WIDTH) * (0.5f + 0.5f * chargeProgress);

                        String spritePathToUse = DEFAULT_FX_BEAM_SPRITE;
                        if (!FX_BEAM_SPRITE_PATHS.isEmpty()) {
                            int randomIndex = MathUtils.getRandomNumberInRange(0, FX_BEAM_SPRITE_PATHS.size() - 1);
                            spritePathToUse = FX_BEAM_SPRITE_PATHS.get(randomIndex);
                        }

                        SpriteAPI sprite = Global.getSettings().getSprite(spritePathToUse);
                        Vector2f renderCenterOffset = MathUtils.getPointOnCircumference(null, beamLength / 2f, beamAngle);
                        Vector2f beamRenderLocation = Vector2f.add(shipCoreLocationCache, renderCenterOffset, new Vector2f());

                        Vector2f beamOffset = Vector2f.sub(beamRenderLocation, shipCoreLocationCache, new Vector2f());

                        Global.getSoundPlayer().playSound("bt_feregether_lightray", 0.8f, 0.2f, beamRenderLocation, ZERO);
                        Global.getSoundPlayer().playSound("bt_feregether_lightray_oomph", 1.2f, 1.2f, beamRenderLocation, ZERO);

                        beams.add(new LightBeam(sprite, beamOffset, new Vector2f(beamWidth, beamLength), beamAngle - 90f));
                    }
                } else {

                    fadeOutAlpha -= amount / FADE_OUT_DURATION;

                    if (fadeOutAlpha <= 0) {
                        Global.getCombatEngine().removePlugin(this);
                        return;
                    }

                    if (!explosionStarted) {
                        explosionStarted = true;

                        ship.setJitterUnder(this, JITTER_COLOR, 0f, 0, 0f, 0f);
                        ship.setJitter(this, JITTER_COLOR, 0f, 0, 0f, 0f);
                        ship.setAngularVelocity(0f);
                        ship.setOwner(ship.getOriginalOwner());

                        float explosionTime = EXPLOSION_LENGTH.get(ship.getHullSize());
                        float area = EXPLOSION_AREA_INCREASE.get(ship.getHullSize()) + ship.getCollisionRadius();
                        float damage = 5f * (float) Math.sqrt(ship.getFluxTracker().getMaxFlux()) * EXPLOSION_INTENSITY.get(ship.getHullSize());
                        float emp = 20f * (float) Math.sqrt(ship.getFluxTracker().getMaxFlux()) * EXPLOSION_INTENSITY.get(ship.getHullSize());

                        for (int i = 0; i <= (float) Math.sqrt(ship.getCollisionRadius()) * 1f * EXPLOSION_INTENSITY.get(ship.getHullSize()); i++) {
                            float angle = (float) Math.random() * 360f;
                            float distance = (float) Math.random() * area * 0.5f + area * 0.5f;
                            Vector2f point1 = MathUtils.getPointOnCircumference(shipCoreLocationCache, distance * (float) Math.random(), angle);
                            Vector2f point2 = MathUtils.getPointOnCircumference(shipCoreLocationCache, distance * (float) Math.random(), angle + 45f * (float) Math.random());
                            engine.spawnEmpArc(ship, point1, new SimpleEntity(point1), new SimpleEntity(point2), DamageType.ENERGY, 100f, 100f, 1f, null, EXPLOSION_INTENSITY.get(ship.getHullSize()) * 10f + 10f, COLOR_EMP_FRINGE, COLOR_EMP_CORE);
                        }
                        for (int i = 0; i <= ship.getCollisionRadius() * EXPLOSION_INTENSITY.get(ship.getHullSize()); i++) {
                            if (Math.random() > 0.5) {
                                Vector2f point1 = MathUtils.getRandomPointInCircle(shipCoreLocationCache, (float) Math.random() * area * 0.5f + area * 0.5f);
                                Vector2f point2 = MathUtils.getRandomPointInCircle(shipCoreLocationCache, ship.getCollisionRadius() * 0.25f);
                                engine.spawnEmpArc(ship, point2, new SimpleEntity(point2), new SimpleEntity(point1), DamageType.ENERGY, 100f, 100f, 0.1f, null, EXPLOSION_INTENSITY.get(ship.getHullSize()) * 10f + 10f, COLOR_EMP_FRINGE, COLOR_EMP_CORE);
                            }
                        }

                        engine.spawnExplosion(shipCoreLocationCache, ZERO, COLOR_SUPERBRITE, area, 0.1f * explosionTime);
                        engine.spawnExplosion(shipCoreLocationCache, ZERO, COLOR_ATTACHED_LIGHT, area * 0.1f, explosionTime * 1.25f);

                        List<ShipAPI> nearbyShips = CombatUtils.getShipsWithinRange(shipCoreLocationCache, area);
                        for (ShipAPI thisShip : nearbyShips) {
                            if (thisShip.getCollisionClass() == CollisionClass.NONE) {
                                continue;
                            }
                            Vector2f damagePoint = CollisionUtils.getCollisionPoint(shipCoreLocationCache, thisShip.getLocation(), thisShip);
                            if (damagePoint == null) {
                                damagePoint = thisShip.getLocation();
                            }
                            Vector2f forward = new Vector2f(damagePoint);
                            forward.normalise();
                            forward.scale(5f);
                            Vector2f.add(forward, damagePoint, damagePoint);
                            float falloff = 1f - MathUtils.getDistance(ship, thisShip) / area;
                            if (ship.getOwner() == thisShip.getOwner() && ship != thisShip) {
                                falloff *= 0.5f;
                            }
                            engine.applyDamage(thisShip, damagePoint, damage * falloff, DamageType.ENERGY, emp * falloff * 0.85f, true, false, ship);

                            for (int i = 0; i <= (int) (damage * (falloff / 950f) * EXPLOSION_INTENSITY.get(ship.getHullSize())); i++) {
                                Vector2f point = MathUtils.getRandomPointInCircle(thisShip.getLocation(), thisShip.getCollisionRadius());
                                engine.spawnEmpArc(ship, point, thisShip, thisShip, DamageType.ENERGY, damage * falloff * 0.9f, emp * falloff * 0.5f, 0.1f, null, (float) Math.sqrt(damage), COLOR_EMP_FRINGE, COLOR_EMP_CORE);
                            }
                        }

                        ShipAPI victim;
                        Vector2f dir;
                        float force, mod;
                        List<CombatEntityAPI> entities = CombatUtils.getEntitiesWithinRange(ship.getLocation(), EXPLOSION_PUSH_RADIUS);
                        for (CombatEntityAPI tmp : entities) {
                            if (tmp == ship) continue;

                            mod = 1f - (MathUtils.getDistance(ship, tmp) / EXPLOSION_PUSH_RADIUS);
                            force = FORCE_VS_ASTEROID * mod;

                            if (tmp instanceof ShipAPI) {
                                victim = (ShipAPI) tmp;
                                if (victim.getHullSize() == ShipAPI.HullSize.FIGHTER) force = FORCE_VS_FIGHTER * mod;
                                else if (victim.getHullSize() == HullSize.FRIGATE) force = FORCE_VS_FRIGATE * mod;
                                else if (victim.getHullSize() == HullSize.DESTROYER) force = FORCE_VS_DESTROYER * mod;
                                else if (victim.getHullSize() == HullSize.CRUISER) force = FORCE_VS_CRUISER * mod;
                                else if (victim.getHullSize() == HullSize.CAPITAL_SHIP) force = FORCE_VS_CAPITAL * mod;
                                if (victim.getOwner() == ship.getOwner()) force *= EXPLOSION_FORCE_VS_ALLIES_MODIFIER;
                            }
                            dir = VectorUtils.getDirectionalVector(ship.getLocation(), tmp.getLocation());
                            dir.scale(force);
                            Vector2f.add(tmp.getVelocity(), dir, tmp.getVelocity());
                        }

                        float time = EXPLOSION_INTENSITY.get(ship.getHullSize());
                        RippleDistortion ripple = new RippleDistortion(shipCoreLocationCache, ZERO);
                        ripple.setSize(area);
                        ripple.setIntensity(150f * EXPLOSION_INTENSITY.get(ship.getHullSize()));
                        ripple.setFrameRate(60f / (time));
                        ripple.fadeInSize(time);
                        ripple.fadeOutIntensity(time);
                        DistortionShader.addDistortion(ripple);

                        MagicRender.battlespace(Global.getSettings().getSprite("fx", "bultach_holy_explosion_shockwave"), shipCoreLocationCache, new Vector2f(), new Vector2f(50, 50), new Vector2f(1500, 1500), 360 * (float) Math.random(), 10, new Color(255, 228, 187, 140), true, 0, 1f, 1.2f);
                        MagicRender.battlespace(Global.getSettings().getSprite("fx", "bultach_holy_explosion_shockwave"), shipCoreLocationCache, new Vector2f(), new Vector2f(250, 250), new Vector2f(550, 550), 360 * (float) Math.random(), 5, new Color(255, 228, 187, 30), true, 0, 2.2f, 4f);
                        MagicRender.battlespace(Global.getSettings().getSprite("fx", "bt_cleave_cloud"), shipCoreLocationCache, new Vector2f(), new Vector2f(200, 200), new Vector2f(425, 425), 360 * (float) Math.random(), 20, new Color(255, 237, 207, 45), true, 0.55f, 2f, 2.8f);
                        MagicRender.battlespace(Global.getSettings().getSprite("fx", "bt_cleave_cloud"), shipCoreLocationCache, new Vector2f(), new Vector2f(100, 100), new Vector2f(225, 225), 360 * (float) Math.random(), 15, new Color(255, 237, 207, 69), true, 0.85f, 2f, 3.2f);
                        MagicRender.battlespace(Global.getSettings().getSprite("campaignEntities", "fusion_lamp_glow"), shipCoreLocationCache, new Vector2f(), new Vector2f(2000 * MathUtils.getRandomNumberInRange(0.8f, 1.2f), 3500 * MathUtils.getRandomNumberInRange(0.8f, 1.2f)), new Vector2f(), 360 * (float) Math.random(), 0, new Color(255, 246, 225, 250), true, 0, 0, 0.7f, 0.20f, MathUtils.getRandomNumberInRange(0.05f, 0.2f), 0, MathUtils.getRandomNumberInRange(0.4f, 0.6f), MathUtils.getRandomNumberInRange(0.1f, 0.3f), CombatEngineLayers.CONTRAILS_LAYER);

                        Global.getSoundPlayer().playSound("bt_gestalt_explosion_ship", PITCH_BEND.get(ship.getHullSize()), EXPLOSION_INTENSITY.get(ship.getHullSize()), shipCoreLocationCache, ZERO);

                        ship.setOwner(100);
                        for (int i = 0; i < MathUtils.getRandomNumberInRange(4, 8); i++) {
                            ship.splitShip();
                        }
                    }
                }
            }

            @Override
            public void renderInWorldCoords(ViewportAPI viewport) {
                if (beams.isEmpty()) {
                    return;
                }

                float alpha = Math.max(0, fadeOutAlpha);
                if (alpha <= 0) return;

                GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
                try {
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

                    Color renderColor = new Color(
                            BEAM_COLOR.getRed(),
                            BEAM_COLOR.getGreen(),
                            BEAM_COLOR.getBlue(),
                            (int) (BEAM_COLOR.getAlpha() * alpha)
                    );

                    Color overlayRenderColor = new Color(
                            OVERLAY_BEAM_COLOR.getRed(),
                            OVERLAY_BEAM_COLOR.getGreen(),
                            OVERLAY_BEAM_COLOR.getBlue(),
                            (int) (OVERLAY_BEAM_COLOR.getAlpha() * alpha)
                    );

                    for (LightBeam beam : beams) {
                        Vector2f baseRenderPos = Vector2f.add(shipCoreLocationCache, beam.offset, null);

                        Vector2f mainJitter = MathUtils.getRandomPointInCircle(null, BEAM_JITTER_MAX_RADIUS);
                        Vector2f mainRenderPos = Vector2f.add(baseRenderPos, mainJitter, null);

                        beam.sprite.setColor(renderColor);
                        beam.sprite.setSize(beam.size.x, beam.size.y);
                        beam.sprite.setAngle(beam.angle);
                        beam.sprite.renderAtCenter(mainRenderPos.x, mainRenderPos.y);

                        Vector2f overlayJitter = MathUtils.getRandomPointInCircle(null, OVERLAY_BEAM_JITTER_MAX_RADIUS);
                        Vector2f overlayRenderPos = Vector2f.add(baseRenderPos, overlayJitter, null);

                        beam.sprite.setColor(overlayRenderColor);
                        beam.sprite.setSize(beam.size.x * 0.4f, beam.size.y);
                        beam.sprite.setAngle(beam.angle);
                        beam.sprite.renderAtCenter(overlayRenderPos.x, overlayRenderPos.y);
                    }
                } finally {
                    GL11.glPopAttrib();
                }
            }
        };
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        if (engine.getCustomData().get(DATA_KEY) == null) {
            engine.getCustomData().put(DATA_KEY, new LocalData());
        }
    }

    private static final class LocalData {
        final Set<ShipAPI> deadShips = new LinkedHashSet<>(50);
        final List<ExplodingShip> explodingShips = new ArrayList<>(50);
    }

    private static final class ExplodingShip {
        float chargeLevel;
        float chargingTime;
        ShipAPI ship;

        private ExplodingShip(ShipAPI ship, float chargingTime) {
            this.ship = ship;
            this.chargingTime = chargingTime;
            this.chargeLevel = 0f;
        }
    }

    private static final class LightBeam {
        final SpriteAPI sprite;
        final Vector2f offset;
        final Vector2f size;
        final float angle;

        LightBeam(SpriteAPI sprite, Vector2f offset, Vector2f size, float angle) {
            this.sprite = sprite;
            this.offset = offset;
            this.size = size;
            this.angle = angle;
        }
    }
}