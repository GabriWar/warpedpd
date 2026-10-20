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
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Sentinel;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Statue;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.ItemGhost;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.HitCard;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.HitFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Piece;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Wayward;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.RectF;

/**
 * The enchantments' pictures (docs/fx/enchant.md): every entry here is any thread (it hands its
 * work to the render thread, Fx.post), draws nothing of the game's Random (FxRandom only) and
 * changes nothing of the game.
 *
 *   proc(look, attacker, defender, s, level, flags)   a weapon's enchantment (or curse) went off
 *   ward(look, wearer, attacker, s, level, flags)       a glyph answered a blow: the ward arc, then its family
 *   passive(look, wearer, moment, s)                    a glyph's quiet moment: DODGE, ABSORB, SHRUG, STEP, TICK
 *   blowEnded(attacker, defender)                       a blow that ended in its proc (Char.attack returning
 *                                                       early): the hit star and the stamped family's death accent
 *   swing(attacker, defender, landed)                   every melee blow of an enchanted weapon: its trail
 *   markGained(item) / gainedAgo(item)                  an item just gained its magic (the slot's aura runs faster)
 *
 * A proc's strength s (0..1, strength() gives the usual shape) picks its tier: WEAK under MID_FROM,
 * MID under STRONG_FROM, STRONG past it, FINISHER when the defender is dead as the picture is made
 * (the proc killed it). Beat by beat: the defender's sprite is stamped (blowLook, STAMP_FOR s: HitFx's
 * star, bloom and crescent take the element), a passing light at the hit, a ring on the ground, the
 * family's core and bits, the weapon flaring at the wielder's side FLARE_AT s later with two motes
 * flicked at the foe, the defender's afterglow once its white flash is over, the follow-through, a
 * mark left on the floor, one sting. Own quads at most CAP[tier] (6/10/14/18), 2 lights, 1 decal.
 *
 * Several procs on one blow: the first plays in full and owns the stamp, the second at 80% with
 * its ring 0.05 s late, the third and later join (no core, no flare, no light, half the bits, at
 * most JOIN_CAP). A MULTI proc's first RICH_TARGETS targets get the full look, the rest a tinted
 * star and two motes (MINIMAL_CAP); and scene-wide no more than RICH_STARTS rich procs start in
 * RICH_WINDOW s, later ones minimal. ECHO (a vestige): a beat late, hollow, pale, a ghost double,
 * the doubled thin ring at the feet only echoes draw, quiet, at most ECHO_CAP; a vestigial glyph's
 * ward or quiet moment plays the same echo on the wearer, with no ward arc. CURSE: the family
 * without its light core and the hex over it (HexOverlay). The host sends each to co-op guests
 * (EnchantNet), who play it from their own sprites with the sting.
 *
 * A call site's own twist (an area's row) rides the same beat through the Twist overloads, inside
 * the same caps: it keeps TWIST_KEEPS[tier] of the quads from the family's draw (TWIST_KEEPS_ECHO
 * an echo's), so a weak row still shows its own pieces. An echo of a target past the first
 * RICH_TARGETS is its minimal star, pale.
 */
public final class EnchantFx {

	private EnchantFx(){}

	// ------------------------------------------------------------------ flags, tiers, moments, caps

	/** A vestige's echo; a curse's proc; one of several targets (its index: multi(i)). */
	public static final int ECHO = 1, CURSE = 2, MULTI = 4;

	/** MULTI with the target's index (0 the first). */
	public static int multi( int index ){
		return MULTI | (Math.max( 0, Math.min( 255, index ) ) << 8);
	}

	/** The target's index of a MULTI flags, 0 otherwise. */
	public static int targetIndex( int flags ){
		return (flags & MULTI) != 0 ? (flags >>> 8) & 0xFF : 0;
	}

	public static final int WEAK = 0, MID = 1, STRONG = 2, FINISHER = 3;
	/** Where a tier starts, by strength. */
	public static final float MID_FROM = 0.30f, STRONG_FROM = 0.60f;

	/** A glyph's quiet moments (passive): it made the wearer dodge, absorbed, shrugged a thing
	 *  off; a step it answers; a tick of what it keeps. */
	public static final int DODGE = 0, ABSORB = 1, SHRUG = 2, STEP = 3, TICK = 4;

	/** Own quads by tier, and for an echo, a third-or-later join, a minimal proc. */
	public static final int[] CAP = { 6, 10, 14, 18 };
	public static final int ECHO_CAP = 7, JOIN_CAP = 5, MINIMAL_CAP = 3;
	/** The quads a call site's twist keeps from its family's draw, by tier, and an echo's: the row's
	 *  own pieces show at every tier, and what it leaves goes on to the flick. */
	public static final int[] TWIST_KEEPS = { 3, 4, 5, 6 };
	public static final int TWIST_KEEPS_ECHO = 2;
	/** Lights and decals a proc may hold. */
	public static final int LIGHTS = 2, DECALS = 1;
	/** How long a stamp holds, s; how late an echo comes; when the weapon flares; when the
	 *  defender's afterglow and motion come; how late a second proc's ring is. */
	public static final float STAMP_FOR = 0.15f, ECHO_LATE = 0.08f, FLARE_AT = 0.04f, REACT_AT = 0.05f,
			RING_LATE = 0.05f;
	/** Rich procs: the first targets, and starts scene-wide in a window. */
	public static final int RICH_TARGETS = 3, RICH_STARTS = 3;
	public static final float RICH_WINDOW = 0.25f;
	/** How far off the wielder's middle its weapon side is, px (Crackle.WEAPON_SIDE). */
	public static final float WEAPON_SIDE = 5f;
	/** How often a glyph answers a step, at most, s. */
	public static final float STEP_GAP = 0.2f;

	/** The tier of a strength (WEAK, MID, STRONG; a kill is told apart as it is drawn). */
	public static int tier( float s ){
		return s >= STRONG_FROM ? STRONG : (s >= MID_FROM ? MID : WEAK);
	}

	/**
	 * The usual strength: 0.2 + 0.12 x the enchant level + 0.6 x how much of the defender's health
	 * the proc's amount is (at most all) + 0.5 x the power multiplier past 1; clamped to 0..1.
	 */
	public static float strength( int level, float amount, int ht, float pm ){
		float share = ht > 0 ? Math.min( 1f, Math.max( 0f, amount ) / ht ) : 0f;
		float s = 0.2f + 0.12f * level + 0.6f * share + 0.5f * Math.max( 0f, pm - 1f );
		return Math.max( 0f, Math.min( 1f, s ) );
	}

	/** What a call site adds to the family's look, on the same beat and within its caps. Render
	 *  thread, FxRandom only. */
	public interface Twist {
		void play( Beat b );
	}

	// ------------------------------------------------------------------ proc

	/** A weapon's enchantment (or curse, flags CURSE) went off on a defender. Any thread. */
	public static void proc( EnchantLook l, Char a, Char d, float s, int level, int flags ){
		proc( l, a, d, s, level, flags, null );
	}

	/** The same with a call site's twist. */
	public static void proc( final EnchantLook l, final Char a, final Char d, final float s, final int level, final int flags,
			final Twist twist ){
		if (l == null || d == null || d.sprite == null) return;
		final CharSprite as = a != null ? a.sprite : null, ds = d.sprite;
		final boolean seen = fightSeen( a, d ), hero = involvesHero( a, d );
		//inside a blow: its piece, drawn as the blow's lead or an accent (BlowPresenter)
		if (Blow.offer( new ProcPiece( l, a, as, d, ds, s, level, flags, twist, seen, hero, false, -1, -1 ) )) return;
		Fx.post( () -> runProc( l, a, as, d, ds, s, level, flags, -1, seen, hero, false, twist ) );
	}

	//a guest's: what the host sent (EnchantNet), from the guest's own sprites
	static void replayProc( EnchantLook l, CharSprite as, CharSprite ds, float s, int level, int flags, int tier ){
		if (l == null || ds == null) return;
		Fx.post( () -> runProc( l, as != null ? as.ch : null, as, ds.ch, ds, s, level, flags, tier, ds.visible,
				involvesHero( as != null ? as.ch : null, ds.ch ), true, null ) );
	}

	static void runProc( EnchantLook l, Char a, CharSprite as, Char d, CharSprite ds, float s, int level, int flags,
			int forcedTier, boolean seen, boolean hero, boolean guest, Twist twist ){
		boolean killed = forcedTier >= 0 ? forcedTier == FINISHER : (d != null && !d.isAlive());
		int tier = forcedTier >= 0 ? forcedTier : (killed ? FINISHER : tier( s ));
		if (!guest) EnchantNet.sendProc( l, a, d, s, level, flags, tier );
		if (ds.parent == null || Fx.glowAir() == null) return;
		boolean echo = (flags & ECHO) != 0;
		EnchantState st = EnchantState.of( ds );
		if (seen) Sting.ask( st, l, d, tier, echo, guest );
		if (!ds.visible && (as == null || !as.visible)) return;

		if (echo){
			//a target past the rich ones echoes as its minimal star, pale
			final boolean rich = targetIndex( flags ) < RICH_TARGETS;
			final Beat b = new Beat( l, as, ds, tier, level, flags, 0, rich ? Beat.ECHO_MODE : Beat.MINIMAL, false, killed, hero,
					rich ? ECHO_CAP : MINIMAL_CAP );
			later( ECHO_LATE, () -> {
				if (rich) EnchantFx.echo( b, twist );
				else if (b.ds.parent != null) play( b, twist );
			} );
			return;
		}

		float now = Game.timeTotal;
		if (now >= ds.blowUntil || ds.blowLook == null){
			st.blowIndex = 0;
			ds.blowLook = l;
			ds.blowUntil = now + STAMP_FOR;
		} else {
			st.blowIndex++;
		}
		int index = st.blowIndex;
		int mode = index == 0 ? Beat.RICH : (index == 1 ? Beat.SECOND : Beat.JOIN);
		if (targetIndex( flags ) >= RICH_TARGETS) mode = Beat.MINIMAL;
		if ((mode == Beat.RICH || mode == Beat.SECOND) && !richRoom()) mode = Beat.MINIMAL;
		int cap = mode == Beat.MINIMAL ? MINIMAL_CAP : mode == Beat.JOIN ? JOIN_CAP
				: mode == Beat.SECOND ? Math.round( CAP[tier] * 0.8f ) : CAP[tier];
		Beat b = new Beat( l, as, ds, tier, level, flags, index, mode, false, killed, hero, cap );
		play( b, twist );
		if (killed) st.accentUntil = ds.blowUntil;
	}

