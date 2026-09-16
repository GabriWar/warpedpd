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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ParryRiposte;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

public class ParryStance extends PassiveSkillA3 {

	{
		name = "Parry Stance";
		image = 85;
		tier = 3;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//chance, in percent, that a blow from an adjacent attacker is caught on the blade: 10 / 15 / 20
	private int parryChance(){
		return 5 + 5 * level;
	}

	//a parried blow does nothing, and the blade comes straight back at the attacker (ParryRiposte)
	@Override
	public int onDefendProc( Char enemy, int damage ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || enemy == null || damage <= 0 || hero == null || !enemy.isAlive()
				|| !Dungeon.level.adjacent( enemy.pos, hero.pos ) || Random.Int( 100 ) >= parryChance()){
			return damage;
		}
		if (hero.sprite != null){
			hero.sprite.showStatus( CharSprite.NEUTRAL, Messages.get( this, "parry" ) );
			hero.sprite.emitter().burst( Speck.factory( Speck.STAR ), 6 );
			new Flare( 5, 16 ).color( 0xFFFFFF, true ).show( hero.sprite, 0.4f );
		}
		Sample.INSTANCE.play( Assets.Sounds.HIT_PARRY, 1f, 1f );
		ParryRiposte riposte = Buff.affect( hero, ParryRiposte.class );
		riposte.enemy = enemy;
		riposte.sweep = level >= MAX_LEVEL;
		return 0;
	}
}
