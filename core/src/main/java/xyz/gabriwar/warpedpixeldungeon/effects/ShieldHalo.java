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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.RectF;

//a shield of light round a creature: a painted bubble (BUBBLE_23, 31 or 47 by the sprite's
//half-diagonal), rim-bright, at ALPHA, added; a bright arc runs round its rim (RIM_ARC_23, eight
//steps in ARC_FOR s) every ARC_EVERY s, and the bubble brightens a moment (RIPPLE for RIPPLE_FOR s)
//as a blow lands on it (the sprite flashes). Put out after it has held a while, it breaks: shards
//of it flung out and falling, the bubble stepping out a size and gone in BREAK_FOR s; put out at
//once (a skill's pulse of light), it steps out through the bigger bubbles and fades in PULSE_FOR s.
//Its picture is an asset's frame: safe to make on any thread
public class ShieldHalo extends Image {

	public static final int COLOR = 0xBBAACC;
	public static final float ALPHA = 0.33f, RIPPLE = 0.2f, RIPPLE_FOR = 0.15f, ARC_EVERY = 2f, ARC_FOR = 0.4f,
			ARC_ALPHA = 0.5f, BREAK_FOR = 0.25f, PULSE_FOR = 0.4f, WORN = 0.2f;
	public static final int SHARDS_MIN = 6, SHARDS_MAX = 8;
	private static final int[][] BUBBLES = { FxFrames.Light.BUBBLE_23, FxFrames.Light.BUBBLE_31, FxFrames.Light.BUBBLE_47 };
	private static final int[] SIZES = { 23, 31, 47 };

	private CharSprite target;
	private final RectF uv = new RectF();
	private int bubble, shownBubble = -1;
	private float age, ripple, arcPhase;
	private boolean wasFlashing;
	//put out: -1 not yet; else how long ago, and whether it breaks or pulses
	private volatile boolean outAsked;
	private float out = -1;
	private boolean breaks;
	//the arc running round its rim (light sheet) and, round a middling bubble, a twinkle (sheet)
	private Image arc, twinkle;
	private final RectF arcUv = new RectF(), twinkleUv = new RectF();

	public ShieldHalo( CharSprite sprite ) {
		super( Assets.Effects.FX_LIGHT );
		target = sprite;
		bubble = bubbleFor( sprite.width(), sprite.height() );
		show( bubble );
		resetColor();
		hardlight( COLOR );
		am = ALPHA;
		arcPhase = FxRandom.Float( ARC_EVERY );
	}

	/** The bubble for a sprite (0: 23 px, 1: 31, 2: 47): the smallest round its half-diagonal. */
	public static int bubbleFor( float w, float h ){
		float d = 2f * (float)Math.sqrt( (w / 2f) * (w / 2f) + (h / 2f) * (h / 2f) );
		return d <= 26f ? 0 : (d <= 38f ? 1 : 2);
	}

	/** Its width in px now. */
	public int size(){
		return SIZES[shownBubble];
	}

	private void show( int b ){
		if (b == shownBubble) return;
		shownBubble = b;
		int[] f = BUBBLES[b];
		float tw = texture.width, th = texture.height;
		uv.set( f[0] / tw, f[1] / th, (f[0] + f[2]) / tw, (f[1] + f[3]) / th );
		frame( uv );
	}

	@Override
	public void update() {
		super.update();
		float dt = Game.elapsed;
		age += dt;

		if (outAsked && out < 0){
			out = 0;
			breaks = age >= WORN;
			if (breaks) shatter();
		}

		//a blow landing on it: it brightens a moment
		boolean flashing = target.isFlashing();
		if (flashing && !wasFlashing) ripple = RIPPLE_FOR;
		wasFlashing = flashing;
		if (ripple > 0) ripple -= dt;

		float a = ALPHA + (ripple > 0 ? RIPPLE : 0);
		if (out >= 0){
			out += dt;
			float life = breaks ? BREAK_FOR : PULSE_FOR;
			if (out >= life){
				killAndErase();
				return;
			}
			float p = out / life;
			//stepping out a size (breaking) or through every bigger one (a pulse), fading
			show( breaks ? Math.min( 2, bubble + 1 ) : Math.min( 2, bubble + (int)(p * (3 - bubble)) ) );
			a = ALPHA * (1f - p) * (breaks ? 1.5f : 1.8f);
		}
		am = a;

		if (visible = target.visible) {
			x = Math.round( target.x + target.width() / 2f - width / 2f );
			y = Math.round( target.y + target.height() / 2f - height / 2f );
		}
	}

