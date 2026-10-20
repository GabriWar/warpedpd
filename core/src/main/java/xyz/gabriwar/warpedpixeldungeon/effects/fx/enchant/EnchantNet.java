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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.BlowPresenter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Piece;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.net.NetVisuals;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * The enchantments' pictures for co-op guests: the host sends each as it plays it
 * (NetVisuals.recordEnchant, event "eh": a kind, a list of whole numbers and, for some, a list of
 * cells) and the guest plays it from his own sprites (play), the sting with it - sounds have no
 * other way over. Characters go by their ids (Char.id()). Visual only. The kinds are numbers on
 * the wire: new ones at the end, never renumbered.
 *
 *   PROC 0      look code, attacker id (-1 none), defender id, strength x 1000, enchant level, flags, tier
 *   GLYPH 1     look code, wearer id, attacker id (-1 none), strength x 1000, level, flags, moment (-1: a
 *               ward answering a blow; else EnchantFx's DODGE..TICK), tier
 *   GAIN 2      look code (-1: none, today's picture), owner id, the item's image, its glow colour (-1
 *               none): Enchanting over the owner (GainFx), its sting with it
 *   WIELD 3     character id, weapon look codes (2, -1 none), enchant level, armour look codes (2),
 *               the echo's code, Kinetic's charge (0-3): what a hero or a statue carries, sent as the
 *               host's poll sees it change (and every few seconds while there is any), played by
 *               filling the guest's sprite's EnchantState (its WieldLook)
 *   PIECE 4     a gameplay piece's look (PieceFx): the piece (PieceFx.CRIT ... DISARM), then its numbers -
 *               character ids, cells, an image, a look code (-1 none) - as PieceFx sends each
 *   REVEAL 5    what (GainFx.CURSE, VESTIGE, PICKUP), look code, character id: a curse or a vestige
 *               identified, an enchanted item's first pickup, at that character (GainFx)
 *   BLOW_END 6  attacker id (-1 none), defender id
 *   SWING 7     look code, attacker id, defender id, landed (1/0), melee (1/0), enchant level, wayward
 *               (1/0), cells between them: an enchanted weapon's swing trail. It comes after the
 *               blow's own star (that replays from the blood burst, sent first), so a guest's plain
 *               blow draws the trail but not the 35% tint on its star
 *   SURGE 8     character id, stacks: a surge's stacks as the host reads them (SURGING's motes; a
 *               guest's characters hold no buffs)
 *   CARD 9      a closed blow's card on one struck character (BlowPresenter.card: its numbers, its
 *               lead's and accents' places among the blow's pieces, its chips, what flowed back to
 *               the wielder), sent after the blow's pieces
 *   NOTES 10    hero id, notes held toward the next chord (0-3), whether that chord is a terrifying one
 *               (1/0): a Bard's held notes as the host reads them, sent as they change (WieldLook's pips;
 *               a guest's heroes hold no weapon's quality)
 *
 * Every picture of a blow (PROC, GLYPH, SWING, BLOW_END, PIECE, and the blood burst and the flash
 * NetVisuals sends) carries the blow's serial (effects/fx/combat/Blow; the event's "s"). A guest
 * holds a tagged picture until its blow's CARD comes, then presents the blow as its host did
 * (BlowPresenter.guest) from his own sprites; a serial whose CARD has not come HOLD_FOR s after
 * its first picture (an older host, a blow split over two deltas that lost its card) plays its
 * pictures as they are. A guest on an older build ignores CARD and so shows no damage numbers of a
 * blow (they are no longer sent as texts): there is no version handshake in net/.
 *
 * Bursts a guest already gets from the host (Venomous's poison, Blooming's leaves, Entanglement's
 * earth, registered CellEmitter bursts) are not sent again.
 */
public final class EnchantNet {

	private EnchantNet(){}

	public static final int PROC = 0, GLYPH = 1, GAIN = 2, WIELD = 3, PIECE = 4, REVEAL = 5, BLOW_END = 6, SWING = 7, SURGE = 8,
			CARD = 9, NOTES = 10;

