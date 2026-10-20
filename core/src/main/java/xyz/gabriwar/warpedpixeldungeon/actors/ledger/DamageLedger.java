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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Electricity;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.StormCloud;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AscensionChallenge;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cocoshield;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Corrosion;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Corruption;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.LifeLink;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Ooze;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Preparation;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.RoseBarrier;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.ArmorAbility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.ClericSpell;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CrystalSpire;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DwarfKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollGeomancer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Necromancer;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.HitCard;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.AntiMagic;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Viscosity;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.DriedRose;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.items.quest.Pickaxe;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.GearPerk;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.RarityLine;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.WeaponPerk;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.homestead.gathering.TreeFall;
import xyz.gabriwar.warpedpixeldungeon.levels.features.Chasm;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.CaveSites;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GeyserTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GnollRockfallTrap;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.DeviceCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * The damage ledger: every hit the hero deals or takes, kept as the record of how its number came
 * to be (HitRecord), the last DamageLog.KEEP of each in Dungeon.hero.damageLog.
 *
 * A per-thread stack of frames, as Blow's: combat runs on the actor thread and on the render
 * thread (a blow the hero sees closes in its sprite's callback). Char.attack opens a blow frame
 * (openBlow / closeBlow, which commits it before Blow.end so the presenter finds it for its card);
 * Hero.damage and Char.damage open a damage frame (enterDamage / exitDamage), which a blow adopts
 * as its main damage (expectMain) and Hero.damage then Char.damage share. Every rule that changes
 * the number on the way is a step: the game's code hands the value before and after it (mul, add,
 * to, ...), so nothing is computed twice. A call whose insides are not all named is bracketed
 * (push / pop): whatever it changed that no inner step names becomes one step named after it
 * ("Sword's effect", "Golem's defense"), so a record's steps always run from its base to the
 * number it showed (HitRecord.sums). The roll to hit is gathered the same way in Char.hit.
 *
 * Rules every hook keeps: cheap (one thread-local read when nothing is open), never a draw of the
 * game's Random, never a getter that changes the game (attackSkill, drRoll, damageRoll...: the
 * values are handed in), and never an exception into combat (a failure drops the frame and is
 * logged once). Only the hero's own damage is dealt (his blows and whatever comes out of them,
 * his skills, wands, spells, abilities and bombs), every damage on him is taken; nothing is kept
 * on a co-op guest (the host keeps the list).
 */
public final class DamageLedger {

	private DamageLedger(){}

	/** Tests only: false turns every hook into nothing (the determinism check). */
	static boolean recording = true;

	/** an op for a bracket's remainder: MUL when the values are not whole, SET to 0, ADD else */
	public static final int AUTO = -1;

	// ------------------------------------------------------------------ labels

	/** A label resolved only when a step is recorded: a key under actors.ledger.damageledger,
	 *  with its arguments (anything a label can be: a creature, an item, a buff...). */
	public static class Label {
		final String key;
		final Object[] args;
		final int icon;

		Label( String key, int icon, Object... args ){
			this.key = key;
			this.icon = icon;
			this.args = args;
		}

		/** The same label with these arguments. */
		public Label of( Object... args ){
			return new Label( key, icon, args );
		}

		String text(){
			Object[] a = new Object[args.length];
			for (int i = 0; i < a.length; i++){
				Object o = args[i];
				a[i] = o instanceof Number ? o : DamageLedger.text( o );
			}
			return Messages.get( DamageLedger.class, key, a );
		}

		int icon(){
			return icon;
		}
	}

	private static Label key( String k ){
		return new Label( k, Step.NO_ICON );
	}

	private static Label key( String k, int icon ){
		return new Label( k, icon );
	}

	public static final Label BASE        = key( "base" );
	public static final Label ROLL_OF     = key( "mob_roll" );          //%s's roll
	public static final Label RANGE       = key( "weapon_range" );      //%1$s roll, %2$d to %3$d
	public static final Label BEST_OF     = key( "best_of" );           //%1$s, best of %2$d rolls
	public static final Label MULTIPLIER  = key( "multiplier" );
	public static final Label BONUS       = key( "bonus" );
	public static final Label ABILITY     = key( "ability" );           //%s ability
	public static final Label ARMOR       = key( "armor", 37 );         //the armour cell (FloatingText.HIT_ARM)
	public static final Label ROUNDED     = key( "rounded" );
	public static final Label UNDEAD_NIGHT = key( "undead_night" );
	public static final Label BLOOD_MOON  = key( "blood_moon" );
	public static final Label POWER       = key( "power" );
	public static final Label OWN_RULES   = key( "own_rules" );         //%s's own rules
	public static final Label DEFENSE     = key( "defense" );           //%s's defense
	public static final Label ATTACK_EFFECT = key( "attack_effect" );   //%s's attack
	public static final Label WEAPON_EFFECT = key( "weapon_effect" );   //%s's effect
	public static final Label RESIST      = key( "resist" );
	public static final Label IMMUNE      = key( "immune" );
	public static final Label MAGIC_ARMOR = key( "magic_armor" );
	public static final Label HEALTH_HOLD = key( "health_hold" );
	public static final Label EXECUTE     = key( "execute", 32 );       //FloatingText.EXECUTE
	public static final Label GRIM        = key( "grim", 32 );
	public static final Label OTHER       = key( "other" );
	public static final Label SOMETHING   = key( "something" );
	public static final Label ACCURACY    = key( "accuracy" );          //%s's accuracy
	public static final Label EVASION     = key( "evasion" );           //%s's evasion
	public static final Label RAW_ROLL    = key( "raw_roll" );
	public static final Label SURPRISE    = key( "surprise" );
	public static final Label AIM         = key( "aim" );
	public static final Label SKILLS      = key( "skills" );
	public static final Label SKILL_LEVELS = key( "skill_levels" );
	public static final Label GEAR        = key( "gear" );
	public static final Label GEAR_PROTECTION = key( "gear_protection" );
	public static final Label CRIT        = key( "crit", 4 );           //FloatingText.CRIT
	public static final Label EDGE        = key( "edge" );              //%s
	public static final Label NO_NEGATIVE = key( "no_negative" );
	public static final Label SKILL_MARKS = key( "skill_marks" );
	public static final Label HERO_POWER  = key( "hero_power" );
	public static final Label BARRIER     = key( "barrier" );
	public static final Label BLEED       = key( "bleed", 10 );         //%d (FloatingText.BLEEDING)
	public static final Label STASIS      = key( "stasis" );
	public static final Label NO_DAMAGE   = key( "no_damage" );
	public static final Label AUGMENT     = key( "augment" );
	public static final Label EXCESS_STR  = key( "excess_str" );
	public static final Label WIND        = key( "wind" );
	public static final Label UNARMED     = key( "unarmed" );
	/** the season (summer or winter, as the factor is) */
	public static final Label SEASON = new Label( "summer", Step.NO_ICON ){
		@Override
		String text(){
			return Messages.get( DamageLedger.class, GameCalendar.seasonDamageMultiplier() < 1f ? "winter" : "summer" );
		}
	};
	/** the day of the week, by its own name */
	public static final Label WEEKDAY = new Label( "weekday", Step.NO_ICON ){
		@Override
		String text(){
			return Messages.capitalize( GameCalendar.weekdayString() );
		}
	};

	/** The localised "Other rules": a step no rule could be named for. */
	public static String otherLabel(){
		return OTHER.text();
	}

	private static boolean known( String s ){
		return s != null && !s.isEmpty() && !s.equals( Messages.NO_TEXT_FOUND );
	}

	/** What a label source reads as, localised now. */
	static String text( Object what ){
		try {
			if (what == null) return SOMETHING.text();
			if (what instanceof String) return (String) what;
			if (what instanceof Label) return ((Label) what).text();
			String s = null;
			if (what instanceof Char) s = Messages.titleCase( ((Char) what).name() );
			else if (what instanceof Skill) s = ((Skill) what).name();
			else if (what instanceof Talent) s = ((Talent) what).title();
			else if (what instanceof Buff) s = Messages.titleCase( ((Buff) what).name() );
			else if (what instanceof Weapon.Enchantment) s = Messages.titleCase( ((Weapon.Enchantment) what).name() );
			else if (what instanceof Armor.Glyph) s = Messages.titleCase( ((Armor.Glyph) what).name() );
			else if (what instanceof Item) s = Messages.titleCase( ((Item) what).name() );
			else if (what instanceof WeaponPerk) s = ((WeaponPerk) what).title();
			else if (what instanceof GearPerk) s = ((GearPerk) what).title();
			else if (what instanceof RarityLine) s = Messages.capitalize( ((RarityLine) what).shortName() );
			if (known( s )) return s;
			return classText( what instanceof Class ? (Class<?>) what : what.getClass() );
		} catch (RuntimeException e){
			return "?";
		}
	}

