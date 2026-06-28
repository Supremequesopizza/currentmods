package data.scripts.weapons.chorus;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.utilities.bt_yoinked_graphicLibEffects;
import data.scripts.utils.bt_divine_cleaver_util;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fs.starfarer.api.util.Misc.ZERO;

public class bt_HolyGlare_muzzleflash implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {


    private static final Color PARTICLE_COLOR_EXTRA = new Color(255, 238, 193, 165);
    private static final Color GLOW_COLOR = new Color(255, 245, 211, 50);
    private static final Color FLASH_COLOR = new Color(255, 230, 203, 77);
    private static final int NUM_PARTICLES = 30;

    private boolean light = false;


    private final IntervalUtil effectInterval = new IntervalUtil(0.05f, 0.1f);

    boolean weaponChargedownBlowback = false;
    boolean emitSmokeBlowback = false;

    float
            durationBlowback = 0.75f,
            timeBlowback = 0f;


    final float MUZZLE_OFFSET_HARDPOINT_SHOCKWAVE = 0f;
    final float MUZZLE_OFFSET_TURRET_SHOCKWAVE = 0f;
    final float MUZZLE_OFFSET_HARDPOINT_SHOCKWAVE_SPRITE1 = 0f;
    final float MUZZLE_OFFSET_TURRET_SHOCKWAVE_SPRITE1 = 0f;
    final float MUZZLE_OFFSET_HARDPOINT_SHOCKWAVE_SPRITE2 = 0f;
    final float MUZZLE_OFFSET_TURRET_SHOCKWAVE_SPRITE2 = 0f;
    final float MUZZLE_OFFSET_HARDPOINT_SHOCKWAVE_SPRITE3 = 0f;
    final float MUZZLE_OFFSET_TURRET_SHOCKWAVE_SPRITE3 = 0f;
    final float MUZZLE_OFFSET_TURRET_BLOWBACK = 0f;
    final float MUZZLE_OFFSET_HARDPOINT_BLOWBACK = 0f;


    /*

          HOW TO USE:
          USED_IDS specifies which IDs to use for the rest of the script; any ID is valid EXCEPT the unique ID "default". Each ID should only be used once on the same weapon
          The script will spawn one particle "system" for each ID in this list, with the specific attributes of that ID.

          All the different Maps<> specify the attributes of each of the particle "systems"; they MUST have something defined as "default", and can have specific fields for specific IDs
          in the USED_IDS list; any field not filled in for a specific ID will revert to "default" instead.

          NOTE FROM NOOF: I would not recommend using this version of the script I have practically butchered it for my uses on a beam!!!!

      */
    private static final List<String> USED_IDS = new ArrayList<>();

    static {
        USED_IDS.add("CHARGEUP_PARTICLES1");
        USED_IDS.add("CHARGEUP_PARTICLES2");
        USED_IDS.add("CHARGEUP_PARTICLES3");
        USED_IDS.add("CHARGEUP_PARTICLES4");
        USED_IDS.add("CHARGEUP_PARTICLES5");
        USED_IDS.add("CHARGEUP_PARTICLES6");
        USED_IDS.add("CHARGEUP_PARTICLES_DIM1");
        USED_IDS.add("CHARGEUP_PARTICLES_DIM2");
        USED_IDS.add("CHARGEUP_PARTICLES_DIM3");
        USED_IDS.add("CHARGEUP_PARTICLES_DIM4");

    }

    //The amount of particles spawned immediately when the weapon reaches full charge level
    //  -For projectile weapons, this is when the projectile is actually fired
    //  -For beam weapons, this is when the beam has reached maximum brightness
    private static final Map<String, Integer> ON_SHOT_PARTICLE_COUNT = new HashMap<>();

    static {
        ON_SHOT_PARTICLE_COUNT.put("default", 15);
        ON_SHOT_PARTICLE_COUNT.put("CHARGEUP_PARTICLES1", 0);
        ON_SHOT_PARTICLE_COUNT.put("CHARGEUP_PARTICLES2", 0);
        ON_SHOT_PARTICLE_COUNT.put("CHARGEUP_PARTICLES3", 0);
        ON_SHOT_PARTICLE_COUNT.put("CHARGEUP_PARTICLES4", 0);
        ON_SHOT_PARTICLE_COUNT.put("CHARGEUP_PARTICLES5", 0);
        ON_SHOT_PARTICLE_COUNT.put("CHARGEUP_PARTICLES6", 0);


    }

    //How many particles are spawned each second the weapon is firing, on average
    private static final Map<String, Float> PARTICLES_PER_SECOND = new HashMap<>();

    static {
        PARTICLES_PER_SECOND.put("default", 0f);
        PARTICLES_PER_SECOND.put("CHARGEUP_PARTICLES1", 50f);
        PARTICLES_PER_SECOND.put("CHARGEUP_PARTICLES2", 50f);
        PARTICLES_PER_SECOND.put("CHARGEUP_PARTICLES3", 50f);
        PARTICLES_PER_SECOND.put("CHARGEUP_PARTICLES4", 50f);
        PARTICLES_PER_SECOND.put("CHARGEUP_PARTICLES5", 50f);
        PARTICLES_PER_SECOND.put("CHARGEUP_PARTICLES6", 50f);
        PARTICLES_PER_SECOND.put("CHARGEUP_PARTICLES_DIM1", 25f);
        PARTICLES_PER_SECOND.put("CHARGEUP_PARTICLES_DIM2", 25f);
        PARTICLES_PER_SECOND.put("CHARGEUP_PARTICLES_DIM3", 25f);
        PARTICLES_PER_SECOND.put("CHARGEUP_PARTICLES_DIM4", 25f);

    }

    //Does the PARTICLES_PER_SECOND field get multiplied by the weapon's current chargeLevel?
    private static final Map<String, Boolean> AFFECTED_BY_CHARGELEVEL = new HashMap<>();

    static {
        AFFECTED_BY_CHARGELEVEL.put("default", false);
        AFFECTED_BY_CHARGELEVEL.put("CHARGEUP_PARTICLES1", true);
        AFFECTED_BY_CHARGELEVEL.put("CHARGEUP_PARTICLES2", true);
        AFFECTED_BY_CHARGELEVEL.put("CHARGEUP_PARTICLES3", true);
        AFFECTED_BY_CHARGELEVEL.put("CHARGEUP_PARTICLES4", true);
        AFFECTED_BY_CHARGELEVEL.put("CHARGEUP_PARTICLES5", true);
        AFFECTED_BY_CHARGELEVEL.put("CHARGEUP_PARTICLES6", true);

    }

    //When are the particles spawned (only used for PARTICLES_PER_SECOND)? Valid values are "CHARGEUP", "FIRING", "CHARGEDOWN", "READY" (not on cooldown or firing) and "COOLDOWN".
    //  Multiple of these values can be combined via "-" inbetween; "CHARGEUP-CHARGEDOWN" is for example valid
    private static final Map<String, String> PARTICLE_SPAWN_MOMENT = new HashMap<>();

