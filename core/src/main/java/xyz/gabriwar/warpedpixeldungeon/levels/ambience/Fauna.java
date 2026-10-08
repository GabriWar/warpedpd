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

package xyz.gabriwar.warpedpixeldungeon.levels.ambience;

import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase;

/**
 * Who lives where in the dungeon, and when (docs/ambience.md, "The critters of each place"):
 * the kinds of small life and the airs (drips, gnats, spores...) of every place, how many of a
 * kind may be out at once, how readily each turns up and how near it lets anything come, and the
 * hours each keeps. The clock reaches the dungeon, and most of its life is out at dusk and at
 * night. Pure, so the tests pin the table; DungeonLife puts it on the ground.
 */
final class Fauna {

	private Fauna(){}

	/** No more pictures than this out at once, every kind together (a flock counts its birds). */
	static final int GLOBAL_CAP = 10;

	/**
	 * A kind of small life. Its cap is how many may be out at once (for the birds that come down
	 * in flocks, how many flocks); its chance, how readily one turns up when its turn comes
	 * round; near and far, the ring of cells around the hero it turns up in; and in scene pixels
	 * (sixteen to a cell) how near the hero (scare) or anyone else (pass) may come before it is
	 * off, and how far off a fight still sends it away (noise). Zero: nothing sends it off (a fish
	 * already in the air; a butterfly minds no one passing but the hero). A fly stuck in the web
	 * cannot be off: what comes near only sets it struggling.
	 */
	enum Kind {
		//            cap  chance  near far   scare  pass  noise
		FROG        ( 3, 0.40f,  2, 7,   40f,  24f,  96f ),
		ROACH       ( 3, 0.35f,  2, 7,   40f,  24f,  96f ),
		MOUSE       ( 2, 0.30f,  2, 7,   48f,  28f,  96f ),
		MOTH        ( 4, 0.45f,  1, 7,   20f,  16f,  80f ),
		SPIDER      ( 2, 0.25f,  2, 6,   40f,  24f,  64f ),
		SPIDERLING  ( 3, 0.40f,  2, 6,   36f,  24f,  80f ),
		NEWT        ( 2, 0.35f,  2, 7,   40f,  24f,  96f ),
		SALAMANDER  ( 2, 0.40f,  2, 6,   32f,  24f,  96f ),
		LIZARD      ( 2, 0.35f,  2, 7,   44f,  28f,  96f ),
		SNAIL       ( 2, 0.30f,  2, 6,   24f,  20f,  64f ),
		BEETLE      ( 2, 0.30f,  2, 7,   36f,  24f,  96f ),
		EMBER_BEETLE( 3, 0.40f,  2, 6,   32f,  24f,  96f ),
		CENTIPEDE   ( 2, 0.30f,  2, 7,   36f,  24f,  96f ),
		SILVERFISH  ( 3, 0.35f,  2, 7,   40f,  24f,  96f ),
		STRIDER     ( 3, 0.35f,  2, 7,   32f,  24f,  80f ),
		FLY         ( 2, 0.35f,  1, 6,   24f,  16f,  64f ),
		SWIFT       ( 3, 0.30f,  3, 8,   52f,  36f, 128f ),
		CROW        ( 3, 0.25f,  2, 6,   52f,  36f, 128f ),
		BUNTING     ( 1, 0.30f,  4, 8,   52f,  36f, 128f ),
		FINCH       ( 2, 0.35f,  5, 9,   52f,  36f, 128f ),
		GULL        ( 2, 0.35f,  5, 9,   52f,  36f, 128f ),
		HARE        ( 2, 0.25f,  5, 9,   68f,  36f,  96f ),
		FISH        ( 2, 0.50f,  2, 7,    0f,   0f,   0f ),
		BUTTERFLY   ( 4, 0.50f,  2, 6,   20f,   0f,  64f );

		final int cap, near, far;
		final float chance, scare, pass, noise;

