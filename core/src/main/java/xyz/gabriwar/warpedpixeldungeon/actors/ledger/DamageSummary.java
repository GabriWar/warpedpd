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

package xyz.gabriwar.warpedpixeldungeon.actors.ledger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The numbers over a list of the Damage tab (dealt or taken), and how a step's change and value
 * read: plain arithmetic on the records, no game state and no fonts, so it is tested headlessly.
 *
 * Landed: a record whose outcome is HIT (it took something). Rolled: a record with a roll to hit
 * (a blow or a zap; a miss always has one).
 * - average: what the landed records showed, over how many there are (a DOT run is one record:
 *   its ticks summed);
 * - top: the landed record that showed the most (the best dealt, the worst taken), the newest of
 *   equals;
 * - crits: crit blows over landed blows;
 * - hits: rolled records that hit over rolled records (dealt: the hero's hit chance in practice;
 *   taken: one minus it is how often he dodged);
 * - blocked: what armour and shields took away over all that came in (armour's cut plus what was
 *   shown, a shield's share being part of what was shown), armour being every armour step's cut
 *   (Step icon ARMOR_CELL);
 * - sources: the share of everything shown by each source icon, the three largest, the rest
 *   lumped.
 * A ratio with nothing under it is NaN (the tab writes a dash).
 */
public final class DamageSummary {

	/** The armour step's icon (FloatingText.HIT_ARM, as DamageLedger.ARMOR records it). */
	public static final int ARMOR_CELL = 37;
	/** Char.INFINITE_ACCURACY and INFINITE_EVASION: a value this large reads as endless. */
	static final float INFINITE = 1_000_000f;
	/** How many sources the summary names. */
	public static final int TOP = 3;

	public final int count, landed, rolled;
	public final float average, crits, hits, blocked;
	public final HitRecord top;
	/** The largest sources' icons (a record's sourceIcon) and their shares of all shown, largest
	 *  first, at most TOP; then what the rest took. */
	public final int[] sourceIcons;
	public final float[] sourceShares;
	public final float otherShare;
	/** Totals behind the ratios. */
	public final int shown, shieldTaken;
	public final float armorTaken;

	public DamageSummary( List<HitRecord> records ){
		int n = 0, land = 0, roll = 0, hit = 0, blows = 0, crit = 0, sum = 0, shield = 0;
		float armor = 0f;
		HitRecord best = null;
		Map<Integer, Integer> bySource = new HashMap<>();
		for (HitRecord r : records){
			if (r == null) continue;
			n++;
			if (r.roll != null){
				roll++;
				if (r.roll.hit) hit++;
			}
			armor += armorCut( r );
			shield += r.shieldLost;
			if (r.outcome != HitRecord.HIT) continue;
			land++;
			sum += r.total();
			if (r.kind == HitRecord.BLOW){
				blows++;
				if (r.crit) crit++;
			}
			//newest first: a later equal keeps the newer
			if (best == null || r.total() > best.total()) best = r;
			Integer k = r.sourceIcon;
			Integer had = bySource.get( k );
			bySource.put( k, (had == null ? 0 : had) + r.total() );
		}
		count = n;
		landed = land;
		rolled = roll;
		shown = sum;
		shieldTaken = shield;
		armorTaken = armor;
		top = best;
		average = land == 0 ? Float.NaN : sum / (float) land;
		crits = blows == 0 ? Float.NaN : crit / (float) blows;
		hits = roll == 0 ? Float.NaN : hit / (float) roll;
		//what came in is what armour cut plus what was shown (a shield's share is in what was shown)
		float stopped = armor + shield;
		blocked = armor + sum <= 0f ? Float.NaN : stopped / (armor + sum);

		List<Map.Entry<Integer, Integer>> e = new ArrayList<>( bySource.entrySet() );
		//largest first, the smaller icon first between equals (a stable order)
		Collections.sort( e, ( a, b ) -> {
			int c = Integer.compare( b.getValue(), a.getValue() );
			return c != 0 ? c : Integer.compare( a.getKey(), b.getKey() );
		} );
		int k = 0;
		for (Map.Entry<Integer, Integer> x : e) if (x.getValue() > 0 && k < TOP) k++;
		sourceIcons = new int[k];
		sourceShares = new float[k];
		float named = 0f;
		for (int i = 0; i < k; i++){
			sourceIcons[i] = e.get( i ).getKey();
			sourceShares[i] = sum == 0 ? 0f : e.get( i ).getValue() / (float) sum;
			named += sourceShares[i];
		}
		otherShare = sum == 0 || k == 0 ? 0f : Math.max( 0f, 1f - named );
	}

	/** What armour took off a record: every armour step's cut (a DOT run's latest tick only has
	 *  steps, and armour never stops a tick). */
	public static float armorCut( HitRecord r ){
		float cut = 0f;
		for (Step s : r.steps){
			if (s.icon == ARMOR_CELL && s.change() < 0f) cut -= s.change();
		}
		return cut;
	}

	/** Whether a ratio has something under it. */
	public static boolean known( float ratio ){
		return !Float.isNaN( ratio );
	}

	/** A ratio as a whole percent, "62". */
	public static int percent( float ratio ){
		return Math.round( ratio * 100f );
	}

	// ------------------------------------------------------------------ how numbers read

	/** A value: at most two decimals, trailing zeros dropped ("12", "12.25", "13.2"), with this
	 *  decimal separator. */
	public static String value( float v, char sep ){
		//a stat that cannot miss or cannot be hit (Char's INFINITE_ACCURACY / INFINITE_EVASION)
		if (Math.abs( v ) >= INFINITE) return v < 0 ? "-∞" : "∞";
		long hundredths = Math.round( v * 100.0 );
		if (hundredths == 0) return "0";
		boolean neg = hundredths < 0;
		long a = Math.abs( hundredths );
		long whole = a / 100, frac = a % 100;
		StringBuilder s = new StringBuilder();
		if (neg) s.append( '-' );
		s.append( whole );
		if (frac != 0){
			s.append( sep );
			if (frac % 10 == 0) s.append( frac / 10 );
			else s.append( frac < 10 ? "0" : "" ).append( frac );
		}
		return s.toString();
	}

	/** A multiplier: "x" and two decimals, trailing zeros dropped down to one ("x1.5", "x1.33",
	 *  "x2.0"); three decimals for a factor under 0.1 so it never reads as nothing. */
	public static String factor( float f, char sep ){
		int places = Math.abs( f ) < 0.1f ? 3 : 2;
		double scale = Math.pow( 10, places );
		long n = Math.round( Math.abs( f ) * scale );
		long whole = (long) (n / scale), frac = n - (long) (whole * scale);
		String digits = Long.toString( frac );
		while (digits.length() < places) digits = "0" + digits;
		while (digits.length() > 1 && digits.endsWith( "0" )) digits = digits.substring( 0, digits.length() - 1 );
		return (f < 0 ? "x-" : "x") + whole + sep + digits;
	}

	/** A step's change column: "x1.5" for a multiplier, "+4" / "-1" for an addition, "= 1" for a
	 *  value set, "≈" for a rounding, nothing for the base. A multiplier from 0 reads as a set. */
	public static String change( Step s, char sep ){
		switch (s.op){
			case Step.BASE:
				return "";
			case Step.ROUND:
				return "≈";
			case Step.SET:
				return "= " + value( s.after, sep );
			case Step.MUL:
				float f = s.factor();
				if (Float.isNaN( f ) || Float.isInfinite( f )) return "= " + value( s.after, sep );
				return factor( f, sep );
			default:
				float c = s.change();
				String v = value( c, sep );
				return c > 0f || (c == 0f) ? "+" + v : v;
		}
	}

	/** How a step reads for the hero: 1 good, -1 bad, 0 neither. Dealt: more is good; taken: less
	 *  is good. A rounding and the base are neither. */
	public static int mood( Step s, boolean dealt ){
		if (s.op == Step.BASE || s.op == Step.ROUND) return 0;
		float c = s.change();
		if (Math.abs( c ) <= HitRecord.EPS) return 0;
		boolean more = c > 0f;
		return more == dealt ? 1 : -1;
	}
}
