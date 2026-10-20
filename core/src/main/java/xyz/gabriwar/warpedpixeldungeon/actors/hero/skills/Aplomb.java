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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Daze;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;

public class Aplomb extends Skill {

	private static final float DAZE_TURNS = 2f;

	{
		tag = "PA4";
		name = "Aplomb";
		image = 136;
		tier = 4;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//a blow from a creature that deals no more than 3% / 5% / 7% of max health bounces off whole.
	//Only creatures count: starvation, poison, bleeding and every other damage over time has
	//no attacker, so this can never cancel them
	@Override
	public int incomingDamageReduction( int damage, Object source ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || damage <= 0 || !(source instanceof Char) || source == hero){
			return 0;
		}
		int threshold = Math.max( level, Math.round( hero.HT * (0.01f + 0.02f * level) ) );
		if (damage > threshold){
			return 0;
		}
		if (hero.sprite != null){
			xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow.icon( hero, xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.RESISTED, 0 );
			chipOn( hero );
			hero.sprite.emitter().burst( SkillBursts.METAL_SPARK, 4 );
			PixelFlare.show( SkillVfx.midX( hero.sprite ), SkillVfx.midY( hero.sprite ), 0xB9DEEE, PixelFlare.S, 0.3f );
		}
		SpatialSound.play( Assets.Sounds.STURDY, hero, 1f, 1.2f );

		Char attacker = (Char) source;
		if (level >= 2 && attacker.isAlive() && Dungeon.level.adjacent( attacker.pos, hero.pos )){
			Buff.prolong( attacker, Daze.class, DAZE_TURNS );
			//+3: the rebound rings through its arm and it loses its next turn
			if (level >= MAX_LEVEL && !Char.hasProp( attacker, Char.Property.BOSS )
					&& attacker.buff( Paralysis.class ) == null){
				Buff.prolong( attacker, Paralysis.class, 1f );
			}
			if (attacker.sprite != null){
				chipOn( attacker );
				attacker.sprite.emitter().burst( SkillBursts.DAZE, 4 );
			}
		}
		return damage;
	}
}
