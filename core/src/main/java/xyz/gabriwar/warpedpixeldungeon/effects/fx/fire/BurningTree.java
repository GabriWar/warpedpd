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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.fire;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ChimneySmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;

/**
 * A tree on fire (FireField: each tree an open fire or an inferno burns, the CROWNS nearest the
 * hero), a burning roof's kin (BurningRoof): its crown alight from the moment it catches until it
 * burns down or goes out. Dense flames rise off the crown (FLAME: FFF4B0 at their core, FFB040, FF5A20
 * at their tips) as hot as its fire is - a tongue every FLAMES_HOT s while the volume burning on it
 * is HOT or more (a bolt's fire as it catches: StormStrikes), FLAMES s down to LOW, FLAMES_LOW s in
 * its last turns - with an ember rising out of them every EMBER_EVERY s; a column of the chimneys'
 * grey smoke climbs off the crown's top, leaning with the wind (TreeSmoke, a puff every SMOKE_EVERY
 * s, fewer as the budget thins). The flames show only while the hero sees the tree; the smoke, over
 * the fog, wherever he has seen the ground it rises from. One caught hot goes up at once in
 * IGNITION tongues, if he sees it.
 *
 * Where the crown is: an oak's hangs over the cell above its trunk where the world's dressing
 * draws it there (OverworldLevel.crownAbove), else it tops the trunk's own cell, as a pine's tops
 * its cell (a dungeon floor's trees are one cell). A storm's bolt strikes it there (struck:
 * WeatherOverlay draws it onto the crown, not onto the tree's foot).
 *
 * Visual only, its numbers the effects' own (FxRandom); everything rides the scene's slide of the
 * overworld's window. Render thread.
 */
public final class BurningTree {

	/** Its flames' colours: a tongue flows from the core to the tips over its life. */
	public static final Ramp FLAME = new Ramp( 0xFFF4B0, 0xFFB040, 0xFF5A20 ).keyed( 0, 0.4f, 1 );
	/** A tongue every this many s while its fire is HOT or more, over LOW, and LOW or less. */
	public static final float FLAMES_HOT = 0.04f, FLAMES = 0.06f, FLAMES_LOW = 0.1f;
	public static final int HOT = 6, LOW = 2;
	/** An ember every this many s; tongues going up at once as it catches hot. */
	public static final float EMBER_EVERY = 0.2f;
	public static final int IGNITION = 6;
	/** A puff of its smoke every this many s, and how far the budget may thin them (to a quarter). */
	public static final float SMOKE_EVERY = 0.18f, SMOKE_FLOOR = 0.25f;

	/** A burning tree's smoke: the chimneys' column, but a kind of its own, so the scene never
	 *  hands a tree a column SettlementAmbience still holds as it fades, nor the other way round. */
	public static final class TreeSmoke extends ChimneySmokeParticle.Column {}

	//its shape: an oak whose crown hangs over the cell above its trunk, an oak whose crown tops its
	//own cell, a pine (or a dungeon floor's tree)
	private static final int OAK_TALL = 0, OAK = 1, PINE = 2;
	//by shape, off its cell's top-left, px (overworld_dress.png): where its flames take root
	//{x, y, w, h}, the mouth its smoke climbs from {x, y, w, h}, and where a bolt strikes it {x, y}
	private static final int[][] FLAMES_AT = { { 2, -9, 12, 14 }, { 2, 1, 12, 6 }, { 4, 4, 8, 8 } };
	private static final int[][] SMOKE_AT = { { 5, -15, 6, 3 }, { 5, -1, 6, 3 }, { 6, 0, 4, 2 } };
	private static final int[][] STRIKE_AT = { { 8, -10 }, { 8, 2 }, { 8, 4 } };
	private static final float HALF = DungeonTilemap.SIZE / 2f;

	private final FxEmitter flames;
	private final Fuel fuel;
	private TreeSmoke smoke;
	private float every;

	private BurningTree( FxEmitter flames, Fuel fuel ){
		this.flames = flames;
		this.fuel = fuel;
	}

	/** A standing tree: an oak or a pine. */
	public static boolean tree( int terrain ){
		return terrain == Terrain.TREE_OAK || terrain == Terrain.TREE_PINE;
	}

