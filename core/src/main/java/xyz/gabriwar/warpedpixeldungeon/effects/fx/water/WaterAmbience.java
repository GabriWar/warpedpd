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
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.Source;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/**
 * What lives on the water's surface of itself (docs/fx/water-life.md, "A live surface" 3-4): the
 * glints of light on open water, the foam the water laps against its banks, the whitecaps of the
 * open sea in a wind and the swamp's methane bubbling up. Run by the live surface (WaterLive) on
 * the render thread, every mark through Marks (the surface slot, capped by kind).
 *
 * Twice a second, and at each step of the hero's, it looks over the water in view that he sees:
 * its cells (up to CELLS), its open cells (water on all four sides) and its lapping cells (water
 * with dry ground beside it and three liquid neighbours or more: Source.lapping), each lapping cell
 * keeping its own timer from one look to the next.
 *
 *  - Glints: a dot, a plus, a star, a plus, a dot over 0.38 s, added, on whole pixels, on open
 *    water - six a second per hundred cells on a clear day (one a second at least), 0.3 of that
 *    under clouds or underground, half a one per hundred at night, none in rain of 0.4 or more
 *    (WaterGeom.glintRate); the city's oil 0.3 and the caves' 0.4 of it, none on the lava; as the
 *    budget's governor keeps them (FxBudget.glints); under an open sky three in four nearer the
 *    sun's side of the water (east before noon, west after). Glint colour by day, the moon's at
 *    night; alpha 0.75 by day, 0.35 at night. At most 8.
 *  - Lap foam: every 3 to 7 s (on the overworld's beaches twice as often) each lapping cell lays
 *    foam along each of its dry sides, 2 px inside the water so the shore's lip hides some of it
 *    and it follows the shore's line: FOAM_H on a north or south side, FOAM_V on an east or west,
 *    its frames 0-1-2-1-0 over 1.6 s, its alpha 0 to 0.55 and back, in the liquid's foam. At
 *    most 8. None on the lava.
 *  - Whitecaps: over the open sea in a wind of 6 m/s or more, one or two a second per sixteen open
 *    cells, WHITECAP's frames over 0.5 s. At most 6.
 *  - Methane: in the swamps two to four bubbles per sixteen open cells every ten seconds, rising
 *    3 px and popping (POP_5) into a small ring. Bubbles at most 12 (with the lava's).
 *  - The cold water (the frozen places'): slush, and a mist lying on it at night (cold()).
 *  - The lava: its own life instead of the foam (WaterLava), the banks' light at each look.
 *  - Gnats over the water dip to it (dip(), from DungeonLife's clouds).
 */
public final class WaterAmbience {

	private static final float SIZE = DungeonTilemap.SIZE;

	/** Water cells looked at once at most, lapping cells kept. */
	static final int CELLS = 256, LAPS = 64;
	/** The look over the water: this often (and at each of the hero's steps), s. */
	static final float LOOK = 0.5f;

	static final int[][] GLINT = { FxFrames.MOTE_1, FxFrames.PLUS_3, FxFrames.STAR4_5, FxFrames.PLUS_3, FxFrames.MOTE_1 };
	static final float GLINT_LIFE = 0.38f;
	static final int[][] FOAM_H = { FxFrames.FOAM_H[0], FxFrames.FOAM_H[1], FxFrames.FOAM_H[2], FxFrames.FOAM_H[1], FxFrames.FOAM_H[0] };
	static final int[][] FOAM_V = { FxFrames.FOAM_V[0], FxFrames.FOAM_V[1], FxFrames.FOAM_V[2], FxFrames.FOAM_V[1], FxFrames.FOAM_V[0] };
	static final float FOAM_LIFE = 1.6f, FOAM_ALPHA = 0.55f, LAP_MIN = 3f, LAP_MAX = 7f;
	static final float WHITECAP_WIND = 6f, WHITECAP_LIFE = 0.5f;
	//the swamp's bubbles: per open cell a second (three per sixteen cells every ten seconds)
	static final float METHANE = 3f / 16f / 10f;

