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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.WeaponLight;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Obfuscation;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.WeaponPerk;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;

/**
 * What a wielder carries, shown on him (the hero, and the NEAREST armed mobs in view: a statue's
 * or a sentinel's weapon, an armoured statue's glyph): the weapon's motif shedding a mote every WEAPON_EVERY s at
 * his weapon side; a glyph's every GLYPH_EVERY s on a 6x3 ellipse at his hips - a mirage's glyph
 * leaving instead a faint copy of him at MIRAGE_ALPHA each step he takes, Obfuscation a wisp of
 * smoke at his feet every SMOKE_EVERY s, twice as often while no foe in view sees him; the echo
 * (an identified vestige) a pale mote every ECHO_EVERY s; a Kinetic weapon's ring beating faster
 * and brighter as it stores damage (EnchantState.kineticBin). At most PER_LOOK motes alive a look
 * and PER_SPRITE a sprite (P3, the world's budget thinning them as it does any). The hero's
 * emissive weapon also lights the night round him (WeaponLight).
 *
 * A Bard's instrument holds its notes toward the next chord over the hero's weapon hand: a pip a
 * note (PIPS at most) on a short arc, in the colour of the chord coming, breathing; they fly into
 * the hand as the chord plays (TypeFx's chord draws the rest). They come first among his PER_SPRITE.
 *
 * It reads what the core's poll keeps in EnchantState (weaponCodes, armorCodes, echoCode,
 * kineticBin; a co-op guest's from the host's WIELD), so equipping, unequipping and enchanting show
 * within a poll with no hook of their own; a Bard's notes from the weapon's quality on the host, and
 * from the host's NOTES on a guest. Stepped from EnchantArea.afterDraw (render thread); FxRandom
 * only (MotifFx), no game Random; nothing allocated a frame.
 */
public final class WieldLook {

	private WieldLook(){}

	/** Seconds between two motes: a weapon's look, a glyph's, the echo's, Obfuscation's smoke. */
	public static final float WEAPON_EVERY = 1.2f, GLYPH_EVERY = 1.8f, ECHO_EVERY = 3.6f, SMOKE_EVERY = 2f;
	/** Kinetic's ring beats this often by its charge. */
	public static final float[] KINETIC_EVERY = { 1.2f, 0.8f, 0.5f, 0.35f };
	/** Motes alive a look and a sprite; mobs shown besides the heroes; how often they are ranked. */
	public static final int PER_LOOK = 2, PER_SPRITE = 6, NEAREST = 3;
	public static final float RANK_EVERY = 0.25f;
	/** A mirage glyph's copy of the wearer as he steps: its alpha and life; the echo's alpha. */
	public static final float MIRAGE_ALPHA = 0.15f, MIRAGE_LIFE = 0.4f, ECHO_ALPHA = 0.45f;

	//the channels: two weapon looks, two glyphs, the echo
	static final int W0 = 0, W1 = 1, G0 = 2, G1 = 3, ECHO = 4;

	/** A Bard's held notes: the most pips, their breath (lowest alpha, Hz), their flight into the hand
	 *  at a chord (s), the arc they sit on (px between them, px over the hand). */
	public static final int PIPS = 3;
	public static final float PIP_LO = 0.7f, PIP_HZ = 1f, PIP_HOME = 0.08f, PIP_GAP = 3f, PIP_UP = 6f;

