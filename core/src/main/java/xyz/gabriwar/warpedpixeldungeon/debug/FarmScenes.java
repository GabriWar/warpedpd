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

package xyz.gabriwar.warpedpixeldungeon.debug;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bunny;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Deer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Goat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.Roping;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.FarmRules;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.Farming;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Plank;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sickle;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Apricobush;
import xyz.gabriwar.warpedpixeldungeon.plants.BlandfruitBush;
import xyz.gabriwar.warpedpixeldungeon.plants.Cornwheat;
import xyz.gabriwar.warpedpixeldungeon.plants.Eggbloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Firebloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Gobgrape;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import xyz.gabriwar.warpedpixeldungeon.plants.Tomatobush;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

/**
 * The fields' and the animals' debug scenes (the Scenes menu's "Homestead" group):
 *
 * homestead-farm: a spring day; on the surface near the hero an 11x8 fenced farm with a gate on
 * its south side: two beds of farmland, a well, an irrigation channel run off the well between the
 * beds (the farmland beside it and near the well turns wet), crops of eight kinds at every stage
 * from just sown to ripe, a compost bin; a sickle and seeds in the pack.
 *
 * homestead-greenhouse-winter: a winter day on a snowfield (or the tundra): a plank greenhouse
 * under glass, its bed thriving, and beside it the same bed in the open, which the frost kills on
 * the first turns.
 *
 * homestead-pen: a summer day in a meadow: goats, deer and bunnies grazing round a half-built
 * fence pen (its north side open) with a gate and a tie post; three ropes and planks to finish it.
 *
 * homestead-pests: a summer night; an unfenced bed of crops and, eight cells off, bunnies and a
 * deer: rest and watch them come for it.
 */
final class FarmScenes {

	private FarmScenes(){}

	static final DebugScenes.Scene[] SCENES = {
			new Farm(),
			new GreenhouseWinter(),
			new Pen(),
			new PestsScene()
	};

	//a crop's stages in the scenes: share of its first cycle grown, RIPE for a ripe one
	private static final float RIPE = -1f;

	// ----------------------------------------------------------------- the scenes

	private static final class Farm extends OnSurface {
		Farm(){ super( "homestead-farm", "Homestead: a fenced, watered farm in spring", 11, 8 ); }

		@Override boolean clock(){
			return DebugScenes.walkClock( GameCalendar.Season.SPRING, DayNightCycle.Phase.DAY, 0.3f );
		}

		@Override void lay( OverworldLevel ow, int o, Hero hero ){
			int w = ow.width();
			ring( ow, o, 11, 8, 5 );
			//two beds of farmland, a channel off the well between them
			for (int y = 1; y <= 6; y++){
				if (y == 4) continue;
				for (int x = 1; x <= 5; x++) build( ow, o + x + y * w, "farm_plot" );
			}
			build( ow, o + 7 + 4 * w, "well" );
			for (int x = 1; x <= 6; x++) build( ow, o + x + 4 * w, "channel" );
			build( ow, o + 9 + 2 * w, "compost_bin" );

			float[] stages = { 0f, 0.35f, 0.7f, RIPE, RIPE };
			Class<?>[][] beds = {
					{ Cornwheat.Seed.class, Cornwheat.Seed.class, Cornwheat.Seed.class, Cornwheat.Seed.class, Cornwheat.Seed.class },
					{ BlandfruitBush.Seed.class, BlandfruitBush.Seed.class, Tomatobush.Seed.class, Tomatobush.Seed.class, Tomatobush.Seed.class },
					{ Apricobush.Seed.class, Gobgrape.Seed.class, Eggbloom.Seed.class, Apricobush.Seed.class, Gobgrape.Seed.class },
					{ Firebloom.Seed.class, Sungrass.Seed.class, Firebloom.Seed.class, Sungrass.Seed.class, Eggbloom.Seed.class }
			};
			int[] rows = { 1, 2, 5, 6 };
			for (int r = 0; r < rows.length; r++){
				for (int x = 0; x < 5; x++){
					sow( ow, o + 1 + x + rows[r] * w, beds[r][x], stages[(x + r) % stages.length] );
				}
			}
			give( hero, new Sickle() );
			give( hero, new Cornwheat.Seed().quantity( 3 ) );
			give( hero, new BlandfruitBush.Seed().quantity( 2 ) );
			give( hero, new Tomatobush.Seed().quantity( 2 ) );
			give( hero, new Firebloom.Seed().quantity( 2 ) );
			GLog.i( "Scene: spring. A fenced farm (gate south): two beds, a well with a channel run off it between them"
					+ " (the farmland beside it and near the well is wet), crops sown, half grown and ripe, a compost bin."
					+ " Tap a ripe crop with the sickle in your pack: cornwheat gives shafts, tomato bushes fused tomatoes."
					+ " Sow the seeds in your pack on any free farmland." );
		}
	}

