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

package xyz.gabriwar.warpedpixeldungeon.sprites;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.TextureFilm;

/**
 * The frames of the homesteads' stations and furniture (sprites/homestead.png, painted by
 * tools/homestead_art_gen.py, which keeps the same frame numbers as the constants here): 16x16
 * frames, sixteen to a row, drawn like the Warped rooms' furniture (TemperingAnvilSprite,
 * BreakersBenchSprite, BedrollSprite). A station's sprite class plays the frames listed for it:
 * the lit ones loop as a fire, the glows as a slow pulse.
 *
 * The watchtower is one 16x32 frame, standing in column 15 of rows 1 and 2: {@link #film()} gives
 * it under its own id, and the cell under its top half ({@link #WATCHTOWER} + 16) is part of it.
 */
public class HomesteadSprites {

	public static final String TEXTURE = Assets.Sprites.HOMESTEAD;

	//row 0
	public static final int WORKBENCH           = 0;
	public static final int MASONS_BENCH        = 1;
	public static final int KILN                = 2;
	public static final int KILN_LIT            = 3;    //3 frames
	public static final int HEARTH              = 6;
	public static final int HEARTH_LIT          = 7;    //3 frames
	public static final int COOKING_POT         = 10;
	public static final int COOKING_POT_STEAM   = 11;   //2 frames
	public static final int TANNING_RACK        = 13;
	public static final int TANNING_RACK_HIDE   = 14;
	public static final int QUERN               = 15;
	//row 1
	public static final int FORGE               = 16;
	public static final int FORGE_LIT           = 17;   //3 frames
	public static final int COMPOST_BIN         = 20;
	public static final int BED_STRAW           = 21;
	public static final int BED_WOOD            = 22;
	public static final int BED_FUR             = 23;
	public static final int CHEST_WOOD          = 24;
	public static final int CHEST_IRON          = 25;
	public static final int ALARM_BELL          = 26;
	public static final int ALARM_BELL_RINGING  = 27;   //2 frames
	public static final int HEARTHSTONE         = 29;
	public static final int HEARTHSTONE_LIT     = 30;
	public static final int WATCHTOWER          = 31;   //16x32: this cell and the one under it
	//row 2
	public static final int WAYSTONE            = 32;
	public static final int WAYSTONE_ATTUNED    = 33;   //2 frames
	public static final int TIE_POST            = 35;
	public static final int TRAP_SPIKES         = 36;

	public static final int LIT_FRAMES = 3, STEAM_FRAMES = 2, RING_FRAMES = 2, GLOW_FRAMES = 2;

	public static final int WATCHTOWER_WIDTH = 16, WATCHTOWER_HEIGHT = 32;

	/** The sheet's frames by the constants above, the watchtower's id holding its whole 16x32 frame. */
	public static TextureFilm film(){
		TextureFilm film = new TextureFilm( TEXTURE, 16, 16 );
		int x = (WATCHTOWER % 16) * 16, y = (WATCHTOWER / 16) * 16;
		film.add( WATCHTOWER, x, y, x + WATCHTOWER_WIDTH, y + WATCHTOWER_HEIGHT );
		return film;
	}

	/** The frame ids of an animation that starts at `first` - first, first + 1, ... - ready for
	 *  Animation.frames( film, HomesteadSprites.frames( KILN_LIT, LIT_FRAMES ) ). */
	public static Object[] frames( int first, int count ){
		Object[] out = new Object[count];
		for (int i = 0; i < count; i++) out[i] = first + i;
		return out;
	}
}