	//the water seen in view: its cells, its open cells, its lapping cells and their timers
	private final int[] cells = new int[CELLS], open = new int[CELLS];
	private int nCells, nOpen, nWater;
	private final int[] laps = new int[LAPS], lapsWas = new int[LAPS];
	private final float[] lapIn = new float[LAPS], lapInWas = new float[LAPS];
	private int nLaps;
	private float lookIn, glints, caps, bubbles;
	private int lookedFrom = -1;

	//the lava's life (crust, bubbles, heat, the banks' light, spits)
	private final WaterLava lava = new WaterLava();

	void update( Level level, Element.Liquid l, float dt, int x0, int y0, int x1, int y1 ){
		if (Dungeon.hero == null || level.heroFOV == null) return;
		lookIn -= dt;
		if (lookIn <= 0f || Dungeon.hero.pos != lookedFrom){
			lookIn = LOOK;
			lookedFrom = Dungeon.hero.pos;
			look( level, x0, y0, x1, y1 );
			if (l == Element.Liquid.LAVA) lava.banks( level, x0, y0, x1, y1 );
			else lava.dark();
		}
		if (dt <= 0f || nCells == 0) return;
		glints( l, dt );
		if (l == Element.Liquid.LAVA){
			lava.update( level, cells, nCells, dt );
			return;
		}
		foam( level, l, dt );
		if (l == Element.Liquid.FROZEN) cold( l, dt );
		if (level instanceof OverworldLevel) sea( (OverworldLevel) level, l, dt );
	}

	/** The lava's banks lit now (tests). */
	int banksLit(){
		return lava.lit();
	}

	// ------------------------------------------------------------------ the cold water

	/** The frozen places' water: slush (white motes at 0.5 riding its drift, one for every three
	 *  open cells, 3-5 s, at most 16) and at night a mist lying on it (EDGE_WISP in C8DCF0 at 0.12,
	 *  4-6 s, drifting 2 px/s, a new one every two seconds per sixteen cells, at most 6). */
	public static final float SLUSH_A_CELL = 1f / 3f, SLUSH_ALPHA = 0.5f, MIST_ALPHA = 0.12f;
	public static final int MIST = 0xC8DCF0;
	private float slushIn, mists;

	private void cold( Element.Liquid l, float dt ){
		if (nOpen == 0) return;
		int want = Math.min( Marks.CAPS[Marks.SLUSH], Math.round( nOpen * SLUSH_A_CELL ) );
		if (Marks.live( Marks.SLUSH ) < want && (slushIn -= dt) <= 0f){
			slushIn = 0.25f;
			int c = open[FxRandom.Int( nOpen )];
			float x = cellX( c ) + FxRandom.IntRange( 1, 14 ), y = cellY( c ) + FxRandom.IntRange( 1, 8 );
			if (FxBudget.keep( FxBudget.P3, x, y )){
				Marks.Mark m = Marks.make( Marks.SLUSH, false, x, y );
				float life = FxRandom.Float( 3f, 5f );
				if (m != null) m.look( FxFrames.MOTE_1 ).paint( 0xFFFFFF ).life( life ).alpha( SLUSH_ALPHA, 0.5f, 0.6f )
						.smooth().vel( FxRandom.Float( -0.5f, 0.5f ), l.scroll * 0.6f ).snap().priority( FxBudget.P3 );
			}
		}
		if (WaterGeom.night( GameScene.nightTintAlpha() ) <= 0.5f) return;
		mists += nOpen / 16f / 2f * dt;
		while (mists >= 1f){
			mists -= 1f;
			if (!Marks.room( Marks.MIST )) continue;
			int c = open[FxRandom.Int( nOpen )];
			float x = cellX( c ) + FxRandom.IntRange( 3, 12 ), y = cellY( c ) + FxRandom.IntRange( 4, 12 );
			if (!FxBudget.keep( FxBudget.P3, x, y )) continue;
			Marks.Mark m = Marks.make( Marks.MIST, false, x, y );
			float life = FxRandom.Float( 4f, 6f );
			if (m != null) m.look( FxFrames.EDGE_WISP, 0 ).paint( MIST ).life( life ).alpha( MIST_ALPHA, 1.5f, 2f )
					.smooth().vel( FxRandom.sign() * 2f, 0f ).priority( FxBudget.P3 );
		}
	}

