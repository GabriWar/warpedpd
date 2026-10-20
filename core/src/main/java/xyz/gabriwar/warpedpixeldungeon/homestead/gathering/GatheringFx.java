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

package xyz.gabriwar.warpedpixeldungeon.homestead.gathering;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.audio.WallBreak;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.Nature;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EarthParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.net.NetVisuals;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;

/**
 * What the gathering looks and sounds like (Gathering, TreeFall): an axe's stroke on a trunk, a
 * pick's or a hammer's on a boulder, a spadeful, the sickle through tall grass, a boulder breaking
 * and a felled tree coming down. Each is something the host's game did on a cell: shown on his own
 * screen when that level is on it, and sent to the co-op guests (NetVisuals.recordGather), who play
 * it from their own view of the level (play) - a guest hears the axe and sees the tree fall as the
 * host does. Visual and sound only: matter goes through the kit's emitters (GameScene.fxEmitter,
 * never relayed on their own: this event is what reaches the guest) and the kit's randomness. The
 * kinds are sent as numbers: add new ones at the end, never renumber. A stroke of a typed
 * Woodsman's axe or Quarry's tool (n = TYPED) adds one piece to its look: a bark chip flung higher
 * off the trunk, a steel spark off the stone or the earth; a typed tool breaking the world's rock
 * shows that spark alone (WALL_SPARK: the wall's own break already sounded).
 */
public final class GatheringFx {

	private GatheringFx(){}

	public static final int CHOP = 0, QUARRY = 1, DIG = 2, REAP = 3, BOULDER_BREAKS = 4, TREE_FALLS = 5,
			WALL_SPARK = 6;

	/** A CHOP, QUARRY or DIG stroke's n: struck with a typed Woodsman's axe or Quarry's tool. */
	public static final int TYPED = 1;

	/** The bark chip's wood, the spark's steel. */
	private static final int WOOD = 0xB89C6C, STEEL = 0xE8F0FF;

	/** The host's: a piece of the work done on a cell of a slice, shown here when the slice is on
	 *  screen and sent on to the guests (nothing sent off a host). */
	public static void show( OverworldLevel ow, int kind, int cell, int n, int m ){
		if (ow != Dungeon.level) return;
		NetVisuals.recordGather( kind, cell, n, m );
		if (Gathering.live( ow )) play( kind, cell, n, m );
	}

	/** A tree's fall, as sent (n): which way it falls, a step each way, and whether it lands on
	 *  the window (not off its rim). */
	public static int direction( int dx, int dy, boolean lands ){
		return (Integer.signum( dx ) + 1) + 3 * (Integer.signum( dy ) + 1) + (lands ? 9 : 0);
	}

	/** A tree's picture, as sent (m): the dressing's tile of its trunk (a pine's whole tile) and
	 *  of its crown above it, -1 for none. */
	public static int picture( int trunk, int top ){
		return trunk | ((top + 1) << 12);
	}

	/**
	 * Plays one on this screen: the host's own, or a guest's from what the host sent. `n` and `m`
	 * are the kind's numbers (a tree's direction and picture). Any thread: handed to the render
	 * thread. Nothing without a game scene.
	 */
	public static void play( final int kind, final int cell, final int n, final int m ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> play( kind, cell, n, m ) );
			return;
		}
		Level level = Dungeon.level;
		if (level == null || cell < 0 || cell >= level.length() || !(Game.scene() instanceof GameScene)) return;
		switch (kind){
			case CHOP:
				SpatialSound.play( Assets.Sounds.CHOP, cell, 1f, FxRandom.Float( 0.92f, 1.08f ) );
				burst( cell, LeafParticle.GENERAL, 2 );
				if (n == TYPED) chip( cell );
				break;
			case QUARRY:
				WallBreak.play( cell, 0.4f, FxRandom.Float( 1.1f, 1.3f ) );
				burst( cell, Speck.factory( Speck.ROCK ), 2 );
				if (n == TYPED) spark( cell );
				break;
			case DIG:
				SpatialSound.play( Assets.Sounds.DIG, cell );
				burst( cell, EarthParticle.SMALL, 3 );
				if (n == TYPED) spark( cell );
				break;
			case WALL_SPARK:
				spark( cell );
				break;
			case REAP:
				SpatialSound.play( Assets.Sounds.REAP, cell );
				burst( cell, LeafParticle.GENERAL, 3 );
				break;
			case BOULDER_BREAKS: {
				//a boulder is a smaller break than a wall: quieter, a little higher (as the pick's on a mine boulder)
				WallBreak.play( cell, 0.5f, 1.2f );
				burst( cell, Speck.factory( Speck.ROCK ), 4 );
				PointF p = DungeonTilemap.tileCenterToWorld( cell );
				FxRecipes.dust( p.x, p.y, 5 );
				break;
			}
			case TREE_FALLS: {
				int d = n % 9;
				TreeFall.show( level, cell, d % 3 - 1, d / 3 - 1, n >= 9, m & 0xFFF, (m >> 12) - 1 );
				break;
			}
		}
	}

	//a typed axe's bark chip: flung up off the trunk, higher than the leaves, back down to its foot
	private static void chip( int cell ){
		FxEmitter air = Fx.air();
		if (air == null || !FxBudget.keep( FxBudget.P2 )) return;
		PointF c = DungeonTilemap.tileCenterToWorld( cell );
		Nature.make( air, c.x + FxRandom.Float( -2f, 2f ), c.y )
				.look( FxFrames.BARK[FxRandom.Int( FxFrames.BARK.length )] ).paint( WOOD )
				.vel( FxRandom.Float( -14f, 14f ), -FxRandom.Float( 62f, 74f ) ).gravity( 160f )
				.ground( c.y + 6f, FxParticle.BOUNCE, 0.3f ).spin( 0.15f )
				.life( 0.9f ).alpha( 1f, 0, 0.25f ).priority( FxBudget.P2 );
	}

	//a typed pick's, hammer's or shovel's bite: one steel spark flicked up off the stone or the earth
	private static void spark( int cell ){
		FxEmitter glow = Fx.glowAir();
		if (glow == null || !FxBudget.keep( FxBudget.P2 )) return;
		PointF c = DungeonTilemap.tileCenterToWorld( cell );
		Nature.make( glow, c.x + FxRandom.Float( -2f, 2f ), c.y + 2f ).look( FxFrames.SPARK_2 ).paint( STEEL )
				.light( true ).orient().polar( (float)(-Math.PI / 2) + FxRandom.Float( -0.7f, 0.7f ), FxRandom.Float( 45f, 60f ) )
				.drag( 6f ).life( 0.16f ).alpha( 1f, 0, 0.08f ).priority( FxBudget.P2 );
	}

	/** A burst of matter over a cell, from the kit's emitter (this screen's alone). */
	static void burst( int cell, Emitter.Factory factory, int n ){
		FxEmitter e = GameScene.fxEmitter();
		if (e == null) return;
		PointF p = DungeonTilemap.tileToWorld( cell );
		e.pos( p.x, p.y, DungeonTilemap.SIZE, DungeonTilemap.SIZE );
		e.burst( factory, n );
	}
}
