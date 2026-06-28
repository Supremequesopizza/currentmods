package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// Phase teleporter system, with some extras

public class bt_pather_tele_test extends BaseShipSystemScript {

    private static final String CHARGEUP_SOUND = "reactor_startup";
    private static final String EXPLOSION_SOUND = "bt_pathertele_arrive";
    private static final String ARC_SOUND = "tachyon_lance_emp_impact";
    private static final float DAMAGE_MOD_VS_CAPITAL = 0.55f;
    private static final float DAMAGE_MOD_VS_CRUISER = 0.60f;
    private static final float DAMAGE_MOD_VS_DESTROYER = 0.75f;
    private static final float DAMAGE_MOD_VS_FIGHTER = 0.97f;
    private static final float DAMAGE_MOD_VS_FRIGATE = 0.88f;
    private static final float EXPLOSION_VISUAL_RADIUS = 1550f;
    private static final float DISTORTION_BLAST_RADIUS = 1200f;
    private static final float DISTORTION_CHARGE_RADIUS = 0f;

    // Explosion effect constants
    private static final Color EXPLOSION_COLOR = new Color(134, 55, 160);
    private static final float EXPLOSION_DAMAGE_AMOUNT = 2350f;

    private static final DamageType EXPLOSION_DAMAGE_TYPE = DamageType.ENERGY;
    private static final float EXPLOSION_EMP_DAMAGE_AMOUNT = 2000f;
    private static final float EXPLOSION_FORCE_VS_ALLIES_MODIFIER = .3f;
    private static final float EXPLOSION_PUSH_RADIUS = 900f;
    private static final float FORCE_VS_ASTEROID = 590f;
    private static final float FORCE_VS_CAPITAL = 85f;
    private static final float FORCE_VS_CRUISER = 105f;
    private static final float FORCE_VS_DESTROYER = 215f;
    private static final float FORCE_VS_FIGHTER = 455f;
    private static final float FORCE_VS_FRIGATE = 380f;
    private static final float EXPLOSION_DAMAGE_VS_ALLIES_MODIFIER = .11f;
    private static final float EXPLOSION_EMP_VS_ALLIES_MODIFIER = .05f;
    private static final int MAX_PARTICLES_PER_FRAME = 3;
    private static final Color PARTICLE_COLOR = new Color(212, 155, 240, 244);
    private static final float PARTICLE_OPACITY = 0.55f;
    private static final float PARTICLE_RADIUS = 500f;
    private static final float PARTICLE_SIZE = 3f;
    private static final Vector2f ZERO = new Vector2f();

    // Local variables, don't touch these
    private boolean isActive = false;
    private StandardLight light;
    private WaveDistortion wave;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        // instanceof also acts as a null check
        if (!(stats.getEntity() instanceof ShipAPI)) {
            return;
        }

