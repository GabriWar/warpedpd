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


import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import com.watabou.utils.Random;

public class FrostArrows extends ActiveSkill {

	{
		tag = "A5A";
		name = "Frost Arrows";
		castText = "Freeze.";
		image = 129;
		tier = 4;
		mana = 2;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute(hero, action);
		if (action.equals(Skill.AC_ACTIVATE)){
			Sample.INSTANCE.play( Assets.Sounds.DEGRADE, 1f, 1.6f );
			hero.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 3 );
			//mutually exclusive with its fork partner
			for (Skill s : hero.heroSkills.activeSkills){
				if (s instanceof EmberArrows) s.active = false;
			}
		}
	}

	@Override
	public int getManaCost(){
		//one mana per arrow, two once the tips are fully rimed
		return level >= 3 ? 2 : 1;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (!ranged || !active || level == 0 || enemy == null
				|| Dungeon.hero.MP < getManaCost())
			return damage;

		Dungeon.hero.MP -= getManaCost();

		//+2: a target that is already chilled takes the brunt of it
		if (level >= 2 && enemy.buff( Chill.class ) != null){
			damage = Math.round( damage * 1.15f );
		}

		boolean froze = level >= 3 && Random.Int(100) < 20;
		if (froze){
			SkillInteractions.affectAfterHit( enemy, Frost.class, 2f );
		} else {
			Buff.affect( enemy, Chill.class, 2 + level );
		}
		if (Dungeon.level.heroFOV[enemy.pos]){
			CellEmitter.get( enemy.pos ).burst( Speck.factory( Speck.BLUE_LIGHT ), froze ? 8 : 4 );
			Sample.INSTANCE.play( froze ? Assets.Sounds.SHATTER : Assets.Sounds.DEGRADE, 0.7f, froze ? 1.2f : 1.5f );
			if (froze && enemy.sprite != null) enemy.sprite.flash();
		}

		return damage;
	}
}
