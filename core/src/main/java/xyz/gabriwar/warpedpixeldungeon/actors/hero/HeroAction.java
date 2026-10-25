/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.actors.hero;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;

public class HeroAction {
	
	public int dst;
	
	public static class Move extends HeroAction {
		public Move( int dst ) {
			this.dst = dst;
		}
	}
	
	public static class PickUp extends HeroAction {
		public PickUp( int dst ) {
			this.dst = dst;
		}
	}
	
	public static class OpenChest extends HeroAction {
		public OpenChest( int dst ) {
			this.dst = dst;
		}
	}
	
	public static class Buy extends HeroAction {
		public Buy( int dst ) {
			this.dst = dst;
		}
	}
	
	public static class Interact extends HeroAction {
		public Char ch;
		//the tap that asked it was held (CellSelector.heldTap, gone by the time the hero acts): a
		//horse of his shows its menu rather than taking him up (homestead/mounts/Mounts.tapped)
		public boolean held;
		public Interact( Char ch ) {
			this.ch = ch;
		}
	}
	
	public static class Unlock extends HeroAction {
		public Unlock( int door ) {
			this.dst = door;
		}
	}
	
	public static class LvlTransition extends HeroAction {
		public LvlTransition(int stairs ) {
			this.dst = stairs;
		}
	}

	public static class Mine extends HeroAction {
		//how many turns the blow takes: the pick's one, the mason's hammer's three on rock
		public final float turns;
		public Mine( int wall ) {
			this( wall, 1f );
		}
		public Mine( int wall, float turns ) {
			this.dst = wall;
			this.turns = turns;
		}
	}

	//a homestead's work on a cell: felling, quarrying, digging, reaping (homestead/gathering/Gathering)
	public static class Work extends HeroAction {
		public final xyz.gabriwar.warpedpixeldungeon.homestead.gathering.Gathering.Job job;
		public Work( int cell, xyz.gabriwar.warpedpixeldungeon.homestead.gathering.Gathering.Job job ) {
			this.dst = cell;
			this.job = job;
		}
	}
	
	public static class Alchemy extends HeroAction {
		public Alchemy( int pot ) {
			this.dst = pot;
		}
	}
	
	public static class Attack extends HeroAction {
		public Char target;
		public Attack( Char target ) {
			this.target = target;
		}
	}

	public static class InteractPet extends HeroAction {
		public Char pet;
		public InteractPet( Char pet ) {
			this.pet = pet;
		}
	}

	public static class CrouchToggle extends HeroAction {
	}
}
