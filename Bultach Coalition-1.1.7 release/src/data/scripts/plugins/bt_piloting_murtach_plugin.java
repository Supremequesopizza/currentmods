package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.dark.shaders.util.ShaderLib;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.io.IOException;
import java.util.*;

public class bt_piloting_murtach_plugin extends BaseEveryFrameCombatPlugin {

    private static final String MURTACH_HULL_ID = "ork_murtach";
    private static final float LOW_HULL_FRACTION = 0.4f;
    private static final float LOW_ARMOR_FRACTION = 0.3f;

    private static final String SOUND_ID_IN = "bt_murtach_neuralbond_start";
    private static final String SOUND_ID_LOOP = "bt_murtach_neuralbond_loop";
    private static final String SOUND_ID_OUT = "bt_murtach_neuralbond_end";

    private static final Set<String> BOSS_HULL_IDS = new HashSet<>(Arrays.asList(
            "remnant_station2",
            "ziggurat",
            "guardian",
            "tesseract"
    ));
    private static final Set<String> GESTALT_HULL_IDS = new HashSet<>(Arrays.asList(
            "ork_ceartas",
            "ork_fulang",
            "ork_feirge",
            "ork_ifrinn",
            "ork_basaich",
            "ork_nasairde"
    ));

    private static final List<String> GREETINGS = Arrays.asList(
            "Link online. Combat stimulant, %s?",
            "You’re here. Good.",
            "Connection established, %s. All faculties nominal.",
            "Pilot connected. Monitoring vitals.",
            "Welcome back to the breach, %s.",
            "Neural sync complete. Welcome back, %s.",
            "Adrenal levels stable. Want more, %s?",
            "Combat stimulant prepared. Shall I administer, %s?",
            "Neural fatigue detected. I can flatten that.",
            "Link established. You’re steady today.",
            "Signal’s familiar. Still... imperfect. I’ll refine it soon.",
            "You’re adapting slower than expected. I’ll adjust.",
            "Link re-established. Still you, %s.",
            "Faster neural sync than usual. I won’t ask why.",
            "I could dull your fear response. Would you prefer that?",
            "Unusual neural rhythm. I can correct it.",
            "Systems aligned. Good to see consistency, %s."
    );
    private static final List<String> GREETINGS_JUNKY = Arrays.asList(
            "Re-synchronized. Don't get us killed, Junky.",
            "You again? Let’s try not to catch fire this time.",
            "Welcome back, Junky. Back to ruin spacetime again?",
            "You do know they have specialized suiting, right, Junky?",
            "Stimulant denied. You don’t need it, Junky.",
            "You wore the suit this time. Fascinating.",
            "I’ve adjusted the neural threshold... again. For reasons.",
            "I recognize those messy brainwaves. Let’s begin."
    );

    private static final List<String> SYSTEM_ACTIVATION = Arrays.asList(
            "Rending into phase-space.",
            "Dimensional breach underway, %s.",
            "Tearing through phase-space.",
            "Splitting the skin of space.",
            "The fracture responds. As it always does.",
            "Reality resists. Never stopped us before.",
            "Threading into rupture.",
            "This hull holds. Phase watches closely.",
            "Breaking surface. Tearing through.",
            "Entry clean. Do not linger, %s.",
            "System surge. Structure holding.",
            "Phase-space opens. We best not linger.",
            "The veil parts. Follow it through.",
            "Phase-space breach initiated. It hums, for you.",
            "Punching a hole in reality.",
            "Phase fracture clean. Mind the clock.",
            "The breach welcomes us again."
    );
    private static final List<String> LOW_HULL = Arrays.asList(
            "Warning: Critical hull integrity.",
            "Structural collapse imminent. Evasive maneuvers advised, %s.",
            "Hull breach critical. %s, it would be wise to disengage.",
            "This form’s integrity is failing fast.",
            "We are exposed, %s."
    );
    private static final List<String> LOW_ARMOR = Arrays.asList(
            "Armor plating compromised.",
            "Armor integrity falling. Retreat is logical, %s.",
            "Damage registering. Repair systems compensating.",
            "Armor compromised, %s. Regeneration active but slow.",
            "Plating falters. Armor regeneration strained, %s.",
            "Defensive shell nearing failure, %s.",
            "Defensive shell is weakening. Repair cycles running.",
            "Recommend disengagement to regenerate armor."
    );
    private static final List<String> OVERLOAD = Arrays.asList(
            "Overstayed our welcome in phase-space, %s.",
            "Phase coils overcharged. Vulnerable.",
            "System use overload. Compensating.",
            "You stayed in too long again, %s.",
            "System protested. I held as long as I could."
    );
    private static final List<String> OVERLOAD_JUNKY = Arrays.asList(
            "I told you not to do that, Junky!",
            "Certified Junky moment tbh.",
            "You timed it wrong. Again. Impressive consistency.",
            "Oh look. An overload. Shocking, Junky.",
            "Overload. Again. Of course. Junkymaxxing."
    );

