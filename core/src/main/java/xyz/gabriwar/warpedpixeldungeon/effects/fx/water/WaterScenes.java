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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes;
import xyz.gabriwar.warpedpixeldungeon.effects.CritterSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.DungeonCritterSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.SliceCritterSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.DripParticle;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.Source;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldCritters;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Visual;

import java.util.ArrayList;
import java.util.List;

/**
 * The water's debug scenes (debug menu > Scenes, or -Pscene=<id>), each taking the hero where the
 * water has something to show and keeping it lively while he stays:
 *
 *  - water-life-sewers: the second sewer floor, by a drain pipe over its pool (trickling or
 *    pouring as it sounds), two striders and two fish kept on the pool, a drop from the roof every
 *    1.2 s somewhere on the water in view.
 *  - water-life-lake: the surface by a meadow's river near the world's origin, held at day: the
 *    fish shadows under the open water, a leap every 2-4 s, the banks' foam.
 *  - water-life-rain: the same water in a rain of 0.5 (the rings and, past 0.4, the pocks).
 *  - water-life-caves: the twelfth floor, by a pool, three fish kept gliding under it.
 *  - water-life-lava: the twenty-second floor, on the lava's bank: its glow, crust, bubbles, heat,
 *    the banks' light and its spits.
 *
 * A scene that must travel first is applied again once the new floor's scene stands (the water's
 * module hears every scene made: arrived()). What a scene holds (the hour, the rain) is let go
 * when its floor's scene goes.
 */
public final class WaterScenes {

	private WaterScenes(){}

	private static final float SIZE = DungeonTilemap.SIZE;

	/** The water's scenes, in menu order. */
	public static void all( List<DebugScenes.Scene> out ){
		out.add( new Sewers() );
		out.add( new Lake( false ) );
		out.add( new Lake( true ) );
		out.add( new Depth( "water-life-caves", "Water: a cave pool and its fish gliding under it", 12, Depth.CAVES ) );
		out.add( new Depth( "water-life-lava", "Water: the halls' lava, its bank lit, bubbling and spitting", 22, Depth.LAVA ) );
	}

	//the scene on its way to its floor, finished once that floor's scene stands
	private static Travel pending;

	/** A game scene was made (WaterLifeFx.sceneCreated): a scene on its way here is finished on its
	 *  first frame. */
	public static void arrived( GameScene scene ){
		final Travel t = pending;
		if (t == null) return;
		pending = null;
		scene.add( new Gizmo(){
			@Override
			public void update(){
				killAndErase();
				if (Dungeon.hero != null && Dungeon.level != null) t.arrive( Dungeon.hero );
			}
		} );
	}

	//a scene that may have to travel to its ground first
	private abstract static class Travel implements DebugScenes.Scene {
		final String id, title;

		Travel( String id, String title ){
			this.id = id;
			this.title = title;
		}

		@Override
		public String id(){
			return id;
		}

		@Override
		public String title(){
			return title;
		}

		//to a floor of the dungeon, the scene finished there
		void toDepth( int depth, int branch ){
			pending = this;
			InterlevelScene.mode = InterlevelScene.Mode.RETURN;
			InterlevelScene.returnDepth = depth;
			InterlevelScene.returnBranch = branch;
			InterlevelScene.returnPos = -1;
			Game.switchScene( InterlevelScene.class );
		}

		abstract void arrive( Hero hero );
	}

	// ------------------------------------------------------------------ the scenes

	//the sewers' second floor by a pipe
	private static final class Sewers extends Travel {
		Sewers(){
			super( "water-life-sewers", "Water: the sewers' pipes, striders, fish and drops" );
		}

		@Override
		public void apply( Hero hero ){
			if (Dungeon.depth != 2 || Dungeon.branch != 0 || Dungeon.level instanceof OverworldLevel) toDepth( 2, 0 );
			else arrive( hero );
		}

