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


package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.journal.Guidebook;
import xyz.gabriwar.warpedpixeldungeon.journal.Document;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;

import java.util.ArrayList;

//a line of chevrons on the floor from the hero to where a new player should go
//next: the adventurer's guide while it lies on this level, then - once the intro
//is read and the dungeon is still unvisited - the gate into the dungeon. Laid along
//the walking path, each arrow turned toward the next step, pulsing in sequence so
//the eye runs down it.
//
//The path is found on the actor thread, once per hero turn (onHeroTurn): PathFinder
//keeps one static distance map, and searching from the render thread while the
//hero's own move is being planned corrupts it - the hero stalls and the frame drags
public class GuideTrail extends Group {

	private static final int MAX_STEPS = 80;

	//the steps of the current path, without the target's own cell, and a counter
	//the renderer watches to know they changed. Written on the actor thread
	private static int[] steps = new int[0];
	private static volatile int version = 0;
	private static volatile boolean refresh = false;

	private final ArrayList<Image> arrows = new ArrayList<>();
	private int shown = -1;
	private float time = 0;

	//reading a page costs no turn, so the trail would only appear after the next
	//step: ask for a fresh path now. It is found on the render thread only while the
	//hero waits for input, when the actor thread is idle and PathFinder is free
	public static void refresh() {
		refresh = true;
	}

	//actor thread: where to, and how. Nothing to show leaves an empty path
	public static void onHeroTurn() {
		refresh = false;
		find();
	}

	private static void find() {
		if (Dungeon.hero == null || Dungeon.level == null) return;
		int target = targetCell();
		int[] found = new int[0];
		if (target != -1 && target != Dungeon.hero.pos) {
			PathFinder.Path path = PathFinder.find( Dungeon.hero.pos, target, Dungeon.level.passable );
			if (path != null && path.size() <= MAX_STEPS) {
				found = new int[path.size()];
				for (int i = 0; i < found.length; i++) found[i] = path.get( i );
			}
		}
		steps = found;
		version++;
	}

	private static int targetCell() {
		for (Heap heap : Dungeon.level.heaps.valueList()) {
			if (heap.peek() instanceof Guidebook) return heap.pos;
		}
		//book read, dungeon never entered: the north gate - the dungeon's front door
		//(the plaza's staircase goes down to the mines) - while the town window shows it
		if (Dungeon.level instanceof OverworldLevel
				&& Statistics.deepestFloor == 0
				&& Document.ADVENTURERS_GUIDE.isPageRead( Document.GUIDE_INTRO )) {
			return ((OverworldLevel) Dungeon.level).townLocal( WorldStructures.TOWN_DUNGEON_GATE );
		}
		return -1;
	}

	@Override
	public void update() {
		super.update();
		if (refresh && Dungeon.hero != null && Dungeon.hero.ready) {
			refresh = false;
			find();
		}
		if (shown != version) {
			shown = version;
			relay( steps );
		}
		time += Game.elapsed;
		for (int i = 0; i < arrows.size(); i++) {
			arrows.get(i).alpha( 0.45f + 0.45f * (float)Math.max( 0, Math.sin( time * 5f - i * 0.7f ) ) );
		}
	}

	//one arrow per step, none on the target's own cell (the last step)
	private void relay( int[] path ) {
		clear();
		arrows.clear();
		for (int i = 0; i + 1 < path.length; i++) {
			place( path[i], path[i + 1] );
		}
	}

	private void place( int cell, int next ) {
		Image arrow = new Image( Assets.Effects.GUIDE_ARROW );
		PointF c = DungeonTilemap.tileCenterToWorld( cell );
		arrow.origin.set( arrow.width / 2, arrow.height / 2 );
		arrow.x = c.x - arrow.width / 2;
		arrow.y = c.y - arrow.height / 2;
		int w = Dungeon.level.width();
		int dx = next % w - cell % w, dy = next / w - cell / w;
		arrow.angle = (float)Math.toDegrees( Math.atan2( dy, dx ) );
		add( arrow );
		arrows.add( arrow );
	}
}
