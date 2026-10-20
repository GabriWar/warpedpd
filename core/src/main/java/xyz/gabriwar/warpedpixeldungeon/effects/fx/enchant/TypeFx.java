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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxOrbit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.BlowPresenter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.HitFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Piece;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.WeaponPerk;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Flute;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Harp;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Triangolo;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Trumpet;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Wardrum;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;

import java.util.List;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.Beat;

/**
 * The weapon types' looks (docs/fx/type-looks.md): Woodsman's axes splinter and fell, Quarry's
 * hammers daze and crack the floor, the Bard's instruments play notes and chords, Lash's whips drag
 * and disarm; the reviewed types' accents (a Marksman's or Gunner's shot from afar, a Reach blow, a
 * Flurry's free hit, a Crush sunder, Momentum's stacks, a Guard's parry, a Siphon's mana, a Bulwark
 * blow spent). One lead a blow, the rest small accents (BlowPresenter): every piece here is a
 * Piece handed to the open blow (Blow.offer) and drawn by the presenter, or - no blow open, an unseen
 * sprite - drawn at once in its full form where its sprite shows. Its card chip is its own (chip) or
 * the blow's (Blow.chip, for the wielder's lane and the motions, whose chips the card does not take).
 *
 * Every entry is any thread (Fx.post), draws nothing of the game's Random (FxRandom only), changes
 * nothing of the game and keeps to its own quads. The host sends each to co-op guests as a PieceFx
 * kind (SPLINTER .. SIPHON, EnchantNet PIECE), who make it again on their own sprites.
 */
public final class TypeFx {

	private TypeFx(){}

	// ------------------------------------------------------------------ palettes

	/** Woodsman's wood (once HitFx's arrow splinters'), the lash's leather, Quarry's daze violet
	 *  (the HIT_DAZE icon's), the Bard's chords: a blessing chord's gold, a terrifying one's violet. */
	public static final int WOOD = 0xB89C6C, WOOD_DARK = 0x8A6B4A, LEATHER = 0x9A6A40, DAZE = 0x9A6AD0,
			RALLY = 0xFFE38A, DREAD = 0xA070E0;
	/** The type icons' silver, a Siphon's mana. */
	public static final int SILVER = 0xC8D6EC, MANA = 0x44CCFF;

	static final Ramp WOOD_RAMP = new Ramp( WOOD, WOOD_DARK );

	// ------------------------------------------------------------------ caps and times

	/** Own quads: a splinter, a felling blow (its throw's dust and ghosts are Pushing's), a daze,
	 *  a crack (CRACK_PER more for each of its first RICH crippled neighbours) and a crack as an accent,
	 *  a note, a chord (one more a mark, MARKS marks at most) and a chord as an accent, a resonance
	 *  (one more a struck neighbour, RICH at most), a lash's drag, a disarm, a siphon. */
	public static final int SPLINTER_CAP = 3, FELL_CAP = 4, DAZE_CAP = 2, CRACK_CAP = 10, CRACK_PER = 2, CRACK_ACCENT = 3,
			NOTE_CAP = 1, CHORD_CAP = 8, CHORD_ACCENT = 4, MARKS = 3, RESONANCE_CAP = 1, LASH_CAP = 5, UNARM_CAP = 1, SIPHON_CAP = 1,
			RICH = 3;

	/** The perk accents (PieceFx.PERK's first number): a Crush sunder, a Flurry's free hit, Momentum's
	 *  stacks, a Bulwark blow spent, a Marksman's or Gunner's shot from afar, a Reach blow, a Guard's
	 *  parry. Append only: they go on the wire. */
	public static final int SUNDER = 0, FLURRY = 1, MOMENTUM = 2, BULWARK = 3, LONGSHOT = 4, REACH = 5, PARRY = 6;
	/** Their quads (Momentum's and a shot's by their numbers: at most these). */
	public static final int[] PERK_CAP = { 2, 4, 4, 2, 4, 3, 8 };
	/** Momentum's stacks at full; how many a tint toward gold needs; a shot's distance at its full bonus. */
	public static final int FULL_STACKS = 4, TINT_STACKS = 3, FULL_SHOT = 5;

	/** A felling blow's crescent, s. A daze's turn over the head, s. A crack's mark held and faded, s;
	 *  each crippled neighbour after the last, s. */
	public static final float FELL_FOR = 0.12f, DAZE_FOR = 0.3f, CRACK_HOLD = 2f, CRACK_FADE = 0.5f, NEIGHBOUR_STEP = 0.04f;
	/** A note: its life, how far it rises (px), its fade at the end, its sway (px either way, Hz). */
	public static final float NOTE_FOR = 0.5f, NOTE_RISE = 8f, NOTE_FADE = 0.2f, NOTE_SWAY = 1f, NOTE_SWAY_HZ = 2f;
	/** A chord: its three notes' gap and flight (s) and rise (px); its ring's life (s) and alpha; its
	 *  arpeggio's gap (s); a charm's note on its way (s). */
	public static final float FAN_GAP = 0.05f, FAN_FOR = 0.4f, FAN_RISE = 12f, CHORD_RING_FOR = 0.4f, CHORD_RING_ALPHA = 0.4f,
			ARPEGGIO_GAP = 0.06f, CHARM_FOR = 0.3f;
	/** A resonance's ring alpha and each struck neighbour's star after the last, s. */
	public static final float RESONANCE_ALPHA = 0.5f, RESONANCE_STEP = 0.04f;
	/** A lash's taut line while the drag runs (Pushing's 0.15 s) and its fade, s; a disarm's tumble,
	 *  s; a siphon's mote home, s; a Bulwark's motes into the weapon, s. */
	public static final float LINE_FOR = 0.15f, LINE_FADE = 0.1f, UNARM_FOR = 0.45f, SIPHON_FOR = 0.25f, BULWARK_FOR = 0.1f;
	/** A chord's three pitches: a blessing chord major, a terrifying one minor. */
	public static final float[] RALLY_CHORD = { 1f, 1.25f, 1.5f }, DREAD_CHORD = { 1f, 1.2f, 1.5f };

	/** The instruments (a chord's sample on the wire); -1 for none. */
	public static final int FLUTE = 0, TRUMPET = 1, HARP = 2, WARDRUM = 3, TRIANGOLO = 4;
	/** Each instrument's sample, its pitch for the chord's root and its volume. */
	static final String[] SAMPLE = { Assets.Sounds.FLUTE_NOTE, Assets.Sounds.TRUMPET_NOTE, Assets.Sounds.HARP_NOTE,
			Assets.Sounds.HIT_CRUSH, Assets.Sounds.BELL };
	static final float[] ROOT = { 1f, 1f, 1f, 0.7f, 1.8f }, VOLUME = { 0.8f, 0.8f, 0.8f, 0.5f, 0.3f };

	//the felling blow's crescent: each octant sweeping into its frame from the one before, either way
	private static final int[][][] WIDE_CW = new int[8][][], WIDE_CCW = new int[8][][];
	static {
		for (int k = 0; k < 8; k++){
			WIDE_CW[k] = new int[][]{ FxFrames.SLASH_24[(k + 7) % 8], FxFrames.SLASH_24[k] };
			WIDE_CCW[k] = new int[][]{ FxFrames.SLASH_24[(k + 1) % 8], FxFrames.SLASH_24[k] };
		}
	}

	// ------------------------------------------------------------------ what a type shows on its item

	/** The silver icon a type's perk wears (a FloatingText cell, the one its event puts on a hit card),
	 *  -1 for a type with none. */
	public static int perkIcon( WeaponPerk p ){
		if (p == null) return -1;
		switch (p){
			case WOODSMAN: return FloatingText.FELL;
			case QUARRY:   return FloatingText.CRACK;
			case BARD:     return FloatingText.CHORD;
			case LASH:     return FloatingText.PULL;
			case CRUSH:    return FloatingText.SHRED;
			case MOMENTUM: return FloatingText.MOMENTUM;
			case SWIFT:    return FloatingText.FLURRY;
			case SHOT: case GUN: return FloatingText.LONGSHOT;
			case REACH:    return FloatingText.REACH;
			case GUARD:    return FloatingText.PARRY;
			case IMPACT:   return FloatingText.HEAVE;
			case KEEN:     return FloatingText.EXECUTE;
			default:       return -1;
		}
	}

