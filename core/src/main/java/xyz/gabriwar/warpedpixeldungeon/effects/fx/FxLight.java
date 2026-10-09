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

package xyz.gabriwar.warpedpixeldungeon.effects.fx;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.RectF;

/**
 * A light, drawn additively in the light layer over the night's tint (GameScene.light): a round
 * glow on a source in the air, or a 2:1 pool on the ground under it. How bright it shows is its
 * level x its flicker x how plainly the hero sees it x the time of day (Fx.nightMul) x its life,
 * capped at 0.55 for a glow, 0.35 for a pool (1 for a core's 0.05 s, core()).
 *
 * A transient light (a strike, an impact: life()) shows only while the hero sees its cell; a
 * persistent one (a fire, a torch, an aura: persistent()) as plainly as he sees its ground, 0.8
 * once explored, and keeps more of itself by day. Flickers: STEADY, BREATH (+-8% at 0.3-0.6 Hz),
 * HEARTH (the hearths' three sines and a gutter now and then), TORCH (the hearth's, livelier),
 * STRIKE (a keyed strike and decay over its life), SHIMMER (+-6% at 18-24 Hz, beams only); all
 * but STRIKE within +-12%. Near water it can throw a broken reflection down it (reflect()).
 *
 * At most FxBudget.lightCap() at once (24, fewer on a struggling phone), half of them persistent:
 * a new transient takes the place of the dimmest transient, a new persistent one the dimmest
 * persistent's. Pooled in the light layer, pools behind the glows. Off the game's scene a light
 * is made all the same, on no layer (a test's).
 */
public class FxLight extends Image {

	public static final int STEADY = 0, BREATH = 1, HEARTH = 2, TORCH = 3, STRIKE = 4, SHIMMER = 5;
	public static final float CORE_CAP = 1f, GLOW_CAP = 0.55f, POOL_CAP = 0.35f;
	/** Reflections on the water at once, all lights together. */
	public static final int REFLECTIONS = 6;
	//a strike's brightness through 0.3 s (time, brightness), stretched over a light's own life
	static final float[] STRIKE_KEYS = { 0f, 1f, 0.04f, 0.85f, 0.08f, 0.3f, 0.12f, 0.85f, 0.14f, 1f,
			0.2f, 0.45f, 0.3f, 0f };
	//a reflection's frame swaps every this many seconds, and wobbles a pixel either way as it does
	static final float REFLECT_SWAP = 0.2f;
	//how far below its ground a reflection is looked for, rows
	static final int REFLECT_ROWS = 3;

	private static final FxLight[] live = new FxLight[96];
	private static int count, persistentCount, reflections;

	private boolean pool, persistent, core;
	private float level, dayShare;
	private int profile;
	private float phase, hz, ha, hb, hc, gutterIn, gutterLeft;
	private boolean hasLife;
	private float attack, hold, release, age;
	private float outFor = -1, outLeft;
	private Visual follow;
	private float fdx, fdy;
	private float cx, cy;
	//where it was last put: a move from outside since (a window's slide) moves its middle too
	private float placedX, placedY;
	private boolean reflect, reflecting;
	private Image reflection;
	private int reflectFrame;
	private float reflectSwap, reflectWobble;
	//how bright it showed last frame: the dimmest makes way
	private float shown;
	private final RectF uv = new RectF(), reflectUv = new RectF();

	public FxLight(){
		super( Assets.Effects.FX_LIGHT );
	}

	/** A new scene: none of the last one's lights left. */
	public static synchronized void reset(){
		for (int i = 0; i < count; i++) live[i] = null;
		count = persistentCount = reflections = 0;
	}

	public static synchronized int live(){
		return count;
	}

	public static synchronized int livePersistent(){
		return persistentCount;
	}

	/** Reflections shown now. */
	public static synchronized int reflecting(){
		return reflections;
	}

	/** A round glow of `size` px (7, 11, 15, 23, 31, 47, 63; past 63 a doubled one) at (x, y), in
	 *  `color` at `level` (0..1): transient and steady until told otherwise. */
	public static FxLight glow( float x, float y, int size, int color, float level ){
		return make( false, x, y, size, color, level );
	}

