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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Freezing;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.IceStorm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.WarmthBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfChilli;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfSun;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class ElixirOfWarmth extends Elixir {

	{
		image = ItemSpriteSheet.ELIXIR_WARMTH;
	}

	@Override
	public void apply( Hero hero ) {
		Buff.prolong( hero, WarmthBuff.class, WarmthBuff.DURATION );
		hero.sprite.emitter().burst( FlameParticle.FACTORY, 10 );
	}

	@Override
	public void shatter( int cell ) {
		splash( cell );
		if (Dungeon.level.heroFOV[cell]) {
			SpatialSound.play( Assets.Sounds.SHATTER, cell );
			SpatialSound.play( Assets.Sounds.BURNING, cell );
		}

		Blob freezing = Dungeon.level.blobs.get( Freezing.class );
		Blob iceStorm = Dungeon.level.blobs.get( IceStorm.class );
		for (int offset : PathFinder.NEIGHBOURS9) {
			int c = cell + offset;
			if (freezing != null) freezing.clear( c );
			if (iceStorm != null) iceStorm.clear( c );

			Char ch = Actor.findChar( c );
			if (ch != null) {
				Buff.detach( ch, Frost.class );
				Buff.detach( ch, Chill.class );
			}

			if (!Dungeon.level.solid[c] && Dungeon.level.heroFOV[c]) {
				CellEmitter.get( c ).burst( FlameParticle.FACTORY, 2 );
			}
		}

		Char target = Actor.findChar( cell );
		if (target != null && target.alignment == Char.Alignment.ENEMY) {
			Buff.affect( target, Burning.class ).reignite( target );
		}
	}

	public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {

		{
			inputs =  new Class[]{PotionOfChilli.class, PotionOfSun.class};
			inQuantity = new int[]{1, 1};

			cost = 2;

			output = ElixirOfWarmth.class;
			outQuantity = 1;
		}

	}

	@Override
	public ItemSprite.Glowing potionGlowing() {
		return new ItemSprite.Glowing( 0xFF7020 );
	}
}