    static {
        PARTICLE_SPAWN_MOMENT.put("default", "FIRING");
        PARTICLE_SPAWN_MOMENT.put("CHARGEUP_PARTICLES1", "CHARGEUP");
        PARTICLE_SPAWN_MOMENT.put("CHARGEUP_PARTICLES2", "CHARGEUP");
        PARTICLE_SPAWN_MOMENT.put("CHARGEUP_PARTICLES3", "CHARGEUP");
        PARTICLE_SPAWN_MOMENT.put("CHARGEUP_PARTICLES4", "CHARGEUP");
        PARTICLE_SPAWN_MOMENT.put("CHARGEUP_PARTICLES5", "CHARGEUP");
        PARTICLE_SPAWN_MOMENT.put("CHARGEUP_PARTICLES6", "CHARGEUP");
        PARTICLE_SPAWN_MOMENT.put("CHARGEUP_PARTICLES_DIM1", "CHARGEUP");
        PARTICLE_SPAWN_MOMENT.put("CHARGEUP_PARTICLES_DIM2", "CHARGEUP");
        PARTICLE_SPAWN_MOMENT.put("CHARGEUP_PARTICLES_DIM3", "CHARGEUP");
        PARTICLE_SPAWN_MOMENT.put("CHARGEUP_PARTICLES_DIM4", "CHARGEUP");

    }

    //If this is set to true, the particles spawn with regard to *barrel*, not *center*. Only works for ALTERNATING barrel types on weapons: for LINKED barrels you
    //  should instead set up their coordinates manually with PARTICLE_SPAWN_POINT_TURRET and PARTICLE_SPAWN_POINT_HARDPOINT
    private static final Map<String, Boolean> SPAWN_POINT_ANCHOR_ALTERNATION = new HashMap<>();

    static {
        SPAWN_POINT_ANCHOR_ALTERNATION.put("default", false);

    }

    //The position the particles are spawned (or at least where their arc originates when using offsets) compared to their weapon's center [or shot offset, see
    //SPAWN_POINT_ANCHOR_ALTERNATION above], if the weapon is a turret (or HIDDEN)
    private static final Map<String, Vector2f> PARTICLE_SPAWN_POINT_TURRET = new HashMap<>();
    private static final Map<String, Vector2f> PARTICLE_SPAWN_POINT_TURRET_RIGHT = new HashMap<>();

    static {
        PARTICLE_SPAWN_POINT_TURRET.put("default", new Vector2f(0f, 0f));
        PARTICLE_SPAWN_POINT_TURRET.put("CHARGEUP_PARTICLES1", new Vector2f(0f, 0f));
        PARTICLE_SPAWN_POINT_TURRET.put("CHARGEUP_PARTICLES2", new Vector2f(0f, 0f));
        PARTICLE_SPAWN_POINT_TURRET.put("CHARGEUP_PARTICLES3", new Vector2f(0f, 0f));
        PARTICLE_SPAWN_POINT_TURRET.put("CHARGEUP_PARTICLES4", new Vector2f(0f, 0f));
        PARTICLE_SPAWN_POINT_TURRET.put("CHARGEUP_PARTICLES5", new Vector2f(0f, 0f));
        PARTICLE_SPAWN_POINT_TURRET.put("CHARGEUP_PARTICLES6", new Vector2f(0f, 0f));
        PARTICLE_SPAWN_POINT_TURRET.put("CHARGEUP_PARTICLES_DIM2", new Vector2f(0f, 0f));
        PARTICLE_SPAWN_POINT_TURRET.put("CHARGEUP_PARTICLES_DIM3", new Vector2f(0f, 0f));
        PARTICLE_SPAWN_POINT_TURRET.put("CHARGEUP_PARTICLES_DIM4", new Vector2f(0f, 0f));
        PARTICLE_SPAWN_POINT_TURRET.put("CHARGEUP_PARTICLES_DIM1", new Vector2f(0f, 0f));

        PARTICLE_SPAWN_POINT_TURRET_RIGHT.put("default", new Vector2f(0f, 0f));


    }

    //The position the particles are spawned (or at least where their arc originates when using offsets) compared to their weapon's center [or shot offset, see
    //SPAWN_POINT_ANCHOR_ALTERNATION above], if the weapon is a hardpoint
    private static final Map<String, Vector2f> PARTICLE_SPAWN_POINT_HARDPOINT = new HashMap<>();
    private static final Map<String, Vector2f> PARTICLE_SPAWN_POINT_HARDPOINT_RIGHT = new HashMap<>();

    static {
        PARTICLE_SPAWN_POINT_HARDPOINT.put("default", new Vector2f(0f, 0f));

        PARTICLE_SPAWN_POINT_HARDPOINT_RIGHT.put("default", new Vector2f(0f, 0f));

    }

    //Which kind of particle is spawned (valid values are "SMOOTH", "BRIGHT" and "SMOKE")
    private static final Map<String, String> PARTICLE_TYPE = new HashMap<>();

    static {
        PARTICLE_TYPE.put("default", "BRIGHT");
        PARTICLE_TYPE.put("CHARGEUP_PARTICLES1", "BRIGHT");
        PARTICLE_TYPE.put("CHARGEUP_PARTICLES2", "BRIGHT");
        PARTICLE_TYPE.put("CHARGEUP_PARTICLES3", "BRIGHT");
        PARTICLE_TYPE.put("CHARGEUP_PARTICLES4", "BRIGHT");
        PARTICLE_TYPE.put("CHARGEUP_PARTICLES5", "BRIGHT");
        PARTICLE_TYPE.put("CHARGEUP_PARTICLES6", "BRIGHT");
        PARTICLE_TYPE.put("CHARGEUP_PARTICLES_DIM1", "BRIGHT");
        PARTICLE_TYPE.put("CHARGEUP_PARTICLES_DIM2", "BRIGHT");
        PARTICLE_TYPE.put("CHARGEUP_PARTICLES_DIM3", "BRIGHT");
        PARTICLE_TYPE.put("CHARGEUP_PARTICLES_DIM4", "BRIGHT");


    }

    //What color does the particles have?
    private static final Map<String, Color> PARTICLE_COLOR = new HashMap<>();

    static {
        PARTICLE_COLOR.put("default", new Color(200, 190, 190, 50));
        PARTICLE_COLOR.put("CHARGEUP_PARTICLES1", new Color(255, 238, 193, 105));
        PARTICLE_COLOR.put("CHARGEUP_PARTICLES2", new Color(255, 238, 193, 105));
        PARTICLE_COLOR.put("CHARGEUP_PARTICLES3", new Color(255, 238, 193, 105));
        PARTICLE_COLOR.put("CHARGEUP_PARTICLES4", new Color(255, 238, 193, 105));
        PARTICLE_COLOR.put("CHARGEUP_PARTICLES5", new Color(255, 238, 193, 105));
        PARTICLE_COLOR.put("CHARGEUP_PARTICLES6", new Color(255, 238, 193, 105));
        PARTICLE_COLOR.put("CHARGEUP_PARTICLES_DIM1", new Color(255, 238, 193, 45));
        PARTICLE_COLOR.put("CHARGEUP_PARTICLES_DIM2", new Color(255, 238, 193, 45));
        PARTICLE_COLOR.put("CHARGEUP_PARTICLES_DIM3", new Color(255, 238, 193, 45));
        PARTICLE_COLOR.put("CHARGEUP_PARTICLES_DIM4", new Color(255, 238, 193, 45));


    }

    //What's the smallest size the particles can have?
    private static final Map<String, Float> PARTICLE_SIZE_MIN = new HashMap<>();

