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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.water;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;

/**
 * The lava's life (docs/fx/water-life.md, "Regions": the halls, their boss's floor, the last
 * level, the dead end - wherever the level's water is the halls' lava), run by the live surface on
 * the render thread from the lava cells in view the hero sees. Its glow over the whole lava is
 * the live surface's (WaterLive: lava_glow.png, added in FF7A20 at 0.10-0.25 on a 3.5 s sine).
 *
 *  - Crust: 2x1 flecks of the cooled skin (3A0A06 at 0.6) riding the lava's crawl down at 2 px/s,
 *    one for every two cells in view, each 3-5 s; at most 16.
 *  - Bubbles: one a cell every 2-4 s - a dark bulge of a ring (5A0000 at 0.7, laid on), a bubble
 *    of light (BUBBLE_3, FFC040 added) for 0.2 s, then its pop (POP_5, 0.12 s), two or three embers
 *    thrown up at 30-50 px/s falling back at 80, and a glow of 11 px in FF9030 dying from 0.5 in
 *    0.3 s; at most 12 at once (with the swamp's).
 *  - Heat: every 1.5 s a heat ray (a broken streak of light, the kit's REFLECT frames wavering at
 *    4 a second, added in FFE8C0 at 0.15) rising 4 px/s off one cell in three (of those with lava
 *    north of them as well, so it never stands over the bank) for 1.5 s; at most 8.
 *  - The banks: a pool of its light (POOL_23, FF6A20 at 0.25, breathing) on each of the six dry
 *    cells beside it nearest the hero (persistent lights, put out as he leaves them behind).
 *  - Spits (HallsLevel.Stream: every 1-3 s a cell): a tongue of its flame leaping out of it and
 *    three sparks thrown up on an arc; at most 10 at once.
 */
public final class WaterLava {

	private static final float SIZE = DungeonTilemap.SIZE;

	/** Its crust: colour, alpha, its drift (the lava's crawl), its life, flecks a cell. */
	public static final int CRUST = 0x3A0A06;
	public static final float CRUST_ALPHA = 0.6f, CRUST_DRIFT = 2f, CRUST_MIN = 3f, CRUST_MAX = 5f, CRUST_A_CELL = 0.5f;
	/** Its bubbles: the bulge, the bubble's light, the pop's glow; one a cell every BUBBLE_EVERY s
	 *  on the mean (2-4). */
	public static final int BULGE = 0x5A0000, BUBBLE = 0xFFC040, POP_LIGHT = 0xFF9030, POP_GLOW = 11;
	public static final float BULGE_ALPHA = 0.7f, BUBBLE_LIFE = 0.2f, POP_LIFE = 0.12f, POP_LEVEL = 0.5f,
			POP_FADE = 0.3f, BUBBLE_EVERY = 3f;
	/** Its heat rays: colour, alpha, how often, the share of cells, their life, at most. */
	public static final int HEAT = 0xFFE8C0;
	public static final float HEAT_ALPHA = 0.15f, HEAT_EVERY = 1.5f, HEAT_SHARE = 1f / 3f, HEAT_LIFE = 1.5f,
			HEAT_FPS = 4f, HEAT_RISE = 4f;
	/** Its banks' pools: colour, alpha, how many. */
	public static final int EDGE = 0xFF6A20, EDGES = 6, EDGE_SIZE = 23;
	public static final float EDGE_ALPHA = 0.25f;
	/** Its spits: at most at once, how long one is in the air. */
	public static final int SPITS = 10;
	public static final float SPIT_LIFE = 0.6f;

	private float crustIn, bubbles, heatIn;
	//the banks lit: their cells and their lights
	private final int[] edgeCell = new int[EDGES];
	private final FxLight[] edgeLight = new FxLight[EDGES];
	private final float[] edgeX = new float[EDGES], edgeY = new float[EDGES];

	WaterLava(){
		for (int i = 0; i < EDGES; i++) edgeCell[i] = -1;
	}

	/** Every frame over the lava cells in view the hero sees (`cells`, `n` of them). */
	void update( Level level, int[] cells, int n, float dt ){
		if (n == 0) return;
		crust( cells, n, dt );
		bubbles( cells, n, dt );
		heat( cells, n, dt );
	}