	/** An instrument's place in SAMPLE, -1 for anything else. */
	public static int instrument( Item w ){
		if (w instanceof Flute) return FLUTE;
		if (w instanceof Trumpet) return TRUMPET;
		if (w instanceof Harp) return HARP;
		if (w instanceof Wardrum) return WARDRUM;
		if (w instanceof Triangolo) return TRIANGOLO;
		return -1;
	}

	/** The colour of the chord an instrument has coming: a terrifying chord's violet, else gold. */
	public static int chordColor( boolean dread ){
		return dread ? DREAD : RALLY;
	}

	// ------------------------------------------------------------------ Woodsman

	/** A Woodsman's every hit (its splinters keep the struck one's armor open): two chips of bark
	 *  flicked off the struck one away from the blow, bouncing once at its feet, and a dark splinter.
	 *  SHRED on its card when the hit added a stack, as Crush's sunder. */
	public static void splinter( Char a, Char d, boolean added ){
		if (!target( d )) return;
		Splinter p = new Splinter( a, sprite( a ), d.sprite, EnchantFx.fightSeen( a, d ), EnchantFx.involvesHero( a, d ) );
		if (!added) p.chip = -1;
		hand( p );
	}

	/** A felling blow (an alpha Woodsman's every third hit on one enemy, the rival statue's every
	 *  third), before its throw: a wide crescent along the blow in the wood's colours and three chips of
	 *  bark flung the way it is thrown. The throw itself (throwChar with WOOD) draws its dust, its wood
	 *  ghosts, its ring and any wall it slams into. FELL on its card. */
	public static void fell( Char a, Char d ){
		if (!target( d )) return;
		hand( new Fell( a, sprite( a ), d.sprite, EnchantFx.fightSeen( a, d ), EnchantFx.involvesHero( a, d ) ) );
	}

	// ------------------------------------------------------------------ Quarry

	/** A daze (Quarry): two violet specks circling the head once on the mark's ellipse, then gone -
	 *  never amber, never the stun's stars. Its card shows the Daze buff's own chip. */
	public static void daze( Char a, Char d ){
		if (!target( d )) return;
		hand( new Daze( a, sprite( a ), d.sprite, EnchantFx.fightSeen( a, d ), EnchantFx.involvesHero( a, d ) ) );
	}

	/** An alpha Quarry's dazing blow cracking the floor (and the rival statue's): the daze specks with
	 *  it, a crack under the struck one, a ring of dust, pebbles, and two pebbles hopping at the feet of
	 *  each neighbour it cripples (`crippled`, the struck one aside). CRACK on its card, its Cripple
	 *  folded into it. */
	public static void crack( Char a, Char d, List<Char> crippled ){
		if (!target( d )) return;
		CharSprite as = sprite( a );
		boolean seen = EnchantFx.fightSeen( a, d ), hero = EnchantFx.involvesHero( a, d );
		final Crack c = new Crack( a, as, d.sprite, sprites( crippled, d ), seen, hero );
		Daze z = new Daze( a, as, d.sprite, seen, hero );
		z.crack = c;
		if (Blow.offer( c )){
			hand( z );
			return;
		}
		//outside a blow the crack leads, so the specks keep away: the crack alone
		Fx.post( () -> c.play( true ) );
	}

	// ------------------------------------------------------------------ Bard

	/** A Bard's landed hit: a note rising from the weapon side in the colour of the chord coming. */
	public static void note( Char wielder, boolean dread ){
		if (!target( wielder )) return;
		hand( new Note( wielder, wielder.sprite, dread, EnchantFx.fightSeen( wielder, null ) ) );
	}

	/**
	 * A Bard's chord, centred on its wielder: three notes fanning up, a ring on the ground in the
	 * chord's colour, a mark over the first MARKS of those it blessed (a rising plus) or terrified (an
	 * exclamation), a note flying to the one it charmed (null: none), and its arpeggio on the
	 * instrument's sample. CHORD on the struck one's card; Bless, Terror and Charm folded into it.
	 */
	public static void chord( Char wielder, Char d, Item weapon, boolean dread, List<Char> blessed, List<Char> terrified,
			Char charmed ){
		if (!target( wielder )) return;
		Char on = d != null && d.sprite != null ? d : wielder;
		CharSprite[] marked = sprites( dread ? terrified : blessed, null );
		hand( new Chord( on, wielder.sprite, on.sprite, dread, instrument( weapon ), marked,
				charmed != null ? charmed.sprite : null, EnchantFx.fightSeen( wielder, d ), EnchantFx.involvesHero( wielder, d ) ) );
	}

	/** An instrument's resonance (each landed hit striking the other side beside the struck one): a ring
	 *  round the struck one in the colour of the chord coming, and a small star on each of the first
	 *  RICH it struck (`struck`), one after another. Nothing when it struck no one: the ring stands for
	 *  the splash. */
	public static void resonance( Char a, Char d, Item weapon, List<Char> struck ){
		if (struck == null || struck.isEmpty() || !target( d )) return;
		Quality q = Quality.of( weapon );
		hand( new Resonance( a, sprite( a ), d.sprite, q != null && q.dreadNext, sprites( struck, d ),
				EnchantFx.fightSeen( a, d ) ) );
	}

	// ------------------------------------------------------------------ Lash

	/** A Lash's drag (the pull is a plain Pushing): the whip's crack at the struck one and, while it is
	 *  pulled, the taut line from the wielder's hand to it. PULL on its card, and its Cripple's icon beside it. */
	public static void lash( Char a, Char d ){
		if (!target( d )) return;
		Lash p = new Lash( a, sprite( a ), d.sprite, EnchantFx.fightSeen( a, d ) );
		if (Blow.offer( p )){
			Blow.chip( d, FloatingText.PULL, 0 );
			return;
		}
		Fx.post( () -> p.play( true ) );
	}

	/** A Lash's disarm (an alpha's, the rival statue's): a spark knocked from the struck one's grip,
	 *  tumbling to its feet. Its card shows the Disarmed buff's own chip. */
	public static void unarm( Char a, Char d ){
		if (!target( d )) return;
		hand( new Unarm( a, sprite( a ), d.sprite, EnchantFx.fightSeen( a, d ) ) );
	}

	// ------------------------------------------------------------------ the reviewed types

	/** A Siphon's mana: one blue mote from the struck one into the wielder's chest; the mana's small
	 *  card waits for it (Blow.flowBack). */
	public static void siphon( Char wielder, Char d ){
		if (!target( wielder ) || !target( d )) return;
		Siphon p = new Siphon( wielder, wielder.sprite, d.sprite );
		if (Blow.offer( p )){
			Blow.flowBack( wielder, BlowPresenter.BEAT + SIPHON_FOR );
			return;
		}
		Fx.post( () -> p.play( true ) );
	}

	/** A Crush sunder (an alpha Crush's every hit): two flakes of steel off the struck one; SHRED on its
	 *  card when the hit added a stack. */
	public static void sunder( Char a, Char d, boolean added ){
		perk( SUNDER, a, d, added ? 1 : 0, added ? FloatingText.SHRED : -1 );
	}

	/** A Flurry's free hit (every fifth): a wink at the weapon side and a small burst. FLURRY on the
	 *  struck one's card. */
	public static void flurry( Char a, Char d ){
		perk( FLURRY, a, d, 0, FloatingText.FLURRY );
	}

	/** Momentum's stacks after a hit: a mote of gold a stack under full, a gold star and sparks at full
	 *  (MOMENTUM on the card); from TINT_STACKS the blow's star and crescent lean toward gold (the
	 *  swing's tint: an enchanted weapon's own wins). */
	public static void momentum( Char a, Char d, int stacks ){
		if (target( d ) && stacks >= TINT_STACKS){
			final CharSprite ds = d.sprite;
			Fx.post( () -> lean( ds ) );
		}
		perk( MOMENTUM, a, d, stacks, stacks >= FULL_STACKS ? FloatingText.MOMENTUM : -1 );
	}