	/** A guest's sprite of a host's character id, null for none (NetVisuals' lookup). */
	public interface Sprites {
		CharSprite of( int id );
	}

	// ------------------------------------------------------------------ the host's

	static void send( int kind, int[] ints, int[] cells ){
		send( kind, ints, cells, 0 );
	}

	//the same tagged with the blow it belongs to (0: none)
	static void send( int kind, int[] ints, int[] cells, int serial ){
		if (!NetManager.isHost()) return;
		NetVisuals.recordEnchant( kind, ints, cells, serial );
	}

	static int id( Char c ){
		return c != null ? c.id() : -1;
	}

	static void sendProc( EnchantLook l, Char a, Char d, float s, int level, int flags, int tier ){
		sendProc( l, a, d, s, level, flags, tier, 0 );
	}

	static void sendProc( EnchantLook l, Char a, Char d, float s, int level, int flags, int tier, int serial ){
		if (!NetManager.isHost()) return;
		send( PROC, new int[]{ l.code, id( a ), id( d ), Math.round( s * 1000f ), level, flags, tier }, null, serial );
	}

	static void sendWard( EnchantLook l, Char wearer, Char attacker, float s, int level, int flags, int moment, int tier ){
		sendWard( l, wearer, attacker, s, level, flags, moment, tier, 0 );
	}

	static void sendWard( EnchantLook l, Char wearer, Char attacker, float s, int level, int flags, int moment, int tier,
			int serial ){
		if (!NetManager.isHost()) return;
		send( GLYPH, new int[]{ l.code, id( wearer ), id( attacker ), Math.round( s * 1000f ), level, flags, moment, tier }, null,
				serial );
	}

	static void sendBlowEnded( Char a, Char d ){
		sendBlowEnded( a, d, 0 );
	}

	static void sendBlowEnded( Char a, Char d, int serial ){
		if (!NetManager.isHost()) return;
		send( BLOW_END, new int[]{ id( a ), id( d ) }, null, serial );
	}

	static void sendSwing( EnchantLook l, Char a, Char d, boolean landed, boolean melee, int level, boolean wayward, int dist ){
		sendSwing( l, a, d, landed, melee, level, wayward, dist, 0 );
	}

	static void sendSwing( EnchantLook l, Char a, Char d, boolean landed, boolean melee, int level, boolean wayward, int dist,
			int serial ){
		if (!NetManager.isHost()) return;
		send( SWING, new int[]{ l.code, id( a ), id( d ), landed ? 1 : 0, melee ? 1 : 0, level, wayward ? 1 : 0, dist }, null,
				serial );
	}

	// ------------------------------------------------------------------ WIELD (ench-item-looks)

	/** What a character carries, as its poll found it (EnchantState, on the host). */
	static void sendWield( Char c, EnchantState st ){
		if (c == null || !NetManager.isHost()) return;
		send( WIELD, new int[]{ c.id(), st.weaponCodes[0], st.weaponCodes[1], st.weaponLevel, st.armorCodes[0],
				st.armorCodes[1], st.echoCode, st.kineticBin }, null );
	}

	/** A gameplay piece's look (PieceFx), its piece first. */
	static void sendPiece( int[] ints ){
		sendPiece( ints, 0 );
	}

	/** The same tagged with the blow it belongs to (0: none). */
	static void sendPiece( int[] ints, int serial ){
		if (!NetManager.isHost()) return;
		send( PIECE, ints, null, serial );
	}

	/** A surge's stacks on a character changed (StateLooks reads them on the host). Any thread. */
	public static void sendSurge( Char c, int stacks ){
		if (c == null || !NetManager.isHost()) return;
		send( SURGE, new int[]{ c.id(), stacks }, null );
	}

	/** A Bard's held notes on a hero changed (WieldLook reads them on the host). Render thread. */
	static void sendNotes( Char c, int notes, boolean dread ){
		if (c == null || !NetManager.isHost()) return;
		send( NOTES, new int[]{ c.id(), notes, dread ? 1 : 0 }, null );
	}

