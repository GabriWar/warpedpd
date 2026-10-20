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
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxOrbit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantState;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.noosa.Game;
import com.watabou.utils.PointF;
import com.watabou.utils.RectF;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The looks of the states (StateLooks), one class each. Numbers are the area's spec
 * (docs/fx/characters-combat.md): what each pours, how often, how long its pieces live, and so
 * how many quads it keeps alive at most.
 */
final class Looks {

	private Looks(){}

	// ================================================================== fire

	/**
	 * A burning body (BURNING, also SoulFire's and FiendBurning's; HALOMETHANEBURNING in its blue,
	 * smokeless): tongues of its element licking up the lower 60% of the sprite every 0.06 s, as
	 * light; it catches with six middling and large tongues in 0.15 s and its light leaping from 0
	 * to 0.6 in 0.1 s, settling to 0.5 (the BURNING sound's frame). A puff of smoke off the head
	 * every 0.35 s (under the night's tint), an ember every 0.4 s, a 15 px glow on the body and a
	 * 23 px pool at the feet, flickering as a torch, the glow thrown down any water below. Put
	 * out: three puffs; in water (not flying) a hiss of steam and a ring. At most CAP quads (the
	 * catching's six aside). Only the first rich() burning bodies get the smoke, embers and light:
	 * RICH, fewer as the kit's light cap falls (a quarter of it), so their held lights leave room.
	 */
	static final class Burning extends StateLooks.Look {

		static final float TONGUE_EVERY = 0.06f, SMOKE_EVERY = 0.35f, EMBER_EVERY = 0.4f;
		static final int IGNITION = 6;
		static final float IGNITION_FOR = 0.15f;
		/** The light's level: up to PEAK in RISE s as it catches, then BURN by SETTLE s. */
		static final float PEAK = 0.6f, BURN = 0.5f, RISE = 0.1f, SETTLE = 0.3f;
		static final int GLOW = 15, POOL = 23;
		/** Quads a burning body keeps alive at most, and the bodies that get every layer. */
		static final int CAP = 12, RICH = 6;
		static final AtomicInteger rich = new AtomicInteger();

		/** How many burning bodies get every layer now: RICH, at most a quarter of the light cap. */
		static int rich(){
			return Math.min( RICH, FxBudget.lightCap() / 4 );
		}

		final Element el;
		final boolean smoky;
		private boolean isRich;
		private FxEmitter tongues, smoke, embers;
		private HeldLight glow, pool;

		Burning( CharSprite s, Element el, boolean smoky ){
			super( s );
			this.el = el;
			this.smoky = smoky && el.smoke != null;
		}

		@Override
		void start(){
			tongues = pour( true, CombatFactories.tongue( el ), TONGUE_EVERY, 0.15f, 0.4f, 0.7f, 0.6f );
			once( true, CombatFactories.ignition( el ), IGNITION_FOR / IGNITION, IGNITION, 0.2f, 0.45f, 0.6f, 0.5f );
			//one of the rich few, if there is room among them
			isRich = rich.incrementAndGet() <= rich();
			if (!isRich) rich.decrementAndGet();
			if (isRich){
				if (smoky) smoke = pour( false, CombatFactories.smoke( el ), SMOKE_EVERY, 0.3f, 0f, 0.4f, 0.2f );
				embers = pour( true, CombatFactories.ember( el ), EMBER_EVERY, 0.2f, 0.3f, 0.6f, 0.5f );
				glow = new HeldLight( false, GLOW, el.light, FxLight.TORCH, true );
				pool = new HeldLight( true, POOL, el.light, FxLight.TORCH, false );
			}
		}

		/** Its light's level `t` s after it caught. */
		static float level( float t ){
			if (t < RISE) return PEAK * t / RISE;
			if (t < SETTLE) return PEAK + (BURN - PEAK) * (t - RISE) / (SETTLE - RISE);
			return BURN;
		}

		@Override
		boolean step( float dt ){
			if (stopping) return false;
			shown( tongues );
			shown( smoke );
			shown( embers );
			if (glow != null){
				float l = level( age );
				glow.update( s, 0, 0, l, dt );
				pool.update( s, 0, s.height() / 2f - 1f, l, dt );
			}
			return true;
		}

		@Override
		void stop(){
			if (!s.visible) return;
			float x = cx(), y = cy();
			if (smoky) FxRecipes.puff( x, y - s.height() / 4f, el, 3 );
			Char ch = s.ch;
			Level level = Dungeon.level;
			if (ch != null && level != null && !ch.flying && ch.pos >= 0 && ch.pos < level.length() && level.water[ch.pos]){
				//doused: the flames on it go out at once, in a hiss of steam and a ring on the water
				//it stepped into (its cell's, wherever the sprite is mid-step)
				douse( tongues, x, y );
				douse( embers, x, y );
				PointF c = DungeonTilemap.tileCenterToWorld( ch.pos );
				float wy = c.y + DungeonTilemap.SIZE / 2f - 2f;
				FxRecipes.steam( c.x, wy, FxRandom.IntRange( 6, 8 ) );
				WaterFX.ring( c.x, wy, FxRandom.chance( 0.5f ) ? WaterFX.S : WaterFX.M );
			}
		}

		/** How near the body's middle a flame goes out with it, px (the ones left behind on dry
		 *  ground as it walked burn out as they would). */
		static final float DOUSED_X = 10f, DOUSED_Y = 14f;

		//every flame of an emitter on the body put out now (a one-off, not a frame loop)
		static void douse( FxEmitter e, float cx, float cy ){
			if (e == null) return;
			for (com.watabou.noosa.Gizmo g : e.membersView()){
				if (!(g instanceof com.watabou.noosa.Visual) || !g.alive) continue;
				com.watabou.noosa.Visual v = (com.watabou.noosa.Visual) g;
				if (Math.abs( v.x - cx ) <= DOUSED_X && Math.abs( v.y - cy ) <= DOUSED_Y) g.kill();
			}
		}

		@Override
		void release(){
			stopPour( tongues );
			stopPour( smoke );
			stopPour( embers );
			tongues = smoke = embers = null;
			if (glow != null) glow.putOut( 0.25f );
			if (pool != null) pool.putOut( 0.25f );
			if (isRich){
				isRich = false;
				rich.decrementAndGet();
			}
		}
	}