	//the world's own blows, which have no name of their own: the key of their name here, and of
	//the line that explains them ("<key>_desc")
	private static final HashMap<Class<?>, String> WORLD = new HashMap<>();
	static {
		WORLD.put( Chasm.class, "fall" );
		WORLD.put( Heap.class, "haunted_remains" );
		WORLD.put( TreeFall.class, "tree_fall" );
		WORLD.put( CaveSites.class, "rift_flare" );
	}

	private static String worldKey( String sourceClass ){
		if (sourceClass == null) return null;
		try {
			return WORLD.get( Class.forName( sourceClass ) );
		} catch (ClassNotFoundException e){
			return null;
		}
	}

	/** What a record's source is called: a world source by its own name, even in records kept before it had one. */
	public static String sourceName( HitRecord r ){
		String k = worldKey( r.sourceClass );
		if (k != null) return Messages.get( DamageLedger.class, k );
		return r.sourceName;
	}

	/** The line that explains a world source (a fall, a falling tree...), null for anything else. */
	public static String sourceDesc( HitRecord r ){
		String k = worldKey( r.sourceClass );
		return k != null ? Messages.get( DamageLedger.class, k + "_desc" ) : null;
	}

	//a class's "name", or its enclosing class's (Warlock.DarkBolt reads as the warlock)
	private static String classText( Class<?> c ){
		for (Class<?> k = c; k != null; k = k.getEnclosingClass()){
			if (WORLD.containsKey( k )) return Messages.get( DamageLedger.class, WORLD.get( k ) );
			String s = Messages.get( k, "name" );
			if (known( s )) return Messages.titleCase( s );
		}
		return SOMETHING.text();
	}

	/** The chip code a label source shows with (plan's chip namespace), NO_ICON for none. */
	static int iconOf( Object what ){
		if (what instanceof Label) return ((Label) what).icon();
		if (what instanceof Buff){
			int i = ((Buff) what).icon();
			return i < 0 || i == BuffIndicator.NONE ? Step.NO_ICON : -(i + 1);
		}
		if (what instanceof Skill) return SKILL_BASE + ((Skill) what).image();
		return Step.NO_ICON;
	}

	/** Skill icons in the chip namespace: SKILL_BASE + the skill's image. */
	public static final int SKILL_BASE = HitCard.SKILL_BASE;

	static int tintOf( Object what ){
		if (what instanceof Buff && iconOf( what ) != Step.NO_ICON){
			try {
				return Blow.tintOf( (Buff) what );
			} catch (RuntimeException e){
				return 0;
			}
		}
		return 0;
	}

	// ------------------------------------------------------------------ the frames

	private static final class Build {
		final HitRecord rec = new HitRecord();
		final ArrayList<Step> steps = new ArrayList<>();
		float running;
		boolean started, landed;
		int outcome = -1;
		int hpBefore = Integer.MIN_VALUE, shieldBefore = -1, hpLost, shieldLost;
	}

	private static final class Frame {
		static final int BLOW = 0, DAMAGE = 1;
		int type, gen;
		boolean on;
		Build b;
		Char attacker, target;
		Object src;
		int serial;
		//damage frames: how many layers enter it (Hero.damage then Char.damage), how many did
		int layers, entered;
		boolean main;
		//blow frames
		boolean expect, mainEntered, baseScope;
		Roll roll;
		ArrayList<Build> scratches;
		Build scratch;

		void reset(){
			type = 0;
			on = false;
			b = null;
			attacker = target = null;
			src = null;
			serial = 0;
			layers = entered = 0;
			main = expect = mainEntered = baseScope = false;
			roll = null;
			scratches = null;
			scratch = null;
		}
	}

	private static final class Ctx {
		Frame f;
		int gen, side, op;
		Object what;
	}

	private static final class Chain {
		final ArrayList<Step> steps = new ArrayList<>();
		float running;
		boolean started;
		//a stat composed when it is read: base, adds, then factors, transforms and sets in order
		Object baseWhat;
		float base;
		boolean hasBase;
		final ArrayList<Object[]> parts = new ArrayList<>();
		float mul = 1f;
	}

	private static final class RollScope {
		Char a, d;
		int phase;          //0 the attacker's accuracy is read, 1 the defender's evasion, 2 the stats' own rules, 3 the rolls
		final Chain acc = new Chain(), def = new Chain(), accRoll = new Chain(), defRoll = new Chain();
	}

	private static final class Pending {
		Object src;
		final ArrayList<Step> steps = new ArrayList<>();
		float value;
		float at;
	}

	private static final class State {
		Frame[] frames = new Frame[8];
		int depth;
		Ctx[] ctx = new Ctx[16];
		int cdepth;
		RollScope roll;
		Pending pending;

		Frame push( int type ){
			if (depth == frames.length) frames = Arrays.copyOf( frames, depth * 2 );
			Frame f = frames[depth];
			if (f == null) f = frames[depth] = new Frame();
			int g = f.gen + 1;
			f.reset();
			f.gen = g;
			f.type = type;
			depth++;
			return f;
		}

		Frame top(){
			return depth > 0 ? frames[depth - 1] : null;
		}

		Frame blow(){
			for (int i = depth - 1; i >= 0; i--) if (frames[i].type == Frame.BLOW) return frames[i];
			return null;
		}
	}

	private static final ThreadLocal<State> STATE = ThreadLocal.withInitial( State::new );
	private static int serials = 0;
	private static boolean logged = false;

	/** Whether the ledger keeps anything now: off on a co-op guest, with no hero, or for tests. */
	static boolean enabled(){
		return recording && Dungeon.hero != null && !NetManager.isNetClient();
	}

	private static synchronized int nextSerial(){
		return -(++serials);
	}

	private static void fail( Throwable t ){
		try {
			State s = STATE.get();
			Frame f = s.top();
			if (f != null) f.on = false;
		} catch (Throwable ignored){ }
		if (logged) return;
		logged = true;
		try {
			DeviceCompat.log( "DamageLedger", "dropped a record: " + t );
		} catch (Throwable ignored){ }
	}

	private static void complain( String what ){
		if (logged) return;
		logged = true;
		try {
			DeviceCompat.log( "DamageLedger", what );
		} catch (Throwable ignored){ }
	}

	//the frame a step about this character goes to: the top one, when it is about it and kept
	private static Frame frameFor( State s, Char subject ){
		Frame f = s.top();
		return f != null && f.on && f.b != null && f.target == subject ? f : null;
	}

	// ------------------------------------------------------------------ steps

	private static int autoOp( float before, float after ){
		if (after == 0f && before != 0f) return Step.SET;
		boolean whole = before == Math.rint( before ) && after == Math.rint( after );
		return before != 0f && !whole ? Step.MUL : Step.ADD;
	}

	private static void append( ArrayList<Step> into, int op, int side, Object what, float before, float after ){
		into.add( new Step( op == AUTO ? autoOp( before, after ) : op, side, text( what ), iconOf( what ), tintOf( what ), before, after ) );
	}

	//the innermost bracket open on this frame, null for none
	private static Ctx bracket( State s, Frame f ){
		for (int i = s.cdepth - 1; i >= 0; i--){
			Ctx c = s.ctx[i];
			if (c.f == f && c.gen == f.gen) return c;
		}
		return null;
	}

	//bring a frame's running value to `to`: a base when it has none, else one step for whatever
	//changed it that no step named (its innermost bracket's name, or Other rules)
	private static void reconcile( State s, Frame f, Build b, float to, Object fallback ){
		if (!b.started){
			b.steps.add( new Step( Step.BASE, Step.WORLD, text( fallback == OTHER ? BASE : fallback ), iconOf( fallback ), tintOf( fallback ), to, to ) );
			b.running = to;
			b.started = true;
			return;
		}
		if (Math.abs( b.running - to ) <= HitRecord.EPS) return;
		Ctx c = s != null && f != null ? bracket( s, f ) : null;
		if (c != null) append( b.steps, c.op, c.side, c.what, b.running, to );
		else append( b.steps, AUTO, Step.WORLD, fallback, b.running, to );
		b.running = to;
	}

