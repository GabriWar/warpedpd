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
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Necrotoxin extends Skill {

	{
		tag = "PB4";
		name = "Necrotoxin";
		castText = "Rot";
		image = 71;
		tier = 4;
	}

	@Override
	protected boolean upgrade(){ return true; }

	//a hit can rupture the poison in an enemy: what was left of it lands at once, and half again
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level <= 0 || enemy == null) return damage;
		Poison poison = enemy.buff( Poison.class );
		if (poison == null || Random.Int( 100 ) >= 5 + 15 * level) return damage;

		int burst = Math.round( poison.totalIncomingDMG() * 1.5f );
		poison.detach();

		castTextYell();
		CellEmitter.center( enemy.pos ).burst( PoisonParticle.SPLASH, 10 );
		CellEmitter.get( enemy.pos ).burst( Speck.factory( Speck.TOXIC ), 6 );
		if (enemy.sprite != null) new Flare( 6, 20 ).color( 0x66DD44, true ).show( enemy.sprite, 0.6f );
		Sample.INSTANCE.play( Assets.Sounds.GAS, 0.9f, 0.8f );

		//at mastery the rupture sprays onto every enemy next to it
		if (level >= MAX_LEVEL){
			for (int n : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar( enemy.pos + n );
				if (ch != null && ch.isAlive() && ch.alignment == Char.Alignment.ENEMY){
					Buff.affect( ch, Poison.class ).set( 5f );
					CellEmitter.center( ch.pos ).burst( PoisonParticle.SPLASH, 4 );
				}
			}
		}
		return damage + burst;
	}
}