	/** Heatstroke: a wisp of steam off the head every 0.4 s and a heat ray shimmering up every
	 *  0.8 s (at most 6 quads). */
	static final class Heat extends StateLooks.Look {

		static final float STEAM_EVERY = 0.4f, RAY_EVERY = 0.8f;
		private FxEmitter steam, rays;

		Heat( CharSprite s ){
			super( s );
		}

		@Override
		void start(){
			steam = pour( false, FxFactories.steam(), STEAM_EVERY, 0.3f, 0f, 0.4f, 0.3f );
			rays = pour( true, CombatFactories.HEAT_RAY, RAY_EVERY, 0.2f, 0.2f, 0.6f, 0.5f );
		}

		@Override
		boolean step( float dt ){
			shown( steam );
			shown( rays );
			return !stopping;
		}

		@Override
		void release(){
			stopPour( steam );
			stopPour( rays );
			steam = rays = null;
		}
	}

	// ================================================================== cold

	/**
	 * Frozen (the ice's tint is IceBlock's): three or four pixels of frost on the sprite's edge
	 * where it froze, still; and every GLINT_EVERY s a glint sweeping across it in GLINT_FOR s, a
	 * short diagonal drawn added only where the sprite is. 4 quads at most.
	 */
	static final class Frozen extends StateLooks.Look {

		static final float GLINT_EVERY = 2f, GLINT_FOR = 0.25f, FROST_IN = 0.3f;
		static final float GLINT_ALPHA = 0.75f;
		static final int FROST_MIN = 3, FROST_MAX = 4;

		//the frost's pixels, in the frame's own pixels
		final int[] fx = new int[FROST_MAX], fy = new int[FROST_MAX];
		int frost;
		private final Quad dot = new Quad( false ).show( FxFrames.MOTE_1 );
		private final Quad glint = new Quad( false ).show( FxFrames.MOTE_1 );
		private float glintPhase;

		Frozen( CharSprite s ){
			super( s );
		}

		@Override
		void start(){
			frost = edges( s, fx, fy, FxRandom.IntRange( FROST_MIN, FROST_MAX ) );
			glintPhase = FxRandom.Float( GLINT_EVERY );
		}

		@Override
		void drawOver(){
			RectF f = s.frameNow();
			SmartTexture t = s.texture;
			if (f == null || t == null) return;
			int fw = Math.round( f.width() * t.width );
			float a = Math.min( 1f, age / FROST_IN );
			for (int i = 0; i < frost; i++){
				dot.draw( s, px( fw, fx[i] ), py( fy[i] ), CombatFactories.FROST, 0.9f * a, false, 1, false );
			}
			float g = ((age + glintPhase) % GLINT_EVERY) / GLINT_FOR;
			if (g >= 1f || t.bitmap == null) return;
			int fh = Math.round( f.height() * t.height );
			int x0 = Math.round( f.left * t.width ), y0 = Math.round( f.top * t.height );
			//the sweep's line x + y = c crosses the frame; three pixels of it about its middle
			int c = Math.round( g * (fw + fh) );
			int mid = c / 2;
			for (int k = -1; k <= 1; k++){
				int x = mid + k, y = c - x;
				if (x < 0 || y < 0 || x >= fw || y >= fh) continue;
				if ((t.getPixel( x0 + x, y0 + y ) >>> 24) < 128) continue;
				glint.draw( s, px( fw, x ), py( y ), 0xFFFFFF, GLINT_ALPHA, true, 1, false );
			}
		}

		//a frame pixel's middle on the screen, mirrored with the sprite
		float px( int fw, int x ){
			int mx = s.flipHorizontal ? fw - 1 - x : x;
			return s.x + (mx + 0.5f) * s.scale.x;
		}

		float py( int y ){
			return s.y + (y + 0.5f) * s.scale.y;
		}

		@Override
		void release(){
			dot.release();
			glint.release();
		}
	}

	/**
	 * Up to n pixels on the edge of the sprite's frame now (solid, a clear pixel beside it), in its
	 * upper 70%, picked at random: where frost forms. Their frame coordinates go to xs, ys; returns
	 * how many were found. Render thread (it reads the sprite's texture).
	 */
	static int edges( CharSprite s, int[] xs, int[] ys, int n ){
		RectF f = s.frameNow();
		SmartTexture t = s.texture;
		if (f == null || t == null || t.bitmap == null) return 0;
		int x0 = Math.round( f.left * t.width ), y0 = Math.round( f.top * t.height );
		int fw = Math.round( f.width() * t.width ), fh = Math.round( f.height() * t.height );
		int found = 0, seen = 0;
		int top = Math.max( 1, Math.round( fh * 0.7f ) );
		for (int y = 0; y < top; y++){
			for (int x = 0; x < fw; x++){
				if (!solid( t, x0, y0, fw, fh, x, y )) continue;
				if (solid( t, x0, y0, fw, fh, x - 1, y ) && solid( t, x0, y0, fw, fh, x + 1, y )
						&& solid( t, x0, y0, fw, fh, x, y - 1 ) && solid( t, x0, y0, fw, fh, x, y + 1 )) continue;
				//reservoir sampling: each edge pixel as likely as any other
				seen++;
				if (found < n){
					xs[found] = x;
					ys[found] = y;
					found++;
				} else {
					int k = FxRandom.Int( seen );
					if (k < n){
						xs[k] = x;
						ys[k] = y;
					}
				}
			}
		}
		return found;
	}

	private static boolean solid( SmartTexture t, int x0, int y0, int fw, int fh, int x, int y ){
		if (x < 0 || y < 0 || x >= fw || y >= fh) return false;
		return (t.getPixel( x0 + x, y0 + y ) >>> 24) >= 128;
	}

	/**
	 * Chilled, or in hypothermia (the chill's tint is StateLooks'): a pixel of frost falling over the
	 * body every 0.25 s (hypothermia: rising every 0.2 s), and - for creatures that breathe, not the
	 * inorganic nor the undead - a puff of breath from the head's front every 1.2-1.8 s (hypothermia:
	 * every 0.8-1.2 s, thicker); hypothermia shivers, a pixel either way at 8 Hz for 0.2 s every 2 s,
	 * on the screen only (CharSprite.shiver). About 6 quads.
	 */
	static final class Cold extends StateLooks.Look {