	private static void stepIn( State s, Frame f, int side, int op, Object what, float before, float after ){
		Build b = f.b;
		if (b.landed) return;
		if (!b.started){
			b.steps.add( new Step( Step.BASE, Step.WORLD, text( BASE ), Step.NO_ICON, 0, before, before ) );
			b.running = before;
			b.started = true;
		} else {
			reconcile( s, f, b, before, OTHER );
		}
		if (Math.abs( after - before ) <= HitRecord.EPS) return;
		append( b.steps, op, side, what, before, after );
		b.running = after;
	}

	/** One rule on the damage about to hit `subject`: the value before it and after it. Nothing
	 *  when no kept frame is about that character, or the rule changed nothing. */
	public static void step( Char subject, int side, int op, Object what, float before, float after ){
		if (!recording) return;
		try {
			State s = STATE.get();
			Frame f = frameFor( s, subject );
			if (f != null) stepIn( s, f, side, op, what, before, after );
		} catch (Throwable t){
			fail( t );
		}
	}

	/** value * factor, recorded as a MUL step. */
	public static float mul( Char subject, int side, Object what, float value, float factor ){
		float after = value * factor;
		step( subject, side, Step.MUL, what, value, after );
		return after;
	}

	/** value + amount, recorded as an ADD step. */
	public static float add( Char subject, int side, Object what, float value, float amount ){
		float after = value + amount;
		step( subject, side, Step.ADD, what, value, after );
		return after;
	}

	/** A rule the game computed itself: recorded from before to after, returns after. */
	public static float to( Char subject, int side, int op, Object what, float before, float after ){
		step( subject, side, op, what, before, after );
		return after;
	}

	public static int to( Char subject, int side, int op, Object what, int before, int after ){
		step( subject, side, op, what, before, after );
		return after;
	}

	/** Math.round(value), recorded as a ROUND step. */
	public static int round( Char subject, float value ){
		int after = Math.round( value );
		step( subject, Step.WORLD, Step.ROUND, ROUNDED, value, after );
		return after;
	}

	/** A multiplier the game rounded (or cut, or ceiled) to a whole number: before * factor as a
	 *  MUL, then the rounding to `after` it really took. Returns after. */
	public static int mulRound( Char subject, int side, Object what, int before, float factor, int after ){
		if (!recording) return after;
		float exact = before * factor;
		step( subject, side, Step.MUL, what, before, exact );
		step( subject, Step.WORLD, Step.ROUND, ROUNDED, exact, after );
		return after;
	}

	public static float mulRound( Char subject, int side, Object what, float before, float factor, float after ){
		if (!recording) return after;
		float exact = before * factor;
		step( subject, side, Step.MUL, what, before, exact );
		step( subject, Step.WORLD, Step.ROUND, ROUNDED, exact, after );
		return after;
	}

	/** The label of Char.attack's damage multiplier: a weapon ability's, else the plain one. */
	public static Object multiplierLabel( Char attacker ){
		KindOfWeapon w = attacker instanceof Hero ? ((Hero) attacker).belongings.abilityWeapon : null;
		return w != null ? ABILITY.of( w ) : MULTIPLIER;
	}

	/** The label of Char.attack's flat bonus: a weapon ability's, else the plain one. */
	public static Object bonusLabel( Char attacker ){
		KindOfWeapon w = attacker instanceof Hero ? ((Hero) attacker).belongings.abilityWeapon : null;
		return w != null ? ABILITY.of( w ) : BONUS;
	}

	/** Open a bracket over a call whose insides are not all named: what it changes and no inner
	 *  step names becomes one step named `what`. Always paired with pop. */
	public static void push( Char subject, int side, int op, Object what ){
		try {
			State s = STATE.get();
			if (s.cdepth == s.ctx.length){
				if (s.cdepth >= 256){
					//a bracket left open by an exception, many times over: start again
					s.cdepth = 0;
					complain( "brackets left open" );
				} else {
					s.ctx = Arrays.copyOf( s.ctx, s.cdepth * 2 );
				}
			}
			Ctx c = s.ctx[s.cdepth];
			if (c == null) c = s.ctx[s.cdepth] = new Ctx();
			s.cdepth++;
			Frame f = recording ? frameFor( s, subject ) : null;
			c.f = f;
			c.gen = f != null ? f.gen : 0;
			c.side = side;
			c.op = op;
			c.what = what;
		} catch (Throwable t){
			fail( t );
		}
	}

	/** Close the innermost bracket with the value its call returned (returned as is). */
	public static int pop( int result ){
		try {
			State s = STATE.get();
			if (s.cdepth == 0) return result;
			Ctx c = s.ctx[--s.cdepth];
			Frame f = c.f;
			Object what = c.what;
			c.f = null;
			c.what = null;
			if (f != null && c.gen == f.gen && f.on && f.b != null && !f.b.landed && f.b.started){
				Build b = f.b;
				if (Math.abs( b.running - result ) > HitRecord.EPS) append( b.steps, c.op, c.side, what, b.running, result );
				b.running = result;
			}
		} catch (Throwable t){
			fail( t );
		}
		return result;
	}

	public static float pop( float result ){
		try {
			State s = STATE.get();
			if (s.cdepth == 0) return result;
			Ctx c = s.ctx[--s.cdepth];
			Frame f = c.f;
			Object what = c.what;
			c.f = null;
			c.what = null;
			if (f != null && c.gen == f.gen && f.on && f.b != null && !f.b.landed && f.b.started){
				Build b = f.b;
				if (Math.abs( b.running - result ) > HitRecord.EPS) append( b.steps, c.op, c.side, what, b.running, result );
				b.running = result;
			}
		} catch (Throwable t){
			fail( t );
		}
		return result;
	}

	// ------------------------------------------------------------------ the blow

	/** Char.attack, right after Blow.begin. */
	public static void openBlow( Char a, Char d, float dmgMulti, float dmgBonus, float accMulti ){
		try {
			State s = STATE.get();
			Frame f = s.push( Frame.BLOW );
			f.attacker = a;
			f.target = d;
			f.serial = Blow.serial();
			if (!enabled() || a == null || d == null) return;
			Hero hero = Dungeon.hero;
			boolean dealt = a == hero && d != hero, taken = d == hero;
			if (!dealt && !taken) return;
			if (a == hero && Dungeon.debugOneHitKill) return;
			Build b = f.b = new Build();
			HitRecord r = b.rec;
			r.kind = HitRecord.BLOW;
			r.serial = f.serial != 0 ? f.serial : nextSerial();
			r.parent = parentOf( s, f );
			r.dealt = dealt;
			stamp( r, d );
			r.attacker = side( a );
			r.defender = side( d );
			r.sourceIcon = FloatingText.PHYS_DMG;
			KindOfWeapon w = a instanceof Hero ? ((Hero) a).belongings.attackingWeapon() : null;
			if (w != null){
				r.sourceName = itemName( w );
				r.sourceItem = w.image();
				r.sourceClass = w.getClass().getName();
			} else {
				r.sourceName = text( a );
				r.sourceClass = a.getClass().getName();
			}
			f.on = true;
		} catch (Throwable t){
			fail( t );
		}
	}

	//the blow a frame came out of: the nearest blow frame under it
	private static int parentOf( State s, Frame f ){
		for (int i = s.depth - 1; i >= 0; i--){
			Frame g = s.frames[i];
			if (g != f && g.type == Frame.BLOW) return g.serial;
		}
		return 0;
	}

	private static void stamp( HitRecord r, Char target ){
		r.turn = Actor.now();
		r.depth = Dungeon.depth;
		r.targetId = target.id();
	}