	// ------------------------------------------------------------------ crust

	private void crust( int[] cells, int n, float dt ){
		int want = Math.min( Marks.CAPS[Marks.CRUST], Math.round( n * CRUST_A_CELL ) );
		if (Marks.live( Marks.CRUST ) >= want || (crustIn -= dt) > 0f) return;
		//at most four new a second: the skin forms, it does not pop into being
		crustIn = 0.25f;
		int c = cells[FxRandom.Int( n )];
		float x = cellX( c ) + FxRandom.IntRange( 2, 13 ), y = cellY( c ) + FxRandom.IntRange( 2, 9 );
		if (!FxBudget.keep( FxBudget.P3, x, y )) return;
		Marks.Mark m = Marks.make( Marks.CRUST, false, x, y );
		if (m == null) return;
		float life = FxRandom.Float( CRUST_MIN, CRUST_MAX );
		m.look( FxFrames.SEED ).paint( CRUST ).life( life ).alpha( CRUST_ALPHA, 0.6f, 0.8f ).smooth()
				.vel( FxRandom.Float( -0.4f, 0.4f ), CRUST_DRIFT ).snap().priority( FxBudget.P3 );
	}

	// ------------------------------------------------------------------ bubbles

	private void bubbles( int[] cells, int n, float dt ){
		bubbles += n / BUBBLE_EVERY * dt;
		while (bubbles >= 1f){
			bubbles -= 1f;
			if (!Marks.room( Marks.BUBBLE )) continue;
			int c = cells[FxRandom.Int( n )];
			bubble( cellX( c ) + FxRandom.IntRange( 3, 12 ), cellY( c ) + FxRandom.IntRange( 4, 12 ) );
		}
	}

	/** A bubble of the lava at (x, y): its bulge, its light, its pop. Render thread. */
	public static void bubble( float x, float y ){
		if (!FxBudget.keep( FxBudget.P3, x, y )) return;
		Marks.Mark m = Marks.make( Marks.BUBBLE, true, x, y - 1 );
		if (m == null) return;
		WaterFX.crustRing( x, y, WaterFX.S, BULGE, BULGE_ALPHA, FxBudget.P3 );
		m.look( FxFrames.BUBBLE_3 ).paint( BUBBLE ).life( BUBBLE_LIFE ).alpha( 0.9f, 0.08f, 0f ).snap()
				.fate( POPPED ).priority( FxBudget.P3 );
	}

	//its pop: the burst skin, two or three embers, a glow dying in 0.3 s
	static final FxParticle.Fate POPPED = ( p, landed ) -> {
		float x = p.px, y = p.py;
		Marks.Mark pop = Marks.make( Marks.BUBBLE, true, x, y );
		if (pop != null) pop.look( FxFrames.POP_5 ).paint( BUBBLE ).life( POP_LIFE ).alpha( 0.85f, 0, 0.06f ).snap()
				.priority( FxBudget.P3 );
		FxEmitter glow = Fx.glowAir();
		if (glow != null){
			int k = FxBudget.allow( FxRandom.IntRange( 2, 3 ), FxBudget.P3 );
			for (int i = 0; i < k; i++){
				((FxParticle) glow.recycle( FxParticle.class )).reset( x, y - 1 )
						.look( FxRandom.chance( 0.7f ) ? FxFrames.EMBER_1 : FxFrames.EMBER_2 ).ramp( Element.LAVA.ember, true )
						.vel( FxRandom.Float( -10f, 10f ), -FxRandom.Float( 30f, 50f ) ).gravity( 80f )
						.ground( y + 1, FxParticle.DIE, 0 ).life( 1f ).alpha( 1f, 0, 0.2f ).priority( FxBudget.P3 );
			}
		}
		if (FxBudget.keep( FxBudget.P3, x, y )){
			FxLight.glow( x, y - 1, POP_GLOW, POP_LIGHT, POP_LEVEL / FxLight.GLOW_CAP ).life( 0f, 0f, POP_FADE );
		}
	};

	// ------------------------------------------------------------------ heat