	//a proc's beat, past its stamp: the skeleton, the family, the accent, the hex, the twist
	static void play( Beat b, Twist twist ){
		if (b.minimal){
			minimal( b );
			if (twist != null) twist.play( b );
			return;
		}
		lights( b );
		ring( b );
		boolean flares = !b.join && !b.ward && !b.curse;
		if (flares) flare( b );
		Beat accent = b.tier == FINISHER ? b.split( 6 ) : null;
		int kept = twist != null ? b.take( TWIST_KEEPS[b.tier] ) : 0;
		if (b.curse) HexOverlay.play( b );
		family( b );
		if (accent != null) finisher( accent );
		b.giveBack( kept );
		if (twist != null) twist.play( b );
		if (flares) flick( b );
		shake( b );
	}

	//a minimal proc: a star tinted by the look and two motes
	private static void minimal( Beat b ){
		FxParticle p = b.lit( b.hx, b.hy, FxBudget.P0 );
		if (p != null){
			p.look( FxFrames.IMPACT_STAR, 0 ).ramp( b.echo ? b.look.pale : b.look.star, true ).life( 0.08f )
					.alpha( b.a( 0.9f ), 0, 0.04f ).snap();
		}
		MotifFx.burst( b, b.hx, b.hy, 2 );
	}

	/** A hex's light at the hit: VOID's dim violet at this level, never a pool (a curse eats the
	 *  light an enchantment would throw). */
	public static final float HEX_LIGHT = 0.3f;

	//one passing light at the hit (15/23/31 by tier); a pool under it once strong. A hex's: one
	//dim violet glow, no pool
	private static void lights( Beat b ){
		if (b.join) return;
		int size = b.tier == WEAK ? 15 : b.tier == MID ? 23 : 31;
		FxLight g = b.glow( b.hx, b.hy, size, b.curse ? Element.VOID.light : b.look.light, b.curse ? HEX_LIGHT : 0.55f );
		if (g != null) g.life( 0f, 0.03f, 0.25f );
		if (b.tier >= STRONG && !b.curse){
			FxLight p = b.pool( b.cx, b.feet + 2, b.tier == FINISHER ? 31 : 23, b.look.light, 0.3f );
			if (p != null) p.life( 0f, 0.1f, b.tier == FINISHER ? 0.5f : 0.4f );
		}
	}

	/** The ground ring's widths by tier (from, to); an enchant level of 3 or more one size up. */
	public static final int[] RING_FROM = { 9, 9, 13, 13 }, RING_TO = { 17, 23, 31, 41 };
	public static final float[] RING_LIFE = { 0.35f, 0.4f, 0.45f, 0.5f };

	static int ringTo( int tier, int level ){
		int to = RING_TO[tier];
		if (level >= 3){
			int i = FxFrames.ringIndex( to );
			to = FxFrames.RING_WIDTHS[Math.min( FxFrames.RING_WIDTHS.length - 1, i + 1 )];
		}
		return to;
	}

	//the ground ring rolling out (a curse's none: its hex closes a ring inward instead)
	private static void ring( Beat b ){
		if (b.curse) return;
		FxRing r = b.ring( b.cx, b.feet, RING_FROM[b.tier], ringTo( b.tier, b.level ), RING_LIFE[b.tier], b.look.light,
				0.6f, b.look.emissive );
		if (r != null && b.index > 0) r.delay( RING_LATE * b.index );
	}

	/** The motes the weapon side flicks at the foe as it flares. */
	public static final int FLICKS = 2;

	//the wielder's weapon side flaring just after the blow (a curse's never: the hex eats it)
	private static void flare( Beat b ){
		if (b.as == null) return;
		Flash f = b.flash( true, STAR_WINK, 0f, b.wx, b.wy, b.look.light, 0.85f, 0.1f );
		if (f != null) f.delay( FLARE_AT );
	}

	//two motes flicked from the flaring weapon side at the foe, paid from what the family and the
	//twist left of the tier's quads (a weak proc's core comes first)
	private static void flick( Beat b ){
		if (b.as == null) return;
		for (int i = 0; i < FLICKS; i++){
			FxParticle p = MotifFx.mote( b, MotifFx.TRAIL, b.wx, b.wy, b.dx * 40f + FxRandom.Float( -6f, 6f ),
					b.dy * 40f + FxRandom.Float( -10f, 2f ) );
			if (p != null) p.alpha( b.a( 0.9f ), FLARE_AT, 0.15f );
		}
	}

	/** A star winking: the light sheet's four-point star along the axes, then turned. */
	public static final int[][] STAR_WINK = { FxFrames.Light.STAR4_9_A, FxFrames.Light.STAR4_9_B };

	private static void shake( Beat b ){
		if (b.tier < STRONG || !b.hero || b.echo || b.join || b.minimal) return;
		if (b.fam == Family.FORCE || b.fam == Family.REAPER) Fx.shake( 1.5f, 0.12f );
		else Fx.shake( 1f, 0.08f );
	}

	//an echo: its doubled thin ring at the feet, one faint light, the family hollow and pale, and
	//its ghost double 0.08 s later two pixels on along the blow
	private static void echo( Beat b, Twist twist ){
		if (b.ds.parent == null) return;
		FxRing r1 = b.ring( b.cx, b.feet, 9, 17, 0.35f, EnchantLook.GHOST, 0.45f, true );
		FxRing r2 = b.ring( b.cx, b.feet, 9, 17, 0.35f, EnchantLook.GHOST, 0.45f, true );
		if (r2 != null) r2.delay( 0.06f );
		if (FxBudget.tier() == FxBudget.MINIMAL) return;
		FxLight g = b.glow( b.hx, b.hy, 11, EnchantLook.paled( b.look.light ), 0.3f );
		if (g != null) g.life( 0f, 0.03f, 0.2f );
		final Beat ghost = b.split( 2 );
		ghost.alphaMul = 0.3f;
		ghost.shift( b.dx * 2f, b.dy * 2f );
		int kept = twist != null ? b.take( TWIST_KEEPS_ECHO ) : 0;
		if (b.curse) HexOverlay.play( b );
		if (b.killed){
			b.scale = 0.5f;
			finisher( b );
		} else {
			family( b );
		}
		b.giveBack( kept );
		if (twist != null) twist.play( b );
		later( ECHO_LATE, () -> {
			if (ghost.ds.parent == null) return;
			if (ghost.killed){
				ghost.scale = 0.5f;
				finisher( ghost );
			} else {
				family( ghost );
			}
		} );
	}

	// ------------------------------------------------------------------ ward

	/** A glyph (or an armour curse, flags CURSE) answered a blow: the ward arc on the wearer toward
	 *  the attacker, then its family; a vestigial glyph's (flags ECHO) its echo on the wearer, with
	 *  no arc. Any thread. */
	public static void ward( EnchantLook l, Char wearer, Char attacker, float s, int level, int flags ){
		ward( l, wearer, attacker, s, level, flags, null );
	}

	/** The same with a call site's twist. */
	public static void ward( final EnchantLook l, final Char wearer, final Char attacker, final float s, final int level,
			final int flags, final Twist twist ){
		if (l == null || wearer == null || wearer.sprite == null) return;
		final CharSprite ws = wearer.sprite, as = attacker != null ? attacker.sprite : null;
		final boolean seen = fightSeen( attacker, wearer ), hero = involvesHero( attacker, wearer );
		if (Blow.offer( new ProcPiece( l, attacker, as, wearer, ws, s, level, flags, twist, seen, hero, true, -1, -1 ) )) return;
		Fx.post( () -> runWard( l, wearer, ws, attacker, as, s, level, flags, -1, -1, seen, hero, false, twist ) );
	}

	/** A glyph's quiet moment (DODGE, ABSORB, SHRUG: the arc and a weak answer; STEP, TICK: a
	 *  small answer only, a step at most once per STEP_GAP s on one wearer). Any thread. */
	public static void passive( EnchantLook l, Char wearer, int moment, float s ){
		passive( l, wearer, moment, s, null );
	}

	/** The same with a call site's twist. */
	public static void passive( final EnchantLook l, final Char wearer, final int moment, final float s, final Twist twist ){
		passive( l, wearer, moment, s, 0, twist );
	}

	/** The same with flags (ECHO: a vestigial glyph's quiet moment). */
	public static void passive( final EnchantLook l, final Char wearer, final int moment, final float s, final int flags,
			final Twist twist ){
		if (l == null || wearer == null || wearer.sprite == null) return;
		final CharSprite ws = wearer.sprite;
		final boolean seen = fightSeen( wearer, wearer ), hero = wearer == Dungeon.hero;
		//a moment of a blow (a dodge, an absorb, a shrug) is the blow's; a step or a tick its own
		if (moment != STEP && moment != TICK
				&& Blow.offer( new ProcPiece( l, null, null, wearer, ws, s, 0, flags, twist, seen, hero, true, moment, WEAK ) )) return;
		Fx.post( () -> runWard( l, wearer, ws, null, null, s, 0, flags, moment, WEAK, seen, hero, false, twist ) );
	}