	/** Once a frame for a drawn sprite (EnchantArea.afterDraw, after its poll). */
	public static void step( CharSprite s ){
		EnchantState st = EnchantState.get( s );
		Char ch = s.ch;
		if (st == null || ch == null) return;
		boolean hero = ch instanceof Hero;
		float now = Game.timeTotal;
		if (hero && ch == Dungeon.hero) WeaponLight.step( s, emissive( st ), now );
		//a Bard's held notes, read (and told to the guests) whether he shows or not; a restaged one's
		Held h = hero ? held( s, ch, now ) : staged( s, now );
		if (!s.visible || s.parent == null) return;

		float cx = s.x + s.width() / 2f, cy = s.y + s.height() / 2f;
		float wx = cx + (s.flipHorizontal ? -EnchantFx.WEAPON_SIDE : EnchantFx.WEAPON_SIDE), wy = cy + 1f;
		int pips = h != null ? pips( h, s, wx, wy, now ) : 0;
		if (!hero && !shown( s, st, now )) return;
		int live = live( st, now ) + pips;

		//the weapon's looks at its side
		for (int i = 0; i < st.weaponCount && live < PER_SPRITE; i++){
			EnchantLook l = EnchantLooks.byCode( st.weaponCodes[i] );
			if (l == null) continue;
			boolean kinetic = l.of == xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Kinetic.class;
			float every = kinetic ? KINETIC_EVERY[Math.min( KINETIC_EVERY.length - 1, st.kineticBin )] : WEAPON_EVERY;
			if (due( st, W0 + i, every, i * 0.6f, now ) && room( st, W0 + i, now )){
				FxParticle p = MotifFx.wield( l, wx, wy );
				if (p != null){
					if (kinetic && st.kineticBin > 0) p.alpha( Math.min( 1f, 0.7f * (1f + 0.2f * st.kineticBin) ), 0f, 0.2f );
					shed( st, W0 + i, l.motif.life, now );
					live++;
				}
			}
		}

		//a glyph's at the hips; a mirage's shows as he steps instead; Obfuscation's smoke at his feet
		for (int i = 0; i < st.armorCount && live < PER_SPRITE; i++){
			EnchantLook l = EnchantLooks.byCode( st.armorCodes[i] );
			if (l == null || l.fam == Family.MIRAGE) continue;
			boolean smoke = l.of == Obfuscation.class;
			float every = smoke ? SMOKE_EVERY : GLYPH_EVERY;
			if (!due( st, G0 + i, every, 0.3f + i * 0.9f, now ) || !room( st, G0 + i, now )) continue;
			if (smoke && !seen( ch )) st.wieldNext[G0 + i] = now + every / 2f;
			float x, y;
			if (smoke){
				x = cx;
				y = s.y + s.height() - 1f;
			} else {
				double a = 2 * Math.PI * Profile.hash( (int)(now * 10f), i + 1 );
				x = cx + 3f * (float)Math.cos( a );
				y = cy + s.height() * 0.15f + 1.5f * (float)Math.sin( a );
			}
			if (MotifFx.wield( l, x, y ) != null){
				shed( st, G0 + i, l.motif.life, now );
				live++;
			}
		}

		//the echo: a pale mote of the vestige's motif
		EnchantLook echo = EnchantLooks.byCode( st.echoCode );
		if (echo != null && live < PER_SPRITE && due( st, ECHO, ECHO_EVERY, 1.7f, now ) && room( st, ECHO, now )){
			FxParticle p = MotifFx.wield( echo, wx, wy );
			if (p != null){
				p.paint( EnchantLook.paled( echo.el.light ) ).alpha( ECHO_ALPHA, 0.1f, 0.4f );
				shed( st, ECHO, echo.motif.life, now );
			}
		}
	}

	/** A sprite stepped (EnchantArea.stepped, any thread): a mirage's glyph leaves a faint copy of
	 *  its wearer behind. */
	public static void stepped( final CharSprite s ){
		EnchantState st = EnchantState.get( s );
		if (st == null || st.armorCount == 0) return;
		EnchantLook m = null;
		for (int i = 0; i < st.armorCount && m == null; i++){
			EnchantLook l = EnchantLooks.byCode( st.armorCodes[i] );
			if (l != null && l.fam == Family.MIRAGE) m = l;
		}
		if (m == null) return;
		final int color = m.curse ? xyz.gabriwar.warpedpixeldungeon.effects.fx.Element.VOID.light : m.glow;
		Fx.post( () -> {
			if (s.parent == null || !s.visible || s.ch == null) return;
			if (!(s.ch instanceof Hero) && !shown( s, EnchantState.get( s ), Game.timeTotal )) return;
			Afterimage.of( s, color, MIRAGE_ALPHA, MIRAGE_LIFE, 0, 0 );
		} );
	}

