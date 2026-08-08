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


package xyz.gabriwar.warpedpixeldungeon.items.food;

import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

/**
 * The bridge fisherman's catch: a whole fish, as filling as a pasty and a
 * little restorative. What he pulls out of the river (and what he calls it)
 * follows the season; the winter ice-hole catch is the cheapest.
 */
public class Fish extends Food {

	{
		image = ItemSpriteSheet.STEAMED_FISH;
		energy = Hunger.STARVING;
	}

	private static String seasonKey(){
		return GameCalendar.season().name().toLowerCase();
	}

	@Override
	public String name() {
		if (seed != null) return super.name();
		return Messages.get( this, "name_" + seasonKey() );
	}

	@Override
	public String desc() {
		return Messages.get( this, "desc_" + seasonKey() );
	}

	@Override
	protected void satisfy( Hero hero ) {
		super.satisfy( hero );
		//a fresh meal heals for 5% of max hp, at least 3
		int toHeal = Math.max( 3, hero.HT / 20 );
		hero.HP = Math.min( hero.HP + toHeal, hero.HT );
		hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( toHeal ), FloatingText.HEALING );
	}

	@Override
	public int value() {
		return (GameCalendar.season() == GameCalendar.Season.WINTER ? 8 : 12) * quantity;
	}
}
