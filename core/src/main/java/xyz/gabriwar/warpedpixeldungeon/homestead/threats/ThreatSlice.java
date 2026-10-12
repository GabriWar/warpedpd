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

package xyz.gabriwar.warpedpixeldungeon.homestead.threats;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.homestead.StoreSection;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.TalismanOfForesight;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.Trap;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * The threats on one world slice, turn by turn while the hero is on it (host only):
 *
 * - every home whose hearthstone is in the window has its notice worked out as the hero comes into
 *   its window, at each dusk and every REFRESH_TURNS turns (Notice.refresh), kept for its ledger;
 * - the nights gone by there that were never rolled (he was away, or slept through them) are
 *   settled as soon as its whole ground is in the window (ThreatSettler.settleNights), and what
 *   they brought is told once he is back at the home (AwaySettlers after a day or more away, here
 *   after less);
 * - once a night, in the dark, its siege and its raid are rolled (HomeThreats.siegeNight /
 *   raidNight, ThreatWorld.roll: never twice, never both live and settled, never one skipped) and
 *   set going (WolfSiege.start, HomeRaider.band), its alarm bell ringing as they come;
 * - a siege or a raid with none of its wolves or raiders left in the window, or whose night is
 *   past, is over: a line in the home's ledger (the map pins it while under way: ThreatMarks);
 * - a home's alarm bell rings for anything hostile coming onto its ground while the hero sleeps
 *   or rests there (AlarmBell);
 * - the hero up a watchtower (Lookout) is shown every wolf and raider of a siege or a raid within
 *   Watchtower.SPOT_RANGE of it, with a warning of where they come from;
 * - its spike traps (SpikeTrap) are laid on each new window and armed again once their wait is up.
 *   When each was sprung is kept here, world-keyed and saved with the slice.
 */
public class ThreatSlice extends StoreSection {

	/** The build menu's piece that lays a spike trap (DefencePieces). */
	public static final String SPIKE_PIECE = "spike_trap";
	/** How long a monster shown from a watchtower stays shown, in turns (renewed while in range). */
	static final float SPOTTED_TURNS = 5f;
	/** How often a home in the window has its notice worked out again, in turns. */
	static final float REFRESH_TURNS = 20f;

	//world key of a spike trap's cell -> the world clock (FarmCrop.clock) it was sprung at, while
	//it lies sprung
	private final HashMap<Long, Float> sprung = new HashMap<>();

	//not saved, worked out again on the next turn: the homes whose window the hero is in (and when
	//their notice was last worked out), the hour last seen, the hostiles on each home's ground, the
	//sieges and raids seen from a tower
	private final HashMap<String, Float> present = new HashMap<>();
	private DayNightCycle.Phase lastPhase;
	private final HashMap<String, HashSet<Integer>> inside = new HashMap<>();
	private final HashSet<Long> spotted = new HashSet<>();

	/** This slice's threats, null when the threats are not built. */
	public static ThreatSlice of( OverworldLevel level ){
		return level.homestead().section( ThreatSlice.class );
	}

	private static long key( OverworldLevel level, int cell ){
		return OverworldLevel.worldKey( level.worldX + cell % level.width(), level.worldY + cell / level.width() );
	}

	// ----------------------------------------------------------------- spike traps

	/** A spike trap was sprung (SpikeTrap.activate). */
	void sprung( OverworldLevel level, int cell, float at ){
		sprung.put( key( level, cell ), at );
	}

	/** Lays the spike trap of a cell (a piece just built, or one coming into the window). */
	public void lay( OverworldLevel level, int cell ){
		SpikeTrap t = new SpikeTrap();
		Float at = sprung.get( key( level, cell ) );
		if (at != null) t.sprungAt = at;
		t.color = t.armed() ? SpikeTrap.ARMED : SpikeTrap.SPRUNG;
		level.setTrap( t, cell );
	}

	/** Takes the spike trap off a cell (its piece taken down). */
	public void lift( OverworldLevel level, int cell ){
		sprung.remove( key( level, cell ) );
		if (level.traps.get( cell ) instanceof SpikeTrap){
			level.traps.remove( cell );
			GameScene.updateMap( cell );
		}
	}

	@Override
	public void materialize( OverworldLevel level ){
		//the window's traps were cleared with its old cells: its spike traps go down again
		level.homestead().forEachBuilt( level.worldX, level.worldY, level.worldX + level.width(), level.worldY + level.height(),
				( x, y, b ) -> {
					if (!SPIKE_PIECE.equals( b.piece )) return;
					int c = level.localCell( x, y );
					if (c != -1 && level.map[c] == Terrain.INACTIVE_TRAP && !(level.traps.get( c ) instanceof SpikeTrap)) lay( level, c );
				} );
		present.clear();
		inside.clear();
	}

	//every spike trap whose wait is up is armed again
	private void rearm( OverworldLevel level ){
		for (Trap t : level.traps.valueList()){
			if (t instanceof SpikeTrap && t.color == SpikeTrap.SPRUNG && ((SpikeTrap) t).armed()){
				((SpikeTrap) t).rearm();
				sprung.remove( key( level, t.pos ) );
			}
		}
	}

