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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.HitFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.Beat;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.Flash;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.Twist;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLook;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLooks;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.ForceFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.HexOverlay;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.MotifFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.StormFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.VenomFx;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Bulk;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.AntiMagic;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Brimstone;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Viscosity;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Polarized;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Ancient;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Crystal;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Kinetic;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Parasitic;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

import java.util.List;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.MID;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.STRONG;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.WEAK;

/**
 * The rows of the elements and nature (docs/fx/enchant.md, "Elements and nature"): what each
 * enchantment, glyph and curse of the EMBER, RIME, STORM, VENOM, VERDANT, FORCE, WARD and RADIANCE
 * families adds to its family's look where it goes off.
 *
 * Two kinds of entries. A Twist (blazing(...), chilling(...)) rides a proc's or a ward's beat
 * (EnchantFx.proc / ward / passive with it): drawn on the render thread after the family, from the
 * share of the tier's quads it keeps from the family's draw (EnchantFx.TWIST_KEEPS) and what the
 * family left, in the order the row's look puts first, so it never passes the cap; an echo's pale
 * and hollow (no extra marks), a joined or a minimal one's nothing but its lights. The rest
 * (crystalHit, kineticStore, seedTick, tarTick, polarFizzle, killRune, attuned, brimstone, bulk,
 * antiMagic, shocking, neptune, lokis) are entries of their own: any thread
 * (they post themselves), and a moment of no proc draws from a tally of its own (Beat.at), at most
 * CAP[WEAK] but the self-upgrade's FINISHER-sized circle.
 *
 * FxRandom only: nothing here draws, adds or moves a call of the game's Random; nothing writes the
 * game's state. A row's marks past the beat's one decal (Chilling's crawl and its second rime,
 * Venomous's second etch, AntiEntropy's and Corrosion's marks on the cells round the wearer) are the
 * rows' own, at most EXTRA_DECALS a proc, never an echo's.
 */
public final class ElementRows {

	private ElementRows(){}

	/** A row's marks on the floor past its beat's one decal, at most. */
	public static final int EXTRA_DECALS = 3;
	/** How often Brimstone may answer a fire it refuses on one character, s; how often Bulk's
	 *  doorway may show on one wearer standing in the same door, s. */
	public static final float BRIMSTONE_GAP = 1f, BULK_GAP = 1.5f;
	/** The motes a kill's stored force sends into the weapon (Kinetic); a buff tick's quads. */
	public static final int STORE_MOTES = 4, TICK_CAP = 6;
	/** Rich targets of a multi-target row (the core's RICH_TARGETS). */
	static final int RICH = EnchantFx.RICH_TARGETS;

	// ------------------------------------------------------------------ helpers

	static float clamp( float s ){
		return Math.max( 0f, Math.min( 1f, s ) );
	}

	//a beat that may leave marks of its own and lights: not an echo's, a joined or a minimal one
	static boolean full( Beat b ){
		return !b.echo && !b.join && !b.minimal;
	}

	//the weapon side of a sprite (Crackle's), its middle
	static float sideX( CharSprite s ){
		return s.x + s.width() / 2f + (s.flipHorizontal ? -EnchantFx.WEAPON_SIDE : EnchantFx.WEAPON_SIDE);
	}

	static float midX( CharSprite s ){
		return s.x + s.width() / 2f;
	}

	static float midY( CharSprite s ){
		return s.y + s.height() / 2f;
	}

	static float feetY( CharSprite s ){
		return s.y + s.height() - 2f;
	}

	//a cell's middle and its feet (where a mark lies), world px
	static float cellX( int cell ){
		return (cell % Dungeon.level.width() + 0.5f) * DungeonTilemap.SIZE;
	}

	static float cellFeet( int cell ){
		return (cell / Dungeon.level.width() + 1) * DungeonTilemap.SIZE - 3f;
	}

	//a dry floor cell a mark can lie on (render side: a read of the level)
	static boolean dryFloor( int cell ){
		Level l = Dungeon.level;
		return l != null && cell >= 0 && cell < l.length() && l.passable[cell] && !l.water[cell] && !l.pit[cell];
	}

	//a mark past the beat's own one, the row's: never an echo's; null off the scene
	static FxDecal mark( Beat b, float x, float y, int[] frame, int color, float alpha, float hold, float fade ){
		if (!full( b )) return null;
		return FxDecal.at( x, y, frame, b.c( color ), b.a( alpha ), hold, fade, FxRandom.chance( 0.5f ) );
	}

	//a moment of no proc may draw: the scene's light is up and one of the sprites is seen
	static boolean shows( CharSprite a, CharSprite b ){
		if (Fx.glowAir() == null) return false;
		boolean seen = (a != null && a.parent != null && a.visible) || (b != null && b.parent != null && b.visible);
		return seen;
	}

	//a spark of light thrown off (x, y) in the beat's colours (its element's bits)
	static FxParticle spark( Beat b, float x, float y, Ramp r, float angle, float speed, int pri ){
		FxParticle p = b.lit( x, y, pri );
		if (p == null) return null;
		return p.look( FxFrames.SPARK_2 ).orient().slow( FxFrames.MOTE_1, 15f ).ramp( b.echo ? b.look.pale : r, true )
				.polar( angle, speed ).gravity( 120f ).drag( 1.2f ).ground( b.feet + FxRandom.Float( -1f, 3f ), FxParticle.BOUNCE, 0.35f )
				.life( FxRandom.Float( 0.3f, 0.45f ) ).alpha( b.a( 1f ), 0, 0.1f );
	}

	//an ember rising off (x, y)
	static FxParticle ember( Beat b, float x, float y, Ramp r, float speed ){
		FxParticle p = b.lit( x, y, FxBudget.P2 );
		if (p == null) return null;
		return p.look( FxRandom.chance( 0.6f ) ? FxFrames.EMBER_1 : FxFrames.EMBER_2 ).ramp( b.echo ? b.look.pale : r, true )
				.vel( FxRandom.Float( -10f, 10f ) * speed, -FxRandom.Float( 18f, 32f ) * speed ).gravity( -8f ).drag( 1.2f )
				.sway( 8f, FxRandom.Float( 6f, 10f ) ).wind( 0.4f ).life( FxRandom.Float( 0.5f, 0.8f ) ).alpha( b.a( 1f ), 0, 0.3f );
	}

	//a puff of matter off (x, y) in a ramp
	static FxParticle puff( Beat b, int[][] frames, float x, float y, Ramp r, float vx, float vy, float life, float alpha ){
		FxParticle p = b.mat( x, y, FxBudget.P2 );
		if (p == null) return null;
		return p.look( frames, 0 ).ramp( b.echo ? b.look.pale : r, false ).smooth().vel( vx, vy ).drag( 0.8f ).life( life )
				.alpha( b.a( alpha ), 0.05f, life * 0.55f );
	}

	//a rune of a ring, lit in the beat's ramp, showing `late` s on
	static FxParticle rune( Beat b, float x, float y, float late, float life ){
		FxParticle p = b.lit( x, y, FxBudget.P1 );
		if (p == null) return null;
		return p.look( FxFrames.RUNE_3[FxRandom.Int( FxFrames.RUNE_3.length )] ).ramp( b.ramp(), true )
				.life( late + life ).alpha( b.a( 0.95f ), late, life * 0.6f ).snap();
	}

	//a mote at (x, y) drawn into (tx, ty) over `life` s, bending a little on its way (a curl off the line)
	static FxParticle inward( Beat b, boolean light, int[] frame, int color, float x, float y, float tx, float ty, float life, float bend ){
		FxParticle p = b.particle( light, x, y, FxBudget.P1 );
		if (p == null) return null;
		float vx = (tx - x) / life, vy = (ty - y) / life;
		return p.look( frame ).paint( b.c( color ) ).vel( vx - vy * bend, vy + vx * bend ).drag( bend > 0 ? 1.2f : 0f )
				.life( life ).alpha( b.a( 1f ), 0.04f, life * 0.4f ).snap();
	}

	//a run of ring frames: grown over `grow` s in steps of STEP, held `hold` s, ended in `end`
	static final float STEP = 0.05f;

	static int[][] ringRun( int[][] grow, float hold, int[][] end ){
		int held = Math.max( 1, Math.round( hold / STEP ) );
		int[][] run = new int[grow.length + held + end.length][];
		int k = 0;
		for (int[] f : grow) run[k++] = f;
		for (int i = 0; i < held; i++) run[k++] = grow[grow.length - 1];
		for (int[] f : end) run[k++] = f;
		return run;
	}

	// ================================================================== weapon enchantments

	// ------------------------------------------------------------------ Blazing (EMBER)

	/** Blazing's strength on an ignite: 0.3, 0.12 more for each enchant level and 0.4 of the power
	 *  past 1 (igniteStrength); on a flare-up the usual, by its burn (EnchantFx.strength). */
	public static float igniteStrength( int level, float pm ){
		return clamp( 0.3f + 0.12f * level + 0.4f * (pm - 1f) );
	}