		Kind( int cap, float chance, int near, int far, float scare, float pass, float noise ){
			this.cap = cap;
			this.chance = chance;
			this.near = near;
			this.far = far;
			this.scare = scare;
			this.pass = pass;
			this.noise = noise;
		}

		/**
		 * These birds come down on the ground in flocks: their cap counts flocks, not birds. The
		 * swifts and the crows sit one to a statue's top, and are counted by the bird.
		 */
		boolean flocks(){
			return this == BUNTING || this == FINCH || this == GULL;
		}
	}

	/**
	 * The air of a place: particles of the effects already in the game, a few at a time and only
	 * where the hero sees. Clouds (gnats, flies) hang over one cell a while; the rest come one by
	 * one, every so many seconds (between `every` and `most`) at their best hour.
	 */
	enum Air {
		//           every  most   near far  cloud
		GNATS      ( 1.5f, 3.0f,  1, 6,  true ),
		DRIPS      ( 0.6f, 1.6f,  1, 6,  false ),
		FLIES      ( 1.5f, 3.0f,  1, 6,  true ),
		GLOW_WORMS ( 0.5f, 1.0f,  1, 6,  false ),
		SPORES     ( 0.6f, 1.2f,  1, 6,  false ),
		EMBERS     ( 0.35f, 0.7f, 0, 5,  false ),
		SILK       ( 0.8f, 1.6f,  1, 6,  false ),
		DUST       ( 0.5f, 1.0f,  0, 6,  false ),
		FIREFLIES  ( 0.6f, 1.2f,  1, 6,  false );

		final float every, most;
		final int near, far;
		final boolean cloud;

		Air( float every, float most, int near, int far, boolean cloud ){
			this.every = every;
			this.most = most;
			this.near = near;
			this.far = far;
			this.cloud = cloud;
		}
	}

	//gnat and fly clouds over a cell at once, each kind
	static final int CLOUDS = 2;

	/** The life of a place: who turns up there, in the order their turns come round. */
	static Kind[] kinds( Place p ){
		if (p == null) return new Kind[0];
		switch (p){
			case SEWERS:   return new Kind[]{ Kind.FROG, Kind.ROACH, Kind.SNAIL, Kind.STRIDER };
			case PRISON:   return new Kind[]{ Kind.MOTH, Kind.MOUSE, Kind.ROACH, Kind.SPIDER };
			case CAVES:    return new Kind[]{ Kind.NEWT, Kind.CENTIPEDE, Kind.BEETLE, Kind.FISH };
			case CITY:     return new Kind[]{ Kind.MOTH, Kind.LIZARD, Kind.SILVERFISH, Kind.SWIFT, Kind.BUTTERFLY };
			//the city's, and no swifts: the vault is a stealth floor
			case VAULT:    return new Kind[]{ Kind.MOTH, Kind.LIZARD, Kind.SILVERFISH, Kind.BUTTERFLY };
			case HALLS:    return new Kind[]{ Kind.SALAMANDER, Kind.EMBER_BEETLE, Kind.MOTH, Kind.CROW };
			case FROZEN:   return new Kind[]{ Kind.HARE, Kind.MOTH, Kind.BUNTING, Kind.FISH };
			case NEST:     return new Kind[]{ Kind.SPIDERLING, Kind.FLY, Kind.SPIDER };
			case TEMPLE:   return new Kind[]{ Kind.MOTH };
			case MINES:    return new Kind[]{ Kind.CENTIPEDE, Kind.BEETLE };
			case MEADOW:   return new Kind[]{ Kind.BUTTERFLY, Kind.FINCH, Kind.HARE };
			case SHORE:    return new Kind[]{ Kind.GULL, Kind.FISH };
			case CATACOMB: return new Kind[]{ Kind.ROACH, Kind.MOTH, Kind.SPIDER, Kind.MOUSE };
			//the overworld's own: OverworldCritters, CaveLife, PeakLife
			default:       return new Kind[0];
		}
	}

