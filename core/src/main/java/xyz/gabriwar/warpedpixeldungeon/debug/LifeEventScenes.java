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
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.FestivalEvent;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.MigrationEvent;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RoamerEvent;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEvents;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

/**
 * The life events' debug scenes (the Scenes menu's "Surface" group; levels/overworld FestivalEvent,
 * MigrationEvent, RoamerEvent). Each forces its event through the very registry the world reads
 * (WorldEvents.eventsNear's forced list), so what shows is what players get, and puts the fog
 * back on once the hero is there.
 *
 * overworld-festival: the nearest human village's festival on a fair morning, the hero set down
 * by its well with gold to spend: the maypole and bunting, the two cooks' stalls at half price,
 * the champion (a word with him tells the terms, a second within 30 turns starts the bout) and
 * the folk crowding the well. overworld-festival-night: the same at dusk, the rain held off: the
 * lanterns lit, the folk dancing round the well in two rings and the band playing.
 *
 * overworld-migration: a spring morning, a herd forced onto a road of the deer's ground beside
 * the hero, its middle passing him now, going north: the deer grazing their way along and the
 * wolves trailing them. overworld-migration-dusk: an autumn dusk, the herd going south, the
 * wolves running down a straggler as night falls.
 *
 * overworld-roamer: a great beast forced to have made its lair on the ground near the hero a day
 * and a half ago, the hero set down 14 cells from where it is now, on its fresh tracks: see them,
 * or the beast, and it is pinned on the map. overworld-roamer-rumour: the same beast two sectors
 * from the nearest human village, the hero at its well: talk to the folk. overworld-roamer-yeti
 * and overworld-roamer-scorpion: the beast near the origin's nearest snowfield or desert, so an
 * elder yeti or a giant scorpion.
 */
final class LifeEventScenes {

	private LifeEventScenes(){}

	static final DebugScenes.Scene[] SCENES = {
			new Festival( "overworld-festival", "Surface: a village festival (stalls, champion)", DayNightCycle.Phase.DAY, 0.15f ),
			new Festival( "overworld-festival-night", "Surface: a festival night (lanterns, dance, band)", DayNightCycle.Phase.DUSK, 0.35f ),
			new Migration( "overworld-migration", "Surface: a deer herd migrating past (spring)",
					GameCalendar.Season.SPRING, DayNightCycle.Phase.DAY, 0.3f ),
			new Migration( "overworld-migration-dusk", "Surface: wolves on a herd's trail at dusk (autumn)",
					GameCalendar.Season.AUTUMN, DayNightCycle.Phase.DUSK, 0.2f ),
			new Roamer( "overworld-roamer", "Surface: a great beast and its tracks", null, false ),
			new Roamer( "overworld-roamer-rumour", "Surface: a village talks of a great beast", null, true ),
			new Roamer( "overworld-roamer-yeti", "Surface: an elder yeti in the snow", WorldModel.Biome.SNOWFIELD, false ),
			new Roamer( "overworld-roamer-scorpion", "Surface: a giant scorpion in the desert", WorldModel.Biome.DESERT, false ),
	};

	//the hero's world cell on the surface, or the origin's off it
	private static int[] here( Hero hero ){
		if (Dungeon.level instanceof OverworldLevel && ((OverworldLevel) Dungeon.level).altitude() == 0){
			OverworldLevel ow = (OverworldLevel) Dungeon.level;
			return new int[]{ ow.worldX() + hero.pos % ow.width(), ow.worldY() + hero.pos / ow.width() };
		}
		return new int[]{ 0, 0 };
	}

	//the scene's arrival: on the surface, the fog back on (what players see). false off it
	private static OverworldLevel arrived(){
		if (!(Dungeon.level instanceof OverworldLevel) || ((OverworldLevel) Dungeon.level).altitude() != 0){
			GLog.w( "Scene: could not reach the surface" );
			return null;
		}
		Dungeon.debugNoFog = false;
		Dungeon.observe();
		return (OverworldLevel) Dungeon.level;
	}

	private static void clock( GameCalendar.Season season, DayNightCycle.Phase phase, float into ){
		if (!DebugScenes.walkClock( season, phase, into )) GLog.w( "Scene: a real-clock run keeps its own season and hour" );
		//the rain would send the folk indoors and the band home
		ClimateManager.debugPrecipOverride = 0f;
		ClimateManager.debugForceStorm = false;
	}

	//the nearest human village's festival, forced on arrival, the hero by its well
	private static final class Festival implements DebugScenes.Scene {
		final String id, title;
		final DayNightCycle.Phase phase;
		final float into;
		boolean travelling;
		int[] village;   //{sx, sy, cx, cy}
		Festival( String id, String title, DayNightCycle.Phase phase, float into ){
			this.id = id;
			this.title = title;
			this.phase = phase;
			this.into = into;
		}
		@Override public String id(){ return id; }
		@Override public String title(){ return title; }
		@Override public void apply( Hero hero ){
			long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			if (travelling){
				travelling = false;
				OverworldLevel ow = arrived();
				if (ow == null) return;
				Dungeon.gold = Math.max( Dungeon.gold, 5000 );
				ow.forceEvent( FestivalEvent.forcedFestival( seed, village[0], village[1], ow.eventTurn() ) );
				ow.pollEvents();
				GLog.i( "Scene: " + WorldStructures.villageName( seed, village[0], village[1] ) + " holds its festival until"
						+ " tomorrow morning. The cooks sell at half price (5000 gold to spend); talk to the champion twice"
						+ " for a bout, the prize once. From dusk the folk dance round the well and the band plays"
						+ " (rain held off; fog on: what players see)." );
				return;
			}
			int[] at = here( hero );
			village = FestivalEvent.nearestVillage( seed, at[0], at[1], 12 );
			if (village == null){
				GLog.w( "Scene: no human village with folk in it within twelve sectors" );
				return;
			}
			clock( null, phase, into );
			travelling = true;
			DebugScenes.travelThen( this, 0, village[2], village[3] + 3 );
		}
	}

