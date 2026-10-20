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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.HolyWard;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.HolyWeapon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Ancient;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.ElfBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.TaurcenBow;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Bundle;
import org.json.JSONArray;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * What an item shows of its magic wherever it is drawn as itself (a slot, the info window, a heap,
 * a flight): up to two looks (EnchantLook) crossfading, the vestige's echo, the enchant level that
 * makes its motes denser, the holy override's colour, and the glow a look takes from the item's
 * state (a crystal's flaws, an explosive's fuse). ItemSprite resolves it as it views an item; the
 * enchantment and glyph classes and their Glowings stay as they are.
 *
 * Whose magic: a weapon's own, or a launcher's shot's its launcher's (Weapon.magicSource: gun,
 * minigun, shotgun and cannon rounds, BowWeapon arrows), the spirit, elf and Taurcen bows' arrows
 * their bow's (launcher()); armour its glyphs. Good magic shows even unidentified (its glow already
 * shows today); a curse only once known; the echo only on an identified item. A co-op guest's
 * stand-in items carry what the host sent (sign).
 *
 * The glow itself: each look's profile (Profile) at most MAX_SHARE of the colour, the colour the
 * backlight carries; two looks hold HOLD s each and blend over BLEND s (crossB); a known curse
 * darkens with its rim showing; the holy override shimmers yellow. Pure functions of the time and
 * a phase of the item's own: no Random of either kind.
 */
public final class ItemLooks {

	private ItemLooks(){}

	/** The most of the colour a profile tints an item (the backlight carries the rest). */
	public static final float MAX_SHARE = 0.45f;
	/** Two looks: each held HOLD s, blended over BLEND s. */
	public static final float HOLD = 1.2f, BLEND = 0.4f;
	/** The backlight's bounds: a beat from TROUGH to PEAK, a lasting glow round MIDDLE by at most
	 *  LASTING_SWING of it. */
	public static final float PEAK = 0.45f, TROUGH = 0.25f, MIDDLE = 0.35f, LASTING_SWING = 0.12f;
	/** How much of its rim a known curse shows (INVERT shows Profile.RIM_SHARE). */
	public static final float CURSE_RIM = 0.12f;
	/** The holy override's colour (HolyWeapon's and HolyWard's glow). */
	public static final int HOLY = 0xFFFF00;
	/** CYCLE: each colour this long, s. */
	public static final float CYCLE_EACH = 1f;

	/** What an item shows: filled in place (ItemSprite keeps one), so a refresh allocates nothing. */
	public static final class Set {
		/** The looks, the first first; b only with a; the echo apart (it never tints). */
		public EnchantLook a, b, echo;
		/** The enchant level the motes go by (the first look's). */
		public int level;
		/** The holy override's colour, or -1. */
		public int holy = -1;
		/** The period of each look's Glowing as the item shows it now (a fuse, a flaw), 1 by default. */
		public float periodA = 1f, periodB = 1f;
		/** An Ancient weapon's runes of age: 1 + its counted kills / 4 (at most ANCIENT_RUNES); 0
		 *  for any other look, 1 for a guest's stand-in (the wire carries no kills). */
		public int runes;

		public void clear(){
			a = b = echo = null;
			level = 0;
			holy = -1;
			periodA = periodB = 1f;
			runes = 0;
		}

		/** Whether it shows anything of its own (a look or the holy glow). */
		public boolean any(){
			return a != null || holy >= 0;
		}

		/** Whether it shows the same as another (the key a slot's aura is re-keyed by). */
		public boolean same( Set o ){
			return o != null && a == o.a && b == o.b && echo == o.echo && level == o.level && holy == o.holy
					&& periodA == o.periodA && periodB == o.periodB && runes == o.runes;
		}

		public void copy( Set o ){
			a = o.a;
			b = o.b;
			echo = o.echo;
			level = o.level;
			holy = o.holy;
			periodA = o.periodA;
			periodB = o.periodB;
			runes = o.runes;
		}

		//one more look shown; the first sets the level and its glow's period
		void add( EnchantLook l, int lvl, float period ){
			if (l == null) return;
			if (a == null){
				a = l;
				level = lvl;
				periodA = period;
			} else if (b == null && l != a){
				b = l;
				periodB = period;
			}
		}
	}

	/** What the item shows, in a new Set (tests, the wire). */
	public static Set of( Item item ){
		Set s = new Set();
		into( item, item != null ? item.glowing() : null, s );
		return s;
	}

