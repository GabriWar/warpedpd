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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor;
import xyz.gabriwar.warpedpixeldungeon.effects.WorldEventDecor;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * A co-op guest's share of the world's timed events on the ground: a fallen star's streak, its
 * crater's smoke, flames and glow, and a travelling market's maypole and bunting. The host shows
 * his on his hero's step (OverworldLevel.tickEvents), which a guest's mirror never takes; this,
 * among the surface's visuals (OverworldLevel.addVisuals), works the same out on the render
 * thread whenever the turn or the guest's hero's world cell changes - from the world itself
 * (WorldEvents.eventsNear, pure) and the log its host shipped it (adoptSharedEvents: the
 * markets chased off, the events he forced), the traders being the host's stand-ins round each
 * market's well - through the host's own OverworldLevel.eventDecor, so both screens show the
 * same. A star is seen to fall, as the host sees one, when it comes down within earshot of the
 * guest's own hero (or in his window) and is still fresh. A mirror re-labels its window without
 * sliding the scene, so what stands on the ground is moved with it by hand first, as
 * SettlementAmbience.Mirror does. On a host, or alone, it does nothing.
 */
final class EventDecorMirror extends Gizmo {

	private final OverworldLevel level;
	private final WorldEventDecor decor = new WorldEventDecor();
	//the window origin what is shown was placed in
	private int visX, visY;
	//when it was last worked out, for which scene, and what it came to
	private int lastTurn = Integer.MIN_VALUE, lastWX, lastWY, lastScene;
	private List<WorldEventDecor.Crater> craters = Collections.emptyList();
	private List<WorldEventDecor.Fair> fairs = Collections.emptyList();
	//the events about the window, per centre sector, day and forced set (as nearEvents keeps them)
	private List<WorldEvents.Event> near = Collections.emptyList();
	private long nearKey = Long.MIN_VALUE;
	private int nearDay = Integer.MIN_VALUE, nearForced = -1;

	//the stars this guest has seen fall, and when they came down: one scene or the next, a star
	//falls for him once (render thread only)
	private static final HashMap<Long, Integer> FALLEN = new HashMap<>();

	EventDecorMirror( OverworldLevel level ){
		this.level = level;
		visX = level.worldX();
		visY = level.worldY();
	}

	@Override
	public void update(){
		if (!NetManager.isNetClient() || Dungeon.level != level || Dungeon.hero == null) return;
		Object scene = Game.scene();
		if (!(scene instanceof GameScene)) return;
		int sceneId = System.identityHashCode( scene );
		int ox = level.worldX(), oy = level.worldY();
		if (ox != visX || oy != visY){
			//what is on screen stands where the old origin put it
			if (lastScene == sceneId) decor.slide( (visX - ox) * DungeonTilemap.SIZE, (visY - oy) * DungeonTilemap.SIZE );
			visX = ox;
			visY = oy;
			lastTurn = Integer.MIN_VALUE;
		}
		int pos = Dungeon.hero.pos, w = level.width();
		if (pos < 0 || pos >= level.length()) return;
		int hwx = ox + pos % w, hwy = oy + pos / w;
		int turn = level.eventLog().peekClock( WorldClock.turn() );
		if (lastScene == sceneId && turn == lastTurn && hwx == lastWX && hwy == lastWY) return;
		lastTurn = turn;
		lastWX = hwx;
		lastWY = hwy;

		long t = LagMonitor.begin();
		ArrayList<WorldEventDecor.Crater> c = new ArrayList<>();
		ArrayList<WorldEventDecor.Fair> f = new ArrayList<>();
		gather( turn, c, f );
		if (lastScene != sceneId || !WorldEventDecor.same( c, f, craters, fairs )) decor.show( sceneId, c, f );
		lastScene = sceneId;
		craters = c;
		fairs = f;
		fall( turn, pos, hwx, hwy );
		LagMonitor.end( "OW events mirror", t );
	}

	/** What this guest's screen shows of the window's events now, as the host's step would lay
	 *  it out (OverworldLevel.eventDecor). */
	void gather( int turn, List<WorldEventDecor.Crater> c, List<WorldEventDecor.Fair> f ){
		level.eventDecor( events( turn ), turn, level::guestTradesFor, c, f );
	}

	//the events that can reach the window: anchored within EVENT_RADIUS sectors of its centre
	private List<WorldEvents.Event> events( int turn ){
		int cx = Math.floorDiv( level.worldX() + level.width() / 2, WorldStructures.SECTOR );
		int cy = Math.floorDiv( level.worldY() + level.height() / 2, WorldStructures.SECTOR );
		int day = WorldEvents.day( turn );
		long key = WorldStructures.sectorOf( cx, cy );
		WorldEventLog log = level.eventLog();
		if (day != nearDay || key != nearKey || log.forcedVersion() != nearForced){
			near = WorldEvents.eventsNear( level.worldSeed(), cx, cy, OverworldLevel.EVENT_RADIUS, day, log.forced );
			nearDay = day;
			nearKey = key;
			nearForced = log.forcedVersion();
		}
		return near;
	}

	//a fresh star heard of from where this guest's hero stands - within its kind's reach of his
	//sector, or come down in his window - falls on his screen once, as the host's does on his
	private void fall( int turn, int heroPos, int hwx, int hwy ){
		FALLEN.values().removeIf( start -> turn - start >= WorldEvents.STAR_FRESH );
		int hsx = Math.floorDiv( hwx, WorldStructures.SECTOR ), hsy = Math.floorDiv( hwy, WorldStructures.SECTOR );
		for (WorldEvents.Event e : near){
			if (e.type != WorldEvents.Type.FALLEN_STAR || FALLEN.containsKey( e.id )) continue;
			int since = turn - e.startTurn;
			if (since < 0 || since >= WorldEvents.STAR_FRESH) continue;
			boolean heard = WorldEvents.within( e, hsx, hsy, WorldEvents.Type.FALLEN_STAR.announceSectors )
					|| level.localCell( e.wx, e.wy ) != -1;
			if (!heard) continue;
			FALLEN.put( e.id, e.startTurn );
			level.showFall( e, heroPos, hwx, hwy, turn );
		}
	}
}
