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

import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Tengu;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;

import java.util.ArrayList;

/**
 * A fire bomb the rogue threw: it sits where it landed and for five turns keeps
 * setting the ground around it ablaze, 1 tile out, 2 at mastery, where it also goes
 * off in the smoke bomb's blast at the end. Enemies in reach catch fire; the flames
 * themselves burn anyone who stands in them.
 */
public class TenguFireBomb extends Buff implements TenguToy {

	/** Tengu's bomb, spitting flame instead of smoke */
	public static class RogueFireBombItem extends Tengu.BombAbility.BombItem {
		@Override
		public boolean doPickUp( Hero hero, int pos ) {
			return TenguToy.pickUp( this, TenguFireBomb.class, hero, pos );
		}

		@Override
		public Emitter emitter() {
			Emitter emitter = new Emitter();
			emitter.pos( 7.5f, 3.5f );
			emitter.fillTarget = false;
			emitter.pour( FlameParticle.FACTORY, 0.08f );
			return emitter;
		}
	}

	{
		type = buffType.NEUTRAL;
		announced = false;
	}

	public static final int TURNS = 5;

	private int cell = -1;
	private int depth = -1, branch = 0;
	private int turns = TURNS;
	private int level = 2;

	public void set( int cell, int level ){
		this.cell = cell;
		this.level = level;
		this.depth = Dungeon.depth;
		this.branch = Dungeon.branch;
		this.turns = TURNS;
		//it counts down where the toy came to rest: a drop onto a cell that takes no heap lays it beside
		Heap landed = Dungeon.level.drop( new RogueFireBombItem(), cell );
		if (landed != null && landed.pos >= 0) this.cell = landed.pos;
		SpatialSound.play( Assets.Sounds.BURNING, cell, 1f, 0.9f );
		if (Dungeon.level.heroFOV[cell]) CellEmitter.center( cell ).burst( FlameParticle.FACTORY, 10 );
		ignite();
		spend( TICK );
	}

	/** thrown on another floor: it burnt out where it was left */
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
		if (offFloor() || cell < 0){
			detach();
			return true;
		}
		ignite();
		turns--;
		if (turns <= 0){
			clearItem();
			if (level >= Skill.MAX_LEVEL) TenguSmokeBomb.blast( this, cell, level );
			detach();
		} else {
			spend( TICK );
		}
		return true;
	}

	/** fire dropped on every open cell in reach, and enemies standing there set alight */
	private void ignite(){
		for (int c : fireCells( cell, level )){
			GameScene.add( Blob.seed( c, 4, Fire.class ) );
			if (Dungeon.level.heroFOV[c] && c != cell) CellEmitter.get( c ).burst( FlameParticle.FACTORY, 3 );
			Char ch = Actor.findChar( c );
			if (ch != null && ch.alignment == Char.Alignment.ENEMY){
				Buff.affect( ch, Burning.class ).reignite( ch );
			}
		}
	}

	/** the cells the bomb keeps ablaze: open cells within 1 tile, 2 at mastery */
	public static ArrayList<Integer> fireCells( int cell, int level ){
		int range = level >= Skill.MAX_LEVEL ? 2 : 1;
		PathFinder.buildDistanceMap( cell, BArray.not( Dungeon.level.solid, null ), range );
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c = 0; c < PathFinder.distance.length; c++){
			if (PathFinder.distance[c] <= range) cells.add( c );
		}
		return cells;
	}

	/** takes away this bomb's own item, and only one, should two lie on the same cell */
	private void clearItem(){
		Heap h = Dungeon.level.heaps.get( cell );
		if (h == null) return;
		for (Item i : h.items.toArray( new Item[0] )){
			if (i instanceof RogueFireBombItem){
				h.remove( i );
				break;
			}
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
		depth = bundle.getInt( "depth" );
		branch = bundle.getInt( "branch" );
		turns = bundle.getInt( TURNS_LEFT );
		level = bundle.getInt( LEVEL );
	}
}