	/**
	 * Blazing: a fresh ignite leaves two small tongues clinging to the body for 0.4 s, handing over to
	 * the burning look; the extra burn (a body already alight) flares up: a large tongue over the
	 * head, embers thrown up from it and a passing GLOW_23 (no new mark). The BURNING sting is the
	 * row's (Burning plays none).
	 */
	public static Twist blazing( final boolean ignite, final boolean flare ){
		return b -> {
			if (b.minimal || b.join) return;
			Ramp fire = b.el.ember != null ? b.el.ember : b.el.ramp;
			if (ignite){
				for (int i = 0; i < 2; i++){
					FxParticle p = b.lit( b.cx + (i == 0 ? -3f : 3f), b.cy + 2f, FxBudget.P1 );
					if (p == null) break;
					p.look( FxFrames.TONGUE_S, 12f ).ramp( b.ramp(), true ).vel( 0, -4f ).wind( 0.1f ).life( 0.4f )
							.alpha( b.a( 0.9f ), 0.03f, 0.15f );
				}
			}
			if (flare){
				FxParticle t = b.lit( b.cx, b.top - 1f, FxBudget.P1 );
				if (t != null) t.look( FxFrames.TONGUE_L, 10f ).ramp( b.ramp(), true ).vel( 0, -12f ).wind( 0.15f ).life( 0.35f )
						.alpha( b.a( 1f ), 0, 0.15f );
				if (full( b )){
					FxLight g = b.glow( b.cx, b.top, 23, b.look.light, 0.45f );
					if (g != null) g.life( 0f, 0.03f, 0.3f );
				}
				int k = Math.min( 4, b.left() );
				for (int i = 0; i < k; i++) ember( b, b.cx + FxRandom.Float( -4f, 4f ), b.top + FxRandom.Float( -2f, 3f ), fire, 1.3f );
			}
		};
	}

	// ------------------------------------------------------------------ Chilling (RIME)

	/**
	 * Chilling: three frost motes cling to the body for 1.2 s; the rime crawls on past its feet along
	 * the blow (held 2 s and 0.3 s more for every turn of chill, at most 4 s); a strong one lays a
	 * second rime on the next floor cell along the blow.
	 */
	public static Twist chilling( final float chillTurns ){
		return b -> {
			if (b.minimal || b.join) return;
			int frost = b.c( Element.FROST.ramp.stop( 1 ) );
			int n = Math.min( 3, b.left() );
			for (int i = 0; i < n; i++){
				FxParticle p = b.lit( b.cx + (i - 1) * 4f + FxRandom.Float( -1f, 1f ), b.cy + FxRandom.Float( -4f, 3f ), FxBudget.P2 );
				if (p == null) break;
				p.look( FxFrames.SNOWFLAKE_5, 3f ).paint( frost ).vel( FxRandom.Float( -1f, 1f ), 2f ).life( 1.2f )
						.alpha( b.a( 0.85f ), 0.1f, 0.4f ).snap();
			}
			if (!full( b )) return;
			float hold = Math.min( 4f, 2f + 0.3f * Math.max( 0f, chillTurns ) );
			int rimeColor = Element.FROST.ramp.stop( 2 );
			float x = b.cx + b.dx * 7f, y = b.feet + 1f + b.dy * 3f;
			if (!b.onWater( x, y )){
				FxDecal d = mark( b, x, y, FxFrames.RIME[0], rimeColor, 0.55f, hold, 1f );
				if (d != null) d.revealCrop( b.dx >= 0 ? FxDecal.FROM_LEFT : FxDecal.FROM_RIGHT, 0.5f );
			}
			if (b.tier >= STRONG){
				float nx = b.cx + Math.signum( b.dx ) * DungeonTilemap.SIZE, ny = b.feet + Math.signum( Math.round( b.dy ) ) * DungeonTilemap.SIZE;
				int cell = Fx.cellAt( nx, ny );
				if (dryFloor( cell )){
					FxDecal d = mark( b, cellX( cell ), cellFeet( cell ) + 1f, FxFrames.RIME[1], rimeColor, 0.5f, hold, 1f );
					if (d != null) d.revealCrop( FxDecal.FROM_MIDDLE, 0.45f );
				}
			}
		};
	}

	// ------------------------------------------------------------------ Shocking and Neptune's shock (STORM)

	/** A shock's strength: the characters it struck, the defender among them, a fifth each, and a
	 *  tenth for each enchant level (three struck at +0 is strong). */
	public static float shockStrength( int struck, int level ){
		return clamp( struck / 5f + 0.1f * level );
	}

	/**
	 * Shocking: its look on the defender and on every character the lightning struck (the first
	 * RICH_TARGETS rich, the rest a tinted star and two motes), each with the shock's twist. Kept as
	 * it was: the Lightning, its sound, the crackle and the sparks. Any thread.
	 */
	public static void shocking( EnchantLook l, Char attacker, Char defender, List<Char> others, float s, int level, int flags ){
		EnchantFx.proc( l, attacker, defender, s, level, flags | EnchantFx.multi( 0 ), shock( 0, -1 ) );
		int i = 1;
		for (Char ch : others){
			if (ch == defender) continue;
			EnchantFx.proc( l, attacker, ch, s, level, flags | EnchantFx.multi( i ), shock( i, -1 ) );
			i++;
		}
	}

	/**
	 * Neptune's shock: its look on every enemy it struck (the first RICH_TARGETS rich), the first
	 * with a ring at the reach's radius round the wielder and a ring of sea foam at his feet. Kept as
	 * it was: the Lightning, its sound, the crackle, the sparks and its shake. Any thread.
	 */
	public static void neptune( EnchantLook l, Char attacker, List<Char> struck, float s, int level, int reach ){
		for (int i = 0; i < struck.size(); i++){
			EnchantFx.proc( l, attacker, struck.get( i ), s, level, EnchantFx.multi( i ), shock( i, i == 0 ? reach : -1 ) );
		}
	}

	/**
	 * A struck character's share of a shock: the impact at the first (a white-hot flash at the hit),
	 * a static etch at its feet on dry floor at any strength (the family leaves one from MID), in
	 * water the wash and two rings (the family's one and this), and a passing pool of its light under
	 * each of the rich ones; with a reach, the wielder's rings.
	 */
	public static Twist shock( final int index, final int reach ){
		return b -> {
			if (b.minimal) return;
			boolean wet = b.onWater( b.cx, b.feet );
			if (index == 0 && !b.echo && !b.join){
				FxParticle p = b.lit( b.hx, b.hy, FxBudget.P1 );
				if (p != null) p.look( FxFrames.CORE_FLASH ).ramp( b.look.star, true ).life( 0.08f ).alpha( 1f, 0, 0.04f ).snap();
			}
			if (wet){
				if (!b.echo && b.takeAll( 1 )) WaterFX.ring( b.cx + 3f, b.feet, WaterFX.S );
				if (b.tier == WEAK && !b.echo){
					int cell = Fx.cellAt( b.cx, b.feet );
					if (cell >= 0 && b.takeAll( 1 )) WaterFX.wash( cell, b.look.light, 0.5f, 0.6f );
				}
			} else if (b.tier == WEAK){
				b.decal( b.cx, b.feet + 1f, FxFrames.ETCH, StormFx.ETCH, 0.6f, StormFx.ETCH_HOLD, 0.5f );
			}
			if (full( b )){
				FxLight pool = b.pool( b.cx, b.feet + 2f, 23, b.look.light, 0.3f );
				if (pool != null) pool.life( 0f, 0.05f, 0.35f );
			}
			if (reach > 0 && b.as != null && !b.echo){
				float fx = midX( b.as ), fy = feetY( b.as );
				int w = FxFrames.RING_WIDTHS[FxFrames.ringIndex( Math.min( 73, (2 * reach - 1) * DungeonTilemap.SIZE ) )];
				b.ring( fx, fy, 13, Math.max( 17, w ), 0.5f, b.look.light, 0.5f, b.look.emissive );
				b.ring( fx, fy, 9, 23, 0.45f, WaterFX.liquid().foam, 0.6f, false );
			}
		};
	}

	// ------------------------------------------------------------------ Kinetic (FORCE)

	/**
	 * Kinetic's release (a blow carrying stored force): a ring sized by the stored force against the
	 * defender's health (23 px for little, 55 for more than all of it), a white flash at the hit and
	 * the body lit white as it is thrown back; a crack under it even when weak.
	 */
	public static Twist kinetic( final float share ){
		return b -> {
			if (b.minimal || b.join) return;
			if (!b.echo){
				FxParticle p = b.lit( b.hx, b.hy, FxBudget.P0 );
				if (p != null) p.look( FxFrames.CORE_FLASH ).paint( 0xFFFFFF ).life( 0.07f ).alpha( 1f, 0, 0.04f ).snap();
				final CharSprite s = b.ds;
				if (s != null) EnchantFx.later( EnchantFx.REACT_AT, () -> {
					if (s.parent != null) s.afterglow( 0xFFFFFF, 0.15f );
				} );
			}
			int to = share >= 1f ? 55 : share >= 0.5f ? 41 : share >= 0.25f ? 31 : 23;
			b.ring( b.cx, b.feet, 13, to, 0.4f, Ramp.lerp( 0xFFFFFF, b.look.light, 0.4f ), 0.7f, true );
			if (b.tier == WEAK && !b.onWater( b.cx, b.feet )){
				b.decal( b.cx, b.feet + 1f, FxFrames.CRACK_11[FxRandom.Int( 2 )], ForceFx.CRACK, 0.5f, 2f, 1f );
			}
		};
	}

	/** The force a kill stores (Kinetic, Char.damage): STORE_MOTES motes of it flying from the
	 *  corpse into the wielder's weapon side in 0.3 s. Any thread. */
	public static void kineticStore( final Char corpse, final Char wielder, final int stored ){
		if (corpse == null || wielder == null || corpse.sprite == null || wielder.sprite == null || stored <= 0) return;
		final CharSprite cs = corpse.sprite, ws = wielder.sprite;
		Fx.post( () -> {
			if (!shows( cs, ws )) return;
			EnchantLook l = EnchantLooks.of( Kinetic.class );
			float x = midX( cs ), y = midY( cs ), tx = sideX( ws ), ty = midY( ws ) + 1f;
			Beat b = Beat.at( l, x, y, STORE_MOTES );
			Ramp metal = l.el.ember != null ? l.el.ember : l.el.ramp;
			for (int i = 0; i < STORE_MOTES; i++){
				float ox = FxRandom.Float( -5f, 5f ), oy = FxRandom.Float( -5f, 4f ), life = 0.3f + 0.03f * i;
				FxParticle p = b.lit( x + ox, y + oy, FxBudget.P2 );
				if (p == null) break;
				p.look( i % 2 == 0 ? FxFrames.MOTE_2 : FxFrames.SPARK_2 ).ramp( metal, true )
						.vel( (tx - x - ox) / life, (ty - y - oy) / life ).life( life ).alpha( 1f, 0.03f, 0.08f ).snap();
			}
		} );
	}

