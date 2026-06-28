package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;
public class bt_autofire_fluxsink implements AutofireAIPlugin {

  private final WeaponAPI weapon;
  private final ShipAPI ship;
  private boolean shouldFire = false;
  private final CombatEngineAPI engine;

  public bt_autofire_fluxsink(WeaponAPI weapon) {
    this.weapon = weapon;
    this.ship = weapon.getShip();
    this.engine = Global.getCombatEngine();
  }

  @Override
  public boolean shouldFire() {
    return shouldFire;
  }

  @Override
  public void advance(float amount) {
    shouldFire = false;
    // Aim the weapon to the midpoint of its arc
    weapon.setCurrAngle(weapon.getSlot().computeMidArcAngle(ship));
    if (ship.getFluxLevel() > 0.51f) {
      shouldFire = true;
      System.out.println("Shield Damage taken mult");
    }
  }

  @Override
  public void forceOff() {
    // Optionally, force the weapon to stop firing
  }

  @Override
  public Vector2f getTarget() {
    return null;
  }

  @Override
  public ShipAPI getTargetShip() {
    return null;
  }

  @Override
  public MissileAPI getTargetMissile() {
    return null;
  }

  @Override
  public WeaponAPI getWeapon() {
    return weapon;
  }
}
