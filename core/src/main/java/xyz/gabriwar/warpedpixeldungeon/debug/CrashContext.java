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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;

/**
 * What a crash or freeze report says about the game beside the stack: the hero and what he was
 * doing, the level, the last turns the loop took (who acted, the clock before and after) and the
 * last lines of the game log. The turns and the log lines are kept as they happen in fixed rings
 * (nothing allocated a turn); describe() reads them when a report is written.
 */
public final class CrashContext {

	private static final int TURNS = 24, LINES = 10;

	//the last turns, the slot `turn` points at the next to fill. Written by the actor thread only
	private static final Class<?>[] who = new Class<?>[TURNS];
	private static final Class<?>[] action = new Class<?>[TURNS];
	private static final float[] before = new float[TURNS], after = new float[TURNS];
	//0 still acting (it never came back: the crash is in it), 1 handed on, 2 the loop waited
	private static final byte[] end = new byte[TURNS];
	private static volatile int turn = 0;

	private static final String[] lines = new String[LINES];
	private static volatile int line = 0;

	private CrashContext(){}

	/** The loop picked an actor at this time; for the hero, the action he set out on. */
	public static void begin( Actor a, float now, Class<?> heroAction ){
		int i = turn % TURNS;
		who[i] = a.getClass();
		action[i] = heroAction;
		before[i] = now;
		after[i] = Float.NaN;
		end[i] = 0;
		turn++;
	}

	/** The actor it picked last came back from its act, its clock now at `time`. */
	public static void end( float time, boolean doNext ){
		int i = (turn + TURNS - 1) % TURNS;
		after[i] = time;
		end[i] = (byte)(doNext ? 1 : 2);
	}

	/** A line of the game log. */
	public static void log( String text ){
		lines[line % LINES] = text;
		line++;
	}

	/** The game's state as text, for the end of a report. Read off a helper thread for at most a
	 *  second: the thread that crashed or froze may hold a lock the reading needs (a hero's buffs),
	 *  and a report must never hang on it. */
	public static String describe(){
		final StringBuilder sb = new StringBuilder();
		Thread t = new Thread( () -> write( sb ), "WPD Crash Context" );
		t.setDaemon( true );
		t.start();
		try {
			t.join( 1000 );
		} catch (InterruptedException ignored) {}
		synchronized (sb) {
			if (t.isAlive()) sb.append( "\n(the rest could not be read: a lock the game holds)\n" );
			return sb.toString();
		}
	}

	private static void write( StringBuilder sb ){
		part( sb, "\n--- game state ---\n", () -> hero( sb ) );
		part( sb, "", () -> level( sb ) );
		part( sb, "\nlast turns (oldest first; time before -> after; next = handed on, wait = loop waited):\n", () -> turns( sb ) );
		part( sb, "\nlast log lines:\n", () -> log( sb ) );
	}

	//each part on its own: one that fails must not cost the others
	private static void part( StringBuilder sb, String title, Runnable body ){
		synchronized (sb) {
			sb.append( title );
		}
		try {
			body.run();
		} catch (Throwable e) {
			synchronized (sb) {
				sb.append( "  (unreadable: " ).append( e ).append( ")\n" );
			}
		}
	}

	private static void hero( StringBuilder sb ){
		Hero h = Dungeon.hero;
		StringBuilder s = new StringBuilder();
		if (h == null){
			s.append( "hero: none\n" );
		} else {
			Level l = Dungeon.level;
			int w = l != null ? l.width() : 0;
			s.append( "hero: " ).append( h.heroClass ).append( '/' ).append( h.subClass )
					.append( " lvl " ).append( h.lvl ).append( ", HP " ).append( h.HP ).append( '/' ).append( h.HT )
					.append( ", cell " ).append( h.pos );
			if (w > 0) s.append( " (" ).append( h.pos % w ).append( ',' ).append( h.pos / w ).append( ')' );
			s.append( ", time " ).append( h.cooldown() + Actor.now() ).append( '\n' );
			s.append( "  ready " ).append( h.ready ).append( ", paralysed " ).append( h.paralysed )
					.append( ", resting " ).append( h.resting ).append( ", action " )
					.append( h.curAction == null ? "none" : h.curAction.getClass().getSimpleName() + " -> " + h.curAction.dst )
					.append( '\n' );
			s.append( "  buffs:" );
			for (Buff b : h.buffs()) s.append( ' ' ).append( b.getClass().getSimpleName() );
			s.append( '\n' );
		}
		synchronized (sb) {
			sb.append( s );
		}
	}

	private static void level( StringBuilder sb ){
		Level l = Dungeon.level;
		StringBuilder s = new StringBuilder( "level: " );
		s.append( l == null ? "none" : l.getClass().getSimpleName() )
				.append( ", depth " ).append( Dungeon.depth ).append( ", branch " ).append( Dungeon.branch )
				.append( ", now " ).append( Actor.now() );
		if (l instanceof OverworldLevel){
			OverworldLevel ow = (OverworldLevel) l;
			s.append( ", world window at " ).append( ow.worldX() ).append( ',' ).append( ow.worldY() )
					.append( " altitude " ).append( ow.altitude() );
		}
		synchronized (sb) {
			sb.append( s ).append( '\n' );
		}
	}

	private static void turns( StringBuilder sb ){
		StringBuilder s = new StringBuilder();
		int last = turn;
		for (int n = Math.max( 0, last - TURNS ); n < last; n++){
			int i = n % TURNS;
			if (who[i] == null) continue;
			s.append( "  " ).append( who[i].getSimpleName() );
			if (action[i] != null) s.append( " [" ).append( action[i].getSimpleName() ).append( ']' );
			s.append( ' ' ).append( before[i] ).append( " -> " );
			s.append( end[i] == 0 ? "(still acting)" : after[i] + (end[i] == 1 ? " next" : " wait") );
			s.append( '\n' );
		}
		synchronized (sb) {
			sb.append( s );
		}
	}

	private static void log( StringBuilder sb ){
		StringBuilder s = new StringBuilder();
		int last = line;
		for (int n = Math.max( 0, last - LINES ); n < last; n++){
			String text = lines[n % LINES];
			if (text != null) s.append( "  " ).append( text ).append( '\n' );
		}
		synchronized (sb) {
			sb.append( s );
		}
	}
}
