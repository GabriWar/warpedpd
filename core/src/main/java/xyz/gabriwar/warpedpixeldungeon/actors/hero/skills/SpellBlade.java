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
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EnergyParticle;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMagic;


public class SpellBlade extends SubSkill1 {

	{
		name = "Spell Blade";
		image = 177;
		tier = 1;
	}

	//what a proc turns into instead of mana when the pool is already full
	private static final int OVERFLOW_DAMAGE = 4;

	@Override
	protected boolean upgrade(){ return true; }

	//a passive: nothing to switch on, so it stays out of the quick panel
	@Override
	public boolean toggleable(){ return false; }

	@Override
	public java.util.ArrayList<String> actions( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero ){
		return new java.util.ArrayList<>();
	}

	@Override
	public int onHitProc( xyz.gabriwar.warpedpixeldungeon.actors.Char enemy, int damage, boolean ranged ){
		if (!ranged && level > 0 && com.watabou.utils.Random.Int(100) < 15 * level){
			Hero hero = Dungeon.hero;
			int effectiveMT = hero.MT + RingOfMagic.manaBonus( hero );
			//overflow: a full pool spills the drink into the blade instead
			if (level >= MAX_LEVEL && hero.MP >= effectiveMT){
				if (enemy.sprite != null){
					enemy.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 6 );
					enemy.sprite.flash();
				}
				SpatialSound.play( Assets.Sounds.HIT_MAGIC, enemy, 0.8f, 1.2f );
				return damage + OVERFLOW_DAMAGE;
			}
			hero.MP = Math.max( hero.MP, Math.min( effectiveMT, hero.MP + 1 + level ) );
			//the blade drinks, and it shows
			if (hero.sprite != null){
				hero.sprite.emitter().burst( EnergyParticle.FACTORY, 2 + level );
				SpatialSound.play( Assets.Sounds.CHARGEUP, hero, 0.4f, 1.6f );
			}
		}
		return damage;
	}
}
