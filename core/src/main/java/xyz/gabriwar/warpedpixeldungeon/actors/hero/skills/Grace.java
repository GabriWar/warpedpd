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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMagic;

public class Grace extends PassiveSkillA2 {

	//at max rank a mote that finds the pool already full heals instead
	private static final float OVERFLOW_HEAL = 0.03f;

	{
		name = "Grace";
		image = 105;
		tier = 2;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//every enemy you slay sends a mote of light home: 1 mana per level
	@Override
	public void onKill( Mob mob, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive())
			return;

		int maxMana = hero.MT + RingOfMagic.manaBonus( hero );
		boolean overflow = hero.MP >= maxMana;
		if (overflow && (level < MAX_LEVEL || hero.HP >= hero.HT))
			return;

		if (mob.sprite != null && hero.sprite != null && hero.sprite.parent != null)
			MagicMissile.boltFromChar( hero.sprite.parent, MagicMissile.LIGHT_MISSILE, mob.sprite, hero.pos, null );

		if (overflow){
			hero.heal( SkillInteractions.ofHealth( hero.HT, OVERFLOW_HEAL ) );
		} else {
			int gain = Math.min( level, maxMana - hero.MP );
			hero.MP += gain;
			if (hero.sprite != null){
				hero.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 2 + gain );
				hero.sprite.showStatus( 0x8ac0ff, "+" + gain );
			}
		}
		SpatialSound.play( Assets.Sounds.CHARMS, hero, 0.5f, 1.5f );
	}
}