	/** A Marksman's or Gunner's shot past two tiles: a silver mark at the struck one, bigger the bigger
	 *  the bonus (one quad at three tiles, two at four, four at five and on); LONGSHOT on its card
	 *  whenever the bonus applies. */
	public static void longshot( Char a, Char d, int dist ){
		perk( LONGSHOT, a, d, dist, FloatingText.LONGSHOT );
	}

	/** A Reach blow from two tiles or more: a silver tip where the blow lands; REACH on its card. */
	public static void reach( Char a, Char d ){
		perk( REACH, a, d, 0, FloatingText.REACH );
	}

	/** A Bulwark blow spent (GearPerk.heroHit): two steel motes from the chest into the weapon side;
	 *  BULWARK on the struck one's card. */
	public static void bulwark( Char hero, Char d ){
		perk( BULWARK, hero, d, 0, FloatingText.BULWARK );
	}

	/** An alpha Guard's riposte, on the hero who turned the blow aside: the parry's star and sparks as
	 *  the blow's twist; PARRY on the hero's card (the blow struck at him), the answer's number on the
	 *  attacker's own. */
	public static void parry( Char attacker, Char hero, int damage ){
		perk( PARRY, attacker, hero, damage, FloatingText.PARRY );
	}

	//a perk's accent: made, handed to the blow, its chip where the card shows it
	private static void perk( int code, Char a, Char d, int n, int chip ){
		if (!target( d )) return;
		boolean wielder = code == FLURRY || code == BULWARK;
		if (wielder && !target( a )) return;
		Perk p = new Perk( code, a, sprite( a ), d.sprite, n, EnchantFx.fightSeen( a, d ), EnchantFx.involvesHero( a, d ) );
		if (!wielder) p.chip = chip;
		if (Blow.offer( p )){
			//the wielder's lane's chips are not the card's: the blow's
			if (wielder && chip >= 0) Blow.chip( d, chip, 0 );
			return;
		}
		Fx.post( () -> p.play( true ) );
		//a riposte to a hit dealt outside any attack (a kobold icemancer's zap): no card to land on, so
		//its PARRY rises alone over the hero, where its word used to (the other perks only act inside
		//the hero's own blows)
		if (code == PARRY && !Blow.chip( d, chip, 0 )) d.sprite.showStatusWithIcon( CharSprite.POSITIVE, "", chip );
	}

	//the struck one's star and crescent leaning toward gold (the swing's tint, as an enchanted weapon's
	//plain blow): nothing over an enchantment's stamp. Render thread
	private static void lean( CharSprite ds ){
		float now = Game.timeTotal;
		if (ds.parent == null || now < ds.blowUntil) return;
		EnchantState st = EnchantState.of( ds );
		if (now < st.swingUntil && st.swingLook != null && st.swingLook != PieceFx.GOLD) return;
		st.swingLook = PieceFx.GOLD;
		st.swingUntil = now + EnchantFx.STAMP_FOR;
	}

	// ------------------------------------------------------------------ the pieces

	//a piece handed to the open blow, or drawn now in its full form
	private static void hand( final TypePiece p ){
		if (Blow.offer( p )) return;
		Fx.post( () -> p.play( true ) );
	}

	/**
	 * A type's piece: what it sends, its quads, its full form (leading, or alone outside a blow), its
	 * accent form (on its beat; the full form unless it says otherwise) and its sound. A type's
	 * accent always draws, on its own beat.
	 */
	abstract static class TypePiece extends Piece {

		final CharSprite as, ds;
		final boolean seen, hero;

		TypePiece( int kind, int rank, int lane, Char on, CharSprite as, CharSprite ds, boolean seen, boolean hero ){
			super( kind, rank, lane, on );
			this.as = as;
			this.ds = ds;
			this.seen = seen;
			this.hero = hero;
		}

		/** Its numbers on the wire, its kind first. */
		abstract int[] wire();

		/** The quads its full form may make; its accent's. */
		abstract int own();

		int accentOwn(){
			return own();
		}

		/** The sprite it draws on. */
		CharSprite where(){
			return ds;
		}

		/** Its full form from a tally. */
		abstract void full( int[] t, boolean mayShake, boolean mayLight );

		/** Its accent form from a tally at (x, y) (the presenter's point; a placed piece keeps its own). */
		void small( int[] t, float x, float y ){
			full( t, false, false );
		}

		/** Its sound: leading (its sting), or on its accent's beat. */
		void sound( boolean leading ){}

		@Override
		public void send( int serial ){
			EnchantNet.sendPiece( wire(), serial );
		}

		@Override
		public void sting(){
			if (seen) sound( true );
		}

		@Override
		public void lead( boolean mayShake ){
			if (!PieceFx.drawable( where() )) return;
			full( new int[]{ own() }, mayShake, true );
		}

		@Override
		public void accent( float x, float y ){
			if (!PieceFx.drawable( where() )) return;
			if (seen) sound( false );
			small( new int[]{ accentOwn() }, x, y );
		}

		/** Outside a blow (the host's, sent first) or a guest's without its card: its full form now,
		 *  with its sound. Render thread. */
		void play( boolean send ){
			if (send && NetManager.isHost()) EnchantNet.sendPiece( wire() );
			if (!PieceFx.drawable( where() )) return;
			if (seen) sound( true );
			full( new int[]{ own() }, hero, true );
		}

		//a beat of a look between its sprites, from a tally (no light unless it may)
		Beat beat( EnchantLook l, int[] t, boolean light ){
			return new Beat( l, as, ds, EnchantFx.STRONG, 0, 0, 0, Beat.RICH, false, false, hero, t, light ? -1 : 0 );
		}
	}

	// ------------------------------------------------------------------ Woodsman's

	static final class Splinter extends TypePiece {

		Splinter( Char a, CharSprite as, CharSprite ds, boolean seen, boolean hero ){
			super( PieceFx.SPLINTER, Blow.MINOR, Blow.TARGET, ds.ch, as, ds, seen, hero );
			chip = FloatingText.SHRED;
		}

		@Override int[] wire(){ return new int[]{ PieceFx.SPLINTER, PieceFx.id( as ), PieceFx.id( ds ) }; }

		@Override int own(){ return SPLINTER_CAP; }

		@Override
		void full( int[] t, boolean mayShake, boolean mayLight ){
			Beat b = beat( PieceFx.STEEL, t, false );
			chips( b, b.cx + b.dx * 3f, b.cy, 2, WOOD, true );
		}

		@Override
		void small( int[] t, float x, float y ){
			Beat b = beat( PieceFx.STEEL, t, false );
			chips( b, x + b.dx * 6f, y, 2, WOOD, true );
		}
	}

	//chips of bark flicked off along the blow's way (and a splinter of the dark wood), bouncing once
	//at the struck one's feet
	static int chips( Beat b, float x, float y, int n, int color, boolean splinter ){
		int made = 0;
		for (int i = 0; i < n; i++){
			FxParticle p = b.mat( x, y, i == 0 ? FxBudget.P1 : FxBudget.P2 );
			if (p == null) break;
			p.look( FxFrames.BARK[i % 2] ).paint( color ).vel( b.dx * FxRandom.Float( 18f, 34f ) + FxRandom.Float( -8f, 8f ),
					-FxRandom.Float( 32f, 48f ) ).gravity( 160f ).ground( b.feet + FxRandom.Float( 0f, 2f ), FxParticle.BOUNCE, 0.3f )
					.spin( 0.1f ).life( 0.6f ).alpha( 1f, 0f, 0.2f );
			made++;
		}
		if (splinter){
			FxParticle p = b.mat( x, y, FxBudget.P2 );
			if (p != null){
				p.look( FxFrames.SPARK_2 ).paint( WOOD_DARK ).orient().vel( b.dx * FxRandom.Float( 30f, 45f ), -FxRandom.Float( 20f, 34f ) )
						.gravity( 160f ).ground( b.feet + 1f, FxParticle.BOUNCE, 0.3f ).life( 0.5f ).alpha( 1f, 0f, 0.2f );
				made++;
			}
		}
		return made;
	}

