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

package xyz.gabriwar.warpedpixeldungeon.levels.overworld;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Settler;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The moon across the sun (WorldEvents.Type.ECLIPSE): a few hours of a day turned to dusk.
 *
 * WHEN. Only at a new moon, as a real one: on the day of each lunation whose noon lies nearest the
 * new moon (the run's own calendar, GameCalendar; a real-clock run's the real moon's), and on one
 * lunation in two by the world's hash - an eclipse about every two months, the same for a world
 * whoever looks. It darkens the sun for TURNS (300 turns, about three hours) around midday: the
 * middle of that day's DAY on a turn-counted run (its length is the season's), local noon on a
 * real-clock one. A pure function of the seed and the day (window).
 *
 * THE DAY BEFORE. From a day before it the host writes it into the log (WorldEventLog.forced),
 * anchored on the nearest human village's well within ELDER_REACH sectors - its elders read the sky
 * - or where the hero stands: every player hears of it then (the elders' word, or the road's), the
 * world map pins it until the sun is back, and a village elder asked speaks of nothing else.
 *
 * THE DARK. The climate darkens the sun (ClimateManager: its light falls to DEPTH's share at the
 * deepest, a dusk at midday, rising and setting along a curve) and shows the corona (the weather's
 * CORONA motes) and a few stars (effects/particles/EclipseStarParticle) - a co-op guest's climate
 * mirrors his host's. The night's beasts come out on the surface as if it were night (OverworldFauna.
 * phaseNow): the day's go to ground and the window is topped up with the night's. And dark magic
 * runs strong: a wand of corruption is CORRUPTION times as strong while the sun is eclipsed.
 */
public final class EclipseEvent {

	private EclipseEvent(){}

	private static final int FC = WorldEvents.FC;

	/** How long the sun is dark, in turns. */
	public static final int TURNS = 300;
	/** How long before the dark it is heard of. */
	public static final int HERALD = FC;
	/** Sectors: the village whose elders speak of it lies within this many of the hero. */
	public static final int ELDER_REACH = 6;
	/** The share of the sun the moon hides at the deepest. */
	public static final float DEPTH = 0.88f;
	/** How much stronger a wand of corruption is while the sun is eclipsed. */
	public static final float CORRUPTION = 1.5f;

	private static final long SALT = 0xEC11F5E5L;
	//the real moon's epoch (a new moon, as a Julian date) and its synodic month: GameCalendar's
	private static final double NEW_MOON_JD = 2451550.1, SYNODIC = 29.530588;

	// ------------------------------------------------------------ the schedule (pure)

	//the moon's age at a real-clock day's local noon, in days since the new moon
	private static double realAge( int day ){
		double jd = day + 0.5 + 2440587.5;
		double age = (jd - NEW_MOON_JD) % SYNODIC;
		return age < 0 ? age + SYNODIC : age;
	}

	/** Is this the day of a lunation whose noon lies nearest its new moon? */
	public static boolean newMoonDay( int day ){
		if (Dungeon.isChallenged( Challenges.REAL_CLOCK )){
			double age = realAge( day );
			return age < 0.5 || age >= SYNODIC - 0.5;
		}
		//the turn-counted moon: a 29-day cycle from the run's own first day (GameCalendar)
		return Math.floorMod( Dungeon.calendarStartDay + day, 29 ) == 1;
	}

	//which lunation a day belongs to
	private static long lunation( int day ){
		if (Dungeon.isChallenged( Challenges.REAL_CLOCK )){
			return (long) Math.floor( (day + 0.5 + 2440587.5 - NEW_MOON_JD) / SYNODIC + 0.5 );
		}
		return Math.floorDiv( Dungeon.calendarStartDay + day, 29 );
	}

	/** Is the sun eclipsed on this day: a new moon's, and its lunation's hash says so (one in two). */
	public static boolean scheduled( long seed, int day ){
		return newMoonDay( day ) && (WorldEvents.mix( seed ^ SALT, lunation( day ), 0 ) & 1L) == 0;
	}

	/** The middle of a day's light, a world turn: the middle of its DAY on a turn-counted run (by its
	 *  season's length), local noon on a real-clock one. */
	public static int midday( int day ){
		if (Dungeon.isChallenged( Challenges.REAL_CLOCK )) return day * FC + FC / 2;
		return day * FC + DayNightCycle.phaseDuration( DayNightCycle.Phase.DAWN )
				+ DayNightCycle.dayTurns( GameCalendar.seasonOfDay( day ) ) / 2;
	}

