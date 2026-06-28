package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;

public class bt_iod_everyframe implements OnFireEffectPlugin {

    private static org.apache.log4j.Logger log = Global.getLogger(bt_iod_everyframe.class);
    int index = 0;

    public static Color LIGHTNING_CORE_COLOR = new Color(155, 41, 217, 100);
    public static Color LIGHTNING_FRINGE_COLOR = new Color(155, 41, 217, 0);
    public static Color LIGHTNING_FRINGE_COLOR2 = new Color(155, 41, 217, 90);

    public static Color hit = new Color(242, 85, 85, 162);
    public static Color hitfringe = new Color(200, 100, 100, 0);

    public static Color FRINGE_COLOR = new Color(164, 197, 219, 175);
    public static Color FRINGE_COLOR2 = new Color(19, 26, 14, 250);
    public static Color FRINGE_COLOR3 = new Color(255, 255, 200, 175);
    public static Color FRINGE_COLOR4 = new Color(19, 26, 14, 125);

    boolean firstRun = true;
    IntervalUtil interval = new IntervalUtil(0.05f, 0.05f);
    IntervalUtil forceterval = new IntervalUtil(0f, 0.1f);

    float exptime = 0.05f;

    float maxoffset = 50f;

    float AOE = 1000f;

    float time = 0.5f;


    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {

        projectile.setCollisionClass(CollisionClass.NONE);

        if(projectile.getSource()!=null) {
            projectile.getSource().addListener(new iodlistener(projectile.getSource(), projectile));
        }

        if(projectile.getWeapon()!=null){
            projectile.getWeapon().setAmmo(0);
        }
    }

    public class iodlistener implements AdvanceableListener {

        IntervalUtil interval = new IntervalUtil(0.05f, 0.05f);

        float timer = 0f;

        public final ShipAPI ship;
        public final DamagingProjectileAPI projectile;

        boolean runonce = true;

        public iodlistener(ShipAPI ship, DamagingProjectileAPI projectile) {
            this.ship = ship;
            this.projectile = projectile;
        }

        ArrayList<ShipAPI> aships = new ArrayList<>();

        @Override
        public void advance(float amount) {
            interval.advance(amount);

            if (interval.intervalElapsed()) {
                timer+=0.05f;
            }


            if (timer > exptime) {
                if(runonce) {
                    ship.getSystem().deactivate();
                    ship.getVelocity().set(0.15f,0.15f);
                    runonce = false;
                }
                for (WeaponAPI w : ship.getAllWeapons()) {
                    Vector2f offset = new Vector2f(Math.min(-maxoffset+(maxoffset*(timer-exptime)),0f), 0f);
                    if (w.getSpec().hasTag("pusherplate")) {
                        w.setRenderOffsetForDecorativeBeamWeaponsOnly(offset);
                    }
                }
            }

            if(timer-exptime>=1f){
                runonce = true;
                ship.removeListener(this);
            }

        }
    }
}