	/** Char.attack's finally, before Blow.end: the blow is committed (a miss, an immune target, a
	 *  blow stopped short, or its damage with all it took). */
	public static void closeBlow(){
		try {
			State s = STATE.get();
			Frame f = null;
			while (s.depth > 0){
				Frame g = s.frames[--s.depth];
				if (g.type == Frame.BLOW){
					f = g;
					break;
				}
			}
			if (f == null || !f.on || f.b == null) return;
			f.on = false;
			Build b = f.b;
			HitRecord r = b.rec;
			r.roll = f.roll;
			Char d = f.target;
			if (f.roll != null && !f.roll.hit){
				r.outcome = HitRecord.MISS;
				b.steps.clear();
				b.hpLost = b.shieldLost = 0;
			} else if (b.landed){
				r.outcome = HitRecord.HIT;
			} else if (b.outcome >= 0){
				r.outcome = b.outcome;
			} else if (f.roll == null && !b.started){
				r.outcome = f.attacker != null && d.isInvulnerable( f.attacker.getClass() ) ? HitRecord.IMMUNE : HitRecord.NOTHING;
			} else {
				r.outcome = HitRecord.NOTHING;
				if (b.started && Math.abs( b.running ) > HitRecord.EPS){
					append( b.steps, Step.SET, Step.DEFENDER,
							f.expect && !f.mainEntered && r.request >= 0 ? OWN_RULES.of( d ) : NO_DAMAGE, b.running, 0f );
					b.running = 0f;
				}
			}
			r.killed = !d.isAlive();
			commit( b );
		} catch (Throwable t){
			fail( t );
		}
	}

	/** Char.attack, just before the struck one takes the blow's damage. */
	public static void expectMain( Char target, int request ){
		try {
			State s = STATE.get();
			Frame f = s.top();
			if (f == null || f.type != Frame.BLOW || f.target != target) return;
			f.expect = true;
			if (f.on && f.b != null){
				f.b.rec.request = request;
				//an execute in the attack (Silent Death) already ended the chain on what it took
				if (!f.b.landed) reconcile( s, f, f.b, request, OTHER );
			}
		} catch (Throwable t){
			fail( t );
		}
	}

	/** Char.attack's armour: the roll, the share pierced, what it came to. */
	public static void armor( Char target, int raw, float pierce, int dr ){
		try {
			Frame f = frameFor( STATE.get(), target );
			if (f == null || f.type != Frame.BLOW) return;
			f.b.rec.drRaw = raw;
			f.b.rec.drPierce = pierce;
			f.b.rec.dr = dr;
		} catch (Throwable t){
			fail( t );
		}
	}

	/** Char.attack's armour counted for nothing, and why. */
	public static void armorIgnored( Char target, Object why ){
		try {
			Frame f = frameFor( STATE.get(), target );
			if (f == null || f.type != Frame.BLOW) return;
			if (f.b.rec.drNote == null) f.b.rec.drNote = text( why );
			f.b.rec.dr = 0;
		} catch (Throwable t){
			fail( t );
		}
	}

	/** A crit landed on this blow (WeaponPerk.crit, True Edge). */
	public static void crit( Char target ){
		try {
			State s = STATE.get();
			Frame f = s.blow();
			if (f != null && f.on && f.b != null && f.target == target) f.b.rec.crit = true;
		} catch (Throwable t){
			fail( t );
		}
	}

	/** Health set to nothing outside Char.damage (Preparation's knock-out, Combined Lethality,
	 *  Silent Death): everything left was taken, as one step. */
	public static void execute( Char target, Object what, int hpBefore ){
		try {
			Frame f = frameFor( STATE.get(), target );
			if (f == null) return;
			Build b = f.b;
			int hp = Math.max( 0, hpBefore );
			if (b.landed){
				append( b.steps, Step.ADD, Step.ATTACKER, what, b.running, b.running + hp );
				b.running += hp;
				b.hpLost += hp;
			} else {
				if (!b.started){
					b.steps.add( new Step( Step.BASE, Step.WORLD, text( BASE ), Step.NO_ICON, 0, 0f, 0f ) );
					b.started = true;
					b.running = 0f;
				}
				append( b.steps, Step.SET, Step.ATTACKER, what, b.running, hp );
				b.running = hp;
				b.hpLost = hp;
				b.shieldLost = 0;
				b.hpBefore = hpBefore;
				b.landed = true;
			}
			b.rec.executed = true;
		} catch (Throwable t){
			fail( t );
		}
	}

	/** A rule that finishes the struck one inside the steps (an alpha Ambush): the blow counts as
	 *  an execute. */
	public static void executed( Char target ){
		try {
			Frame f = frameFor( STATE.get(), target );
			if (f != null) f.b.rec.executed = true;
		} catch (Throwable t){
			fail( t );
		}
	}

	// ------------------------------------------------------------------ the base roll

	/** Char.attack, around the base damage roll. */
	public static void beginBaseRoll(){
		try {
			Frame f = STATE.get().top();
			if (f == null || f.type != Frame.BLOW || !f.on) return;
			f.baseScope = true;
			f.scratches = new ArrayList<>( 1 );
		} catch (Throwable t){
			fail( t );
		}
	}

	public static void endBaseRoll( float value ){
		try {
			State s = STATE.get();
			Frame f = s.top();
			if (f == null || f.type != Frame.BLOW || !f.on || !f.baseScope) return;
			f.baseScope = false;
			f.scratch = null;
			Build b = f.b;
			Build pick = null;
			ArrayList<Build> sc = f.scratches;
			f.scratches = null;
			if (sc != null){
				for (Build x : sc) if (x.started && Math.abs( x.running - value ) <= HitRecord.EPS){
					pick = x;
					break;
				}
				if (pick == null) for (Build x : sc) if (x.started && (pick == null || x.running > pick.running)) pick = x;
			}
			if (pick == null){
				b.steps.add( new Step( Step.BASE, Step.ATTACKER, text( ROLL_OF.of( f.attacker ) ), Step.NO_ICON, 0, value, value ) );
				b.running = value;
				b.started = true;
				return;
			}
			b.steps.addAll( pick.steps );
			b.running = pick.running;
			b.started = true;
			if (Math.abs( b.running - value ) > HitRecord.EPS){
				Preparation prep = f.attacker.buff( Preparation.class );
				Object what = prep != null ? (sc.size() > 1 ? BEST_OF.of( prep, sc.size() ) : prep) : OTHER;
				append( b.steps, AUTO, Step.ATTACKER, what, b.running, value );
				b.running = value;
			}
		} catch (Throwable t){
			fail( t );
		}
	}

	//the scratch a weapon roll writes into: the open blow's, while its base roll is made by `who`
	private static Build scratch( State s ){
		Frame f = s.top();
		return f != null && f.type == Frame.BLOW && f.on && f.baseScope ? f.scratch : null;
	}

	/** Hero.damageRoll, at its top: a roll of the open blow's base begins. */
	public static void rollStart( Char who ){
		try {
			State s = STATE.get();
			Frame f = s.top();
			if (f == null || f.type != Frame.BLOW || !f.on || !f.baseScope || f.attacker != who) return;
			f.scratch = new Build();
			f.scratches.add( f.scratch );
		} catch (Throwable t){
			fail( t );
		}
	}

	/** KindOfWeapon.damageRoll: the weapon's range and what it rolled, as the base. */
	public static void weaponRange( Item weapon, int lo, int hi, int value ){
		try {
			Build b = scratch( STATE.get() );
			if (b == null || b.started) return;
			b.steps.add( new Step( Step.BASE, Step.ATTACKER, text( RANGE.of( weapon, lo, hi ) ), Step.NO_ICON, 0, value, value ) );
			b.running = value;
			b.started = true;
		} catch (Throwable t){
			fail( t );
		}
	}

	/** A rule on the roll being made (an augment, a ring, an empowering buff). */
	public static void rollPart( int op, Object what, float before, float after ){
		try {
			Build b = scratch( STATE.get() );
			if (b == null) return;
			if (!b.started){
				b.steps.add( new Step( Step.BASE, Step.ATTACKER, text( BASE ), Step.NO_ICON, 0, before, before ) );
				b.running = before;
				b.started = true;
			} else if (Math.abs( b.running - before ) > HitRecord.EPS){
				append( b.steps, AUTO, Step.ATTACKER, OTHER, b.running, before );
				b.running = before;
			}
			if (Math.abs( after - before ) <= HitRecord.EPS) return;
			append( b.steps, op, Step.ATTACKER, what, before, after );
			b.running = after;
		} catch (Throwable t){
			fail( t );
		}
	}

