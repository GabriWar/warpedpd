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

package xyz.gabriwar.warpedpixeldungeon.effects.skillfx;

import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.utils.Callback;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

/**
 * What SkillFX.rain and SkillFX.streak fly when a skill's falling or thrown thing is not an item
 * (SkillFX.Fall, SkillFX.Streak): a picture of the kit's frames moving along the line a
 * MissileSprite would, in the time it would take, calling back on the frame its tweener would
 * (Tweener's count, its early finish within half a frame and all). Its matter rides in the group it
 * was added to (the hero's layer, where the MissileSprite flew), its light in the light layer.
 * 1-3 quads and a glow; a meteor's trail 2 particles every TRAIL s (P2). What is only light (a
 * spirit arrow, a holy blade: never a real item) also leaves a stepped core behind it, a 2 px then
 * 1 px mote laid every CORE_EVERY s where it was at that moment, living CORE_LIFE s (P2, at most
 * CORE_MOST alive, beginning CLEAR px out), pale for its first quarter then its colour (nature leaf
 * green, holy gold), and lands with a STAR4_5 glint for LANDING s (P1). Only CORE_RICH such lights
 * in flight at once carry a core and the glint (counted per scene): a volley's others fly as they did, so a rain
 * of arrows never turns into a rain of streaks.
 */
public class SkillFlight extends Group {

	/** Kinds: what falls (SkillFX.Fall's) and what is thrown (SkillFX.Streak's), one numbering. */
	public static final int METEOR = 0, SPIRIT_ARROW = 1, HOLY_BLADE = 2, WEIGHT = 3, SHOCK = 4,
			COIN = 5, FEATHER_DART = 6, PELLET = 7, SNARE = 8;
	/** A meteor sheds its trail this often, s; turns a quarter this often; a snare's crescents
	 *  turn a quarter this often. */
	public static final float TRAIL = 0.02f, METEOR_TURN = 0.1f, SNARE_TURN = 0.04f;
	public static final int SPIRIT = SkillPalette.NATURE_LIGHT, HOLY = 0xF1D99C, STEEL = 0xB9DEEE, ROPE = 0x3A3A40,
			METEOR_STONE = 0x6E5A4E, METEOR_GLOW = 0xFF9030, PELLET_INK = 0x3A2A55;
	/** A light's core: a mote laid this often (s), living this long (s), at most this many alive; the
	 *  glint where it lands (s). */
	public static final float CORE_EVERY = 0.015f, CORE_LIFE = 0.2f, LANDING = 0.12f;
	public static final int CORE_MOST = 14, CORE_RICH = 4;
	/** How far out (px along its way) a light's core begins: never a wash over the one who loosed it. */
	public static final float CLEAR = 6f;
	/** The cores' ramps, pale for their first quarter then their colour: nature, holy. */
	static final Ramp SPIRIT_CORE = SkillRay.flashed( SkillPalette.NATURE.stop( 0 ), SkillPalette.NATURE_LIGHT ),
			HOLY_CORE = SkillRay.flashed( SkillPalette.HOLY.stop( 0 ), HOLY );
	static final int[][] CORE_STEPS = { FxFrames.MOTE_2, FxFrames.MOTE_1 };

	private final int kind;
	private final float fromX, fromY, toX, toY, angle;
	private final float interval;
	private final Callback callback;
	private float elapsed, trailLeft, turnLeft;
	private int turns;
	private final int depth = Dungeon.depth, branch = Dungeon.branch;
	//the pictures: the head (matter or light), a second (a shaft, a rope, a crescent), its ghosts
	private FxPic head, second, ghost1, ghost2;
	private Group lights;
	private FxLight glow;
	private float p1x, p1y, p2x, p2y;
	private boolean begun;
	//a light's core: its next mote in (s), the flight's clock each alive mote dies at
	private float coreLeft;
	private final float[] coreUntil = new float[CORE_MOST];
	private int coreAlive;
	private boolean cored;
	//the lights in flight carrying a core, in the scene whose light layer they were counted in
	private static int coredLive;
	private static Object coredIn;

	private static synchronized boolean enterCore(){
		Object layer = GameScene.layer( GameScene.Layer.LIGHTS );
		if (layer != coredIn){
			coredIn = layer;
			coredLive = 0;
		}
		if (coredLive >= CORE_RICH) return false;
		coredLive++;
		return true;
	}

	private static synchronized void leaveCore(){
		coredLive = Math.max( 0, coredLive - 1 );
	}

	/** Lights in flight carrying a core now (this scene's). */
	public static synchronized int cored(){
		return coredIn == GameScene.layer( GameScene.Layer.LIGHTS ) ? coredLive : 0;
	}

