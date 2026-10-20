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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.combat;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.HitCard;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantNet;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The blow ledger (docs/fx/hit-card.md): one blow, from Char.attack's opening to its close, as
 * one sentence. While a blow is open on a thread every text its characters raise belongs to it
 * (claim: numbers sum onto the struck one's card, what flows back onto the wielder's small card,
 * anything else is released as a plain text above the card), every buff announced becomes a chip
 * (announce), and every picture is handed in as a Piece (offer) instead of drawn. At the close the
 * presenter (BlowPresenter) draws one lead, a few accents and one card per struck character.
 *
 * A per-thread stack: the render thread runs the blows the hero sees (Hero.onAttackComplete,
 * Mob.onAttackComplete), the actor thread unseen ones (Mob.doAttack); a blow opened inside another
 * gets its own record and closes before its parent. Records are pooled; one that gathered anything
 * is handed whole to the presenter, which arranges its pictures and never thins them. Nothing here
 * draws the game's Random or changes the game.
 */
public final class Blow {

	private Blow(){}

	/** Piece ranks, best first: the lead is the best-ranked piece of the blow. */
	public static final int FINISHER = 0, PROC = 1, TWIST = 2, CRIT = 3, PLAIN = 4, MINOR = 5;
	/** Piece lanes: on the struck one (an accent at the hit point), placed (its own geometry), at
	 *  the wielder's weapon side, riding a motion. */
	public static final int TARGET = 0, WIELDER = 1, PLACED = 2, MOTION = 3;
	/** Accent priority: the order of the beats, first first. */
	public static final int A_TWIST = 0, A_CRIT = 1, A_SECOND = 2, A_ECHO = 3, A_PERK = 4, A_STUN = 5, A_LATE = 6;

	// ------------------------------------------------------------------ the record

	/** One blow as it was gathered; the presenter reads it on the render thread once closed. */
	static final class Record {
		Char attacker, defender;
		int serial, reason = -1;
		boolean seen, heroIn, killed;
		final ArrayList<Piece> pieces = new ArrayList<>( 4 );
		final ArrayList<Mark> marks = new ArrayList<>( 2 );
		final ArrayList<CharSprite> flashed = new ArrayList<>( 2 );
		//what flows back to the attacker: amounts, and when the last of what carries it lands
		int heal, mana, shield;
		float flowDelay;
		boolean carried;
		//whether it gathered anything to present
		boolean any;

		void clear(){
			attacker = defender = null;
			serial = 0;
			reason = -1;
			seen = heroIn = killed = carried = any = false;
			pieces.clear();
			marks.clear();
			flashed.clear();
			heal = mana = shield = 0;
			flowDelay = 0f;
		}

		Mark mark( Char ch ){
			for (int i = 0; i < marks.size(); i++) if (marks.get( i ).ch == ch) return marks.get( i );
			Mark m = new Mark( ch );
			marks.add( m );
			any = true;
			return m;
		}

		Mark find( Char ch ){
			for (int i = 0; i < marks.size(); i++) if (marks.get( i ).ch == ch) return marks.get( i );
			return null;
		}
	}

	/** What a blow did to one character: its summed number (its colour and first icon, whether
	 *  the first physical one was armour-piercing), its card-only chips in arrival order, its
	 *  announced buffs (folded at the close if a piece stands for them), and a mark for a card with
	 *  no number (a miss's verb and its reason, immunity). */
	static final class Mark {
		final Char ch;
		boolean hasNumber, noBlock;
		int number, color, icon = -1;
		int markCell = -1, markReason = -1;
		int[] chips = new int[4], tints = new int[4];
		int chipCount;
		final ArrayList<Class<?>> buffs = new ArrayList<>( 0 );
		int[] buffCodes = new int[2], buffTints = new int[2];

		Mark( Char ch ){
			this.ch = ch;
		}

