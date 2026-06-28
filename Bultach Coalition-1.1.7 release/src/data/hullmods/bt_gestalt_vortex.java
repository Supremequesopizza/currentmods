package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.Color;

public class bt_gestalt_vortex extends BaseHullMod {

	private static final String SPRITE_ID = "bt_vortex_swirl";
	private static final String SPRITE_CATEGORY = "fx";
	private static final Vector2f SPRITE_SIZE = new Vector2f(100f, 100f);
	private static final float TOTAL_ACTIVE_DURATION_SECONDS = 5.0f;
	private static final float FADE_IN_SECONDS = 0.5f;
	private static final float FADE_OUT_SECONDS = 1.5f;
	private static final float SPIN_SPEED_DPS = 75f;
	private static final String DATA_KEY_PREFIX = "bt_gestalt_vortex_";

	private enum VortexState {
		FADING_IN,
		ACTIVE,
		FADING_OUT,
		DONE
	}

	private static class DroneVisualData {
		float currentAngle = MathUtils.getRandomNumberInRange(0f, 360f);
		float currentAlpha = 0f;
		float lifetimeCounter = 0f;
		VortexState state = VortexState.FADING_IN;
		transient SpriteAPI sprite;

		DroneVisualData() {}
	}

	private SpriteAPI getSprite() {
		try {
			return Global.getSettings().getSprite(SPRITE_CATEGORY, SPRITE_ID);
		} catch (Exception e) {
			try {
				return Global.getSettings().getSprite("graphics/fx/" + SPRITE_ID + ".png");
			} catch (Exception e2){
				Global.getLogger(bt_gestalt_vortex.class).error("Failed to load sprite: " + SPRITE_ID, e2);
				return null;
			}
		}
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		if (ship.getShipAI() != null) {
			ship.getShipAI().forceCircumstanceEvaluation();
		}
		String dataKey = DATA_KEY_PREFIX + ship.getId();
		if (ship.getCustomData().get(dataKey) == null) {
			ship.setCustomData(dataKey, new DroneVisualData());
		}
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine.isPaused() || !ship.isAlive()) {
			return;
		}

		MagicRender.battlespace(
				Global.getSettings().getSprite("fx", "bt_cleave_aura"),
				ship.getLocation(), new Vector2f(), new Vector2f(0, 0), new Vector2f(140, 140),
				360 * (float) Math.random(), 0, new Color(255, 227, 165, 15), false, 0.1f, 0.4f, 0.1f

		);

		String dataKey = DATA_KEY_PREFIX + ship.getId();
		DroneVisualData visualData = (DroneVisualData) ship.getCustomData().get(dataKey);
		if (visualData == null) {
			visualData = new DroneVisualData();
			ship.setCustomData(dataKey, visualData);
		}

		if (visualData.state == VortexState.DONE) {
			return;
		}

		if (visualData.sprite == null) {
			visualData.sprite = getSprite();
			if (visualData.sprite != null) {
			} else {
				if(ship.getCustomData().get(dataKey + "_logflag_sprite") == null){
					Global.getLogger(bt_gestalt_vortex.class).warn("Sprite is null for vortex drone: " + ship.getHullSpec().getHullId() + ". Sprite ID: " + SPRITE_ID);
					ship.setCustomData(dataKey + "_logflag_sprite", true);
				}
			}
		}

		visualData.lifetimeCounter += amount;
		visualData.currentAngle += SPIN_SPEED_DPS * amount;
		visualData.currentAngle = MathUtils.clampAngle(visualData.currentAngle);

		switch (visualData.state) {
			case FADING_IN:
				if (visualData.lifetimeCounter >= FADE_IN_SECONDS) {
					visualData.currentAlpha = 1f;
					visualData.lifetimeCounter = 0f;
					visualData.state = VortexState.ACTIVE;
				} else {
					visualData.currentAlpha = visualData.lifetimeCounter / FADE_IN_SECONDS;
				}
				break;
			case ACTIVE:
				visualData.currentAlpha = 1f;
				if (visualData.lifetimeCounter >= TOTAL_ACTIVE_DURATION_SECONDS) {
					visualData.lifetimeCounter = 0f;
					visualData.state = VortexState.FADING_OUT;
				}
				break;
			case FADING_OUT:
				if (visualData.lifetimeCounter >= FADE_OUT_SECONDS) {
					visualData.currentAlpha = 0f;
					visualData.state = VortexState.DONE;
					if (ship.isAlive()) {
						engine.removeEntity(ship);
					}
					return;
				} else {
					visualData.currentAlpha = 1f - (visualData.lifetimeCounter / FADE_OUT_SECONDS);
				}
				break;
		}
		visualData.currentAlpha = MathUtils.clamp(visualData.currentAlpha, 0f, 1f);

		if (Global.getSettings().getModManager().isModEnabled("MagicLib")) {
			if (visualData.sprite != null && visualData.currentAlpha > 0) {
				MagicRender.battlespace(
						visualData.sprite,
						ship.getLocation(),
						new Vector2f(0, 0),
						SPRITE_SIZE,
						new Vector2f(0, 0),
						visualData.currentAngle,
						0f,
						new Color(1f, 1f, 1f, visualData.currentAlpha),
						true,
						0.5f,
						0.05f,
						0f,
						0f,
						0f,
						0f,
						0f,
						0.05f,
						CombatEngineLayers.ABOVE_SHIPS_LAYER
				);
			}

		}

		ship.getVelocity().set(0,0);
		ship.setAngularVelocity(0);
	}

	@Override
	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return "" + (int) (FADE_IN_SECONDS + TOTAL_ACTIVE_DURATION_SECONDS + FADE_OUT_SECONDS) + "s";
		return null;
	}

	@Override
	public boolean affectsOPCosts() {
		return false;
	}
}