		static final float FLAKE_EVERY = 0.25f, RISING_EVERY = 0.2f;
		static final float BREATH_MIN = 1.2f, BREATH_MAX = 1.8f, HYPO_BREATH_MIN = 0.8f, HYPO_BREATH_MAX = 1.2f;
		static final float BREATH_ALPHA = 0.35f, HYPO_BREATH_ALPHA = 0.45f, BREATH_LIFE = 0.9f;
		static final int BREATH = 0xE8F4FF;
		static final float SHIVER_EVERY = 2f, SHIVER_FOR = 0.2f, SHIVER_HZ = 8f;

		final boolean hypo;
		private final boolean breathes;
		private float breathIn;
		private FxEmitter flakes;

		//breathes: read off the character beforehand (StateLooks.readBuffs), outside the looks' lock
		Cold( CharSprite s, boolean hypo, boolean breathes ){
			super( s );
			this.hypo = hypo;
			this.breathes = breathes;
		}

		@Override
		void start(){
			breathIn = FxRandom.Float( 0.3f, 1f );
			flakes = hypo ? pour( false, CombatFactories.RISING_FLAKE, RISING_EVERY, 0.1f, 0.2f, 0.8f, 0.8f )
					: pour( false, CombatFactories.FLAKE, FLAKE_EVERY, 0.1f, 0f, 0.8f, 0.6f );
		}

		/** The shiver's offset `t` s in, px: +-1 at SHIVER_HZ for SHIVER_FOR s every SHIVER_EVERY s. */
		static int shiver( float t ){
			float in = t % SHIVER_EVERY;
			if (in >= SHIVER_FOR) return 0;
			return ((int)(in * SHIVER_HZ * 2) % 2 == 0) ? 1 : -1;
		}

		@Override
		boolean step( float dt ){
			if (stopping) return false;
			shown( flakes );
			if (hypo) s.shiver( shiver( age ) );
			if (breathes && (breathIn -= dt) <= 0){
				breathIn = hypo ? FxRandom.Float( HYPO_BREATH_MIN, HYPO_BREATH_MAX ) : FxRandom.Float( BREATH_MIN, BREATH_MAX );
				if (s.visible) breathe();
			}
			return true;
		}

		//a puff of breath from the front of the head, drifting out and up
		private void breathe(){
			breath( s, hypo );
		}

		/** A puff of breath from the front of a sprite's head, drifting out and up: the chill's, a
		 *  thicker one in hypothermia, and a hero's in freezing air (ColdBreath). */
		static void breath( CharSprite s, boolean hypo ){
			float cx = s.x + s.width() / 2f, cy = s.y + s.height() / 2f;
			FxEmitter air = Fx.air();
			if (air == null || !FxBudget.keep( FxBudget.P3, cx, cy )) return;
			float dir = s.flipHorizontal ? -1f : 1f;
			float x = cx + dir * s.width() * 0.3f, y = s.y + s.height() * 0.3f;
			CombatFactories.make( air, x, y ).look( FxFrames.EDGE_WISP[FxRandom.Int( FxFrames.EDGE_WISP.length )] )
					.paint( BREATH ).smooth().life( BREATH_LIFE ).alpha( hypo ? HYPO_BREATH_ALPHA : BREATH_ALPHA, 0.1f, 0.5f )
					.vel( dir * FxRandom.Float( 6f, 10f ), -FxRandom.Float( 1f, 3f ) ).drag( 1.5f ).priority( FxBudget.P3 );
		}

		@Override
		void release(){
			stopPour( flakes );
			flakes = null;
			if (hypo) s.shiver( 0 );
		}
	}

	// ================================================================== shadow

	/** Darkened (DarkBlock dims and tints it): a wisp of shadow rising off it every 0.7 s (two
	 *  at once, about). */
	static final class Darkened extends StateLooks.Look {

		static final float WISP_EVERY = 0.7f;
		private FxEmitter wisps;

		Darkened( CharSprite s ){
			super( s );
		}

		@Override
		void start(){
			wisps = pour( false, CombatFactories.INK_WISP, WISP_EVERY, 0.2f, 0.2f, 0.6f, 0.6f );
		}

		@Override
		boolean step( float dt ){
			shown( wisps );
			return !stopping;
		}

		@Override
		void release(){
			stopPour( wisps );
			wisps = null;
		}
	}

	/**
	 * Marked: wisps of shadow rising off it every 0.3 s (1.5 s each, so about five), into a faint
	 * rune (RUNE_3, its glyph changing every 2 s) over the head, A070E0 at 0.4 breathing +-12% at
	 * 0.5 Hz (a persistent light's most), added.
	 */
	static final class Marked extends StateLooks.Look {

		static final float WISP_EVERY = 0.3f, RUNE_ALPHA = 0.4f, RUNE_HZ = 0.5f, RUNE_EVERY = 2f;
		static final int RUNE = 0xA070E0;
		private FxEmitter wisps;
		private final Quad rune = new Quad( false );
		private int glyph;

		Marked( CharSprite s ){
			super( s );
		}

		@Override
		void start(){
			wisps = pour( false, CombatFactories.INK_WISP, WISP_EVERY, 0.25f, 0.1f, 0.5f, 0.7f );
			glyph = FxRandom.Int( FxFrames.RUNE_3.length );
		}

		/** The rune's alpha `t` s in: within 12% of RUNE_ALPHA, at RUNE_HZ. */
		static float runeAlpha( float t ){
			return RUNE_ALPHA * (1f + 0.12f * (float)Math.sin( 2 * Math.PI * RUNE_HZ * t ));
		}

		@Override
		boolean step( float dt ){
			shown( wisps );
			return !stopping;
		}

		@Override
		void drawOver(){
			int g = (glyph + (int)(age / RUNE_EVERY)) % FxFrames.RUNE_3.length;
			rune.show( FxFrames.RUNE_3[g] );
			float a = runeAlpha( age ) * Math.min( 1f, age / 0.4f );
			rune.draw( s, cx(), s.y - 4f, RUNE, a, true, 1, false );
		}

		@Override
		void release(){
			stopPour( wisps );
			wisps = null;
			rune.release();
		}
	}

	// ================================================================== light

	/** Healing: a pool of its light at the feet (9CFFB0, 0.12-0.2 breathing at 1 Hz) and its
	 *  specks rising every 0.5 s. */
	static final class Healing extends StateLooks.Look {

