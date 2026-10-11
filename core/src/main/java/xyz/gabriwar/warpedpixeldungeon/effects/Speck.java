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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.Arc;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.ColorMath;
import com.watabou.utils.PointF;
import com.watabou.utils.RectF;
import com.watabou.utils.SparseArray;

//a speck: a small painted sign (a cross, a star, a note, a puff). Pixel-true: it never spins
//freely nor scales by fractions - a star turns a quarter at a time, a puff mirrors itself and
//steps once to x2, a light twinkles between its plus and the kit's cross - and slow ones sit on
//whole pixels. The lights (LIGHT, DISCOVER and the coloured ones) are added by default and warm
//from white to their tint; what lands answers (a rock's dust, a bone's bounce); its visual
//randomness is the effects' own (FxRandom), and it makes no object of its own as it lives.
public class Speck extends Image {

	public static final int HEALING     = 0;
	public static final int STAR        = 1;
	public static final int LIGHT       = 2;
	public static final int QUESTION    = 3;
	public static final int UP          = 4;
	public static final int SCREAM      = 5;
	public static final int BONE        = 6;
	public static final int WOOL        = 7;
	public static final int ROCK        = 8;
	public static final int NOTE        = 9;
	public static final int CHANGE      = 10;
	public static final int HEART       = 11;
	public static final int BUBBLE      = 12;
	public static final int STEAM       = 13;
	public static final int COIN        = 14;
	public static final int DOWN        = 15;
	public static final int HUNGER      = 16;

	public static final int DISCOVER    = 101;
	public static final int EVOKE       = 102;
	public static final int MASK        = 103;
	public static final int CROWN       = 104;
	public static final int RATTLE      = 105;
	public static final int JET         = 106;
	public static final int TOXIC       = 107;
	public static final int CORROSION   = 108;
	public static final int PARALYSIS   = 109;
	public static final int DUST        = 110;
	public static final int STENCH      = 111;
	public static final int FORGE       = 112;
	public static final int CONFUSION   = 113;
	public static final int RED_LIGHT   = 114;
	public static final int CALM        = 115;
	public static final int SMOKE       = 116;
	public static final int STORM       = 117;
	public static final int INFERNO     = 118;
	public static final int BLIZZARD    = 119;
	public static final int YELLOW_LIGHT= 120;
	public static final int BLUE_LIGHT  = 121;
	public static final int POPGAS      = 127;
	public static final int SLOW        = 132;
	public static final int MIASMA      = 133;
	public static final int NECTAR      = 134;
	public static final int DEPRESSANT  = 135;
	public static final int FIREWIND    = 136;
	public static final int DIRTCLOUD   = 137;
	public static final int KNOWWIND    = 138;
	public static final int CLOUDOFCORR = 139;
	public static final int THUNDER_STORM = 201;

	private static final int SIZE = 7;

	/** A light's twinkle: its plus and the kit's cross swap every TWINKLE s. */
	public static final float TWINKLE = 0.1f;
	/** A star's quarter turn, s; how much its value varies, +-. */
	public static final float STAR_TURN = 0.08f, STAR_VALUE = 0.1f;
	/** A healing cross's pop (s), its sway (px, Hz), its rise (px/s) and drag, where it warms. */
	public static final float HEAL_POP = 0.08f, HEAL_SWAY = 2f, HEAL_SWAY_HZ = 2f, HEAL_RISE = 20f, HEAL_DRAG = 0.5f, HEAL_WARM = 0.7f;
	/** A puff of gas mirrors itself every FLIP_MIN..FLIP_MAX s and steps to x2 at PUFF_STEP of its life. */
	public static final float FLIP_MIN = 0.4f, FLIP_MAX = 0.7f, PUFF_STEP = 0.5f;
	/** Dust: its colour, its alpha at most, its drag, when it steps to x2. */
	public static final int DUST_COLOR = 0xE0D0A8;
	public static final float DUST_ALPHA = 0.5f, DUST_DRAG = 3f, DUST_STEP = 0.4f;
	/** Under this speed (px/s) a speck sits on whole pixels. */
	public static final float SLOW_SPEED = 12f;

