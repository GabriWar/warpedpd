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
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.audio.Sample;

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
			hero.sprite.showStatus( CharSprite.NEUTRAL, Messages.get( this, "shrug" ) );
			hero.sprite.emitter().burst( Speck.factory( Speck.FORGE ), 4 );
			new Flare( 4, 12 ).color( 0xD8D8D8, true ).show( hero.sprite, 0.3f );
		}
		Sample.INSTANCE.play( Assets.Sounds.STURDY, 1f, 1.2f );

		Char attacker = (Char) source;
		if (level >= 2 && attacker.isAlive() && Dungeon.level.adjacent( attacker.pos, hero.pos )){
			Buff.prolong( attacker, Daze.class, DAZE_TURNS );
			//+3: the rebound rings through its arm and it loses its next turn
			if (level >= MAX_LEVEL && !Char.hasProp( attacker, Char.Property.BOSS )
					&& attacker.buff( Paralysis.class ) == null){
				Buff.prolong( attacker, Paralysis.class, 1f );
			}
			if (attacker.sprite != null){
				attacker.sprite.showStatus( CharSprite.WARNING, Messages.get( this, "reel" ) );
				attacker.sprite.emitter().burst( Speck.factory( Speck.STAR ), 4 );
			}
		}
		return damage;
	}
}