	/** A closed blow's card (BlowPresenter), its serial at its third place. Any thread. */
	public static void sendCard( int[] ints ){
		if (!NetManager.isHost()) return;
		send( CARD, ints, null, ints.length > 2 ? ints[2] : 0 );
	}

	// ------------------------------------------------------------------ GAIN and REVEAL (ench-gain's)

	static void sendGain( EnchantLook l, Char owner, int image, int color ){
		if (owner == null || !NetManager.isHost()) return;
		send( GAIN, new int[]{ l != null ? l.code : -1, owner.id(), image, color }, null );
	}

	static void sendReveal( int what, EnchantLook l, Char ch ){
		if (l == null || ch == null || !NetManager.isHost()) return;
		send( REVEAL, new int[]{ what, l.code, ch.id() }, null );
	}

	// ------------------------------------------------------------------ the guest's

	// ------------------------------------------------------------------ a blow's pictures, held

	/** How long a guest holds a blow's pictures for its card, s; how old a held blow is when it is
	 *  dropped unplayed (its release timer went with its scene: a floor change, no scene yet). */
	public static final float HOLD_FOR = 0.25f, STALE = 1f;

	//a blow's pictures as they came: events [kind, ints, cells], blood bursts [sprite id, damage],
	//when its first one came (Game.timeTotal)
	private static final class Held {
		final java.util.ArrayList<Object[]> events = new java.util.ArrayList<>( 4 );
		final java.util.ArrayList<int[]> bloods = new java.util.ArrayList<>( 1 );
		Sprites sprites;
		float since;
	}

	//by serial, render thread
	private static final java.util.HashMap<Integer, Held> held = new java.util.HashMap<>();

	//held blows whose timer never ran, dropped: none outlives STALE s, so a serial a new host
	//session hands out again starts afresh
	private static void sweep(){
		if (held.isEmpty()) return;
		float now = com.watabou.noosa.Game.timeTotal;
		java.util.Iterator<Held> it = held.values().iterator();
		while (it.hasNext()){
			float age = now - it.next().since;
			if (age >= STALE || age < 0f) it.remove();
		}
	}

	/** How many blows a guest holds now (tests). */
	public static int heldBlows(){
		return held.size();
	}

	private static Held held( int serial, Sprites sprites ){
		sweep();
		Held h = held.get( serial );
		if (h == null){
			h = new Held();
			h.since = com.watabou.noosa.Game.timeTotal;
			held.put( serial, h );
			final int s = serial;
			//no card in time: its pictures as they are
			EnchantFx.later( HOLD_FOR, () -> release( s ) );
		}
		h.sprites = sprites;
		return h;
	}

	/** What the host sent tagged with a blow's serial (0: none, played at once). A guest's, render
	 *  thread. */
	public static void play( int kind, int[] ints, int[] cells, Sprites sprites, int serial ){
		if (ints == null || sprites == null) return;
		if (kind == CARD){
			card( ints, sprites );
		} else if (serial != 0 && kind != WIELD && kind != GAIN && kind != REVEAL && kind != SURGE){
			held( serial, sprites ).events.add( new Object[]{ kind, ints, cells } );
		} else {
			play( kind, ints, cells, sprites );
		}
	}

	/** A blood burst the host sent tagged with a blow's serial: held with its blow. False when it is
	 *  none (0: NetVisuals plays it as ever). */
	public static boolean holdBlood( int serial, int id, int damage, Sprites sprites ){
		if (serial == 0 || sprites == null) return false;
		held( serial, sprites ).bloods.add( new int[]{ id, damage } );
		return true;
	}

	//a serial whose card never came: every picture as the host's older builds sent them
	private static void release( int serial ){
		Held h = held.remove( serial );
		if (h == null) return;
		for (Object[] e : h.events) play( (Integer) e[0], (int[]) e[1], (int[]) e[2], h.sprites );
		for (int[] b : h.bloods){
			CharSprite s = sprite( h.sprites, b[0] );
			if (s != null) bloodAsEver( s, b[1] );
		}
	}

