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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.AntiEntropy;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Bulk;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Corrosion;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Displacement;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Metabolism;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Multiplicity;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Overgrowth;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Stench;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Adaptation;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Affection;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.AntiMagic;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Bounce;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Brimstone;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Camouflage;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Entanglement;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Flow;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Mirrorimage;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Mirroring;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Obfuscation;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Phasing;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Potential;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Purity;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Repulsion;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Satisfying;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Siphoning;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Stone;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Swiftness;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Thorns;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Viscosity;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Annoying;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Dazzling;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Displacing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Explosive;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Friendly;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Polarized;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Pressurized;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Sacrificial;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Wayward;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Wondrous;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Ancient;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.AresLeech;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blocking;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blooming;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.BuzzSaw;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Corrupting;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.CromLuck;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Elastic;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Eldritch;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Holy;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Horror;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Hunting;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.JupitersHorror;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Kinetic;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.LokisPoison;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Luck;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Lucky;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Midas;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.NeptuneShock;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Nomnom;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Osmose;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Parasitic;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Poison;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Projecting;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Shocking;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Slashing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Surging;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Unstable;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Venomous;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vicious;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vorpal;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.Family.*;

/**
 * Every enchantment's, glyph's and curse's look (EnchantLook), 78 rows, by class, by code and by
 * the Glowing its class shows: built once as the class loads and never changed after, so it is
 * read freely on any thread.
 *
 * The codes are pinned and append only (EnchantNet sends them; a guest of another build reads the
 * same number): the 37 weapon enchantments 0-36, the 23 glyphs 37-59, the 10 weapon curses 60-69,
 * the 8 armour curses 70-77, each list in the art spec's order. A new enchantment takes the next
 * free code at the end; nobody renumbers. An area tuning its families' rows changes their
 * colours, motifs, profiles and sounds only.
 *
 * Rows by the art spec (its sections 6, 6.1, 7 and 7.2); a curse's motifs are drawn inward and
 * its glow is the CURSE profile (Explosive keeps its fuse's PULSE periods).
 */
public final class EnchantLooks {

	private EnchantLooks(){}

	/** How many rows: every code below this has one. */
	public static final int COUNT = 78;
	/** Where each list starts. */
	public static final int ENCHANTMENTS = 0, GLYPHS = 37, WEAPON_CURSES = 60, ARMOR_CURSES = 70;

	private static final EnchantLook[] BY_CODE = new EnchantLook[COUNT];
	private static final HashMap<Class<?>, EnchantLook> BY_CLASS = new HashMap<>();
	private static final IdentityHashMap<ItemSprite.Glowing, EnchantLook> BY_GLOWING = new IdentityHashMap<>();

	static {
		weaponEnchantments();
		glyphs();
		weaponCurses();
		armorCurses();
		for (int i = 0; i < COUNT; i++){
			if (BY_CODE[i] == null) throw new IllegalStateException( "no look for code " + i );
		}
		for (EnchantLook l : BY_CODE) glowsOf( l );
	}

	/** The look of an enchantment, glyph or curse class; null for none. */
	public static EnchantLook of( Class<?> c ){
		return c == null ? null : BY_CLASS.get( c );
	}

	/** The look of a code; null off the table (a newer host's code). */
	public static EnchantLook byCode( int code ){
		return code >= 0 && code < COUNT ? BY_CODE[code] : null;
	}

	/** The look whose class shows this very Glowing (every state of it: a crystal's flaws, an
	 *  explosive's fuse); null for any other glow (a plain item's, an averaged two-enchant one). */
	public static EnchantLook forGlowing( ItemSprite.Glowing g ){
		return g == null ? null : BY_GLOWING.get( g );
	}

	/** Every look, by code. */
	public static java.util.List<EnchantLook> all(){
		return Collections.unmodifiableList( java.util.Arrays.asList( BY_CODE ) );
	}

	// ------------------------------------------------------------------ the rows

