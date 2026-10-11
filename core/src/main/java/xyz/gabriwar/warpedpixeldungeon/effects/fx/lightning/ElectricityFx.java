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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxCurves;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;
import com.watabou.utils.RectF;

import java.util.Arrays;

/**
 * Electricity's look (actors/blobs/Electricity, its emitter's factory): every tick (INTERVAL s) for
 * each cell of it the hero sees, a spark or two of the lightning's blues (ChargeSpark: a still
 * crackle, or a short hop), SPARKS of them at most at once; on the water, the charge lights the
 * cells it runs through (a flickering wash of WASH_COLOR at WASH_ALPHA, +-WASH_FLICKER at under
 * 3 Hz, the WASHES cells nearest the hero), faint one-leg arcs crawl between neighbouring charged
 * cells (one for every CRAWL_PER cells every CRAWL_EVERY s, CRAWLS at once), and all of it flares
 * BOOST brighter for BOOST_FOR s when the charge rises (its volume, as the render side sees it).
 *
 * The factory is the blob's own (one a blob), its randomness the effects' own (FxRandom): it draws
 * none of the game's.
 */
public final class ElectricityFx extends Emitter.Factory implements FxBudget.Prioritized {

	/** How often the emitter ticks, s (one draw of the game's Random to start it, as ever). */
	public static final float INTERVAL = 0.2f;
	/** Sparks at once, all of them; a share of them hop, the rest crackle in place. */
	public static final int SPARKS = 150;
	static final float HOPS = 0.4f;
	/** The washes: how many, colour, alpha, flicker, its rate (noise steps a second), how long one
	 *  holds after its cell was last ticked, how long it fades. */
	public static final int WASHES = 24, WASH_COLOR = 0x9CC8FF;
	public static final float WASH_ALPHA = 0.18f, WASH_FLICKER = 0.2f, WASH_RATE = 2.5f, HOLD = 0.45f, FADE = 0.2f;
	/** The crawls between charged cells. */
	public static final int CRAWLS = 6, CRAWL_PER = 4;
	public static final float CRAWL_EVERY = 0.25f;
	/** A flare when the charge rises. */
	public static final float BOOST = 0.3f, BOOST_FOR = 0.1f;

	private final Blob blob;
	private int lastVolume = Integer.MIN_VALUE;

	public ElectricityFx( Blob blob ){
		this.blob = blob;
	}

	@Override
	public void emit( Emitter emitter, int index, float x, float y ){
		BlobEmitter.Cell c = BlobEmitter.cell();
		Field field = Field.get();
		//it runs down a little every turn; it flares only when charged again (or spreading anew)
		if (blob.volume != lastVolume){
			if (lastVolume != Integer.MIN_VALUE && blob.volume > lastVolume && field != null) field.boost();
			lastVolume = blob.volume;
		}
		Level level = Dungeon.level;
		if (field != null && level != null && c.cell >= 0 && c.cell < level.length() && level.water[c.cell]){
			field.mark( c.cell );
		}
		int n = FxRandom.chance( 0.5f ) ? 2 : 1;
		for (int i = 0; i < n; i++){
			if (ChargeSpark.count() >= SPARKS) return;
			float px = i == 0 ? x : x + FxRandom.Float( -6f, 6f ), py = i == 0 ? y : y + FxRandom.Float( -6f, 6f );
			if (!FxBudget.keep( FxBudget.P2, px, py )) continue;
			((ChargeSpark) emitter.recycle( ChargeSpark.class )).charge( px, py );
		}
	}

	@Override
	public boolean lightMode(){
		return true;
	}

	//every cell it ticks is kept (its washes hold by it): its sparks are thinned themselves
	@Override
	public int priority(){
		return FxBudget.P1;
	}

	@Override
	public float density( int cur ){
		return 1f;
	}

	/** A new scene: none of the last one's. */
	public static void sceneCreated(){
		Field.current = null;
		ChargeSpark.reset();
	}

	// ------------------------------------------------------------------ its sparks

	/** A spark of the charge: a still crackle, or a short hop that bounces once; counted. */
	public static class ChargeSpark extends SparkParticle {