    static {
        PARTICLE_SIZE_MIN.put("default", 5f);
        PARTICLE_SIZE_MIN.put("CHARGEUP_PARTICLES1", 3f);
        PARTICLE_SIZE_MIN.put("CHARGEUP_PARTICLES2", 3f);
        PARTICLE_SIZE_MIN.put("CHARGEUP_PARTICLES3", 3f);
        PARTICLE_SIZE_MIN.put("CHARGEUP_PARTICLES4", 3f);
        PARTICLE_SIZE_MIN.put("CHARGEUP_PARTICLES5", 3f);
        PARTICLE_SIZE_MIN.put("CHARGEUP_PARTICLES6", 3f);
        PARTICLE_SIZE_MIN.put("BCHARGEUP_PARTICLES_DIM1", 7.5f);
        PARTICLE_SIZE_MIN.put("CHARGEUP_PARTICLES_DIM2", 7.5f);
        PARTICLE_SIZE_MIN.put("CHARGEUP_PARTICLES_DIM3", 7.5f);
        PARTICLE_SIZE_MIN.put("CHARGEUP_PARTICLES_DIM4", 7.5f);


    }

    //What's the largest size the particles can have?
    private static final Map<String, Float> PARTICLE_SIZE_MAX = new HashMap<>();

    static {
        PARTICLE_SIZE_MAX.put("default", 20f);
        PARTICLE_SIZE_MAX.put("CHARGEUP_PARTICLES1", 12.5f);
        PARTICLE_SIZE_MAX.put("CHARGEUP_PARTICLES2", 12.5f);
        PARTICLE_SIZE_MAX.put("CHARGEUP_PARTICLES3", 12.5f);
        PARTICLE_SIZE_MAX.put("CHARGEUP_PARTICLES4", 12.5f);
        PARTICLE_SIZE_MAX.put("CHARGEUP_PARTICLES5", 12.5f);
        PARTICLE_SIZE_MAX.put("CHARGEUP_PARTICLES6", 12.5f);
        PARTICLE_SIZE_MAX.put("CHARGEUP_PARTICLES_DIM1", 15f);
        PARTICLE_SIZE_MAX.put("CHARGEUP_PARTICLES_DIM2", 15f);
        PARTICLE_SIZE_MAX.put("CHARGEUP_PARTICLES_DIM3", 15f);
        PARTICLE_SIZE_MAX.put("CHARGEUP_PARTICLES_DIM4", 15f);


    }

    //What's the lowest velocity a particle can spawn with (can be negative)?
    private static final Map<String, Float> PARTICLE_VELOCITY_MIN = new HashMap<>();

    static {
        PARTICLE_VELOCITY_MIN.put("default", 0f);
        PARTICLE_VELOCITY_MIN.put("CHARGEUP_PARTICLES1", 250f);
        PARTICLE_VELOCITY_MIN.put("CHARGEUP_PARTICLES2", 250f);
        PARTICLE_VELOCITY_MIN.put("CHARGEUP_PARTICLES3", 250f);
        PARTICLE_VELOCITY_MIN.put("CHARGEUP_PARTICLES4", 250f);
        PARTICLE_VELOCITY_MIN.put("CHARGEUP_PARTICLES5", 250f);
        PARTICLE_VELOCITY_MIN.put("CHARGEUP_PARTICLES6", 250f);
        PARTICLE_VELOCITY_MIN.put("CHARGEUP_PARTICLES_DIM1", 135f);
        PARTICLE_VELOCITY_MIN.put("CHARGEUP_PARTICLES_DIM2", 135f);
        PARTICLE_VELOCITY_MIN.put("CHARGEUP_PARTICLES_DIM3", 135f);
        PARTICLE_VELOCITY_MIN.put("CHARGEUP_PARTICLES_DIM4", 135f);


    }

    //What's the highest velocity a particle can spawn with (can be negative)?
    private static final Map<String, Float> PARTICLE_VELOCITY_MAX = new HashMap<>();

    static {
        PARTICLE_VELOCITY_MAX.put("default", 40f);
        PARTICLE_VELOCITY_MAX.put("CHARGEUP_PARTICLES1", 500f);
        PARTICLE_VELOCITY_MAX.put("CHARGEUP_PARTICLES2", 500f);
        PARTICLE_VELOCITY_MAX.put("CHARGEUP_PARTICLES3", 500f);
        PARTICLE_VELOCITY_MAX.put("CHARGEUP_PARTICLES4", 500f);
        PARTICLE_VELOCITY_MAX.put("CHARGEUP_PARTICLES5", 500f);
        PARTICLE_VELOCITY_MAX.put("CHARGEUP_PARTICLES6", 500f);
        PARTICLE_VELOCITY_MAX.put("CHARGEUP_PARTICLES_DIM1", 240f);
        PARTICLE_VELOCITY_MAX.put("CHARGEUP_PARTICLES_DIM2", 240f);
        PARTICLE_VELOCITY_MAX.put("CHARGEUP_PARTICLES_DIM3", 240f);
        PARTICLE_VELOCITY_MAX.put("CHARGEUP_PARTICLES_DIM4", 240f);


    }

    //The shortest duration a particle will last before completely fading away
    private static final Map<String, Float> PARTICLE_DURATION_MIN = new HashMap<>();

    static {
        PARTICLE_DURATION_MIN.put("default", 1f);
        PARTICLE_DURATION_MIN.put("CHARGEUP_PARTICLES1", 0.15f);
        PARTICLE_DURATION_MIN.put("CHARGEUP_PARTICLES2", 0.15f);
        PARTICLE_DURATION_MIN.put("CHARGEUP_PARTICLES3", 0.15f);
        PARTICLE_DURATION_MIN.put("CHARGEUP_PARTICLES4", 0.15f);
        PARTICLE_DURATION_MIN.put("CHARGEUP_PARTICLES5", 0.15f);
        PARTICLE_DURATION_MIN.put("CHARGEUP_PARTICLES6", 0.15f);
        PARTICLE_DURATION_MIN.put("CHARGEUP_PARTICLES_DIM1", 0.33f);
        PARTICLE_DURATION_MIN.put("CHARGEUP_PARTICLES_DIM2", 0.33f);
        PARTICLE_DURATION_MIN.put("CHARGEUP_PARTICLES_DIM3", 0.33f);
        PARTICLE_DURATION_MIN.put("CHARGEUP_PARTICLES_DIM4", 0.33f);


    }

    //The longest duration a particle will last before completely fading away
    private static final Map<String, Float> PARTICLE_DURATION_MAX = new HashMap<>();

    static {
        PARTICLE_DURATION_MAX.put("default", 1.5f);
        PARTICLE_DURATION_MAX.put("CHARGEUP_PARTICLES1", 0.55f);
        PARTICLE_DURATION_MAX.put("CHARGEUP_PARTICLES2", 0.55f);
        PARTICLE_DURATION_MAX.put("CHARGEUP_PARTICLES3", 0.55f);
        PARTICLE_DURATION_MAX.put("CHARGEUP_PARTICLES4", 0.55f);
        PARTICLE_DURATION_MAX.put("CHARGEUP_PARTICLES5", 0.55f);
        PARTICLE_DURATION_MAX.put("CHARGEUP_PARTICLES6", 0.55f);
        PARTICLE_DURATION_MAX.put("CHARGEUP_PARTICLES_DIM1", 1.35f);
        PARTICLE_DURATION_MAX.put("CHARGEUP_PARTICLES_DIM2", 1.35f);
        PARTICLE_DURATION_MAX.put("CHARGEUP_PARTICLES_DIM3", 1.35f);
        PARTICLE_DURATION_MAX.put("CHARGEUP_PARTICLES_DIM4", 1.35f);

    }