	// ------------------------------------------------------------------ a Bard's held notes

	//a hero's held notes: his sprite, how many, which chord comes; what the guests were last told; the
	//pips over his hand, where each was last put, and since when and how many fly home at a chord
	private static final class Held {
		CharSprite s;
		int notes, sent = -1, flying, color = -1;
		boolean dread, sentDread;
		float homeAt = -1f, stagedUntil = -1f;
		final TypeFx.Mote[] pips = new TypeFx.Mote[PIPS];
		final float[] px = new float[PIPS], py = new float[PIPS];
	}

	private static final Held[] held = new Held[6];
	static {
		for (int i = 0; i < held.length; i++) held[i] = new Held();
	}

	//a sprite's held notes, its slot claimed if it had none (one whose sprite is gone, else the first)
	private static Held slot( CharSprite s ){
		Held free = null;
		for (Held h : held){
			if (h.s == s) return h;
			if (free == null && (h.s == null || h.s.parent == null)) free = h;
		}
		Held h = free != null ? free : held[0];
		for (int i = 0; i < PIPS; i++){
			if (owned( h.pips[i], h.s )) h.pips[i].kill();
			h.pips[i] = null;
		}
		h.s = s;
		h.notes = h.flying = 0;
		h.sent = h.color = -1;
		h.dread = h.sentDread = false;
		h.homeAt = h.stagedUntil = -1f;
		return h;
	}

	//the host's read of a hero's notes (the instrument in his hand, typed: a Bard's), told to the guests
	//as it changes; a guest's come from the host (notes)
	private static Held held( CharSprite s, Char ch, float now ){
		if (NetManager.isNetClient()){
			for (Held h : held) if (h.s == s) return h;
			return null;
		}
		Weapon w = EnchantState.wielded( ch );
		Quality q = Quality.of( w );
		boolean bard = q != null && WeaponPerk.of( w ) == WeaponPerk.BARD;
		int n = bard ? Math.min( PIPS, q.notes ) : 0;
		boolean dread = bard && q.dreadNext;
		Held h = null;
		for (Held k : held) if (k.s == s) h = k;
		if (h == null){
			if (n == 0) return null;
			h = slot( s );
		}
		set( h, n, dread, now );
		if (NetManager.isHost() && (n != h.sent || dread != h.sentDread)){
			h.sent = n;
			h.sentDread = dread;
			EnchantNet.sendNotes( ch, n, dread );
		}
		return h;
	}

	/** Notes held on any sprite for `seconds` (the effects gallery's and the previews' restaging: a
	 *  puppet holds no instrument), then flying home; 0 notes flies them home now, as a chord does.
	 *  Render thread. */
	public static void stage( CharSprite s, int notes, boolean dread, float seconds ){
		EnchantState.of( s );
		Held h = slot( s );
		set( h, Math.max( 0, Math.min( PIPS, notes ) ), dread, Game.timeTotal );
		h.stagedUntil = Game.timeTotal + Math.max( 0f, seconds );
	}

	//a restaged sprite's notes, flying home once their time is up
	private static Held staged( CharSprite s, float now ){
		for (Held h : held){
			if (h.s != s || h.stagedUntil < 0f) continue;
			if (now > h.stagedUntil && h.notes > 0) set( h, 0, h.dread, now );
			return h;
		}
		return null;
	}

	/** The host's NOTES (EnchantNet): a hero's held notes as the guest shows them. Render thread. */
	static void notes( CharSprite s, int notes, boolean dread ){
		set( slot( s ), Math.max( 0, Math.min( PIPS, notes ) ), dread, Game.timeTotal );
	}

