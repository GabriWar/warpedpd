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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.fire;

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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;

/**
 * A burning rift's crack (levels/overworld/CaveSites), seen: the molten line glowing on the floor
 * of every crack cell near the hero (a breathing pool of lava light, at most CELLS, each let go
 * KEEP s after the rift last named it), the turn before a flare (the cell's glow swelling to its
 * height over 0.8 s, flickering, sparks bubbling up off it), and the flare (tall tongues leaping
 * 30 px, a ring of heat 13 to 31 px and a glow). Visual only, from any thread (handed to the render
 * thread), and sent on to co-op guests (FireNet): the rift's heat and its harm are the game's.
 */
public class RiftFx extends Group {

	public static final int CELLS = 20, COLOR = 0xFF7A20, RING = 0xFF9030;
	public static final float LEVEL = 0.3f, KEEP = 3f, SWELL = 0.8f, FLARE_GLOW = 0.7f;

	private static RiftFx field;

	private final int[] cell = new int[CELLS];
	private final float[] seenAt = new float[CELLS];
	private final FloorGlow[] glow = new FloorGlow[CELLS];
	private float clock;

	/** The crack cells the rift warms this turn: their glow kept (or lit). */
	public static void glow( int[] cells ){
		FireNet.send( FireNet.RIFT_GLOW, -1, 0, 0, cells );
		keepGlowing( cells );
	}

	static void keepGlowing( final int[] cells ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> keepGlowing( cells ) );
			return;
		}
		RiftFx f = field();
		if (f == null) return;
		for (int c : cells) f.keep( c );
	}

	/** A flare announced on a cell: its glow swells, sparks bubble. */
	public static void telegraph( int c ){
		FireNet.send( FireNet.RIFT_TELEGRAPH, c, 0, 0, null );
		swell( c );
	}

	static void swell( final int c ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> swell( c ) );
			return;
		}
		if (!seen( c )) return;
		float x = x( c ), y = y( c );
		Group lights = GameScene.layer( GameScene.Layer.LIGHTS );
		if (lights != null){
			LightFrame swell = new LightFrame();
			swell.setup( FxFrames.Light.POOL_23, x, y + 3, COLOR, 0.5f ).life( SWELL, 0.3f, 0.3f ).breathe( 3f, 0.1f ).seenAt( x, y );
			lights.add( swell );
		}
		FxEmitter e = GameScene.lightEmitter();
		if (e != null){
			e.pos( x - 5, y - 2, 10, 6 );
			e.start( FxFactories.spark( Element.LAVA ), 0.5f, 3 );
		}
	}

	/** The flare bursting out of a cell. */
	public static void flare( int c ){
		FireNet.send( FireNet.RIFT_FLARE, c, 0, 0, null );
		burst( c );
	}

	static void burst( final int c ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> burst( c ) );
			return;
		}
		if (!seen( c )) return;
		float x = x( c ), y = y( c );
		FxEmitter glow = Fx.glowAir();
		if (glow != null){
			for (int i = 0; i < 6; i++){
				if (!FxBudget.keep( FxBudget.P1, x, y )) continue;
				Flames.tongue( Flames.make( glow, FxParticle.class, x + FxRandom.Float( -5f, 5f ), y + FxRandom.Float( 0f, 4f ) ),
						Element.LAVA, Flames.L ).vel( FxRandom.Float( -4f, 4f ), -FxRandom.Float( 30f, 40f ) ).gravity( -20f ).source( x, y );
			}
		}
		FxRing.ground( GameScene.layer( GameScene.Layer.FLOOR ), x, y + 4, 13, 31, 0.3f, RING, 0.6f, true );
		FxLight.glow( x, y - 4, 31, COLOR, FLARE_GLOW ).life( 0, 0.1f, 0.4f );
	}

	private static RiftFx field(){
		Group floor = GameScene.layer( GameScene.Layer.FLOOR );
		if (floor == null) return null;
		if (field == null || field.parent != floor || !field.alive){
			field = new RiftFx();
			for (int i = 0; i < CELLS; i++) field.cell[i] = -1;
			floor.add( field );
		}
		return field;
	}

	private void keep( int c ){
		int free = -1;
		for (int i = 0; i < CELLS; i++){
			if (cell[i] == c){
				seenAt[i] = clock;
				return;
			}
			if (cell[i] == -1 && free == -1) free = i;
		}
		if (free == -1 || Dungeon.level == null) return;
		cell[free] = c;
		seenAt[free] = clock;
		FloorGlow g = (FloorGlow) recycle( FloorGlow.class );
		g.reset( x( c ), y( c ) + 2, FxFrames.Light.POOL_15, COLOR, LEVEL, FxLight.BREATH );
		glow[free] = g;
	}

	@Override
	public void update(){
		super.update();
		clock += Game.elapsed;
		Level level = Dungeon.level;
		for (int i = 0; i < CELLS; i++){
			if (cell[i] == -1) continue;
			boolean seen = level != null && cell[i] < level.length() && level.heroFOV[cell[i]];
			if (clock - seenAt[i] > KEEP || level == null){
				if (glow[i] != null) glow[i].out();
				glow[i] = null;
				cell[i] = -1;
			} else if (glow[i] != null){
				glow[i].level( seen ? LEVEL : 0 );
			}
		}
	}

	private static boolean seen( int c ){
		return Dungeon.level != null && c >= 0 && c < Dungeon.level.length() && Dungeon.level.heroFOV[c];
	}

	private static float x( int c ){
		return (c % Dungeon.level.width() + 0.5f) * DungeonTilemap.SIZE;
	}

	private static float y( int c ){
		return (c / Dungeon.level.width() + 0.5f) * DungeonTilemap.SIZE;
	}
}
