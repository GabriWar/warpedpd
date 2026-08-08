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

package xyz.gabriwar.warpedpixeldungeon.levels;

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderNest;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderQueen;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.CavesPainter;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.Room;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Spider Nest, reimplemented from Remixed PD. Remixed ran it as nodes 6s-10s off the
//depth-5 boss, and crucially the branch LOOPS: descending past the deepest floor puts
//you back at the top, so it is a repeatable farm rather than a dead end.
public class SpiderNestLevel extends RegularLevel {

	public static final int SPIDER_BRANCH = 5;
	public static final int FIRST_DEPTH   = 6;
	public static final int LAST_DEPTH    = 10;

	{
		color1 = 0x3b2f4a;
		color2 = 0x6e6480;

		viewDistance = Math.min(6, viewDistance);
	}

	@Override
	public void playLevelMusic() {
		Music.INSTANCE.playTracks(
				CavesLevel.CAVES_TRACK_LIST,
				CavesLevel.CAVES_TRACK_CHANCES,
				false);
	}

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_SPIDERNEST;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_CAVES;
	}

	@Override
	protected int standardRooms(boolean forceMax) {
		//the nest widens the deeper it goes, as it did in Remixed
		int bonus = Dungeon.depth - FIRST_DEPTH;
		if (forceMax) return 8 + bonus;
		return 7 + Random.chances(new float[]{2, 1}) + bonus;
	}

	@Override
	protected int specialRooms(boolean forceMax) {
		if (forceMax) return 2;
		return 1 + Random.chances(new float[]{3, 1});
	}

	@Override
	protected Painter painter() {
		return new CavesPainter()
				.setWater(feeling == Feeling.WATER ? 0.55f : 0.15f, 5)
				.setGrass(feeling == Feeling.GRASS ? 0.60f : 0.35f, 4)
				.setTraps(nTraps(), trapClasses(), trapChances());
	}

	//The loop, which is the whole point of the nest: descending from the deepest floor
	//drops you back at the top instead of into the main dungeon, so it can be farmed.
	//RegularLevel's exit defaults to depth+1, so the transition is rewritten after build.
	@Override
	protected void createMobs() {
		super.createMobs();

		if (Dungeon.depth == LAST_DEPTH) {
			//the queen holds the bottom of the nest
			int qCell;
			do {
				qCell = randomRespawnCell( null );
			} while (qCell == -1);
			SpiderQueen queen = new SpiderQueen();
			queen.pos = qCell;
			mobs.add( queen );
		} else {
			//a rooted egg sac on the way down, one per floor
			int nCell;
			do {
				nCell = randomRespawnCell( null );
			} while (nCell == -1);
			SpiderNest nest = new SpiderNest();
			nest.pos = nCell;
			mobs.add( nest );
		}
	}

	@Override
	protected boolean build() {
		if (!super.build()) return false;

		for (LevelTransition t : transitions) {
			//the loop: descending from the deepest floor returns to the top of the nest
			if (t.type == LevelTransition.Type.REGULAR_EXIT && Dungeon.depth >= LAST_DEPTH) {
				t.destDepth  = FIRST_DEPTH;
				t.destBranch = SPIDER_BRANCH;
				t.destType   = LevelTransition.Type.REGULAR_ENTRANCE;
			}
			//climbing out of the top floor puts you back in the prison
			if (t.type == LevelTransition.Type.REGULAR_ENTRANCE && Dungeon.depth <= FIRST_DEPTH) {
				t.destDepth  = PrisonLevel.SPIDER_BRANCH_DEPTH;
				t.destBranch = 0;
				t.destType   = LevelTransition.Type.BRANCH_EXIT;
			}
		}

		return true;
	}

	@Override
	public String tileName( int tile ) {
		switch (tile) {
			case Terrain.HIGH_GRASS:
				return Messages.get(SpiderNestLevel.class, "high_grass_name");
			case Terrain.GRASS:
				return Messages.get(SpiderNestLevel.class, "grass_name");
			default:
				return super.tileName( tile );
		}
	}

	@Override
	public String tileDesc( int tile ) {
		switch (tile) {
			case Terrain.HIGH_GRASS:
				return Messages.get(SpiderNestLevel.class, "high_grass_desc");
			case Terrain.EMPTY_DECO:
				return Messages.get(SpiderNestLevel.class, "empty_deco_desc");
			default:
				return super.tileDesc( tile );
		}
	}
}