	//new notes held: those there were fly home when a chord takes them all
	private static void set( Held h, int n, boolean dread, float now ){
		if (n == 0 && h.notes > 0){
			h.flying = h.notes;
			h.homeAt = now;
		}
		h.notes = n;
		h.dread = dread;
	}

	//the pips over the hand at (wx, wy): one a note held, breathing; flying into the hand at a chord.
	//How many show
	private static int pips( Held h, CharSprite s, float wx, float wy, float now ){
		float home = h.homeAt < 0 || now < h.homeAt ? 1f : (now - h.homeAt) / PIP_HOME;
		if (home >= 1f) h.flying = 0;
		int color = TypeFx.chordColor( h.dread );
		int shown = 0;
		for (int i = 0; i < PIPS; i++){
			TypeFx.Mote m = h.pips[i];
			boolean live = owned( m, s );
			if (!live) h.pips[i] = null;
			if (i < h.flying){
				//on its way into the hand, in the colour it had
				if (!live) continue;
				m.put( h.px[i] + (wx - h.px[i]) * home, h.py[i] + (wy - h.py[i]) * home, 1f );
				shown++;
				continue;
			}
			if (i >= h.notes){
				if (live) m.kill();
				h.pips[i] = null;
				continue;
			}
			if (!live){
				if (!FxBudget.keep( FxBudget.P1 )) continue;
				m = h.pips[i] = TypeFx.Mote.make( FxFrames.MOTE_2, color, 1f, true );
				if (m == null) continue;
				m.pip( s );
			}
			if (h.color != color) m.lit( color );
			float off = i - (h.notes - 1) / 2f;
			h.px[i] = wx + off * PIP_GAP;
			h.py[i] = wy - PIP_UP + Math.abs( off );
			float breath = PIP_LO + (1f - PIP_LO) * (0.5f + 0.5f * (float)Math.sin( 2 * Math.PI * PIP_HZ * now + i * 0.6f ));
			m.put( h.px[i], h.py[i], breath );
			shown++;
		}
		if (h.flying == 0) h.color = color;
		return shown;
	}

	//a pip still s's own: a pooled mote, once killed, may come back as another piece's
	private static boolean owned( TypeFx.Mote m, CharSprite s ){
		return m != null && s != null && m.alive && m.parent != null && m.isPipOf( s );
	}

	/** A hero's pips showing now (tests). */
	static int pipsShown( CharSprite s ){
		int n = 0;
		for (Held h : held){
			if (h.s != s) continue;
			for (TypeFx.Mote m : h.pips) if (owned( m, s ) && m.visible) n++;
		}
		return n;
	}

	/** A sprite went (EnchantArea.gone, any thread): the hero's night light with it. */
	public static void gone( CharSprite s ){
		WeaponLight.gone( s );
	}

	//the hero's weapon's emissive colour, -1 for none
	private static int emissive( EnchantState st ){
		for (int i = 0; i < st.weaponCount; i++){
			EnchantLook l = EnchantLooks.byCode( st.weaponCodes[i] );
			if (l != null && l.emissive && !l.curse) return l.light;
		}
		return -1;
	}

	//whether a channel is due (its first time `first` s after the sprite's state was made); the
	//next time set
	private static boolean due( EnchantState st, int ch, float every, float first, float now ){
		float next = st.wieldNext[ch];
		if (next == 0f){
			st.wieldNext[ch] = now + first;
			return false;
		}
		if (now < next && next - now <= every) return false;
		st.wieldNext[ch] = now + every;
		return true;
	}

	//room for a channel's next mote: under PER_LOOK of its own alive
	private static boolean room( EnchantState st, int ch, float now ){
		int n = 0;
		for (int k = 0; k < PER_LOOK; k++) if (now - st.wieldAt[ch * 2 + k] < st.wieldLife[ch * 2 + k]) n++;
		return n < PER_LOOK;
	}

