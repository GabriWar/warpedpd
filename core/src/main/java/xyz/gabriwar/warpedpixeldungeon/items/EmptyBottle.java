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

package xyz.gabriwar.warpedpixeldungeon.items;

import com.watabou.utils.PathFinder;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfWater;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

/**
 * An empty bottle: filled from the water the hero stands in or beside, it becomes a bottle of
 * water (PotionOfWater). The alchemy pot fills one too; every shop sells them by five.
 */
public class EmptyBottle extends Item {

	public static final String AC_FILL = "FILL";

	{
		image = ItemSpriteSheet.EMPTY_BOTTLE;
		stackable = true;
		defaultAction = AC_FILL;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_FILL );
		return actions;
	}

	/** Whether there is water to fill a bottle from: on the hero's cell or beside it. */
	public static boolean waterAt( Level level, int cell ){
		for (int n : PathFinder.NEIGHBOURS9){
			int c = cell + n;
			if (c >= 0 && c < level.length() && level.water[c]) return true;
		}
		return false;
	}

	/** A drunk or poured bottle comes back empty, into the pack or at the hero's feet. */
	public static void giveBack( Hero hero ){
		Item bottle = new EmptyBottle();
		if (!bottle.collect( hero.belongings.backpack )){
			Dungeon.level.drop( bottle, hero.pos ).sprite.drop();
		}
	}

	/** The bottle a potion brewed from seeds goes into: an empty one first, else a bottle of water. Null when the pack has neither. */
	public static Item forBrewing( Hero hero ){
		if (hero == null) return null;
		Item bottle = hero.belongings.getItem( EmptyBottle.class );
		return bottle != null ? bottle : hero.belongings.getItem( PotionOfWater.class );
	}

	@Override
	public void execute( Hero hero, String action ) {

		super.execute( hero, action );

		if (action.equals( AC_FILL )) {

			if (!waterAt( Dungeon.level, hero.pos )){
				GLog.w( Messages.get( this, "no_water" ) );
				return;
			}

			detach( hero.belongings.backpack );
			Item water = new PotionOfWater();
			if (!water.collect( hero.belongings.backpack )){
				Dungeon.level.drop( water, hero.pos ).sprite.drop();
			}

			hero.spend( 1f );
			hero.busy();
			hero.sprite.operate( hero.pos );
			SpatialSound.play( Assets.Sounds.WATER, hero );
			GLog.i( Messages.get( this, "filled" ) );
		}
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 5 * quantity;
	}

	/** The recipe book's bottle for a potion from seeds: an empty bottle or a bottle of water will do. */
	public static class AnyBottle extends EmptyBottle {

		@Override
		public boolean isSimilar( Item item ) {
			return item instanceof EmptyBottle || item instanceof PotionOfWater;
		}

		@Override
		public String name() {
			return Messages.get( EmptyBottle.class, "any_name" );
		}

		@Override
		public String desc() {
			return Messages.get( EmptyBottle.class, "any_desc" );
		}
	}

	/** The alchemy pot's water fills an empty bottle, at no energy. */
	public static class Fill extends Recipe.SimpleRecipe {
		{
			inputs = new Class[]{ EmptyBottle.class };
			inQuantity = new int[]{ 1 };

			cost = 0;

			output = PotionOfWater.class;
			outQuantity = 1;
		}
	}
}
