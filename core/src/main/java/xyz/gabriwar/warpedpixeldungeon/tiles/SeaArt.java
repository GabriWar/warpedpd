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

package xyz.gabriwar.warpedpixeldungeon.tiles;

import xyz.gabriwar.warpedpixeldungeon.effects.LighthouseLamp;
import xyz.gabriwar.warpedpixeldungeon.items.sea.Boat;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.SeaSites;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.NoosaScript;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.Tilemap;

import java.util.ArrayList;

/**
 * The sea's places as the surface shows them (levels/overworld/SeaSites, SeaWorld), drawn from one
 * sheet of their own (environment/custom_tiles/sea_sites.png, tools/assets/sea_art.py) in two
 * window-sized layers laid as the dressing's are: the ground layer under everyone (a wreck's hull, a
 * lighthouse's tower and apron, a treasure's mark in the sand, the palms' trunks, a moored boat's
 * hull) and the over layer above everyone (the lighthouse's lamp room, the palms' fronds - swaying in
 * the wind as the trees do - and a moored sailboat's mast). A lighthouse's lamp burns at night
 * (effects/LighthouseLamp).
 *
 * Everything is worked out from the level as it stands (its sea's places, its map, its moored boats)
 * whenever it is put on screen: a window adopted or moved, a boat moored or boarded, a palm felled, a
 * treasure dug up. A cell is drawn only while it still holds what the place laid: a felled palm, a dug
 * mark or a hull broken by a mod is gone.
 */
public final class SeaArt {

	public static final String TEXTURE = "environment/custom_tiles/sea_sites.png";

	//the sheet (16 tiles a row): the wreck lying east-west, its bow east then west (4x2 each); lying
	//north-south, its bow north (2x4); the lighthouse's lamp room, shaft and base; the treasure's
	//mark; three palms (trunk, fronds); a moored raft; a moored sailboat (hull, mast); the apron's
	//paving
	public static final int WRECK_EAST = 0, WRECK_WEST = 4, WRECK_NS = 8;
	public static final int LAMP = 10, SHAFT = 26, BASE = 42;
	public static final int DIG = 11;
	public static final int[] PALM_TRUNK = { 12, 13, 14 }, PALM_FRONDS = { 28, 29, 30 };
	public static final int RAFT = 15, SAIL_HULL = 31, SAIL_MAST = 27;
	public static final int[] APRON = { 43, 44, 45 };
	public static final int ROW = 16;

	private SeaArt(){}

	/** A layer of the sea's art: window-sized, its data swapped whole. */
	public static class Layer extends CustomTilemap {

		{
			texture = TEXTURE;
		}

		private int[] data;
		private final boolean sways;

		public Layer( boolean sways ){
			this.sways = sways;
		}

		void setData( int[] data ){
			this.data = data;
		}

		/** The tile on one window cell, -1 for none. */
		public int get( int cell ){
			int[] d = data;
			return d == null || cell < 0 || cell >= d.length ? -1 : d[cell];
		}

		@Override
		public Tilemap create(){
			return create( data != null ? data : new int[tileW * tileH], tileW );
		}

		@Override
		protected Tilemap tilemap(){
			WindSway.Batch sway = sways ? WindSway.batch( SWAY_ROWS ) : null;
			return sway == null ? super.tilemap() : new Swaying( texture, sway );
		}
	}

	//the fronds sway whole, as an oak's crown does (WindSway)
	static final int[] SWAY_ROWS = new int[64];
	static {
		for (int t : PALM_FRONDS) SWAY_ROWS[t] = DungeonTilemap.SIZE;
	}

	//the over layer under the open sky: its swaying tiles left out of its chunks and drawn after them
	private static final class Swaying extends Tilemap {

		private final WindSway.Batch sway;

		Swaying( Object texture, WindSway.Batch sway ){
			super( texture, new TextureFilm( texture, DungeonTilemap.SIZE, DungeonTilemap.SIZE ) );
			this.sway = sway;
		}

		@Override
		protected NoosaScript script(){
			return NoosaScript.get();
		}

		@Override
		protected boolean needsRender( int pos ){
			return super.needsRender( pos ) && !sway.moves( data[pos] );
		}

		@Override
		public void draw(){
			super.draw();
			sway.draw( this, matrix, script(), texture, tileset, data, mapWidth, mapHeight );
		}

		@Override
		public void destroy(){
			super.destroy();
			sway.destroy();
		}
	}

	/** What one level shows of the sea: its two layers and its lamps. Kept by the level, never saved. */
	public static final class Shown {
		private Layer ground, over;
		private final ArrayList<LighthouseLamp> lamps = new ArrayList<>();
	}

