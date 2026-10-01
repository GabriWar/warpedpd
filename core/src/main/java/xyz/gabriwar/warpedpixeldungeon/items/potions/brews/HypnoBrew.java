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

package xyz.gabriwar.warpedpixeldungeon.items.potions.brews;

import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import java.util.ArrayList;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHypno;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLove;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;

public class HypnoBrew extends Brew {

	{
		image = ItemSpriteSheet.BREW_HYPNO;
	}

	@Override
	public void shatter( int cell ) {
		splash( cell );
		if (Dungeon.level.heroFOV[cell]) {
			Sample.INSTANCE.play( Assets.Sounds.SHATTER );
			Sample.INSTANCE.play( Assets.Sounds.CHARMS );
		}

		PathFinder.buildDistanceMap( cell, BArray.not( Dungeon.level.solid, null ), 2 );
		for (int i = 0; i < PathFinder.distance.length; i++) {
			if (PathFinder.distance[i] < Integer.MAX_VALUE) {
				Char ch = Actor.findChar( i );
				if (ch == null || ch.alignment != Char.Alignment.ENEMY) continue;

				if (Char.hasProp( ch, Char.Property.BOSS ) || Char.hasProp( ch, Char.Property.MINIBOSS )) {
					Buff.affect( ch, Charm.class, 3f ).object = Dungeon.hero.id();
					if (ch.sprite != null) ch.sprite.centerEmitter().start( Speck.factory( Speck.HEART ), 0.2f, 3 );
				} else {
					Buff.prolong( ch, Amok.class, 8f );
				}
			}
		}
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_DRINK );
		return actions;
	}

	@Override
	public void apply( Hero hero ) {
		Sample.INSTANCE.play( Assets.Sounds.CHARMS );
		for (int offset : PathFinder.NEIGHBOURS8) {
			Char ch = Actor.findChar( hero.pos + offset );
			if (ch == null || ch.alignment != Char.Alignment.ENEMY) continue;
			if (Char.hasProp( ch, Char.Property.BOSS ) || Char.hasProp( ch, Char.Property.MINIBOSS )) continue;

			Buff.affect( ch, Charm.class, 5f ).object = hero.id();
			if (ch.sprite != null) ch.sprite.centerEmitter().start( Speck.factory( Speck.HEART ), 0.2f, 3 );
		}
	}

	public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {

		{
			inputs =  new Class[]{PotionOfHypno.class, PotionOfLove.class};
			inQuantity = new int[]{1, 1};

			cost = 3;

			output = HypnoBrew.class;
			outQuantity = 1;
		}

	}

	@Override
	public ItemSprite.Glowing potionGlowing() {
		return new ItemSprite.Glowing( 0xFF60C0 );
	}
}
