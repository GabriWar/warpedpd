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

import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;

/**
 * What a bolt is besides its trail (MagicMissile pours that as it always did): a head painted
 * for its kind, the light it carries along (a glow, and a pool on the ground under it), a bloom
 * and three sparks as it leaves, a ring and a glint on every water cell it crosses, the bits it
 * sheds, and the impact it lands with, made on the arrival frame before the bolt's callback.
 *
 * Every bolt flies at raisedTileCenterToWorld, so its ground lies Arc.GROUND px under its middle.
 * The first RICH bolts at once get all of it; later ones their head and impact only. Cones (types
 * of 100 and up) carry nothing per ray but their trail; the ray that holds the cone's callback
 * carries one glow and a pool sweeping along. Render thread (MagicMissile.update); FxRandom only.
 */
public final class BoltFx {

	/** Bolts at once with every layer (light, bits, water). */
	public static final int RICH = 4;
	/** The launch's bloom (its alpha, its life), the sparks thrown toward the target. */
	public static final float LAUNCH_ALPHA = 0.6f, LAUNCH_LIFE = 0.1f;
	public static final int LAUNCH_SPARKS = 3;
	/** The glow a bolt carries and the pool under it, their alphas. */
	public static final float GLOW_ALPHA = 0.35f, POOL_ALPHA = 0.2f;
	/** A water cell crossed: its glint's alpha. */
	public static final float WATER_GLINT = 0.25f;
	/** The most bits a bolt sheds alive at once (embers, drips, wisps, seeds). */
	public static final int BITS = 10;

	private static int richLive, conesLive;
	//the scene the counts are of (its light layer): a bolt left on the last scene when the level
	//changed is never let go of, so a new scene starts the counts again
	private static Group countedIn;
	private Group counted;

	private int type;
	private boolean cone, rich, timing, live;
	private int color;
	private float dirX, dirY;
	private float age;
	private float cx, cy;
	private ArcImage head, head2, head3;
	private FxLight glow, pool;
	private float bitIn;
	private int bitsOut;
	private float bitsFor;
	private int lastCell = -1;
	private float[] bitLife = new float[BITS];

	/** Live bolts with every layer, and live cone rays. */
	public static int richLive(){
		return countedIn == GameScene.layer( GameScene.Layer.LIGHTS ) ? richLive : 0;
	}

	public static int conesLive(){
		return countedIn == GameScene.layer( GameScene.Layer.LIGHTS ) ? conesLive : 0;
	}

	/** The colour of a bolt's light, by type. */
	public static int lightOf( int type ){
		switch (type % 100){
			case MagicMissile.MAGIC_MISSILE: return Element.ARCANE.light;
			case MagicMissile.FROST:         return Element.FROST.light;
			case MagicMissile.FIRE:          return 0xFF9A40;
			case MagicMissile.CORROSION:     return 0xC8F040;
			case MagicMissile.FOLIAGE:       return 0x98E070;
			case MagicMissile.FORCE:         return 0xE8E0D0;
			case MagicMissile.BEACON:        return 0xE0D0FF;
			case MagicMissile.SHADOW:        return 0x9A5AD0;
			case MagicMissile.RAINBOW:       return Element.PRISM.light;
			case MagicMissile.EARTH:         return Element.EARTH.light;
			case MagicMissile.WARD:          return 0xB080FF;
			case MagicMissile.SHAMAN_RED:    return 0xFF4D4D;
			case MagicMissile.SHAMAN_BLUE:   return 0x6699FF;
			case MagicMissile.SHAMAN_PURPLE: return 0xBB33FF;
			case MagicMissile.SHAMAN_ELMO:   return 0x66EE99;
			case MagicMissile.ELMO:          return Element.ELMO.light;
			case MagicMissile.POISON:        return 0xA8F050;
			case MagicMissile.LIGHT_MISSILE: return Element.HOLY.light;
			default:                         return Element.ARCANE.light;
		}
	}

	//cones of their own colour: purple 111, spark 112, blood 113, poison 114
	private static int coneLight( int type ){
		switch (type){
			case MagicMissile.PURPLE_CONE: return 0xC060FF;
			case MagicMissile.SPARK_CONE:  return Element.ELECTRIC.light;
			case MagicMissile.BLOOD_CONE:  return 0xFF5050;
			case MagicMissile.POISON_CONE: return 0xA8F050;
			default:                       return lightOf( type );
		}
	}

	/** Whether a type's head and trail are matter (normal blend, under the night's tint). */
	public static boolean matter( int type ){
		switch (type % 100){
			case MagicMissile.CORROSION: case MagicMissile.FOLIAGE: case MagicMissile.SHADOW:
			case MagicMissile.EARTH: case MagicMissile.POISON:
				return true;
			default:
				return false;
		}
	}

