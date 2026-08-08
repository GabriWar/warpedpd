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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DemonLord;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.IceDemon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.keys.SkeletonKey;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Group;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//ported from Unleashed PD: the frozen branch's arena. Pillared ice hall that
//seals when you step off the entry chamber, then fills with the demon lord and
//his ice demons. Killing him drops the key that opens the way out.
public class FrozenBossLevel extends Level {

	{
		color1 = 0x484876;
		color2 = 0x4b5999;
		viewDistance = 5;
	}

	private static final int SIZE = 32;

	private int arenaLeft, arenaRight, arenaTop, arenaBottom;
	private int stairs = -1;
	private boolean enteredArena = false;
	private boolean keyDropped = false;

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_FROZEN;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_FROZEN;
	}

	private static final String STAIRS  = "stairs";
	private static final String ENTERED = "entered";
	private static final String DROPPED = "dropped";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( STAIRS, stairs );
		bundle.put( ENTERED, enteredArena );
		bundle.put( DROPPED, keyDropped );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		stairs = bundle.getInt( STAIRS );
		enteredArena = bundle.getBoolean( ENTERED );
		keyDropped = bundle.getBoolean( DROPPED );
	}

	@Override
	protected boolean build() {

		setSize( SIZE, SIZE );

		arenaLeft   = width() / 2 - 1;
		arenaRight  = width() / 2 + 1;
		arenaTop    = height() / 2 - 1;
		arenaBottom = height() / 2 + 1;

		int exitPos = -1;

		//five vertical ice galleries, the middle one carrying the way out
		for (int i = 0; i < 5; i++) {
			int top    = Random.IntRange( 2, arenaTop - 1 );
			int bottom = Random.IntRange( arenaBottom + 1, height() - 4 );
			Painter.fill( this, 2 + i * 5, top, 4, bottom - top + 1, Terrain.EMPTY );

			if (i == 2) {
				exitPos = (2 + i * 5 + 1) + (top - 1) * width();
			}

			//seams of ice in the gallery walls
			for (int j = 0; j < 4; j++) {
				if (Random.Int( 2 ) == 0) {
					int y = Random.IntRange( top + 1, bottom - 1 );
					map[2 + i * 5 + j + y * width()] = Terrain.WALL_DECO;
				}
			}
		}

		map[exitPos] = Terrain.LOCKED_EXIT;
		transitions.add( new xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition(
				this, exitPos,
				xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition.Type.REGULAR_EXIT ) );

		//the sealed entry chamber in the middle
		Painter.fill( this, arenaLeft - 1, arenaTop - 1,
				arenaRight - arenaLeft + 3, arenaBottom - arenaTop + 3, Terrain.WALL );
		Painter.fill( this, arenaLeft, arenaTop,
				arenaRight - arenaLeft + 1, arenaBottom - arenaTop + 1, Terrain.EMPTY );

		int entrancePos = Random.IntRange( arenaLeft, arenaRight )
				+ Random.IntRange( arenaTop, arenaBottom ) * width();
		map[entrancePos] = Terrain.ENTRANCE;
		transitions.add( new xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition(
				this, entrancePos,
				xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition.Type.REGULAR_ENTRANCE ) );

		//meltwater pooling across the floor
		boolean[] patch = Patch.generate( width(), height(), 0.45f, 6, true );
		for (int i = 0; i < length(); i++) {
			if (map[i] == Terrain.EMPTY && patch[i]) {
				map[i] = Terrain.WATER;
			}
		}

		return true;
	}

	@Override
	protected void createMobs() {
		//the arena stays empty until the hero steps out of the entry chamber
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

	private boolean insideEntryChamber( int cell ) {
		int x = cell % width(), y = cell / width();
		return x >= arenaLeft && x <= arenaRight && y >= arenaTop && y <= arenaBottom;
	}

	@Override
	public void occupyCell( Char ch ) {
		super.occupyCell( ch );

		if (!enteredArena && ch == Dungeon.hero && !insideEntryChamber( ch.pos )) {
			enteredArena = true;
			seal();

			//the chamber walls freeze over behind you
			for (int i = arenaLeft - 1; i <= arenaRight + 1; i++) {
				freeze( (arenaTop - 1) * width() + i );
				freeze( (arenaBottom + 1) * width() + i );
			}
			for (int i = arenaTop; i <= arenaBottom; i++) {
				freeze( i * width() + arenaLeft - 1 );
				freeze( i * width() + arenaRight + 1 );
			}

			GameScene.updateMap();
			Dungeon.observe();

			DemonLord boss = new DemonLord();
			boss.state = boss.HUNTING;
			int tries = 0;
			do {
				boss.pos = Random.Int( length() );
			} while (!passable[boss.pos]
					|| insideEntryChamber( boss.pos )
					|| (heroFOV[boss.pos] && tries++ < 20));
			GameScene.add( boss );

			for (int i = 0; i < 8; i++) {
				Mob mob = new IceDemon();
				mob.state = mob.HUNTING;
				int t = 0;
				do {
					mob.pos = Random.Int( length() );
					if (t++ > 50) break;
				} while (!passable[mob.pos]
						|| insideEntryChamber( mob.pos )
						|| Actor.findChar( mob.pos ) != null);
				if (passable[mob.pos] && Actor.findChar( mob.pos ) == null) {
					GameScene.add( mob );
				}
			}
		}
	}

	private void freeze( int cell ) {
		set( cell, Terrain.EMPTY_SP );
		CellEmitter.get( cell ).start( SnowParticle.FACTORY, 0.1f, 3 );
	}

	@Override
	public Heap drop( Item item, int cell ) {
		if (!keyDropped && item instanceof SkeletonKey) {
			keyDropped = true;
			unseal();
			GameScene.updateMap();
			Dungeon.observe();
		}
		return super.drop( item, cell );
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
			case Terrain.WATER:
				return Messages.get(FrozenLevel.class, "water_name");
			default:
				return super.tileName( tile );
		}
	}

	@Override
	public String tileDesc( int tile ) {
		switch (tile) {
			case Terrain.WATER:
				return Messages.get(FrozenLevel.class, "water_desc");
			case Terrain.WALL_DECO:
				return Messages.get(FrozenLevel.class, "wall_deco_desc");
			default:
				return super.tileDesc( tile );
		}
	}
}