	private static final class GreenhouseWinter implements DebugScenes.Scene {
		//set while the hero is on his way to the snow
		boolean travelling;

		@Override public String id(){ return "homestead-greenhouse-winter"; }
		@Override public String title(){ return "Homestead: a greenhouse thriving beside a frozen plot in winter"; }

		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				arrive( hero );
				return;
			}
			if (!DebugScenes.walkClock( GameCalendar.Season.WINTER, DayNightCycle.Phase.DAY, 0.3f )){
				GLog.w( "Scene: a real-clock run keeps its own season and hour" );
			}
			final long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			final float shift = WorldModel.calendarShift();
			DebugScenes.search( "a snowfield near the origin",
					() -> DebugScenes.biomeGround( seed, shift, WorldModel.Biome.SNOWFIELD, WorldModel.Biome.TUNDRA ),
					spot -> {
						if (spot == null){
							GLog.w( "Scene: no snowfield or tundra near the origin in this world" );
							return;
						}
						travelling = true;
						DebugScenes.travelThen( this, 0, spot[0], spot[1] );
					} );
		}

		private void arrive( Hero hero ){
			OverworldLevel ow = hostSurface();
			if (ow == null) return;
			int o = ground( ow, hero.pos, 14, 5 );
			if (o == -1){
				GLog.w( "Scene: no open ground near the hero; walk somewhere flatter" );
				return;
			}
			int w = ow.width();
			//the greenhouse: plank walls, a door south, a bed of farmland inside, glass over all
			for (int y = 0; y < 5; y++){
				for (int x = 0; x < 7; x++){
					boolean edge = x == 0 || y == 0 || x == 6 || y == 4;
					build( ow, o + x + y * w, !edge ? "farm_plot" : x == 3 && y == 4 ? "door" : "wall_plank" );
				}
			}
			for (int y = 0; y < 5; y++){
				for (int x = 0; x < 7; x++) build( ow, o + x + y * w, "roof_glass" );
			}
			//the same bed out in the open, two cells east
			for (int y = 1; y <= 3; y++){
				for (int x = 9; x <= 13; x++) build( ow, o + x + y * w, "farm_plot" );
			}
			Class<?>[] kinds = { Cornwheat.Seed.class, Tomatobush.Seed.class, BlandfruitBush.Seed.class, Firebloom.Seed.class, Apricobush.Seed.class };
			float[] stages = { 0.2f, 0.5f, 0.8f, RIPE, RIPE };
			for (int y = 1; y <= 3; y++){
				for (int x = 0; x < 5; x++){
					sow( ow, o + 1 + x + y * w, kinds[x], stages[(x + y) % stages.length] );
					sow( ow, o + 9 + x + y * w, kinds[x], stages[(x + y) % stages.length] );
				}
			}
			int inside = o + 3 + 2 * w;
			give( hero, new Sickle() );
			Dungeon.observe();
			GLog.i( "Scene: winter on the snow, the air " + Messages.decimalFormat( "#.#", ClimateManager.airAt( ow, inside ) )
					+ "C. The greenhouse (glass roof) grows its crops at summer speed and keeps the frost off; the same"
					+ " bed in the open east of it dies of frost in the first turns, and nothing grows in the open in winter." );
		}
	}

	private static final class Pen implements DebugScenes.Scene {
		boolean travelling;

		@Override public String id(){ return "homestead-pen"; }
		@Override public String title(){ return "Homestead: a half-built pen in a meadow full of animals"; }

		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				arrive( hero );
				return;
			}
			DebugScenes.walkClock( GameCalendar.Season.SUMMER, DayNightCycle.Phase.DAY, 0.2f );
			final long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			final float shift = WorldModel.calendarShift();
			DebugScenes.search( "a meadow near the origin",
					() -> DebugScenes.biomeGround( seed, shift, WorldModel.Biome.MEADOW, WorldModel.Biome.PLAINS ),
					spot -> {
						if (spot == null){
							GLog.w( "Scene: no meadow or plains near the origin in this world" );
							return;
						}
						travelling = true;
						DebugScenes.travelThen( this, 0, spot[0], spot[1] );
					} );
		}

		private void arrive( Hero hero ){
			OverworldLevel ow = hostSurface();
			if (ow == null) return;
			int o = ground( ow, hero.pos, 9, 8 );
			if (o == -1){
				GLog.w( "Scene: no open ground near the hero; walk somewhere flatter" );
				return;
			}
			int w = ow.width();
			//a 9x8 ring of fence, its north side left open in the middle, a gate south, a tie post in it
			for (int y = 0; y < 8; y++){
				for (int x = 0; x < 9; x++){
					boolean edge = x == 0 || y == 0 || x == 8 || y == 7;
					if (!edge || (y == 0 && x >= 3 && x <= 5)) continue;
					build( ow, o + x + y * w, x == 4 && y == 7 ? "gate" : "fence" );
				}
			}
			build( ow, o + 6 + 2 * w, "tie_post" );
			//the animals graze a little way off, out of shying distance
			Class<?>[] kinds = { Goat.class, Goat.class, Deer.class, Deer.class, Bunny.class, Bunny.class, Bunny.class };
			int placed = 0;
			for (int i = 0; i < kinds.length; i++){
				int c = freeNear( ow, o + 4 + (-6 + (i % 4) * 4) + (10 + (i / 4) * 3) * w );
				if (c == -1) continue;
				@SuppressWarnings("unchecked")
				Mob m = DebugScenes.spawn( (Class<? extends Mob>) kinds[i], c );
				if (m != null) placed++;
			}
			give( hero, new Rope().quantity( 3 ) );
			give( hero, new Plank().quantity( 6 ) );
			GLog.i( "Scene: summer in a meadow. A fence pen with its north side open (6 planks in your pack to close it"
					+ " with the build menu), a gate south and a tie post, and " + placed + " goats, deer and bunnies south of it."
					+ " Use a rope (LEAD) on an animal up to " + Roping.LASSO + " tiles off with nothing in the way: walk up slowly"
					+ " and throw it before the beast bolts. TIE the one you lead to a fence or the post; walk it into the pen,"
					+ " close the fence and let it go: it is penned for good. Tap a tied or penned one to butcher it." );
		}
	}

	private static final class PestsScene extends OnSurface {
		PestsScene(){ super( "homestead-pests", "Homestead: an open bed of crops and hungry animals at night", 5, 3 ); }

		@Override boolean clock(){
			return DebugScenes.walkClock( GameCalendar.Season.SUMMER, DayNightCycle.Phase.NIGHT, 0.1f );
		}

		@Override void lay( OverworldLevel ow, int o, Hero hero ){
			int w = ow.width();
			Class<?>[] kinds = { Cornwheat.Seed.class, Tomatobush.Seed.class, Gobgrape.Seed.class, Firebloom.Seed.class, Eggbloom.Seed.class };
			for (int y = 0; y < 3; y++){
				for (int x = 0; x < 5; x++){
					build( ow, o + x + y * w, "farm_plot" );
					sow( ow, o + x + y * w, kinds[x], y == 2 ? RIPE : 0.3f * (y + 1) );
				}
			}
			int placed = 0;
			Class<?>[] grazers = { Bunny.class, Bunny.class, Bunny.class, Deer.class };
			for (int i = 0; i < grazers.length; i++){
				int c = freeNear( ow, o + 2 + (i - 1) * 3 + 9 * w );
				if (c == -1) continue;
				@SuppressWarnings("unchecked")
				Mob m = DebugScenes.spawn( (Class<? extends Mob>) grazers[i], c );
				if (m != null) placed++;
			}
			GLog.i( "Scene: a summer night. A bed of crops with no fence round it, and " + placed + " grazers eight cells"
					+ " south. Stay more than 5 cells off and rest: within a few turns they come and eat the crops."
					+ " Walk up to one and it bolts. Build a fence round the bed and they leave it alone." );
		}
	}

	// ----------------------------------------------------------------- a surface scene's frame

	//a scene laid on the surface near the hero: he is taken there first from anywhere else (south
	//of the town), the clock walked on, a w x h stretch of open ground found near him
	private abstract static class OnSurface implements DebugScenes.Scene {
		private final String id, title;
		private final int w, h;
		boolean travelling;

		OnSurface( String id, String title, int w, int h ){
			this.id = id;
			this.title = title;
			this.w = w;
			this.h = h;
		}

		@Override public String id(){ return id; }
		@Override public String title(){ return title; }

		/** Walks the clock to the scene's hour; false on a real-clock run. */
		abstract boolean clock();
		/** Lays the scene with its top-left at a window cell. */
		abstract void lay( OverworldLevel ow, int origin, Hero hero );

		@Override public void apply( Hero hero ){
			if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != 0){
				if (travelling){
					travelling = false;
					GLog.w( "Scene: could not reach the surface" );
					return;
				}
				travelling = true;
				DebugScenes.travelThen( this, 0, WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ),
						WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA ) + 40 );
				return;
			}
			travelling = false;
			if (!clock()) GLog.w( "Scene: a real-clock run keeps its own season and hour" );
			OverworldLevel ow = hostSurface();
			if (ow == null) return;
			int o = ground( ow, hero.pos, w, h );
			if (o == -1){
				GLog.w( "Scene: no open ground near the hero; walk somewhere flatter" );
				return;
			}
			lay( ow, o, hero );
			Dungeon.observe();
		}
	}

	// ----------------------------------------------------------------- helpers

	//the live surface, the host's own; null (and the hero told) otherwise
	private static OverworldLevel hostSurface(){
		if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != 0){
			GLog.w( "Scene: could not reach the surface" );
			return null;
		}
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		if (ow.isMirror()){
			GLog.w( "Scene: only the host can build" );
			return null;
		}
		return ow;
	}

	/** The top-left window cell of the nearest open w x h stretch (and a cell of ground round it)
	 *  keeping two cells from the hero, -1 within 40 cells of none. */
	static int ground( OverworldLevel ow, int heroPos, int w, int h ){
		int width = ow.width(), hx = heroPos % width, hy = heroPos / width;
		for (int r = 3; r <= 40; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int ox = hx + dx, oy = hy + dy;
					if (fits( ow, ox, oy, w, h, hx, hy )) return ox + oy * width;
				}
			}
		}
		return -1;
	}

	private static boolean fits( OverworldLevel ow, int ox, int oy, int w, int h, int hx, int hy ){
		int width = ow.width();
		for (int y = oy - 1; y <= oy + h; y++){
			for (int x = ox - 1; x <= ox + w; x++){
				if (x < 2 || y < 2 || x >= width - 2 || y >= ow.height() - 2) return false;
				if (Math.abs( x - hx ) <= 1 && Math.abs( y - hy ) <= 1) return false;
				int c = x + y * width;
				int t = ow.map[c];
				if (!ow.passable[c] || ow.pit[c] || (Terrain.flags[t] & Terrain.LIQUID) != 0
						|| t == Terrain.FROZEN_WATER || t == Terrain.BRIDGE || Terrain.homestead( t )
						|| ow.protectedForBuilding( c ) || Actor.findChar( c ) != null || ow.heaps.get( c ) != null){
					return false;
				}
			}
		}
		return true;
	}

	//the free ground nearest a cell (within 4), -1 for none
	private static int freeNear( OverworldLevel ow, int cell ){
		int width = ow.width(), cx = cell % width, cy = cell / width;
		for (int r = 0; r <= 4; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int x = cx + dx, y = cy + dy;
					if (x < 2 || y < 2 || x >= width - 2 || y >= ow.height() - 2) continue;
					int c = x + y * width;
					if (ow.passable[c] && !ow.pit[c] && !ow.water[c] && !ow.avoid[c] && Actor.findChar( c ) == null) return c;
				}
			}
		}
		return -1;
	}

	private static void build( OverworldLevel ow, int cell, String piece ){
		Homestead.build( ow, cell, BuildCatalog.byId( piece ), Dungeon.hero );
	}

	//a crop of a seed sown on a farmland cell, grown to a stage (RIPE: ripe)
	@SuppressWarnings("unchecked")
	private static void sow( OverworldLevel ow, int cell, Class<?> seed, float stage ){
		FarmCrop m = Farming.plant( ow, cell, (Class<? extends Plant.Seed>) seed );
		Class<? extends Plant> plant = m.crop.plant();
		if (plant == null) return;
		if (stage == RIPE){
			m.crop.cycles = 1;
			m.crop.growth = 0;
		} else {
			m.crop.growth = stage * FarmRules.cycleTurns( plant, Farming.waterAt( ow, cell ) );
		}
	}

	private static void give( Hero hero, Item item ){
		if (!item.collect( hero.belongings.backpack )){
			Heap h = Dungeon.level.drop( item, hero.pos );
			if (h.sprite != null) h.sprite.drop();
		}
	}

	private static void ring( OverworldLevel ow, int o, int w, int h, int gateX ){
		int width = ow.width();
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++){
				boolean edge = x == 0 || y == 0 || x == w - 1 || y == h - 1;
				if (!edge) continue;
				build( ow, o + x + y * width, x == gateX && y == h - 1 ? "gate" : "fence" );
			}
		}
	}
}
