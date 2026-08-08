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

import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.IceGuardianCore;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.IceGuardian;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.CagedKobold;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import java.util.ArrayList;
import com.watabou.utils.ColorMath;
import com.watabou.noosa.particles.PixelParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.Room;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.AmbientSnowParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.CavesPainter;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

//ported from Unleashed PD. Its FrozenLevel sat on the old RegularLevel generator;
//this rebuilds the region against Warped's room-graph builder, keeping the ice
//palette, the wetter-than-caves layout and the icy-water naming.
public class FrozenLevel extends RegularLevel {

	//the frozen branch runs 21-24; you arrive at the top
	public static final int FIRST_DEPTH = 21;

	{
		color1 = 0x484876;
		color2 = 0x4b5999;
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
		return Assets.Environment.TILES_FROZEN;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_FROZEN;
	}


	@Override
	protected int standardRooms(boolean forceMax) {
		if (forceMax) return 9;
		//8 to 9, average 8.33 - same density as the caves it is carved from
		return 8 + Random.chances(new float[]{2, 1});
	}

	@Override
	protected int specialRooms(boolean forceMax) {
		if (forceMax) return 3;
		//2 to 3, average 2.2
		return 2 + Random.chances(new float[]{4, 1});
	}

	@Override
	protected Painter painter() {
		//UL's frozen levels were markedly wetter than caves (meltwater) and sparsely grassed
		return new CavesPainter()
				.setWater(feeling == Feeling.WATER ? 0.90f : 0.45f, 6)
				.setGrass(feeling == Feeling.GRASS ? 0.55f : 0.10f, 3)
				.setTraps(nTraps(), trapClasses(), trapChances());
	}

	//the guardian core sits mid-branch, ringed by its guardians
	public static final int GUARDIAN_DEPTH = 23;

	@Override
	protected void createMobs() {
		super.createMobs();

		if (Dungeon.depth == GUARDIAN_DEPTH) {
			int coreCell;
			do {
				coreCell = randomRespawnCell( null );
			} while (coreCell == -1);

			IceGuardianCore core = new IceGuardianCore();
			core.pos = coreCell;
			mobs.add( core );

			//four guardians standing around it
			int placed = 0;
            for (int i : PathFinder.NEIGHBOURS8) {
				if (placed >= 4) break;
				int c = coreCell + i;
				if (c >= 0 && c < length() && passable[c] && findMob( c ) == null) {
					IceGuardian g = new IceGuardian();
					g.pos = c;
					mobs.add( g );
					placed++;
				}
			}
		}
		//one caged kobold per run, on the top floor of the branch
		if (Dungeon.depth == FIRST_DEPTH && !CagedKobold.Quest.spawned()) {
			int cell;
			do {
				cell = randomRespawnCell( null );
			} while (cell == -1);
			CagedKobold.Quest.spawn( this, cell );
		}
	}

	@Override
	protected boolean build() {
		if (!super.build()) return false;

		//climbing out of the top floor returns to the Halls floor holding the entrance
		if (Dungeon.depth <= FIRST_DEPTH) {
			for (LevelTransition t : transitions) {
				if (t.type == LevelTransition.Type.REGULAR_ENTRANCE) {
					t.destDepth  = HallsLevel.FROZEN_BRANCH_DEPTH;
					t.destBranch = 0;
					t.destType   = LevelTransition.Type.BRANCH_EXIT;
				}
			}
		}

		return true;
	}

	@Override
	public Group addVisuals() {
		super.addVisuals();
		addFrozenVisuals( this, visuals );
		return visuals;
	}

	//ambient snowfall, plus meltwater dripping from the ice formations in the walls
	public static void addFrozenVisuals( Level level, Group group ) {
		group.add( new Snowfall( level ) );
		for (int i = 0; i < level.length(); i++) {
			if (level.map[i] == Terrain.WALL_DECO) {
				group.add( new Melt( i ) );
			}
		}
	}

	private static class Snowfall extends Emitter {

		public Snowfall( Level level ) {
			super();
			pos( 0, 0, level.width() * DungeonTilemap.SIZE, level.height() * DungeonTilemap.SIZE );
			pour( AmbientSnowParticle.FACTORY, 0.15f );
		}
	}

	private static class Melt extends Emitter {

		private int pos;

		public Melt( int pos ) {
			super();
			this.pos = pos;
			PointF p = DungeonTilemap.tileCenterToWorld( pos );
			pos( p.x - 2, p.y + 1, 4, 0 );
			pour( FrostDrip.FACTORY, 0.06f );
		}

		@Override
		public void update() {
			if (visible = (pos < Dungeon.level.heroFOV.length && Dungeon.level.heroFOV[pos])) {
				super.update();
			}
		}
	}

	public static final class FrostDrip extends PixelParticle {

		public static final Emitter.Factory FACTORY = new Emitter.Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((FrostDrip) emitter.recycle( FrostDrip.class )).reset( x, y );
			}
		};

		public FrostDrip() {
			super();
			acc.y = 50;
			am = 0.5f;
			color( ColorMath.random( 0xc8e5f2, 0x4e78a0 ) );
			size( 2 );
		}

		public void reset( float x, float y ) {
			revive();
			this.x = x;
			this.y = y;
			speed.set( Random.Float( -2, +2 ), 0 );
			left = lifespan = 0.5f;
		}
	}

	@Override
	public String tileName( int tile ) {
		switch (tile) {
			case Terrain.WATER:
				return Messages.get(FrozenLevel.class, "water_name");
			case Terrain.GRASS:
				return Messages.get(FrozenLevel.class, "grass_name");
			case Terrain.HIGH_GRASS:
				return Messages.get(FrozenLevel.class, "high_grass_name");
			default:
				return super.tileName( tile );
		}
	}

	@Override
	public String tileDesc( int tile ) {
		switch (tile) {
			case Terrain.WATER:
				return Messages.get(FrozenLevel.class, "water_desc");
			case Terrain.EMPTY_DECO:
				return Messages.get(FrozenLevel.class, "empty_deco_desc");
			case Terrain.WALL_DECO:
				return Messages.get(FrozenLevel.class, "wall_deco_desc");
			default:
				return super.tileDesc( tile );
		}
	}
}
