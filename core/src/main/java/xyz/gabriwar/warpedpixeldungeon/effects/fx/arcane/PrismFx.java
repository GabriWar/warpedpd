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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

/**
 * The prismatic laser round its white core (Beam.PrismRay paints the core): a warm fringe on
 * one side and a cool one on the other, a halo, packets of light racing down it in the prism's
 * hues, a bloom and a star at the wand, a star at the end with three chromatic ghosts and a pool,
 * glints laid along it as a rainbow, six pools of the prism's hues on the floor under it, and a
 * wash of colour through the water cells it crosses (up to washes()). At most DRAWS pictures, all light, all
 * gone within LIFE s. Built on the beam's first frame (render thread), FxRandom only.
 *
 * Its timeline (s): the core 1 px at 0, 3 at 0.02, 5 at 0.04, back to 3 at 0.45 and to 1 at 0.7,
 * gone at CORE_END; the fringes in at 0.04, spreading 3, 4, 5 px off the axis at 0.4 and 0.6 as
 * they fade; a packet every 0.18 s from 0.04 to 0.5 at 240 px/s; the impact at 0.04.
 */
public final class PrismFx {

	/** Its whole life, and when its core is gone. */
	public static final float LIFE = 1.0f, CORE_END = 0.9f;
	/** The core's widths and when each begins. */
	public static final int[] CORE_W = { 1, 3, 5, 3, 1 };
	public static final float[] CORE_AT = { 0f, 0.02f, 0.04f, 0.45f, 0.7f };
	/** The fringes: in at FRINGE_IN, their offsets off the axis and when each begins. */
	public static final float FRINGE_IN = 0.04f;
	public static final int[] FRINGE_OFF = { 3, 4, 5 };
	public static final float[] FRINGE_AT = { 0f, 0.4f, 0.6f };
	/** Packets: their speed, how often one leaves, from when to when; two drawn at most. */
	public static final float PACKET_SPEED = 240f, PACKET_EVERY = 0.18f, PACKET_FROM = 0.04f, PACKET_TO = 0.5f;
	public static final int PACKETS = 2;
	/** The impact's moment, its star's life and swap, its ghosts' life. */
	public static final float IMPACT_AT = 0.04f, STAR_LIFE = 0.3f, STAR_SWAP = 0.06f, GHOST_LIFE = 0.25f;
	/** Glints along the path: one per GLINT_EVERY px, at most GLINTS, spawned over GLINT_OVER s. */
	public static final int GLINT_EVERY = 12, GLINTS = 16;
	public static final float GLINT_OVER = 0.3f;
	/** Floor pools under the path. */
	public static final int POOLS = 6;
	/** Pictures it draws at most (FxBudget's per-effect number). */
	public static final int DRAWS = 35;
	/** The three ghosts' hues. */
	public static final int[] GHOSTS = { 0xFF5A5A, 0x70F070, 0x6878FF };

	private float sx, sy, ex, ey, len, dx, dy, nx, ny;
	private ArcImage warm, cool, halo;
	private final ArcImage[] packets = new ArcImage[PACKETS];
	private final int[] packetHue = new int[PACKETS];
	private final float[] packetAt = new float[PACKETS];
	private int nextPacket;
	private FxLight source;
	private boolean impacted, built;
	private int glintsLeft, glintsMade;
	private float glintIn, glintEvery;
	//the water cells along it (at most one per 4 px of it)
	private int[] wet;
	private int wetCount;

	/** The core's width at t s. */
	public static int coreWidth( float t ){
		int w = CORE_W[0];
		for (int i = 0; i < CORE_AT.length; i++) if (t >= CORE_AT[i]) w = CORE_W[i];
		return w;
	}

	/** The core's alpha at t s: whole, then fading from 0.7 s to CORE_END. */
	public static float coreAlpha( float t ){
		if (t < 0.7f) return 1f;
		return Math.max( 0f, 1f - (t - 0.7f) / (CORE_END - 0.7f) );
	}

	/** The fringes' offset off the axis at t s. */
	public static int fringeOffset( float t ){
		int o = FRINGE_OFF[0];
		for (int i = 0; i < FRINGE_AT.length; i++) if (t >= FRINGE_AT[i]) o = FRINGE_OFF[i];
		return o;
	}

	/** The fringes' alpha at t s: in at 0.04, whole to 0.4, fading out by 0.8. */
	public static float fringeAlpha( float t ){
		if (t < FRINGE_IN) return 0f;
		if (t < 0.4f) return 0.9f;
		return Math.max( 0f, 0.9f * (1f - (t - 0.4f) / 0.4f) );
	}

	/** The prism's hue along the path, 0..1. */
	public static int hueAt( float p ){
		return Element.PRISM.ramp.at( Math.max( 0f, Math.min( 1f, p ) ) );
	}

