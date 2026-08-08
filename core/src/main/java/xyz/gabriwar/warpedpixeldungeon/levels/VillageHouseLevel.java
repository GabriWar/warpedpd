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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Villager;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import com.watabou.utils.Bundle;

/**
 * The one room behind a village door: a single 9x8 cottage - hearth, bed and
 * table - lit from wall to wall. Every house in the world owns one, keyed by
 * the depth id its door hashes to (WorldStructures.houseInteriorDepth), so a
 * house you have furnished stays furnished. Stepping back onto the doorway
 * puts you down on that exact door cell out on the surface.
 */
public class VillageHouseLevel extends Level {

	public static final int BRANCH = 7;

	private static final int W = 9, H = 8;
	//the doorway: middle of the room's bottom row. it stays one cell in from
	//the shell's edge - the engine takes the outer ring of any level for solid
	private static final int ENTRANCE = 4 + (H-2) * W;

	//the door the hero came in by, in world coordinates. stamped by the
	//overworld on the way in (a hash id can serve two far-apart houses, so
	//the way out is re-aimed every visit rather than trusted from the save)
	private static boolean pendingDoor = false;
	private static int pendingWX, pendingWY;

	/** The next village house entered opens off this world cell. */
	public static void enterAt( int wx, int wy ){
		pendingDoor = true;
		pendingWX = wx;
		pendingWY = wy;
	}

	public int doorWX, doorWY;

	private void takePendingDoor(){
		if (!pendingDoor) return;
		pendingDoor = false;
		doorWX = pendingWX;
		doorWY = pendingWY;
	}

	//lamplit from wall to wall - a one-room cottage has nothing to hide
	@Override
	public boolean noFogOfWar() { return true; }

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_PRISON;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_PRISON;
	}

	@Override
	protected boolean build() {
		takePendingDoor();
		setSize( W, H );

		for (int i = 0; i < length(); i++) map[i] = Terrain.WALL;
		for (int y = 1; y < H-1; y++){
			for (int x = 1; x < W-1; x++){
				map[x + y * W] = Terrain.EMPTY_SP;
			}
		}
		//the hearth against the back wall, the bed in the west corner and the
		//table under the east window
		map[4 + W]     = Terrain.EMBERS;
		map[1 + W]     = Terrain.EMPTY_DECO;
		map[2 + W]     = Terrain.EMPTY_DECO;
		map[6 + 2 * W] = Terrain.BARRICADE;
		map[7 + 2 * W] = Terrain.BARRICADE;
		map[ENTRANCE]  = Terrain.DOOR;

		transitions.add( new LevelTransition( this, ENTRANCE, LevelTransition.Type.BRANCH_ENTRANCE ) );
		return true;
	}

	/**
	 * A network mirror of the cottage: the host's authoritative map over the
	 * same one room every house in the world has, and the door's world cell
	 * handed over EXPLICITLY - the static handoff is a host-side stamp set on
	 * the way through the door, and a client never walks through it. No
	 * resident: the host owns them and ships them. noFogOfWar() still holds.
	 *
	 * Never runs create()/build(): they would spawn the family.
	 *
	 * @return false when the wire's room is not this build's room, leaving the
	 *         level untouched - the caller must fall back to a generic one
	 *         rather than hand out a level with no map and no collections.
	 */
	public boolean applyNetworkMap( int doorWX, int doorWY, int[] map, int w, int h ){
		if (map == null || w != W || h != H || map.length != w * h) return false;
		OverworldLevel.initNetworkCollections( this );
		this.doorWX = doorWX;
		this.doorWY = doorWY;
		setSize( W, H );
		System.arraycopy( map, 0, this.map, 0, map.length );
		buildFlagMaps();
		cleanWalls();
		transitions.add( new LevelTransition( this, ENTRANCE, LevelTransition.Type.BRANCH_ENTRANCE ) );
		return true;
	}

	//half the houses are lived in; the family shares the look of the one door
	//they live behind, so the same house always holds the same face
	@Override
	protected void createMobs() {
		long h = houseHash();
		if ((h & 1L) != 0) return;
		Villager v = new Villager();
		v.look = (int)Math.floorMod( h >> 8, 3 );
		v.tint = (int)Math.floorMod( h >> 16, 8 );
		v.pos = 4 + 3 * W;
		mobs.add( v );
	}

	private long houseHash(){
		long h = 0x40025EL;
		h ^= doorWX * 0x9E3779B97F4A7C15L;
		h = Long.rotateLeft( h, 31 );
		h ^= doorWY * 0xC2B2AE3D27D4EB4FL;
		h *= 0xFF51AFD7ED558CCDL;
		return h ^ (h >>> 33);
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

	//out through the door: back onto the surface, on the very cell you knocked at
	@Override
	public boolean activateTransition( Hero hero, LevelTransition transition ) {
		OverworldLevel.arriveAt( doorWX, doorWY );
		OverworldLevel.travelToSurface();
		return true;
	}

	private static final String DOOR_WX = "door_wx";
	private static final String DOOR_WY = "door_wy";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( DOOR_WX, doorWX );
		bundle.put( DOOR_WY, doorWY );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		doorWX = bundle.getInt( DOOR_WX );
		doorWY = bundle.getInt( DOOR_WY );
		takePendingDoor();
	}
}
