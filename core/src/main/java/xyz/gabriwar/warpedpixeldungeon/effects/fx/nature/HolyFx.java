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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.nature;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxOrbit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Visual;
import com.watabou.noosa.particles.Emitter;

/**
 * Holy light, in the language's HOLY palette: a star blooming on the blessed (STAR8, turning a
 * step as it twinkles, its glow and motes rising) where a scaled flare stood, a bubble of light,
 * a glint sweeping across a sprite, the hand's glint, holy impacts and blooms, the avatar's halo
 * and the sparkles on its edge, the guiding light's mark circling, dark wisps drawn out, the
 * lance's trail and its glow, and the spirits' light rising as wisps. Every entry point may be
 * called from the actor thread (it hands itself to the render thread); at most 40 particles and
 * 3 pictures a cast.
 */
public final class HolyFx {

	private HolyFx(){}

	/** A star: its life, peak, its frames' turn rate; its glow's size and level; motes rising
	 *  with it. The star blooms over the head and the glow stays soft, so the blessed is still
	 *  seen mid-fight (a white star on its middle hid it). */
	public static final float STAR_LIFE = 1f, STAR_PEAK = 0.6f, STAR_FPS = 8f, STAR_GLOW_LEVEL = 0.4f;
	public static final int STAR_GLOW = 23;
	/** Bless's yellow-white, Cleanse's pink, the soft gold of the rest. */
	public static final int BLESS = 0xFFF6A0, CLEANSE = 0xFF80D8;
	private static final int[][] STAR_31 = { FxFrames.Light.STAR8_31_A, FxFrames.Light.STAR8_31_B };
	private static final int[][] STAR_47 = { FxFrames.Light.STAR8_47_A, FxFrames.Light.STAR8_47_B };

	private static float midX( Visual v ){
		return v.x + v.width() / 2f;
	}

	private static float midY( Visual v ){
		return v.y + v.height() / 2f;
	}

	private static boolean shows( CharSprite s ){
		return s != null && s.visible && s.parent != null;
	}

	// ------------------------------------------------------------------ stars, bubbles, glints