	private static void weaponEnchantments(){
		row( 0, Blazing.class, EMBER, Element.FIRE, 0xFF4400 ).motif( Motif.RISE, Profile.SMOULDER ).sound( Assets.Sounds.BURNING, 0.4f, 1.2f ).add();
		row( 1, Chilling.class, RIME, Element.FROST, 0x00FFFF ).motif( Motif.FALL, Profile.BREATH ).sound( Assets.Sounds.SHATTER, 0.25f, 1.7f )
				.rune( FxFrames.SNOWFLAKE_5[0] ).add();
		row( 2, Shocking.class, STORM, Element.ELECTRIC, 0xFFFFFF ).motif( Motif.CRACKLE, Profile.FLICKER ).sound( Assets.Sounds.LIGHTNING, 0.35f, 1f ).own().add();
		row( 3, Kinetic.class, FORCE, Element.METAL, 0xD8B040 ).motif( Motif.THROB, Profile.PULSE ).sound( Assets.Sounds.HIT_STRONG, 0.4f, 0.7f ).add();
		row( 4, Venomous.class, VENOM, Element.POISON, 0x6CCC3C ).motif( Motif.DRIP, Profile.BREATH ).sound( Assets.Sounds.GAS, 0.3f, 1.6f ).add();
		row( 5, Blocking.class, WARD, Element.ARCANE, 0x4A7AFF ).motif( Motif.ORBIT, Profile.STEADY ).sound( Assets.Sounds.HIT_PARRY, 0.35f, 1.5f ).add();
		row( 6, Blooming.class, VERDANT, Element.NATURE, 0x008800 ).motif( Motif.SPROUT, Profile.BREATH ).sound( Assets.Sounds.PLANT, 0.4f, 1.2f ).add();
		row( 7, Elastic.class, FORCE, Element.LIFE, 0xFF00FF ).motif( Motif.THROB, Profile.PULSE ).sound( Assets.Sounds.HIT_PARRY, 0.25f, 0.6f ).add();
		row( 8, Eldritch.class, DREAD, Element.SHADOW, 0x222222 ).rim( 0xA070E0 ).eye( 0xFF4060 ).motif( Motif.SEEP, Profile.INVERT ).sound( Assets.Sounds.GHOST, 0.4f, 0.6f ).add();
		row( 9, Lucky.class, FORTUNE, Element.GOLD, 0x00FF00 ).motif( Motif.ORBIT, Profile.SHIMMER ).sound( Assets.Sounds.DEWDROP, 0.4f, 1.6f )
				.rune( FxFrames.CLOVER_5 ).add();
		row( 10, Projecting.class, MIRAGE, Element.ARCANE, 0x8844CC ).motif( Motif.GHOST, Profile.STEADY ).sound( Assets.Sounds.MELD, 0.25f, 1.4f ).add();
		row( 11, Unstable.class, FORTUNE, Element.PRISM, 0x999999 ).motif( Motif.CYCLE, Profile.CYCLE ).sound( Assets.Sounds.ZAP, 0.3f, 1.6f )
				.cycle( 0xFF4400, 0x00FFFF, 0xFFFFFF, 0xD8B040, 0x6CCC3C, 0x4A7AFF, 0x008800, 0xFF00FF, 0xA070E0, 0x00FF00, 0x8844CC, 0xAA6666, 0x9A5AD0, 0xD01818 ).add();
		row( 12, Vorpal.class, CUT, Element.BLOOD, 0xAA6666 ).motif( Motif.DRIP, Profile.SHIMMER ).sound( Assets.Sounds.HIT_SLASH, 0.3f, 0.85f ).add();
		row( 13, Corrupting.class, DREAD, Element.SHADOW, 0x440066 ).rim( 0x9A5AD0 ).motif( Motif.SEEP, Profile.INVERT ).sound( Assets.Sounds.GHOST, 0.45f, 0.6f ).add();
		row( 14, xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Crystal.class, WARD, Element.STEEL, 0x0088FF )
				.motif( Motif.GLINT, Profile.SHIMMER ).sound( Assets.Sounds.SHATTER, 0.3f, 1.4f ).own().rune( FxFrames.SHARD_GLASS[0] ).add();
		row( 15, Grim.class, REAPER, Element.DEATH, 0x111133 ).rim( 0xA070E0 ).motif( Motif.SEEP, Profile.INVERT ).sound( Assets.Sounds.BONES, 0.6f, 0.8f ).add();
		row( 16, Vampiric.class, DRAIN, Element.BLOOD, 0x660022 ).rim( 0xD01818 ).motif( Motif.SIP, Profile.HEARTBEAT ).sound( Assets.Sounds.DRINK, 0.4f, 0.85f ).add();
		row( 17, Paralysis.class, STORM, Element.METAL, 0xCCAA44 ).motif( Motif.THROB, Profile.STEADY ).sound( Assets.Sounds.ZAP, 0.4f, 0.6f ).add();
		row( 18, Surging.class, STORM, Element.METAL, 0xFFCC00 ).motif( Motif.ORBIT, Profile.FLICKER ).sound( Assets.Sounds.CHARGEUP, 0.3f, 0.9f ).add();
		row( 19, Parasitic.class, VERDANT, Element.NATURE, 0x88CC44 ).motif( Motif.SPROUT, Profile.BREATH ).sound( Assets.Sounds.PLANT, 0.35f, 0.7f )
				.rune( FxFrames.SEED ).add();
		row( 20, Horror.class, DREAD, Element.SHADOW, 0x6A4A8A ).rim( 0x9A5AD0 ).motif( Motif.SEEP, Profile.INVERT ).sound( Assets.Sounds.GHOST, 0.35f, 0.55f ).add();
		row( 21, Hunting.class, CUT, Element.BLOOD, 0xFF3333 ).motif( Motif.GLINT, Profile.SHIMMER ).sound( Assets.Sounds.HIT_STAB, 0.3f, 0.8f ).add();
		row( 22, Midas.class, FORTUNE, Element.GOLD, 0xFFD700 ).motif( Motif.ORBIT, Profile.SHIMMER ).sound( Assets.Sounds.GOLD, 0.3f, 1f ).own()
				.rune( FxFrames.COIN_6[0] ).add();
		row( 23, Holy.class, RADIANCE, Element.HOLY, 0xFFE9A8 ).motif( Motif.RISE, Profile.SHIMMER ).sound( Assets.Sounds.HIT_MAGIC, 0.4f, 1.3f ).add();
		row( 24, Vicious.class, CUT, Element.BLOOD, 0x991111 ).motif( Motif.DRIP, Profile.FLICKER ).sound( Assets.Sounds.HIT_SLASH, 0.3f, 0.75f ).add();
		row( 25, Ancient.class, FORCE, Element.EARTH, 0xA08850 ).motif( Motif.FALL, Profile.BREATH ).sound( Assets.Sounds.ROCKS_LIGHT, 0.35f, 1f )
				.rune( FxFrames.RUNE_3[2] ).add();
		row( 26, Luck.class, FORTUNE, Element.PRISM, 0x33CC66 ).motif( Motif.ORBIT, Profile.SHIMMER ).sound( Assets.Sounds.DEWDROP, 0.3f, 1.5f ).add();
		row( 27, Slashing.class, CUT, Element.STEEL, 0xAAFF00 ).motif( Motif.GLINT, Profile.SHIMMER ).sound( Assets.Sounds.HIT_SLASH, 0.3f, 1.3f ).add();
		row( 28, CromLuck.class, FORTUNE, Element.GOLD, 0x800000 ).rim( 0xFFD860 ).motif( Motif.ORBIT, Profile.SHIMMER ).sound( Assets.Sounds.GOLD, 0.2f, 1.6f )
				.rune( FxFrames.COIN_6[0] ).add();
		row( 29, AresLeech.class, DRAIN, Element.BLOOD, 0x660066 ).rim( 0xFF6080 ).motif( Motif.SIP, Profile.HEARTBEAT ).sound( Assets.Sounds.DRINK, 0.5f, 0.7f ).add();
		row( 30, BuzzSaw.class, CUT, Element.STEEL, 0xBB3300 ).motif( Motif.ORBIT, Profile.STEADY ).sound( Assets.Sounds.SAW, 0.45f, 1f ).add();
		row( 31, JupitersHorror.class, DREAD, Element.SHADOW, 0x554488 ).rim( 0xC8B0FF ).motif( Motif.SEEP, Profile.INVERT ).sound( Assets.Sounds.GHOST, 0.35f, 0.65f ).add();
		row( 32, LokisPoison.class, VENOM, Element.POISON, 0x7700CC ).rim( 0xF0507A ).motif( Motif.SEEP, Profile.BREATH ).sound( Assets.Sounds.GAS, 0.4f, 0.8f ).add();
		row( 33, NeptuneShock.class, STORM, Element.ELECTRIC, 0x0044FF ).motif( Motif.CRACKLE, Profile.FLICKER ).sound( Assets.Sounds.LIGHTNING, 0.35f, 1f ).own().add();
		row( 34, Nomnom.class, HEART, Element.LIFE, 0xCC2266 ).motif( Motif.RISE, Profile.HEARTBEAT ).sound( Assets.Sounds.EAT, 0.5f, 1f ).add();
		row( 35, Osmose.class, DRAIN, Element.SACRIFICIAL, 0x2244CC ).motif( Motif.SIP, Profile.BREATH ).sound( Assets.Sounds.HIT_MAGIC, 0.3f, 1.5f ).add();
		row( 36, Poison.class, VENOM, Element.POISON, 0x4400AA ).rim( 0x9A5AD0 ).motif( Motif.BUBBLE, Profile.BREATH ).sound( Assets.Sounds.GAS, 0.25f, 1.2f ).add();
	}

