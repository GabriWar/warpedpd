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

package xyz.gabriwar.warpedpixeldungeon.scenes;

import xyz.gabriwar.warpedpixeldungeon.Assets;

/** Loading artwork follows the destination, including branches with shared depth numbers. */
public enum LoadingSplash {
	SEWERS(Assets.Splashes.SEWERS),
	PRISON(Assets.Splashes.PRISON),
	CAVES(Assets.Splashes.CAVES),
	CITY(Assets.Splashes.CITY),
	HALLS(Assets.Splashes.HALLS),
	OVERWORLD(Assets.Splashes.OVERWORLD),
	FROZEN(Assets.Splashes.FROZEN),
	TEMPLE(Assets.Splashes.TEMPLE),
	SPIDER_NEST(Assets.Splashes.SPIDER_NEST),
	MINES(Assets.Splashes.MINES),
	VAULT(Assets.Splashes.VAULT),
	FIELD(Assets.Splashes.FIELD),
	BATTLE(Assets.Splashes.BATTLE),
	FISHING(Assets.Splashes.FISHING),
	CATACOMBS(Assets.Splashes.CATACOMBS),
	FORTRESS(Assets.Splashes.FORTRESS),
	CHASM(Assets.Splashes.CHASM),
	INFESTATION(Assets.Splashes.INFESTATION),
	TENGU_DEN(Assets.Splashes.TENGU_DEN),
	SANCTUARY(Assets.Splashes.SANCTUARY),
	SOKOBAN(Assets.Splashes.SOKOBAN),
	DRAGON_CAVE(Assets.Splashes.DRAGON_CAVE),
	ZOT(Assets.Splashes.ZOT),
	CHURCH(Assets.Splashes.CHURCH),
	CINEMA(Assets.Splashes.CINEMA),
	LIBRARY(Assets.Splashes.LIBRARY),
	SHOP(Assets.Splashes.SHOP),
	FORTUNE(Assets.Splashes.FORTUNE),
	INN(Assets.Splashes.INN),
	HOUSE(Assets.Splashes.HOUSE);

	public final String asset;

	LoadingSplash(String asset) {
		this.asset = asset;
	}

	public static LoadingSplash forLevel(int depth, int branch) {
		switch (branch) {
			case 7: return HOUSE;
			case 6:
				switch (depth) {
					case 1: return CHURCH;
					case 2: return CINEMA;
					case 3: return LIBRARY;
					case 4: return SHOP;
					case 5: return FORTUNE;
					case 6: return INN;
					default: return HOUSE;
				}
			case 5: return SPIDER_NEST;
			case 3: case 4: return TEMPLE;
			case 2: return FROZEN;
			case 1: return depth >= 16 ? VAULT : MINES;
			default: break;
		}
		if (depth >= 1 && depth <= 5) return SEWERS;
		if (depth <= 10 && depth >= 6) return PRISON;
		if (depth <= 15 && depth >= 11) return CAVES;
		if (depth <= 20 && depth >= 16) return CITY;
		if (depth <= 26 && depth >= 21) return HALLS;
		if (depth >= 56 && depth <= 65) return MINES;
		switch (depth) {
			case 0: case 55: case 97: return OVERWORLD;
			case 27: return FIELD;
			case 28: return BATTLE;
			case 29: case 38: return FISHING;
			case 30: case 40: return VAULT;
			case 31: case 37: return CATACOMBS;
			case 32: return FORTRESS;
			case 33: return CHASM;
			case 35: return INFESTATION;
			case 36: case 41: return TENGU_DEN;
			case 50: return SANCTUARY;
			case 51: case 52: case 53: case 54: case 66: return SOKOBAN;
			case 67: return DRAGON_CAVE;
			case 98: return SEWERS; // Developer room gallery uses sewer masonry.
			case 99: return ZOT;
			default: return HALLS;
		}
	}

	/** Portal destinations shared by the loading preview and the actual transfer. */
	public static int portalDepth(InterlevelScene.Mode mode, int journalPage) {
		switch (mode) {
			case PORT1: return 31;
			case PORT2: return 32;
			case PORT3: return 33;
			case PORT4: return 35;
			case PORTSEWERS: return 27;
			case PORTPRISON: return 28;
			case PORTCAVES: return 29;
			case PORTCITY: return 30;
			case PORTHALLS: return 25;
			case PORTCRAB: return 38;
			case PORTTENGU: return 36;
			case PORTCOIN: return 40;
			case PORTBONE: return 37;
			case PALANTIR: return 99;
			case JOURNAL:
				switch (journalPage) {
					case 1: return 51;
					case 2: return 52;
					case 3: return 53;
					case 4: return 54;
					case 5: return 97;
					case 6: return 66;
					case 7: return 67;
					default: return 50;
				}
			default: return -1;
		}
	}
}

