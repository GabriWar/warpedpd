/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Lit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.Arc;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.ArcImage;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Visual;
import com.watabou.utils.PointF;

//a flare: a painted star of light, never a fan of rays turned and scaled. Up to 4 rays it is a
//four-pointed star, 5 to 8 an eight-pointed one, 9 or more an eight-pointed one with a four-pointed
//one at half its size inside; its size the painted frame nearest twice its radius without going
//over (past 24 px a frame of half that drawn at x2). A burst steps up through the sizes over its
//first quarter, holds, and steps back one at 70% as it fades; its spin is its star swapping between
//its A and B frames; a white core shows its first 0.1 s; a soft glow half again its size lies
//under it. A lasting one (an aura) breathes +-10% at 0.5 Hz and swaps every 0.4 s, with a pool of
//its light on the ground while it shows (never under the fog). At most three pictures; bursts at a point in the game's world draw over
//the night's tint, bursts on a sprite stay behind it and lay a pool of their light at its feet;
//more than MOST bursts at once, the oldest are cut short.
public class Flare extends Visual implements Lit {

	private float duration = 0;
	private float lifespan;

	private boolean lightMode = true;

	private final int nRays;
	private final float radius;

	//its painted parts, made when first drawn and let go of with it
	private ArcImage rays, inner, under, core;
	//the star's frames (A, B) smallest first, the index of its size, its scale, and the inner star's
	private int[][] famA, famB;
	private int size, k;
	private int[][] innerA, innerB;
	private int innerSize;
	private float age;
	private boolean started, cut, world;
	//shown behind a visual (a sprite): it stays behind it, never carried over it
	private boolean behind;
	private Gizmo carrier;
	private FxLight pool;

	/** Bursts at once; past it the oldest are cut short (to CUT s). */
	public static final int MOST = 12;
	public static final float CUT = 0.15f;
	/** A lasting flare's breath (a share of its alpha, Hz) and its swap; a burst's spin (fps). */
	public static final float BREATH = 0.1f, BREATH_HZ = 0.5f, SWAP_LASTING = 0.4f, SPIN_FPS = 6f;
	/** The under-glow's alpha and size (a share of the star's), the core's moment. */
	public static final float UNDER = 0.25f, UNDER_SIZE = 1.5f, CORE_FOR = 0.1f;
	/** The level of the pool a burst behind a sprite lays on the ground (a share of a pool's cap). */
	public static final float BEHIND_POOL = 0.6f;

	private static final Flare[] bursts = new Flare[MOST];
	private static int burstCount;

	//the star families, A and B, their sizes
	static final int[][] STAR4_A = { FxFrames.Light.STAR4_9_A, FxFrames.Light.STAR4_15_A, FxFrames.Light.STAR4_23_A, FxFrames.Light.STAR4_31_A };
	static final int[][] STAR4_B = { FxFrames.Light.STAR4_9_B, FxFrames.Light.STAR4_15_B, FxFrames.Light.STAR4_23_B, FxFrames.Light.STAR4_31_B };
	static final int[] STAR4_SIZES = { 9, 15, 23, 31 };
	static final int[][] STAR8_A = { FxFrames.Light.STAR8_15_A, FxFrames.Light.STAR8_23_A, FxFrames.Light.STAR8_31_A, FxFrames.Light.STAR8_47_A };
	static final int[][] STAR8_B = { FxFrames.Light.STAR8_15_B, FxFrames.Light.STAR8_23_B, FxFrames.Light.STAR8_31_B, FxFrames.Light.STAR8_47_B };
	static final int[] STAR8_SIZES = { 15, 23, 31, 47 };

	public Flare( int nRays, float radius ) {

		super( 0, 0, 0, 0 );

		this.nRays = nRays;
		this.radius = radius;

		angle = 45;
		angularSpeed = 180;

		boolean four = nRays <= 4;
		famA = four ? STAR4_A : STAR8_A;
		famB = four ? STAR4_B : STAR8_B;
		int[] sizes = four ? STAR4_SIZES : STAR8_SIZES;
		k = scaleFor( radius );
		size = frameFor( sizes, radius );
		if (nRays >= 9){
			innerA = STAR4_A;
			innerB = STAR4_B;
			innerSize = innerFrameFor( radius );
		}
	}

	/** The inner four-pointed star's size index for a radius: the painted size nearest the radius
	 *  (half the outer star's span) without going over, at the outer star's scale. */
	public static int innerFrameFor( float radius ){
		float want = radius / scaleFor( radius );
		int best = 0;
		for (int i = 0; i < STAR4_SIZES.length; i++) if (STAR4_SIZES[i] <= want) best = i;
		return best;
	}