	// ----------------------------------------------------------------- the turn

	@Override
	public void worldTurn( OverworldLevel level ){
		if (level.isMirror() || Dungeon.level != level) return;
		rearm( level );
		Hero hero = Dungeon.hero;
		if (hero == null || ThreatWorld.get() == null) return;
		DayNightCycle.Phase phase = DayNightCycle.phase();
		boolean dusk = phase == DayNightCycle.Phase.DUSK && lastPhase != DayNightCycle.Phase.DUSK;
		lastPhase = phase;
		float now = Actor.now();
		for (HomesteadWorld.Home home : HomeThreats.homesOn( level )){
			String k = HomeThreats.key( home );
			if (HomeThreats.hearth( level, home ) == -1){
				present.remove( k );
				inside.remove( k );
				continue;
			}
			Float seen = present.get( k );
			if (seen == null){
				//first seen: the nights it owes are counted from the hero's last visit
				ThreatWorld.get().settledTo( home );
			}
			if (seen == null || dusk || now - seen >= REFRESH_TURNS){
				Notice.refresh( level, home );
				present.put( k, now );
			}
			catchUp( level, home );
			if (HomeThreats.atHome( level, home, hero.pos )) tell( home );
			if (phase == DayNightCycle.Phase.NIGHT) nightfall( level, home );
			upkeep( level, home );
			bell( level, home, hero );
		}
		lookout( level, hero );
	}

	//the nights gone by at a home and never rolled, settled once its whole ground is in the window
	//(its chests all here to be robbed)
	private static void catchUp( OverworldLevel level, HomesteadWorld.Home home ){
		if (HomeThreats.wholeInWindow( level, home )) ThreatSettler.settleNights( level, home );
	}

	//the hero back at a home within a day (AwaySettlers tells a longer absence): what was settled
	//while he was out, or asleep, told and written in its ledger
	private static void tell( HomesteadWorld.Home home ){
		List<String> lines = ThreatWorld.get().tell( home );
		int day = WorldClock.day();
		for (String line : lines){
			home.note( day, line );
			GLog.w( line );
		}
	}

	/** The night's siege and raid at a home, rolled once (the hero there in the dark), after the
	 *  nights before it are settled: none is ever skipped. */
	void nightfall( OverworldLevel level, HomesteadWorld.Home home ){
		ThreatWorld world = ThreatWorld.get();
		int night = WorldClock.night();
		catchUp( level, home );
		int done = world.settledTo( home );
		//already rolled; or nights before it still owed (its ground not all in the window yet):
		//tonight waits for them, and is settled with them if it goes by first
		if (done >= night || done < night - 1) return;
		world.roll( home, night );
		int notice = world.notice( home ).total();
		long seed = level.worldSeed;
		if (HomeThreats.siegeNight( seed, home, night, notice )) begin( level, home, ThreatWorld.Event.SIEGE, night );
		if (HomeThreats.raidNight( seed, home, night, notice )) begin( level, home, ThreatWorld.Event.RAID, night );
	}

	//a siege or a raid set going on a night: its pack or its band, of the night's own size
	private static ThreatWorld.Event begin( OverworldLevel level, HomesteadWorld.Home home, int kind, int night ){
		ThreatWorld.Event e = ThreatWorld.get().begin( home, kind, night );
		long seed = level.worldSeed;
		if (kind == ThreatWorld.Event.SIEGE){
			WolfSiege.start( level, home, e, HomeThreats.packSize( seed, home, night ), HomeThreats.greyWolves( seed, home ) );
		} else {
			HomeRaider.band( level, home, e, HomeThreats.bandSize( seed, home, night ) );
		}
		//the home's bell rings as they come
		if (e.came > 0){
			AlarmBell bell = AlarmBell.of( level, home );
			if (bell != null) AlarmBell.warn( level, bell, home, Dungeon.hero );
		}
		return e;
	}

	/** Debug (the threats' scenes): a siege or a raid on a home now, as tonight would bring it;
	 *  tonight is marked rolled, so it brings no other. Null when the threats are not built. */
	public static ThreatWorld.Event force( OverworldLevel level, HomesteadWorld.Home home, int kind ){
		ThreatWorld world = ThreatWorld.get();
		if (world == null || HomeThreats.hearth( level, home ) == -1) return null;
		int night = WorldClock.night();
		catchUp( level, home );
		world.roll( home, night );
		return begin( level, home, kind, night );
	}

	//a siege or a raid with none of its own left in the window, or its night long past, is over
	private void upkeep( OverworldLevel level, HomesteadWorld.Home home ){
		ThreatWorld world = ThreatWorld.get();
		int night = WorldClock.night();
		for (ThreatWorld.Event e : world.events( home )){
			if (night <= e.night + 1 && left( level, e ) > 0) continue;
			world.end( home, e );
			//none of its own ever came (no room at the edge): nothing to tell
			if (e.came == 0) continue;
			String line = Messages.get( ThreatSlice.class, ledgerKey( e ), home.title(), e.slain, e.stolen );
			home.note( WorldClock.day(), line );
			GLog.i( line );
			spotted.remove( e.id );
		}
	}