	//a guest's
	static void replayWard( EnchantLook l, CharSprite ws, CharSprite as, float s, int level, int flags, int moment, int tier ){
		if (l == null || ws == null) return;
		Fx.post( () -> runWard( l, ws.ch, ws, as != null ? as.ch : null, as, s, level, flags, moment, tier, ws.visible,
				involvesHero( as != null ? as.ch : null, ws.ch ), true, null ) );
	}

	static void runWard( EnchantLook l, Char wearer, CharSprite ws, Char attacker, CharSprite as, float s, int level,
			int flags, int moment, int forcedTier, boolean seen, boolean hero, boolean guest, Twist twist ){
		boolean killed = forcedTier >= 0 ? forcedTier == FINISHER && moment < 0 : (attacker != null && !attacker.isAlive());
		int tier = forcedTier >= 0 ? forcedTier : (killed ? FINISHER : tier( s ));
		if (!guest) EnchantNet.sendWard( l, wearer, attacker, s, level, flags, moment, tier );
		if (ws.parent == null || Fx.glowAir() == null) return;
		EnchantState st = EnchantState.of( ws );
		float now = Game.timeTotal;
		if (moment == STEP){
			if (st.stepAt >= 0 && now - st.stepAt < STEP_GAP && now >= st.stepAt) return;
			st.stepAt = now;
		}
		boolean quiet = moment == STEP || moment == TICK;
		if (seen && !quiet) Sting.ask( st, l, wearer, tier, (flags & ECHO) != 0, guest );
		if (!ws.visible) return;
		if ((flags & ECHO) != 0 && !quiet){
			//a vestigial glyph's answer: the proc's echo on the wearer - a beat late, no arc, hollow
			//and pale, its doubled ring at his feet and its ghost double; an attacker it killed gets no
			//accent of its own, so the echo keeps to its cap
			final Beat b = new Beat( l, as, ws, Math.min( tier, STRONG ), level, flags, 0, Beat.ECHO_MODE, true, false, hero,
					ECHO_CAP );
			if (moment >= 0) b.facing( ws.flipHorizontal ? -1f : 1f );
			later( ECHO_LATE, () -> echo( b, twist ) );
			return;
		}
		//a quiet moment is always minimal and takes none of the scene's rich starts
		int mode = quiet ? Beat.MINIMAL : (richRoom() ? Beat.RICH : Beat.MINIMAL);
		int cap = mode == Beat.MINIMAL ? MINIMAL_CAP : CAP[Math.min( tier, STRONG )];
		Beat b = new Beat( l, as, ws, Math.min( tier, STRONG ), level, flags, 0, mode, true, false, hero, cap );
		if (moment >= 0 && !quiet) b.facing( ws.flipHorizontal ? -1f : 1f );
		if (!b.curse && !quiet) wardArc( b );
		if (quiet){
			MotifFx.burst( b, b.cx, moment == STEP ? b.feet - 1 : b.cy, 2 );
			if (twist != null) twist.play( b );
			return;
		}
		play( b, twist );
		if (killed && as != null && as.parent != null && attacker != null){
			Beat k = new Beat( l, ws, as, FINISHER, level, flags, 0, Beat.RICH, false, true, hero, 6 );
			finisher( k );
		}
	}

	/** The ward arc: the quarter of a shield's rim facing the blow, on the wearer's middle, in the
	 *  glyph's light, two frames at 0.9 then gone over 0.25 s. */
	public static final float ARC_ALPHA = 0.9f, ARC_HOLD = 0.067f, ARC_FADE = 0.25f;

	/** Each of the ward arc's eight frames as a run of one. */
	public static final int[][][] ARCS = new int[8][][];
	static {
		for (int k = 0; k < 8; k++) ARCS[k] = new int[][]{ FxFrames.Light.RIM_ARC_23[k] };
	}
	static final int[][] RING_5 = { FxFrames.RING_5 };

	private static void wardArc( Beat b ){
		int oct = HitFx.octant( -b.dx, -b.dy );
		Flash f = b.flash( true, ARCS[oct], 0f, b.cx, b.cy, b.look.light, ARC_ALPHA, ARC_HOLD + ARC_FADE );
		if (f != null) f.envelope( 0f, ARC_HOLD );
	}

	// ------------------------------------------------------------------ the end of a blow

	/** A blow that ended in its proc (Char.attack returning as the defender fell to it, before its
	 *  swing line): the swing's trail all the same (the killing blow is the one most worth seeing),
	 *  the hit star, with the stamp's colours, and the stamped family's death accent if a proc
	 *  stamped the blow and drew none; else the star alone. Never the blood burst (Splash draws the
	 *  game's Random). Any thread. */
	public static void blowEnded( final Char a, final Char d ){
		if (d == null || d.sprite == null) return;
		swing( a, d, true );
		final CharSprite as = a != null ? a.sprite : null, ds = d.sprite;
		final int style = HitFx.styleOf( a );
		final boolean hero = involvesHero( a, d );
		if (Blow.offer( new EndPiece( a, as, d, ds, style, hero ) )) return;
		Fx.post( () -> runBlowEnded( a, as, d, ds, style, hero, false ) );
	}

	static void replayBlowEnded( CharSprite as, CharSprite ds ){
		if (ds == null) return;
		Fx.post( () -> runBlowEnded( as != null ? as.ch : null, as, ds.ch, ds, HitFx.HIT,
				involvesHero( as != null ? as.ch : null, ds.ch ), true ) );
	}

	static void runBlowEnded( Char a, CharSprite as, Char d, CharSprite ds, int style, boolean hero, boolean guest ){
		if (!guest) EnchantNet.sendBlowEnded( a, d );
		drawEnded( a, as, d, ds, style, hero, true );
	}

	//the star and the stamped family's death accent if none was drawn; its shakes only when
	//`mayShake` (a blow's: the hero in it)
	static void drawEnded( Char a, CharSprite as, Char d, CharSprite ds, int style, boolean hero, boolean mayShake ){
		if (ds.parent == null || Fx.glowAir() == null) return;
		float fx, fy;
		if (as != null){
			fx = as.x + as.width() / 2f;
			fy = as.y + as.height() / 2f;
		} else {
			fx = ds.x + ds.width() / 2f + (ds.flipHorizontal ? 8f : -8f);
			fy = ds.y + ds.height() / 2f;
		}
		int ht = d != null ? Math.max( 1, d.HT ) : 1;
		HitFx.draw( ds, fx, fy, ht, style, hero, mayShake, false );
		float now = Game.timeTotal;
		EnchantLook stamp = now < ds.blowUntil ? ds.blowLook : null;
		EnchantState st = EnchantState.of( ds );
		if (stamp != null && now >= st.accentUntil){
			st.accentUntil = ds.blowUntil;
			finisher( new Beat( stamp, as, ds, FINISHER, 0, 0, 0, Beat.RICH, false, true, hero && mayShake, 6 ) );
		}
	}

	// ------------------------------------------------------------------ the swing

	/** The swing trail's motes (+1 for each 2 enchant levels, at most SWING_MOST; REDUCED 2,
	 *  MINIMAL none) and how long it takes from the weapon side to the hit. */
	public static final int SWING = 4, SWING_MOST = 6, SWING_REDUCED = 2;
	public static final float SWING_FOR = 0.12f;

	/**
	 * Every melee blow of an enchanted weapon (Char.attack, right before the blow's HitFx; a miss
	 * beside its whoosh): its look's motes trailing in an arc from the weapon side to where it
	 * lands (or past the foe, fading, on a miss), a flick of light for the hero's glowing families,
	 * a thrust across the gap for a reach blow, curving an octant away under Wayward's curse; and a
	 * landed blow that procs nothing still draws its star and crescent TINT toward the look. Shots
	 * and throws get no trail. Any thread.
	 */
	public static void swing( final Char a, final Char d, final boolean landed ){
		if (a == null || d == null || a.sprite == null || d.sprite == null) return;
		final Weapon w = attacking( a );
		final EnchantLook l = lookOf( w );
		if (l == null) return;
		final boolean melee = !(w instanceof MissileWeapon);
		final int level = levelOf( w );
		final boolean wayward = a.buff( Wayward.WaywardBuff.class ) != null;
		Level lv = Dungeon.level;
		final int dist = lv != null && a.pos >= 0 && d.pos >= 0 && a.pos < lv.length() && d.pos < lv.length()
				? lv.distance( a.pos, d.pos ) : 1;
		final boolean hero = a == Dungeon.hero;
		final CharSprite as = a.sprite, ds = d.sprite;
		if (Blow.offer( new SwingPiece( a, l, as, ds, landed, melee, level, wayward, dist, hero ) )) return;
		Fx.post( () -> runSwing( l, as, ds, landed, melee, level, wayward, dist, hero, false ) );
	}

	//a guest's (EnchantNet SWING): the host's swing, from the guest's own sprites
	static void replaySwing( EnchantLook l, CharSprite as, CharSprite ds, boolean landed, boolean melee, int level,
			boolean wayward, int dist ){
		if (l == null || as == null || ds == null) return;
		Fx.post( () -> runSwing( l, as, ds, landed, melee, level, wayward, dist, as.ch != null && as.ch == Dungeon.hero, true ) );
	}

	static void runSwing( EnchantLook l, CharSprite as, CharSprite ds, boolean landed, boolean melee, int level,
			boolean wayward, int dist, boolean hero, boolean guest ){
		if (!guest) EnchantNet.sendSwing( l, as.ch, ds.ch, landed, melee, level, wayward, dist );
		drawSwing( l, as, ds, landed, melee, level, wayward, dist, hero );
	}

