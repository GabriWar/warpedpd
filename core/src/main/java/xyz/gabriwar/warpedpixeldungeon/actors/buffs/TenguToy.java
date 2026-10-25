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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.HashSet;

/**
 * One of the rogue's bombs from Tengu's Arsenal (TenguSmokeBomb, TenguFireBomb, TenguShockBomb),
 * counting down where its toy lies on the ground.
 */
public interface TenguToy {

	/** true while this bomb still counts down on `cell` of the floor the hero is on */
	boolean ticksOn( int cell );

	/** the overworld window slid by (dx, dy): the cell it counts down on follows its ground */
	void slide( int dx, int dy, int width, int height );

	/** is a bomb of this kind counting down on `pos`, whoever threw it */
	static boolean ticking( Class<? extends Buff> kind, Hero hero, int pos ){
		HashSet<Char> throwers = Actor.chars();
		if (hero != null) throwers.add( hero );
		for (Char ch : throwers){
			for (Buff b : ch.buffs( kind )){
				if (((TenguToy) b).ticksOn( pos )) return true;
			}
		}
		return false;
	}

	//the bomb a toy on the ground stands for, or null for any other item
	static Class<? extends Buff> kindOf( Item item ){
		if (item instanceof TenguSmokeBomb.RogueBombItem) return TenguSmokeBomb.class;
		if (item instanceof TenguFireBomb.RogueFireBombItem) return TenguFireBomb.class;
		if (item instanceof TenguShockBomb.RogueShockerItem) return TenguShockBomb.class;
		return null;
	}

	/**
	 * Clears every toy on the level no bomb counts down on any more: one left behind on a floor the
	 * rogue left mid-fuse, one whose window scrolled away, one saved before this rule. A toy goes
	 * off and is gone; none stays on the ground. On arriving on a floor, and after a window slide.
	 */
	static void sweep( Level level ){
		if (level == null || level.heaps == null) return;
		for (Heap h : level.heaps.valueList()){
			for (Item i : h.items.toArray( new Item[0] )){
				Class<? extends Buff> kind = kindOf( i );
				if (kind != null && !ticking( kind, Dungeon.hero, h.pos )) h.remove( i );
			}
		}
	}

	/**
	 * Picking up one of the rogue's toys: not while a bomb of its kind still counts down on its
	 * cell. Once none does (it went off, it burnt out when the rogue left the floor, the toy
	 * ended up off its cell) the spent toy comes apart in his hands and is gone. Before, such a
	 * toy lay there for good, and on a stair or a doorway it stood between him and the way on:
	 * nothing he cannot pick up is moved off one (Char.throwItems).
	 */
	static boolean pickUp( Item toy, Class<? extends Buff> kind, Hero hero, int pos ){
		if (ticking( kind, hero, pos )){
			GLog.w( Messages.get( toy, "cant_pickup" ) );
			return false;
		}
		GLog.i( Messages.get( TenguToy.class, "spent", toy.name() ) );
		hero.spendAndNext( toy.pickupDelay() );
		return true;
	}
}
