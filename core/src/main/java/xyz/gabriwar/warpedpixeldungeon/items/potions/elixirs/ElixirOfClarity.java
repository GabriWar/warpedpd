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

package xyz.gabriwar.warpedpixeldungeon.items.potions.elixirs;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Adrenaline;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ArcaneArmor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barkskin;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Haste;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ShieldBuff;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ClarityBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfExperience;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMana;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class ElixirOfClarity extends Elixir {

	{
		image = ItemSpriteSheet.ELIXIR_CLARITY;
	}

	@Override
	public void apply( Hero hero ) {
		PotionOfMana.restoreMana( hero );
		Buff.affect( hero, ClarityBuff.class ).reset();
	}

	@Override
	public void shatter( int cell ) {
		splash( cell );
		if (Dungeon.level.heroFOV[cell]) {
			Sample.INSTANCE.play( Assets.Sounds.SHATTER );
		}

		for (int offset : PathFinder.NEIGHBOURS9) {
			Char ch = Actor.findChar( cell + offset );
			if (ch == null || ch.alignment != Char.Alignment.ENEMY) continue;

			Buff.detach( ch, Haste.class );
			Buff.detach( ch, Adrenaline.class );
			Buff.detach( ch, Bless.class );
			Buff.detach( ch, Invisibility.class );
			Buff.detach( ch, ArcaneArmor.class );
			for (Buff b : ch.buffs( Barkskin.class )) {
				b.detach();
			}
			for (Buff b : ch.buffs( ShieldBuff.class )) {
				b.detach();
			}
			if (ch.sprite != null && Dungeon.level.heroFOV[ch.pos]) {
				ch.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 4 );
			}
		}
	}

	public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {

		{
			inputs =  new Class[]{PotionOfMana.class, PotionOfExperience.class};
			inQuantity = new int[]{1, 1};

			cost = 3;

			output = ElixirOfClarity.class;
			outQuantity = 1;
		}

	}

	@Override
	public ItemSprite.Glowing potionGlowing() {
		return new ItemSprite.Glowing( 0x60A0FF );
	}
}
