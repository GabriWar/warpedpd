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


import com.watabou.noosa.Camera;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class FrostNova extends Skill {

	private static final int RADIUS = 3;

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

			ArrayList<Integer> frozenGround = new ArrayList<>();
			for (int c : SkillInteractions.area( hero.pos, RADIUS )) if (!Dungeon.level.pit[c]) frozenGround.add( c );
			//at mastery the ring leaves the ground bristling with ice spikes
			if (level >= MAX_LEVEL && !frozenGround.isEmpty())
				SkillField.place( hero, SkillField.ICE, level, 4, frozenGround );

			ArrayList<Mob> caught = new ArrayList<>();
			for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
				if (mob.alignment == Char.Alignment.ENEMY
						&& frozenGround.contains(mob.pos))
					caught.add( mob );
			}

			CellEmitter.center( hero.pos ).burst( SnowParticle.FACTORY, 12 );
			new Flare( 6, 32 ).color( 0x88DDFF, true ).show( hero.sprite, 0.8f );
			//the ring is seen over the whole floor it covers
			for (int c : frozenGround){
				if (Dungeon.level.heroFOV[c] && c != hero.pos)
					CellEmitter.get( c ).burst( SnowParticle.FACTORY, 2 );
			}
			Sample.INSTANCE.play( Assets.Sounds.SHATTER );
			Camera.main.shake( 1, 0.3f );

			for (Mob mob : caught){
				CellEmitter.get( mob.pos ).burst( SnowParticle.FACTORY, 5 );
				mob.damage( Random.NormalIntRange( 2 + level, 4 + 3 * level ), this );
				if (mob.sprite != null) mob.sprite.flash();
				if (mob.isAlive())
					Buff.prolong( mob, Chill.class, 3 + 2 * level );
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
