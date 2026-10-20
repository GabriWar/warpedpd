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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.MirrorImage;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxOrbit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.HitFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.DrainFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.DreadFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.Beat;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.Flash;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLook;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLooks;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.FortuneFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.HexOverlay;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.MirageFx;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Corrupting;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Hunting;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Lucky;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vicious;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vorpal;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Visual;
import com.watabou.utils.PointF;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.*;

/**
 * The blood, shadow, fortune and mirage rows' own pieces (docs/fx/enchant.md, "Blood, shadow,
 * fortune and mirage"): what each of their enchantments, glyphs and curses adds to its family's
 * look, as EnchantFx twists riding the proc's own beat and caps, and the few moments that are no
 * blow (a corrupted foe's ink and its rising, Lucky's mark, a step through water). Every entry is
 * any thread (the twists run inside EnchantFx's posted work; the rest post themselves), draws
 * nothing of the game's Random (FxRandom only) and changes nothing of the game: what it reads of a
 * character it reads, never writes.
 *
 * Its rows: DREAD (Eldritch, Horror, Jupiter's horror, Corrupting), REAPER (Grim), CUT (Vorpal,
 * Hunting, Vicious, Slashing, Buzz saw; Thorns), DRAIN (Vampiric, Ares' leech, Osmose; Siphoning;
 * Sacrificial, Metabolism under the hex), FORTUNE (Lucky, Unstable, Midas, Luck, Crom's luck;
 * Wondrous), MIRAGE (Afterimage, Flow, Mirror image, Mirroring, Phasing, Swiftness; Displacing,
 * Wayward, Displacement, Multiplicity) and HEART (Affection, Nomnom; Friendly). Projecting and
 * Obfuscation have no proc moment and no row here.
 */
public final class ShadowRows {

	private ShadowRows(){}

	/** The witnesses, drained foes and images drawn in full (the rest a bead or nothing). */
	public static final int RICH = EnchantFx.RICH_TARGETS;

	/** Eldritch's red (its eye, its witnesses' alarm), Jupiter's lilac, the hunter's amber. */
	public static final int EYE_RED = 0xFF4060, LILAC = 0xC8B0FF, AMBER = 0xFFB040;

	static final int[][] EXCLAIM = FxFrames.EXCLAIM;
	static final int[][] RING_9 = { FxFrames.RING_9 };
	static final int[][] PLUS_5 = { FxFrames.PLUS_5 };
	static final int[][] BUBBLE_31 = { FxFrames.Light.BUBBLE_31 };
	static final int[][] SHAFT_24 = { FxFrames.Light.SHAFT_24 };
	/** A wide ring closing on its middle (Ares' leech's drain radius). */
	static final int[][] CLOSING_41 = { FxFrames.RING_41, FxFrames.RING_31, FxFrames.RING_23, FxFrames.RING_17, FxFrames.RING_9 };
	/** Sound waves widening (Annoying's scream). */
	static final int[][] WIDENING = { FxFrames.RING_9, FxFrames.RING_13, FxFrames.RING_17, FxFrames.RING_23 };

	// ------------------------------------------------------------------ the pieces

	static float midX( CharSprite s ){
		return s.x + s.width() / 2f;
	}

	static float midY( CharSprite s ){
		return s.y + s.height() / 2f;
	}

	/** A sprite's weapon side (EnchantFx's WEAPON_SIDE off its middle, the way it faces). */
	static float sideX( CharSprite s ){
		return midX( s ) + (s.flipHorizontal ? -WEAPON_SIDE : WEAPON_SIDE);
	}

	static float sideY( CharSprite s ){
		return midY( s ) + 1f;
	}

	//whether a beat draws its own full pieces (an echo's hollow ones too; a join's and a
	//minimal one's are its family's alone)
	static boolean full( Beat b ){
		return !b.join && !b.minimal;
	}

	//a line of light from (x, y) its way, len px (a twig of static, a claw's rake)
	static FxParticle line( Beat b, float x, float y, float angle, float len, int color, float life, int pri ){
		FxParticle p = b.lit( x + (float)Math.cos( angle ) * len / 2f, y + (float)Math.sin( angle ) * len / 2f, pri );
		if (p == null) return null;
		p.look( FxFrames.SPARK_3 ).paint( b.c( color ) ).life( life ).alpha( b.a( 1f ), 0, life * 0.5f ).snap();
		p.angle = (float)Math.toDegrees( angle ) - 90f;
		p.scale.y = Math.max( 1f, Math.round( len / 3f ) );
		return p;
	}

	//a crescent across octant k, sweeping into it, along a ramp
	static FxParticle crescent( Beat b, float x, float y, int k, boolean wide, Ramp ramp, float alpha, int pri ){
		FxParticle p = b.lit( x, y, pri );
		if (p == null) return null;
		int[][] set = wide ? FxFrames.SLASH_24 : FxFrames.SLASH_16;
		return p.look( new int[][]{ set[(k + 7) % 8], set[k] }, 0 ).ramp( b.echo ? b.look.pale : ramp, true ).life( 0.12f )
				.alpha( b.a( alpha ), 0, 0.06f ).snap();
	}

	//an ink wisp at (x, y) drifting (vx, vy)
	static FxParticle wisp( Beat b, float x, float y, float vx, float vy, int color, float life, int pri ){
		FxParticle p = b.mat( x, y, pri );
		if (p == null) return null;
		return p.look( FxFrames.INK_WISP, 0 ).paint( b.c( color ) ).smooth().vel( vx, vy ).turbulence( 1f, 5f ).life( life )
				.alpha( b.a( 0.85f ), 0.05f, life * 0.5f );
	}

	//a mote of light at (x, y) drawn into (tx, ty) over `life` s
	static FxParticle inward( Beat b, float x, float y, float tx, float ty, int color, float life, int pri ){
		FxParticle p = b.lit( x, y, pri );
		if (p == null) return null;
		return p.look( FxFrames.MOTE_2 ).paint( b.c( color ) ).vel( (tx - x) / life, (ty - y) / life ).life( life )
				.alpha( b.a( 1f ), 0.05f, 0.1f ).snap();
	}

	//a glint spinning a quarter every 0.1 s
	static FxParticle glint( Beat b, float x, float y, float vx, float vy, int color, float life, int pri ){
		FxParticle p = b.lit( x, y, pri );
		if (p == null) return null;
		return p.look( FxFrames.STAR4_5 ).paint( b.c( color ) ).vel( vx, vy ).drag( 3f ).spin( 0.1f ).life( life )
				.alpha( b.a( 1f ), 0, life * 0.5f ).snap();
	}