	/** What the item shows, given the Glowing it shows now (`shown`, for the holy override), into
	 *  `out`. Any thread; allocates nothing (an Ancient weapon's kills aside, read off its bundle). */
	public static void into( Item item, ItemSprite.Glowing shown, Set out ){
		out.clear();
		if (item == null) return;
		if (item instanceof Weapon){
			Weapon w = (Weapon) item;
			if (holy( w, shown )){
				out.holy = HOLY;
				return;
			}
			Weapon src = launcherOf( w );
			add( out, src.enchantment, src.cursedKnown );
			add( out, src.enchantment2, src.cursedKnown );
			if (src.enchantment instanceof Ancient) out.runes = runes( src.enchantment );
			else if (src.enchantment2 instanceof Ancient) out.runes = runes( src.enchantment2 );
			if (src.isIdentified()){
				Weapon.Enchantment v = Quality.vestigeOf( src );
				if (v != null) out.echo = EnchantLooks.of( v.getClass() );
			}
		} else if (item instanceof Armor){
			Armor ar = (Armor) item;
			if (holy( ar, shown )){
				out.holy = HOLY;
				return;
			}
			add( out, ar.glyph, ar.cursedKnown );
			add( out, ar.glyph2, ar.cursedKnown );
			if (ar.isIdentified()){
				Armor.Glyph v = Quality.glyphVestigeOf( ar );
				if (v != null) out.echo = EnchantLooks.of( v.getClass() );
			}
		} else {
			int[] sig = signed( item );
			if (sig != null) fromSig( sig, out );
		}
	}

	/** An Ancient weapon's most runes of age. */
	public static final int ANCIENT_RUNES = 3;

	//an Ancient weapon's runes of age: 1 + its kills toward the next attunement / 4 (it keeps the
	//count to itself; its bundle names it)
	private static int runes( Weapon.Enchantment ancient ){
		Bundle b = new Bundle();
		ancient.storeInBundle( b );
		return Math.min( ANCIENT_RUNES, 1 + Math.max( 0, b.getInt( "num_kills" ) ) / 4 );
	}

	/** The weapon whose magic a weapon shows: a shot's launcher (magicSource), a bow's arrow its bow. */
	public static Weapon launcherOf( Weapon w ){
		if (w instanceof SpiritBow.SpiritArrow) return ((SpiritBow.SpiritArrow) w).launcher();
		if (w instanceof ElfBow.ElfArrow) return ((ElfBow.ElfArrow) w).launcher();
		if (w instanceof TaurcenBow.TaurcenArrow) return ((TaurcenBow.TaurcenArrow) w).launcher();
		Weapon src = w.magicSource();
		return src != null ? src : w;
	}

	private static void add( Set out, Weapon.Enchantment e, boolean cursedKnown ){
		if (e == null || (e.curse() && !cursedKnown)) return;
		ItemSprite.Glowing g = e.glowing();
		out.add( EnchantLooks.of( e.getClass() ), e.level(), g != null ? g.period : 1f );
	}

	private static void add( Set out, Armor.Glyph g, boolean cursedKnown ){
		if (g == null || (g.curse() && !cursedKnown)) return;
		ItemSprite.Glowing gl = g.glowing();
		out.add( EnchantLooks.of( g.getClass() ), g.level(), gl != null ? gl.period : 1f );
	}

	//the override HolyWeapon puts on the hero's weapon: a yellow glow none of its enchantments shows
	private static boolean holy( Weapon w, ItemSprite.Glowing shown ){
		if (!yellowOverride( shown )) return false;
		Hero h = Dungeon.hero;
		return h != null && w.isEquipped( h ) && h.buff( HolyWeapon.HolyWepBuff.class ) != null;
	}

	private static boolean holy( Armor a, ItemSprite.Glowing shown ){
		if (!yellowOverride( shown )) return false;
		Hero h = Dungeon.hero;
		return h != null && a.isEquipped( h ) && h.buff( HolyWard.HolyArmBuff.class ) != null;
	}

	private static boolean yellowOverride( ItemSprite.Glowing g ){
		return g != null && (g.color & 0xFFFFFF) == HOLY && EnchantLooks.forGlowing( g ) == null;
	}

	// ------------------------------------------------------------------ the wire

	//a co-op guest's stand-in items and what the host said they show
	private static final Map<Item, int[]> SIGNED = Collections.synchronizedMap( new WeakHashMap<>() );

