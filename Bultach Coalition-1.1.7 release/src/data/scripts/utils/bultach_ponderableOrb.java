package data.scripts.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Sphere;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

// this is basically 100% credit to ruddygreat
// thank you ruddy
public class bultach_ponderableOrb
{

    //orb pondering engaged

    private final float baseRadius;
    public float currRadius;
    private SpriteAPI sprite;
    private float rotationSpeed;
    private final int numSlices;
    private final int numStacks;

    private final float pitch;
    private final float tilt;

    private final Sphere sphere = new Sphere();
    private final boolean blend;

    /**
     * @param baseRadius in pixels
     * @param pitch  pitch towards / away from the camera, negative pitches towards the camera
     * @param tilt   tilt left / right, negative goes to the left
     * @param sprite texture used for the sphere
     * @param blend  should the sprite blend? set to false for solid colour
     */
    public bultach_ponderableOrb(float baseRadius, float pitch, float tilt, float rotationSpeed, int numSlices, int numStacks, SpriteAPI sprite, boolean blend) {
        this.baseRadius = baseRadius;
        this.currRadius = baseRadius;
        this.sprite = sprite;
        this.blend = blend;
        this.pitch = pitch;
        this.tilt = tilt;
        this.rotationSpeed = rotationSpeed;
        this.numSlices = numSlices;
        this.numStacks = numStacks;

        sphere.setTextureFlag(true);
        sphere.setDrawStyle(GLU.GLU_FILL);
        sphere.setNormals(GLU.GLU_SMOOTH);
        sphere.setOrientation(GLU.GLU_OUTSIDE);
    }

    /**
     * @param colour colour to render the orb as, defaults to white if null
     */
    public void ponder(Color colour, Vector2f loc) {

        if (colour == null) {
            colour = Color.WHITE;
        }

        boolean texEnabledBefore = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        if (!blend) {
            GL11.glDisable(GL11.GL_BLEND);
        }
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_CULL_FACE);

        GL11.glPushMatrix();
        GL11.glTranslatef(loc.x, loc.y, 0);

        //rotates the sphere

        GL11.glRotatef(tilt, 0, 0, 1);
        GL11.glRotatef(pitch, 1, 0, 0);
        GL11.glRotatef(Global.getCombatEngine().getTotalElapsedTime(true) * rotationSpeed, 0, 1, 0);
        GL11.glColor4ub((byte) colour.getRed(), (byte) colour.getGreen(), (byte) colour.getBlue(), (byte) colour.getAlpha());

        //todo also need to figure out lighting?
        //look at planet again, this seems fairly easy?

        sprite.bindTexture();

        sphere.draw(currRadius, numSlices, numStacks);

        GL11.glPopMatrix();

        //unset caps for safety
        GL11.glDisable(GL11.GL_CULL_FACE);
        if (!texEnabledBefore) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
        }
        if (!blend) {
            GL11.glEnable(GL11.GL_BLEND);
        }
    }

    public int getNumSlices() {
        return numSlices;
    }

    public int getNumStacks() {
        return numStacks;
    }

    public void setRotationSpeed(float rotationSpeed) {
        this.rotationSpeed = rotationSpeed;
    }

    public float getBaseRadius() {
        return baseRadius;
    }

    public void setSprite(SpriteAPI sprite) {
        this.sprite = sprite;
    }

    public SpriteAPI getSprite() {
        return sprite;
    }
}