    //The shortest along their velocity vector any individual particle is allowed to spawn (can be negative to spawn behind their origin point)
    private static final Map<String, Float> PARTICLE_OFFSET_MIN = new HashMap<>();

    static {
        PARTICLE_OFFSET_MIN.put("default", 0f);
        PARTICLE_OFFSET_MIN.put("CHARGEUP_PARTICLES1", -225f);
        PARTICLE_OFFSET_MIN.put("CHARGEUP_PARTICLES2", -225f);
        PARTICLE_OFFSET_MIN.put("CHARGEUP_PARTICLES3", -225f);
        PARTICLE_OFFSET_MIN.put("CHARGEUP_PARTICLES4", -225f);
        PARTICLE_OFFSET_MIN.put("CHARGEUP_PARTICLES5", -225f);
        PARTICLE_OFFSET_MIN.put("CHARGEUP_PARTICLES6", -225f);
        PARTICLE_OFFSET_MIN.put("CHARGEUP_PARTICLES_DIM1", -325f);
        PARTICLE_OFFSET_MIN.put("CHARGEUP_PARTICLES_DIM2", -325f);
        PARTICLE_OFFSET_MIN.put("CHARGEUP_PARTICLES_DIM3", -325f);
        PARTICLE_OFFSET_MIN.put("CHARGEUP_PARTICLES_DIM4", -325f);



    }

    //The furthest along their velocity vector any individual particle is allowed to spawn (can be negative to spawn behind their origin point)
    private static final Map<String, Float> PARTICLE_OFFSET_MAX = new HashMap<>();

    static {
        PARTICLE_OFFSET_MAX.put("default", 0f);
        PARTICLE_OFFSET_MAX.put("CHARGEUP_PARTICLES1", -75f);
        PARTICLE_OFFSET_MAX.put("CHARGEUP_PARTICLES2", -75f);
        PARTICLE_OFFSET_MAX.put("CHARGEUP_PARTICLES3", -75f);
        PARTICLE_OFFSET_MAX.put("CHARGEUP_PARTICLES4", -75f);
        PARTICLE_OFFSET_MAX.put("CHARGEUP_PARTICLES5", -75f);
        PARTICLE_OFFSET_MAX.put("CHARGEUP_PARTICLES6", -75f);
        PARTICLE_OFFSET_MAX.put("CHARGEUP_PARTICLES_DIM1", -130f);
        PARTICLE_OFFSET_MAX.put("CHARGEUP_PARTICLES_DIM2", -130f);
        PARTICLE_OFFSET_MAX.put("CHARGEUP_PARTICLES_DIM3", -130f);
        PARTICLE_OFFSET_MAX.put("CHARGEUP_PARTICLES_DIM4", -130f);


    }

    //The width of the "arc" the particles spawn in; affects both offset and velocity. 360f = full circle, 0f = straight line
    private static final Map<String, Float> PARTICLE_ARC = new HashMap<>();

    static {
        PARTICLE_ARC.put("default", 10f);
        PARTICLE_ARC.put("CHARGEUP_PARTICLES1", 360f);
        PARTICLE_ARC.put("CHARGEUP_PARTICLES2", 360f);
        PARTICLE_ARC.put("CHARGEUP_PARTICLES3", 360f);
        PARTICLE_ARC.put("CHARGEUP_PARTICLES4", 360f);
        PARTICLE_ARC.put("CHARGEUP_PARTICLES5", 360f);
        PARTICLE_ARC.put("CHARGEUP_PARTICLES6", 360f);
        PARTICLE_ARC.put("CHARGEUP_PARTICLES_DIM1", 360f);
        PARTICLE_ARC.put("CHARGEUP_PARTICLES_DIM2", 360f);
        PARTICLE_ARC.put("CHARGEUP_PARTICLES_DIM3", 360f);
        PARTICLE_ARC.put("CHARGEUP_PARTICLES_DIM4", 360f);


    }

    //The offset of the "arc" the particles spawn in, compared to the weapon's forward facing.
    //  For example: 90f = the center of the arc is 90 degrees clockwise around the weapon, 0f = the same arc center as the weapon's facing.
    private static final Map<String, Float> PARTICLE_ARC_FACING = new HashMap<>();
    private static final Map<String, Float> PARTICLE_ARC_FACING_RIGHT = new HashMap<>();

    static {
        PARTICLE_ARC_FACING.put("default", 0f);
        PARTICLE_ARC_FACING.put("CHARGEUP_PARTICLES1", 360f);
        PARTICLE_ARC_FACING.put("CHARGEUP_PARTICLES2", 360f);
        PARTICLE_ARC_FACING.put("CHARGEUP_PARTICLES3", 360f);
        PARTICLE_ARC_FACING.put("CHARGEUP_PARTICLES4", 360f);
        PARTICLE_ARC_FACING.put("CHARGEUP_PARTICLES5", 360f);
        PARTICLE_ARC_FACING.put("CHARGEUP_PARTICLES6", 360f);
        PARTICLE_ARC_FACING.put("CHARGEUP_PARTICLES_DIM1", 360f);
        PARTICLE_ARC_FACING.put("CHARGEUP_PARTICLES_DIM2", 360f);
        PARTICLE_ARC_FACING.put("CHARGEUP_PARTICLES_DIM3", 360f);
        PARTICLE_ARC_FACING.put("CHARGEUP_PARTICLES_DIM4", 360f);


        PARTICLE_ARC_FACING_RIGHT.put("default", 0f);

    }

    //How far away from the screen's edge the particles are allowed to spawn. Lower values mean better performance, but
    //too low values will cause pop-in of particles. Generally, the longer the particle's lifetime, the higher this
    //value should be
    private static final Map<String, Float> PARTICLE_SCREENSPACE_CULL_DISTANCE = new HashMap<>();

    static {
        PARTICLE_SCREENSPACE_CULL_DISTANCE.put("default", 650f);
    }


    //-----------------------------------------------------------You don't need to touch stuff beyond this point!------------------------------------------------------------


    //These are used in-script, so don't touch them!
    private boolean hasFiredThisCharge = false;
    private boolean hasFiredThisChargeBlowback = false;
    private int currentBarrel = 0;
    private boolean shouldOffsetBarrelExtra = false;
    private boolean RIGHT = false;

    //Instantiator

    boolean doOnce = true;

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (doOnce) {
            RIGHT = weapon.getSlot().getLocation().y > 0;
            doOnce = false;
        }
        //Don't run while paused, or without a weapon
        if (weapon == null || amount <= 0f) {
            return;
        }

        //Saves handy variables used later
        float chargeLevel = weapon.getChargeLevel();
        String sequenceState = "READY";
        if (chargeLevel > 0 && (!weapon.isBeam() || weapon.isFiring())) {
            if (chargeLevel >= 1f) {
                sequenceState = "FIRING";
            } else if (!hasFiredThisCharge) {
                sequenceState = "CHARGEUP";
            } else {
                sequenceState = "CHARGEDOWN";
            }
        } else if (weapon.getCooldownRemaining() > 0) {
            sequenceState = "COOLDOWN";
        }

