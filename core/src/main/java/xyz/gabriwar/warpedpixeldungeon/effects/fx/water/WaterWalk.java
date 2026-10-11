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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.water;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.Ripple;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxModules;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;
import com.watabou.utils.RectF;

/**
 * Walking in the water (docs/fx/water-life.md, "Walking"), from the kit's sprite hooks: what a
 * sprite's steps and landings do to the water - and to the grass and the snow - seen on the
 * render side alone, so a co-op guest's replayed moves wade alike. Fliers make nothing.
 *
 *  - Into the water: a crown thrown up at the feet where it will stand (5x4 to 9x5 over 0.18 s),
 *    three to five droplets (30-60 px/s within 50 degrees of up, falling at 240 to ring the water
 *    within 4 px) and a middling ring 0.06 s on - from the step's first frame, with the WATER sound.
 *  - Through it: a wake behind the feet (WAKE_H, WAKE_V or WAKE_D by the way it goes, mirrored for
 *    the other four, growing over 0.6 s and fading from 0.55) and a small ring where it stepped
 *    from (today's middling ripple made small).
 *  - Out of it: a small ring where it stepped from, two droplets shed from its feet (a wet mark
 *    where they fall on the floor).
 *  - Coming to a stop in it: a small ring at its feet.
 *  - Standing in it (the eight waders nearest the hero, looked over four times a second): a
 *    waterline drawn right after the sprite - a band of the water's body 2 px deep at 0.65 over its
 *    lowest two rows (its width less 4, centred), and the front half of a collar of foam round its
 *    legs (9, 13 or 17 px by its width) at 0.55 - bobbing a pixel at 1.2 Hz on whole pixels, and a
 *    small ring every 1.4 to 2.6 s. A wide sprite (more than 18 px: the large) makes middling rings
 *    for small, large for middling, and half again as many droplets.
 *  - A jump landing in it: the large crown, six to eight droplets, a middling ring and a large one
 *    0.12 s on.
 *  - On the lava: an added ring in its glow and two sparks for a step, the band in its body at
 *    0.75, no droplets; a jump's landing a ring and embers.
 *  - Footsteps elsewhere (one a 0.2 s a sprite at most): a blade or two of grass flicked back off
 *    the grass in the level's colours (20-40 px/s, falling at 120, settling and fading in 0.4 s),
 *    a small white puff off the snow.
 *
 * The hooks may come on any thread (a step is the actor thread's, mostly): the work is handed to
 * the render thread. The waterline's two pictures are kept by the live surface (so they go with
 * the scene) but drawn only by the sprite's own draw.
 */
public final class WaterWalk {

	private WaterWalk(){}

	private static final float SIZE = DungeonTilemap.SIZE;

	/** Wider than this, a wader is large: bigger rings and crowns, more droplets. */
	static final float LARGE_W = 18f;
	/** Waterlines at once, and how often the waders are looked over, s. */
	public static final int WATERLINES = 8;
	static final float POLL = 0.25f;
	static final float BAND_ALPHA = 0.65f, BAND_LAVA = 0.75f, COLLAR_ALPHA = 0.55f;
	static final float STAND_MIN = 1.4f, STAND_MAX = 2.6f;
	static final float WAKE_LIFE = 0.6f, WAKE_ALPHA = 0.55f, WAKE_BEHIND = 3f;
	static final float ENTER_RING_AFTER = 0.06f, JUMP_RING_AFTER = 0.12f;
	static final float FOOTSTEP_GAP = 0.2f;

	private static final int[][][] WAKES = { FxFrames.WAKE_H, FxFrames.WAKE_V, FxFrames.WAKE_D };

	// ------------------------------------------------------------------ the hooks

	/** A sprite stepped (any thread): what the water, the grass or the snow make of it. */
	public static void stepped( final CharSprite s, final int from, final int to ){
		Level level = Dungeon.level;
		if (level == null || s.ch == null || s.ch.flying || !s.visible) return;
		if (from < 0 || to < 0 || from >= level.length() || to >= level.length()) return;
		Fx.post( () -> step( s, from, to ) );
	}

