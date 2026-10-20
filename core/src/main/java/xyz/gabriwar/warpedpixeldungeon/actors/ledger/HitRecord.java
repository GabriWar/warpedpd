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

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * One hit as the ledger kept it: who struck whom, the roll to hit, every rule that made its number
 * (steps, from the BASE to what it showed), and what it took. A blow (Char.attack) is one record,
 * hit or miss; any other damage (a proc, a wand, a skill, a trap, a tick of poison) is its own,
 * tagged with the blow it came out of (parent). A run of damage-over-time ticks is one record
 * (ticks, tickAmounts): its steps are the latest tick's.
 *
 * The invariant (sums): the steps are one chain (each before is the last after) ending on the
 * number shown (the latest tick's for a run); shown = hpLost + shieldLost; a miss has a roll and no
 * steps; an outcome that took nothing shows 0.
 */
public final class HitRecord implements Bundlable {

	public static final int BLOW = 0, DIRECT = 1, DOT = 2;
	public static final int HIT = 0, MISS = 1, IMMUNE = 2, BARRIER = 3, BLEED = 4, STASIS = 5, NOTHING = 6;

	/** how close two values must be to count as the same (they are the game's own floats) */
	public static final float EPS = 0.001f;

	/** serial: the blow's (Blow.serial) for a blow, a negative number of the ledger's own for the
	 *  rest; parent: the blow it came out of, 0 for none */
	public int serial, parent, kind, outcome;
	public float turn;
	/** the latest tick's turn for a DOT run (turn is its first's) */
	public float lastTurn;
	public int depth;
	public Side attacker, defender;              //attacker null when what struck is not a creature
	public int sourceIcon = -1, sourceItem = -1; //a text cell (as the hit's number had) / an ItemSpriteSheet image
	public String sourceName;                    //what dealt it: a weapon, a wand, a skill, a poison...
	public String sourceClass;                   //its class (DOT runs fold by it)
	public int targetId;                         //the struck one's actor id (DOT runs fold by it)
	public Roll roll;                            //null when nothing rolled to hit
	public Step[] steps = {};
	public int dr = -1;                          //armour rolled, after the Ascension and pierce (-1: none)
	public int drRaw = -1;                       //the armour roll itself
	public float drPierce;                       //the share of it pierced
	public String drNote;                        //why it counted for nothing (a sniper's shot...), null
	public int request, shown, hpLost, shieldLost, overkill;
	public boolean crit, killed, executed;
	public int cardIcon = -1, cardColor;
	/** the number on the struck one's card: this blow with what it claimed (its procs' damage, kept
	 *  as their own records under it); -1 when no card had a number */
	public int cardValue = -1;
	public int[] chips = {}, chipTints = {};     //the struck one's card as it was seen
	public int ticks = 1;
	public int[] tickAmounts = {};               //a DOT run's ticks, oldest first (empty for one tick)
	public boolean dealt;                        //which list it is in

	/** The invariant (see the class comment). */
	public boolean sums(){
		if (hpLost + shieldLost != shown || overkill < 0) return false;
		if (outcome == MISS) return roll != null && steps.length == 0 && shown == 0;
		if (outcome != HIT && shown != 0) return false;
		if (ticks > 1){
			if (tickAmounts.length != ticks) return false;
			int sum = 0;
			for (int t : tickAmounts) sum += t;
			if (sum != shown) return false;
		}
		if (steps.length == 0) return shown == 0;
		if (steps[0].op != Step.BASE) return false;
		for (int i = 1; i < steps.length; i++){
			if (Math.abs( steps[i].before - steps[i - 1].after ) > EPS) return false;
		}
		int last = ticks > 1 ? tickAmounts[ticks - 1] : shown;
		return Math.abs( steps[steps.length - 1].after - last ) <= EPS;
	}

	/** When it last took anything: the latest tick of a run, else when it happened. */
	public float lastTick(){
		return ticks > 1 ? lastTurn : turn;
	}

	/** A field-by-field copy (the arrays and sides shared: a committed record's are never changed). */
	public HitRecord copy(){
		HitRecord m = new HitRecord();
		m.serial = serial; m.parent = parent; m.kind = kind; m.outcome = outcome;
		m.turn = turn; m.lastTurn = lastTurn; m.depth = depth;
		m.attacker = attacker; m.defender = defender;
		m.sourceIcon = sourceIcon; m.sourceItem = sourceItem; m.sourceName = sourceName; m.sourceClass = sourceClass;
		m.targetId = targetId; m.roll = roll; m.steps = steps;
		m.dr = dr; m.drRaw = drRaw; m.drPierce = drPierce; m.drNote = drNote;
		m.request = request; m.shown = shown; m.hpLost = hpLost; m.shieldLost = shieldLost; m.overkill = overkill;
		m.crit = crit; m.killed = killed; m.executed = executed;
		m.cardIcon = cardIcon; m.cardColor = cardColor; m.cardValue = cardValue; m.chips = chips; m.chipTints = chipTints;
		m.ticks = ticks; m.tickAmounts = tickAmounts; m.dealt = dealt;
		return m;
	}

	/** What it showed, every tick of a run summed. */
	public int total(){
		return shown;
	}

	/** How many steps no rule could be named for ("Other rules"): 0 in a fully known hit. */
	public int unnamed(){
		String other = DamageLedger.otherLabel();
		int n = 0;
		for (Step s : steps) if (other.equals( s.label )) n++;
		return n;
	}

	// ------------------------------------------------------------------ bundle
	//
	// Kept small (a full log is two hundred of these in the save): a chain of steps is one array of
	// strings, a step "op;side;icon;tint;before;after;label" (an empty field is the usual value: no
	// icon, no tint, before = the last step's after), a side's buffs one string each
	// "icon;tint;class;name", class names without the game's package, whole numbers without ".0",
	// and nothing written that is at its default.

	private static final String PKG = "xyz.gabriwar.warpedpixeldungeon.";

	static String pack( String cls ){
		if (cls == null) return "";
		return cls.startsWith( PKG ) ? "~" + cls.substring( PKG.length() ) : cls;
	}

	static String unpack( String s ){
		if (s == null || s.isEmpty()) return null;
		return s.startsWith( "~" ) ? PKG + s.substring( 1 ) : s;
	}

	//a float as its shortest exact text ("4" for 4.0)
	static String num( float f ){
		if (f == (int) f && Math.abs( f ) < 1e7f) return Integer.toString( (int) f );
		return Float.toString( f );
	}

	static float num( String s, float dflt ){
		if (s == null || s.isEmpty()) return dflt;
		try {
			return Float.parseFloat( s );
		} catch (NumberFormatException e){
			return dflt;
		}
	}

	private static int whole( String s, int dflt ){
		if (s == null || s.isEmpty()) return dflt;
		try {
			return Integer.parseInt( s );
		} catch (NumberFormatException e){
			return dflt;
		}
	}

	private static String opt( Bundle b, String key ){
		if (!b.contains( key )) return null;
		String s = b.getString( key );
		return s.isEmpty() ? null : s;
	}

	private static int[] ints( Bundle b, String key ){
		if (!b.contains( key )) return new int[0];
		int[] a = b.getIntArray( key );
		return a == null ? new int[0] : a;
	}

	private static String[] strings( Bundle b, String key ){
		if (!b.contains( key )) return new String[0];
		String[] a = b.getStringArray( key );
		return a == null ? new String[0] : a;
	}

	private static int getInt( Bundle b, String key, int dflt ){
		return b.contains( key ) ? b.getInt( key ) : dflt;
	}

	private static void putInt( Bundle b, String key, int v, int dflt ){
		if (v != dflt) b.put( key, v );
	}

	private static void putTrue( Bundle b, String key, boolean v ){
		if (v) b.put( key, true );
	}

	static void putSteps( Bundle b, String key, Step[] steps ){
		if (steps.length == 0) return;
		String[] out = new String[steps.length];
		for (int i = 0; i < steps.length; i++){
			Step s = steps[i];
			boolean chained = i > 0 && s.before == steps[i - 1].after;
			out[i] = s.op + ";" + s.side + ";" + (s.icon == Step.NO_ICON ? "" : Integer.toString( s.icon )) + ";"
					+ (s.tint == 0 ? "" : Integer.toString( s.tint )) + ";" + (chained ? "" : num( s.before )) + ";"
					+ num( s.after ) + ";" + (s.label == null ? "" : s.label);
		}
		b.put( key, out );
	}

	static Step[] getSteps( Bundle b, String key ){
		String[] in = strings( b, key );
		ArrayList<Step> out = new ArrayList<>( in.length );
		for (String line : in){
			String[] f = line.split( ";", 7 );
			if (f.length < 7) continue;
			Step s = new Step();
			s.op = whole( f[0], Step.ADD );
			s.side = whole( f[1], Step.WORLD );
			s.icon = whole( f[2], Step.NO_ICON );
			s.tint = whole( f[3], 0 );
			s.after = num( f[5], 0f );
			s.before = f[4].isEmpty() && !out.isEmpty() ? out.get( out.size() - 1 ).after : num( f[4], s.after );
			s.label = f[6];
			out.add( s );
		}
		return out.toArray( new Step[0] );
	}

	static void putSide( Bundle b, String p, Side s ){
		if (s == null) return;
		b.put( p + "n", s.name == null ? "" : s.name );
		if (s.cls != null) b.put( p + "c", pack( s.cls ) );
		putTrue( b, p + "h", s.hero );
		putInt( b, p + "hc", s.heroClass, -1 );
		putInt( b, p + "ht", s.heroTier, 0 );
		b.put( p + "hp", s.hp );
		b.put( p + "mx", s.ht );
		putInt( b, p + "sh", s.shield, 0 );
		if (s.buffIcons.length > 0){
			String[] buffs = new String[s.buffIcons.length];
			for (int i = 0; i < buffs.length; i++){
				buffs[i] = s.buffIcons[i] + ";" + (s.buffTints[i] == 0 ? "" : Integer.toString( s.buffTints[i] )) + ";"
						+ pack( s.buffClasses[i] ) + ";" + (s.buffNames[i] == null ? "" : s.buffNames[i]);
			}
			b.put( p + "b", buffs );
		}
		if (s.weapon != null) b.put( p + "w", s.weapon );
		if (s.armor != null) b.put( p + "ar", s.armor );
		if (s.weaponRarity != null) b.put( p + "wr", s.weaponRarity );
		if (s.armorRarity != null) b.put( p + "rr", s.armorRarity );
		putInt( b, p + "wc", s.weaponColor, 0 );
		putInt( b, p + "rc", s.armorColor, 0 );
	}

	static Side getSide( Bundle b, String p ){
		if (!b.contains( p + "n" )) return null;
		Side s = new Side();
		s.name = b.getString( p + "n" );
		s.cls = unpack( opt( b, p + "c" ) );
		s.hero = b.getBoolean( p + "h" );
		s.heroClass = getInt( b, p + "hc", -1 );
		s.heroTier = getInt( b, p + "ht", 0 );
		s.hp = b.getInt( p + "hp" );
		s.ht = b.getInt( p + "mx" );
		s.shield = getInt( b, p + "sh", 0 );
		String[] buffs = strings( b, p + "b" );
		ArrayList<String[]> ok = new ArrayList<>( buffs.length );
		for (String line : buffs){
			String[] f = line.split( ";", 4 );
			if (f.length == 4) ok.add( f );
		}
		int n = ok.size();
		s.buffIcons = new int[n];
		s.buffTints = new int[n];
		s.buffClasses = new String[n];
		s.buffNames = new String[n];
		for (int i = 0; i < n; i++){
			String[] f = ok.get( i );
			s.buffIcons[i] = whole( f[0], 0 );
			s.buffTints[i] = whole( f[1], 0 );
			s.buffClasses[i] = unpack( f[2] );
			s.buffNames[i] = f[3];
		}
		s.weapon = opt( b, p + "w" );
		s.armor = opt( b, p + "ar" );
		s.weaponRarity = opt( b, p + "wr" );
		s.armorRarity = opt( b, p + "rr" );
		s.weaponColor = getInt( b, p + "wc", 0 );
		s.armorColor = getInt( b, p + "rc", 0 );
		return s;
	}

	static void putRoll( Bundle b, String p, Roll r ){
		if (r == null) return;
		b.put( p + "s", num( r.accStat ) + ";" + num( r.defStat ) + ";" + num( r.accRoll ) + ";" + num( r.defRoll )
				+ ";" + num( r.accMul ) + ";" + num( r.defMul ) );
		putTrue( b, p + "h", r.hit );
		putTrue( b, p + "su", r.sure );
		putTrue( b, p + "un", r.untouchable );
		putInt( b, p + "ri", r.reasonIcon, -1 );
		putSteps( b, p + "A", r.accSteps );
		putSteps( b, p + "D", r.defSteps );
		putSteps( b, p + "X", r.accRollSteps );
		putSteps( b, p + "Y", r.defRollSteps );
	}

	static Roll getRoll( Bundle b, String p ){
		String stats = opt( b, p + "s" );
		if (stats == null) return null;
		String[] f = stats.split( ";", -1 );
		Roll r = new Roll();
		r.accStat = num( f.length > 0 ? f[0] : null, 0f );
		r.defStat = num( f.length > 1 ? f[1] : null, 0f );
		r.accRoll = num( f.length > 2 ? f[2] : null, 0f );
		r.defRoll = num( f.length > 3 ? f[3] : null, 0f );
		r.accMul = num( f.length > 4 ? f[4] : null, 1f );
		r.defMul = num( f.length > 5 ? f[5] : null, 1f );
		r.hit = b.getBoolean( p + "h" );
		r.sure = b.getBoolean( p + "su" );
		r.untouchable = b.getBoolean( p + "un" );
		r.reasonIcon = getInt( b, p + "ri", -1 );
		r.accSteps = getSteps( b, p + "A" );
		r.defSteps = getSteps( b, p + "D" );
		r.accRollSteps = getSteps( b, p + "X" );
		r.defRollSteps = getSteps( b, p + "Y" );
		return r;
	}

	@Override
	public void storeInBundle( Bundle b ){
		b.put( "se", serial );
		putInt( b, "pa", parent, 0 );
		putInt( b, "k", kind, 0 );
		putInt( b, "o", outcome, 0 );
		b.put( "tu", num( turn ) );
		if (ticks > 1) b.put( "lt", num( lastTurn ) );
		b.put( "dp", depth );
		putSide( b, "a.", attacker );
		putSide( b, "d.", defender );
		putInt( b, "si", sourceIcon, -1 );
		putInt( b, "sit", sourceItem, -1 );
		if (sourceName != null) b.put( "sn", sourceName );
		if (sourceClass != null) b.put( "sc", pack( sourceClass ) );
		b.put( "ti", targetId );
		putRoll( b, "r.", roll );
		putSteps( b, "s", steps );
		putInt( b, "dr", dr, -1 );
		putInt( b, "drr", drRaw, -1 );
		if (drPierce != 0f) b.put( "drp", num( drPierce ) );
		if (drNote != null) b.put( "drn", drNote );
		putInt( b, "rq", request, 0 );
		putInt( b, "sw", shown, 0 );
		putInt( b, "hl", hpLost, 0 );
		putInt( b, "sl", shieldLost, 0 );
		putInt( b, "ok", overkill, 0 );
		putTrue( b, "cr", crit );
		putTrue( b, "ki", killed );
		putTrue( b, "ex", executed );
		putInt( b, "ci", cardIcon, -1 );
		putInt( b, "cc", cardColor, 0 );
		putInt( b, "cv", cardValue, -1 );
		if (chips.length > 0){
			b.put( "ch", chips );
			b.put( "ct", chipTints );
		}
		if (ticks > 1){
			b.put( "tk", ticks );
			b.put( "ta", tickAmounts );
		}
	}

	@Override
	public void restoreFromBundle( Bundle b ){
		serial = b.getInt( "se" );
		parent = getInt( b, "pa", 0 );
		kind = getInt( b, "k", 0 );
		outcome = getInt( b, "o", 0 );
		turn = num( opt( b, "tu" ), 0f );
		lastTurn = num( opt( b, "lt" ), turn );
		depth = b.getInt( "dp" );
		attacker = getSide( b, "a." );
		defender = getSide( b, "d." );
		sourceIcon = getInt( b, "si", -1 );
		sourceItem = getInt( b, "sit", -1 );
		sourceName = opt( b, "sn" );
		sourceClass = unpack( opt( b, "sc" ) );
		targetId = b.getInt( "ti" );
		roll = getRoll( b, "r." );
		steps = getSteps( b, "s" );
		dr = getInt( b, "dr", -1 );
		drRaw = getInt( b, "drr", -1 );
		drPierce = num( opt( b, "drp" ), 0f );
		drNote = opt( b, "drn" );
		request = getInt( b, "rq", 0 );
		shown = getInt( b, "sw", 0 );
		hpLost = getInt( b, "hl", 0 );
		shieldLost = getInt( b, "sl", 0 );
		overkill = getInt( b, "ok", 0 );
		crit = b.getBoolean( "cr" );
		killed = b.getBoolean( "ki" );
		executed = b.getBoolean( "ex" );
		cardIcon = getInt( b, "ci", -1 );
		cardColor = getInt( b, "cc", 0 );
		cardValue = getInt( b, "cv", -1 );
		chips = ints( b, "ch" );
		chipTints = ints( b, "ct" );
		if (chipTints.length != chips.length) chipTints = java.util.Arrays.copyOf( chipTints, chips.length );
		ticks = Math.max( 1, getInt( b, "tk", 1 ) );
		tickAmounts = ints( b, "ta" );
		if (ticks > 1 && tickAmounts.length != ticks) ticks = 1;
	}
}
