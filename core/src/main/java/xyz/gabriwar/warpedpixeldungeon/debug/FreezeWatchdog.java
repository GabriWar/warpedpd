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

import java.util.Map;

/**
 * A freeze leaves no crash log: the game just stops and the player kills it. This
 * watches the two threads that can hang (render and actor) from a third one, and
 * when either stops making progress it throws with the stack of every thread, so
 * the freeze arrives through the same report screen a crash does.
 *
 * It counts its own one-second ticks rather than reading a clock: when the system
 * freezes the whole process in the background this thread is frozen with it, and
 * nothing looks stalled on the way back.
 */
public class FreezeWatchdog {

	//ticks (seconds) without progress before each kind of stall is reported
	private static final int RENDER_STALL = 15;
	private static final int TURN_SPIN    = 10;
	private static final int ACT_STALL    = 20;
	private static final int SPRITE_STALL = 15;

	private static volatile boolean inGame = false;
	private static volatile boolean paused = true;
	private static volatile long frames = 0;

	private static volatile long turnLoops = 0;
	private static volatile float turnTime = 0;
	private static volatile String acting = null;
	private static volatile boolean inAct = false;
	private static volatile boolean inSpriteWait = false;

	private static Thread watcher;

	//once the clock has stood still for TRACE ticks, every turn handed back for free is caught where
	//it happens: the stack of that hand-back (Actor.next) is the line that spins, which the stacks
	//of a spinning loop never show (between turns the actor thread only waits)
	private static final int TRACE = 2;
	public static volatile boolean tracing = false;
	private static volatile String freeWho = null;
	private static volatile Throwable freeAt = null;
	private static volatile long freeCount = 0;

	//what the turns of the second just gone did while tracing: one paid for itself (the actor's own
	//clock went past the game's), one went for free
	private static volatile boolean paid = false, free = false;

	/** Actor.next with the clock where it was, while tracing: this is where the turn went back. */
	public static void freeTurn( Object actor, Throwable where ){
		freeWho = actor.getClass().getName() + " handed its turn back without spending time";
		freeAt = where;
		freeCount++;
		free = true;
	}

	/** An act() that returned true (go on) without spending time, while tracing. */
	public static void freeAct( Object actor ){
		freeWho = actor.getClass().getName() + ".act() returned true without spending time";
		freeAt = null;
		freeCount++;
		free = true;
	}

	/** A turn handed back or gone on with the actor's clock past the game's, while tracing: paid for. */
	public static void paidTurn(){
		paid = true;
	}

	/** the game scene came up (true) or went away (false): only a run is watched */
	public static synchronized void inGame( boolean value ){
		inGame = value;
		paused = true;   //until the first frame says otherwise
		if (value && watcher == null){
			watcher = new Thread( FreezeWatchdog::watch, "WPD Freeze Watchdog" );
			watcher.setDaemon( true );
			watcher.start();
		}
	}

	/** once a frame, from the game scene */
	public static void frame(){
		frames++;
		paused = false;
	}

	/** the app went to the background: the render thread stops on purpose */
	public static void paused(){
		paused = true;
	}

	/** the turn loop picked an actor, at this game time */
	public static void turn( Object actor, float time ){
		turnLoops++;
		turnTime = time;
		acting = actor.getClass().getName();
	}

	public static void spriteWait( boolean value ){ inSpriteWait = value; }

	public static void act( boolean value ){ inAct = value; }

	//turns the player takes for free are no spin. a hero stopped before a lock or in roots and tapped
	//again and again, or walking through a time freeze or a swiftthistle's bubble, leaves the clock
	//where it was; ten seconds of that threw this "freeze" and closed a game that never hung (most of
	//the reports it sent). a second in which the loop stood waiting on the hero's player, or in which
	//the clock was held still on purpose, is a second the game was not stuck in
	private static volatile long playerWaits = 0;
	private static volatile long heldClock = 0;

	/** The turn loop parked with a hero ready: it waits on his player, who has the game in hand. */
	public static void playerWait(){ playerWaits++; }

	/** A turn's time went into a time freeze or a time bubble (Char.spendConstant): the clock stands still by design. */
	public static void clockHeld(){ heldClock++; }