	/** The water in view that the hero sees, looked over afresh; the lapping cells keep their timers. */
	private void look( Level level, int x0, int y0, int x1, int y1 ){
		int w = level.width(), h = level.height();
		x0 = Math.max( 1, x0 );
		y0 = Math.max( 1, y0 );
		x1 = Math.min( w - 2, x1 );
		y1 = Math.min( h - 2, y1 );
		boolean[] water = level.water, fov = level.heroFOV;
		int[] map = level.map;
		int was = nLaps;
		System.arraycopy( laps, 0, lapsWas, 0, was );
		System.arraycopy( lapIn, 0, lapInWas, 0, was );
		nCells = nOpen = nWater = nLaps = 0;
		for (int y = y0; y <= y1; y++){
			for (int x = x0; x <= x1; x++){
				int c = x + y * w;
				if (!water[c] || !fov[c]) continue;
				nWater++;
				if (nCells < CELLS) cells[nCells++] = c;
				if (water[c - 1] && water[c + 1] && water[c - w] && water[c + w]){
					if (nOpen < CELLS) open[nOpen++] = c;
				} else if (nLaps < LAPS && Source.lapping( map, w, c )){
					laps[nLaps] = c;
					float t = -1f;
					for (int i = 0; i < was; i++) if (lapsWas[i] == c){ t = lapInWas[i]; break; }
					lapIn[nLaps++] = t >= 0f ? t : FxRandom.Float( 0f, LAP_MAX );
				}
			}
		}
		seen = nWater;
	}

	/** The water cells in view the hero saw at the last look (the rain's rings go by it). */
	static volatile int seen;

	// ------------------------------------------------------------------ glints

	/** The share of glints a liquid shows: the city's oil 0.3, the caves' 0.4, the lava none. */
	static float glintShare( Element.Liquid l ){
		switch (l){
			case CITY: return 0.3f;
			case CAVE: return 0.4f;
			case LAVA: return 0f;
			default:   return 1f;
		}
	}

	private void glints( Element.Liquid l, float dt ){
		if (nOpen == 0) return;
		float rain = rain();
		float tint = GameScene.nightTintAlpha();
		boolean sky = Fx.openSky();
		float rate = WaterGeom.glintRate( nWater, sky, ClimateManager.cloudCover(), tint, rain, glintShare( l ) )
				* FxBudget.glints();
		glints += rate * dt;
		int sun = sky ? sunSide() : 0;
		while (glints >= 1f){
			glints -= 1f;
			if (!Marks.room( Marks.GLINT )) continue;
			int c = open[FxRandom.Int( nOpen )];
			if (sun != 0 && FxRandom.chance( 0.75f )){
				//the one of three nearest the sun's side
				for (int k = 0; k < 2; k++){
					int o = open[FxRandom.Int( nOpen )];
					if ((o % width() - c % width()) * sun > 0) c = o;
				}
			}
			float x = cellX( c ) + FxRandom.IntRange( 2, 13 ), y = cellY( c ) + FxRandom.IntRange( 3, 13 );
			if (!FxBudget.keep( FxBudget.P3, x, y )) continue;
			Marks.Mark m = Marks.make( Marks.GLINT, true, x, y );
			if (m == null) continue;
			m.look( GLINT, 0 ).paint( Ramp.lerp( l.glint, WaterLive.MOON, WaterGeom.night( tint ) ) )
					.life( GLINT_LIFE ).alpha( WaterGeom.glintAlpha( tint ), 0, 0.05f ).snap().priority( FxBudget.P3 );
		}
	}