	//the swing's stamp, then its trail and its flick of light
	static void drawSwing( EnchantLook l, CharSprite as, CharSprite ds, boolean landed, boolean melee, int level,
			boolean wayward, int dist, boolean hero ){
		if (landed){
			EnchantState st = EnchantState.of( ds );
			st.swingLook = l;
			st.swingUntil = Game.timeTotal + STAMP_FOR;
		}
		if (!melee || ds.parent == null || Fx.glowAir() == null || (!as.visible && !ds.visible)) return;
		int n = Math.min( SWING_MOST, SWING + level / 2 );
		if (FxBudget.tier() == FxBudget.REDUCED) n = Math.min( n, SWING_REDUCED );
		if (FxBudget.tier() == FxBudget.MINIMAL) n = 0;
		Beat b = new Beat( l, as, ds, MID, level, 0, 0, Beat.RICH, false, false, hero, n + 2 );
		if (dist >= 2){
			thrust( b );
		} else if (n > 0){
			trail( b, n, landed, wayward );
		}
		if (hero && l.emissive && FxLight.live() < FxBudget.lightCap()){
			FxLight g = b.glow( b.wx, b.wy, 7, l.light, 0.5f );
			if (g != null) g.life( 0f, 0.02f, 0.04f );
		}
	}

	//an arc of the look's motes from the weapon side to the hit (or past the foe on a miss),
	//each showing a little after the last: the stroke drawn as it goes
	private static void trail( Beat b, int n, boolean landed, boolean wayward ){
		float tx = b.hx, ty = b.hy;
		//its bulge: up off the stroke, an octant further round under Wayward
		float px = b.dy, py = -b.dx;
		if (py > 0){
			px = -px;
			py = -py;
		}
		if (wayward){
			float c = 0.7071f, sn = 0.7071f * (b.dx >= 0 ? 1f : -1f);
			float rx = px * c - py * sn, ry = px * sn + py * c;
			px = rx;
			py = ry;
		}
		if (!landed){
			tx += px * 6f + b.dx * 3f;
			ty += py * 6f + b.dy * 3f;
		}
		float bulge = wayward ? 7f : 4f;
		for (int i = 0; i < n; i++){
			float t = (i + 0.5f) / n;
			float k = (float)Math.sin( Math.PI * t ) * bulge;
			float x = b.wx + (tx - b.wx) * t + px * k, y = b.wy + (ty - b.wy) * t + py * k;
			FxParticle p = MotifFx.mote( b, MotifFx.TRAIL, x, y, b.dx * 12f, b.dy * 12f );
			if (p != null) p.alpha( landed ? 0.9f : 0.5f, SWING_FOR * t, landed ? 0.2f : 0.1f );
		}
	}

	//a reach blow: one tapering streak across the gap and a small ring at its tip
	private static void thrust( Beat b ){
		float gx = b.hx - b.wx, gy = b.hy - b.wy;
		float len = (float)Math.sqrt( gx * gx + gy * gy );
		FxParticle p = b.lit( (b.wx + b.hx) / 2f, (b.wy + b.hy) / 2f, FxBudget.P1 );
		if (p != null){
			p.look( FxFrames.STREAK_TAPER ).ramp( b.look.star, true ).life( SWING_FOR ).alpha( 0.8f, 0, SWING_FOR * 0.6f );
			p.angle = (float)Math.toDegrees( Math.atan2( gy, gx ) );
			p.scale.x = len > 24f ? 2f : 1f;
		}
		Flash tip = b.flash( false, true, RING_5, 0f, b.hx, b.hy + 2, b.look.light, 0.7f, 0.2f );
		if (tip != null) tip.delay( SWING_FOR * 0.6f );
	}

	// ------------------------------------------------------------------ inside a blow (BlowPresenter)

	/** A family's chip on a hit card: its cell (F_EMBER + the family; a curse's the hex), the
	 *  hollow twin for an echo. */
	public static int chipOf( EnchantLook l, boolean echo ){
		int cell = l.curse ? FloatingText.F_HEX : FloatingText.F_EMBER + l.fam.ordinal();
		return echo ? FloatingText.hollow( cell ) : cell;
	}

	/** Its tint: the look's glow lifted to CHIP_LUMA luminance at least (a curse's VOID's light),
	 *  an echo's drawn PALE toward the echo's grey. */
	public static int chipTintOf( EnchantLook l, boolean echo ){
		int c = l.curse ? Element.VOID.light : lifted( l.glow, CHIP_LUMA );
		return echo ? EnchantLook.paled( c ) : c;
	}

	/** The least luminance a family chip's colour is drawn at. */
	public static final float CHIP_LUMA = 0.45f;

	//a colour scaled up to a luminance (Rec. 601) at least, hue kept
	static int lifted( int color, float luma ){
		int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
		float y = (0.299f * r + 0.587f * g + 0.114f * b) / 255f;
		if (y >= luma) return color & 0xFFFFFF;
		if (y <= 0.001f){
			int v = Math.round( luma * 255f );
			return (v << 16) | (v << 8) | v;
		}
		float k = luma / y;
		r = Math.min( 255, Math.round( r * k ) );
		g = Math.min( 255, Math.round( g * k ) );
		b = Math.min( 255, Math.round( b * k ) );
		return (r << 16) | (g << 8) | b;
	}

	/**
	 * A proc, a glyph's answer or a blow's moment of one (a dodge, an absorb, a shrug), handed to
	 * the open blow. Every one draws its full look as before the ledger (the owner's call: a blow
	 * never thins a proc): the first leads at its tier (a kill draws its finisher); a second (at
	 * 80%, its ring a little late) and a third or later (a join) each play their own beat as the
	 * blow's accents, a beat apart and a few pixels higher each, their lights with them; a
	 * vestige's echo plays its whole echo (hollow, pale, its ghost double, its light) on its beat.
	 * Its chip is its family's (the echo's hollow).
	 */
	public static final class ProcPiece extends Piece {

		final EnchantLook l;
		final Char a, d;
		final CharSprite as, ds;
		final float s;
		final int level, flags, moment, forced;
		final Twist twist;
		final boolean seen, hero, ward;
		/** Its place among the blow's procs on its target (the presenter's: 0 leads, 1 the second, then
		 *  joins). */
		public int index;
		/** Made on a co-op guest from what the host sent (EnchantNet): its sting is heard even when
		 *  the look has its own sound (the host's code plays that one, on the host only). */
		boolean guest;
		//when its drawn beat's last bead home lands (Beat.home), and whether it drew: home() reads a lead's
		private float drewHome = -1f;
		private boolean drew;

		ProcPiece( EnchantLook l, Char a, CharSprite as, Char d, CharSprite ds, float s, int level, int flags, Twist twist,
				boolean seen, boolean hero, boolean ward, int moment, int forced ){
			super( ward ? EnchantNet.GLYPH : EnchantNet.PROC, (flags & ECHO) != 0 ? Blow.MINOR : Blow.PROC,
					(flags & ECHO) != 0 ? Blow.PLACED : Blow.TARGET, d );
			this.l = l;
			this.a = a;
			this.as = as;
			this.d = d;
			this.ds = ds;
			this.s = s;
			this.level = level;
			this.flags = flags;
			this.twist = twist;
			this.seen = seen;
			this.hero = hero;
			this.ward = ward;
			this.moment = moment;
			this.forced = forced;
			boolean echo = echo();
			priority = echo ? Blow.A_ECHO : Blow.A_SECOND;
			chip = chipOf( l, echo );
			chipTint = chipTintOf( l, echo );
		}

		/** A vestige's echo. */
		public boolean echo(){
			return (flags & ECHO) != 0;
		}

		/** A glyph's (or an armour curse's) answer, not a weapon's proc. */
		public boolean ward(){
			return ward;
		}

		/** Its look. */
		public EnchantLook look(){
			return l;
		}

		//its tier as it plays now: a kill's finisher (a ward's: the attacker it killed), else its own
		//(or the one it was given)
		int tierNow(){
			boolean killed = ward ? (moment < 0 && a != null && !a.isAlive()) : (d != null && !d.isAlive());
			if (killed) return FINISHER;
			return forced >= 0 ? forced : tier( s );
		}

		/** Its stamp on the struck one: HitFx's star, bloom and crescent take its colours (put on
		 *  before the star is drawn). A ward stamps nothing. */
		public void stamp(){
			if (ward || ds == null) return;
			ds.blowLook = l;
			ds.blowUntil = Game.timeTotal + STAMP_FOR;
			EnchantState.of( ds ).blowIndex = 0;
		}

		@Override
		public void send( int serial ){
			int tier = tierNow();
			if (ward) EnchantNet.sendWard( l, d, a, s, level, flags, moment, tier, serial );
			else EnchantNet.sendProc( l, a, d, s, level, flags, tier, serial );
		}

		@Override
		public void sting(){
			if (!seen || ds == null) return;
			boolean quiet = moment == STEP || moment == TICK;
			if (!quiet) Sting.ask( EnchantState.of( ds ), l, d, tierNow(), echo(), guest );
		}

		/** A drain's beads reach its wielder this long after its beat, -1 for any other proc, a ward's
		 *  (it drains into the wearer) or one drawing no line: a lead's as its beat drew them
		 *  (Beat.home), an accent's as its place plans them (DrainFx.home: a second, a join; a target
		 *  past the rich ones minimal), which the kit's thinning only brings sooner. */
		@Override
		public float home(){
			if (ward || l.fam != Family.DRAIN) return -1f;
			if (drew) return drewHome;
			if (!shown()) return -1f;
			int tier = tierNow();
			int mode;
			if (echo()){
				mode = targetIndex( flags ) < RICH_TARGETS && FxBudget.tier() != FxBudget.MINIMAL ? Beat.ECHO_MODE : Beat.MINIMAL;
			} else {
				mode = index == 0 ? Beat.RICH : (index == 1 ? Beat.SECOND : Beat.JOIN);
				if (targetIndex( flags ) >= RICH_TARGETS) mode = Beat.MINIMAL;
			}
			return DrainFx.home( new Beat( l, as, ds, tier, level, flags, index, mode, false, tier == FINISHER, false, 0 ), twist );
		}

