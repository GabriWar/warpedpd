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

import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

/**
 * The small critters the bug net takes (BugNet), each into its jar (CritterJar): the surface's
 * butterflies (their four colours, CritterSprite.Butterfly) and fireflies, and the dungeon's small
 * life (levels/ambience/DungeonLife: Fauna.Kind) that is small enough for a net - its moths (cream
 * by the torches, cinder in the halls, frost blue in the ice), the nest's flies, its beetles, roaches,
 * centipedes, silverfish, spiders and spiderlings, the sewers' snails, striders and frogs, the caves'
 * newts and the city's lizards. Never a bird, a hare, a mouse, a fish, a bat or a fire salamander.
 *
 * Each goes into one of three jars (the jar's picture), is one kind of bait (Bait), is netted at its
 * own chance (a snail never gets away, a lizard often does), and is worth a few coins. A glowing
 * one's jar glows in its colour. Saved by name: entries may be added, never renamed.
 */
public enum Critter {

	//                jar           bait        chance  value  glow
	CABBAGE_WHITE   ( Jar.WINGS,    Bait.FLY,   0.75f,  3,     0 ),
	BRIMSTONE       ( Jar.WINGS,    Bait.FLY,   0.75f,  3,     0 ),
	ORANGE_TIP      ( Jar.WINGS,    Bait.FLY,   0.75f,  3,     0 ),
	COMMON_BLUE     ( Jar.WINGS,    Bait.FLY,   0.75f,  3,     0 ),
	FIREFLY         ( Jar.WINGS,    Bait.GLOW,  0.65f,  4,     0xE8F060 ),
	MOTH            ( Jar.WINGS,    Bait.FLY,   0.80f,  2,     0 ),
	CINDER_MOTH     ( Jar.WINGS,    Bait.FLY,   0.80f,  3,     0 ),
	FROST_MOTH      ( Jar.WINGS,    Bait.FLY,   0.80f,  3,     0 ),
	FLY             ( Jar.WINGS,    Bait.FLY,   0.90f,  1,     0 ),
	BEETLE          ( Jar.CRAWLER,  Bait.GRUB,  0.85f,  2,     0 ),
	EMBER_BEETLE    ( Jar.CRAWLER,  Bait.GLOW,  0.80f,  4,     0xF08838 ),
	ROACH           ( Jar.CRAWLER,  Bait.GRUB,  0.60f,  1,     0 ),
	CENTIPEDE       ( Jar.CRAWLER,  Bait.GRUB,  0.70f,  2,     0 ),
	SILVERFISH      ( Jar.CRAWLER,  Bait.GRUB,  0.60f,  2,     0 ),
	SPIDER          ( Jar.CRAWLER,  Bait.GRUB,  0.80f,  2,     0 ),
	SPIDERLING      ( Jar.CRAWLER,  Bait.GRUB,  0.75f,  1,     0 ),
	SNAIL           ( Jar.CRAWLER,  Bait.GRUB,  1.00f,  2,     0 ),
	STRIDER         ( Jar.CRAWLER,  Bait.FLY,   0.60f,  2,     0 ),
	FROG            ( Jar.FROG,     Bait.LIVE,  0.60f,  3,     0 ),
	NEWT            ( Jar.FROG,     Bait.LIVE,  0.70f,  3,     0 ),
	LIZARD          ( Jar.FROG,     Bait.LIVE,  0.55f,  3,     0 );

	/** The jar a critter is kept in: what its item looks like. */
	public enum Jar { WINGS, CRAWLER, FROG }

	public final Jar jar;
	public final Bait bait;
	/** The share of swings at it that net it, before the stretch of land grows wary (CritterCatch.thinned). */
	public final float chance;
	/** A jar of it is worth this many coins. */
	public final int value;
	/** Its glow (0xRRGGBB), 0 for none. */
	public final int glow;

	Critter( Jar jar, Bait bait, float chance, int value, int glow ){
		this.jar = jar;
		this.bait = bait;
		this.chance = chance;
		this.value = value;
		this.glow = glow;
	}

	/** Its jar's picture. */
	public int image(){
		switch (jar){
			case CRAWLER: return ItemSpriteSheet.JAR_CRAWLER;
			case FROG:    return ItemSpriteSheet.JAR_FROG;
			default:      return ItemSpriteSheet.JAR_WINGS;
		}
	}

	//the butterflies by their colour's place in CritterSprite.Butterfly's: cabbage white,
	//brimstone, orange tip, common blue
	private static final Critter[] BUTTERFLIES = { CABBAGE_WHITE, BRIMSTONE, ORANGE_TIP, COMMON_BLUE };

	/** A butterfly by its colour (CritterSprite.Butterfly.colour()). */
	public static Critter butterfly( int colour ){
		return BUTTERFLIES[Math.max( 0, Math.min( BUTTERFLIES.length - 1, colour ) )];
	}

	/** The colour a butterfly of this kind is drawn in (CritterSprite.Butterfly.COLOURS), -1 for none. */
	public int butterflyColour(){
		for (int i = 0; i < BUTTERFLIES.length; i++) if (BUTTERFLIES[i] == this) return i;
		return -1;
	}

	/** A name stored in a save, or the plainest critter when it is no longer known. */
	public static Critter named( String name ){
		if (name != null){
			for (Critter c : values()) if (c.name().equals( name )) return c;
		}
		return BEETLE;
	}
}