	//a sway of a body: a pixel one way, then the other, then still (Vertigo's)
	static void sway( final CharSprite s, float from ){
		later( from, () -> s.shiver( 1 ) );
		later( from + 0.15f, () -> s.shiver( -1 ) );
		later( from + 0.3f, () -> s.shiver( 0 ) );
	}

	//the ward arc on a sprite's middle facing octant k (a glyph's "my armour did this", drawn by a
	//row whose beat runs the other way, from the wearer, and the hexed arc of a cursed one)
	static Flash arc( Beat b, float x, float y, int k, int color ){
		Flash f = b.flash( true, ARCS[k], 0f, x, y, color, ARC_ALPHA, ARC_HOLD + ARC_FADE );
		if (f != null) f.envelope( 0f, ARC_HOLD );
		return f;
	}

	/** The middle of a cell, world px. */
	static PointF center( int cell ){
		return DungeonTilemap.tileCenterToWorld( cell );
	}

	/** The look's flags for a vestige's echo (else none). */
	public static int echo( boolean echo ){
		return echo ? ECHO : 0;
	}

	// ------------------------------------------------------------------ DREAD

	/**
	 * Eldritch: each terrified witness (the first RICH) cries a red alarm over its head for 0.4 s
	 * and shivers, one after the other; the eye that opens over the defender is the family's, red by
	 * the row's eye colour (its rim stays violet).
	 */
	public static Twist eldritch( List<Char> terrified ){
		int n = terrified == null ? 0 : Math.min( RICH, terrified.size() );
		final CharSprite[] ws = new CharSprite[n];
		for (int i = 0; i < n; i++) ws[i] = terrified.get( i ).sprite;
		return b -> {
			if (!full( b )) return;
			for (int i = 0; i < ws.length; i++){
				final CharSprite w = ws[i];
				if (w == null || w.parent == null || !w.visible) continue;
				float at = 0.1f + 0.06f * i;
				Flash f = b.flash( false, false, EXCLAIM, 0f, midX( w ), w.y - 5f, EYE_RED, 0.95f, 0.4f );
				if (f != null) f.envelope( 0.03f, 0.25f ).delay( at );
				if (!b.echo) later( at, () -> b.moveNow( w, Beat.SHIVER, 1 ) );
			}
		};
	}

	/** Horror: a wider violet etch of dread under the feet for 1.2 s; a hero struck (Vertigo) sways. */
	public static final Twist HORROR = ShadowRows::horror;

	static void horror( Beat b ){
		if (!full( b )) return;
		if (!b.onWater( b.cx, b.feet )){
			FxDecal d = b.decal( b.cx, b.feet + 1f, FxFrames.ETCH, Element.SHADOW.ramp.stop( 2 ), 0.55f, 1.2f, 0.4f );
			if (d != null){
				d.origin.set( FxFrames.ETCH[2] / 2f, FxFrames.ETCH[3] / 2f );
				d.scale.x = 1.7f;
			}
		}
		if (!b.echo && b.ds != null && b.ds.ch != null && b.ds.ch == Dungeon.hero) sway( b.ds, 0.2f );
	}

	/** Jupiter's horror: Horror's, and two twigs of lilac static running down the body to the ground. */
	public static final Twist JUPITERS = ShadowRows::jupiters;

	static void jupiters( Beat b ){
		horror( b );
		if (!full( b )) return;
		final Beat twigs = b.split( 2 );
		later( 0.06f, () -> {
			if (twigs.ds != null && twigs.ds.parent == null) return;
			for (int i = 0; i < 2; i++){
				line( twigs, twigs.cx + (i == 0 ? -3f : 3f), twigs.cy + 1f, (float)(Math.PI / 2) + FxRandom.Float( -0.35f, 0.35f ),
						6f, LILAC, 0.08f, FxBudget.P1 );
			}
		} );
	}

	/** The corruption's tracker set on a foe (it turns if this blow kills it): two ink tendrils
	 *  wrapping round it for 0.6 s. Any thread. */
	public static void corruptMark( Char defender, final boolean echo ){
		if (defender == null || defender.sprite == null) return;
		final CharSprite s = defender.sprite;
		Fx.post( () -> {
			if (s.parent == null || !s.visible || Fx.glowAir() == null) return;
			EnchantLook l = EnchantLooks.of( Corrupting.class );
			final Beat b = Beat.at( l, midX( s ), midY( s ), 6 );
			if (echo) b.alphaMul = 0.5f;
			final float rx = s.width() / 2f + 2f, ry = 3f, a0 = FxRandom.angle();
			final int ink = echo ? EnchantLook.paled( l.rim ) : Element.SHADOW.ramp.stop( 1 );
			for (int k = 0; k < 3; k++){
				final float a = a0 + 1.1f * k;
				final Beat step = b.split( 2 );
				later( 0.15f * k, () -> {
					if (s.parent == null) return;
					for (int t = 0; t < 2; t++){
						float at = a + (float)Math.PI * t;
						float x = midX( s ) + (float)Math.cos( at ) * rx, y = midY( s ) + 1f + (float)Math.sin( at ) * ry;
						wisp( step, x, y, -(float)Math.sin( at ) * rx * 4f, (float)Math.cos( at ) * ry * 4f - 2f, ink, 0.3f, FxBudget.P1 );
					}
				} );
			}
		} );
	}

	/** A corrupted foe's body going (read here, before it is erased): the ink collapsing where it
	 *  stood, wisps rising off it; an echo's at half and pale. Any thread. */
	public static void corruptCollapse( CharSprite s, final boolean echo ){
		if (s == null || !s.visible) return;
		final float x = midX( s ), y = midY( s ), feet = s.y + s.height() - 2f, top = s.y + 1f;
		Fx.post( () -> {
			if (Fx.glowAir() == null) return;
			EnchantLook l = EnchantLooks.of( Corrupting.class );
			Beat b = Beat.at( l, x, y, echo ? ECHO_CAP : 7 );
			b.feet = feet;
			b.top = top;
			if (echo){
				b.scale = 0.5f;
				b.alphaMul = 0.5f;
			}
			b.flash( false, true, DreadFx.CLOSING, 0f, x, feet, echo ? EnchantLook.paled( l.rim ) : l.rim, 0.8f, 0.25f );
			DreadFx.finisher( b );
		} );
	}

	/** The corrupted ally risen in its place (after it is added to the scene): the dread pouring
	 *  into it at full strength (the eye opening violet), a violet bubble flashing over it. Any thread. */
	public static void corruptRise( final Char ally, final boolean echo ){
		if (ally == null) return;
		Fx.post( () -> {
			if (ally.sprite == null) return;
			EnchantFx.proc( EnchantLooks.of( Corrupting.class ), null, ally, 0.85f, 0, echo( echo ), RISE );
		} );
	}