	/** A multiplier on the roll that the game rounded: MUL then the rounding. */
	public static int rollMulRound( Object what, int before, float factor, int after ){
		if (!recording) return after;
		float exact = before * factor;
		rollPart( Step.MUL, what, before, exact );
		rollPart( Step.ROUND, ROUNDED, exact, after );
		return after;
	}

	/** The roll is now `value`: a base named `what` when nothing was recorded yet, else whatever
	 *  changed it unnamed becomes one step named `what` (a weapon's own roll rules). */
	public static void rollMark( Object what, int value ){
		try {
			Build b = scratch( STATE.get() );
			if (b == null) return;
			if (!b.started){
				b.steps.add( new Step( Step.BASE, Step.ATTACKER, text( what ), iconOf( what ), tintOf( what ), value, value ) );
				b.running = value;
				b.started = true;
			} else if (Math.abs( b.running - value ) > HitRecord.EPS){
				append( b.steps, AUTO, Step.ATTACKER, ROLL_OF.of( what ), b.running, value );
				b.running = value;
			}
		} catch (Throwable t){
			fail( t );
		}
	}

	/** Hero.damageRoll's result: the roll is done. */
	public static void rollEnd( Char who, int value ){
		try {
			State s = STATE.get();
			Frame f = s.top();
			if (f == null || f.type != Frame.BLOW || !f.on || !f.baseScope || f.attacker != who || f.scratch == null) return;
			Build b = f.scratch;
			if (!b.started){
				b.steps.add( new Step( Step.BASE, Step.ATTACKER, text( BASE ), Step.NO_ICON, 0, value, value ) );
				b.running = value;
				b.started = true;
			} else if (Math.abs( b.running - value ) > HitRecord.EPS){
				append( b.steps, AUTO, Step.ATTACKER, OTHER, b.running, value );
				b.running = value;
			}
			f.scratch = null;
		} catch (Throwable t){
			fail( t );
		}
	}

	// ------------------------------------------------------------------ a roll waiting for its damage (wands)

	/** A wand's damage roll begins: its range and what it rolled. The next damage this thread
	 *  deals with `src` starts from it. */
	public static void pendingStart( Object src, Object what, int lo, int hi, int value ){
		if (!recording) return;
		try {
			State s = STATE.get();
			Pending p = s.pending;
			if (p == null) p = s.pending = new Pending();
			p.src = src;
			p.steps.clear();
			p.steps.add( new Step( Step.BASE, Step.ATTACKER, text( RANGE.of( what, lo, hi ) ), Step.NO_ICON, 0, value, value ) );
			p.value = value;
			p.at = Actor.now();
		} catch (Throwable t){
			fail( t );
		}
	}

	/** A rule on the waiting roll. */
	public static void pendingStep( int op, Object what, float before, float after ){
		if (!recording) return;
		try {
			Pending p = STATE.get().pending;
			if (p == null || p.src == null) return;
			if (Math.abs( p.value - before ) > HitRecord.EPS) append( p.steps, AUTO, Step.ATTACKER, OTHER, p.value, before );
			if (Math.abs( after - before ) > HitRecord.EPS) append( p.steps, op, Step.ATTACKER, what, before, after );
			p.value = after;
		} catch (Throwable t){
			fail( t );
		}
	}

	/** The waiting roll is done: `value`, dealt by `src`. */
	public static void pendingRoll( Object src, int value ){
		if (!recording) return;
		try {
			Pending p = STATE.get().pending;
			if (p == null || p.src != src) return;
			if (Math.abs( p.value - value ) > HitRecord.EPS){
				append( p.steps, AUTO, Step.ATTACKER, OTHER, p.value, value );
				p.value = value;
			}
		} catch (Throwable t){
			fail( t );
		}
	}

	// ------------------------------------------------------------------ the roll to hit

	/** Char.hit, before the two stats are read. */
	public static void beginRoll( Char a, Char d ){
		try {
			State s = STATE.get();
			s.roll = null;
			if (!enabled() || a == null || d == null) return;
			Hero hero = Dungeon.hero;
			if (a != hero && d != hero) return;
			RollScope r = new RollScope();
			r.a = a;
			r.d = d;
			s.roll = r;
		} catch (Throwable t){
			fail( t );
		}
	}

	//the stat being read of `who` now: the attacker's accuracy, then the defender's evasion
	private static Chain stat( Char who ){
		RollScope r = STATE.get().roll;
		if (r == null) return null;
		if (r.phase == 0 && who == r.a) return r.acc;
		if (r.phase == 1 && who == r.d) return r.def;
		return null;
	}

	/** A stat's starting value (the hero's own accuracy or evasion), named `what` (null: "<his>
	 *  accuracy" or "<his> evasion"). */
	public static void statBase( Char who, Object what, float value ){
		try {
			Chain c = stat( who );
			if (c == null || c.hasBase) return;
			c.hasBase = true;
			c.base = value;
			c.baseWhat = what;
		} catch (Throwable t){
			fail( t );
		}
	}

	/** A factor on the stat (returned as is). */
	public static float statFactor( Char who, Object what, float factor ){
		if (!recording) return factor;
		try {
			Chain c = stat( who );
			if (c != null && factor != 1f) c.parts.add( new Object[]{ Step.MUL, what, factor } );
		} catch (Throwable t){
			fail( t );
		}
		return factor;
	}

	/** An amount added to the stat's base (the hero's to-hit bonuses come before every factor). */
	public static int statAdd( Char who, Object what, int amount ){
		if (!recording) return amount;
		try {
			Chain c = stat( who );
			if (c != null && amount != 0) c.parts.add( new Object[]{ Step.ADD, what, (float) amount } );
		} catch (Throwable t){
			fail( t );
		}
		return amount;
	}

	/** The stat becomes this value (it cannot miss, it cannot be hit; returned as is). */
	public static int statSet( Char who, Object what, int value ){
		if (!recording) return value;
		try {
			Chain c = stat( who );
			if (c != null) c.parts.add( new Object[]{ Step.SET, what, (float) value } );
		} catch (Throwable t){
			fail( t );
		}
		return value;
	}

	/** The stat as a call turned it (armour's evasion): from what it was to `after`. */
	public static float statTo( Char who, Object what, float after ){
		if (!recording) return after;
		try {
			Chain c = stat( who );
			if (c != null) c.parts.add( new Object[]{ AUTO, what, after } );
		} catch (Throwable t){
			fail( t );
		}
		return after;
	}

	//the stat's chain: base, the adds, then each factor, transform and set in order, then the
	//rounding to what the getter returned
	private static void compose( Chain c, Object label, float value ){
		c.steps.clear();
		float run = c.hasBase ? c.base : value;
		Object baseWhat = c.hasBase ? (c.baseWhat != null ? c.baseWhat : label) : label;
		c.steps.add( new Step( Step.BASE, Step.WORLD, text( baseWhat ), iconOf( baseWhat ), tintOf( baseWhat ), run, run ) );
		if (c.hasBase){
			for (Object[] p : c.parts){
				if ((Integer) p[0] != Step.ADD) continue;
				float after = run + (Float) p[2];
				append( c.steps, Step.ADD, Step.WORLD, p[1], run, after );
				run = after;
			}
			boolean set = false;
			for (Object[] p : c.parts){
				int op = (Integer) p[0];
				if (op == Step.ADD) continue;
				float v = (Float) p[2];
				if (op == Step.MUL){
					//an endless factor (a sure hit) sets the stat to what it came to
					if (Float.isInfinite( v ) || v >= Char.INFINITE_ACCURACY){
						append( c.steps, Step.SET, Step.WORLD, p[1], run, value );
						run = value;
						set = true;
						break;
					}
					float after = run * v;
					append( c.steps, Step.MUL, Step.WORLD, p[1], run, after );
					run = after;
				} else if (op == Step.SET){
					//it cannot miss, it cannot be hit (or it can always be): the stat the getter returned
					append( c.steps, Step.SET, Step.WORLD, p[1], run, value );
					run = value;
					set = true;
					break;
				} else {
					if (Math.abs( v - run ) > HitRecord.EPS) append( c.steps, AUTO, Step.WORLD, p[1], run, v );
					run = v;
				}
			}
			if (!set && Math.abs( run - value ) > HitRecord.EPS){
				append( c.steps, Step.ROUND, Step.WORLD, ROUNDED, run, value );
				run = value;
			} else if (set && Math.abs( run - value ) > HitRecord.EPS){
				append( c.steps, AUTO, Step.WORLD, OTHER, run, value );
				run = value;
			}
		}
		c.running = run;
		c.started = true;
		c.parts.clear();
	}

