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
import xyz.gabriwar.warpedpixeldungeon.effects.FestivalDecor;
import xyz.gabriwar.warpedpixeldungeon.effects.RoamerTracks;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A co-op guest's share of the festivals and the roaming beasts on the ground: a festival's
 * maypole, bunting and lanterns (FestivalEvent.fetes) and its band (FestivalEvent.music), and a
 * roaming beast's tracks (RoamerEvent.prints). The host shows his on his hero's step, which a
 * guest's mirror never takes; this, among the surface's visuals (OverworldLevel.addVisuals), works
 * the same out on the render thread whenever the turn or the guest's hero's world cell changes -
 * from the world itself (WorldEvents.eventsNear, pure) and the log its host shipped it (the
 * festivals and the beasts settled, the events he forced) - through the host's own builders, so
 * both screens show the same and both players hear the band from where each stands. A mirror
 * re-labels its window without sliding the scene, so what stands on the ground is moved with it
 * by hand first, as EventDecorMirror does. On a host, or alone, it does nothing.
 */
final class LifeDecorMirror extends Gizmo {

	private final OverworldLevel level;
	private final FestivalDecor festivals = new FestivalDecor();
	private final RoamerTracks tracks = new RoamerTracks();
	//the window origin what is shown was placed in
	private int visX, visY;
	//when it was last worked out, for which scene, and what it came to
	private int lastTurn = Integer.MIN_VALUE, lastWX, lastWY, lastScene;
	private List<FestivalDecor.Fete> fetes = Collections.emptyList();
	private List<RoamerTracks.Print> prints = Collections.emptyList();
	//the events about the window, per centre sector, day and forced set (as nearEvents keeps them)
	private List<WorldEvents.Event> near = Collections.emptyList();
	private long nearKey = Long.MIN_VALUE;
	private int nearDay = Integer.MIN_VALUE, nearForced = -1;

	LifeDecorMirror( OverworldLevel level ){
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
			if (lastScene == sceneId){
				float dx = (visX - ox) * DungeonTilemap.SIZE, dy = (visY - oy) * DungeonTilemap.SIZE;
				festivals.slide( dx, dy );
				tracks.slide( dx, dy );
			}
			visX = ox;
			visY = oy;
			lastTurn = Integer.MIN_VALUE;
		}
		int pos = Dungeon.hero.pos, w = level.width();
		if (pos < 0 || pos >= level.length()) return;
		int turn = level.eventLog().peekClock( WorldClock.turn() );
		//the band keeps its own time (a few seconds between phrases)
		FestivalEvent.music( level, events( turn ), pos );
		int hwx = ox + pos % w, hwy = oy + pos / w;
		if (lastScene == sceneId && turn == lastTurn && hwx == lastWX && hwy == lastWY) return;
		lastTurn = turn;
		lastWX = hwx;
		lastWY = hwy;

		long t = LagMonitor.begin();
		ArrayList<FestivalDecor.Fete> f = FestivalEvent.fetes( level, events( turn ), turn );
		ArrayList<RoamerTracks.Print> p = RoamerEvent.prints( level, events( turn ), turn );
		if (lastScene != sceneId || !FestivalDecor.same( f, fetes )) festivals.show( sceneId, f );
		if (lastScene != sceneId || !RoamerTracks.same( p, prints )) tracks.show( sceneId, p );
		lastScene = sceneId;
		fetes = f;
		prints = p;
		LagMonitor.end( "OW life events mirror", t );
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
}
