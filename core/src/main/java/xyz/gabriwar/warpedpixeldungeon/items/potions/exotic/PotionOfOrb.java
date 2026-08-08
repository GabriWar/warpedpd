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

package xyz.gabriwar.warpedpixeldungeon.items.potions.exotic;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.MagicOrb;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.tweeners.AlphaTweener;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class PotionOfOrb extends ExoticPotion {

	{
		icon = ItemSpriteSheet.Icons.POTION_ORB;
	}
	@Override
	public void shatter(int cell) {
		if (Dungeon.level.heroFOV[cell]) {
			identify();
			splash(cell);
			Sample.INSTANCE.play(Assets.Sounds.SHATTER);
		}
		MagicOrb orb = new MagicOrb();
		orb.HP = orb.HT;
		orb.pos = cell;
		GameScene.add(orb);
		Actor.addDelayed(new Pushing(orb, cell, cell), -1f);
		orb.sprite.alpha(0);
		orb.sprite.parent.add(new AlphaTweener(orb.sprite, 1, 0.15f));
		Sample.INSTANCE.play(Assets.Sounds.RAY);
	}
}
