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


import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.DivineWrathGround;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;

import java.util.ArrayList;
import java.util.List;

public class DivineWrath extends ActiveSkill {

	{
		name = "Divine Wrath";
		tag = "PB4";
		image = 153;
		tier = 4;
		mana = 3;
		level = 0;
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }

	//one holy stance at a time: raising Divine Wrath lowers Holy Smite
	@Override
	public void execute( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero, String action ){
		super.execute( hero, action );
		if (action.equals( Skill.AC_ACTIVATE )){
			Skill smite = hero.heroSkills.get( HolySmite.class );
			if (smite != null) smite.active = false;
		}
	}

	//each paid blow consecrates the ground: the target's tile, and from +2 every tile around it
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level <= 0 || !active || ranged || enemy == null)
			return damage;

		Hero hero = Dungeon.hero;
		if (hero == null || hero.MP < getManaCost())
			return damage;

		hero.MP -= getManaCost();

		List<Integer> cells;
		if (level >= 2){
			cells = SkillInteractions.area( enemy.pos, 1 );
		} else {
			cells = new ArrayList<>();
			cells.add( enemy.pos );
		}
		Buff.affect( hero, DivineWrathGround.class ).consecrate( cells, level );

		if (enemy.sprite != null) enemy.sprite.emitter().burst( Speck.factory( Speck.YELLOW_LIGHT ), 5 );
		Sample.INSTANCE.play( Assets.Sounds.HIT_MAGIC, 0.8f, 1.3f );
		Sample.INSTANCE.play( Assets.Sounds.BURNING, 0.5f, 1.4f );

		return damage;
	}
}