	static final class Fell extends TypePiece {

		Fell( Char a, CharSprite as, CharSprite ds, boolean seen, boolean hero ){
			super( PieceFx.FELL, Blow.TWIST, Blow.TARGET, ds.ch, as, ds, seen, hero );
			chip = FloatingText.FELL;
			priority = Blow.A_TWIST;
		}

		@Override int[] wire(){ return new int[]{ PieceFx.FELL, PieceFx.id( as ), PieceFx.id( ds ) }; }

		@Override int own(){ return FELL_CAP; }

		@Override
		void sound( boolean leading ){
			if (leading) PieceFx.sting( PieceFx.FELL, Assets.Sounds.WOOD_BREAK, ds.ch, 0.4f, 1.1f, Sting.SAME_GAP );
		}

		@Override
		void full( int[] t, boolean mayShake, boolean mayLight ){
			Beat b = beat( PieceFx.STEEL, t, false );
			FxParticle c = b.lit( b.cx, b.cy, FxBudget.P0 );
			if (c != null){
				int k = HitFx.octant( b.dx, b.dy );
				c.look( (ds.flipHorizontal ? WIDE_CCW : WIDE_CW)[k], 0 ).ramp( WOOD_RAMP, true ).life( FELL_FOR )
						.alpha( 0.95f, 0f, FELL_FOR * 0.5f ).snap();
			}
			fling( b, b.cx, b.cy, WOOD, WOOD_DARK );
		}

		//its accent: the chips alone, in the colours of the proc that led
		@Override
		void small( int[] t, float x, float y ){
			Beat b = beat( PieceFx.STEEL, t, false );
			EnchantLook stamp = Game.timeTotal < ds.blowUntil ? ds.blowLook : null;
			fling( b, x, y, stamp != null ? stamp.light : WOOD, stamp != null ? stamp.strike.ramp.stop( 3 ) : WOOD_DARK );
		}

		//three chips of bark flung the way the struck one is thrown
		private static void fling( Beat b, float x, float y, int c0, int c1 ){
			for (int i = 0; i < 3; i++){
				FxParticle p = b.mat( x, y, i == 0 ? FxBudget.P1 : FxBudget.P2 );
				if (p == null) break;
				p.look( FxFrames.BARK[i % 2] ).paint( i == 1 ? c1 : c0 )
						.vel( b.dx * FxRandom.Float( 40f, 60f ) + b.dy * FxRandom.Float( -10f, 10f ), b.dy * FxRandom.Float( 30f, 50f ) - FxRandom.Float( 26f, 40f ) )
						.gravity( 160f ).ground( b.feet + FxRandom.Float( 0f, 2f ), FxParticle.BOUNCE, 0.3f ).spin( 0.08f ).life( 0.65f )
						.alpha( 1f, 0f, 0.2f );
			}
		}
	}

	// ------------------------------------------------------------------ Quarry's

	static final class Daze extends TypePiece {

		/** The crack of the same blow on the same sprite, null for none: the specks keep away when it led. */
		Crack crack;

		Daze( Char a, CharSprite as, CharSprite ds, boolean seen, boolean hero ){
			super( PieceFx.DAZE, Blow.MINOR, Blow.PLACED, ds.ch, as, ds, seen, hero );
		}

		@Override int[] wire(){ return new int[]{ PieceFx.DAZE, PieceFx.id( as ), PieceFx.id( ds ) }; }

		@Override int own(){ return DAZE_CAP; }

		@Override
		void full( int[] t, boolean mayShake, boolean mayLight ){
			if (crack != null && crack.led) return;
			for (int i = 0; i < 2 && t[0] > 0; i++){
				if (!FxBudget.keep( FxBudget.P1 )) break;
				Mote m = Mote.make( FxFrames.PLUS_3, DAZE, 0.9f, true );
				if (m == null) break;
				m.orbit( ds, (float)(Math.PI * i), DAZE_FOR );
				t[0]--;
			}
		}
	}

	static final class Crack extends TypePiece {

		final CharSprite[] around;
		/** Whether its full form drew (it led, or played alone): its blow's daze then draws nothing. */
		boolean led;

		Crack( Char a, CharSprite as, CharSprite ds, CharSprite[] around, boolean seen, boolean hero ){
			super( PieceFx.CRACK, Blow.TWIST, Blow.TARGET, ds.ch, as, ds, seen, hero );
			this.around = around;
			chip = FloatingText.CRACK;
			priority = Blow.A_TWIST;
			folds = new Class<?>[]{ Cripple.class };
		}

		@Override int[] wire(){ return withIds( new int[]{ PieceFx.CRACK, PieceFx.id( as ), PieceFx.id( ds ) }, around ); }

		@Override int own(){ return CRACK_CAP + CRACK_PER * Math.min( RICH, around.length ); }

		@Override int accentOwn(){ return CRACK_ACCENT; }

		@Override
		void sound( boolean leading ){
			if (leading) PieceFx.sting( PieceFx.CRACK, Assets.Sounds.ROCKS_LIGHT, ds.ch, 0.45f, 0.85f, Sting.SAME_GAP );
		}

		@Override
		void full( int[] t, final boolean mayShake, boolean mayLight ){
			led = true;
			final Beat b = beat( PieceFx.force( Element.EARTH ), t, false );
			if (!b.onWater( b.cx, b.feet )) b.decal( b.cx, b.feet + 1f, FxFrames.CRACK_17[ds.flipHorizontal ? 1 : 0], ForceFx.CRACK,
					0.6f, CRACK_HOLD, CRACK_FADE );
			b.ring( b.cx, b.feet, 13, 31, 0.4f, Element.EARTH.light, 0.5f, false );
			for (int i = 0; i < 3; i++) ForceFx.pebble( b, b.cx + (i - 1) * 4f, b.feet - 3f );
			if (mayShake && hero) Fx.shake( 1f, 0.1f );
			//the crippled neighbours, nearest first, one after another
			CharSprite[] near = around.clone();
			order( near, b.cx, b.cy );
			for (int i = 0; i < near.length && i < RICH; i++){
				final CharSprite n = near[i];
				if (n == null) continue;
				final Beat share = b.split( CRACK_PER );
				EnchantFx.later( NEIGHBOUR_STEP * (i + 1), () -> {
					if (n.parent == null || !n.visible) return;
					share.feet = n.y + n.height() - 2f;
					float nx = n.x + n.width() / 2f;
					for (int k = 0; k < CRACK_PER; k++) ForceFx.pebble( share, nx + (k == 0 ? -2f : 2f ), share.feet - 2f );
				} );
			}
		}

		//its accent: a crack under the feet that fades on its own (no decal) and two pebbles
		@Override
		void small( int[] t, float x, float y ){
			Beat b = beat( PieceFx.force( Element.EARTH ), t, false );
			FxParticle p = b.mat( b.cx, b.feet + 1f, FxBudget.P1 );
			if (p != null) p.look( FxFrames.CRACK_11[ds.flipHorizontal ? 1 : 0] ).paint( ForceFx.CRACK ).life( 1.2f )
					.alpha( 0.6f, 0f, 0.4f ).snap();
			for (int i = 0; i < 2; i++) ForceFx.pebble( b, b.cx + (i == 0 ? -3f : 3f ), b.feet - 3f );
		}
	}

	// ------------------------------------------------------------------ the Bard's

	static final class Note extends TypePiece {

		final boolean dread;

		Note( Char wielder, CharSprite ws, boolean dread, boolean seen ){
			super( PieceFx.NOTE, Blow.MINOR, Blow.WIELDER, wielder, ws, ws, seen, false );
			this.dread = dread;
		}

		@Override int[] wire(){ return new int[]{ PieceFx.NOTE, PieceFx.id( as ), dread ? 1 : 0 }; }

		@Override int own(){ return NOTE_CAP; }

		@Override CharSprite where(){ return as; }

		@Override
		void full( int[] t, boolean mayShake, boolean mayLight ){
			rise( t, PieceFx.handX( as ), PieceFx.handY( as ) );
		}

