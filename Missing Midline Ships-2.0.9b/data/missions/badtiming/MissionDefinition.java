package data.missions.badtiming;

import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

public class MissionDefinition implements MissionDefinitionPlugin {

	public void defineMission(MissionDefinitionAPI api) {

		// Set up the fleets
		api.initFleet(FleetSide.PLAYER, "MSS", FleetGoal.ATTACK, false, 10);
		api.initFleet(FleetSide.ENEMY, "HSS", FleetGoal.ATTACK, true, 10);

		// Set a blurb for each fleet
		api.setFleetTagline(FleetSide.PLAYER, "Mayasurian Task Force returning to Mairaath");
		api.setFleetTagline(FleetSide.ENEMY, "Hegemony fleet loitering in Gelans Shackle");
		
		// These show up as items in the bulleted list under 
		// "Tactical Objectives" on the mission detail screen
		api.addBriefingItem("Defeat all enemy forces");
		api.addBriefingItem("MSS Kartikeya & MSS Sedna must survive");
		api.addBriefingItem("Maintain tactical awareness and use superior mobility to choose your battles.");
		api.addBriefingItem("Engage enemy capitals at worst 2 v 2, dealing with the third one is the difficulty");
		
		// Set up the player's fleet
		api.addToFleet(FleetSide.PLAYER, "missp_paradigm_Elite", FleetMemberType.SHIP, "MSS Kartikeya",true);
		api.addToFleet(FleetSide.PLAYER, "missp_chrysaor_Standard", FleetMemberType.SHIP, "MSS Devasena",false);
		api.addToFleet(FleetSide.PLAYER, "champion_Elite", FleetMemberType.SHIP, "MSS Angre",false);
		api.addToFleet(FleetSide.PLAYER, "gryphon_DEM", FleetMemberType.SHIP, "MSS Bhatkar",false);
		api.addToFleet(FleetSide.PLAYER, "heron_Strike", FleetMemberType.SHIP, "MSS Bhandari",false);
		api.addToFleet(FleetSide.PLAYER, "hammerhead_Balanced", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "drover_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "drover_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "centurion_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "centurion_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "vigilance_FS", FleetMemberType.SHIP, false);
		
//		api.addToFleet(FleetSide.PLAYER, "warthog_wing", FleetMemberType.FIGHTER_WING, false);
//		api.addToFleet(FleetSide.PLAYER, "warthog_wing", FleetMemberType.FIGHTER_WING, false);
//		api.addToFleet(FleetSide.PLAYER, "gladius_wing", FleetMemberType.FIGHTER_WING, false);
//		api.addToFleet(FleetSide.PLAYER, "gladius_wing", FleetMemberType.FIGHTER_WING, false);
//		api.addToFleet(FleetSide.PLAYER, "thunder_wing", FleetMemberType.FIGHTER_WING, false);
//		api.addToFleet(FleetSide.PLAYER, "thunder_wing", FleetMemberType.FIGHTER_WING, false);
//		api.addToFleet(FleetSide.PLAYER, "thunder_wing", FleetMemberType.FIGHTER_WING, false);
		
		// Mark player flagship as essential
		api.defeatOnShipLoss("MSS Kartikeya");
		api.defeatOnShipLoss("MSS Devasena");
		
		// Set up the enemy fleet
        api.addToFleet(FleetSide.ENEMY, "onslaught_xiv_Elite", FleetMemberType.SHIP, "HSS Legacy of the Domain", true);
		api.addToFleet(FleetSide.ENEMY, "onslaught_xiv_Elite", FleetMemberType.SHIP, "HSS Fire of Phoenix", false);
		api.addToFleet(FleetSide.ENEMY, "legion_xiv_Elite", FleetMemberType.SHIP, "HSS Spirit of the XIV", false);
//		api.addToFleet(FleetSide.ENEMY, "onslaught_xiv_Elite", FleetMemberType.SHIP, false);
//		api.addToFleet(FleetSide.ENEMY, "legion_xiv_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "dominator_XIV_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "dominator_XIV_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "mora_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "mora_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "mora_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "mora_Support", FleetMemberType.SHIP, false);
		//api.addToFleet(FleetSide.ENEMY, "dominator_XIV_Elite", FleetMemberType.SHIP, false);
		//api.addToFleet(FleetSide.ENEMY, "dominator_XIV_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "condor_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "condor_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "enforcer_XIV_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "enforcer_XIV_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "enforcer_XIV_Elite", FleetMemberType.SHIP, false);
		//api.addToFleet(FleetSide.ENEMY, "enforcer_XIV_Elite", FleetMemberType.SHIP, false);
		//api.addToFleet(FleetSide.ENEMY, "hound_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "hound_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "lasher_CS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "lasher_CS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "lasher_CS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "lasher_CS", FleetMemberType.SHIP, false);
		
//		api.addToFleet(FleetSide.ENEMY, "talon_wing", FleetMemberType.FIGHTER_WING, false);
//		api.addToFleet(FleetSide.ENEMY, "talon_wing", FleetMemberType.FIGHTER_WING, false);
//		api.addToFleet(FleetSide.ENEMY, "talon_wing", FleetMemberType.FIGHTER_WING, false);
//		api.addToFleet(FleetSide.ENEMY, "talon_wing", FleetMemberType.FIGHTER_WING, false);
//		api.addToFleet(FleetSide.ENEMY, "broadsword_wing", FleetMemberType.FIGHTER_WING, false);
//		api.addToFleet(FleetSide.ENEMY, "broadsword_wing", FleetMemberType.FIGHTER_WING, false);
//		api.addToFleet(FleetSide.ENEMY, "broadsword_wing", FleetMemberType.FIGHTER_WING, false);
//		api.addToFleet(FleetSide.ENEMY, "broadsword_wing", FleetMemberType.FIGHTER_WING, false);
//		api.addToFleet(FleetSide.ENEMY, "piranha_wing", FleetMemberType.FIGHTER_WING, false);
//		api.addToFleet(FleetSide.ENEMY, "piranha_wing", FleetMemberType.FIGHTER_WING, false);
//		api.addToFleet(FleetSide.ENEMY, "piranha_wing", FleetMemberType.FIGHTER_WING, false);
//		api.addToFleet(FleetSide.ENEMY, "piranha_wing", FleetMemberType.FIGHTER_WING, false);
		
		
		// Set up the map.
		float width = 24000f;
		float height = 18000f;
		api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);
		