	/** A sprite landed from a jump on its character's cell. */
	public static void jumped( final CharSprite s ){
		Level level = Dungeon.level;
		if (level == null || s.ch == null || s.ch.flying || !s.visible) return;
		final int at = s.ch.pos;
		if (at < 0 || at >= level.length() || !level.water[at]) return;
		Fx.post( () -> landed( s, at ) );
	}

	static void step( CharSprite s, int from, int to ){
		Level level = Dungeon.level;
		if (level == null || to >= level.length() || from >= level.length()) return;
		boolean wFrom = level.water[from], wTo = level.water[to];
		if (!wFrom && !wTo){
			footstep( s, from, to, level );
			return;
		}
		Element.Liquid l = WaterFX.liquid();
		boolean lava = l.ringAdditive;
		boolean large = s.width() > LARGE_W;
		int small = large ? WaterFX.M : WaterFX.S, middling = large ? WaterFX.L : WaterFX.M;
		PointF at = feet( s, to ), was = feet( s, from );
		if (wTo && !wFrom){
			//into the water
			if (lava){
				lavaStep( at.x, at.y );
				return;
			}
			WaterFX.crown( at.x, at.y, large ? WaterFX.L : WaterFX.M, l.foam );
			WaterFX.droplets( at.x, at.y - 1, Math.round( FxRandom.IntRange( 3, 5 ) * (large ? 1.5f : 1f) ), 30f, 60f, 100f, l.foam );
			Ripple r = WaterFX.ring( at.x, at.y, middling, l.foam, WaterFX.alphaOf( middling ), FxBudget.P1 );
			if (r != null) r.delay( ENTER_RING_AFTER );
		} else if (wTo){
			//through it
			if (lava){
				lavaStep( was.x, was.y );
				return;
			}
			wake( at.x, at.y, to % level.width() - from % level.width(), to / level.width() - from / level.width(), l );
			WaterFX.ring( was.x, was.y, small, l.foam, WaterFX.alphaOf( small ), FxBudget.P1 );
		} else {
			//out of it
			if (lava){
				lavaStep( was.x, was.y );
				return;
			}
			WaterFX.ring( was.x, was.y, small, l.foam, WaterFX.alphaOf( small ), FxBudget.P1 );
			WaterFX.droplets( was.x, was.y - 1, large ? 3 : 2, 25f, 45f, 80f, l.foam );
		}
	}

	//a step on the lava: an added ring in its glow, two sparks
	private static void lavaStep( float x, float y ){
		WaterFX.ring( x, y, WaterFX.S );
		FxRecipes.sparks( x, y - 1, Element.LAVA, 2 );
	}

	static void landed( CharSprite s, int at ){
		Level level = Dungeon.level;
		if (level == null || at >= level.length() || !level.water[at]) return;
		Element.Liquid l = WaterFX.liquid();
		PointF p = feet( s, at );
		if (l.ringAdditive){
			WaterFX.ring( p.x, p.y, WaterFX.M );
			FxRecipes.embers( p.x, p.y - 2, Element.LAVA, 6 );
			return;
		}
		WaterFX.crown( p.x, p.y, WaterFX.L, l.foam );
		WaterFX.droplets( p.x, p.y - 1, FxRandom.IntRange( 6, 8 ), 35f, 70f, 110f, l.foam );
		WaterFX.ring( p.x, p.y, WaterFX.M, l.foam, WaterFX.alphaOf( WaterFX.M ), FxBudget.P1 );
		Ripple big = WaterFX.ring( p.x, p.y, WaterFX.L, l.foam, WaterFX.alphaOf( WaterFX.L ), FxBudget.P1 );
		if (big != null) big.delay( JUMP_RING_AFTER );
	}

	/** Where a sprite's feet stand on a cell: the middle of its lowest row as it is drawn there. */
	static PointF feet( CharSprite s, int cell ){
		PointF p = s.worldToCamera( cell );
		p.x += s.width() / 2f;
		p.y += s.height();
		return p;
	}

