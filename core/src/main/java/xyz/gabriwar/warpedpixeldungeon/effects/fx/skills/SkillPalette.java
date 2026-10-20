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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.skills;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;

/**
 * The skills' colours by what they MEAN (docs/fx/skills.md, the palette): a heal is green light and
 * red crosses, blood and a heartbeat crimson, fury red, fire orange, frost pale cyan, poison sickly
 * green, fear and shadow violet-black, the mage's soul a cold lavender-grey, a curse a void drawn
 * inward, holy light gold, the arcane blue-violet, mana sky blue, a storm white-blue, steel cold
 * grey-blue, the warrior's earth brown, the huntress leaf green, luck gold, a daze the game's own
 * stun yellow. Each follows the game's own picture of the thing (named on each ramp).
 *
 * Light ramps run from their pale COLOURED stop to their deepest (white only as a bolt's 1 px heart
 * or a one-frame impact core, never a ramp's first stop). The rules every user keeps:
 * - the palest stop is a flash, not a colour: a particle living longer than 0.1 s sits on its
 *   ramp's stop 0 for at most its first 25% (an evenly spaced ramp of three or more stops lerped
 *   over a life already does: it is nearer stop 1 from 25% on; a one-colour paint uses stop 1 or
 *   deeper);
 * - heal pictures never show anything paler than HEAL's A8FFC0 (HEAL_HEART is a bolt's 1 px heart
 *   only, never a particle's colour);
 * - every new FxLight is at most 0.6 (a level: a bolt's head ~0.5, a knot 0.3 to 0.6);
 * - arrivals and the ends of links are crisp pixels (a one-frame IMPACT_STAR, motes, crosses,
 *   runes), never a soft blob; no new reticle ring (ClosingRing) on an enemy.
 * Constants only: any thread.
 */
public final class SkillPalette {

	private SkillPalette(){}

	// ------------------------------------------------------------------ life, blood, rage, fire, cold, poison

	/** HP gained: green light (Element.HEAL, CharSprite.POSITIVE), from mint to a deep green; never
	 *  cream, never paler than A8FFC0. Its crosses are HEAL_CROSS (Speck.HEALING's red). */
	public static final Ramp HEAL = new Ramp( 0xA8FFC0, 0x58E08C, 0x22A060 );
	/** The heal's light (glows, pools, rings). */
	public static final int HEAL_LIGHT = 0x58E08C;
	/** A heal bolt's 1 px heart, and nothing else. */
	public static final int HEAL_HEART = 0xEAFFEA;
	/** The heal's cross (Speck.HEALING, the kit's PLUS_5 in FF2020), warming to HEAL_CROSS_WARM at
	 *  its end. */
	public static final int HEAL_CROSS = 0xFF2020, HEAL_CROSS_WARM = 0xFFE8A0;

	/** A cut, bleeding, a blood price: crimson (Element.BLOOD, BloodParticle CC0000 / 8A0A14, the
	 *  BLEEDING icon); never pink. */
	public static final Ramp BLOOD = new Ramp( 0xFF5050, 0xD01818, 0x8A0A14, 0x4A0610 );
	public static final int BLOOD_LIGHT = 0xFF5050;
	/** Its drops (matter). */
	public static final int BLOOD_DROP = 0xCC0000, BLOOD_DROP_DEEP = 0x8A0A14;

	/** A heartbeat (specks.png's HEART FF3355 to the blood's own reds). */
	public static final Ramp HEART = new Ramp( 0xFF3355, 0xCC0000, 0x8A0A14 );
	public static final int HEART_LIGHT = 0xFF3355;

	/** Rage, berserk, a crimson ward (Combo.FURY, StanceAura FURY, Speck RED_TINT FFE0D0, the red
	 *  legacy flares of Blood Rush and Carnage); never salmon. */
	public static final Ramp FURY = new Ramp( 0xFFE0D0, 0xFF5A4A, 0xCC1111, 0x6A0A0A );
	public static final int FURY_LIGHT = 0xFF5A4A;

	/** Fire (Element.FIRE, the BURNING icon FFAA33, the meteor's FF9030): orange, never peach. */
	public static final Ramp FIRE = new Ramp( 0xFFD24A, 0xFF8418, 0xE04A10, 0xA82008 );
	public static final int FIRE_LIGHT = 0xFF9A48;
	/** Its one-frame white-hot core at an impact. */
	public static final int FIRE_CORE = 0xFFF3C8;
	/** Embers (light) and scorch (matter). */
	public static final int EMBER_HOT = 0xFFC060, EMBER = 0xF06020, EMBER_DEEP = 0x7A1808, SCORCH = 0x2A1E16;

