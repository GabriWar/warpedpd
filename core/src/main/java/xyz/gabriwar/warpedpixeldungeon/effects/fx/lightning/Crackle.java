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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning;

import xyz.gabriwar.warpedpixeldungeon.effects.Effects;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherOverlay;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;

/**
 * Where a lightning strikes, the charge still crawling on what it struck: two or three micro twigs
 * (3-5 px zigzags) within REACH px of the point, thrown anew every EVERY s for LIFE s, white and
 * dimming, drawn as light on the weather's layer with the lightning. One asked for within REACH
 * px of a live one on the same layer (a struck sprite's and its lightning's) is that one renewed.
 * At most MOST at once; a strike past them goes without (the shape still reads). Render thread.
 *
 * Also the proc of a shocking weapon (weapon()): a faint static crackle on the attacker's weapon
 * side for WEAPON_FOR s after the strike, never before it.
 */
public class Crackle extends Group {

	public static final int TWIGS_MIN = 2, TWIGS_MAX = 3, MOST = 8;
	public static final float REACH = 6f, LEN_MIN = 3f, LEN_MAX = 5f, EVERY = 0.05f, LIFE = 0.15f;
	/** The micro twigs' width (the core's share) and colour. */
	static final float WIDTH = 0.4f;
	static final int COLOR = 0xE8F0FF;
	/** A weapon's static: how far off the attacker's middle (px, on his facing side), from when
	 *  after the strike, how long, and how many specks. */
	public static final float WEAPON_SIDE = 5f, WEAPON_AFTER = 0.04f, WEAPON_FOR = 0.3f;
	public static final int WEAPON_SPECKS = 5;

	private static int alive;
	//the ones crackling now, for a strike on the same place
	private static final Crackle[] live = new Crackle[MOST];

	private final Image[] lines = new Image[2 * TWIGS_MAX];
	private float cx, cy, level, left, next;
	private int twigs;
	private boolean counted;

	public Crackle(){
		for (int i = 0; i < lines.length; i++){
			lines[i] = Effects.get( Effects.Type.LIGHTNING );
			lines[i].origin.set( 0, lines[i].height / 2 );
			lines[i].scale.y = WIDTH;
			lines[i].hardlight( COLOR );
			add( lines[i] );
		}
	}

	/** How many are crackling now. */
	public static synchronized int alive(){
		return alive;
	}

	/** A new scene: none of the last one's counted. */
	public static synchronized void reset(){
		alive = 0;
		for (int i = 0; i < MOST; i++) live[i] = null;
	}

	private static synchronized boolean claim(){
		if (alive >= MOST) return false;
		alive++;
		return true;
	}

	//one claimed, crackling now
	private static synchronized void enlist( Crackle c ){
		for (int i = 0; i < MOST; i++){
			if (live[i] == null){
				live[i] = c;
				return;
			}
		}
	}

	private static synchronized void release( Crackle c ){
		if (alive > 0) alive--;
		for (int i = 0; i < MOST; i++){
			if (live[i] == c) live[i] = null;
		}
	}

	//a live one on `into` within REACH px of (x, y), null for none
	private static synchronized Crackle near( Group into, float x, float y ){
		for (int i = 0; i < MOST; i++){
			Crackle c = live[i];
			if (c != null && c.parent == into && Math.abs( c.cx - x ) <= REACH && Math.abs( c.cy - y ) <= REACH) return c;
		}
		return null;
	}

	/** A crackle at (x, y) at `level` (0..1) in a group (the weather's layer); null when MOST
	 *  crackle already or there is no group. Render thread. */
	public static Crackle at( Group into, float x, float y, float level ){
		if (into == null) return null;
		Crackle c = near( into, x, y );
		if (c != null){
			c.left = LIFE;
			c.level = Math.max( c.level, level );
			return c;
		}
		if (!claim()) return null;
		c = (Crackle) into.recycle( Crackle.class );
		c.counted = true;
		enlist( c );
		c.setup( x, y, level );
		return c;
	}