		static final float SPECK_EVERY = 0.5f, LOW = 0.12f, HIGH = 0.2f, HZ = 1f;
		static final int POOL = 23;
		private FxEmitter specks;
		private HeldLight pool;

		Healing( CharSprite s ){
			super( s );
		}

		@Override
		void start(){
			specks = pour( false, Speck.factory( Speck.HEALING ), SPECK_EVERY, 0f, 0f, 1f, 1f );
			pool = new HeldLight( true, POOL, Element.HEAL.light, FxLight.STEADY, false );
		}

		/** The pool's alpha `t` s in: LOW to HIGH and back at HZ. */
		static float alpha( float t ){
			return LOW + (HIGH - LOW) * (0.5f + 0.5f * (float)Math.sin( 2 * Math.PI * HZ * t ));
		}

		@Override
		boolean step( float dt ){
			if (stopping) return false;
			shown( specks );
			pool.update( s, 0, s.height() / 2f - 1f, alpha( age ) / FxLight.POOL_CAP, dt );
			return true;
		}

		@Override
		void release(){
			stopPour( specks );
			specks = null;
			pool.putOut( 0.3f );
		}
	}

	/** Glowing (GlowBlock warms it): a 23 px glow behind it, 0.25 breathing 8%, and a mote of
	 *  light rising off it every 0.5 s. */
	static final class Glowing extends StateLooks.Look {

		static final float MOTE_EVERY = 0.5f, ALPHA = 0.25f;
		private final Quad glow = new Quad( true ).show( FxFrames.Light.GLOW_23 );
		private FxEmitter motes;

		Glowing( CharSprite s ){
			super( s );
		}

		@Override
		void start(){
			motes = pour( true, CombatFactories.riser( Element.HOLY.ember.stop( 0 ), true ), MOTE_EVERY, 0.2f, 0.2f, 0.6f, 0.6f );
		}

		@Override
		boolean step( float dt ){
			shown( motes );
			return !stopping;
		}

		@Override
		void drawBehind(){
			float a = ALPHA * (1f + 0.08f * (float)Math.sin( 2 * Math.PI * 0.5f * age )) * Math.min( 1f, age / 0.3f );
			glow.draw( s, Math.round( cx() ), Math.round( cy() ), Element.HOLY.light, a, true, 1, false );
		}

		@Override
		void release(){
			stopPour( motes );
			motes = null;
			glow.release();
		}
	}

	/** Illuminated (a magical light: Glowing, Eyeing, Guiding Light, a light that is no flame): a
	 *  steady 47 px glow on it, 0.18 breathing, over the night's tint, with a transient light's
	 *  share by day (Fx.DAY_TRANSIENT: about 0.15, as plain as the old halo, where a persistent
	 *  share left a tenth). Not shown while a torch lights it (TORCHLIT). */
	static final class Illuminated extends StateLooks.Look {

		static final int SIZE = 47, COLOR = 0xFFDDCC;
		static final float ALPHA = 0.18f;
		private final StateLooks owner;
		private HeldLight glow;

		Illuminated( CharSprite s, StateLooks owner ){
			super( s );
			this.owner = owner;
		}

		@Override
		void start(){
			glow = new HeldLight( false, SIZE, COLOR, FxLight.BREATH, false, Fx.DAY_TRANSIENT );
		}

		@Override
		boolean step( float dt ){
			if (stopping) return false;
			float l = owner.has( CharSprite.State.TORCHLIT ) ? 0f : ALPHA / FxLight.GLOW_CAP;
			glow.update( s, 0, 0, l * Math.min( 1f, age / 0.5f ), dt );
			return true;
		}

		@Override
		void release(){
			glow.putOut( 0.5f );
		}
	}

	/**
	 * Lit by a torch (Light.flame, the town guard's): a 47 px glow at the torch hand, 0.26, and a
	 * 63 px pool at the feet, 0.26, flickering as a torch, a persistent light's share by day, the
	 * glow thrown down any water below; a spark hopping off the flame every 0.5 s. 3-4 quads and
	 * two lights. (The spec's 0.22 glow and 31 px pool at 0.18 read as a faint halo in the night
	 * previews, not a torch: fx-previews/characters-combat/index.md.)
	 */
	static final class Torchlit extends StateLooks.Look {

		static final int GLOW = 47, POOL = 63;
		static final float GLOW_ALPHA = 0.26f, POOL_ALPHA = 0.26f, SPARK_EVERY = 0.5f;
		static final int COLOR = 0xFFA850;
		private HeldLight glow, pool;
		private FxEmitter sparks;

		Torchlit( CharSprite s ){
			super( s );
		}

		@Override
		void start(){
			glow = new HeldLight( false, GLOW, COLOR, FxLight.TORCH, true );
			pool = new HeldLight( true, POOL, COLOR, FxLight.TORCH, false );
			sparks = pour( true, CombatFactories.TORCH_SPARK, SPARK_EVERY, 0.5f, 0.3f, 0.2f, 0.1f );
		}

		//the torch hand's offset off the middle: in front, a little above
		float handX(){
			return (s.flipHorizontal ? -1f : 1f) * Math.max( 2f, s.width() / 2f - 3f );
		}

		@Override
		boolean step( float dt ){
			if (stopping) return false;
			float in = Math.min( 1f, age / 0.3f );
			float hx = handX();
			glow.update( s, hx, -1f, in * GLOW_ALPHA / FxLight.GLOW_CAP, dt );
			pool.update( s, 0, s.height() / 2f - 1f, in * POOL_ALPHA / FxLight.POOL_CAP, dt );
			float l = (hx + s.width() / 2f) / Math.max( 1f, s.width() ) - 0.1f;
			place( sparks, l, 0.3f, 0.2f, 0.1f );
			shown( sparks );
			return true;
		}

		@Override
		void release(){
			if (glow != null) glow.putOut( 0.3f );
			if (pool != null) pool.putOut( 0.3f );
			stopPour( sparks );
			sparks = null;
		}
	}

	/** An aura (a champion's, invulnerability's; its Flare is the sprite's): a 23 px pool of its
	 *  colour at the feet, 0.5 breathing. */
	static final class Aura extends StateLooks.Look {

		static final int POOL = 23;
		static final float LEVEL = 0.5f;
		private HeldLight pool;

		Aura( CharSprite s ){
			super( s );
		}

		@Override
		void start(){
			pool = new HeldLight( true, POOL, s.auraColor(), FxLight.BREATH, false );
		}

