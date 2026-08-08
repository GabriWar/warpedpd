/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2018-2019 Anon
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

package xyz.gabriwar.warpedpixeldungeon.items.spells;

import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

public class CrimsonEpithet extends TargetedSpell {

	{
		image = ItemSpriteSheet.CRIMSON_EPITHET;
		talentChance = 1 / (float) Recipe.OUT_QUANTITY;
	}

	@Override
	protected void affectTarget(Ballistica bolt, Hero hero) {
		final Char ch = Actor.findChar(bolt.collisionPos);

		if (ch != null) {
			if (!ch.properties().contains(Char.Property.IMMOVABLE)) {
				int chPos   = ch.pos;
				int heroPos = hero.pos;
				ScrollOfTeleportation.appear(ch, heroPos);
				ScrollOfTeleportation.appear(hero, chPos);
			} else {
				GLog.w(Messages.get(this, "tele_fail"));
			}
		} else {
			ScrollOfTeleportation.appear(hero, bolt.collisionPos);
		}
		onSpellused();
	}

	@Override
	public int value() {
		return Math.round(quantity * ((30 + 30) / 6f));
	}

	public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {

		static final int OUT_QUANTITY = 6;

		{
			inputs     = new Class[]{ScrollOfTeleportation.class, ScrollOfMagicMapping.class};
			inQuantity = new int[]{1, 1};
			cost       = 3;
			output     = CrimsonEpithet.class;
			outQuantity = OUT_QUANTITY;
		}
	}
}
