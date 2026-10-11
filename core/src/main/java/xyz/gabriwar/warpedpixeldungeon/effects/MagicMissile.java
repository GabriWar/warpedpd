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

import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Lit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.BoltFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.BoltTrails;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BloodParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.CorrosionParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ElmoParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PurpleParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.RainbowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Visual;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Callback;
import com.watabou.utils.ColorMath;
import com.watabou.utils.PointF;

//a bolt: the trail it pours (its one Random draw is the pour in reset, as ever), and from its
//first frame on the render thread its head, its light, its launch, what it sheds and the impact
//it lands with (effects/fx/arcane/BoltFx), made on the arrival frame before its callback. A bolt
//of light is drawn over the night's tint (carried to the light layer); one of matter under it.
public class MagicMissile extends Emitter implements Lit {

	private static final float SPEED	= 200f;

	private Callback callback;

	private PointF to;

	private float sx;
	private float sy;
	private float time;

	//what it is, its flight so far, its look's parts, and its stand-in in the light layer
	private int type;
	private float total, flown;
	private boolean fresh;
	private final BoltFx fx = new BoltFx();
	private Gizmo carrier;

	//missile types
	public static final int MAGIC_MISSILE   = 0;
	public static final int FROST           = 1;
	public static final int FIRE            = 2;
	public static final int CORROSION       = 3;
	public static final int FOLIAGE         = 4;
	public static final int FORCE           = 5;
	public static final int BEACON          = 6;
	public static final int SHADOW          = 7;
	public static final int RAINBOW         = 8;
	public static final int EARTH           = 9;
	public static final int WARD            = 10;
	public static final int SHAMAN_RED      = 11;
	public static final int SHAMAN_BLUE     = 12;
	public static final int SHAMAN_PURPLE   = 13;
	public static final int SHAMAN_ELMO     = 14;
	public static final int ELMO            = 15;
	public static final int POISON          = 16;
	public static final int LIGHT_MISSILE   = 17;

	public static final int MAGIC_MISS_CONE = 100;
	public static final int FROST_CONE      = 101;
	public static final int FIRE_CONE       = 102;
	public static final int CORROSION_CONE  = 103;
	public static final int FOLIAGE_CONE    = 104;
	public static final int FORCE_CONE      = 105;
	public static final int SHADOW_CONE     = 107;
	public static final int RAINBOW_CONE    = 108;
	public static final int EARTH_CONE      = 109;
	public static final int WARD_CONE       = 110;
	public static final int PURPLE_CONE     = 111;
	public static final int SPARK_CONE      = 112;
	public static final int BLOOD_CONE      = 113;
	public static final int POISON_CONE     = 114;

	//use SPECK + the constant of the Speck you want. e.g. MagicMissile.SPECK + Speck.TOXIC
	public static final int SPECK           = 1000;

	/** A cone's pour interval per ray: CONE_RATE, or CONE_RATE x rays / CONE_RAYS when wider
	 *  (at most ~160 particles a cone). */
	public static final float CONE_RATE = 0.03f;
	/** A bolt's pour interval where its trail's particles live half a second (at most 50 alive);
	 *  those living 0.6 s pour every 0.015 s. */
	public static final float TRAIL_RATE = 0.012f;
	public static final int CONE_RAYS = 6;

	public void reset( int type, int from, int to, Callback callback ) {
		reset( type,
				DungeonTilemap.raisedTileCenterToWorld( from ),
				DungeonTilemap.raisedTileCenterToWorld( to ),
				callback );
	}

	public void reset( int type, Visual from, Visual to, Callback callback ) {
		reset( type,
				from.center(),
				to.center(),
				callback);
	}

	public void reset( int type, Visual from, int to, Callback callback ) {
		reset( type,
				from.center(),
				DungeonTilemap.raisedTileCenterToWorld( to ),
				callback);
	}

