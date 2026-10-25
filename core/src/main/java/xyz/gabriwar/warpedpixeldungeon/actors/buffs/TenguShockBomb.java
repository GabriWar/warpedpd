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

package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Tengu;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;

import java.util.ArrayList;

/**
 * Tengu's shocker, thrown by the rogue: it sits where it landed and for six turns
 * arcs lightning in all eight directions at once, 2 tiles out, then goes off in the
 * smoke bomb's blast. Enemies the arcs pass through are shocked; the rogue and
 * allies are not.
 */
public class TenguShockBomb extends Buff implements TenguToy {

	/** Tengu's shocker with the rogue's own description: it is the hero who threw it */
	public static class RogueShockerItem extends Tengu.ShockerAbility.ShockerItem {
		@Override
		public boolean doPickUp( Hero hero, int pos ) {
			return TenguToy.pickUp( this, TenguShockBomb.class, hero, pos );
		}
	}

	{
		type = buffType.NEUTRAL;
		announced = false;
	}

	private int cell = -1;
	private int depth = -1, branch = 0;
	public static final int TURNS = 6, RANGE = 2;

	private int turns = TURNS;
	private int level = 1;

	public void set( int cell, int level ){
		this.cell = cell;
		this.level = level;
		this.depth = Dungeon.depth;
		this.branch = Dungeon.branch;
		this.turns = TURNS;
		//the shocker sits where it landed, sparking, for as long as it arcs
		//it counts down where the toy came to rest: a drop onto a cell that takes no heap lays it beside
		Heap landed = Dungeon.level.drop( new RogueShockerItem(), cell );
		if (landed != null && landed.pos >= 0) this.cell = landed.pos;
		if (Dungeon.level.heroFOV[cell]) CellEmitter.center( cell ).burst( SparkParticle.FACTORY, 8 );
		SpatialSound.play( Assets.Sounds.ZAP, cell, 1f, 0.8f );
		spend( TICK );
	}

	/** thrown on another floor: it went off, or fizzled, where it was left */
	private boolean offFloor(){
		return depth >= 0 && (depth != Dungeon.depth || branch != Dungeon.branch);
	}

	@Override
	public boolean ticksOn( int cell ){
		return cell == this.cell && !offFloor();
	}

	@Override
	public void slide( int dx, int dy, int width, int height ){
		if (cell < 0 || offFloor()) return;
		int x = cell % width - dx, y = cell / width - dy;
		if (x <= 0 || y <= 0 || x >= width - 1 || y >= height - 1){
			//scrolled out of the window: it fizzles where it lay, and TenguToy.sweep clears its toy
			detach();
		} else {
			cell = x + y * width;
		}
	}

	@Override
	public boolean act() {
		if (offFloor()){
			detach();
			return true;
		}
		if (cell < 0 || turns <= 0){
			if (cell >= 0) clearItem();
			detach();
			return true;
		}
		ArrayList<Integer> cells = arcCells( cell );
		if (Dungeon.level.heroFOV[cell] && target.sprite != null && target.sprite.parent != null){
			//one bolt across each of the four lines through the shocker, as far as the arcs reach
			for (int i = 0; i < 4; i++){
				int dir = PathFinder.NEIGHBOURS8[i];
				target.sprite.parent.add( new Lightning( arcEnd( cell, dir ), arcEnd( cell, -dir ), null ).noGlow() );
			}
			CellEmitter.center( cell ).burst( SparkParticle.FACTORY, 4 );
			SpatialSound.play( Assets.Sounds.LIGHTNING, cell, 0.8f, Random.Float( 0.9f, 1.2f ) );
		}
		int lvl = target instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero
				? ((xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero) target).lvl : Dungeon.scalingDepth();
		for (int c : cells){
			Char ch = Actor.findChar( c );
			if (ch == null || ch.alignment != Char.Alignment.ENEMY) continue;
			int dmg = Random.NormalIntRange( 3 + lvl / 4, 7 + lvl / 2 ) + level;
			if (Dungeon.level.water[c]) dmg = Math.round( dmg * 1.33f );
			ch.damage( dmg, this );
			if (ch.sprite != null) ch.sprite.flash();
			if (ch.isAlive() && level >= 3 && !ch.properties().contains( Char.Property.BOSS )) Buff.affect( ch, Paralysis.class, 1f );
		}
		turns--;
		if (turns <= 0){
			clearItem();
			TenguSmokeBomb.blast( this, cell, level );
			detach();
		} else {
			spend( TICK );
		}
		return true;
	}

	/** the cells the arcs pass through: all eight directions, up to RANGE tiles out,
	 *  each stopped by the first wall */
	public static ArrayList<Integer> arcCells( int cell ){
		ArrayList<Integer> cells = new ArrayList<>();
		for (int dir : PathFinder.NEIGHBOURS8){
			for (int step = 1; step <= RANGE; step++){
				int c = cell + dir * step;
				if (c < 0 || c >= Dungeon.level.length() || Dungeon.level.solid[c]) break;
				cells.add( c );
			}
		}
		return cells;
	}

	/** the last cell an arc reaches going `dir` from the shocker */
	private static int arcEnd( int cell, int dir ){
		int end = cell;
		for (int step = 1; step <= RANGE; step++){
			int c = cell + dir * step;
			if (c < 0 || c >= Dungeon.level.length() || Dungeon.level.solid[c]) break;
			end = c;
		}
		return end;
	}

	private void clearItem(){
		Heap h = Dungeon.level.heaps.get( cell );
		if (h != null){
			for (Item i : h.items.toArray( new Item[0] )){
				if (i instanceof Tengu.ShockerAbility.ShockerItem) h.remove( i );
			}
			if (Dungeon.level.heroFOV[cell]) CellEmitter.center( cell ).burst( SparkParticle.FACTORY, 6 );
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.NONE;
	}

	private static final String CELL = "cell", TURNS_LEFT = "turns", LEVEL = "level";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( CELL, cell );
		bundle.put( "depth", depth );
		bundle.put( "branch", branch );
		bundle.put( TURNS_LEFT, turns );
		bundle.put( LEVEL, level );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		cell = bundle.getInt( CELL );
		depth = bundle.contains( "depth" ) ? bundle.getInt( "depth" ) : -1;
		branch = bundle.getInt( "branch" );
		turns = bundle.getInt( TURNS_LEFT );
		level = bundle.getInt( LEVEL );
	}
}