	//the coloured lights: white warming to these
	static final int[] RED_TINT = { 0xFFE0D0, 0xE04030 }, YELLOW_TINT = { 0xFFF0A0, 0xF0C040 },
			BLUE_TINT = { 0xE0F4FF, 0x60B0FF }, WHITE_TINT = { 0xFFFFFF, 0xF0E8FF };

	private int type;
	private float lifespan;
	private float left;

	//its look as it lives: the kit's frame shown (and nudged a pixel to keep its middle), its
	//tint's two ends, the next mirror flip, its value, its quarter turns, a landing, its sway
	private boolean onKit;
	private int[] tint;
	private float flipIn;
	private float value = 1f;
	private int turns;
	private float groundY;
	private boolean bounced, ended;
	private float sway;

	private static TextureFilm film;
	//the kit's frames on its sheet, as uv rects (the sheets themselves looked up in the cache each
	//time: a reloaded cache hands out new ones)
	private static RectF cross, plus5;
	private static final RectF[] coins = new RectF[FxFrames.COIN_6.length];

	private static SparseArray<Emitter.Factory> factories = new SparseArray<>();

	public Speck() {
		super();

		texture( Assets.Effects.SPECKS );
		if (film == null) {
			film = new TextureFilm( texture, SIZE, SIZE );
		}

		origin.set( SIZE / 2f );
	}

	//the kit's frames as uv rects, made once
	private static synchronized void sheets(){
		if (cross != null) return;
		SmartTexture kit = TextureCache.get( Assets.Effects.FX_SHEET );
		cross = uv( kit, FxFrames.CROSS_5 );
		plus5 = uv( kit, FxFrames.PLUS_5 );
		for (int i = 0; i < coins.length; i++) coins[i] = uv( kit, FxFrames.COIN_6[i] );
	}

	private static RectF uv( SmartTexture kit, int[] f ){
		float w = kit.width, h = kit.height;
		return new RectF( f[0] / w, f[1] / h, (f[0] + f[2]) / w, (f[1] + f[3]) / h );
	}

	public Speck image( int type ){
		reset(0, 0, 0, type);
		//its sign as the specks' sheet paints it (a healing cross, a coin)
		if (onKit) onSpecks( film.get( type ) );

		left = lifespan = Float.POSITIVE_INFINITY;
		this.type = -1;

		resetColor();
		scale.set( 1 );
		speed.set( 0 );
		acc.set( 0 );
		angle = 0;
		angularSpeed = 0;

		return this;
	}

	//a frame of the specks' sheet (its 7 px middle), or of the kit's (5 or 6 px, nudged to keep it)
	private void onSpecks( RectF f ){
		if (onKit){
			onKit = false;
			x -= nudge();
			y -= nudge();
		}
		texture = TextureCache.get( Assets.Effects.SPECKS );
		frame( f );
	}

	private void onKit( RectF f ){
		sheets();
		boolean was = onKit;
		float before = was ? nudge() : 0;
		texture = TextureCache.get( Assets.Effects.FX_SHEET );
		frame( f );
		onKit = true;
		float after = nudge();
		x += after - before;
		y += after - before;
	}

	//how far a kit frame sits in from the specks' 7 px square
	private float nudge(){
		return (SIZE - width) / 2f;
	}