	/** How many of an event's wolves or raiders are still in the window. */
	static int left( OverworldLevel level, ThreatWorld.Event e ){
		int n = 0;
		for (Mob m : level.mobs){
			if (!m.isAlive()) continue;
			if (m instanceof SiegeWolf && ((SiegeWolf) m).pack().event == e.id) n++;
			else if (m instanceof HomeRaider && ((HomeRaider) m).event == e.id) n++;
		}
		return n;
	}

	/** The ledger line of an event over: its message key under ThreatSlice. */
	static String ledgerKey( ThreatWorld.Event e ){
		if (e.kind == ThreatWorld.Event.SIEGE){
			if (e.came > 0 && e.slain >= e.came) return "siege_slain";
			return e.slain > 0 ? "siege_fought" : "siege_left";
		}
		if (e.stolen > 0) return "raid_stolen";
		if (e.came > 0 && e.slain >= e.came) return "raid_slain";
		return "raid_driven";
	}

	//a home's bell rings for a hostile coming onto its ground while the hero sleeps or rests there
	private void bell( OverworldLevel level, HomesteadWorld.Home home, Hero hero ){
		String k = HomeThreats.key( home );
		HashSet<Integer> was = inside.get( k );
		HashSet<Integer> now = new HashSet<>();
		Mob newcomer = null;
		for (Mob m : level.mobs){
			if (!hostile( m ) || !HomeThreats.inArea( level, home, m.pos )) continue;
			now.add( m.id() );
			if (was != null && !was.contains( m.id() )) newcomer = m;
		}
		inside.put( k, now );
		if (newcomer == null || !AlarmBell.sleeping( hero ) || !HomeThreats.inArea( level, home, hero.pos )) return;
		AlarmBell bell = AlarmBell.of( level, home );
		if (bell != null) AlarmBell.wake( bell, hero );
	}

	/** What a bell rings for: anything out for blood (no townsfolk, no beast minding its own). */
	static boolean hostile( Mob m ){
		return m.isAlive() && m.alignment == Mob.Alignment.ENEMY && !(m instanceof NPC);
	}

	//the hero up a tower: the wolves and the raiders coming within its reach shown to him
	private void lookout( OverworldLevel level, Hero hero ){
		Lookout up = hero.buff( Lookout.class );
		Watchtower tower = up == null ? null : up.tower();
		if (tower == null) return;
		boolean shownNew = false;
		for (Mob m : level.mobs){
			if (!m.isAlive() || level.distance( tower.pos, m.pos ) > Watchtower.SPOT_RANGE) continue;
			long event;
			if (m instanceof SiegeWolf) event = ((SiegeWolf) m).pack().event;
			else if (m instanceof HomeRaider) event = ((HomeRaider) m).event;
			else continue;
			if (!shown( hero, m )){
				Buff.append( hero, TalismanOfForesight.CharAwareness.class, SPOTTED_TURNS ).charID = m.id();
				shownNew = true;
			}
			if (spotted.add( event )){
				int w = level.width();
				String from = HomeThreats.bearing( m.pos % w - tower.pos % w, m.pos / w - tower.pos / w );
				GLog.w( Messages.get( ThreatSlice.class, m instanceof SiegeWolf ? "spotted_wolves" : "spotted_raiders", from ) );
				hero.interrupt();
			}
		}
		if (shownNew && HomeThreats.live( level )) Dungeon.observe();
	}

	//is this mob shown to the hero by a tower already?
	private static boolean shown( Hero hero, Mob m ){
		for (TalismanOfForesight.CharAwareness a : hero.buffs( TalismanOfForesight.CharAwareness.class )){
			if (a.charID == m.id()) return true;
		}
		return false;
	}

	// ----------------------------------------------------------------- the bundle

	private static final String SPRUNG_KEYS = "threat_sprung_keys", SPRUNG_AT = "threat_sprung_at";

	@Override
	public void storeInBundle( Bundle bundle ){
		long[] keys = new long[sprung.size()];
		float[] at = new float[sprung.size()];
		int i = 0;
		for (HashMap.Entry<Long, Float> e : sprung.entrySet()){
			keys[i] = e.getKey();
			at[i++] = e.getValue();
		}
		bundle.put( SPRUNG_KEYS, keys );
		bundle.put( SPRUNG_AT, at );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		sprung.clear();
		if (!bundle.contains( SPRUNG_KEYS ) || !bundle.contains( SPRUNG_AT )) return;
		long[] keys = bundle.getLongArray( SPRUNG_KEYS );
		float[] at = bundle.getFloatArray( SPRUNG_AT );
		for (int i = 0; i < Math.min( keys.length, at.length ); i++) sprung.put( keys[i], at[i] );
	}
}
