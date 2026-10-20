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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.HitCard;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantNet;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.HealFx;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

import java.util.ArrayList;

/**
 * A closed blow presented (docs/fx/hit-card.md), on the render thread: at once for a blow the hero
 * sees (it ran there), on the next frame for one the actor thread ran. One LEAD at the hit (a
 * kill's finisher, else the first proc, else a type's twist, else a crit, else the plain star),
 * drawn with what goes with it (the swing trail, the hit star in the lead's colours, a finisher's
 * star); every other piece an ACCENT on its own beat (0.08 s apart, a few pixels higher each, the
 * sideways side alternating from the one away from the attacker), in priority order; what flows
 * back at the wielder's side, a beat each; and one card per struck character, every chip landing
 * on its piece's beat. The owner's call: a blow arranges its pictures, it never thins them -
 * every piece draws its whole look (a proc, the echo, a twist, the star, its bloom and the swing
 * trail as they were before the ledger, at any pace of blows), the blood as it always did. Only
 * the blow's frame shakes (the lead's and the star's own rules) and the lead alone stings.
 *
 * Draws nothing of the game's Random; allocates once per blow, never per frame.
 */
public final class BlowPresenter {

	private BlowPresenter(){}

	/** How far apart accents come, s (EnchantFx.ECHO_LATE); how much higher each is, and how far
	 *  sideways, px; the wielder lane's beats and step. */
	public static final float BEAT = EnchantFx.ECHO_LATE, ACCENT_UP = 4f, ACCENT_SIDE = 2f, WIELDER_UP = 4f;
	/** How far toward the attacker the hit point is from the struck one's middle, px. */
	static final float HIT_OFF = 4f;
	/** The wielder's small cards' colours: a heal, mana, a shield. */
	public static final int HEAL_COLOR = CharSprite.POSITIVE, MANA_COLOR = 0x44CCFF, SHIELD_COLOR = CharSprite.POSITIVE;

