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
			xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAuraBuff.sync( hero, HolySmite.Radiance.class, false );
			//the wrath is kindled: a gout of holy flame and a low pulse of gold
			if (hero.sprite != null){
				hero.sprite.emitter().burst( xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle.FACTORY, 8 );
				xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX.around( hero.sprite, 0xFFB050, 12, 0.4f );
			}
			Sample.INSTANCE.play( Assets.Sounds.BURNING, 0.7f, 1.2f );
		} else if (action.equals( Skill.AC_DEACTIVATE )){
			if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.SMOKE ), 3 );
			Sample.INSTANCE.play( Assets.Sounds.DEGRADE, 0.5f, 1.2f );
		}
		xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAuraBuff.sync( hero, Embers.class, active && level > 0 );
	}

	/** the stance held: two ember motes round the cleric, and now and then a lick of flame at the feet */
	public static class Embers extends xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAuraBuff {
		@Override
		protected Class<? extends Skill> stance(){ return DivineWrath.class; }
		@Override
		protected xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAuraFX build(){
			return new xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAuraFX( target, 0xFFB050, 2,
					xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle.FACTORY, 1.1f );
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

		//the ground catches: flame under the blow, a hot flash on the enemy
		if (enemy.sprite != null){
			enemy.sprite.emitter().burst( Speck.factory( Speck.YELLOW_LIGHT ), 5 );
			enemy.sprite.flash();
			new xyz.gabriwar.warpedpixeldungeon.effects.Flare( 5, 12 ).color( 0xFFB050, true ).show( enemy.sprite, 0.3f );
		}
		for (int c : cells) xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter.floor( c ).burst( xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle.FACTORY, 2 );
		Sample.INSTANCE.play( Assets.Sounds.HIT_MAGIC, 0.8f, 1.3f );
		Sample.INSTANCE.play( Assets.Sounds.BURNING, 0.5f, 1.4f );

		return damage;
	}
}