	/**
	 * A flight of a kind from (fx, fy) to (tx, ty) (middles), over the distance a MissileSprite
	 * would cross at `speed` px/s: its tweener's interval (its sprite's 16 px taken off both ends'
	 * corners, as its setup does). Added to `parent` now, on the caller's thread, as the sprite's
	 * tweener is; null with no parent.
	 */
	public static SkillFlight fly( Group parent, float fx, float fy, float tx, float ty, int kind, float speed, Callback cb ){
		if (parent == null) return null;
		SkillFlight f = new SkillFlight( kind, fx, fy, tx, ty, speed, cb );
		parent.add( f );
		return f;
	}

	private SkillFlight( int kind, float fx, float fy, float tx, float ty, float speed, Callback cb ){
		this.kind = kind;
		fromX = fx;
		fromY = fy;
		toX = tx;
		toY = ty;
		callback = cb;
		//MissileSprite.setup: both corners moved by half its 16 px, then their difference
		float ax = fx - 8f, ay = fy - 8f, bx = tx - 8f, by = ty - 8f;
		float dx = bx - ax, dy = by - ay;
		interval = (float) Math.sqrt( dx * dx + dy * dy ) / speed;
		angle = (float) Math.toDegrees( Math.atan2( ty - fy, tx - fx ) );
		p1x = p2x = fx;
		p1y = p2y = fy;
		build();
	}

	/** Its flight's length, s (the tweener's interval). */
	public float interval(){
		return interval;
	}

	//the pictures its kind is drawn with (any thread: no GL)
	private void build(){
		switch (kind){
			case METEOR:
				head = matter( FxFrames.METEOR_7, METEOR_STONE );
				break;
			case WEIGHT:
				second = new FxPic( SkillVfx.SHEET );
				second.show( FxFrames.MOTE_1 );
				second.lit( ROPE );
				second.origin.set( 0.5f, 0 );
				add( second );
				head = matter( FxFrames.WEIGHT_9, 0x5A5F66 );
				break;
			case COIN:
				head = matter( FxFrames.COIN_6[0], Element.GOLD.ramp.stop( 2 ) );
				break;
			case PELLET:
				head = matter( FxFrames.INK_CORE, PELLET_INK );
				break;
			case FEATHER_DART:
				head = matter( FxFrames.FEATHER, 0xF3EBD7 );
				break;
			case SPIRIT_ARROW:
				ghost2 = light( FxFrames.ARROW_9, SPIRIT, false );
				ghost1 = light( FxFrames.ARROW_9, SPIRIT, false );
				head = light( FxFrames.ARROW_9, Ramp.lerp( SPIRIT, 0xFFFFFF, 0.5f ), false );
				break;
			case HOLY_BLADE:
				second = light( FxFrames.Light.CORE_BAND_1, HOLY, true );
				second.origin.set( 0, 0.5f );
				second.scale.set( 10f / 16f, 1 );
				head = light( FxFrames.LANCE_HEAD_9, Ramp.lerp( HOLY, 0xFFFFFF, 0.4f ), false );
				break;
			case SNARE:
				second = light( FxFrames.SLASH_16[4], STEEL, false );
				head = light( FxFrames.SLASH_16[0], STEEL, false );
				break;
			default:
				//a shock: nothing drawn, only its time and its strike
				break;
		}
		if (head != null && kind != SNARE && kind != WEIGHT){
			//line frames turn about their socket at their back, the rest about their middle
			boolean line = kind == SPIRIT_ARROW || kind == HOLY_BLADE;
			if (line) head.origin.set( 0, head.height / 2f );
			if (ghost1 != null){
				ghost1.origin.set( head.origin.x, head.origin.y );
				ghost2.origin.set( head.origin.x, head.origin.y );
			}
		}
	}

	private FxPic matter( int[] frame, int color ){
		FxPic p = new FxPic( SkillVfx.SHEET );
		p.show( frame );
		p.lit( color );
		p.origin.set( frame[2] / 2f, frame[3] / 2f );
		p.visible = false;
		add( p );
		return p;
	}

	private FxPic light( int[] frame, int color, boolean lightSheet ){
		FxPic p = new FxPic( lightSheet ? SkillVfx.LIGHT_SHEET : SkillVfx.SHEET );
		p.additive = true;
		p.show( frame );
		p.lit( color );
		p.origin.set( frame[2] / 2f, frame[3] / 2f );
		p.visible = false;
		if (lights == null) lights = new Group();
		lights.add( p );
		return p;
	}

