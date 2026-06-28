package data.missions.supportshipspack1;

import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

public class MissionDefinition implements MissionDefinitionPlugin {

	public void defineMission(MissionDefinitionAPI api) {

		// Set up the fleets so we can add ships and fighter wings to them.
		// In this scenario, the fleets are attacking each other, but
		// in other scenarios, a fleet may be defending or trying to escape
		api.initFleet(FleetSide.PLAYER, "", FleetGoal.ATTACK, false);
		api.initFleet(FleetSide.ENEMY, "", FleetGoal.ATTACK, true);

//		api.getDefaultCommander(FleetSide.PLAYER).getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 3);
//		api.getDefaultCommander(FleetSide.PLAYER).getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 3);
		
		// Set a small blurb for each fleet that shows up on the mission detail and
		// mission results screens to identify each side.
		api.setFleetTagline(FleetSide.PLAYER, "Scraped together local forces");
		api.setFleetTagline(FleetSide.ENEMY, "A small pirate raid force");
		
		// These show up as items in the bulleted list under 
		// "Tactical Objectives" on the mission detail screen
		api.addBriefingItem("Protect your home from a pirate raid");
		api.addBriefingItem("Your flagship is the only real military ship you have");
		
		// Set up the player's fleet.  Variant names come from the
		// files in data/variants and data/variants/fighters
		//api.addToFleet(FleetSide.PLAYER, "station_small_Standard", FleetMemberType.SHIP, "Test Station", false);
		api.addToFleet(FleetSide.PLAYER, "supportships_lammergeier_Balanced", FleetMemberType.SHIP, true);
                api.addToFleet(FleetSide.PLAYER, "supportships_tercio_Shielded", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "supportships_nebulae_Support", FleetMemberType.SHIP, false);
                api.addToFleet(FleetSide.PLAYER, "supportships_maunder_Balanced", FleetMemberType.SHIP, false);
                api.addToFleet(FleetSide.PLAYER, "supportships_gardener_Standard", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "supportships_carton_Escort", FleetMemberType.SHIP, false);
				api.addToFleet(FleetSide.PLAYER, "supportships_carton_Strike", FleetMemberType.SHIP, false);
				

		
		// Set up the enemy fleet.
		api.addToFleet(FleetSide.ENEMY, "supportships_aspirant_Ranged", FleetMemberType.SHIP,  false);
		api.addToFleet(FleetSide.ENEMY, "colossus3_Pirate", FleetMemberType.SHIP,  false);
		api.addToFleet(FleetSide.ENEMY, "shrike_p_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "shrike_p_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "supportships_maunderp_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "supportships_maunderp_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "supportships_cartonp_Barrage", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "supportships_cartonp_Barrage", FleetMemberType.SHIP, false);

		
		// Set up the map.
		float width = 12000f;
		float height = 12000f;
		api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);
		
		float minX = -width/2;
		float minY = -height/2;
		
		// Add an asteroid field
		api.addAsteroidField(minX, minY + height / 2, 0, 8000f,20f, 70f, 100);
		
		api.addPlanet(0, 0, 50f, StarTypes.YELLOW, 250f, true);
		
	}


}