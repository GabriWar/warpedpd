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

package xyz.gabriwar.warpedpixeldungeon.ui;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.actors.ledger.DamageLedger;
import xyz.gabriwar.warpedpixeldungeon.actors.ledger.HitRecord;
import xyz.gabriwar.warpedpixeldungeon.actors.ledger.Side;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLook;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLooks;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.LineLooks;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.TypeFx;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.GearPerk;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.RarityLine;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.SlayerFamily;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.WeaponPerk;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.Reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * What a chip of a hit card means, in words (the Damage tab's "what it showed"): every code of the
 * plan's chip namespace (ChipArt) has a name.
 * - a text cell: the game's own name where one says it (a weapon type's perk, a rarity line, the
 *   enchantment whose mark and colour it is, the ledger's crit and execute), else its own string
 *   under ui.chiplegend;
 * - a buff icon: the name of the buff that wore it, from the hit's snapshots of both sides, then
 *   from any other record's (a buff laid on by the hit is in the next hits' snapshots), then the
 *   buff the icon's BuffIndicator constant is named for;
 * - a skill icon: the hero's skill with that image (his own first, then every class's), its name
 *   and, when it has one, the shout it gives ("Creeping Dread: Boo.").
 * Never null and never empty: what cannot be named reads as a skill, an effect or a mark.
 */
public final class ChipLegend {

	private ChipLegend(){}

	//the text cells that stand for words: the miss verbs, the immune and resisted marks, a monster's
	//rage, a cursed fuse, a quick reload
	static final int MISS_DODGE = FloatingText.MISS_DODGE, MISS_BLOCK = FloatingText.MISS_BLOCK,
			GUARDED = FloatingText.GUARDED, IMMUNE = FloatingText.IMMUNE, RESISTED = FloatingText.RESISTED,
			RAGE = FloatingText.RAGE, FUSE = FloatingText.FUSE, RELOAD = FloatingText.RELOAD;

	/** The hit-reason cells' twins for a blow armour could not block sit this many cells on. */
	static final int NO_BLOCK_ROW = 18;

	private static HashMap<Integer, String> cellKeys;

	//the text cells that have a string of their own, by cell (built once FloatingText is loaded)
	private static synchronized HashMap<Integer, String> cellKeys(){
		if (cellKeys != null) return cellKeys;
		HashMap<Integer, String> m = new HashMap<>();
		m.put( FloatingText.PHYS_DMG, "dmg_phys" );
		m.put( FloatingText.PHYS_DMG_NO_BLOCK, "dmg_pierce" );
		m.put( FloatingText.MAGIC_DMG, "dmg_magic" );
		m.put( FloatingText.PICK_DMG, "dmg_pick" );
		m.put( FloatingText.HUNGER, "hunger" );
		m.put( FloatingText.BURNING, "burning" );
		m.put( FloatingText.SHOCKING, "shocking" );
		m.put( FloatingText.FROST, "frost" );
		m.put( FloatingText.WATER, "water" );
		m.put( FloatingText.BLEEDING, "bleeding" );
		m.put( FloatingText.TOXIC, "toxic" );
		m.put( FloatingText.CORROSION, "corrosion" );
		m.put( FloatingText.POISON, "poison" );
		m.put( FloatingText.OOZE, "ooze" );
		m.put( FloatingText.DEFERRED, "deferred" );
		m.put( FloatingText.CORRUPTION, "corruption" );
		m.put( FloatingText.AMULET, "amulet" );
		m.put( FloatingText.HEALING, "healing" );
		m.put( FloatingText.SHIELDING, "shielding" );
		m.put( FloatingText.EXPERIENCE, "experience" );
		m.put( FloatingText.STRENGTH, "strength" );
		m.put( FloatingText.MANA, "mana" );
		m.put( FloatingText.GOLD, "gold" );
		m.put( FloatingText.ENERGY, "energy" );
		m.put( FloatingText.PARRY, "parried" );
		m.put( FloatingText.HIT_WEP, "hit_wep" );
		m.put( FloatingText.HIT_ARM, "hit_arm" );
		m.put( FloatingText.HIT_BLS, "hit_bls" );
		m.put( FloatingText.HIT_HEX, "hit_hex" );
		m.put( FloatingText.HIT_DAZE, "hit_daze" );
		m.put( FloatingText.HIT_ACC, "hit_acc" );
		m.put( FloatingText.HIT_EVA, "hit_eva" );
		m.put( FloatingText.HIT_LIQ, "hit_liq" );
		m.put( FloatingText.HIT_DANCE, "hit_dance" );
		m.put( FloatingText.HIT_SUPR, "hit_supr" );
		m.put( FloatingText.HIT_PRES, "hit_pres" );
		m.put( FloatingText.HIT_MOMEN, "hit_momen" );
		m.put( FloatingText.MISS_WEP, "miss_wep" );
		m.put( FloatingText.MISS_ARM, "miss_arm" );
		m.put( FloatingText.MISS_BLS, "miss_bls" );
		m.put( FloatingText.MISS_HEX, "miss_hex" );
		m.put( FloatingText.MISS_DAZE, "miss_daze" );
		m.put( FloatingText.MISS_ACC, "miss_acc" );
		m.put( FloatingText.MISS_EVA, "miss_eva" );
		m.put( FloatingText.MISS_LIQ, "miss_liq" );
		m.put( FloatingText.MISS_DEF, "miss_def" );
		m.put( FloatingText.MISS_TUFT, "miss_tuft" );
		m.put( FloatingText.MISS_RUN, "miss_run" );
		m.put( MISS_DODGE, "miss_dodge" );
		m.put( MISS_BLOCK, "miss_block" );
		m.put( GUARDED, "guarded" );
		m.put( IMMUNE, "immune" );
		m.put( RESISTED, "resisted" );
		m.put( RAGE, "rage" );
		m.put( FUSE, "fuse" );
		m.put( RELOAD, "reload" );
		cellKeys = m;
		return m;
	}

