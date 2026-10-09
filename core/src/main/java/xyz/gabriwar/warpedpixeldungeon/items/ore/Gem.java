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
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

/**
 * A rough gem out of a crystal of a cave seam (levels/overworld/Ores.gemAt): one class per stone,
 * so each stacks on its own. Any merchant buys it, an alchemist breaks it down into energy.
 * Every find is told - they are rare.
 */
public abstract class Gem extends Item {

	{
		stackable = true;
	}

	/** The stone (its price, energy, colour: Ores.GemKind). */
	public abstract Ores.GemKind kind();

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
		return super.desc() + Messages.get( Gem.class, "uses", kind().energy );
	}

	/** The pick breaks open the crystal that held it: a splash of its colour, a twinkle, a chime. */
	public void struck( Hero hero, int cell ){
		Ores.GemKind k = kind();
		Splash.at( cell, k.colour, 8 );
		CellEmitter.center( cell ).burst( Speck.factory( Speck.STAR ), 4 );
		SpatialSound.play( Assets.Sounds.CHARMS, cell, 1f, k.pitch );
		mined( hero, cell );
	}

	/** Into the miner's pack, or at his feet when it is full; no time of its own (see Ore.mined). */
	public boolean mined( Hero hero, int cell ){
		NetManager.heroLog( hero, GLog.POSITIVE + Messages.get( Gem.class, "found", name() ) );
		if (collect( hero.belongings.backpack )){
			GameScene.pickUp( this, cell );
			SpatialSound.play( Assets.Sounds.ITEM, hero );
			return true;
		}
		Heap h = Dungeon.level.drop( this, hero.pos );
		if (h.sprite != null) h.sprite.drop();
		return false;
	}
}
