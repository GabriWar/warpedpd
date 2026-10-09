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

package xyz.gabriwar.warpedpixeldungeon.effects.fx;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.Ripple;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.RectF;

/**
 * What answers on the water: rings (S, M, L) spreading on its surface in the liquid's foam,
 * drops landing in it, splashes with their crown and droplets, washes of colour through a cell,
 * and the disturbances everything that lives in the water reacts to (fish scatter from a ring,
 * come to look at a crumb).
 *
 * Rings: S 5x3 to 13x7 in 0.35 s; M 9x5 to 23x11 in 0.6 s with a second a size smaller 0.12 s
 * later; L 13x7 to 41x19 in 0.9 s with its second; eased out, alpha 0.8 (L 0.7) x (1 - p)^1.2,
 * mirrored (the far crest bright), on the surface (masked by the shore's tiles, under the tint
 * and the fog), lava's added in FF9030. No more than RINGS_A_FRAME new rings a frame: past them,
 * or past the rings' cap, a ring the player made (P1) takes the oldest one's place and any other
 * is dropped. Every ring disturbs the water round it (24, 40 or 64 px).
 *
 * The liquid is the level's water texture's (Element.Liquid.of), never its class, so a co-op
 * guest's water answers as the host's. Nothing shows off the level or while it is mid-rebase.
 * Kit-seeded: the water's life owns it from here, its public signatures fixed.
 */
public final class WaterFX {

	private WaterFX(){}

	public static final int S = 0, M = 1, L = 2;

	/** Each size's rings: the width it starts and ends at, its second ring's, its life, alpha and
	 *  the reach of the disturbance it makes, px. */
	public static final int[] FROM = { 5, 9, 13 }, TO = { 13, 23, 41 };
	public static final int[] SECOND_FROM = { 5, 5, 9 }, SECOND_TO = { 9, 17, 31 };
	public static final float[] LIFE = { 0.35f, 0.6f, 0.9f };
	public static final float[] ALPHA = { 0.8f, 0.8f, 0.7f };
	public static final float[] DISTURB = { 24f, 40f, 64f };
	public static final float SECOND_AFTER = 0.12f;
	public static final int RINGS_A_FRAME = 6;
	/** The disturbances a frame can hold. */
	public static final int BUS = 16;
	/** A splash's droplets by size, at least and at most; a crown's life. */
	public static final int[] DROPLETS_MIN = { 3, 5, 6 }, DROPLETS_MAX = { 5, 7, 8 };
	public static final float CROWN_LIFE = 0.18f;
	/** Washes at once. */
	public static final int WASHES = 12;

	public static int sizeOf( int size ){
		return size < S ? S : (size > L ? L : size);
	}

	public static float alphaOf( int size ){
		return ALPHA[sizeOf( size )];
	}

	// ------------------------------------------------------------------ the liquid

	private static Level liquidOf;
	private static String liquidTex;
	private static Element.Liquid liquid = Element.Liquid.SEWER;

	/** The level's liquid, by its water texture (which the world's slices change with their
	 *  height, so it is asked each time). */
	public static Element.Liquid liquid(){
		Level level = Dungeon.level;
		String tex = level == null ? null : level.waterTex();
		if (level != liquidOf || tex != liquidTex){
			liquidOf = level;
			liquidTex = tex;
			liquid = Element.Liquid.of( tex );
		}
		return liquid;
	}

	/** Whether a world point lies over the level's water (false off it, and while its map is
	 *  mid-rebase). */
	public static boolean onWater( float x, float y ){
		Level level = Dungeon.level;
		if (level == null || level.water == null || level.fogHeld()) return false;
		int cell = Fx.cellAt( x, y );
		return cell >= 0 && cell < level.water.length && level.water[cell];
	}

	// ------------------------------------------------------------------ rings

	private static int ringsThisFrame;

