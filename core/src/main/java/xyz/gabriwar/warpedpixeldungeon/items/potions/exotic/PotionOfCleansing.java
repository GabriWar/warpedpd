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

import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicImmune;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AllyBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.LostInventory;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;

public class PotionOfCleansing extends ExoticPotion {
	
	{
		icon = ItemSpriteSheet.Icons.POTION_CLEANSE;
	}
	
	@Override
	public void apply( Hero hero ) {
		identify();
		
		cleanse( hero );
		new Flare( 6, 32 ).color(0xFF4CD2, true).show( curUser.sprite, 2f );
	}
	
	@Override
	public void shatter(int cell) {
		if (Actor.findChar(cell) == null){
			super.shatter(cell);
		} else {
			splash( cell );
			if (Dungeon.level.heroFOV[cell]) {
				Sample.INSTANCE.play(Assets.Sounds.SHATTER);
				identify();
			}
			
			if (Actor.findChar(cell) != null){
				cleanse(Actor.findChar(cell));
			}
		}
	}

	public static void cleanse(Char ch){
		cleanse(ch, Cleanse.DURATION);
	}

	public static void cleanse(Char ch, float duration){
		for (Buff b : ch.buffs()){
			if (b.type == Buff.buffType.NEGATIVE
					&& !(b instanceof AllyBuff)
					&& !(b instanceof LostInventory)){
				b.detach();
			}
			if (b instanceof Hunger){
				((Hunger) b).satisfy(Hunger.STARVING);
			}
		}
		Buff.prolong(ch, Cleanse.class, duration);
	}

	public static class Cleanse extends FlavourBuff {

		{
			type = buffType.POSITIVE;
		}

		public static final float DURATION = 5f;

		@Override
		public int icon() {
			return BuffIndicator.IMMUNITY;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(1f, 0f, 2f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (DURATION - visualcooldown()) / DURATION);
		}

	}


	@Override
	public void potionProc(Hero hero, Char enemy, float damage) {
		Buff.prolong(hero, MagicImmune.class, 4f);
	}

	@Override
	public ItemSprite.Glowing potionGlowing() {
		return new ItemSprite.Glowing( 0xFF4CD2 );
	}
}