		private static int count;
		private boolean counted;

		public static synchronized int count(){
			return count;
		}

		static synchronized void reset(){
			count = 0;
		}

		void charge( float x, float y ){
			if (FxRandom.chance( HOPS )){
				resetElectric( x, y );
				polar( FxRandom.Float( (float)-Math.PI, 0f ), FxRandom.Float( 15f, 35f ) )
						.ground( y + FxRandom.Float( 2f, 4f ), FxParticle.BOUNCE, 0.35f ).priority( FxBudget.P2 );
			} else {
				resetStatic( x, y );
			}
		}

		@Override
		public void revive(){
			super.revive();
			if (!counted){
				counted = true;
				synchronized (ChargeSpark.class){
					count++;
				}
			}
		}

		@Override
		public void kill(){
			super.kill();
			if (counted){
				counted = false;
				synchronized (ChargeSpark.class){
					if (count > 0) count--;
				}
			}
		}
	}

	// ------------------------------------------------------------------ the charged water

	/**
	 * The charged water's light, one for the scene (on the water's surface, masked by the shore,
	 * added): its washes, kept to the WASHES cells nearest the hero, each flickering on its own,
	 * held while its cell is ticked and fading after, and the crawls between them.
	 */
	public static class Field extends Group {

		static Field current;

		//where its crawls go (the light layer), and where it lies (the water's surface): a test's own
		private final Group crawlsInto;
		private final ChargeWash[] washes = new ChargeWash[WASHES];
		private final int[] cells = new int[WASHES];
		private final float[] marked = new float[WASHES];
		private int count;
		private final Lightning[] crawls = new Lightning[CRAWLS];
		private float clock, boostLeft, crawlIn = CRAWL_EVERY;
		private final int[] candidates = new int[WASHES];

		Field( Group crawlsInto ){
			this.crawlsInto = crawlsInto;
			Arrays.fill( cells, -1 );
		}

		/** The scene's, made on the water's surface the first time it is asked for; null off the
		 *  game's scene. Render thread. */
		static Field get(){
			if (current != null && current.exists && current.parent != null) return current;
			Group surface = GameScene.layer( GameScene.Layer.SURFACE );
			Group lights = GameScene.layer( GameScene.Layer.LIGHTS );
			if (surface == null || lights == null) return null;
			current = new Field( lights );
			surface.add( current );
			return current;
		}

		/** A field of its own on a group, its crawls into another: a test's. */
		public static Field forTests( Group on, Group crawlsInto ){
			Field f = new Field( crawlsInto );
			on.add( f );
			return f;
		}

		/** Its washes now. */
		public int washes(){
			return count;
		}

		/** Its crawls alive now. */
		public int crawls(){
			int n = 0;
			for (Lightning l : crawls) if (l != null && l.alive && l.parent != null) n++;
			return n;
		}

		/** The charge rose: its light flares. */
		public void boost(){
			boostLeft = BOOST_FOR;
		}

		/** Whether its light is flaring now. */
		boolean flaring(){
			return boostLeft > 0;
		}

		/** A charged water cell was ticked: its wash lit (or kept), the WASHES nearest the hero. */
		public void mark( int cell ){
			for (int i = 0; i < count; i++){
				if (cells[i] == cell){
					marked[i] = clock;
					return;
				}
			}
			int slot = -1;
			if (count < WASHES){
				slot = count++;
			} else {
				int far = farthest();
				if (far >= 0 && distance( cell ) < distance( cells[far] )) slot = far;
			}
			if (slot < 0) return;
			Level level = Dungeon.level;
			int w = level.width();
			if (washes[slot] == null){
				washes[slot] = new ChargeWash();
				add( washes[slot] );
			}
			cells[slot] = cell;
			marked[slot] = clock;
			washes[slot].light( (cell % w) * DungeonTilemap.SIZE, (cell / w) * DungeonTilemap.SIZE, FxRandom.Int( 1 << 20 ) );
		}

		private int farthest(){
			int best = -1;
			for (int i = 0; i < count; i++){
				if (best < 0 || distance( cells[i] ) > distance( cells[best] )) best = i;
			}
			return best;
		}