	/** What an item shows, for the wire: { first look's code, second's, the echo's, the level, its
	 *  rarity's ordinal (its launcher's for a shot) }, -1 for none; null when it shows nothing (or
	 *  only the holy glow, which a guest does not hold). A reader takes what it knows by place, so an
	 *  older peer reads the first four and a newer one finds no rarity in an older sig. */
	public static int[] sig( Item item ){
		if (item == null) return null;
		Set s = of( item );
		if (s.a == null && s.echo == null) return null;
		Item whose = item instanceof Weapon ? launcherOf( (Weapon) item ) : item;
		Quality q = Quality.of( whose );
		int rarity = q != null && q.rarity != null ? q.rarity.ordinal() : -1;
		return new int[]{ code( s.a ), code( s.b ), code( s.echo ), s.level, rarity };
	}

	private static int code( EnchantLook l ){
		return l != null ? l.code : -1;
	}

	/** A guest's stand-in item shows what the host sent for it (sig's numbers); null or empty
	 *  forgets it. Any thread. */
	public static void sign( Item item, int[] sig ){
		if (item == null) return;
		if (sig == null || sig.length == 0) SIGNED.remove( item );
		else SIGNED.put( item, sig );
	}

	/** The same from the wire's list (a heap's "g", an item's or a missile's "sig"); null or empty
	 *  for none. Any thread. */
	public static void sign( Item item, JSONArray a ){
		if (a == null || a.length() == 0){
			sign( item, (int[]) null );
			return;
		}
		int[] sig = new int[a.length()];
		for (int k = 0; k < sig.length; k++) sig[k] = a.optInt( k, -1 );
		sign( item, sig );
	}

	/** What a stand-in item was signed with, null for nothing. */
	public static int[] signed( Item item ){
		return item == null ? null : SIGNED.get( item );
	}

	/** The rarity the host sent for a stand-in item, null for none (or an older host's sig). */
	public static Rarity signedRarity( Item item ){
		int[] sig = signed( item );
		if (sig == null || sig.length < 5 || sig[4] < 0 || sig[4] >= Rarity.values().length) return null;
		return Rarity.values()[sig[4]];
	}

	/** A sig's numbers into a Set: codes off the table (a newer host's) show nothing. */
	public static void fromSig( int[] sig, Set out ){
		out.clear();
		if (sig == null) return;
		if (sig.length > 0) out.add( EnchantLooks.byCode( sig[0] ), sig.length > 3 ? Math.max( 0, sig[3] ) : 0, 1f );
		if (sig.length > 1) out.add( EnchantLooks.byCode( sig[1] ), 0, 1f );
		if (sig.length > 2) out.echo = EnchantLooks.byCode( sig[2] );
		if (ancient( out.a ) || ancient( out.b )) out.runes = 1;
	}

	private static boolean ancient( EnchantLook l ){
		return l != null && l.of == Ancient.class;
	}

	// ------------------------------------------------------------------ the glow

	/** Where an item's own clock starts: a phase of its own, 0-10 s (System.identityHashCode). */
	public static float phase( Object item ){
		return item == null ? 0f : (System.identityHashCode( item ) & 0xFFFF) / 6553.6f;
	}

	/** The second look's weight at t s: 0 for HOLD s, up to 1 over BLEND s, 1 for HOLD s, back
	 *  down over BLEND s. */
	public static float crossB( float t ){
		float cycle = 2 * (HOLD + BLEND);
		float c = Profile.mod( t, cycle );
		if (c < HOLD) return 0f;
		if (c < HOLD + BLEND) return (c - HOLD) / BLEND;
		if (c < 2 * HOLD + BLEND) return 1f;
		return 1f - (c - 2 * HOLD - BLEND) / BLEND;
	}

	/** A look's share of the colour (of the dark, for INVERT and CURSE) at t s: its profile capped
	 *  at MAX_SHARE; PULSE as today's triangle on its Glowing's period, scaled under the cap. */
	public static float share( EnchantLook l, float t, float period ){
		float v;
		if (l.profile == Profile.PULSE){
			v = Profile.pulse( t, period ) * (MAX_SHARE / 0.6f);
		} else {
			v = l.profile.value( t, 0f );
		}
		return Math.max( 0f, Math.min( MAX_SHARE, v ) );
	}

	/** Whether a look's glow darkens (INVERT, CURSE) rather than tints. */
	public static boolean dark( EnchantLook l ){
		return l != null && (l.profile == Profile.INVERT || l.profile == Profile.CURSE);
	}

	/** The colour a look's glow and backlight carry at t s: its glow, CYCLE's walking colour, a
	 *  dark one's rim. */
	public static int color( EnchantLook l, float t ){
		if (l.cycle != null && (l.profile == Profile.CYCLE || l.motif == Motif.CYCLE)){
			return Profile.cycleColor( l.cycle, t, CYCLE_EACH );
		}
		return dark( l ) ? l.rim : l.glow;
	}