	public void reset( int type, PointF from, PointF to, Callback callback ) {
		this.callback = callback;

		this.to = to;

		x = from.x;
		y = from.y;
		width = 0;
		height = 0;

		PointF d = PointF.diff( to, from );
		PointF speed = new PointF( d ).normalize().scale( SPEED );
		sx = speed.x;
		sy = speed.y;
		time = d.length() / SPEED;
		total = time;
		flown = 0;
		this.type = type;
		fresh = true;

		//for now all specks share the same size and volume, this can easily be customized later if needed
		if (type >= SPECK){
			size( 10 );
			pour( Speck.factory(type-SPECK), 0.02f);
			revive();
			return;
		}

		switch(type){
			case MAGIC_MISSILE: default:
				size( 4 );
				pour( WhiteParticle.FACTORY, 0.01f );
				break;
			case FROST:
				pour( MagicParticle.FACTORY, TRAIL_RATE );
				break;
			case FIRE:
				size( 4 );
				pour( BoltTrails.FIRE, 0.015f );
				break;
			case CORROSION:
				size( 3 );
				pour( CorrosionParticle.MISSILE, 0.015f );
				break;
			case FOLIAGE:
				size( 4 );
				pour( BoltTrails.LEAF, 0.02f );
				break;
			case FORCE:
				pour( SlowParticle.FACTORY, 0.015f );
				break;
			case BEACON:
				pour( ForceParticle.FACTORY, TRAIL_RATE );
				break;
			case SHADOW:
				size( 4 );
				pour( ShadowParticle.MISSILE, 0.01f );
				break;
			case RAINBOW:
				size( 4 );
				pour( RainbowParticle.BURST, TRAIL_RATE );
				break;
			case EARTH:
				size( 4 );
				pour( EarthParticle.FACTORY, TRAIL_RATE );
				break;
			case WARD:
				size( 4 );
				pour( WardParticle.FACTORY, 0.015f );
				break;
			case SHAMAN_RED:
				size( 2 );
				pour( ShamanParticle.RED, 0.015f );
				break;
			case SHAMAN_BLUE:
				size( 2 );
				pour( ShamanParticle.BLUE, 0.015f );
				break;
			case SHAMAN_PURPLE:
				size( 2 );
				pour( ShamanParticle.PURPLE, 0.015f );
				break;
			case SHAMAN_ELMO:
				size( 2 );
				pour( ShamanParticle.ELMO, 0.015f );
				break;
			case ELMO:
				size( 5 );
				pour( ElmoParticle.FACTORY, 0.015f );
				break;
			case POISON:
				size( 3 );
				pour( PoisonParticle.MISSILE, 0.015f );
				break;
			case LIGHT_MISSILE:
				size( 4 );
				pour( WhiteParticle.YELLOW, 0.01f );
				break;

			case MAGIC_MISS_CONE:
				size( 10 );
				pour( WhiteParticle.FACTORY, CONE_RATE );
				break;
			case FROST_CONE:
				size( 10 );
				pour( MagicParticle.FACTORY, CONE_RATE );
				break;
			case FIRE_CONE:
				size( 10 );
				pour( BoltTrails.FIRE_CONE, CONE_RATE );
				break;
			case CORROSION_CONE:
				size( 10 );
				pour( CorrosionParticle.MISSILE, CONE_RATE );
				break;
			case FOLIAGE_CONE:
				size( 10 );
				pour( BoltTrails.LEAF_CONE, CONE_RATE );
				break;
			case FORCE_CONE:
				size( 10 );
				pour( SlowParticle.FACTORY, 0.02f );
				break;
			case SHADOW_CONE:
				size( 10 );
				pour( ShadowParticle.MISSILE, CONE_RATE );
				break;
			case RAINBOW_CONE:
				size( 10 );
				pour( RainbowParticle.BURST, CONE_RATE );
				break;
			case EARTH_CONE:
				size( 10 );
				pour( EarthParticle.FACTORY, CONE_RATE );
				break;
			case WARD_CONE:
				size( 10 );
				pour( WardParticle.FACTORY, CONE_RATE );
				break;
			case PURPLE_CONE:
				size( 10 );
				pour( PurpleParticle.MISSILE, CONE_RATE );
				break;
			case SPARK_CONE:
				size( 10 );
				pour( SparkParticle.FACTORY, CONE_RATE );
				break;
			case BLOOD_CONE:
				size( 10 );
				pour( BloodParticle.FACTORY, CONE_RATE );
				break;
			case POISON_CONE:
				size( 10 );
				pour( PoisonParticle.MISSILE, CONE_RATE );
				break;
		}

		revive();
	}

