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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.Source;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;

/**
 * Water falling (docs/fx/water-life.md, "Pipes and waterfalls"): the sewers' drain pipes over
 * their pools (SewerLevel's Sink) and the water spilling over the lip of a pit (FlowParticle.Flow),
 * each an FxEmitter of its own in the level's visuals that lays its particles in itself (none of
 * the game's random numbers drawn, nothing sent to a co-op guest: his level builds the same).
 *
 * A pipe: a drop every 0.12 s let go from its mouth and landing on the water at its foot 2 px into
 * the pool below (gravity 200: DROP_2 or DROP_3 in the water's highlight or foam), and a small ring
 * there every 0.4-0.6 s. A pipe that pours (one in three: Source.pours) lets go a sheet instead: a
 * DROP_3 streak every 0.03 s in turn down three columns, foam churning at its foot (FOAM_H at
 * 6 frames a second, 0.55) and a middling ring every 0.3 s. Its rings stir nothing.
 *
 * A pit's lip: a 1 px line of foam along the water's edge over the pit at 0.6, flickering between
 * two looks four times a second; streaks of the highlight falling 10-14 px into the pit at 200
 * px/s^2 (no turning), six a second; a droplet thrown off the lip every 2 s that falls back into
 * the pit; a mist (EDGE_WISP at 0.15) sinking 1 px/s below it every 2 s. Half as busy beyond six
 * cells of the hero, and thinned as more than six such edges show at once; at most 8 particles an
 * edge, 64 for all of them. An edge going out of sight lets what it had falling go (its emitter
 * stops updating them), so edges left behind never hold the others' share.
 */
public final class WaterFalls {

	private WaterFalls(){}

	private static final float SIZE = DungeonTilemap.SIZE;

	// ------------------------------------------------------------------ pipes

	/** A pipe's trickle: a drop this often, its gravity, its ring's wait. */
	public static final float TRICKLE_EVERY = 0.12f, FALL_G = 200f, RING_MIN = 0.4f, RING_MAX = 0.6f;
	/** A pouring pipe's sheet: a streak this often (down three columns in turn), its ring's wait,
	 *  its foot's foam's life and alpha. */
	public static final float POUR_EVERY = 0.03f, POUR_RING = 0.3f, POUR_FOAM_LIFE = 0.5f, POUR_FOAM_ALPHA = 0.55f;
	/** How far into the pool below the foot lies, px. */
	public static final float FOOT_IN = 2f;

	/** A drain pipe on a wall cell: where its water leaves it and lands, whether it pours. */
	public static final class Pipe {
		public final int pos;
		public final float mouthX, mouthY, footY;
		public final boolean pours, overWater;
		float dropIn, ringIn, foamIn;
		int column;

		Pipe( int pos, float mouthX, float mouthY, float footY, boolean pours, boolean overWater ){
			this.pos = pos;
			this.mouthX = mouthX;
			this.mouthY = mouthY;
			this.footY = footY;
			this.pours = pours;
			this.overWater = overWater;
			ringIn = FxRandom.Float( RING_MIN, RING_MAX );
		}
	}

	/** The pipe on the wall cell `pos` of the level: its mouth 3 px below the cell's middle, its
	 *  foot 2 px into the cell below. */
	public static Pipe pipe( Level level, int pos ){
		int w = level.width();
		float cx = (pos % w + 0.5f) * SIZE, top = (pos / w) * SIZE;
		boolean water = pos + w < level.length() && level.water[pos + w];
		return new Pipe( pos, cx, top + SIZE / 2f + 3f, top + SIZE + FOOT_IN, water && Source.pours( pos ), water );
	}