	// ------------------------------------------------------------------ Venomous (VENOM)

	/** Venomous's strength: a fresh poison middling, poison laid on poison strong, a tenth more for
	 *  each enchant level (venomStrength). */
	public static float venomStrength( boolean fresh, int level ){
		return clamp( (fresh ? 0.35f : 0.65f) + 0.1f * level );
	}

	/**
	 * Venomous: a green drop of venom circling over the head where the MARK orbit runs for 1.6 s (the
	 * poison takes three turns to bite); a strong one throws more drops and eats a second etch into
	 * the floor beside the first.
	 */
	public static Twist venomous(){
		return b -> {
			if (b.minimal || b.join) return;
			int green = b.c( b.look.glow );
			FxParticle m = b.mat( b.cx, b.cy - 14f, FxBudget.P1 );
			if (m != null) m.look( FxFrames.DROP_2 ).paint( green ).sway( 30f, 5f ).life( 1.6f ).alpha( b.a( 0.95f ), 0.1f, 0.4f ).snap();
			if (b.tier < STRONG) return;
			int n = Math.min( 4, b.left() );
			for (int i = 0; i < n; i++){
				FxParticle d = b.mat( b.hx, b.hy, FxBudget.P2 );
				if (d == null) break;
				float a = (float)(-Math.PI / 2) + (i - (n - 1) / 2f) * 0.6f + FxRandom.Float( -0.2f, 0.2f );
				d.look( FxFrames.DROP_3 ).paint( b.c( MotifFx.matterColor( b.look ) ) ).orient().polar( a, FxRandom.Float( 26f, 44f ) )
						.gravity( 240f ).ground( b.feet + FxRandom.Float( -1f, 3f ), FxParticle.DIE, 0 ).life( 0.8f ).alpha( b.a( 1f ), 0, 0.15f );
			}
			float x = b.cx + (b.dx >= 0 ? 6f : -6f);
			if (!b.onWater( x, b.feet )){
				mark( b, x, b.feet + 2f, FxFrames.ETCH, b.el.ramp.stop( Math.min( 3, b.el.ramp.size() - 1 ) ), 0.5f, VenomFx.ETCH_HOLD, 0.8f );
			}
		};
	}

	// ------------------------------------------------------------------ Blocking (WARD)

	static final int[][] BUBBLE_31 = { FxFrames.Light.BUBBLE_31 };

	/**
	 * Blocking, on the wielder: a ward on him toward the defender (an echo, from a vestige, the
	 * proc's echo with the roles swapped, so it lands on him late, hollow and pale): the family's arc
	 * facing the defender, its bubble and runes; this adds the runes the ring lacks (four from MID),
	 * and a strong one a wider bubble and six runes, drifting up as the shield takes over. Any thread.
	 */
	public static void blocking( EnchantLook l, Char wielder, Char defender, float s, int level, boolean echo ){
		if (echo){
			//roles swapped so the family lands on the wielder: late, hollow and pale, unstamped
			EnchantFx.proc( l, defender, wielder, s, level, EnchantFx.ECHO, blocking() );
		} else {
			EnchantFx.ward( l, wielder, defender, s, level, 0, blocking() );
		}
	}

	/** Blocking's share of its beat (blocking(...) above). */
	public static Twist blocking(){
		return b -> {
			if (b.minimal || b.join) return;
			int want = b.tier >= STRONG ? 2 : (b.tier == MID ? 1 : 0);
			if (b.tier >= STRONG){
				Flash f = b.flash( true, BUBBLE_31, 0f, b.cx, b.cy, b.look.light, b.echo ? 0.25f : 0.4f, 0.2f );
				if (f != null) f.envelope( 0.02f, 0.08f );
			}
			float a0 = FxRandom.angle();
			for (int i = 0; i < want; i++){
				float a = a0 + (float)(Math.PI * i);
				FxParticle r = rune( b, b.cx + (float)Math.cos( a ) * 9f, b.cy + (float)Math.sin( a ) * 4f, 0.3f + 0.08f * i, 0.35f );
				if (r == null) break;
				r.vel( 0, -6f );
			}
		};
	}

	// ------------------------------------------------------------------ Blooming (VERDANT)

	/** Blooming's strength: the cells it grew against what it grows on average. */
	public static float bloomStrength( int cells, int level, float pm ){
		return clamp( cells / (1.5f + 0.1f * level) / Math.max( 1f, pm ) );
	}

	/**
	 * Blooming: a blade shooting up on each grown cell (the first RICH_TARGETS, each 0.04 s later than
	 * the last by its distance from the defender), and a strong one's green ring rolled out over the
	 * whole patch. `cells` are the grown cells, the defender's cell `from`.
	 */
	public static Twist blooming( final int[] cells, final int from ){
		return b -> {
			if (b.minimal || b.join || Dungeon.level == null) return;
			int n = Math.min( RICH, cells.length );
			for (int i = 0; i < n; i++){
				final int c = cells[i];
				final float late = 0.04f * Math.max( 0, Dungeon.level.distance( from, c ) );
				final Beat one = b.split( 1 );
				if (one.left() <= 0) break;
				EnchantFx.later( late, () -> {
					FxParticle p = one.mat( cellX( c ) + FxRandom.Float( -2f, 2f ), cellFeet( c ), FxBudget.P1 );
					if (p != null){
						p.look( FxFrames.BLADE_4 ).paint( one.c( Element.NATURE.ramp.stop( 2 + FxRandom.Int( 2 ) ) ) ).vel( FxRandom.Float( -2f, 2f ), -36f )
								.drag( 7f ).life( 0.7f ).alpha( one.a( 1f ), 0, 0.3f ).snap();
					}
				} );
			}
			if (b.tier >= STRONG){
				b.ring( b.cx, b.feet, 13, cells.length > 2 ? 55 : 41, 0.6f, Element.NATURE.light, 0.5f, false );
			}
		};
	}

	// ------------------------------------------------------------------ Elastic (FORCE)

	static final int[][] RING_17 = { FxFrames.RING_17 };

	/** Elastic: a magenta launch ring at the defender's feet as it is thrown (its push's ghosts take
	 *  the same magenta, and a rebound ring where it lands: Pushing). */
	public static Twist elastic(){
		return b -> {
			if (b.minimal || b.join) return;
			Flash f = b.flash( false, true, RING_17, 0f, b.cx, b.feet, b.look.glow, 0.8f, 0.25f );
			if (f != null) f.envelope( 0f, 0.06f );
		};
	}

	// ------------------------------------------------------------------ Crystal (WARD glass)

	/** Crystal's every hit (its magic always bites): two glass shards and a glint at the hit, no light,
	 *  no sound. Any thread. */
	public static void crystalHit( final Char attacker, final Char defender ){
		if (attacker == null || defender == null || attacker.sprite == null || defender.sprite == null) return;
		final CharSprite as = attacker.sprite, ds = defender.sprite;
		Fx.post( () -> {
			if (!shows( as, ds ) || ds.parent == null) return;
			EnchantLook l = EnchantLooks.of( Crystal.class );
			float cx = midX( ds ), cy = midY( ds ), vx = cx - midX( as ), vy = cy - midY( as );
			float len = (float)Math.sqrt( vx * vx + vy * vy );
			float x = len > 0.5f ? cx - vx / len * 4f : cx, y = len > 0.5f ? cy - vy / len * 4f : cy;
			Beat b = Beat.at( l, x, y, 3 );
			FxParticle g = b.lit( x, y, FxBudget.P1 );
			if (g != null) g.look( FxFrames.PLUS_3 ).paint( Element.STEEL.glint ).life( 0.12f ).alpha( 1f, 0, 0.06f ).snap();
			for (int i = 0; i < 2; i++){
				FxParticle p = b.mat( x, y, FxBudget.P2 );
				if (p == null) break;
				float a = (float)(-Math.PI / 2) + FxRandom.Float( -1.2f, 1.2f );
				p.look( FxFrames.SHARD_GLASS, 0 ).paint( Ramp.lerp( l.glow, 0xFFFFFF, 0.5f ) ).polar( a, FxRandom.Float( 25f, 45f ) )
						.gravity( 160f ).spin( 0.1f ).ground( feetY( ds ) + FxRandom.Float( -1f, 2f ), FxParticle.BOUNCE, 0.3f )
						.life( 0.5f ).alpha( 1f, 0, 0.2f );
			}
		} );
	}

