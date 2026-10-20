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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;

/**
 * How one enchantment, glyph or curse looks: a row of EnchantLooks, visual data only (the item's
 * Glowing colour, which names and texts take, is untouched). Immutable once made: safe to read on
 * any thread.
 *
 *   code     its number on the wire (EnchantNet) and in the registry: append only, never renumbered
 *   of       the enchantment, glyph or curse class it is the look of
 *   fam      how its proc acts out (Family); a curse plays it with the hex over it
 *   el       its palette (Element): ramp, light, glint, smoke and ember, light or matter
 *   glow     its look colour: the defender's afterglow, the item's tint and backlight
 *   rim      its edge colour (the dark ones' glint, so a dark glow still reads)
 *   eye      the colour of the eye a DREAD look opens (its rim unless the row names one)
 *   motif    how the item shows it while held, lying or flying (Motif)
 *   profile  how the item's glow lives (Profile)
 *   sound    its sting (an Assets.Sounds name, null for none), at vol and pitch; ownSound: the
 *            enchantment's own code already plays it on the host, so only co-op guests sting
 *   rune     the frame a gained item's runes take (Enchanting); null: the item's own frame (ghosts)
 *   cycle    the colours its CYCLE walks, null for none
 *   curse    a curse (the hex); glyph: armour's (its answers begin with the ward arc)
 *   emissive light (added, over the night) rather than matter: its element's
 *
 * And made from those, once: light (its element's), strike (the element its blows are drawn in:
 * its own, or a curse's VOID - the hex eats the light an enchantment would throw), pale (its ramp
 * drawn 45% toward the echo's grey, for a vestige's echo), tint (STEEL's ramp drawn 35% toward
 * strike's light: the crescent of an enchanted weapon's blow that did not proc) and star (white
 * to strike's light, a curse's from VOID's pale glint: a stamped hit star).
 */
public final class EnchantLook {

	/** The echo's grey, and how far an echo's colours are drawn toward it. */
	public static final int GHOST = 0xC8D0E0;
	public static final float PALE = 0.45f;
	/** How far a blow of an enchanted weapon that did not proc is drawn toward its look. */
	public static final float TINT = 0.35f;

	public final int code;
	public final Class<?> of;
	public final Family fam;
	public final Element el;
	public final int glow, rim, eye;
	public final Motif motif;
	public final Profile profile;
	public final String sound;
	public final float vol, pitch;
	public final boolean ownSound;
	public final int[] rune;
	public final int[] cycle;
	public final boolean curse, glyph, emissive;

	public final int light;
	public final Element strike;
	public final Ramp pale, tint, star;

	EnchantLook( int code, Class<?> of, Family fam, Element el, int glow, int rim, int eye, Motif motif, Profile profile,
			String sound, float vol, float pitch, boolean ownSound, int[] rune, int[] cycle, boolean curse, boolean glyph ){
		this.code = code;
		this.of = of;
		this.fam = fam;
		this.el = el;
		this.glow = glow & 0xFFFFFF;
		this.rim = rim & 0xFFFFFF;
		this.eye = eye & 0xFFFFFF;
		this.motif = motif;
		this.profile = profile;
		this.sound = sound;
		this.vol = vol;
		this.pitch = pitch;
		this.ownSound = ownSound;
		this.rune = rune;
		this.cycle = cycle;
		this.curse = curse;
		this.glyph = glyph;
		emissive = el.emissive;
		light = el.light;
		strike = curse ? Element.VOID : el;
		pale = toward( el.ramp, GHOST, PALE );
		tint = toward( Element.STEEL.ramp, strike.light, TINT );
		star = new Ramp( curse ? Element.VOID.glint : 0xFFFFFF, strike.light );
	}

	/** A colour of it as an echo shows it: drawn PALE toward GHOST. */
	public static int paled( int color ){
		return Ramp.lerp( color & 0xFFFFFF, GHOST, PALE );
	}

	//a ramp with every stop drawn k of the way toward a colour
	static Ramp toward( Ramp r, int color, float k ){
		int[] stops = new int[r.size()];
		for (int i = 0; i < stops.length; i++) stops[i] = Ramp.lerp( r.stop( i ), color, k );
		return new Ramp( stops );
	}

	@Override
	public String toString(){
		return of.getSimpleName() + "#" + code;
	}
}
