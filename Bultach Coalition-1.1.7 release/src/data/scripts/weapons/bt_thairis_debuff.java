package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import org.lwjgl.util.vector.Vector2f;
import java.util.Random;

import java.awt.*;

public class bt_thairis_debuff {

    public
    void applyDebuff(DamagingProjectileAPI projectile, ShipAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (!target.hasListenerOfClass(BultachDebuffListener.class)) {
            target.addListener(new BultachDebuffListener(target));

            if (target.getFluxTracker().showFloaty() || target == Global.getCombatEngine().getPlayerShip()) {
                String[] messages = {
                        "Let go.",
                        "Weighed.",
                        "You can sense it.",
                        "Listen.",
                        "We know you.",
                        "Not yet forsaken.",
                        "You can hear it."
                };

                // Select a random message
                Random random = new Random();
                String randomMessage = messages[random.nextInt(messages.length)];

                // Show the selected floaty text
                target.getFluxTracker().showOverloadFloatyIfNeeded(randomMessage, new Color(255, 155, 155), 4f, true);
            }
        } else {
            target.getListeners(BultachDebuffListener.class).get(0).addHit();
        }
    }

    public static
    class BultachDebuffListener implements DamageTakenModifier, AdvanceableListener {
        public static final float DEBUFF_DURATION = 10f;
        public static final float BONUS_PER_DEBUFF = 0.15f;
        public static final int MAX_NUM_DEBUFFS = 10;
        private static final Color JITTER_COLOR = new Color(255, 55, 55, 75);
        private static final Color JITTER_UNDER_COLOR = new Color(255, 55, 55, 155);
        private String statusMessage = null;
        private float damageMult;
        private float timer;
        private final ShipAPI target;

        public
        BultachDebuffListener(ShipAPI target) {
            this.target = target;
            this.timer = DEBUFF_DURATION;
            this.damageMult = 1.5f;
        }

        public
        void addHit() {
            timer = DEBUFF_DURATION;
            damageMult = Math.min(damageMult + BONUS_PER_DEBUFF, MAX_NUM_DEBUFFS * BONUS_PER_DEBUFF + 1.5f);
        }

        @Override
        public
        String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (damage == null) return null;

            damage.getModifier().modifyMult("bultach_debuff", damageMult);
            return "bultach_debuff";
        }

        @Override
        public
        void advance(float amount) {
            if (Global.getCombatEngine() == null) return;

            timer -= amount;
            float effectLevel = ((damageMult - 1f) / BONUS_PER_DEBUFF) / MAX_NUM_DEBUFFS;
            if (timer <= 1) {
                effectLevel *= timer;
            }
            if (timer <= 0) {
                target.removeListener(this);
            }
            target.setJitterUnder(this, JITTER_UNDER_COLOR, effectLevel, 5, 2f, 6f + effectLevel * 17);
            target.setJitter(this, JITTER_COLOR, effectLevel, 4, 1f, 2 + effectLevel * 17);

            if (target == Global.getCombatEngine().getPlayerShip()) {
                String[] statusMessages = {
                        "Your knees buckle.",
                        "The air feels heavy.",
                        "You can't recall your name.",
                        "Your heart seizes.",
                        "The stars beckon.",
                        "Something prickles behind your eyes.",
                        "Is something there?",
                        "You hear a faint hum.",
                        "You taste copper.",
                        "Something is wrong."
                };

                // Pick a random message
                statusMessage = statusMessages[(int) (Math.random() * statusMessages.length)];

                // Display it
                Global.getCombatEngine().maintainStatusForPlayerShip(this, "graphics/icons/hullsys/mote_attractor.png", "Beckoned.",
                        statusMessage, true);
            }

            if (timer > 0) {
                timer -= amount;
            }

            if (timer <= 1 && statusMessage != null) {
                statusMessage = null;  // Reset message after debuff ends

            }
        }
    }
}
