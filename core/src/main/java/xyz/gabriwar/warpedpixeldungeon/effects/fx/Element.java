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

package xyz.gabriwar.warpedpixeldungeon.effects.fx;

import xyz.gabriwar.warpedpixeldungeon.Assets;

/**
 * The elements the effects are made of, each with its own palette (docs/fx/README.md, the
 * palettes): a six-stop ramp hottest (or lightest) first, the colour of the light it throws on
 * the ground and walls, its glint, what it sheds as matter (smoke, soot, mist, ash, fumes; none
 * for the clean fires) and the lit bits it throws (embers, sparks, petals, pebbles), and whether
 * it is light (drawn additively over the night's tint) or matter (drawn normally, under it).
 */
public enum Element {

	FIRE( ramp( 0xFFF3C8, 0xFFD24A, 0xFF8418, 0xE04A10, 0xA82008, 0x4A1006 ), 0xFF9A48, 0xFFF3C8,
			ramp( 0x8A847C, 0x5E5850, 0x3E3A34 ), ramp( 0xFFC060, 0xF06020, 0x7A1808 ), true ),
	HELL( ramp( 0xFFE8D8, 0xFF8466, 0xF0303C, 0xB0102E, 0x6A0824, 0x2A0612 ), 0xFF5040, 0xFFE8D8,
			ramp( 0x3A2426, 0x241618 ), ramp( 0xFF8466, 0xF0303C, 0x6A0824 ), true ),
	FIEND( ramp( 0xFFF0C8, 0xFFA060, 0xF0507A, 0xA82C80, 0x5E1E58, 0x2A1028 ), 0xFF6A8A, 0xFFF0C8,
			ramp( 0x4A3040, 0x2E1C28 ), ramp( 0xD8F070, 0xA0B848, 0x5E6A28 ), true ),
	MAGIC_FIRE( ramp( 0xFFFFF0, 0xFFE680, 0xF4B0FF, 0xB070F0, 0x6A3CC0, 0x2E1A6A ), 0xE8B8FF, 0xFFF0FF,
			null, ramp( 0xFFF0FF, 0xF4B0FF, 0x6A3CC0 ), true ),
	HALOMETHANE( ramp( 0xF0FFFF, 0xA8F4FF, 0x40CCFF, 0x1480E8, 0x0C40A0, 0x061C50 ), 0x60D8FF, 0xC8F8FF,
			null, ramp( 0xC8F8FF, 0x40CCFF, 0x0C40A0 ), true ),
	HOLY( ramp( 0xFFFFFF, 0xFFF6CC, 0xFFE38A, 0xF2C050, 0xC08A2A, 0x6A4614 ), 0xFFE9A8, 0xFFFFFF,
			null, ramp( 0xFFE070, 0xF2C050, 0x6A4614 ), true ),
	ELMO( ramp( 0xF2FFE8, 0xB0FFA0, 0x48EC78, 0x14A858, 0x0A6034, 0x042A18 ), 0x70FF98, 0xF2FFE8,
			ramp( 0x3A4A3A ), ramp( 0xB0FFA0, 0x48EC78, 0x0A6034 ), true ),
	SACRIFICIAL( ramp( 0xF0F8FF, 0xB0D4FF, 0x5C9CF4, 0x2C5CCC, 0x18307C, 0x0A1438 ), 0x6AA8FF, 0xF0F8FF,
			ramp( 0x2050B0 ), ramp( 0xB0D4FF, 0x5C9CF4, 0x18307C ), true ),
	//its matter is the crust that skins it
	LAVA( ramp( 0xFFF0B0, 0xFFC040, 0xFF7A14, 0xD83A00, 0x9B0100, 0x5A0000 ), 0xFF7A20, 0xFFC040,
			ramp( 0x3A0A06 ), ramp( 0xFFC040, 0xFF7A14, 0x9B0100 ), true ),
	//its matter is the mist it breathes
	FROST( ramp( 0xFFFFFF, 0xE8F6FF, 0xBCE0FF, 0x84BCEC, 0x4E80B8, 0x2A4A7A ), 0xA8D8FF, 0xFFFFFF,
			ramp( 0xE4F0FA, 0xC2D6E8, 0x9AB4CC ), ramp( 0xFFFFFF, 0xBCE0FF, 0x4E80B8 ), false ),
	//its light the halo, its glint the bloom, its bits the sparks
	ELECTRIC( ramp( 0xFFFFFF, 0xE4F0FF, 0xA8C8FF, 0x6A90FF, 0x3A56D8, 0x1A2878 ), 0x7FA6FF, 0xD8E4FF,
			null, ramp( 0xFFFFFF, 0xBFE6FF, 0x6AA8FF, 0x2E50C0 ), true ),
	METAL( ramp( 0xFFFFFF, 0xFFE8A0, 0xFFA040, 0xC04010, 0x5A2010 ), 0xFFA040, 0xFFFFFF,
			null, ramp( 0xFFFFFF, 0xFFE8A0, 0xFFA040, 0xC04010, 0x5A2010 ), true ),
	STEEL( ramp( 0xFFFFFF, 0xE8F0FF, 0xA8C0E0, 0x50607A ), 0xE8F0FF, 0xFFFFFF,
			null, ramp( 0xFFFFFF, 0xE8F0FF, 0xA8C0E0, 0x50607A ), true ),
	ARCANE( ramp( 0xFFFFFF, 0xF0E8FF, 0xC8B0FF, 0x9070F0, 0x5A38C0, 0x2A1870 ), 0xB8A0FF, 0xFFFFFF,
			null, ramp( 0xF0E8FF, 0xC8B0FF, 0x5A38C0 ), true ),
	//the prism's rainbow round a white core
	PRISM( ramp( 0xFF5A5A, 0xFFA040, 0xFFE860, 0x70F070, 0x50C8FF, 0x6878FF, 0xB070F0 ), 0xE8E0FF, 0xFFFFFF,
			null, ramp( 0xFFFFFF, 0xFFE860, 0x50C8FF, 0xB070F0 ), true ),
	//its matter the ash it leaves
	DEATH( ramp( 0xFFFFFF, 0xFFD0E0, 0xFF4060, 0xC01848, 0x6A0A50, 0x2A0430 ), 0xFF6080, 0xFFD0E0,
			ramp( 0x3A3040, 0x22182A ), ramp( 0xFFD0E0, 0xFF4060, 0x6A0A50 ), true ),
	LIFE( ramp( 0xFFFFFF, 0xFFE0F0, 0xFF80C0, 0xE0408E, 0x9A1A5A, 0x4A0A2A ), 0xFF90C8, 0xFFFFFF,
			null, ramp( 0xFFE0F0, 0xFF80C0, 0x9A1A5A ), true ),
	HEAL( ramp( 0xFFFFFF, 0xEAFFEA, 0xA8FFC0, 0x58E08C, 0x22A060, 0x0E5A34 ), 0x9CFFB0, 0xFFFFFF,
			null, ramp( 0xEAFFEA, 0xA8FFC0, 0x22A060 ), true ),
	//matter: its rim lightest, then its body darkening to black; its glint the rim's
	SHADOW( ramp( 0x9A5AD0, 0x6A4A8A, 0x4A2A6A, 0x2E1648, 0x1A0C2C, 0x0C0614 ), 0xA070E0, 0xA070E0,
			ramp( 0x4A2A6A, 0x2E1648, 0x1A0C2C ), null, false ),
	POISON( ramp( 0xF0FFB0, 0xB8F060, 0x6CCC3C, 0x2F8A2A, 0x1C5420, 0x0E2C12 ), 0xD8FF80, 0xF0FFB0,
			ramp( 0x6CCC3C, 0x2F8A2A, 0x1C5420 ), null, false ),
	//its glint the sizzle, its matter the fumes
	ACID( ramp( 0xFFF4B0, 0xD8F048, 0xA8C030, 0x6A7A20 ), 0xD8F048, 0xFFB040,
			ramp( 0x9C9688, 0x6E6A60, 0x4A4640 ), null, false ),
	//its bits the blossom (leaves take the level's own colours)
	NATURE( ramp( 0xFFF0A0, 0xC8F070, 0x88CC44, 0x4E9A30, 0x2E6A24, 0x163E16 ), 0xC8F070, 0xFFF0A0,
			null, ramp( 0xFFF0F6, 0xFFC8DC, 0xF890B8, 0xC85A8A ), false ),
	//its matter the dust, its bits the stone
	EARTH( ramp( 0xE0D0A8, 0xB89C6C, 0x8A7048, 0x5E4A30, 0x3A2C1C ), 0xE0D0A8, 0xE0D0A8,
			ramp( 0xE0D0A8, 0xB89C6C, 0x8A7048 ), ramp( 0xD0CCC0, 0xA8A498, 0x7C786E, 0x52504A, 0x2E2C2A ), false ),
	//a creature's own (CharSprite.blood()) comes first where there is one
	BLOOD( ramp( 0xFF5050, 0xD01818, 0x8A0A14, 0x4A0610 ), 0xFF5050, 0xFF5050, null, null, false ),
	SMOKE( ramp( 0x8A847C, 0x5E5850, 0x3E3A34, 0x242220 ), 0x8A847C, 0x8A847C,
			ramp( 0x8A847C, 0x5E5850, 0x3E3A34, 0x242220 ), null, false ),
	STEAM( ramp( 0xFFFFFF, 0xEEF2F4, 0xD4DCE2, 0xAEB8C2 ), 0xFFFFFF, 0xFFFFFF,
			ramp( 0xFFFFFF, 0xEEF2F4, 0xD4DCE2, 0xAEB8C2 ), null, false );

