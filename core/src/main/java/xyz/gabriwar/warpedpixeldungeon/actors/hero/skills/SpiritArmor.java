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
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SpiritArmorMotes;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAura;

import java.util.ArrayList;

/**
 * Mage: while active, part of every blow is paid with mana, and the mana it eats condenses
 * into motes that circle the hero. A full ring fires itself at the nearest enemies.
 */
public class SpiritArmor extends PassiveSkillA3 {

	@Override
	public boolean toggleable(){ return true; }

	{
		name = "Spirit Armor";
		tier = 3;
		image = 27;
		level = 0;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (!active && level > 0)
			actions.add(AC_ACTIVATE);
		else if (level > 0)
			actions.add(AC_DEACTIVATE);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_ACTIVATE)){
			active = true;
            xyz.gabriwar.warpedpixeldungeon.effects.SkillCastFX.play(this,hero);
			//one mana ward at a time
			Skill other = hero.heroSkills.get( Transcendence.class );
			if (other != null) other.active = false;
			if (hero.sprite != null){
				Sample.INSTANCE.play( Assets.Sounds.MELD, 1f, 1.3f );
				hero.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 6 );
				new Flare( 6, 18 ).color( 0x66CCFF, true ).show( hero.sprite, 0.5f );
			}
		} else if (action.equals(Skill.AC_DEACTIVATE)){
			active = false;
			//the ring scatters when the ward drops
			Buff.detach( hero, SpiritArmorMotes.class );
			Sample.INSTANCE.play( Assets.Sounds.MELD, 0.5f, 0.8f );
			if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 3 );
		}
		//the ward hums: a faint blue mote now and then while it is up
		StanceAura.sync( hero );
	}

	@Override
	public int incomingDamageReduction(int damage, Object source){
		if (!active || level <= 0 || Skill.isTickDamage( source ))
			return 0;
		Hero hero = Dungeon.hero;
		StanceAura.sync( hero );
		int absorbed = Math.min( Math.max( 1, (int)(damage * 0.1f * level) ), hero.MP );
		if (absorbed <= 0)
			return 0;
		hero.MP -= absorbed;
		if (hero.sprite != null){
			hero.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 1 + absorbed / 2 );
			if (absorbed >= 3) Sample.INSTANCE.play( Assets.Sounds.HIT_MAGIC, 0.5f, 1.3f );
		}
		Buff.affect( hero, SpiritArmorMotes.class ).absorb( absorbed, level );
		return absorbed;
	}
}