	/** A star blooming over a sprite's head (31 px, or 47 for a big one), its glow round it, and
	 *  `motes` motes rising (Bless: 8). */
	public static void star( final CharSprite s, final int color, final boolean big, final int motes ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> star( s, color, big, motes ) );
			return;
		}
		if (!shows( s )) return;
		Emitter glow = Nature.glow( null );
		if (glow == null) return;
		float x = midX( s ), y = midY( s );
		Nature.lit( glow, x, s.y + 1 ).look( big ? STAR_47 : STAR_31, STAR_FPS ).paint( color )
				.life( STAR_LIFE ).alpha( STAR_PEAK, 0.05f, STAR_LIFE * 0.8f ).snap().priority( FxBudget.P1 );
		FxLight.glow( x, y, big ? 47 : STAR_GLOW, Element.HOLY.light, STAR_GLOW_LEVEL ).life( 0.05f, 0.15f, 0.7f );
		int n = FxBudget.allow( motes, FxBudget.P2 );
		for (int i = 0; i < n; i++){
			Nature.make( glow, x + FxRandom.Float( -6f, 6f ), y + FxRandom.Float( -2f, 8f ) )
					.look( FxRandom.chance( 0.7f ) ? FxFrames.MOTE_1 : FxFrames.MOTE_2 ).paint( color ).light( true )
					.vel( FxRandom.Float( -2f, 2f ), -FxRandom.Float( 10f, 18f ) ).drag( 0.5f )
					.life( FxRandom.Float( 0.7f, 1.1f ) ).alpha( 0.9f, 0.05f + 0.03f * i, 0.4f ).snap().priority( FxBudget.P2 );
		}
	}

	/** A bubble of light round a sprite (BUBBLE_23), up to `alpha` and gone over `life` s; in
	 *  front (dx toward its target's side) for a shield. */
	public static void bubble( final CharSprite s, final int color, final float alpha, final float life, final float dx ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> bubble( s, color, alpha, life, dx ) );
			return;
		}
		if (!shows( s )) return;
		Emitter glow = Nature.glow( null );
		if (glow == null) return;
		Nature.lit( glow, Math.round( midX( s ) + dx ), Math.round( midY( s ) ) ).look( FxFrames.Light.BUBBLE_23 ).paint( color )
				.life( life ).alpha( alpha, 0.06f, life * 0.6f ).snap().priority( FxBudget.P1 );
	}

	/** A glint sweeping across a sprite's box: a 2 px line of light at 45 degrees crossing it from
	 *  its top left to its bottom right in 0.3 s (the forms, the wards, the holy weapon). */
	public static void glintSweep( final CharSprite s ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> glintSweep( s ) );
			return;
		}
		if (!shows( s )) return;
		Emitter glow = Nature.glow( null );
		if (glow == null || !FxBudget.keep( FxBudget.P1, s.x, s.y )) return;
		float w = s.width(), h = s.height();
		FxParticle p = Nature.lit( glow, s.x, s.y ).look( FxFrames.Light.CORE_BAND_1 ).paint( 0xFFF8E0 )
				.vel( w / 0.3f, h / 0.3f ).life( 0.3f ).alpha( 0.8f, 0.05f, 0.12f ).priority( FxBudget.P1 );
		p.angle = -45;
		p.scale.set( 1, 2 );
	}

	/** The hand's glint as a spell leaves it: a star for one frame at the caster's side toward
	 *  `cell`. */
	public static void handGlint( final CharSprite caster, final int cell ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> handGlint( caster, cell ) );
			return;
		}
		if (!shows( caster ) || Dungeon.level == null) return;
		Emitter glow = Nature.glow( null );
		if (glow == null) return;
		int w = Dungeon.level.width();
		float tx = (cell % w + 0.5f) * DungeonTilemap.SIZE, x = midX( caster ), y = midY( caster );
		float side = tx > x + 1 ? 5 : tx < x - 1 ? -5 : 0;
		Nature.lit( glow, x + side, y + 1 ).look( FxFrames.Light.STAR4_9_A ).paint( 0xFFFFFF )
				.life( 1 / 30f ).alpha( 1f, 0, 0 ).snap().priority( FxBudget.P1 );
	}

	/** Dark wisps drawn up out of a sprite and dissolving (Cleanse: what it cleansed). */
	public static void wisps( final CharSprite s, final int n ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> wisps( s, n ) );
			return;
		}
		if (!shows( s )) return;
		Emitter air = Nature.air( null );
		if (air == null) return;
		float x = midX( s ), y = midY( s );
		int k = FxBudget.allow( n, FxBudget.P2 );
		for (int i = 0; i < k; i++){
			Nature.make( air, x + FxRandom.Float( -4f, 4f ), y + FxRandom.Float( -3f, 4f ) )
					.look( FxFrames.INK_WISP, 6 ).paint( Element.SHADOW.ramp.stop( 2 ) )
					.vel( FxRandom.Float( -3f, 3f ), -FxRandom.Float( 12f, 18f ) ).drag( 0.8f )
					.turbulence( 1f, 4f ).life( FxRandom.Float( 0.6f, 0.9f ) ).alpha( 0.55f, 0.05f, 0.4f ).smooth()
					.priority( FxBudget.P2 );
		}
	}

	// ------------------------------------------------------------------ impacts and blooms

	/** A holy impact at a sprite (Smite): the kit's impact in HOLY. */
	public static void impact( final CharSprite s ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> impact( s ) );
			return;
		}
		if (!shows( s )) return;
		FxRecipes.impact( midX( s ), midY( s ), Element.HOLY, 1f );
	}

	/** A holy impact at a cell's middle (the lance striking the ground). */
	public static void impactAt( final int cell ){
		Level level = Dungeon.level;
		if (level == null || cell < 0 || cell >= level.length() || !level.heroFOV[cell]) return;
		int w = level.width();
		FxRecipes.impact( (cell % w + 0.5f) * DungeonTilemap.SIZE, (cell / w + 0.5f) * DungeonTilemap.SIZE, Element.HOLY, 1f );
	}

	/** Radiance's and Judgement's light. */
	public static final int RADIANCE = 0xFFF4D0;

	/**
	 * Where a full-screen flash was (Radiance, Judgement; the soft flash still goes, gated): a
	 * wide bloom of FFF4D0 round the caster, a glow and a pool 63 px wide over 0.85 s, its white
	 * core for a moment only (the kit's bloom keeps a core's full brightness all its life: a second
	 * white disc four cells wide on the flash), and twelve motes thrown out.
	 */
	public static void radiance( final CharSprite s ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> radiance( s ) );
			return;
		}
		if (s == null || s.parent == null) return;
		float x = midX( s ), y = midY( s );
		FxLight.glow( x, y, 31, RADIANCE, 1f ).core().life( 0, 0.04f, 0.08f );
		FxLight.glow( x, y, 63, RADIANCE, 1f ).life( 0.05f, 0.16f, 0.64f );
		FxLight.pool( x, y + 6, 63, RADIANCE, 1f ).life( 0.05f, 0.16f, 0.64f );
		Emitter glow = Nature.glow( null );
		if (glow == null) return;
		int n = FxBudget.allow( 12, FxBudget.P2 );
		for (int i = 0; i < n; i++){
			float a = (float)(2 * Math.PI * i / 12) + FxRandom.Float( -0.2f, 0.2f );
			Nature.make( glow, x, y ).look( FxFrames.MOTE_2 ).paint( RADIANCE ).light( true )
					.polar( a, FxRandom.Float( 30f, 50f ) ).drag( 3f ).life( FxRandom.Float( 0.4f, 0.7f ) )
					.alpha( 1f, 0, 0.3f ).priority( FxBudget.P2 );
		}
	}

	/** A soft pop of light round a sprite (a sense opening). */
	public static void pop( final CharSprite s ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> pop( s ) );
			return;
		}
		if (!shows( s )) return;
		FxLight.glow( midX( s ), midY( s ), 15, Element.HOLY.light, 1f ).life( 0.05f, 0.05f, 0.3f );
	}

	/** Where a sunray struck: a bloom of sunlight on the one it struck (its core for a moment, a
	 *  glow, a pool at its feet) and five motes of the sun thrown off it. */
	public static void sunMotes( final CharSprite s ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> sunMotes( s ) );
			return;
		}
		if (!shows( s )) return;
		float x = midX( s ), y = midY( s );
		FxLight.glow( x, y, 15, Nature.SUN, 1f ).core().life( 0, 0.04f, 0.06f );
		FxLight.glow( x, y, 23, Nature.SUN, 1f ).life( 0, 0.05f, 0.45f );
		FxLight.pool( x, s.y + s.height() + 2, 31, Nature.SUN, 0.8f ).life( 0.02f, 0.1f, 0.6f );
		Emitter glow = Nature.glow( null );
		if (glow == null) return;
		for (int i = 0; i < 5; i++){
			if (!FxBudget.keep( FxBudget.P2, x, y )) continue;
			Nature.make( glow, x, y ).look( FxFrames.MOTE_2 ).paint( Nature.SUN ).light( true )
					.polar( FxRandom.Float( (float)-Math.PI, 0f ), FxRandom.Float( 14f, 26f ) ).drag( 2.5f )
					.life( FxRandom.Float( 0.5f, 0.8f ) ).alpha( 1f, 0, 0.3f ).priority( FxBudget.P2 );
		}
	}

	// ------------------------------------------------------------------ holy water

	/** Holy water's splash colour; its droplets; the most sparkling cells, steaming undead. */
	public static final int HOLY_WATER = 0xBFE0FF, WATER_DROPLETS = 8, WATER_CELLS = 8, WATER_STEAM = 4;

	/**
	 * A vial of holy water bursting at `center`: droplets arcing out of it (at most 8), a sparkle
	 * on each splashed cell in view (`cells`, at most 8) and steam off the undead caught in it
	 * (`undead`, at most 4 puffs). Twenty at most.
	 */
	public static void holyWater( final int center, final int[] cells, final int n, final int[] undead, final int m ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> holyWater( center, cells, n, undead, m ) );
			return;
		}
		Level level = Dungeon.level;
		if (level == null || center < 0 || center >= level.length()) return;
		int w = level.width();
		if (level.heroFOV[center]){
			WaterFX.droplets( (center % w + 0.5f) * DungeonTilemap.SIZE, (center / w + 0.5f) * DungeonTilemap.SIZE,
					WATER_DROPLETS, 30f, 60f, 70f, HOLY_WATER );
		}
		Emitter glow = Nature.glow( null );
		if (glow != null){
			Emitter.Factory glint = FxFactories.glint( HOLY_WATER );
			for (int i = 0; i < n && i < WATER_CELLS; i++){
				int c = cells[i];
				glint.emit( glow, i, (c % w + 0.5f) * DungeonTilemap.SIZE + FxRandom.Float( -4f, 4f ),
						(c / w + 0.5f) * DungeonTilemap.SIZE + FxRandom.Float( -2f, 4f ) );
			}
		}
		for (int i = 0; i < m && i < WATER_STEAM; i++){
			int c = undead[i];
			FxRecipes.steam( (c % w + 0.5f) * DungeonTilemap.SIZE, (c / w + 0.5f) * DungeonTilemap.SIZE, 1 );
		}
	}

	// ------------------------------------------------------------------ the lance

	/** The holy lance's trail: motes of holy light shed behind it (in the light layer). */
	public static final Emitter.Factory LANCE_TRAIL = new Emitter.Factory(){
		@Override
		public void emit( Emitter emitter, int index, float x, float y ){
			if (!FxBudget.keep( FxBudget.P2, x, y )) return;
			Nature.make( Nature.glow( emitter ), x + FxRandom.Float( -1f, 1f ), y + FxRandom.Float( -1f, 1f ) )
					.look( index % 3 == 0 ? FxFrames.MOTE_2 : FxFrames.MOTE_1 ).paint( Element.HOLY.ramp.at( FxRandom.Float( 0.1f, 0.5f ) ) )
					.light( true ).vel( FxRandom.Float( -3f, 3f ), FxRandom.Float( -4f, 0f ) ).drag( 1.5f )
					.life( FxRandom.Float( 0.25f, 0.45f ) ).alpha( 0.9f, 0, 0.2f ).priority( FxBudget.P2 );
		}

		@Override
		public boolean lightMode(){
			return true;
		}
	};

	/** The lance's glow, carried with it until it is gone. */
	public static void lanceGlow( final Visual lance ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> lanceGlow( lance ) );
			return;
		}
		//(it may be set going just after: the light follows it once it is, and goes out with it)
		if (lance == null) return;
		FxLight.glow( midX( lance ), midY( lance ), 11, Element.HOLY.light, 1f ).follow( lance, 0, 0 ).life( 0.05f, 3f, 0.1f );
	}

	// ------------------------------------------------------------------ the avatar's halo

	/** The avatar of light's halo: a pool and a glow (FFE9A0, about 0.22, breathing) round its
	 *  bearer, until put out. */
	public static final class Halo {
		//its lights, set and read on the render thread only
		private FxLight glow, pool;
		//put out (any thread): a light not lit yet never is
		private volatile boolean out;

		/** Put out over 0.4 s, from any thread (its lights are read where they were made, so one
		 *  put out before its light came never lights). */
		public void out(){
			out = true;
			Fx.post( () -> {
				if (glow != null) glow.putOut( 0.4f );
				if (pool != null) pool.putOut( 0.4f );
				glow = pool = null;
			} );
		}
	}

	/** A halo round a sprite reaching `radius` cells (1 or 2). */
	public static Halo halo( final CharSprite s, final int radius ){
		final Halo h = new Halo();
		Fx.post( () -> {
			if (h.out || s == null || s.parent == null) return;
			int size = radius >= 2 ? 63 : 47;
			//0.22 shown at night: of a glow's 0.55 and a pool's 0.35
			h.glow = FxLight.glow( midX( s ), midY( s ), size, Nature.HALO, 0.22f / FxLight.GLOW_CAP )
					.flicker( FxLight.BREATH ).follow( s, 0, 0 ).persistent();
			h.pool = FxLight.pool( midX( s ), midY( s ) + 6, size, Nature.HALO, 0.22f / FxLight.POOL_CAP )
					.flicker( FxLight.BREATH ).follow( s, 0, 6 ).persistent();
		} );
		return h;
	}

	/** The halo's edge sparkling on the floor this turn: a glint at each of these cells (at most
	 *  12). */
	public static void sparkles( final int[] cells, final int n ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> sparkles( cells, n ) );
			return;
		}
		Level level = Dungeon.level;
		Emitter glow = Nature.glow( null );
		if (level == null || glow == null) return;
		int w = level.width(), shown = 0;
		Emitter.Factory glint = FxFactories.glint( Nature.HALO );
		//every other cell of a big ring: twelve at most
		int step = n > 12 ? 2 : 1;
		for (int i = 0; i < n && shown < 12; i += step){
			int c = cells[i];
			if (c < 0 || c >= level.length() || !level.heroFOV[c]) continue;
			shown++;
			glint.emit( glow, i, (c % w + 0.5f) * DungeonTilemap.SIZE + FxRandom.Float( -3f, 3f ),
					(c / w + 0.5f) * DungeonTilemap.SIZE + FxRandom.Float( -2f, 3f ) );
		}
	}

	// ------------------------------------------------------------------ the guiding light's mark

	/** The guiding light's mark over its target: a pale gold star circling above its head,
	 *  breathing at 0.5 Hz, until taken off (mark(s, false)). */
	public static void mark( final CharSprite s, final boolean on ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> mark( s, on ) );
			return;
		}
		if (s == null) return;
		FxOrbit.Ring r = FxOrbit.get( s, FxOrbit.Slot.MARK );
		if (on){
			if (r == null || r.isDetached()){
				r = FxOrbit.attach( s, FxOrbit.Slot.MARK );
				if (r != null) r.motes( 1, FxFrames.STAR4_7, Nature.MARK, true ).breath( 0.6f, 1f, 0.5f ).speed( 1.2f );
			}
		} else if (r != null){
			r.detach();
		}
	}

	// ------------------------------------------------------------------ the spirits

	/** A spirit's light rising: a wisp lit in C8E0FF, rising 20 px/s, fading into two motes.
	 *  Light. */
	public static final Emitter.Factory SPIRIT_WISP = new Emitter.Factory(){
		@Override
		public void emit( Emitter emitter, int index, float x, float y ){
			if (!FxBudget.keep( FxBudget.P2, x, y )) return;
			Nature.make( Nature.glow( emitter ), x, y ).look( FxFrames.INK_WISP, 8 ).paint( Nature.SPIRIT ).light( true )
					.vel( FxRandom.Float( -2f, 2f ), -20f ).sway( 2f, 3f ).life( FxRandom.Float( 0.7f, 0.9f ) )
					.alpha( 0.8f, 0.1f, 0.35f ).fate( WISP_END ).priority( FxBudget.P2 );
		}

		@Override
		public boolean lightMode(){
			return true;
		}
	};

	//a wisp's end: two motes going on up
	private static final FxParticle.Fate WISP_END = ( p, landed ) -> {
		Emitter e = (Emitter) p.parent;
		if (e == null) return;
		for (int i = 0; i < 2; i++){
			if (!FxBudget.keep( FxBudget.P3, p.px, p.py )) continue;
			Nature.make( e, p.px + FxRandom.Float( -2f, 2f ), p.py ).look( FxFrames.MOTE_1 ).paint( Nature.SPIRIT ).light( true )
					.vel( FxRandom.Float( -2f, 2f ), -FxRandom.Float( 10f, 16f ) ).life( FxRandom.Float( 0.4f, 0.6f ) )
					.alpha( 0.7f, 0, 0.3f ).snap().priority( FxBudget.P3 );
		}
	};

	// ------------------------------------------------------------------ the aura of protection

	/** The aura of protection's twinkles: on a 2:1 ring 1.5-2 cells out round its bearer (the
	 *  emitter set on the bearer's top-left), 0.9 s each (about 12 alive at one every 0.08 s). */
	public static final Emitter.Factory AURA = new Emitter.Factory(){
		@Override
		public void emit( Emitter emitter, int index, float x, float y ){
			float cx = x + 8, cy = y + 12;
			float a = FxRandom.angle(), r = FxRandom.Float( 1.5f, 2f ) * DungeonTilemap.SIZE;
			float px = cx + (float)Math.cos( a ) * r, py = cy + (float)Math.sin( a ) * r * 0.5f;
			if (!FxBudget.keep( FxBudget.P3, px, py )) return;
			Nature.make( Nature.glow( emitter ), px, py ).look( TWINKLE, 0 ).paint( Nature.HOLY ).light( true )
					.vel( 0, -FxRandom.Float( 2f, 5f ) ).life( 0.9f ).alpha( 0.8f, 0.1f, 0.3f ).snap().priority( FxBudget.P3 );
		}

		@Override
		public boolean lightMode(){
			return true;
		}
	};

	//a twinkle's look: a dot, a plus, a star, a plus, a dot (as the kit's glints)
	private static final int[][] TWINKLE = { FxFrames.MOTE_1, FxFrames.PLUS_3, FxFrames.STAR4_5, FxFrames.PLUS_3, FxFrames.MOTE_1 };
}