		float minX = -width/2;
		float minY = -height/2;
		
		for (int i = 0; i < 15; i++) {
			float x = (float) Math.random() * width - width/2;
			float y = (float) Math.random() * height - height/2;
			float radius = 100f + (float) Math.random() * 900f; 
			api.addNebula(x, y, radius);
		}
		
		api.addNebula(minX + width * 0.8f - 1000, minY + height * 0.4f, 2000);
		api.addNebula(minX + width * 0.8f - 1000, minY + height * 0.5f, 2000);
		api.addNebula(minX + width * 0.8f - 1000, minY + height * 0.6f, 2000);
		
		api.addObjective(minX + width * 0.8f - 1000, minY + height * 0.4f, "nav_buoy");
		api.addObjective(minX + width * 0.8f - 1000, minY + height * 0.6f, "nav_buoy");
		api.addObjective(minX + width * 0.3f + 1000, minY + height * 0.3f, "comm_relay");
		api.addObjective(minX + width * 0.3f + 1000, minY + height * 0.7f, "comm_relay");
		api.addObjective(minX + width * 0.5f, minY + height * 0.5f, "sensor_array");
		api.addObjective(minX + width * 0.2f + 1000, minY + height * 0.5f, "sensor_array");
		
		// Add an asteroid field
		api.addAsteroidField(minX + width * 0.15f, minY, 85, width * 0.3f, 20f, 70f, 50);
	
		// Add some planets.  These are defined in data/config/planets.json.
		api.addPlanet(-1000f, 0, 200f, "toxic", 350f, true);
		api.addPlanet(-256, -512, 32f, "star_yellow", 1f, true);
	}

}