	public void size( float size ) {
		x -= size / 2;
		y -= size / 2;
		width = height = size;
	}

	public void setSpeed( float newSpeed ){
		PointF d = PointF.diff( to, new PointF(x, y) );
		PointF speed = new PointF( d ).normalize().scale( newSpeed );
		sx = speed.x;
		sy = speed.y;
		time = d.length() / newSpeed;
		total = flown + time;
	}

	/** Its velocity, px/s. */
	public float velocityX(){
		return sx;
	}

	public float velocityY(){
		return sy;
	}

	/** How far through its flight, 0..1. */
	public float progress(){
		return total > 0 ? Math.min( 1f, flown / total ) : 1f;
	}

	/** Seconds it has flown. */
	public float flown(){
		return flown;
	}

	/** Its type (MAGIC_MISSILE ... SPECK + n). */
	public int type(){
		return type;
	}

	//convenience method for the common case of a bolt going from a character to a tile or enemy
	public static MagicMissile boltFromChar(Group group, int type, Visual sprite, int to, Callback callback){
		MagicMissile missile = ((MagicMissile)group.recycle( MagicMissile.class ));
		if (Actor.findChar(to) != null){
			missile.reset(type, sprite.center(), Actor.findChar(to).sprite.destinationCenter(), callback);
		} else {
			missile.reset(type, sprite, to, callback);
		}
		return missile;
	}

	@Override
	protected boolean isFrozen() {
		return false; //cannot be frozen
	}

	//its trail's spawn points from the effects' own random numbers: the game's are not drawn here
	@Override
	protected void emit( int index ) {
		if (target == null) {
			factory.emit( this, index, x + FxRandom.Float( width ), y + FxRandom.Float( height ) );
		} else if (fillTarget) {
			factory.emit( this, index, target.x + FxRandom.Float( target.width() ), target.y + FxRandom.Float( target.height() ) );
		} else {
			factory.emit( this, index, target.x + x + FxRandom.Float( width ), target.y + y + FxRandom.Float( height ) );
		}
	}

	@Override
	public void update() {
		if (fresh && on) {
			fresh = false;
			setOff();
		}
		super.update();
		if (on) {
			float d = Game.elapsed;
			x += sx * d;
			y += sy * d;
			flown += d;
			fx.step( x + width / 2f, y + height / 2f, d );
			if (type >= MAGIC_MISS_CONE && type < SPECK) {
				int rays = Math.max( 1, BoltFx.conesLive() );
				interval = Math.max( CONE_RATE, CONE_RATE * rays / CONE_RAYS );
			}
			if ((time -= d) <= 0) {
				on = false;
				fx.arrive( x + width / 2f, y + height / 2f );
				if (callback != null ) callback.call();
			}
		}
	}

	//its first frame: its look made, and a bolt of light carried over the night's tint
	private void setOff(){
		fx.begin( type, x + width / 2f, y + height / 2f, sx, sy, callback != null );
		boolean light = factory != null && factory.lightMode();
		if (light) {
			if (carrier == null || !carrier.exists || carrier.parent == null) carrier = GameScene.carryLight( this );
		} else if (carrier != null) {
			carrier.killAndErase();
			carrier = null;
		}
	}

	@Override
	public void kill() {
		super.kill();
		fx.end();
	}

	@Override
	public void draw() {
		if (carrier != null && carrier.exists && carrier.parent != null) return;
		super.draw();
	}

	@Override
	public void drawLit() {
		super.draw();
	}