	static void present( Blow.Record r ){
		Char d = r.defender, a = r.attacker;
		CharSprite ds = d != null ? d.sprite : null;

		//every piece tagged with the blow for co-op guests, before anything else of it; untagged when
		//no card of the struck one will follow (a guest would hold them for it in vain)
		if (NetManager.isHost()){
			int tag = carded( r, d ) ? r.serial : 0;
			for (Piece p : r.pieces) p.send( tag );
		}

		// ---------------------------------------------------------- sorting the pieces
		HitFx.StarPiece star = null;
		EnchantFx.SwingPiece swing = null;
		Piece end = null;
		ArrayList<Piece> procs = new ArrayList<>( 2 ), twists = new ArrayList<>( 1 ), crits = new ArrayList<>( 1 ),
				accents = new ArrayList<>( 3 ), wielder = new ArrayList<>( 2 ), motion = new ArrayList<>( 1 ),
				elsewhere = new ArrayList<>( 1 );
		for (Piece p : r.pieces){
			if (p.lane == Blow.WIELDER){
				wielder.add( p );
			} else if (p.lane == Blow.MOTION){
				motion.add( p );
			} else if (p instanceof EnchantFx.SwingPiece && swing == null){
				swing = (EnchantFx.SwingPiece) p;
			} else if (p.on != d){
				elsewhere.add( p );
			} else if (p instanceof HitFx.StarPiece && star == null){
				star = (HitFx.StarPiece) p;
			} else if (p.rank == Blow.FINISHER && end == null){
				end = p;
			} else if (p.rank == Blow.PROC){
				procs.add( p );
			} else if (p.rank == Blow.TWIST){
				twists.add( p );
			} else if (p.rank == Blow.CRIT){
				crits.add( p );
			} else {
				accents.add( p );
			}
		}

		// ---------------------------------------------------------- the lead
		//the best rank leads: a kill's finisher (its first proc's, else the blow's end, else the
		//star's), a proc, a twist, a crit, the plain star
		Piece first = procs.isEmpty() ? null : procs.get( 0 );
		boolean finisher = r.killed;
		Piece lead;
		if (finisher){
			lead = first != null ? first : end != null ? end : star;
			if (lead == star && star != null){
				star.strong = true;
				star.puff = true;
			}
		} else if (first != null){
			lead = first;
		} else if (!twists.isEmpty()){
			lead = twists.get( 0 );
		} else if (!crits.isEmpty()){
			lead = crits.get( 0 );
		} else {
			lead = star != null ? star : end;
		}
		//what did not lead is an accent: a twist, a crit, every other proc - each its own full beat
		//(the second's at 80% as ever, the later ones joining)
		for (Piece p : twists) if (p != lead) accents.add( p );
		for (Piece p : crits) if (p != lead) accents.add( p );
		order( procs, lead, accents );
		sortByPriority( accents );

		//the blow's frame: the hero in it lets the lead, the star and the end shake by their own rules
		boolean mayShake = r.heroIn;

		//the first proc's stamp, before the star takes its colours
		if (first instanceof EnchantFx.ProcPiece) ((EnchantFx.ProcPiece) first).stamp();

		//the swing's tint and trail (its stamp too comes before the star)
		if (swing != null) swing.lead( mayShake );
		//the hit star, whole, with its bloom: the lead itself, or drawn with it in its colours (a
		//crit's leading STRONG)
		if (star != null){
			if (lead != null && lead.rank == Blow.CRIT) star.strong = true;
			star.lead( mayShake );
		}
		if (lead != null && lead != star) lead.lead( mayShake );
		if (end != null && end != lead) end.lead( mayShake );
		if (r.seen && lead != null) lead.sting();

		// ---------------------------------------------------------- accents
		float[] beatAt = new float[accents.size()];
		//what the lead carries home (a drain's beads, as it drew them) lands after it, as does what
		//flowed back (a lifesteal's beads): the wielder's card waits for it; an accent's, for what it
		//draws on its beat (Home)
		carry( r, lead );
		final Home home = new Home();
		if (r.carried) home.lands( r.flowDelay );
		for (int i = 0; i < accents.size(); i++){
			final Piece p = accents.get( i );
			final int beat = i + 1;
			beatAt[i] = BEAT * beat;
			if (ds == null) continue;
			final float side = sideAway( a, d ) * (beat % 2 == 1 ? 1f : -1f);
			final CharSprite as = a != null ? a.sprite : null;
			//one that may carry something home: the wielder's card waits for its beat, then for what
			//it does draw (the guests measure their own)
			final boolean carries = a != null && p.home() >= 0f;
			if (carries) home.pending++;
			later( BEAT * beat, () -> {
				if (ds.parent != null) p.accent( hitX( as, ds ) + side * ACCENT_SIDE, hitY( as, ds ) - ACCENT_UP * beat );
				if (carries) home.landed( p.home() );
			} );
		}

		// ---------------------------------------------------------- the wielder's lane
		final CharSprite ws = a != null ? a.sprite : null;
		for (int i = 0; i < wielder.size() && ws != null; i++){
			final Piece p = wielder.get( i );
			final int beat = i + 1;
			later( BEAT * beat, () -> {
				if (ws.parent == null) return;
				p.accent( wieldX( ws ), wieldY( ws, beat ) );
			} );
		}

		// ---------------------------------------------------------- motions and the others struck
		for (Piece p : motion){
			p.accent( ds != null ? hitX( ws, ds ) : 0f, ds != null ? hitY( ws, ds ) : 0f );
		}
		//another's drain sends its beads to the wielder too (Ares' leech): drawn now, its landing known
		for (Piece p : elsewhere){
			elsewhere( p, mayShake );
			if (carry( r, p )) home.lands( p.home() );
		}

		// ---------------------------------------------------------- the cards
		cards( r, lead, star, accents, beatAt, elsewhere, crits, home );
	}

	//a wielder lane's beat: at the weapon side, a few pixels higher each beat
	private static float wieldX( CharSprite ws ){
		return ws.x + ws.width() / 2f + (ws.flipHorizontal ? -EnchantFx.WEAPON_SIDE : EnchantFx.WEAPON_SIDE);
	}

	private static float wieldY( CharSprite ws, int beat ){
		return ws.y + ws.height() / 2f + 1f - WIELDER_UP * (beat - 1);
	}

	/** When what the blow carries to the wielder has landed (Blow.flowBack, the lead's, the
	 *  accents' and the others' beads): it shows his small card then, the heal's own look with it.
	 *  An accent that may carry something counts as pending till its beat draws and says when its
	 *  last bead lands. Render thread. */
	static final class Home {
		float due;
		int pending;
		Runnable then;

		Home(){
			due = com.watabou.noosa.Game.timeTotal;
		}

		//something carried in lands `in` s from now (-1: nothing comes)
		void lands( float in ){
			if (in < 0f) return;
			due = Math.max( due, com.watabou.noosa.Game.timeTotal + in );
		}

		//an accent drew: its last bead lands `in` s from now (-1: it sent none)
		void landed( float in ){
			lands( in );
			pending--;
			go();
		}