		@Override
		void small( int[] t, float x, float y ){
			rise( t, x, y );
		}

		private void rise( int[] t, float x, float y ){
			Beat b = Beat.at( PieceFx.GOLD, x, y, t );
			FxParticle p = b.lit( x, y - 2f, FxBudget.P1 );
			if (p == null) return;
			float rate = (float)(2 * Math.PI * NOTE_SWAY_HZ);
			p.look( FxFrames.NOTE_5 ).paint( chordColor( dread ) ).vel( 0f, -NOTE_RISE / NOTE_FOR ).sway( NOTE_SWAY * rate, rate )
					.life( NOTE_FOR ).alpha( 1f, 0f, NOTE_FADE ).snap();
		}
	}

	static final class Chord extends TypePiece {

		final boolean dread;
		final int instrument;
		final CharSprite[] marked;
		final CharSprite charmed;

		Chord( Char on, CharSprite ws, CharSprite ds, boolean dread, int instrument, CharSprite[] marked, CharSprite charmed,
				boolean seen, boolean hero ){
			super( PieceFx.CHORD, Blow.TWIST, Blow.PLACED, on, ws, ds, seen, hero );
			this.dread = dread;
			this.instrument = instrument;
			this.marked = marked;
			this.charmed = charmed;
			chip = FloatingText.CHORD;
			priority = Blow.A_TWIST;
			folds = new Class<?>[]{ Bless.class, Terror.class, Charm.class };
		}

		@Override
		int[] wire(){
			return withIds( new int[]{ PieceFx.CHORD, PieceFx.id( as ), PieceFx.id( ds ), dread ? 1 : 0, instrument,
					PieceFx.id( charmed ) }, marked );
		}

		@Override int own(){ return CHORD_CAP + Math.min( MARKS, marked.length ); }

		@Override int accentOwn(){ return CHORD_ACCENT; }

		@Override CharSprite where(){ return as; }

		@Override
		void sound( boolean leading ){
			arpeggio( instrument, dread, as.ch, leading ? 1f : 0.6f );
			if (charmed != null) PieceFx.sting( PieceFx.CHORD, Assets.Sounds.CHARMS, charmed.ch, 0.25f, 1.3f, Sting.SAME_GAP );
		}

		@Override
		void full( int[] t, boolean mayShake, boolean mayLight ){
			final int color = chordColor( dread );
			final float cx = as.x + as.width() / 2f, feet = as.y + as.height() - 2f, top = as.y + 2f;
			final Beat b = Beat.at( PieceFx.GOLD, cx, as.y + as.height() / 2f, t );
			b.feet = feet;
			b.ring( cx, feet, 23, 55, CHORD_RING_FOR, color, CHORD_RING_ALPHA, true );
			//three notes fanning up from the wielder, one after another
			for (int i = 0; i < 3; i++){
				final Beat share = b.split( 1 );
				final float angle = (float)(-Math.PI / 2 + (i - 1) * 0.5f);
				EnchantFx.later( FAN_GAP * i, () -> {
					if (as.parent == null) return;
					FxParticle p = share.lit( as.x + as.width() / 2f, top, FxBudget.P1 );
					if (p != null) p.look( FxFrames.NOTE_5 ).paint( color ).polar( angle, FAN_RISE / FAN_FOR ).life( FAN_FOR )
							.alpha( 1f, 0f, 0.15f ).snap();
				} );
			}
			charm( b );
			marks( b, MARKS );
		}

		//its accent: a smaller ring, the charm's note and the marks
		@Override
		void small( int[] t, float x, float y ){
			float cx = as.x + as.width() / 2f, feet = as.y + as.height() - 2f;
			Beat b = Beat.at( PieceFx.GOLD, cx, as.y + as.height() / 2f, t );
			b.feet = feet;
			b.ring( cx, feet, 9, 23, CHORD_RING_FOR, chordColor( dread ), CHORD_RING_ALPHA, true );
			charm( b );
			marks( b, MARKS );
		}

		//a note flying from the wielder to the one charmed, gone as it arrives (its hearts take over)
		private void charm( Beat b ){
			if (charmed == null || charmed.parent == null || !charmed.visible) return;
			float fx = as.x + as.width() / 2f, fy = as.y + 3f;
			float tx = charmed.x + charmed.width() / 2f, ty = charmed.y + charmed.height() / 2f - 2f;
			FxParticle p = b.lit( fx, fy, FxBudget.P1 );
			if (p != null) p.look( FxFrames.NOTE_5 ).paint( chordColor( dread ) ).vel( (tx - fx) / CHARM_FOR, (ty - fy) / CHARM_FOR )
					.life( CHARM_FOR ).alpha( 1f, 0f, 0.06f ).snap();
		}

		//over the first `most` it blessed a rising plus of holy light, over those it terrified an exclamation
		private void marks( Beat b, int most ){
			for (int i = 0; i < marked.length && i < most; i++){
				CharSprite m = marked[i];
				if (m == null || m.parent == null || !m.visible) continue;
				float mx = m.x + m.width() / 2f;
				FxParticle p = b.lit( mx, dread ? m.y - 5f : m.y - 1f, FxBudget.P1 );
				if (p == null) return;
				if (dread){
					p.look( FxFrames.EXCLAIM, 0 ).paint( DREAD ).life( 0.5f ).alpha( 1f, 0f, 0.15f ).snap();
				} else {
					p.look( FxFrames.PLUS_3 ).paint( Element.HOLY.light ).vel( 0f, -10f ).life( 0.5f ).alpha( 1f, 0f, 0.2f ).snap();
				}
			}
		}
	}

	/** A chord's arpeggio: three fixed pitches ARPEGGIO_GAP s apart on the instrument's sample, major
	 *  for a blessing chord, minor for a terrifying one, `share` of its volume. No random of either
	 *  kind. Render thread. */
	static void arpeggio( int instrument, boolean dread, final Char at, float share ){
		if (instrument < 0 || instrument >= SAMPLE.length) return;
		final String sample = SAMPLE[instrument];
		final float[] chord = dread ? DREAD_CHORD : RALLY_CHORD;
		final float root = ROOT[instrument], vol = VOLUME[instrument] * share;
		Sting.out.play( sample, at, vol, root * chord[0] );
		for (int i = 1; i < chord.length; i++){
			final float pitch = root * chord[i];
			EnchantFx.later( ARPEGGIO_GAP * i, () -> Sting.out.play( sample, at, vol, pitch ) );
		}
	}

	static final class Resonance extends TypePiece {

		final boolean dread;
		final CharSprite[] struck;

		Resonance( Char a, CharSprite as, CharSprite ds, boolean dread, CharSprite[] struck, boolean seen ){
			super( PieceFx.RESONANCE, Blow.MINOR, Blow.PLACED, ds.ch, as, ds, seen, false );
			this.dread = dread;
			this.struck = struck;
		}

		@Override
		int[] wire(){
			return withIds( new int[]{ PieceFx.RESONANCE, PieceFx.id( as ), PieceFx.id( ds ), dread ? 1 : 0 }, struck );
		}

		@Override int own(){ return RESONANCE_CAP + Math.min( RICH, struck.length ); }

		@Override
		void full( int[] t, boolean mayShake, boolean mayLight ){
			final int color = chordColor( dread );
			Beat b = beat( PieceFx.GOLD, t, false );
			b.ring( b.cx, b.feet, 9, 23, 0.35f, color, RESONANCE_ALPHA, true );
			for (int i = 0; i < struck.length && i < RICH; i++){
				final CharSprite n = struck[i];
				if (n == null) continue;
				final Beat share = b.split( 1 );
				EnchantFx.later( RESONANCE_STEP * (i + 1), () -> {
					if (n.parent == null || !n.visible) return;
					FxParticle p = share.lit( n.x + n.width() / 2f, n.y + n.height() / 2f, FxBudget.P1 );
					if (p != null) p.look( FxFrames.STAR4_5 ).paint( color ).life( 0.15f ).alpha( 0.9f, 0f, 0.08f ).snap();
				} );
			}
		}
	}

