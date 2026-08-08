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
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.TalismanOfForesight;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MindVision;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.SpellSprite;
import xyz.gabriwar.warpedpixeldungeon.items.misc.Spectacles;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

public class PotionOfMindVision extends Potion {

	{
		icon = ItemSpriteSheet.Icons.POTION_MINDVIS;
	}

	@Override
	public void apply( Hero hero ) {
		identify();

		//same anti-scrying gate as the Blueberry and the mapping scroll
		if (Dungeon.depth > 50 && hero.buff(Spectacles.MagicSight.class) == null) {
			GLog.w( Messages.get(this, "no_scrying") );
			return;
		}

		//and the Spectacles stretch it, which is the perk that made them worth wearing
		Buff.prolong( hero, MindVision.class, hero.buff(Spectacles.MagicSight.class) != null
				? MindVision.DURATION * 4 : MindVision.DURATION );
		SpellSprite.show(hero, SpellSprite.VISION, 1, 0.77f, 0.9f);
		Dungeon.observe();
		
		if (Dungeon.level.mobs.size() > 0) {
			GLog.i( Messages.get(this, "see_mobs") );
		} else {
			GLog.i( Messages.get(this, "see_none") );
		}
	}
	
	@Override
	public int value() {
		return isKnown() ? 30 * quantity : super.value();
	}


	@Override
	public void potionProc(Hero hero, Char enemy, float damage) {
		Buff.append(curUser, TalismanOfForesight.CharAwareness.class, 50f).charID = enemy.id();
		Dungeon.observe();
		Dungeon.hero.checkVisibleMobs();
		GameScene.updateFog();
	}

	@Override
	public ItemSprite.Glowing potionGlowing() {
		return new ItemSprite.Glowing( 0xFFC4E5 );
	}
}