	private void heat( int[] cells, int n, float dt ){
		if ((heatIn -= dt) > 0f) return;
		heatIn = HEAT_EVERY;
		FxEmitter glow = Fx.glowAir();
		if (glow == null) return;
		int k = Math.max( 1, Math.round( n * HEAT_SHARE ) );
		int w = Dungeon.level.width();
		boolean[] water = Dungeon.level.water;
		for (int i = 0; i < k && Marks.room( Marks.HEAT ); i++){
			//a cell with lava north of it too: the ray rises over the lava, never over its bank
			int c = cells[FxRandom.Int( n )];
			if (c < w || !water[c - w]) continue;
			float x = cellX( c ) + FxRandom.IntRange( 3, 12 ), y = cellY( c ) + FxRandom.IntRange( 8, 13 );
			if (FxBudget.keep( FxBudget.P3, x, y )) heatRay( glow, x, y );
		}
	}

	/** A heat ray rising off the lava at (x, y): a broken streak of light (REFLECT's frames, the
	 *  kit's) wavering at 4 frames a second, added in FFE8C0 at 0.15, rising 4 px/s for 1.5 s. */
	static void heatRay( FxEmitter glow, float x, float y ){
		Marks.Mark m = Marks.makeIn( Marks.HEAT, glow, true, x, y - 7 );
		if (m != null) m.look( FxFrames.REFLECT, HEAT_FPS ).paint( HEAT ).life( HEAT_LIFE )
				.alpha( HEAT_ALPHA, HEAT_LIFE * 0.3f, HEAT_LIFE * 0.4f ).vel( 0, -HEAT_RISE ).snap()
				.priority( FxBudget.P3 );
	}

	// ------------------------------------------------------------------ the banks

	/** The banks' pools after a look over the lava in view (`x0..x1`, `y0..y1`, cells): the six
	 *  dry cells beside it nearest the hero lit, the rest put out. */
	void banks( Level level, int x0, int y0, int x1, int y1 ){
		int w = level.width();
		int hero = Dungeon.hero != null ? Dungeon.hero.pos : -1;
		if (hero < 0 || level.heroFOV == null) return;
		int hx = hero % w, hy = hero / w;
		//the nearest six, by a few picks over the view (no allocation)
		int[] best = picked;
		int nBest = 0;
		for (int y = Math.max( 1, y0 ); y <= Math.min( level.height() - 2, y1 ); y++){
			for (int x = Math.max( 1, x0 ); x <= Math.min( w - 2, x1 ); x++){
				int c = x + y * w;
				if (!bank( level, c )) continue;
				int d = Math.max( Math.abs( x - hx ), Math.abs( y - hy ) );
				//insert it among the nearest, keeping them sorted
				int at = nBest;
				while (at > 0 && pickedD[at - 1] > d) at--;
				if (at >= EDGES) continue;
				for (int i = Math.min( nBest, EDGES - 1 ); i > at; i--){
					best[i] = best[i - 1];
					pickedD[i] = pickedD[i - 1];
				}
				best[at] = c;
				pickedD[at] = d;
				if (nBest < EDGES) nBest++;
			}
		}
		//out with the ones no longer among them
		for (int i = 0; i < EDGES; i++){
			if (edgeCell[i] < 0 || contains( best, nBest, edgeCell[i] )) continue;
			putOut( i );
		}
		//in with the new
		for (int k = 0; k < nBest; k++){
			int c = best[k];
			if (contains( edgeCell, EDGES, c )) continue;
			for (int i = 0; i < EDGES; i++){
				if (edgeCell[i] >= 0) continue;
				light( level, i, c );
				break;
			}
		}
	}

	private final int[] picked = new int[EDGES], pickedD = new int[EDGES];

	/** Every light of the banks put out (the lava gone from view, another liquid shown). */
	void dark(){
		for (int i = 0; i < EDGES; i++) if (edgeCell[i] >= 0) putOut( i );
	}

	/** The banks lit now (tests). */
	int lit(){
		int n = 0;
		for (int c : edgeCell) if (c >= 0) n++;
		return n;
	}

	/** A dry cell beside the lava that the hero sees: open ground with lava to one side of it. */
	static boolean bank( Level level, int c ){
		int w = level.width();
		if (level.water[c] || !level.heroFOV[c] || (Terrain.flags[level.map[c]] & Terrain.SOLID) != 0) return false;
		return level.water[c - 1] || level.water[c + 1] || level.water[c - w] || level.water[c + w];
	}