	/** Frost (Element.FROST, Frost Nova 88DDFF): pale cyan to an ice blue. */
	public static final Ramp FROST = new Ramp( 0xE8F6FF, 0xA4E9FF, 0x5FB8D8, 0x4E80B8 );
	public static final int FROST_LIGHT = 0xA8D8FF;

	/** Poison, venom (PoisonParticle 6CCC3C / 8ADE4E / B8F060): a sickly green, never the heal's mint. */
	public static final Ramp POISON = new Ramp( 0xD8FF80, 0xB8F060, 0x6CCC3C, 0x2F8A2A );
	public static final int POISON_LIGHT = 0xA8F050;

	// ------------------------------------------------------------------ fear, the soul, the void

	/** Shadow and dread as LIGHT, for links and ink (the game's ShadowParticle: FLASH A070E0, RIM
	 *  9A5AD0): violet-black, saturated. Stop 0 (D0B0F0) is a one-frame flash only; a link's motes
	 *  never get paler than A070E0. School.SHADOW (B89CE4) and ShoutFX.DREAD stay as they are. */
	public static final Ramp SHADOW = new Ramp( 0xD0B0F0, 0xA070E0, 0x7A48B0, 0x3A2A55 );
	public static final int SHADOW_FLASH = 0xD0B0F0, SHADOW_LIGHT = 0x7A48B0;
	/** Ink (matter, normal-blended, it sinks and never rises) and its pools. */
	public static final int SHADOW_INK = 0x2A1F3A, SHADOW_POOL = 0x1E1628;

	/** The mage's soul (Soul Drain, Soul Detonation): a ghost's cold lavender-grey, apart from the
	 *  rogue's shadow lilac (B89CE4), arcane's blue-violet and a storm's sky blue. */
	public static final Ramp SOUL = new Ramp( 0xDCDCFA, 0xB4B4EC, 0x7C7AC4, 0x4A4888 );
	public static final int SOUL_LIGHT = 0xB4B4EC;

	/** A curse, a hex (Element.VOID): light being eaten, always drawn inward. */
	public static final Ramp VOID = new Ramp( 0xD8B8FF, 0x8A5AC0, 0x4A2A78, 0x24103E );
	public static final int VOID_LIGHT = 0x7A48C8;

	// ------------------------------------------------------------------ holy, arcane, wards, mana, the storm

	/** Holy light (Element.HOLY, Beam.LightRay / RayFx.LIGHT FFF6CC, Speck.LIGHT / YELLOW_LIGHT): gold. */
	public static final Ramp HOLY = new Ramp( 0xFFF4D0, 0xF1D99C, 0xC9A05A, 0x8A6420 );
	public static final int HOLY_LIGHT = 0xFFE9A8;
	/** A holy ray's core, and the gold of a holy star or a ray's halo. */
	public static final int HOLY_RAY = 0xFFF6CC, HOLY_STAR = 0xF2C050;

	/** Magic and runes (Element.ARCANE, the MAGIC_DMG icon 3499FF): blue-violet, never the rogue's lilac. */
	public static final Ramp ARCANE = new Ramp( 0xD8C8FF, 0x9070F0, 0x5A38C0, 0x2A1870 );
	public static final int ARCANE_LIGHT = 0xB8A0FF;
	/** Its accents: magic damage's blue and a ward's violet. */
	public static final int ARCANE_MAGIC = 0x3499FF, ARCANE_WARD = 0x8822FF;

	/** Spirit Volley's ward (MagicMissile.WARD, BoltFx's WARD: exact). */
	public static final Ramp WARD = new Ramp( 0xD8C0FF, 0xB080FF, 0x8822FF );
	public static final int WARD_LIGHT = 0xB080FF;

	/** Mana (the MANA text icon 44CCFF / 227AD0 / D0F6FF, Speck.BLUE_LIGHT, BlowPresenter.MANA_COLOR):
	 *  sky blue; never lilac, never gold. */
	public static final Ramp MANA = new Ramp( 0xD0F6FF, 0x44CCFF, 0x227AD0, 0x14457A );
	public static final int MANA_LIGHT = 0x44CCFF;

	/** A storm's sparks (Lightning's own halo 7FA6FF / 6A80FF, Element.ELECTRIC): white-blue, never
	 *  steel grey. Lightning.STORM tints the bolts themselves. */
	public static final Ramp LIGHTNING = new Ramp( 0xE4F0FF, 0xA8C8FF, 0x6A90FF, 0x3A56D8 );
	public static final int LIGHTNING_LIGHT = 0x7FA6FF;