	/** Where a bolt strikes the tree standing on a cell: a few px into its crown, into `at` (world
	 *  px); false, `at` as it was, where no tree stands. */
	public static boolean struck( Level level, int cell, PointF at ){
		if (level == null || cell < 0 || cell >= level.length() || !tree( level.map[cell] )) return false;
		int w = level.width();
		int[] s = STRIKE_AT[shape( level, cell )];
		at.set( (cell % w) * DungeonTilemap.SIZE + s[0], (cell / w) * DungeonTilemap.SIZE + s[1] );
		return true;
	}

	private static int shape( Level level, int cell ){
		if (level.map[cell] != Terrain.TREE_OAK) return PINE;
		return level instanceof OverworldLevel && ((OverworldLevel) level).crownAbove( cell ) ? OAK_TALL : OAK;
	}

	/** The tree on a cell set alight, the volume of its fire `cur`, seen by the hero or not; null off
	 *  the game's scene. */
	static BurningTree light( Level level, int cell, int cur, boolean seen ){
		FxEmitter flames = GameScene.lightEmitter();
		if (flames == null) return null;
		int shape = shape( level, cell ), w = level.width();
		float x = (cell % w) * DungeonTilemap.SIZE, y = (cell / w) * DungeonTilemap.SIZE;
		int[] f = FLAMES_AT[shape], m = SMOKE_AT[shape];
		flames.pos( x + f[0], y + f[1], f[2], f[3] );
		flames.autoKill = false;
		BurningTree t = new BurningTree( flames, new Fuel( HALF - f[0], HALF - f[1] ) );
		float puff = SMOKE_EVERY / Math.max( SMOKE_FLOOR, FxBudget.factor( FxBudget.P2 ) );
		t.smoke = ChimneySmokeParticle.kindle( TreeSmoke.class, x + m[0], y + m[1], m[2], m[3], puff, 0, 0, true );
		t.burn( cur, seen );
		if (seen && cur >= HOT) t.catches();
		return t;
	}

	/** The cell it burns on now, read off where its flames are (the window's slide moves them with
	 *  the world); -1 off the level. */
	int cell(){
		return Fx.cellAt( flames.x + fuel.treeDX, flames.y + fuel.treeDY );
	}

	/** Its tree's fire now: its volume, and whether the hero sees the tree. */
	void burn( int cur, boolean seen ){
		float want = cur >= HOT ? FLAMES_HOT : cur > LOW ? FLAMES : FLAMES_LOW;
		if (want != every){
			every = want;
			fuel.emberShare = every / EMBER_EVERY;
			flames.pour( fuel, every );
		}
		flames.on = seen;
	}

	//it catches: IGNITION middling and large tongues at once over the crown
	private void catches(){
		for (int i = 0; i < IGNITION; i++){
			float x = flames.x + FxRandom.Float( flames.width ), y = flames.y + FxRandom.Float( flames.height );
			if (!FxBudget.keep( FxBudget.P1, x, y )) continue;
			Flames.tongue( Flames.make( flames, FxParticle.class, x, y ), FLAME, FxRandom.chance( 0.5f ) ? Flames.M : Flames.L )
					.source( flames.x + fuel.treeDX, flames.y + fuel.treeDY );
		}
	}

	/** Its tree gone, or its fire out: no more flames (those alight die down as they do), and its
	 *  smoke thins away. */
	void out(){
		flames.on = false;
		flames.autoKill = true;
		if (smoke != null && smoke.parent != null) smoke.on = false;
		smoke = null;
	}

	//the crown's fire: a tongue a visit (S, M or L, 20/50/30) and now and then an ember rising out
	//of it, each hidden where the hero does not see the tree (its trunk's middle, off the emitter)
	private static final class Fuel extends Emitter.Factory {

		final float treeDX, treeDY;
		float emberShare;

		Fuel( float treeDX, float treeDY ){
			this.treeDX = treeDX;
			this.treeDY = treeDY;
		}

		@Override
		public void emit( Emitter e, int index, float x, float y ){
			float sx = e.x + treeDX, sy = e.y + treeDY;
			if (FxBudget.keep( FxBudget.P1, x, y )){
				float r = FxRandom.Float();
				Flames.tongue( Flames.make( e, FxParticle.class, x, y ), FLAME, r < 0.2f ? Flames.S : r < 0.7f ? Flames.M : Flames.L )
						.source( sx, sy );
			}
			if (FxRandom.Float() < emberShare && FxBudget.keep( FxBudget.P2, x, y )){
				Flames.ember( Flames.make( e, FxParticle.class, x, y - 3 ), Element.FIRE.ember ).source( sx, sy );
			}
		}

		@Override
		public boolean lightMode(){
			return true;
		}
	}
}
