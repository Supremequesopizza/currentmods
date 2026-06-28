package data.scripts.world.systems.haven;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

public class Mausoleum {

	public void generate(SectorAPI sector) {
		StarSystemAPI system = sector.createStarSystem("Mausoleum");
		
		system.setBackgroundTextureFilename("graphics/backgrounds/anvil_background_exalted.png");

		PlanetAPI mausoleum_star = system.initStar("mausoleum", // unique id for this star 
				"star_yellow", // Star type. Vanilla star types can be found in starsector-core/data/campaign/procgen/star_gen_data.csv and starsector-core/data/config/planets.json
										    700f, 		  //gfbdfgb radius (in pixels at default zoom)
										    350); // corona radius, from star edge
		
		system.setLightColor(new Color(255,255,181)); // light color in entire system, affects all entities

		PlanetAPI tomb = system.addPlanet("anvil_tomb", mausoleum_star, "Tomb", "arid", 180, 130, 2000, 50);
		tomb.setCustomDescriptionId("anvil_tomb");
		tomb.setInteractionImage("illustrations", "anvil_tomb");

	
		
		// Mausoleum Inner Jumppoint
		JumpPointAPI jumpPoint2 = Global.getFactory().createJumpPoint("mausoleum_jump", "Mausoleum Inner Jump-point");
		jumpPoint2.setCircularOrbit( system.getEntityById("mausoleum"), 120 + 60, 2700, 80);
		system.addEntity(jumpPoint2);

		
		
		
		PlanetAPI grave = system.addPlanet("anvil_grave", mausoleum_star, "Grave", "lava_minor", 180, 90, 5700, 400);
		grave.setCustomDescriptionId("anvil_grave");
		// Morn accretion cloud

		
		
		SectorEntityToken station1 = system.addCustomEntity("anvil_crypt", "Crypt", "station_side00", "exalted");
		station1.setCustomDescriptionId("anvil_crypt");
		station1.setInteractionImage("illustrations", "orbital");
		station1.setCircularOrbitWithSpin(mausoleum_star, 180+60, 4300, 400, -1f, -3f);
	
		
		SectorEntityToken mausoleum_loc1 = system.addCustomEntity(null, null, "sensor_array_makeshift", "exalted");
		mausoleum_loc1.setCircularOrbitPointingDown(mausoleum_star, 180-120, 6600, 400);

		SectorEntityToken mausoleum_loc2 = system.addCustomEntity(null, null, "comm_relay_makeshift", "exalted");
		mausoleum_loc2.setCircularOrbitPointingDown(mausoleum_star, 180+120, 7300, 400);


		system.addAsteroidBelt(mausoleum_star, // Variable stocking the entity you want the asteroid belt to encircle
				400, // Number of asteroids in the belt
				4300, // Diameter (I think) of the belt, in pixels (I guess, again)
				700, // How thicc the belt is. The asteroids will spread more the higher the value
				460, // Minimum of days required for the asteroid belt to do an entire cycle
				500); // Maximum days the belt will take to do a cycle
		// Adding a ring band to make the belt feel less empty



		float radiusAfter = StarSystemGenerator.addOrbitingEntities(system, mausoleum_star, StarAge.AVERAGE,
				2, 4, // min/max entities to add
				10000, // radius to start adding at
				5, // name offset - next planet will be <system name> <roman numeral of this parameter + 1>
				true); // whether to use custom or system-name based names

		system.autogenerateHyperspaceJumpPoints(true, true);

		//Getting rid of some hyperspace nebula, just in case
		HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
		NebulaEditor editor = new NebulaEditor(plugin);
		float minRadius = plugin.getTileSize() * 2f;

		float radius = system.getMaxRadiusInHyperspace();
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f);
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);
	}

}
