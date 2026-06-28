package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.combat.ShipAIConfig;

import static com.fs.starfarer.api.impl.campaign.ids.Personalities.RECKLESS;

public class bt_pather_assault_ai implements ShipSystemAIScript {

   private ShipAPI ship;
   private ShipSystemAPI system;
   private IntervalUtil tracker = new IntervalUtil(0.5f, 1.0f);
   private ShipAIConfig savedConfig = new ShipAIConfig();
   private ShipwideAIFlags flags;

   @Override
   public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
      this.ship = ship;
      this.system = system;
      this.flags = flags;

      if (ship.getShipAI() != null) {
         this.savedConfig = ship.getShipAI().getConfig();
      } else {
         this.savedConfig = new ShipAIConfig();
      }
   }

   @Override
   public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
      if (ship == null || system == null || !ship.isAlive()) {
         return;
      }

      tracker.advance(amount);
      if (!tracker.intervalElapsed()) {
         return;
      }

      if (flags.hasFlag(ShipwideAIFlags.AIFlags.BACKING_OFF)) {
         if (system.isActive()) {
            deactivateSystem();
         }
         return;
      }

      float cr = ship.getCurrentCR();
      if (cr < 0.2f) {
         if (system.isActive()) {
            deactivateSystem();
         }
         return;
      }

      ShipAPI closestEnemy = findClosestEnemy();
      float distanceToEnemy = closestEnemy != null ? MathUtils.getDistance(ship, closestEnemy) : Float.MAX_VALUE;

      if (distanceToEnemy > 2000f) {
         if (system.isActive()) {
            deactivateSystem();
         }
         return;
      }

      if (!system.isActive() && system.getState() == ShipSystemAPI.SystemState.IDLE && !ship.getFluxTracker().isOverloadedOrVenting()) {
         activateSystem();
      }

   }

   private void activateSystem() {
      ship.useSystem();
      savedConfig.personalityOverride = ship.getShipAI().getConfig().personalityOverride = RECKLESS;
   }


   private void deactivateSystem() {
      if (system.isActive()) {
         ship.useSystem();
      }
      if (ship.getShipAI().getConfig() != null) {
         ship.getShipAI().getConfig().personalityOverride = savedConfig.personalityOverride;
      }
   }

   private ShipAPI findClosestEnemy() {
      float minDistance = Float.MAX_VALUE;
      ShipAPI closestEnemy = null;

      for (ShipAPI enemy : AIUtils.getEnemiesOnMap(ship)) {
         float distance = MathUtils.getDistance(ship, enemy);
         if (distance < minDistance) {
            minDistance = distance;
            closestEnemy = enemy;
         }
      }

      return closestEnemy;
   }
}