		private boolean shown(){
			return ds != null && ds.parent != null && Fx.glowAir() != null && ds.visible;
		}

		@Override
		public void lead( boolean mayShake ){
			if (!shown()) return;
			if (echo()){
				accent( 0f, 0f );
				return;
			}
			draw( hero && mayShake, Float.NaN, 0f );
		}

		@Override
		public void accent( float x, float y ){
			if (!shown()) return;
			if (echo()){
				//today's echo, on the presenter's beat instead of its own: hollow, pale, its doubled
				//thin ring at the feet, its light, its ghost double
				int tier = tierNow();
				boolean killed = tier == FINISHER && !ward;
				boolean rich = targetIndex( flags ) < RICH_TARGETS;
				Beat b = new Beat( l, as, ds, ward ? Math.min( tier, STRONG ) : tier, level, flags, 0,
						rich ? Beat.ECHO_MODE : Beat.MINIMAL, ward, killed, false, rich ? ECHO_CAP : MINIMAL_CAP );
				if (ward && moment >= 0) b.facing( ds.flipHorizontal ? -1f : 1f );
				if (rich) EnchantFx.echo( b, twist );
				else play( b, twist );
				drew = true;
				drewHome = b.home();
				return;
			}
			draw( false, x, y );
		}

		//its own beat as runProc and runWard draw it at its place on the blow (a lead's at the hit,
		//an accent's moved to (x, y)), from its own caps
		private void draw( boolean shake, float x, float y ){
			int tier = tierNow();
			boolean killed = tier == FINISHER;
			Beat b;
			if (ward){
				boolean quiet = moment == STEP || moment == TICK;
				int mode = quiet ? Beat.MINIMAL : (richRoom() ? Beat.RICH : Beat.MINIMAL);
				b = new Beat( l, as, ds, Math.min( tier, STRONG ), level, flags, 0, mode, true, false, shake,
						mode == Beat.MINIMAL ? MINIMAL_CAP : CAP[Math.min( tier, STRONG )] );
				if (moment >= 0) b.facing( ds.flipHorizontal ? -1f : 1f );
				if (!Float.isNaN( x )) b.shift( x - b.hx, y - b.hy );
				if (!b.curse && !quiet) wardArc( b );
				play( b, twist );
				if (killed && moment < 0 && as != null && as.parent != null && a != null){
					finisher( new Beat( l, ds, as, FINISHER, level, flags, 0, Beat.RICH, false, true, shake, 6 ) );
				}
			} else {
				int mode = index == 0 ? Beat.RICH : (index == 1 ? Beat.SECOND : Beat.JOIN);
				if (targetIndex( flags ) >= RICH_TARGETS) mode = Beat.MINIMAL;
				if ((mode == Beat.RICH || mode == Beat.SECOND) && !richRoom()) mode = Beat.MINIMAL;
				int own = mode == Beat.MINIMAL ? MINIMAL_CAP : mode == Beat.JOIN ? JOIN_CAP
						: mode == Beat.SECOND ? Math.round( CAP[tier] * 0.8f ) : CAP[tier];
				b = new Beat( l, as, ds, tier, level, flags, index, mode, false, killed, shake, own );
				if (!Float.isNaN( x )) b.shift( x - b.hx, y - b.hy );
				play( b, twist );
				if (killed) EnchantState.of( ds ).accentUntil = ds.blowUntil;
			}
			drew = true;
			drewHome = b.home();
		}
	}

	/** An enchanted weapon's swing trail handed to the open blow: drawn with the lead (its stamp
	 *  before the star), whole: its trail and its flick of light. */
	public static final class SwingPiece extends Piece {

		final EnchantLook l;
		final CharSprite as, ds;
		final boolean landed, melee, wayward, hero;
		final int level, dist;

		SwingPiece( Char a, EnchantLook l, CharSprite as, CharSprite ds, boolean landed, boolean melee, int level,
				boolean wayward, int dist, boolean hero ){
			super( EnchantNet.SWING, Blow.MINOR, Blow.PLACED, a );
			this.l = l;
			this.as = as;
			this.ds = ds;
			this.landed = landed;
			this.melee = melee;
			this.level = level;
			this.wayward = wayward;
			this.dist = dist;
			this.hero = hero;
		}

		@Override
		public void send( int serial ){
			EnchantNet.sendSwing( l, as.ch, ds.ch, landed, melee, level, wayward, dist, serial );
		}

		@Override
		public void lead( boolean mayShake ){
			drawSwing( l, as, ds, landed, melee, level, wayward, dist, hero );
		}

		@Override
		public void accent( float x, float y ){
			lead( false );
		}
	}

	/** A blow that ended in its proc, handed to the open blow: the finisher's star and the stamped
	 *  family's death accent if the proc drew none, as runBlowEnded draws them. */
	public static final class EndPiece extends Piece {

		final Char a;
		final CharSprite as, ds;
		final int style;
		final boolean hero;

		EndPiece( Char a, CharSprite as, Char d, CharSprite ds, int style, boolean hero ){
			super( EnchantNet.BLOW_END, Blow.FINISHER, Blow.TARGET, d );
			this.a = a;
			this.as = as;
			this.ds = ds;
			this.style = style;
			this.hero = hero;
		}

		@Override
		public void send( int serial ){
			EnchantNet.sendBlowEnded( a, on, serial );
		}

		@Override
		public void lead( boolean mayShake ){
			drawEnded( a, as, on, ds, style, hero, mayShake );
		}

		@Override
		public void accent( float x, float y ){
			lead( false );
		}
	}

	// ------------------------------------------------------------------ restaged (the gallery, previews)

	/** A strength at the middle of a tier (for a restaging). */
	public static float strengthOf( int tier ){
		return tier <= WEAK ? 0.15f : tier == MID ? 0.45f : 0.8f;
	}

	/** A proc restaged between two sprites at a given tier (FINISHER draws the death accent though
	 *  nobody died), as the gallery and the previews show it: nothing sent, nothing of the game read
	 *  but the sprites. Render thread. */
	public static void stage( EnchantLook l, CharSprite as, CharSprite ds, int tier, int level, int flags ){
		if (l == null || ds == null) return;
		runProc( l, as != null ? as.ch : null, as, ds.ch, ds, strengthOf( tier ), level, flags, tier, true, false, true, null );
	}

	/** The same with a twist (an area's row restaged). */
	public static void stage( EnchantLook l, CharSprite as, CharSprite ds, int tier, int level, int flags, Twist twist ){
		if (l == null || ds == null) return;
		runProc( l, as != null ? as.ch : null, as, ds.ch, ds, strengthOf( tier ), level, flags, tier, true, false, true, twist );
	}

	/** A glyph's answer restaged on a wearer's sprite against an attacker's (moment -1: a blow). */
	public static void stageWard( EnchantLook l, CharSprite ws, CharSprite as, int tier, int level, int flags, int moment ){
		if (l == null || ws == null) return;
		runWard( l, ws.ch, ws, as != null ? as.ch : null, as, strengthOf( tier ), level, flags, moment,
				Math.min( tier, STRONG ), true, false, true, null );
	}

	/** A blow ending in its proc restaged: the star and the stamped family's accent. */
	public static void stageEnded( CharSprite as, CharSprite ds ){
		if (ds == null) return;
		runBlowEnded( as != null ? as.ch : null, as, ds.ch, ds, HitFx.SLASH, false, true );
	}

	/** A swing of an enchanted weapon restaged (dist 2 or more: a reach blow's thrust). */
	public static void stageSwing( EnchantLook l, CharSprite as, CharSprite ds, boolean landed, int level, int dist, boolean wayward ){
		if (l == null || as == null || ds == null) return;
		runSwing( l, as, ds, landed, true, level, wayward, dist, false, true );
	}

	// ------------------------------------------------------------------ gained items

	//the last items that gained their magic, render thread: identities and when
	private static final int[] gainedIds = new int[8];
	private static final float[] gainedAt = { -1, -1, -1, -1, -1, -1, -1, -1 };
	private static int gainedNext;

	/** An item just gained an enchantment or glyph (Enchanting, the pedestal, a pickup): its slot's
	 *  aura may run faster for a second. Render thread. */
	public static void markGained( Item item ){
		if (item == null) return;
		gainedIds[gainedNext] = System.identityHashCode( item );
		gainedAt[gainedNext] = Game.timeTotal;
		gainedNext = (gainedNext + 1) % gainedIds.length;
	}

	/** Seconds since the item was marked gained, or Float.MAX_VALUE if it was not lately. Render thread. */
	public static float gainedAgo( Item item ){
		if (item == null) return Float.MAX_VALUE;
		int id = System.identityHashCode( item );
		float best = Float.MAX_VALUE, now = Game.timeTotal;
		for (int i = 0; i < gainedIds.length; i++){
			if (gainedAt[i] >= 0 && gainedIds[i] == id && now >= gainedAt[i]) best = Math.min( best, now - gainedAt[i] );
		}
		return best;
	}

	// ------------------------------------------------------------------ reading the game

	/** The weapon a character strikes with: the hero's attacking weapon (a throw's, a shot's), a
	 *  statue's, a sentinel's. Null for anyone else. Reads only. */
	public static Weapon attacking( Char a ){
		if (a instanceof Hero){
			Hero h = (Hero) a;
			KindOfWeapon k = h.belongings != null ? h.belongings.attackingWeapon() : null;
			return k instanceof Weapon ? (Weapon) k : null;
		}
		if (a instanceof Statue) return ((Statue) a).weapon();
		if (a instanceof Sentinel) return ((Sentinel) a).weapon();
		return null;
	}

