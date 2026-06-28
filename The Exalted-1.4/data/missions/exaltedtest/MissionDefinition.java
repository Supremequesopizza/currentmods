package data.missions.exaltedtest;

import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

public class MissionDefinition implements MissionDefinitionPlugin {

	public void defineMission(MissionDefinitionAPI api) {

		// Set up the fleets so we can add ships and fighter wings to them.
		// In this scenario, the fleets are attacking each other, but
		// in other scenarios, a fleet may be defending or trying to escape
		api.initFleet(FleetSide.PLAYER, "EXS", FleetGoal.ATTACK, false);
		api.initFleet(FleetSide.ENEMY, "WHORE", FleetGoal.ATTACK, true);

//		api.getDefaultCommander(FleetSide.PLAYER).getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 3);
//		api.getDefaultCommander(FleetSide.PLAYER).getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 3);
		
		// Set a small blurb for each fleet that shows up on the mission detail and
		// mission results screens to identify each side.
		api.setFleetTagline(FleetSide.PLAYER, "Awesome fleet of awesomeness");
		api.setFleetTagline(FleetSide.ENEMY, "Stupid tri-tach Astral with ugly Astral escort");
		

		// These show up as items in the bulleted list under 
		// "Tactical Objectives" on the mission detail screen
		api.addBriefingItem("Take down these Nerds");
		api.addBriefingItem("Test me ships");
		api.addBriefingItem("Got any Grapes?");
		
		// Set up the player's fleet.  Variant names come from the
		// files in data/variants and data/variants/fighters
		api.addToFleet(FleetSide.PLAYER, "anvil_wendigo_exalted", FleetMemberType.SHIP, "test9", true);
		api.addToFleet(FleetSide.PLAYER, "anvil_shrike_ex_exalted", FleetMemberType.SHIP, "test55f9",false);
		api.addToFleet(FleetSide.PLAYER, "anvil_cielo_exalted", FleetMemberType.SHIP, "test554f9",false);
		api.addToFleet(FleetSide.PLAYER, "anvil_cielo_civ_support", FleetMemberType.SHIP, "test5554f9",false);
		api.addToFleet(FleetSide.PLAYER, "anvil_eerie_exalted", FleetMemberType.SHIP, "test5f9",false);
		api.addToFleet(FleetSide.PLAYER, "anvil_nightmare_exalted", FleetMemberType.SHIP, "test51", false);
		api.addToFleet(FleetSide.PLAYER, "anvil_vampire_exalted", FleetMemberType.SHIP, "test57",false);
		api.addToFleet(FleetSide.PLAYER, "anvil_urn_exalted", FleetMemberType.SHIP, "test56",false);
		api.addToFleet(FleetSide.PLAYER, "anvil_apparition_exalted", FleetMemberType.SHIP, "test1", false);
		api.addToFleet(FleetSide.PLAYER, "anvil_phylactery_exalted", FleetMemberType.SHIP, "test59",false);	
		api.addToFleet(FleetSide.PLAYER, "anvil_fantasma_exalted", FleetMemberType.SHIP, "test58",false);
		api.addToFleet(FleetSide.PLAYER, "anvil_lich_exalted", FleetMemberType.SHIP, "test17", false);
		api.addToFleet(FleetSide.PLAYER, "anvil_wraith_exalted", FleetMemberType.SHIP, "test21",false);
		api.addToFleet(FleetSide.PLAYER, "anvil_omen_ex_exalted", FleetMemberType.SHIP, "test42", false);
		api.addToFleet(FleetSide.PLAYER, "anvil_spirit_exalted", FleetMemberType.SHIP, "test49", false);
		api.addToFleet(FleetSide.PLAYER, "anvil_hoshiko_civ_support", FleetMemberType.SHIP, "test439", false);
		api.addToFleet(FleetSide.PLAYER, "anvil_hoshiko_exalted", FleetMemberType.SHIP, "test497", false);
		api.addToFleet(FleetSide.PLAYER, "anvil_faint_exalted", FleetMemberType.SHIP, "test4907", false);

		
		// Set up the enemy fleet.
		api.addToFleet(FleetSide.ENEMY, "astral_Strike", FleetMemberType.SHIP, "Stupid", false);
		api.addToFleet(FleetSide.ENEMY, "astral_Elite", FleetMemberType.SHIP, "Ugly", false);

		
		
		// Set up the map.
		float width = 12000f;
		float height = 12000f;
		api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);
		
		float minX = -width/2;
		float minY = -height/2;
		
		// Add an asteroid field
		api.addAsteroidField(minX, minY + height / 2, 0, 8000f,
							 20f, 70f, 100);
		
		api.addPlanet(-320, -140, 200f, "tundra", 250f, true);
		
	}

}