	/** A 2:1 pool of light on the ground, `size` px wide (15, 23, 31, 47, 63; past 63 doubled),
	 *  its middle at (x, y). */
	public static FxLight pool( float x, float y, int size, int color, float level ){
		return make( true, x, y, size, color, level );
	}

	private static FxLight make( boolean pool, float x, float y, int size, int color, float level ){
		FxLight l = GameScene.recycle( GameScene.Layer.LIGHTS, FxLight.class );
		if (l == null) l = new FxLight();
		l.setup( pool, x, y, size, color, level );
		enter( l );
		//pools first, so the glows lie over them
		if (l.parent != null){
			if (pool) l.parent.sendToBack( l );
			else l.parent.bringToFront( l );
		}
		return l;
	}

	private void setup( boolean pool, float x, float y, int size, int color, float level ){
		this.pool = pool;
		persistent = core = false;
		this.level = level;
		dayShare = Fx.DAY_TRANSIENT;
		profile = STEADY;
		phase = FxRandom.angle();
		hz = 1;
		ha = FxRandom.angle();
		hb = FxRandom.angle();
		hc = FxRandom.angle();
		gutterIn = FxRandom.Float( 3f, 9f );
		gutterLeft = 0;
		hasLife = false;
		age = 0;
		outFor = -1;
		follow = null;
		cx = x;
		cy = y;
		reflect = false;
		shown = level;
		int[] f;
		float sc = 1;
		if (size > 63){
			sc = 2;
			size = Math.round( size / 2f );
		}
		f = pool ? FxFrames.Light.pool( size ) : FxFrames.Light.glow( size );
		float tw = texture.width, th = texture.height;
		uv.set( f[0] / tw, f[1] / th, (f[0] + f[2]) / tw, (f[1] + f[3]) / th );
		frame( uv );
		scale.set( sc );
		resetColor();
		hardlight( ((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f );
		am = 0;
		visible = false;
		revive();
		place();
	}

	// ------------------------------------------------------------------ its setup

	/** Its flicker: STEADY, BREATH, HEARTH, TORCH, STRIKE or SHIMMER, from a phase of its own. */
	public FxLight flicker( int profile ){
		this.profile = profile;
		if (profile == BREATH) hz = FxRandom.Float( 0.3f, 0.6f );
		else if (profile == SHIMMER) hz = FxRandom.Float( 18f, 24f );
		else if (profile == TORCH) gutterIn = FxRandom.Float( 2f, 5f );
		return this;
	}

	/** A transient's life: up over `attack` s, held `hold` s, down over `release` s, then gone. */
	public FxLight life( float attack, float hold, float release ){
		hasLife = true;
		this.attack = Math.max( 0f, attack );
		this.hold = Math.max( 0f, hold );
		this.release = Math.max( 0.01f, release );
		age = 0;
		return this;
	}

	/** Persistent (a fire, a torch, an aura): shown as plainly as its ground is seen, 0.8 once
	 *  explored; 0.35 of it by day; until put out. */
	public FxLight persistent(){
		if (!persistent){
			persistent = true;
			hasLife = false;
			dayShare = Fx.DAY_PERSISTENT;
			persist( this );
		}
		return this;
	}

	/** The share of it that shows by day (Fx.nightMul). */
	public FxLight day( float dayShare ){
		this.dayShare = dayShare;
		return this;
	}

	/** Its level, 0..1. */
	public FxLight level( float l ){
		level = l;
		return this;
	}

	/** A core: up to full for its moment (the brightest frame of an impact, 0.05 s). */
	public FxLight core(){
		core = true;
		return this;
	}

	/** Keeps to a visual's middle, (dx, dy) off it, every frame; out when it is gone. */
	public FxLight follow( Visual v, float dx, float dy ){
		follow = v;
		fdx = dx;
		fdy = dy;
		return this;
	}

	/** Fades out over `fade` s, then gone. */
	public void putOut( float fade ){
		if (outFor >= 0 && outLeft <= fade) return;
		outFor = Math.max( 0.01f, fade );
		outLeft = outFor;
	}

	/** Throws a broken reflection down the water within three rows below it. */
	public FxLight reflect(){
		reflect = true;
		return this;
	}

	public float centerX(){
		return cx;
	}

	public float centerY(){
		return cy;
	}

	public boolean isPersistent(){
		return persistent;
	}

	/** How bright it showed last frame, 0..its cap. */
	public float shown(){
		return shown;
	}

	// ------------------------------------------------------------------ the frame

	@Override
	public void update(){
		super.update();
		if (Emitter.freezeEmitters && Game.timeTotal > 1) return;
		cx += x - placedX;
		cy += y - placedY;
		float dt = Game.elapsed;
		age += dt;

		float env = 1f;
		if (hasLife){
			if (age < attack) env = attack > 0 ? age / attack : 1f;
			else if (age > attack + hold){
				float r = (age - attack - hold) / release;
				if (r >= 1f){
					retire();
					return;
				}
				env = 1f - r;
			}
		}
		if (outFor >= 0){
			outLeft -= dt;
			if (outLeft <= 0){
				retire();
				return;
			}
			env *= outLeft / outFor;
		}
		if (follow != null){
			if (!follow.exists || follow.parent == null){
				follow = null;
				putOut( 0.2f );
			} else {
				cx = follow.x + follow.width() / 2f + fdx;
				cy = follow.y + follow.height() / 2f + fdy;
			}
		}

		float f = flickerNow( dt );
		int ground = Fx.cellAt( cx, groundY() );
		float seen = persistent ? Fx.groundSeen( ground ) : (Fx.seen( cx, groundY() ) ? 1f : 0f);
		float cap = core ? CORE_CAP : (pool ? POOL_CAP : GLOW_CAP);
		//never past its cap, a flicker's swells too
		float a = Math.min( cap, Math.max( 0f, cap * level * f * seen * Fx.nightMul( dayShare ) * env ) );
		shown = a;
		am = a;
		visible = a > 0.004f;
		place();

		if (reflect) reflectOn( ground, f * env );
	}

	//where its ground is: a pool lies on it, a glow stands 6 px over it
	private float groundY(){
		return pool ? cy : cy + 6f;
	}

	private void place(){
		x = placedX = Math.round( cx - width() / 2f );
		y = placedY = Math.round( cy - height() / 2f );
	}

	private float flickerNow( float dt ){
		switch (profile){
			case BREATH:
				return 1f + 0.08f * (float)Math.sin( 2 * Math.PI * hz * age + phase );
			case SHIMMER:
				return 1f + 0.06f * (float)Math.sin( 2 * Math.PI * hz * age + phase );
			case HEARTH:
			case TORCH: {
				float dip = 0;
				if (gutterLeft > 0){
					gutterLeft -= dt;
					dip = 0.22f * (float)Math.sin( Math.PI * (1f - Math.max( 0f, gutterLeft ) / 0.45f) );
				} else if ((gutterIn -= dt) <= 0){
					gutterLeft = 0.45f;
					gutterIn = profile == TORCH ? FxRandom.Float( 2f, 5f ) : FxRandom.Float( 3f, 9f );
				}
				float raw = 2.2f * FxCurves.hearth( age, ha, hb, hc ) - dip;
				if (profile == TORCH) raw *= 1.4f;
				return Math.max( 0.88f, Math.min( 1.12f, 1f + 0.6f * raw ) );
			}
			case STRIKE: {
				float span = hasLife ? attack + hold + release : 0.3f;
				return FxCurves.keyed( STRIKE_KEYS, age * 0.3f / Math.max( 0.01f, span ) );
			}
			default:
				return 1f;
		}
	}

	//a broken streak of it down the first water within REFLECT_ROWS rows below its ground, kept a
	//couple of px off the shore's lips, swapping and wobbling every REFLECT_SWAP s
	private void reflectOn( int ground, float strength ){
		Level level = Dungeon.level;
		int water = -1;
		if (level != null && ground >= 0 && FxBudget.reflections() && visible){
			int w = level.width();
			for (int k = 1; k <= REFLECT_ROWS; k++){
				int c = ground + k * w;
				if (c >= level.length()) break;
				if (level.water[c]){
					water = c;
					break;
				}
			}
		}
		float seen = water >= 0 ? Fx.groundSeen( water ) : 0f;
		if (seen <= 0 || !claimReflection()){
			hideReflection();
			return;
		}
		if (reflection == null){
			reflection = new Image( Assets.Effects.FX_SHEET ){
				@Override
				public void draw(){
					Blending.setLightMode();
					super.draw();
					Blending.setNormalMode();
				}
			};
			if (parent != null) parent.add( reflection );
		} else if (reflection.parent == null && parent != null){
			parent.add( reflection );
		}
		if ((reflectSwap -= Game.elapsed) <= 0 || !reflection.visible){
			reflectSwap = REFLECT_SWAP;
			reflectFrame = (reflectFrame + 1 + FxRandom.Int( 2 )) % FxFrames.REFLECT.length;
			reflectWobble = FxRandom.IntRange( -1, 1 );
			int[] f = FxFrames.REFLECT[reflectFrame];
			float tw = reflection.texture.width, th = reflection.texture.height;
			reflectUv.set( f[0] / tw, f[1] / th, (f[0] + f[2]) / tw, (f[1] + f[3]) / th );
			reflection.frame( reflectUv );
		}
		int w = level.width();
		float x0 = (water % w) * DungeonTilemap.SIZE, y0 = (water / w) * DungeonTilemap.SIZE;
		float left = x0 + (level.water[water - 1] ? 0 : 2);
		float right = x0 + DungeonTilemap.SIZE - reflection.width
				- (water + 1 < level.length() && level.water[water + 1] ? 0 : 2);
		reflection.x = Math.round( Math.max( left, Math.min( right, cx - reflection.width / 2f + reflectWobble ) ) );
		reflection.y = y0 + (level.water[water - w] ? 0 : 2);
		reflection.rm = rm;
		reflection.gm = gm;
		reflection.bm = bm;
		reflection.am = Math.min( POOL_CAP, POOL_CAP * this.level * strength ) * seen;
		reflection.visible = reflection.am > 0.004f;
	}

	private boolean claimReflection(){
		if (reflecting) return true;
		synchronized (FxLight.class){
			if (reflections >= REFLECTIONS) return false;
			reflections++;
		}
		reflecting = true;
		return true;
	}

	private void hideReflection(){
		if (reflection != null) reflection.visible = false;
		if (reflecting){
			reflecting = false;
			synchronized (FxLight.class){
				reflections--;
			}
		}
	}

	@Override
	public void draw(){
		Blending.setLightMode();
		super.draw();
		Blending.setNormalMode();
	}

	//gone: off its count, its reflection hidden, ready to be recycled
	private void retire(){
		hideReflection();
		kill();
	}

	@Override
	public void kill(){
		super.kill();
		leave( this );
	}

	// ------------------------------------------------------------------ the count

	private static synchronized void enter( FxLight l ){
		for (int i = 0; i < count; i++) if (live[i] == l) return;
		if (count >= FxBudget.lightCap() || count >= live.length){
			FxLight victim = dimmest( false );
			if (victim == null) victim = dimmest( true );
			if (victim != null) victim.retire();
		}
		if (count < live.length){
			live[count++] = l;
			FxBudget.add( FxBudget.LIGHTS, 1 );
		}
	}

	private static synchronized void persist( FxLight l ){
		if (persistentCount >= FxBudget.persistentLightCap()){
			FxLight victim = null;
			for (int i = 0; i < count; i++){
				FxLight o = live[i];
				if (o != l && o.persistent && (victim == null || o.shown < victim.shown)) victim = o;
			}
			if (victim != null) victim.retire();
		}
		persistentCount++;
	}

	private static FxLight dimmest( boolean persistent ){
		FxLight best = null;
		for (int i = 0; i < count; i++){
			FxLight o = live[i];
			if (o.persistent == persistent && (best == null || o.shown < best.shown)) best = o;
		}
		return best;
	}

	private static synchronized void leave( FxLight l ){
		for (int i = 0; i < count; i++){
			if (live[i] == l){
				live[i] = live[--count];
				live[count] = null;
				FxBudget.add( FxBudget.LIGHTS, -1 );
				if (l.persistent) persistentCount--;
				l.persistent = false;
				return;
			}
		}
	}
}
