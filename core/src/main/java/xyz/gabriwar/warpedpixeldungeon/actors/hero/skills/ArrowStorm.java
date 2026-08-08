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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

public class ArrowStorm extends Skill {

	{
		tag = "A4";
		name = "Arrow Storm";
		castText = "Rain of arrows!";
		image = 125;
		tier = 4;
		mana = 10;
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
			GameScene.selectCell( new Volley() );
			Dungeon.hero.heroSkills.lastUsed = this;
		}
	}

	private class Volley extends CellSelector.Listener {

		@Override
		public void onSelect( Integer target ){
			if (target == null) return;

			Hero curUser = Dungeon.hero;
			if (curUser.MP < getManaCost()) return;

			Ballistica shot = new Ballistica( curUser.pos, target, Ballistica.PROJECTILE );
			int cell = shot.collisionPos;
			curUser.sprite.zap( cell );
			curUser.MP -= getManaCost();
			castTextYell();

			for (int n : PathFinder.NEIGHBOURS9){
				int c = cell + n;
				if (c < 0 || c >= Dungeon.level.length()) continue;

				if (Dungeon.level.heroFOV[c]){
					CellEmitter.get( c ).burst( Speck.factory( Speck.STAR ), 3 );
				}

				Char ch = Actor.findChar( c );
				if (ch != null && ch != curUser && ch.alignment == Char.Alignment.ENEMY){
					ch.damage( Math.round( curUser.damageRoll() * (0.4f + 0.2f * level) ), curUser );
					if (ch.isAlive()){
						Buff.prolong( ch, Cripple.class, 2 + level );
					}
				}
			}

			Invisibility.dispel();
			curUser.spendAndNext( TIME_TO_USE );
		}

		@Override
		public String prompt(){
			return "Choose where to loose the volley";
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
}
