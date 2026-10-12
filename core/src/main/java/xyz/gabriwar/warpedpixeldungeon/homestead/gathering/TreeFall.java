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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;

/**
 * A felled tree coming down (Gathering.fell). The game's side: it falls away from the hero, onto
 * the cell beyond the trunk, and whoever stands there takes a third of the health he has left. The
 * screen's: the tree as the world dressed it (its oak crown and trunk for the season, or its pine)
 * tips over from its foot, slowly and then fast, and crashes down in a burst of leaves and dust, a
 * short shake when the hero is close; then it fades, and the stump stays. The tree's long groan,
 * crack and crash (TREE_FALL) is heard from where it stood.
 */
public final class TreeFall {

	private TreeFall(){}

	/** How long the tree takes to come down, seconds, and to fade once it lies there. */
	public static final float FALL_TIME = 0.6f, FADE_TIME = 0.6f;
	/** The hero shakes with the ground when he is this close to where it lands. */
	public static final int SHAKE_RANGE = 3;

	/** Whoever stands where the tree lands loses this share of the health he has left. */
	public static int damageTo( Char ch ){
		return ch.HP / 3;
	}

	/**
	 * The tree on `cell` (already a stump in the map) falls toward `landing`: the cell beyond it
	 * on the line from the feller. Whoever stands there is struck; the fall is shown and heard,
	 * on the host's screen and the guests' (GatheringFx).
	 */
	static void fall( OverworldLevel ow, int cell, int landing ){
		int w = ow.width();
		int dx = landing % w - cell % w, dy = landing / w - cell / w;
		boolean onMap = landing >= 0 && landing < ow.length() && Math.abs( dx ) <= 1 && Math.abs( dy ) <= 1;
		if (onMap){
			Char ch = Actor.findChar( landing );
			if (ch != null && ch.isAlive()){
				int dmg = damageTo( ch );
				if (dmg > 0) ch.damage( dmg, TreeFall.class );
			}
		}
		int[] tiles = picture( ow, cell );
		GatheringFx.show( ow, GatheringFx.TREE_FALLS, cell, GatheringFx.direction( dx, dy, onMap ),
				GatheringFx.picture( tiles[0], tiles[1] ) );
	}

	/** The felled tree's picture as the world dressed it: the oak's trunk and crown for the season,
	 *  or the pine's one tile (WindowGenerator.dress: the same hash, the same pick) and no crown. */
	static int[] picture( OverworldLevel ow, int cell ){
		if (ow.map[cell] == Terrain.STUMP_OAK){
			switch (ow.fieldSeason()){
				case AUTUMN: return new int[]{ OverworldDress.OAK_AUTUMN_TRUNK, OverworldDress.OAK_AUTUMN_TOP };
				case WINTER: return new int[]{ OverworldDress.OAK_WINTER_TRUNK, OverworldDress.OAK_WINTER_TOP };
				default:     return new int[]{ OverworldDress.OAK_SUMMER_TRUNK, OverworldDress.OAK_SUMMER_TOP };
			}
		}
		int[] set = ow.frozenAt( cell ) ? OverworldDress.WINTER_PINES : OverworldDress.SUMMER_PINES;
		long h = WindowGenerator.dressHash( ow.worldSeed(), ow.worldX + cell % ow.width(), ow.worldY + cell / ow.width(), 0x7EEE5L );
		return new int[]{ set[(int) Math.floorMod( h, set.length )], -1 };
	}

	/**
	 * The fall on this screen (GatheringFx.play): the long groan, crack and crash heard from where
	 * the tree stood; when the hero sees the tree or where it lands, a burst of leaves and the tree's
	 * picture (trunk tile, crown tile or -1) tipping over the way given (dx, dy a step each).
	 */
	static void show( Level level, int cell, int dx, int dy, boolean lands, int trunk, int top ){
		SpatialSound.play( Assets.Sounds.TREE_FALL, cell );
		int landing = cell + dx + dy * level.width();
		boolean seen = level.heroFOV != null && (level.heroFOV[cell]
				|| (lands && landing >= 0 && landing < level.length() && level.heroFOV[landing]));
		if (!seen) return;
		GatheringFx.burst( cell, LeafParticle.GENERAL, 6 );
		Group layer = GameScene.layer( GameScene.Layer.EFFECTS );
		if (layer != null) layer.add( new Falling( level.width(), cell, dx, dy, lands, trunk, top ) );
	}

	/** The felled tree's picture, falling: the oak's crown and trunk, or the pine. */
	static final class Falling extends Group {

		private final Image[] parts;
		private final float target;
		private final boolean away;
		//which way it falls, and whether it lands on the window (not off its rim)
		private final int dx, dy;
		private final boolean lands;
		private float time;
		private boolean landed;

		Falling( int w, int cell, int dx, int dy, boolean lands, int trunk, int top ){
			this.dx = dx;
			this.dy = dy;
			this.lands = lands;
			float x = (cell % w) * DungeonTilemap.SIZE, y = (cell / w) * DungeonTilemap.SIZE;
			int s = DungeonTilemap.SIZE;
			parts = top >= 0
					? new Image[]{ tile( trunk, x, y, s / 2f, s ), tile( top, x, y - s, s / 2f, 2 * s ) }
					: new Image[]{ tile( trunk, x, y, s / 2f, s ) };
			for (Image i : parts) add( i );
			//straight away from the viewer it tips back out of sight; any other way it swings down
			//about its foot: east a quarter turn clockwise, south a half
			away = dx == 0 && dy < 0;
			target = away ? 0f : (float) Math.toDegrees( Math.atan2( dx, -dy ) );
		}

		//a dressing tile at (x, y), turning about the point (ox, oy) of itself: the trunk's foot
		private static Image tile( int id, float x, float y, float ox, float oy ){
			int s = DungeonTilemap.SIZE;
			Image i = new Image( Assets.Environment.OVERWORLD_DRESS, (id % 16) * s, (id / 16) * s, s, s );
			i.x = x;
			i.y = y;
			i.origin.set( ox, oy );
			return i;
		}

		@Override
		public void update(){
			super.update();
			time += Game.elapsed;
			if (!landed){
				//slow at first, then fast, as a trunk goes once it is past its balance
				float p = Math.min( 1f, time / FALL_TIME );
				float e = p * p;
				for (Image i : parts){
					if (away) i.scale.y = 1f - 0.45f * e;
					else i.angle = target * e;
				}
				if (p >= 1f){
					landed = true;
					time = 0f;
					crash();
				}
			} else {
				float a = 1f - Math.min( 1f, time / FADE_TIME );
				for (Image i : parts) i.alpha( a );
				if (a <= 0f) killAndErase();
			}
		}

		//the crown hits the ground: leaves and dust where it lies, the ground shaking under a hero
		//close by. found from the trunk's picture, which a move of the world's window slides along
		//with everything else, so the cell is the one it lies on now
		private void crash(){
			if (!lands) return;
			int s = DungeonTilemap.SIZE;
			float x = parts[0].x + s / 2f + dx * s, y = parts[0].y + s / 2f + dy * s;
			int landing = Fx.cellAt( x, y );
			if (landing == -1 || Dungeon.level == null || landing >= Dungeon.level.length()) return;
			GatheringFx.burst( landing, LeafParticle.GENERAL, 10 );
			FxRecipes.dust( x, y, 8 );
			if (Dungeon.hero != null && Dungeon.level.distance( Dungeon.hero.pos, landing ) <= SHAKE_RANGE){
				Fx.shake( 2f, 0.25f );
			}
		}
	}
}
