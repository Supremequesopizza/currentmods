package data.scripts.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.fleets.PersonalFleetScript;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetQuality;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetSize;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerNum;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerQuality;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionFleetAutoDespawn;
import org.lwjgl.util.vector.Vector2f;

public class bt_PersonalFleetAdmiral1 extends PersonalFleetScript {

    public bt_PersonalFleetAdmiral1() {
        super("bt_admiral1");
        setMinRespawnDelayDays(10f);
        setMaxRespawnDelayDays(20f);
    }

    @Override
    protected
    MarketAPI getSourceMarket() {
        return null;
    }

    @Override
    public CampaignFleetAPI spawnFleet() {

        MarketAPI dregruk = Global.getSector().getEconomy().getMarket("dregruk");

        FleetCreatorMission m = new FleetCreatorMission(random);
        m.beginFleet();

        Vector2f loc = dregruk.getLocationInHyperspace();

        m.triggerCreateFleet(FleetSize.MAXIMUM, FleetQuality.SMOD_2, "orks", FleetTypes.TASK_FORCE, loc);
        m.triggerSetFleetOfficers( OfficerNum.MORE, OfficerQuality.UNUSUALLY_HIGH);
        m.triggerSetFleetCommander(getPerson());
        m.triggerSetFleetFaction("orks");
        m.triggerSetPatrol();
        m.triggerSetFleetMemoryValue(MemFlags.MEMORY_KEY_SOURCE_MARKET, dregruk);
        m.triggerFleetSetNoFactionInName();
        m.triggerFleetSetName("Bultach Liberation Armada");
        m.triggerPatrolAllowTransponderOff();
        m.triggerOrderFleetPatrol(dregruk.getStarSystem());

        CampaignFleetAPI fleet = m.createFleet();
        FleetMemberAPI oldFlagship = fleet.getFlagship();
        FleetMemberAPI newFlagship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "ork_superdread_main_standard");
        fleet.getFleetData().addFleetMember(newFlagship);
        if (newFlagship != null && oldFlagship != null) {
            newFlagship.setCaptain(oldFlagship.getCaptain());
            oldFlagship.setFlagship(false);
            newFlagship.setFlagship(true);
            fleet.getFleetData().setFlagship(newFlagship);
            fleet.getFleetData().removeFleetMember(oldFlagship);}
        fleet.removeScriptsOfClass(MissionFleetAutoDespawn.class);
        dregruk.getContainingLocation().addEntity(fleet);
        fleet.setLocation(dregruk.getPlanetEntity().getLocation().x, dregruk.getPlanetEntity().getLocation().y);
        fleet.setFacing((float) random.nextFloat() * 360f);
        fleet.getFlagship().setShipName("BCN Enduring Defense");
        fleet.getFleetData().sort();
        return fleet;
    }

    @Override
    public boolean canSpawnFleetNow() {
        MarketAPI dregruk = Global.getSector().getEconomy().getMarket("dregruk");
        if (dregruk == null || dregruk.hasCondition(Conditions.DECIVILIZED)) return false;
        if (!dregruk.getFactionId().equals("orks")) return false;
        return true;
    }

    @Override
    public boolean shouldScriptBeRemoved() {
        return false;
    }
}