	private void light( Level level, int i, int c ){
		//only into room under the kit's caps, as the fires' and torches' lights ask: a bank lit as
		//the hero walks along the lava never puts out a fire's or a torch's light, which would come
		//back when the bank went out and be put out again by the next (blinking); unlit, it is
		//asked for again on the next look
		if (!FxLight.room( 1, 1 )) return;
		int w = level.width();
		//a little toward the lava beside it
		float dx = level.water[c - 1] ? -4 : level.water[c + 1] ? 4 : 0;
		float dy = level.water[c - w] ? -4 : level.water[c + w] ? 4 : 0;
		float x = cellX( c ) + SIZE / 2f + dx, y = cellY( c ) + SIZE / 2f + dy;
		FxLight l = FxLight.pool( x, y, EDGE_SIZE, EDGE, EDGE_ALPHA / FxLight.POOL_CAP ).flicker( FxLight.BREATH ).persistent();
		edgeCell[i] = c;
		edgeLight[i] = l;
		edgeX[i] = x;
		edgeY[i] = y;
	}

	private void putOut( int i ){
		FxLight l = edgeLight[i];
		//still ours: a light the cap took from us may be another's by now
		if (l != null && l.alive && l.isPersistent() && l.centerX() == edgeX[i] && l.centerY() == edgeY[i]) l.putOut( 0.5f );
		edgeCell[i] = -1;
		edgeLight[i] = null;
	}

	private static boolean contains( int[] a, int n, int v ){
		for (int i = 0; i < n; i++) if (a[i] == v) return true;
		return false;
	}

	// ------------------------------------------------------------------ spits

	//when the spits in the air were made (the scene's clock)
	private static final float[] spitAt = new float[SPITS];
	private static int nextSpit;

	static {
		resetSpits();
	}

	/** A new scene: none of the last one's heat rays or spits. */
	static void reset(){
		resetSpits();
	}

	static void resetSpits(){
		for (int i = 0; i < SPITS; i++) spitAt[i] = -1000f;
		nextSpit = 0;
	}

	/** The spits in the air now. */
	static int spitting(){
		float now = Game.timeTotal;
		int n = 0;
		for (float t : spitAt) if (now - t < SPIT_LIFE && now >= t) n++;
		return n;
	}

	/**
	 * The lava spits at (x, y) (HallsLevel.Stream): a tongue of its flame leaps out of it and three
	 * sparks are thrown up on an arc to fall back; none past SPITS in the air. False when it made
	 * none. Render thread.
	 */
	public static boolean spit( float x, float y ){
		if (!WaterFX.onWater( x, y ) || spitting() >= SPITS) return false;
		FxEmitter glow = Fx.glowAir();
		if (glow == null || !FxBudget.keep( FxBudget.P2, x, y )) return false;
		spitAt[nextSpit] = Game.timeTotal;
		nextSpit = (nextSpit + 1) % SPITS;
		FxFactories.tongue( Element.LAVA ).emit( glow, 0, x, y );
		float lean = FxRandom.Float( -0.5f, 0.5f );
		for (int i = 0; i < 3; i++){
			float a = (float)(-Math.PI / 2) + lean + (i - 1) * 0.45f;
			((FxParticle) glow.recycle( FxParticle.class )).reset( x, y - 1 ).look( FxFrames.SPARK_2 ).orient()
					.slow( FxFrames.MOTE_1, 15f ).ramp( Element.LAVA.ramp, true ).polar( a, FxRandom.Float( 32f, 48f ) )
					.gravity( 120f ).ground( y + FxRandom.Float( 1f, 3f ), FxParticle.DIE, 0 ).life( 0.9f )
					.alpha( 1f, 0, 0.15f ).priority( FxBudget.P2 );
		}
		return true;
	}

	// ------------------------------------------------------------------ cells

	private static float cellX( int c ){
		return (c % Dungeon.level.width()) * SIZE;
	}

	private static float cellY( int c ){
		return (c / Dungeon.level.width()) * SIZE;
	}
}