	private static void watch(){
		while (true){
			try {
				Thread.sleep( 1000 );
			} catch (InterruptedException e){
				return;
			}

			String what = tick();
			if (what != null){
				throw new RuntimeException( report( what ) );
			}
		}
	}

	private static long lastFrames = -1, lastLoops = -1, lastPlayerWaits = -1, lastHeldClock = -1;
	private static float lastTime = Float.NaN;
	private static int renderStall = 0, spin = 0, actStall = 0, spriteStall = 0;

	/** One second's look at the two threads: what has stalled, or null. */
	static String tick(){
		long f = frames, l = turnLoops, w = playerWaits, h = heldClock;
		float t = turnTime;
		boolean rendering = f != lastFrames;
		boolean playersOwn = w != lastPlayerWaits || h != lastHeldClock;
		//a traced second whose turns paid for themselves, none for free: a crowd at one game time, each
		//taking its turn in earnest, holds the clock still without hanging. Fifty piranhas set on each
		//other by a scroll of rage, all in sight through mind vision, held it at 100.5 for over ten
		//seconds: thirty bit there, one after another, every bite waiting on its animation
		boolean paidFor = paid && !free;
		paid = free = false;

		if (!inGame || paused){
			renderStall = spin = actStall = spriteStall = 0;
			tracing = false;
		} else {
			renderStall = rendering ? 0 : renderStall + 1;
			//actors taking turns while the game clock stands still: something acts for free
			spin = (l != lastLoops && t == lastTime && !playersOwn && !paidFor) ? spin + 1 : 0;
			if (spin == 0){
				tracing = false;
				freeWho = null;
				freeAt = null;
				freeCount = 0;
			} else if (spin >= TRACE){
				tracing = true;
			}
			actStall = (l == lastLoops && inAct) ? actStall + 1 : 0;
			//a move animation that never ends, with the screen still drawing
			spriteStall = (l == lastLoops && inSpriteWait && rendering) ? spriteStall + 1 : 0;
		}

		lastFrames = f;
		lastLoops = l;
		lastTime = t;
		lastPlayerWaits = w;
		lastHeldClock = h;

		String what = null;
		if (renderStall >= RENDER_STALL)      what = "render thread stalled for " + renderStall + "s";
		else if (spin >= TURN_SPIN)           what = "turn loop spinning for " + spin + "s with game time stuck at " + t + cause();
		else if (actStall >= ACT_STALL)       what = "one act() running for " + actStall + "s";
		else if (spriteStall >= SPRITE_STALL) what = "actor thread waiting " + spriteStall + "s on a sprite that never stops moving";
		return what;
	}

	//what the spin trace caught, with the stack of the free hand-back: the exact line
	static String cause(){
		String who = freeWho;
		Throwable at = freeAt;
		if (who == null) return "\ncause: not caught (no free hand-back or free act while tracing)";
		StringBuilder sb = new StringBuilder( "\ncause: " ).append( who )
				.append( " (" ).append( freeCount ).append( " times while tracing)" );
		if (at != null){
			for (StackTraceElement el : at.getStackTrace()) sb.append( "\n    at " ).append( el );
		}
		return sb.toString();
	}

	private static boolean ours( Thread t ){
		return t.getName().contains( "Actor" ) || t.getName().contains( "GL" ) || t.getName().contains( "main" );
	}

	private static String report( String what ){
		StringBuilder sb = new StringBuilder( "FREEZE: " ).append( what )
				.append( "\nlast actor: " ).append( acting ).append( "\n" );
		//the two threads that matter first, in case the report gets cut short
		java.util.ArrayList<Map.Entry<Thread, StackTraceElement[]>> threads = new java.util.ArrayList<>( Thread.getAllStackTraces().entrySet() );
		java.util.Collections.sort( threads, (a, b) -> Boolean.compare( ours( b.getKey() ), ours( a.getKey() ) ) );
		for (Map.Entry<Thread, StackTraceElement[]> e : threads){
			if (e.getKey() == Thread.currentThread() || e.getValue().length == 0) continue;
			sb.append( "\n--- " ).append( e.getKey().getName() ).append( " (" ).append( e.getKey().getState() ).append( ")\n" );
			for (StackTraceElement el : e.getValue()) sb.append( "    at " ).append( el ).append( "\n" );
		}
		return sb.toString();
	}
}
