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

import com.watabou.utils.Random;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Freezing;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

public class PotionOfFrost extends Potion {

	{
		icon = ItemSpriteSheet.Icons.POTION_FROST;
	}
	
	@Override
	public void shatter( int cell ) {

		splash( cell );
		if (Dungeon.level.heroFOV[cell]) {
			identify();

			Sample.INSTANCE.play( Assets.Sounds.SHATTER );
		}
		
		// Colder ambient = frost lasts longer; water gives medium duration
		int ambientBonus = (int)(-ClimateManager.localTemp() * 0.5f);
		for (int offset : PathFinder.NEIGHBOURS9){
			int c = cell + offset;
			if (!Dungeon.level.solid[c]) {
				int base = Dungeon.level.water[c] ? 18 : 30;
				int amount = Math.max(5, base + ambientBonus);
				GameScene.add(Blob.seed(c, amount, Freezing.class));
			}
			Char ch = Actor.findChar(c);
			if (ch != null) {
				if (Float.isNaN(ch.bodyTemp)) ch.bodyTemp = 20f;
				ch.bodyTemp -= 15f;
			}
		}
		
	}
	
	@Override
	public int value() {
		return isKnown() ? 30 * quantity : super.value();
	}


	@Override
	public void potionProc(Hero hero, Char enemy, float damage) {
		Buff.affect(enemy, Chill.class, 4f);

		if (Random.Float() < 0.2f) {
			//need to delay this through an actor so that the freezing isn't broken by taking damage from the staff hit.
			new FlavourBuff() {
				{
					actPriority = VFX_PRIO;
				}

				public boolean act() {
					Buff.affect(target, Frost.class, Math.round(Frost.DURATION));
					return super.act();
				}
			}.attachTo(enemy);
		}
	}

	@Override
	public ItemSprite.Glowing potionGlowing() {
		return new ItemSprite.Glowing( 0x00E5FF );
	}
}
