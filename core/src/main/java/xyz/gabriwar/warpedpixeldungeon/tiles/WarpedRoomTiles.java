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

import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.noosa.Game;
import com.watabou.noosa.Tilemap;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;

/**
 * The floor and furniture art of the Warped rooms, one 16x16 cell each, all cut from
 * environment/custom_tiles/warped_rooms.png (painted by tools/warped_rooms_art.py, which
 * keeps the same frame numbers as the constants here).
 *
 * {@link Deco} is a picture that never changes; {@link Switch} is one with two faces
 * that the room's own logic flips (a rune lit, a plate pressed, coals burning). The
 * state is kept on the tile, so it survives a save with no help from whatever flipped it.
 */
public class WarpedRoomTiles {

	public static final String TEXTURE = "environment/custom_tiles/warped_rooms.png";

	//row 0: the elemental lock, asleep; the plates
	public static final int RUNE_EMBER      = 0;
	public static final int RUNE_RIME       = 1;
	public static final int RUNE_TIDE       = 2;
	public static final int RUNE_BLOOM      = 3;
	public static final int RUNE_GATE       = 4;
	public static final int PLAQUE          = 5;
	public static final int PLATE_UP        = 6;
	public static final int PLATE_DOWN      = 7;
	//row 1: the same runes awake (rune + 8); the vault's bars, the nest, the coals
	public static final int RUNE_LIT_OFFSET = 8;
	public static final int PORTCULLIS      = 12;
	public static final int NEST            = 13;
	public static final int COALS_COLD      = 14;
	public static final int COALS_LIT       = 15;
	//row 2: the black market's stalls
	public static final int CRATES          = 16;
	public static final int STALL           = 17;
	public static final int LANTERN         = 18;
	public static final int BARREL          = 19;
	public static final int SACKS           = 20;
	public static final int FURNACE         = 21;
	public static final int TOOL_RACK       = 22;
	public static final int SCRAP           = 23;
	//row 3 and the first cell of row 4: the dealer's rug, three by three
	public static final int RUG             = 24;
	//row 4
	public static final int ROPE_POST       = 33;
	public static final int PLANTER         = 34;
	public static final int FIRE_RING       = 35;
	public static final int LOG_SEAT        = 36;
	public static final int SUPPLIES        = 37;
	public static final int ACORNS          = 38;
	public static final int BEDDING         = 39;
	//row 5: the bellows
	public static final int GRILLE_IN       = 40;
	public static final int GRILLE_OUT      = 41;
	public static final int WIND_EAST       = 42;
	public static final int WIND_SOUTH      = 43;
	public static final int WIND_WEST       = 44;
	public static final int WIND_NORTH      = 45;
	public static final int BRAZIER         = 46;
	public static final int PLINTH          = 47;
	//row 6
	public static final int PORTCULLIS_OPEN = 48;
	public static final int FIRE_RING_LIT   = 49;
	public static final int WARNING_SIGN    = 50;

	/** A single cell of furniture or floor art. {@code key} names its strings:
	 *  tiles.warpedroomtiles.&lt;key&gt;_name and _desc. */
	public static class Deco extends CustomTilemap {

		{
			texture = TEXTURE;
			tileW = tileH = 1;
		}

		public int frame = 0;
		public String key = "";

		public Deco(){
		}

		public Deco( int frame, String key ){
			this.frame = frame;
			this.key = key;
		}

		protected int currentFrame(){
			return frame;
		}

		@Override
		public Tilemap create(){
			Tilemap v = super.create();
			v.map( new int[]{ currentFrame() }, 1 );
			return v;
		}

		@Override
		public String name( int tileX, int tileY ){
			return key.isEmpty() ? null : Messages.get( WarpedRoomTiles.class, key + "_name" );
		}

		@Override
		public String desc( int tileX, int tileY ){
			return key.isEmpty() ? null : Messages.get( WarpedRoomTiles.class, key + "_desc" );
		}

		private static final String FRAME = "frame";
		private static final String KEY   = "key";

		@Override
		public void storeInBundle( Bundle bundle ){
			super.storeInBundle( bundle );
			bundle.put( FRAME, frame );
			bundle.put( KEY, key );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ){
			super.restoreFromBundle( bundle );
			frame = bundle.getInt( FRAME );
			key = bundle.getString( KEY );
		}
	}

	/** A cell with two faces. Its strings follow the face too: &lt;key&gt;_on_desc when
	 *  it is on, &lt;key&gt;_desc when it is not. */
	public static class Switch extends Deco {

		public int onFrame = 0;
		public boolean on = false;

		public Switch(){
		}

		public Switch( int offFrame, int onFrame, String key ){
			super( offFrame, key );
			this.onFrame = onFrame;
		}

		@Override
		protected int currentFrame(){
			return on ? onFrame : frame;
		}

		public void set( boolean value ){
			if (on == value) return;
			on = value;
			Game.runOnRenderThread( new Callback(){
				@Override
				public void call(){
					if (vis != null && vis.alive) vis.map( new int[]{ currentFrame() }, 1 );
				}
			} );
		}

		@Override
		public String desc( int tileX, int tileY ){
			if (key.isEmpty()) return null;
			return Messages.get( WarpedRoomTiles.class, key + (on ? "_on_desc" : "_desc") );
		}

		private static final String ON_FRAME = "on_frame";
		private static final String ON       = "on";

		@Override
		public void storeInBundle( Bundle bundle ){
			super.storeInBundle( bundle );
			bundle.put( ON_FRAME, onFrame );
			bundle.put( ON, on );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ){
			super.restoreFromBundle( bundle );
			onFrame = bundle.getInt( ON_FRAME );
			on = bundle.getBoolean( ON );
		}
	}

	/** lays one picture on a cell while a room is being painted */
	public static Deco place( Level level, int cell, int frame, String key ){
		Deco deco = new Deco( frame, key );
		deco.pos( cell, level );
		level.customTiles.add( deco );
		return deco;
	}

	public static Switch placeSwitch( Level level, int cell, int offFrame, int onFrame, String key ){
		Switch tile = new Switch( offFrame, onFrame, key );
		tile.pos( cell, level );
		level.customTiles.add( tile );
		return tile;
	}

	/** the two-faced tile lying on a cell, if there is one */
	public static Switch switchAt( Level level, int cell ){
		int x = cell % level.width(), y = cell / level.width();
		for (CustomTilemap tile : level.customTiles){
			if (tile instanceof Switch && tile.tileX == x && tile.tileY == y) return (Switch) tile;
		}
		return null;
	}

	public static void flip( Level level, int cell, boolean on ){
		Switch tile = switchAt( level, cell );
		if (tile != null) tile.set( on );
	}

	/** takes the picture off a cell for good (an opened gate leaves nothing behind) */
	public static void remove( Level level, int cell ){
		int x = cell % level.width(), y = cell / level.width();
		for (CustomTilemap tile : level.customTiles.toArray( new CustomTilemap[0] )){
			if (tile instanceof Deco && tile.tileX == x && tile.tileY == y){
				level.customTiles.remove( tile );
				//the scene graph belongs to the render thread; rooms call this from the actor's
				final Tilemap shown = tile.vis;
				if (shown != null){
					Game.runOnRenderThread( new Callback(){
						@Override
						public void call(){
							if (shown.alive) shown.killAndErase();
						}
					} );
				}
			}
		}
	}
}
