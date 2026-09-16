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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DemonLord;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

//the frozen branch's arena: a round hall of ice with the demon lord waiting on a dais
//at its heart. Six ice pillars give cover from his breath, frozen pools ring the floor,
//a short hall leads in from the south and the locked way on sits to the north.
//Stepping into the hall wakes him and locks the floor; the way back up stays open.
public class FrozenBossLevel extends Level {

	{
		color1 = 0x484876;
		color2 = 0x4b5999;
		viewDistance = 8;
	}

	private static final int WIDTH  = 33;
	private static final int HEIGHT = 33;

	private static final int CX = 16;
	private static final int CY = 15;
	private static final int RADIUS = 8;

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_FROZEN;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_FROZEN;
	}

	private int cell( int x, int y ) {
		return x + y * width();
	}

	private boolean insideArena( int pos ) {
		int dx = pos % width() - CX, dy = pos / width() - CY;
		return dx * dx + dy * dy <= RADIUS * RADIUS + RADIUS;
	}

	@Override
	protected boolean build() {

		setSize( WIDTH, HEIGHT );
		Painter.fill( this, 0, 0, WIDTH, HEIGHT, Terrain.WALL );

		boolean[] pools = Patch.generate( WIDTH, HEIGHT, 0.35f, 4, true );
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				int dx = x - CX, dy = y - CY, d2 = dx * dx + dy * dy;
				if (d2 > RADIUS * RADIUS + RADIUS) continue;
				if (d2 <= 5) {
					map[cell( x, y )] = Terrain.EMPTY_SP;
				} else if (d2 >= 16 && pools[cell( x, y )]) {
					map[cell( x, y )] = Terrain.FROZEN_WATER;
				} else {
					map[cell( x, y )] = Terrain.EMPTY;
				}
			}
		}

		//six ice pillars in a ring around the dais
		for (int i = 0; i < 6; i++) {
			double a = Math.toRadians( 30 + 60 * i );
			int px = CX + (int) Math.round( 5 * Math.cos( a ) );
			int py = CY + (int) Math.round( 5 * Math.sin( a ) );
			map[cell( px, py )] = Terrain.WALL_DECO;
		}

		//frozen braziers at the four points of the compass, just inside the wall
		map[cell( CX - RADIUS + 1, CY )] = Terrain.STATUE_SP;
		map[cell( CX + RADIUS - 1, CY )] = Terrain.STATUE_SP;

		//south: a short hall and the chamber holding the way back up
		for (int y = CY + RADIUS; y <= CY + RADIUS + 2; y++) {
			map[cell( CX, y )] = Terrain.EMPTY;
		}
		Painter.fill( this, CX - 2, CY + RADIUS + 3, 5, 4, Terrain.EMPTY );
		int entrancePos = cell( CX, CY + RADIUS + 5 );
		map[entrancePos] = Terrain.ENTRANCE;
		transitions.add( new LevelTransition( this, entrancePos, LevelTransition.Type.REGULAR_ENTRANCE ) );

		//north: the locked way on, at the end of a short passage behind the dais
		for (int y = CY - RADIUS - 2; y <= CY - RADIUS; y++) {
			map[cell( CX, y )] = Terrain.EMPTY;
		}
		int exitPos = cell( CX, CY - RADIUS - 3 );
		map[exitPos] = Terrain.LOCKED_EXIT;
		transitions.add( new LevelTransition( this, exitPos, LevelTransition.Type.REGULAR_EXIT ) );

		return true;
	}

	@Override
	protected void createMobs() {
		DemonLord boss = new DemonLord();
		boss.pos = cell( CX, CY );
		mobs.add( boss );
	}

	@Override
	protected void createItems() {
	}

	@Override
	public Actor addRespawner() {
		return null;
	}

	@Override
	public int randomRespawnCell( Char ch ) {
		return -1;
	}

	@Override
	public void occupyCell( Char ch ) {
		super.occupyCell( ch );

		if (ch == Dungeon.hero && !locked && insideArena( ch.pos )) {
			for (Mob mob : mobs.toArray( new Mob[0] )) {
				if (mob instanceof DemonLord && mob.isAlive() && mob.state != mob.HUNTING) {
					mob.state = mob.HUNTING;
					mob.notice();
					break;
				}
			}
		}
	}

	private boolean bossAlive() {
		for (Mob mob : mobs) {
			if (mob instanceof DemonLord && mob.isAlive()) return true;
		}
		return false;
	}

	@Override
	public void playLevelMusic() {
		if (locked) {
			Music.INSTANCE.play( Assets.Music.HALLS_BOSS, true );
		} else if (bossAlive()) {
			Music.INSTANCE.end();
		} else {
			Music.INSTANCE.playTracks( CavesLevel.CAVES_TRACK_LIST, CavesLevel.CAVES_TRACK_CHANCES, false );
		}
	}

	@Override
	public void seal() {
		if (locked) return;
		super.seal();
		Statistics.qualifiedForBossChallengeBadge = true;

		Game.runOnRenderThread( new Callback() {
			@Override
			public void call() {
				Music.INSTANCE.play( Assets.Music.HALLS_BOSS, true );
			}
		} );
	}

	@Override
	public void unseal() {
		if (!locked) return;
		super.unseal();

		Game.runOnRenderThread( new Callback() {
			@Override
			public void call() {
				Music.INSTANCE.fadeOut( 5f, new Callback() {
					@Override
					public void call() {
						Music.INSTANCE.end();
					}
				} );
			}
		} );
	}

	@Override
	public Group addVisuals() {
		super.addVisuals();
		FrozenLevel.addFrozenVisuals( this, visuals );
		return visuals;
	}

	@Override
	public String tileName( int tile ) {
		switch (tile) {
			case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT:
				return Messages.get(FrozenLevel.class, "region_deco_name");
			case Terrain.STATUE:
			case Terrain.STATUE_SP:
				return Messages.get(FrozenLevel.class, "statue_name");
			case Terrain.WATER:
				return Messages.get(FrozenLevel.class, "water_name");
			default:
				return super.tileName( tile );
		}
	}

	@Override
	public String tileDesc( int tile ) {
		switch (tile) {
			case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT:
				return Messages.get(FrozenLevel.class, "region_deco_desc");
			case Terrain.STATUE:
			case Terrain.STATUE_SP:
				return Messages.get(FrozenLevel.class, "statue_desc");
			case Terrain.WATER:
				return Messages.get(FrozenLevel.class, "water_desc");
			case Terrain.WALL_DECO:
				return Messages.get(FrozenLevel.class, "wall_deco_desc");
			default:
				return super.tileDesc( tile );
		}
	}
}
