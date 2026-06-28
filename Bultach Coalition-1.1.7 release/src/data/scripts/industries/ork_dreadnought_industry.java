package data.scripts.industries;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.OrbitalStation;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.world.ork_station;

/**
 * Anargaias special industry script. Taken and modified with permission from Gwyvern.
 */
public class ork_dreadnought_industry extends OrbitalStation {
    //The defense bonus provided by the industry: 0.5f = 50% bonus (Station level), 2f = 200% bonus (Star Fortress level) etc.
    private static final float DEFENSE_BONUS = 4f;

    public static float IMPROVE_STABILITY_BONUS = 4f;


    @Override
    public boolean isAvailableToBuild() {
        return false;
    }

    @Override
    public String getUnavailableReason() {
        return "Station type unavailable.";
    }

    @Override
    public boolean showWhenUnavailable() {
        return false;
    }


    //Changes the cost and bonuses provided by the station
    //Copied from vanilla, and adjusted to use our custom defense levels
    @Override
    public void apply() {
        super.apply(false);

        int size = 7; // To match a star fortress

        modifyStabilityWithBaseMod();

        applyIncomeAndUpkeep(size);

        demand(Commodities.CREW, size);
        demand(Commodities.SUPPLIES, size);

        market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD)
                .modifyMult(getModId(), 1f + DEFENSE_BONUS, getNameForModifier());

        matchCommanderToAICore(aiCoreId);

        if (!isFunctional()) {
            supply.clear();
            unapply();
        } else {
            applyCRToStation();
        }
    }

    // Adds the special AI commander core. Mostly copied from the vanilla implementation, except we don't care about AI core
    //  -- TODO: Double-check that this matches up to vanilla expectations nowadays
    @Override
    protected void matchCommanderToAICore(String aiCore) {
        if (stationFleet == null) return;

        int level = 20;
        PersonAPI commander = OfficerManagerEvent.createOfficer(
                Global.getSector().getFaction("orks"), level, OfficerManagerEvent.SkillPickPreference.GENERIC, null);
        if (commander.getStats().getSkillLevel(Skills.GUNNERY_IMPLANTS) == 0) {
            commander.getStats().increaseSkill(Skills.GUNNERY_IMPLANTS); // Stations should always have Gunnery Implants leveled
        }
        //By vanilla convention, AI cores have all skills as Elite
        //  If we ask for more elite skills than possible, it just gives the maximum possible: thus, we ask for 100 to get everything elite
        OfficerManagerEvent.addEliteSkills(commander, 100, null);
        commander.setPortraitSprite(ork_station.ADMIN_PORTRAIT_PATH);
        commander.setName(new FullName(ork_station.ADMIN_FIRST_NAME,ork_station.ADMIN_LAST_NAME, FullName.Gender.ANY));

        // We don't want the flagship icon on the "station"
        if (stationFleet.getFlagship() != null) {
            stationFleet.getFlagship().setCaptain(commander);
            stationFleet.getFlagship().setFlagship(false);
        }
    }
}
