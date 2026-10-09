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

package xyz.gabriwar.warpedpixeldungeon.audio;

/**
 * How long room acoustics' extras keep a voice, for its budget (RoomAcoustics): each effect's room
 * tail, its hall tail, its cavern tail and the effect itself (an echo plays that), in seconds at
 * pitch 1, every frame of the file counted as libGDX's desktop decoder plays them, its Info frame
 * too: the longest of the platforms.
 *
 * Written by tools/room_acoustics_gen.py from the files it renders and the effects they are made
 * from: run it again rather than edit this. RoomAcousticsAssetsTest fails while this and the files
 * disagree.
 */
final class RoomAcousticsLengths {

	private RoomAcousticsLengths(){}

	/** { room tail, hall tail, cavern tail, the effect itself } for an effect with tails, by its
	 *  file's name; null for any other. */
	static float[] of( String name ){
		switch (name){
			case "atk_crossbow":   return new float[]{ 0.7053f, 1.7502f, 2.9518f, 0.4560f };
			case "atk_spiritbow":  return new float[]{ 0.7053f, 1.7241f, 3.0302f, 0.4800f };
			case "blast":          return new float[]{ 1.4106f, 2.0376f, 3.1869f, 1.3061f };
			case "bones":          return new float[]{ 1.0710f, 1.9592f, 3.1608f, 0.8620f };
			case "chains":         return new float[]{ 0.8359f, 1.5151f, 2.4816f, 0.7576f };
			case "door_open":      return new float[]{ 0.8098f, 1.8808f, 3.1086f, 0.3918f };
			case "grass":          return new float[]{ 0.6792f, 1.6980f, 2.8735f, 0.2640f };
			case "hit":            return new float[]{ 0.6531f, 1.6718f, 2.8212f, 0.3600f };
			case "hit_arrow":      return new float[]{ 0.7576f, 1.8024f, 3.0302f, 0.4320f };
			case "hit_crush":      return new float[]{ 0.7053f, 1.7502f, 3.0302f, 0.3840f };
			case "hit_magic":      return new float[]{ 0.6792f, 1.7502f, 2.8996f, 0.6000f };
			case "hit_parry":      return new float[]{ 0.7576f, 1.6718f, 2.8996f, 0.5040f };
			case "hit_slash":      return new float[]{ 0.6792f, 1.7763f, 3.0563f, 0.3600f };
			case "hit_stab":       return new float[]{ 0.6792f, 1.6980f, 2.8996f, 0.3120f };
			case "hit_strong":     return new float[]{ 0.8620f, 1.9331f, 3.1869f, 0.7200f };
			case "lightning":      return new float[]{ 1.1494f, 1.8286f, 2.9257f, 1.1755f };
			case "mine":           return new float[]{ 1.0971f, 2.0376f, 3.1869f, 0.8160f };
			case "miss":           return new float[]{ 0.7314f, 1.7502f, 2.9780f, 0.4180f };
			case "puff":           return new float[]{ 0.7314f, 1.7763f, 3.0563f, 0.4441f };
			case "rocks":          return new float[]{ 1.5151f, 2.2988f, 3.3959f, 1.2960f };
			case "rocks_light":    return new float[]{ 0.8359f, 1.8547f, 3.0302f, 0.5224f };
			case "shatter":        return new float[]{ 0.7314f, 1.4629f, 2.4555f, 0.5486f };
			case "step":           return new float[]{ 0.7314f, 1.7502f, 3.0563f, 0.2351f };
			case "sturdy":         return new float[]{ 0.6531f, 1.7241f, 3.0041f, 0.2160f };
			case "trample":        return new float[]{ 0.6531f, 1.6457f, 2.7951f, 0.2640f };
			case "trap":           return new float[]{ 0.6008f, 1.5673f, 2.6906f, 0.1306f };
			case "unlock":         return new float[]{ 0.8098f, 1.6457f, 2.6122f, 0.5486f };
			case "wall_break_1":   return new float[]{ 1.4629f, 2.1943f, 3.2653f, 1.3584f };
			case "wall_break_2":   return new float[]{ 1.6718f, 2.2465f, 3.4482f, 1.5935f };
			case "wall_break_3":   return new float[]{ 1.8024f, 2.2465f, 3.2914f, 1.6196f };
			case "water":          return new float[]{ 0.6269f, 1.6196f, 2.6906f, 0.2090f };
			case "zap":            return new float[]{ 0.9665f, 1.9331f, 3.1608f, 0.7053f };
			default: return null;
		}
	}
}
