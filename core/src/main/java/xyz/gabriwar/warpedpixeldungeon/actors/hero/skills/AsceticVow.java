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

import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;


public class AsceticVow extends Skill {

	//at max rank an answered vow also blesses the cleric
	private static final float BLESS_TURNS = 5f;

	{
		name = "Ascetic Vow";
		tag = "CAA";
		image = 147;
		tier = 4;
		level = 0;
	}

	@Override
	protected boolean upgrade(){ return true; }

	/** the vow: health you pray back returns mana. 1 MP per 6 HP at level 1, per 5 at level 2, per 4 at level 3 */
	public static void tithe( Hero hero, int healed ){
		int lvl = CurrentSkills.skillLevel( AsceticVow.class );
		if (lvl <= 0 || healed <= 0 || hero == null) return;
		int back = healed / (7 - lvl);
		if (back <= 0) return;
		hero.MP = Math.max( hero.MP, Math.min( hero.MT + xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMagic.manaBonus( hero ), hero.MP + back ) );
		if (lvl >= MAX_LEVEL) Buff.prolong( hero, Bless.class, BLESS_TURNS );
		if (hero.sprite != null){
			hero.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), Math.min( 6, back ) );
			if (lvl >= MAX_LEVEL) hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 4 );
			hero.sprite.showStatus( CharSprite.NEUTRAL, "+" + back + " MP" );
		}
	}
}