	private static void glyphs(){
		row( 37, Adaptation.class, VERDANT, Element.NATURE, 0x006633 ).motif( Motif.CYCLE, Profile.BREATH ).sound( Assets.Sounds.GRASS, 0.25f, 1f )
				.cycle( 0x44AA44, 0x3399CC, 0x228822, 0xFF8418, 0x9A5AD0 ).glyph().add();
		row( 38, Affection.class, HEART, Element.LIFE, 0xFF4488 ).motif( Motif.RISE, Profile.HEARTBEAT ).sound( Assets.Sounds.CHARMS, 0.3f, 1f ).glyph().add();
		row( 39, AntiMagic.class, WARD, Element.ARCANE, 0x88EEFF ).motif( Motif.ORBIT, Profile.STEADY ).sound( Assets.Sounds.BEACON, 0.2f, 0.8f ).glyph().add();
		row( 40, Afterimage.class, MIRAGE, Element.STEEL, 0xA0A8C8 ).motif( Motif.GHOST, Profile.STEADY ).sound( Assets.Sounds.MELD, 0.25f, 1.5f ).glyph().add();
		row( 41, Bounce.class, FORCE, Element.HALOMETHANE, 0x44AAFF ).motif( Motif.THROB, Profile.PULSE ).sound( Assets.Sounds.HIT_PARRY, 0.25f, 1.3f ).glyph().add();
		row( 42, Brimstone.class, EMBER, Element.FIRE, 0xFFAA22 ).motif( Motif.RISE, Profile.SMOULDER ).sound( Assets.Sounds.BURNING, 0.2f, 0.7f ).glyph().add();
		row( 43, Camouflage.class, VERDANT, Element.NATURE, 0x448822 ).motif( Motif.SPROUT, Profile.BREATH ).sound( Assets.Sounds.MELD, 0.3f, 1f ).own().glyph().add();
		row( 44, xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Crystal.class, WARD, Element.FROST, 0xDDEEFF )
				.motif( Motif.GLINT, Profile.SHIMMER ).sound( Assets.Sounds.SHATTER, 0.2f, 1.8f ).rune( FxFrames.SHARD_GLASS[0] ).glyph().add();
		row( 45, Entanglement.class, VERDANT, Element.EARTH, 0x663300 ).motif( Motif.FALL, Profile.BREATH ).sound( Assets.Sounds.PLANT, 0.3f, 0.7f )
				.rune( FxFrames.BARK[0] ).glyph().add();
		row( 46, Flow.class, MIRAGE, Element.HALOMETHANE, 0x3399CC ).motif( Motif.DRIP, Profile.BREATH ).glyph().add();
		row( 47, Mirrorimage.class, MIRAGE, Element.GOLD, 0xE0AD2F ).motif( Motif.GHOST, Profile.STEADY ).sound( Assets.Sounds.TELEPORT, 0.3f, 1f ).own().glyph().add();
		row( 48, Mirroring.class, MIRAGE, Element.STEEL, 0xCCAA88 ).motif( Motif.GHOST, Profile.STEADY ).glyph().add();
		row( 49, Obfuscation.class, DREAD, Element.SMOKE, 0x5E5850 ).rim( 0x8A847C ).motif( Motif.SEEP, Profile.BREATH ).glyph().add();
		row( 50, Phasing.class, MIRAGE, Element.HALOMETHANE, 0x66AAFF ).motif( Motif.GHOST, Profile.FLICKER ).sound( Assets.Sounds.TELEPORT, 0.3f, 1f ).own().glyph().add();
		row( 51, Potential.class, STORM, Element.ELECTRIC, 0xDDAAFF ).motif( Motif.CRACKLE, Profile.FLICKER ).sound( Assets.Sounds.ZAP, 0.2f, 1.5f ).glyph().add();
		row( 52, Purity.class, RADIANCE, Element.HOLY, 0xFFFFCC ).motif( Motif.RISE, Profile.SHIMMER ).sound( Assets.Sounds.BEACON, 0.25f, 1.4f ).glyph().add();
		row( 53, Repulsion.class, FORCE, Element.STEEL, 0xCCCCFF ).motif( Motif.THROB, Profile.PULSE ).sound( Assets.Sounds.BLAST, 0.25f, 1.5f ).glyph().add();
		row( 54, Satisfying.class, RADIANCE, Element.HOLY, 0xFFE0A0 ).motif( Motif.ORBIT, Profile.BREATH ).sound( Assets.Sounds.EAT, 0.25f, 1.3f ).glyph().add();
		row( 55, Siphoning.class, DRAIN, Element.BLOOD, 0x550033 ).rim( 0xFF6080 ).motif( Motif.SIP, Profile.HEARTBEAT ).sound( Assets.Sounds.DRINK, 0.35f, 0.8f ).glyph().add();
		row( 56, Stone.class, FORCE, Element.EARTH, 0x776655 ).motif( Motif.FALL, Profile.BREATH ).sound( Assets.Sounds.ROCKS_LIGHT, 0.25f, 1f ).glyph().add();
		row( 57, Swiftness.class, MIRAGE, Element.HEAL, 0x66FFCC ).motif( Motif.GLINT, Profile.SHIMMER ).glyph().add();
		row( 58, Thorns.class, CUT, Element.NATURE, 0x557711 ).rim( 0xD01818 ).motif( Motif.SPROUT, Profile.BREATH ).sound( Assets.Sounds.HIT_STAB, 0.3f, 1.4f )
				.rune( FxFrames.BLADE_4 ).glyph().add();
		row( 59, Viscosity.class, VENOM, Element.ACID, 0x99AA33 ).motif( Motif.DRIP, Profile.BREATH ).sound( Assets.Sounds.WATER, 0.2f, 0.5f ).glyph().add();
	}

