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
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;

/**
 * Tengu's shocker, thrown by the rogue: it sits where it landed and for three turns
 * arcs lightning across its cell, first along the diagonals, then the cross, and
 * back. Enemies on the four cells it arcs through are shocked; the rogue and allies
 * are not.
 */
public class TenguShockBomb extends Buff {

	/** Tengu's shocker with the rogue's own description: it is the hero who threw it */
	public static class RogueShockerItem extends Tengu.ShockerAbility.ShockerItem {}

	{
		type = buffType.NEUTRAL;
		announced = false;
	}

	private int cell = -1;
	private int depth = -1, branch = 0;
	private int turns = 3;
	private int level = 1;
	private boolean diagonals = true;

	public void set( int cell, int level ){
		this.cell = cell;
		this.level = level;
		this.depth = Dungeon.depth;
		this.branch = Dungeon.branch;
		this.turns = 3;
		this.diagonals = true;
		//the shocker sits where it landed, sparking, for as long as it arcs
		Dungeon.level.drop( new RogueShockerItem(), cell );
		if (Dungeon.level.heroFOV[cell]) CellEmitter.center( cell ).burst( SparkParticle.FACTORY, 8 );
		SpatialSound.play( Assets.Sounds.ZAP, cell, 1f, 0.8f );
		spend( TICK );
	}

	/** thrown on another floor: it went off, or fizzled, where it was left */
	private boolean offFloor(){
		return depth >= 0 && (depth != Dungeon.depth || branch != Dungeon.branch);
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
		int w = Dungeon.level.width();
		int[] cells = diagonals
				? new int[]{ cell - 1 - w, cell + 1 + w, cell - 1 + w, cell + 1 - w }
				: new int[]{ cell - w, cell + w, cell - 1, cell + 1 };
		if (Dungeon.level.heroFOV[cell] && target.sprite != null && target.sprite.parent != null){
			if (diagonals){
				target.sprite.parent.add( new Lightning( cell - 1 - w, cell + 1 + w, null ).noGlow() );
				target.sprite.parent.add( new Lightning( cell - 1 + w, cell + 1 - w, null ).noGlow() );
			} else {
				target.sprite.parent.add( new Lightning( cell - w, cell + w, null ).noGlow() );
				target.sprite.parent.add( new Lightning( cell - 1, cell + 1, null ).noGlow() );
			}
			CellEmitter.center( cell ).burst( SparkParticle.FACTORY, 4 );
			SpatialSound.play( Assets.Sounds.LIGHTNING, cell, 0.8f, Random.Float( 0.9f, 1.2f ) );
		}
		int lvl = target instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero
				? ((xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero) target).lvl : Dungeon.scalingDepth();
		for (int c : cells){
			if (c < 0 || c >= Dungeon.level.length()) continue;
			Char ch = Actor.findChar( c );
			if (ch == null || ch.alignment != Char.Alignment.ENEMY) continue;
			int dmg = Random.NormalIntRange( 3 + lvl / 4, 7 + lvl / 2 ) + level;
			if (Dungeon.level.water[c]) dmg = Math.round( dmg * 1.33f );
			ch.damage( dmg, this );
			if (ch.sprite != null) ch.sprite.flash();
			if (ch.isAlive() && level >= 3 && !ch.properties().contains( Char.Property.BOSS )) Buff.affect( ch, Paralysis.class, 1f );
		}
		diagonals = !diagonals;
		turns--;
		if (turns <= 0){
			clearItem();
			detach();
		} else {
			spend( TICK );
		}
		return true;
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

	private static final String CELL = "cell", TURNS = "turns", LEVEL = "level", DIAG = "diag";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( CELL, cell );
		bundle.put( "depth", depth );
		bundle.put( "branch", branch );
		bundle.put( TURNS, turns );
		bundle.put( LEVEL, level );
		bundle.put( DIAG, diagonals );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		cell = bundle.getInt( CELL );
		depth = bundle.contains( "depth" ) ? bundle.getInt( "depth" ) : -1;
		branch = bundle.getInt( "branch" );
		turns = bundle.getInt( TURNS );
		level = bundle.getInt( LEVEL );
		diagonals = bundle.getBoolean( DIAG );
	}
}
