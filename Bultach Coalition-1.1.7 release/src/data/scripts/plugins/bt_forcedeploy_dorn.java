package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

/**
 * Simple script that deploys Anar-- THE DORNDHE into battle should it somehow end up in reserves
 * @author Nicke535
 */
public class bt_forcedeploy_dorn extends BaseEveryFrameCombatPlugin {
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();

        if (engine.isSimulation()) {
            return;
        }

        //Check the reserves of both fleets
        for (FleetMemberAPI member : engine.getFleetManager(FleetSide.PLAYER).getReservesCopy()) {
            //If the reserve is Dorn, deploy it
            if (member.getHullId().contains("ork_superdread_main")) {
                ShipAPI dorn = engine.getFleetManager(FleetSide.PLAYER).spawnFleetMember(member,
                        new Vector2f(0f, engine.getFleetManager(FleetSide.PLAYER).getDeploymentYOffset()/2f - engine.getMapHeight()/3f),
                        90f, 0f);
                //On the player side, Dorn is always an ally
                dorn.setAlly(true);
            }
        }
        for (FleetMemberAPI member : engine.getFleetManager(FleetSide.ENEMY).getReservesCopy()) {
            //If the reserve is Dorn, deploy it
            if (member.getHullId().contains("ork_superdread_main")) {
                engine.getFleetManager(FleetSide.ENEMY).spawnFleetMember(member,
                        new Vector2f(0f, engine.getFleetManager(FleetSide.ENEMY).getDeploymentYOffset()/2f + engine.getMapHeight()/3f),
                        -90f, 0f);
            }
        }
    }
}