		@Override
		void again(){
			if (pool != null) pool.putOut( 0.1f );
			start();
		}

		@Override
		boolean step( float dt ){
			if (stopping) return false;
			pool.update( s, 0, s.height() / 2f - 1f, LEVEL * Math.min( 1f, age / 0.3f ), dt );
			return true;
		}

		@Override
		void release(){
			if (pool != null) pool.putOut( 0.3f );
		}
	}

	/** Levitating: a jet of air puffing under it every 0.05 s (0.5 s each: 10 at once). */
	static final class Levitating extends StateLooks.Look {

		static final float JET_EVERY = 0.05f;
		private FxEmitter jets;

		Levitating( CharSprite s ){
			super( s );
		}

		@Override
		void start(){
			jets = pour( false, Speck.factory( Speck.JET ), JET_EVERY, 0f, 0f, 1f, 1f );
		}

		@Override
		boolean step( float dt ){
			shown( jets );
			return !stopping;
		}

		@Override
		void release(){
			stopPour( jets );
			jets = null;
		}
	}

	/** Electric: a crackle of static over it every 0.3 s and an 11 px glow, A8C8FF at 0.2. */
	static final class Electric extends StateLooks.Look {

		static final float CRACKLE_EVERY = 0.3f, ALPHA = 0.2f;
		static final int GLOW = 11, COLOR = 0xA8C8FF;
		private FxEmitter crackle;
		private HeldLight glow;

		Electric( CharSprite s ){
			super( s );
		}

		@Override
		void start(){
			crackle = pour( true, SparkParticle.STATIC, CRACKLE_EVERY, 0f, 0f, 1f, 1f );
			glow = new HeldLight( false, GLOW, COLOR, FxLight.BREATH, false, Fx.DAY_TRANSIENT );
		}

		@Override
		boolean step( float dt ){
			if (stopping) return false;
			shown( crackle );
			glow.update( s, 0, 0, ALPHA / FxLight.GLOW_CAP, dt );
			return true;
		}

		@Override
		void release(){
			stopPour( crackle );
			crackle = null;
			if (glow != null) glow.putOut( 0.2f );
		}
	}

	// ================================================================== matter on the body

	/**
	 * Buttered (ButterEffect tints it): a glint of light sliding across it every 1.5 s in 0.25 s, a
	 * short diagonal drawn added only where the sprite is, a pixel either way with each step it
	 * takes; and a drip of butter (2x2, F0E691) from its bottom every 0.8 s, falling 6 px to splat.
	 */
	static final class Butter extends StateLooks.Look {

		static final int COLOR = 0xF0E691, SHINE = 0xFFF8C8;
		static final float SHINE_EVERY = 1.5f, SHINE_FOR = 0.25f, SHINE_ALPHA = 0.6f, DRIP_EVERY = 0.8f;
		private final Quad shine = new Quad( false ).show( FxFrames.MOTE_1 );
		private float dripIn = 0.4f;
		private int jiggle;
		private int lastCell = -1;

		Butter( CharSprite s ){
			super( s );
		}

		@Override
		boolean step( float dt ){
			if (stopping) return false;
			//each step it takes shifts the shine a pixel the other way
			int cell = s.ch != null ? s.ch.pos : -1;
			if (lastCell != -1 && cell != lastCell) jiggle = jiggle >= 0 ? -1 : 1;
			lastCell = cell;
			if ((dripIn -= dt) <= 0){
				dripIn = DRIP_EVERY;
				if (s.visible) drip();
			}
			return true;
		}

		private void drip(){
			FxEmitter air = Fx.air();
			if (air == null || !FxBudget.keep( FxBudget.P3, cx(), cy() )) return;
			float x = Math.round( cx() + FxRandom.Float( -s.width() / 4f, s.width() / 4f ) ), y = feet() - 2f;
			CombatFactories.make( air, x, y ).look( FxFrames.MOTE_2 ).paint( COLOR ).vel( 0, 4f ).gravity( 60f )
					.ground( y + 6f, FxParticle.SPLAT, 0 ).mark( COLOR ).life( 1f ).snap().priority( FxBudget.P3 );
		}

		@Override
		void drawOver(){
			float g = (age % SHINE_EVERY) / SHINE_FOR;
			RectF f = s.frameNow();
			SmartTexture t = s.texture;
			if (g >= 1f || f == null || t == null || t.bitmap == null) return;
			int fw = Math.round( f.width() * t.width ), fh = Math.round( f.height() * t.height );
			int x0 = Math.round( f.left * t.width ), y0 = Math.round( f.top * t.height );
			int c = Math.round( g * (fw + fh) ) + jiggle;
			int mid = c / 2;
			for (int k = -1; k <= 1; k++){
				int x = mid + k, y = c - x;
				if (x < 0 || y < 0 || x >= fw || y >= fh) continue;
				if ((t.getPixel( x0 + x, y0 + y ) >>> 24) < 128) continue;
				int mx = s.flipHorizontal ? fw - 1 - x : x;
				shine.draw( s, s.x + (mx + 0.5f) * s.scale.x, s.y + (y + 0.5f) * s.scale.y, SHINE, SHINE_ALPHA, true, 1, false );
			}
		}

		@Override
		void release(){
			shine.release();
		}
	}

	/**
	 * Poisoned, corroded, oozed: every 0.8 s a bubble (BUBBLE_3) from the top of the sprite and a
	 * smaller one (BUBBLE_2) 0.3 s after it, each rising 8 px and popping into POP_5 - poison's
	 * B8F060; acid's D8F048 with a FFB040 sizzle as it pops; ooze's 1A1A22 with a white glint riding
	 * the bigger one. Two bubbles on a body at a time, at most.
	 */
	static final class Bubbles extends StateLooks.Look {

		static final int POISON = 0, ACID = 1, OOZE = 2;
		static final int[] COLORS = { 0xB8F060, 0xD8F048, 0x1A1A22 };
		static final int SIZZLE = 0xFFB040, GLINT = 0xFFFFFF;
		static final float EVERY = 0.8f, SECOND = 0.3f, RISE = 8f, LIFE = 0.6f;

		final int kind;
		private float next, secondIn;

		Bubbles( CharSprite s, int kind ){
			super( s );
			this.kind = kind;
		}

