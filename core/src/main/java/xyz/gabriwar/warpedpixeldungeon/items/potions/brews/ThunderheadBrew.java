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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.StormCharge;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ChargedSteam;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Steam;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLightning;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfSteam;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

public class ThunderheadBrew extends Brew {

	{
		image = ItemSpriteSheet.BREW_THUNDERHEAD;
	}

	@Override
	public void shatter( int cell ) {
		splash( cell );
		if (Dungeon.level.heroFOV[cell]) {
			Sample.INSTANCE.play( Assets.Sounds.SHATTER );
			Sample.INSTANCE.play( Assets.Sounds.LIGHTNING );
		}

		for (int offset : PathFinder.NEIGHBOURS9){
			int c = cell + offset;
			if (Dungeon.level.passable[c]) {
				GameScene.add( Blob.seed( c, 10, Steam.class ) );
				GameScene.add( Blob.seed( c, ChargedSteam.DURATION, ChargedSteam.class ) );
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
		Buff.prolong( hero, StormCharge.class, StormCharge.DURATION );
		Sample.INSTANCE.play( Assets.Sounds.LIGHTNING );
		hero.sprite.emitter().burst( SparkParticle.FACTORY, 8 );
	}

	public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {

		{
			inputs =  new Class[]{PotionOfLightning.class, PotionOfSteam.class};
			inQuantity = new int[]{1, 1};

			cost = 3;

			output = ThunderheadBrew.class;
			outQuantity = 1;
		}

	}

	@Override
	public ItemSprite.Glowing potionGlowing() {
		return new ItemSprite.Glowing( 0xE0F0FF );
	}
}