	//a herd forced onto a road of the deer's ground beside the hero, its middle passing him now
	private static final class Migration implements DebugScenes.Scene {
		final String id, title;
		final GameCalendar.Season season;
		final DayNightCycle.Phase phase;
		final float into;
		boolean travelling;
		WorldEvents.Event herd;
		Migration( String id, String title, GameCalendar.Season season, DayNightCycle.Phase phase, float into ){
			this.id = id;
			this.title = title;
			this.season = season;
			this.phase = phase;
			this.into = into;
		}
		@Override public String id(){ return id; }
		@Override public String title(){ return title; }
		@Override public void apply( Hero hero ){
			long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			if (travelling){
				travelling = false;
				OverworldLevel ow = arrived();
				if (ow == null) return;
				ow.forceEvent( herd );
				ow.pollEvents();
				GLog.i( "Scene: a herd of " + MigrationEvent.herdSize( herd.id ) + " deer and " + MigrationEvent.packSize( herd.id )
						+ " wolves is passing you, its middle here now; it fills in over a few steps. Stay a while:"
						+ " the herd moves on a cell every thirty turns or so, the wolves keep behind its stragglers"
						+ " and run one down after dark (fog on: what players see)." );
				return;
			}
			clock( season, phase, into );
			final int[] at = here( hero );
			final int now = WorldClock.turn();
			DebugScenes.search( "a road of deer ground", () -> {
				int day = WorldEvents.day( MigrationEvent.debugSetOff( now, 30 ) );
				return MigrationEvent.debugStart( seed, at[0], at[1], day, 30, 3 );
			}, start -> {
				if (start == null){
					GLog.w( "Scene: no road of the deer's ground runs by here: try open woods, meadows or plains" );
					return;
				}
				herd = MigrationEvent.forcedMigration( start[0], start[1], MigrationEvent.debugSetOff( now, 30 ) );
				//ten cells to the side of the herd's middle: near enough to watch, far enough not to scatter it
				int[] mid = MigrationEvent.middleNow( seed, herd, now );
				float[] u = MigrationEvent.heading( herd.startDay );
				travelling = true;
				DebugScenes.travelThen( this, 0, mid[0] - Math.round( u[1] * 10 ), mid[1] + Math.round( u[0] * 10 ) );
			} );
		}
	}

	//a great beast that made its lair near the hero a day and a half ago: the hero set down on its
	//fresh tracks, or in the nearest village two sectors from it
	private static final class Roamer implements DebugScenes.Scene {
		final String id, title;
		final WorldModel.Biome biome;   //null: the ground about the hero
		final boolean rumour;
		boolean travelling;
		WorldEvents.Event beast;
		Roamer( String id, String title, WorldModel.Biome biome, boolean rumour ){
			this.id = id;
			this.title = title;
			this.biome = biome;
			this.rumour = rumour;
		}
		@Override public String id(){ return id; }
		@Override public String title(){ return title; }
		@Override public void apply( Hero hero ){
			long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			if (travelling){
				travelling = false;
				OverworldLevel ow = arrived();
				if (ow == null) return;
				ow.forceEvent( beast );
				ow.pollEvents();
				RoamerEvent.Haunts h = RoamerEvent.haunts( seed, beast );
				GLog.i( "Scene: " + RoamerEvent.title( h.kind, RoamerEvent.nameOf( beast.id ) ) + " ("
						+ h.kind.name().toLowerCase( java.util.Locale.ENGLISH ) + ") roams here for "
						+ RoamerEvent.DAYS + " days. "
						+ (rumour ? "Talk to the folk: they tell where it was seen, and it goes on the map."
								: "Its fresh tracks lead to it, 14 cells off: see them, or it, and it goes on the map.")
						+ " Kill it for its trophy, a purse and a good item (fog on: what players see)." );
				return;
			}
			clock( null, DayNightCycle.Phase.DAY, 0.35f );
			final int[] at = here( hero );
			final float shift = WorldModel.calendarShift();
			final int now = WorldClock.turn();
			DebugScenes.search( biome == null ? "ground for a lair" : biome.name().toLowerCase( java.util.Locale.ENGLISH ), () -> {
				int[] from = at;
				int[] village = null;
				if (rumour){
					village = FestivalEvent.nearestVillage( seed, at[0], at[1], 12 );
					if (village == null) return null;
					from = new int[]{ village[2], village[3] };
				} else if (biome != null){
					from = DebugScenes.biomeGround( seed, shift, biome );
					if (from == null) return null;
				}
				int[] lair = RoamerEvent.debugLair( seed, from[0], from[1], rumour ? 2 * WorldStructures.SECTOR : 16 );
				if (lair == null) return null;
				//a day and a half into its days: left its lair this morning, a third of the way on
				WorldEvents.Event e = RoamerEvent.forcedRoamer( lair[0], lair[1], now - 3 * WorldEvents.FC / 2 );
				int[] p = RoamerEvent.where( RoamerEvent.haunts( seed, e ), e, now );
				beast = e;
				return rumour ? new int[]{ village[2], village[3] + 3 } : new int[]{ p[0] + 14, p[1] };
			}, spot -> {
				if (spot == null){
					GLog.w( "Scene: found no " + (rumour ? "village with a lair two sectors off" : "ground for a lair") + " near here" );
					return;
				}
				travelling = true;
				DebugScenes.travelThen( this, 0, spot[0], spot[1] );
			} );
		}
	}
}