	private static final String[] FAMILY_KEYS = { "f_ember", "f_rime", "f_storm", "f_venom", "f_verdant",
			"f_dread", "f_reaper", "f_cut", "f_drain", "f_force", "f_ward", "f_radiance", "f_fortune",
			"f_mirage", "f_heart", "f_hex" };

	private static String get( String key, Object... args ){
		return Messages.get( ChipLegend.class, key, args );
	}

	private static boolean known( String s ){
		return s != null && !s.trim().isEmpty() && !s.equals( Messages.NO_TEXT_FOUND );
	}

	/** The name of a chip as it showed on this record's card (record may be null); `others` are the
	 *  rest of the records, for a buff laid on by the hit (may be null). */
	public static String name( int code, int tint, HitRecord record, List<HitRecord> others ){
		try {
			String s;
			if (code >= ChipArt.SKILL_BASE) s = skill( code - ChipArt.SKILL_BASE );
			else if (code < 0) s = buff( -code - 1, record, others );
			else s = cell( code, tint );
			if (known( s )) return s;
		} catch (RuntimeException e){
			//a name that cannot be made falls to the general one below
		}
		return get( code >= ChipArt.SKILL_BASE ? "skill" : code < 0 ? "effect" : "mark" );
	}

	// ------------------------------------------------------------------ text cells

	/** A text cell's name; tint picks the very enchantment a family mark stands for. */
	public static String cell( int code, int tint ){
		//a family's mark and its echo's hollow twin
		int fam = code - FloatingText.F_EMBER, echoFam = code - FloatingText.hollow( FloatingText.F_EMBER );
		int families = FloatingText.F_HEX - FloatingText.F_EMBER + 1;
		if (fam >= 0 && fam < families) return family( code, tint, false );
		if (echoFam >= 0 && echoFam < families) return get( "echo", family( code, tint, true ) );

		//a hit's reason through armour that could not block it
		if (code >= FloatingText.HIT_WEP + NO_BLOCK_ROW && code <= FloatingText.HIT_MOMEN + NO_BLOCK_ROW){
			return get( "no_block", cell( code - NO_BLOCK_ROW, 0 ) );
		}

		//the ledger's own words for the X and the execute mark
		if (code == FloatingText.CRIT) return Messages.get( DamageLedger.class, "crit" );
		if (code == FloatingText.EXECUTE) return Messages.get( DamageLedger.class, "execute" );

		//a weapon type's silver mark: the perk (or perks) that wear it
		String perks = perkNames( code );
		if (perks != null) return perks;

		//a rarity line's gold one
		String line = lineName( code );
		if (line != null) return line;

		//the armour perk's mark a Bulwark blow wears
		if (code == FloatingText.BULWARK) return Messages.titleCase( GearPerk.BULWARK.title() );

		String key = cellKeys().get( code );
		return key != null ? get( key ) : null;
	}

	//every weapon perk whose mark is this cell, "Ambush" or "Shot / Gun"; null for none (the parry
	//cell reads as a parry: a guard's riposte and the stances that turn a blow aside share it)
	private static String perkNames( int code ){
		if (code == FloatingText.PARRY) return get( "parried" );
		StringBuilder s = null;
		ArrayList<String> seen = new ArrayList<>();
		for (WeaponPerk p : WeaponPerk.values()){
			if (TypeFx.perkIcon( p ) != code) continue;
			String t = p.title();
			if (!known( t ) || seen.contains( t )) continue;
			seen.add( t );
			if (s == null) s = new StringBuilder( t );
			else s.append( " / " ).append( t );
		}
		return s == null ? null : s.toString();
	}

	//the rarity line whose gold mark this cell is; a slayer's names the kind it slays
	private static String lineName( int code ){
		for (RarityLine l : RarityLine.values()){
			if (l == RarityLine.SLAYER){
				for (SlayerFamily f : SlayerFamily.values()){
					if (LineLooks.chipOf( l, f ) == code){
						return Messages.capitalize( l.shortName() ) + ": " + f.title();
					}
				}
			} else if (LineLooks.chipOf( l, null ) == code){
				return Messages.capitalize( l.shortName() );
			}
		}
		return null;
	}