	/** Builds it from the wand (sx, sy) to the end (ex, ey). Render thread. */
	public void build( float sx, float sy, float ex, float ey ){
		this.sx = sx;
		this.sy = sy;
		this.ex = ex;
		this.ey = ey;
		float vx = ex - sx, vy = ey - sy;
		len = (float)Math.sqrt( vx * vx + vy * vy );
		dx = len > 0 ? vx / len : 1;
		dy = len > 0 ? vy / len : 0;
		nx = -dy;
		ny = dx;
		built = true;
		impacted = false;
		nextPacket = 0;
		if (!Arc.onScene()) return;

		halo = band( FxFrames.Light.BAND_21, 0xF0F0FF );
		warm = band( FxFrames.Light.PRISM_WARM, 0xFFFFFF );
		cool = band( FxFrames.Light.PRISM_COOL, 0xFFFFFF );
		for (int i = 0; i < PACKETS; i++){
			packets[i] = ArcImage.get( GameScene.Layer.LIGHTS, true, true );
			packets[i].show( FxFrames.Light.PULSE_DOT );
			packets[i].visible = false;
			packetAt[i] = -1;
		}

		//at the wand: a bloom and a star, on the ray's sound
		source = Arc.glow( sx, sy, 15, 0xFFFFFF, 0.55f );
		if (source != null) source.life( 0, 0.15f, 0.6f );
		ArcFlash star = ArcFlash.light( sx, sy, FxFrames.Light.STAR4_9_A, 0xFFFFFF, 1f, 0.2f );
		if (star != null) star.swap( FxFrames.Light.STAR4_9_B, 0.06f ).fade( 0, 0.12f );

		//six pools of the prism's hues on the floor under it
		for (int i = 0; i < POOLS; i++){
			float t = (i + 0.5f) / POOLS;
			FxLight l = Arc.pool( sx + vx * t, sy + vy * t + Arc.GROUND, 23, Element.PRISM.ramp.stop( i ), 0.22f );
			if (l != null) l.life( 0.04f, 0, 0.7f );
		}

		//glints laid along it, and the water it crosses: the path's GLINTS shared out, a wash for
		//each wet cell first (spread along the water when it crosses more than washes()), then up
		//to two glints on each washed cell, and what is left glints along the path; DRAWS kept
		//however much water it crosses
		findWater();
		int washes = washes( wetCount );
		int wetGlints = wetGlints( washes );
		glintsLeft = Math.max( 0, Math.min( GLINTS - washes - wetGlints, Math.round( len / GLINT_EVERY ) ) );
		glintsMade = 0;
		glintEvery = glintsLeft > 0 ? GLINT_OVER / glintsLeft : 1f;
		glintIn = 0;
		int w = washes > 0 ? Dungeon.level.width() : 0;
		for (int i = 0; i < washes; i++){
			int cell = wet[i * wetCount / washes];
			float cx = (cell % w + 0.5f) * 16f, cy = (cell / w + 0.5f) * 16f;
			int hue = hueAt( along( cx, cy - Arc.GROUND ) );
			WaterFX.wash( cell, hue, 0.2f, 0.5f );
			//this cell's share of the wet glints, spread evenly over the washed cells
			int glints = (i + 1) * wetGlints / washes - i * wetGlints / washes;
			if (glints > 0) Arc.glint( cx + FxRandom.Float( -4, 4 ), cy + FxRandom.Float( -3, 3 ), hue );
			if (glints > 1) Arc.glint( cx + FxRandom.Float( -4, 4 ), cy + FxRandom.Float( -3, 3 ), 0xFFFFFF );
		}
	}

	/** The wet cells washed when it crosses `wet` of them: every one, up to GLINTS (and the water's
	 *  own WaterFX.WASHES). */
	public static int washes( int wet ){
		return Math.min( wet, Math.min( GLINTS, WaterFX.WASHES ) );
	}

	/** The glints laid on its washed cells: two each, out of what the washes leave of GLINTS. */
	public static int wetGlints( int washes ){
		return Math.min( 2 * washes, GLINTS - washes );
	}

	private ArcImage band( int[] frame, int color ){
		ArcImage a = ArcImage.get( GameScene.Layer.LIGHTS, true, true );
		a.show( frame ).rgb( color );
		a.am = 0;
		a.line( sx, sy, ex, ey );
		return a;
	}

	//how far along the path a point lies, 0..1
	private float along( float x, float y ){
		if (len <= 0) return 0;
		return ((x - sx) * dx + (y - sy) * dy) / len;
	}

	//the water cells under the path, each once
	private void findWater(){
		wetCount = 0;
		if (Dungeon.level == null || Dungeon.level.water == null) return;
		int steps = Math.max( 1, (int)(len / 4f) );
		if (wet == null || wet.length < steps + 1) wet = new int[steps + 1];
		int last = -1;
		for (int i = 0; i <= steps; i++){
			float t = i / (float)steps;
			float x = sx + (ex - sx) * t, y = sy + (ey - sy) * t + Arc.GROUND;
			int cell = Fx.cellAt( x, y );
			if (cell == last || cell < 0) continue;
			last = cell;
			if (WaterFX.onWater( x, y )) wet[wetCount++] = cell;
		}
	}

