package data.scripts.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

public class bt_MurtachLinkRenderer {

    public bt_MurtachLinkRenderer() {

    }

    public void render(SpriteAPI hexSprite, SpriteAPI wipeSprite, float alpha, boolean isFadingOut, float fadeOutProgress) {


        if (hexSprite == null || wipeSprite == null) {
            return;
        }


        float w = Global.getSettings().getScreenWidth();
        float h = Global.getSettings().getScreenHeight();

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);


        GL11.glBegin(GL11.GL_QUADS);
        {
            GL11.glTexCoord2f(0, 0);
            GL11.glVertex2f(0, 0);

            GL11.glTexCoord2f(0, 1);
            GL11.glVertex2f(0, h);

            GL11.glTexCoord2f(1, 1);
            GL11.glVertex2f(w, h);

            GL11.glTexCoord2f(1, 0);
            GL11.glVertex2f(w, 0);
        }
        GL11.glEnd();

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }
}