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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

public class ImpalingThrust extends Skill {

	{
		tag = "A4";
		name = "Impaling Thrust";
		castText = "Thrust!";
		image = 143;
		tier = 4;
		mana = 9;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && hero.MP >= getManaCost())
			actions.add(AC_CAST);
		return actions;
	}

	@Override
	public void execute( final Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && hero.MP >= getManaCost()){
			GameScene.selectCell( new CellSelector.Listener(){
				@Override
				public void onSelect( Integer target ){
					if (target != null && target != hero.pos){
						thrust( hero, target );
					}
				}

				@Override
				public String prompt(){
					return "Choose a direction to thrust";
				}
			} );
		}
	}

	private void thrust( Hero hero, int target ){
		//the selector stays live across other casts, so re-check before spending
		if (level <= 0 || hero.MP < getManaCost()) return;

		Ballistica traj = new Ballistica( hero.pos, target, Ballistica.STOP_SOLID );
		int reach = Math.min( 2 + level, traj.dist );

		boolean hit = false;
		for (int cell : traj.subPath( 1, reach )){
			Char ch = Actor.findChar( cell );
			if (ch != null && ch.alignment == Char.Alignment.ENEMY){
				ch.damage( Math.round( hero.damageRoll() * (0.5f + 0.15f * level) ), this );
				if (ch.isAlive()){
					Buff.affect( ch, Bleeding.class ).set( 2 + level );
				}
				hit = true;
			}
		}

		if (!hit){
			GLog.w( Messages.get(this, "no_target") );
			return;
		}

		hero.MP -= getManaCost();
		castTextYell();
		Dungeon.hero.heroSkills.lastUsed = this;
		hero.spend( TIME_TO_USE );
		hero.busy();
		hero.sprite.operate( target );
		Invisibility.dispel();
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){
		return true;
	}
}