	// ------------------------------------------------------------------ steel, earth, nature, gold, a daze, air

	/** Steel (Element.STEEL): cold grey-blue. */
	public static final Ramp STEEL = new Ramp( 0xE8F0FF, 0xB9DEEE, 0x7FA9C9, 0x50607A );
	public static final int STEEL_LIGHT = 0xB9DEEE;

	/** The warrior's earth and shouts (Element.EARTH, WarriorImpactFX): warm browns. */
	public static final Ramp MARTIAL = new Ramp( 0xF3DDB5, 0xD9AE75, 0x8C6A40 );
	public static final int MARTIAL_LIGHT = 0xD9AE75;

	/** The huntress (toward Element.NATURE C8F070 / 88CC44 / 4E9A30): leaf green, never the old
	 *  sage that read cream, and apart from the heal's mint. */
	public static final Ramp NATURE = new Ramp( 0xD4EEB4, 0xA8D880, 0x7CB850, 0x4E8A30 );
	public static final int NATURE_LIGHT = 0xA8D880;

	/** Luck, coins (Element.GOLD). */
	public static final Ramp GOLD = new Ramp( 0xFFF0A0, 0xFFD24A, 0xE0A020, 0xA06A10 );
	public static final int GOLD_LIGHT = 0xFFD860;

	/** A real daze, vertigo, paralysis: the game's own stun look (Looks.Stunned.COLOR, E0C369; a
	 *  test pins the two equal). */
	public static final int DAZE = Ramp.lerp( Element.METAL.ramp.stop( 1 ), 0xCCAA44, 0.6f );

	/** Air, a breath caught, calm (never a heal: real HP shows crosses): steam-white. */
	public static final Ramp BREATH = new Ramp( 0xE8F6FF, 0xD4E8F0, 0xB0CCD8 );

	// ------------------------------------------------------------------ runes (RuneFX)

	/** The runes' ramps (pale, mid, deep): arcane, mana, holy, a curse's, steel's, a ward's, blood's
	 *  (crimson, never the madness pink CC3355). */
	public static final Ramp RUNE_ARCANE = new Ramp( 0xD8C8FF, 0xB8A0FF, 0x9070F0 );
	public static final Ramp RUNE_MANA = new Ramp( 0xD0F6FF, 0x66CCFF, 0x227AD0 );
	public static final Ramp RUNE_HOLY = new Ramp( 0xFFF4D0, 0xF1D99C, 0xC9A05A );
	public static final Ramp RUNE_VOID = new Ramp( 0xD8B8FF, 0x8A5AC0, 0x4A2A78 );
	public static final Ramp RUNE_STEEL = new Ramp( 0xE8F0FF, 0xB9DEEE, 0x7FA9C9 );
	public static final Ramp RUNE_WARD = new Ramp( 0xD8C0FF, 0xB080FF, 0x8822FF );
	public static final Ramp RUNE_BLOOD = new Ramp( 0xFF5050, 0xD01818, 0x8A0A14 );

	/** Every light ramp by its name, in the palette sheet's order (the tests and the previews read it). */
	public static final Object[][] RAMPS = {
			{ "HEAL", HEAL }, { "BLOOD", BLOOD }, { "HEART", HEART }, { "FURY", FURY }, { "FIRE", FIRE },
			{ "FROST", FROST }, { "POISON", POISON }, { "SHADOW", SHADOW }, { "SOUL", SOUL }, { "VOID", VOID },
			{ "HOLY", HOLY }, { "ARCANE", ARCANE }, { "WARD", WARD }, { "MANA", MANA }, { "LIGHTNING", LIGHTNING },
			{ "STEEL", STEEL }, { "MARTIAL", MARTIAL }, { "NATURE", NATURE }, { "GOLD", GOLD }, { "BREATH", BREATH },
			{ "RUNE_ARCANE", RUNE_ARCANE }, { "RUNE_MANA", RUNE_MANA }, { "RUNE_HOLY", RUNE_HOLY },
			{ "RUNE_VOID", RUNE_VOID }, { "RUNE_STEEL", RUNE_STEEL }, { "RUNE_WARD", RUNE_WARD },
			{ "RUNE_BLOOD", RUNE_BLOOD } };

	/** A ramp's body, its stop 1 (the first coloured stop past the flash: a one-colour paint for
	 *  anything living past 0.1 s). */
	public static int mid( Ramp r ){
		return r.stop( 1 );
	}

	/** A ramp's deepest stop. */
	public static int deep( Ramp r ){
		return r.stop( r.size() - 1 );
	}
}