		@Override
		void arrive( Hero hero ){
			Level level = Dungeon.level;
			int w = level.width(), pipe = -1, pours = 0, trickles = 0;
			int best = Integer.MAX_VALUE;
			for (int c = w; c < level.length() - w; c++){
				if (level.map[c] != Terrain.WALL_DECO || !level.water[c + w]) continue;
				if (Source.pours( c )) pours++;
				else trickles++;
				int d = dist( level, c, hero.pos );
				if (d < best){
					best = d;
					pipe = c;
				}
			}
			if (pipe < 0){
				GLog.w( "Scene: this floor has no pipe over its water" );
			} else {
				settle( hero, level, pipe + w, 2, 4 );
				GLog.i( "Scene: " + pours + " pipes pour and " + trickles + " trickle on this floor; the nearest "
						+ (Source.pours( pipe ) ? "pours" : "trickles") );
			}
			Game.scene().add( new Keeper( 2, 2, 1.2f, 0f, null ) );
			GLog.i( "Scene: two striders and two fish kept on the water, a drop from the roof every 1.2 s" );
		}
	}

	//the surface by a river, by day, in the rain or not
	private static final class Lake extends Travel {
		final boolean rain;

		Lake( boolean rain ){
			super( rain ? "water-life-rain" : "water-life-lake",
					rain ? "Water: rain of 0.5 on the surface's river" : "Water: the surface's river by day, shadows and leaps" );
			this.rain = rain;
		}

		@Override
		public void apply( Hero hero ){
			final long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			final float shift = WorldModel.calendarShift();
			GLog.i( "Scene: looking for a meadow by a river..." );
			Thread t = new Thread( () -> {
				final int[] spot = OverworldCritters.findCritterGround( seed, shift );
				Game.runOnRenderThread( () -> {
					if (spot == null){
						GLog.w( "Scene: no river near the origin in this world" );
						return;
					}
					pending = this;
					OverworldLevel.arriveAt( spot[0], spot[1] );
					OverworldLevel.travelToSurface();
				} );
			}, "water-scene-search" );
			t.setDaemon( true );
			t.start();
		}

		@Override
		void arrive( Hero hero ){
			Level level = Dungeon.level;
			if (!(level instanceof OverworldLevel)){
				GLog.w( "Scene: could not reach the surface" );
				return;
			}
			int to = byOpenWater( level, hero.pos, 14 );
			if (to >= 0 && to != hero.pos) ScrollOfTeleportation.appear( hero, to );
			Game.scene().add( new Keeper( 0, 0, 0f, 3f, rain ? PrecipType.RAIN : null ) );
			GLog.i( "Scene: " + (rain ? "rain of 0.5 held: rings below 0.4, the pocks above it" : "held at day")
					+ "; a fish leaps every 2-4 s; the fish shadows come with twelve water cells in view" );
		}
	}

	//a floor of the caves or the halls, by its water
	private static final class Depth extends Travel {
		static final int CAVES = 0, LAVA = 1;
		final int depth, kind;

		Depth( String id, String title, int depth, int kind ){
			super( id, title );
			this.depth = depth;
			this.kind = kind;
		}

		@Override
		public void apply( Hero hero ){
			if (Dungeon.depth != depth || Dungeon.branch != 0 || Dungeon.level instanceof OverworldLevel) toDepth( depth, 0 );
			else arrive( hero );
		}

		@Override
		void arrive( Hero hero ){
			Level level = Dungeon.level;
			int water = nearest( level, hero.pos, kind == CAVES ? 3 : 1 );
			if (water < 0){
				GLog.w( "Scene: no " + (kind == CAVES ? "pool" : "lava") + " on this floor" );
				return;
			}
			settle( hero, level, water, 1, 3 );
			if (kind == CAVES){
				Game.scene().add( new Keeper( 0, 3, 0f, 0f, null ) );
				GLog.i( "Scene: three fish kept gliding under the pool: walk to it, drop something in it" );
			} else {
				GLog.i( "Scene: the lava's glow, crust, bubbles, heat rays and spits; the six banks nearest you lit" );
			}
		}
	}