	/** Its colours, hottest or lightest first. */
	public final Ramp ramp;
	/** The colour of the light it throws (FxLight's glows and pools). */
	public final int light;
	/** Its glint: a twinkle of it. */
	public final int glint;
	/** The matter it sheds, or null: smoke, soot, mist, ash, fumes, crust, dust. */
	public final Ramp smoke;
	/** The lit bits it throws, or null: embers, sparks, petals, stones. */
	public final Ramp ember;
	/** Light (additive, over the night's tint) or matter (normal, under it). */
	public final boolean emissive;

	Element( Ramp ramp, int light, int glint, Ramp smoke, Ramp ember, boolean emissive ){
		this.ramp = ramp;
		this.light = light;
		this.glint = glint;
		this.smoke = smoke;
		this.ember = ember;
		this.emissive = emissive;
	}

	private static Ramp ramp( int... stops ){
		return new Ramp( stops );
	}

	/** Not a colour: where a liquid has no deep (lava shows no fish). */
	public static final int NONE = -1;

	/**
	 * The level's liquids, by the water texture the level draws (never its class: a co-op guest's
	 * mirror and the host's own level agree on the texture): the water's body dark to light, its
	 * highlight, its foam (rings, wakes, lap foam), its glint, its deep (what swims under it shows
	 * as), how fast its palette cycles and its surface scrolls, its sheens and whether its rings
	 * are light.
	 */
	public enum Liquid {
		SEWER( ramp( 0x355F4B, 0x3E6A55, 0x4A7561 ), 0x56806D, 0xB3C6BD, 0xE6ECE9, 0x2A4C3C, 6, 5f, 0.22f, 0.14f, NONE, NONE, false ),
		PRISON( ramp( 0x334949, 0x384F4F, 0x3F5656 ), 0x465D5D, 0xACB6B6, 0xE3E7E7, 0x263838, 5, 5f, 0.22f, 0.14f, NONE, NONE, false ),
		CAVE( ramp( 0x173030, 0x1A3432, 0x213B37 ), 0x28423C, 0x8A9C98, 0xC0ECE4, 0x0E2222, 4, 5f, 0.12f, 0.08f, NONE, NONE, false ),
		//oily: its two sheens iridescent
		CITY( ramp( 0x381C21, 0x411E1E, 0x4A2626, 0x572B2B ), 0x643030, 0xA28383, 0xD8C0C0, 0x2A1216, 3, 3.5f, 0.12f, 0.10f, 0x9A7AC0, 0x6AA898, false ),
		//its rings are light: FF9030 added; no sheen, no deep; its crust 3A0A06 (Element.LAVA)
		LAVA( ramp( 0x8A0000, 0x9B0100, 0xA90100, 0xBF2000 ), 0xD53F00, 0xFF9030, 0xFFC040, NONE, 4, 2f, 0f, 0f, NONE, NONE, true ),
		FROZEN( ramp( 0x4C778C ), 0x6E9AB0, 0xC4E2F0, 0xFFFFFF, 0x34566A, 0, 5f, 0.22f, 0.14f, NONE, NONE, false ),
		TEMPLE( ramp( 0x544F2D, 0x595633, 0x605C3B ), 0x676243, 0xBAB8AA, 0xF0EEE0, 0x3E3A20, 5, 5f, 0.22f, 0.14f, NONE, NONE, false );