		void number( int n, int color, int icon ){
			if (!hasNumber){
				hasNumber = true;
				this.color = color;
				this.icon = icon;
				noBlock = icon == FloatingText.PHYS_DMG_NO_BLOCK;
			}
			if (n > 0) number += n;
		}

		void chip( int code, int tint ){
			for (int i = 0; i < chipCount; i++) if (chips[i] == code) return;
			if (chipCount == chips.length){
				chips = java.util.Arrays.copyOf( chips, chipCount * 2 );
				tints = java.util.Arrays.copyOf( tints, chipCount * 2 );
			}
			chips[chipCount] = code;
			tints[chipCount] = tint;
			chipCount++;
		}

		void buff( Class<?> c, int code, int tint ){
			int n = buffs.size();
			for (int i = 0; i < n; i++) if (buffCodes[i] == code) return;
			if (n == buffCodes.length){
				buffCodes = java.util.Arrays.copyOf( buffCodes, n * 2 );
				buffTints = java.util.Arrays.copyOf( buffTints, n * 2 );
			}
			buffs.add( c );
			buffCodes[n] = code;
			buffTints[n] = tint;
		}
	}

	// ------------------------------------------------------------------ the stack

	private static final class Ledger {
		Record[] stack = new Record[4];
		int depth;
		//chips waiting for the next blow on nextOn (nextBlow)
		Char nextOn;
		int[] nextCodes = new int[4], nextTints = new int[4];
		int nextN;

		Record push(){
			if (depth == stack.length) stack = java.util.Arrays.copyOf( stack, depth * 2 );
			Record r = stack[depth];
			if (r == null) r = stack[depth] = new Record();
			r.clear();
			depth++;
			return r;
		}

		Record top(){
			return depth > 0 ? stack[depth - 1] : null;
		}
	}

	private static final ThreadLocal<Ledger> LEDGER = ThreadLocal.withInitial( Ledger::new );
	private static final AtomicInteger serials = new AtomicInteger();

	private static Record top(){
		return LEDGER.get().top();
	}

	// ------------------------------------------------------------------ opening and closing

	/** Char.attack's wrapper only. */
	public static void begin( Char attacker, Char defender ){
		Record r = LEDGER.get().push();
		r.attacker = attacker;
		r.defender = defender;
		int s;
		do {
			s = serials.incrementAndGet();
		} while (s == 0);
		r.serial = s;
		r.seen = seen( attacker ) || seen( defender );
		Char hero = Dungeon.hero;
		r.heroIn = hero != null && (attacker == hero || defender == hero);
		Ledger l = LEDGER.get();
		if (l.nextN > 0){
			if (l.nextOn == defender && defender != null){
				for (int i = 0; i < l.nextN; i++) r.mark( defender ).chip( l.nextCodes[i], l.nextTints[i] );
			}
			l.nextN = 0;
			l.nextOn = null;
		}
	}

	/** Chips for the next blow this thread opens on `on` (a riposte, called out just before its
	 *  swing): that blow takes them onto its struck one's card; the next blow on anyone else drops
	 *  them. */
	public static void nextBlow( Char on, int code, int tint ){
		Ledger l = LEDGER.get();
		if (l.nextOn != on) l.nextN = 0;
		l.nextOn = on;
		if (l.nextN == l.nextCodes.length){
			l.nextCodes = java.util.Arrays.copyOf( l.nextCodes, l.nextN * 2 );
			l.nextTints = java.util.Arrays.copyOf( l.nextTints, l.nextN * 2 );
		}
		l.nextCodes[l.nextN] = code;
		l.nextTints[l.nextN] = tint;
		l.nextN++;
	}

	/** Char.attack's wrapper only. */
	public static void end(){
		Ledger l = LEDGER.get();
		if (l.depth == 0) return;
		final Record r = l.stack[--l.depth];
		if (!r.any){
			r.clear();
			return;
		}
		r.killed = r.defender != null && !r.defender.isAlive();
		//the presenter keeps this one: the slot gets a fresh record next time
		l.stack[l.depth] = null;
		Fx.post( () -> BlowPresenter.present( r ) );
	}