		@Override
		void start(){
			next = FxRandom.Float( 0.2f, 0.8f );
		}

		@Override
		boolean step( float dt ){
			if (stopping) return false;
			if ((next -= dt) <= 0){
				next = EVERY;
				secondIn = SECOND;
				if (s.visible) bubble( true );
			}
			if (secondIn > 0 && (secondIn -= dt) <= 0 && s.visible) bubble( false );
			return true;
		}

		//the beat's bubble (big: always made), or the smaller one after it (thinned as dressing)
		private void bubble( boolean big ){
			FxEmitter air = Fx.air();
			if (air == null) return;
			if (big) FxBudget.keep( FxBudget.P0 );
			else if (!FxBudget.keep( FxBudget.P2, cx(), cy() )) return;
			float x = Math.round( cx() + FxRandom.Float( -s.width() / 5f, s.width() / 5f ) ), y = s.y + 2f;
			CombatFactories.make( air, x, y ).look( big ? FxFrames.BUBBLE_3 : FxFrames.BUBBLE_2 ).paint( COLORS[kind] )
					.vel( 0, -RISE / LIFE ).life( LIFE ).alpha( 1f, 0.1f, 0f ).snap().fate( POPS[kind] )
					.priority( big ? FxBudget.P1 : FxBudget.P2 );
			if (big && kind == OOZE){
				CombatFactories.make( air, x - 1f, y - 1f ).look( FxFrames.MOTE_1 ).paint( GLINT ).vel( 0, -RISE / LIFE )
						.life( LIFE ).alpha( 0.9f, 0.1f, 0f ).snap().priority( FxBudget.P2 );
			}
		}

		//a bubble's end: popping (POP_5 for a tenth of a second), acid sizzling as it does
		static final FxParticle.Fate[] POPS = { pop( false ), pop( true ), pop( false ) };

		private static FxParticle.Fate pop( final boolean sizzle ){
			return ( p, landed ) -> {
				FxEmitter air = Fx.air();
				if (air == null) return;
				CombatFactories.make( air, p.px, p.py - 1f ).look( FxFrames.POP_5 ).paint( p.colorNow() ).life( 0.1f )
						.alpha( 0.9f, 0, 0.05f ).snap().priority( FxBudget.P2 );
				if (sizzle){
					FxEmitter glow = Fx.glowAir();
					if (glow != null && FxBudget.keep( FxBudget.P3 )){
						CombatFactories.make( glow, p.px, p.py - 1f ).look( FxFrames.PLUS_3 ).paint( SIZZLE ).light( true )
								.life( 0.08f ).alpha( 0.9f, 0, 0.04f ).snap().priority( FxBudget.P3 );
					}
				}
			};
		}
	}

	// ================================================================== nature

	/**
	 * Rooted: vines (VINE, 3 frames at 3 fps) at the feet, grown bottom-up over 0.2 s, drawn over
	 * them; a leaf flicked up off them every 1.5 s; withdrawn over 0.15 s as the roots let go.
	 * 1 picture and 1 leaf.
	 */
	static final class Rooted extends StateLooks.Look {

		static final float GROW = 0.2f, WITHDRAW = 0.15f, LEAF_EVERY = 1.5f, FPS = 3f;
		static final int VINE = 0x4E9A30;
		private final Quad vine = new Quad( false ).show( FxFrames.VINE[0] );
		private float leafIn = 0.6f, stoppedAt = -1;

		Rooted( CharSprite s ){
			super( s );
		}

		/** How much of the vine shows `t` s in, or `since` s after the roots let go (-1: not yet). */
		static float grown( float t, float since ){
			if (since >= 0) return Math.max( 0f, 1f - since / WITHDRAW );
			return Math.min( 1f, t / GROW );
		}

		@Override
		void stop(){
			stoppedAt = age;
		}

		@Override
		boolean step( float dt ){
			if (stopping) return age - stoppedAt < WITHDRAW;
			if ((leafIn -= dt) <= 0){
				leafIn = LEAF_EVERY;
				if (s.visible) leaf();
			}
			return true;
		}

		private void leaf(){
			FxEmitter air = Fx.air();
			if (air == null || !FxBudget.keep( FxBudget.P3, cx(), cy() )) return;
			Level level = Dungeon.level;
			int c = level != null ? level.color2 : Element.NATURE.ramp.stop( 2 );
			float x = cx() + FxRandom.Float( -5f, 5f );
			CombatFactories.make( air, x, feet() - 3f ).look( FxFrames.LEAF_D, 4 ).paint( c )
					.vel( FxRandom.Float( -12f, 12f ), -FxRandom.Float( 22f, 30f ) ).gravity( 60f ).drag( 1.2f )
					.ground( feet() - 1f, FxParticle.SETTLE, 0 ).life( 1.2f ).alpha( 1f, 0, 0.3f ).priority( FxBudget.P3 );
		}

		@Override
		void drawOver(){
			vine.show( FxFrames.VINE[(int)(age * FPS) % FxFrames.VINE.length] );
			vine.cropUp( grown( age, stopping ? age - stoppedAt : -1 ) );
			vine.draw( s, Math.round( cx() ), feet() - vine.height() / 2f, VINE, 1f, false, 1, false );
		}

		@Override
		void release(){
			vine.release();
		}
	}

	/** Barkskin: a ring of bark at the feet (RING_13, 5E4A30 at 0.7) with its lip a pixel over
	 *  it caught in the light (RING_13, D8A860 at 0.35, added: it reads on any floor), and three
	 *  chips of bark struck off whenever a blow lands (the sprite flashes) while it holds. At most
	 *  5 quads. */
	static final class Barked extends StateLooks.Look {

		static final int BARK = 0x8A6A40, BARK_DARK = 0x5E4A30, LIP = 0xD8A860, CHIPS = 3;
		static final float ALPHA = 0.7f, LIP_ALPHA = 0.35f;
		private final Quad ring = new Quad( false ).show( FxFrames.RING_13 );
		private final Quad lip = new Quad( false ).show( FxFrames.RING_13 );

		Barked( CharSprite s ){
			super( s );
		}

