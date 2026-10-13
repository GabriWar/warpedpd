/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package xyz.gabriwar.warpedpixeldungeon.ui.guide;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.GuidePuppet;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.PointF;
import com.watabou.utils.RectF;

//A creature's live animation cropped to a box, so a giant stays the size of the rest: never
//scaled, every pixel whole. Its footprint is its idle frame's size (clipped to the box) and never
//changes, so what holds it never lays out again mid-strike: a larger frame is cut to it (centred
//across, the cut taken a third from the top), a smaller one sits in it, centred, on its bottom row.
//Every so often the creature acts (GuidePuppet.act); a sprite that cannot live without its
//creature in a level keeps the frame it had.
public class LiveCrop extends Image {

	private final CharSprite src;
	private final int footW, footH;
	private final float actMin, actMax;
	private float actIn;

	//the source frame last cropped, by value: CharSprite.frame() hands out a fresh copy
	private final RectF lastFrame = new RectF( -1, -1, -1, -1 );
	private final RectF crop = new RectF();
	//where the drawn frame sits in the footprint, whole pixels
	private int offX, offY;
	private boolean still = false;

	//the sprite's colour last copied: taken again only when it changes, so a tint laid on the
	//crop itself (a card's reveal) holds
	private final float[] tint = new float[7];

	/** The act timer runs in seconds; 0, 0 never acts on its own. */
	public LiveCrop( CharSprite src, int maxW, int maxH, float actMin, float actMax ){
		super( src );
		this.src = src;
		//Image.copy shares the source's scale: a crop is never scaled
		scale = new PointF( 1, 1 );
		this.actMin = actMin;
		this.actMax = actMax;
		actIn = actMax > 0 ? FxRandom.Float( 1f, 4f ) : 0;

		RectF f = src.frame();
		int fw = f == null || texture == null ? maxW : Math.round( f.width() * texture.width );
		int fh = f == null || texture == null ? maxH : Math.round( f.height() * texture.height );
		footW = Math.max( 1, Math.min( fw, maxW ) );
		footH = Math.max( 1, Math.min( fh, maxH ) );
		cropFrame();
		takeTint( true );
	}

	@Override
	public float width() {
		return footW;
	}

	@Override
	public float height() {
		return footH;
	}

	/** Acts now (a tap). */
	public void act(){
		if (still) return;
		try {
			GuidePuppet.act( src );
		} catch (Exception e){
			still = true;
		}
		if (actMax > 0) actIn = FxRandom.Float( actMin, actMax );
	}

	@Override
	public void update() {
		super.update();
		if (still) return;
		try {
			src.update();
		} catch (Exception e){
			//a sprite that can't live without its creature in a level: keep the frame it has
			still = true;
			return;
		}
		if (actMax > 0 && (actIn -= Game.elapsed) <= 0) act();
		if (texture != src.texture){
			//a variant swapped its pelt
			texture = src.texture;
			lastFrame.set( -1, -1, -1, -1 );
		}
		cropFrame();
		takeTint( false );
	}

	@Override
	public void resetColor() {
		super.resetColor();
		//Visual's constructor resets the colour before this crop has its sprite
		if (src != null) takeTint( true );
	}

	@Override
	protected void updateMatrix() {
		float ox = x, oy = y;
		x += offX;
		y += offY;
		super.updateMatrix();
		x = ox;
		y = oy;
	}

	@Override
	public void destroy() {
		super.destroy();
		try {
			src.destroy();
		} catch (Exception e){
			//it was never on a scene: nothing of it to let go
		}
	}

	private void cropFrame(){
		RectF f = src.frame();
		if (f == null || texture == null) return;
		if (f.left == lastFrame.left && f.top == lastFrame.top
				&& f.right == lastFrame.right && f.bottom == lastFrame.bottom) return;
		lastFrame.set( f.left, f.top, f.right, f.bottom );
		float tw = texture.width, th = texture.height;
		int fw = Math.round( f.width() * tw ), fh = Math.round( f.height() * th );
		crop.set( f.left, f.top, f.right, f.bottom );
		offX = offY = 0;
		if (fw > footW){
			int cut = (fw - footW) / 2;
			crop.left = f.left + cut / tw;
			crop.right = crop.left + footW / tw;
		} else {
			offX = (footW - fw) / 2;
		}
		if (fh > footH){
			int cut = (fh - footH) / 3;
			crop.top = f.top + cut / th;
			crop.bottom = crop.top + footH / th;
		} else {
			offY = footH - fh;
		}
		frame( crop );
	}

	private void takeTint( boolean always ){
		if (!always && src.rm == tint[0] && src.gm == tint[1] && src.bm == tint[2]
				&& src.ra == tint[3] && src.ga == tint[4] && src.ba == tint[5] && src.aa == tint[6]) return;
		tint[0] = rm = src.rm;
		tint[1] = gm = src.gm;
		tint[2] = bm = src.bm;
		tint[3] = ra = src.ra;
		tint[4] = ga = src.ga;
		tint[5] = ba = src.ba;
		tint[6] = aa = src.aa;
	}
}