        ShipAPI ship = (ShipAPI) stats.getEntity();
        // Chargeup, show particle inhalation effect
        if (state == State.IN) {
            Vector2f loc = new Vector2f(ship.getLocation());
            loc.x -= 70f * FastTrig.cos(ship.getFacing() * Math.PI / 180f);
            loc.y -= 70f * FastTrig.sin(ship.getFacing() * Math.PI / 180f);

            // Everything in this block is only done once per chargeup
            if (!isActive) {
                isActive = true;
                Global.getSoundPlayer().playSound(CHARGEUP_SOUND, 1f, 1f, ship.getLocation(), ship.getVelocity());

                light = new StandardLight(loc, ZERO, ZERO, null);
                light.setIntensity(1.45f);
                light.setSize(EXPLOSION_VISUAL_RADIUS);
                light.setColor(PARTICLE_COLOR);
                light.fadeIn(2.95f);
                light.setLifetime(0.1f);
                light.setAutoFadeOutTime(0.17f);
                LightShader.addLight(light);

                wave = new WaveDistortion(loc, ZERO);
                wave.setSize(DISTORTION_CHARGE_RADIUS);
                wave.setIntensity(DISTORTION_CHARGE_RADIUS / 4f);
                wave.fadeInSize(2.95f);
                wave.fadeInIntensity(2.95f);
                wave.setLifetime(0f);
                wave.setAutoFadeSizeTime(-0.5f);
                wave.setAutoFadeIntensityTime(0.17f);
                DistortionShader.addDistortion(wave);
            } else {
                light.setLocation(loc);
                wave.setLocation(loc);
            }

            // Exact amount per second doesn't matter since it's purely decorative
            Vector2f particlePos, particleVel;
            int numParticlesThisFrame = Math.round(effectLevel * MAX_PARTICLES_PER_FRAME);
            for (int x = 0; x < numParticlesThisFrame; x++) {
                particlePos = MathUtils.getRandomPointOnCircumference(ship.getLocation(), PARTICLE_RADIUS);
                particleVel = Vector2f.sub(ship.getLocation(), particlePos, null);
                Global.getCombatEngine().addSmokeParticle(particlePos, particleVel, PARTICLE_SIZE, PARTICLE_OPACITY,
                        1f, PARTICLE_COLOR);
            }

            // Inserting the arcOnBounds effect here from a second script

            arcOnBounds(ship, Global.getCombatEngine(), state);

        } // Cooldown, explode once the system is finished
            else if (state == State.OUT) {
            // Everything in this section is only done once per cooldown
            if (isActive) {
                CombatEngineAPI engine = Global.getCombatEngine();
                engine.spawnExplosion(ship.getLocation(), ship.getVelocity(), EXPLOSION_COLOR, EXPLOSION_VISUAL_RADIUS,
                        0.6f);
                engine.spawnExplosion(ship.getLocation(), ship.getVelocity(), EXPLOSION_COLOR, EXPLOSION_VISUAL_RADIUS /
                        2f, 0.6f);

                Vector2f loc = new Vector2f(ship.getLocation());
                loc.x -= 70f * FastTrig.cos(ship.getFacing() * Math.PI / 180f);
                loc.x -= 70f * FastTrig.cos(ship.getFacing() * Math.PI / 180f);
                loc.y -= 70f * FastTrig.sin(ship.getFacing() * Math.PI / 180f);

                light = new StandardLight();
                light.setLocation(loc);
                light.setIntensity(2f);
                light.setSize(EXPLOSION_VISUAL_RADIUS * 2f);
                light.setColor(EXPLOSION_COLOR);
                light.fadeOut(1.45f);
                LightShader.addLight(light);

                wave = new WaveDistortion();
                wave.setLocation(loc);
                wave.setSize(DISTORTION_BLAST_RADIUS);
                wave.setIntensity(DISTORTION_BLAST_RADIUS * 0.075f);
                wave.fadeInSize(1.2f);
                wave.fadeOutIntensity(0.9f);
                wave.setSize(DISTORTION_BLAST_RADIUS * 0.25f);
                DistortionShader.addDistortion(wave);

                Global.getSoundPlayer().playSound(EXPLOSION_SOUND, 1f, 1f, ship.getLocation(), ship.getVelocity());

                ShipAPI victim = null;
                Vector2f dir;
                float force, damage = 100, emp = 200, mod;
                List<CombatEntityAPI> entities = CombatUtils.getEntitiesWithinRange(ship.getLocation(),
                        EXPLOSION_PUSH_RADIUS);
                int size = entities.size();
                for (int i = 0; i < size; i++) {
                    CombatEntityAPI tmp = entities.get(i);
                    if (tmp == ship) {
                        continue;
                    }

                    mod = 1f - (MathUtils.getDistance(ship, tmp) / EXPLOSION_PUSH_RADIUS);
                    force = FORCE_VS_ASTEROID * mod;
                    damage = EXPLOSION_DAMAGE_AMOUNT * mod;
                    emp = EXPLOSION_EMP_DAMAGE_AMOUNT * mod;

                    if (tmp instanceof ShipAPI) {
                        victim = (ShipAPI) tmp;

                        // Modify push strength based on ship class
                        if (victim.getHullSize() == ShipAPI.HullSize.FIGHTER) {
                            force = FORCE_VS_FIGHTER * mod;
                            damage /= DAMAGE_MOD_VS_FIGHTER;
                        } else if (victim.getHullSize() == ShipAPI.HullSize.FRIGATE) {
                            force = FORCE_VS_FRIGATE * mod;
                            damage /= DAMAGE_MOD_VS_FRIGATE;
                        } else if (victim.getHullSize() == ShipAPI.HullSize.DESTROYER) {
                            force = FORCE_VS_DESTROYER * mod;
                            damage /= DAMAGE_MOD_VS_DESTROYER;
                        } else if (victim.getHullSize() == ShipAPI.HullSize.CRUISER) {
                            force = FORCE_VS_CRUISER * mod;
                            damage /= DAMAGE_MOD_VS_CRUISER;
                        } else if (victim.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) {
                            force = FORCE_VS_CAPITAL * mod;
                            damage /= DAMAGE_MOD_VS_CAPITAL;
                        }

                        if (victim.getOwner() == ship.getOwner()) {
                            damage *= EXPLOSION_DAMAGE_VS_ALLIES_MODIFIER;
                            emp *= EXPLOSION_EMP_VS_ALLIES_MODIFIER;
                            force *= EXPLOSION_FORCE_VS_ALLIES_MODIFIER;
                        }
                    }

                    dir = VectorUtils.getDirectionalVector(ship.getLocation(), tmp.getLocation());
                    dir.scale(force);

                    Vector2f.add(tmp.getVelocity(), dir, tmp.getVelocity());
                }

                isActive = false;
            }
            arcOnBounds(ship, Global.getCombatEngine(), state);
        }
    }

    // arcs on charging effect
    private void arcOnBounds(ShipAPI target, CombatEngineAPI engine, State state) {
            target.getExactBounds().update(target.getLocation(), target.getFacing());
            List<BoundsAPI.SegmentAPI> Segments = target.getExactBounds().getSegments();
            int firstBound = MathUtils.getRandomNumberInRange(0, Segments.size() - 1);
            int secondBound = firstBound + 2;
            if (secondBound >= Segments.size() - 1) secondBound -= Segments.size() - 1;
            Vector2f firstBoundLoc = Segments.get(firstBound).getP1();
            Vector2f secondBoundLoc = Segments.get(secondBound).getP1();
            engine.spawnEmpArcVisual(firstBoundLoc, target, secondBoundLoc, target, 10,
                    new Color(255, 0, 242, 147), new Color(170, 0, 255, 165));
            arcToOtherShips(target, state);
    }
    private void arcToOtherShips(ShipAPI ship, State state){
        ArrayList<ShipAPI> validtargets = new ArrayList<>();
        CombatEngineAPI engine = Global.getCombatEngine();
        ShipAPI possibletarget;
        float range = 0;
        float damage = 0;
        if (state == State.IN) {
            range = 500;
            damage = 50;
        }
        if (state == State.OUT){
            range = 1250;
            damage = 200;
        }
        Iterator shipiter = engine.getShips().iterator();
        while (shipiter.hasNext()){
            possibletarget = (ShipAPI)shipiter.next();
            double targetX = possibletarget.getLocation().x;
            double targetY = possibletarget.getLocation().y;
            double shipX = ship.getLocation().x;
            double shipY = ship.getLocation().y;
            double distance = Math.sqrt(Math.pow(shipX - targetX, 2) + Math.pow(shipY - targetY, 2));
            if (distance <= range && !(possibletarget == ship)){
                validtargets.add(possibletarget);
            }
        }
        ship.getExactBounds().update(ship.getLocation(), ship.getFacing());
        List<BoundsAPI.SegmentAPI> Segments = ship.getExactBounds().getSegments();
        int firstBound = MathUtils.getRandomNumberInRange(0, Segments.size() - 1);
        int secondBound = firstBound + 2;
        if (secondBound >= Segments.size() - 1) secondBound -= Segments.size() - 1;
        Vector2f firstBoundLoc = Segments.get(firstBound).getP1();
        if (!validtargets.isEmpty()) {
            ShipAPI target = validtargets.get(MathUtils.getRandomNumberInRange(0,validtargets.size()-1));
            engine.spawnEmpArc(ship, firstBoundLoc, ship, target, DamageType.ENERGY, damage, damage ,range, ARC_SOUND,15, new Color(255, 0, 242, 147), new Color(170, 0, 255, 165));
        } else {
            Vector2f randloc = MathUtils.getRandomPointInCircle(ship.getLocation(), range);
            engine.spawnEmpArcVisual(firstBoundLoc, ship, randloc, ship, 15, new Color(255, 0, 242, 147), new Color(170, 0, 255, 165));

        }
    }


    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (state == State.COOLDOWN) {
            if (index == 0) {
                return new StatusData("Phase Coils Cooling", false);
            }
        }
        return null;
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
    }
}