		/** Its body, dark to light. */
		public final Ramp body;
		public final int highlight, foam, glint;
		/** What swims under it shows as; NONE where nothing does. */
		public final int deep;
		/** Its palette cycle's frames a second (0: none) and its surface's scroll, px/s. */
		public final int cycleFps;
		public final float scroll;
		/** Its two sheens' alphas (0: none). */
		public final float sheenA, sheenB;
		/** Its sheens' own tints where they are iridescent (the city's oil), else NONE: the glint. */
		public final int sheenTintA, sheenTintB;
		/** Its rings are light (added in its foam colour) rather than matter. */
		public final boolean ringAdditive;

		Liquid( Ramp body, int highlight, int foam, int glint, int deep, int cycleFps, float scroll,
				float sheenA, float sheenB, int sheenTintA, int sheenTintB, boolean ringAdditive ){
			this.body = body;
			this.highlight = highlight;
			this.foam = foam;
			this.glint = glint;
			this.deep = deep;
			this.cycleFps = cycleFps;
			this.scroll = scroll;
			this.sheenA = sheenA;
			this.sheenB = sheenB;
			this.sheenTintA = sheenTintA;
			this.sheenTintB = sheenTintB;
			this.ringAdditive = ringAdditive;
		}

		/** The liquid a level's water texture shows (Level.waterTex); the sewers' for any other,
		 *  the surface's included. */
		public static Liquid of( String waterTex ){
			if (waterTex == null) return SEWER;
			switch (waterTex){
				case Assets.Environment.WATER_PRISON: return PRISON;
				case Assets.Environment.WATER_CAVES:  return CAVE;
				case Assets.Environment.WATER_CITY:   return CITY;
				case Assets.Environment.WATER_HALLS:  return LAVA;
				case Assets.Environment.WATER_FROZEN: return FROZEN;
				case Assets.Environment.WATER_TEMPLE: return TEMPLE;
				default:                              return SEWER;
			}
		}
	}
}