	/** A ring of a size on a point, the liquid's own; the player's (P1). */
	public static Ripple ring( float x, float y, int size ){
		Element.Liquid l = liquid();
		return ring( x, y, size, l.foam, alphaOf( size ), FxBudget.P1 );
	}

	/** A ring in a colour and alpha of its own; the player's (P1). */
	public static Ripple ring( float x, float y, int size, int color, float alpha ){
		return ring( x, y, size, color, alpha, FxBudget.P1 );
	}

	/**
	 * A ring of a size on a point in a colour and alpha, of a priority: a P1 ring (the player's)
	 * is never dropped, it takes the oldest ring's place; a P2 or P3 one (the rain's, the ambience's)
	 * is dropped past the frame's RINGS_A_FRAME or the rings' cap. Null when dropped or off the scene.
	 * Any thread.
	 */
	public static Ripple ring( float x, float y, int size, int color, float alpha, int priority ){
		int s = sizeOf( size );
		synchronized (WaterFX.class){
			boolean full = ringsThisFrame >= RINGS_A_FRAME || FxRing.live() >= FxBudget.ringCap();
			if (full){
				if (priority > FxBudget.P1) return null;
				FxRing oldest = FxRing.oldest();
				if (oldest != null) oldest.kill();
			}
			ringsThisFrame++;
		}
		Ripple r = GameScene.recycle( GameScene.Layer.SURFACE, Ripple.class );
		if (r == null) return null;
		boolean light = liquid().ringAdditive;
		r.reset( x, y, s, color, alpha, light );
		if (s > S){
			Ripple second = GameScene.recycle( GameScene.Layer.SURFACE, Ripple.class );
			if (second != null) second.second( r, s, color, alpha, light );
		}
		disturb( x, y, DISTURB[s], false );
		return r;
	}

	/**
	 * Something landing in the water at (x, y), as hard as `strength` (0..1): a small ring under
	 * 0.34 (with a plink a time in four), a middling one under 0.67, a large one past it; in the
	 * liquid's foam, or in `color` (Element.NONE for the foam). Nothing off the water.
	 */
	public static void drop( float x, float y, float strength, int color ){
		if (!onWater( x, y )) return;
		Element.Liquid l = liquid();
		int c = color == Element.NONE ? l.foam : color;
		int size = strength < 0.34f ? S : (strength < 0.67f ? M : L);
		ring( x, y, size, c, alphaOf( size ), size == S ? FxBudget.P2 : FxBudget.P1 );
		if (size == S && FxRandom.chance( 0.25f ) && Fx.onRenderThread()) plink( x, y, c );
	}

	// ------------------------------------------------------------------ splashes and droplets

