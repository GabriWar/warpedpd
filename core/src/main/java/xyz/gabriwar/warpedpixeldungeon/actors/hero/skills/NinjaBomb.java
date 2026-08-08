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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicalSleep;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

public class NinjaBomb extends ActiveSkill2 {

	{
		name = "Ninja Bomb";
		castText = "Go to sleep";
		tier = 2;
		image = 65;
		mana = 8;
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
		if (action.equals(Skill.AC_CAST) && hero.MP >= getManaCost()){
			GameScene.selectCell( thrower );
			Dungeon.hero.heroSkills.lastUsed = this;
		}
	}

	private static CellSelector.Listener thrower = new CellSelector.Listener() {
		@Override
		public void onSelect( Integer target ){
			if (target != null){
				Hero curUser = Dungeon.hero;
				Skill skill = curUser.heroSkills.active2;
				Ballistica shot = new Ballistica( curUser.pos, target, Ballistica.PROJECTILE );
				int cell = shot.collisionPos;
				curUser.sprite.zap(cell);
				curUser.MP -= skill.getManaCost();
				skill.castTextYell();

				//a burst of sleeping gas around the impact point
				CellEmitter.get( cell ).burst( Speck.factory( Speck.STEAM ), 10 );
				for (int n : PathFinder.NEIGHBOURS9){
					int c = cell + n;
					if (c < 0 || c >= Dungeon.level.length()) continue;
					Char ch = Actor.findChar( c );
					if (ch != null && ch != curUser && ch instanceof Mob){
						Buff.affect( ch, MagicalSleep.class );
					}
					if (Dungeon.level.heroFOV[c]){
						CellEmitter.get( c ).burst( Speck.factory( Speck.STEAM ), 3 );
					}
				}
				curUser.spendAndNext( TIME_TO_USE );
				curUser.busy();
			}
		}

		@Override
		public String prompt(){
			return "Choose where to throw the bomb";
		}
	};

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){
		return true;
	}
}