	/**
	 * The sprite's tint at t s, into out (rm, gm, bm, ra, ga, ba); returns the backlight's level
	 * (TROUGH..PEAK) and leaves its colour in out[6]. The item's own clock is t (its phase added).
	 */
	public static float tint( Set s, float t, float[] out ){
		if (s.holy >= 0){
			float v = 0.30f + 0.06f * (float)Math.sin( 2 * Math.PI * 3f * t );
			channels( s.holy, v, false, 0, 0f, out, 0 );
			out[6] = s.holy;
			return MIDDLE * (1f + LASTING_SWING * (v - 0.30f) / 0.06f);
		}
		float bA = channels( s.a, t, s.periodA, out, 0 );
		int colorA = color( s.a, t );
		if (s.b == null){
			out[6] = colorA;
			return bA;
		}
		float bB = channels( s.b, t, s.periodB, out, 7 );
		float k = crossB( t );
		for (int i = 0; i < 6; i++) out[i] += (out[7 + i] - out[i]) * k;
		out[6] = Ramp.lerp( colorA, color( s.b, t ), k );
		return bA + (bB - bA) * k;
	}

	//one look's channels at t s into out[at..at+5]; its backlight level
	private static float channels( EnchantLook l, float t, float period, float[] out, int at ){
		float v = share( l, t, period );
		boolean curse = l.profile == Profile.CURSE;
		channels( color( l, t ), v, dark( l ), l.rim, curse ? CURSE_RIM : Profile.RIM_SHARE, out, at );
		return backlight( l.profile, v );
	}

	private static void channels( int color, float v, boolean dark, int rim, float rimShare, float[] out, int at ){
		float m = 1f - v;
		out[at] = out[at + 1] = out[at + 2] = m;
		if (dark){
			out[at + 3] = ((rim >> 16) & 0xFF) / 255f * rimShare;
			out[at + 4] = ((rim >> 8) & 0xFF) / 255f * rimShare;
			out[at + 5] = (rim & 0xFF) / 255f * rimShare;
		} else {
			out[at + 3] = ((color >> 16) & 0xFF) / 255f * v;
			out[at + 4] = ((color >> 8) & 0xFF) / 255f * v;
			out[at + 5] = (color & 0xFF) / 255f * v;
		}
	}

	/** The backlight's level for a profile's share: a lasting glow MIDDLE, moving by at most
	 *  LASTING_SWING of it as its share moves; a beat TROUGH at rest to PEAK at its height. */
	public static float backlight( Profile p, float v ){
		if (p.lasting){
			float k = Math.max( -1f, Math.min( 1f, (v - p.base) / Profile.MOST_SWING ) );
			return MIDDLE * (1f + LASTING_SWING * k);
		}
		float lo = p == Profile.CURSE ? 0.05f : (p == Profile.PULSE ? 0f : p.base);
		float k = Math.max( 0f, Math.min( 1f, (v - lo) / Math.max( 0.01f, MAX_SHARE - lo ) ) );
		return TROUGH + (PEAK - TROUGH) * k;
	}

	// ------------------------------------------------------------------ twitches

	/**
	 * How far the item is drawn off its place at t s, whole px, for the few looks whose item moves
	 * (into out[0], out[1]): Horror's shiver and the Annoying curse's twitch (2 frames every 2 and
	 * 1.5 s), Nomnom's chomp (every 2 s), Elastic's and Bounce's 1 px bob at 1.2 Hz. 0 for the rest.
	 */
	public static void twitch( EnchantLook l, float t, float[] out ){
		out[0] = out[1] = 0;
		if (l == null) return;
		Class<?> c = l.of;
		if (c == xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Horror.class){
			float k = Profile.mod( t, 2f );
			if (k < TWITCH_FOR) out[0] = k < TWITCH_FOR / 2 ? 1 : -1;
		} else if (c == xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Annoying.class){
			float k = Profile.mod( t, 1.5f );
			if (k < TWITCH_FOR) out[0] = k < TWITCH_FOR / 2 ? -1 : 1;
		} else if (c == xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Nomnom.class){
			if (Profile.mod( t, 2f ) < TWITCH_FOR) out[1] = 1;
		} else if (c == xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Elastic.class
				|| c == xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Bounce.class){
			if (Math.sin( 2 * Math.PI * 1.2f * t ) > 0.3f) out[1] = -1;
		}
	}

	/** A twitch lasts two frames. */
	public static final float TWITCH_FOR = 0.067f;
}