	/**
	 * Lays the sea's art for the level's window and puts it on screen (render thread): the layers'
	 * data worked out afresh and refilled (only the chunks that changed are rebuilt), slid along first
	 * when the window moved by (dcx, dcy); a lamp on every lighthouse standing in the window.
	 */
	public static void present( OverworldLevel level, Shown shown, int dcx, int dcy ){
		if (level.altitude() != 0) return;
		int w = level.width(), h = level.height(), len = level.length();
		int[] ground = new int[len], over = new int[len];
		java.util.Arrays.fill( ground, -1 );
		java.util.Arrays.fill( over, -1 );
		ArrayList<int[]> lights = new ArrayList<>();
		lay( level, ground, over, lights );
		if (shown.ground == null){
			shown.ground = new Layer( false );
			shown.over = new Layer( true );
		}
		Layer[] all = { shown.ground, shown.over };
		int[][] data = { ground, over };
		for (int i = 0; i < 2; i++){
			if (dcx != 0 || dcy != 0) all[i].shiftVisual( dcx, dcy );
			all[i].setRect( 0, 0, w, h );
			all[i].setData( data[i] );
			GameScene.add( all[i], i == 1 );
		}
		for (LighthouseLamp l : shown.lamps) l.killAndErase();
		shown.lamps.clear();
		for (int[] l : lights){
			LighthouseLamp lamp = new LighthouseLamp( l[0], l[1] );
			shown.lamps.add( lamp );
			GameScene.effectOverFog( lamp );
		}
	}

	/** The tiles of the sea's places on the level's window (pure but for reading the level): the
	 *  ground and over layers' data, and the lamp cells {x, y} of the lighthouses standing. */
	public static void lay( OverworldLevel level, int[] ground, int[] over, ArrayList<int[]> lamps ){
		int w = level.width();
		for (SeaSites.Sector s : level.sea().sectors()){
			if (s.wreck != null) wreck( level, s.wreck, ground );
			if (s.light != null) light( level, s.light, ground, over, lamps );
			if (s.island != null) island( level, s.island, ground, over );
		}
		for (int[] m : level.sea().moorings()){
			int c = level.localCell( m[0], m[1] );
			if (c == -1) continue;
			if (m[2] == Boat.Kind.SAILBOAT.ordinal()){
				ground[c] = SAIL_HULL;
				if (c - w >= 0) over[c - w] = SAIL_MAST;
			} else {
				ground[c] = RAFT;
			}
		}
	}

	private static void wreck( OverworldLevel level, SeaSites.Wreck wr, int[] ground ){
		for (int y = wr.y0; y <= wr.y1(); y++){
			for (int x = wr.x0; x <= wr.x1(); x++){
				int c = level.localCell( x, y );
				if (c == -1) continue;
				int t = level.map[c];
				if (t != Terrain.CUSTOM_DECO && t != Terrain.CUSTOM_DECO_EMPTY) continue;
				int lx = x - wr.x0, ly = y - wr.y0;
				int base = wr.upright ? WRECK_NS : wr.bowWest ? WRECK_WEST : WRECK_EAST;
				ground[c] = base + lx + ly * ROW;
			}
		}
	}

	private static void light( OverworldLevel level, SeaSites.Light l, int[] ground, int[] over, ArrayList<int[]> lamps ){
		int base = level.localCell( l.x, l.y ), shaft = level.localCell( l.x, l.y - 1 );
		if (base == -1 || level.map[base] != Terrain.CUSTOM_DECO) return;
		ground[base] = BASE;
		if (shaft != -1 && level.map[shaft] == Terrain.CUSTOM_DECO){
			ground[shaft] = SHAFT;
			int lamp = level.localCell( l.x, l.y - 2 );
			if (lamp != -1){
				over[lamp] = LAMP;
				lamps.add( new int[]{ l.x, l.y - 2 } );
			}
		}
		for (int dx = -1; dx <= 1; dx++){
			int c = level.localCell( l.x + dx, l.y + 1 );
			if (c != -1 && level.map[c] == Terrain.CUSTOM_DECO_EMPTY) ground[c] = APRON[dx + 1];
		}
	}

	private static void island( OverworldLevel level, SeaSites.Island isl, int[] ground, int[] over ){
		int w = level.width();
		if (isl.digX != Integer.MIN_VALUE){
			int c = level.localCell( isl.digX, isl.digY );
			if (c != -1 && level.map[c] == Terrain.EMPTY_SP && !level.sea().dug( isl )) ground[c] = DIG;
		}
		if (isl.climate != SeaSites.Climate.PALM) return;
		for (int y = isl.y0; y <= isl.y1; y++){
			for (int x = isl.x0; x <= isl.x1; x++){
				int c = level.localCell( x, y );
				if (c == -1 || !level.sea().palmAt( c )) continue;
				int v = (int) Math.floorMod( WindowGenerator.dressHash( level.worldSeed(), x, y, 0x9A1A5L ), (long) PALM_TRUNK.length );
				ground[c] = PALM_TRUNK[v];
				if (c - w >= 0) over[c - w] = PALM_FRONDS[v];
			}
		}
	}
}