	/** The blood burst and the plain star a guest draws of a blood event, as it always has. */
	public static void bloodAsEver( CharSprite s, int damage ){
		com.watabou.utils.PointF c = s.center();
		int n = Math.min( 9, Math.max( 1, (int)(9 * Math.sqrt( (double) damage / 20 )) ) );
		xyz.gabriwar.warpedpixeldungeon.effects.Splash.at( c, 0xFFBB0000, n );
		s.hitFx( c.x + (s.flipHorizontal ? 8 : -8), c.y, damage, xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.HitFx.HIT, false );
	}

	//a blow's card: the struck one's presents the blow held for it; another's is its small card
	private static void card( int[] c, Sprites sprites ){
		if (c.length < BlowPresenter.CARD_HEAD) return;
		CharSprite ds = sprite( sprites, c[0] ), as = sprite( sprites, c[1] );
		if (c[BlowPresenter.CARD_SMALL] != 0 || ds == null){
			BlowPresenter.guestCard( c, ds, as );
			return;
		}
		sweep();
		Held h = held.remove( c[2] );
		if (h == null){
			BlowPresenter.guestCard( c, ds, as );
			return;
		}
		Piece[] pieces = new Piece[h.events.size()];
		for (int i = 0; i < pieces.length; i++){
			Object[] e = h.events.get( i );
			pieces[i] = pieceOf( (Integer) e[0], (int[]) e[1], sprites );
			//a picture of the blow that is no piece of it (a free reload in a volley): as it is
			if (pieces[i] == null) play( (Integer) e[0], (int[]) e[1], (int[]) e[2], sprites );
		}
		TypeFx.link( pieces );
		CharSprite[] bloodOn = new CharSprite[h.bloods.size()];
		int[] bloods = new int[bloodOn.length];
		for (int i = 0; i < bloodOn.length; i++){
			bloodOn[i] = sprite( sprites, h.bloods.get( i )[0] );
			bloods[i] = h.bloods.get( i )[1];
		}
		BlowPresenter.guest( c, pieces, as, ds, bloodOn, bloods );
	}

	//a blow's picture made again as its piece on the guest's sprites; null for one that is none
	//(or whose sprites the guest has not)
	static Piece pieceOf( int kind, int[] n, Sprites sprites ){
		switch (kind){
			case PROC: {
				if (n.length < 7) return null;
				EnchantLook l = EnchantLooks.byCode( n[0] );
				CharSprite as = sprite( sprites, n[1] ), ds = sprite( sprites, n[2] );
				if (l == null || ds == null || ds.ch == null) return null;
				EnchantFx.ProcPiece p = new EnchantFx.ProcPiece( l, as != null ? as.ch : null, as, ds.ch, ds, n[3] / 1000f, n[4],
						n[5], null, ds.visible, EnchantFx.involvesHero( as != null ? as.ch : null, ds.ch ), false, -1, tier( n[6] ) );
				p.guest = true;
				return p;
			}
			case GLYPH: {
				if (n.length < 8) return null;
				EnchantLook l = EnchantLooks.byCode( n[0] );
				CharSprite ws = sprite( sprites, n[1] ), as = sprite( sprites, n[2] );
				if (l == null || ws == null || ws.ch == null) return null;
				EnchantFx.ProcPiece p = new EnchantFx.ProcPiece( l, as != null ? as.ch : null, as, ws.ch, ws, n[3] / 1000f, n[4],
						n[5], null, ws.visible, EnchantFx.involvesHero( as != null ? as.ch : null, ws.ch ), true, n[6], tier( n[7] ) );
				p.guest = true;
				return p;
			}
			case SWING: {
				if (n.length < 8) return null;
				EnchantLook l = EnchantLooks.byCode( n[0] );
				CharSprite as = sprite( sprites, n[1] ), ds = sprite( sprites, n[2] );
				if (l == null || as == null || ds == null || as.ch == null) return null;
				return new EnchantFx.SwingPiece( as.ch, l, as, ds, n[3] != 0, n[4] != 0, n[5], n[6] != 0, n[7],
						as.ch == xyz.gabriwar.warpedpixeldungeon.Dungeon.hero );
			}
			case BLOW_END: {
				if (n.length < 2) return null;
				CharSprite as = sprite( sprites, n[0] ), ds = sprite( sprites, n[1] );
				if (ds == null || ds.ch == null) return null;
				return new EnchantFx.EndPiece( as != null ? as.ch : null, as, ds.ch, ds,
						xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.HitFx.HIT,
						EnchantFx.involvesHero( as != null ? as.ch : null, ds.ch ) );
			}
			case PIECE:
				return PieceFx.pieceOf( n, sprites );
			default:
				return null;
		}
	}