	public void reset( int index, float x, float y, int type ) {
		revive();
		sheets();

		this.type = type;
		onKit = false;
		texture = TextureCache.get( Assets.Effects.SPECKS );
		switch (type) {
		case DISCOVER:
		case RED_LIGHT:
		case YELLOW_LIGHT:
		case BLUE_LIGHT:
			frame( film.get( LIGHT ) );
			break;
		case EVOKE:
		case MASK:
		case CROWN:
		case FORGE:
			frame( film.get( STAR ) );
			break;
		case RATTLE:
			frame( film.get( BONE ) );
			break;
		case JET:
		case TOXIC:
		case CORROSION:
		case PARALYSIS:
		case STENCH:
		case CONFUSION:
		case STORM:
		case DUST:
		case SMOKE:
		case BLIZZARD:
		case INFERNO:
		case POPGAS:
		case SLOW:
		case MIASMA:
		case NECTAR:
		case DEPRESSANT:
		case FIREWIND:
		case DIRTCLOUD:
		case KNOWWIND:
		case CLOUDOFCORR:
			frame( film.get( STEAM ) );
			break;
		case CALM:
			frame( film.get( SCREAM ) );
			break;
		default:
			frame( film.get( type ) );
		}

		this.x = x - origin.x;
		this.y = y - origin.y;

		resetColor();
		scale.set( 1 );
		speed.set( 0 );
		acc.set( 0 );
		angle = 0;
		angularSpeed = 0;
		flipHorizontal = flipVertical = false;
		updateFrame();
		tint = null;
		value = 1f;
		turns = 0;
		flipIn = FxRandom.Float( FLIP_MIN, FLIP_MAX );
		groundY = Float.NaN;
		bounced = ended = false;
		sway = 0;

		switch (type) {

		case HEALING:
			speed.set( 0, -HEAL_RISE );
			lifespan = 1f;
			//it pops in as the kit's small plus, lit red
			onKit( plus5 );
			hardlight( 0xFF2020 );
			break;

		case HUNGER:
			speed.set( 0, -20 );
			lifespan = 1f;
			break;

		case STAR:
			polar( FxRandom.Float( 2 * 3.1415926f ), FxRandom.Float( 128 ) );
			acc.set( 0, 128 );
			turns = FxRandom.Int( 4 );
			value = 1f + FxRandom.Float( -STAR_VALUE, STAR_VALUE );
			lifespan = 1f;
			break;

		case FORGE:
			polar( FxRandom.Float( -3.1415926f ), FxRandom.Float( 64 ) );
			acc.set( 0, 128 );
			turns = FxRandom.Int( 4 );
			value = 1f + FxRandom.Float( -STAR_VALUE, STAR_VALUE );
			lifespan = 0.51f;
			break;

		case EVOKE:
			polar( FxRandom.Float( -3.1415926f ), 50 );
			acc.set( 0, 50 );
			turns = FxRandom.Int( 4 );
			lifespan = 1f;
			break;

		case MASK:
			polar( index * 3.1415926f / 5, 50 );
			acc.set( -speed.x, -speed.y );
			turns = index;
			lifespan = 1f;
			break;

		case CROWN:
			acc.set( index % 2 == 0 ? FxRandom.Float( -512, -256 ) : FxRandom.Float( +256, +512 ), 0 );
			lifespan = 0.5f;
			break;

		case RED_LIGHT:
			tint = RED_TINT;
			lifespan = 1f;
			speed.set( 0, -FxRandom.Float( 3f, 6f ) );
			break;

		case LIGHT:
			tint = WHITE_TINT;
			lifespan = 1f;
			speed.set( 0, -FxRandom.Float( 3f, 6f ) );
			break;

		case YELLOW_LIGHT:
			tint = YELLOW_TINT;
			lifespan = 1f;
			speed.set( 0, -FxRandom.Float( 3f, 6f ) );
			break;

		case BLUE_LIGHT:
			tint = BLUE_TINT;
			lifespan = 1f;
			speed.set( 0, -FxRandom.Float( 3f, 6f ) );
			break;

		case DISCOVER:
			tint = BLUE_TINT;
			lifespan = 0.5f;
			am = 0;
			break;

		case QUESTION:
			lifespan = 0.8f;
			break;

		case UP:
			speed.set( 0, -20 );
			lifespan = 1f;
			break;
		case DOWN:
			speed.set( 0, 20 );
			lifespan = 1f;
			break;

		case CALM:
			color(0, 1, 1);
		case SCREAM:
			lifespan = 0.9f;
			break;

		case BONE:
			lifespan = 0.35f;
			polar( FxRandom.Float( 2 * 3.1415926f ), 24 / 0.2f );
			acc.set( 0, 128 );
			turns = FxRandom.Int( 4 );
			//it lands a little under where it flew from, and bounces once
			groundY = this.y + FxRandom.Float( 4f, 8f );
			break;

		case RATTLE:
			lifespan = 0.5f;
			speed.set( 0, -100 );
			acc.set( 0, -2 * speed.y / lifespan );
			turns = FxRandom.Int( 4 );
			break;

		case WOOL:
			lifespan = 0.5f;
			speed.set( 0, -50 );
			turns = FxRandom.Int( 4 );
			break;

		case ROCK:
			turns = FxRandom.Int( 4 );
			scale.set( FxRandom.chance( 0.5f ) ? 1 : 2 );
			speed.set( 0, 64 );
			lifespan = 0.2f;
			this.y -= speed.y * lifespan;
			break;

		case NOTE: {
			float tilt = FxRandom.Float( -30, +30 );
			polar( (tilt - 90) * PointF.G2R, 30 );
			lifespan = 1f;
			break;
		}

		case CHANGE: {
			float a = FxRandom.Float( 360 );
			polar( (a - 90) * PointF.G2R, FxRandom.Float( 4, 12 ) );
			lifespan = 1.5f;
			break;
		}

		case HEART:
			speed.set( FxRandom.IntRange( -10, +10 ), -40 );
			lifespan = 1f;
			break;

		case BUBBLE:
			speed.set( 0, -15 );
			lifespan = FxRandom.Float( 0.8f, 1.5f );
			break;

		case THUNDER_STORM:
		case STEAM:
			speed.y = -FxRandom.Float( 10, 15 );
			lifespan = 1f;
			break;

		case JET:
			speed.y = +32;
			acc.y = -64;
			lifespan = 0.5f;
			break;

		case TOXIC:
			hardlight( 0x50FF60 );
			lifespan = FxRandom.Float( 1f, 3f );
			break;

		case CORROSION:
			hardlight( 0xAAAAAA );
			lifespan = FxRandom.Float( 1f, 3f );
			break;

		case PARALYSIS:
			hardlight( 0xFFFF66 );
			lifespan = FxRandom.Float( 1f, 3f );
			break;

		case STENCH:
			hardlight( 0x003300 );
			lifespan = FxRandom.Float( 1f, 3f );
			break;

		case CONFUSION:
			//a hue of the prism, never a random colour
			hardlight( Element.PRISM.ramp.stop( FxRandom.Int( Element.PRISM.ramp.size() ) ) );
			lifespan = FxRandom.Float( 1f, 3f );
			break;

		case STORM:
			hardlight( 0x8AD8D8 );
			lifespan = FxRandom.Float( 1f, 3f );
			break;

		case INFERNO:
			hardlight( 0xEE7722 );
			lifespan = FxRandom.Float( 1f, 3f );
			break;

		case BLIZZARD:
			hardlight( 0xFFFFFF );
			lifespan = FxRandom.Float( 1f, 3f );
			break;

		case POPGAS:
			hardlight( 0xAC2121 );
			lifespan = FxRandom.Float( 1f, 3f );
			break;

		case SLOW:
			hardlight( 0x89DFCB );
			lifespan = FxRandom.Float( 1f, 1.5f );
			break;

		case MIASMA:
			hardlight( 0xEE29DC );
			lifespan = FxRandom.Float( 1f, 3f );
			break;

		case NECTAR:
			hardlight( 0x9DD360 );
			lifespan = FxRandom.Float( 0.5f, 1.5f );
			break;

		case DEPRESSANT:
			hardlight( 0x806FCC );
			lifespan = FxRandom.Float( 3f, 5f );
			break;

		case FIREWIND:
			hardlight( 0xEE6E29 );
			lifespan = FxRandom.Float( 1f, 3f );
			break;

		case DIRTCLOUD:
			hardlight( 0x331A0A );
			lifespan = FxRandom.Float( 3f, 5f );
			break;

		case KNOWWIND:
			hardlight( 0x93DDAC );
			lifespan = FxRandom.Float( 1f, 4f );
			break;

		case CLOUDOFCORR:
			hardlight( 0x1D1717 );
			lifespan = FxRandom.Float( 1f, 2f );
			break;

		case SMOKE:
			hardlight( 0x000000 );
			lifespan = FxRandom.Float( 1f, 1.5f );
			break;

		case DUST:
			hardlight( DUST_COLOR );
			polar( FxRandom.Float( 2 * 3.1415926f ), FxRandom.Float( 16, 48 ) );
			lifespan = 0.5f;
			break;

		case COIN:
			polar( -PointF.PI * FxRandom.Float( 0.3f, 0.7f ), FxRandom.Float( 48, 96 ) );
			acc.y = 256;
			lifespan = -speed.y / acc.y * 2;
			//a painted coin turning (the kit's four frames), lit gold
			onKit( coins[0] );
			hardlight( 0xFFD24A );
			break;
		}

		left = lifespan;
		if (turns != 0) angle = 90 * turns;
	}