	/** A weapon's own primary look (a known curse's, or a good enchantment's): its first slot, else
	 *  its second; null for none. */
	public static EnchantLook lookOf( Weapon w ){
		if (w == null) return null;
		Weapon.Enchantment e = shown( w, w.enchantment );
		if (e == null) e = shown( w, w.enchantment2 );
		return e != null ? EnchantLooks.of( e.getClass() ) : null;
	}

	private static Weapon.Enchantment shown( Weapon w, Weapon.Enchantment e ){
		return e != null && (!e.curse() || w.cursedKnown) ? e : null;
	}

	private static int levelOf( Weapon w ){
		Weapon.Enchantment e = shown( w, w.enchantment );
		if (e == null) e = shown( w, w.enchantment2 );
		return e != null ? e.level() : 0;
	}

	//whether the hero sees the fight (Char.attack's visibleFight)
	static boolean fightSeen( Char a, Char d ){
		Level l = Dungeon.level;
		if (l == null || l.heroFOV == null) return true;
		return in( l, a ) || in( l, d );
	}

	private static boolean in( Level l, Char c ){
		return c != null && c.pos >= 0 && c.pos < l.heroFOV.length && l.heroFOV[c.pos];
	}

	static boolean involvesHero( Char a, Char d ){
		Hero h = Dungeon.hero;
		return h != null && (a == h || d == h);
	}

	//scene-wide: when the last rich procs started
	private static final float[] richAt = { -1e9f, -1e9f, -1e9f };
	private static int richNext;

	//room for one more rich proc in the window (taken if there is)
	static boolean richRoom(){
		float now = Game.timeTotal;
		float oldest = richAt[richNext];
		if (now - oldest < RICH_WINDOW && now >= oldest) return false;
		richAt[richNext] = now;
		richNext = (richNext + 1) % richAt.length;
		return true;
	}

	/** A new scene (EnchantArea.sceneCreated): none of the last one's rich starts, gained items or
	 *  stings count. */
	public static void reset(){
		for (int i = 0; i < richAt.length; i++) richAt[i] = -1e9f;
		for (int i = 0; i < gainedAt.length; i++) gainedAt[i] = -1;
		Sting.reset();
		HitCard.reset();
	}

	// ------------------------------------------------------------------ the families

	/** The family's own look on the beat. */
	public static void family( Beat b ){
		switch (b.fam){
			case EMBER:    EmberFx.play( b ); break;
			case RIME:     RimeFx.play( b ); break;
			case STORM:    StormFx.play( b ); break;
			case VENOM:    VenomFx.play( b ); break;
			case VERDANT:  VerdantFx.play( b ); break;
			case DREAD:    DreadFx.play( b ); break;
			case REAPER:   ReaperFx.play( b ); break;
			case CUT:      CutFx.play( b ); break;
			case DRAIN:    DrainFx.play( b ); break;
			case FORCE:    ForceFx.play( b ); break;
			case WARD:     WardFx.play( b ); break;
			case RADIANCE: RadianceFx.play( b ); break;
			case FORTUNE:  FortuneFx.play( b ); break;
			case MIRAGE:   MirageFx.play( b ); break;
			case HEART:    HeartFx.play( b ); break;
		}
	}

	/** The family's death accent on the beat (at most 6 quads, half that at scale 0.5). */
	public static void finisher( Beat b ){
		if (b.ds == null || b.ds.parent == null) return;
		switch (b.fam){
			case EMBER:    EmberFx.finisher( b ); break;
			case RIME:     RimeFx.finisher( b ); break;
			case STORM:    StormFx.finisher( b ); break;
			case VENOM:    VenomFx.finisher( b ); break;
			case VERDANT:  VerdantFx.finisher( b ); break;
			case DREAD:    DreadFx.finisher( b ); break;
			case REAPER:   ReaperFx.finisher( b ); break;
			case CUT:      CutFx.finisher( b ); break;
			case DRAIN:    DrainFx.finisher( b ); break;
			case FORCE:    ForceFx.finisher( b ); break;
			case WARD:     WardFx.finisher( b ); break;
			case RADIANCE: RadianceFx.finisher( b ); break;
			case FORTUNE:  FortuneFx.finisher( b ); break;
			case MIRAGE:   MirageFx.finisher( b ); break;
			case HEART:    HeartFx.finisher( b ); break;
		}
	}

	/** A gained item's family gathering into (x, y) over `seconds` (Enchanting's gather). Any thread. */
	public static void gather( final EnchantLook l, final float x, final float y, final float seconds ){
		if (l == null) return;
		if (!Fx.onRenderThread()){
			Fx.post( () -> gather( l, x, y, seconds ) );
			return;
		}
		if (Fx.glowAir() == null) return;
		//a curse taking hold runs the hex's own way: its motes leave, the item darkens
		if (l.curse){
			HexOverlay.gather( x, y, l, seconds );
			return;
		}
		switch (l.fam){
			case EMBER:    EmberFx.gather( x, y, l, seconds ); break;
			case RIME:     RimeFx.gather( x, y, l, seconds ); break;
			case STORM:    StormFx.gather( x, y, l, seconds ); break;
			case VENOM:    VenomFx.gather( x, y, l, seconds ); break;
			case VERDANT:  VerdantFx.gather( x, y, l, seconds ); break;
			case DREAD:    DreadFx.gather( x, y, l, seconds ); break;
			case REAPER:   ReaperFx.gather( x, y, l, seconds ); break;
			case CUT:      CutFx.gather( x, y, l, seconds ); break;
			case DRAIN:    DrainFx.gather( x, y, l, seconds ); break;
			case FORCE:    ForceFx.gather( x, y, l, seconds ); break;
			case WARD:     WardFx.gather( x, y, l, seconds ); break;
			case RADIANCE: RadianceFx.gather( x, y, l, seconds ); break;
			case FORTUNE:  FortuneFx.gather( x, y, l, seconds ); break;
			case MIRAGE:   MirageFx.gather( x, y, l, seconds ); break;
			case HEART:    HeartFx.gather( x, y, l, seconds ); break;
		}
	}

	/** A gained item's family ending on its owner at (x, y) (his middle), at a scale (Enchanting's
	 *  0.5, a reveal's 0.35). Any thread. */
	public static void finale( final EnchantLook l, final float x, final float y, final float scale ){
		if (l == null) return;
		if (!Fx.onRenderThread()){
			Fx.post( () -> finale( l, x, y, scale ) );
			return;
		}
		if (Fx.glowAir() == null) return;
		if (l.curse){
			HexOverlay.finale( x, y, l, scale );
			return;
		}
		switch (l.fam){
			case EMBER:    EmberFx.finale( x, y, l, scale ); break;
			case RIME:     RimeFx.finale( x, y, l, scale ); break;
			case STORM:    StormFx.finale( x, y, l, scale ); break;
			case VENOM:    VenomFx.finale( x, y, l, scale ); break;
			case VERDANT:  VerdantFx.finale( x, y, l, scale ); break;
			case DREAD:    DreadFx.finale( x, y, l, scale ); break;
			case REAPER:   ReaperFx.finale( x, y, l, scale ); break;
			case CUT:      CutFx.finale( x, y, l, scale ); break;
			case DRAIN:    DrainFx.finale( x, y, l, scale ); break;
			case FORCE:    ForceFx.finale( x, y, l, scale ); break;
			case WARD:     WardFx.finale( x, y, l, scale ); break;
			case RADIANCE: RadianceFx.finale( x, y, l, scale ); break;
			case FORTUNE:  FortuneFx.finale( x, y, l, scale ); break;
			case MIRAGE:   MirageFx.finale( x, y, l, scale ); break;
			case HEART:    HeartFx.finale( x, y, l, scale ); break;
		}
	}

	/** The quads a gather and a finale (at scale 1) may make. */
	public static final int GATHER_CAP = 8, FINALE_CAP = 12;

	// ------------------------------------------------------------------ later

	/** Runs `r` `seconds` from now on the render thread (a pooled timer in the light layer that
	 *  goes with the scene); nothing off the scene. Render thread. */
	public static void later( float seconds, Runnable r ){
		Later t = GameScene.recycle( GameScene.Layer.LIGHTS, Later.class );
		if (t == null) return;
		t.left = seconds;
		t.run = r;
		t.revive();
	}

	/** A timer: one runnable once, then dead (and pooled). */
	public static final class Later extends Gizmo {
		float left;
		Runnable run;

		@Override
		public void update(){
			super.update();
			if ((left -= Game.elapsed) > 0) return;
			Runnable r = run;
			run = null;
			kill();
			if (r != null) r.run();
		}
	}

	// ------------------------------------------------------------------ a beat

	/**
	 * One proc's (or ward's, swing's, gather's, finale's) moment as its pieces see it: the look,
	 * the two sprites (as the wielder or the attacker, ds where the core lands), where everything
	 * is (the hit point, the defender's middle, feet and top, the wielder's middle and weapon side,
	 * the blow's way), its tier and how it plays (rich, second, join, minimal, echo, ward), and its
	 * tally - the quads, lights and decals it may still make. Every maker here counts itself and
	 * makes nothing once the tally is spent, so a family can never pass its cap. Render thread.
	 */
	public static final class Beat {

		static final int RICH = 0, SECOND = 1, JOIN = 2, MINIMAL = 3, ECHO_MODE = 4;