        //Adjustment for burst beams, since they are a pain
        if (weapon.isBurstBeam() && sequenceState.contains("CHARGEDOWN")) {
            chargeLevel = Math.max(0f, Math.min(Math.abs(weapon.getCooldownRemaining() - weapon.getCooldown()) / weapon.getSpec().getDerivedStats().getBurstFireDuration(), 1f));
        }

        //The sequenceStates "CHARGEDOWN" and "COOLDOWN" counts its barrel as 1 earlier than usual, due to code limitations
        shouldOffsetBarrelExtra = sequenceState.contains("CHARGEDOWN") || sequenceState.contains("COOLDOWN");

        //We go through each of our particle systems and handle their particle spawning
        for (String ID : USED_IDS) {
            //Screenspace check: simplified but should do the trick 99% of the time
            float screenspaceCullingDistance = PARTICLE_SCREENSPACE_CULL_DISTANCE.get("default");
            if (PARTICLE_SCREENSPACE_CULL_DISTANCE.containsKey(ID)) {
                screenspaceCullingDistance = PARTICLE_SCREENSPACE_CULL_DISTANCE.get(ID);
            }
            if (!engine.getViewport().isNearViewport(weapon.getLocation(), screenspaceCullingDistance)) {
                continue;
            }

            //Store all the values used for this check, and use default values if we don't have specific values for our ID specified
            //Note that particle count, specifically, is not declared here and is only used in more local if-cases
            boolean affectedByChargeLevel = AFFECTED_BY_CHARGELEVEL.get("default");
            if (AFFECTED_BY_CHARGELEVEL.containsKey(ID)) {
                affectedByChargeLevel = AFFECTED_BY_CHARGELEVEL.get(ID);
            }

            String particleSpawnMoment = PARTICLE_SPAWN_MOMENT.get("default");
            if (PARTICLE_SPAWN_MOMENT.containsKey(ID)) {
                particleSpawnMoment = PARTICLE_SPAWN_MOMENT.get(ID);
            }

            boolean spawnPointAnchorAlternation = SPAWN_POINT_ANCHOR_ALTERNATION.get("default");
            if (SPAWN_POINT_ANCHOR_ALTERNATION.containsKey(ID)) {
                spawnPointAnchorAlternation = SPAWN_POINT_ANCHOR_ALTERNATION.get(ID);
            }

            //Here, we only store one value, depending on if we're a hardpoint or not
            Vector2f particleSpawnPoint = PARTICLE_SPAWN_POINT_TURRET.get("default");
            if (weapon.getSlot().isHardpoint()) {
                particleSpawnPoint = PARTICLE_SPAWN_POINT_HARDPOINT.get("default");
                if (PARTICLE_SPAWN_POINT_HARDPOINT.containsKey(ID)) {
                    particleSpawnPoint = PARTICLE_SPAWN_POINT_HARDPOINT.get(ID);
                }
            } else {
                if (PARTICLE_SPAWN_POINT_TURRET.containsKey(ID)) {
                    particleSpawnPoint = PARTICLE_SPAWN_POINT_TURRET.get(ID);
                }
            }

            String particleType = PARTICLE_TYPE.get("default");
            if (PARTICLE_TYPE.containsKey(ID)) {
                particleType = PARTICLE_TYPE.get(ID);
            }

            Color particleColor = PARTICLE_COLOR.get("default");
            if (PARTICLE_COLOR.containsKey(ID)) {
                particleColor = PARTICLE_COLOR.get(ID);
            }

            float particleSizeMin = PARTICLE_SIZE_MIN.get("default");
            if (PARTICLE_SIZE_MIN.containsKey(ID)) {
                particleSizeMin = PARTICLE_SIZE_MIN.get(ID);
            }
            float particleSizeMax = PARTICLE_SIZE_MAX.get("default");
            if (PARTICLE_SIZE_MAX.containsKey(ID)) {
                particleSizeMax = PARTICLE_SIZE_MAX.get(ID);
            }

            float particleVelocityMin = PARTICLE_VELOCITY_MIN.get("default");
            if (PARTICLE_VELOCITY_MIN.containsKey(ID)) {
                particleVelocityMin = PARTICLE_VELOCITY_MIN.get(ID);
            }
            float particleVelocityMax = PARTICLE_VELOCITY_MAX.get("default");
            if (PARTICLE_VELOCITY_MAX.containsKey(ID)) {
                particleVelocityMax = PARTICLE_VELOCITY_MAX.get(ID);
            }

            float particleDurationMin = PARTICLE_DURATION_MIN.get("default");
            if (PARTICLE_DURATION_MIN.containsKey(ID)) {
                particleDurationMin = PARTICLE_DURATION_MIN.get(ID);
            }
            float particleDurationMax = PARTICLE_DURATION_MAX.get("default");
            if (PARTICLE_DURATION_MAX.containsKey(ID)) {
                particleDurationMax = PARTICLE_DURATION_MAX.get(ID);
            }

            float particleOffsetMin = PARTICLE_OFFSET_MIN.get("default");
            if (PARTICLE_OFFSET_MIN.containsKey(ID)) {
                particleOffsetMin = PARTICLE_OFFSET_MIN.get(ID);
            }
            float particleOffsetMax = PARTICLE_OFFSET_MAX.get("default");
            if (PARTICLE_OFFSET_MAX.containsKey(ID)) {
                particleOffsetMax = PARTICLE_OFFSET_MAX.get(ID);
            }

            float particleArc = PARTICLE_ARC.get("default");
            if (PARTICLE_ARC.containsKey(ID)) {
                particleArc = PARTICLE_ARC.get(ID);
            }
            float particleArcFacing = PARTICLE_ARC_FACING.get("default");
            if (PARTICLE_ARC_FACING.containsKey(ID)) {
                particleArcFacing = PARTICLE_ARC_FACING.get(ID);
            }
            //---------------------------------------END OF DECLARATIONS-----------------------------------------

            //First, spawn "on full firing" particles, since those ignore sequence state
            if (chargeLevel >= 1f && !hasFiredThisCharge) {

                //Count spawned particles: only trigger if the spawned particles are more than 0
                float particleCount = ON_SHOT_PARTICLE_COUNT.get("default");
                if (ON_SHOT_PARTICLE_COUNT.containsKey(ID)) {
                    particleCount = ON_SHOT_PARTICLE_COUNT.get(ID);
                }

                if (particleCount > 0) {
                    spawnParticles(engine, weapon, particleCount, particleType, spawnPointAnchorAlternation, particleSpawnPoint, particleColor, particleSizeMin, particleSizeMax, particleVelocityMin, particleVelocityMax,
                            particleDurationMin, particleDurationMax, particleOffsetMin, particleOffsetMax, particleArc, particleArcFacing);
                }
            }

            //Then, we check if we should spawn particles over duration; only spawn if our spawn moment is in the declaration
            if (particleSpawnMoment.contains(sequenceState)) {
                //Get how many particles should be spawned this frame
                float particleCount = PARTICLES_PER_SECOND.get("default");
                if (PARTICLES_PER_SECOND.containsKey(ID)) {
                    particleCount = PARTICLES_PER_SECOND.get(ID);
                }
                particleCount *= amount;
                if (affectedByChargeLevel && (sequenceState.contains("CHARGEUP") || sequenceState.contains("CHARGEDOWN"))) {
                    particleCount *= chargeLevel;
                }
                if (affectedByChargeLevel && sequenceState.contains("COOLDOWN")) {
                    particleCount *= (weapon.getCooldownRemaining() / weapon.getCooldown());
                }

                //Then, if the particle count is greater than 0, we actually spawn the particles
                if (particleCount > 0f) {
                    spawnParticles(engine, weapon, particleCount, particleType, spawnPointAnchorAlternation, particleSpawnPoint, particleColor, particleSizeMin, particleSizeMax,
                            particleVelocityMin, particleVelocityMax, particleDurationMin, particleDurationMax, particleOffsetMin, particleOffsetMax,
                            particleArc, particleArcFacing);
                }
            }
        }