		//what to show once it has all landed
		void then( Runnable r ){
			then = r;
			go();
		}

		private void go(){
			if (pending > 0 || then == null) return;
			Runnable r = then;
			then = null;
			later( Math.max( 0f, due - com.watabou.noosa.Game.timeTotal ), r );
		}
	}

	//whether the struck one's card will be drawn (and sent to the guests): its sprite seen now, or a
	//number claimed on it while it was (CharSprite claims one only then; a proc after the damage may
	//have moved it out of sight before this blow is presented). The card stays where it was born.
	private static boolean carded( Blow.Record r, Char d ){
		return d != null && d.sprite != null && (d.sprite.visible || numbered( r, d ));
	}

	private static boolean numbered( Blow.Record r, Char d ){
		Blow.Mark m = r.find( d );
		return m != null && m.hasNumber;
	}

	//another character's own picture (a chain's proc, a sweep's star, Thorns on the attacker): its
	//whole look at once, as it always played; a proc's stamp before it
	private static void elsewhere( Piece p, boolean mayShake ){
		if (p instanceof EnchantFx.ProcPiece) ((EnchantFx.ProcPiece) p).stamp();
		p.lead( mayShake );
	}

	//the procs on the struck one in order: the first leads (its place 0), every other an accent at
	//its place (1 the second, then the joins)
	private static void order( ArrayList<Piece> procs, Piece lead, ArrayList<Piece> accents ){
		int k = 0;
		for (Piece p : procs){
			int place = p == lead ? 0 : ++k;
			if (p instanceof EnchantFx.ProcPiece) ((EnchantFx.ProcPiece) p).index = place;
			if (p == lead) continue;
			p.priority = place == 1 ? Blow.A_SECOND : Blow.A_LATE;
			accents.add( p );
		}
	}

	//what a piece drawn now carries to the wielder lands its home() from now (Blow.flowBack's rule,
	//the guests' CARD); whether it carries anything
	private static boolean carry( Blow.Record r, Piece p ){
		float h = p != null ? p.home() : -1f;
		if (h < 0f || r.attacker == null) return false;
		r.flowDelay = Math.max( r.flowDelay, h );
		r.carried = true;
		return true;
	}

	private static void later( float seconds, Runnable r ){
		if (seconds <= 0f) r.run();
		else EnchantFx.later( seconds, r );
	}

	//the blow's hit point: 4 px from the struck one's middle toward the attacker
	static float hitX( CharSprite as, CharSprite ds ){
		float cx = ds.x + ds.width() / 2f;
		float[] u = way( as, ds );
		return cx - u[0] * HIT_OFF;
	}

	static float hitY( CharSprite as, CharSprite ds ){
		float cy = ds.y + ds.height() / 2f;
		float[] u = way( as, ds );
		return cy - u[1] * HIT_OFF;
	}

	//the blow's way (unit, attacker to struck); along the struck one's facing with no attacker
	private static float[] way( CharSprite as, CharSprite ds ){
		float cx = ds.x + ds.width() / 2f, cy = ds.y + ds.height() / 2f;
		float sx, sy;
		if (as != null && as != ds){
			sx = as.x + as.width() / 2f;
			sy = as.y + as.height() / 2f;
		} else {
			sx = cx + (ds.flipHorizontal ? 8f : -8f);
			sy = cy;
		}
		float vx = cx - sx, vy = cy - sy, len = (float)Math.sqrt( vx * vx + vy * vy );
		if (len < 0.5f) return new float[]{ ds.flipHorizontal ? -1f : 1f, 0f };
		return new float[]{ vx / len, vy / len };
	}

	//the side away from the attacker: +1 when it stands to the left of the struck one
	static float sideAway( Char a, Char d ){
		if (a == null || d == null || a.sprite == null || d.sprite == null) return 1f;
		return a.sprite.x <= d.sprite.x ? 1f : -1f;
	}

	//stable: an insertion sort, a handful at most
	private static void sortByPriority( ArrayList<Piece> l ){
		for (int i = 1; i < l.size(); i++){
			Piece k = l.get( i );
			int j = i - 1;
			while (j >= 0 && l.get( j ).priority > k.priority){
				l.set( j + 1, l.get( j ) );
				j--;
			}
			l.set( j + 1, k );
		}
	}

	// ------------------------------------------------------------------ the cards