	/** A pipe's frame, while the hero sees it (its emitter's update). */
	public static void pipe( Pipe p, FxEmitter e, float dt ){
		if (dt <= 0f) return;
		Element.Liquid l = WaterFX.liquid();
		if (p.pours){
			p.dropIn -= dt;
			while (p.dropIn <= 0f){
				p.dropIn += POUR_EVERY;
				p.column = (p.column + 1) % 3;
				streak( e, p.mouthX - 1 + p.column, p.mouthY, p.footY, l.highlight, 0.85f );
			}
			if ((p.foamIn -= dt) <= 0f){
				p.foamIn = POUR_FOAM_LIFE * 0.8f;
				Marks.Mark m = Marks.make( Marks.POUR, false, p.mouthX, p.footY + 1 );
				if (m != null) m.look( FxFrames.FOAM_H, 6 ).paint( l.foam ).life( POUR_FOAM_LIFE )
						.alpha( POUR_FOAM_ALPHA, 0.1f, 0.15f ).smooth().snap().priority( FxBudget.P2 );
			}
			if ((p.ringIn -= dt) <= 0f){
				p.ringIn = POUR_RING;
				WaterFX.quietRing( p.mouthX, p.footY, WaterFX.M, l.foam, WaterFX.alphaOf( WaterFX.M ) * 0.8f, FxBudget.P3 );
			}
			return;
		}
		p.dropIn -= dt;
		while (p.dropIn <= 0f){
			p.dropIn += TRICKLE_EVERY;
			streak( e, p.mouthX + FxRandom.Float( -1.5f, 1.5f ), p.mouthY, p.footY,
					FxRandom.chance( 0.6f ) ? l.highlight : l.foam, 0.7f );
		}
		if (p.overWater && (p.ringIn -= dt) <= 0f){
			p.ringIn = FxRandom.Float( RING_MIN, RING_MAX );
			WaterFX.quietRing( p.mouthX, p.footY, WaterFX.S, l.foam, WaterFX.alphaOf( WaterFX.S ), FxBudget.P3 );
		}
	}

	//a drop falling from (x, y) to its foot
	private static void streak( FxEmitter e, float x, float y, float foot, int color, float alpha ){
		if (!FxBudget.keep( FxBudget.P2, x, y )) return;
		((FxParticle) e.recycle( FxParticle.class )).reset( Math.round( x ), y )
				.look( FxRandom.chance( 0.7f ) ? FxFrames.DROP_3 : FxFrames.DROP_2 ).paint( color ).vel( 0, 6f )
				.gravity( FALL_G ).ground( foot, FxParticle.DIE, 0 ).life( 0.5f ).alpha( alpha, 0, 0f ).priority( FxBudget.P2 );
	}

	// ------------------------------------------------------------------ waterfalls

	/** A lip's foam: its alpha's two looks and their pace. */
	public static final float LIP_ALPHA = 0.6f, LIP_DIM = 0.42f, LIP_FPS = 4f;
	/** Its streaks: a second, their fall's reach, their speed off the lip. */
	public static final float STREAKS = 6f, REACH_MIN = 10f, REACH_MAX = 14f;
	/** A droplet off the lip, a mist below it: this often, s; the mist's alpha and sinking. */
	public static final float BOUNCE_EVERY = 2f, MIST_EVERY = 2f, MIST_ALPHA = 0.15f, MIST_SINK = 1f;
	/** At most an edge's, and all of them. */
	public static final int PER_EDGE = 8, ALL = 64;
	/** Beyond this many cells from the hero, half as busy; past this many edges in sight, thinned. */
	public static final int NEAR = 6, EDGES = 6;

	//the edges seen last frame and this one, by the scene's clock
	private static int shownLast, shownNow;
	private static float countedAt = -1f;

	/** An edge of water over a pit: its lip's foam and what falls from it. */
	public static final class Fall {
		public final int pos;
		public final float x0, lipY;
		final ColorBlock lip;
		float streaks, bounceIn, mistIn, flick;
		int live;

		Fall( int pos, float x0, float lipY ){
			this.pos = pos;
			this.x0 = x0;
			this.lipY = lipY;
			lip = new ColorBlock( SIZE, 1, 0xFFFFFFFF );
			lip.x = x0;
			lip.y = lipY;
			lip.visible = false;
			bounceIn = FxRandom.Float( 0f, BOUNCE_EVERY );
			mistIn = FxRandom.Float( 0f, MIST_EVERY );
		}

		/** Its lip's foam (its emitter holds it, to draw it with what falls). */
		public ColorBlock lip(){
			return lip;
		}

		/** Its particles out now. */
		public int live(){
			return live;
		}
	}

	/** The edge of the water cell `pos` over the pit below it: its lip the cell's lowest row. */
	public static Fall fall( Level level, int pos ){
		int w = level.width();
		return new Fall( pos, (pos % w) * SIZE, (pos / w + 1) * SIZE - 1f );
	}

	/** The particles every edge has out (tests). */
	public static int falling(){
		return FallDrop.count;
	}

	/** A new scene: none of the last one's. */
	public static void reset(){
		FallDrop.count = 0;
		shownLast = shownNow = 0;
		countedAt = -1f;
	}