	// ------------------------------------------------------------------ setting off

	/**
	 * A bolt sets off from (x, y) toward its way (vx, vy px/s): its head, its light, the launch's
	 * bloom and sparks. `timing` says it holds the callback (a cone's timing ray).
	 */
	public void begin( int type, float x, float y, float vx, float vy, boolean timing ){
		end();
		this.type = type;
		this.timing = timing;
		cone = type >= MagicMissile.MAGIC_MISS_CONE && type < MagicMissile.SPECK;
		live = type < MagicMissile.SPECK;
		age = 0;
		cx = x;
		cy = y;
		lastCell = -1;
		bitIn = 0;
		bitsOut = 0;
		float len = (float)Math.sqrt( vx * vx + vy * vy );
		dirX = len > 0 ? vx / len : 1;
		dirY = len > 0 ? vy / len : 0;
		if (!live || !Arc.onScene()) {
			live = false;
			return;
		}
		color = cone ? coneLight( type ) : lightOf( type );
		Group scene = GameScene.layer( GameScene.Layer.LIGHTS );
		if (scene != countedIn){
			countedIn = scene;
			richLive = conesLive = 0;
		}
		counted = scene;

		if (cone){
			conesLive++;
			if (timing){
				head = ArcImage.get( GameScene.Layer.LIGHTS, true, true );
				head.show( FxFrames.Light.GLOW_7 );
				head.visible = false;
				glow = Arc.glow( x, y, 15, color, GLOW_ALPHA );
				pool = Arc.pool( x, y + Arc.GROUND, 31, color, POOL_ALPHA );
				if (glow != null) glow.follow( head, 0, 0 );
				if (pool != null) pool.follow( head, 0, Arc.GROUND );
				head.centre( x, y );
			}
			return;
		}

		rich = richLive < RICH;
		if (rich) richLive++;
		makeHead( x, y );
		if (rich && type != MagicMissile.EARTH){
			glow = Arc.glow( x, y, 15, color, type == MagicMissile.SHADOW ? 0.25f : GLOW_ALPHA );
			pool = Arc.pool( x, y + Arc.GROUND, 15, color, POOL_ALPHA );
			if (glow != null) glow.follow( head, 0, 0 );
			if (pool != null) pool.follow( head, 0, Arc.GROUND );
		}
		//off: a bloom on the caster's hand and three sparks thrown the bolt's way
		FxLight l = Arc.glow( x, y, 11, color, LAUNCH_ALPHA );
		if (l != null) l.life( 0, 0, LAUNCH_LIFE );
		if (rich && !matter( type )){
			for (int i = 0; i < LAUNCH_SPARKS; i++){
				FxParticle p = Arc.light( x, y, FxBudget.P2 );
				if (p == null) break;
				float a = (float)Math.atan2( dirY, dirX ) + FxRandom.Float( -0.45f, 0.45f );
				p.look( FxFrames.SPARK_2 ).orient().slow( FxFrames.MOTE_1, 12f ).paint( color )
						.polar( a, FxRandom.Float( 40f, 70f ) ).drag( 4f ).life( FxRandom.Float( 0.15f, 0.25f ) )
						.alpha( 1f, 0, 0.1f );
			}
		}
	}