        //If this was our "reached full charge" frame, register that
        if (chargeLevel >= 1f && !hasFiredThisCharge) {
            hasFiredThisCharge = true;

            // beams don't like onfires so I mashed this in here
            Vector2f weaponLocation = weapon.getLocation();
            float weaponFacing = weapon.getCurrAngle();
            Vector2f muzzleLocationShockwave = MathUtils.getPointOnCircumference(weaponLocation, weapon.getSlot().isTurret() ? MUZZLE_OFFSET_TURRET_SHOCKWAVE : MUZZLE_OFFSET_HARDPOINT_SHOCKWAVE, weaponFacing);
            Vector2f muzzleLocationShockwaveSprite1 = MathUtils.getPointOnCircumference(weaponLocation, weapon.getSlot().isTurret() ? MUZZLE_OFFSET_TURRET_SHOCKWAVE_SPRITE1 : MUZZLE_OFFSET_HARDPOINT_SHOCKWAVE_SPRITE1, weaponFacing);
            Vector2f muzzleLocationShockwaveSprite2 = MathUtils.getPointOnCircumference(weaponLocation, weapon.getSlot().isTurret() ? MUZZLE_OFFSET_TURRET_SHOCKWAVE_SPRITE2 : MUZZLE_OFFSET_HARDPOINT_SHOCKWAVE_SPRITE2, weaponFacing);
            Vector2f muzzleLocationShockwaveSprite3 = MathUtils.getPointOnCircumference(weaponLocation, weapon.getSlot().isTurret() ? MUZZLE_OFFSET_TURRET_SHOCKWAVE_SPRITE3 : MUZZLE_OFFSET_HARDPOINT_SHOCKWAVE_SPRITE3, weaponFacing);

            float trueArcFacing = weapon.getCurrAngle();
            try {
                trueArcFacing += weapon.getSpec().getTurretAngleOffsets().get(currentBarrel);
            } catch (Exception e) { }


            // Check if shader LIBERALS exist
            boolean shaderLibExists = Global.getSettings().getModManager().isModEnabled("shaderLib");
            light = shaderLibExists;

            if (light) {
                WaveDistortion wave = new WaveDistortion(muzzleLocationShockwave, ZERO);
                wave.setIntensity(5f);
                wave.setSize(250f);
                wave.flip(true);
                wave.setLifetime(0f);
                wave.fadeOutIntensity(0.35f);
                wave.setLocation(muzzleLocationShockwave);
                DistortionShader.addDistortion(wave);
            }

            // render funny ripple
            if (light) {
                bt_yoinked_graphicLibEffects.CustomRippleDistortion(muzzleLocationShockwave, ZERO, 450, 4, false, trueArcFacing + 180f, 160, 1f, 0.1f, 0.35f, 0.5f, 0.35f, 0f);
                bt_yoinked_graphicLibEffects.CustomRippleDistortion(muzzleLocationShockwave, ZERO, 650, 2.5f, false, trueArcFacing + 180f, 160, 1f, 0.1f, 0.55f, 0.5f, 0.6f, 0f);
                bt_yoinked_graphicLibEffects.CustomRippleDistortion(muzzleLocationShockwave, ZERO, 235, 2, false, trueArcFacing, 200, 1f, 0.1f, 0.15f, 0.9f, 0.35f, 0f);
                bt_yoinked_graphicLibEffects.CustomRippleDistortion(muzzleLocationShockwave, ZERO, 350, 1.5f, false, trueArcFacing, 200, 1f, 0.1f, 0.25f, 0.9f, 0.6f, 0f);
            }

            // render FX
            boolean magicLibExists = Global.getSettings().getModManager().isModEnabled("MagicLib");
            if (magicLibExists) {
                MagicRender.battlespace(Global.getSettings().getSprite("fx", "bt_holy_explosion"), muzzleLocationShockwaveSprite1, ZERO, new Vector2f(50f, 50), new Vector2f(1000, 1000), 0f, 0f, new Color(255, 253, 200, 255), true, 0.0f, 0f, 1.2f);
                MagicRender.battlespace(Global.getSettings().getSprite("fx", "bultach_holy_explosion_shockwave"), muzzleLocationShockwaveSprite2, ZERO, new Vector2f(50, 50), new Vector2f(30f, 100), weaponFacing, 45f, new Color(255, 243, 216, 219), true, 0.0f, 0f, 0.66f);
                MagicRender.battlespace(Global.getSettings().getSprite("fx", "bt_cleave_aura"), muzzleLocationShockwaveSprite3, ZERO, new Vector2f(50, 50), new Vector2f(1400, 1400), weaponFacing, 20f, new Color(255, 241, 233, 124), true, 0.0f, 0f, 1f);
            }
        }

        //Increase our current barrel if we have <= 0 chargeLevel OR have ceased firing for now, if we alternate, and have fired at least once since we last increased it
        //Also make sure the barrels "loop around", and reset our hasFired variable
        if (hasFiredThisCharge && (chargeLevel <= 0f || !weapon.isFiring())) {
            hasFiredThisCharge = false;
            currentBarrel++;

            //We can *technically* have different barrel counts for hardpoints, hiddens and turrets, so take that into account
            int barrelCount = weapon.getSpec().getTurretAngleOffsets().size();
            if (weapon.getSlot().isHardpoint()) {
                barrelCount = weapon.getSpec().getHardpointAngleOffsets().size();
            } else if (weapon.getSlot().isHidden()) {
                barrelCount = weapon.getSpec().getHiddenAngleOffsets().size();
            }

            if (currentBarrel >= barrelCount) {
                currentBarrel = 0;
            }
        }


        //Muzzle location calculation
        Vector2f pointExtra = new Vector2f();

        if (weapon.getSlot().isHardpoint()) {
            pointExtra.x = weapon.getSpec().getHardpointFireOffsets().get(0).x;
            pointExtra.y = weapon.getSpec().getHardpointFireOffsets().get(0).y;
        } else if (weapon.getSlot().isTurret()) {
            pointExtra.x = weapon.getSpec().getTurretFireOffsets().get(0).x;
            pointExtra.y = weapon.getSpec().getTurretFireOffsets().get(0).y;
        } else {
            pointExtra.x = weapon.getSpec().getHiddenFireOffsets().get(0).x;
            pointExtra.y = weapon.getSpec().getHiddenFireOffsets().get(0).y;
        }

        pointExtra = VectorUtils.rotate(pointExtra, weapon.getCurrAngle(), new Vector2f(0f, 0f));
        pointExtra.x += weapon.getLocation().x;
        pointExtra.y += weapon.getLocation().y;