	private static void weaponCurses(){
		row( 60, Annoying.class, FORCE, Element.STEEL, 0x000000 ).motif( Motif.SEEP, Profile.CURSE ).curse().sound( Assets.Sounds.MIMIC, 0.3f, 1f ).own().add();
		row( 61, Dazzling.class, RADIANCE, Element.HOLY, 0x000000 ).motif( Motif.GLINT, Profile.CURSE ).curse().sound( Assets.Sounds.BLAST, 0.3f, 1f ).own().add();
		row( 62, Displacing.class, MIRAGE, Element.SHADOW, 0x000000 ).motif( Motif.GHOST, Profile.CURSE ).curse().sound( Assets.Sounds.TELEPORT, 0.3f, 0.7f ).add();
		row( 63, Explosive.class, EMBER, Element.HELL, 0x000000 ).motif( Motif.RISE, Profile.PULSE ).curse().sound( Assets.Sounds.BLAST, 0.3f, 1f ).own().add();
		row( 64, Friendly.class, HEART, Element.LIFE, 0x000000 ).motif( Motif.RISE, Profile.CURSE ).curse().sound( Assets.Sounds.CHARMS, 0.25f, 0.8f ).add();
		row( 65, Polarized.class, STORM, Element.ELECTRIC, 0x000000 ).motif( Motif.CYCLE, Profile.CURSE ).curse().sound( Assets.Sounds.ZAP, 0.2f, 1.3f )
				.cycle( 0xFF4040, 0x4060FF ).add();
		row( 66, Pressurized.class, FORCE, Element.STEAM, 0x000000 ).motif( Motif.SEEP, Profile.CURSE ).curse().sound( Assets.Sounds.GAS, 0.3f, 1f ).own().add();
		row( 67, Sacrificial.class, DRAIN, Element.SACRIFICIAL, 0x000000 ).motif( Motif.SIP, Profile.CURSE ).curse().sound( Assets.Sounds.DRINK, 0.3f, 0.5f ).add();
		row( 68, Wayward.class, MIRAGE, Element.SMOKE, 0x000000 ).motif( Motif.GHOST, Profile.CURSE ).curse().sound( Assets.Sounds.DEBUFF, 0.3f, 1f ).add();
		row( 69, Wondrous.class, FORTUNE, Element.PRISM, 0x000000 ).motif( Motif.CYCLE, Profile.CURSE ).curse().sound( Assets.Sounds.ZAP, 0.3f, 0.7f )
				.cycle( 0xFF5A5A, 0xFFE860, 0x50C8FF, 0xB070F0 ).add();
	}