	//the head, painted for its kind: up to three images
	private void makeHead( float x, float y ){
		switch (type){
			case MagicMissile.MAGIC_MISSILE: default:
				head = light( FxFrames.Light.GLOW_7, 0xFFFFFF, 0.9f );
				head2 = sheet( FxFrames.MOTE_1, 0xFFFFFF, 1f, true );
				break;
			case MagicMissile.FROST:
				head = sheet( FxFrames.STAR4_5, 0xFFFFFF, 1f, true );
				head2 = light( FxFrames.Light.GLOW_7, Element.FROST.light, 0.5f );
				break;
			case MagicMissile.FIRE:
				head = sheet( FxFrames.TONGUE_M[0], 0xFFD24A, 1f, true );
				head2 = light( FxFrames.Light.GLOW_11, 0xFF9A40, 0.45f );
				break;
			case MagicMissile.CORROSION:
				head = sheet( FxFrames.BUBBLE_3, 0xD8F048, 1f, false );
				head2 = light( FxFrames.Light.GLOW_7, 0xC8F040, 0.3f );
				break;
			case MagicMissile.FOLIAGE:
				head = sheet( FxFrames.LEAF_D[0], BoltTrails.leafB(), 1f, false );
				head2 = sheet( FxFrames.LEAF_E[0], BoltTrails.leafA(), 1f, false );
				head3 = light( FxFrames.Light.GLOW_7, 0x98E070, 0.25f );
				break;
			case MagicMissile.FORCE:
				head = light( FxFrames.Light.GLOW_11, 0xE8E0D0, 0.35f );
				break;
			case MagicMissile.BEACON:
				head = light( FxFrames.Light.GLOW_7, 0xE0D0FF, 0.6f );
				break;
			case MagicMissile.SHADOW:
				head = sheet( FxFrames.INK_CORE, 0x4A2A6A, 1f, false );
				head2 = light( FxFrames.Light.GLOW_7, 0x9A5AD0, 0.35f );
				break;
			case MagicMissile.RAINBOW:
				head = light( FxFrames.Light.GLOW_11, Element.PRISM.ramp.stop( 0 ), 0.5f );
				head2 = light( FxFrames.Light.GLOW_7, 0xFFFFFF, 1f );
				break;
			case MagicMissile.EARTH:
				head = sheet( FxFrames.PEBBLE_3, 0xB89C6C, 1f, false );
				break;
			case MagicMissile.WARD:
				head = sheet( FxFrames.RUNE_3[0], 0xD8C0FF, 1f, true );
				head2 = light( FxFrames.Light.GLOW_7, 0xB080FF, 0.45f );
				break;
			case MagicMissile.SHAMAN_RED: case MagicMissile.SHAMAN_BLUE:
			case MagicMissile.SHAMAN_PURPLE: case MagicMissile.SHAMAN_ELMO:
				head = light( FxFrames.Light.GLOW_7, color, 0.8f );
				head2 = sheet( FxFrames.MOTE_1, 0xFFFFFF, 1f, true );
				break;
			case MagicMissile.ELMO:
				head = sheet( FxFrames.TONGUE_S[0], Element.ELMO.ramp.stop( 1 ), 1f, true );
				head2 = light( FxFrames.Light.GLOW_7, Element.ELMO.light, 0.45f );
				break;
			case MagicMissile.POISON:
				head = sheet( FxFrames.BUBBLE_3, 0x8AD040, 1f, false );
				head2 = light( FxFrames.Light.GLOW_7, 0xA8F050, 0.3f );
				break;
			case MagicMissile.LIGHT_MISSILE:
				head = sheet( FxFrames.STAR4_5, 0xFFF6CC, 1f, true );
				head2 = light( FxFrames.Light.GLOW_11, Element.HOLY.light, 0.45f );
				if (rich){
					head3 = sheet( FxFrames.STAR4_5, Element.HOLY.light, 0.4f, true );
				}
				break;
		}
		place( x, y );
	}

	private ArcImage light( int[] frame, int color, float alpha ){
		ArcImage a = ArcImage.get( GameScene.Layer.LIGHTS, true, true );
		a.show( frame ).rgb( color );
		a.am = alpha;
		return a;
	}

	private ArcImage sheet( int[] frame, int color, float alpha, boolean additive ){
		ArcImage a = ArcImage.get( additive ? GameScene.Layer.LIGHTS : GameScene.Layer.EFFECTS, false, additive );
		a.show( frame ).rgb( color );
		a.am = alpha;
		return a;
	}

	// ------------------------------------------------------------------ in flight

	/** A frame of its flight, its middle now at (x, y). */
	public void step( float x, float y, float dt ){
		if (!live) return;
		cx = x;
		cy = y;
		age += dt;
		place( x, y );
		if (cone || !rich) return;

		//a ring and a glint on every water cell it crosses
		int cell = Fx.cellAt( x, y + Arc.GROUND );
		if (cell != lastCell){
			lastCell = cell;
			if (WaterFX.onWater( x, y + Arc.GROUND )){
				WaterFX.ring( x, y + Arc.GROUND, WaterFX.S, color, 0.5f, FxBudget.P2 );
				FxLight g = Arc.glow( x, y + Arc.GROUND, 7, color, WATER_GLINT );
				if (g != null) g.life( 0, 0.05f, 0.3f );
			}
		}

		//the bits it sheds
		for (int i = 0; i < bitsOut; i++) bitLife[i] -= dt;
		int k = 0;
		for (int i = 0; i < bitsOut; i++) if (bitLife[i] > 0) bitLife[k++] = bitLife[i];
		bitsOut = k;
		if ((bitIn -= dt) <= 0){
			bitIn = shed( x, y );
			if (bitsFor > 0 && bitsOut < BITS) bitLife[bitsOut++] = bitsFor;
		}
	}