	/** An edge's frame (its emitter's update): `shown` whether the hero sees it. */
	public static void fall( Fall f, FxEmitter e, boolean shown, float dt ){
		float now = Game.timeTotal;
		if (now != countedAt){
			countedAt = now;
			shownLast = shownNow;
			shownNow = 0;
		}
		f.lip.visible = shown;
		if (!shown){
			if (f.live > 0) letGo( f, e );
			return;
		}
		if (dt <= 0f) return;
		shownNow++;
		Element.Liquid l = WaterFX.liquid();
		f.flick += dt * LIP_FPS;
		f.lip.resetColor();
		f.lip.hardlight( l.foam );
		f.lip.am = ((int)f.flick & 1) == 0 ? LIP_ALPHA : LIP_DIM;

		float share = share( f );
		f.streaks += STREAKS * share * dt;
		while (f.streaks >= 1f){
			f.streaks -= 1f;
			float x = f.x0 + FxRandom.IntRange( 1, 14 ), y = f.lipY + 1;
			FallDrop d = drop( f, e, x, y );
			if (d != null) d.look( FxFrames.DROP_3 ).paint( l.highlight ).vel( 0, FxRandom.Float( 8f, 16f ) ).gravity( FALL_G )
					.ground( y + FxRandom.Float( REACH_MIN, REACH_MAX ), FxParticle.DIE, 0 ).life( 0.6f )
					.alpha( 0.8f, 0, 0.1f ).priority( FxBudget.P2 );
		}
		if ((f.bounceIn -= dt) <= 0f){
			f.bounceIn = BOUNCE_EVERY / Math.max( 0.25f, share );
			float x = f.x0 + FxRandom.IntRange( 2, 13 );
			FallDrop d = drop( f, e, x, f.lipY );
			if (d != null) d.look( FxFrames.DROP_2 ).paint( l.foam ).orient()
					.polar( (float)(-Math.PI / 2) + FxRandom.Float( -0.6f, 0.6f ), FxRandom.Float( 15f, 25f ) ).gravity( 240f )
					.ground( f.lipY + FxRandom.Float( REACH_MIN, REACH_MAX ), FxParticle.DIE, 0 ).life( 0.8f )
					.alpha( 0.9f, 0, 0.1f ).priority( FxBudget.P3 );
		}
		if ((f.mistIn -= dt) <= 0f){
			f.mistIn = MIST_EVERY / Math.max( 0.25f, share );
			float x = f.x0 + FxRandom.IntRange( 4, 12 ), y = f.lipY + FxRandom.Float( 4f, 8f );
			FallDrop d = drop( f, e, x, y );
			float life = FxRandom.Float( 2f, 3f );
			if (d != null) d.look( FxFrames.EDGE_WISP, 0 ).paint( l.foam ).vel( FxRandom.Float( -1f, 1f ), MIST_SINK )
					.life( life ).alpha( MIST_ALPHA, life * 0.3f, life * 0.4f ).smooth().priority( FxBudget.P3 );
		}
	}

	/** An edge's share of its full life: half beyond six cells of the hero, and six edges' worth
	 *  shared among however many show past six. */
	static float share( Fall f ){
		float s = shownLast > EDGES ? EDGES / (float)shownLast : 1f;
		Level level = Dungeon.level;
		if (Dungeon.hero != null && level != null && Dungeon.hero.pos >= 0){
			int w = level.width();
			int d = Math.max( Math.abs( f.pos % w - Dungeon.hero.pos % w ), Math.abs( f.pos / w - Dungeon.hero.pos / w ) );
			if (d > NEAR) s *= 0.5f;
		}
		return s;
	}

	//what an edge out of sight had falling: gone, its count with it (once as it goes out of sight:
	//nothing of it is left falling after)
	private static void letGo( Fall f, FxEmitter e ){
		for (Gizmo g : e.membersView()){
			if (g instanceof FallDrop && g.alive && ((FallDrop) g).of == f) g.kill();
		}
	}

	private static FallDrop drop( Fall f, FxEmitter e, float x, float y ){
		if (f.live >= PER_EDGE || FallDrop.count >= ALL || !FxBudget.keep( FxBudget.P2 )) return null;
		FallDrop d = (FallDrop) e.recycle( FallDrop.class );
		d.reset( x, y );
		d.of( f );
		return d;
	}

	/** What falls from an edge: counted against its edge's 8 and every edge's 64. */
	public static class FallDrop extends FxParticle {
		static int count;
		private Fall of;

		void of( Fall f ){
			if (of == null) count++;
			else of.live--;
			of = f;
			f.live++;
		}

		@Override
		public void kill(){
			super.kill();
			if (of != null){
				of.live--;
				of = null;
				count--;
			}
		}
	}
}
