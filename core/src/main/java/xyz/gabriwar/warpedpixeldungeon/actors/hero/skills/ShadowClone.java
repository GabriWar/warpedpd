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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummonedPet;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ElmoParticle;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.MirrorSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class ShadowClone extends ActiveSkill3 {

	{
		name = "Shadow Clone";
		castText = "Shadow clone";
		tier = 3;
		image = 67;
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
		if (action.equals(Skill.AC_CAST)){

			if (SummonedPet.summonedPets >= 3 + hero.heroSkills.allSummonLimit()){
				GLog.w( Messages.get(this, "too_many") );
				return;
			}

			ArrayList<Integer> respawnPoints = new ArrayList<>();
			for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++){
				int p = hero.pos + PathFinder.NEIGHBOURS8[i];
				if (p < 0 || p >= Dungeon.level.length())
					continue;
				if (Actor.findChar(p) == null && (Dungeon.level.passable[p] || Dungeon.level.avoid[p])){
					respawnPoints.add(p);
				}
			}
			int nImages = 1;
			while (nImages > 0 && respawnPoints.size() > 0){
				int index = Random.index(respawnPoints);
				SummonedPet minion = new SummonedPet(MirrorSprite.class);
				minion.name = "Shadow Clone";
				minion.HT = 10 + 8 * level;
				minion.HP = minion.HT;
				minion.setLevel(level);
				minion.pos = respawnPoints.get(index);
				//measured against the clone itself, so the reading never trips the
				//non-adjacent evasion hooks; the clone also inherits the stealth training
				minion.defenseSkill = (int)(Dungeon.hero.defenseSkill(minion) * ((1f + level) / 4f))
						+ CurrentSkills.skillLevel(Stealth.class);
				GameScene.add(minion);
				ScrollOfTeleportation.appear(minion, respawnPoints.get(index));
				minion.sprite.alpha(0);
				minion.sprite.parent.add(new AlphaTweener(minion.sprite, 1, 0.15f));
				CellEmitter.get(minion.pos).burst(ElmoParticle.FACTORY, 4);
				respawnPoints.remove(index);
				nImages--;
			}
			hero.MP -= getManaCost();
			castTextYell();
			Dungeon.hero.heroSkills.lastUsed = this;
			hero.spend( TIME_TO_USE );
			hero.busy();
			hero.sprite.operate( hero.pos );
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