	//its head, painted where it is now
	private void place( float x, float y ){
		if (head == null) return;
		switch (type){
			case MagicMissile.MAGIC_MISSILE: default: {
				head.centre( x, y );
				//its 1-px core wobbling a pixel either way at 9 Hz
				int w = Math.round( (float)Math.sin( age * 9f * 2f * Math.PI ) );
				if (head2 != null) head2.centre( x - dirY * w, y + dirX * w );
				break;
			}
			case MagicMissile.FROST:
				head.show( ((int)(age * 10f) & 1) == 0 ? FxFrames.STAR4_5 : FxFrames.PLUS_5 ).centre( x, y );
				if (head2 != null) head2.centre( x, y );
				break;
			case MagicMissile.FIRE:
				head.show( FxFrames.TONGUE_M[(int)(age * 14f) % FxFrames.TONGUE_M.length] ).centre( x, y - 1 );
				if (head2 != null) head2.centre( x, y );
				break;
			case MagicMissile.FOLIAGE: {
				//two leaves turning round each other, 2 px off its middle at 12 rad/s
				float a = age * 12f;
				int ox = Math.round( 2f * (float)Math.cos( a ) ), oy = Math.round( 2f * (float)Math.sin( a ) );
				head.show( FxFrames.LEAF_D[(int)(age * 6f) & 1] ).centre( x + ox, y + oy );
				if (head2 != null) head2.show( FxFrames.LEAF_E[(int)(age * 6f) & 1] ).centre( x - ox, y - oy );
				if (head3 != null) head3.centre( x, y );
				break;
			}
			case MagicMissile.FORCE:
				//a pale ring of force pulsing over two frames
				head.show( ((int)(age * 15f) & 1) == 0 ? FxFrames.Light.GLOW_11 : FxFrames.Light.GLOW_7 ).centre( x, y );
				break;
			case MagicMissile.RAINBOW: {
				float p = (age / 0.5f) % 1f;
				head.rgb( Element.PRISM.ramp.at( p ) );
				head.centre( x, y );
				if (head2 != null) head2.centre( x, y );
				break;
			}
			case MagicMissile.EARTH:
				//a pebble turning a quarter at a time
				head.mirror( ((int)(age / 0.06f) & 1) == 1, ((int)(age / 0.12f) & 1) == 1 );
				head.show( ((int)(age / 0.06f) & 1) == 0 ? FxFrames.PEBBLE_3 : FxFrames.PEBBLE_3V ).centre( x, y );
				break;
			case MagicMissile.WARD:
				head.show( FxFrames.RUNE_3[(int)(age * 8f) % FxFrames.RUNE_3.length] ).centre( x, y );
				if (head2 != null) head2.centre( x, y );
				break;
			case MagicMissile.SHAMAN_RED: case MagicMissile.SHAMAN_BLUE:
			case MagicMissile.SHAMAN_PURPLE: case MagicMissile.SHAMAN_ELMO: {
				//a zigzag 2 px either side of its way, stepped at 10 Hz
				int z = ((int)(age * 10f) & 1) == 0 ? 2 : -2;
				head.centre( x - dirY * z, y + dirX * z );
				if (head2 != null) head2.centre( x - dirY * z, y + dirX * z );
				break;
			}
			case MagicMissile.ELMO:
				head.show( FxFrames.TONGUE_S[(int)(age * 12f) % FxFrames.TONGUE_S.length] ).centre( x, y - 1 );
				if (head2 != null) head2.centre( x, y );
				break;
			case MagicMissile.LIGHT_MISSILE:
				head.show( ((int)(age * 10f) & 1) == 0 ? FxFrames.STAR4_5 : FxFrames.PLUS_5 ).centre( x, y );
				if (head2 != null) head2.centre( x, y );
				//a ghost of it 4 px behind (and its glow's echo at 8)
				if (head3 != null) head3.show( head.shown() ).centre( x - dirX * 4, y - dirY * 4 );
				break;
			case MagicMissile.CORROSION: case MagicMissile.POISON: case MagicMissile.SHADOW: case MagicMissile.BEACON:
				head.centre( x, y );
				if (head2 != null) head2.centre( x, y );
				break;
		}
	}

