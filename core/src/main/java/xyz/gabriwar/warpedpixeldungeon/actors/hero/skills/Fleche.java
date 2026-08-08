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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

public class Fleche extends Skill {

	{
		tag = "D2";
		name = "Fleche";
		castText = "En garde!";
		image = 142;
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
	public void execute( final Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && hero.MP >= getManaCost()){
			GameScene.selectCell( new CellSelector.Listener(){
				@Override
				public void onSelect( Integer target ){
					if (target != null){
						charge( hero, target );
					}
				}

				@Override
				public String prompt(){
					return "Choose a target to close in on";
				}
			} );
		}
	}

	private void charge( Hero hero, int target ){
		//the selector stays live across other casts, so re-check before spending
		if (level <= 0 || hero.MP < getManaCost()) return;

		Char ch = Actor.findChar( target );
		if (ch == null || ch.alignment != Char.Alignment.ENEMY
				|| !Dungeon.level.heroFOV[target]
				|| Dungeon.level.distance( hero.pos, target ) > 3 + level){
			GLog.w( Messages.get(this, "no_target") );
			return;
		}

		int landing = hero.pos;
		if (!Dungeon.level.adjacent( hero.pos, ch.pos )){
			landing = -1;

			//first choice: the last cell of the straight line before the target
			Ballistica traj = new Ballistica( hero.pos, ch.pos, Ballistica.STOP_TARGET );
			if (traj.dist >= 1){
				int c = traj.path.get( traj.dist - 1 );
				if (Dungeon.level.passable[c] && Actor.findChar( c ) == null){
					landing = c;
				}
			}

			//otherwise any free cell beside the target, nearest to where you stand
			if (landing == -1){
				for (int n : PathFinder.NEIGHBOURS8){
					int c = ch.pos + n;
					if (c < 0 || c >= Dungeon.level.length()) continue;
					if (!Dungeon.level.passable[c] || Actor.findChar( c ) != null) continue;
					if (landing == -1
							|| Dungeon.level.distance( hero.pos, c ) < Dungeon.level.distance( hero.pos, landing )){
						landing = c;
					}
				}
			}

			if (landing == -1){
				GLog.w( Messages.get(this, "no_room") );
				return;
			}
		}

		int strikeCell = ch.pos;
		if (landing != hero.pos){
			ScrollOfTeleportation.appear( hero, landing );
			Dungeon.observe();
			GameScene.updateFog();
		}
		ch.damage( Math.round( hero.damageRoll() * (1f + 0.15f * level) ), this );

		hero.MP -= getManaCost();
		castTextYell();
		Dungeon.hero.heroSkills.lastUsed = this;
		hero.spend( TIME_TO_USE );
		hero.busy();
		hero.sprite.operate( strikeCell );
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
