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

package xyz.gabriwar.warpedpixeldungeon.items.ore;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.Ores;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

/**
 * A lump of ore prised out of a vein in the world's rock (levels/overworld/Ores): one class per
 * metal, so the pack stacks each on its own and the save needs nothing but the class. Shops buy
 * it; the troll blacksmith in town smelts a batch of it into a masterwork core, one crucible a
 * day (Blacksmith2.pourCrucible); the two rarest also give an alchemist energy.
 *
 * What the miner is told: the first lump of a metal is named ("You prise a lump of iron ore
 * from the rock"), then nothing until the stack passes a multiple of ten - a seam worked lump
 * by lump would flood the log otherwise.
 */
public abstract class Ore extends Item {

	{
		stackable = true;
	}

	/** The metal (its price, batch, colour and sound: Ores.Kind). */
	public abstract Ores.Kind kind();

	@Override
	public boolean isUpgradable(){
		return false;
	}

	@Override
	public boolean isIdentified(){
		return true;
	}

	@Override
	public int value(){
		return kind().price * quantity;
	}

	@Override
	public int energyVal(){
		return kind().energy * quantity;
	}

	@Override
	public String desc(){
		Ores.Kind k = kind();
		String desc = super.desc() + Messages.get( Ore.class, k.bonus > 0 ? "smith_bonus" : "smith", k.batch, k.bonus );
		if (k.energy > 0) desc += Messages.get( Ore.class, "energy", k.energy );
		return desc;
	}

	/** The pick breaks a vein cell: the metal's splash, stars and ring, then the lump into the pack. */
	public void struck( Hero hero, int cell ){
		Ores.Kind k = kind();
		Splash.at( cell, k.colour, 6 );
		if (k.stars > 0) CellEmitter.center( cell ).burst( Speck.factory( Speck.STAR ), k.stars );
		SpatialSound.play( k.sound, cell, 1f, k.pitch );
		mined( hero, cell );
	}

	/**
	 * The lump goes into the miner's pack, or drops at his feet when it is full. It takes no
	 * time of its own: the swing that broke the rock is the turn (Hero.actMine spends it), so
	 * nothing here spends or ends the turn. Returns whether it went into the pack.
	 */
	public boolean mined( Hero hero, int cell ){
		int n = quantity;
		String name = name();
		Ore held = hero.belongings.getItem( getClass() );
		int before = held == null ? 0 : held.quantity();
		if (collect( hero.belongings.backpack )){
			GameScene.pickUp( this, cell );
			SpatialSound.play( Assets.Sounds.ITEM, hero );
			int have = before + n;
			if (before == 0){
				NetManager.heroLog( hero, Messages.get( Ore.class, n > 1 ? "prised_two" : "prised", name ) );
			} else if (have / 10 != before / 10){
				NetManager.heroLog( hero, Messages.get( Ore.class, "you_now_have", have, name ) );
			}
			return true;
		}
		Heap h = Dungeon.level.drop( this, hero.pos );
		if (h.sprite != null) h.sprite.drop();
		return false;
	}
}
