package data.scripts.utils;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;

public class bt_relationship_util implements EveryFrameScript {

    // just another check to lock relations
    private static final String BULTACH_HATER_ID = "orks";
    private static final String CORPORTATE_SCUM_ID = "tritachyon";

    private final float maxReputationAllowed;

    private static final float CHECK_INTERVAL_IN_DAYS = 1.0f;
    private float daysUntilNextCheck;

    public bt_relationship_util(float maxReputationCap) {
        this.maxReputationAllowed = maxReputationCap;
        this.daysUntilNextCheck = CHECK_INTERVAL_IN_DAYS * (float) Math.random(); // stagger initial check
    }

    @Override
    public void advance(float amount) {
        float daysPassed = Global.getSector().getClock().convertToDays(amount);
        daysUntilNextCheck -= daysPassed;

        if (daysUntilNextCheck <= 0f) {
            enforceTheGrudge();
            daysUntilNextCheck = CHECK_INTERVAL_IN_DAYS;
        }
    }

    private void enforceTheGrudge() {
        FactionAPI factionA = Global.getSector().getFaction(BULTACH_HATER_ID);
        FactionAPI factionB = Global.getSector().getFaction(CORPORTATE_SCUM_ID);

        // don't do it if they don't exist
        if (factionA == null || factionB == null) {
            return;
        }

        float currentRep = factionA.getRelationship(factionB.getId());

        if (currentRep > maxReputationAllowed) {
            factionA.setRelationship(factionB.getId(), maxReputationAllowed);
        }
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }
}