	/** Its painted scale for a radius: x2 past 24 px across. */
	public static int scaleFor( float radius ){
		return radius * 2 > 24 ? 2 : 1;
	}

	/** The index of the painted size nearest 2 x radius without going over (at its scale). */
	public static int frameFor( int[] sizes, float radius ){
		float want = radius * 2 / scaleFor( radius );
		int best = 0;
		for (int i = 0; i < sizes.length; i++) if (sizes[i] <= want) best = i;
		return best;
	}

	/** The star's size index at a share p of a burst's life: up through the sizes over the first
	 *  quarter, held, one back from 70%. */
	public static int sizeAt( int target, float p ){
		if (p < 0.25f) return Math.min( target, (int)(p / 0.25f * (target + 1)) );
		if (p >= 0.7f) return Math.max( 0, target - 1 );
		return target;
	}

	/** A burst's alpha at a share p of its life. */
	public static float alphaAt( float p ){
		if (p < 0.25f) return 0.5f + 0.5f * p / 0.25f;
		if (p < 0.7f) return 1f - 0.3f * (p - 0.25f) / 0.45f;
		return Math.max( 0f, 0.7f * (1f - p) / 0.3f );
	}

	public Flare color( int color, boolean lightMode ) {
		this.lightMode = lightMode;
		hardlight( color );

		return this;
	}

	public Flare show( Visual visual, float duration ) {
		if (visual instanceof CharSprite){
			point(((CharSprite) visual).destinationCenter());
		} else {
			point(visual.center());
		}
		visual.parent.addToBack( this );
		behind = true;

		lifespan = this.duration = duration;

		return this;
	}

	public Flare show( Group parent, PointF pos, float duration ) {
		point( pos );
		parent.add( this );

		lifespan = this.duration = duration;

		return this;
	}

	//its first frame, on the render thread: a burst of light at a point in the world drawn over the
	//night's tint (a dark one stays under it, darkened with the rest), one behind a sprite kept
	//behind it (carried over the tint its star would hide the sprite) with a pool of its light on
	//the ground to read at night; every burst counted; a lasting one in the world given its pool
	private void start(){
		started = true;
		world = Arc.onScene() && camera() == Camera.main;
		if (duration > 0){
			enter( this );
			if (world && duration <= 2f){
				if (!behind){
					if (lightMode) carrier = GameScene.carryLight( this );
				} else if (parent != null){
					int drawn = (famA == STAR4_A ? STAR4_SIZES : STAR8_SIZES)[size] * k;
					pool = FxLight.pool( x, y + Arc.GROUND, Math.min( 63, drawn ), color(), BEHIND_POOL );
					pool.life( duration * 0.25f, duration * 0.45f, duration * 0.3f );
				}
			}
		} else {
			lastingPool();
		}
	}

	//a lasting one's pool, only while its star shows: a hidden aura (its char under the fog) keeps
	//following its char, and a persistent light shows on explored ground, so it would mark where
	//the char walks; put out at once when it hides, laid again when it shows (only into room under
	//the light caps, asked again each frame till there is: an aura coming into view never puts out
	//a fire's or a torch's light)
	private void lastingPool(){
		if (!world) return;
		if (visible){
			if (pool == null && parent != null && FxLight.room( 1, 1 )){
				pool = FxLight.pool( x, y + Arc.GROUND, 23, color(), 0.6f ).persistent();
				pool.follow( this, 0, Arc.GROUND );
			}
		} else if (pool != null){
			pool.putOut( 0f );
			pool = null;
		}
	}

	//its colour as its rays show it
	private int color(){
		float r = ra > 0 || ga > 0 || ba > 0 ? ra : rm, g = ra > 0 || ga > 0 || ba > 0 ? ga : gm, b = ra > 0 || ga > 0 || ba > 0 ? ba : bm;
		return (Math.round( Math.min( 1f, r ) * 255 ) << 16) | (Math.round( Math.min( 1f, g ) * 255 ) << 8) | Math.round( Math.min( 1f, b ) * 255 );
	}

	@Override
	public void update() {
		super.update();
		if (!started) start();

		if (duration > 0) {
			age += Game.elapsed;
			if ((lifespan -= Game.elapsed) <= 0) {
				killAndErase();
			}
		} else {
			age += Game.elapsed;
			lastingPool();
		}
	}