		@Override
		void flashed(){
			FxEmitter air = Fx.air();
			if (air == null || !s.visible) return;
			int n = FxBudget.allow( CHIPS, FxBudget.P2 );
			for (int i = 0; i < n; i++){
				CombatFactories.make( air, cx() + FxRandom.Float( -3f, 3f ), cy() )
						.look( FxFrames.BARK[FxRandom.Int( FxFrames.BARK.length )] ).paint( FxRandom.chance( 0.5f ) ? BARK : BARK_DARK )
						.polar( (float)(-Math.PI / 2) + FxRandom.Float( -1.1f, 1.1f ), FxRandom.Float( 30f, 50f ) ).gravity( 160f )
						.ground( feet() - FxRandom.Float( 0f, 2f ), FxParticle.BOUNCE, 0.3f ).spin( 0.1f )
						.life( FxRandom.Float( 0.5f, 0.7f ) ).alpha( 1f, 0, 0.2f ).priority( FxBudget.P2 );
			}
		}

		@Override
		void drawBehind(){
			float in = Math.min( 1f, age / 0.2f );
			ring.draw( s, Math.round( cx() ), feet() - 3f, BARK_DARK, ALPHA * in, false, 1, false );
			lip.draw( s, Math.round( cx() ), feet() - 4f, LIP, LIP_ALPHA * in, true, 1, false );
		}

		@Override
		void release(){
			ring.release();
			lip.release();
		}
	}

	/** Verdant (Nature's Power): three leaves circling it (FxOrbit, the VERDANT slot) and one
	 *  shed every 0.8 s, falling to the ground. 5 quads. */
	static final class Verdant extends StateLooks.Look {

		static final float SHED_EVERY = 0.8f;
		static final int LEAVES = 3;
		private FxOrbit.Ring orbit;
		private float shedIn = 0.4f;

		Verdant( CharSprite s ){
			super( s );
		}

		private int color(){
			Level level = Dungeon.level;
			return level != null ? level.color2 & 0xFFFFFF : Element.NATURE.ramp.stop( 2 );
		}

		@Override
		void start(){
			orbit = FxOrbit.attach( s, FxOrbit.Slot.VERDANT ).motes( LEAVES, FxFrames.LEAF_D[0], color(), false ).breath( 0.85f, 1f, 0.5f );
		}

		@Override
		boolean step( float dt ){
			if (stopping) return false;
			if ((shedIn -= dt) <= 0){
				shedIn = SHED_EVERY;
				if (s.visible) shed();
			}
			return true;
		}

		private void shed(){
			FxEmitter air = Fx.air();
			if (air == null || !FxBudget.keep( FxBudget.P3, cx(), cy() )) return;
			float a = FxRandom.angle();
			float x = cx() + (float)Math.cos( a ) * FxOrbit.Slot.VERDANT.rx;
			float y = cy() + FxOrbit.Slot.VERDANT.h + (float)Math.sin( a ) * FxOrbit.Slot.VERDANT.ry;
			CombatFactories.make( air, x, y ).look( FxFrames.LEAF_E, 3 ).paint( color() ).vel( FxRandom.Float( -4f, 4f ), 6f )
					.gravity( 28f ).drag( 2.5f ).sway( 6f, 4f ).ground( feet() - 1f, FxParticle.SETTLE, 0 )
					.life( 1.4f ).alpha( 1f, 0, 0.3f ).priority( FxBudget.P3 );
		}

		@Override
		void release(){
			if (orbit != null) orbit.detach();
			orbit = null;
		}
	}

	/** Sungrass's health: a mote of FFF0A0 light rising from the feet every 0.6 s (about 4). */
	static final class Herb extends StateLooks.Look {

		static final int COLOR = 0xFFF0A0;
		static final float EVERY = 0.6f;
		private FxEmitter motes;

		Herb( CharSprite s ){
			super( s );
		}

		@Override
		void start(){
			motes = pour( true, CombatFactories.riser( COLOR, true ), EVERY, 0.25f, 0.85f, 0.5f, 0.1f );
		}

		@Override
		boolean step( float dt ){
			shown( motes );
			return !stopping;
		}

		@Override
		void release(){
			stopPour( motes );
			motes = null;
		}
	}

	/** Charmed hearts, as always: a heart every 0.5 s. */
	static final class Hearts extends StateLooks.Look {

		static final float EVERY = 0.5f;
		private FxEmitter hearts;

		Hearts( CharSprite s ){
			super( s );
		}

		@Override
		void start(){
			hearts = pour( false, Speck.factory( Speck.HEART ), EVERY, 0f, 0f, 1f, 1f );
		}

		@Override
		boolean step( float dt ){
			shown( hearts );
			return !stopping;
		}

		@Override
		void release(){
			stopPour( hearts );
			hearts = null;
		}
	}

	// ================================================================== the enchantments' states

	/** A stun (a Paralysis) put on ch inside the open blow (Blow.announce): its stars hold till the
	 *  blow's stun beat (a StunPiece, priority A_STUN), then drop in. Stars already circling are left
	 *  be. Any thread. */
	static void stunning( Char ch ){
		CharSprite s = ch.sprite;
		if (s == null || s.stunStars()) return;
		if (Blow.offer( new StunPiece( ch ) )) EnchantState.of( s ).stunAt = Game.timeTotal + Stunned.HOLD;
	}

	/** How far above their orbit the stun stars of s are now, px: DROP falling to 0 over DROP_FOR s
	 *  from the moment they were let drop (EnchantState.stunAt); -1 while they wait for the beat. */
	static float stunLift( CharSprite s ){
		EnchantState st = EnchantState.get( s );
		if (st == null || st.stunAt < 0) return 0f;
		float t = Game.timeTotal - st.stunAt;
		if (t < 0) return -1f;
		return t >= Stunned.DROP_FOR ? 0f : Stunned.DROP * (1f - t / Stunned.DROP_FOR);
	}

	/** A stun's beat on its blow: it lets the stars drop (on the lead's frame when it plays on
	 *  another character). Nothing drawn of its own; host-only (a guest's stars come by name, at once). */
	static final class StunPiece extends Piece {

		StunPiece( Char ch ){
			super( -1, Blow.MINOR, Blow.PLACED, ch );
			priority = Blow.A_STUN;
		}

		@Override
		public void lead( boolean mayShake ){
			drop();
		}

		@Override
		public void accent( float x, float y ){
			drop();
		}

		private void drop(){
			EnchantState st = on.sprite != null ? EnchantState.get( on.sprite ) : null;
			if (st != null && st.stunAt >= 0) st.stunAt = Game.timeTotal;
		}
	}