	/** Char.hit: the attacker's accuracy as read. */
	public static void accRead( float value ){
		try {
			RollScope r = STATE.get().roll;
			if (r == null || r.phase != 0) return;
			compose( r.acc, ACCURACY.of( r.a ), value );
			r.phase = 1;
		} catch (Throwable t){
			fail( t );
		}
	}

	/** Char.hit: the defender's evasion as read. */
	public static void defRead( float value ){
		try {
			RollScope r = STATE.get().roll;
			if (r == null || r.phase != 1) return;
			compose( r.def, EVASION.of( r.d ), value );
			r.phase = 2;
		} catch (Throwable t){
			fail( t );
		}
	}

	/** A rule of Char.hit's own, on a stat (before the rolls) or on a roll (after rollBase). */
	public static void rollStep( boolean attackerSide, Object what, float before, float after ){
		try {
			RollScope r = STATE.get().roll;
			if (r == null || r.phase < 2) return;
			Chain c = r.phase == 2 ? (attackerSide ? r.acc : r.def) : (attackerSide ? r.accRoll : r.defRoll);
			if (!c.started){
				c.steps.add( new Step( Step.BASE, Step.WORLD, text( RAW_ROLL ), Step.NO_ICON, 0, before, before ) );
				c.running = before;
				c.started = true;
			} else if (Math.abs( c.running - before ) > HitRecord.EPS){
				append( c.steps, AUTO, Step.WORLD, OTHER, c.running, before );
				c.running = before;
			}
			if (Math.abs( after - before ) <= HitRecord.EPS) return;
			int op = before != 0f && after != 0f && !Float.isInfinite( after ) && after < Char.INFINITE_ACCURACY ? Step.MUL : Step.SET;
			append( c.steps, op, attackerSide ? Step.ATTACKER : Step.DEFENDER, what, before, after );
			if (r.phase == 3 && op == Step.MUL) c.mul *= after / before;
			c.running = after;
		} catch (Throwable t){
			fail( t );
		}
	}

	/** value * factor on a stat or a roll, recorded. */
	public static float rollMul( boolean attackerSide, Object what, float value, float factor ){
		float after = value * factor;
		if (recording) rollStep( attackerSide, what, value, after );
		return after;
	}

	/** One side's random roll was made: its multipliers follow. */
	public static void rollBase( boolean attackerSide, float raw ){
		try {
			RollScope r = STATE.get().roll;
			if (r == null || r.phase < 2) return;
			r.phase = 3;
			Chain c = attackerSide ? r.accRoll : r.defRoll;
			c.steps.clear();
			c.steps.add( new Step( Step.BASE, attackerSide ? Step.ATTACKER : Step.DEFENDER, text( RAW_ROLL ), Step.NO_ICON, 0, raw, raw ) );
			c.running = raw;
			c.started = true;
			c.mul = 1f;
		} catch (Throwable t){
			fail( t );
		}
	}

	/** Char.hit's verdict, before each return. */
	public static void roll( Char a, Char d, float accStat, float defStat, float accRoll, float defRoll, boolean hit, int reasonIcon ){
		try {
			State s = STATE.get();
			RollScope r = s.roll;
			s.roll = null;
			if (r == null || r.a != a || r.d != d) return;
			Roll out = new Roll();
			out.accStat = accStat;
			out.defStat = defStat;
			out.accRoll = accRoll;
			out.defRoll = defRoll;
			out.hit = hit;
			out.reasonIcon = reasonIcon;
			out.sure = accStat >= Char.INFINITE_ACCURACY;
			out.untouchable = defStat >= Char.INFINITE_EVASION;
			if (r.phase < 1) compose( r.acc, ACCURACY.of( a ), accStat );
			if (r.phase < 2) compose( r.def, EVASION.of( d ), defStat );
			settle( r.acc, accStat );
			settle( r.def, defStat );
			out.accSteps = r.acc.steps.toArray( new Step[0] );
			out.defSteps = r.def.steps.toArray( new Step[0] );
			if (r.accRoll.started){
				settle( r.accRoll, accRoll );
				out.accRollSteps = r.accRoll.steps.toArray( new Step[0] );
				out.accMul = r.accRoll.mul;
			}
			if (r.defRoll.started){
				settle( r.defRoll, defRoll );
				out.defRollSteps = r.defRoll.steps.toArray( new Step[0] );
				out.defMul = r.defRoll.mul;
			}
			attach( s, a, d, out );
		} catch (Throwable t){
			fail( t );
		}
	}

	private static void settle( Chain c, float value ){
		if (c.started && Math.abs( c.running - value ) > HitRecord.EPS){
			append( c.steps, AUTO, Step.WORLD, OTHER, c.running, value );
			c.running = value;
		}
	}

	/** Char.hit for an attacker that cannot miss before anything is read (SpaceTimePowers). */
	public static void sureRoll( Char a, Char d ){
		try {
			if (!enabled() || a == null || d == null) return;
			Hero hero = Dungeon.hero;
			if (a != hero && d != hero) return;
			State s = STATE.get();
			s.roll = null;
			Roll out = new Roll();
			out.hit = out.sure = true;
			attach( s, a, d, out );
		} catch (Throwable t){
			fail( t );
		}
	}

	//a roll goes to the open blow it is for; outside a blow a hit waits for its damage (a zap's
	//bolt lands later), a miss is kept at once
	private static void attach( State s, Char a, Char d, Roll out ){
		Frame f = s.top();
		if (f != null && f.type == Frame.BLOW && f.attacker == a && f.target == d && f.roll == null){
			f.roll = out;
			return;
		}
		if (out.hit){
			synchronized (ZAPS){
				if (ZAPS.size() >= 16) ZAPS.remove( 0 );
				ZAPS.add( new Object[]{ a, d, Actor.now(), out } );
			}
			return;
		}
		Hero hero = Dungeon.hero;
		HitRecord r = new HitRecord();
		r.kind = HitRecord.DIRECT;
		r.outcome = HitRecord.MISS;
		r.serial = nextSerial();
		r.parent = parentOf( s, null );
		r.dealt = d != hero;
		stamp( r, d );
		r.attacker = side( a );
		r.defender = side( d );
		r.sourceName = text( a );
		r.sourceClass = a.getClass().getName();
		r.roll = out;
		add( r );
	}

	private static final ArrayList<Object[]> ZAPS = new ArrayList<>();

	//the hit a zap rolled at this target in this instant, taken out of the waiting list
	private static Roll zapRoll( Char a, Char d ){
		synchronized (ZAPS){
			float now = Actor.now();
			for (int i = ZAPS.size() - 1; i >= 0; i--){
				Object[] z = ZAPS.get( i );
				if (z[1] == d && (a == null || z[0] == a) && Math.abs( (Float) z[2] - now ) < 0.01f){
					ZAPS.remove( i );
					return (Roll) z[3];
				}
			}
			return null;
		}
	}

	private static Char zapAttacker( Char d ){
		synchronized (ZAPS){
			float now = Actor.now();
			for (int i = ZAPS.size() - 1; i >= 0; i--){
				Object[] z = ZAPS.get( i );
				if (z[1] == d && Math.abs( (Float) z[2] - now ) < 0.01f) return (Char) z[0];
			}
			return null;
		}
	}

	// ------------------------------------------------------------------ damage