		private static int distance( int cell ){
			Level level = Dungeon.level;
			if (level == null || Dungeon.hero == null) return 0;
			return level.distance( Dungeon.hero.pos, cell );
		}

		@Override
		public void update(){
			super.update();
			if (Emitter.freezeEmitters && Game.timeTotal > 1) return;
			float dt = Game.elapsed;
			clock += dt;
			if (boostLeft > 0) boostLeft -= dt;
			float boost = 1f + (boostLeft > 0 ? BOOST * boostLeft / BOOST_FOR : 0f);
			for (int i = 0; i < count; i++){
				ChargeWash w = washes[i];
				float idle = clock - marked[i];
				if (idle > HOLD + FADE){
					//its cell is no longer charged: the last wash takes its place
					count--;
					cells[i] = cells[count];
					marked[i] = marked[count];
					washes[i] = washes[count];
					washes[count] = w;
					cells[count] = -1;
					w.visible = false;
					i--;
					continue;
				}
				float fade = idle > HOLD ? 1f - (idle - HOLD) / FADE : 1f;
				w.show( clock, boost * fade );
			}
			if ((crawlIn -= dt) <= 0){
				crawlIn = CRAWL_EVERY;
				crawl();
			}
		}

		//faint one-leg arcs between neighbouring charged cells: one for every CRAWL_PER cells, at
		//most CRAWLS alive, made render-side from the effects' own numbers
		private void crawl(){
			Level level = Dungeon.level;
			if (level == null || crawlsInto == null || count < 2) return;
			int w = level.width(), n = 0;
			for (int i = 0; i < count; i++){
				if (clock - marked[i] <= HOLD) candidates[n++] = cells[i];
			}
			int want = Math.min( n / CRAWL_PER, CRAWLS - crawls() );
			for (int k = 0, tries = 0; k < want && tries < 3 * want; tries++){
				int a = candidates[FxRandom.Int( n )];
				int b = neighbour( a, w, n );
				if (b < 0) continue;
				PointF from = DungeonTilemap.tileCenterToWorld( a ), to = DungeonTilemap.tileCenterToWorld( b );
				Lightning l = Lightning.visual( Arrays.asList( Lightning.Arc.visual( from, to ).crawl( 0 ) ), null ).noGlow();
				for (int s = 0; s < CRAWLS; s++){
					if (crawls[s] == null || !crawls[s].alive || crawls[s].parent == null){
						crawls[s] = l;
						break;
					}
				}
				crawlsInto.add( l );
				k++;
			}
		}

		//a charged cell next to `a` (four ways), from a random start; -1 for none
		private int neighbour( int a, int w, int n ){
			int start = FxRandom.Int( 4 );
			for (int i = 0; i < 4; i++){
				int d = (start + i) % 4;
				int b = a + (d == 0 ? 1 : d == 1 ? -1 : d == 2 ? w : -w);
				for (int j = 0; j < n; j++){
					if (candidates[j] == b) return b;
				}
			}
			return -1;
		}

		@Override
		public void draw(){
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}

	/** A charged water cell's wash: the light sheet's soft square, flickering on its own. */
	public static class ChargeWash extends Image {

		private int seed;
		private final RectF uv = new RectF();

		public ChargeWash(){
			super( Assets.Effects.FX_LIGHT );
			int[] f = FxFrames.Light.WASH_16;
			float tw = texture.width, th = texture.height;
			uv.set( f[0] / tw, f[1] / th, (f[0] + f[2]) / tw, (f[1] + f[3]) / th );
			frame( uv );
			hardlight( WASH_COLOR );
		}

		void light( float x, float y, int seed ){
			this.x = x;
			this.y = y;
			this.seed = seed;
			am = 0;
			visible = false;
		}

		//its flicker: noise under 3 Hz, +-WASH_FLICKER, times what it is given
		void show( float clock, float k ){
			am = Math.min( 1f, WASH_ALPHA * (1f + WASH_FLICKER * FxCurves.noise1( seed, clock * WASH_RATE )) * k );
			visible = am > 0.004f;
		}
	}
}
