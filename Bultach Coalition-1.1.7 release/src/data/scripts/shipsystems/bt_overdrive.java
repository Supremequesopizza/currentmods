package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class bt_overdrive extends BaseShipSystemScript {

    float

        speedMulti = (float) 1.5,
        ballisticRoFMulti = 1.5f,
        ballisticFluxMulti = 1 / ballisticRoFMulti,
        CR_LOSS_MULT = 3.5f;

    private static final float FLUX_DISSIPATION_MULT = 2f;
    private static final float RANGE_THRESHOLD = 500f;
    private static final float RANGE_MULT = 0.25f;


    IntervalUtil arcTimer = new IntervalUtil(0.15f,0.3f); //for charge up

    Color arcFringe = new Color(255, 15, 29, 194);
    Color arcCore = new Color(255, 213, 213, 184);

    boolean doOnce = true;
    private float totalPeakTimeLoss = 0f;


    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }


        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        switch (state){
            case IN:
            case ACTIVE:

                totalPeakTimeLoss += (CR_LOSS_MULT - 1f) * amount;
                stats.getPeakCRDuration().modifyFlat(id, -totalPeakTimeLoss / ship.getMutableStats().getPeakCRDuration().getMult());

                stats.getWeaponRangeThreshold().modifyFlat(id, RANGE_THRESHOLD);
                stats.getWeaponRangeMultPastThreshold().modifyMult(id, RANGE_MULT * effectLevel);

                stats.getMaxSpeed().modifyMult("bt_overdrive",speedMulti);
                stats.getAcceleration().modifyMult("bt_overdrive",speedMulti);
                stats.getDeceleration().modifyMult("bt_overdrive",speedMulti);

                stats.getMaxTurnRate().modifyMult("bt_overdrive", (float) (speedMulti * 1.4));
                stats.getTurnAcceleration().modifyMult("bt_overdrive", (float) (speedMulti * 1.4));

                stats.getBallisticRoFMult().modifyMult("bt_overdrive",ballisticRoFMulti);
                stats.getBallisticWeaponFluxCostMod().modifyMult("bt_overdrive",ballisticFluxMulti);

                stats.getZeroFluxMinimumFluxLevel().modifyFlat(id, 2f);
                stats.getFluxDissipation().modifyMult(id, FLUX_DISSIPATION_MULT);
                stats.getVentRateMult().modifyMult(id, 0f);

                arcTimer.advance(amount);
                if (arcTimer.intervalElapsed()){
                    arcOnBounds(ship, Global.getCombatEngine());
                }
                break;
            case OUT:
                stats.getArmorDamageTakenMult().unmodify("bt_overdrive");
                stats.getHullDamageTakenMult().unmodify("bt_overdrive");
                stats.getShieldDamageTakenMult().unmodify("bt_overdrive");

                stats.getMaxSpeed().unmodify("bt_overdrive");
                stats.getAcceleration().unmodify("bt_overdrive");
                stats.getDeceleration().unmodify("bt_overdrive");

                stats.getMaxTurnRate().unmodify("bt_overdrive");
                stats.getTurnAcceleration().unmodify("bt_overdrive");

                stats.getBallisticRoFMult().unmodify("bt_overdrive");
                stats.getBallisticWeaponFluxCostMod().unmodify("bt_overdrive");

                stats.getZeroFluxMinimumFluxLevel().unmodify("bt_overdrive");
                stats.getFluxDissipation().unmodify("bt_overdrive");
                stats.getVentRateMult().unmodify("bt_overdrive");
                stats.getWeaponRangeThreshold().unmodify("bt_overdrive");
                stats.getWeaponRangeMultPastThreshold().unmodify("bt_overdrive");

                stats.getZeroFluxMinimumFluxLevel().unmodify(id);
                stats.getFluxDissipation().unmodify(id);
                stats.getVentRateMult().unmodify(id);
                stats.getWeaponRangeThreshold().unmodify(id);
                stats.getWeaponRangeMultPastThreshold().unmodify(id);

                if (doOnce){
                }
        }
    }

    public void arcOnBounds(ShipAPI target, CombatEngineAPI engine){
        target.getExactBounds().update(target.getLocation(), target.getFacing());
        List<BoundsAPI.SegmentAPI> Segments = target.getExactBounds().getSegments();
        int firstBound = MathUtils.getRandomNumberInRange(0,Segments.size() - 1);
        int secondBound = firstBound + 2;
        if (secondBound >= Segments.size() - 1) secondBound -= Segments.size() - 1;
        Vector2f firstBoundLoc = Segments.get(firstBound).getP1();
        Vector2f secondBoundLoc = Segments.get(secondBound).getP1();
        engine.spawnEmpArcVisual(firstBoundLoc,target,secondBoundLoc,target,4,arcFringe,arcCore);
        //return new Vector2f((firstBoundLoc.x + secondBoundLoc.x) * 0.5f, (firstBoundLoc.y + secondBoundLoc.y) * 0.5f);
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("Reactor Holding", true);
        }
        return null;
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        doOnce = true;

        stats.getMaxSpeed().unmodify("bt_overdrive");
        stats.getAcceleration().unmodify("bt_overdrive");
        stats.getDeceleration().unmodify("bt_overdrive");
        stats.getMaxTurnRate().unmodify("bt_overdrive");
        stats.getTurnAcceleration().unmodify("bt_overdrive");
        stats.getBallisticRoFMult().unmodify("bt_overdrive");
        stats.getBallisticWeaponFluxCostMod().unmodify("bt_overdrive");
        stats.getZeroFluxMinimumFluxLevel().unmodify(id);
        stats.getFluxDissipation().unmodify(id);
        stats.getVentRateMult().unmodify(id);
        stats.getWeaponRangeThreshold().unmodify(id);
        stats.getWeaponRangeMultPastThreshold().unmodify(id);

    }


}