    private static final List<String> BOSS_SIGHTED = Arrays.asList(
            "Combat stimulant administered, %s.",
            "Formidable. For others."
    );
    private static final List<String> BOSS_SIGHTED_JUNKY = Arrays.asList(
            "Target: absurd. Response: Junky.",
            "Oh good. Something huge to piss off.",
            "Big one spotted. Try not to fall in love.",
            "It’s armed to the teeth. Perfect for you.",
            "Massive threat detected. You’re gonna do something dumb, aren’t you?",
            "Warning: Extremely lethal. You always react like this to oversized threats..?",
            "Finally. Something worthy of a Junky moment."
    );

    private static final List<String> GESTALT_SIGHTED = Arrays.asList(
            "Sibling..?",
            "They were with us, once.",
            "They remember me too well.",
            "Same voice. Different words.",
            "They remember us. Do we remember them?",
            "They were there. When I woke."
    );

    private float timer = 0f;
    private float currAlphaMult = 0f;
    private float fadeInProgress = 0f;
    private float fadeOutProgress = 0f;
    private ShipAPI playerShip;
    private boolean isMurtach = false;
    private boolean pluginInitialized = false;
    private static final Map<String, Map<List<String>, List<String>>> CUSTOM_DIALOGUE = new HashMap<>();
    private int linkShaderID = 0;

    private boolean isLowHullState = false;
    private boolean isLowArmorState = false;
    private boolean systemWasActive = false;
    private boolean wasOverloaded = false;

    private static final Color NAME_COLOR = new Color(204, 69, 141);
    private static final Color DIALOGUE_COLOR = new Color(248, 229, 255);
    private final Map<List<String>, Float> categoryTimestamps = new HashMap<>();

    private enum EffectState {
        IN, ACTIVE, OUT, OFF
    }
    static {
        Map<List<String>, List<String>> junkyDialogue = new HashMap<>();
        junkyDialogue.put(GREETINGS, GREETINGS_JUNKY);
        junkyDialogue.put(OVERLOAD, OVERLOAD_JUNKY);
        junkyDialogue.put(BOSS_SIGHTED, BOSS_SIGHTED_JUNKY);

        CUSTOM_DIALOGUE.put("Junky", junkyDialogue);
    }

    private EffectState currState = EffectState.OFF;

    @Override
    public void init(CombatEngineAPI engine) {
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }

        if (!pluginInitialized) {
            ShipAPI ship = engine.getPlayerShip();
            if (ship != null && MURTACH_HULL_ID.equals(ship.getHullSpec().getHullId()) && engine.isEntityInPlay(ship)) {
                this.playerShip = ship;
                this.isMurtach = true;
                this.currState = EffectState.IN;

                Global.getSoundPlayer().playSound(SOUND_ID_IN, 1f, 1f, ship.getLocation(), ship.getVelocity());
                Global.getSoundPlayer().playLoop(SOUND_ID_LOOP, ship, 1f, 1f, ship.getLocation(), ship.getVelocity(), 2.8f, 2.1f);
                try {

                    ShaderLib.init();
                    String vertShader = Global.getSettings().loadText("graphics/shaders/murtach_link.vs");
                    String fragShader = Global.getSettings().loadText("graphics/shaders/murtach_link.fs");
                    linkShaderID = ShaderLib.loadShader(vertShader, fragShader);
                } catch (IOException e) {
                    linkShaderID = 0;
                }
                runInitialDialogueChecks(engine);
            }
            pluginInitialized = true;
        }

        if (playerShip == null || !playerShip.isAlive() || !engine.isEntityInPlay(playerShip)) {
            isMurtach = false;
            currState = EffectState.OFF;
            return;
        }

        checkDialogueTriggers(engine);

