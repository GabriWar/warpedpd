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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummonedPet;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BlastParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class SoulDetonation extends Skill {

	private static final int MAX_DETONATIONS = 3;

	{
		tag = "A4";
		name = "Soul Detonation";
		castText = "Serve me one last time!";
		tier = 4;
		image = 39;
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

			ArrayList<SummonedPet> pets = new ArrayList<>();
			for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
				if (mob instanceof SummonedPet && mob.alignment == Char.Alignment.ALLY){
					pets.add( (SummonedPet)mob );
					if (pets.size() >= MAX_DETONATIONS)
						break;
				}
			}

			if (pets.isEmpty()){
				GLog.w( Messages.get(this, "no_summons") );
				return;
			}

			for (SummonedPet pet : pets){
				int origin = pet.pos;
				CellEmitter.center( origin ).burst( BlastParticle.FACTORY, 6 );
				Sample.INSTANCE.play( Assets.Sounds.BLAST );

				for (int n : PathFinder.NEIGHBOURS9){
					int c = origin + n;
					if (c < 0 || c >= Dungeon.level.length())
						continue;
					Char ch = Actor.findChar( c );
					if (ch != null && ch.alignment == Char.Alignment.ENEMY)
						ch.damage( Random.NormalIntRange( 3 + 2 * level, 6 + 3 * level ), this );
				}

				pet.die( this );
			}

			if (level >= MAX_LEVEL){
				hero.HP = Math.min( hero.HT, hero.HP + 2 * pets.size() );
				hero.sprite.emitter().burst( Speck.factory( Speck.HEALING ), 2 * pets.size() );
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

	@Override
	public String info(){
		return Messages.get(this, "desc", 3 + 2 * Math.max(1, level), 6 + 3 * Math.max(1, level)) + "\n"
				+ costUpgradeInfo();
	}
}