	private static void armorCurses(){
		row( 70, AntiEntropy.class, RIME, Element.FROST, 0x000000 ).motif( Motif.FALL, Profile.CURSE ).curse().glyph().sound( Assets.Sounds.CURSED, 0.35f, 1f ).add();
		row( 71, Bulk.class, FORCE, Element.EARTH, 0x000000 ).motif( Motif.THROB, Profile.CURSE ).curse().glyph().sound( Assets.Sounds.CURSED, 0.35f, 1f ).add();
		row( 72, Corrosion.class, VENOM, Element.ACID, 0x000000 ).motif( Motif.DRIP, Profile.CURSE ).curse().glyph().sound( Assets.Sounds.CURSED, 0.35f, 1f ).add();
		row( 73, Displacement.class, MIRAGE, Element.SHADOW, 0x000000 ).motif( Motif.GHOST, Profile.CURSE ).curse().glyph().sound( Assets.Sounds.CURSED, 0.35f, 1f ).add();
		row( 74, Metabolism.class, DRAIN, Element.HEAL, 0x000000 ).motif( Motif.SIP, Profile.CURSE ).curse().glyph().sound( Assets.Sounds.CURSED, 0.35f, 1f ).add();
		row( 75, Multiplicity.class, MIRAGE, Element.VOID, 0x000000 ).motif( Motif.GHOST, Profile.CURSE ).curse().glyph().sound( Assets.Sounds.CURSED, 0.35f, 1f ).add();
		row( 76, Overgrowth.class, VERDANT, Element.NATURE, 0x000000 ).motif( Motif.SPROUT, Profile.CURSE ).curse().glyph().sound( Assets.Sounds.CURSED, 0.35f, 1f ).add();
		row( 77, Stench.class, VENOM, Element.POISON, 0x000000 ).motif( Motif.SEEP, Profile.CURSE ).curse().glyph().sound( Assets.Sounds.CURSED, 0.35f, 1f ).add();
	}