	//the side of the water the sun is on: east (+1) from dawn to noon, west (-1) after
	private static int sunSide(){
		DayNightCycle.Phase p = DayNightCycle.phase();
		if (p == DayNightCycle.Phase.NIGHT) return 0;
		if (p == DayNightCycle.Phase.DAWN) return 1;
		if (p == DayNightCycle.Phase.DUSK) return -1;
		return DayNightCycle.phaseProgress() < 0.5f ? 1 : -1;
	}

	/** The rain (or sleet) falling on the level's water now, 0..1. */
	public static float rain(){
		PrecipType t = ClimateManager.localPrecipType();
		return t == PrecipType.RAIN || t == PrecipType.SLEET ? ClimateManager.localPrecipRate() : 0f;
	}

	// ------------------------------------------------------------------ lap foam

	private void foam( Level level, Element.Liquid l, float dt ){
		for (int i = 0; i < nLaps; i++){
			if ((lapIn[i] -= dt) > 0f) continue;
			int c = laps[i];
			lapIn[i] = FxRandom.Float( LAP_MIN, LAP_MAX ) * (beach( level, c ) ? 0.5f : 1f);
			lapAt( level, l, c );
		}
	}

	private static boolean beach( Level level, int cell ){
		return level instanceof OverworldLevel
				&& ((OverworldLevel) level).biomeAtCell( cell ) == WorldModel.Biome.BEACH;
	}

	/** The water at a lapping cell laps its banks now: foam along each of its dry sides. */
	void lapAt( Level level, Element.Liquid l, int c ){
		int w = level.width();
		if (c < w || c >= level.length() - w) return;
		boolean[] water = level.water;
		float x = cellX( c ), y = cellY( c );
		if (!water[c - w]) laid( x + 8, y + 2, FOAM_H, l );
		if (!water[c + w]) laid( x + 8, y + SIZE - 2, FOAM_H, l );
		if (!water[c - 1]) laid( x + 2, y + 8, FOAM_V, l );
		if (!water[c + 1]) laid( x + SIZE - 2, y + 8, FOAM_V, l );
	}

	private static void laid( float x, float y, int[][] frames, Element.Liquid l ){
		Marks.Mark m = Marks.make( Marks.FOAM, false, x, y );
		if (m == null) return;
		m.look( frames, 0 ).paint( l.foam ).smooth().life( FOAM_LIFE ).alpha( FOAM_ALPHA, FOAM_LIFE / 2f, FOAM_LIFE / 2f )
				.snap().priority( FxBudget.P3 );
	}

	// ------------------------------------------------------------------ the sea and the swamp

	private void sea( OverworldLevel level, Element.Liquid l, float dt ){
		if (nOpen == 0) return;
		boolean windy = ClimateManager.localWindSpeed() >= WHITECAP_WIND;
		caps += windy ? 1.5f * nOpen / 16f * dt : 0f;
		bubbles += METHANE * nOpen * dt;
		while (caps >= 1f){
			caps -= 1f;
			int c = open[FxRandom.Int( nOpen )];
			if (!Marks.room( Marks.WHITECAP ) || level.biomeAtCell( c ) != WorldModel.Biome.OCEAN) continue;
			float x = cellX( c ) + FxRandom.IntRange( 3, 12 ), y = cellY( c ) + FxRandom.IntRange( 4, 12 );
			if (!FxBudget.keep( FxBudget.P3, x, y )) continue;
			Marks.Mark m = Marks.make( Marks.WHITECAP, false, x, y );
			if (m != null) m.look( FxFrames.WHITECAP, 0 ).paint( l.foam ).life( WHITECAP_LIFE ).alpha( 0.75f, 0.06f, 0.15f )
					.vel( Fx.windX() * 0.15f, 0 ).snap().priority( FxBudget.P3 );
		}
		while (bubbles >= 1f){
			bubbles -= 1f;
			int c = open[FxRandom.Int( nOpen )];
			if (level.biomeAtCell( c ) != WorldModel.Biome.SWAMP) continue;
			bubble( cellX( c ) + FxRandom.IntRange( 3, 12 ), cellY( c ) + FxRandom.IntRange( 5, 12 ), l.foam );
		}
	}

