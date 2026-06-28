package data.missions.times;

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
		api.initFleet(FleetSide.PLAYER, "ESS", FleetGoal.ATTACK, false);
		api.initFleet(FleetSide.ENEMY, "PISS", FleetGoal.ATTACK, true);

//		api.getDefaultCommander(FleetSide.PLAYER).getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 3);
//		api.getDefaultCommander(FleetSide.PLAYER).getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 3);
		
		// Set a small blurb for each fleet that shows up on the mission detail and
		// mission results screens to identify each side.
		api.setFleetTagline(FleetSide.PLAYER, "Exalted Supply Fleet");
		api.setFleetTagline(FleetSide.ENEMY, "Pirate Inc. Deep Range Hunter's");
		

		// These show up as items in the bulleted list under 
		// "Tactical Objectives" on the mission detail screen
		api.addBriefingItem("Destroy The Pirate Scum");
		
		// Set up the player's fleet.  Variant names come from the
		// files in data/variants and data/variants/fighters 			
		api.addToFleet(FleetSide.PLAYER, "anvil_odyssey_ex_exalted", FleetMemberType.SHIP, "Final Rest", true);
		api.addToFleet(FleetSide.PLAYER, "anvil_fantasma_exalted", FleetMemberType.SHIP, "Grim",false);
		api.addToFleet(FleetSide.PLAYER, "anvil_wraith_exalted", FleetMemberType.SHIP, "Reaper",false);
		api.addToFleet(FleetSide.PLAYER, "anvil_lich_exalted", FleetMemberType.SHIP, "Corpse IV", false);
		api.addToFleet(FleetSide.PLAYER, "anvil_vampire_exalted", FleetMemberType.SHIP, "Gods Bless This Ship", false);
		api.addToFleet(FleetSide.PLAYER, "anvil_vampire_exalted", FleetMemberType.SHIP, "drac",false);
		api.addToFleet(FleetSide.PLAYER, "anvil_apparition_exalted", FleetMemberType.SHIP, "DownWithLudd", false);
		api.addToFleet(FleetSide.PLAYER, "anvil_hoshiko_exalted", FleetMemberType.SHIP, "Aore", false);
		api.addToFleet(FleetSide.PLAYER, "anvil_hoshiko_exalted", FleetMemberType.SHIP, "ore", false);
		api.addToFleet(FleetSide.PLAYER, "anvil_omen_ex_exalted", FleetMemberType.SHIP, "Lighter Side", false);
		api.addToFleet(FleetSide.PLAYER, "anvil_omen_ex_exalted", FleetMemberType.SHIP, "False Money", false);
		api.addToFleet(FleetSide.PLAYER, "anvil_spirit_exalted", FleetMemberType.SHIP, "Bone Marrow", false);
		api.addToFleet(FleetSide.PLAYER, "anvil_eerie_exalted", FleetMemberType.SHIP, "Brain Matter", false);
		api.addToFleet(FleetSide.PLAYER, "anvil_eerie_exalted", FleetMemberType.SHIP, "Idiot Brain Fungus", false);
		api.addToFleet(FleetSide.PLAYER, "anvil_urn_exalted", FleetMemberType.SHIP, "Not Carrying Ash",false);
		api.addToFleet(FleetSide.PLAYER, "anvil_phylactery_exalted", FleetMemberType.SHIP, "Egger",false);
		api.addToFleet(FleetSide.PLAYER, "anvil_phylactery_exalted", FleetMemberType.SHIP, "Smegger",false);
		api.addToFleet(FleetSide.PLAYER, "anvil_phylactery_exalted", FleetMemberType.SHIP, "Legger",false);

		
		// Set up the enemy fleet.
		api.addToFleet(FleetSide.ENEMY, "onslaught_Standard", FleetMemberType.SHIP, "Is Your Fridge Running?", false);
		api.addToFleet(FleetSide.ENEMY, "onslaught_Outdated", FleetMemberType.SHIP, "Well You Better Go Catch It!", false);
		api.addToFleet(FleetSide.ENEMY, "colossus3_Pirate", FleetMemberType.SHIP, "Count Dooku", false);
		api.addToFleet(FleetSide.ENEMY, "colossus3_Pirate", FleetMemberType.SHIP, "Eggman", false);
		api.addToFleet(FleetSide.ENEMY, "mora_Support", FleetMemberType.SHIP, "Fat Basterd", false);
		api.addToFleet(FleetSide.ENEMY, "mora_Assault", FleetMemberType.SHIP, "Handsome Jack", false);
		api.addToFleet(FleetSide.ENEMY, "shrike_p_Attack", FleetMemberType.SHIP, "Dr. Evil", false);
		api.addToFleet(FleetSide.ENEMY, "enforcer_d_pirates_Strike", FleetMemberType.SHIP, "Piglet", false);
		api.addToFleet(FleetSide.ENEMY, "enforcer_d_pirates_Strike", FleetMemberType.SHIP, "Pineapple On Pizza", false);
		api.addToFleet(FleetSide.ENEMY, "enforcer_d_pirates_Strike", FleetMemberType.SHIP, "All Lives Matter", false);
		api.addToFleet(FleetSide.ENEMY, "mule_d_pirates_Standard", FleetMemberType.SHIP, "Capitalism", false);
		api.addToFleet(FleetSide.ENEMY, "mule_d_pirates_Standard", FleetMemberType.SHIP, "Guzma", false);
		api.addToFleet(FleetSide.ENEMY, "sunder_Overdriven", FleetMemberType.SHIP, "Father", false);
		api.addToFleet(FleetSide.ENEMY, "wolf_d_pirates_Attack", FleetMemberType.SHIP, "All For One", false);
		api.addToFleet(FleetSide.ENEMY, "wolf_d_pirates_Attack", FleetMemberType.SHIP, "Light", false);
		api.addToFleet(FleetSide.ENEMY, "scarab_Experimental", FleetMemberType.SHIP, "My Dad", false);
		api.addToFleet(FleetSide.ENEMY, "lasher_Standard", FleetMemberType.SHIP, "The Unicorn From Peggle", false);
		api.addToFleet(FleetSide.ENEMY, "lasher_Standard", FleetMemberType.SHIP, "The Onceler", false);
		api.addToFleet(FleetSide.ENEMY, "lasher_Standard", FleetMemberType.SHIP, "Flat Earther", false);
		api.addToFleet(FleetSide.ENEMY, "shade_d_pirates_Assault", FleetMemberType.SHIP, "Logen Paul", false);
		api.addToFleet(FleetSide.ENEMY, "shade_d_pirates_Assault", FleetMemberType.SHIP, "You", false);


		
		
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