	@Override
	public void update(){
		super.update();
		if (!begun){
			begun = true;
			if (lights != null){
				if (GameScene.layer( GameScene.Layer.LIGHTS ) != null) GameScene.light( lights );
				else lights = null;
			}
			int glowColor = kind == METEOR ? METEOR_GLOW : kind == SPIRIT_ARROW ? SPIRIT : kind == HOLY_BLADE ? HOLY
					: kind == SNARE ? STEEL : -1;
			if (kind == SPIRIT_ARROW || kind == HOLY_BLADE) cored = enterCore();
			if (glowColor != -1 && head != null){
				glow = FxLight.glow( fromX, fromY, kind == METEOR ? 15 : 7, glowColor, kind == METEOR ? 1f : 0.7f ).follow( head, 0, 0 );
			}
		}
		//the tweener's count, as Tweener.update keeps it
		elapsed += Game.elapsed;
		if ((interval - elapsed) < Game.elapsed / 2f) elapsed = interval;
		boolean done = elapsed >= interval;
		float p = done ? 1f : elapsed / interval;
		boolean here = depth == Dungeon.depth && branch == Dungeon.branch;
		if (here) lay( p );
		if (done){
			if (here && cored) landed();
			release();
			Fx.dispose( this );
			if (callback != null) callback.call();
		}
	}

	//its pictures at a share p of the way
	private void lay( float p ){
		float x = fromX + (toX - fromX) * p, y = fromY + (toY - fromY) * p;
		if (kind == COIN) y -= 6f * 4f * p * (1f - p);
		if (kind == PELLET) y -= 8f * 4f * p * (1f - p);
		boolean seen = Fx.seen( x, y ) || Fx.seen( toX, toY );
		float dt = Game.elapsed;
		switch (kind){
			case METEOR:
				if ((turnLeft -= dt) <= 0){
					turnLeft += METEOR_TURN;
					head.angle = (head.angle + 90) % 360;
				}
				if ((trailLeft -= dt) <= 0){
					trailLeft += TRAIL;
					if (seen) meteorTrail( x, y );
				}
				break;
			case COIN:
				head.show( FxFrames.COIN_6[((int) (elapsed * 12)) % 4] );
				break;
			case SNARE:
				if ((turnLeft -= dt) <= 0){
					turnLeft += SNARE_TURN;
					turns++;
					head.show( FxFrames.SLASH_16[(turns * 2) % 8] );
					second.show( FxFrames.SLASH_16[(turns * 2 + 4) % 8] );
				}
				//its two jaws held 2 px apart, each out along its own side: a snare open, tumbling
				int d = (turns * 2) % 8;
				float ox = 2f * (float) Math.cos( Math.toRadians( d * 45 ) ), oy = 2f * (float) Math.sin( Math.toRadians( d * 45 ) );
				head.place( x + ox, y + oy );
				second.place( x - ox, y - oy );
				second.am = 0.85f;
				second.visible = head.visible = seen;
				return;
			case FEATHER_DART:
				head.angle = angle;
				if ((trailLeft -= dt) <= 0){
					trailLeft += TRAIL * 2;
					if (seen) featherTrail( x, y );
				}
				break;
			case PELLET:
				if ((trailLeft -= dt) <= 0){
					trailLeft += TRAIL * 2;
					if (seen) inkTrail( x, y );
				}
				break;
			case WEIGHT:
				//straight down over its cell, on its rope from where it fell
				x = toX;
				second.x = x - 0.5f;
				second.y = fromY;
				second.scale.set( 1, Math.max( 0f, y - 4 - fromY ) );
				second.visible = seen;
				break;
			default:
				break;
		}
		if (head == null) return;
		if (kind == SPIRIT_ARROW || kind == HOLY_BLADE){
			//pointing along its way, its tip on the line
			float c = (float) Math.cos( Math.toRadians( angle ) ), s = (float) Math.sin( Math.toRadians( angle ) );
			float len = head.width - 1;
			if (cored) core( x, y, c, s, len + (second != null ? 10 : 0), seen );
			head.x = x - c * len - head.origin.x;
			head.y = y - s * len - head.origin.y;
			head.angle = angle;
			if (second != null){
				second.x = x - c * (len + 10) - second.origin.x;
				second.y = y - s * (len + 10) - second.origin.y;
				second.angle = angle;
				second.am = 0.85f;
				second.visible = seen;
			}
			if (ghost1 != null){
				ghost( ghost1, p1x, p1y, c, s, len, 0.5f, seen );
				ghost( ghost2, p2x, p2y, c, s, len, 0.25f, seen );
			}
			p2x = p1x;
			p2y = p1y;
			p1x = x;
			p1y = y;
		} else {
			head.place( x, y );
		}
		head.visible = seen;
	}

	private void ghost( FxPic g, float x, float y, float c, float s, float len, float a, boolean seen ){
		g.x = x - c * len - g.origin.x;
		g.y = y - s * len - g.origin.y;
		g.angle = angle;
		g.am = a;
		g.visible = seen && (x != fromX || y != fromY);
	}