	/** Stunned (PARALYSED by a real stun: a Paralysis buff, StateLooks.stunShown): two amber stun
	 *  stars (STAR4_5, the METAL ramp drawn toward CCAA44) circling over its head at 0.8 turns a
	 *  second where the MARK orbit runs, breathing a little, the far one behind the body. Drawn
	 *  here, not on the MARK orbit itself: that ring is the guiding light's and a skill mark's, and
	 *  a stun must neither draw over theirs nor take it away as it ends. Hidden while the stun is a
	 *  frost's, a time freeze's or a duel's pause, or ice holds the body. On the blow that stuns it
	 *  they wait for its stun beat and drop in from DROP px above in DROP_FOR s (stunLift). 2 quads. */
	static final class Stunned extends StateLooks.Look {

		static final int STARS = 2;
		static final int COLOR = Ramp.lerp( Element.METAL.ramp.stop( 1 ), 0xCCAA44, 0.6f );
		/** Its turns a second, its breath (lowest, highest, Hz). */
		static final float TURNS = 0.8f, BREATH_LO = 0.8f, BREATH_HI = 1f, BREATH_HZ = 1f;
		/** The drop in: from how high over the orbit, px, in how long, s; the longest the stars wait
		 *  for their blow's beat, s (a beat that never comes). */
		static final float DROP = 4f, DROP_FOR = 0.1f, HOLD = 1f;
		private final StateLooks owner;
		private final Quad[] stars = new Quad[STARS];
		private float phase;

		Stunned( CharSprite s, StateLooks owner ){
			super( s );
			this.owner = owner;
			for (int i = 0; i < STARS; i++) stars[i] = new Quad( false ).show( FxFrames.STAR4_5 );
		}

		@Override
		boolean step( float dt ){
			phase += (float)(2 * Math.PI * TURNS) * dt;
			return !stopping;
		}

		@Override
		void drawBehind(){
			draw( true );
		}

		@Override
		void drawOver(){
			draw( false );
		}

		//the stars on the far (behind the body) or the near side of their circle
		private void draw( boolean far ){
			if (stopping || !owner.stunShown()) return;
			float lift = stunLift( s );
			if (lift < 0) return;
			FxOrbit.Slot m = FxOrbit.Slot.MARK;
			float a = BREATH_LO + (BREATH_HI - BREATH_LO) * (0.5f + 0.5f * (float)Math.sin( 2 * Math.PI * BREATH_HZ * age ));
			for (int i = 0; i < STARS; i++){
				float t = phase + (float)(2 * Math.PI * i / STARS);
				float sn = (float)Math.sin( t );
				if ((sn < 0) != far) continue;
				stars[i].draw( s, cx() + m.rx * (float)Math.cos( t ), cy() + m.h + m.ry * sn - lift, COLOR, a, true, 1, false );
			}
		}

		@Override
		void release(){
			for (Quad q : stars) q.release();
		}
	}

	/** Seeded (a parasitic seed): one SEED mote at the chest beating as a heart does (1.2 s: a
	 *  lub, a softer dub, a rest) and two vine wisps curling out of it to either side, NATURE's
	 *  88CC44, as matter. The wisps are INK_WISP's 3x7 curls painted green, not VINE: a VINE frame
	 *  is 14x6 (the roots' wrap at the feet), and two of them would cover the whole chest. 3 quads. */
	static final class Seeded extends StateLooks.Look {

		static final int COLOR = 0x88CC44;
		static final float BEAT = 1.2f, FPS = 3f;
		private final Quad seed = new Quad( false ).show( FxFrames.SEED );
		private final Quad left = new Quad( false ).show( FxFrames.INK_WISP[0] ), right = new Quad( false ).show( FxFrames.INK_WISP[0] );

		Seeded( CharSprite s ){
			super( s );
		}

		/** Its beat `t` s in: 1 on the lub, 0.75 on the dub, 0.4 at rest. */
		static float beat( float t ){
			float k = t % BEAT;
			if (k < 0.1f) return 1f;
			if (k >= 0.25f && k < 0.33f) return 0.75f;
			return 0.4f;
		}

		@Override
		void drawOver(){
			float b = beat( age );
			float cx = Math.round( cx() ), cy = Math.round( cy() ) + 1;
			int[] f = FxFrames.INK_WISP[(int)(age * FPS) % FxFrames.INK_WISP.length];
			left.show( f );
			right.show( f );
			left.draw( s, cx - 3, cy - 3, COLOR, 0.35f + 0.3f * b, false, 1, true );
			right.draw( s, cx + 3, cy - 3, COLOR, 0.35f + 0.3f * b, false, 1, false );
			seed.draw( s, cx, cy, Ramp.lerp( COLOR, 0xFFF0A0, b - 0.4f ), 0.6f + 0.4f * b, false, 1, false );
		}

		@Override
		void release(){
			seed.release();
			left.release();
			right.release();
		}
	}

	/** A surge (Surging): one amber mote for each of its stacks (StateLooks.surge(), read every
	 *  0.25 s) on the ELEMENTAL orbit, the orbit flaring as each new one comes. At most 5 quads. */
	static final class Surge extends StateLooks.Look {

		static final int COLOR = 0xFFCC00, MOST = 5;
		private final StateLooks owner;
		private FxOrbit.Ring orbit;
		private int shown;

		Surge( CharSprite s, StateLooks owner ){
			super( s );
			this.owner = owner;
		}

		@Override
		void start(){
			shown = Math.min( MOST, owner.surge() );
			orbit = FxOrbit.attach( s, FxOrbit.Slot.ELEMENTAL ).motes( shown, FxFrames.MOTE_2, COLOR, true ).breath( 0.85f, 1f, 0.8f );
			orbit.flare( 0.6f, 0.35f, 0 );
		}

		@Override
		boolean step( float dt ){
			if (stopping) return false;
			int n = Math.min( MOST, owner.surge() );
			if (n != shown && orbit != null){
				boolean more = n > shown;
				shown = n;
				orbit.motes( n, FxFrames.MOTE_2, COLOR, true );
				if (more) orbit.flare( 0.6f, 0.35f, 0 );
			}
			return true;
		}

		@Override
		void again(){
			if (orbit != null) orbit.flare( 0.6f, 0.35f, 0 );
		}

		@Override
		void release(){
			if (orbit != null) orbit.detach();
			orbit = null;
		}
	}
}
