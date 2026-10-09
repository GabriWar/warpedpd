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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.Random;

public class RimeAffinity extends Skill {

	//its damage is already a share of a blow, a hit or a health pool, so it grows with the hero on its own
	@Override
	public boolean weaponScaled(){ return true; }


	{
		tag = "D4B";
		name = "Cryomancy";
		tier = 4;
		image = 37;
		level = 0;
	}

	//the target was frozen solid before the zap landed (the zap's own damage thaws it)
	private Char frozenBefore = null;

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public void beforeMagicHit( Char target, Object source ){
		frozenBefore = target != null && target.buff( Frost.class ) != null ? target : null;
	}

	//wand zaps and bolt spells that hurt an enemy can freeze it solid; at mastery a zap on a frozen
	//enemy shatters the ice for half the zap again
	@Override
	public void onMagicDamage( Char target, int damage, Object source ){
		boolean shatter = level >= MAX_LEVEL && target != null && target == frozenBefore;
		frozenBefore = null;
		if (level <= 0 || target == null) return;
		if (shatter){
			if (target.isAlive()) target.damage( Math.max( 1, damage / 2 ), this );
			//the ice goes to pieces: splinters flung out, a white flash, a jolt
			xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter.get( target.pos ).burst( SnowParticle.FACTORY, 10 );
			xyz.gabriwar.warpedpixeldungeon.effects.Splash.at( target.pos, 0xCCEEFF, 10 );
			if (target.sprite != null) target.sprite.flash();
			com.watabou.noosa.Camera.main.shake( 1, 0.15f );
			SpatialSound.play( Assets.Sounds.SHATTER, target, 1f, 1.1f );
			return;
		}
		if (!target.isAlive() || Random.Int(100) >= 10 * level) return;
		SkillInteractions.affectAfterHit( target, Frost.class, 2f );
		if (target.sprite != null){
			//crystals grow over it: snow off the body and a pale blue flare as it seizes
			target.sprite.emitter().burst( SnowParticle.FACTORY, 3 + level );
			new xyz.gabriwar.warpedpixeldungeon.effects.Flare( 6, 12 ).color( 0xA4E9FF, true ).show( target.sprite, 0.5f );
		}
		SpatialSound.play( Assets.Sounds.SHATTER, target, 0.5f, 1.4f );
	}

	@Override
	public String info(){
		return Messages.get(this, "desc", 10 * Math.max(1, level)) + "\n"
				+ costUpgradeInfo();
	}
}