	/** Char.hit's hit branches: the blow's reason icon, recorded only for the open blow of this
	 *  attacker and defender. */
	public static void reason( Char attacker, Char defender, int icon ){
		Record r = top();
		if (r != null && r.attacker == attacker && r.defender == defender) r.reason = icon;
	}

	/** True while a blow is open on this thread. */
	public static boolean open(){
		return top() != null;
	}

	/** The open blow's serial for co-op tags, 0 when none. */
	public static int serial(){
		Record r = top();
		return r != null ? r.serial : 0;
	}

	/** The open blow's serial for tagging a blood burst or a flash to co-op guests: 0 when none is
	 *  open or its struck one's card will not be drawn (out of the host's sight: no CARD would come
	 *  for a guest to wait on). */
	public static int cardSerial(){
		Record r = top();
		return r != null && r.defender != null && r.defender.sprite != null && r.defender.sprite.visible ? r.serial : 0;
	}

	private static boolean seen( Char c ){
		Level l = Dungeon.level;
		if (l == null || l.heroFOV == null) return true;
		return c != null && c.pos >= 0 && c.pos < l.heroFOV.length && l.heroFOV[c.pos];
	}

	// ------------------------------------------------------------------ texts

	/**
	 * CharSprite.showStatusWithIcon, after its visible test: true when the open blow took the text
	 * (draw and send nothing). Numbers with a damage icon sum onto that character's card; a
	 * healing, shielding or mana number on the attacker goes to his small card; anything else is
	 * released (false) and shows as a plain text above the card. By icon only.
	 */
	public static boolean claim( Char ch, String text, int color, int icon ){
		Record r = top();
		if (r == null || ch == null) return false;
		if (damageIcon( icon )){
			int n = number( text );
			if (n == Integer.MIN_VALUE) return false;
			r.mark( ch ).number( n, color, icon );
			return true;
		}
		if (ch == r.attacker && (icon == FloatingText.HEALING || icon == FloatingText.SHIELDING || icon == FloatingText.MANA)){
			int n = number( text );
			if (n == Integer.MIN_VALUE) return false;
			if (n > 0){
				if (icon == FloatingText.HEALING) r.heal += n;
				else if (icon == FloatingText.SHIELDING) r.shield += n;
				else r.mana += n;
				r.any = true;
			}
			return true;
		}
		return false;
	}

	/** Whether a text icon is a damage number's: the damage icons, every damage-over-time cell,
	 *  every hit reason and its armour-piercing twin. */
	public static boolean damageIcon( int icon ){
		return (icon >= FloatingText.PHYS_DMG && icon <= FloatingText.PICK_DMG)
				|| (icon >= FloatingText.HUNGER && icon <= FloatingText.AMULET)
				|| (icon >= FloatingText.HIT_WEP && icon <= FloatingText.HIT_MOMEN)
				|| (icon >= FloatingText.HIT_WEP + 18 && icon <= FloatingText.HIT_MOMEN + 18);
	}

	//a whole number in a text ("12", "+12", "-3"), MIN_VALUE when the text is anything else
	static int number( String text ){
		if (text == null) return Integer.MIN_VALUE;
		String t = text.trim();
		int i = 0, sign = 1;
		if (t.startsWith( "+" )) i = 1;
		else if (t.startsWith( "-" )){
			i = 1;
			sign = -1;
		}
		if (i >= t.length() || t.length() - i > 9) return Integer.MIN_VALUE;
		int n = 0;
		for (; i < t.length(); i++){
			char c = t.charAt( i );
			if (c < '0' || c > '9') return Integer.MIN_VALUE;
			n = n * 10 + (c - '0');
		}
		return n * sign;
	}

	// ------------------------------------------------------------------ buffs

