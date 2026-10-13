/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxCurves;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.utils.RectF;

//a surprise attack: an exclamation (the sheet's EXCLAIM, lit COLOR) drawn tall for TALL_FOR s, then
//at its own height, popping up into place, a ring of RING little crosses bursting out of it as
//light; it holds, then fades in painted steps over its last quarter second. Turned only in quarter
//turns
public class Surprise extends Image {

	public static final float TIME_TO_FADE = 1f, TALL_FOR = 0.08f, POP_FOR = 0.15f, POP = 3f;
	public static final int COLOR = 0xFFE070, RING = 6;

	private float time;
	private float cx, cy;
	private int[] shown;
	private boolean burst;
	private final RectF uv = new RectF();

	public Surprise() {
		super( Assets.Effects.FX_SHEET );
		show( FxFrames.EXCLAIM[0] );
	}

	private void show( int[] f ){
		if (f == shown) return;
		shown = f;
		float tw = texture.width, th = texture.height;
		uv.set( f[0] / tw, f[1] / th, (f[0] + f[2]) / tw, (f[1] + f[3]) / th );
		frame( uv );
		origin.set( f[2] / 2f, f[3] / 2f );
	}

	public void reset(int p) {
		revive();
		cx = (p % Dungeon.level.width() + 0.5f) * DungeonTilemap.SIZE;
		cy = (p / Dungeon.level.width() + 0.5f) * DungeonTilemap.SIZE;
		start();
	}

	public void reset(Visual v) {
		revive();
		cx = v.x + v.width() / 2f;
		cy = v.y + v.height() / 2f;
		start();
	}

	private void start(){
		time = TIME_TO_FADE;
		burst = false;
		//a recycled one went out hidden: shown again, so its ring bursts too
		visible = true;
		resetColor();
		hardlight( COLOR );
		show( FxFrames.EXCLAIM[0] );
		place( 0 );
	}

	//its middle on whole pixels, `lift` px below where it settles
	private void place( float lift ){
		x = Math.round( cx - width / 2f );
		y = Math.round( cy - height / 2f + lift );
	}

	/** Its alpha with `left` s of its life left: whole until its last quarter second, then 0.6,
	 *  0.3, gone (painted steps). */
	public static float alphaLeft( float left ){
		if (left >= 0.25f) return 1f;
		float k = left / 0.25f;
		return k > 0.75f ? 1f : k > 0.5f ? 0.6f : k > 0.25f ? 0.3f : 0f;
	}

	@Override
	public void update() {
		super.update();

		if (!burst){
			burst = true;
			ring();
		}
		if ((time -= Game.elapsed) <= 0) {
			kill();
		} else {
			float age = TIME_TO_FADE - time;
			show( age < TALL_FOR ? FxFrames.EXCLAIM[0] : FxFrames.EXCLAIM[1] );
			float pop = age < POP_FOR ? POP * (1f - FxCurves.backOut( age / POP_FOR, 1.15f )) : 0f;
			place( pop );
			am = alphaLeft( time );
			visible = am > 0;
		}
	}

	//six crosses of light bursting out of it, as many as the budget allows (at least one), spaced
	//evenly round it (render thread). Only in the game's world: a guide scene's surprise, under a
	//window's own camera, has no lights of the world to throw them into
	private void ring(){
		Camera c = camera();
		if (!visible || (c != null && c != Camera.main)) return;
		FxEmitter glow = Fx.glowAir();
		if (glow == null) return;
		int n = FxBudget.allow( RING, FxBudget.P1 );
		float a0 = FxRandom.angle();
		for (int i = 0; i < n; i++){
			((FxParticle) glow.recycle( FxParticle.class )).reset( cx, cy - 2f ).look( FxFrames.PLUS_3 ).paint( COLOR )
					.light( true ).polar( a0 + (float)(2 * Math.PI * i / n), 30f ).drag( 4f ).life( 0.35f )
					.alpha( 1f, 0, 0.2f ).snap().priority( FxBudget.P1 );
		}
	}

	/** An angle in quarter turns. */
	public static float quarter( float angle ){
		return Math.round( angle / 90f ) * 90f;
	}

	public static void hit(Char ch) {
		hit(ch, 0);
	}

	public static void hit(Char ch, float angle) {
		if (ch.sprite != null && ch.sprite.parent != null) {
			Surprise s = (Surprise) ch.sprite.parent.recycle(Surprise.class);
			ch.sprite.parent.bringToFront(s);
			s.reset(ch.sprite);
			s.angle = quarter( angle );
		}
	}

	public static void hit(int pos) {
		hit(pos, 0);
	}

	public static void hit(int pos, float angle) {
		Group parent = Dungeon.hero.sprite.parent;
		Surprise s = (Surprise) parent.recycle(Surprise.class);
		parent.bringToFront(s);
		s.reset(pos);
		s.angle = quarter( angle );
	}
}