	/** Crystal shattering in the wielder's hand: ten glass shards fanned out from his weapon side, a
	 *  cyan ring and a passing glow (Crystal plays its own SHATTER). Any thread. */
	public static void crystalShatter( final Char wielder ){
		if (wielder == null || wielder.sprite == null) return;
		final CharSprite ws = wielder.sprite;
		Fx.post( () -> {
			if (!shows( ws, null )) return;
			EnchantLook l = EnchantLooks.of( Crystal.class );
			float x = sideX( ws ), y = midY( ws ) + 1f, face = ws.flipHorizontal ? (float)Math.PI : 0f;
			Beat b = Beat.at( l, x, y, EnchantFx.CAP[STRONG] );
			FxLight g = b.glow( x, y, 23, l.light, 0.5f );
			if (g != null) g.life( 0f, 0.03f, 0.3f );
			b.flash( false, true, RING_17, 0f, x, y, 0x40E0FF, 0.8f, 0.3f );
			for (int i = 0; i < 10; i++){
				FxParticle p = b.mat( x, y, i < 4 ? FxBudget.P1 : FxBudget.P2 );
				if (p == null) break;
				float a = face + (i - 4.5f) * 0.22f + FxRandom.Float( -0.08f, 0.08f );
				p.look( FxFrames.SHARD_GLASS, 0 ).paint( Ramp.lerp( l.glow, 0xFFFFFF, 0.3f + 0.05f * (i % 4) ) ).polar( a, FxRandom.Float( 40f, 70f ) )
						.gravity( 160f ).spin( 0.08f ).ground( feetY( ws ) + FxRandom.Float( -2f, 4f ), FxParticle.BOUNCE, 0.3f )
						.life( FxRandom.Float( 0.5f, 0.7f ) ).alpha( 1f, 0, 0.2f );
			}
		} );
	}

	// ------------------------------------------------------------------ Paralysis (STORM stun)

	static final int[][] STUN_GROW = { FxFrames.RING_9, FxFrames.RING_13, FxFrames.RING_17, FxFrames.RING_23 },
			STUN_GROW_STRONG = { FxFrames.RING_13, FxFrames.RING_17, FxFrames.RING_23, FxFrames.RING_31 };
	static final int[][] SHATTER = { FxFrames.IMPACT_STAR[0], FxFrames.IMPACT_STAR[FxFrames.IMPACT_STAR.length - 1] };
	/** The amber sparks the stun's ring shatters into. */
	public static final int SHATTER_SPARKS = 6;
	static final Ramp AMBER = new Ramp( 0xFFF4C8, 0xFFD860, 0xCCAA44, 0x6A5422 );

	/**
	 * Paralysis: an amber ring opening round the body from 9 px to 23 (a strong one's from 13 to 31)
	 * that stops dead, holds 0.3 to 0.6 s by how long the stun lasts, and shatters into six amber
	 * sparks; a strong one a third stun star circling where the MARK orbit runs a while (the stun look
	 * draws the other two; the MARK ring itself is the guiding light's and the skills', never this).
	 */
	public static Twist paralysis( final float turns ){
		return b -> {
			if (b.minimal || b.join) return;
			float hold = Math.max( 0.3f, Math.min( 0.6f, 0.15f + 0.1f * turns ) );
			int[][] run = ringRun( b.tier >= STRONG ? STUN_GROW_STRONG : STUN_GROW, hold, SHATTER );
			final float rx = b.cx, ry = b.cy + 2f;
			Flash f = b.flash( false, true, run, 0f, rx, ry, b.look.glow, 0.85f, run.length * STEP );
			if (f != null) f.envelope( 0f, (run.length - 1) * STEP );
			if (b.tier >= STRONG){
				FxParticle s = b.lit( b.cx, b.cy - 14f, FxBudget.P1 );
				if (s != null) s.look( FxFrames.STAR4_5 ).paint( b.c( b.look.glow ) ).sway( 30f, 5f ).life( 1.4f )
						.alpha( b.a( 0.9f ), 0.15f, 0.4f ).snap();
			}
			//as the ring shatters: its sparks thrown all round
			if (f == null) return;
			final Beat shards = b.split( SHATTER_SPARKS );
			final int n = shards.left();
			if (n <= 0) return;
			final float r = (b.tier >= STRONG ? 31 : 23) / 2f;
			EnchantFx.later( (run.length - SHATTER.length) * STEP, () -> {
				if (shards.ds != null && shards.ds.parent == null) return;
				float a0 = FxRandom.angle();
				for (int i = 0; i < n; i++){
					float a = a0 + (float)(2 * Math.PI * i / n) + FxRandom.Float( -0.2f, 0.2f );
					if (spark( shards, rx + (float)Math.cos( a ) * r, ry + (float)Math.sin( a ) * r * 0.5f, AMBER, a,
							FxRandom.Float( 25f, 40f ), FxBudget.P2 ) == null) break;
				}
			} );
		};
	}

	// ------------------------------------------------------------------ Surging (STORM gold)

	/**
	 * Surging: an impact at the hit growing with the stacks (a flash of a fifth more for each), and
	 * once the surge adds to the blow a gold streak from the weapon side to the hit. (Its stacks
	 * circle the wielder as the SURGING state.)
	 */
	public static Twist surging( final int stacks, final int bonus ){
		return b -> {
			if (b.minimal || b.join) return;
			if (bonus > 0 && b.as != null){
				float gx = b.hx - b.wx, gy = b.hy - b.wy, len = (float)Math.sqrt( gx * gx + gy * gy );
				FxParticle p = b.lit( (b.wx + b.hx) / 2f, (b.wy + b.hy) / 2f, FxBudget.P1 );
				if (p != null){
					p.look( FxFrames.STREAK_TAPER ).paint( b.c( b.look.glow ) ).life( 0.14f ).alpha( b.a( 0.95f ), 0, 0.08f );
					p.angle = (float)Math.toDegrees( Math.atan2( gy, gx ) );
					p.scale.x = len > 24f ? 2f : 1f;
				}
			}
			if (!b.echo){
				FxParticle c = b.lit( b.hx, b.hy, FxBudget.P1 );
				if (c != null){
					c.look( FxFrames.CORE_FLASH ).ramp( b.look.star, true ).life( 0.09f ).alpha( 1f, 0, 0.05f ).snap();
					c.scale.set( 0.8f + 0.2f * Math.max( 1, Math.min( 5, stacks ) ) );
				}
			}
		};
	}

	// ------------------------------------------------------------------ Parasitic (VERDANT sick)

	static final int[][] RING_5 = { FxFrames.RING_5, FxFrames.RING_9 };

	/** Parasitic: the seed streaking from the weapon side into the defender and a small ring where it
	 *  plants (the SEEDED state then beats on its chest while the seed lasts). */
	public static Twist parasitic(){
		return b -> {
			if (b.minimal || b.join) return;
			float t = 0.15f;
			if (b.as != null){
				FxParticle s = b.mat( b.wx, b.wy, FxBudget.P1 );
				if (s != null) s.look( FxFrames.SEED ).paint( b.c( b.look.glow ) ).vel( (b.hx - b.wx) / t, (b.hy - b.wy) / t ).life( t )
						.alpha( b.a( 1f ), 0, 0.03f ).snap();
			}
			Flash r = b.flash( false, true, RING_5, 0f, b.hx, b.hy + 1f, b.look.glow, 0.8f, 0.3f );
			if (r != null) r.delay( b.as != null ? t : 0f );
		};
	}

	/**
	 * A parasitic seed's tick (ParasiticSeed.act, the actor thread): a blade pops from the host's body
	 * and a green lifeline of beads runs from it to each foe it fed (the first RICH_TARGETS; two beads
	 * each, one for the third). TICK_CAP quads. Any thread.
	 */
	public static void seedTick( final Char host, final List<Char> fed ){
		if (host == null || host.sprite == null) return;
		final CharSprite hs = host.sprite;
		final int n = Math.min( RICH, fed.size() );
		final CharSprite[] to = new CharSprite[n];
		for (int i = 0; i < n; i++) to[i] = fed.get( i ).sprite;
		Fx.post( () -> {
			if (!shows( hs, null ) || hs.parent == null) return;
			EnchantLook l = EnchantLooks.of( Parasitic.class );
			float x = midX( hs ), y = midY( hs );
			Beat b = Beat.at( l, x, y, TICK_CAP );
			int green = Element.NATURE.ramp.stop( 2 );
			FxParticle blade = b.mat( x + FxRandom.Float( -3f, 3f ), y + 2f, FxBudget.P1 );
			if (blade != null) blade.look( FxFrames.BLADE_3 ).paint( green ).vel( 0, -20f ).drag( 5f ).life( 0.5f ).alpha( 1f, 0, 0.25f ).snap();
			for (int i = 0; i < n; i++){
				CharSprite f = to[i];
				if (f == null || f.parent == null) continue;
				float tx = midX( f ), ty = midY( f );
				int beads = i < 2 ? 2 : 1;
				for (int k = 0; k < beads; k++){
					float life = 0.35f + 0.06f * k;
					FxParticle p = b.lit( x, y - 2f, FxBudget.P2 );
					if (p == null) break;
					//over the top: up off the line, falling into the foe
					p.look( FxFrames.MOTE_2 ).paint( Ramp.lerp( green, 0xC8F070, 0.5f ) ).vel( (tx - x) / life, (ty - y) / life - 20f )
							.gravity( 2 * 20f / life ).life( life ).alpha( 1f, 0.04f * k + 0.01f, 0.1f ).snap();
				}
			}
		} );
	}

	// ------------------------------------------------------------------ Holy (RADIANCE)

	/** Holy on the demonic (a weaker bite than on the undead): two puffs of hell smoke burnt off the
	 *  body. On the undead the family alone (its shaft, star, motes and pool). */
	public static Twist holy( final boolean demonic ){
		return b -> {
			if (!demonic || b.minimal || b.join) return;
			for (int i = 0; i < 2; i++){
				if (puff( b, FxFrames.PUFF_S, b.cx + (i == 0 ? -3f : 3f), b.cy, Element.HELL.smoke, (i == 0 ? -3f : 3f), -9f, 0.7f, 0.55f ) == null) break;
			}
		};
	}

	// ------------------------------------------------------------------ Ancient (FORCE age)

	static final int BRONZE = 0xE0C060;