	// ------------------------------------------------------------------ Lash's

	static final class Lash extends TypePiece {

		Lash( Char a, CharSprite as, CharSprite ds, boolean seen ){
			super( PieceFx.LASH, Blow.MINOR, Blow.MOTION, ds.ch, as, ds, seen, false );
		}

		@Override int[] wire(){ return new int[]{ PieceFx.LASH, PieceFx.id( as ), PieceFx.id( ds ) }; }

		@Override int own(){ return LASH_CAP; }

		@Override
		void sound( boolean leading ){
			PieceFx.sting( PieceFx.LASH, Assets.Sounds.HIT_SLASH, ds.ch, 0.35f, 1.5f, Sting.SAME_GAP );
		}

		@Override
		void full( int[] t, boolean mayShake, boolean mayLight ){
			Beat b = beat( PieceFx.STEEL, t, false );
			//the crack at the struck one: a white star for a blink, a spark off the whip's tip
			FxParticle s = b.lit( b.hx, b.hy, FxBudget.P0 );
			if (s != null) s.look( FxFrames.IMPACT_STAR, 0 ).paint( PieceFx.WHITE ).life( 0.1f ).alpha( 1f, 0f, 0.05f ).snap();
			FxParticle k = b.lit( b.hx, b.hy, FxBudget.P1 );
			if (k != null) k.look( FxFrames.SPARK_2 ).paint( PieceFx.WHITE ).orient()
					.polar( (float)Math.atan2( -b.dy, -b.dx ) + FxRandom.Float( -0.5f, 0.5f ), FxRandom.Float( 40f, 60f ) ).drag( 3f )
					.life( 0.18f ).alpha( 1f, 0f, 0.08f );
			//the taut line while it is pulled in
			if (as == null || as.parent == null) return;
			for (int i = 1; i <= 3 && b.left() > 0; i++){
				if (!FxBudget.keep( FxBudget.P1 )) break;
				Mote m = Mote.make( FxFrames.MOTE_1, LEATHER, 1f, false );
				if (m == null) break;
				m.line( as, ds, i / 4f, LINE_FOR, LINE_FADE );
				b.take( 1 );
			}
		}
	}

	static final class Unarm extends TypePiece {

		Unarm( Char a, CharSprite as, CharSprite ds, boolean seen ){
			super( PieceFx.UNARM, Blow.MINOR, Blow.PLACED, ds.ch, as, ds, seen, false );
		}

		@Override int[] wire(){ return new int[]{ PieceFx.UNARM, PieceFx.id( as ), PieceFx.id( ds ) }; }

		@Override int own(){ return UNARM_CAP; }

		@Override
		void sound( boolean leading ){
			PieceFx.sting( PieceFx.UNARM, Assets.Sounds.HIT_PARRY, ds.ch, 0.3f, 1.6f, Sting.SAME_GAP );
		}

		//a spark knocked from its grip, tumbling off its weapon side to its feet
		@Override
		void full( int[] t, boolean mayShake, boolean mayLight ){
			Beat b = beat( PieceFx.STEEL, t, false );
			float side = ds.flipHorizontal ? -1f : 1f;
			FxParticle p = b.lit( PieceFx.handX( ds ), PieceFx.handY( ds ), FxBudget.P1 );
			if (p != null) p.look( FxFrames.SPARK_3 ).paint( Element.STEEL.light ).vel( side * FxRandom.Float( 18f, 28f ), -FxRandom.Float( 48f, 58f ) )
					.gravity( 200f ).ground( b.feet + 1f, FxParticle.BOUNCE, 0.35f ).spin( 0.06f ).life( UNARM_FOR ).alpha( 1f, 0f, 0.15f );
		}
	}

	// ------------------------------------------------------------------ the reviewed types'

	static final class Siphon extends TypePiece {

		Siphon( Char wielder, CharSprite ws, CharSprite ds ){
			super( PieceFx.SIPHON, Blow.MINOR, Blow.WIELDER, wielder, ws, ds, true, false );
		}

		@Override int[] wire(){ return new int[]{ PieceFx.SIPHON, PieceFx.id( as ), PieceFx.id( ds ) }; }

		@Override int own(){ return SIPHON_CAP; }

		@Override CharSprite where(){ return as; }

		//one blue mote from the struck one into the wielder's chest
		@Override
		void full( int[] t, boolean mayShake, boolean mayLight ){
			if (ds == null || ds.parent == null) return;
			float fx = ds.x + ds.width() / 2f, fy = ds.y + ds.height() / 2f;
			float tx = as.x + as.width() / 2f, ty = as.y + as.height() / 2f - 1f;
			Beat b = Beat.at( PieceFx.GOLD, fx, fy, t );
			FxParticle p = b.lit( fx, fy, FxBudget.P1 );
			if (p != null) p.look( FxFrames.MOTE_2 ).paint( MANA ).vel( (tx - fx) / SIPHON_FOR, (ty - fy) / SIPHON_FOR )
					.life( SIPHON_FOR ).alpha( 0.9f, 0.02f, 0.05f );
		}
	}

	static final class Perk extends TypePiece {

		final int code, n;

		Perk( int code, Char a, CharSprite as, CharSprite ds, int n, boolean seen, boolean hero ){
			super( PieceFx.PERK, code == PARRY ? Blow.TWIST : Blow.MINOR, code == FLURRY || code == BULWARK ? Blow.WIELDER : Blow.TARGET,
					code == FLURRY || code == BULWARK ? as.ch : ds.ch, as, ds, seen, hero );
			this.code = code;
			this.n = n;
			if (code == PARRY) priority = Blow.A_TWIST;
		}

		@Override int[] wire(){ return new int[]{ PieceFx.PERK, code, PieceFx.id( as ), PieceFx.id( ds ), n }; }

		@Override
		int own(){
			switch (code){
				case MOMENTUM: return Math.max( 1, Math.min( FULL_STACKS, n ) );
				case LONGSHOT: return n >= FULL_SHOT ? 4 : n == FULL_SHOT - 1 ? 2 : 1;
				default:       return PERK_CAP[code];
			}
		}

		@Override int accentOwn(){ return code == PARRY ? 2 : own(); }

		@Override CharSprite where(){ return lane == Blow.WIELDER ? as : ds; }

		@Override
		void sound( boolean leading ){
			if (leading && code == PARRY) PieceFx.sting( PieceFx.PERK, Assets.Sounds.HIT_PARRY, ds.ch, 0.5f, 1.1f, Sting.SAME_GAP );
		}

		@Override
		void full( int[] t, boolean mayShake, boolean mayLight ){
			if (code == PARRY){
				//the parry's own blow: the star and the steel's sparks of HitFx's PARRY style
				float fx = as != null ? as.x + as.width() / 2f : ds.x + (ds.flipHorizontal ? -8f : 8f);
				float fy = as != null ? as.y + as.height() / 2f : ds.y + ds.height() / 2f;
				HitFx.draw( ds, fx, fy, Math.max( 1, n ), HitFx.PARRY, hero, mayShake, false );
				return;
			}
			if (lane == Blow.WIELDER) small( t, PieceFx.handX( as ), PieceFx.handY( as ) );
			else {
				Beat b = beat( PieceFx.STEEL, t, false );
				small( t, b.hx, b.hy );
			}
		}

