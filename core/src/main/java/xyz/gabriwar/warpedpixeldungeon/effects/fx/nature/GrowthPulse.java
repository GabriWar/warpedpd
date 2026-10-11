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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.nature;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.tiles.TerrainFeaturesTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;

/**
 * Grass growing, as the render side sees the terrain change (bare floor or embers to grass, grass
 * to high grass: Regrowth, growth water, the wand of regrowth, the gardens' growth, hallowed
 * ground; a co-op guest through the map's changes): a copy of the new tile in light, revealed from
 * its foot up in four steps (0.2 s at ALPHA, fading 0.3 s), two to four sprouts, and a time in
 * three for high grass a bud popping open.
 *
 * After a wand's cast (waveFrom, its callback's frame) the growths of the next WINDOW s wait by
 * their distance from the caster at SPEED px/s: a wave of light races down the cone over the new
 * grass. The grass itself is there at once: the map's layers take the new terrain on the
 * callback's frame, as the game sets it. At most QUEUE waiting and ALIVE lights at once (past
 * that, sprouts only). Render thread; visual only, its randomness FxRandom's.
 */
public final class GrowthPulse {

	private GrowthPulse(){}

	/** A wave's speed, px/s, and how long after it a growth still waits for it, s. */
	public static final float SPEED = 160f, WINDOW = 0.6f;
	/** The light's alpha (bright enough that its wave reads over grass already grown); the most
	 *  waiting, the most lights at once. */
	public static final float ALPHA = 0.55f;
	public static final int QUEUE = 64, ALIVE = 24;
	/** Sprouts a growth, and the share of high grass growths that pop a bud. */
	public static final int SPROUTS_MIN = 2, SPROUTS_MAX = 4;
	public static final float BUD_CHANCE = 0.3f;

	private static volatile float waveX, waveY, waveAt = -1e9f;

	private static final int[] qCell = new int[QUEUE], qTerrain = new int[QUEUE];
	private static final float[] qAt = new float[QUEUE];
	private static int queued;
	private static int alive;
	//growths played since the scene began (tests read it)
	private static int played;

	/** A cast spreads growth from this cell (its callback's frame). Any thread. */
	public static void waveFrom( int cell ){
		Level level = Dungeon.level;
		if (level == null || cell < 0) return;
		int w = level.width();
		waveX = (cell % w + 0.5f) * DungeonTilemap.SIZE;
		waveY = (cell / w + 0.5f) * DungeonTilemap.SIZE;
		waveAt = Game.timeTotal;
	}

	/** A new scene: nothing waiting. */
	public static synchronized void reset(){
		queued = 0;
		alive = 0;
		played = 0;
		waveAt = -1e9f;
	}

	/** Growths played since the scene began. */
	public static synchronized int played(){
		return played;
	}

	/** Growths waiting for their wave. */
	public static synchronized int waiting(){
		return queued;
	}

	/** Whether a change of terrain is grass growing. */
	public static boolean grew( int from, int to ){
		boolean bare = from == Terrain.EMPTY || from == Terrain.EMBERS || from == Terrain.EMPTY_DECO;
		boolean grass = from == Terrain.GRASS || from == Terrain.FURROWED_GRASS;
		return (bare && (to == Terrain.GRASS || to == Terrain.HIGH_GRASS)) || (grass && to == Terrain.HIGH_GRASS);
	}

	/** A cell's terrain changed (TerrainWatch, render thread): if grass grew where the hero sees,
	 *  its growth plays now or waits for its wave. */
	public static synchronized void changed( int cell, int from, int to ){
		if (!grew( from, to )) return;
		Level level = Dungeon.level;
		if (level == null || cell < 0 || cell >= level.length() || !level.heroFOV[cell]) return;
		float wait = 0;
		if (Game.timeTotal - waveAt <= WINDOW){
			int w = level.width();
			float dx = (cell % w + 0.5f) * DungeonTilemap.SIZE - waveX, dy = (cell / w + 0.5f) * DungeonTilemap.SIZE - waveY;
			wait = (float)Math.sqrt( dx * dx + dy * dy ) / SPEED;
		}
		if (wait <= 0){
			play( cell, to );
		} else if (queued < QUEUE){
			qCell[queued] = cell;
			qTerrain[queued] = to;
			qAt[queued] = Game.timeTotal + wait;
			queued++;
		}
	}

	/** Once a frame: the growths whose wave has reached them. */
	public static synchronized void tick(){
		float now = Game.timeTotal;
		for (int i = 0; i < queued; i++){
			if (qAt[i] > now) continue;
			int cell = qCell[i], to = qTerrain[i];
			queued--;
			qCell[i] = qCell[queued];
			qTerrain[i] = qTerrain[queued];
			qAt[i] = qAt[queued];
			i--;
			Level level = Dungeon.level;
			//still grass, and still seen
			if (level != null && cell < level.length() && level.map[cell] == to && level.heroFOV[cell]) play( cell, to );
		}
	}

	//the growth itself: its light running up the new tile, sprouts, perhaps a bud
	private static void play( int cell, int to ){
		played++;
		Level level = Dungeon.level;
		int w = level.width();
		float x = (cell % w) * DungeonTilemap.SIZE, y = (cell / w) * DungeonTilemap.SIZE;

		Group lights = GameScene.layer( GameScene.Layer.LIGHTS );
		if (lights != null && alive < ALIVE && FxBudget.keep( FxBudget.P2, x + 8, y + 8 )){
			Image src = TerrainFeaturesTilemap.tile( cell, to );
			if (src != null){
				alive++;
				lights.add( new TileCrop( src, x, y, TileCrop.REVEAL, ALPHA, true ).then( GrowthPulse::gone ) );
			}
		}

		Emitter floor = Nature.floor( null );
		if (floor != null) Sprout.scatter( floor, x, y, FxRandom.IntRange( SPROUTS_MIN, SPROUTS_MAX ) );
		if (to == Terrain.HIGH_GRASS && FxRandom.chance( BUD_CHANCE )) bud( x + FxRandom.IntRange( 3, 12 ), y + FxRandom.IntRange( 4, 10 ) );
	}

	private static synchronized void gone(){
		alive = Math.max( 0, alive - 1 );
	}

	/** A bud popping open: a petal of blossom thrown up a little, falling to lie and fade. */
	public static void bud( float x, float y ){
		Emitter air = Nature.air( null );
		if (air == null || !FxBudget.keep( FxBudget.P3, x, y )) return;
		Nature.make( air, x, y ).look( FxFrames.PETAL_B ).paint( Nature.BLOSSOM.step( FxRandom.Float() ) )
				.vel( FxRandom.Float( -8f, 8f ), -FxRandom.Float( 24f, 34f ) ).gravity( 120f )
				.ground( y + FxRandom.Float( 1f, 3f ), xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle.SETTLE, 0 )
				.life( 1.2f ).alpha( 1f, 0, 0.3f ).priority( FxBudget.P3 );
	}
}