	/** A frame, t s since it began. */
	public void update( float t, float dt ){
		if (!built || halo == null) return;
		float shimmer = 1f + 0.06f * (float)Math.sin( 2 * Math.PI * 20f * t );
		halo.am = 0.3f * shimmer * coreAlpha( t );
		halo.visible = halo.am > 0.004f;

		int off = fringeOffset( t );
		float fa = fringeAlpha( t );
		warm.line( sx + nx * off, sy + ny * off, ex + nx * off, ey + ny * off );
		cool.line( sx - nx * off, sy - ny * off, ex - nx * off, ey - ny * off );
		warm.am = cool.am = fa;
		warm.visible = cool.visible = fa > 0.004f;

		packets( t );

		if (!impacted && t >= IMPACT_AT){
			impacted = true;
			impact();
		}

		if (glintsLeft > 0 && (glintIn -= dt) <= 0){
			glintIn += glintEvery;
			glintsLeft--;
			glint( glintsMade++ );
		}
	}

	//light packets racing down it, each the next hue of the prism
	private void packets( float t ){
		if (t >= PACKET_FROM && t <= PACKET_TO){
			int due = (int)((t - PACKET_FROM) / PACKET_EVERY);
			while (nextPacket <= due){
				int slot = nextPacket % PACKETS;
				packetAt[slot] = PACKET_FROM + nextPacket * PACKET_EVERY;
				packetHue[slot] = Element.PRISM.ramp.stop( nextPacket % Element.PRISM.ramp.size() );
				packets[slot].rgb( packetHue[slot] );
				nextPacket++;
			}
		}
		float angle = (float)Math.toDegrees( Math.atan2( dy, dx ) );
		for (int i = 0; i < PACKETS; i++){
			ArcImage p = packets[i];
			if (p == null) continue;
			float d = packetAt[i] < 0 ? -1 : (t - packetAt[i]) * PACKET_SPEED;
			if (d < 0 || d > len){
				p.visible = false;
				continue;
			}
			p.origin.set( p.width / 2f, p.height / 2f );
			p.x = sx + dx * d - p.width / 2f;
			p.y = sy + dy * d - p.height / 2f;
			p.angle = angle;
			p.am = 1f;
			p.visible = true;
		}
	}

	//the end: a star swapping its points, three chromatic ghosts round it, a pool
	private void impact(){
		ArcFlash star = ArcFlash.light( ex, ey, FxFrames.Light.STAR8_23_A, 0xFFFFFF, 1f, STAR_LIFE );
		if (star != null) star.swap( FxFrames.Light.STAR8_23_B, STAR_SWAP ).fade( 0, STAR_LIFE * 0.6f );
		for (int i = 0; i < GHOSTS.length; i++){
			float ox = (i - 1), oy = (i == 1 ? -1 : 1);
			ArcFlash g = ArcFlash.light( ex + ox, ey + oy, FxFrames.Light.STAR4_15_A, GHOSTS[i], 0.6f, GHOST_LIFE );
			if (g != null) g.swap( FxFrames.Light.STAR4_15_B, STAR_SWAP * 2 ).fade( 0, GHOST_LIFE * 0.6f ).seenAt( ex, ey );
		}
		FxLight pool = Arc.pool( ex, ey + Arc.GROUND, 31, Element.PRISM.light, 0.35f );
		if (pool != null) pool.life( 0, 0, 0.5f );
	}

	//the i-th glint along it, its hue the prism's at its place: a rainbow laid on the path
	private void glint( int i ){
		float t = (i + FxRandom.Float( 0.2f, 0.8f )) / Math.max( 1, Math.round( len / GLINT_EVERY ) );
		t = Math.min( 0.98f, t );
		float x = sx + (ex - sx) * t, y = sy + (ey - sy) * t;
		FxParticle p = Arc.light( x, y, FxBudget.P3 );
		if (p == null) return;
		float s = FxRandom.sign() * FxRandom.Float( 4f, 8f );
		float life = FxRandom.Float( 0.4f, 0.7f );
		p.look( GLINT_FRAMES, 0 ).paint( hueAt( t ) ).vel( nx * s, ny * s ).life( life ).alpha( 1f, 0, life * 0.3f ).snap();
	}

	static final int[][] GLINT_FRAMES = { FxFrames.PLUS_3, FxFrames.STAR4_5, FxFrames.PLUS_3 };

	/** Lets go of what it built. */
	public void dispose(){
		if (halo != null) halo.kill();
		if (warm != null) warm.kill();
		if (cool != null) cool.kill();
		halo = warm = cool = null;
		for (int i = 0; i < PACKETS; i++){
			if (packets[i] != null) packets[i].kill();
			packets[i] = null;
		}
		if (source != null) source.putOut( 0.1f );
		source = null;
		built = false;
	}
}
