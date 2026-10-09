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


import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;

import java.util.ArrayList;

public class Vanish extends SubSkill3 {

	{
		name = "Vanish";
		castText = "...";
		image = 183;
		mana = 4;
		tier = 3;
	}

	@Override
	public boolean toggleable(){ return false; }

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && hero.MP >= getManaCost())
			actions.add(AC_CAST);
		return actions;
	}

	//a step into shadow that costs no time: enemies hunting you lose the trail, and your next blow on
	//each of them is a surprise attack
	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && hero.MP >= getManaCost()){
			hero.MP -= getManaCost();
			castTextYell();
			SpatialSound.play( Assets.Sounds.MELD, hero, 1f, 1.1f );
			hero.sprite.emitter().burst( ShadowParticle.UP, 10 );
			loseTrail( hero );
			Buff.affect( hero, Invisibility.class, level >= Skill.MAX_LEVEL ? 3f : 1f );
			hero.heroSkills.lastUsed = this;
			hero.sprite.operate( hero.pos );
			hero.next();
		}
	}

	//3 / 5 tiles; at mastery every enemy in sight
	private void loseTrail( Hero hero ){
		int range = 1 + 2 * level;
		for (Mob mob : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (mob.alignment != Char.Alignment.ENEMY || mob.state != mob.HUNTING || !mob.isTargeting( hero )) continue;
			if (level >= Skill.MAX_LEVEL){
				if (hero.fieldOfView == null || mob.pos < 0 || mob.pos >= hero.fieldOfView.length || !hero.fieldOfView[mob.pos]) continue;
			} else if (Dungeon.level.distance( hero.pos, mob.pos ) > range) continue;
			mob.clearEnemy();
			mob.state = mob.WANDERING;
			if (mob.sprite != null) mob.sprite.emitter().burst( Speck.factory( Speck.QUESTION ), 1 );
		}
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }
}