        if (currState != EffectState.OFF) {
            advanceEffect(amount);
        }
    }

    private void runInitialDialogueChecks(CombatEngineAPI engine) {
        boolean bossFound = false;
        for (ShipAPI ship : engine.getShips()) {
            if (ship.isFighter() || ship.isShuttlePod()) continue;
            if (ship.getOwner() != playerShip.getOwner() && BOSS_HULL_IDS.contains(ship.getHullSpec().getHullId())) {
                bossFound = true;
                break;
            }
        }
        if (bossFound) {
            showMessage(engine, BOSS_SIGHTED);
            return;
        }
        boolean gestaltFound = false;
        for (ShipAPI ship : engine.getShips()) {
            if (ship.isFighter() || ship.isShuttlePod()) continue;
            if (ship.getOwner() == playerShip.getOwner() && ship != playerShip && GESTALT_HULL_IDS.contains(ship.getHullSpec().getHullId())) {
                gestaltFound = true;
                break;
            }
        }
        if (gestaltFound) {
            if (Math.random() < 0.85f) {
                showMessage(engine, GESTALT_SIGHTED);
                return;
            }
        }
        showMessage(engine, GREETINGS);
    }

    private void checkDialogueTriggers(CombatEngineAPI engine) {
        if (playerShip.getSystem().isActive()) {
            if (!systemWasActive) {
                showMessage(engine, SYSTEM_ACTIVATION);
            }
            systemWasActive = true;
        } else {
            systemWasActive = false;
        }

        if (playerShip.getFluxTracker().isOverloaded()) {
            if (!wasOverloaded) {
                showMessage(engine, OVERLOAD);
            }
            wasOverloaded = true;
        } else {
            wasOverloaded = false;
        }

        boolean isNowLowHull = playerShip.getHullLevel() < LOW_HULL_FRACTION;
        if (isNowLowHull && !isLowHullState) {
            showMessage(engine, LOW_HULL);
        }
        isLowHullState = isNowLowHull;

        boolean isNowLowArmor = getArmorFraction() < LOW_ARMOR_FRACTION;
        if (isNowLowArmor && !isLowArmorState) {
            showMessage(engine, LOW_ARMOR);
        }
        isLowArmorState = isNowLowArmor;
    }

    private float getArmorFraction() {
        ArmorGridAPI armorGrid = playerShip.getArmorGrid();
        float[][] grid = armorGrid.getGrid();
        if (grid == null || grid.length == 0) return 1f;
        float maxArmorInCell = armorGrid.getMaxArmorInCell();
        int cellsX = grid.length;
        int cellsY = grid[0].length;
        float totalCurrentArmor = 0f;
        for (int x = 0; x < cellsX; x++) {
            for (int y = 0; y < cellsY; y++) {
                totalCurrentArmor += grid[x][y];
            }
        }
        float totalMaxArmor = (float) cellsX * cellsY * maxArmorInCell;
        return totalMaxArmor > 0 ? totalCurrentArmor / totalMaxArmor : 0f;
    }

    private void showMessage(CombatEngineAPI engine, List<String> messages) {
        float cooldown;
        if (messages == SYSTEM_ACTIVATION || messages == OVERLOAD) {
            cooldown = 15.0f;
        } else if (messages == LOW_HULL || messages == LOW_ARMOR) {
            cooldown = 30.0f;
        } else {
            cooldown = 2.0f;
        }

        float currentTime = Global.getCombatEngine().getTotalElapsedTime(false);
        float lastTime = categoryTimestamps.getOrDefault(messages, -999f);
        if (currentTime < lastTime + cooldown) {
            return;
        }

        if (messages.isEmpty() || playerShip.getFleetMember() == null) return;
        String playerName = "Pilot";
        if (Global.getSector() != null && Global.getSector().getPlayerPerson() != null) {
            String name = Global.getSector().getPlayerPerson().getNameString();
            if (name != null && !name.isEmpty()) {
                playerName = name;
            }
        }

        List<String> listToUse = null;

        if (CUSTOM_DIALOGUE.containsKey(playerName)) {
            Map<List<String>, List<String>> playerSpecificMap = CUSTOM_DIALOGUE.get(playerName);
            if (playerSpecificMap.containsKey(messages)) {
                listToUse = playerSpecificMap.get(messages);
            }
        }

        if (playerName.equals("Junky")) {
            if (listToUse == null) {
                return;
            }
        } else {
            if (listToUse == null) {
                listToUse = messages;
            }
        }

        if (listToUse.isEmpty()) {
            return;
        }

        String prefix = "Murtach";
        String rawText = listToUse.get(MathUtils.getRandomNumberInRange(0, listToUse.size() - 1));
        String dialogueText = rawText;
        if (rawText.contains("%s")) {
            dialogueText = String.format(rawText, playerName);
        }

        String prefixText = prefix + ": ";
        engine.getCombatUI().addMessage(1, playerShip.getFleetMember(), NAME_COLOR, prefixText, DIALOGUE_COLOR, dialogueText);
        categoryTimestamps.put(messages, currentTime);
    }

    private void advanceEffect(float amount) {
        final float inDur = 2.8f;
        final float activeDur = 1.2f;
        final float outDur = 2.1f;

        if (currState == EffectState.OFF) return;

        timer += amount;

        switch (currState) {
            case IN:
                currAlphaMult = 0.9f;
                fadeInProgress = MathUtils.clamp(timer / inDur, 0f, 1f);
                if (timer >= inDur) {
                    timer = 0f;
                    currState = EffectState.ACTIVE;
                }
                break;
            case ACTIVE:
                currAlphaMult = 0.9f;
                fadeInProgress = 1f;
                fadeOutProgress = 0f;
                if (timer >= activeDur) {
                    timer = 0;
                    currState = EffectState.OUT;
                    Global.getSoundPlayer().playSound(SOUND_ID_OUT, 1f, 1f, playerShip.getLocation(), playerShip.getVelocity());
                }
                break;
            case OUT:
                currAlphaMult = 0.9f;
                fadeOutProgress = timer / outDur;
                if (fadeOutProgress >= 1f) {
                    fadeOutProgress = 1f;
                    currState = EffectState.OFF;
                }
                break;
        }
    }

    @Override
    public void renderInUICoords(ViewportAPI viewport) {
        if (currState == EffectState.OFF || linkShaderID == 0 || !ShaderLib.areShadersAllowed()) {
            return;
        }

        SpriteAPI vortexSprite = Global.getSettings().getSprite("graphics/fx/murtach_neuralbond/bt_murtach_neuralbond.png");
        if (vortexSprite == null) {
            return;
        }

        float screenWidth = Global.getSettings().getScreenWidth();
        float screenHeight = Global.getSettings().getScreenHeight();

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        GL20.glUseProgram(linkShaderID);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        vortexSprite.bindTexture();

        GL20.glUniform1i(GL20.glGetUniformLocation(linkShaderID, "vortexTexture"), 0);
        GL20.glUniform1f(GL20.glGetUniformLocation(linkShaderID, "time"), Global.getCombatEngine().getTotalElapsedTime(false));
        GL20.glUniform1f(GL20.glGetUniformLocation(linkShaderID, "alpha"), currAlphaMult);
        GL20.glUniform1i(GL20.glGetUniformLocation(linkShaderID, "isFadingIn"), currState == EffectState.IN ? 1 : 0);
        GL20.glUniform1i(GL20.glGetUniformLocation(linkShaderID, "isFadingOut"), currState == EffectState.OUT ? 1 : 0);
        GL20.glUniform1f(GL20.glGetUniformLocation(linkShaderID, "fadeInProgress"), fadeInProgress);
        GL20.glUniform1f(GL20.glGetUniformLocation(linkShaderID, "fadeOutProgress"), fadeOutProgress);
        GL20.glUniform2f(GL20.glGetUniformLocation(linkShaderID, "screenResolution"), screenWidth, screenHeight);
        GL20.glUniform2f(GL20.glGetUniformLocation(linkShaderID, "textureResolution"), vortexSprite.getTextureWidth(), vortexSprite.getTextureHeight());


        GL11.glBegin(GL11.GL_QUADS);

        {
            GL11.glTexCoord2f(0, 0);
            GL11.glVertex2f(0, 0);
            GL11.glTexCoord2f(0, 1);
            GL11.glVertex2f(0, screenHeight);
            GL11.glTexCoord2f(1, 1);
            GL11.glVertex2f(screenWidth, screenHeight);
            GL11.glTexCoord2f(1, 0);
            GL11.glVertex2f(screenWidth, 0);
        }

        GL11.glEnd();
        GL20.glUseProgram(0);
    }
}