	//the enchantment, glyph or curse whose mark (family cell, echo or not) and colour this is; the
	//family's own name when no single one matches
	private static String family( int code, int tint, boolean echo ){
		String found = null;
		int matches = 0;
		for (EnchantLook l : EnchantLooks.all()){
			if (l == null || EnchantFx.chipOf( l, echo ) != code) continue;
			if (tint != 0 && (EnchantFx.chipTintOf( l, echo ) & 0xFFFFFF) != (tint & 0xFFFFFF)) continue;
			String n = lookName( l );
			if (n == null) continue;
			if (found == null || !found.equals( n )) matches++;
			found = n;
		}
		if (matches == 1 && tint != 0) return found;
		int fam = (echo ? code - FloatingText.hollow( FloatingText.F_EMBER ) : code - FloatingText.F_EMBER);
		return get( FAMILY_KEYS[Math.max( 0, Math.min( FAMILY_KEYS.length - 1, fam ) )] );
	}

	private static String lookName( EnchantLook l ){
		try {
			Object o = Reflection.newInstanceUnhandled( l.of );
			String n = null;
			if (o instanceof Weapon.Enchantment) n = ((Weapon.Enchantment) o).name();
			else if (o instanceof Armor.Glyph) n = ((Armor.Glyph) o).name();
			return known( n ) ? Messages.titleCase( n ) : null;
		} catch (Exception e){
			return null;
		}
	}

	// ------------------------------------------------------------------ buffs

	/** A buff icon's name: from the sides of this hit, then of any other, then the buff the
	 *  icon's BuffIndicator constant names. */
	public static String buff( int icon, HitRecord record, List<HitRecord> others ){
		if (record != null){
			String s = fromSides( icon, record );
			if (s != null) return s;
		}
		if (others != null){
			for (HitRecord r : others){
				if (r == null || r == record) continue;
				String s = fromSides( icon, r );
				if (s != null) return s;
			}
		}
		return byIndicator( icon );
	}

	private static String fromSides( int icon, HitRecord r ){
		String s = fromSide( icon, r.defender );
		return s != null ? s : fromSide( icon, r.attacker );
	}

	private static String fromSide( int icon, Side side ){
		if (side == null) return null;
		int n = Math.min( side.buffIcons.length, side.buffNames.length );
		for (int i = 0; i < n; i++){
			if (side.buffIcons[i] == icon && known( side.buffNames[i] )) return side.buffNames[i];
		}
		return null;
	}

	private static HashMap<Integer, String> indicatorNames;

	//BuffIndicator's constant for this icon ("TERROR"), read as the buff of that name
	//(actors.buffs.terror.name), else as the words of the constant
	private static String byIndicator( int icon ){
		synchronized (ChipLegend.class){
			if (indicatorNames == null){
				indicatorNames = new HashMap<>();
				for (Field f : BuffIndicator.class.getFields()){
					int mod = f.getModifiers();
					if (f.getType() != int.class || !Modifier.isStatic( mod ) || !Modifier.isFinal( mod )) continue;
					try {
						int v = f.getInt( null );
						if (v != BuffIndicator.NONE && !indicatorNames.containsKey( v )) indicatorNames.put( v, f.getName() );
					} catch (IllegalAccessException e){
						//a field it cannot read names nothing
					}
				}
			}
		}
		String constant = indicatorNames.get( icon );
		if (constant == null) return null;
		String lower = constant.toLowerCase( Locale.ENGLISH );
		String n = Messages.get( "actors.buffs." + lower.replace( "_", "" ) + ".name" );
		if (!known( n )) n = Messages.get( "actors.buffs." + lower + ".name" );
		if (known( n )) return Messages.titleCase( n );
		return Messages.titleCase( lower.replace( '_', ' ' ) );
	}

	// ------------------------------------------------------------------ skills

	/** The hero's skill with this image (his own first, then any class's), with its shout when it
	 *  has one: "Creeping Dread: Boo." */
	public static String skill( int image ){
		Skill s = skillOf( image );
		if (s == null) return null;
		String name = s.name();
		if (!known( name )) return null;
		String shout = s.castText();
		return known( shout ) ? name + ": " + shout : name;
	}

	static Skill skillOf( int image ){
		Hero hero = Dungeon.hero;
		if (hero != null && hero.heroSkills != null){
			CurrentSkills t = hero.heroSkills;
			ArrayList<List<Skill>> trees = new ArrayList<>();
			trees.add( t.passiveASkills );
			trees.add( t.passiveBSkills );
			trees.add( t.activeSkills );
			trees.add( t.fourthSkills );
			trees.add( t.subSkills );
			trees.add( t.foreignSkills() );
			for (List<Skill> l : trees){
				for (Skill s : l) if (s != null && s.image() == image) return s;
			}
		}
		for (CurrentSkills.Origin o : CurrentSkills.catalog()){
			Skill s = Reflection.newInstance( o.cls );
			if (s != null && s.image() == image) return s;
		}
		return null;
	}
}
