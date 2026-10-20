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
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.DivineWrathGround;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;

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
			//the wrath is kindled: holy motes rising off the cleric (never a flame on him: that is
			//Burning) and a low pulse of deep gold
			if (hero.sprite != null){
				hero.sprite.emitter().burst( SkillBursts.HOLY_MOTE, 8 );
				xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX.around( hero.sprite, WRATH, 12, 0.4f );
			}
			SpatialSound.play( Assets.Sounds.CHARGEUP, hero, 0.7f, 1.2f );
		} else if (action.equals( Skill.AC_DEACTIVATE )){
			if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.SMOKE ), 3 );
			SpatialSound.play( Assets.Sounds.DEGRADE, hero, 0.5f, 1.2f );
		}
		xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAuraBuff.sync( hero, Embers.class, active && level > 0 );
	}

	/** The wrath's gold: Element.HOLY's deep step, warmer than Holy Smite's radiance, never orange. */
	public static final int WRATH = 0xF2C050;

	/** the stance held: two deep gold motes round the cleric, and now and then a holy mote rising */
	public static class Embers extends xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAuraBuff {
		@Override
		protected Class<? extends Skill> stance(){ return DivineWrath.class; }
		@Override
		protected xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAuraFX build(){
			return new xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAuraFX( target, WRATH, SkillBursts.HOLY_MOTE, 1.1f );
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

		//the ground is hallowed: holy motes rising off it under the blow (never fire on the floor), a
		//star of gold on the enemy
		if (enemy.sprite != null){
			enemy.sprite.emitter().burst( Speck.factory( Speck.YELLOW_LIGHT ), 5 );
			enemy.sprite.flash();
			PixelFlare.show( enemy.sprite.x + enemy.sprite.width() / 2f, enemy.sprite.y + enemy.sprite.height() / 2f,
					0xF1D99C, PixelFlare.S, 0.3f );
		}
		for (int c : cells) xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter.floor( c ).burst( SkillBursts.HOLY_MOTE, 2 );
		SpatialSound.play( Assets.Sounds.HIT_MAGIC, enemy, 0.8f, 1.3f );
		SpatialSound.play( Assets.Sounds.RAY, enemy, 0.5f, 1.4f );

		return damage;
	}
}