        //active beam visuals
        if (sequenceState.equals("FIRING")) {
            if (engine == null || weapon == null || engine.isPaused()) return;

            SpriteAPI flare1 = Global.getSettings().getSprite("fx", "bt_holy_explosion");
            SpriteAPI flare2 = Global.getSettings().getSprite("fx", "bt_flare1");
            SpriteAPI flare3 = Global.getSettings().getSprite("fx", "bt_flare1");

            Vector2f point = new Vector2f(0f, 0f);
            VectorUtils.rotate(point, weapon.getShip().getFacing());
            Vector2f.add(point, weapon.getLocation(), point);

            MagicRender.singleframe(
                    flare1,
                    MathUtils.getRandomPointInCircle(point, MathUtils.getRandomNumberInRange(0f, 3f)),
                    new Vector2f(90f, 90f),
                    0f,
                    new Color(255, 229, 200, 115),
                    false
            );

            MagicRender.singleframe(
                    flare2,
                    MathUtils.getRandomPointInCircle(point, MathUtils.getRandomNumberInRange(0f, 3f)),
                    new Vector2f(350f, 50f),
                    0f,
                    new Color(255, 223, 175, 125),
                    true
            );

            MagicRender.singleframe(
                    flare3,
                    MathUtils.getRandomPointInCircle(point, MathUtils.getRandomNumberInRange(0f, 3f)),
                    new Vector2f(350f, 50f),
                    90f,
                    new Color(255, 223, 175, 125),
                    true

            );

            for (int i = 0; i < 3; i++) {
                MagicRender.battlespace(
                        Global.getSettings().getSprite("fx", "bt_cleave_aura"),
                        MathUtils.getRandomPointInCircle(weapon.getLocation(), MathUtils.getRandomNumberInRange(0f, 3f)),
                        new Vector2f(),
                        new Vector2f(18, 18),
                        new Vector2f(84 + 136 * i, 84 + 136 * i),
                        MathUtils.getRandomNumberInRange(0, 360),
                        MathUtils.getRandomNumberInRange(-15, 15),
                        new Color(255, 248, 238, 15),
                        true,
                        0.08f * i,
                        0.0f,
                        0.4f - i / 8f
                );
            }
        }
        //charging visuals
        if (sequenceState.equals("CHARGEUP")) {

            effectInterval.advance(engine.getElapsedInLastFrame());
            if (effectInterval.intervalElapsed()) {
                Vector2f arcPoint = MathUtils.getRandomPointInCircle(pointExtra, 350f * chargeLevel);
                EmpArcEntityAPI arc = engine.spawnEmpArcPierceShields(weapon.getShip(), pointExtra, weapon.getShip(),
                        new SimpleEntity(arcPoint),
                        DamageType.FRAGMENTATION,
                        0f,
                        0f,
                        350f,
                        null,
                        2f + 5f * chargeLevel,
                        PARTICLE_COLOR_EXTRA,
                        FLASH_COLOR
                );
                bt_divine_cleaver_util.CustomBubbleDistortion(
                        weapon.getLocation(),
                        weapon.getShip().getVelocity(),
                        (150+MathUtils.getRandomNumberInRange(0, 50))*chargeLevel,
                        2+(MathUtils.getRandomNumberInRange(0, 10))*chargeLevel,
                        true,
                        0,
                        0,
                        0,
                        0.5f,
                        0f,
                        0.05f,
                        0.5f,
                        0
                );
            }
        }


        //chargedown blowback visuals, unused in this script

        if (weapon.getChargeLevel() >= 1f && !hasFiredThisChargeBlowback) {
            hasFiredThisChargeBlowback = true;
        }
        if (hasFiredThisChargeBlowback && (weapon.getChargeLevel() <= 0f || !weapon.isFiring())) {
            hasFiredThisChargeBlowback = false;
        }

        if (weapon.getChargeLevel() > 0f && hasFiredThisChargeBlowback) {
            weaponChargedownBlowback = true;
            hasFiredThisChargeBlowback = false;
        }
        if (weaponChargedownBlowback) {
            emitSmokeBlowback = true;
            weaponChargedownBlowback = false;
        }


        if (timeBlowback >= durationBlowback) {
            timeBlowback = 0f;
            emitSmokeBlowback = false;
        }
        if (emitSmokeBlowback) {
            timeBlowback += amount;

            float smokeDir = weapon.getCurrAngle() + 180f;
            float smokeDirAngle = smokeDir + MathUtils.getRandomNumberInRange(-20, 20);

            Vector2f weaponLocation = weapon.getLocation();
            float shipFacing = weapon.getCurrAngle();
            Vector2f muzzleLocationBlowback = MathUtils.getPointOnCircumference(weaponLocation,
                    weapon.getSlot().isTurret() ? MUZZLE_OFFSET_TURRET_BLOWBACK : MUZZLE_OFFSET_HARDPOINT_BLOWBACK, shipFacing);

            Vector2f vel = (Vector2f) Misc.getUnitVectorAtDegreeAngle(smokeDirAngle).scale(100f);

            engine.addNebulaParticle(muzzleLocationBlowback, vel, MathUtils.getRandomNumberInRange(0f, 0f), 2.5f, 0.3f, 0.3f, 0.5f, new Color(255, 243, 220, 150));
            engine.addNebulaParticle(muzzleLocationBlowback, vel, MathUtils.getRandomNumberInRange(0f, 0f), 2.5f, 0.3f, 0.3f, 0.15f, new Color(255, 244, 217, 255));
        }


    }


    //Shorthand function for actually spawning the particles
    private void spawnParticles(CombatEngineAPI engine, WeaponAPI weapon, float count, String type, boolean anchorAlternation, Vector2f spawnPoint, Color color, float sizeMin, float sizeMax,
                                float velocityMin, float velocityMax, float durationMin, float durationMax,
                                float offsetMin, float offsetMax, float arc, float arcFacing) {
        //First, ensure we take barrel position into account if we use Anchor Alternation (note that the spawn location is actually rotated 90 degrees wrong, so we invert their x and y values)
        Vector2f trueCenterLocation = new Vector2f(spawnPoint.y, spawnPoint.x);
        float trueArcFacing = arcFacing;
        if (anchorAlternation) {
            if (weapon.getSlot().isHardpoint()) {
                trueCenterLocation.x += weapon.getSpec().getHardpointFireOffsets().get(currentBarrel).x;
                trueCenterLocation.y += weapon.getSpec().getHardpointFireOffsets().get(currentBarrel).y;
                trueArcFacing += weapon.getSpec().getHardpointAngleOffsets().get(currentBarrel);
            } else if (weapon.getSlot().isTurret()) {
                trueCenterLocation.x += weapon.getSpec().getTurretFireOffsets().get(currentBarrel).x;
                trueCenterLocation.y += weapon.getSpec().getTurretFireOffsets().get(currentBarrel).y;
                trueArcFacing += weapon.getSpec().getTurretAngleOffsets().get(currentBarrel);
            } else {
                trueCenterLocation.x += weapon.getSpec().getHiddenFireOffsets().get(currentBarrel).x;
                trueCenterLocation.y += weapon.getSpec().getHiddenFireOffsets().get(currentBarrel).y;
                trueArcFacing += weapon.getSpec().getHiddenAngleOffsets().get(currentBarrel);
            }
        }

        //Then, we offset the true position and facing with our weapon's position and facing, while also rotating the position depending on facing
        trueArcFacing += weapon.getCurrAngle();
        trueCenterLocation = VectorUtils.rotate(trueCenterLocation, weapon.getCurrAngle(), new Vector2f(0f, 0f));
        trueCenterLocation.x += weapon.getLocation().x;
        trueCenterLocation.y += weapon.getLocation().y;

        //Then, we can finally start spawning particles
        float counter = count;
        while (Math.random() < counter) {
            //Ticks down the counter
            counter--;

            //Gets a velocity for the particle
            float arcPoint = MathUtils.getRandomNumberInRange(trueArcFacing - (arc / 2f), trueArcFacing + (arc / 2f));
            Vector2f velocity = MathUtils.getPointOnCircumference(weapon.getShip().getVelocity(), MathUtils.getRandomNumberInRange(velocityMin, velocityMax),
                    arcPoint);

            //Gets a spawn location in the cone, depending on our offsetMin/Max
            Vector2f spawnLocation = MathUtils.getPointOnCircumference(trueCenterLocation, MathUtils.getRandomNumberInRange(offsetMin, offsetMax),
                    arcPoint);

            //Gets our duration
            float duration = MathUtils.getRandomNumberInRange(durationMin, durationMax);

            //Gets our size
            float size = MathUtils.getRandomNumberInRange(sizeMin, sizeMax);

            //Finally, determine type of particle to actually spawn and spawns it
            switch (type) {
                case "SMOOTH":
                    engine.addSmoothParticle(spawnLocation, velocity, size, 1f, duration, color);
                    break;
                case "SMOKE":
                    engine.addSmokeParticle(spawnLocation, velocity, size, 1f, duration, color);
                    break;
                case "NEBULA":
                    engine.addNebulaParticle(spawnLocation, velocity, size, 1.5f, 0.1f, 0.2f, duration, color);
                    break;
                case "NEBULA_SMOKE":
                    engine.addNebulaSmokeParticle(spawnLocation, velocity, size, 1.5f, 0.1f, 0.2f, duration, color);
                    break;
                case "NEBULA_SWIRLY":
                    engine.addSwirlyNebulaParticle(spawnLocation, velocity, size, 1.5f, 0.1f, 0.2f, duration, color, true);
                    break;
                default:
                    engine.addHitParticle(spawnLocation, velocity, size, 10f, duration, color);
                    break;
            }
        }

    }