	//its parts made (no GL until they are drawn)
	private void build(){
		rays = new ArcImage( true, true );
		under = new ArcImage( true, true );
		if (innerA != null) inner = new ArcImage( true, true );
		if (duration > 0) core = new ArcImage( true, true );
	}

	//its parts set for this frame
	private void pose(){
		float p = duration > 0 ? Math.min( 1f, age / duration ) : 0f;
		float env;
		int idx, swap;
		if (duration > 0){
			idx = sizeAt( size, p );
			env = alphaAt( p );
			//cut short for a newer one: gone over what it was left
			if (cut) env *= Math.max( 0f, lifespan / CUT );
			swap = (int)(age * SPIN_FPS);
		} else {
			idx = size;
			env = 1f + BREATH * (float)Math.sin( 2 * Math.PI * BREATH_HZ * age );
			swap = (int)(age / SWAP_LASTING);
		}
		boolean b = (swap & 1) == 1;
		part( rays, b ? famB[idx] : famA[idx], k, env );
		int drawn = (famA == STAR4_A ? STAR4_SIZES : STAR8_SIZES)[idx] * k;
		int glowSize = Math.round( drawn * UNDER_SIZE );
		int gk = glowSize > 63 ? 2 : 1;
		part( under, FxFrames.Light.glow( glowSize / gk ), gk, env * UNDER );
		if (inner != null){
			int ii = duration > 0 ? Math.min( innerSize, sizeAt( innerSize, p ) ) : innerSize;
			part( inner, b ? innerA[ii] : innerB[ii], k, env );
		}
		if (core != null){
			if (age < CORE_FOR){
				part( core, FxFrames.Light.GLOW_7, 1, 1f );
				core.rgb( 0xFFFFFF );
				core.am = am;
			} else {
				core.visible = false;
			}
		}
	}

	private void part( ArcImage img, int[] frame, int scale, float alpha ){
		img.show( frame );
		img.scale.set( scale );
		img.rm = rm; img.gm = gm; img.bm = bm;
		img.ra = ra; img.ga = ga; img.ba = ba;
		img.am = am * alpha;
		img.aa = aa;
		img.centre( x, y );
		img.camera = camera();
		img.visible = img.am > 0.004f;
	}

	@Override
	public void draw() {
		if (carrier != null && carrier.exists && carrier.parent != null) return;
		drawLit();
	}

	@Override
	public void drawLit() {
		super.draw();
		if (rays == null) build();
		pose();
		if (lightMode) Blending.setLightMode();
		drawPart( under );
		drawPart( rays );
		drawPart( inner );
		drawPart( core );
		if (lightMode) Blending.setNormalMode();
	}

	private static void drawPart( ArcImage img ){
		if (img != null && img.visible) img.drawLit();
	}

	@Override
	public void kill() {
		super.kill();
		leave( this );
		if (pool != null) pool.putOut( 0.2f );
		pool = null;
		if (carrier != null) carrier.killAndErase();
		carrier = null;
		//its parts' buffers freed on the render thread, made again if it is ever drawn again
		final ArcImage a = rays, b = inner, c = under, d = core;
		rays = inner = under = core = null;
		if (a != null) Fx.post( () -> {
			a.destroy();
			if (b != null) b.destroy();
			c.destroy();
			if (d != null) d.destroy();
		} );
	}

	@Override
	public void destroy() {
		super.destroy();
		if (rays != null) rays.destroy();
		if (inner != null) inner.destroy();
		if (under != null) under.destroy();
		if (core != null) core.destroy();
		rays = inner = under = core = null;
	}

	//the bursts alive, oldest first: past MOST the oldest is cut short
	private static synchronized void enter( Flare f ){
		if (burstCount >= MOST){
			Flare oldest = bursts[0];
			System.arraycopy( bursts, 1, bursts, 0, MOST - 1 );
			burstCount--;
			if (oldest != null && oldest.lifespan > CUT){
				oldest.lifespan = CUT;
				oldest.cut = true;
			}
		}
		bursts[burstCount++] = f;
	}

	private static synchronized void leave( Flare f ){
		for (int i = 0; i < burstCount; i++){
			if (bursts[i] == f){
				System.arraycopy( bursts, i + 1, bursts, i, burstCount - i - 1 );
				bursts[--burstCount] = null;
				return;
			}
		}
	}

	/** Bursts alive now. */
	public static synchronized int bursts(){
		return burstCount;
	}
}