	public static final Twist RISE = b -> {
		if (!full( b )) return;
		Flash f = b.flash( true, BUBBLE_31, 0f, b.cx, b.cy, b.look.rim, 0.5f, 0.35f );
		if (f != null) f.envelope( 0.05f, 0.12f );
		for (int i = 0; i < 3; i++){
			FxParticle p = b.lit( b.cx + (i - 1) * 4f, b.feet, FxBudget.P2 );
			if (p == null) break;
			p.look( FxFrames.MOTE_2 ).paint( b.c( b.look.rim ) ).vel( 0, -FxRandom.Float( 14f, 20f ) ).drag( 1f ).life( 0.6f )
					.alpha( b.a( 0.9f ), 0.05f, 0.3f );
		}
	};

	// ------------------------------------------------------------------ REAPER

	/** Grim's execute (Char.damage, after its shadow burst): always the killing look (the death
	 *  stroke, the skull, the soul), the camera shaken 1.5 for 0.15 s with the hero in it; an
	 *  echo's hollow. Any thread. */
	public static void grimExecute( Char attacker, Char defender, boolean echo ){
		if (defender == null) return;
		EnchantFx.proc( EnchantLooks.of( Grim.class ), attacker, defender, 1f, 0, echo( echo ), GRIM );
	}

	public static final Twist GRIM = b -> {
		//after the family's own shake (1.5 for 0.12 s): the execute's lasts 0.15 s
		if (b.hero && !b.echo && full( b ) && b.tier >= STRONG) later( 0f, () -> Fx.shake( 1.5f, 0.15f ) );
	};

	// ------------------------------------------------------------------ CUT

	/** Vorpal's blow turned to bleeding (Char.damage, after its blood cone): a long cut and a
	 *  trail of three drops along it, on the family's wound and splats. Any thread. */
	public static void vorpalBleed( Char attacker, Char defender, float bleed, boolean echo ){
		if (defender == null) return;
		EnchantFx.proc( EnchantLooks.of( Vorpal.class ), attacker, defender, EnchantFx.strength( 0, bleed, defender.HT, 1f ), 0,
				echo( echo ), VORPAL );
	}

	public static final Twist VORPAL = b -> {
		if (!full( b )) return;
		int k = HitFx.octant( b.dx, b.dy );
		if (b.tier < STRONG && !b.curse) crescent( b, b.cx, b.cy, k, true, blood( b ), 0.6f, FxBudget.P1 );
		final Beat trail = b.split( 3 );
		for (int i = 0; i < 3; i++){
			final float along = -4f + 4f * i;
			later( 0.04f * (i + 1), () -> {
				if (trail.ds != null && trail.ds.parent == null) return;
				FxParticle p = trail.mat( trail.cx + trail.dx * along, trail.cy + trail.dy * along - 2f, FxBudget.P2 );
				if (p != null){
					p.look( FxFrames.DROP_3 ).paint( bloodColor( trail ) ).orient().vel( trail.dx * 6f, 10f ).gravity( 200f )
							.ground( trail.feet + FxRandom.Float( 0f, 2f ), FxParticle.DIE, 0 ).life( 0.7f ).alpha( trail.a( 1f ), 0, 0.15f );
				}
			} );
		}
	};

	static int bloodColor( Beat b ){
		return b.ds != null ? b.c( b.ds.blood() & 0xFFFFFF ) : b.c( b.look.glow );
	}

	static Ramp blood( Beat b ){
		return new Ramp( 0xFFFFFF, bloodColor( b ) );
	}

	/** Hunting's bonus on prey, shown once it is at least half its most: a red crosshair snapping
	 *  on for two frames, then three claws raking across, 0.08 s apart. */
	public static final Twist HUNTING = b -> {
		if (!full( b )) return;
		int red = b.look.glow;
		Flash ring = b.flash( false, true, RING_9, 0f, b.cx, b.cy, red, 0.9f, 0.14f );
		if (ring != null) ring.envelope( 0f, 0.067f );
		Flash plus = b.flash( false, true, PLUS_5, 0f, b.cx, b.cy, red, 0.9f, 0.14f );
		if (plus != null) plus.envelope( 0f, 0.067f );
		final Beat claws = b.split( 3 );
		final float rake = (float)Math.toRadians( b.dx >= 0 ? 60 : 120 );
		for (int i = 0; i < 3; i++){
			final float off = (i - 1) * 3f;
			later( 0.08f * (i + 1), () -> {
				if (claws.ds != null && claws.ds.parent == null) return;
				FxParticle p = line( claws, claws.cx + off - (float)Math.cos( rake ) * 4f, claws.cy - (float)Math.sin( rake ) * 4f,
						rake, 9f, bloodColor( claws ), 0.1f, FxBudget.P1 );
				if (p != null) p.vel( (float)Math.cos( rake ) * 30f, (float)Math.sin( rake ) * 30f );
			} );
		}
		if (!b.onWater( b.cx, b.feet )) b.decal( b.cx, b.feet + 1f, FxFrames.SPLAT[1], bloodColor( b ), 0.6f, 2f, 2f );
	};

	/** Meat cut from a hunted kill: an amber ring on its heap. Any thread. */
	public static void huntMeat( final int cell ){
		if (cell < 0) return;
		Fx.post( () -> {
			if (Dungeon.level == null || cell >= Dungeon.level.length() || Fx.glowAir() == null) return;
			PointF c = center( cell );
			if (!Fx.seen( c.x, c.y )) return;
			Beat b = Beat.at( EnchantLooks.of( Hunting.class ), c.x, c.y, 2 );
			b.ring( c.x, c.y + 3f, 9, 17, 0.5f, AMBER, 0.8f, true );
			FxParticle p = b.lit( c.x, c.y - 3f, FxBudget.P2 );
			if (p != null) p.look( FxFrames.STAR4_5 ).paint( AMBER ).life( 0.3f ).alpha( 0.9f, 0.03f, 0.15f ).snap();
		} );
	}

	/** Vicious's bonus (at least 5% of the foe's health): the cut's serrated edge, three teeth
	 *  of light along the crescent. */
	public static final Twist VICIOUS = b -> {
		if (!full( b )) return;
		float px = -b.dy, py = b.dx;
		for (int i = 0; i < 3; i++){
			float o = (i - 1) * 4f;
			FxParticle p = b.lit( b.cx + px * o, b.cy + py * o, FxBudget.P2 );
			if (p == null) break;
			p.look( FxFrames.SPARK_2 ).paint( b.c( 0xFF6060 ) ).vel( b.dx * 12f, b.dy * 12f ).life( 0.12f )
					.alpha( b.a( 1f ), 0.02f * i, 0.06f ).snap();
		}
	};