	// ------------------------------------------------------------------ building

	/** The rune frame a family's gained item takes unless its row names one (null: the item's own
	 *  frame, a ghost of it). */
	static int[] rune( Family f ){
		switch (f){
			case EMBER:    return FxFrames.EMBER_2;
			case RIME:     return FxFrames.SHARD_ICE[0];
			case STORM:    return FxFrames.SPARK_2;
			case VENOM:    return FxFrames.BUBBLE_3;
			case VERDANT:  return FxFrames.LEAF_D[0];
			case DREAD:    return FxFrames.INK_WISP[0];
			case REAPER:   return FxFrames.SKULL_5;
			case CUT:      return FxFrames.MOTE_2;
			case DRAIN:    return FxFrames.MOTE_2;
			case FORCE:    return FxFrames.PEBBLE_2;
			case WARD:     return FxFrames.RUNE_3[0];
			case RADIANCE: return FxFrames.STAR4_5;
			case FORTUNE:  return FxFrames.PLUS_3;
			case HEART:    return FxFrames.MOTE_2;
			default:       return null;
		}
	}

	private static Row row( int code, Class<?> of, Family fam, Element el, int glow ){
		return new Row( code, of, fam, el, glow );
	}

	//one row being written
	private static final class Row {
		final int code;
		final Class<?> of;
		final Family fam;
		final Element el;
		final int glow;
		int rim = -1, eye = -1;
		Motif motif = Motif.RISE;
		Profile profile = Profile.PULSE;
		String sound;
		float vol = 0.3f, pitch = 1f;
		boolean own, curse, glyph;
		int[] rune, cycle;