	private static void cards( Blow.Record r, Piece lead, HitFx.StarPiece star, ArrayList<Piece> accents, float[] beatAt,
			ArrayList<Piece> elsewhere, ArrayList<Piece> crits, Home home ){
		Char d = r.defender, a = r.attacker;
		boolean host = NetManager.isHost();
		boolean flowsSent = false;
		int[] codes = new int[16], tints = new int[16];
		float[] lands = new float[16];
		Blow.Mark dm = d != null ? r.find( d ) : null;

		//the struck one's card
		if (carded( r, d )){
			int n = 0;
			if (lead != null && lead != star && lead.chip != -1) n = put( codes, tints, lands, n, lead.chip, lead.chipTint, 0f );
			for (int i = 0; i < accents.size(); i++){
				Piece p = accents.get( i );
				if (p.chip != -1) n = put( codes, tints, lands, n, p.chip, p.chipTint, beatAt[i] );
			}
			//what the type, the lines and the rest put on it: procs, echoes, silver, gold, then buffs
			if (dm != null){
				for (int cls = 0; cls < 4; cls++){
					for (int i = 0; i < dm.chipCount; i++){
						if (chipClass( dm.chips[i] ) == cls) n = put( codes, tints, lands, n, dm.chips[i], dm.tints[i], 0f );
					}
				}
				n = buffs( r, dm, codes, tints, lands, n );
			}
			//the X shows on its picture's beat: at once leading, on its accent's beat else
			boolean crit = false;
			float critIn = 0f;
			for (Piece p : crits){
				if (p.on != d) continue;
				float at = p == lead ? 0f : beatOf( p, accents, beatAt );
				critIn = crit ? Math.min( critIn, at ) : at;
				crit = true;
			}
			boolean number = dm != null && dm.hasNumber;
			int icon = -1, color = CharSprite.NEGATIVE, value = -1;
			if (number){
				value = dm.number;
				color = dm.color;
				icon = leftIcon( r, dm );
			}
			//a miss's or immunity's mark: the left slot of a card with no number, its reason the first
			//chip; beside a number (a thorn's on a miss) the two lead the row
			if (dm != null && dm.markCell >= 0){
				if (dm.markReason >= 0) n = front( codes, tints, lands, n, dm.markReason );
				if (number) n = front( codes, tints, lands, n, dm.markCell );
				else icon = dm.markCell;
			}
			if (number || n > 0 || crit || icon >= 0){
				HitCard.show( d.sprite, a != null ? a.id() : -1, value, color, icon, crit, critIn, codes, tints, lands, n );
			}
			xyz.gabriwar.warpedpixeldungeon.actors.ledger.DamageLedger.card( r.serial, d, value, color, icon, crit, codes, tints, n );
			//the guests' word of it, after the blow's pieces (and of what flowed back): every accent's
			//place on its beat, -1 for one the guests were not sent
			if (host){
				int[] acc = new int[accents.size()];
				for (int i = 0; i < acc.length; i++) acc[i] = sentIndex( r, accents.get( i ) );
				EnchantNet.sendCard( card( r, d, value, color, icon, crit, critIn, false, sentIndex( r, lead ), acc,
						codes, tints, lands, n, true ) );
				flowsSent = true;
			}
		}

		//everyone else it struck (the attacker's own reflected damage among them): seen now, or with a
		//number claimed while it was (as for the struck one, carded)
		for (Blow.Mark m : r.marks){
			if (m.ch == d || m.ch.sprite == null || !(m.ch.sprite.visible || m.hasNumber)) continue;
			//every chip it carries, none dropped: its mark and reason, then by class, then its buffs
			int n = chips( r, m, codes, tints, lands );
			if (m.ch == a && m.hasNumber){
				HitCard.show( a.sprite, d != null ? d.id() : -1, m.number, m.color, m.icon, false, 0f, codes, tints, lands, n );
				if (host) EnchantNet.sendCard( card( r, a, m.number, m.color, m.icon, false, 0f, false, -1, NONE_SENT,
						codes, tints, lands, n, false ) );
				continue;
			}
			int value = m.hasNumber ? m.number : -1, color = m.hasNumber ? m.color : CharSprite.NEGATIVE;
			if (value >= 0 || n > 0){
				HitCard.small( m.ch.sprite, value, color, codes, tints, n );
				if (host) EnchantNet.sendCard( card( r, m.ch, value, color, -1, false, 0f, true, -1, NONE_SENT,
						codes, tints, lands, n, false ) );
			}
		}
		//another struck one with a picture of its own and nothing else on it: its chip
		for (Piece p : elsewhere){
			if (p.chip == -1 || p.on == null || p.on.sprite == null || !p.on.sprite.visible || r.find( p.on ) != null) continue;
			HitCard.small( p.on.sprite, -1, CharSprite.NEGATIVE, new int[]{ p.chip }, new int[]{ p.chipTint }, 1 );
			if (host) EnchantNet.sendCard( card( r, p.on, -1, CharSprite.NEGATIVE, -1, false, 0f, true, -1, NONE_SENT,
					new int[]{ p.chip }, new int[]{ p.chipTint }, new float[]{ 0f }, 1, false ) );
		}
		//no card of the struck one, but something flowed back: its word goes alone
		if (host && !flowsSent && (r.heal > 0 || r.mana > 0 || r.shield > 0) && d != null){
			EnchantNet.sendCard( card( r, d, -1, CharSprite.NEGATIVE, -1, false, 0f, false, -1, NONE_SENT,
					codes, tints, lands, 0, true ) );
		}

		//what flows back to the wielder, when the last of what carries it lands
		if (a != null && a.sprite != null && (r.heal > 0 || r.mana > 0 || r.shield > 0)){
			final CharSprite as = a.sprite;
			final int heal = r.heal, mana = r.mana, shield = r.shield, cell = a.pos;
			home.then( () -> {
				if (as.parent == null || !as.visible) return;
				float x = as.x + as.width() / 2f, y = as.y;
				if (heal > 0){
					HitCard.gain( as, FloatingText.HEALING, heal, HEAL_COLOR );
					HealFx.heal( x, y, cell, heal );
				}
				if (mana > 0) HitCard.gain( as, FloatingText.MANA, mana, MANA_COLOR );
				if (shield > 0){
					HitCard.gain( as, FloatingText.SHIELDING, shield, SHIELD_COLOR );
					HealFx.shield( x, y, cell );
				}
			} );
		}
	}