	/** Vicious's spikes biting the wielder: his blood cut and drawn back into the blade (a reverse
	 *  lifeline, the weapon feeding on him). Any thread. */
	public static void viciousBackfire( Char wielder, int level, int flags ){
		if (wielder == null) return;
		EnchantFx.proc( EnchantLooks.of( Vicious.class ), null, wielder, 0.45f, level, flags, BACKFIRE );
	}

	public static final Twist BACKFIRE = b -> {
		if (!full( b ) || b.ds == null) return;
		DrainFx.lifeline( b, b.cx, b.cy + 2f, sideX( b.ds ), sideY( b.ds ), 3, bloodColor( b ), b.look.glow );
	};

	/** Slashing's reroll that bettered the blow: a second crescent in lime steel crossing the
	 *  blow's own 0.04 s later (an X). */
	public static final Twist SLASHING = b -> {
		if (!full( b )) return;
		final Beat x = b.split( 1 );
		final int k = (HitFx.octant( b.dx, b.dy ) + 2) % 8;
		later( 0.04f, () -> {
			if (x.ds != null && x.ds.parent == null) return;
			crescent( x, x.cx, x.cy, k, false, new Ramp( 0xFFFFFF, 0xE8F0FF, x.look.glow ), 0.9f, FxBudget.P1 );
		} );
	};

	/** How long after the last each of Buzz saw's extra cuts lands, s. */
	public static final float CUT_GAP = 0.06f;

	/** One of Buzz saw's extra cuts (the i-th): its crescent an octant further round each time
	 *  (the saw turning), metal sparks, the body shivering; each cut CUT_GAP s after the one
	 *  before, so the blade is seen going round (the game makes them all at once). */
	public static Twist buzz( final int i ){
		return b -> {
			if (b.minimal) return;
			final Beat cut = b.split( i > 0 ? 3 : 2 );
			later( CUT_GAP * i, () -> {
				if (cut.ds != null && cut.ds.parent == null) return;
				int k = (HitFx.octant( cut.dx, cut.dy ) + i) % 8;
				if (i > 0) crescent( cut, cut.cx, cut.cy, k, false, new Ramp( 0xFFFFFF, Element.STEEL.ramp.stop( 2 ), cut.look.glow ),
						0.9f, FxBudget.P1 );
				for (int s = 0; s < 2; s++){
					FxParticle p = cut.lit( cut.hx, cut.hy, FxBudget.P2 );
					if (p == null) break;
					p.look( FxFrames.SPARK_2 ).orient().ramp( cut.echo ? cut.look.pale : Element.METAL.ember, true )
							.polar( FxRandom.Float( (float)-Math.PI, 0f ), FxRandom.Float( 40f, 70f ) ).gravity( 140f ).life( 0.3f )
							.alpha( cut.a( 1f ), 0, 0.1f );
				}
				if (i > 0 && !cut.echo && cut.ds != null) cut.moveNow( cut.ds, Beat.SHIVER, 1 );
			} );
		};
	}

	/** Thorns: the ward arc on the wearer toward the attacker and four thorns shot from him into
	 *  it in 0.1 s; the family's wound and the attacker's blood are its cut. Its beat runs from the
	 *  wearer (as) to the attacker (ds). */
	public static final Twist THORNS = b -> {
		if (!full( b ) || b.as == null) return;
		arc( b, b.sx, b.sy, HitFx.octant( b.dx, b.dy ), b.look.light );
		float px = -b.dy, py = b.dx, gx = b.cx - b.sx, gy = b.cy - b.sy;
		for (int i = 0; i < 4; i++){
			float o = (i - 1.5f) * 2.5f;
			FxParticle p = b.mat( b.sx + px * o, b.sy + py * o, FxBudget.P1 );
			if (p == null) break;
			p.look( FxFrames.BLADE_4 ).paint( b.c( i % 2 == 0 ? b.look.glow : Ramp.lerp( b.look.glow, b.look.rim, 0.5f ) ) ).orient()
					.vel( gx / 0.1f, gy / 0.1f ).life( 0.11f ).alpha( b.a( 1f ), 0, 0.03f );
		}
	};

	// ------------------------------------------------------------------ DRAIN

	/** Vampiric: the blood's wound on the foe; from STRONG a red mist and three beads more (ten in
	 *  all), coming after the family's seven. */
	public static final Twist VAMPIRIC = b -> {
		if (!full( b )) return;
		if (!b.echo && b.ds != null && b.ds.ch != null && b.ds.parent != null && b.takeAll( 1 )){
			Wound.hit( b.ds.ch, (float)Math.toDegrees( Math.atan2( b.dy, b.dx ) ) );
		}
		if (b.tier < STRONG || b.as == null || b.as == b.ds) return;
		FxParticle mist = b.mat( b.hx, b.hy, FxBudget.P2 );
		if (mist != null){
			mist.look( FxFrames.PUFF_M, 0 ).paint( bloodColor( b ) ).smooth().vel( b.dx * 4f, -3f ).drag( 1f ).life( 0.6f )
					.alpha( b.a( 0.4f ), 0.05f, 0.4f );
		}
		final Beat more = b.split( DrainFx.MORE );
		if (more.left() > 0) b.home( DrainFx.MORE_AT + DrainFx.landing( Math.min( DrainFx.MORE, more.left() ) ) );
		later( DrainFx.MORE_AT, () -> {
			if (more.ds != null && more.ds.parent == null) return;
			DrainFx.lifeline( more, more.hx, more.hy, more.sx, more.sy, more.wx, more.wy - DrainFx.RISE, DrainFx.MORE, bloodColor( more ),
					DrainFx.LIFE );
		} );
	};

	/** Ares' leech: one lifeline from each drained foe to the wielder, spokes of a wheel (the first
	 *  RICH in full, the rest a star and two motes), and round the wielder a ring of blood the
	 *  drain's width closing once. Any thread. */
	public static void aresLeech( EnchantLook l, Char wielder, List<Char> drained, float s, int level ){
		if (wielder == null || drained == null) return;
		for (int i = 0; i < drained.size(); i++){
			EnchantFx.proc( l, wielder, drained.get( i ), s, level, EnchantFx.multi( i ), i == 0 ? ARES_RING : null );
		}
	}

	public static final Twist ARES_RING = b -> {
		if (!full( b ) || b.as == null || b.echo) return;
		float feet = b.as.y + b.as.height() - 2f;
		b.floorFlash( CLOSING_41, b.sx, feet, Element.BLOOD.ramp.stop( 1 ), 0.7f, 0.45f );
	};