		Row( int code, Class<?> of, Family fam, Element el, int glow ){
			this.code = code;
			this.of = of;
			this.fam = fam;
			this.el = el;
			this.glow = glow;
			rune = EnchantLooks.rune( fam );
		}

		Row rim( int c ){ rim = c; return this; }
		Row eye( int c ){ eye = c; return this; }
		Row motif( Motif m, Profile p ){ motif = m; profile = p; return this; }
		Row sound( String s, float v, float p ){ sound = s; vol = v; pitch = p; return this; }
		Row own(){ own = true; return this; }
		Row curse(){ curse = true; return this; }
		Row glyph(){ glyph = true; return this; }
		Row rune( int[] f ){ rune = f; return this; }
		Row cycle( int... c ){ cycle = c; return this; }

		void add(){
			if (code < 0 || code >= COUNT || BY_CODE[code] != null) throw new IllegalStateException( "code " + code + " twice or off the table" );
			if (BY_CLASS.containsKey( of )) throw new IllegalStateException( of + " twice" );
			//a curse's edge: the hex's violet unless it names one; anyone else's its own light
			int edge = rim != -1 ? rim : (curse ? Element.VOID.glint : el.light);
			EnchantLook l = new EnchantLook( code, of, fam, el, glow, edge, eye != -1 ? eye : edge, motif, profile, sound, vol, pitch, own, rune,
					cycle, curse, glyph );
			BY_CODE[code] = l;
			BY_CLASS.put( of, l );
		}
	}

	//every Glowing the class shows: the one a fresh one shows, and every one it keeps as a static
	//field (a crystal's flaws, an explosive's fuse)
	private static void glowsOf( EnchantLook l ){
		Object made = Reflection.newInstance( l.of );
		if (made != null){
			try {
				Method m = l.of.getMethod( "glowing" );
				Object g = m.invoke( made );
				if (g instanceof ItemSprite.Glowing) BY_GLOWING.put( (ItemSprite.Glowing) g, l );
			} catch (ReflectiveOperationException | RuntimeException ignored){
				//a glow that cannot be asked for outside a game is found through its fields below
			}
		}
		for (Field f : l.of.getDeclaredFields()){
			if (!Modifier.isStatic( f.getModifiers() ) || f.getType() != ItemSprite.Glowing.class) continue;
			try {
				f.setAccessible( true );
				Object g = f.get( null );
				if (g instanceof ItemSprite.Glowing) BY_GLOWING.put( (ItemSprite.Glowing) g, l );
			} catch (ReflectiveOperationException | RuntimeException ignored){
				//a field a shrunk build hides: the glowing() above still found its main glow
			}
		}
	}
}