	/** What the host sent, played from the guest's sprites (nothing sent on). An unknown kind, a
	 *  short list, a code off the table or a character the guest does not have: nothing. */
	public static void play( int kind, int[] ints, int[] cells, Sprites sprites ){
		if (ints == null || sprites == null) return;
		switch (kind){
			case PROC:
				if (ints.length < 7) return;
				EnchantFx.replayProc( EnchantLooks.byCode( ints[0] ), sprite( sprites, ints[1] ), sprite( sprites, ints[2] ),
						ints[3] / 1000f, ints[4], ints[5], tier( ints[6] ) );
				break;
			case GLYPH:
				if (ints.length < 8) return;
				EnchantFx.replayWard( EnchantLooks.byCode( ints[0] ), sprite( sprites, ints[1] ), sprite( sprites, ints[2] ),
						ints[3] / 1000f, ints[4], ints[5], ints[6], tier( ints[7] ) );
				break;
			case BLOW_END:
				if (ints.length < 2) return;
				EnchantFx.replayBlowEnded( sprite( sprites, ints[0] ), sprite( sprites, ints[1] ) );
				break;
			case SWING:
				if (ints.length < 8) return;
				EnchantFx.replaySwing( EnchantLooks.byCode( ints[0] ), sprite( sprites, ints[1] ), sprite( sprites, ints[2] ),
						ints[3] != 0, ints[4] != 0, ints[5], ints[6] != 0, ints[7] );
				break;
			case WIELD: {
				if (ints.length < 8) return;
				CharSprite s = sprite( sprites, ints[0] );
				if (s != null) EnchantState.of( s ).wielded( ints[1], ints[2], ints[3], ints[4], ints[5], ints[6], ints[7] );
				break;
			}
			case GAIN: {
				if (ints.length < 4) return;
				EnchantLook l = ints[0] < 0 ? null : EnchantLooks.byCode( ints[0] );
				if (ints[0] >= 0 && l == null) return;
				GainFx.replayGain( sprite( sprites, ints[1] ), l, ints[2], ints[3] );
				break;
			}
			case REVEAL:
				if (ints.length < 3) return;
				GainFx.replayReveal( ints[0], EnchantLooks.byCode( ints[1] ), sprite( sprites, ints[2] ) );
				break;
			case PIECE:
				if (ints.length < 2) return;
				PieceFx.replay( ints, sprites );
				break;
			case SURGE: {
				if (ints.length < 2) return;
				CharSprite s = sprite( sprites, ints[0] );
				if (s != null) s.surgeStacks( ints[1] );
				break;
			}
			case NOTES: {
				if (ints.length < 3) return;
				CharSprite s = sprite( sprites, ints[0] );
				if (s != null) WieldLook.notes( s, ints[1], ints[2] != 0 );
				break;
			}
			default:
				break;
		}
	}

	private static CharSprite sprite( Sprites s, int id ){
		return id < 0 ? null : s.of( id );
	}

	private static int tier( int t ){
		return Math.max( EnchantFx.WEAK, Math.min( EnchantFx.FINISHER, t ) );
	}
}