	/** Osmose: the defender's mana drawn as blue beads through the hero's weapon side into him,
	 *  landing in a blue ring (the call gives the family no wielder, so no blood line of its own). */
	public static Twist osmose( Char hero ){
		final CharSprite hs = hero != null ? hero.sprite : null;
		return b -> {
			if (!full( b ) || hs == null || hs.parent == null) return;
			int n = DrainFx.BEADS[b.tier];
			final Beat land = b.split( 1 );
			int made = DrainFx.lifeline( b, b.hx, b.hy, midX( hs ), midY( hs ), sideX( hs ), sideY( hs ) - DrainFx.RISE, n,
					Element.SACRIFICIAL.light, Element.SACRIFICIAL.ramp.stop( 1 ) );
			if (made <= 0) return;
			b.home( DrainFx.landing( made ) );
			later( DrainFx.landing( made ), () -> {
				if (hs.parent == null) return;
				Flash r = land.flash( false, true, RING_9, 0f, midX( hs ), midY( hs ) + 3f, Element.SACRIFICIAL.light, 0.8f, 0.3f );
				if (r != null) r.envelope( 0f, 0.1f );
			} );
		};
	}

	/** Sacrificial (its beat on the wielder): his own blood drawn up out of him into the blade, and
	 *  a blue lick of sacrifice off its edge. */
	public static final Twist SACRIFICIAL = b -> {
		if (!full( b ) || b.ds == null) return;
		float wx = sideX( b.ds ), wy = sideY( b.ds );
		DrainFx.lifeline( b, b.cx, b.cy + 2f, wx, wy, 3, bloodColor( b ), Element.SACRIFICIAL.light );
		final Beat lick = b.split( 1 );
		later( DrainFx.LINE_FOR, () -> {
			if (lick.ds != null && lick.ds.parent == null) return;
			FxParticle p = lick.lit( wx, wy - 2f, FxBudget.P1 );
			if (p != null){
				p.look( FxFrames.TONGUE_S, 0 ).ramp( lick.echo ? lick.look.pale : Element.SACRIFICIAL.ramp, true ).vel( 0, -6f )
						.life( 0.3f ).alpha( lick.a( 1f ), 0.03f, 0.15f );
			}
		} );
	};

	/** Metabolism (its beat on the wearer): green motes of healing drawn into him, an amber mote of
	 *  hunger leaving his belly. */
	public static final Twist METABOLISM = b -> {
		if (!full( b )) return;
		for (int i = 0; i < 3; i++){
			float a = (float)(2 * Math.PI * i / 3) + FxRandom.Float( -0.4f, 0.4f );
			if (inward( b, b.cx + (float)Math.cos( a ) * 9f, b.cy + (float)Math.sin( a ) * 6f, b.cx, b.cy, Element.HEAL.light, 0.35f,
					FxBudget.P1 ) == null) break;
		}
		FxParticle p = b.lit( b.cx, b.cy + 2f, FxBudget.P2 );
		if (p != null){
			p.look( FxFrames.MOTE_2 ).paint( b.c( AMBER ) ).vel( b.ds != null && b.ds.flipHorizontal ? 6f : -6f, -10f ).drag( 0.5f )
					.life( 0.7f ).alpha( b.a( 0.9f ), 0.2f, 0.4f ).snap();
		}
	};

	// ------------------------------------------------------------------ FORTUNE

	/** Lucky's mark: a green star winking where the mark settles over the head, once the family's
	 *  gold one has flashed, and a green clover rising off the foe (its star on the mark's orbit
	 *  comes with the mark itself, luckMark). */
	public static final Twist LUCKY = b -> {
		if (!full( b )) return;
		Flash wink = b.flash( true, STAR_WINK, 0f, b.cx, b.cy + FxOrbit.Slot.MARK.h, b.look.glow, 0.9f, 0.2f );
		if (wink != null) wink.envelope( 0.02f, 0.08f ).delay( 0.1f );
		FxParticle p = b.lit( b.cx, b.top - 1f, FxBudget.P1 );
		if (p != null){
			p.look( FxFrames.CLOVER_5 ).paint( b.c( b.look.glow ) ).vel( 0, -10f ).drag( 1.5f ).life( 0.55f )
					.alpha( b.a( 1f ), 0.04f, 0.25f ).snap();
		}
	};

	//the stars of luck on the mark's orbit this row put there (render thread); the ring held weakly,
	//as it holds its sprite (a sprite thrown away with its mark on is collected all the same)
	private static final WeakHashMap<CharSprite, WeakReference<FxOrbit.Ring>> LUCK = new WeakHashMap<>();

	/** Lucky's mark on a foe (Lucky.LuckProc's own fx): a green star circling over its head while
	 *  it lasts, on the mark's orbit when nothing else holds it. Any thread. */
	public static void luckMark( Char target, final boolean on ){
		if (target == null || target.sprite == null) return;
		final CharSprite s = target.sprite;
		Fx.post( () -> {
			if (on){
				if (s.parent == null || FxOrbit.get( s, FxOrbit.Slot.MARK ) != null) return;
				EnchantLook l = EnchantLooks.of( Lucky.class );
				FxOrbit.Ring r = FxOrbit.attach( s, FxOrbit.Slot.MARK ).motes( 1, FxFrames.STAR4_5, l.glow, true ).speed( 1.6f )
						.breath( 0.7f, 1f, 1.5f ).flare( 0.6f, 0.3f, 0 );
				LUCK.put( s, new WeakReference<>( r ) );
			} else {
				WeakReference<FxOrbit.Ring> ref = LUCK.remove( s );
				FxOrbit.Ring r = ref != null ? ref.get() : null;
				if (r != null) r.detach();
			}
		} );
	}

	/** A luck mark lost to a failed roll: its star fizzling into two grey motes. Any thread. */
	public static void luckFizzle( Char target ){
		if (target == null || target.sprite == null) return;
		final CharSprite s = target.sprite;
		Fx.post( () -> {
			if (s.parent == null || !s.visible || Fx.glowAir() == null) return;
			float x = midX( s ), y = midY( s ) + FxOrbit.Slot.MARK.h;
			Beat b = Beat.at( EnchantLooks.of( Lucky.class ), x, y, 2 );
			for (int i = 0; i < 2; i++){
				FxParticle p = b.mat( x + (i == 0 ? -2f : 2f), y, FxBudget.P2 );
				if (p == null) break;
				p.look( FxFrames.MOTE_1 ).paint( 0x8A8A8A ).vel( i == 0 ? -4f : 4f, 2f ).gravity( 30f ).life( 0.5f )
						.alpha( 0.9f, 0, 0.3f ).snap();
			}
		} );
	}