	// ------------------------------------------------------------------ co-op (EnchantNet CARD)

	private static final int[] NONE_SENT = {};

	//a piece's place among the blow's pieces its guests were sent (those with a wire kind), -1 none
	static int sentIndex( Blow.Record r, Piece p ){
		if (p == null || p.kind < 0) return -1;
		int k = 0;
		for (Piece q : r.pieces){
			if (q == p) return k;
			if (q.kind >= 0) k++;
		}
		return -1;
	}

	/** A CARD's numbers before its accents: struck id, attacker id (-1), serial, number (-1 none),
	 *  its colour, the left icon, crit (1/0), when the X shows (ms), small (1/0), killed (1/0), the
	 *  lead's place among the blow's sent pieces (-1 none), how many accents. */
	public static final int CARD_HEAD = 12;
	/** Where in a CARD its number, crit, small (1/0), lead and accents' count are. */
	public static final int CARD_NUMBER = 3, CARD_CRIT = 6, CARD_SMALL = 8, CARD_LEAD = 10, CARD_ACCENTS = 11;

	/**
	 * A CARD (EnchantNet.CARD): CARD_HEAD numbers, the accents' places among the blow's sent pieces,
	 * one a beat in the beats' order (-1: a beat with nothing for the guests), the chips' count and each chip's code, tint and landing (ms); then, on the
	 * struck one's card, what flowed back to the wielder: heal, mana, shield, when what the host drew
	 * at once carries it in (ms: a lifesteal's beads, the lead's and the others' drains; a guest
	 * waits for his own accents' beads on top), carried.
	 */
	static int[] card( Blow.Record r, Char ch, int number, int color, int icon, boolean crit, float critIn, boolean small,
			int lead, int[] accents, int[] codes, int[] tints, float[] lands, int n, boolean flows ){
		int m = accents.length;
		int[] out = new int[CARD_HEAD + m + 1 + 3 * n + (flows ? 5 : 0)];
		out[0] = ch.id();
		out[1] = r.attacker != null ? r.attacker.id() : -1;
		out[2] = r.serial;
		out[CARD_NUMBER] = number;
		out[4] = color;
		out[5] = icon;
		out[CARD_CRIT] = crit ? 1 : 0;
		out[7] = Math.round( critIn * 1000f );
		out[CARD_SMALL] = small ? 1 : 0;
		out[9] = r.killed ? 1 : 0;
		out[CARD_LEAD] = lead;
		out[CARD_ACCENTS] = m;
		for (int i = 0; i < m; i++) out[CARD_HEAD + i] = accents[i];
		int c = CARD_HEAD + m;
		out[c] = n;
		for (int i = 0; i < n; i++){
			out[c + 1 + 3 * i] = codes[i];
			out[c + 2 + 3 * i] = tints[i];
			out[c + 3 + 3 * i] = Math.round( lands[i] * 1000f );
		}
		if (flows){
			int f = c + 1 + 3 * n;
			out[f] = r.heal;
			out[f + 1] = r.mana;
			out[f + 2] = r.shield;
			out[f + 3] = Math.round( (r.carried ? r.flowDelay : 0f) * 1000f );
			out[f + 4] = r.carried ? 1 : 0;
		}
		return out;
	}