	/** Ancient's bonus: a gold rune stamped at the hit (twice its size, a moment), a crack under it
	 *  even when weak; and when this blow is a counted kill, a small rune rising from the body into the
	 *  weapon side. */
	public static Twist ancient( final boolean killCounted ){
		return b -> {
			if (b.minimal || b.join) return;
			FxParticle r = b.lit( b.hx, b.hy, FxBudget.P1 );
			if (r != null){
				r.look( FxFrames.RUNE_3[2] ).paint( b.c( BRONZE ) ).life( 0.35f ).alpha( b.a( 1f ), 0, 0.2f ).snap();
				r.scale.set( 2f );
			}
			if (b.tier == WEAK && !b.onWater( b.cx, b.feet )){
				b.decal( b.cx, b.feet + 1f, FxFrames.CRACK_11[FxRandom.Int( 2 )], ForceFx.CRACK, 0.5f, 2f, 1f );
			}
			if (killCounted && b.as != null) rise( b, b.cx, b.cy, b.wx, b.wy );
		};
	}

	//a rune rising from (x, y) into the weapon side (wx, wy), drawn up over 0.5 s
	static void rise( Beat b, float x, float y, float wx, float wy ){
		final Beat one = b.split( 1 );
		EnchantFx.later( 0.12f, () -> {
			FxParticle p = one.lit( x, y - 2f, FxBudget.P1 );
			if (p != null) p.look( FxFrames.RUNE_3[0] ).paint( one.c( BRONZE ) ).vel( (wx - x) / 0.5f, (wy - y + 2f) / 0.5f - 10f )
					.gravity( 40f ).life( 0.5f ).alpha( one.a( 1f ), 0.05f, 0.12f ).snap();
		} );
	}

	/** Ancient counting a kill on a blow whose bonus did not bite: the rune rising from the body into
	 *  the weapon side, alone. Any thread. */
	public static void killRune( final Char wielder, final Char victim ){
		if (wielder == null || victim == null || wielder.sprite == null || victim.sprite == null) return;
		final CharSprite ws = wielder.sprite, vs = victim.sprite;
		Fx.post( () -> {
			if (!shows( ws, vs )) return;
			Beat b = Beat.at( EnchantLooks.of( Ancient.class ), midX( vs ), midY( vs ), 1 );
			rise( b, midX( vs ), midY( vs ), sideX( ws ), midY( ws ) + 1f );
		} );
	}

	/** The weapon attuning itself (Ancient's self-upgrade): the gain's ring of six runes locking round
	 *  the wielder one by one, then rising, a METAL flash at his weapon side and a ring at his feet;
	 *  FINISHER-sized (its cap). Any thread. */
	public static void attuned( final Char wielder ){
		if (wielder == null || wielder.sprite == null) return;
		final CharSprite ws = wielder.sprite;
		Fx.post( () -> {
			if (!shows( ws, null )) return;
			EnchantLook l = EnchantLooks.of( Ancient.class );
			float x = midX( ws ), y = midY( ws ), feet = feetY( ws );
			Beat b = Beat.at( l, x, y, EnchantFx.CAP[EnchantFx.FINISHER] );
			FxLight g = b.glow( x, y, 31, Element.METAL.light, 0.5f );
			if (g != null) g.life( 0.05f, 0.1f, 0.5f );
			for (int i = 0; i < 6; i++){
				float a = (float)(2 * Math.PI * i / 6), late = 0.08f * i;
				FxParticle r = b.lit( x + (float)Math.cos( a ) * 10f, feet - 3f + (float)Math.sin( a ) * 4f, FxBudget.P1 );
				if (r == null) break;
				r.look( FxFrames.RUNE_3[i % FxFrames.RUNE_3.length] ).paint( BRONZE ).vel( 0, -14f ).life( late + 0.9f )
						.alpha( 1f, late, 0.35f ).snap();
			}
			final Beat flash = b.split( 2 );
			EnchantFx.later( 0.5f, () -> {
				FxParticle c = flash.lit( sideX( ws ), midY( ws ) + 1f, FxBudget.P0 );
				if (c != null) c.look( FxFrames.CORE_FLASH ).ramp( new Ramp( 0xFFFFFF, Element.METAL.light ), true ).life( 0.12f )
						.alpha( 1f, 0, 0.06f ).snap();
				flash.ring( midX( ws ), feetY( ws ), 13, 41, 0.5f, Element.METAL.light, 0.6f, true );
			} );
			int k = Math.min( 4, b.left() );
			for (int i = 0; i < k; i++){
				FxParticle d = b.mat( x + FxRandom.Float( -7f, 7f ), feet - 1f, FxBudget.P2 );
				if (d == null) break;
				d.look( FxFrames.PUFF_S, 0 ).ramp( Element.EARTH.smoke, false ).smooth().vel( FxRandom.Float( -8f, 8f ), -FxRandom.Float( 3f, 6f ) )
						.drag( 2f ).life( 0.6f ).alpha( 0.45f, 0.03f, 0.35f );
			}
		} );
	}

	// ------------------------------------------------------------------ Loki's poison (VENOM plague)

	/**
	 * Loki's poison: its look on every character it poisoned (allies too, as it does), the first
	 * RICH_TARGETS rich: a sickly ring at the poison's radius round the wielder with the first, a puff
	 * of fumes off each. Any thread.
	 */
	public static void lokis( EnchantLook l, Char attacker, List<Char> poisoned, float s, int level, int reach ){
		for (int i = 0; i < poisoned.size(); i++){
			EnchantFx.proc( l, attacker, poisoned.get( i ), s, level, EnchantFx.multi( i ), plague( i == 0 ? reach : -1 ) );
		}
	}

	/** Loki's poison's share on one it poisoned: the ring at the radius (`reach` cells round the
	 *  wielder) with the first, a puff of fumes off each. */
	public static Twist plague( final int reach ){
		return b -> {
			if (b.minimal || b.join) return;
			if (reach > 0 && b.as != null && !b.echo){
				int w = FxFrames.RING_WIDTHS[FxFrames.ringIndex( Math.min( 55, (2 * reach - 1) * DungeonTilemap.SIZE ) )];
				b.ring( midX( b.as ), feetY( b.as ), 13, Math.max( 17, w ), 0.6f, Ramp.lerp( Element.POISON.light, b.look.rim, 0.45f ), 0.5f, false );
			}
			puff( b, FxFrames.PUFF_S, b.cx, b.cy, Element.POISON.smoke, FxRandom.Float( -2f, 2f ), -6f, 0.8f, 0.5f );
		};
	}

	// ------------------------------------------------------------------ Poison (VENOM nightshade)

	/** Poison ("venomous"): two violet puffs of spores off the body and spores clinging to it 1.5 s,
	 *  before the poisoned look takes over. */
	public static Twist poison(){
		return b -> {
			if (b.minimal || b.join) return;
			Ramp spores = new Ramp( b.look.rim, b.look.glow, 0x24103E );
			for (int i = 0; i < 2; i++){
				if (puff( b, FxFrames.PUFF_S, b.cx + (i == 0 ? -3f : 3f), b.cy + 1f, spores, i == 0 ? -4f : 4f, -5f, 0.8f, 0.5f ) == null) return;
			}
			int n = Math.min( 4, b.left() );
			for (int i = 0; i < n; i++){
				FxParticle p = b.mat( b.cx + FxRandom.Float( -5f, 5f ), b.cy + FxRandom.Float( -5f, 4f ), FxBudget.P2 );
				if (p == null) break;
				p.look( FxFrames.MOTE_2 ).paint( b.c( b.look.rim ) ).vel( FxRandom.Float( -1f, 1f ), -1.5f ).sway( 3f, 3f ).life( 1.5f )
						.alpha( b.a( 0.9f ), 0.1f, 0.5f ).snap();
			}
		};
	}

	// ================================================================== glyphs

	// ------------------------------------------------------------------ Adaptation (VERDANT)

	/** Adaptation's and Camouflage's strength: a middling answer, a little more for each glyph level,
	 *  never strong (each answers the ground underfoot, often, so it never shakes the screen). */
	public static float verdantStrength( int level ){
		return Math.min( 0.55f, 0.35f + 0.05f * Math.max( 0, level ) );
	}

	/** The terrains Adaptation answers, and each one's colour (the row's cycle). */
	public static final int GRASS = 0, WATER = 1, TALL_GRASS = 2, EMBERS = 3, TRAP = 4;
	static final int[] TERRAIN_COLORS = { 0x44AA44, 0x3399CC, 0x228822, 0xFF8418, 0x9A5AD0 };