	//the wake behind the feet at (x, y), the step having gone (dx, dy) cells
	static void wake( float x, float y, int dx, int dy, Element.Liquid l ){
		int kind = WaterGeom.wakeKind( dx, dy );
		int[][] frames = WAKES[kind];
		int[] mid = frames[1];
		//the painted wakes trail a wader going east, south or south-east, their point at its feet
		float cx = x, cy = y;
		if (dx != 0) cx -= Math.signum( dx ) * (WAKE_BEHIND + mid[2] / 2f);
		if (dy != 0) cy -= Math.signum( dy ) * (WAKE_BEHIND + mid[3] / 2f);
		if (!FxBudget.keep( FxBudget.P1 )) return;
		Marks.Mark m = Marks.make( Marks.WAKE, false, cx, cy );
		if (m == null) return;
		m.look( frames, 0 ).paint( l.foam ).smooth().life( WAKE_LIFE ).alpha( WAKE_ALPHA, 0f, WAKE_LIFE ).snap()
				.priority( FxBudget.P1 );
		m.flip( WaterGeom.wakeFlipX( dx ), WaterGeom.wakeFlipY( dy ) );
	}

	// ------------------------------------------------------------------ footsteps

	private static void footstep( CharSprite s, int from, int to, Level level ){
		int t = level.map[from];
		boolean grass = t == Terrain.GRASS || t == Terrain.FURROWED_GRASS;
		boolean snow = t == Terrain.SNOW;
		if (!grass && !snow) return;
		Wader w = wader( s );
		float now = Game.timeTotal;
		if (now >= w.stepAt && now - w.stepAt < FOOTSTEP_GAP) return;
		w.stepAt = now;
		PointF p = feet( s, from );
		FxEmitter air = Fx.air();
		if (air == null) return;
		int dx = to % level.width() - from % level.width(), dy = to / level.width() - from / level.width();
		if (snow){
			if (!FxBudget.keep( FxBudget.P3, p.x, p.y )) return;
			((FxParticle) air.recycle( FxParticle.class )).reset( p.x - dx * 2, p.y - 2 ).look( FxFrames.PUFF_S, 0 )
					.paint( 0xFFFFFF ).smooth().life( 0.4f ).alpha( 0.4f, 0.05f, 0.3f ).vel( -dx * 6f, -4f )
					.snap().priority( FxBudget.P3 );
			return;
		}
		int n = FxRandom.IntRange( 1, 2 );
		for (int i = 0; i < n; i++){
			if (!FxBudget.keep( FxBudget.P3, p.x, p.y )) continue;
			//flung back, against the way it went, and up
			float a = (float)Math.atan2( -dy - 1.2f, -dx + FxRandom.Float( -0.6f, 0.6f ) );
			((FxParticle) air.recycle( FxParticle.class )).reset( p.x, p.y - 1 )
					.look( FxRandom.chance( 0.5f ) ? FxFrames.BLADE_3 : FxFrames.BLADE_4 )
					.paint( FxRandom.chance( 0.5f ) ? level.color1 : level.color2 )
					.polar( a, FxRandom.Float( 20f, 40f ) ).gravity( 120f ).spin( 0.12f )
					.ground( p.y + FxRandom.Float( 0f, 3f ), FxParticle.SETTLE, 0 ).life( 0.9f ).alpha( 1f, 0, 0.4f )
					.priority( FxBudget.P3 );
		}
	}

	// ------------------------------------------------------------------ waders

	/** What the water keeps for a sprite (in its fxSlots): its waterline's pictures, whether it is
	 *  one of the eight drawn, its stand's ring timer, its last footstep. */
	static final class Wader {
		final CharSprite sprite;
		ColorBlock band;
		Image collar;
		RectF collarFrame;
		boolean active, wasMoving, wet;
		float standIn = FxRandom.Float( STAND_MIN, STAND_MAX ), stepAt = -1f, phase = FxRandom.angle();

		Wader( CharSprite sprite ){
			this.sprite = sprite;
		}

		//its pictures, kept by the live surface (they go with the scene), never drawn by it
		void build(){
			if (band != null) return;
			band = new ColorBlock( 1, 2, 0xFFFFFFFF );
			collar = new Image( Assets.Effects.FX_SHEET );
			band.visible = collar.visible = false;
			Group keep = WaterLive.keeper();
			if (keep != null){
				keep.add( band );
				keep.add( collar );
			}
		}