	/** The strike's crackle on a struck sprite's middle, on the weather's layer, where the hero sees
	 *  it. Any thread (handed to the render thread). */
	public static void on( final CharSprite sprite ){
		if (sprite == null) return;
		Fx.post( () -> {
			if (sprite.parent == null || !sprite.visible) return;
			float x = sprite.x + sprite.width() / 2f, y = sprite.y + sprite.height() / 2f;
			if (!Fx.seen( x, y )) return;
			WeatherOverlay sky = GameScene.getWeatherOverlay();
			at( sky, x, y, 0.85f );
		} );
	}

	/** A faint static crackle on the attacker's weapon side for WEAPON_FOR s, starting just after
	 *  the strike (no anticipation on a proc). Any thread (handed to the render thread); draws no
	 *  game randomness. */
	public static void weapon( final CharSprite attacker ){
		if (attacker == null) return;
		Fx.post( () -> {
			if (attacker.parent == null || !attacker.visible) return;
			Emitter e = GameScene.lightEmitter();
			if (e == null) return;
			float side = attacker.flipHorizontal ? -WEAPON_SIDE : WEAPON_SIDE;
			float x = attacker.x + attacker.width() / 2f + side, y = attacker.y + attacker.height() / 2f + 1f;
			e.pos( x - 1.5f, y - 1.5f, 3f, 3f );
			e.startDelayed( SparkParticle.STATIC, WEAPON_FOR / WEAPON_SPECKS, WEAPON_SPECKS, WEAPON_AFTER );
		} );
	}

	private void setup( float x, float y, float level ){
		cx = x;
		cy = y;
		this.level = level;
		left = LIFE;
		next = 0;
		revive();
		toss();
	}

	@Override
	public void update(){
		super.update();
		if (Emitter.freezeEmitters && Game.timeTotal > 1) return;
		float dt = Game.elapsed;
		left -= dt;
		if (left <= 0){
			kill();
			return;
		}
		if ((next -= dt) <= 0) toss();
		float a = level * (0.55f + 0.45f * left / LIFE);
		for (int i = 0; i < lines.length; i++){
			Lines.shade( lines[i], i < 2 * twigs ? a : 0f );
		}
	}

	//its twigs thrown anew: each from a point within REACH of the strike, a bent zigzag of
	//LEN_MIN-LEN_MAX px pointing away from it
	private void toss(){
		next = EVERY;
		twigs = FxRandom.IntRange( TWIGS_MIN, TWIGS_MAX );
		for (int i = 0; i < twigs; i++){
			float a = FxRandom.angle();
			float r = FxRandom.Float( 1f, REACH - 2f );
			float x0 = cx + (float)Math.cos( a ) * r, y0 = cy + (float)Math.sin( a ) * r * 0.75f;
			float len = FxRandom.Float( LEN_MIN, LEN_MAX );
			float out = a + FxRandom.Float( -0.6f, 0.6f );
			float x1 = x0 + (float)Math.cos( out ) * len * 0.55f, y1 = y0 + (float)Math.sin( out ) * len * 0.55f;
			out += FxRandom.sign() * FxRandom.Float( 0.7f, 1.2f );
			float x2 = x1 + (float)Math.cos( out ) * len * 0.45f, y2 = y1 + (float)Math.sin( out ) * len * 0.45f;
			Lines.place( lines[2 * i], x0, y0, x1, y1 );
			Lines.place( lines[2 * i + 1], x1, y1, x2, y2 );
		}
	}

	@Override
	public void kill(){
		super.kill();
		if (counted){
			counted = false;
			release( this );
		}
	}

	@Override
	public void destroy(){
		if (counted){
			counted = false;
			release( this );
		}
		super.destroy();
	}

	@Override
	public void draw(){
		Blending.setLightMode();
		super.draw();
		Blending.setNormalMode();
	}
}