	private void polar( float a, float s ){
		speed.set( (float)Math.cos( a ) * s, (float)Math.sin( a ) * s );
	}

	//a share k of the way from one colour to another, as its light
	private void light( int a, int b, float k ){
		int c = Ramp.lerp( a, b, k );
		hardlight( ((c >> 16) & 0xFF) / 255f * value, ((c >> 8) & 0xFF) / 255f * value, (c & 0xFF) / 255f * value );
	}

	@Override
	public void update() {
		super.update();

		left -= Game.elapsed;
		if (left <= 0) {

			end();
			kill();

		} else {

			float p = 1 - left / lifespan;	// 0 -> 1
			float age = lifespan - left;

			switch (type) {

			case STAR:
			case FORGE:
				//a quarter turn every STAR_TURN, its value its own, fading over its last 35%
				angle = 90 * (turns + (int)(age / STAR_TURN));
				am = p < 0.2f ? p * 5f : (p > 0.65f ? (1 - p) / 0.35f : 1f);
				rm = gm = bm = value;
				break;

			case MASK:
			case CROWN:
				angle = 90 * (turns + (int)(age / 0.12f));
				am = 1 - p * p;
				break;

			case EVOKE:
				angle = 90 * (turns + (int)(age / STAR_TURN));
				am = p < 0.5f ? 1 : 2 - p * 2;
				break;

			case HEALING:
				//the plus gives way to the cross; it sways as it rises, slowing, warming at the end
				if (onKit && age >= HEAL_POP) {
					onSpecks( film.get( HEALING ) );
					resetColor();
				}
				speed.y *= Math.max( 0f, 1f - HEAL_DRAG * Game.elapsed );
				float s = HEAL_SWAY * (float)Math.sin( 2 * Math.PI * HEAL_SWAY_HZ * age );
				x += s - sway;
				sway = s;
				if (!onKit && p > HEAL_WARM) tint( 0xFFE8A0, (p - HEAL_WARM) / (1 - HEAL_WARM) );
				am = p < 0.5f ? 1 : 2 - p * 2;
				break;

			case HUNGER:
				am = p < 0.5f ? 1 : 2 - p * 2;
				break;

			case RED_LIGHT:
			case YELLOW_LIGHT:
			case BLUE_LIGHT:
			case LIGHT:
				twinkle( age );
				am = p < 0.2f ? p * 5f : (1 - p) * 1.25f;
				light( tint[0], tint[1], p );
				break;

			case DISCOVER:
				twinkle( age );
				am = 1 - p;
				light( tint[0], tint[1], p );
				break;

			case QUESTION:
				//up a step and back: 1, 2, 1
				scale.set( p > 0.2f && p < 0.8f ? 2 : 1 );
				break;

			case UP:
			case DOWN:
				am = p < 0.2f ? p * 5f : (p > 0.8f ? (1 - p) * 5f : 1f);
				break;

			case CALM:
			case SCREAM:
				am = (float)Math.sqrt( (p < 0.5f ? p : 1 - p) * 2f );
				scale.set( p < 0.33f ? 1 : (p < 0.66f ? 2 : 3) );
				break;

			case BONE:
				angle = 90 * (turns + (int)(age / 0.06f));
				if (!bounced && !Float.isNaN( groundY ) && y >= groundY && speed.y > 0){
					bounced = true;
					speed.y = -speed.y * 0.35f;
					speed.x *= 0.6f;
				}
				am = p < 0.8f ? 1 : (1 - p) * 5;
				break;

			case RATTLE:
				angle = 90 * (turns + (int)(age / 0.06f));
				am = p < 0.9f ? 1 : (1 - p) * 10;
				break;

			case ROCK:
				angle = 90 * (turns + (int)(age / 0.06f));
				am = p < 0.2f ? p * 5 : 1 ;
				break;

			case NOTE:
				am = 1 - p * p;
				break;

			case WOOL:
				angle = 90 * (turns + (int)(age / 0.1f));
				am = 1 - p;
				break;

			case CHANGE:
				am = (float)Math.sqrt( (p < 0.5f ? p : 1 - p) * 2);
				mirror( ((int)(age * 7.5f) & 1) == 1 );
				break;

			case HEART:
				am = 1 - p * p;
				break;

			case BUBBLE:
				am = p < 0.2f ? p * 5 : 1;
				break;

			case DUST:
				speed.scale( Math.max( 0f, 1f - DUST_DRAG * Game.elapsed ) );
				am = DUST_ALPHA * (float)Math.sqrt( (p < 0.5f ? p : 1 - p) * 2f );
				scale.set( p < DUST_STEP ? 1 : 2 );
				break;

			case STEAM:
			case THUNDER_STORM:
			case TOXIC:
			case PARALYSIS:
			case CONFUSION:
			case STORM:
			case BLIZZARD:
			case INFERNO:
			case POPGAS:
			case SLOW:
			case MIASMA:
			case DEPRESSANT:
			case FIREWIND:
			case DIRTCLOUD:
			case KNOWWIND:
				am = (float)Math.sqrt( (p < 0.5f ? p : 1 - p) * 0.5f );
				puff( p );
				break;

			case CORROSION:
				hardlight( ColorMath.interpolate( 0xAAAAAA, 0xFF8800 , p ));
			case STENCH:
			case SMOKE:
			case NECTAR:
			case CLOUDOFCORR:
				am = (float)Math.sqrt( (p < 0.5f ? p : 1 - p) );
				puff( p );
				break;

			case JET:
				am = (p < 0.5f ? p : 1 - p) * 2;
				puff( p );
				break;

			case COIN:
				//its four painted frames turning, a glint as it tops its arc
				onKit( coins[(int)(age * 12f) % coins.length] );
				am = p < 0.9f ? 1 : (1 - p) * 10;
				if (!ended && speed.y >= 0 && p > 0.3f){
					ended = true;
					if (world()) Arc.glint( x + width / 2f, y + height / 2f, 0xFFF3C8 );
				}
				break;
			}
		}
	}