	//a light's core: every CORE_EVERY s a mote behind it (its tail's end), laid where the tail was at
	//that moment (several in one long frame spread along the way it flew this frame), stepping 2 px
	//to 1 px, pale then its colour, still (it is the flight that moves); at most CORE_MOST alive
	private void core( float x, float y, float c, float s, float back, boolean seen ){
		for (int i = 0; i < coreAlive; ){
			if (coreUntil[i] <= elapsed) coreUntil[i] = coreUntil[--coreAlive];
			else i++;
		}
		float dt = Game.elapsed;
		coreLeft -= dt;
		int n = 0;
		while (coreLeft <= 0){
			coreLeft += CORE_EVERY;
			n++;
		}
		if (!seen) return;
		Ramp r = kind == SPIRIT_ARROW ? SPIRIT_CORE : HOLY_CORE;
		for (int k = 0; k < n && coreAlive < CORE_MOST; k++){
			float u = (k + 1f) / n;
			float px = p1x + (x - p1x) * u - c * back, py = p1y + (y - p1y) * u - s * back;
			//nothing over the one who loosed it: the core begins CLEAR px out
			if ((px - fromX) * c + (py - fromY) * s < CLEAR) continue;
			FxParticle m = SkillVfx.glow( px, py, FxBudget.P2 );
			if (m == null) continue;
			coreUntil[coreAlive++] = elapsed + CORE_LIFE;
			m.look( CORE_STEPS, 0 ).ramp( r, true ).life( CORE_LIFE ).alpha( 0.9f, 0, CORE_LIFE * 0.4f ).snap();
		}
	}

	//where it lands, a glint of its light (a moment, crisp)
	private void landed(){
		if (!Fx.seen( toX, toY )) return;
		FxParticle g = SkillVfx.glow( toX, toY, FxBudget.P1 );
		if (g != null) g.look( FxFrames.STAR4_5 ).paint( kind == SPIRIT_ARROW ? SkillPalette.NATURE.stop( 0 ) : SkillPalette.HOLY.stop( 0 ) )
				.life( LANDING ).alpha( 1f, 0, LANDING * 0.5f ).snap();
	}

	//a meteor's trail: an ember of the skills' fire and a streak of flame behind it
	private void meteorTrail( float x, float y ){
		float c = (float) Math.cos( Math.toRadians( angle ) ), s = (float) Math.sin( Math.toRadians( angle ) );
		FxParticle e = SkillVfx.glow( x - c * 3, y - s * 3, FxBudget.P2 );
		if (e != null) e.look( FxRandom.chance( 0.6f ) ? FxFrames.EMBER_1 : FxFrames.EMBER_2 ).ramp( SkillBursts.FIRE, true )
				.vel( FxRandom.Float( -8f, 8f ), FxRandom.Float( -12f, -4f ) ).drag( 1f ).life( FxRandom.Float( 0.3f, 0.5f ) )
				.alpha( 1f, 0, 0.2f );
		FxParticle f = SkillVfx.glow( x - c * 6, y - s * 6, FxBudget.P2 );
		if (f != null){
			f.look( FxFrames.STREAK_FLAME[FxRandom.Int( 2 )] ).ramp( SkillBursts.FIRE, true ).life( 0.12f ).alpha( 0.9f, 0, 0.08f );
			f.angle = angle;
		}
	}

	//a dart of feather: a short taper of nature light behind it
	private void featherTrail( float x, float y ){
		float c = (float) Math.cos( Math.toRadians( angle ) ), s = (float) Math.sin( Math.toRadians( angle ) );
		FxParticle t = SkillVfx.glow( x - c * 5, y - s * 5, FxBudget.P2 );
		if (t != null){
			t.look( FxFrames.STREAK_TAPER ).paint( SPIRIT ).life( 0.08f ).alpha( 0.6f, 0, 0.06f );
			t.angle = angle;
			t.scale.set( 0.5f, 1 );
		}
	}

	//a pellet of ink: wisps behind it
	private void inkTrail( float x, float y ){
		FxParticle w = SkillVfx.matter( x, y, FxBudget.P2 );
		if (w != null) w.look( FxFrames.INK_WISP, 5 ).paint( Element.SHADOW.ramp.step( 0.35f ) )
				.vel( FxRandom.Float( -3f, 3f ), FxRandom.Float( 1f, 5f ) ).turbulence( 1f, 4f ).life( 0.35f )
				.alpha( 0.6f, 0.02f, 0.2f ).smooth();
	}

	private void release(){
		if (cored){
			cored = false;
			leaveCore();
		}
		if (lights != null){
			Fx.dispose( lights );
			lights = null;
		}
		if (glow != null){
			glow.putOut( 0.08f );
			glow = null;
		}
	}

	@Override
	public void kill(){
		release();
		super.kill();
	}

	@Override
	public void destroy(){
		release();
		super.destroy();
	}
}