	/**
	 * A co-op guest's blow, as its host presented it (EnchantNet: the blow's pieces held until its
	 * CARD came, made again on the guest's own sprites): the host's lead, its accents on the host's
	 * beats, what flows back on the wielder's lane, the motions and what played on another character
	 * (a chain, a sweep, Thorns on the attacker), the blood the host sent with its star, as they
	 * always were, and the card. `pieces` in the order the host sent them (null where one could not
	 * be made). Render thread.
	 */
	public static void guest( int[] card, Piece[] pieces, CharSprite as, CharSprite ds, CharSprite[] bloodOn, int[] bloods ){
		if (ds == null || card.length < CARD_HEAD) return;
		boolean killed = card[9] != 0;
		Piece lead = pick( pieces, card[CARD_LEAD] );
		int m = Math.max( 0, Math.min( card[CARD_ACCENTS], card.length - CARD_HEAD ) );
		//the accents on the host's beats (null: a beat with nothing for the guest)
		Piece[] accents = new Piece[m];
		for (int i = 0; i < m; i++) accents[i] = pick( pieces, card[CARD_HEAD + i] );
		boolean mayShake = involvesHero( as, ds );
		//the procs' places (the first leads), the first's stamp, the swing's tint and trail, as on the host
		ArrayList<Piece> procs = new ArrayList<>( 2 );
		EnchantFx.SwingPiece swing = null;
		for (Piece p : pieces){
			if (p == null) continue;
			if (p.rank == Blow.PROC && p.on == ds.ch) procs.add( p );
			if (swing == null && p instanceof EnchantFx.SwingPiece) swing = (EnchantFx.SwingPiece) p;
		}
		order( procs, lead, new ArrayList<>( procs.size() ) );
		if (!procs.isEmpty() && procs.get( 0 ) instanceof EnchantFx.ProcPiece) ((EnchantFx.ProcPiece) procs.get( 0 )).stamp();
		if (swing != null) swing.lead( false );
		//the blood the host sent and the blow's star from it, as they always were (STRONG on the
		//struck one when a kill or a crit leads)
		for (int i = 0; i < bloodOn.length; i++){
			CharSprite b = bloodOn[i];
			if (b == null) continue;
			com.watabou.utils.PointF c = b.center();
			int n = Math.min( 9, Math.max( 1, (int)(9 * Math.sqrt( bloods[i] / 20.0 )) ) );
			xyz.gabriwar.warpedpixeldungeon.effects.Splash.at( c, 0xFFBB0000, n );
			HitFx.draw( b, c.x + (b.flipHorizontal ? 8 : -8), c.y, bloods[i], HitFx.HIT, false, true,
					b == ds && (killed || (lead != null && lead.rank == Blow.CRIT)) );
		}
		//the wielder's card waits for what the guest's own pieces carry home, as the host's does
		final Home home = new Home();
		if (lead != null){
			lead.lead( mayShake );
			if (ds.visible) lead.sting();
			if (as != null) home.lands( lead.home() );
		}
		for (Piece p : pieces){
			if (p != null && p.rank == Blow.FINISHER && p != lead) p.lead( mayShake );
		}
		for (int i = 0; i < m; i++){
			final Piece p = accents[i];
			if (p == null) continue;
			final int beat = i + 1;
			final float side = (as == null || as.x <= ds.x ? 1f : -1f) * (beat % 2 == 1 ? 1f : -1f);
			final boolean carries = as != null && p.home() >= 0f;
			if (carries) home.pending++;
			later( BEAT * beat, () -> {
				if (ds.parent != null) p.accent( hitX( as, ds ) + side * ACCENT_SIDE, hitY( as, ds ) - ACCENT_UP * beat );
				if (carries) home.landed( p.home() );
			} );
		}
		int lane = 0;
		for (Piece p : pieces){
			if (p == null || p.lane != Blow.WIELDER || as == null) continue;
			final Piece w = p;
			final int beat = ++lane;
			later( BEAT * beat, () -> {
				if (as.parent == null) return;
				w.accent( wieldX( as ), wieldY( as, beat ) );
			} );
		}
		//the motions at the hit, and what played on another character, as on the host
		for (Piece p : pieces){
			if (p == null || p == lead || p == swing || p.lane == Blow.WIELDER || p.rank == Blow.FINISHER) continue;
			if (p.lane == Blow.MOTION){
				p.accent( hitX( as, ds ), hitY( as, ds ) );
			} else if (p.on != ds.ch){
				elsewhere( p, mayShake );
				if (as != null) home.lands( p.home() );
			}
		}
		guestCard( card, ds, as, home );
	}