		@Override
		void small( int[] t, float x, float y ){
			Beat b = beat( PieceFx.STEEL, t, false );
			switch (code){
				case SUNDER:
					//two flakes of steel off the struck one, away from the blow, bouncing once (the first
					//always: it is the sunder's sign)
					for (int i = 0; i < 2; i++){
						FxParticle p = b.mat( x, y, i == 0 ? FxBudget.P0 : FxBudget.P2 );
						if (p == null) break;
						p.look( FxFrames.SPARK_2 ).paint( Element.STEEL.ramp.stop( 2 + i ) ).orient()
								.vel( b.dx * FxRandom.Float( 22f, 38f ) + FxRandom.Float( -8f, 8f ), -FxRandom.Float( 26f, 40f ) ).gravity( 200f )
								.ground( b.feet + FxRandom.Float( 0f, 2f ), FxParticle.BOUNCE, 0.3f ).life( 0.5f ).alpha( 1f, 0f, 0.2f );
					}
					break;
				case FLURRY: {
					//a wink at the weapon side and a small burst of silver
					EnchantFx.Flash w = b.flash( true, EnchantFx.STAR_WINK, 0f, x, y, SILVER, 0.9f, 0.12f );
					if (w != null) w.envelope( 0f, PieceFx.STAR_HOLD );
					burst( b, x, y, 3, SILVER, 30f, 45f, 0.2f );
					break;
				}
				case MOMENTUM:
					if (n >= FULL_STACKS){
						//at full: a gold star (always: it is the sign) and three sparks thrown up
						FxParticle s = b.lit( x, y, FxBudget.P0 );
						if (s != null) s.look( FxFrames.STAR4_5 ).paint( Element.GOLD.light ).life( 0.2f ).alpha( 1f, 0f, 0.1f ).snap();
						burst( b, x, y, 3, Element.GOLD.light, 35f, 50f, 0.25f );
					} else {
						//a mote a stack, side by side, rising (the first always: it is the sign)
						int k = Math.max( 1, n );
						for (int i = 0; i < k; i++){
							FxParticle p = b.lit( x + (i - (k - 1) / 2f) * 3f, y - 2f, i == 0 ? FxBudget.P0 : FxBudget.P1 );
							if (p == null) break;
							p.look( FxFrames.MOTE_2 ).paint( Element.GOLD.light ).vel( 0f, -14f ).life( 0.35f ).alpha( 1f, 0f, 0.15f ).snap();
						}
					}
					break;
				case BULWARK: {
					//two steel motes from the chest into the weapon side
					float cx = as.x + as.width() / 2f, cy = as.y + as.height() / 2f - 1f;
					for (int i = 0; i < 2; i++){
						FxParticle p = b.lit( cx, cy + i, FxBudget.P1 );
						if (p == null) break;
						p.look( FxFrames.MOTE_1 ).paint( Element.STEEL.light ).vel( (x - cx) / BULWARK_FOR, (y - cy - i) / BULWARK_FOR )
								.life( BULWARK_FOR + 0.03f * i ).alpha( 1f, 0f, 0.05f );
					}
					break;
				}
				case LONGSHOT: {
					//a silver star at the struck one (always: it is the sign), and sparks back the way the shot
					//came the bigger its bonus
					FxParticle s = b.lit( x, y, FxBudget.P0 );
					if (s != null) s.look( FxFrames.STAR4_5 ).paint( SILVER ).life( 0.18f ).alpha( 1f, 0f, 0.09f ).snap();
					int k = own() - 1;
					float back = (float)Math.atan2( -b.dy, -b.dx );
					for (int i = 0; i < k; i++){
						FxParticle p = b.lit( x, y, FxBudget.P2 );
						if (p == null) break;
						p.look( FxFrames.SPARK_2 ).paint( SILVER ).orient().polar( back + (i - (k - 1) / 2f) * 0.5f, FxRandom.Float( 35f, 50f ) )
								.drag( 4f ).life( 0.2f ).alpha( 1f, 0f, 0.1f );
					}
					break;
				}
				case REACH: {
					//a silver tip where the blow lands (always: it is the sign), two motes trailing it along the blow
					FxParticle s = b.lit( x, y, FxBudget.P0 );
					if (s != null) s.look( FxFrames.STAR4_5 ).paint( SILVER ).life( 0.16f ).alpha( 1f, 0f, 0.08f ).snap();
					for (int i = 1; i <= 2; i++){
						FxParticle p = b.lit( x - b.dx * 3f * i, y - b.dy * 3f * i, FxBudget.P2 );
						if (p == null) break;
						p.look( FxFrames.MOTE_1 ).paint( SILVER ).vel( b.dx * 20f, b.dy * 20f ).life( 0.18f ).alpha( 0.9f, 0f, 0.1f );
					}
					break;
				}
				default: {
					//a parry as an accent: a white star and a spark
					FxParticle s = b.lit( x, y, FxBudget.P1 );
					if (s != null) s.look( FxFrames.IMPACT_STAR, 0 ).paint( PieceFx.WHITE ).life( 0.1f ).alpha( 1f, 0f, 0.05f ).snap();
					burst( b, x, y, 1, Element.STEEL.light, 40f, 55f, 0.18f );
					break;
				}
			}
		}

		//n sparks of a colour thrown every way from (x, y)
		private static void burst( Beat b, float x, float y, int n, int color, float lo, float hi, float life ){
			for (int i = 0; i < n; i++){
				FxParticle p = b.lit( x, y, FxBudget.P2 );
				if (p == null) return;
				p.look( FxFrames.SPARK_2 ).paint( color ).orient().polar( FxRandom.angle(), FxRandom.Float( lo, hi ) ).drag( 4f )
						.life( life ).alpha( 1f, 0f, life * 0.5f );
			}
		}
	}

	// ------------------------------------------------------------------ guests

	/** A type's piece the host sent with no blow's card to wait for: its full form on the guest's own
	 *  sprites, its sound with it. Render thread or any: it posts. */
	static void replay( int[] n, EnchantNet.Sprites sprites ){
		final TypePiece p = pieceOf( n, sprites );
		if (p != null) Fx.post( () -> p.play( false ) );
	}

	/** A blow's pieces made again on a guest: each daze knows the crack of its blow on the same sprite,
	 *  as the host's does. */
	static void link( Piece[] pieces ){
		for (Piece p : pieces){
			if (!(p instanceof Daze)) continue;
			for (Piece q : pieces){
				if (q instanceof Crack && ((Crack) q).ds == ((Daze) p).ds) ((Daze) p).crack = (Crack) q;
			}
		}
	}

	/** A type's piece made again from what the host sent (PieceFx's kinds SPLINTER .. SIPHON); null for
	 *  a short list or a sprite the guest has not. */
	static TypePiece pieceOf( int[] n, EnchantNet.Sprites s ){
		if (n.length < 3) return null;
		switch (n[0]){
			case PieceFx.SPLINTER: case PieceFx.FELL: case PieceFx.DAZE: case PieceFx.LASH: case PieceFx.UNARM: {
				CharSprite as = sprite( s, n[1] ), ds = sprite( s, n[2] );
				if (ds == null || ds.ch == null) return null;
				boolean seen = ds.visible, hero = heroIn( as, ds );
				switch (n[0]){
					case PieceFx.SPLINTER: return new Splinter( null, as, ds, seen, hero );
					case PieceFx.FELL:     return new Fell( null, as, ds, seen, hero );
					case PieceFx.DAZE:     return new Daze( null, as, ds, seen, hero );
					case PieceFx.LASH:     return new Lash( null, as, ds, seen );
					default:               return new Unarm( null, as, ds, seen );
				}
			}
			case PieceFx.CRACK: {
				CharSprite as = sprite( s, n[1] ), ds = sprite( s, n[2] );
				if (ds == null || ds.ch == null) return null;
				return new Crack( null, as, ds, sprites( s, n, 3 ), ds.visible, heroIn( as, ds ) );
			}
			case PieceFx.NOTE: {
				CharSprite ws = sprite( s, n[1] );
				if (ws == null || ws.ch == null) return null;
				return new Note( ws.ch, ws, n[2] != 0, ws.visible );
			}
			case PieceFx.CHORD: {
				if (n.length < 6) return null;
				CharSprite ws = sprite( s, n[1] ), ds = sprite( s, n[2] );
				if (ws == null || ws.ch == null) return null;
				CharSprite on = ds != null && ds.ch != null ? ds : ws;
				return new Chord( on.ch, ws, on, n[3] != 0, n[4], sprites( s, n, 6 ), sprite( s, n[5] ), ws.visible, heroIn( ws, ds ) );
			}
			case PieceFx.RESONANCE: {
				if (n.length < 4) return null;
				CharSprite as = sprite( s, n[1] ), ds = sprite( s, n[2] );
				if (ds == null || ds.ch == null) return null;
				return new Resonance( null, as, ds, n[3] != 0, sprites( s, n, 4 ), ds.visible );
			}
			case PieceFx.PERK: {
				if (n.length < 5 || n[1] < 0 || n[1] >= PERK_CAP.length) return null;
				CharSprite as = sprite( s, n[2] ), ds = sprite( s, n[3] );
				boolean wielder = n[1] == FLURRY || n[1] == BULWARK;
				if (ds == null || ds.ch == null || (wielder && (as == null || as.ch == null))) return null;
				Perk p = new Perk( n[1], null, as, ds, n[4], ds.visible, heroIn( as, ds ) );
				if (n[1] == MOMENTUM && n[4] >= TINT_STACKS) lean( ds );
				return p;
			}
			case PieceFx.SIPHON: {
				CharSprite ws = sprite( s, n[1] ), ds = sprite( s, n[2] );
				if (ws == null || ws.ch == null || ds == null) return null;
				return new Siphon( ws.ch, ws, ds );
			}
			default:
				return null;
		}
	}