	/** The dark of a day's eclipse as {first turn, first turn of light again}, null for none. */
	public static int[] window( long seed, int day ){
		if (!scheduled( seed, day )) return null;
		int mid = midday( day );
		return new int[]{ mid - TURNS / 2, mid + TURNS / 2 };
	}

	/** The dark of an eclipse in the log: its last TURNS turns. */
	public static int darkFrom( WorldEvents.Event e ){
		return e.endTurn - TURNS;
	}

	/** The eclipse dark at a world turn, as {first, end}, or null: the world's schedule, or one in
	 *  the surface's log (heralded, or a debug scene's). Any thread the climate's turn runs on. */
	public static int[] windowAt( int turn ){
		Level l = Dungeon.level;
		if (l instanceof OverworldLevel && ((OverworldLevel) l).altitude() == 0){
			for (WorldEvents.Event e : ((OverworldLevel) l).eventLog().forced){
				if (e.type == WorldEvents.Type.ECLIPSE && turn >= darkFrom( e ) && turn < e.endTurn){
					return new int[]{ darkFrom( e ), e.endTurn };
				}
			}
		}
		if (Dungeon.seed == 0) return null;
		int[] w = window( OverworldLevel.worldSeedOf( Dungeon.seed ), WorldEvents.day( turn ) );
		return w != null && turn >= w[0] && turn < w[1] ? w : null;
	}

	/** How far the moon covers the sun at a turn of its dark, 0..1: in over the first part, whole
	 *  through the middle, out over the last. */
	public static float depth( int from, int to, int turn ){
		if (turn < from || turn >= to || to <= from) return 0f;
		float t = (turn - from) / (float)(to - from);
		return Math.min( 1f, 1.6f * (float) Math.sin( Math.PI * t ) );
	}

	/** The share of the sun's light left at a turn of an eclipse's dark (ClimateManager.computeLight). */
	public static float light( int from, int to, int turn ){
		return 1f - DEPTH * depth( from, to, turn );
	}

	// ------------------------------------------------------------ what it does

	/** A wand of corruption's strength now: CORRUPTION times while the sun is eclipsed. */
	public static float corruptionBoost(){
		return ClimateManager.isSolarEclipse() ? CORRUPTION : 1f;
	}

	/** The world's eclipse heralded on the surface and not yet over, or null. */
	static WorldEvents.Event heralded( OverworldLevel ow, int turn ){
		for (WorldEvents.Event e : ow.eventLog().forced){
			if (e.type == WorldEvents.Type.ECLIPSE && turn < e.endTurn) return e;
		}
		return null;
	}

	/** What a village elder says of an eclipse heralded, or null: of the dark to come, or of the dark
	 *  itself (Settler.interact). */
	public static String elderLine( Settler elder ){
		Level l = Dungeon.level;
		if (!(l instanceof OverworldLevel) || ((OverworldLevel) l).altitude() != 0) return null;
		OverworldLevel ow = (OverworldLevel) l;
		int turn = ow.eventTurn();
		WorldEvents.Event e = heralded( ow, turn );
		if (e == null) return null;
		if (turn >= darkFrom( e )) return Messages.get( elder, "eclipse_now_" + Random.Int( 2 ) );
		return Messages.get( elder, "eclipse_" + Random.Int( 3 ) );
	}

	// ------------------------------------------------------------ the host's step

	//the eclipse (by the turn its dark ends) whose coming dark has been said: once, whether the
	//climate saw it begin under the sky or the hero came up into it (not bundled: harmless again)
	private static int beganSaid = Integer.MIN_VALUE;

	/** The host hero's step on the surface (OverworldLevel.tickEvents): an eclipse of the world's
	 *  within a day ahead is written into the log and heard of. Actor thread. */
	public static void onHeroStep( OverworldLevel level ){
		long t0 = LagMonitor.begin();
		int turn = level.eventTurn();
		int today = WorldEvents.day( turn );
		for (int d = today; d <= today + 1; d++){
			int[] w = window( level.worldSeed, d );
			if (w == null || turn >= w[1] || w[0] - turn > HERALD) continue;
			if (bornFor( level.eventLog(), w[1] ) != null) continue;
			herald( level, d, w, turn );
		}
		LagMonitor.end( "OW eclipse", t0 );
	}

	//the eclipse in the log that ends at this turn, or null
	private static WorldEvents.Event bornFor( WorldEventLog log, int end ){
		for (WorldEvents.Event e : log.forced){
			if (e.type == WorldEvents.Type.ECLIPSE && e.endTurn == end) return e;
		}
		return null;
	}