		void dispose(){
			if (band != null){
				Fx.dispose( band );
				Fx.dispose( collar );
				band = null;
				collar = null;
			}
		}
	}

	private static Wader wader( CharSprite s ){
		Object o = s.fxSlots[FxModules.WATER_LIFE];
		if (o instanceof Wader) return (Wader) o;
		Wader w = new Wader( s );
		s.fxSlots[FxModules.WATER_LIFE] = w;
		return w;
	}

	/** A sprite was killed: its waterline's pictures go (any thread). */
	public static void gone( CharSprite s ){
		Object o = s.fxSlots[FxModules.WATER_LIFE];
		if (o instanceof Wader){
			s.fxSlots[FxModules.WATER_LIFE] = null;
			((Wader) o).dispose();
		}
	}

	//the waders standing in the water drawn since the last look, and the ones with a waterline
	private static final Wader[] seen = new Wader[32], lined = new Wader[WATERLINES];
	private static int nSeen, nLined;
	private static float pollIn;

	/** A new scene: no wader of the last one's. */
	static void reset(){
		for (int i = 0; i < seen.length; i++) seen[i] = null;
		for (int i = 0; i < lined.length; i++) lined[i] = null;
		nSeen = nLined = 0;
		pollIn = 0;
	}

	//where a sprite stands as it is drawn: the cell under the middle of its lowest row
	private static int drawnCell( CharSprite s ){
		return Fx.cellAt( s.x + s.width() / 2f, s.y + s.height() - 1f );
	}

	/** Right after a sprite is drawn (render thread): its waterline, if it is one of the eight. */
	public static void afterDraw( CharSprite s ){
		Level level = Dungeon.level;
		if (level == null || s.ch == null || s.ch.flying || level.water == null) return;
		int c = drawnCell( s );
		boolean wet = c >= 0 && level.water[c];
		Object o = s.fxSlots[FxModules.WATER_LIFE];
		if (!wet){
			if (o instanceof Wader) ((Wader) o).wet = false;
			return;
		}
		Wader w = wader( s );
		w.wet = true;
		if (nSeen < seen.length && !contains( seen, nSeen, w )) seen[nSeen++] = w;
		//in the air mid-jump it leaves the water behind
		if (w.active && !s.jumping()) drawLine( w );
	}

	private static boolean contains( Wader[] a, int n, Wader w ){
		for (int i = 0; i < n; i++) if (a[i] == w) return true;
		return false;
	}

	//the collar's front halves: the lower rows of each collar frame, by width
	private static final RectF[][] COLLARS = { lower( FxFrames.COLLAR_9 ), lower( FxFrames.COLLAR_13 ), lower( FxFrames.COLLAR_17 ) };
	private static final int[] COLLAR_H = { half( FxFrames.COLLAR_9 ), half( FxFrames.COLLAR_13 ), half( FxFrames.COLLAR_17 ) };

	private static int half( int[][] frames ){
		return (frames[0][3] + 1) / 2;
	}

	private static RectF[] lower( int[][] frames ){
		RectF[] out = new RectF[frames.length];
		float tw = 256f, th = 256f;
		for (int i = 0; i < frames.length; i++){
			int[] f = frames[i];
			int h = (f[3] + 1) / 2;
			out[i] = new RectF( f[0] / tw, (f[1] + f[3] - h) / th, (f[0] + f[2]) / tw, (f[1] + f[3]) / th );
		}
		return out;
	}