	/** Hero.damage and Char.damage, first thing: true when it opened a frame (the caller then
	 *  calls exitDamage in its finally). Hero.damage then Char.damage is one frame; a blow's
	 *  damage joins its blow. */
	public static boolean enterDamage( Char target, int dmg, Object src ){
		try {
			if (!recording || target == null || dmg < 0 || !target.isAlive() || NetManager.isNetClient()) return false;
			State s = STATE.get();
			Frame top = s.top();
			//the second layer of the same damage (Char.damage under Hero.damage)
			if (top != null && top.type == Frame.DAMAGE && top.target == target && top.src == src && top.entered < top.layers){
				top.entered++;
				if (top.on && top.b != null) reconcile( s, top, top.b, dmg, OTHER );
				return false;
			}
			//the blow's own damage
			if (top != null && top.type == Frame.BLOW && top.expect && !top.mainEntered && top.target == target && top.attacker == src){
				top.mainEntered = true;
				Frame f = s.push( Frame.DAMAGE );
				f.main = true;
				f.target = target;
				f.src = src;
				f.attacker = top.attacker;
				f.layers = target instanceof Hero ? 2 : 1;
				f.entered = 1;
				f.serial = top.serial;
				if (top.on && top.b != null){
					f.on = true;
					f.b = top.b;
					if (!f.b.started) reconcile( s, f, f.b, dmg, OTHER );
					//whatever the struck one's own damage rules did before it got here
					if (Math.abs( f.b.running - dmg ) > HitRecord.EPS){
						append( f.b.steps, AUTO, Step.DEFENDER, OWN_RULES.of( target ), f.b.running, dmg );
						f.b.running = dmg;
					}
				}
				return true;
			}
			Frame f = s.push( Frame.DAMAGE );
			f.target = target;
			f.src = src;
			f.layers = target instanceof Hero ? 2 : 1;
			f.entered = 1;
			if (!enabled()) return true;
			Hero hero = Dungeon.hero;
			Frame blow = s.blow();
			Object cur = Actor.current();
			Char by = src instanceof Char ? (Char) src : null;
			if (by == null) by = zapAttacker( target );
			if (by == null && cur instanceof Char && cur != target) by = (Char) cur;
			boolean taken = target == hero;
			boolean dealt = !taken && (src == hero
					|| src instanceof Wand || src instanceof ClericSpell || src instanceof ArmorAbility
					|| src instanceof Skill || src instanceof Bomb
					|| SkillInteractions.heroSkillSource( src )
					|| (blow != null && blow.attacker == hero)
					|| (blow != null && blow.target == hero && target == blow.attacker)
					|| cur == hero);
			if (!taken && !dealt) return true;
			if (src == null) return true;
			//what the hero set going (a wand's bolt, a bomb, a skill) is his even when no creature
			//stands behind it this moment
			if (dealt && by == null) by = hero;
			Build b = f.b = new Build();
			HitRecord r = b.rec;
			r.kind = blow == null && (src instanceof Buff || src instanceof Blob) ? HitRecord.DOT : HitRecord.DIRECT;
			r.serial = nextSerial();
			r.parent = blow != null ? blow.serial : 0;
			r.dealt = dealt;
			stamp( r, target );
			if (by != null && by != target){
				r.attacker = side( by );
				f.attacker = by;
			}
			r.defender = side( target );
			r.request = dmg;
			r.sourceIcon = iconFor( src );
			r.sourceItem = src instanceof Item ? ((Item) src).image() : -1;
			r.sourceName = src instanceof Item ? itemName( (Item) src ) : text( src );
			r.sourceClass = (src instanceof Class ? (Class<?>) src : src.getClass()).getName();
			r.roll = zapRoll( by, target );
			//a wand's roll, made a moment ago on this thread
			Pending p = s.pending;
			if (p != null && p.src == src && Math.abs( p.at - Actor.now() ) < 0.01f && !p.steps.isEmpty()){
				b.steps.addAll( p.steps );
				b.running = p.value;
				b.started = true;
				if (Math.abs( b.running - dmg ) > HitRecord.EPS){
					append( b.steps, AUTO, Step.ATTACKER, WEAPON_EFFECT.of( src ), b.running, dmg );
					b.running = dmg;
				}
			} else {
				b.steps.add( new Step( Step.BASE, by != null ? Step.ATTACKER : Step.WORLD, r.sourceName, iconOf( src ), tintOf( src ), dmg, dmg ) );
				b.running = dmg;
				b.started = true;
			}
			f.on = true;
			return true;
		} catch (Throwable t){
			fail( t );
			return false;
		}
	}

	/** Char.damage, just before health is taken: what goes to health and what the shields took. */
	public static void landed( Char target, int dmgToHp, int shielded ){
		try {
			State s = STATE.get();
			Frame f = frameFor( s, target );
			if (f == null || f.type != Frame.DAMAGE || f.b.landed) return;
			Build b = f.b;
			reconcile( s, f, b, dmgToHp + shielded, OTHER );
			b.landed = true;
			b.hpBefore = target.HP;
			//the shields already gave what they took
			b.shieldBefore = target.shielding() + shielded;
			b.hpLost = dmgToHp;
			b.shieldLost = shielded;
		} catch (Throwable t){
			fail( t );
		}
	}

	/** Grim's execute, after health was taken: `extra` more. */
	public static void grim( Char target, int extra ){
		try {
			Frame f = frameFor( STATE.get(), target );
			if (f == null || !f.b.landed || extra == 0) return;
			Build b = f.b;
			append( b.steps, Step.ADD, Step.ATTACKER, GRIM, b.running, b.running + extra );
			b.running += extra;
			b.hpLost += extra;
			b.rec.executed = true;
		} catch (Throwable t){
			fail( t );
		}
	}

	/** An early end of Char.damage or Hero.damage that took nothing: bleeding instead (with how
	 *  much), time stopped. */
	public static void outcome( Char target, int outcome, float amount ){
		try {
			Frame f = frameFor( STATE.get(), target );
			if (f == null || f.b.landed) return;
			Build b = f.b;
			b.outcome = outcome;
			Object what = outcome == HitRecord.BLEED ? BLEED.of( Math.round( amount ) ) : outcome == HitRecord.STASIS ? STASIS : NO_DAMAGE;
			if (b.started && Math.abs( b.running ) > HitRecord.EPS){
				append( b.steps, Step.SET, Step.DEFENDER, what, b.running, 0f );
				b.running = 0f;
			}
		} catch (Throwable t){
			fail( t );
		}
	}

	/** Whoever opened the frame (Hero.damage or Char.damage), in its finally. */
	public static void exitDamage( Char target ){
		try {
			State s = STATE.get();
			Frame f = null;
			for (int i = s.depth - 1; i >= 0; i--){
				Frame g = s.frames[i];
				if (g.type == Frame.DAMAGE && g.target == target){
					f = g;
					s.depth = i;
					break;
				}
			}
			if (f == null || !f.on || f.b == null) return;
			f.on = false;
			Build b = f.b;
			if (!b.landed && b.outcome < 0){
				Object what;
				if (f.src != null && target.isInvulnerable( f.src.getClass() )){
					b.outcome = HitRecord.IMMUNE;
					what = IMMUNE;
				} else if (target.buff( RoseBarrier.class ) != null || target.buff( Cocoshield.class ) != null){
					b.outcome = HitRecord.BARRIER;
					what = BARRIER;
				} else {
					b.outcome = HitRecord.NOTHING;
					what = NO_DAMAGE;
				}
				if (b.started && Math.abs( b.running ) > HitRecord.EPS){
					append( b.steps, Step.SET, Step.DEFENDER, what, b.running, 0f );
					b.running = 0f;
				}
			}
			//a blow's damage is the blow's: it commits at closeBlow
			if (f.main) return;
			HitRecord r = b.rec;
			r.outcome = b.landed ? HitRecord.HIT : b.outcome;
			//a ping (0 asked, nothing taken) and a damage that never reached anyone are not hits
			if (r.outcome == HitRecord.NOTHING) return;
			if (r.request == 0 && b.hpLost == 0 && b.shieldLost == 0 && r.outcome == HitRecord.HIT) return;
			r.killed = !target.isAlive();
			commit( b );
		} catch (Throwable t){
			fail( t );
		}
	}

	// ------------------------------------------------------------------ committing