		public final EnchantLook look;
		public final Family fam;
		public final Element el;
		/** The wielder (or, for a ward, the attacker) and where the core lands. */
		public final CharSprite as, ds;
		public final int tier, level, flags, index;
		public final boolean echo, curse, join, second, minimal, ward, killed, hero;
		/** A death accent's scale (0.5 for an echo's), and every alpha's share (0.3 for an echo's ghost double). */
		public float scale = 1f, alphaMul = 1f;
		/** The hit point, the core sprite's middle, feet, top and width, the blow's way (unit, from
		 *  the wielder to the defender), the wielder's middle and weapon side. */
		public float hx, hy, cx, cy, feet, top, width, dx, dy, sx, sy, wx, wy;

		//quads left (its own), lights and decals left (shared with its splits)
		private final int[] quads;
		private final int[] holds;
		//when the last bead it sends its wielder lands, s after the beat (shared with its splits)
		private final float[] home;

		Beat( EnchantLook l, CharSprite as, CharSprite ds, int tier, int level, int flags, int index, int mode,
				boolean ward, boolean killed, boolean hero, int cap ){
			this( l, as, ds, tier, level, flags, index, mode, ward, killed, hero, new int[]{ cap }, -1 );
		}

		/** The same drawing from a tally it shares (a blow's piece's share, Piece.share), with at most
		 *  `lights` lights (-1: its own). */
		Beat( EnchantLook l, CharSprite as, CharSprite ds, int tier, int level, int flags, int index, int mode,
				boolean ward, boolean killed, boolean hero, int[] tally, int lights ){
			look = l;
			fam = l.fam;
			el = l.el;
			this.as = as;
			this.ds = ds;
			this.tier = tier;
			this.level = level;
			this.flags = flags;
			this.index = index;
			echo = mode == ECHO_MODE || (flags & ECHO) != 0;
			curse = (flags & CURSE) != 0;
			join = mode == JOIN;
			second = mode == SECOND;
			minimal = mode == MINIMAL;
			this.ward = ward;
			this.killed = killed;
			this.hero = hero;
			quads = tally;
			int own = echo ? 1 : LIGHTS;
			holds = new int[]{ lights >= 0 ? Math.min( lights, own ) : own, DECALS };
			home = new float[]{ -1f };
			place();
		}

		//a split: its own share of quads, the rest shared
		private Beat( Beat parent, int quadsTaken ){
			look = parent.look;
			fam = parent.fam;
			el = parent.el;
			as = parent.as;
			ds = parent.ds;
			tier = parent.tier;
			level = parent.level;
			flags = parent.flags;
			index = parent.index;
			echo = parent.echo;
			curse = parent.curse;
			join = parent.join;
			second = parent.second;
			minimal = parent.minimal;
			ward = parent.ward;
			killed = parent.killed;
			hero = parent.hero;
			scale = parent.scale;
			alphaMul = parent.alphaMul;
			hx = parent.hx; hy = parent.hy; cx = parent.cx; cy = parent.cy; feet = parent.feet; top = parent.top;
			width = parent.width; dx = parent.dx; dy = parent.dy; sx = parent.sx; sy = parent.sy; wx = parent.wx; wy = parent.wy;
			quads = new int[]{ quadsTaken };
			holds = parent.holds;
			home = parent.home;
		}

		/** A beat of no proc at (x, y) (a gather's, a finale's): no sprites, `cap` quads, a light. */
		public static Beat at( EnchantLook l, float x, float y, int cap ){
			return at( l, x, y, new int[]{ cap } );
		}

		/** The same drawing from a tally it shares (a blow's piece's share). */
		public static Beat at( EnchantLook l, float x, float y, int[] tally ){
			Beat b = new Beat( l, null, null, MID, 0, l.curse ? CURSE : 0, 0, RICH, false, false, false, tally, -1 );
			b.hx = b.cx = b.sx = b.wx = x;
			b.hy = b.cy = b.sy = b.wy = y;
			b.feet = y + 7f;
			b.top = y - 7f;
			b.width = 12f;
			return b;
		}

		//where everything is, from the sprites
		private void place(){
			if (ds == null) return;
			cx = ds.x + ds.width() / 2f;
			cy = ds.y + ds.height() / 2f;
			feet = ds.y + ds.height() - 2f;
			top = ds.y + 1f;
			width = ds.width();
			if (as != null && as != ds){
				sx = as.x + as.width() / 2f;
				sy = as.y + as.height() / 2f;
			} else {
				sx = cx + (ds.flipHorizontal ? 8f : -8f);
				sy = cy;
			}
			float vx = cx - sx, vy = cy - sy;
			float len = (float)Math.sqrt( vx * vx + vy * vy );
			if (len < 0.5f){
				dx = ds.flipHorizontal ? -1f : 1f;
				dy = 0f;
			} else {
				dx = vx / len;
				dy = vy / len;
			}
			hx = cx - dx * 4f;
			hy = cy - dy * 4f;
			if (as != null){
				wx = sx + (as.flipHorizontal ? -WEAPON_SIDE : WEAPON_SIDE);
				wy = sy + 1f;
			} else {
				wx = sx;
				wy = sy;
			}
		}

		//a passive moment: the blow taken as coming from the side the wearer faces
		void facing( float side ){
			dx = -side;
			dy = 0f;
			hx = cx + side * 4f;
			hy = cy;
		}

		//every point moved (an echo's ghost double)
		void shift( float ox, float oy ){
			hx += ox; hy += oy; cx += ox; cy += oy; feet += oy; top += oy;
		}

		/** Its own share of `n` quads (as many as are left), for pieces drawn later on. */
		public Beat split( int n ){
			int k = take( n );
			return new Beat( this, k );
		}

		/** Quads it may still make. */
		public int left(){
			return quads[0];
		}

		/** Takes up to n quads now; how many it got. */
		public int take( int n ){
			int k = Math.max( 0, Math.min( n, quads[0] ) );
			quads[0] -= k;
			return k;
		}

		//quads taken back to it (a twist's share, kept from the family)
		void giveBack( int n ){
			quads[0] += Math.max( 0, n );
		}

		/** Takes n quads if it has them all (a piece of several, a ring and its second); none if not. */
		public boolean takeAll( int n ){
			if (quads[0] < n) return false;
			quads[0] -= n;
			return true;
		}

		/** A bead it sends its wielder (a drain's line, DrainFx) lands `at` s after the beat. */
		public void home( float at ){
			home[0] = Math.max( home[0], at );
		}

		/** When the last bead it (or a split of it) sent its wielder lands, s after the beat; -1 none. */
		public float home(){
			return home[0];
		}

		/** Lights it may still throw, decals it may still leave. */
		public int lightsLeft(){
			return holds[0];
		}

		public int decalsLeft(){
			return holds[1];
		}

		// -------------------------------------------------------------- colour

		/** A colour as this beat shows it (an echo's drawn pale). */
		public int c( int color ){
			return echo ? EnchantLook.paled( color ) : color & 0xFFFFFF;
		}

		/** An alpha as this beat shows it (an echo's at 0.6, its ghost's at 0.3 of that). */
		public float a( float alpha ){
			return alpha * alphaMul * (echo ? 0.6f : 1f);
		}

		/** Its ramp (an echo's pale). */
		public Ramp ramp(){
			return echo ? look.pale : el.ramp;
		}

		/** A count of bits as this beat makes them: half for a join or an echo, 80% for a second,
		 *  times its scale; at least one of one. */
		public int n( int count ){
			float k = join || echo ? 0.5f : second ? 0.8f : 1f;
			return count <= 0 ? 0 : Math.max( 1, Math.round( count * k * scale ) );
		}

		/** Whether (x, y) is on water. */
		public boolean onWater( float x, float y ){
			return WaterFX.onWater( x, y );
		}

		// -------------------------------------------------------------- makers

		/** A kit particle of light (added, over the night) at (x, y): one quad; null when spent, culled
		 *  by the budget (P0 never) or off the scene. */
		public FxParticle lit( float x, float y, int priority ){
			return particle( true, x, y, priority );
		}

		/** A kit particle of matter (normal, under the night's tint). */
		public FxParticle mat( float x, float y, int priority ){
			return particle( false, x, y, priority );
		}

		public FxParticle particle( boolean light, float x, float y, int priority ){
			if (quads[0] <= 0) return null;
			FxEmitter e = light ? Fx.glowAir() : Fx.air();
			if (e == null) return null;
			if (!FxBudget.keep( priority, x, y )) return null;
			quads[0]--;
			return ((FxParticle) e.recycle( FxParticle.class )).reset( x, y ).light( light ).priority( priority );
		}

		/** How many of n bits of a burst to make now: the tally's and the budget's say together. */
		public int burst( int n, int priority ){
			int k = FxBudget.allow( Math.min( n, quads[0] ), priority );
			return Math.min( k, quads[0] );
		}

		/** A ring on the ground (FxRing) in the floor layer; one quad. */
		public FxRing ring( float x, float y, int fromW, int toW, float life, int color, float alpha, boolean light ){
			if (quads[0] <= 0) return null;
			FxRing r = FxRing.ground( GameScene.layer( GameScene.Layer.FLOOR ), x, y, fromW, toW, life, c( color ), a( alpha ), light );
			if (r != null) quads[0]--;
			return r;
		}

		/** A painted frame or a run of them (Flash) of either sheet; one quad. Light frames are added
		 *  in the light layer, matter ones drawn normally among the effects. */
		public Flash flash( boolean lightSheet, int[][] frames, float fps, float x, float y, int color, float alpha, float life ){
			return flash( lightSheet, lightSheet, frames, fps, x, y, color, alpha, life );
		}

		/** The same, its sheet and its blend apart (a ring frame of the sheet added as light). */
		public Flash flash( boolean lightSheet, boolean additive, int[][] frames, float fps, float x, float y, int color,
				float alpha, float life ){
			if (quads[0] <= 0) return null;
			Flash f = Flash.make( lightSheet, additive, frames, fps, x, y, c( color ), a( alpha ), life );
			if (f != null) quads[0]--;
			return f;
		}