	/**
	 * Adaptation: the ward arc again in the colour of the terrain it drew on, and that terrain's sign
	 * - grass, a ring of earth pebbles hopping round the feet; water, a blue streak of speed and two
	 * drops; tall grass, leaves closing in on the wearer as he vanishes; embers, three embers rising;
	 * a spent trap, a violet rune. (Plain floor gives nothing, so there is nothing to show.)
	 */
	public static Twist adaptation( final int terrain ){
		return b -> {
			if (b.minimal || b.join || terrain < 0 || terrain >= TERRAIN_COLORS.length) return;
			int color = TERRAIN_COLORS[terrain];
			Flash arc = b.flash( true, EnchantFx.ARCS[HitFx.octant( -b.dx, -b.dy )], 0f, b.cx, b.cy, color, 0.7f,
					EnchantFx.ARC_HOLD + EnchantFx.ARC_FADE );
			if (arc != null) arc.envelope( 0f, EnchantFx.ARC_HOLD );
			switch (terrain){
				case GRASS:
					for (int i = 0; i < 3; i++){
						float a = (float)(2 * Math.PI * i / 3) + FxRandom.Float( -0.3f, 0.3f );
						FxParticle p = b.mat( b.cx + (float)Math.cos( a ) * 6f, b.feet + (float)Math.sin( a ) * 2f, FxBudget.P2 );
						if (p == null) break;
						p.look( FxFrames.PEBBLE_2 ).paint( b.c( Element.EARTH.ember.stop( 1 + i % 2 ) ) ).vel( 0, -FxRandom.Float( 22f, 30f ) )
								.gravity( 180f ).ground( b.feet + 1f, FxParticle.BOUNCE, 0.3f ).life( 0.6f ).alpha( b.a( 1f ), 0, 0.2f ).snap();
					}
					break;
				case WATER: {
					float side = b.ds != null && b.ds.flipHorizontal ? -1f : 1f;
					FxParticle s = b.lit( b.cx - side * 5f, b.feet - 2f, FxBudget.P1 );
					if (s != null){
						s.look( FxFrames.STREAK_TAPER ).paint( b.c( color ) ).vel( side * 20f, 0 ).life( 0.25f ).alpha( b.a( 0.9f ), 0, 0.15f );
						if (side < 0) s.angle = 180f;
					}
					for (int i = 0; i < 2; i++){
						FxParticle d = b.mat( b.cx + (i == 0 ? -3f : 3f), b.feet - 3f, FxBudget.P2 );
						if (d == null) break;
						d.look( FxFrames.DROP_2 ).paint( b.c( 0x9CD8FF ) ).polar( (float)(-Math.PI / 2) + (i == 0 ? -0.5f : 0.5f), 30f ).gravity( 200f )
								.ground( b.feet + 1f, FxParticle.DIE, 0 ).life( 0.6f ).alpha( b.a( 1f ), 0, 0.1f );
					}
					break;
				}
				case TALL_GRASS:
					for (int i = 0; i < 3; i++){
						float a = (float)(2 * Math.PI * i / 3) + FxRandom.Float( -0.4f, 0.4f );
						FxParticle p = b.mat( b.cx + (float)Math.cos( a ) * 10f, b.cy + (float)Math.sin( a ) * 6f, FxBudget.P2 );
						if (p == null) break;
						p.look( FxFrames.LEAF_D, 4f ).paint( b.c( color ) ).vel( -(float)Math.cos( a ) * 10f / 0.4f, -(float)Math.sin( a ) * 6f / 0.4f )
								.drag( 2f ).life( 0.5f ).alpha( b.a( 1f ), 0.05f, 0.2f );
					}
					break;
				case EMBERS: {
					Ramp fire = Element.FIRE.ember;
					for (int i = 0; i < 3; i++) if (ember( b, b.cx + FxRandom.Float( -4f, 4f ), b.feet - 2f, fire, 1f ) == null) break;
					break;
				}
				case TRAP: {
					FxParticle r = b.lit( b.cx, b.feet - 2f, FxBudget.P1 );
					if (r != null){
						r.look( FxFrames.RUNE_3[1] ).paint( b.c( color ) ).vel( 0, -6f ).life( 0.7f ).alpha( b.a( 1f ), 0.05f, 0.3f ).snap();
						r.scale.set( 2f );
					}
					break;
				}
			}
		};
	}

	// ------------------------------------------------------------------ AntiMagic (WARD null)

	/**
	 * AntiMagic absorbing a magic (Char.damage, only when its roll took something off): the ward's
	 * answer on the wearer - toward a character who cast it, else toward the side he faces (an
	 * ABSORB) - and the absorbed magic's motes bending round into the armour, four to six as there is
	 * room. Any thread.
	 */
	public static void antiMagic( Char wearer, Object src, int unwarded, int absorbed ){
		//what its roll took of the blow (a roll past the blow takes no more than all of it)
		absorbed = Math.min( unwarded, absorbed );
		if (wearer == null || absorbed <= 0) return;
		EnchantLook l = EnchantLooks.of( AntiMagic.class );
		float s = clamp( absorbed / (float)unwarded );
		if (src instanceof Char){
			EnchantFx.ward( l, wearer, (Char) src, s, 0, 0, nullified( absorbed ) );
		} else {
			EnchantFx.passive( l, wearer, EnchantFx.ABSORB, s, nullified( absorbed ) );
		}
	}

	/** The absorbed magic's motes bending into the armour (four to six, by how much it took). */
	public static Twist nullified( final int absorbed ){
		return b -> {
			if (b.minimal || b.join) return;
			int n = Math.min( Math.min( 6, 3 + absorbed / 3 ), b.left() );
			float side = -b.dx;
			for (int i = 0; i < n; i++){
				float a = (float)((i - (n - 1) / 2f) * 0.5f), d = 11f;
				float x = b.cx + side * (float)Math.cos( a ) * d, y = b.cy + (float)Math.sin( a ) * d * 0.7f;
				inward( b, true, i % 2 == 0 ? FxFrames.MOTE_2 : FxFrames.MOTE_1, b.look.light, x, y, b.cx, b.cy + 1f, 0.3f + 0.03f * i,
						(i % 2 == 0 ? 0.4f : -0.4f) );
			}
		};
	}

	// ------------------------------------------------------------------ Bounce (FORCE spring)

	static final int[][] SPROING = { FxFrames.RING_5, FxFrames.RING_9, FxFrames.RING_9 };

	/** Bounce: a "sproing" ring where the blow met the wearer (his push's ghosts take its blue:
	 *  Pushing). */
	public static Twist bounce(){
		return b -> {
			if (b.minimal || b.join) return;
			b.flash( false, true, SPROING, 0f, (b.cx + b.sx) / 2f, (b.cy + b.sy) / 2f, b.look.glow, 0.85f, 0.25f );
		};
	}

	// ------------------------------------------------------------------ Repulsion (FORCE steel)

	/** Repulsion: a shock ring rolling out from the wearer, 9 px to 31, whatever its strength (his
	 *  foe's push's ghosts take its white: Pushing). */
	public static Twist repulsion(){
		return b -> {
			if (b.minimal || b.join) return;
			b.ring( b.cx, b.feet, 9, 31, 0.4f, b.look.light, b.echo ? 0.35f : 0.6f, true );
		};
	}

	// ------------------------------------------------------------------ Brimstone (EMBER sulphur)

	private static final int GATES = 8;
	private static final int[] brimIds = new int[GATES];
	private static final float[] brimAt = { -1, -1, -1, -1, -1, -1, -1, -1 };

	/**
	 * Brimstone refusing a fire (Burning.reignite on a wearer the glyph keeps from burning): the arc
	 * toward his facing in its sulphur fire, tongues licking up the body and dying into embers that
	 * never catch (a SHRUG); at most once in BRIMSTONE_GAP s on one character. Any thread.
	 */
	public static void brimstone( final Char wearer ){
		if (wearer == null || wearer.sprite == null) return;
		final CharSprite ws = wearer.sprite;
		Fx.post( () -> {
			if (!gate( ws, brimIds, brimAt, BRIMSTONE_GAP )) return;
			EnchantFx.passive( EnchantLooks.of( Brimstone.class ), wearer, EnchantFx.SHRUG, 0.2f );
		} );
	}

	//whether a sprite's moment may play (none in its last `gap` s); if so, marked now. Render thread
	private static boolean gate( CharSprite s, int[] ids, float[] at, float gap ){
		int id = System.identityHashCode( s );
		float now = com.watabou.noosa.Game.timeTotal;
		int free = -1, oldest = 0;
		for (int i = 0; i < ids.length; i++){
			if (at[i] >= 0 && ids[i] == id){
				if (now >= at[i] && now - at[i] < gap) return false;
				at[i] = now;
				return true;
			}
			if (at[i] < 0 && free < 0) free = i;
			if (at[i] < at[oldest]) oldest = i;
		}
		int k = free >= 0 ? free : oldest;
		ids[k] = id;
		at[k] = now;
		return true;
	}

	// ------------------------------------------------------------------ Camouflage (VERDANT)

	/** Camouflage's trample: blades flicking up round the feet and leaves swirling closed over the
	 *  wearer as he vanishes (its MELD is its own). */
	public static Twist camouflage(){
		return b -> {
			if (b.minimal || b.join) return;
			FxParticle blade = b.mat( b.cx + FxRandom.Float( -6f, 6f ), b.feet, FxBudget.P1 );
			if (blade != null) blade.look( FxFrames.BLADE_3 ).paint( b.c( Element.NATURE.ramp.stop( 3 ) ) ).vel( 0, -32f ).drag( 6f ).life( 0.6f )
					.alpha( b.a( 1f ), 0, 0.3f ).snap();
			int n = Math.min( 6, b.left() );
			for (int i = 0; i < n; i++){
				final float a = (float)(2 * Math.PI * i / Math.max( 1, n ));
				FxParticle p = b.mat( b.cx + (float)Math.cos( a ) * 11f, b.cy + (float)Math.sin( a ) * 7f, FxBudget.P2 );
				if (p == null) break;
				//round and in: a swirl closing over him
				p.look( FxFrames.LEAF_D, 4f ).paint( b.c( b.look.glow ) )
						.vel( -(float)Math.cos( a ) * 22f - (float)Math.sin( a ) * 14f, -(float)Math.sin( a ) * 14f + (float)Math.cos( a ) * 9f )
						.drag( 1.5f ).life( 0.55f ).alpha( b.a( 1f ), 0.1f + 0.03f * i, 0.2f );
			}
		};
	}

	// ------------------------------------------------------------------ Crystal glyph (WARD glass)

	static final int[][] BUBBLE_23 = { FxFrames.Light.BUBBLE_23 };