	/** A bubble rising 3 px through the surface and popping into a small ring (the swamp's gas, a
	 *  fish's kiss, a drain's gurgle). */
	public static void bubble( float x, float y, int color ){
		if (!FxBudget.keep( FxBudget.P3, x, y )) return;
		Marks.Mark m = Marks.make( Marks.BUBBLE, false, x, y );
		if (m == null) return;
		m.look( FxFrames.BUBBLE_2 ).paint( color ).life( 0.6f ).alpha( 0.85f, 0.1f, 0f ).vel( 0, -5f ).snap()
				.fate( POP ).priority( FxBudget.P3 );
	}

	//a bubble's end: a pop and a small ring where it broke
	static final FxParticle.Fate POP = ( p, landed ) -> {
		Marks.Mark pop = Marks.make( Marks.BUBBLE, false, p.px, p.py );
		if (pop != null) pop.look( FxFrames.POP_5 ).paint( p.colorNow() ).life( 0.12f ).alpha( 0.8f, 0, 0.06f ).snap()
				.priority( FxBudget.P3 );
		WaterFX.quietRing( p.px, p.py + 1, WaterFX.S, WaterFX.liquid().foam, 0.5f, FxBudget.P3 );
	};

	// ------------------------------------------------------------------ gnats

	/** A gnat over the water dips to it this often, s, from its cloud; a fish rises to one dip in
	 *  this many. */
	public static final float DIP_MIN = 2f, DIP_MAX = 4f;
	public static final int RISE_ONE_IN = 8;
	static final float DIP_DROP = 5f, DIP_TIME = 0.2f;
	static final int GNAT = 0x2A2620;

	/** A gnat of a cloud over the water dips to its surface at (x, y): down 5 px in 0.2 s, a small
	 *  ring where it touches, and up again - or, one time in eight, a fish rises to it (a middling
	 *  ring) and the gnat is gone. The rings stir nothing. Render thread. */
	public static void dip( float x, float y ){
		if (!WaterFX.onWater( x, y ) || !FxBudget.keep( FxBudget.P3, x, y )) return;
		FxEmitter air = Fx.air();
		if (air == null) return;
		((FxParticle) air.recycle( FxParticle.class )).reset( x, y - DIP_DROP ).look( FxFrames.MOTE_1 ).paint( GNAT )
				.vel( 0, DIP_DROP / DIP_TIME ).ground( y, FxParticle.DIE, 0 ).fate( DIPPED ).life( DIP_TIME + 0.1f )
				.alpha( 0.9f, 0, 0f ).snap().priority( FxBudget.P3 );
	}

	//its touch on the water: a ring, then up again, or a fish's rise and gone
	static final FxParticle.Fate DIPPED = ( p, landed ) -> {
		if (!landed) return;
		Element.Liquid l = WaterFX.liquid();
		if (FxRandom.Int( RISE_ONE_IN ) == 0){
			WaterFX.quietRing( p.px, p.py, WaterFX.M, l.foam, WaterFX.alphaOf( WaterFX.M ) * 0.8f, FxBudget.P3 );
			return;
		}
		WaterFX.quietRing( p.px, p.py, WaterFX.S, l.foam, 0.5f, FxBudget.P3 );
		FxEmitter air = Fx.air();
		if (air == null) return;
		((FxParticle) air.recycle( FxParticle.class )).reset( p.px, p.py ).look( FxFrames.MOTE_1 ).paint( GNAT )
				.vel( 0, -DIP_DROP / DIP_TIME ).life( DIP_TIME ).alpha( 0.9f, 0, 0.1f ).snap().priority( FxBudget.P3 );
	};

	// ------------------------------------------------------------------ cells

	private static int width(){
		return Dungeon.level.width();
	}

	private static float cellX( int c ){
		return (c % width()) * SIZE;
	}

	private static float cellY( int c ){
		return (c / width()) * SIZE;
	}
}
