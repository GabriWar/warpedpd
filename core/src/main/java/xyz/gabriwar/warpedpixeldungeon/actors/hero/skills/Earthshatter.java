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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillSequence;


import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import com.watabou.noosa.Camera;

import java.util.ArrayList;

public class Earthshatter extends Skill {

	{
		tag = "A4";
		name = "Earthshatter";
		castText = "Break!";
		image = 7;
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
            xyz.gabriwar.warpedpixeldungeon.effects.WarriorImpactFX.show(hero.pos,true);
            SkillSequence.start(hero,SkillSequence.QUAKE,level,hero.pos,3+2*level,1+level,java.util.Collections.emptyList());
			for (Mob mob : Dungeon.level.mobs.toArray( new Mob[0] )){
				if (mob.alignment == Char.Alignment.ENEMY
						&& mob.isAlive()
						&& Dungeon.level.heroFOV[mob.pos]
						&& Dungeon.level.distance( hero.pos, mob.pos ) <= 2){
					CellEmitter.get( mob.pos ).burst( Speck.factory( Speck.ROCK ), 1 );
					Buff.prolong( mob, Roots.class, 2 + level );
                    xyz.gabriwar.warpedpixeldungeon.effects.WarriorImpactFX.show(mob.pos);
					mob.damage( 6 + 4 * level, this );
				}
			}
			for (int c = 0; c < Dungeon.level.length(); c++){
				if (Dungeon.level.distance( hero.pos, c ) <= 2 && Dungeon.level.heroFOV[c] && !Dungeon.level.solid[c] && c != hero.pos){
					CellEmitter.bottom( c ).burst( Speck.factory( Speck.DUST ), 1 );
				}
			}
			for (Mob mob : Dungeon.level.mobs.toArray( new Mob[0] )){
				if (mob.alignment == Char.Alignment.ENEMY && Dungeon.level.distance( hero.pos, mob.pos ) <= 2 && mob.sprite != null && mob.sprite.visible){
					Wound.hit( mob );
				}
			}
			Camera.main.shake( 3, 0.7f );
			hero.MP -= getManaCost();
			castTextYell();
			Sample.INSTANCE.play( Assets.Sounds.ROCKS, 1f, 0.9f );
			Dungeon.hero.heroSkills.lastUsed = this;
			hero.spend( TIME_TO_USE );
			hero.busy();
			hero.sprite.operate( hero.pos );
			Invisibility.dispel();
		}
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }
}