	/**
	 * Char.add, before announcing: true when the open blow took the announcement. Its word becomes
	 * the buff's own small icon on the card (the body's look of it, a burning's fire, a stun's
	 * stars, is drawn as ever beside it), unless a piece of this blow stands for it (Piece.folds,
	 * settled at the close). A buff with no icon keeps its word (released over the card). A stun's
	 * stars wait for the blow's beat (Looks.stunning).
	 */
	public static boolean announce( Char ch, Buff buff ){
		Record r = top();
		if (r == null || ch == null || buff == null || ch.sprite == null || !ch.sprite.visible) return false;
		if (buff instanceof Paralysis) Looks.stunning( ch );
		int icon = buff.icon();
		if (icon < 0 || icon == BuffIndicator.NONE) return false;
		r.mark( ch ).buff( buff.getClass(), -(icon + 1), tintOf( buff ) );
		return true;
	}

	private static final ThreadLocal<Image> PROBE = ThreadLocal.withInitial( Image::new );

	//a buff's icon tint as one colour (its tintIcon on a bare picture): 0 for none
	public static int tintOf( Buff b ){
		//one bare picture per thread, its colour reset: a new Image is a fresh native buffer, and the
		//damage ledger asks this for every buff of both sides of a hit
		Image probe = PROBE.get();
		probe.resetColor();
		b.tintIcon( probe );
		if (probe.rm == 1f && probe.gm == 1f && probe.bm == 1f && probe.ra == 0f && probe.ga == 0f && probe.ba == 0f) return 0;
		int r = channel( probe.rm + probe.ra ), g = channel( probe.gm + probe.ga ), bl = channel( probe.bm + probe.ba );
		int c = (r << 16) | (g << 8) | bl;
		return c == 0 ? 0x010101 : c;
	}

	private static int channel( float v ){
		return Math.max( 0, Math.min( 255, Math.round( v * 255f ) ) );
	}

	// ------------------------------------------------------------------ pictures

	/** Hand the open blow a picture. True when taken: the caller must not draw it now. False (no
	 *  blow open on this thread, or the piece's character has no visible sprite): the caller draws
	 *  as it does today. */
	public static boolean offer( Piece p ){
		Record r = top();
		if (r == null || p == null || p.on == null || p.on.sprite == null || !p.on.sprite.visible) return false;
		r.pieces.add( p );
		r.any = true;
		return true;
	}

	/** A card-only chip on ch's card in the open blow. code >= 0: a text-icon cell (FloatingText
	 *  constants); code < 0: a buff icon, -(BuffIndicator index + 1); code >= HitCard.SKILL_BASE: a
	 *  hero skill's icon. tint 0: drawn plain. False when no blow is open. */
	public static boolean chip( Char ch, int code, int tint ){
		Record r = top();
		if (r == null || ch == null) return false;
		r.mark( ch ).chip( code, tint );
		return true;
	}

	// ------------------------------------------------------------------ words as icons

	/** An icon on ch's card: a chip in the open blow, else a late chip (late). What a word over a
	 *  character used to say. Any thread. */
	public static void icon( Char ch, int code, int tint ){
		if (!chip( ch, code, tint )) late( ch, code, tint );
	}

	/** A hero skill's icon (HitCard.SKILL_BASE + its image) where its shout used to float: in the
	 *  open blow on the blow's struck one (the foe for a proc, the hero for a defence, the fallen
	 *  for a kill), on the caster when the struck one is out of sight; with no blow a late chip on
	 *  the caster. Any thread. */
	public static void skillChip( Char caster, int code ){
		Record r = top();
		if (r == null){
			late( caster, code, 0 );
			return;
		}
		Char on = r.defender != null && r.defender.sprite != null && r.defender.sprite.visible ? r.defender : caster;
		if (on != null) r.mark( on ).chip( code, 0 );
	}

