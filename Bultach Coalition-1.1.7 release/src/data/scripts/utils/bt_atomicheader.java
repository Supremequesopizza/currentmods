package data.scripts.utils;

import com.fs.starfarer.api.combat.WeaponAPI;
import org.lwjgl.util.vector.Vector2f;

public class bt_atomicheader {

    public static Vector2f calculateHeader(float facing){
        double angle = Math.toRadians(facing);
        Vector2f dir = new Vector2f((float)Math.cos(angle),(float)Math.sin(angle));
        if (dir.lengthSquared() > 0f) dir.normalise();
        return dir;
    }
}