	private static void drawLine( Wader w ){
		w.build();
		CharSprite s = w.sprite;
		Element.Liquid l = WaterFX.liquid();
		float alpha = s.alpha();
		float feet = s.y + s.height();
		float t = Game.timeTotal;
		int bob = WaterGeom.bob( t, w.phase );

		ColorBlock band = w.band;
		band.camera = s.camera();
		band.x = Math.round( s.x + 2 );
		band.y = Math.round( feet - 2 + bob );
		band.size( Math.max( 1, Math.round( s.width() - 4 ) ), 2 );
		band.resetColor();
		band.hardlight( l.body.stop( Math.min( 1, l.body.size() - 1 ) ) );
		band.am = alpha * (l.ringAdditive ? BAND_LAVA : BAND_ALPHA);
		band.draw();

		int k = WaterGeom.collarWidth( s.width() ) == 9 ? 0 : WaterGeom.collarWidth( s.width() ) == 13 ? 1 : 2;
		Image collar = w.collar;
		RectF[] frames = COLLARS[k];
		//its two frames at the bob's own pace
		RectF f = frames[(((int)(t * 2.4f + w.phase)) & 1) % frames.length];
		if (w.collarFrame != f){
			w.collarFrame = f;
			collar.frame( f );
		}
		collar.camera = s.camera();
		collar.x = Math.round( s.x + s.width() / 2f - collar.width / 2f );
		collar.y = Math.round( feet - COLLAR_H[k] + 1 + bob );
		collar.resetColor();
		collar.hardlight( l.foam );
		collar.am = alpha * COLLAR_ALPHA;
		collar.draw();
	}

	/** Four times a second (the live surface's update): the eight waders nearest the hero get a
	 *  waterline; every frame, a ring as one comes to a stop and now and then as it stands. */
	static void update( float dt ){
		if (Emitter.freezeEmitters) return;
		if ((pollIn -= dt) <= 0f){
			pollIn = POLL;
			poll();
		}
		Level level = Dungeon.level;
		if (level == null) return;
		Element.Liquid l = WaterFX.liquid();
		for (int i = 0; i < nLined; i++){
			Wader w = lined[i];
			CharSprite s = w.sprite;
			if (!w.wet || !s.visible || s.parent == null) continue;
			boolean moving = s.isMoving;
			float x = s.x + s.width() / 2f, y = s.y + s.height();
			boolean large = s.width() > LARGE_W;
			if (w.wasMoving && !moving){
				//it came to a stop in the water
				WaterFX.ring( x, y, large ? WaterFX.M : WaterFX.S, l.foam, WaterFX.alphaOf( WaterFX.S ), FxBudget.P1 );
				w.standIn = FxRandom.Float( STAND_MIN, STAND_MAX );
			} else if (!moving && (w.standIn -= dt) <= 0f){
				w.standIn = FxRandom.Float( STAND_MIN, STAND_MAX );
				WaterFX.ring( x, y, large ? WaterFX.M : WaterFX.S, l.foam, WaterFX.alphaOf( WaterFX.S ) * 0.8f, FxBudget.P2 );
			}
			w.wasMoving = moving;
		}
	}

	//the eight nearest the hero of the waders seen standing in the water since the last look
	private static void poll(){
		for (int i = 0; i < nLined; i++) lined[i].active = false;
		nLined = 0;
		CharSprite hero = Dungeon.hero != null ? Dungeon.hero.sprite : null;
		float hx = 0, hy = 0;
		if (hero != null){
			hx = hero.x;
			hy = hero.y;
		} else if (Dungeon.hero != null && Dungeon.level != null && Dungeon.hero.pos >= 0){
			hx = (Dungeon.hero.pos % Dungeon.level.width()) * SIZE;
			hy = (Dungeon.hero.pos / Dungeon.level.width()) * SIZE;
		}
		//a few picks of the nearest: there are never more than 32 to choose among
		while (nLined < WATERLINES){
			int best = -1;
			float bestD = Float.MAX_VALUE;
			for (int i = 0; i < nSeen; i++){
				Wader w = seen[i];
				if (w == null || w.active || !w.wet) continue;
				float d = Math.max( Math.abs( w.sprite.x - hx ), Math.abs( w.sprite.y - hy ) );
				if (d < bestD){
					bestD = d;
					best = i;
				}
			}
			if (best < 0) break;
			Wader w = seen[best];
			w.active = true;
			lined[nLined++] = w;
		}
		for (int i = 0; i < nSeen; i++) seen[i] = null;
		nSeen = 0;
	}

	/** The waders with a waterline now (tests). */
	static int lined(){
		return nLined;
	}
}
