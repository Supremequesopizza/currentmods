package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.magiclib.plugins.MagicTrailPlugin;
import org.magiclib.util.MagicRender;
import data.scripts.utils.bt_divine_cleaver_util;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import org.lwjgl.util.vector.Vector2f;

//By Tartiflette

public class bt_divine_cleaver_everyframe implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {
    
    private boolean runOnce = false;
    
    private final IntervalUtil DRILL_TIMER = new IntervalUtil(0.09f,0.11f);
    private final IntervalUtil CHARGE_TIMER = new IntervalUtil(0.04f,0.06f);
    private Map<DamagingProjectileAPI,List<Float>>drills = new HashMap<>();
    
    private final Map<Integer,SpriteAPI> SPRITES = new HashMap<>();
    {
        SPRITES.put(1, Global.getSettings().getSprite("fx", "bt_cleavetrail_0"));
        SPRITES.put(2, Global.getSettings().getSprite("fx", "bt_cleavetrail_1"));
        SPRITES.put(3, Global.getSettings().getSprite("fx", "bt_cleavetrail_2"));
        SPRITES.put(4, Global.getSettings().getSprite("fx", "bt_cleavetrail_3"));
        SPRITES.put(5, Global.getSettings().getSprite("fx", "bt_cleavetrail_4"));
        SPRITES.put(6, Global.getSettings().getSprite("fx", "bt_cleavetrail_5"));
    }
    private final SpriteAPI TRAIL = Global.getSettings().getSprite("fx", "bt_cleave_trail");
    private final SpriteAPI SHADOW = Global.getSettings().getSprite("fx", "bt_cleave_shadow");
    
    private boolean SHADER=false;
    private boolean chargeSound=false;
    private final String CHARGE="bt_cleaver_charge";
    private final String FIRE="bt_cleaver_fire";
    private final String LOOP="bt_cleaver_loop";
    private final String EXPLOSION="bt_cleaver_explode";
    