	//its shards flung out from its rim, falling (render thread)
	private void shatter(){
		if (!target.visible) return;
		FxEmitter glow = Fx.glowAir();
		if (glow == null) return;
		float cx = target.x + target.width() / 2f, cy = target.y + target.height() / 2f;
		float r = SIZES[bubble] / 2f - 2f;
		int n = FxBudget.allow( FxRandom.IntRange( SHARDS_MIN, SHARDS_MAX ), FxBudget.P2 );
		float a0 = FxRandom.angle();
		for (int i = 0; i < n; i++){
			float a = a0 + (float)(2 * Math.PI * i / n) + FxRandom.Float( -0.25f, 0.25f );
			float c = (float)Math.cos( a ), s = (float)Math.sin( a );
			((FxParticle) glow.recycle( FxParticle.class )).reset( cx + c * r, cy + s * r / 1f )
					.look( FxFrames.SHARD_GLASS[i % FxFrames.SHARD_GLASS.length] ).paint( COLOR ).light( true )
					.vel( c * FxRandom.Float( 30f, 60f ), s * FxRandom.Float( 30f, 60f ) - 10f ).gravity( 160f )
					.spin( 0.08f ).life( 0.4f ).alpha( 1f, 0, 0.15f ).priority( FxBudget.P2 );
		}
	}

	/** The arc's step `t` s in (0-7 while it runs, -1 between). */
	public static int arcStep( float t ){
		float in = t % ARC_EVERY;
		return in < ARC_FOR ? Math.min( 7, (int)(in / ARC_FOR * 8) ) : -1;
	}

	@Override
	public void draw() {
		Blending.setLightMode();
		super.draw();
		if (out < 0) drawRim();
		Blending.setNormalMode();
	}

	//the bright arc running round the rim (a twinkle round a middling bubble), over the bubble
	private void drawRim(){
		int step = arcStep( age + arcPhase );
		if (step < 0) return;
		float cx = x + width / 2f, cy = y + height / 2f;
		if (shownBubble == 1){
			if (twinkle == null){
				twinkle = new Image( Assets.Effects.FX_SHEET );
				int[] f = FxFrames.PLUS_3;
				float tw = twinkle.texture.width, th = twinkle.texture.height;
				twinkleUv.set( f[0] / tw, f[1] / th, (f[0] + f[2]) / tw, (f[1] + f[3]) / th );
				twinkle.frame( twinkleUv );
			}
			double a = step * Math.PI / 4;
			twinkle.camera = camera();
			twinkle.resetColor();
			twinkle.hardlight( 1f, 1f, 1f );
			twinkle.am = ARC_ALPHA;
			twinkle.x = Math.round( cx + 14 * (float)Math.cos( a ) - 1.5f );
			twinkle.y = Math.round( cy + 14 * (float)Math.sin( a ) - 1.5f );
			twinkle.draw();
			return;
		}
		if (arc == null) arc = new Image( Assets.Effects.FX_LIGHT );
		int[] f = FxFrames.Light.RIM_ARC_23[step];
		float tw = arc.texture.width, th = arc.texture.height;
		arcUv.set( f[0] / tw, f[1] / th, (f[0] + f[2]) / tw, (f[1] + f[3]) / th );
		arc.frame( arcUv );
		int sc = shownBubble == 2 ? 2 : 1;
		arc.scale.set( sc );
		arc.camera = camera();
		arc.resetColor();
		arc.hardlight( 1f, 1f, 1f );
		arc.am = ARC_ALPHA;
		arc.x = Math.round( cx - f[2] * sc / 2f );
		arc.y = Math.round( cy - f[3] * sc / 2f );
		arc.draw();
	}

	@Override
	public void kill() {
		super.kill();
		//its rim's pictures let go of as it goes (killAndErase never destroys it)
		if (arc != null) Fx.dispose( arc );
		if (twinkle != null) Fx.dispose( twinkle );
		arc = twinkle = null;
	}

	@Override
	public void destroy() {
		super.destroy();
		if (arc != null) arc.destroy();
		if (twinkle != null) twinkle.destroy();
	}

	public void putOut() {
		outAsked = true;
	}

}
