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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.skills;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;

/**
 * The skills' flare in pixels, where a fan of rays on a soft gradient (Flare) used to be: a
 * painted four-point star of the light sheet stepping up a size and back down (9, 15, 9 for a
 * small one; 15, 23, 15; 23, 31, 23), its A and B frames swapping as it turns, over a round glow
 * of its colour. Pooled in the light layer; at most CAP at once (a blast of twenty-five cells
 * draws the first CAP stars and only their glows past it).
 */
public class PixelFlare extends FxPic {

	public static final int S = 0, M = 1, L = 2;
	/** Stars at once. */
	public static final int CAP = 24;

	//per size: the small and the large frame, A and B
	private static final int[][][] FRAMES = {
			{ FxFrames.Light.STAR4_9_A, FxFrames.Light.STAR4_9_B, FxFrames.Light.STAR4_15_A, FxFrames.Light.STAR4_15_B },
			{ FxFrames.Light.STAR4_15_A, FxFrames.Light.STAR4_15_B, FxFrames.Light.STAR4_23_A, FxFrames.Light.STAR4_23_B },
			{ FxFrames.Light.STAR4_23_A, FxFrames.Light.STAR4_23_B, FxFrames.Light.STAR4_31_A, FxFrames.Light.STAR4_31_B } };
	private static final int[] GLOWS = { 11, 23, 31 };
	/** Its frames swap A and B this often, s. */
	public static final float TURN = 1 / 12f;

	private static int live;

	private int size;
	private float life, age;
	private int shownFrame = -1;
	private boolean counted;

	public PixelFlare(){
		super( SkillVfx.LIGHT_SHEET );
		additive = true;
	}

	public static synchronized int live(){
		return live;
	}

	/** A new scene: none of the last one's are counted. */
	public static synchronized void reset(){
		live = 0;
	}

	/** A flare at (x, y) in a colour, S, M or L, over `duration` s. Any thread: off the render
	 *  thread it is handed there. */
	public static void show( final float x, final float y, final int color, final int size, final float duration ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> show( x, y, color, size, duration ) );
			return;
		}
		int s = Math.max( S, Math.min( L, size ) );
		FxLight.glow( x, y, GLOWS[s], color, 0.9f ).life( duration * 0.2f, 0, duration * 0.8f );
		synchronized (PixelFlare.class){
			if (live >= CAP) return;
		}
		PixelFlare f = GameScene.recycle( GameScene.Layer.LIGHTS, PixelFlare.class );
		if (f == null) return;
		f.setup( x, y, color, s, duration );
	}

	private void setup( float x, float y, int color, int size, float duration ){
		this.size = size;
		show( FRAMES[size][0] );
		place( x, y );
		life = Math.max( 0.05f, duration );
		age = 0;
		shownFrame = -1;
		resetColor();
		lit( color );
		revive();
		visible = Fx.seen( x, y );
		synchronized (PixelFlare.class){
			if (!counted){
				counted = true;
				live++;
			}
		}
		step();
	}

	/** Which frame shows at a share p of its life: the small one, then the large at its height
	 *  (the middle half), then the small again; A or B by the turn. */
	static int frameAt( float age, float life ){
		float p = age / life;
		boolean large = p >= 0.25f && p < 0.75f;
		boolean b = ((int) (age / TURN)) % 2 == 1;
		return (large ? 2 : 0) + (b ? 1 : 0);
	}

	private void step(){
		int f = frameAt( age, life );
		if (f != shownFrame){
			shownFrame = f;
			takeSlide();
			show( FRAMES[size][f] );
			place( midX(), midY() );
		}
		float p = age / life;
		//up at once, held, then down: the brightest at its start (front-loaded)
		am = p < 0.6f ? 1f : Math.max( 0f, (1f - p) / 0.4f );
	}

	@Override
	public void update(){
		super.update();
		if (Emitter.freezeEmitters && Game.timeTotal > 1) return;
		age += Game.elapsed;
		if (age >= life){
			kill();
			return;
		}
		takeSlide();
		visible = Fx.seen( midX(), midY() );
		step();
	}

	@Override
	public void kill(){
		super.kill();
		leave();
	}

	@Override
	public void destroy(){
		super.destroy();
		leave();
	}

	private void leave(){
		synchronized (PixelFlare.class){
			if (counted){
				counted = false;
				live = Math.max( 0, live - 1 );
			}
		}
	}
}