	//sheds one bit (an ember, a drip, a wisp, a seed); the seconds to the next. bitsFor: its life
	private float shed( float x, float y ){
		bitsFor = 0;
		if (bitsOut >= BITS) return 0.05f;
		FxParticle p;
		switch (type){
			case MagicMissile.MAGIC_MISSILE: default:
				//a plus of light drifting off its way at 6 px/s
				if ((p = Arc.light( x, y, FxBudget.P2 )) != null){
					float s = FxRandom.sign();
					p.look( FxFrames.PLUS_3 ).paint( Element.ARCANE.ramp.stop( 1 ) ).vel( -dirY * 6f * s, dirX * 6f * s )
							.life( 0.4f ).alpha( 0.9f, 0, 0.2f ).snap();
					bitsFor = 0.4f;
				}
				return 0.06f;
			case MagicMissile.FROST:
				//glints sinking off it
				if ((p = Arc.light( x + FxRandom.Float( -2, 2 ), y + FxRandom.Float( -2, 2 ), FxBudget.P2 )) != null){
					p.look( Arc.GLINT, 0 ).paint( 0xE8F6FF ).gravity( 20f ).life( 0.6f ).alpha( 1f, 0, 0.2f );
					bitsFor = 0.6f;
				}
				return 0.08f;
			case MagicMissile.FIRE: {
				FxEmitter e = Fx.glowAir();
				if (e != null){
					FxFactories.ember( Element.FIRE ).emit( e, 0, x + FxRandom.Float( -2, 2 ), y + FxRandom.Float( -2, 2 ) );
					bitsFor = 0.8f;
				}
				return 0.05f;
			}
			case MagicMissile.CORROSION: case MagicMissile.POISON:
				//a drip falling to the ground under it: a splat, or a ring on water
				if ((p = Arc.matter( x, y + 1, FxBudget.P2 )) != null){
					int c = type == MagicMissile.CORROSION ? 0xA8C030 : 0x6CCC3C;
					p.look( FxFrames.DROP_2 ).orient().paint( c ).mark( c ).vel( dirX * 10f, 0 ).gravity( 160f )
							.ground( y + Arc.GROUND, FxParticle.RING, 0 ).landDry( FxParticle.SPLAT ).strength( 0.1f )
							.life( 1f ).alpha( 1f, 0, 0.05f );
					bitsFor = 0.4f;
				}
				return 0.1f;
			case MagicMissile.FOLIAGE:
				if ((p = Arc.matter( x, y, FxBudget.P2 )) != null){
					p.look( FxFrames.SEED ).paint( 0xC8E070 ).vel( FxRandom.Float( -4, 4 ), -4f ).gravity( 28f ).drag( 2f )
							.life( 0.6f ).alpha( 1f, 0, 0.2f );
					bitsFor = 0.6f;
				}
				return 0.08f;
			case MagicMissile.SHADOW:
				//ink rising off it at 6 px/s
				if ((p = Arc.matter( x + FxRandom.Float( -2, 2 ), y, FxBudget.P2 )) != null){
					p.look( FxFrames.INK_WISP, 6 ).paint( 0x2E1648 ).vel( 0, -6f ).life( 0.6f ).alpha( 0.8f, 0.1f, 0.3f ).smooth();
					bitsFor = 0.6f;
				}
				return 0.08f;
			case MagicMissile.WARD:
				if ((p = Arc.light( x, y, FxBudget.P3 )) != null){
					p.look( FxFrames.RUNE_3, 0 ).paint( 0xB080FF ).vel( FxRandom.Float( -3, 3 ), -8f ).life( 0.5f ).alpha( 0.8f, 0, 0.2f ).snap();
					bitsFor = 0.5f;
				}
				return 0.1f;
			case MagicMissile.LIGHT_MISSILE:
				if ((p = Arc.light( x, y, FxBudget.P3 )) != null){
					p.look( FxFrames.MOTE_1 ).paint( 0xFFE070 ).vel( FxRandom.Float( -4, 4 ), -6f ).drag( 2f ).life( 0.5f ).alpha( 1f, 0, 0.25f ).snap();
					bitsFor = 0.5f;
				}
				return 0.07f;
			case MagicMissile.FORCE: case MagicMissile.BEACON: case MagicMissile.RAINBOW: case MagicMissile.EARTH:
			case MagicMissile.SHAMAN_RED: case MagicMissile.SHAMAN_BLUE: case MagicMissile.SHAMAN_PURPLE:
			case MagicMissile.SHAMAN_ELMO: case MagicMissile.ELMO:
				return 1f;
		}
	}

	// ------------------------------------------------------------------ landing

	/** It lands at (x, y): the impact of its kind, before its callback. */
	public void arrive( float x, float y ){
		if (!live){
			end();
			return;
		}
		cx = x;
		cy = y;
		if (cone){
			if (timing){
				//the cones of a bolt's kind land as it does; the purple, spark, blood and poison
				//cones with a flash and a ring of their colour
				if (type <= MagicMissile.WARD_CONE) impact( type - MagicMissile.MAGIC_MISS_CONE, x, y, 0.8f );
				else {
					Arc.core( x, y, Arc.mix( color, 0xFFFFFF, 0.5f ) );
					Arc.ring( x, y + Arc.GROUND, 9, 23, 0.35f, color, 0.5f, true );
					Arc.motes( x, y, 6, color, 20f, 40f, 0.4f );
				}
			} else if (FxBudget.keep( FxBudget.P3, x, y )){
				Arc.ring( x, y + Arc.GROUND, 5, 13, 0.3f, color, 0.4f, !matter( type ) );
			}
		} else {
			impact( type, x, y, rich ? 1f : 0.6f );
		}
		end();
	}

