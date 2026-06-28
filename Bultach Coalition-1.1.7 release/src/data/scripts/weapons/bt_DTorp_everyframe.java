package data.scripts.weapons;

import com.fs.starfarer.api.AnimationAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;

public class bt_DTorp_everyframe implements EveryFrameWeaponEffectPlugin {

    private static org.apache.log4j.Logger log = Global.getLogger(data.scripts.weapons.bt_DTorp_everyframe.class);
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

    float AOE = 500f;

    float time = 1f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || engine.isPaused()) {
            return;
        }

        ShipAPI ship = weapon.getShip();

        if(ship.getCustomData().containsKey("dtorplocs")) {
            log.info("locations in custom data2");
        }

        if (ship.getCustomData().containsKey("dtorplocs")) {

            ArrayList<Vector2f> targets = ((ArrayList<Vector2f>) ship.getCustomData().get("dtorplocs"));

            log.info("4");

            if (!targets.isEmpty()) {

                log.info("5");

                interval.advance(amount);

                ArrayList<Vector2f> locationsr = new ArrayList<>();

                for (Vector2f v : targets) {
                    if (!ship.getCustomData().containsKey(v + "timer")) {
                        ship.getCustomData().put(v + "timer", 0f);
                    }

                    log.info("6");

                    if (interval.intervalElapsed()) {
                        ship.getCustomData().put(v + "timer", (float) ship.getCustomData().get(v + "timer") + 0.05f);
                    }

                    for (int i = 0; i < 15; i++) {
                        float d = AOE * ((float) ship.getCustomData().get(v + "timer") / time);
                        engine.spawnEmpArcVisual(v,null,MathUtils.getRandomPointOnCircumference(v, d),null,10f,hit,hit);
                        //engine.addSmoothParticle(MathUtils.getRandomPointOnCircumference(v, d), ship.getVelocity(), d * 0.1f, 1f, 0.1f, new Color(254, 155, 155));
                        //engine.addSwirlyNebulaParticle(MathUtils.getRandomPointOnCircumference(weapon.getLocation(), d), ship.getVelocity(), d * 0.3f, 1f, 0f, b, 0.1f, new Color(254, 105, 105,Math.round(b*254)), false);
                    }

                    if (((float) ship.getCustomData().get(v + "timer")) > time) {
                        locationsr.add(v);

                        ship.getCustomData().remove(v + "timer");
                    }
                }

                if(!locationsr.isEmpty()) {
                    for (Vector2f lr : locationsr) {
                        ArrayList<Vector2f> locations = ((ArrayList<Vector2f>) ship.getCustomData().get("dtorplocs"));
                        locations.remove(lr);
                        ship.getCustomData().put("dtorplocs", locations);
                    }
                }
            }
        }
    }
}