	/** A guest's card of a CARD (its struck one's, or another's small one), and what flowed back. */
	public static void guestCard( int[] card, CharSprite ds, CharSprite as ){
		guestCard( card, ds, as, new Home() );
	}

	//the same, what flowed back shown once the host's word of it and the guest's own pieces landed
	private static void guestCard( int[] card, CharSprite ds, CharSprite as, final Home home ){
		if (ds == null || card.length < CARD_HEAD + 1) return;
		int m = Math.max( 0, Math.min( card[CARD_ACCENTS], card.length - CARD_HEAD - 1 ) );
		int c = CARD_HEAD + m;
		int n = Math.max( 0, Math.min( card[c], (card.length - c - 1) / 3 ) );
		int[] codes = new int[n], tints = new int[n];
		float[] lands = new float[n];
		for (int i = 0; i < n; i++){
			codes[i] = card[c + 1 + 3 * i];
			tints[i] = card[c + 2 + 3 * i];
			lands[i] = card[c + 3 + 3 * i] / 1000f;
		}
		boolean crit = card[CARD_CRIT] != 0;
		int number = card[CARD_NUMBER];
		if (ds.visible){
			if (card[CARD_SMALL] != 0){
				//a late card (no blow: serial 0) joins the live card as the host's did
				if (card[2] == 0 && number < 0) HitCard.late( ds, codes, tints, n );
				else HitCard.small( ds, number, card[4], codes, tints, n );
			} else if (number >= 0 || n > 0 || crit || card[5] >= 0){
				HitCard.show( ds, card[1], number, card[4], card[5], crit, card[7] / 1000f, codes, tints, lands, n );
			}
		}
		int f = c + 1 + 3 * n;
		if (as == null || card.length < f + 5) return;
		final int heal = card[f], mana = card[f + 1], shield = card[f + 2];
		if (heal <= 0 && mana <= 0 && shield <= 0) return;
		if (card[f + 4] != 0) home.lands( card[f + 3] / 1000f );
		home.then( () -> {
			if (as.parent == null || !as.visible) return;
			float x = as.x + as.width() / 2f, y = as.y;
			int cell = as.ch != null ? as.ch.pos : -1;
			if (heal > 0){
				HitCard.gain( as, FloatingText.HEALING, heal, HEAL_COLOR );
				HealFx.heal( x, y, cell, heal );
			}
			if (mana > 0) HitCard.gain( as, FloatingText.MANA, mana, MANA_COLOR );
			if (shield > 0){
				HitCard.gain( as, FloatingText.SHIELDING, shield, SHIELD_COLOR );
				HealFx.shield( x, y, cell );
			}
		} );
	}

	private static Piece pick( Piece[] pieces, int i ){
		return i >= 0 && i < pieces.length ? pieces[i] : null;
	}

	private static boolean involvesHero( CharSprite as, CharSprite ds ){
		xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero h = xyz.gabriwar.warpedpixeldungeon.Dungeon.hero;
		return h != null && h.sprite != null && (h.sprite == as || h.sprite == ds);
	}

	//an accent's beat (s from the blow), 0 for none
	private static float beatOf( Piece p, ArrayList<Piece> accents, float[] beatAt ){
		int i = accents.indexOf( p );
		return i >= 0 ? beatAt[i] : 0f;
	}

	//the number row's left icon: a bleed's own; else the blow's reason on the struck one (its
	//armour-piercing twin when its first number was), else the first number's icon
	private static int leftIcon( Blow.Record r, Blow.Mark m ){
		if (m.icon == FloatingText.BLEEDING) return m.icon;
		if (m.ch == r.defender && r.reason >= 0){
			int reason = r.reason;
			if (m.noBlock && reason < FloatingText.HIT_WEP + 18) reason += 18;
			return reason;
		}
		return m.icon;
	}