//I didn't remove this because im scared it will break
    public void onFire(final DamagingProjectileAPI projectile, WeaponAPI weapon, final CombatEngineAPI engine) {
        Vector2f weaponLocation = weapon.getLocation();
        float weaponFacing = weapon.getCurrAngle();
        Vector2f additionalOffset = VectorUtils.rotate(new Vector2f(0, 50), weaponFacing - 90);
        Vector2f.add(additionalOffset, weaponLocation, null);
        Vector2f muzzleLocationShockwave = MathUtils.getPointOnCircumference(weaponLocation,
                weapon.getSlot().isTurret() ? MUZZLE_OFFSET_HARDPOINT_SHOCKWAVE : MUZZLE_OFFSET_TURRET_SHOCKWAVE, weaponFacing);
        Vector2f muzzleLocationShockwaveSprite1 = MathUtils.getPointOnCircumference(weaponLocation,
                weapon.getSlot().isTurret() ? MUZZLE_OFFSET_HARDPOINT_SHOCKWAVE_SPRITE1 : MUZZLE_OFFSET_TURRET_SHOCKWAVE_SPRITE1, weaponFacing);
        Vector2f muzzleLocationShockwaveSprite2 = MathUtils.getPointOnCircumference(weaponLocation,
                weapon.getSlot().isTurret() ? MUZZLE_OFFSET_HARDPOINT_SHOCKWAVE_SPRITE2 : MUZZLE_OFFSET_TURRET_SHOCKWAVE_SPRITE2, weaponFacing);
        Vector2f muzzleLocationShockwaveSprite3 = MathUtils.getPointOnCircumference(weaponLocation,
                weapon.getSlot().isTurret() ? MUZZLE_OFFSET_HARDPOINT_SHOCKWAVE_SPRITE3 : MUZZLE_OFFSET_TURRET_SHOCKWAVE_SPRITE3, weaponFacing);

        float trueArcFacing = weapon.getCurrAngle();
        trueArcFacing += weapon.getSpec().getTurretAngleOffsets().get(currentBarrel);


        Vector2f speed = weapon.getShip().getVelocity();

        WaveDistortion wave = new WaveDistortion(muzzleLocationShockwave, ZERO);
        wave.setIntensity(5f);
        wave.setSize(250f);
        wave.flip(true);
        wave.setLifetime(0f);
        wave.fadeOutIntensity(0.35f);
        wave.setLocation(muzzleLocationShockwave);
        DistortionShader.addDistortion(wave);

        if (Global.getSettings().getModManager().isModEnabled("shaderLib")) {
            light = true;
        }

        if (light) {
            bt_yoinked_graphicLibEffects.CustomRippleDistortion(
                    muzzleLocationShockwave,
                    ZERO,
                    250,
                    4,
                    false,
                    trueArcFacing + 180f,
                    160,
                    1f,
                    0.1f,
                    0.15f,
                    0.5f,
                    0.35f,
                    0f
            );

            bt_yoinked_graphicLibEffects.CustomRippleDistortion(
                    muzzleLocationShockwave,
                    ZERO,
                    350,
                    2.5f,
                    false,
                    trueArcFacing + 180f,
                    160,
                    1f,
                    0.1f,
                    0.25f,
                    0.5f,
                    0.6f,
                    0f
            );
            bt_yoinked_graphicLibEffects.CustomRippleDistortion(
                    muzzleLocationShockwave,
                    ZERO,
                    135,
                    2,
                    false,
                    trueArcFacing,
                    200,
                    1f,
                    0.1f,
                    0.15f,
                    0.5f,
                    0.35f,
                    0f
            );

            bt_yoinked_graphicLibEffects.CustomRippleDistortion(
                    muzzleLocationShockwave,
                    ZERO,
                    250,
                    1.5f,
                    false,
                    trueArcFacing,
                    200,
                    1f,
                    0.1f,
                    0.25f,
                    0.5f,
                    0.6f,
                    0f
            );
        }

        MagicRender.battlespace(
                Global.getSettings().getSprite("fx", "bt_holy_explosion"),
                muzzleLocationShockwaveSprite1,
                ZERO,
                new Vector2f(50f, 50),
                new Vector2f(800, 800),
                weaponFacing,
                80f,
                new Color(255, 253, 200, 255),
                true,
                0.0f,
                0f,
                0.5f
        );
        MagicRender.battlespace(
                Global.getSettings().getSprite("fx", "bultach_siegelaser_shockwave"),
                muzzleLocationShockwaveSprite2,
                ZERO,
                new Vector2f(30f, 100),
                new Vector2f(30f, 100),
                weaponFacing,
                0f,
                new Color(255, 243, 216, 255),
                true,
                0.0f,
                0f,
                0.66f
        );
        MagicRender.battlespace(
                Global.getSettings().getSprite("fx", "bultach_siegelaser_shockwave"),
                muzzleLocationShockwaveSprite3,
                ZERO,
                new Vector2f(20f, 70),
                new Vector2f(20, 70),
                weaponFacing,
                0f,
                new Color(255, 241, 233, 255),
                true,
                0.0f,
                0f,
                0.8f
        );

    }
}
