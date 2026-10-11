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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.Arc;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.ArcImage;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;

//a chain thrown: its links run out from the thrower over dist/320 + 0.2 s (the callback then, as
//ever), hanging in a sag that tightens as it flies (6 px to 1), every other link a shade darker,
//a glint of metal at its head and three sparks as it bites; then it is pulled back and fades over
//RETRACT s. An ethereal chain glows teal at every second link and sheds teal motes.
public class Chains extends Group {

	private static final double A = 180 / Math.PI;

	/** The sag at the start and the end of its flight, px; how long it takes to pull back, s. */
	public static final float SAG = 6f, SAG_TIGHT = 1f, RETRACT = 0.15f;
	/** Every other link's shade; the ethereal glows' colour and alpha, their most, its motes' gap. */
	public static final float SHADE = 0.8f, GLOW_ALPHA = 0.3f, MOTE_EVERY = 0.05f;
	public static final int GLOW_COLOR = 0x38ABAB, GLOWS = 8;

	private float spent = 0f;
	private float duration;

	private Callback callback;

	private Image[] chains;
	private int numChains;
	private float distance;
	private float rotation = 0;

	private PointF from, to;

	private final boolean ethereal;
	private boolean built, bitten;
	private float retract;
	private ArcImage head;
	private ArcImage[] glows;
	private float moteIn;

	public Chains(int from, int to, Effects.Type type, Callback callback){
		this(DungeonTilemap.tileCenterToWorld(from),
				DungeonTilemap.tileCenterToWorld(to),
				type,
				callback);
	}

	public Chains(PointF from, PointF to, Effects.Type type, Callback callback){
		super();

		this.callback = callback;

		this.from = from;
		this.to = to;
		ethereal = type == Effects.Type.ETHEREAL_CHAIN;

		float dx = to.x - from.x;
		float dy = to.y - from.y;
		distance = (float)Math.hypot(dx, dy);

		//base of 200ms, plus 50ms per tile travelled
		duration = distance/320f + 0.2f;

		rotation = (float)(Math.atan2( dy, dx ) * A) + 90f;

		numChains = Math.round(distance/6f)+1;

		chains = new Image[numChains];
		for (int i = 0; i < chains.length; i++){
			chains[i] = new Image(Effects.get(type));
			chains[i].angle = rotation;
			chains[i].origin.set( chains[i].width()/ 2, chains[i].height() );
			if (i % 2 == 1) chains[i].brightness( SHADE );
			add(chains[i]);
		}
	}

	/** Its sag at a share of its flight: SAG easing to SAG_TIGHT as (1 - p)^2. */
	public static float sag( float p ){
		float k = 1f - Math.max( 0f, Math.min( 1f, p ) );
		return SAG_TIGHT + (SAG - SAG_TIGHT) * k * k;
	}

	//its head and (ethereal) its glows, in the light layer: render thread
	private void build(){
		built = true;
		if (!Arc.onScene()) return;
		head = ArcImage.get( GameScene.Layer.LIGHTS, false, true );
		head.show( FxFrames.PLUS_3 ).rgb( ethereal ? 0xB0FFF0 : Element.STEEL.ramp.stop( 1 ) );
		if (ethereal){
			glows = new ArcImage[Math.min( GLOWS, (numChains + 1) / 2 )];
			for (int i = 0; i < glows.length; i++){
				glows[i] = ArcImage.get( GameScene.Layer.LIGHTS, true, true );
				glows[i].show( FxFrames.Light.GLOW_7 ).rgb( GLOW_COLOR );
				glows[i].am = GLOW_ALPHA;
			}
		}
	}

	@Override
	public void update() {
		if (!built) build();
		float dt = Game.elapsed;

		if (!bitten && (spent += dt) > duration) {

			bitten = true;
			bite();
			if (callback != null) {
				callback.call();
			}

		} else if (bitten) {

			//pulled back after its bite, fading
			retract += dt;
			if (retract >= RETRACT){
				finish();
				return;
			}
			float k = 1f - retract / RETRACT;
			lay( k, k );

		} else {
			lay( spent / duration, 1f );
		}
		super.update();
	}

	//its links laid out a share p of the way, hanging in its sag, at an alpha
	private void lay( float p, float alpha ){
		float dx = to.x - from.x;
		float dy = to.y - from.y;
		float hang = sag( spent / duration );
		float hx = 0, hy = 0;
		for (int i = 0; i < chains.length; i++) {
			float t = i / (float)chains.length;
			float s = hang * 4f * t * (1f - t);
			float cx = from.x + dx * t * p, cy = from.y + dy * t * p + s;
			Image c = chains[i];
			c.x = cx - c.width() / 2f;
			c.y = cy - c.height() / 2f;
			c.alpha( alpha );
			hx = cx;
			hy = cy;
			if (glows != null && i % 2 == 0 && i / 2 < glows.length){
				ArcImage g = glows[i / 2];
				g.centre( cx, cy );
				g.am = GLOW_ALPHA * alpha;
			}
		}
		if (head != null){
			head.centre( hx, hy );
			head.am = alpha;
		}
		if (ethereal && alpha >= 1f && (moteIn -= Game.elapsed) <= 0){
			moteIn += MOTE_EVERY;
			float t = FxRandom.Float();
			FxParticle m = Arc.light( from.x + dx * t * p, from.y + dy * t * p, FxBudget.P3 );
			if (m != null) m.look( FxFrames.MOTE_1 ).paint( 0x70E0D0 ).vel( FxRandom.Float( -3, 3 ), -FxRandom.Float( 3, 6 ) )
					.life( 0.4f ).alpha( 0.9f, 0, 0.2f ).snap();
		}
	}

	//the bite: three sparks of metal at its head
	private void bite(){
		float hx = to.x, hy = to.y;
		for (int i = 0; i < 3; i++){
			FxParticle s = Arc.light( hx, hy, FxBudget.P2 );
			if (s == null) break;
			s.look( FxFrames.SPARK_2 ).orient().slow( FxFrames.MOTE_1, 15f ).ramp( Element.METAL.ramp, true )
					.polar( FxRandom.Float( (float)-Math.PI, 0 ), FxRandom.Float( 30f, 60f ) ).gravity( 120f ).drag( 1.2f )
					.life( FxRandom.Float( 0.25f, 0.45f ) ).alpha( 1f, 0, 0.1f );
		}
	}

	//gone: its links' buffers freed, its lights let go of
	private void finish(){
		if (head != null) head.kill();
		head = null;
		if (glows != null) for (ArcImage g : glows) g.kill();
		glows = null;
		Fx.dispose( this );
	}

}