		/** A passing light (a glow); one of its lights. */
		public FxLight glow( float x, float y, int size, int color, float level ){
			if (holds[0] <= 0 || FxLight.live() >= FxBudget.lightCap()) return null;
			holds[0]--;
			return FxLight.glow( x, y, size, c( color ), level * alphaMul );
		}

		/** A pool of light on the ground; one of its lights. */
		public FxLight pool( float x, float y, int size, int color, float level ){
			if (holds[0] <= 0 || FxLight.live() >= FxBudget.lightCap()) return null;
			holds[0]--;
			return FxLight.pool( x, y, size, c( color ), level * alphaMul );
		}

		/** A mark on the floor; its one decal. An echo leaves none, but a rime's or a scorch's, at
		 *  half its alpha and hold. */
		public FxDecal decal( float x, float y, int[] frame, int color, float alpha, float hold, float fade ){
			if (holds[1] <= 0) return null;
			if (echo){
				if (fam != Family.RIME && fam != Family.EMBER) return null;
				alpha *= 0.5f;
				hold *= 0.5f;
			}
			holds[1]--;
			return FxDecal.at( x, y, frame, c( color ), alpha * alphaMul, hold, fade, FxRandom.chance( 0.5f ) );
		}

		/** Its one decal taken for a mark something else leaves (a droplet's splat as it lands):
		 *  true if it had it (never an echo's). */
		public boolean takeDecal(){
			if (echo || holds[1] <= 0) return false;
			holds[1]--;
			return true;
		}

		/** A frame or a run of frames lying on the floor (a ring closing, drawn under everyone),
		 *  matter; one quad. */
		public Flash floorFlash( int[][] frames, float x, float y, int color, float alpha, float life ){
			if (quads[0] <= 0) return null;
			Flash f = Flash.floor( frames, x, y, c( color ), a( alpha ), life );
			if (f != null) quads[0]--;
			return f;
		}

		/** An afterimage of a sprite (Afterimage); one quad. */
		public Afterimage after( CharSprite s, int color, float alpha, float life, float ox, float oy ){
			if (quads[0] <= 0 || s == null) return null;
			Afterimage a = Afterimage.of( s, c( color ), a( alpha ), life, ox, oy );
			if (a != null) quads[0]--;
			return a;
		}

		/** An item image's ghost (ItemGhost); one quad. */
		public ItemGhost ghost( int image, float x, float y, float angle, int color, float alpha, float life, float ox, float oy ){
			if (quads[0] <= 0) return null;
			ItemGhost g = ItemGhost.of( image, x, y, angle, c( color ), a( alpha ), life, ox, oy );
			if (g != null) quads[0]--;
			return g;
		}

		/** The defender's answer `REACT_AT` s on (never an echo's, a join's or a minimal one's):
		 *  its afterglow toward `color` over `seconds`, and its motion. */
		public void react( final int color, final float seconds, final int motion, final int px ){
			if (echo || join || minimal || ds == null) return;
			final CharSprite s = ds;
			final float bx = dx, by = dy;
			later( REACT_AT, () -> {
				if (s.parent == null) return;
				if (color >= 0) s.afterglow( color, seconds );
				move( s, motion, px, bx, by );
			} );
		}

		/** How a body answers: none, a recoil along the blow (px), a shiver (px either way, three
		 *  times), a sink (px down for 0.3 s). */
		public static final int STILL = 0, NUDGE = 1, SHIVER = 2, SINK = 3;

		static void move( final CharSprite s, int motion, int px, float bx, float by ){
			switch (motion){
				case NUDGE:
					s.nudge( Math.round( bx * px ), Math.round( by * px ), 0.08f );
					break;
				case SINK:
					s.nudge( 0, px, 0.3f );
					break;
				case SHIVER:
					s.shiver( px );
					later( 0.04f, () -> s.shiver( -px ) );
					later( 0.08f, () -> s.shiver( px ) );
					later( 0.12f, () -> s.shiver( 0 ) );
					break;
				default:
					break;
			}
		}

		/** A body's motion now (no afterglow): a gather's or a twist's. */
		public void moveNow( CharSprite s, int motion, int px ){
			if (s != null) move( s, motion, px, dx, dy );
		}
	}

	// ------------------------------------------------------------------ a flash

	/**
	 * A painted frame, or a run of frames stepped over its life (fps 0) or flipped through (fps),
	 * of either sheet, where a kit particle cannot go: the light sheet's stars, arcs, shafts and
	 * eyes, a ring closing, a frame turned a quarter. Its middle at (x, y), whole pixels; up to its
	 * alpha (at once, or over `in` s), held, then gone by the end of its life; moving at (vx, vy);
	 * shown only after its delay. Added in the light layer, or drawn normally among the effects
	 * (or on the floor). Pooled; counted as a kit particle while it shows. Render thread.
	 */
	public static class Flash extends Image {

		private int[][] frames;
		private float fps, life, age, delay, peak, in, hold, vx, vy, mx, my, moveAt;
		//its sheet: made on the light sheet, switched as a frame of the other asks
		private boolean additive, lightSheet = true;
		private int shown = -1;
		private boolean counted;
		private final RectF uv = new RectF();

		public Flash(){
			super( Assets.Effects.FX_LIGHT );
		}

		static Flash make( boolean lightSheet, boolean additive, int[][] frames, float fps, float x, float y, int color,
				float alpha, float life ){
			if (frames == null || frames.length == 0) return null;
			Flash f = GameScene.recycle( additive ? GameScene.Layer.LIGHTS : GameScene.Layer.EFFECTS, Flash.class );
			if (f == null) return null;
			f.setup( lightSheet, additive, frames, fps, x, y, color, alpha, life );
			return f;
		}

		/** On the floor (under everyone): a decal-like ring closing. Matter, normal blend. */
		static Flash floor( int[][] frames, float x, float y, int color, float alpha, float life ){
			Flash f = GameScene.recycle( GameScene.Layer.FLOOR, Flash.class );
			if (f == null) return null;
			f.setup( false, false, frames, 0f, x, y, color, alpha, life );
			return f;
		}

		private void setup( boolean lightSheet, boolean additive, int[][] frames, float fps, float x, float y, int color,
				float alpha, float life ){
			if (this.lightSheet != lightSheet){
				texture( lightSheet ? Assets.Effects.FX_LIGHT : Assets.Effects.FX_SHEET );
			}
			this.lightSheet = lightSheet;
			this.additive = additive;
			this.frames = frames;
			this.fps = fps;
			this.life = Math.max( 0.01f, life );
			age = delay = 0;
			peak = alpha;
			in = 0;
			hold = this.life;
			vx = vy = moveAt = 0;
			mx = x;
			my = y;
			angle = 0;
			scale.set( 1 );
			flipHorizontal = flipVertical = false;
			resetColor();
			hardlight( ((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f );
			shown = -1;
			show( 0 );
			am = alpha;
			visible = true;
			revive();
			if (!counted){
				counted = true;
				FxBudget.add( FxBudget.PARTICLES, 1 );
			}
		}

		/** Shown only after `seconds`. */
		public Flash delay( float seconds ){
			delay = Math.max( 0f, seconds );
			visible = false;
			return this;
		}

		/** Up over `in` s, held till `hold` s in, then gone by its end. */
		public Flash envelope( float in, float hold ){
			this.in = Math.max( 0f, in );
			this.hold = Math.max( this.in, hold );
			if (this.in > 0) am = 0;
			return this;
		}

		/** Moving at (vx, vy) px/s. */
		public Flash vel( float vx, float vy ){
			this.vx = vx;
			this.vy = vy;
			return this;
		}

		/** Moving at (vx, vy) px/s from `at` s of its life on (still till then). */
		public Flash vel( float vx, float vy, float at ){
			moveAt = Math.max( 0f, at );
			return vel( vx, vy );
		}

		/** Turned (a quarter turn for an upright ring; any angle for a line frame). */
		public Flash turn( float degrees ){
			angle = degrees;
			return this;
		}

		/** Whether it is added as light (else drawn normally). */
		public boolean additive(){
			return additive;
		}

		private void show( int i ){
			if (i == shown) return;
			shown = i;
			int[] f = frames[i];
			float tw = texture.width, th = texture.height;
			uv.set( f[0] / tw, f[1] / th, (f[0] + f[2]) / tw, (f[1] + f[3]) / th );
			frame( uv );
			origin.set( f[2] / 2f, f[3] / 2f );
			place();
		}

		private void place(){
			x = Math.round( mx - origin.x );
			y = Math.round( my - origin.y );
		}

		@Override
		public void update(){
			super.update();
			if (Emitter.freezeEmitters && Game.timeTotal > 1) return;
			float dt = Game.elapsed;
			if (delay > 0){
				delay -= dt;
				visible = false;
				return;
			}
			age += dt;
			if (age >= life){
				kill();
				return;
			}
			if (age >= moveAt){
				mx += vx * dt;
				my += vy * dt;
			}
			float p = age / life;
			int i = fps > 0 ? (int)(age * fps) % frames.length : Math.min( frames.length - 1, (int)(p * frames.length) );
			show( i );
			place();
			float a;
			if (in > 0 && age < in) a = age / in;
			else if (age <= hold) a = 1f;
			else a = 1f - (age - hold) / Math.max( 0.01f, life - hold );
			am = peak * Math.max( 0f, a );
			visible = am > 0.004f;
		}

		@Override
		public void draw(){
			if (additive){
				Blending.setLightMode();
				super.draw();
				Blending.setNormalMode();
			} else {
				super.draw();
			}
		}

		@Override
		public void kill(){
			super.kill();
			if (counted){
				counted = false;
				FxBudget.add( FxBudget.PARTICLES, -1 );
			}
		}
	}

}
