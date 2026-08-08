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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.tiles.CustomTilemap;

//A Remixed PD town building interior. The room is hand-authored art (see TownInteriors),
//so the base tileset is left transparent and the four Remixed layers supply everything.
//Terrain drives collision only. Stepping on the entrance returns you to the town.
public abstract class TownInteriorLevel extends Level {

	public static final int BRANCH = 6;

	protected abstract int[] terrain();
	protected abstract int mapWidth();
	protected abstract int mapHeight();
	protected abstract int entranceCell();
	//ground layers first, roof layer last (drawn above the hero)
	protected abstract CustomTilemap[] groundLayers();
	protected abstract CustomTilemap roofLayer();
	protected abstract void spawnFolk();

	//the town and its buildings are always fully visible
	@Override
	public boolean noFogOfWar() { return true; }

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_TOWN_BLANK;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_PRISON;
	}

	@Override
	protected boolean build() {
		setSize( mapWidth(), mapHeight() );
		map = terrain();

		buildFlagMaps();
		cleanWalls();

		//the doorway you came in by is the way back out to the town square
		transitions.add( new LevelTransition( this, entranceCell(), LevelTransition.Type.BRANCH_ENTRANCE ) );

		addArt();
		return true;
	}

	/**
	 * A network mirror of this interior: the host's authoritative map, the
	 * flag maps rebuilt off it, the way back out to the town and the authored
	 * art layers - which are the ONLY thing that draws here, the base tileset
	 * being deliberately transparent. No folk, no wares: the host owns those
	 * and ships them. noFogOfWar() still holds, so the room stays lamplit
	 * instead of blacking out its own wall ring.
	 *
	 * Never runs create()/build(): they would spawn this building's people.
	 *
	 * @return false when the wire's room is not this build's room, leaving the
	 *         level untouched - the caller must fall back to a generic one
	 *         rather than hand out a level with no map and no collections.
	 */
	public boolean applyNetworkMap( int[] map, int w, int h ) {
		if (map == null || w != mapWidth() || h != mapHeight() || map.length != w * h) return false;
		xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel.initNetworkCollections( this );
		setSize( w, h );
		System.arraycopy( map, 0, this.map, 0, map.length );
		buildFlagMaps();
		cleanWalls();
		transitions.add( new LevelTransition( this, entranceCell(), LevelTransition.Type.BRANCH_ENTRANCE ) );
		addArt();
		return true;
	}

	private void addArt() {
		for (CustomTilemap c : customTiles) {
			if (c instanceof xyz.gabriwar.warpedpixeldungeon.tiles.TownInteriors.Layer) return;
		}
		for (CustomTilemap g : groundLayers()) {
			g.setRect( 0, 0, mapWidth(), mapHeight() );
			customTiles.add( g );
		}
		CustomTilemap roof = roofLayer();
		roof.setRect( 0, 0, mapWidth(), mapHeight() );
		customWalls.add( roof );
	}

	//out through the door: onto the surface, on this building's doorway cell
	@Override
	public boolean activateTransition( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero, LevelTransition transition ) {
		xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel.arriveInTown(
				xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures.TOWN_DOORS[Dungeon.depth] );
		xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel.travelToSurface();
		return true;
	}

	@Override
	public void restoreFromBundle( com.watabou.utils.Bundle bundle ) {
		super.restoreFromBundle( bundle );
		addArt();
	}

	protected void place( Mob m, int cell ) {
		m.pos = cell;
		mobs.add( m );
	}

	@Override
	protected void createMobs() {
		spawnFolk();
	}

	@Override
	public Mob createMob() {
		return null;
	}

	@Override
	public Actor addRespawner() {
		return null;
	}

	@Override
	protected void createItems() {
	}
}