	//a chip's class for its place on the row: 0 a proc's, 1 an echo's, 2 a type's (silver), 3 a
	//line's (gold), 4 a buff's
	static int chipClass( int code ){
		//a skill's icon, a miss's reason and verb, immunity, a resisted effect, rage, a fuse: what
		//happened, first
		if (code >= HitCard.SKILL_BASE) return 0;
		if (code < 0) return 4;
		if ((code >= FloatingText.MISS_WEP && code <= FloatingText.RAGE) || code == FloatingText.FUSE) return 0;
		if (code == FloatingText.RELOAD) return 3;
		if (code >= FloatingText.F_EMBER && code <= FloatingText.F_HEX) return 0;
		if (code >= FloatingText.hollow( FloatingText.F_EMBER ) && code <= FloatingText.hollow( FloatingText.F_HEX )) return 1;
		if (code == FloatingText.EXECUTIONER || (code >= FloatingText.MOON && code <= FloatingText.DEFIANT)
				|| (code >= FloatingText.S_UNDEAD && code <= FloatingText.S_HUMANOID)) return 3;
		return 2;
	}

	//every chip a mark carries for its own card: its mark (a miss's verb, immunity) and reason, its
	//chips by class (procs, echoes, silver, gold), then its buffs
	private static int chips( Blow.Record r, Blow.Mark m, int[] codes, int[] tints, float[] lands ){
		int n = 0;
		if (m.markCell >= 0) n = put( codes, tints, lands, n, m.markCell, 0, 0f );
		if (m.markReason >= 0) n = put( codes, tints, lands, n, m.markReason, 0, 0f );
		for (int cls = 0; cls < 4; cls++){
			for (int i = 0; i < m.chipCount; i++){
				if (chipClass( m.chips[i] ) == cls) n = put( codes, tints, lands, n, m.chips[i], m.tints[i], 0f );
			}
		}
		return buffs( r, m, codes, tints, lands, n );
	}

	//a plain chip put at the front of the row, landing at once (the rest moved on, the last
	//dropped when full)
	private static int front( int[] codes, int[] tints, float[] lands, int n, int code ){
		for (int i = 0; i < n; i++) if (codes[i] == code) return n;
		int last = Math.min( n, codes.length - 1 );
		for (int i = last; i > 0; i--){
			codes[i] = codes[i - 1];
			tints[i] = tints[i - 1];
			lands[i] = lands[i - 1];
		}
		codes[0] = code;
		tints[0] = 0;
		lands[0] = 0f;
		return last + 1;
	}

	/** A late chips' CARD (Blow.late, Blow.counter: no blow, serial 0, small): the guest's
	 *  HitCard.late, or a small card with the number (-1 none). */
	static int[] lateCard( Char ch, int number, int color, int[] codes, int[] tints ){
		int n = codes.length;
		int[] out = new int[CARD_HEAD + 1 + 3 * n];
		out[0] = ch.id();
		out[1] = -1;
		out[2] = 0;
		out[CARD_NUMBER] = number;
		out[4] = color;
		out[5] = -1;
		out[CARD_SMALL] = 1;
		out[CARD_LEAD] = -1;
		out[CARD_ACCENTS] = 0;
		out[CARD_HEAD] = n;
		for (int i = 0; i < n; i++){
			out[CARD_HEAD + 1 + 3 * i] = codes[i];
			out[CARD_HEAD + 2 + 3 * i] = tints[i];
		}
		return out;
	}

	//the buffs announced on it that no piece of the blow stands for, as chips
	private static int buffs( Blow.Record r, Blow.Mark m, int[] codes, int[] tints, float[] lands, int n ){
		for (int i = 0; i < m.buffs.size(); i++){
			Class<?> c = m.buffs.get( i );
			if (folded( r, m.ch, c )) continue;
			n = put( codes, tints, lands, n, m.buffCodes[i], m.buffTints[i], 0f );
		}
		//buff chips Blow.chip put on it come with them
		for (int i = 0; i < m.chipCount; i++){
			if (chipClass( m.chips[i] ) == 4) n = put( codes, tints, lands, n, m.chips[i], m.tints[i], 0f );
		}
		return n;
	}

	private static boolean folded( Blow.Record r, Char ch, Class<?> buff ){
		for (Piece p : r.pieces){
			if (p.on != ch && p.lane != Blow.PLACED) continue;
			for (Class<?> f : p.folds) if (f.isAssignableFrom( buff )) return true;
		}
		return false;
	}

	private static int put( int[] codes, int[] tints, float[] lands, int n, int code, int tint, float land ){
		if (n >= codes.length) return n;
		for (int i = 0; i < n; i++) if (codes[i] == code) return n;
		codes[n] = code;
		tints[n] = tint;
		lands[n] = land;
		return n + 1;
	}
}