	//a light's twinkle: its plus and the kit's cross, swapping every TWINKLE
	private void twinkle( float age ){
		boolean crossNow = ((int)(age / TWINKLE) & 1) == 1;
		if (crossNow && !onKit) onKit( cross );
		else if (!crossNow && onKit) onSpecks( film.get( LIGHT ) );
	}

	//a puff: mirrored every FLIP_MIN..FLIP_MAX s, one step to x2 at PUFF_STEP of its life
	private void puff( float p ){
		if ((flipIn -= Game.elapsed) <= 0){
			flipIn = FxRandom.Float( FLIP_MIN, FLIP_MAX );
			mirror( !flipHorizontal );
		}
		scale.set( p < PUFF_STEP ? 1 : 2 );
	}

	private void mirror( boolean flip ){
		if (flipHorizontal != flip){
			flipHorizontal = flip;
			updateFrame();
		}
	}

	//what it leaves as it ends: a rock's dust where it lands, an arrow's glint
	private void end(){
		if (!world()) return;
		float cx = x + width / 2f, cy = y + height / 2f;
		switch (type){
			case ROCK: {
				FxParticle d = Arc.matter( cx, cy + 2, FxBudget.P3 );
				if (d != null) d.look( FxFrames.PUFF_S[FxRandom.Int( FxFrames.PUFF_S.length )] ).paint( DUST_COLOR )
						.vel( FxRandom.Float( -4, 4 ), -6f ).life( 0.4f ).alpha( 0.5f, 0, 0.3f ).smooth();
				break;
			}
			case UP:
				Arc.glint( cx, y, 0xFFFFFF );
				break;
		}
	}