	/**
	 * A splash: a crown thrown up over 0.18 s, its droplets (3-5, 5-7 or 6-8 by size) and a ring,
	 * in `color` (Element.NONE for the liquid's foam). Render thread (else handed there).
	 */
	public static void splash( final float x, final float y, final int size, final int color ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> splash( x, y, size, color ) );
			return;
		}
		if (!onWater( x, y )) return;
		int s = sizeOf( size );
		int c = color == Element.NONE ? liquid().foam : color;
		crown( x, y, s, c );
		droplets( x, y, FxRandom.IntRange( DROPLETS_MIN[s], DROPLETS_MAX[s] ), 30f + 10f * s, 60f + 15f * s, 100f, c );
		ring( x, y, s, c, alphaOf( s ), FxBudget.P1 );
	}

	//a crown's frames by size: a small one up to 7 px, a middling one through all three, a large
	//one from 7 to 9
	private static final int[][][] CROWNS = {
			{ FxFrames.CROWN[0], FxFrames.CROWN[1] },
			FxFrames.CROWN,
			{ FxFrames.CROWN[1], FxFrames.CROWN[2] } };

	/** A splash's crown alone: thrown up out of the water over 0.18 s. Render thread. */
	public static void crown( float x, float y, int size, int color ){
		FxEmitter e = Fx.air();
		if (e == null) return;
		int[][] frames = CROWNS[sizeOf( size )];
		FxParticle p = ((FxParticle) e.recycle( FxParticle.class )).reset( x, y );
		int h = frames[frames.length - 1][3];
		p.look( frames, 0 ).paint( color ).life( CROWN_LIFE ).alpha( 0.9f, 0, 0.06f )
				.at( x, y - h / 2f + 1 ).snap().priority( FxBudget.P1 );
	}

	//a drop thrown back up by a small one landing
	private static void plink( float x, float y, int color ){
		FxEmitter e = Fx.air();
		if (e == null || !FxBudget.keep( FxBudget.P3, x, y )) return;
		((FxParticle) e.recycle( FxParticle.class )).reset( x, y - 1 ).look( FxFrames.PLINK )
				.paint( color ).vel( 0, -22f ).gravity( 160f ).life( 0.12f ).alpha( 0.85f, 0, 0.04f )
				.priority( FxBudget.P3 );
	}

	/**
	 * n droplets thrown up from (x, y) at vMin-vMax px/s within spreadDeg of straight up, in
	 * `color`: turned to their way, falling at 240 px/s^2 to land within 4 px of where they left -
	 * a small ring on the water, a wet mark on the floor. P3, and FxBudget.dropletCap() at once.
	 * Render thread.
	 */
	public static void droplets( float x, float y, int n, float vMin, float vMax, float spreadDeg, int color ){
		FxEmitter e = Fx.air();
		if (e == null) return;
		int k = FxBudget.allow( n, FxBudget.P3 );
		float spread = (float)Math.toRadians( spreadDeg ) / 2f;
		for (int i = 0; i < k; i++){
			if (Droplet.count >= FxBudget.dropletCap()) return;
			float a = (float)(-Math.PI / 2) + FxRandom.Float( -spread, spread );
			Droplet d = (Droplet) e.recycle( Droplet.class );
			d.reset( x, y );
			d.look( FxRandom.chance( 0.5f ) ? FxFrames.DROP_2 : FxFrames.DROP_3 ).paint( color ).orient()
					.polar( a, FxRandom.Float( vMin, vMax ) ).gravity( 240f )
					.ground( y + FxRandom.Float( 0f, 4f ), FxParticle.RING, 0 ).landDry( FxParticle.WET )
					.strength( 0.1f ).mark( color ).life( 2f ).alpha( 1f, 0, 0.05f ).priority( FxBudget.P3 );
		}
	}

	/** A droplet: counted against the droplets' own cap as well as the particles'. */
	public static class Droplet extends FxParticle {
		static int count;
		private boolean counted;

		@Override
		public void revive(){
			super.revive();
			if (!counted){
				counted = true;
				synchronized (Droplet.class){
					count++;
				}
				FxBudget.add( FxBudget.DROPLETS, 1 );
			}
		}

		@Override
		public void kill(){
			super.kill();
			if (counted){
				counted = false;
				synchronized (Droplet.class){
					count--;
				}
				FxBudget.add( FxBudget.DROPLETS, -1 );
			}
		}
	}

	// ------------------------------------------------------------------ washes

	private static final Wash[] washes = new Wash[WASHES];
	private static int washCount;

	/** A wash of colour through a water cell (blood clouding it, a potion's tint), fading over
	 *  `life` s; WASHES at once, the oldest giving way. Render thread. */
	public static void wash( int cell, int color, float alpha, float life ){
		Level level = Dungeon.level;
		if (level == null || cell < 0 || cell >= level.length() || !level.water[cell]) return;
		Wash w = GameScene.recycle( GameScene.Layer.SURFACE, Wash.class );
		if (w == null) return;
		synchronized (WaterFX.class){
			if (washCount >= WASHES){
				Wash oldest = washes[0];
				System.arraycopy( washes, 1, washes, 0, WASHES - 1 );
				washCount--;
				if (oldest != null && oldest != w) oldest.kill();
			}
			washes[washCount++] = w;
		}
		w.setup( (cell % level.width()) * DungeonTilemap.SIZE, (cell / level.width()) * DungeonTilemap.SIZE, color, alpha, life );
	}

	private static synchronized void washGone( Wash w ){
		for (int i = 0; i < washCount; i++){
			if (washes[i] == w){
				System.arraycopy( washes, i + 1, washes, i, washCount - i - 1 );
				washes[--washCount] = null;
				return;
			}
		}
	}

	/** A soft square of colour on a water cell. */
	public static class Wash extends Image {
		private float peak, life, age;

		public Wash(){
			super( Assets.Effects.FX_LIGHT );
			int[] f = FxFrames.Light.WASH_16;
			frame( new RectF( f[0] / (float)texture.width, f[1] / (float)texture.height,
					(f[0] + f[2]) / (float)texture.width, (f[1] + f[3]) / (float)texture.height ) );
		}

		void setup( float x, float y, int color, float alpha, float life ){
			this.x = x;
			this.y = y;
			peak = alpha;
			this.life = Math.max( 0.05f, life );
			age = 0;
			resetColor();
			hardlight( ((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f );
			am = 0;
			visible = true;
			revive();
		}

		@Override
		public void update(){
			super.update();
			if (Emitter.freezeEmitters && Game.timeTotal > 1) return;
			age += Game.elapsed;
			float p = age / life;
			if (p >= 1){
				kill();
				return;
			}
			//swirls in over a fifth of its life, then thins away
			am = peak * (p < 0.2f ? p / 0.2f : 1f - (p - 0.2f) / 0.8f);
		}

		@Override
		public void kill(){
			super.kill();
			washGone( this );
		}
	}

	// ------------------------------------------------------------------ disturbances

	//this frame's disturbances, filling, and the last frame's, read by every drainer
	private static final float[] filling = new float[BUS * 4];
	private static int filled;
	private static final float[] snapshot = new float[BUS * 4];
	private static int snapped;

	/**
	 * Something stirred the water at (x, y) within `radius` px: a ring, a step, a stone, a crumb
	 * (food: what fish come to rather than flee). Any thread; BUS a frame, the oldest giving way.
	 */
	public static void disturb( float x, float y, float radius, boolean food ){
		synchronized (filling){
			if (filled == BUS){
				System.arraycopy( filling, 4, filling, 0, (BUS - 1) * 4 );
				filled--;
			}
			int i = filled * 4;
			filling[i] = x;
			filling[i + 1] = y;
			filling[i + 2] = radius;
			filling[i + 3] = food ? 1f : 0f;
			filled++;
		}
	}

	/**
	 * The last frame's disturbances, four floats each (x, y, radius, food 1 or 0), copied into
	 * `out` (BUS x 4 long will hold them all); how many. The same for every caller this frame.
	 */
	public static int disturbances( float[] out ){
		synchronized (filling){
			int n = Math.min( snapped, out.length / 4 );
			System.arraycopy( snapshot, 0, out, 0, n * 4 );
			return n;
		}
	}

	/** Once a frame (GameScene.update): the frame's disturbances become the ones read, and a new
	 *  frame's rings may start. */
	public static void frame(){
		synchronized (filling){
			System.arraycopy( filling, 0, snapshot, 0, filled * 4 );
			snapped = filled;
			filled = 0;
		}
		synchronized (WaterFX.class){
			ringsThisFrame = 0;
		}
	}

	/** A new scene: nothing of the last one's. */
	public static void sceneCreated(){
		synchronized (filling){
			filled = snapped = 0;
		}
		synchronized (WaterFX.class){
			ringsThisFrame = 0;
			for (int i = 0; i < washCount; i++) washes[i] = null;
			washCount = 0;
			Droplet.count = 0;
		}
		liquidOf = null;
	}
}