	private static void shed( EnchantState st, int ch, float life, float now ){
		int a = ch * 2, b = a + 1;
		int i = st.wieldAt[a] <= st.wieldAt[b] ? a : b;
		st.wieldAt[i] = now;
		st.wieldLife[i] = life;
	}

	/** Its motes alive now, all its looks together, and a line look's (LineLooks) sharing its ceiling. */
	public static int live( EnchantState st, float now ){
		int n = 0;
		for (int i = 0; i < st.wieldAt.length; i++) if (now - st.wieldAt[i] < st.wieldLife[i]) n++;
		if (now < st.lineUntil && st.lineUntil - now <= 2f) n += st.lineQuads;
		return n;
	}

	//whether any foe in the hero's view sees the wearer (read: the foes' own fields of view)
	private static boolean seen( Char wearer ){
		Level l = Dungeon.level;
		if (l == null || l.mobs == null || l.heroFOV == null) return true;
		for (Mob m : l.mobs){
			if (m.alignment != Char.Alignment.ENEMY || m.pos < 0 || m.pos >= l.heroFOV.length || !l.heroFOV[m.pos]) continue;
			if (m.fieldOfView != null && wearer.pos >= 0 && wearer.pos < m.fieldOfView.length && m.fieldOfView[wearer.pos]) return true;
		}
		return false;
	}

	// ------------------------------------------------------------------ the armed mobs shown

	//the armed sprites drawn this window and their distances from the hero; those shown, and the
	//window they were ranked in
	private static final CharSprite[] cands = new CharSprite[16];
	private static final int[] dists = new int[16];
	private static final CharSprite[] nearest = new CharSprite[NEAREST];
	private static int candCount;
	private static float windowAt = -1e9f;

	//whether an armed mob's sprite is among the NEAREST to the hero in view (as ranked last window);
	//it joins this window's ranking
	static boolean shown( CharSprite s, EnchantState st, float now ){
		if (now - windowAt >= RANK_EVERY || now < windowAt){
			for (int k = 0; k < NEAREST; k++){
				int best = -1;
				for (int i = 0; i < candCount; i++){
					if (cands[i] == null) continue;
					if (best < 0 || dists[i] < dists[best]) best = i;
				}
				nearest[k] = best >= 0 ? cands[best] : null;
				if (best >= 0) cands[best] = null;
			}
			for (int i = 0; i < candCount; i++) cands[i] = null;
			candCount = 0;
			windowAt = now;
		}
		if (st != null && (st.weaponCount > 0 || st.armorCount > 0 || st.echoCode >= 0)){
			boolean in = false;
			for (int i = 0; i < candCount; i++) if (cands[i] == s) in = true;
			if (!in){
				int d = distance( s.ch );
				if (candCount < cands.length){
					cands[candCount] = s;
					dists[candCount++] = d;
				} else {
					int far = 0;
					for (int i = 1; i < candCount; i++) if (dists[i] > dists[far]) far = i;
					if (dists[far] > d){
						cands[far] = s;
						dists[far] = d;
					}
				}
			}
		}
		for (CharSprite n : nearest) if (n == s) return true;
		return false;
	}

	private static int distance( Char c ){
		Level l = Dungeon.level;
		if (l == null || c == null || Dungeon.hero == null || Dungeon.hero.pos < 0 || c.pos < 0) return 0;
		return l.distance( Dungeon.hero.pos, c.pos );
	}

	/** A test's: nobody ranked, no notes held. */
	static void reset(){
		for (Held h : held){
			h.s = null;
			h.notes = h.flying = 0;
			h.sent = h.color = -1;
			h.homeAt = h.stagedUntil = -1f;
			for (int i = 0; i < PIPS; i++) h.pips[i] = null;
		}
		for (int i = 0; i < cands.length; i++) cands[i] = null;
		for (int i = 0; i < NEAREST; i++) nearest[i] = null;
		candCount = 0;
		windowAt = -1e9f;
	}
}