	/** The crystal glyph: shards of glass converging on the wearer in 0.2 s, then a glass bubble
	 *  flaring round him as the barrier takes over (its "+N" is its own). */
	public static Twist crystalWard(){
		return b -> {
			if (b.minimal || b.join) return;
			final Beat flare = b.split( 1 );
			int n = Math.min( 6, b.left() );
			for (int i = 0; i < n; i++){
				float a = (float)(2 * Math.PI * i / Math.max( 1, n )) + 0.3f;
				float x = b.cx + (float)Math.cos( a ) * 12f, y = b.cy + (float)Math.sin( a ) * 8f;
				FxParticle p = b.mat( x, y, FxBudget.P2 );
				if (p == null) break;
				p.look( FxFrames.SHARD_GLASS[i % FxFrames.SHARD_GLASS.length] ).paint( b.c( b.look.glow ) ).vel( (b.cx - x) / 0.2f, (b.cy - y) / 0.2f )
						.life( 0.2f ).alpha( b.a( 1f ), 0.03f, 0.04f ).snap();
			}
			EnchantFx.later( 0.2f, () -> {
				Flash f = flare.flash( true, BUBBLE_23, 0f, flare.cx, flare.cy, flare.look.light, flare.echo ? 0.3f : 0.55f, 0.25f );
				if (f != null) f.envelope( 0.02f, 0.08f );
			} );
		};
	}

	// ------------------------------------------------------------------ Entanglement (VERDANT bark)

	/** Entanglement: two vines creeping up the legs over 0.4 s (the Earthroot's bark rising round the
	 *  feet is PlantFx.bark, called beside it). */
	public static Twist entanglement(){
		return b -> {
			if (b.minimal || b.join) return;
			for (int i = 0; i < 2; i++){
				Flash v = b.flash( false, false, FxFrames.VINE, 0f, b.cx + (i == 0 ? -3f : 3f), b.feet - 2f,
						Ramp.lerp( b.look.glow, Element.NATURE.ramp.stop( 3 ), 0.5f ), 0.95f, 0.9f );
				if (v == null) break;
				v.vel( 0, -6f ).envelope( 0.4f, 0.55f );
				if (i == 1) v.flipHorizontal = true;
			}
		};
	}

	// ------------------------------------------------------------------ Potential (STORM wand charge)

	/** Potential: when it charged the wearer's wands, two twigs of static running from the armour to
	 *  the wand hand (his weapon side) and lilac sparks drawn into it. */
	public static Twist potential( final int wands ){
		return b -> {
			if (wands <= 0 || b.minimal || b.join || b.ds == null) return;
			float hx = sideX( b.ds ), hy = midY( b.ds ) + 1f;
			for (int i = 0; i < 2; i++){
				float x = b.cx + (i == 0 ? -2f : 2f), y = b.cy + (i == 0 ? -2f : 3f);
				float gx = hx - x, gy = hy - y, len = (float)Math.max( 3f, Math.sqrt( gx * gx + gy * gy ) );
				FxParticle p = b.lit( (x + hx) / 2f, (y + hy) / 2f, FxBudget.P1 );
				if (p == null) break;
				p.look( FxFrames.SPARK_3 ).paint( b.c( b.el.glint ) ).life( 0.08f + 0.04f * i ).alpha( b.a( 1f ), 0.04f * i, 0.03f ).snap();
				p.angle = (float)Math.toDegrees( Math.atan2( gy, gx ) ) - 90f;
				p.scale.y = Math.max( 1f, Math.round( len / 3f ) );
			}
			int n = Math.min( 6, b.left() );
			for (int i = 0; i < n; i++){
				float a = (float)(2 * Math.PI * i / Math.max( 1, n ));
				inward( b, true, FxFrames.SPARK_2, b.look.glow, hx + (float)Math.cos( a ) * 8f, hy + (float)Math.sin( a ) * 5f, hx, hy, 0.25f, 0f );
			}
		};
	}

	// ------------------------------------------------------------------ Purity (RADIANCE)

	static final int[][] RING_13 = { FxFrames.RING_13 };
	//a debuff leaving: dark, turning to light as it rises
	static final Ramp CLEANSED = new Ramp( 0x2E1648, 0x6A4A8A, 0xD8C8B0, 0xFFF0C0 );

	/** Purity: a ring of light washing up the wearer from his feet to his head in 0.3 s, and each
	 *  debuff it washed away leaving him as a dark wisp that turns to light as it rises. */
	public static Twist purity( final int removed ){
		return b -> {
			if (b.minimal || b.join) return;
			float rise = Math.max( 6f, b.feet - b.top );
			Flash wash = b.flash( false, true, RING_13, 0f, b.cx, b.feet, b.look.light, 0.75f, 0.3f );
			if (wash != null) wash.vel( 0, -rise / 0.3f ).envelope( 0.03f, 0.15f );
			int n = Math.min( Math.max( 0, removed ), b.left() );
			for (int i = 0; i < n; i++){
				FxParticle p = b.mat( b.cx + (i - (n - 1) / 2f) * 4f, b.cy + FxRandom.Float( -2f, 3f ), FxBudget.P1 );
				if (p == null) break;
				p.look( FxFrames.INK_WISP, 3f ).ramp( b.echo ? b.look.pale : CLEANSED, false ).smooth().vel( FxRandom.Float( -2f, 2f ), -14f )
						.drag( 0.5f ).life( 0.9f + 0.05f * i ).alpha( b.a( 0.9f ), 0.05f, 0.35f );
			}
		};
	}

	// ------------------------------------------------------------------ Satisfying (RADIANCE warm)

	/** Satisfying's strength: weak, always (a meal's worth of warmth). */
	public static final float SATISFYING = 0.2f;

	/** Satisfying: warm gold motes drawn into the wearer's belly (its hunger speck is its own). */
	public static Twist satisfying(){
		return b -> {
			if (b.minimal || b.join) return;
			int n = Math.min( 3, b.left() );
			float tx = b.cx, ty = b.cy + 2f;
			for (int i = 0; i < n; i++){
				float a = (float)(2 * Math.PI * i / Math.max( 1, n )) - 0.6f;
				inward( b, true, FxFrames.MOTE_2, Element.GOLD.ember.stop( i % 2 ), tx + (float)Math.cos( a ) * 9f, ty + (float)Math.sin( a ) * 6f,
						tx, ty, 0.35f + 0.04f * i, 0.3f );
			}
		};
	}

	// ------------------------------------------------------------------ Stone (FORCE stone)

	/** Stone turning a blow (it took at least a point off): the wearer greyed a moment as stone, pebbles
	 *  falling off him and a faint crack at his feet even when weak. */
	public static Twist stone(){
		return b -> {
			if (b.minimal || b.join) return;
			final CharSprite s = b.ds;
			if (s != null && !b.echo) EnchantFx.later( EnchantFx.REACT_AT, () -> {
				if (s.parent != null) s.afterglow( 0x8A847C, 0.15f );
			} );
			int n = Math.min( 3, b.left() );
			for (int i = 0; i < n; i++){
				FxParticle p = b.mat( b.cx + FxRandom.Float( -5f, 5f ), b.cy + FxRandom.Float( -4f, 2f ), FxBudget.P2 );
				if (p == null) break;
				p.look( FxFrames.PEBBLE_2 ).paint( b.c( Element.EARTH.ember.stop( 1 + FxRandom.Int( 3 ) ) ) ).vel( FxRandom.Float( -8f, 8f ), -4f )
						.gravity( 160f ).ground( b.feet + FxRandom.Float( 0f, 2f ), FxParticle.BOUNCE, 0.3f ).spin( 0.12f ).life( 0.6f )
						.alpha( b.a( 1f ), 0, 0.2f );
			}
			if (b.tier == WEAK && !b.onWater( b.cx, b.feet )){
				b.decal( b.cx, b.feet + 1f, FxFrames.CRACK_11[FxRandom.Int( 2 )], ForceFx.CRACK, 0.35f, 1.5f, 0.8f );
			}
		};
	}

	// ------------------------------------------------------------------ Viscosity (VENOM tar)

	static final int TAR = 0x4A5418, TAR_MARK = 0x262A10;

	/** Viscosity deferring a blow (an ABSORB): the blow sinking into tar bubbles clinging to the wearer
	 *  for 1.5 s. */
	public static Twist viscosity(){
		return b -> {
			if (b.minimal || b.join) return;
			int n = Math.min( 4, b.left() );
			for (int i = 0; i < n; i++){
				FxParticle p = b.mat( b.cx + FxRandom.Float( -5f, 5f ), b.cy + FxRandom.Float( -4f, 5f ), FxBudget.P2 );
				if (p == null) break;
				p.look( i % 2 == 0 ? FxFrames.BUBBLE_3 : FxFrames.BUBBLE_2 ).paint( b.c( Ramp.lerp( TAR, b.look.glow, 0.35f ) ) )
						.vel( 0, 0.6f ).life( 1.5f ).alpha( b.a( 0.9f ), 0.05f, 0.4f ).snap();
			}
		};
	}

	/** A deferred blow's tick (Viscosity.DeferedDamage.act, the actor thread): a drop of tar falling off
	 *  the wearer and leaving a dark wet mark where it lands. Any thread. */
	public static void tarTick( final Char wearer ){
		if (wearer == null || wearer.sprite == null) return;
		final CharSprite ws = wearer.sprite;
		Fx.post( () -> {
			if (!shows( ws, null ) || ws.parent == null) return;
			EnchantLook l = EnchantLooks.of( Viscosity.class );
			float x = midX( ws ) + FxRandom.Float( -4f, 4f ), y = midY( ws ) + 2f;
			Beat b = Beat.at( l, x, y, 1 );
			FxParticle p = b.mat( x, y, FxBudget.P2 );
			if (p == null) return;
			p.look( FxFrames.DROP_3 ).paint( Ramp.lerp( TAR, l.glow, 0.3f ) ).vel( 0, 4f ).gravity( 120f ).life( 0.8f ).alpha( 1f, 0, 0.1f );
			if (b.takeDecal()) p.ground( feetY( ws ) + 1f, FxParticle.WET, 0 ).mark( TAR_MARK );
			else p.ground( feetY( ws ) + 1f, FxParticle.DIE, 0 );
		} );
	}