	private static CharSprite sprite( EnchantNet.Sprites s, int id ){
		return id < 0 ? null : s.of( id );
	}

	//the sprites of the ids from `from` on, those the guest has
	private static CharSprite[] sprites( EnchantNet.Sprites s, int[] n, int from ){
		int k = 0;
		CharSprite[] out = new CharSprite[Math.max( 0, n.length - from )];
		for (int i = from; i < n.length; i++){
			CharSprite c = sprite( s, n[i] );
			if (c != null) out[k++] = c;
		}
		return k == out.length ? out : java.util.Arrays.copyOf( out, k );
	}

	private static boolean heroIn( CharSprite as, CharSprite ds ){
		return EnchantFx.involvesHero( as != null ? as.ch : null, ds != null ? ds.ch : null );
	}

	// ------------------------------------------------------------------ helpers

	//a character with a sprite to show its piece on
	private static boolean target( Char c ){
		return c != null && c.sprite != null;
	}

	private static CharSprite sprite( Char c ){
		return c != null ? c.sprite : null;
	}

	//the sprites of characters, those that have one, one character left out
	private static CharSprite[] sprites( List<Char> chars, Char but ){
		if (chars == null) return new CharSprite[0];
		int k = 0;
		CharSprite[] out = new CharSprite[chars.size()];
		for (Char c : chars) if (c != null && c != but && c.sprite != null) out[k++] = c.sprite;
		return k == out.length ? out : java.util.Arrays.copyOf( out, k );
	}

	//the numbers, then the ids of the sprites
	private static int[] withIds( int[] head, CharSprite[] around ){
		int[] out = java.util.Arrays.copyOf( head, head.length + around.length );
		for (int i = 0; i < around.length; i++) out[head.length + i] = PieceFx.id( around[i] );
		return out;
	}

	//nearest the middle first (an insertion sort: a handful at most)
	private static void order( CharSprite[] s, float cx, float cy ){
		for (int i = 1; i < s.length; i++){
			CharSprite k = s[i];
			float dk = dist2( k, cx, cy );
			int j = i - 1;
			while (j >= 0 && dist2( s[j], cx, cy ) > dk){
				s[j + 1] = s[j];
				j--;
			}
			s[j + 1] = k;
		}
	}

	private static float dist2( CharSprite s, float cx, float cy ){
		if (s == null) return Float.MAX_VALUE;
		float dx = s.x + s.width() / 2f - cx, dy = s.y + s.height() / 2f - cy;
		return dx * dx + dy * dy;
	}

	// ------------------------------------------------------------------ a mote that follows

	/**
	 * One painted frame that follows a sprite where a kit particle cannot (it moves at one speed): two
	 * daze specks circling once over a head on the mark's ellipse (orbit), a taut line's mote held between
	 * a wielder's hand and the one it pulls (line), a Bard's held note over the hand, placed each frame
	 * by WieldLook (pip). Pooled in the light layer (added) or among the effects (matter); counted as a
	 * kit particle while it shows; nothing allocated a frame. Render thread.
	 */
	public static final class Mote extends FxPic {

		static final int ORBIT = 0, LINE = 1, PIP = 2;
		/** A pip not placed for this long hides, and for PIP_GONE goes. */
		static final float PIP_HIDE = 0.1f, PIP_GONE = 1f;

		int mode;
		CharSprite a, b;
		float age, life, phase, share, hold, peak, placedAt;
		private boolean counted;

		public Mote(){
			super( Assets.Effects.FX_SHEET );
		}

		/** A mote of a frame in a colour at an alpha, light (added) or matter; null off the scene. */
		static Mote make( int[] frame, int color, float alpha, boolean light ){
			Mote m = GameScene.recycle( light ? GameScene.Layer.LIGHTS : GameScene.Layer.EFFECTS, Mote.class );
			if (m == null) return null;
			m.additive = light;
			m.show( frame );
			m.resetColor();
			m.lit( color );
			m.peak = alpha;
			m.am = 0f;
			m.age = 0f;
			m.a = m.b = null;
			m.visible = false;
			m.revive();
			if (!m.counted){
				m.counted = true;
				FxBudget.add( FxBudget.PARTICLES, 1 );
			}
			return m;
		}

		/** Once round s's head on the mark's ellipse from `phase` (radians) over `life` s. */
		void orbit( CharSprite s, float phase, float life ){
			mode = ORBIT;
			a = s;
			this.phase = phase;
			this.life = life;
			follow();
		}

		/** At `share` of the way from a's weapon side to b's middle while `hold` s and fading over
		 *  `fade` s. */
		void line( CharSprite a, CharSprite b, float share, float hold, float fade ){
			mode = LINE;
			this.a = a;
			this.b = b;
			this.share = share;
			this.hold = hold;
			life = hold + fade;
			follow();
		}

		/** A pip of s's, placed by its owner each frame (put). */
		void pip( CharSprite s ){
			mode = PIP;
			a = s;
			life = Float.MAX_VALUE;
			placedAt = Game.timeTotal;
		}

		/** Whether it is s's pip now (not a pooled mote made again for something else). */
		boolean isPipOf( CharSprite s ){
			return mode == PIP && a == s;
		}

		/** A pip put at (x, y) at alpha `alpha` this frame. */
		void put( float x, float y, float alpha ){
			placedAt = Game.timeTotal;
			place( x, y );
			am = alpha;
			visible = a != null && a.visible;
		}

		@Override
		public void update(){
			super.update();
			if (a == null || a.parent == null || (mode == LINE && (b == null || b.parent == null))){
				kill();
				return;
			}
			if (mode == PIP){
				float since = Game.timeTotal - placedAt;
				if (since > PIP_GONE || since < 0f) kill();
				else if (since > PIP_HIDE) visible = false;
				return;
			}
			age += Game.elapsed;
			if (age >= life){
				kill();
				return;
			}
			follow();
		}

		//where it is at its age, and how bright
		private void follow(){
			float t = Math.min( 1f, age / life );
			if (mode == ORBIT){
				FxOrbit.Slot m = FxOrbit.Slot.MARK;
				double ang = phase + 2 * Math.PI * t;
				place( a.x + a.width() / 2f + m.rx * (float)Math.cos( ang ), a.y + a.height() / 2f + m.h + m.ry * (float)Math.sin( ang ) );
				am = peak * Math.min( 1f, Math.min( 0.35f + t * 6f, (1f - t) * 5f ) );
			} else {
				float wx = PieceFx.handX( a ), wy = PieceFx.handY( a );
				float tx = b.x + b.width() / 2f, ty = b.y + b.height() / 2f;
				place( wx + (tx - wx) * share, wy + (ty - wy) * share );
				am = age <= hold ? peak : peak * Math.max( 0f, 1f - (age - hold) / Math.max( 0.01f, life - hold ) );
			}
			visible = a.visible && am > 0.004f;
		}

		@Override
		public void kill(){
			super.kill();
			a = b = null;
			if (counted){
				counted = false;
				FxBudget.add( FxBudget.PARTICLES, -1 );
			}
		}
	}
}