	/** Lets go of its head and its light (a light fades over 0.15 s). */
	public void end(){
		if (head != null) head.kill();
		if (head2 != null) head2.kill();
		if (head3 != null) head3.kill();
		head = head2 = head3 = null;
		if (glow != null) glow.putOut( 0.15f );
		if (pool != null) pool.putOut( 0.15f );
		glow = pool = null;
		if (live && counted == countedIn){
			if (cone) conesLive = Math.max( 0, conesLive - 1 );
			else if (rich) richLive = Math.max( 0, richLive - 1 );
		}
		live = false;
		counted = null;
		rich = false;
	}

	/**
	 * A bolt's impact by type at (x, y) (its middle; its ground Arc.GROUND below), its bits by
	 * `scale` (the later bolts' and the cones' smaller). Render thread.
	 */
	public static void impact( int type, float x, float y, float scale ){
		float gy = y + Arc.GROUND;
		boolean water = WaterFX.onWater( x, gy );
		switch (type){
			case MagicMissile.MAGIC_MISSILE: default:
				Arc.core( x, y, 0xFFFFFF );
				Arc.ring( x, gy, 9, 17, 0.3f, Element.ARCANE.light, 0.6f, true );
				Arc.motes( x, y, Math.round( 6 * scale ), Element.ARCANE.ramp.stop( 1 ), 20f, 40f, 0.4f );
				pool( x, gy, 23, Element.ARCANE.light, 0.3f, 0.35f );
				break;
			case MagicMissile.FROST:
				Arc.core( x, y, 0xE8F6FF );
				Arc.ring( x, gy, 9, 23, 0.4f, 0xE8F6FF, 0.25f + (water ? 0.3f : 0f), true );
				for (int i = 0; i < Math.round( (water ? 4 : 6) * scale ); i++){
					FxParticle p = Arc.light( x + FxRandom.Float( -6, 6 ), y + FxRandom.Float( -6, 2 ), FxBudget.P2 );
					if (p != null) p.look( Arc.GLINT, 0 ).paint( 0xE8F6FF ).gravity( 40f ).life( 0.6f ).alpha( 1f, 0, 0.2f );
				}
				if (!water){
					FxDecal.at( x, gy, FxFrames.RIME[FxRandom.Int( FxFrames.RIME.length )], 0xE8F6FF, 0.55f, 1.2f, 0.8f, FxRandom.chance( 0.5f ) );
				}
				pool( x, gy, 23, Element.FROST.light, 0.3f, 0.4f );
				break;
			case MagicMissile.FIRE: {
				Arc.core( x, y, 0xFFF3C8 );
				FxEmitter e = GameScene.lightEmitter();
				if (e != null){
					e.pos( x - 4, y - 3, 8, 6 );
					e.burst( FxFactories.tongue( Element.FIRE ), Math.round( 8 * scale ) );
				}
				FxRecipes.embers( x, y, Element.FIRE, Math.round( 6 * scale ) );
				if (water){
					FxRecipes.steam( x, gy, 4 );
					WaterFX.ring( x, gy, WaterFX.M, 0xFFC090, 0.6f );
				} else {
					FxRecipes.puff( x, y - 2, Element.FIRE, 2 );
					FxDecal.at( x, gy, FxFrames.SCORCH_S, FxRecipes.SCORCH_COLOR, 0.7f, 2f, 1f, FxRandom.chance( 0.5f ) );
				}
				pool( x, gy, 31, 0xFF9A48, 0.35f, 0.4f );
				break;
			}
			case MagicMissile.CORROSION:
				WaterFX.droplets( x, y, Math.round( 6 * scale ), 30f, 60f, 70f, 0xA8C030 );
				if (!water) FxDecal.at( x, gy, FxFrames.ETCH, 0x6A7A20, 0.7f, 2f, 1f, FxRandom.chance( 0.5f ) );
				else WaterFX.ring( x, gy, WaterFX.M, 0xC8F040, 0.6f );
				for (int i = 0; i < 2; i++){
					FxParticle p = Arc.light( x, gy - 1, FxBudget.P2 );
					if (p != null) p.look( FxFrames.SPARK_2 ).orient().paint( 0xFFB040 ).polar( FxRandom.Float( (float)-Math.PI, 0 ), FxRandom.Float( 20, 40 ) )
							.gravity( 120f ).life( 0.25f ).alpha( 1f, 0, 0.1f );
				}
				pool( x, gy, 15, 0xC8F040, 0.2f, 0.3f );
				break;
			case MagicMissile.FOLIAGE:
				for (int i = 0; i < Math.round( 6 * scale ); i++){
					FxParticle p = Arc.matter( x, y, FxBudget.P2 );
					if (p == null) break;
					p.look( FxRandom.chance( 0.5f ) ? FxFrames.LEAF_D : FxFrames.LEAF_E, 6 )
							.paint( FxRandom.chance( 0.5f ) ? BoltTrails.leafA() : BoltTrails.leafB() )
							.polar( FxRandom.Float( (float)-Math.PI, 0 ), FxRandom.Float( 20, 45 ) ).gravity( 28f ).drag( 2.5f )
							.sway( 6f, 4f ).ground( gy + FxRandom.Float( -2, 2 ), water ? FxParticle.RING : FxParticle.SETTLE, 0 )
							.landDry( FxParticle.SETTLE ).life( FxRandom.Float( 1f, 1.6f ) ).alpha( 1f, 0, 0.4f );
				}
				for (int i = 0; i < 2 && !water; i++){
					FxParticle p = Arc.matter( x + FxRandom.Float( -4, 4 ), gy, FxBudget.P3 );
					if (p != null) p.look( FxFrames.BLADE_4 ).paint( BoltTrails.leafB() ).vel( FxRandom.Float( -12, 12 ), -30f )
							.gravity( 160f ).ground( gy + 1, FxParticle.DIE, 0 ).life( 0.5f ).alpha( 1f, 0, 0.1f );
				}
				pool( x, gy, 15, 0x98E070, 0.2f, 0.3f );
				break;
			case MagicMissile.FORCE:
				Arc.groundRing( x, gy, 13, 41, 0.25f, 0xE8E0D0, 0.5f, true );
				if (water) WaterFX.ring( x, gy, WaterFX.L, 0xE8E0D0, 0.6f );
				else FxRecipes.dust( x, gy - 1, Math.round( 4 * scale ) );
				break;
			case MagicMissile.BEACON:
				implosion( x, y, 0xE0D0FF, 6, 10f, 0.12f );
				shaft( x, gy, FxFrames.Light.SHAFT_32, 0xE0D0FF, 0.6f, 0.2f );
				break;
			case MagicMissile.SHADOW:
				for (int i = 0; i < Math.round( 6 * scale ); i++){
					FxParticle p = Arc.matter( x + FxRandom.Float( -5, 5 ), y + FxRandom.Float( -3, 3 ), FxBudget.P2 );
					if (p != null) p.look( FxFrames.INK_WISP, 6 ).paint( 0x2E1648 ).vel( FxRandom.Float( -4, 4 ), -FxRandom.Float( 6, 12 ) )
							.drag( 1f ).life( FxRandom.Float( 0.6f, 1f ) ).alpha( 0.85f, 0.05f, 0.4f ).smooth();
				}
				Arc.ring( x, gy, 9, 17, 0.35f, 0x9A5AD0, 0.5f, true );
				if (!water) FxDecal.at( x, gy, FxFrames.SCORCH_S, 0x1A0C2C, 0.55f, 0.8f, 0.7f, FxRandom.chance( 0.5f ) );
				break;
			case MagicMissile.RAINBOW:
				Arc.core( x, y, 0xFFFFFF );
				prism( x, y, 7, 24f, 40f );
				Arc.ring( x, gy, 9, 17, 0.3f, Element.PRISM.light, 0.5f, true );
				pool( x, gy, 23, Element.PRISM.light, 0.3f, 0.35f );
				break;
			case MagicMissile.EARTH: {
				Emitter e = GameScene.fxEmitter();
				if (e != null){
					e.pos( x, y );
					e.burst( FxFactories.debris( Element.EARTH.ember.stop( 1 ), Element.EARTH.ember.stop( 3 ) ), Math.round( 4 * scale ) );
				}
				if (water) WaterFX.splash( x, gy, WaterFX.M, Element.NONE );
				else Arc.groundRing( x, gy, 9, 23, 0.4f, 0xB89C6C, 0.5f, false );
				break;
			}
			case MagicMissile.WARD:
				Arc.ring( x, gy, 9, 17, 0.35f, 0xB080FF, 0.6f, true );
				for (int i = 0; i < 4; i++){
					float a = (float)(i * Math.PI / 2 + Math.PI / 4);
					Arc.glint( Math.round( x + 7 * (float)Math.cos( a ) ), Math.round( gy + 3.5f * (float)Math.sin( a ) ), 0xD8C0FF );
				}
				pool( x, gy, 23, 0xB080FF, 0.3f, 0.35f );
				break;
			case MagicMissile.SHAMAN_RED: case MagicMissile.SHAMAN_BLUE:
			case MagicMissile.SHAMAN_PURPLE: case MagicMissile.SHAMAN_ELMO: {
				int c = lightOf( type );
				Arc.core( x, y, Arc.mix( c, 0xFFFFFF, 0.5f ) );
				Arc.motes( x, y, Math.round( 4 * scale ), c, 15f, 30f, 0.35f );
				Arc.ring( x, gy, 5, 13, 0.3f, c, 0.5f, true );
				break;
			}
			case MagicMissile.ELMO:
				Arc.core( x, y, Element.ELMO.ramp.stop( 0 ) );
				FxRecipes.embers( x, y, Element.ELMO, Math.round( 4 * scale ) );
				Arc.ring( x, gy, 9, 17, 0.3f, Element.ELMO.light, 0.5f, true );
				break;
			case MagicMissile.POISON:
				WaterFX.droplets( x, y, Math.round( 6 * scale ), 30f, 60f, 70f, 0x6CCC3C );
				FxRecipes.puff( x, y, Element.POISON, 2 );
				if (water) WaterFX.ring( x, gy, WaterFX.M, 0xA8F050, 0.6f );
				break;
			case MagicMissile.LIGHT_MISSILE:
				Arc.core( x, y, 0xFFFFFF );
				Arc.ring( x, gy, 9, 23, 0.35f, 0xF2C050, 0.6f, true );
				for (int i = 0; i < Math.round( 6 * scale ); i++){
					FxParticle p = Arc.light( x + FxRandom.Float( -6, 6 ), y + FxRandom.Float( -2, 4 ), FxBudget.P2 );
					if (p != null) p.look( FxFrames.MOTE_2 ).paint( 0xFFE070 ).vel( 0, -FxRandom.Float( 10, 18 ) ).drag( 1f )
							.life( FxRandom.Float( 0.5f, 0.8f ) ).alpha( 1f, 0, 0.3f ).snap();
				}
				ArcFlash s = ArcFlash.light( x, y, FxFrames.Light.STAR4_9_A, 0xFFF6CC, 1f, 0.2f );
				if (s != null) s.swap( FxFrames.Light.STAR4_9_B, 0.06f ).fade( 0, 0.12f );
				pool( x, gy, 31, Element.HOLY.light, 0.35f, 0.4f );
				break;
		}
	}

