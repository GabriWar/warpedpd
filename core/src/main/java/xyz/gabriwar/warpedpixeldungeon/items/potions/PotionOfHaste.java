/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.items.potions;

import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.bow.BowWeapon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Haste;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.SpellSprite;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

public class PotionOfHaste extends Potion {
	
	{
		icon = ItemSpriteSheet.Icons.POTION_HASTE;
	}
	
	@Override
	public void apply(Hero hero) {
		identify();
		
		GLog.w( Messages.get(this, "energetic") );
		Buff.prolong( hero, Haste.class, Haste.DURATION);
		SpellSprite.show(hero, SpellSprite.HASTE, 1, 1, 0);
	}
	
	@Override
	public int value() {
		return isKnown() ? 40 * quantity : super.value();
	}


	@Override
	public void potionProc(Hero hero, Char enemy, float damage) {
		new FlavourBuff() {
			{
				actPriority = VFX_PRIO;
			}

			public boolean act() {
				if (hero.buff(BowWeapon.BowFatigue.class) != null) {
					hero.buff(BowWeapon.BowFatigue.class).detach();
				}
				return super.act();
			}
		}.attachTo(hero);
	}

	@Override
	public ItemSprite.Glowing potionGlowing() {
		return new ItemSprite.Glowing( 0xFFCF32 );
	}
}