	/** The air of a place. */
	static Air[] air( Place p ){
		if (p == null) return new Air[0];
		switch (p){
			case SEWERS:   return new Air[]{ Air.GNATS, Air.DRIPS };
			case PRISON:   return new Air[]{ Air.FLIES };
			case CAVES:    return new Air[]{ Air.GLOW_WORMS, Air.SPORES, Air.DRIPS };
			case HALLS:    return new Air[]{ Air.EMBERS, Air.FLIES };
			case NEST:     return new Air[]{ Air.SILK };
			case TEMPLE:   return new Air[]{ Air.DUST };
			case MINES:    return new Air[]{ Air.DRIPS };
			case MEADOW:   return new Air[]{ Air.FIREFLIES };
			case CATACOMB: return new Air[]{ Air.FLIES, Air.DRIPS };
			default:       return new Air[0];
		}
	}

	/** How readily life turns up in a place: the vault keeps quiet, at half the city's rate. */
	static float rate( Place p ){
		return p == Place.VAULT ? 0.5f : 1f;
	}

	/** The cap of a kind in a place: the temple keeps only a few moths. */
	static int cap( Kind k, Place p ){
		return p == Place.TEMPLE && k == Kind.MOTH ? 2 : k.cap;
	}

	/**
	 * How much of a kind is out at an hour, 0..1 (0: none; those out go home). The night's life
	 * is out at dusk and at night and only a few by day; the day's at dawn and by day; the rest
	 * keep no hours.
	 */
	static float hour( Kind k, Phase ph ){
		boolean dark = ph == Phase.DUSK || ph == Phase.NIGHT;
		switch (k){
			case FROG: case ROACH: case MOUSE: case MOTH: case SNAIL: case NEWT: case CENTIPEDE: case SILVERFISH:
				return dark ? 1f : ph == Phase.DAWN ? 0.35f : 0.15f;
			case SWIFT: case BUNTING:
				return ph == Phase.DAY ? 1f : ph == Phase.NIGHT ? 0f : 0.6f;
			case BUTTERFLY:
				return ph == Phase.DAY ? 1f : 0f;
			case FINCH:
				return ph == Phase.DAWN || ph == Phase.DAY ? 1f : ph == Phase.DUSK ? 0.4f : 0f;
			case GULL:
				return ph == Phase.NIGHT ? 0f : 1f;
			case LIZARD:
				return ph == Phase.DAY ? 1f : ph == Phase.NIGHT ? 0.1f : 0.5f;
			case STRIDER:
				return ph == Phase.DAY ? 1f : ph == Phase.NIGHT ? 0.4f : 0.8f;
			case HARE:
				return ph == Phase.DAWN || ph == Phase.DUSK ? 1f : ph == Phase.DAY ? 0.7f : 0.3f;
			case CROW:
				return ph == Phase.DAY ? 1f : ph == Phase.NIGHT ? 0.3f : 0.7f;
			default:
				return 1f;
		}
	}

	/** The same for the air: gnats dance at dusk and at night, fireflies light at night. */
	static float hour( Air a, Phase ph ){
		switch (a){
			case GNATS:
				return ph == Phase.DUSK || ph == Phase.NIGHT ? 1f : 0f;
			case FIREFLIES:
				return ph == Phase.NIGHT ? 1f : ph == Phase.DUSK ? 0.4f : 0f;
			case GLOW_WORMS:
				return ph == Phase.DUSK || ph == Phase.NIGHT ? 1f : 0.5f;
			case FLIES:
				return ph == Phase.NIGHT ? 0.5f : ph == Phase.DAY ? 1f : 0.8f;
			default:
				return 1f;
		}
	}

	//the moths' tints: pale art lit by what they circle
	static final int MOTH_CREAM = 0xF2E8C8, MOTH_CINDER = 0xF09848, MOTH_FROST = 0xA8D2FA;

	/** A moth's tint in a place: cream by the torches and the flames, cinder in the halls, frost blue in the ice. */
	static int mothTint( Place p ){
		return p == Place.HALLS ? MOTH_CINDER : p == Place.FROZEN ? MOTH_FROST : MOTH_CREAM;
	}
}