	/** Lucky's loot from a marked foe (beside the ring of wealth's flare; vis is its sprite): the
	 *  look at full strength on it, the killing look on a dead one, a fountain of glints and a pool
	 *  of gold either way; an enchantment entry, so co-op guests see it and it stings. Any thread. */
	public static void luckyDrop( Visual vis ){
		if (!(vis instanceof CharSprite) || ((CharSprite) vis).ch == null) return;
		EnchantFx.proc( EnchantLooks.of( Lucky.class ), null, ((CharSprite) vis).ch, 1f, 0, 0, MIDAS_KILL );
	}

	/** Unstable's dice flicked at the weapon side before its child goes off: three prism glints
	 *  spinning there (the child's own look then plays, second on the blow). */
	public static final Twist DICE = b -> {
		if (!full( b ) || b.as == null) return;
		for (int i = 0; i < 3; i++){
			int c = Element.PRISM.ramp.stop( (i * 2 + FxRandom.Int( 2 )) % Element.PRISM.ramp.size() );
			if (glint( b, b.wx + (i - 1) * 3f, b.wy - 2f, (i - 1) * 8f, -18f, c, 0.3f, FxBudget.P1 ) == null) break;
		}
	};

	/** Midas's killing payout: the family's fountain of coins and gold pool (once the proc itself
	 *  is drawn before the blow kills; a FINISHER draws its own). Lucky's loot's too. */
	public static Twist midas( boolean kill ){
		return kill ? MIDAS_KILL : null;
	}

	public static final Twist MIDAS_KILL = b -> {
		if (!full( b ) || b.tier == FINISHER) return;
		FortuneFx.finisher( b.split( 6 ) );
	};

	/** Crom's luck: the die landing, three gold stars tumbling in an arc from the weapon side onto
	 *  the foe, and two sparks off the axe's edge. */
	public static final Twist CROM = b -> {
		if (!full( b ) || b.as == null) return;
		float gx = b.cx - b.wx;
		for (int i = 0; i < 3; i++){
			float t = 0.3f + 0.04f * i;
			FxParticle p = b.lit( b.wx, b.wy - 3f, FxBudget.P1 );
			if (p == null) break;
			p.look( FxFrames.STAR4_5 ).ramp( b.echo ? b.look.pale : Element.GOLD.ember, true ).vel( gx / t + (i - 1) * 4f, -40f )
					.gravity( 2f * (40f * t + (b.feet - 2f - b.wy)) / (t * t) ).spin( 0.08f ).life( t ).alpha( b.a( 1f ), 0, 0.08f );
		}
		for (int i = 0; i < 2; i++){
			FxParticle s = b.lit( b.wx, b.wy, FxBudget.P2 );
			if (s == null) break;
			s.look( FxFrames.SPARK_2 ).orient().ramp( b.echo ? b.look.pale : Element.METAL.ember, true )
					.polar( (float)(-Math.PI / 2) + FxRandom.Float( -0.9f, 0.9f ), FxRandom.Float( 30f, 50f ) ).gravity( 120f )
					.life( 0.3f ).alpha( b.a( 1f ), 0, 0.1f );
		}
	};

	/** Wondrous (hexed): a dice flick of prism glints at the weapon side, inked. */
	public static final Twist WONDROUS = b -> {
		if (!full( b ) || b.as == null) return;
		for (int i = 0; i < 3; i++){
			int c = Element.PRISM.ramp.stop( (i * 2 + 1) % Element.PRISM.ramp.size() );
			if (glint( b, b.wx + (i - 1) * 3f, b.wy - 2f, (i - 1) * 7f, -14f, c, 0.3f, FxBudget.P1 ) == null) break;
		}
		for (int i = 0; i < 2; i++){
			if (wisp( b, b.wx + (i == 0 ? -2f : 2f), b.wy, 0, -6f, Element.VOID.ramp.stop( 3 ), 0.6f, FxBudget.P2 ) == null) break;
		}
	};

	// ------------------------------------------------------------------ MIRAGE

