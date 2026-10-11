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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Lit;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.RectF;

/**
 * One painted frame of the kit's sheets (FxFrames on effects/fx_sheet.png, FxFrames.Light on
 * effects/fx_light.png) as a picture of its own: a bolt's head, a beam's halo, a flare's star.
 * Set up without allocating (show() keeps one uv rect), placed on whole pixels (centre()) or
 * stretched as a line quad (line()), drawn added (light) or plain (matter).
 *
 * Making one is safe on any thread (its texture uploads when first drawn); it is pooled in the
 * kit's layers by get(): a dead one of a layer brought back, its sheet and blend set again.
 */
public class ArcImage extends Image implements Lit {

	/** Drawn added (light) rather than plainly (matter). */
	public boolean additive;
	private boolean lightSheet;

	private final RectF uv = new RectF();
	private int[] shown;
	private Gizmo carrier;

	public ArcImage(){
		this( true, true );
	}

	public ArcImage( boolean lightSheet, boolean additive ){
		super( lightSheet ? Assets.Effects.FX_LIGHT : Assets.Effects.FX_SHEET );
		this.lightSheet = lightSheet;
		this.additive = additive;
	}

	/**
	 * A picture of a sheet in a layer of the game's scene: a dead one brought back, else a new one
	 * added; off the scene a new one in no group (a test's, a window's). Render thread when a layer
	 * is given.
	 */
	public static ArcImage get( GameScene.Layer layer, boolean lightSheet, boolean additive ){
		ArcImage a = layer == null ? null : GameScene.recycle( layer, ArcImage.class );
		if (a == null) return new ArcImage( lightSheet, additive );
		a.renew( lightSheet, additive );
		return a;
	}

	/** In a group of one's own: a dead one of it brought back, else a new one added. */
	public static ArcImage in( Group group, boolean lightSheet, boolean additive ){
		if (group == null) return new ArcImage( lightSheet, additive );
		ArcImage a = (ArcImage) group.recycle( ArcImage.class );
		a.renew( lightSheet, additive );
		return a;
	}

	/** Back to a fresh state on its sheet: white, whole, unturned, unscaled, shown. */
	public ArcImage renew( boolean lightSheet, boolean additive ){
		if (this.lightSheet != lightSheet){
			this.lightSheet = lightSheet;
			texture( lightSheet ? Assets.Effects.FX_LIGHT : Assets.Effects.FX_SHEET );
			shown = null;
		}
		this.additive = additive;
		revive();
		resetColor();
		scale.set( 1 );
		origin.set( 0 );
		angle = 0;
		angularSpeed = 0;
		speed.set( 0 );
		acc.set( 0 );
		flipHorizontal = flipVertical = false;
		visible = true;
		carrier = null;
		return this;
	}

	/** A frame of its sheet, { x, y, w, h }; nothing done if it already shows it. */
	public ArcImage show( int[] f ){
		if (f == shown) return this;
		shown = f;
		float tw = texture.width, th = texture.height;
		uv.set( f[0] / tw, f[1] / th, (f[0] + f[2]) / tw, (f[1] + f[3]) / th );
		frame( uv );
		return this;
	}

	public int[] shown(){
		return shown;
	}

	/** Mirrored (a flip, never a fractional scale). */
	public ArcImage mirror( boolean h, boolean v ){
		if (flipHorizontal != h || flipVertical != v){
			flipHorizontal = h;
			flipVertical = v;
			updateFrame();
		}
		return this;
	}

	/** Its middle at (cx, cy), drawn at its whole scale from a top-left on whole pixels. */
	public ArcImage centre( float cx, float cy ){
		origin.set( 0 );
		angle = 0;
		x = Math.round( cx - width * scale.x / 2f );
		y = Math.round( cy - height * scale.y / 2f );
		return this;
	}

	/** A line quad from (x0, y0) to (x1, y1): its frame stretched along the way, its rows across. */
	public ArcImage line( float x0, float y0, float x1, float y1 ){
		float dx = x1 - x0, dy = y1 - y0;
		float len = (float)Math.sqrt( dx * dx + dy * dy );
		origin.set( 0, height / 2f );
		x = x0;
		y = y0 - height / 2f;
		angle = (float)Math.toDegrees( Math.atan2( dy, dx ) );
		scale.x = Math.max( 0.001f, len / width );
		return this;
	}

	/** Lit in a colour, 0xRRGGBB: a white light frame shows it, a grey matter frame keeps its shading. */
	public ArcImage rgb( int c ){
		hardlight( ((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f );
		return this;
	}

	/** Drawn over the night's tint from the light layer while it lives (GameScene.carryLight). */
	public ArcImage carry(){
		if (carrier == null || !carrier.exists) carrier = GameScene.carryLight( this );
		return this;
	}

	@Override
	public void draw(){
		if (carrier != null && carrier.exists) return;
		drawLit();
	}

	@Override
	public void drawLit(){
		if (additive){
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		} else {
			super.draw();
		}
	}
}