	//written into the log on the nearest human village's well (or the hero's own cell) and heard
	//of: from the elders the day before or the same morning; once dark, the dark says it
	static WorldEvents.Event herald( OverworldLevel level, int day, int[] w, int turn ){
		return herald( level, day, w, turn, false );
	}

	//...with an id of its own when a debug scene forces it
	private static WorldEvents.Event herald( OverworldLevel level, int day, int[] w, int turn, boolean forced ){
		xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero = Dungeon.hero;
		int hwx = level.worldX + hero.pos % level.width(), hwy = level.worldY + hero.pos / level.width();
		ArrayList<int[]> near = WorldStructures.settlementsNear( level.worldSeed, hwx, hwy, WorldStructures.Faction.HUMAN, 1, ELDER_REACH );
		int ax = hwx, ay = hwy;
		String village = null;
		if (!near.isEmpty()){
			ax = near.get( 0 )[2];
			ay = near.get( 0 )[3];
			village = WorldStructures.villageName( level.worldSeed, near.get( 0 )[0], near.get( 0 )[1] );
		}
		int sx = Math.floorDiv( ax, WorldStructures.SECTOR ), sy = Math.floorDiv( ay, WorldStructures.SECTOR );
		long id = WorldEvents.idOf( WorldEvents.Type.ECLIPSE, sx, sy, day );
		if (forced) id = WorldEvents.mix( id ^ 0xF0BCEDL, turn, w[0] );
		WorldEvents.Event e = new WorldEvents.Event( WorldEvents.Type.ECLIPSE, sx, sy, day, Math.min( turn, w[0] ), w[1],
				ax, ay, id );
		level.forceEvent( e );
		level.markAnnounced( e );
		if (turn >= w[0]){
			//already dark when the hero came up under the sky: the dark itself is the word
			if (beganSaid != w[1]){
				beganSaid = w[1];
				level.tellParty( ( h, x, y ) -> GLog.WARNING + Messages.get( EclipseEvent.class, "begins" ) );
			}
			return e;
		}
		boolean tomorrow = WorldEvents.day( w[0] ) > WorldEvents.day( turn );
		final String when = Messages.get( EclipseEvent.class, tomorrow ? "tomorrow" : "today" );
		final String from = village;
		level.tellParty( ( h, x, y ) -> GLog.HIGHLIGHT + (from != null
				? Messages.get( EclipseEvent.class, "herald", from, when )
				: Messages.get( EclipseEvent.class, "herald_road", when )) );
		xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph.reveal( xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph.TAG_WORLD_EVENT );
		return e;
	}

	/** The climate: the world's eclipse has darkened the sun (ClimateManager). Said under the open
	 *  sky; on the surface the night's beasts come out. Actor thread. */
	public static void began( int end ){
		Level l = Dungeon.level;
		if (!(l instanceof OverworldLevel) || !((OverworldLevel) l).openSky()) return;
		OverworldLevel ow = (OverworldLevel) l;
		if (beganSaid != end){
			beganSaid = end;
			GLog.w( Messages.get( EclipseEvent.class, "begins" ) );
		}
		xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph.reveal( xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph.TAG_WORLD_EVENT );
		if (ow.liveScene()) Sample.INSTANCE.play( Assets.Sounds.DEGRADE, 0.6f, 0.7f );
		ow.refreshFauna();
	}

	/** The climate: the sun is back. Said under the open sky; on the surface the day's beasts come
	 *  back. Actor thread. */
	public static void ended(){
		Level l = Dungeon.level;
		if (!(l instanceof OverworldLevel) || !((OverworldLevel) l).openSky()) return;
		OverworldLevel ow = (OverworldLevel) l;
		GLog.p( Messages.get( EclipseEvent.class, "ends" ) );
		if (ow.liveScene()) SpatialSound.playPanned( Assets.Sounds.CHARMS, 0f, 0.4f, 1.2f, 0f );
		ow.refreshFauna();
	}

	// ------------------------------------------------------------ debug

	/** Debug (the eclipse scenes): an eclipse whose dark begins `in` turns from now and lasts TURNS,
	 *  heard of now as the elders would tell it, through the world's own registry (an id of its own). */
	public static WorldEvents.Event force( OverworldLevel level, int in ){
		int turn = level.eventTurn();
		int from = turn + Math.max( 1, in );
		return herald( level, WorldEvents.day( from ), new int[]{ from, from + TURNS }, turn, true );
	}
}