	/**
	 * A miss on ch: its card in the open blow shows `cell` in the left slot (MISS_DODGE, MISS_BLOCK,
	 * PARRY, GUARDED: missCell's) with no number and the reason it missed (`reason`, a miss-reason
	 * cell, -1 none) as its first chip. With no blow, a late card of the two. Any thread.
	 */
	public static void miss( Char ch, int cell, int reason ){
		if (ch == null) return;
		Record r = top();
		if (r != null){
			Mark m = r.mark( ch );
			if (m.markCell < 0){
				m.markCell = cell;
				m.markReason = reason;
			} else {
				m.chip( cell, 0 );
				if (reason >= 0) m.chip( reason, 0 );
			}
			return;
		}
		if (reason >= 0) late( ch, new int[]{ cell, reason }, new int[]{ 0, 0 }, 2 );
		else late( ch, cell, 0 );
	}

	/** A number-less mark on ch's card (IMMUNE): its left slot in the open blow (a chip when it has
	 *  one already), else a late chip. Any thread. */
	public static void mark( Char ch, int cell ){
		if (ch == null) return;
		Record r = top();
		if (r == null){
			late( ch, cell, 0 );
			return;
		}
		Mark m = r.mark( ch );
		if (m.markCell < 0) m.markCell = cell;
		else m.chip( cell, 0 );
	}

	/**
	 * Which cell a defence verb (Char.defenseVerb, called first for what it does) shows: PARRY for
	 * the hero's or a monk's parry, GUARDED for the hero's round shield, MISS_DODGE for a dodge (the
	 * plain verb, a caravaneer's duck, a velocirooster's), MISS_BLOCK for any other (the 25 that
	 * block). By the verb's text in the game's language.
	 */
	public static int missCell( Char defender, String verb ){
		if (verb == null) return FloatingText.MISS_DODGE;
		boolean hero = defender instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
		if ((hero || defender instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.Monk)
				&& verb.equals( Messages.get( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Monk.class, "parried" ) )
				&& (hero || !verb.equals( Messages.get( defender, "def_verb" ) ))){
			return FloatingText.PARRY;
		}
		if (hero && verb.equals( Messages.get( xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.RoundShield.GuardTracker.class, "guarded" ) )){
			return FloatingText.GUARDED;
		}
		if (hero
				|| verb.equalsIgnoreCase( Messages.get( Char.class, "def_verb" ) )
				|| verb.equalsIgnoreCase( Messages.get( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer.class, "def_verb" ) )
				|| verb.equalsIgnoreCase( Messages.get( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Velocirooster.class, "def_verb" ) )){
			return FloatingText.MISS_DODGE;
		}
		return FloatingText.MISS_BLOCK;
	}

	/** A miss with no blow to show it (a zap, a special attack): the defender's verb (its effects
	 *  happen) as its icon on a late card. CharSprite.showMiss's. */
	public static void missed( Char defender ){
		if (defender == null) return;
		String verb = defender.defenseVerb();
		miss( defender, missCell( defender, verb ), -1 );
	}

	/** A buff taking hold with no blow open: its own icon (as Blow.announce draws it) on a late chip.
	 *  False for a buff with no icon (it keeps its word). Any thread. */
	public static boolean lateBuff( Char ch, Buff buff ){
		if (ch == null || buff == null) return false;
		int icon = buff.icon();
		if (icon < 0 || icon == BuffIndicator.NONE) return false;
		late( ch, -(icon + 1), tintOf( buff ) );
		return true;
	}

	/** A debuff shrugged off as it came (a ward, a calm mind, a blow breaking a paralysis): RESISTED
	 *  and the debuff's own icon greyed, a chip in the open blow, else a late chip. Any thread. */
	public static void resisted( Char ch, Buff debuff ){
		if (ch == null) return;
		int icon = debuff != null ? debuff.icon() : -1;
		if (icon < 0 || icon == BuffIndicator.NONE){
			icon( ch, FloatingText.RESISTED, 0 );
			return;
		}
		if (!chip( ch, FloatingText.RESISTED, 0 )){
			late( ch, new int[]{ FloatingText.RESISTED, -(icon + 1) }, new int[]{ 0, GREYED }, 2 );
			return;
		}
		chip( ch, -(icon + 1), GREYED );
	}