    private final int source = GL_SRC_ALPHA;
    private final int screen = GL_ONE;
    private final int normal = GL_ONE_MINUS_SRC_ALPHA;
        
    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if(!drills.containsKey(projectile)){
                    List<Float> ids = new ArrayList<>();
                    ids.add(MagicTrailPlugin.getUniqueID());
                    ids.add(MagicTrailPlugin.getUniqueID());
                    ids.add(MagicTrailPlugin.getUniqueID());
                    ids.add(MagicTrailPlugin.getUniqueID());
                    ids.add(MagicTrailPlugin.getUniqueID());
                    drills.put(projectile,ids);
                    projectile.setCollisionClass(CollisionClass.NONE);
                }
        fireEffect(engine,weapon);
    }
    
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        
        if (engine.isPaused()) {return;}
        
        if (!runOnce){
            runOnce=true;
            SHADER = Global.getSettings().getModManager().isModEnabled("shaderLib");
        }

        if(weapon.isFiring() && weapon.getCooldownRemaining()==0){
            Global.getSoundPlayer().playLoop(CHARGE, weapon.getShip(), 1, 1, weapon.getLocation(), weapon.getShip().getVelocity());
            CHARGE_TIMER.advance(amount);
            if(CHARGE_TIMER.intervalElapsed()){
                chargeEffect(engine,weapon,weapon.getChargeLevel());
            }
        } else if(chargeSound){
            chargeSound=false;
        }
        
        if(!drills.isEmpty()){
            if(weapon.getCooldownRemaining()<0.1f && weapon.getChargeLevel()!=1){
                weapon.setRemainingCooldownTo(0.1f);
            }
            for(DamagingProjectileAPI p : drills.keySet()){
                //SOUND
                Global.getSoundPlayer().playLoop(LOOP, p, 1, 1, p.getLocation(), p.getVelocity());
                if(p.getCollisionClass()!=CollisionClass.NONE){
                    p.setCollisionClass(CollisionClass.NONE);
                }
            }
            DRILL_TIMER.advance(amount);
            if(DRILL_TIMER.intervalElapsed()){
                for(Iterator <DamagingProjectileAPI> iter = drills.keySet().iterator() ; iter.hasNext();){
                    DamagingProjectileAPI p = iter.next();
                    
                    //remove fading missiles and skip
                    if(!engine.isEntityInPlay(p)){
                        iter.remove();
                        continue;
                    }             
                    
                    float facing =VectorUtils.getFacing(p.getVelocity());
                    float opacity = (p.getDamageAmount() / p.getBaseDamageAmount()) * Math.min(1f, p.getElapsed()/2);
                    
                    //PROJECTILES                    
                    for(int i=0; i<amount*240*opacity; i++){

                        Vector2f point = new Vector2f(
                                MathUtils.getRandomNumberInRange(-48, 48),
                                MathUtils.getRandomNumberInRange(-180, 180)                  
                        );
                        point.scale(opacity);

                        VectorUtils.rotate(point, facing);
                        Vector2f.add(point, p.getLocation(), point);

                        engine.spawnProjectile(
                                weapon.getShip(),
                                weapon,
                                "ork_divine_cleaver_sub",
                                point,
                                facing,
                                new Vector2f()
                        );
                    }
                    
                    //TRAIL
                    MagicTrailPlugin.addTrailMemberAdvanced(
                            p,
                            drills.get(p).get(0),
                            TRAIL,
                            p.getLocation(),
                            -50, 
                            0, 
                            facing, 
                            0, 
                            0,
                            156,
                            156,
                            new Color(255, 239, 172, 187),
                            new Color(199, 191, 140, 195),
                            0.13f*opacity,
                            1f, 
                            0f,
                            2.0f,
                            source,
                            screen,
                            2048,
                            -128,
                            new Vector2f(),
                            null,null,1
                    );

                    
                    Vector2f loc= new Vector2f(p.getLocation());
                    Vector2f.add(loc, p.getVelocity(), loc);
                    MagicTrailPlugin.addTrailMemberAdvanced(
                            p,
                            drills.get(p).get(4),
                            SHADOW,
                            loc,
                            -55,
                            0, 
                            facing, 
                            0, 
                            0,
                            125,
                            210,
                            new Color(255, 213, 150, 176),
                            new Color(157, 141, 75, 148),
                            0.2f*opacity,
                            3.0f,
                            0,
                            2.2f,
                            source,
                            normal,
                            2048,
                            -128,
                            new Vector2f(),
                            null,null,1
                    );
                    
                    //SWIRLING TRAIL
                    if(!p.isFading()){
                        for (int i=1; i<4; i++){
                            float direction = 45*(float)FastTrig.sin((MathUtils.FPI*i/3)+p.getElapsed()*i);
                            MagicTrailPlugin.addTrailMemberAdvanced(
                                    p,
                                    drills.get(p).get(i),
                                    Global.getSettings().getSprite("fx", "Trail_Smooth"),
                                    p.getLocation(),
                                    20,
                                    10, 
                                    direction+facing,
                                    0,
                                    0,
                                    32, 
                                    64,
                                    new Color(255, 230, 210, 115),
                                    new Color(255, 234, 147),
                                    0.25f*opacity,
                                    1f, 
                                    0f,
                                    4f,
                                    source,
                                    screen,
                                    2048,
                                    64,
                                    new Vector2f(),
                                    null,null,1
                            );
                        }
                    }
                    
                    if(MagicRender.screenCheck(0.5f, p.getLocation())){

                        //CLOUDS TRAIL
                        if(Math.random()>0.5f){
                            Vector2f aura=new Vector2f(256,256);  
                            aura.scale(MathUtils.getRandomNumberInRange(0.25f, 0.25f+0.5f*opacity));
                            MagicRender.objectspace(
                                        Global.getSettings().getSprite("fx", "bt_cleave_cloud"),
                                        p,
                                        new Vector2f(),
                                        (Vector2f)new Vector2f(p.getVelocity()).scale(MathUtils.getRandomNumberInRange(-1, -0.66f)),
                                        aura,
                                        (Vector2f)(new Vector2f(aura)).scale(MathUtils.getRandomNumberInRange(0.5f, 1f)),
                                        MathUtils.getRandomNumberInRange(0,360),
                                        MathUtils.getRandomNumberInRange(-30, 30),
                                        false,
                                        new Color(
                                                MathUtils.getRandomNumberInRange(0.75f, 1f),
                                                MathUtils.getRandomNumberInRange(0.75f, 1f),
                                                MathUtils.getRandomNumberInRange(0.75f, 1f),
                                                MathUtils.getRandomNumberInRange(0.1f, 0.25f)), 
                                        true, 
                                        MathUtils.getRandomNumberInRange(0.25f,0.5f),
                                        0f, 
                                        MathUtils.getRandomNumberInRange(1f,2f),
                                        true
                                );
                        }

                        //AURA
                        if(Math.random()>0.5){
                            float size=MathUtils.getRandomNumberInRange(128, 196);

                            MagicRender.objectspace(
                                    Global.getSettings().getSprite("fx", "bt_cleave_aura"),
                                    p,
                                    new Vector2f(),
                                    new Vector2f(),
                                    new Vector2f(size,size),
                                    new Vector2f(-size/2,-size/2),
                                    MathUtils.getRandomNumberInRange(0,360),
                                    MathUtils.getRandomNumberInRange(30,120),
                                    false,
                                    new Color(255, 238, 186,64),
                                    false, 
                                    0.5f,
                                    0.25f, 
                                    0.75f,
                                    false
                            );
                        }

                        //SWOOSHES
                        if(Math.random()>0.5){
                            float size=MathUtils.getRandomNumberInRange(64, 128);
                            float angle = MathUtils.getRandomNumberInRange(0,360);
                            Vector2f point=MathUtils.getPoint(new Vector2f(), MathUtils.getRandomNumberInRange(0, size/4), angle);

                            MagicRender.objectspace(
                                    Global.getSettings().getSprite("fx", "bt_cleave_swoosh"),
                                    p,
                                    point,
                                    new Vector2f((Vector2f)point.scale(-0.5f)),
                                    new Vector2f(size,size),
                                    new Vector2f(-size,-size),
                                    angle+MathUtils.getRandomNumberInRange(-90,90),
                                    MathUtils.getRandomNumberInRange(-90,90),
                                    false,
                                    new Color(255, 246, 236, 81),
                                    false, 
                                    0.1f,
                                    0.1f, 
                                    0.25f,
                                    false
                            );

                        }

                        //BROKEN REALITY
                        
                        if(SHADER){
                            if(Math.random()>0.5){
                                float angle = MathUtils.getRandomNumberInRange(0, 360);
                                float dist = MathUtils.getRandomNumberInRange(25, 75);
                                float duration=((dist/20)+MathUtils.getRandomNumberInRange(0,2))*opacity;
                                bt_divine_cleaver_util.CustomBubbleDistortion(
                                        MathUtils.getPoint(p.getLocation(), dist, angle+180),
                                        (Vector2f)new Vector2f(p.getVelocity()).scale(MathUtils.getRandomNumberInRange(0, 0.5f)),
                                        dist + MathUtils.getRandomNumberInRange(50, 100),
                                        MathUtils.getRandomNumberInRange(10, 30)*opacity,
                                        true,
                                        angle+MathUtils.getRandomNumberInRange(-45, 45),
                                        MathUtils.getRandomNumberInRange(45, 120),
                                        10,
                                        0.15f,
                                        0.1f,
                                        duration,
                                        0f,
                                        5
                                );
                            }
                        }

                        //chance to add lightning
                        if(Math.random()<0.44f*opacity){
                            createArc(p,p.getLocation(),20*opacity,-512);
                        }
                    }
                    //explosion
                    if(opacity<0.25f && p.getElapsed()>1){
                        explosion(engine, p, weapon);
                        iter.remove();
                    }
                }
            }
        }
    }
    
    private void chargeEffect(CombatEngineAPI engine, WeaponAPI weapon, float charge){
        if(SHADER){
            bt_divine_cleaver_util.CustomBubbleDistortion(
                    weapon.getLocation(), 
                    weapon.getShip().getVelocity(),
                    (100+MathUtils.getRandomNumberInRange(0, 50))*charge,
                    2+(MathUtils.getRandomNumberInRange(0, 10))*charge,
                    true,
                    0,
                    0,
                    0,
                    0.1f,
                    0.1f,
                    0.5f,
                    0,
                    0
            );

            bt_divine_cleaver_util.customLight(
                    weapon.getLocation(),
                    weapon.getShip(),
                    15+charge*15,
                    0.25f+0.75f*charge,
                    new Color(255, 224, 157, 234),
                    0.75f,
                    0,
                    0.1f+0.1f*charge
            );
        }
        
        engine.addSmoothParticle(
                weapon.getLocation(),
                weapon.getShip().getVelocity(),
                (80+MathUtils.getRandomNumberInRange(0, 20))*charge,
                0.25f,
                MathUtils.getRandomNumberInRange(0.2f, 0.4f),
                new Color(
                MathUtils.getRandomNumberInRange(0.3f, 0.4f),
                MathUtils.getRandomNumberInRange(0.1f, 0.15f),
                MathUtils.getRandomNumberInRange(0.4f, 0.5f)
                )
        );
        
        createArc(weapon.getShip(),weapon.getLocation(),10+charge*10,-512);
    }
    
    private void fireEffect(CombatEngineAPI engine, WeaponAPI weapon){
        if(SHADER){
            bt_divine_cleaver_util.CustomRippleDistortion(
                    weapon.getLocation(), 
                    weapon.getShip().getVelocity(),
                    200,
                    50,
                    false,
                    weapon.getCurrAngle()+180,
                    60,
                    15f,
                    0.25f,
                    1f,
                    0.5f,
                    0,
                    0
            );
            bt_divine_cleaver_util.customLight(
                    weapon.getLocation(),
                    weapon.getShip(),
                    45,
                    0.35f,
                    new Color(255, 244, 219),
                    0.75f,
                    0,
                    1f
            );
        }
        engine.addSmoothParticle(
                weapon.getLocation(),
                weapon.getShip().getVelocity(),
                200,
                2f,
                0.05f,
                Color.WHITE
        );
        engine.addHitParticle(
                weapon.getLocation(),
                weapon.getShip().getVelocity(),
                300,
                0.5f,
                1f,
                new Color(255, 234, 147)
        );
//        for(int i=0; i<5; i++){
//            createArc(weapon.getShip(),weapon.getLocation(),10+i*2,512);
//        }
    }
    
    private void createArc(CombatEntityAPI anchor, Vector2f location, float size, float textSpeed){
        int rand = Math.round(MathUtils.getRandomNumberInRange(size/3, size));
        float dir = MathUtils.getRandomNumberInRange(0, 360);
        float time = MathUtils.getRandomNumberInRange(0.5f, 1.5f);
        Vector2f point = MathUtils.getRandomPointInCircle(location, 50);

        float ID = MagicTrailPlugin.getUniqueID();
        Vector2f vel = new Vector2f(anchor.getVelocity());
        vel.scale(MathUtils.getRandomNumberInRange(0.05f, 0.1f));
        SpriteAPI sprite = SPRITES.get(MathUtils.getRandomNumberInRange(1, SPRITES.size()));

        for(int i=0; i<rand; i++){    
            if(i==0 && SHADER){
                //LIGHT
                bt_divine_cleaver_util.customLight(
                        point,
                        anchor,
                        200,
                        0.25f,
                        Color.WHITE,
                        0,
                        0.1f,
                        time/2
                );
            }
            MagicTrailPlugin.addTrailMemberAdvanced(
                    anchor,
                    ID,
                    sprite,
                    point,
                    MathUtils.getRandomNumberInRange(0, -5), 
                    0, 
                    dir, 
                    0, 
                    0,
                    (0.25f + 0.75f*i/rand) * MathUtils.getRandomNumberInRange(48, 64),
                    (0.25f + 0.75f*i/rand) * MathUtils.getRandomNumberInRange(64, 96),
                    new Color(255, 242, 200, 175),
                    new Color(200, 188,150, 175),
                    (1-0.33f*i/rand) * MathUtils.getRandomNumberInRange(0.66f, 1),
                    0f, 
                    0f,
                    time,
                    source,
                    screen,
                    128,
                    textSpeed,
                    (Vector2f)(new Vector2f(anchor.getVelocity())).scale(1-(i/rand)),
                    null,null,1
            );
            dir=dir+MathUtils.getRandomNumberInRange(-30, 30);
            point=MathUtils.getPoint(point, MathUtils.getRandomNumberInRange(8, 16), dir);
        }
    }
    
    private void explosion(CombatEngineAPI engine, DamagingProjectileAPI proj, WeaponAPI source){       
        
        Global.getSoundPlayer().playSound(EXPLOSION, 1, 1, proj.getLocation(), proj.getVelocity());
        
        engine.spawnExplosion(
                new Vector2f(proj.getLocation()),
                new Vector2f(proj.getVelocity()),
                new Color(255, 239, 195, 231),
                120,
                1f
        );
        
        engine.addHitParticle(
                new Vector2f(proj.getLocation()),
                new Vector2f(proj.getVelocity()),
                120,
                1,
                0.1f,
                Color.WHITE
        );
        
        if(SHADER){
            bt_divine_cleaver_util.customLight(
                    new Vector2f(proj.getLocation()),
                    null,
                    150,
                    1f,
                    Color.YELLOW,
                    0f,
                    0f,
                    1f
            );

            bt_divine_cleaver_util.CustomBubbleDistortion(
                    new Vector2f(proj.getLocation()),
                    new Vector2f(proj.getVelocity()),
                    350,
                    100,
                    false,
                    0,
                    0,
                    0,
                    0f,
                    0f,
                    0.5f,
                    0.5f,
                    0
            );
        }
        
        for(int i=0; i<3; i++){
            MagicRender.battlespace(
                    Global.getSettings().getSprite("fx", "bt_cleave_aura"),
                    new Vector2f(proj.getLocation()),
                    new Vector2f(proj.getVelocity()),
                    new Vector2f(64,64),
                    new Vector2f(256+512*i,256+512*i),
                    MathUtils.getRandomNumberInRange(0,360),
                    MathUtils.getRandomNumberInRange(-15,15),
                    new Color(1,1,1,1-(0.25f*i)), 
                    false, 
                    0.1f*i,
                    0.0f, 
                    0.5f-i/10
            );
        }        
        
        Vector2f vel = (Vector2f) new Vector2f(proj.getVelocity()).scale(0.25f);
        for(int i=0; i<24; i++){
            engine.spawnProjectile(
                    source.getShip(),
                    source,
                    "ork_divine_cleaver_sub",
                    MathUtils.getRandomPointInCircle(proj.getLocation(),50),
                    i*20,
                    vel
            );
        }
        
        for(int i=0; i<5; i++){
            createArc(proj,proj.getLocation(),10+i*2,512);
        }
    }
}