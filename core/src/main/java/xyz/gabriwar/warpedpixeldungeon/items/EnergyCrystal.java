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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

public class EnergyCrystal extends Item {

	{
		image = ItemSpriteSheet.ENERGY;
		stackable = true;
	}

	public EnergyCrystal() {
		this( 1 );
	}

	public EnergyCrystal( int value ) {
		this.quantity = value;
	}

	@Override
	public ArrayList<String> actions(Hero hero ) {
		return new ArrayList<>();
	}

	@Override
	public boolean doPickUp(Hero hero, int pos) {

		Catalog.setSeen(getClass());
		Statistics.itemTypesDiscovered.add(getClass());

		Dungeon.energy += quantity;
		//TODO track energy collected maybe? We do already track recipes crafted though..

		GameScene.pickUp( this, pos );
		hero.sprite.showStatusWithIcon( 0x44CCFF, Integer.toString(quantity), FloatingText.ENERGY );
		hero.spendAndNext( pickupDelay() );

		Sample.INSTANCE.play( Assets.Sounds.ITEM );

		updateQuickslot();

		return true;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

}