	/** A resisted debuff's icon tint. */
	public static final int GREYED = 0x808080;

	/** A count where a word with a number floated ("Fury 3", "2 plates left", a chain's "x3"): an
	 *  icon (a chip code: a buff's, a skill's) and the number, as the wielder's gain cards are drawn,
	 *  over ch, a newer count replacing the one it shows (HitCard.counter); co-op guests get it as a
	 *  late small card with the number. A count of 0 or less is the icon alone (Blow.icon). Any
	 *  thread. */
	public static void counter( Char ch, int code, int amount, int color ){
		number( ch, code, amount, color, false );
	}

	/** An amount where a word with a number floated (deferred damage, gold taken): an icon and the
	 *  number over ch, summed with the live one of the same icon (HitCard.gain). Never claimed as
	 *  the blow's damage. Any thread. */
	public static void gain( Char ch, int code, int amount, int color ){
		number( ch, code, amount, color, true );
	}

	private static void number( Char ch, int code, int amount, int color, boolean sum ){
		if (ch == null) return;
		//a count of none (a bleed under 1, the last plate gone) still happened: its icon alone
		if (amount <= 0){
			if (!sum) icon( ch, code, 0 );
			return;
		}
		if (ch.sprite == null || !ch.sprite.visible) return;
		final CharSprite s = ch.sprite;
		if (sum) Fx.post( () -> HitCard.gain( s, code, amount, color ) );
		else Fx.post( () -> HitCard.counter( s, code, amount, color ) );
		if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isHost()){
			EnchantNet.sendCard( BlowPresenter.lateCard( ch, amount, color, new int[]{ code }, new int[]{ 0 } ) );
		}
	}

	/** A buff's own icon on ch's card (a barrier that turned a hit away): a chip in the open blow,
	 *  else a late chip. Nothing for a buff with no icon. Any thread. */
	public static void buffIcon( Char ch, Buff buff ){
		if (ch == null || buff == null) return;
		int icon = buff.icon();
		if (icon < 0 || icon == BuffIndicator.NONE) return;
		icon( ch, -(icon + 1), tintOf( buff ) );
	}

	/** A chip that comes when no blow is open (HitCard.late): onto ch's live card, else a small one
	 *  of its own; sent to co-op guests as a late small CARD. Nothing when ch is out of sight. Any
	 *  thread. */
	public static void late( Char ch, int code, int tint ){
		late( ch, new int[]{ code }, new int[]{ tint }, 1 );
	}

	public static void late( Char ch, int[] codes, int[] tints, int n ){
		if (ch == null || ch.sprite == null || !ch.sprite.visible || n <= 0) return;
		final CharSprite s = ch.sprite;
		final int[] c = java.util.Arrays.copyOf( codes, n ), t = java.util.Arrays.copyOf( tints, n );
		Fx.post( () -> HitCard.late( s, c, t, c.length ) );
		if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isHost()) EnchantNet.sendCard( BlowPresenter.lateCard( ch, -1, CharSprite.NEGATIVE, c, t ) );
	}

	/** What flows back to the wielder (beads, a mote) lands this many seconds after the presenter's
	 *  frame; the wielder's small card (heal, mana, shield) waits for the latest such time of the
	 *  blow. */
	public static void flowBack( Char wielder, float delay ){
		Record r = top();
		if (r == null || wielder == null || wielder != r.attacker) return;
		r.flowDelay = Math.max( r.flowDelay, delay );
		r.carried = true;
	}

	/** CharSprite.flash: false when this sprite already flashed inside the open blow. */
	public static boolean mayFlash( CharSprite s ){
		Record r = top();
		if (r == null || s == null) return true;
		for (int i = 0; i < r.flashed.size(); i++) if (r.flashed.get( i ) == s) return false;
		r.flashed.add( s );
		return true;
	}
}