	private static void commit( Build b ){
		HitRecord r = b.rec;
		if (r.outcome == HitRecord.MISS){
			r.steps = new Step[0];
			r.hpLost = r.shieldLost = r.shown = r.overkill = 0;
		} else if (r.outcome == HitRecord.HIT){
			r.hpLost = b.hpLost;
			r.shieldLost = b.shieldLost;
			r.shown = r.hpLost + r.shieldLost;
			r.overkill = b.hpBefore == Integer.MIN_VALUE ? 0 : Math.max( 0, r.hpLost - Math.max( 0, b.hpBefore ) );
			//the struck one as the damage met it: its snapshot was taken when the blow began, and a
			//proc or a skill may have hurt it since
			if (r.defender != null && b.hpBefore != Integer.MIN_VALUE){
				r.defender.hp = b.hpBefore;
				if (b.shieldBefore >= 0) r.defender.shield = b.shieldBefore;
			}
			if (b.started && Math.abs( b.running - r.shown ) > HitRecord.EPS){
				append( b.steps, AUTO, Step.WORLD, OTHER, b.running, r.shown );
				b.running = r.shown;
			}
			r.steps = b.steps.toArray( new Step[0] );
		} else {
			r.hpLost = r.shieldLost = r.shown = r.overkill = 0;
			r.steps = b.steps.toArray( new Step[0] );
		}
		if (!r.sums()) complain( "a record did not sum: " + r.sourceName + " " + Arrays.toString( r.steps ) );
		add( r );
	}

	private static void add( HitRecord r ){
		Hero hero = Dungeon.hero;
		if (hero == null) return;
		DamageLog log = hero.damageLog;
		if (r.kind == HitRecord.DOT && r.outcome == HitRecord.HIT){
			synchronized (log){
				HitRecord last = log.newest( r.dealt );
				if (last != null && last.kind == HitRecord.DOT && last.outcome == HitRecord.HIT
						&& last.targetId == r.targetId && last.ticks < MAX_TICKS
						&& last.sourceClass != null && last.sourceClass.equals( r.sourceClass )
						&& r.turn - last.lastTick() <= 1.01f && r.turn >= last.lastTick()){
					log.replaceNewest( r.dealt, fold( last, r ) );
					return;
				}
			}
		}
		log.add( r );
	}

	/** A DOT run is at most this many ticks; the next tick starts a new record. */
	public static final int MAX_TICKS = 50;

	//a run with one more tick: a new record (a committed one is never changed under a reader)
	private static HitRecord fold( HitRecord run, HitRecord tick ){
		HitRecord m = run.copy();
		int[] amounts = run.ticks > 1 ? run.tickAmounts : new int[]{ run.shown };
		m.tickAmounts = Arrays.copyOf( amounts, amounts.length + 1 );
		m.tickAmounts[amounts.length] = tick.shown;
		m.ticks = m.tickAmounts.length;
		m.shown = run.shown + tick.shown;
		m.hpLost = run.hpLost + tick.hpLost;
		m.shieldLost = run.shieldLost + tick.shieldLost;
		m.overkill = run.overkill + tick.overkill;
		m.killed = tick.killed;
		m.steps = tick.steps;
		m.lastTurn = tick.turn;
		return m;
	}

	// ------------------------------------------------------------------ the card it showed

	/** BlowPresenter, after the struck one's card: what the hit looked like, kept on its record. */
	public static void card( int serial, Char d, int value, int color, int icon, boolean crit, int[] codes, int[] tints, int n ){
		try {
			if (n <= 0 && value < 0 && icon < 0 && !crit) return;
			Hero hero = Dungeon.hero;
			if (hero == null) return;
			DamageLog log = hero.damageLog;
			synchronized (log){
				HitRecord r = log.blow( serial );
				if (r == null) return;
				r.cardIcon = icon;
				r.cardColor = color;
				r.cardValue = value;
				int k = Math.max( 0, Math.min( n, Math.min( codes.length, tints.length ) ) );
				r.chips = Arrays.copyOf( codes, k );
				r.chipTints = Arrays.copyOf( tints, k );
				if (crit) r.crit = true;
				log.changed();
			}
		} catch (Throwable t){
			fail( t );
		}
	}

	// ------------------------------------------------------------------ snapshots

	/** An item's name as its window shows it, its upgrades too (not how many there are). */
	static String itemName( Item item ){
		String n = Messages.titleCase( item.name() );
		int up = item.visiblyUpgraded();
		return up != 0 ? Messages.format( "%s %+d", n, up ) : n;
	}

	/** A character as it is now: name, health, shield, the buffs that show an icon, and the
	 *  hero's gear. Reads only. */
	static Side side( Char c ){
		Side s = new Side();
		s.name = text( c );
		s.hero = c == Dungeon.hero;
		if (!s.hero) s.cls = c.getClass().getName();
		if (c instanceof Hero){
			Hero h = (Hero) c;
			if (h.heroClass != null) s.heroClass = h.heroClass.ordinal();
			s.heroTier = h.tier();
			Item w = h.belongings.weapon();
			if (w != null){
				s.weapon = itemName( w );
				Quality q = Quality.of( w );
				if (q != null){
					s.weaponRarity = Messages.capitalize( q.rarity.title() );
					s.weaponColor = q.titleColor();
				}
			}
			Item a = h.belongings.armor();
			if (a != null){
				s.armor = itemName( a );
				Quality q = Quality.of( a );
				if (q != null){
					s.armorRarity = Messages.capitalize( q.rarity.title() );
					s.armorColor = q.titleColor();
				}
			}
		}
		s.hp = c.HP;
		s.ht = c.HT;
		s.shield = c.shielding();
		ArrayList<Buff> shown = new ArrayList<>();
		for (Buff b : c.buffs()){
			int i = b.icon();
			if (i >= 0 && i != BuffIndicator.NONE) shown.add( b );
		}
		int n = shown.size();
		s.buffIcons = new int[n];
		s.buffTints = new int[n];
		s.buffNames = new String[n];
		s.buffClasses = new String[n];
		for (int k = 0; k < n; k++){
			Buff b = shown.get( k );
			s.buffIcons[k] = b.icon();
			s.buffTints[k] = tintOf( b );
			s.buffNames[k] = text( b );
			s.buffClasses[k] = b.getClass().getName();
		}
		return s;
	}

	/** The text cell a damage number from this source shows (Char.damage's choice, the hit
	 *  reasons aside). */
	public static int iconFor( Object src ){
		if (src == null) return FloatingText.PHYS_DMG;
		Class<?> c = src instanceof Class ? (Class<?>) src : src.getClass();
		int icon = FloatingText.PHYS_DMG;
		if (NO_ARMOR.contains( c ))                                 icon = FloatingText.PHYS_DMG_NO_BLOCK;
		if (AntiMagic.RESISTS.contains( c ))                        icon = FloatingText.MAGIC_DMG;
		if (src instanceof Pickaxe)                                 icon = FloatingText.PICK_DMG;
		if (src instanceof Hunger)                                  icon = FloatingText.HUNGER;
		if (src instanceof Burning)                                 icon = FloatingText.BURNING;
		if (src instanceof Chill || src instanceof Frost)           icon = FloatingText.FROST;
		if (src instanceof GeyserTrap || src instanceof StormCloud) icon = FloatingText.WATER;
		if (src instanceof Electricity)                             icon = FloatingText.SHOCKING;
		if (src instanceof Bleeding)                                icon = FloatingText.BLEEDING;
		if (src instanceof ToxicGas)                                icon = FloatingText.TOXIC;
		if (src instanceof Corrosion)                               icon = FloatingText.CORROSION;
		if (src instanceof Poison)                                  icon = FloatingText.POISON;
		if (src instanceof Ooze)                                    icon = FloatingText.OOZE;
		if (src instanceof Viscosity.DeferedDamage)                 icon = FloatingText.DEFERRED;
		if (src instanceof Corruption)                              icon = FloatingText.CORRUPTION;
		if (src instanceof AscensionChallenge)                      icon = FloatingText.AMULET;
		return icon;
	}

	//Char's misc. physical sources that ignore armour (its NO_ARMOR_PHYSICAL_SOURCES)
	private static final java.util.HashSet<Class<?>> NO_ARMOR = new java.util.HashSet<>( Arrays.asList(
			CrystalSpire.SpireSpike.class, GnollGeomancer.Boulder.class, GnollGeomancer.GnollRockFall.class,
			GnollRockfallTrap.class, DwarfKing.KingDamager.class, DwarfKing.Summoning.class, LifeLink.class,
			Chasm.class, WandOfBlastWave.Knockback.class, Heap.class, Necromancer.SummoningBlockDamage.class,
			DriedRose.GhostHero.NoRoseDamage.class ) );
}
