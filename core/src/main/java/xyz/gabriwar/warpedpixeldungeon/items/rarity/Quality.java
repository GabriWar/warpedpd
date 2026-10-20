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

package xyz.gabriwar.warpedpixeldungeon.items.rarity;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mimic;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.AntiMagic;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Brimstone;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Camouflage;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Flow;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Obfuscation;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Stone;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Swiftness;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Ancient;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Crystal;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Eldritch;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Horror;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Kinetic;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Luck;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Poison;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Projecting;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Slashing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Unstable;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Venomous;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sickle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WarScythe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.Gun;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * The rarity / type / masterwork state of one item. Lives in {@link Item#quality}, null
 * for items the system does not touch. Everything the rest of the game asks is a static
 * helper here, so the hooks in Weapon, Armor, Wand and Mob stay one line each.
 */
public class Quality implements Bundlable {

	public enum Family { WEAPON, ARMOR, MISSILE, WAND, RING, TRINKET, ARTIFACT }

	//five steps, a core each: the first four lift every bonus line by 15% each, the fifth doubles them
	public static final int MASTERWORK_MAX = 5;
	//how many masterwork steps a common-to-rare base can take; the last needs legendary or exotic
	public static final int MASTERWORK_CAP_LOW = 4;

	public Rarity rarity = Rarity.COMMON;
	public ItemType type = ItemType.GAMMA;
	public int masterwork = 0;
	public int kills = 0;
	//the one-shot enchantment a legendary base is born with, so it is never handed twice
	public boolean granted = false;

	public ArrayList<RarityLine> lines = new ArrayList<>();
	public ArrayList<Float> rolls = new ArrayList<>();

	//weapon perk state: consecutive hits on one target (momentum, flurry), a free follow-up
	//shot (marksman); see WeaponPerk
	public int hitTarget = -1;
	public int hitCount = 0;
	public boolean freeNext = false;
	//throw (missile perk): the roll just made was a max-damage hit
	public boolean keenHit = false;
	//the turn a cooldown-gated alpha twist is ready again; not saved, resets on load
	public float twistReady = 0f;
	//the turn a fully-masterworked kill may pay out again, kept apart from twistReady so
	//a weapon's twist and its kill reward never share one cooldown
	public float killReady = 0f;

	//perk state that lives only for the run: ramps, banked amounts and the stamps of the
	//twists that need a memory. None of it is bundled - a load starts them clean, which is
	//what a ramp should do anyway
	public int swiftStacks = 0;
	public int dodgeStacks = 0;
	public float dodgeUntil = 0f;
	public int bankedBlock = 0;
	public Class<?> wardImmune = null;
	public float wardImmuneUntil = 0f;
	public boolean sureNext = false;
	public int assaultStacks = 0;
	public float assaultUntil = 0f;
	public int nextZapLevels = 0;
	//Bard: the notes played toward the next chord, and whether that chord is a dread (a rally first)
	public int notes = 0;
	public boolean dreadNext = false;
	//a keen throw that landed flies back to the thrower's hand (MissileWeapon.rangedHit)
	public boolean returnNext = false;
	//the blow being dealt is a boomerang's strike on its way back, worth a share of a hit
	public boolean returnStrike = false;
	//CRUSH's armour shred, tracked on the one target it is eating through
	public int shredTarget = -1;
	public int shredStacks = 0;
	public float shredUntil = 0f;
	//the blow being dealt has already landed a critical hit (a perk's or the CRIT line's):
	//one crit per blow. Cleared at the start of every weaponHit
	public boolean critBlow = false;

	//the vestige: a faint third magic every legendary and exotic weapon (an enchantment) and armor
	//(a glyph) is born with. Meaningful only while the VESTIGE line is held, so read it through
	//vestigeOf, glyphVestigeOf or vestigeMagic: a scorch or a removed line turns it off with no more code
	public Bundlable vestige = null;
	//the family a SLAYER line is sharpened against, rolled with it. Meaningful only while the line is
	//held (as the vestige is): read it where SLAYER is
	public SlayerFamily slayer = null;
	//false only for a quality loaded from a save older than weapon vestiges (vestigeRolled) or
	//armor ones (glyphVestigeRolled), which settleVestige grants its one vestige once
	private boolean vestigeRolled = true;
	private boolean glyphVestigeRolled = true;

	// ---------------------------------------------------------------- lookup

	public static Family family( Item item ){
		if (item instanceof MissileWeapon) return Family.MISSILE;
		//melee weapons and the guns and bows that extend Weapon directly
		if (item instanceof Weapon) return Family.WEAPON;
		//class armor too: the crown and the armor kit carry the old armor's quality over
		if (item instanceof Armor) return Family.ARMOR;
		if (item instanceof Wand) return Family.WAND;
		if (item instanceof xyz.gabriwar.warpedpixeldungeon.items.rings.Ring) return Family.RING;
		if (item instanceof xyz.gabriwar.warpedpixeldungeon.items.trinkets.Trinket
				&& !(item instanceof xyz.gabriwar.warpedpixeldungeon.items.trinkets.Trinket.PlaceHolder)) return Family.TRINKET;
		if (item instanceof xyz.gabriwar.warpedpixeldungeon.items.artifacts.Artifact) return Family.ARTIFACT;
		return null;
	}

	/** every family has rarity tiers, bonus lines and masterwork; a ring's, trinket's or
	 *  artifact's lines are hero-wide stats (see RarityLine.stat) */
	public static boolean hasRarity( Item item ){
		return family( item ) != null;
	}

	/** whether the rarity also adds a share of the item's own number (potency): weapons,
	 *  armor and wands have damage, block or zap power; rings, trinkets and artifacts do not */
	public static boolean hasPotency( Item item ){
		Family f = family( item );
		return f == Family.WEAPON || f == Family.ARMOR || f == Family.MISSILE || f == Family.WAND;
	}

	public static boolean eligible( Item item ){
		return item != null && family( item ) != null;
	}

	/**
	 * A plain common, gamma quality for a fresh item of a kind, so the journal's catalog can
	 * show what each rarity and type would give that kind (describeRarityTier, perkText).
	 * Rolls nothing (no game Random), and is never put on an item the hero can hold.
	 */
	public static Item sample( Item item ){
		if (!eligible( item )) return item;
		item.quality = new Quality();
		return item;
	}

	/** the item's quality, or null when the item is outside the system */
	public static Quality of( Item item ){
		return item == null ? null : item.quality;
	}

	// ---------------------------------------------------------------- generation

	/** rolls rarity, type and lines for a freshly generated item; a no-op for items that
	 *  already carry a quality (loaded, copied) or are outside the system */
	public static void roll( Item item ){
		roll( item, null, null, 0 );
	}

	/** the same roll with any part forced (null = rolled by depth); used by the debug picker */
	public static void roll( Item item, Rarity rarity, ItemType type, int masterwork ){
		if (!eligible( item ) || item.quality != null) return;
		//a barrow rolls by the depth its floor counts as (Delves.lootDepth)
		int depth = Math.max( 1, xyz.gabriwar.warpedpixeldungeon.levels.Delves.lootDepth() );
		Quality q = new Quality();
		boolean tiered = hasRarity( item );
		q.rarity = !tiered ? Rarity.COMMON : rarity != null ? rarity : Rarity.roll( depth );
		q.type = type != null ? type : ItemType.roll( depth );
		if (tiered) q.rollLines( item );
		item.quality = q;
		q.onGenerated( item );
		//after the birth enchantment, so the vestige never echoes it
		if (tiered) q.rollVestige( item );
		for (int i = 0; tiered && i < masterwork; i++){
			if (q.masterworkCost() == 0) break;
			q.masterworkStep( item );
		}
		q.applyDerived( item );
	}

	//a weapon, thrown weapon or armor fills slots: a common one a plain bonus, an uncommon one a
	//trait, a rare or better one a trait then a plain bonus (so a scorch burns the plain one first).
	//Any other family takes its tier's count from the whole pool. One pick per line, whatever the
	//pool's size, so a line added later never shifts what the game draws after it
	private void rollLines( Item item ){
		lines.clear();
		rolls.clear();
		slayer = null;
		Family fam = family( item );
		if (!RarityLine.slotted( fam )){
			ArrayList<RarityLine> pool = RarityLine.pool( fam, item );
			for (int i = 0; i < rarity.lines; i++) pickLine( pool );
		} else if (rarity == Rarity.COMMON){
			pickLine( RarityLine.plain( fam, item ) );
		} else {
			pickLine( RarityLine.traits( fam, item ) );
			if (rarity.lines > 1) pickLine( RarityLine.plain( fam, item ) );
		}
	}

	//one line from the list, which loses it, at the tier's roll, and a Slayer's family; nothing when
	//the list is empty
	private void pickLine( ArrayList<RarityLine> from ){
		if (from.isEmpty()) return;
		RarityLine line = from.remove( Random.Int( from.size() ) );
		lines.add( line );
		rolls.add( rarity.rollLine() );
		if (line == RarityLine.SLAYER) slayer = SlayerFamily.values()[Random.Int( SlayerFamily.values().length )];
	}

	//the one line a masterworked base with none gains: a plain bonus for the families that fill slots
	private static ArrayList<RarityLine> fallbackPool( Item item ){
		Family fam = family( item );
		return RarityLine.slotted( fam ) ? RarityLine.plain( fam, item ) : RarityLine.pool( fam, item );
	}

	//stats that are stored on the item rather than computed each time; public for the debug editor
	public void applyDerived( Item item ){
		if (item instanceof Wand){
			Wand w = (Wand) item;
			w.maxCharges = Math.min( w.initialCharges() + w.level() + chargeBonus( w ), chargeCap() );
			w.curCharges = Math.min( w.curCharges, w.maxCharges );
		}
		empower( item );
	}

	/**
	 * A legendary or exotic item empowers whatever magic it carries, and keeps doing so as
	 * it is masterworked. Called again every time the magic itself changes - a fresh
	 * enchantment is born at level 0, so without this a re-enchant would quietly throw the
	 * tier's empowerment away. Never lowers what the player already built.
	 */
	public static void empower( Item item ){
		Quality q = of( item );
		if (q == null || q.rarity.ordinal() < Rarity.LEGENDARY.ordinal()) return;
		//one level of empowerment per whole rarity step of potency, counted down, so the
		//channel keeps growing as the item is masterworked instead of stalling at the top
		//tier: legendary 1 -> 2, exotic 2 -> 3, each gaining its level late in the work.
		//The small offset is there so an exact 3.0 cannot fall to 2 on float wobble
		int lv = (int) (q.potency() / Rarity.RARE.potency() + 0.1f);
		if (lv <= 0) return;
		//both slots: a weapon can carry two enchantments and armor two glyphs
		if (item instanceof Weapon){
			for (Weapon.Enchantment e : ((Weapon) item).enchantments()){
				if (e != null) e.level( Math.max( e.level(), lv ) );
			}
		} else if (item instanceof Armor){
			Armor a = (Armor) item;
			if (a.glyph != null) a.glyph.level( Math.max( a.glyph.level(), lv ) );
			if (a.glyph2 != null) a.glyph2.level( Math.max( a.glyph2.level(), lv ) );
		}
	}

	/**
	 * Fired exactly once, when the item is first rolled. A legendary or exotic weapon or
	 * armor comes out of the world already carrying magic - which is why this cannot live
	 * in applyDerived, that also runs on every masterwork step and every reroll, and would
	 * hand out a fresh enchantment each time or overwrite one the player chose.
	 */
	private void onGenerated( Item item ){
		if (granted || rarity.ordinal() < Rarity.LEGENDARY.ordinal()) return;
		granted = true;
		if (item instanceof Weapon && ((Weapon) item).enchantment == null){
			((Weapon) item).enchant();
		} else if (item instanceof Armor && ((Armor) item).glyph == null){
			((Armor) item).inscribe();
		}
	}

	// ---------------------------------------------------------------- line values

	/** effective roll of a line: its rolled share of the maximum, times the masterwork's
	 *  {@link #lineScale} (so a fully masterworked line can read twice its maximum) */
	public float roll( RarityLine kind ){
		int i = lines.indexOf( kind );
		if (i < 0) return 0f;
		return rolls.get( i ) * lineScale();
	}

	/** what masterwork does to every bonus line: 15% more a step (x1.15 to x1.6 after four),
	 *  doubled when full */
	public float lineScale(){
		return fullyMasterworked() ? 2f : 1f + 0.15f * masterwork;
	}

	public boolean has( RarityLine kind ){
		return lines.contains( kind );
	}

	/** the bonus of a line: multiplier fraction, degrees for WARMTH, 0 when absent */
	public float value( RarityLine kind ){
		return has( kind ) ? kind.max() * roll( kind ) : 0f;
	}

	public static float value( Item item, RarityLine kind ){
		Quality q = of( item );
		return q == null ? 0f : q.value( kind );
	}

	/** the tier's own share of the item's number, grown by masterworking it */
	public float potency(){
		return rarity.potency() * (1f + 0.5f * masterwork / (float) MASTERWORK_MAX);
	}

	public static float potency( Item item ){
		Quality q = of( item );
		return q == null || !hasPotency( item ) ? 0f : q.potency();
	}

	/**
	 * A base stat of the item with its rarity worked in: the rolled line and the tier's
	 * potency add inside one parenthesis, so the two can never multiply each other. This
	 * is the ONLY place the rarity multiplier is written.
	 */
	public static int scale( Item item, RarityLine kind, int base ){
		Quality q = of( item );
		if (q == null || !hasPotency( item )) return base;
		return Math.round( base * (1f + q.value( kind ) + q.potency()) );
	}

	/** how many points of strength requirement a rolled STR_REQ line takes off: 1, or 2 once
	 *  its roll is at least a half. Not a share of the requirement: the line shows the cut, and
	 *  a share of the requirement after the cut (or before a weapon's own extra) could round to
	 *  another number than the one taken off, or to 0 */
	public static int strReqStep( float value ){
		if (value <= 0) return 0;
		return value >= RarityLine.STR_REQ.max() / 2f ? 2 : 1;
	}

	public static int strReqBonus( Item item ){
		Quality q = of( item );
		return q == null ? 0 : strReqStep( q.value( RarityLine.STR_REQ ) );
	}

	/** how many whole charges a rolled CHARGES line adds */
	public static int chargeStep( int initial, float value ){
		return Math.round( initial * value );
	}

	public int chargeBonus( Wand w ){
		return chargeStep( w.initialCharges(), value( RarityLine.CHARGES ) );
	}

	/** the hard ceiling on a wand's bar: an exotic wand simply holds more */
	public int chargeCap(){
		return 10 + Math.round( 10f * second() );
	}

	/**
	 * Exotic alone changes the shape of the combat maths rather than the size of a number:
	 * half its potency again, spent on cutting through armor, on turning blows aside, or on
	 * a wand's bar. It joins the channels that are already capped once, so three sources can
	 * never erase a hit.
	 */
	public float second(){
		return rarity == Rarity.EXOTIC ? 0.5f * potency() : 0f;
	}

	public static float second( Item item ){
		Quality q = of( item );
		return q == null || !hasPotency( item ) ? 0f : q.second();
	}

	// ---------------------------------------------------------------- masterwork

	public boolean fullyMasterworked(){
		return masterwork >= MASTERWORK_MAX;
	}

	public int masterworkCap(){
		return rarity.ordinal() >= Rarity.LEGENDARY.ordinal() ? MASTERWORK_MAX : MASTERWORK_CAP_LOW;
	}

	/** cores the next step costs, 0 when no step is possible */
	public int masterworkCost(){
		return masterwork >= masterworkCap() ? 0 : 1;
	}

	/** one masterwork step: lines rise through {@link #roll}; a common base gains a line
	 *  on its first step so masterworking is never empty */
	public void masterworkStep( Item item ){
		if (masterworkCost() == 0) return;
		masterwork++;
		if (lines.isEmpty()){
			ArrayList<RarityLine> pool = fallbackPool( item );
			if (!pool.isEmpty()){
				lines.add( Random.element( pool ) );
				rolls.add( 0f );
			}
		}
		applyDerived( item );
	}

	/** a new rarity with fresh lines; the type stays, and the masterwork too as far
	 *  as the new tier allows (a rare base stops at nine) */
	public void rerollRarity( Item item, Rarity to ){
		if (!hasRarity( item )) return;
		rarity = to;
		rollLines( item );
		//a fresh vestige for a legendary or exotic weapon or armor, none below (rollLines cleared the line)
		vestige = null;
		rollVestige( item );
		masterwork = Math.min( masterwork, masterworkCap() );
		if (masterwork > 0 && lines.isEmpty()){
			//a masterworked common base keeps the line its first step gave it
			ArrayList<RarityLine> pool = fallbackPool( item );
			if (!pool.isEmpty()){
				lines.add( Random.element( pool ) );
				rolls.add( 0f );
			}
		}
		applyDerived( item );
	}

	// ---------------------------------------------------------------- the tempering forge

	/** what a good temper would do to this item: 1 a line driven to its maximum, 2 a free
	 *  masterwork step (every line already at its best), 0 nothing left to gain */
	public int temperGain(){
		for (float r : rolls) if (r < 1f) return 1;
		return masterworkCost() > 0 ? 2 : 0;
	}

	/** A clean quench on the Tempering Forge: the weakest rolled line goes to its maximum,
	 *  or, with nothing left to raise, the item takes a masterwork step no core paid for.
	 *  Returns what {@link #temperGain} promised. */
	public int temper( Item item ){
		int gain = temperGain();
		if (gain == 1){
			int weakest = 0;
			for (int i = 1; i < rolls.size(); i++){
				if (rolls.get( i ) < rolls.get( weakest )) weakest = i;
			}
			rolls.set( weakest, 1f );
			applyDerived( item );
		} else if (gain == 2){
			masterworkStep( item );
		}
		return gain;
	}

	/** Left on the forge too long: the last line is burnt out of the metal. False when
	 *  there was no line to lose (a common piece only blackens). */
	public boolean scorch( Item item ){
		if (lines.isEmpty()) return false;
		lines.remove( lines.size() - 1 );
		rolls.remove( rolls.size() - 1 );
		applyDerived( item );
		return true;
	}

	public boolean shiftType(){
		if (type.alpha()) return false;
		type = type.next();
		return true;
	}

	// ---------------------------------------------------------------- the vestige

	//never a vestige: an every-hit store (Kinetic), a durability of its own (Crystal), a proc
	//that does nothing (Projecting), an upgrade engine (Ancient), a random other enchantment
	//(Unstable), and the two that show another's name (Horror reads "eldritch", Poison "venomous")
	private static final Class<?>[] VESTIGE_EXCLUDED = {
			Kinetic.class, Crystal.class, Projecting.class, Ancient.class, Unstable.class,
			Horror.class, Poison.class
	};

	//enchantments that count as one for every no-duplicate rule (Luck and Slashing run the same code)
	private static final Class<?>[][] KIN = {
			{ Venomous.class, Poison.class }, { Eldritch.class, Horror.class }, { Luck.class, Slashing.class }
	};

	/** whether two enchantments are the same class or kin */
	public static boolean kin( Weapon.Enchantment a, Weapon.Enchantment b ){
		return a != null && b != null && kin( a.getClass(), b.getClass() );
	}

	private static boolean kin( Class<?> a, Class<?> b ){
		if (a == b) return true;
		for (Class<?>[] group : KIN){
			if (Arrays.asList( group ).contains( a ) && Arrays.asList( group ).contains( b )) return true;
		}
		return false;
	}

	//never an armor's vestige: the glyphs whose proc does nothing, as they act only where the game
	//asks whether the armor carries them (Obfuscation, Swiftness, Brimstone, Camouflage, Flow,
	//AntiMagic, Afterimage), and Stone, whose proc is half a trade: carrying it takes the wearer's
	//evasion and the proc pays that back as blows softened, so alone it would be the payment
	//without the price
	private static final Class<?>[] GLYPH_VESTIGE_EXCLUDED = {
			Obfuscation.class, Swiftness.class, Brimstone.class, Camouflage.class, Flow.class,
			AntiMagic.class, Afterimage.class, Stone.class
	};

	//one of the good pools (0 common, 1 uncommon, 2 rare) of enchantments, or of glyphs, less what
	//can never be a vestige
	private static ArrayList<Class<?>> vestigeBucket( boolean glyphs, int bucket ){
		Class<?>[] pool = glyphs
				? (bucket == 0 ? Armor.Glyph.common : bucket == 1 ? Armor.Glyph.uncommon : Armor.Glyph.rare)
				: (bucket == 0 ? Weapon.Enchantment.common : bucket == 1 ? Weapon.Enchantment.uncommon : Weapon.Enchantment.rare);
		ArrayList<Class<?>> out = new ArrayList<>( Arrays.asList( pool ) );
		out.removeAll( Arrays.asList( glyphs ? GLYPH_VESTIGE_EXCLUDED : VESTIGE_EXCLUDED ) );
		return out;
	}

	private static ArrayList<Class<?>> vestigePool( boolean glyphs ){
		ArrayList<Class<?>> out = new ArrayList<>();
		for (int b = 0; b < 3; b++) out.addAll( vestigeBucket( glyphs, b ) );
		return out;
	}

	/** every enchantment a weapon's vestige can be, common ones first */
	public static ArrayList<Class<?>> vestigePool(){
		return vestigePool( false );
	}

	/** every glyph an armor's vestige can be, common ones first */
	public static ArrayList<Class<?>> glyphVestigePool(){
		return vestigePool( true );
	}

	//the good pools' 50/40/10 bucket weights (the glyphs' are the same), uniform inside a bucket,
	//never the item's own enchantments or glyphs or their kin; a bucket that empties drops out of
	//the draw (Enchantment.random and Glyph.random would fall back to an unfiltered pick)
	private static Bundlable pickVestige( Item item ){
		boolean glyphs = item instanceof Armor;
		Object[] own = glyphs ? ((Armor) item).glyphs() : ((Weapon) item).enchantments();
		float[] chances = glyphs ? Armor.Glyph.typeChances : Weapon.Enchantment.typeChances;
		ArrayList<ArrayList<Class<?>>> buckets = new ArrayList<>();
		float[] weights = new float[3];
		for (int b = 0; b < 3; b++){
			ArrayList<Class<?>> bucket = new ArrayList<>();
			for (Class<?> c : vestigeBucket( glyphs, b )){
				boolean taken = false;
				for (Object m : own) taken = taken || kin( c, m.getClass() );
				if (!taken) bucket.add( c );
			}
			buckets.add( bucket );
			weights[b] = bucket.isEmpty() ? 0f : chances[b];
		}
		return (Bundlable) Reflection.newInstance( Random.element( buckets.get( Random.chances( weights ) ) ) );
	}

	//a legendary or exotic weapon's or armor's vestige, at index 0 beside the tier's own lines and
	//rolled like them, so a scorch burns it last; nothing for any other item or tier
	private void rollVestige( Item item ){
		if (!(item instanceof Weapon || item instanceof Armor) || rarity.ordinal() < Rarity.LEGENDARY.ordinal()) return;
		vestige = pickVestige( item );
		lines.add( 0, RarityLine.VESTIGE );
		rolls.add( 0, rarity.rollLine() );
	}

	/** A legendary or exotic weapon or armor from a save older than its kind's vestiges gains its
	 *  one vestige on first load. Seeded from the quality itself, so the game's own sequence is
	 *  untouched and a thrown stack and a child saved apart gain the same one, and still merge. */
	public static void settleVestige( Item item ){
		Quality q = of( item );
		if (q == null) return;
		if (item instanceof Armor){
			if (q.glyphVestigeRolled) return;
			q.glyphVestigeRolled = true;
		} else {
			if (q.vestigeRolled) return;
			q.vestigeRolled = true;
		}
		if (q.rarity.ordinal() < Rarity.LEGENDARY.ordinal()) return;
		long seed = item.getClass().getName().hashCode();
		seed = seed * 31 + q.rarity.ordinal();
		seed = seed * 31 + q.type.ordinal();
		seed = seed * 31 + q.masterwork;
		for (float r : q.rolls) seed = seed * 31 + Float.floatToIntBits( r );
		Random.pushGenerator( seed );
		try {
			q.rollVestige( item );
		} finally {
			Random.popGenerator();
		}
	}

	/** the item's vestige of either kind (a weapon's enchantment, an armor's glyph), or null when it
	 *  has none */
	public static Bundlable vestigeMagic( Item item ){
		Quality q = of( item );
		return q != null && q.has( RarityLine.VESTIGE ) ? q.vestige : null;
	}

	/** the item's vestigial enchantment, or null when it has none */
	public static Weapon.Enchantment vestigeOf( Item item ){
		Bundlable v = vestigeMagic( item );
		return v instanceof Weapon.Enchantment ? (Weapon.Enchantment) v : null;
	}

	/** the item's vestigial glyph, or null when it has none */
	public static Armor.Glyph glyphVestigeOf( Item item ){
		Bundlable v = vestigeMagic( item );
		return v instanceof Armor.Glyph ? (Armor.Glyph) v : null;
	}

	/** a vestige's name, either kind ("blazing enchantment", "glyph of thorns") */
	public static String vestigeName( Bundlable v ){
		return v instanceof Armor.Glyph ? ((Armor.Glyph) v).name() : ((Weapon.Enchantment) v).name();
	}

	//what a vestige does, either kind
	private static String vestigeDesc( Bundlable v ){
		return v instanceof Armor.Glyph ? ((Armor.Glyph) v).desc() : ((Weapon.Enchantment) v).desc();
	}

	/** whether an enchantment proccing for this weapon (or the shot it fired) is the vestige */
	public static boolean isVestige( Weapon weapon, Weapon.Enchantment e ){
		return e != null && e == vestigeOf( weapon.magicSource() );
	}

	/** whether a glyph proccing for this armor is the vestige */
	public static boolean isVestige( Armor armor, Armor.Glyph g ){
		return g != null && g == glyphVestigeOf( armor );
	}

	//a vestige is silent beside the same enchantment or its kin, in a slot or from Trinity
	static boolean vestigeSilent( Weapon src, Weapon.Enchantment v, Weapon.Enchantment trinity ){
		return kin( v, src.enchantment ) || kin( v, src.enchantment2 ) || kin( v, trinity );
	}

	//an armor's beside the same glyph (glyphs have no kin), in a slot or from Trinity
	static boolean vestigeSilent( Armor a, Armor.Glyph v, Armor.Glyph trinity ){
		return sameGlyph( v, a.glyph ) || sameGlyph( v, a.glyph2 ) || sameGlyph( v, trinity );
	}

	private static boolean sameGlyph( Armor.Glyph a, Armor.Glyph b ){
		return a != null && b != null && a.getClass() == b.getClass();
	}

	/** The vestige to proc on a landed blow: null when the weapon has none, when it is silent or
	 *  when its gate fails. One outer gate at the line's value before the enchantment's own
	 *  proc, so it fires exactly that share as often as the bare enchantment would. */
	public static Weapon.Enchantment vestigeFor( Weapon src, Weapon.Enchantment trinity ){
		Weapon.Enchantment v = vestigeOf( src );
		if (v == null || vestigeSilent( src, v, trinity )) return null;
		return vestigeGate( src ) ? v : null;
	}

	/** The vestige to proc on a blow the armor takes, by the same rules and the same gate: it
	 *  answers that share as often as the bare glyph would. */
	public static Armor.Glyph vestigeFor( Armor armor, Armor.Glyph trinity ){
		Armor.Glyph v = glyphVestigeOf( armor );
		if (v == null || vestigeSilent( armor, v, trinity )) return null;
		return vestigeGate( armor ) ? v : null;
	}

	//the one outer gate at the VESTIGE line's value, drawn only when there is a chance
	private static boolean vestigeGate( Item item ){
		float k = value( item, RarityLine.VESTIGE );
		return k > 0f && Random.Float() < k;
	}

	// ---------------------------------------------------------------- perks

	/** the weapon a hit counts for: a launcher's shot counts as its launcher (its magicSource);
	 *  any other projectile that carries no type of its own as the hero's typed launcher; the
	 *  item itself otherwise */
	public static Weapon source( Weapon w, Char owner ){
		Weapon launcher = w.magicSource();
		if (launcher != w) return launcher;
		if (of( w ) != null || !(w instanceof MissileWeapon) || !(owner instanceof Hero)) return w;
		Item held = ((Hero) owner).belongings.weapon();
		return held instanceof Weapon && held != w && of( held ) != null
				&& (WeaponPerk.of( held ) == WeaponPerk.SHOT || WeaponPerk.of( held ) == WeaponPerk.GUN) ? (Weapon) held : w;
	}

	/** the weapon's perk, its edge lines (see {@link #edge}) and its CRIT and LIFESTEAL lines on a
	 *  landed blow; see {@link WeaponPerk} */
	public static int weaponHit( Weapon weapon, Char attacker, Char defender, int damage ){
		Weapon w = source( weapon, attacker );
		Quality q = of( w );
		if (q == null) return damage;
		q.critBlow = false;
		//the damage ledger: what the perk did (every perk multiplies), the edge lines, the crit
		xyz.gabriwar.warpedpixeldungeon.actors.ledger.DamageLedger.push( defender, xyz.gabriwar.warpedpixeldungeon.actors.ledger.Step.ATTACKER, xyz.gabriwar.warpedpixeldungeon.actors.ledger.Step.MUL, WeaponPerk.of( w ) );
		damage = xyz.gabriwar.warpedpixeldungeon.actors.ledger.DamageLedger.pop( WeaponPerk.of( w ).onHit( q, w, attacker, defender, damage ) );
		float e = edge( q, attacker, defender );
		if (e != 0f) damage = xyz.gabriwar.warpedpixeldungeon.actors.ledger.DamageLedger.mulRound( defender, xyz.gabriwar.warpedpixeldungeon.actors.ledger.Step.ATTACKER, xyz.gabriwar.warpedpixeldungeon.actors.ledger.DamageLedger.EDGE.of( edgeNames( q, attacker, defender ) ), damage, 1f + e, Math.round( damage * (1f + e) ) );
		//one crit per blow: the line only rolls when no perk has critted already
		if (!q.critBlow && defender != null){
			float crit = q.value( RarityLine.CRIT );
			if (crit > 0f && Random.Float() < crit){
				damage = xyz.gabriwar.warpedpixeldungeon.actors.ledger.DamageLedger.mulRound( defender, xyz.gabriwar.warpedpixeldungeon.actors.ledger.Step.ATTACKER, xyz.gabriwar.warpedpixeldungeon.actors.ledger.DamageLedger.CRIT, damage, 1.5f, Math.round( damage * 1.5f ) );
				WeaponPerk.crit( q, attacker, defender );
			}
		}
		q.drain( attacker, defender, damage );
		return damage;
	}

	//LIFESTEAL: a third of the blow back as health, by Vampiric's rules on who can be drained,
	//never more than the target has left (overkill heals nothing) or the attacker is missing
	private void drain( Char attacker, Char defender, int damage ){
		float chance = value( RarityLine.LIFESTEAL );
		if (chance <= 0f || attacker == null || defender == null || attacker.alignment == defender.alignment
				|| (defender.alignment == Char.Alignment.NEUTRAL && !(defender instanceof Mimic))) return;
		if (Random.Float() >= chance) return;
		int heal = Math.min( chanceRound( damage / 3f ), Math.min( defender.HP, attacker.HT - attacker.HP ) );
		if (heal > 0 && attacker.isAlive()){
			attacker.HP += heal;
			if (attacker.sprite != null) attacker.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( heal ), FloatingText.HEALING );
			xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx.lifesteal( attacker, defender );
		}
	}

	/** The lines that lift a blow by who is fighting where, as one sum: SLAYER against its family,
	 *  EXECUTIONER against a foe under half health, MOONLIT by the hour, STORMBORN under open falling
	 *  weather, DEFIANT while the attacker is under half health. Read before the blow lands, so the
	 *  health is the health before it. Draws nothing; each line that lifts the blow puts its gold
	 *  chip on the open blow's card (LineLooks.chipOf; none for the noon's loss), no picture. */
	public static float edge( Quality q, Char attacker, Char defender ){
		float e = 0f;
		if (defender != null){
			float slay = q.value( RarityLine.SLAYER );
			if (slay > 0f && q.slayer != null && q.slayer.has( defender )){
				e += slay;
				edgeChip( defender, RarityLine.SLAYER, q.slayer );
			}
			float exe = q.value( RarityLine.EXECUTIONER );
			if (exe > 0f && defender.HP * 2 < defender.HT){
				e += exe;
				edgeChip( defender, RarityLine.EXECUTIONER, null );
			}
		}
		float moon = q.value( RarityLine.MOONLIT );
		if (moon > 0f){
			float f = moonFactor();
			e += moon * f;
			if (f > 0f) edgeChip( defender, RarityLine.MOONLIT, null );
		}
		if (attacker != null){
			float storm = q.value( RarityLine.STORMBORN );
			if (storm > 0f && weatherOn( attacker )){
				e += storm;
				edgeChip( defender, RarityLine.STORMBORN, null );
			}
			float defiant = q.value( RarityLine.DEFIANT );
			if (defiant > 0f && attacker.HP * 2 < attacker.HT){
				e += defiant;
				edgeChip( defender, RarityLine.DEFIANT, null );
			}
		}
		return e;
	}

	/** The names of the lines {@link #edge} counts on this blow, for the damage ledger (reads only:
	 *  the same tests, no chips, no draws). */
	public static String edgeNames( Quality q, Char attacker, Char defender ){
		StringBuilder sb = new StringBuilder();
		if (defender != null){
			if (q.value( RarityLine.SLAYER ) > 0f && q.slayer != null && q.slayer.has( defender )) name( sb, RarityLine.SLAYER );
			if (q.value( RarityLine.EXECUTIONER ) > 0f && defender.HP * 2 < defender.HT) name( sb, RarityLine.EXECUTIONER );
		}
		if (q.value( RarityLine.MOONLIT ) > 0f && moonFactor() != 0f) name( sb, RarityLine.MOONLIT );
		if (attacker != null){
			if (q.value( RarityLine.STORMBORN ) > 0f && weatherOn( attacker )) name( sb, RarityLine.STORMBORN );
			if (q.value( RarityLine.DEFIANT ) > 0f && attacker.HP * 2 < attacker.HT) name( sb, RarityLine.DEFIANT );
		}
		return sb.toString();
	}

	private static void name( StringBuilder sb, RarityLine line ){
		if (sb.length() > 0) sb.append( ", " );
		sb.append( Messages.capitalize( line.shortName() ) );
	}

	//a line's gold chip on the struck one's card in the open blow (nothing outside a blow)
	private static void edgeChip( Char defender, RarityLine line, SlayerFamily family ){
		int chip = xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.LineLooks.chipOf( line, family );
		if (chip >= 0) xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow.chip( defender, chip, 0 );
	}

	/** STEADY's moment: the hero's shot or throw (as Hero.attackSkill reckons it) carries the line on
	 *  its weapon (its launcher's for a shot) and the wind is taking some of its aim, which the line
	 *  gave back. Hero.attackProc puts its gold chip on the struck one's card then. Draws nothing */
	public static boolean steadied( Hero hero, Char target ){
		if (hero == null || target == null || hero.belongings == null) return false;
		Item w = hero.belongings.attackingWeapon();
		if (!(w instanceof MissileWeapon) || value( source( (Weapon) w, hero ), RarityLine.STEADY ) <= 0f) return false;
		xyz.gabriwar.warpedpixeldungeon.actors.buffs.Windswept wind = hero.buff( xyz.gabriwar.warpedpixeldungeon.actors.buffs.Windswept.class );
		return (wind != null && wind.rangedAccuracyFactor() < 1f)
				|| xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager.windProjectileFactor( hero.pos, target.pos, false ) < 1f;
	}

	/** MOONLIT's share of its value by the game's own day: all of it at night, half at dusk and dawn,
	 *  a third of it lost around noon (the middle of the day), none the rest of the day */
	public static float moonFactor(){
		switch (xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.phase()){
			case NIGHT:
				return 1f;
			case DUSK: case DAWN:
				return 0.5f;
			default:
				float p = xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.phaseProgress();
				return p >= 0.35f && p <= 0.65f ? -1f / 3f : 0f;
		}
	}

	/** whether rain, snow or hail falls on the character: open sky, no roof and no room. Asks the
	 *  roof without a thatch's leak roll (Shelter.precipitationReaches), so it draws nothing */
	public static boolean weatherOn( Char ch ){
		return ch != null && Dungeon.level != null
				&& xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager.localPrecipRate() > 0f
				&& xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager.localPrecipType() != xyz.gabriwar.warpedpixeldungeon.actors.PrecipType.NONE
				&& !xyz.gabriwar.warpedpixeldungeon.homestead.Shelter.covered( Dungeon.level, ch.pos );
	}

	public static boolean sureHit( Weapon weapon, Char owner, Char target ){
		if (!(owner instanceof Hero)) return false;
		//an alpha Insight artifact promises the swing after a miss, whatever is held
		if (GearPerk.sureNext( (Hero) owner )) return true;
		Weapon w = source( weapon, owner );
		Quality q = of( w );
		return q != null && WeaponPerk.of( w ).sureHit( q, owner, target );
	}

	public static float perkSpeed( Weapon weapon ){
		Quality q = of( weapon );
		return q == null ? 1f : WeaponPerk.of( weapon ).speedFactor( q, weapon );
	}

	// ---------------------------------------------------------------- damage channels

	/** every rarity fraction that softens a blow is summed here and capped once: the
	 *  weapon's Guard, the armor's Bulwark, the artifact's Warden. They are taken AFTER
	 *  armor, where a fraction is worth what it says, and the cap is what keeps three
	 *  sources from erasing a hit outright */
	public static final float MITIGATE_CAP = 0.30f;

	/** the same for armor the hero's blows ignore: Crush, Ambush, a long Marksman shot
	 *  and an alpha Striker ring, summed and capped as one channel */
	public static final float PIERCE_CAP = 0.60f;

	public static float mitigation( Hero hero ){
		if (hero == null || hero.belongings == null) return 0f;
		float f = GearPerk.heroBonus( hero, GearPerk.Stat.MITIGATE );
		Item w = hero.belongings.weapon();
		Quality q = of( w );
		if (q != null && w instanceof Weapon) f += WeaponPerk.of( w ).guardFraction( q, (Weapon) w );
		//an exotic armor turns blows aside by its tier alone
		f += second( hero.belongings.armor() );
		return Math.min( MITIGATE_CAP, Math.max( 0f, f ) );
	}

	/** the blow after the rarity mitigation channel; only blows from a character are
	 *  softened, the way worn armor already ignores fire, hunger and falls */
	public static float mitigate( Hero hero, float damage, Object src ){
		if (!(src instanceof Char) || damage <= 0 || hero == null || hero.belongings == null) return damage;
		Item w = hero.belongings.weapon();
		Quality q = of( w );
		if (q != null && w instanceof Weapon){
			damage = xyz.gabriwar.warpedpixeldungeon.actors.ledger.DamageLedger.to( hero, xyz.gabriwar.warpedpixeldungeon.actors.ledger.Step.DEFENDER, xyz.gabriwar.warpedpixeldungeon.actors.ledger.DamageLedger.AUTO, WeaponPerk.of( w ), damage, WeaponPerk.of( w ).riposte( q, (Weapon) w, hero, damage, src ) );
			if (damage <= 0) return xyz.gabriwar.warpedpixeldungeon.actors.ledger.DamageLedger.to( hero, xyz.gabriwar.warpedpixeldungeon.actors.ledger.Step.DEFENDER, xyz.gabriwar.warpedpixeldungeon.actors.ledger.Step.SET, xyz.gabriwar.warpedpixeldungeon.actors.ledger.DamageLedger.NO_NEGATIVE, damage, 0f );
		}
		float f = mitigation( hero );
		return f <= 0f ? damage : xyz.gabriwar.warpedpixeldungeon.actors.ledger.DamageLedger.mul( hero, xyz.gabriwar.warpedpixeldungeon.actors.ledger.Step.DEFENDER, xyz.gabriwar.warpedpixeldungeon.actors.ledger.DamageLedger.GEAR_PROTECTION, damage, 1f - f );
	}

	/** the share of the defender's armor roll this attacker's rarity effects ignore */
	public static float drPierce( Char attacker, Char defender ){
		if (!(attacker instanceof Hero) || defender == null) return 0f;
		Hero hero = (Hero) attacker;
		if (hero.belongings == null) return 0f;
		float f = GearPerk.heroBonus( hero, GearPerk.Stat.PIERCE );
		Item w = hero.belongings.attackingWeapon();
		//a shot pierces by its launcher
		if (w instanceof Weapon) w = source( (Weapon) w, hero );
		Quality q = of( w );
		if (q != null && w instanceof Weapon) f += WeaponPerk.of( w ).pierce( q, (Weapon) w, hero, defender );
		//an exotic weapon cuts through armor by its tier alone
		f += second( w );
		return Math.min( PIERCE_CAP, Math.max( 0f, f ) );
	}

	//a reflected blow must never reflect back: one guard for every thorn in the system
	private static boolean reflecting = false;

	/** damage handed back to whoever struck: Guard's riposte and Warden's thorns */
	public static void reflect( Char attacker, Char target, int amount ){
		if (reflecting || amount <= 0 || attacker == null || target == null || !attacker.isAlive()) return;
		reflecting = true;
		try {
			//its number is raised by the damage itself (none when the attacker is immune)
			attacker.damage( amount, target );
		} finally {
			reflecting = false;
		}
	}

	/** the one tile of reach in the system: an alpha Reach weapon, and nothing else */
	public static int reachBonus( Item weapon, Char owner ){
		Quality q = of( weapon );
		return q != null && q.type.alpha() && weapon instanceof Weapon
				&& WeaponPerk.of( weapon ) == WeaponPerk.REACH ? 1 : 0;
	}

	/** the whole tiles a RANGE line of this value adds to a reach of `base` tiles: at least one */
	public static int rangeStep( int base, float value ){
		return value <= 0f ? 0 : Math.max( 1, Math.round( base * value ) );
	}

	/** how much further a flamethrower's cone or a laser's beam reaches than its own `base` */
	public static int rangeBonus( Gun gun, int base ){
		return rangeStep( base, value( gun, RarityLine.RANGE ) );
	}

	/** the chance a launcher's reload takes no time, capped at a half: the RELOAD line and a
	 *  Gunner's 10% a type rank */
	public static float freeReload( Weapon launcher ){
		float f = value( launcher, RarityLine.RELOAD );
		Quality q = of( launcher );
		if (q != null && WeaponPerk.of( launcher ) == WeaponPerk.GUN) f += 0.10f * q.type.rank;
		return Math.min( 0.5f, f );
	}

	/** the chance a launcher's shot spends no ammunition, capped at a half: the AMMO line and
	 *  an alpha Gunner's one shot in five */
	public static float spareAmmo( Weapon launcher ){
		float f = value( launcher, RarityLine.AMMO );
		Quality q = of( launcher );
		if (q != null && q.type.alpha() && WeaponPerk.of( launcher ) == WeaponPerk.GUN) f += 0.20f;
		return Math.min( 0.5f, f );
	}

	/** rolls a free reload, drawing only when there is a chance of one; says so over the shooter */
	public static boolean rollFreeReload( Weapon launcher, Char shooter ){
		float chance = freeReload( launcher );
		if (chance <= 0f || Random.Float() >= chance) return false;
		if (shooter != null && shooter.sprite != null){
			xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow.icon( shooter, xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.RELOAD, 0 );
		}
		xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx.freeReload( shooter );
		return true;
	}

	/** rolls a spared round, drawing only when there is a chance of one */
	public static boolean rollSpareAmmo( Weapon launcher, Char shooter ){
		float chance = spareAmmo( launcher );
		if (chance <= 0f || Random.Float() >= chance) return false;
		xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx.sparedRound( launcher, shooter );
		return true;
	}

	// ---------------------------------------------------------------- type perks

	/** Ambush: how many tiles a held light blade senses enemies through walls, its type rank;
	 *  0 for any other weapon or an untyped one */
	public static int senseRange( Hero h ){
		Item w = h == null || h.belongings == null ? null : h.belongings.weapon();
		Quality q = of( w );
		return q != null && WeaponPerk.of( w ) == WeaponPerk.KEEN ? q.type.rank : 0;
	}

	/** Reaper: the chance a crop or a tuft of tall grass cut gives its seed twice, 20% a type
	 *  rank of the best scythe carried (a sickle or a war scythe, held or in the pack) */
	public static float seedChance( Hero h ){
		if (h == null || h.belongings == null) return 0f;
		int best = 0;
		ArrayList<Item> scythes = new ArrayList<>( h.belongings.getAllItems( Sickle.class ) );
		scythes.addAll( h.belongings.getAllItems( WarScythe.class ) );
		for (Item s : scythes){
			Quality q = of( s );
			if (q != null) best = Math.max( best, q.type.rank );
		}
		return 0.20f * best;
	}

	/** rolls a second seed from what was cut on this cell, drawing only when there is a chance of one */
	public static boolean secondSeed( Hero h, int cell ){
		float chance = seedChance( h );
		if (chance <= 0f || Random.Float() >= chance) return false;
		xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx.secondSeed( h, cell );
		return true;
	}

	/** Woodsman and Quarry at work: the strokes a job of `base` strokes takes with this tool, 20%
	 *  fewer a type rank of a typed axe, hammer, pick or shovel (never under one); `base` for any
	 *  other tool or an untyped one */
	public static int workStrokes( Item tool, int base ){
		int r = workRank( tool );
		return r == 0 ? base : Math.max( 1, Math.round( base * (1f - 0.20f * r) ) );
	}

	/** rolls the one more log, stone or ore a typed Woodsman's or Quarry's tool brings out, 15% a
	 *  type rank; draws only when there is a chance of one */
	public static boolean extraYield( Item tool ){
		float chance = 0.15f * workRank( tool );
		return chance > 0f && Random.Float() < chance;
	}

	//the type rank of a tool whose perk is Woodsman or Quarry, 0 for any other or an untyped one
	private static int workRank( Item tool ){
		Quality q = of( tool );
		if (q == null) return 0;
		WeaponPerk p = WeaponPerk.of( tool );
		return p == WeaponPerk.WOODSMAN || p == WeaponPerk.QUARRY ? q.type.rank : 0;
	}

	//a splash never splashes again: one guard for the sweep, the shockwave and the rival's sweep
	private static boolean splashing = false;

	/** a share of a blow dealt to a character beside it, with no armour roll (as the large
	 *  sword's splash), credited to the attacker so a kill counts as theirs */
	public static void splash( Char attacker, Char target, int amount ){
		if (splashing || amount < 1 || target == null || !target.isAlive()) return;
		splashing = true;
		try {
			target.damage( amount, attacker );
		} finally {
			splashing = false;
		}
	}

	/** a fractional per-hit amount as a whole number that averages to it: 2.3 is 2, or 3
	 *  three times in ten */
	public static int chanceRound( float x ){
		if (x <= 0f) return 0;
		int i = (int) x;
		return Random.Float() < x - i ? i + 1 : i;
	}

	/** an independent copy, for items split off or rebuilt from a typed item */
	public Quality copy(){
		Bundle b = new Bundle();
		storeInBundle( b );
		Quality q = new Quality();
		q.restoreFromBundle( b );
		return q;
	}

	/** whether two items carry the same quality, so a stack never merges different rolls */
	public static boolean sameQuality( Item a, Item b ){
		Quality qa = of( a ), qb = of( b );
		if (qa == null || qb == null) return qa == qb;
		Bundlable va = vestigeMagic( a ), vb = vestigeMagic( b );
		return qa.rarity == qb.rarity && qa.type == qb.type && qa.masterwork == qb.masterwork
				&& qa.lines.equals( qb.lines ) && qa.rolls.equals( qb.rolls )
				&& (va == null ? vb == null : vb != null && va.getClass() == vb.getClass())
				//the lines are equal, so both hold SLAYER or neither does
				&& (!qa.has( RarityLine.SLAYER ) || qa.slayer == qb.slayer);
	}

	/** missile Dart: a tipped dart's hit may keep its tip */
	public static boolean keepTip( MissileWeapon missile ){
		Quality q = of( missile );
		return q != null && missile instanceof xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.TippedDart
				&& WeaponPerk.of( missile ) == WeaponPerk.DART && Random.Float() < 0.15f * q.type.rank;
	}

	/** true once: the hero's coming attack takes no time */
	public static boolean freeAttack( Item weapon ){
		Quality q = of( weapon );
		return q != null && weapon instanceof Weapon && WeaponPerk.of( weapon ).consumeFreeAttack( q );
	}

	/** missile Throw: 5/10/15% of throws deal max damage */
	public static int throwRoll( MissileWeapon missile, Char owner, int damage, int max ){
		Quality q = of( missile );
		if (q == null || !(owner instanceof Hero)) return damage;
		return WeaponPerk.of( missile ).throwRoll( q, damage, max );
	}

	// ---------------------------------------------------------------- kills

	/** called from Mob.die for every enemy: masterwork perks and the core / shifter drops */
	public static void onEnemyKilled( Mob mob, Object cause ){
		Hero hero = Dungeon.hero;
		if (hero == null || mob.alignment != Char.Alignment.ENEMY || mob instanceof NPC) return;

		boolean heroBlow = cause == hero;
		if (heroBlow){
			Item weapon = hero.belongings.thrownWeapon != null ? hero.belongings.thrownWeapon : hero.belongings.weapon();
			Quality q = of( weapon );
			if (q != null && weapon instanceof Weapon) WeaponPerk.of( weapon ).onKill( q, hero );
			//HUNTER: a slain beast may leave meat (a shot's line is its launcher's)
			float hunt = value( weapon instanceof Weapon ? source( (Weapon) weapon, hero ) : weapon, RarityLine.HUNTER );
			if (hunt > 0f && SlayerFamily.BEASTS.has( mob ) && Random.Float() < hunt){
				xyz.gabriwar.warpedpixeldungeon.items.Heap meat = Dungeon.level.drop( new xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat(), mob.pos );
				if (meat.sprite != null) meat.sprite.drop();
				//the extra yield's gold glint, the heap's own drop showing the meat
				xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx.extraGlint( mob.pos );
			}
			if (q != null && q.fullyMasterworked()){
				q.kills++;
				if (hero.HP < hero.HT && killReady( q )) hero.heal( Math.max( 1, Math.round( hero.HT * 0.05f ) ) );
			}
		}
		if (cause instanceof Wand){
			Quality q = of( (Wand) cause );
			if (q != null && q.fullyMasterworked()){
				q.kills++;
				Wand w = (Wand) cause;
				//a fifth of the bar rather than one charge, so a big wand is paid in its own scale
				w.curCharges = Math.min( w.maxCharges, w.curCharges + Math.max( 1, Math.round( 0.20f * w.maxCharges ) ) );
				w.updateQuickslot();
			}
		}
		if (heroBlow || cause instanceof Wand || SkillInteractions.heroSkillSource( cause )){
			Quality armor = of( hero.belongings.armor );
			if (armor != null && armor.fullyMasterworked() && killReady( armor )){
				Buff.affect( hero, Barrier.class ).incShield( Math.max( 1, Math.round( hero.HT * 0.05f ) ) );
			}
		}

		if (Char.hasProp( mob, Char.Property.BOSS )){
			Dungeon.level.drop( new MasterworkCore().quantity( 1 + Random.Int( 2 ) ), mob.pos ).sprite.drop();
		} else if (heroBlow && Random.Float() < 0.0007f){
			//0.07% per kill of the hero's own, on top of the boss drops
			Dungeon.level.drop( new MasterworkCore(), mob.pos ).sprite.drop();
		} else if (heroBlow && !xyz.gabriwar.warpedpixeldungeon.levels.Delves.inDelve() && Dungeon.depth >= 3 && Dungeon.LimitedDrops.TYPE_SHIFTER.count < Dungeon.depth
				&& Random.Float() < 0.0001f){
			//0.01% per kill, from the hero's own kills only, and never more than one per floor reached
			Dungeon.LimitedDrops.TYPE_SHIFTER.count = Dungeon.depth;
			Dungeon.level.drop( new TypeShifter(), mob.pos ).sprite.drop();
		}
	}

	/** a fully-masterworked kill reward pays at most once every ten turns, so a room full
	 *  of weak enemies cannot heal the hero to full */
	private static boolean killReady( Quality q ){
		float now = Actor.now();
		if (now < q.killReady && q.killReady - now <= 10f) return false;
		q.killReady = now + 10f;
		return true;
	}

	// ---------------------------------------------------------------- text

	//an unidentified item shows its rarity and its type - the player sees what they hold and
	//whether it is worth identifying - but not what they do: its rolled lines, their strength
	//and its perk (which for a ring, a wand or an artifact would name the very item)
	private static boolean known( Item item ){
		return item == null || item.isIdentified();
	}

	/** the block appended to an item's description: tier, lines, perk, masterwork */
	public String describe( Item item ){
		Family fam = family( item );
		StringBuilder sb = new StringBuilder();
		sb.append( Messages.get( this, "header", rarityName(), typeName( type ) ) );
		if (!known( item )){
			sb.append( "\n\n" ).append( Messages.get( this, "unknown" ) );
			return sb.toString();
		}
		appendLines( sb, item );
		if (fam != null) sb.append( "\n\n" ).append( perkText( item, type ) );
		if (masterwork > 0){
			sb.append( "\n\n" ).append( Messages.get( this, fullyMasterworked() ? "masterwork_full" : "masterwork", masterwork, MASTERWORK_MAX, kills ) );
		}
		return sb.toString();
	}

	/** the item's group perk at a type: weapons and thrown weapons, or any other gear */
	public static String perkText( Item item, ItemType t ){
		Family fam = family( item );
		if (fam == Family.WEAPON || fam == Family.MISSILE) return WeaponPerk.of( item ).describe( item, t );
		GearPerk p = GearPerk.of( item );
		return p == null ? "" : p.describe( item, t );
	}

	/** the rarity tab of {@link xyz.gabriwar.warpedpixeldungeon.windows.WndItemQuality} */
	public String describeRarity( Item item ){
		StringBuilder sb = new StringBuilder( Messages.get( this, "tab_rarity", rarityName(), rarity.lines ) );
		if (!known( item )){
			sb.append( "\n\n" ).append( Messages.get( this, "unknown_lines" ) );
			sb.append( "\n\n" ).append( Messages.get( this, fullyMasterworked() ? "masterwork_full" : "masterwork", masterwork, MASTERWORK_MAX, kills ) );
			return sb.toString();
		}
		appendLines( sb, item );
		sb.append( "\n\n" ).append( Messages.get( this, fullyMasterworked() ? "masterwork_full" : "masterwork", masterwork, MASTERWORK_MAX, kills ) );
		if (masterworkCost() > 0) sb.append( " " ).append( Messages.get( this, "masterwork_next", masterworkCost() ) );
		return sb.toString();
	}

	//the number lines, then the vestige in a paragraph of its own with what its enchantment or
	//glyph does (it is in no slot, so nothing else would say), then the tier's potency
	private void appendLines( StringBuilder sb, Item item ){
		boolean numbers = false;
		for (int i = 0; i < lines.size(); i++){
			if (lines.get( i ) == RarityLine.VESTIGE) continue;
			if (!numbers) sb.append( "\n" );
			numbers = true;
			sb.append( "\n_" ).append( lines.get( i ).describe( item, roll( lines.get( i ) ) ) ).append( "_" );
		}
		Bundlable v = vestigeMagic( item );
		if (v != null){
			sb.append( "\n\n" ).append( Messages.get( this, v instanceof Armor.Glyph ? "tab_vestige_glyph" : "tab_vestige" ) )
					.append( "\n_" ).append( RarityLine.VESTIGE.describe( item, roll( RarityLine.VESTIGE ) ) ).append( "_\n" )
					.append( vestigeDesc( v ) );
			if (hasPotency( item )) sb.append( "\n" );
		}
		if (hasPotency( item )) sb.append( "\n" ).append( Messages.get( this, "potency", Math.round( potency() * 100f ) ) );
	}

	/** a rarity tab for a tier the item does not have: what that tier would give */
	public String describeRarityTier( Item item, Rarity r ){
		int from = r == Rarity.EXOTIC ? 15 : r == Rarity.LEGENDARY ? 5 : 1;
		String text = Messages.get( this, hasPotency( item ) ? "tab_rarity_other" : "tab_rarity_other_gear",
				Messages.tint( r.color, r.title() ), r.lines,
				Math.round( r.minRoll * 100 ), Math.round( r.maxRoll * 100 ), from,
				Math.round( r.potency() * 100f ) );
		if ((item instanceof Weapon || item instanceof Armor) && r.ordinal() >= Rarity.LEGENDARY.ordinal()){
			//the properties loader drops a value's leading space, so the sentence's is set here
			text += " " + Messages.get( this, item instanceof Armor ? "tab_rarity_other_vestige_glyph" : "tab_rarity_other_vestige" );
		}
		//a weapon, thrown weapon or armor says what kind its lines are, and which traits its kind rolls
		Family fam = family( item );
		if (RarityLine.slotted( fam )){
			text += " " + (r == Rarity.COMMON ? Messages.get( this, "slot_common" )
					: Messages.get( this, r == Rarity.UNCOMMON ? "slot_uncommon" : "slot_rare", traitList( fam, item ) ));
		}
		return text;
	}

	//the traits an item's kind rolls, by their short names: "a, b or c"
	private static String traitList( Family fam, Item item ){
		ArrayList<RarityLine> traits = RarityLine.traits( fam, item );
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < traits.size(); i++){
			if (i > 0) sb.append( i == traits.size() - 1 ? " or " : ", " );
			sb.append( traits.get( i ).shortName() );
		}
		return sb.toString();
	}

	/** one type tab: what the item's perk does at that type, marked when it is the current one */
	public String describeType( Item item, ItemType t ){
		Family fam = family( item );
		StringBuilder sb = new StringBuilder( Messages.get( this, t == type ? "tab_type_current" : "tab_type", typeName( t ) ) );
		if (fam != null) sb.append( "\n\n" ).append( known( item ) ? perkText( item, t ) : Messages.get( this, "unknown_perk" ) );
		if (t.ordinal() > type.ordinal()){
			int cost = 0;
			for (ItemType s = type; s != t; s = s.next()) cost += s.shiftCost();
			sb.append( "\n\n" ).append( Messages.get( this, "tab_type_cost", cost ) );
		}
		return sb.toString();
	}

	/** the rarity's name in its own colour (white-gold once masterworked), for descriptions */
	public String rarityName(){
		return Messages.tint( titleColor(), rarity.title() );
	}

	/** a type's name in its plate's colour */
	public static String typeName( ItemType t ){
		return Messages.tint( t.color, t.title() );
	}

	/** the colour an item's name takes in windows: its rarity's, white-gold once masterworked */
	public int titleColor(){
		return fullyMasterworked() ? Rarity.MASTERWORK_COLOR : rarity.color;
	}

	// ---------------------------------------------------------------- bundle

	private static final String RARITY = "rarity";
	private static final String TYPE = "type";
	private static final String MASTERWORK = "masterwork";
	//how many steps masterwork had when the save was written: a save without it counted ten
	private static final String MASTERWORK_STEPS = "masterwork_steps";
	private static final String KILLS = "kills";
	private static final String GRANTED = "granted";
	private static final String LINES = "lines";
	private static final String ROLLS = "rolls";
	private static final String VESTIGE_ENCHANT = "vestige";
	//written by every save that knows weapon vestiges, and armor ones: a quality without it is
	//older, see settleVestige
	private static final String VESTIGE_ROLLED = "vestige_rolled";
	private static final String GLYPH_VESTIGE_ROLLED = "glyph_vestige_rolled";
	//the SLAYER line's family, by name, written only while the line is held
	private static final String SLAYER = "slayer";

	@Override
	public void storeInBundle( Bundle bundle ){
		bundle.put( RARITY, rarity );
		bundle.put( TYPE, type );
		bundle.put( MASTERWORK, masterwork );
		bundle.put( MASTERWORK_STEPS, MASTERWORK_MAX );
		bundle.put( KILLS, kills );
		bundle.put( GRANTED, granted );
		String[] names = new String[lines.size()];
		float[] values = new float[lines.size()];
		for (int i = 0; i < lines.size(); i++){
			names[i] = lines.get( i ).name();
			values[i] = rolls.get( i );
		}
		bundle.put( LINES, names );
		bundle.put( ROLLS, values );
		if (has( RarityLine.VESTIGE ) && vestige != null) bundle.put( VESTIGE_ENCHANT, vestige );
		if (has( RarityLine.SLAYER ) && slayer != null) bundle.put( SLAYER, slayer.name() );
		bundle.put( VESTIGE_ROLLED, true );
		bundle.put( GLYPH_VESTIGE_ROLLED, true );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		rarity = bundle.getEnum( RARITY, Rarity.class );
		type = bundle.getEnum( TYPE, ItemType.class );
		masterwork = bundle.getInt( MASTERWORK );
		//a save from when masterwork had another number of steps: ten (no mark), where two steps
		//made one and the tenth the fifth, or three, where a full piece stays full
		int steps = bundle.contains( MASTERWORK_STEPS ) ? bundle.getInt( MASTERWORK_STEPS ) : 10;
		if (steps != MASTERWORK_MAX && masterwork > 0){
			if (masterwork >= steps) masterwork = MASTERWORK_MAX;
			else if (steps == 10) masterwork = Math.min( MASTERWORK_MAX - 1, (masterwork + 1) / 2 );
		}
		masterwork = Math.min( masterwork, MASTERWORK_MAX );
		kills = bundle.getInt( KILLS );
		granted = bundle.getBoolean( GRANTED );
		lines.clear();
		rolls.clear();
		String[] names = bundle.getStringArray( LINES );
		float[] values = bundle.getFloatArray( ROLLS );
		if (names != null && values != null){
			for (int i = 0; i < names.length && i < values.length; i++){
				//a line this build does not know (a save from a newer one) is dropped, not fatal
				RarityLine line;
				try {
					line = RarityLine.valueOf( names[i] );
				} catch (IllegalArgumentException e){
					continue;
				}
				lines.add( line );
				rolls.add( values[i] );
			}
		}
		int v = lines.indexOf( RarityLine.VESTIGE );
		vestige = v < 0 ? null : bundle.get( VESTIGE_ENCHANT );
		if (v >= 0 && vestige == null){
			//an enchantment or glyph that no longer exists: the line goes with it
			lines.remove( v );
			rolls.remove( v );
		}
		int s = lines.indexOf( RarityLine.SLAYER );
		slayer = null;
		if (s >= 0){
			//a family this build does not know, or none at all: the line goes with it
			try {
				slayer = SlayerFamily.valueOf( bundle.getString( SLAYER ) );
			} catch (IllegalArgumentException e){
				lines.remove( s );
				rolls.remove( s );
			}
		}
		vestigeRolled = bundle.getBoolean( VESTIGE_ROLLED );
		glyphVestigeRolled = bundle.getBoolean( GLYPH_VESTIGE_ROLLED );
	}
}