	// ================================================================== curses (the hex over their family)

	/** Dazzling: a local bloom of holy light round the wielder for 0.2 s (the screen's flash is its
	 *  own, gated as every flash). */
	public static Twist dazzling(){
		return b -> {
			if (b.minimal || b.join || b.as == null) return;
			Flash f = b.flash( true, BUBBLE_31, 0f, midX( b.as ), midY( b.as ), Element.HOLY.light, b.echo ? 0.3f : 0.6f, 0.2f );
			if (f != null) f.envelope( 0.03f, 0.08f );
		};
	}

	/** Explosive's warnings: smoke puffing off the weapon side, and once hot, hell sparks spitting off
	 *  it (the bomb's explosion is its own). */
	public static Twist explosive( final boolean hot ){
		return b -> {
			if (b.minimal || b.join || b.as == null) return;
			puff( b, FxFrames.PUFF_S, b.wx, b.wy - 1f, Element.SMOKE.smoke, FxRandom.Float( -2f, 2f ), -8f, 0.7f, 0.5f );
			if (!hot) return;
			int n = Math.min( 4, b.left() );
			for (int i = 0; i < n; i++){
				spark( b, b.wx, b.wy, Element.HELL.ember, (float)(-Math.PI / 2) + FxRandom.Float( -1.1f, 1.1f ), FxRandom.Float( 25f, 50f ), FxBudget.P2 );
			}
		};
	}

	static final float POLES_MEET = 0.12f;

	/** Polarized at its worst for the foe (half again): a red spark and a blue one meeting at the
	 *  hit, and a ring flashing out where they meet. */
	public static Twist polarized(){
		return b -> {
			if (b.minimal || b.join) return;
			Flash ring = b.flash( false, true, RING_13, 0f, b.hx, b.hy, b.c( 0xC070FF ), b.echo ? 0.4f : 0.8f, 0.2f );
			if (ring != null) ring.delay( POLES_MEET ).envelope( 0f, 0.08f );
			float px = -b.dy, py = b.dx;
			for (int i = 0; i < 2; i++){
				float side = i == 0 ? -1f : 1f, x = b.hx + px * 9f * side, y = b.hy + py * 9f * side - 2f;
				inward( b, true, FxFrames.SPARK_2, i == 0 ? 0xFF4040 : 0x4060FF, x, y, b.hx, b.hy, POLES_MEET, 0f );
			}
		};
	}

	/** Polarized at its worst for the wielder (nothing): a grey puff fizzling off the hit and a dull
	 *  grey glint. Any thread. */
	public static void polarFizzle( final Char attacker, final Char defender ){
		if (attacker == null || defender == null || attacker.sprite == null || defender.sprite == null) return;
		final CharSprite as = attacker.sprite, ds = defender.sprite;
		Fx.post( () -> {
			if (!shows( as, ds ) || ds.parent == null) return;
			float x = (midX( as ) + midX( ds )) / 2f, y = midY( ds );
			Beat b = Beat.at( EnchantLooks.of( Polarized.class ), x, y, 2 );
			FxParticle p = b.mat( x, y, FxBudget.P2 );
			if (p != null) p.look( FxFrames.PUFF_S, 0 ).ramp( Element.SMOKE.smoke, false ).smooth().vel( 0, -6f ).life( 0.5f ).alpha( 0.45f, 0.03f, 0.3f );
			FxParticle g = b.mat( x, y, FxBudget.P2 );
			if (g != null) g.look( FxFrames.STAR4_5 ).paint( 0x7C786E ).life( 0.15f ).alpha( 0.8f, 0, 0.08f ).snap();
		} );
	}

	/** Pressurized: steam spurting from the weapon side along the blow (the geyser is its own). */
	public static Twist pressurized(){
		return b -> {
			if (b.minimal || b.join || b.as == null) return;
			int n = Math.min( 3, b.left() );
			for (int i = 0; i < n; i++){
				puff( b, FxFrames.STEAM_CURL, b.wx, b.wy, Element.STEAM.smoke, b.dx * FxRandom.Float( 14f, 24f ) + FxRandom.Float( -3f, 3f ),
						b.dy * 10f - FxRandom.Float( 6f, 12f ), 0.6f, 0.55f );
			}
		};
	}

	/** AntiEntropy: rime on the cells round the wearer it froze (the first RICH_TARGETS dry ones), and
	 *  a lick of fire with a hiss of steam on him. `pos` is his cell. */
	public static Twist antiEntropy( final int pos ){
		return b -> {
			if (b.minimal || b.join) return;
			FxParticle l = b.lit( b.cx, b.feet - 2f, FxBudget.P1 );
			if (l != null) l.look( FxFrames.LICK, 10f ).ramp( b.echo ? b.look.pale : Element.FIRE.ramp, true ).vel( 0, -14f ).life( 0.35f )
					.alpha( b.a( 1f ), 0, 0.15f );
			puff( b, FxFrames.STEAM_CURL, b.cx, b.cy - 2f, Element.STEAM.smoke, FxRandom.Float( -2f, 2f ), -10f, 0.7f, 0.5f );
			if (!full( b ) || Dungeon.level == null) return;
			int made = 0;
			int[] around = com.watabou.utils.PathFinder.NEIGHBOURS8;
			for (int k = 0; k < around.length && made < Math.min( RICH, EXTRA_DECALS ); k++){
				int c = pos + around[k];
				if (!dryFloor( c )) continue;
				FxDecal d = mark( b, cellX( c ), cellFeet( c ) + 1f, FxFrames.RIME[k % 2], Element.FROST.ramp.stop( 2 ), 0.5f, 2f, 1f );
				if (d != null) d.revealCrop( FxDecal.FROM_MIDDLE, 0.3f + 0.05f * made );
				made++;
			}
		};
	}

	// ------------------------------------------------------------------ Bulk (FORCE armour curse)

	private static final int[] bulkIds = new int[GATES];
	private static final float[] bulkAt = { -1, -1, -1, -1, -1, -1, -1, -1 };

	/**
	 * Bulk wedging its wearer in a doorway (Char.speed, often: drawn only when the wearer is seen, at
	 * most once in BULK_GAP s on one wearer): the hex closing on him, two puffs of dust kicked up and
	 * the body squashed a pixel. A moment of no blow, so no sting: it is a slowdown, heard by nothing.
	 * Its old shadow particles are its own. CAP[WEAK] quads. Any thread.
	 */
	public static void bulk( final Char owner ){
		if (owner == null || owner.sprite == null) return;
		final CharSprite os = owner.sprite;
		Fx.post( () -> {
			if (!shows( os, null ) || os.parent == null || !gate( os, bulkIds, bulkAt, BULK_GAP )) return;
			float x = midX( os ), y = midY( os );
			Beat b = Beat.at( EnchantLooks.of( Bulk.class ), x, y, EnchantFx.CAP[WEAK] );
			b.feet = feetY( os );
			HexOverlay.play( b );
			for (int i = 0; i < 2; i++){
				if (puff( b, FxFrames.PUFF_S, x + (i == 0 ? -4f : 4f), b.feet - 1f, Element.EARTH.smoke, i == 0 ? -6f : 6f, -3f, 0.5f, 0.45f ) == null) break;
			}
			b.moveNow( os, Beat.SINK, 1 );
		} );
	}

	/** Corrosion: an acid etch on the cells round the wearer (the first RICH_TARGETS dry ones; the
	 *  nine black splashes are its own) and bubbles fizzing off him. `pos` is his cell. */
	public static Twist corrosion( final int pos ){
		return b -> {
			if (b.minimal || b.join) return;
			int n = Math.min( 4, b.left() );
			for (int i = 0; i < n; i++){
				FxParticle p = b.mat( b.cx + FxRandom.Float( -5f, 5f ), b.cy + FxRandom.Float( -3f, 4f ), FxBudget.P2 );
				if (p == null) break;
				p.look( FxFrames.BUBBLE_2 ).paint( b.c( Element.ACID.ramp.stop( 1 ) ) ).vel( FxRandom.Float( -2f, 2f ), -FxRandom.Float( 7f, 11f ) )
						.life( FxRandom.Float( 0.5f, 0.7f ) ).alpha( b.a( 1f ), 0.05f, 0.1f ).snap();
			}
			if (!full( b ) || Dungeon.level == null) return;
			int made = 0;
			int[] around = com.watabou.utils.PathFinder.NEIGHBOURS8;
			for (int k = 0; k < around.length && made < Math.min( RICH, EXTRA_DECALS ); k++){
				int c = pos + around[(k * 3) % around.length];
				if (!dryFloor( c )) continue;
				mark( b, cellX( c ), cellFeet( c ) + 1f, FxFrames.ETCH, Element.ACID.ramp.stop( 3 ), 0.5f, 2.5f, 1f );
				made++;
			}
		};
	}

	/** Stench: the wearer heaving a puff of poison smoke, and the hex's motes drawn into the gas. */
	public static Twist stench(){
		return b -> {
			if (b.minimal || b.join) return;
			puff( b, FxFrames.PUFF_M, b.cx, b.cy, Element.POISON.smoke, 0, -5f, 1f, 0.55f );
			int n = Math.min( 2, b.left() );
			for (int i = 0; i < n; i++){
				float a = (float)(Math.PI * i) + 0.4f;
				inward( b, false, FxFrames.MOTE_2, Element.VOID.ramp.stop( 1 ), b.cx + (float)Math.cos( a ) * 10f, b.cy - 3f + (float)Math.sin( a ) * 5f,
						b.cx, b.cy - 2f, 0.4f, 0.3f );
			}
		};
	}
}