	//in the game's world (not a window's speck)
	private boolean world(){
		return parent != null && Arc.onScene() && camera() == Camera.main;
	}

	@Override
	public void draw() {
		//a slow speck on whole pixels
		if (speed.x * speed.x + speed.y * speed.y < SLOW_SPEED * SLOW_SPEED) {
			float ox = x, oy = y;
			x = Math.round( x );
			y = Math.round( y );
			super.draw();
			x = ox;
			y = oy;
		} else {
			super.draw();
		}
	}

	/** Whether a type is light by default (added): the lights and the discover's twinkle. */
	public static boolean lightByDefault( int type ){
		return type == LIGHT || type == DISCOVER || type == RED_LIGHT || type == YELLOW_LIGHT || type == BLUE_LIGHT;
	}

	public static Emitter.Factory factory( final int type ) {
		return factory( type, lightByDefault( type ) );
	}

	//one factory a type and blend: asked for as light after it was made plain (or the other way
	//round), a type kept handing out the one made first
	private static int key( int type, boolean lightMode ){
		return type * 2 + (lightMode ? 1 : 0);
	}

	public static Emitter.Factory factory( final int type, final boolean lightMode ) {

		Emitter.Factory factory = factories.get( key( type, lightMode ) );

		if (factory == null) {
			factory = new Emitter.Factory() {
				@Override
				public void emit ( Emitter emitter, int index, float x, float y ) {
					Speck p = (Speck)emitter.recycle( Speck.class );
					p.reset( index, x, y, type );
					//a discovered cell: a pale ring across it, once a burst
					if (type == DISCOVER && index == 0 && p.world()) {
						int cell = Fx.cellAt( x, y );
						if (cell >= 0) {
							int w = Dungeon.level.width();
							Arc.ring( (cell % w + 0.5f) * DungeonTilemap.SIZE, (cell / w + 0.5f) * DungeonTilemap.SIZE + 2,
									9, 17, 0.4f, 0xC8E0FF, 0.3f, true );
						}
					}
				}
				@Override
				public boolean lightMode() {
					return lightMode;
				}
			};
			factories.put( key( type, lightMode ), factory );
		}

		return factory;
	}

	//reverse lookup for network recording: returns the speck type backing a cached
	//factory instance, or -1 if the factory isn't a Speck factory.
	public static int typeForFactory( Emitter.Factory factory ) {
		for (int key : factories.keyArray()) {
			if (factories.get( key ) == factory) {
				return Math.floorDiv( key, 2 );
			}
		}
		return -1;
	}
}
