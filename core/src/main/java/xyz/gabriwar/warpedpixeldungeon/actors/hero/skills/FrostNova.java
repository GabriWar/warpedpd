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
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class FrostNova extends Skill {

	{
		tag = "D2";
		name = "Frost Nova";
		castText = "Be still.";
		tier = 2;
		image = 30;
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

			int cryo = cryomancyLevel();
			int radius = 3 + (cryo >= 2 ? 1 : 0);

			ArrayList<Mob> caught = new ArrayList<>();
			for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
				if (mob.alignment == Char.Alignment.ENEMY
						&& Dungeon.level.distance( hero.pos, mob.pos ) <= radius)
					caught.add( mob );
			}

			if (caught.isEmpty()){
				GLog.w( Messages.get(this, "no_targets") );
				return;
			}

			CellEmitter.center( hero.pos ).burst( SnowParticle.FACTORY, 10 );
			Sample.INSTANCE.play( Assets.Sounds.SHATTER );

			for (Mob mob : caught){
				CellEmitter.get( mob.pos ).burst( SnowParticle.FACTORY, 5 );
				mob.damage( Random.NormalIntRange( 2 + level, 4 + 3 * level ), this );
				if (mob.isAlive())
					Buff.prolong( mob, Chill.class, 3 + 2 * level + cryo );
			}

			hero.MP -= getManaCost();
			castTextYell();
			Dungeon.hero.heroSkills.lastUsed = this;
			hero.spend( TIME_TO_USE );
			hero.busy();
			hero.sprite.operate( hero.pos );
		}
	}

	private static int cryomancyLevel(){
		if (Dungeon.hero == null || Dungeon.hero.heroSkills == null)
			return 0;
		for (Skill s : Dungeon.hero.heroSkills.fourthSkills)
			if (s instanceof RimeAffinity)
				return s.level;
		return 0;
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