	//a size stepped down in whole pixels at 25/50/75% of a life (left/lifespan = k): 4, 3, 2, 1
	static float stepped( float size, float k ) {
		float s = k > 0.75f ? 1f : (k > 0.5f ? 0.75f : (k > 0.25f ? 0.5f : 0.25f));
		return Math.max( 1, Math.round( size * s ) );
	}

	//a colour between two, by the effects' own random numbers
	static int between( int a, int b ) {
		return Ramp.lerp( a, b, FxRandom.Float() );
	}

	//the frost bolt's trail: drifting off at random in every reset, growing 1, 2, 3, 4 px as it
	//fades down the frost's ramp. ATTRACTING: motes gathering into a charge, 1, 2, 3 px as they
	//arrive and whitening (the charge's glow at the point is its sprite's)
	public static class MagicParticle extends PixelParticle {

		public static final Emitter.Factory FACTORY = new Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((MagicParticle)emitter.recycle( MagicParticle.class )).reset( x, y );
			}
			@Override
			public boolean lightMode() {
				return true;
			}
		};

		public static final Emitter.Factory ATTRACTING = new Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((MagicParticle)emitter.recycle( MagicParticle.class )).resetAttract( x, y );
			}
			@Override
			public boolean lightMode() {
				return true;
			}
		};

		/** The share of the frost's ramp its colour runs down as it fades. */
		public static final float RAMP_SPAN = 0.6f;

		private boolean attract;

		public MagicParticle() {
			super();

			color( 0x88CCFF );
			lifespan = 0.5f;
		}

		public void reset( float x, float y ) {
			revive();

			this.x = x;
			this.y = y;

			left = lifespan;
			attract = false;
			speed.set( FxRandom.Float( -10, +10 ), FxRandom.Float( -10, +10 ) );
		}

		public void resetAttract( float x, float y) {
			revive();

			left = lifespan;
			attract = true;

			float a = FxRandom.angle(), s = FxRandom.Float( 16, 32 );
			speed.set( (float)Math.cos( a ) * s, (float)Math.sin( a ) * s );
			this.x = x - speed.x * lifespan;
			this.y = y - speed.y * lifespan;
		}

		@Override
		public void update() {
			super.update();
			float k = left / lifespan;
			if (attract) {
				am = 0.35f + 0.65f * (1f - k);
				size( k > 0.66f ? 1 : (k > 0.33f ? 2 : 3) );
				color( Ramp.lerp( 0x88CCFF, 0xFFFFFF, 1f - k ) );
			} else {
				am = k;
				size( Math.min( 4, 1 + (int)((1f - k) * 4f) ) );
				color( Element.FROST.ramp.at( (1f - k) * RAMP_SPAN ) );
			}
		}
	}

	//earth: pebbles and grit of the earth's browns (a fleck of gold a time in ten), shrinking in
	//whole pixels 4, 3, 2, 1
	public static class EarthParticle extends PixelParticle {

		public static final Emitter.Factory FACTORY = new Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((EarthParticle)emitter.recycle( EarthParticle.class )).reset( x, y );
			}
		};

		public static final Emitter.Factory BURST = new Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((EarthParticle)emitter.recycle( EarthParticle.class )).resetBurst( x, y );
			}
		};

		public static final Emitter.Factory ATTRACT = new Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((EarthParticle)emitter.recycle( EarthParticle.class )).resetAttract( x, y );
			}
		};

		public EarthParticle() {
			super();

			lifespan = 0.5f;

			acc.set( 0, +40 );
		}

		public void reset( float x, float y ) {
			revive();

			this.x = x;
			this.y = y;

			left = lifespan;
			size = 4;

			if (FxRandom.Int(10) == 0){
				color( between( 0xFFF266, 0x80771A ) );
			} else {
				color( between( 0x805500, 0x332500 ) );
			}

			speed.set( FxRandom.Float( -10, +10 ), FxRandom.Float( -10, +10 ) );
			acc.set( 0, +40 );
		}

		public void resetBurst( float x, float y ){
			reset(x, y);

			float a = FxRandom.angle(), s = FxRandom.Float( 40, 60 );
			speed.set( (float)Math.cos( a ) * s, (float)Math.sin( a ) * s );
		}

		public void resetAttract( float x, float y ){
			reset(x, y);

			float a = FxRandom.angle(), s = FxRandom.Float( 24, 32 );
			speed.set( (float)Math.cos( a ) * s, (float)Math.sin( a ) * s );

			this.x = x - speed.x * lifespan;
			this.y = y - speed.y * lifespan;

			acc.set( 0, 0 );
		}

		@Override
		public void update() {
			super.update();
			size( stepped( size, left / lifespan ) );
		}
	}

	//a shaman's bolt's trail: 2 px motes from the earth's browns to the shaman's colour
	public static class ShamanParticle extends EarthParticle{

		public static final Emitter.Factory RED = new Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((ShamanParticle)emitter.recycle( ShamanParticle.class ))
						.reset( x, y, between(0xFF4D4D, 0x801A1A) );
			}
		};

		public static final Emitter.Factory BLUE = new Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((ShamanParticle)emitter.recycle( ShamanParticle.class ))
						.reset( x, y, between(0x6699FF, 0x1A3C80) );
			}
		};

		public static final Emitter.Factory PURPLE = new Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((ShamanParticle)emitter.recycle( ShamanParticle.class ))
						.reset( x, y, between(0xBB33FF, 0x5E1A80) );
			}
		};

		public static final Emitter.Factory ELMO = new Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((ShamanParticle)emitter.recycle( ShamanParticle.class ))
						.reset( x, y, between(0x66EE99, 0x22EE66) );
			}
			@Override
			public boolean lightMode() {
				return true;
			}
		};

		/** Its size: the 1 px it was meant to be was lost to the shrink, 2 px stepping to 1 now. */
		public static final int SIZE = 2;

		int startColor;
		int endColor;

		public ShamanParticle() {
			super();

			lifespan = 0.6f;
			acc.set( 0, 0 );
		}

		public void reset( float x, float y, int endColor ){
			super.reset( x, y );

			size = SIZE;
			acc.set( 0, 0 );

			this.endColor = endColor;
			startColor = between(0x805500, 0x332500);

			speed.set( FxRandom.Float( -10, +10 ), FxRandom.Float( -10, +10 ) );
		}

		@Override
		public void update() {
			super.update();
			color( ColorMath.interpolate( endColor, startColor, (left / lifespan) ));
		}
	}

	//a magic missile's trail (and a light missile's, YELLOW): 3, 2, then 1 px squares of light at
	//0.7, down the arcane's (the holy's) ramp; WALL: the wall's runes, two seconds on the grid
	public static class WhiteParticle extends PixelParticle {

		public static final Emitter.Factory FACTORY = new Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((WhiteParticle)emitter.recycle( WhiteParticle.class )).reset( x, y, Element.ARCANE.ramp );
			}
			@Override
			public boolean lightMode() {
				return true;
			}
		};

		public static final Emitter.Factory YELLOW = new Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((WhiteParticle)emitter.recycle( WhiteParticle.class )).reset( x, y, Element.HOLY.ramp );
			}
			@Override
			public boolean lightMode() {
				return true;
			}
		};

		public static final Emitter.Factory WALL = new Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((WhiteParticle)emitter.recycle( WhiteParticle.class )).resetWall( x, y );
			}
			@Override
			public boolean lightMode() {
				return true;
			}
		};

		/** Its alpha, and the share of its ramp it runs down. */
		public static final float ALPHA = 0.7f, RAMP_SPAN = 0.8f;

		private Ramp ramp;

		public WhiteParticle() {
			super();

			lifespan = 0.4f;

			am = ALPHA;
		}

		public void reset( float x, float y ) {
			revive();

			this.x = x;
			this.y = y;

			left = lifespan = 0.4f;
			ramp = null;
			am = ALPHA;
			hardlight(1, 1, 1);
		}

		public void reset( float x, float y, Ramp ramp ) {
			reset( x, y );
			this.ramp = ramp;
		}

		public void reset( float x, float y, float r, float g, float b ) {
			reset(x, y);
			hardlight(r, g, b);
		}

		public void resetWall( float x, float y){
			reset(x, y);

			left = lifespan = 2f;

			this.x = Math.round(x/4)*4;
			this.y = Math.round(y/4)*4 - 6;
			this.x += Math.round(this.y % 16)/4f - 2;
		}

		@Override
		public void update() {
			super.update();
			float k = left / lifespan;
			// size: 3, 2, 1
			size( k > 0.66f ? 3 : (k > 0.33f ? 2 : 1) );
			if (ramp != null) {
				int c = ramp.at( (1f - k) * RAMP_SPAN );
				hardlight( ((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f );
			}
		}
	}

	//a force bolt's trail: pale motes pulled back to it, faint (0.25)
	public static class SlowParticle extends PixelParticle {

		private Emitter emitter;

		public static final Emitter.Factory FACTORY = new Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((SlowParticle)emitter.recycle( SlowParticle.class )).reset( x, y, emitter );
			}
			@Override
			public boolean lightMode() {
				return true;
			}
		};

		/** Its colour and its alpha at most. */
		public static final int COLOR = 0xE0D8C8;
		public static final float ALPHA = 0.25f;

		public SlowParticle() {
			super();

			lifespan = 0.6f;

			color( COLOR );
			size( 2 );
		}

		public void reset( float x, float y, Emitter emitter ) {
			revive();

			this.x = x;
			this.y = y;
			this.emitter = emitter;

			left = lifespan;

			acc.set( 0 );
			speed.set( FxRandom.Float( -20, +20 ), FxRandom.Float( -20, +20 ) );
		}

		@Override
		public void update() {
			super.update();

			am = ALPHA * left / lifespan;
			acc.set( (emitter.x - x) * 10, (emitter.y - y) * 10 );
		}
	}

	//a beacon's: eight motes closing in on it, white, lilac, then violet, 3, 2, 1 px
	public static class ForceParticle extends PixelParticle {

		public static final Emitter.Factory FACTORY = new Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((ForceParticle)emitter.recycle( ForceParticle.class )).reset( index, x, y );
			}

			@Override
			public boolean lightMode() {
				return true;
			}
		};

		/** White, lilac, violet. */
		public static final Ramp RAMP = new Ramp( 0xFFFFFF, 0xE0D0FF, 0xA080F0 );

		public void reset( int index, float x, float y ) {
			super.reset( x, y, 0xFFFFFF, 3, 0.5f );

			float a = (float)(Math.PI * 2 / 8 * index);
			speed.set( (float)Math.cos( a ) * 12, (float)Math.sin( a ) * 12 );
			this.x -= speed.x * lifespan;
			this.y -= speed.y * lifespan;
		}

		@Override
		public void update() {
			super.update();

			float k = left / lifespan;
			am = 0.3f + 0.7f * (1 - k);
			size( stepped( size, k ) );
			color( RAMP.at( 1 - k ) );
		}
	}

	//a ward's: runes of violet light appearing as they shrink, 8, 6, 4, 2 px
	public static class WardParticle extends PixelParticle {

		public static final Emitter.Factory FACTORY = new Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((WardParticle)emitter.recycle( WardParticle.class )).reset( x, y );
			}
			@Override
			public boolean lightMode() {
				return true;
			}
		};

		public static final Emitter.Factory UP = new Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((WardParticle)emitter.recycle( WardParticle.class )).resetUp( x, y );
			}
			@Override
			public boolean lightMode() {
				return true;
			}
		};

		public WardParticle() {
			super();

			lifespan = 0.6f;

			color( 0x8822FF );
		}

		public void reset( float x, float y ) {
			revive();

			this.x = x;
			this.y = y;

			left = lifespan;
			size = 8;
			speed.set( 0 );
		}

		public void resetUp( float x, float y){
			reset(x, y);

			speed.set( FxRandom.Float( -8, +8 ), FxRandom.Float( -32, -48 ) );
		}

		@Override
		public void update() {
			super.update();

			float k = left / lifespan;
			am = 1 - k;
			size( stepped( size, k ) );
		}
	}
}