	/** Afterimage on its wearer's dodge (Char.attack's miss): a ghost of him three pixels toward the
	 *  attacker in its pale blue, so the blow went through an afterimage. Any thread. */
	public static void afterimageDodge( Char wearer, Char attacker ){
		if (wearer == null || wearer.sprite == null) return;
		EnchantFx.passive( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Afterimage.class ), wearer,
				EnchantFx.DODGE, 0.2f, dodge( attacker != null ? attacker.sprite : null ) );
	}

	/** The dodge's ghost, toward the attacker's sprite (as, null for none: the way he faces). */
	public static Twist dodge( final CharSprite as ){
		return b -> {
			if (b.ds == null) return;
			float ox = b.ds.flipHorizontal ? -3f : 3f, oy = 0f;
			if (as != null && as.parent != null){
				float vx = midX( as ) - b.cx, vy = midY( as ) - b.cy, len = (float)Math.sqrt( vx * vx + vy * vy );
				if (len > 0.5f){
					ox = vx / len * 3f;
					oy = vy / len * 3f;
				}
			}
			b.after( b.ds, AFTERIMAGE, 0.5f, 0.35f, ox, oy );
		};
	}

	public static final int AFTERIMAGE = 0xA0A8C8;

	//each walker's last answered step (Game.timeTotal) and how many he has had: a step is
	//answered at most every STEP_GAP s (his speed is asked for more often than he steps, and each
	//answer goes to co-op guests too), and every second one leaves a ghost. Any thread.
	private static final WeakHashMap<Char, float[]> STEPS = new WeakHashMap<>();

	//-1: not now; else 1 for a step that leaves a ghost, 0 for one that does not
	static int step( Char walker ){
		float now = Game.timeTotal;
		synchronized (STEPS){
			float[] st = STEPS.get( walker );
			if (st == null) STEPS.put( walker, st = new float[]{ -1f, 0f } );
			if (st[0] >= 0 && now - st[0] < STEP_GAP && now >= st[0]) return -1;
			st[0] = now;
			st[1] = (st[1] + 1) % 2;
			return st[1] == 0 ? 1 : 0;
		}
	}

	/** Flow's step through water (its speed line, while seen; one in STEP_GAP s on a walker): a
	 *  wake stirred round the feet, a blue streak left behind them and, every second step, a blue
	 *  ghost of him. Any thread. */
	public static void flowStep( Char owner ){
		if (owner == null || owner.sprite == null) return;
		int k = step( owner );
		if (k < 0) return;
		EnchantFx.passive( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Flow.class ), owner,
				EnchantFx.STEP, 0.2f, k == 1 ? FLOW_GHOST : FLOW );
	}

	public static final Twist FLOW = b -> flow( b, false ), FLOW_GHOST = b -> flow( b, true );

	//a quiet moment's three quads: the family's two motes, then the ghost on a ghost's step (it comes
	//first), else the streak
	static void flow( Beat b, boolean ghost ){
		if (b.ds == null) return;
		float back = b.ds.flipHorizontal ? 1f : -1f;
		WaterFX.disturb( b.cx, b.feet, 10f, false );
		if (ghost) b.after( b.ds, b.look.glow, 0.3f, 0.35f, back * 3f, 0 );
		streak( b, back, b.look.glow, 0.5f );
	}

	/** Swiftness's step while no foe is near (its speed line, while seen; one in STEP_GAP s on a
	 *  walker): a mint streak left behind the feet and, every second step, a faint ghost. Any thread. */
	public static void swiftStep( Char owner ){
		if (owner == null || owner.sprite == null) return;
		int k = step( owner );
		if (k < 0) return;
		EnchantFx.passive( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Swiftness.class ), owner,
				EnchantFx.STEP, 0.2f, k == 1 ? SWIFT_GHOST : SWIFT );
	}

	public static final Twist SWIFT = b -> swift( b, false ), SWIFT_GHOST = b -> swift( b, true );

	static void swift( Beat b, boolean ghost ){
		if (b.ds == null) return;
		float back = b.ds.flipHorizontal ? 1f : -1f;
		if (ghost) b.after( b.ds, b.look.glow, 0.2f, 0.3f, back * 3f, 0 );
		streak( b, back, b.look.glow, 0.55f );
	}

	//a streak left behind the feet, the way he came
	static void streak( Beat b, float back, int color, float alpha ){
		FxParticle p = b.lit( b.cx + back * 6f, b.feet - 1f, FxBudget.P2 );
		if (p == null) return;
		p.look( FxFrames.STREAK_TAPER ).paint( b.c( color ) ).vel( back * 12f, 0 ).life( 0.25f ).alpha( b.a( alpha ), 0, 0.15f );
		p.angle = back > 0 ? 0f : 180f;
	}

	/** The mirror images on the level now (a set to tell the ones a glyph is about to make). */
	public static Set<Mob> images(){
		Set<Mob> out = Collections.newSetFromMap( new IdentityHashMap<>() );
		if (Dungeon.level == null || Dungeon.level.mobs == null) return out;
		for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )) if (m instanceof MirrorImage) out.add( m );
		return out;
	}

	//the images made since `had` was taken
	static List<CharSprite> made( Set<Mob> had ){
		List<CharSprite> out = new ArrayList<>();
		for (Mob m : images()) if (had == null || !had.contains( m )) if (m.sprite != null) out.add( m.sprite );
		return out;
	}

	/** Mirror image's images made (they were not in `had`): see the list's. Any thread. */
	public static void mirrorimage( Char wearer, Char attacker, int level, int flags, Set<Mob> had ){
		mirrorimage( wearer, attacker, level, flags, made( had ) );
	}

	/** Mirror image's images (their sprites): the ward (a vestige's echo with flags ECHO), a ring of
	 *  six mirror glints round the wearer and each image (the first RICH) stepping out of him, a
	 *  ghost of him sliding to its place. Nothing with no image. Any thread. */
	public static void mirrorimage( Char wearer, Char attacker, int level, int flags, final List<CharSprite> images ){
		if (wearer == null || images == null || images.isEmpty()) return;
		EnchantFx.ward( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Mirrorimage.class ), wearer, attacker,
				0.7f, level, flags, b -> {
					if (!full( b ) || b.ds == null) return;
					for (int i = 0; i < Math.min( RICH, images.size() ); i++) slide( b, images.get( i ), false );
					for (int i = 0; i < 6; i++){
						float a = (float)(2 * Math.PI * i / 6);
						FxParticle p = b.lit( b.cx + (float)Math.cos( a ) * 9f, b.cy + (float)Math.sin( a ) * 5f, FxBudget.P2 );
						if (p == null) break;
						p.look( FxFrames.STAR4_5 ).paint( b.c( b.look.glow ) ).life( 0.35f ).alpha( b.a( 1f ), 0.03f * i, 0.15f ).snap();
					}
				} );
	}

	/** Mirroring's images made (they were not in `had`): see the list's. Any thread. */
	public static void mirroring( Char wearer, Char attacker, int level, int flags, Set<Mob> had ){
		mirroring( wearer, attacker, level, flags, made( had ) );
	}

	/** Mirroring's images (their sprites): the ward (a vestige's echo with flags ECHO), a mirror pane
	 *  flipping across the wearer, then a flipped ghost of him flashing on each image's place (the
	 *  first RICH). Nothing with no image. Any thread. */
	public static void mirroring( Char wearer, Char attacker, int level, int flags, final List<CharSprite> images ){
		if (wearer == null || images == null || images.isEmpty()) return;
		EnchantFx.ward( EnchantLooks.of( xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Mirroring.class ), wearer, attacker,
				0.7f, level, flags, b -> {
					if (!full( b ) || b.ds == null) return;
					Flash pane = b.flash( true, SHAFT_24, 0f, b.cx - 4f, b.cy - 2f, b.look.glow, 0.8f, 0.1f );
					if (pane != null){
						pane.scale.x = 0.3f;
						pane.vel( 80f, 0 );
					}
					for (int i = 0; i < Math.min( RICH, images.size() ); i++) slide( b, images.get( i ), true );
				} );
	}

	//a ghost of the wearer stepping out to an image's place: halfway at once, there 0.07 s on
	//(flipped for a reflection)
	static void slide( Beat b, final CharSprite image, final boolean flipped ){
		if (image == null || image.parent == null) return;
		final float ox = image.x - b.ds.x, oy = image.y - b.ds.y;
		final int color = b.look.glow;
		if (!flipped){
			Afterimage a = b.after( b.ds, color, 0.35f, 0.2f, ox * 0.5f, oy * 0.5f );
			if (a == null) return;
		}
		final Beat there = b.split( 1 );
		later( flipped ? 0.1f : 0.07f, () -> {
			if (there.ds == null || there.ds.parent == null) return;
			Afterimage a = there.after( there.ds, color, 0.45f, 0.3f, ox, oy );
			if (a != null && flipped) a.flipHorizontal = !a.flipHorizontal;
		} );
	}

	/** Phasing: a blue ghost of the wearer left on the cell he phased out of. */
	public static Twist phase( final int from ){
		return b -> {
			if (!full( b ) || b.ds == null || from < 0 || Dungeon.level == null || from >= Dungeon.level.length()) return;
			PointF p = b.ds.worldToCamera( from );
			b.after( b.ds, b.look.glow, 0.5f, 0.5f, p.x - b.ds.x, p.y - b.ds.y );
			PointF c = center( from );
			MirageFx.fold( b, c.x, c.y, 0.7f, 0.3f );
		};
	}

	/** Displacing (hexed): ink imploding on the cell the foe was thrown from, ink wisps where it
	 *  lands. */
	public static Twist displaced( final int from ){
		return b -> {
			if (!full( b ) || from < 0 || Dungeon.level == null || from >= Dungeon.level.length()) return;
			PointF c = center( from );
			implode( b, c.x, c.y );
			wisp( b, b.cx - 2f, b.cy, 0, -6f, Element.VOID.ramp.stop( 3 ), 0.7f, FxBudget.P2 );
			wisp( b, b.cx + 2f, b.cy, 0, -6f, Element.VOID.ramp.stop( 3 ), 0.7f, FxBudget.P2 );
		};
	}

	//ink imploding on (x, y): a ring closing on it, a spot of ink, a wisp rising
	static void implode( Beat b, float x, float y ){
		b.flash( false, true, HexOverlay.CLOSING, 0f, x, y + 5f, Element.VOID.ramp.stop( 1 ), 0.75f, HexOverlay.CLOSE_FOR );
		FxParticle ink = b.mat( x, y + 5f, FxBudget.P1 );
		if (ink != null) ink.look( FxFrames.INK_CORE ).paint( b.c( Element.VOID.ramp.stop( 4 ) ) ).life( 0.6f ).alpha( b.a( 0.8f ), 0.1f, 0.3f ).snap();
		wisp( b, x, y + 2f, 0, -8f, Element.VOID.ramp.stop( 3 ), 0.8f, FxBudget.P2 );
	}

	/** Displacement (an armour's hex): the blow passing through a grey arc where the wearer stood,
	 *  ink wisps there and where he lands. */
	public static Twist displacement( final int from ){
		return b -> {
			if (!full( b ) || from < 0 || Dungeon.level == null || from >= Dungeon.level.length()) return;
			PointF c = center( from );
			int k = b.as != null ? HitFx.octant( midX( b.as ) - c.x, midY( b.as ) - c.y ) : (b.ds != null && b.ds.flipHorizontal ? 4 : 0);
			arc( b, c.x, c.y, k, Element.SMOKE.light );
			wisp( b, c.x, c.y + 2f, 0, -7f, Element.VOID.ramp.stop( 3 ), 0.8f, FxBudget.P2 );
			wisp( b, b.cx, b.cy + 2f, 0, -7f, Element.VOID.ramp.stop( 3 ), 0.8f, FxBudget.P2 );
		};
	}

	/** Multiplicity (an armour's hex): a dark mirror of the void flashing on the cell the copy
	 *  comes out of. */
	public static Twist multiplied( final int cell ){
		return b -> {
			if (!full( b ) || cell < 0 || Dungeon.level == null || cell >= Dungeon.level.length()) return;
			PointF c = center( cell );
			Flash f = b.flash( true, false, SHAFT_24, 0f, c.x, c.y - 4f, Element.VOID.ramp.stop( 3 ), 0.85f, 0.35f );
			if (f != null) f.envelope( 0.03f, 0.12f );
		};
	}

	/** Wayward (hexed): a grey swirl at the weapon side, the swing's aim twisting away. */
	public static final Twist WAYWARD = b -> {
		if (!full( b ) || b.as == null) return;
		float a0 = FxRandom.angle();
		for (int i = 0; i < 4; i++){
			float a = a0 + (float)(Math.PI / 2) * i;
			FxParticle p = b.mat( b.wx + (float)Math.cos( a ) * 4f, b.wy + (float)Math.sin( a ) * 3f, FxBudget.P2 );
			if (p == null) break;
			p.look( FxFrames.MOTE_2 ).paint( b.c( Element.SMOKE.light ) ).vel( -(float)Math.sin( a ) * 16f, (float)Math.cos( a ) * 12f )
					.drag( 1.2f ).life( 0.45f ).alpha( b.a( 0.8f ), 0.03f, 0.25f ).snap();
		}
	};

	// ------------------------------------------------------------------ HEART

	/** Nomnom: two jaws (crescents over and under the body) snapping shut on it in 0.1 s, six pink
	 *  motes drawn out of the eaten foe into the eater, the body sinking a pixel. */
	public static final Twist NOMNOM = b -> {
		if (!full( b ) || b.as == null || b.as == b.ds) return;
		Ramp bite = new Ramp( 0xFFFFFF, Element.LIFE.light, b.look.glow );
		for (int j = 0; j < 2; j++){
			float side = j == 0 ? -1f : 1f;
			FxParticle jaw = crescent( b, b.cx, b.cy + side * 5f, j == 0 ? 6 : 2, false, bite, 0.95f, FxBudget.P1 );
			if (jaw != null) jaw.vel( 0, -side * 40f ).life( 0.1f );
		}
		for (int i = 0; i < 6; i++){
			float a = (float)(2 * Math.PI * i / 6);
			if (inward( b, b.cx + (float)Math.cos( a ) * 4f, b.cy + (float)Math.sin( a ) * 3f, b.sx, b.sy, Element.LIFE.light,
					0.3f + 0.03f * i, FxBudget.P2 ) == null) break;
		}
		if (!b.echo && b.ds != null) b.moveNow( b.ds, Beat.SINK, 1 );
	};

	/** Friendly (hexed): pink motes curling into both the wielder and the foe. */
	public static final Twist FRIENDLY = b -> {
		if (!full( b )) return;
		curl( b, b.cx, b.cy );
		if (b.as != null && b.as != b.ds) curl( b, b.sx, b.sy );
	};

	//three pink motes curling into (x, y)
	static void curl( Beat b, float x, float y ){
		float a0 = FxRandom.angle();
		for (int i = 0; i < 3; i++){
			float a = a0 + (float)(2 * Math.PI * i / 3), d = 8f;
			float ox = (float)Math.cos( a ) * d, oy = (float)Math.sin( a ) * d * 0.6f;
			FxParticle p = b.lit( x + ox, y + oy, FxBudget.P2 );
			if (p == null) return;
			p.look( FxFrames.MOTE_2 ).paint( b.c( Element.LIFE.light ) ).vel( -ox / 0.4f - oy * 2f, -oy / 0.4f + ox * 1.2f ).life( 0.4f )
					.alpha( b.a( 0.9f ), 0.05f, 0.1f );
		}
	}

	// ------------------------------------------------------------------ the hex's own (Annoying)

	/** Annoying (hexed): three faint sound waves widening off the weapon side, 0.1 s apart. */
	public static final Twist ANNOYING = b -> {
		if (!full( b ) || b.as == null) return;
		for (int i = 0; i < 3; i++){
			Flash f = b.flash( false, true, WIDENING, 0f, b.wx, b.wy, 0xFFFFFF, 0.35f, 0.3f );
			if (f == null) break;
			f.delay( 0.1f * i );
		}
	};
}