	//a pool of light on the ground fading over `life` s
	static void pool( float x, float gy, int size, int color, float alpha, float life ){
		FxLight l = Arc.pool( x, gy, size, color, alpha );
		if (l != null) l.life( 0, 0.03f, life );
	}

	/** n motes falling into (x, y) from `r` px out over `seconds`: an implosion. */
	public static void implosion( float x, float y, int color, int n, float r, float seconds ){
		for (int i = 0; i < n; i++){
			float a = (float)(2 * Math.PI * i / n) + FxRandom.Float( -0.2f, 0.2f );
			float sx = x + (float)Math.cos( a ) * r, sy = y + (float)Math.sin( a ) * r;
			FxParticle p = Arc.light( sx, sy, FxBudget.P1 );
			if (p == null) return;
			p.look( FxFrames.MOTE_2 ).paint( color ).vel( (x - sx) / seconds, (y - sy) / seconds ).life( seconds )
					.alpha( 1f, seconds * 0.3f, seconds * 0.2f );
		}
	}

	/** A shaft of light standing on the ground at (x, gy), up to `alpha`, gone over `life` s. */
	public static ArcFlash shaft( float x, float gy, int[] frame, int color, float alpha, float life ){
		ArcFlash f = ArcFlash.light( x, gy - frame[3] / 2f, frame, color, alpha, life );
		if (f != null) f.fade( 0.03f, life * 0.8f ).seenAt( x, gy - Arc.GROUND );
		return f;
	}

	/** n motes of the prism's seven hues thrown out of (x, y). */
	public static void prism( float x, float y, int n, float vMin, float vMax ){
		int stops = Element.PRISM.ramp.size();
		n = FxBudget.allow( n, FxBudget.P2 );
		int start = FxRandom.Int( stops );
		for (int i = 0; i < n; i++){
			FxParticle p = Arc.light( x, y, FxBudget.P0 );
			if (p == null) return;
			float a = (float)(2 * Math.PI * i / Math.max( 1, n )) + FxRandom.Float( -0.25f, 0.25f );
			p.look( FxRandom.chance( 0.5f ) ? FxFrames.MOTE_2 : FxFrames.PLUS_3 ).paint( Element.PRISM.ramp.stop( (start + i) % stops ) )
					.polar( a, FxRandom.Float( vMin, vMax ) ).drag( 3f ).life( FxRandom.Float( 0.35f, 0.6f ) ).alpha( 1f, 0, 0.25f );
		}
	}
}
