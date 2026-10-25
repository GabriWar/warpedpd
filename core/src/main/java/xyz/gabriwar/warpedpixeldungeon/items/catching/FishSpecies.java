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

package xyz.gabriwar.warpedpixeldungeon.items.catching;

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

/**
 * The fish a rod brings up (FishingRod, CaughtFish), and how readily each takes a hook: in which
 * waters it lives (FishingWaters.Kind), which bait it takes best (Bait), at which hour, in which
 * season and in which weather it bites. FishingRules multiplies them into a weight, and the fish on
 * the hook is drawn by weight. In the caves' pools there is no hour, no season and no weather: there
 * only the water and the bait count.
 *
 *   brook trout  the rivers and the mountain tarns, now and then a lake; takes insects best; bites at
 *                dawn and dusk, in spring and autumn, and in the rain
 *   perch        the lakes, the rivers, a swamp's pools, the holes in the ice; worms and grubs; by day,
 *                in summer, in fair weather
 *   pike         lakes and swamps, rivers and the ice now and then; a live frog or newt; at dusk and
 *                dawn, in autumn
 *   catfish      the swamps, then rivers and lakes, now and then a cave pool; worms; at night and dusk,
 *                in summer, and in rain or a storm
 *   mackerel     the sea alone; takes anything; by day, in summer
 *   arctic char  the holes in the ice and the mountain tarns; worms and glowing bait; in winter
 *   cave fish    blind and pale, the caves' pools alone; drawn by a glowing bait
 *
 * Saved by name: entries may be added, never renamed.
 */
public enum FishSpecies {

	//              image                          value  size
	TROUT   ( ItemSpriteSheet.FISH_TROUT,     10, Size.MEDIUM,
			//         RIVER LAKE SEA SWAMP ICE CAVE TARN
			new float[]{ 10,  2,   0,  0,    0,  0,   10 },
			//         WORM GRUB  FLY LIVE  GLOW
			new float[]{ 1,  1.5f, 3,  0.5f, 0.5f },
			//         DAWN DAY DUSK NIGHT
			new float[]{ 2,   1,  2,   0.3f },
			//         SPRING SUMMER AUTUMN WINTER
			new float[]{ 1.5f, 0.8f,  1.3f,  0.6f },
			//         FAIR RAIN  SNOW  STORM
			new float[]{ 1,  1.5f, 0.8f, 0.5f } ),
	PERCH   ( ItemSpriteSheet.FISH_PERCH,      6, Size.SMALL,
			new float[]{ 6,   10,  0,  3,    3,  0,   0 },
			new float[]{ 2.5f, 2, 1,  0.7f, 0.5f },
			new float[]{ 1,   1.5f, 1, 0.3f },
			new float[]{ 1,    1.5f,  1,     0.7f },
			new float[]{ 1.2f, 1,  0.8f, 0.5f } ),
	PIKE    ( ItemSpriteSheet.FISH_PIKE,      18, Size.LARGE,
			new float[]{ 2,   4,   0,  4,    2,  0,   0 },
			new float[]{ 0.5f, 0.5f, 0.3f, 4, 0.5f },
			new float[]{ 1.5f, 0.8f, 2, 0.5f },
			new float[]{ 1,    0.8f,  1.8f,  1.2f },
			new float[]{ 1,  1.2f, 1,    0.6f } ),
	CATFISH ( ItemSpriteSheet.FISH_CATFISH,   10, Size.MEDIUM,
			new float[]{ 4,   3,   0,  10,   0,  1,   0 },
			new float[]{ 2,  1.5f, 0.5f, 1, 1 },
			new float[]{ 0.7f, 0.4f, 1.5f, 2.5f },
			new float[]{ 1,    1.6f,  1,     0.5f },
			new float[]{ 1,  1.5f, 0.7f, 1.2f } ),
	MACKEREL( ItemSpriteSheet.FISH_MACKEREL,   6, Size.SMALL,
			new float[]{ 0,   0,   10, 0,    0,  0,   0 },
			new float[]{ 1.5f, 1.5f, 1, 1.2f, 1 },
			new float[]{ 1.3f, 1.5f, 1, 0.5f },
			new float[]{ 1,    1.4f,  1,     0.7f },
			new float[]{ 1.2f, 1, 0.8f, 0.4f } ),
	CHAR    ( ItemSpriteSheet.FISH_CHAR,      14, Size.MEDIUM,
			new float[]{ 0,   0,   0,  0,    10, 0,   4 },
			new float[]{ 2,  1,   0.5f, 1, 2.5f },
			new float[]{ 1.3f, 1, 1.3f, 0.7f },
			new float[]{ 0.8f, 0.6f,  1,     2 },
			new float[]{ 1,  1,   1,    0.8f } ),
	CAVEFISH( ItemSpriteSheet.FISH_CAVEFISH,  16, Size.SMALL,
			new float[]{ 0,   0,   0,  0,    0,  10,  0 },
			new float[]{ 1,  1.5f, 0.5f, 1, 4 },
			new float[]{ 1,   1,  1,   1 },
			new float[]{ 1,    1,     1,     1 },
			new float[]{ 1,  1,   1,    1 } );

	/** How much a fish feeds: raw, and cooked (CookedFish). */
	public enum Size {
		SMALL ( Hunger.HUNGRY / 4f, Hunger.HUNGRY / 2f ),
		MEDIUM( Hunger.HUNGRY / 3f, Hunger.HUNGRY * 3f / 4f ),
		LARGE ( Hunger.HUNGRY * 5f / 12f, Hunger.HUNGRY );

		public final float raw, cooked;

		Size( float raw, float cooked ){
			this.raw = raw;
			this.cooked = cooked;
		}
	}

	public final int image;
	/** A raw one is worth this many coins (a cooked one a few more: CookedFish). */
	public final int value;
	public final Size size;
	//by FishingWaters.Kind, Bait, DayNightCycle.Phase, GameCalendar.Season and VillageRoutine.Weather,
	//in their order
	final float[] waters, baits, hours, seasons, skies;

	FishSpecies( int image, int value, Size size, float[] waters, float[] baits, float[] hours, float[] seasons, float[] skies ){
		this.image = image;
		this.value = value;
		this.size = size;
		this.waters = waters;
		this.baits = baits;
		this.hours = hours;
		this.seasons = seasons;
		this.skies = skies;
	}

	/** Does it live in these waters at all? */
	public boolean livesIn( FishingWaters.Kind water ){
		return waters[water.ordinal()] > 0f;
	}

	/** A name stored in a save, or the commonest fish when it is no longer known. */
	public static FishSpecies named( String name ){
		if (name != null){
			for (FishSpecies f : values()) if (f.name().equals( name )) return f;
		}
		return PERCH;
	}
}