	// ------------------------------------------------------------------ places

	private static int dist( Level level, int a, int b ){
		int w = level.width();
		return Math.max( Math.abs( a % w - b % w ), Math.abs( a / w - b / w ) );
	}

	//the hero set down on dry open ground `near`..`far` cells from a cell, nearest first
	private static void settle( Hero hero, Level level, int at, int near, int far ){
		int w = level.width(), best = -1, bestD = Integer.MAX_VALUE;
		for (int dy = -far; dy <= far; dy++){
			for (int dx = -far; dx <= far; dx++){
				int d = Math.max( Math.abs( dx ), Math.abs( dy ) );
				if (d < near) continue;
				int x = at % w + dx, y = at / w + dy;
				if (x <= 0 || y <= 0 || x >= w - 1 || y >= level.height() - 1) continue;
				int c = x + y * w;
				if (!level.passable[c] || level.water[c] || level.pit[c] || Actor.findChar( c ) != null) continue;
				if (d < bestD){
					bestD = d;
					best = c;
				}
			}
		}
		if (best >= 0 && best != hero.pos) ScrollOfTeleportation.appear( hero, best );
	}

	//the water cell nearest `from` with at least `around` water neighbours (of eight)
	private static int nearest( Level level, int from, int around ){
		int w = level.width(), best = -1, bestD = Integer.MAX_VALUE;
		for (int c = w + 1; c < level.length() - w - 1; c++){
			if (!level.water[c]) continue;
			int n = 0;
			for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) if ((dx != 0 || dy != 0) && level.water[c + dx + dy * w]) n++;
			if (n < around) continue;
			int d = dist( level, c, from );
			if (d < bestD){
				bestD = d;
				best = c;
			}
		}
		return best;
	}

	//the dry open cell within `radius` of `from` with the most open water within four cells
	private static int byOpenWater( Level level, int from, int radius ){
		int w = level.width(), best = -1, bestN = 11;
		for (int dy = -radius; dy <= radius; dy++){
			for (int dx = -radius; dx <= radius; dx++){
				int x = from % w + dx, y = from / w + dy;
				if (x <= 4 || y <= 4 || x >= w - 5 || y >= level.height() - 5) continue;
				int c = x + y * w;
				if (!level.passable[c] || level.water[c] || level.pit[c] || Actor.findChar( c ) != null) continue;
				int n = 0;
				for (int j = -4; j <= 4; j++) for (int i = -4; i <= 4; i++) if (FishBrain.openWater( level, c + i + j * w )) n++;
				if (n > bestN){
					bestN = n;
					best = c;
				}
			}
		}
		if (best < 0) GLog.w( "Scene: no open water within " + radius + " cells of the landing" );
		return best;
	}

	// ------------------------------------------------------------------ keeping it lively

	/**
	 * Keeps a scene's water lively while its floor's scene stands: striders and fish on the water
	 * near the hero kept to their numbers, a drop from the roof every so often, a fish leaping
	 * every 2-4 s, the hour (day) and the rain held. All of it let go with the scene.
	 */
	private static final class Keeper extends Gizmo {
		final int striders, swimmers;
		final float dripEvery, leapEvery;
		final PrecipType rain;
		private float dripIn, leapIn, lookIn;
		private final ArrayList<Visual> kept = new ArrayList<>();

		Keeper( int striders, int swimmers, float dripEvery, float leapEvery, PrecipType rain ){
			this.striders = striders;
			this.swimmers = swimmers;
			this.dripEvery = dripEvery;
			this.leapEvery = leapEvery;
			this.rain = rain;
			if (leapEvery > 0f){
				DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
				ClimateManager.debugPrecipTypeOverride = rain;
				ClimateManager.debugPrecipOverride = rain != null ? 0.5f : 0f;
				ClimateManager.debugWindOverride = 2f;
				ClimateManager.debugWindDirOverride = 90f;
				ClimateManager.onHeroTurn();
				GameScene.updateDayNightTint();
			}
		}

		@Override
		public void update(){
			Level level = Dungeon.level;
			Hero hero = Dungeon.hero;
			if (level == null || hero == null) return;
			float dt = Game.elapsed;
			if ((lookIn -= dt) <= 0f){
				lookIn = 1f;
				keep( level, hero );
			}
			if (dripEvery > 0f && (dripIn -= dt) <= 0f){
				dripIn = dripEvery;
				int c = waterInView( level, hero.pos, 6 );
				FxEmitter e = GameScene.fxEmitter();
				if (c >= 0 && e != null){
					e.pos( (c % level.width()) * SIZE + FxRandom.Float( 3f, 13f ), (c / level.width()) * SIZE + FxRandom.Float( 4f, 13f ) );
					e.burst( DripParticle.FALL, 1 );
				}
			}
			if (leapEvery > 0f && (leapIn -= dt) <= 0f){
				leapIn = FxRandom.Float( leapEvery * 0.67f, leapEvery * 1.33f );
				int c = openInView( level, hero.pos, 7 );
				if (c >= 0){
					int w = level.width();
					GameScene.effect( new CritterSprite.Fish( FxRandom.chance( 0.5f ), (c % w + 0.5f) * SIZE,
							(c / w) * SIZE + 10, FxRandom.chance( 0.5f ) ? 1 : -1 ) );
				}
			}
		}

		//the striders and fish kept to their numbers on the water near the hero
		private void keep( Level level, Hero hero ){
			int haveStriders = 0, haveFish = 0;
			for (int i = kept.size() - 1; i >= 0; i--){
				Visual v = kept.get( i );
				if (!v.exists){
					kept.remove( i );
					continue;
				}
				if (v instanceof DungeonCritterSprite.Strider) haveStriders++;
				else haveFish++;
			}
			int w = level.width();
			for (int i = haveStriders; i < striders; i++){
				int c = waterInView( level, hero.pos, 6 );
				if (c < 0) break;
				DungeonCritterSprite.Strider s = new DungeonCritterSprite.Strider( (c % w) * SIZE + FxRandom.Float( 5f, 11f ),
						(c / w) * SIZE + FxRandom.Float( 5f, 11f ) );
				GameScene.floorEffect( s );
				kept.add( s );
			}
			for (int i = haveFish; i < swimmers; i++){
				int c = openInView( level, hero.pos, 6 );
				if (c < 0) break;
				SliceCritterSprite.Swimmer f = new SliceCritterSprite.Swimmer( (c % w) * SIZE + 8, (c / w) * SIZE + 9,
						FxRandom.angle() );
				GameScene.subsurface( f );
				kept.add( f );
			}
		}

		@Override
		public void destroy(){
			super.destroy();
			if (leapEvery > 0f){
				DayNightCycle.debugPhaseOverride = null;
				ClimateManager.debugPrecipTypeOverride = null;
				ClimateManager.debugPrecipOverride = Float.NaN;
				ClimateManager.debugWindOverride = Float.NaN;
				ClimateManager.debugWindDirOverride = Float.NaN;
			}
		}
	}

	//a water cell the hero sees within `r` of him, a few picks at random; -1 for none
	private static int waterInView( Level level, int from, int r ){
		return pick( level, from, r, false );
	}

	//the same, open water (five of its eight neighbours water)
	private static int openInView( Level level, int from, int r ){
		return pick( level, from, r, true );
	}

	private static int pick( Level level, int from, int r, boolean open ){
		int w = level.width(), h = level.height();
		for (int t = 0; t < 40; t++){
			int x = from % w + FxRandom.IntRange( -r, r ), y = from / w + FxRandom.IntRange( -r, r );
			if (x <= 0 || y <= 0 || x >= w - 1 || y >= h - 1) continue;
			int c = x + y * w;
			if (!level.water[c] || level.heroFOV == null || !level.heroFOV[c]) continue;
			if (open && !FishBrain.openWater( level, c )) continue;
			return c;
		}
		return -1;
	}
}
