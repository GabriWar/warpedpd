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
 * Skill system ported from Skillful Pixel Dungeon by bilboldev (Moussa)
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

package xyz.gabriwar.warpedpixeldungeon.actors.hero.skills;


import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.Trap;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

public class BearTrap extends Skill {

	{
		tag = "D2";
		name = "Bear Trap";
		castText = "Mind your step.";
		image = 126;
		tier = 2;
		mana = 6;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && hero.MP >= getManaCost())
			actions.add(AC_CAST);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && hero.MP >= getManaCost()){
			GameScene.selectCell( new Placer() );
			Dungeon.hero.heroSkills.lastUsed = this;
		}
	}

	private class Placer extends CellSelector.Listener {

		@Override
		public void onSelect( Integer target ){
			if (target == null) return;

			Hero curUser = Dungeon.hero;
			if (curUser.MP < getManaCost()) return;

			Ballistica shot = new Ballistica( curUser.pos, target, Ballistica.PROJECTILE );
			int cell = shot.collisionPos;

			//plain ground only - Level.set would otherwise wipe out stairs, a door or a pedestal
			int terr = Dungeon.level.map[cell];
			if (!(terr == Terrain.EMPTY || terr == Terrain.GRASS ||
					terr == Terrain.EMBERS || terr == Terrain.EMPTY_SP ||
					terr == Terrain.HIGH_GRASS || terr == Terrain.FURROWED_GRASS
					|| terr == Terrain.EMPTY_DECO)){
				GLog.w( Messages.get(BearTrap.this, "no_ground") );
				return;
			}
			if (Dungeon.level.traps.get(cell) != null){
				GLog.w( Messages.get(BearTrap.this, "occupied") );
				return;
			}

			Level.set( cell, Terrain.TRAP );
			Dungeon.level.setTrap( new BearTrapHazard(level), cell ).reveal();
			GameScene.updateMap( cell );

			curUser.MP -= getManaCost();
			castTextYell();
			curUser.spend( TIME_TO_USE );
			curUser.busy();
			curUser.sprite.operate( curUser.pos );
		}

		@Override
		public String prompt(){
			return "Choose where to set the trap";
		}
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	public static class BearTrapHazard extends Trap {

		private static final String BUILT_AT = "builtAt";

		public int builtAt = 1;

		{
			color = GREY;
			shape = CROSSHAIR;
			disarmedByActivation = true;
			canBeHidden = false;
		}

		public BearTrapHazard(){
		}

		public BearTrapHazard( int builtAt ){
			this.builtAt = builtAt;
		}

		@Override
		public void activate(){
			Char c = Actor.findChar( pos );
			if (c != null && !c.flying){
				Buff.prolong( c, Roots.class, 3 + 2 * builtAt );
				Buff.affect( c, Bleeding.class ).set( 2 + 2 * builtAt );
				Buff.prolong( c, Cripple.class, 5f );
				Wound.hit( c );
			} else {
				Wound.hit( pos );
			}
		}

		@Override
		public String name(){
			return "bear trap";
		}

		@Override
		public String desc(){
			return "A set of steel jaws held open under tension. Anything heavy enough to trip the plate gets its leg caught and torn.";
		}

		@Override
		public void restoreFromBundle( Bundle bundle ){
			super.restoreFromBundle( bundle );
			builtAt = bundle.getInt( BUILT_AT );
		}

		@Override
		public void storeInBundle( Bundle bundle ){
			super.storeInBundle( bundle );
			bundle.put( BUILT_AT, builtAt );
		}
	}
}
