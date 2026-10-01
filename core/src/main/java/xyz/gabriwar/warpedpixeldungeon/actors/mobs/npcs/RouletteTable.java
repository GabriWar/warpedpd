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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.MasterworkCore;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.RouletteTableSprite;
import xyz.gabriwar.warpedpixeldungeon.windows.WndQuest;
import xyz.gabriwar.warpedpixeldungeon.windows.WndRoulette;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;

/**
 * The roulette table at the inn's bar (TownInnLevel): a real European roulette played
 * for masterwork cores, open day and night. The table is furniture, not a person: it
 * never moves, sleeps or takes a hit.
 */
public class RouletteTable extends NPC {

	{
		spriteClass = RouletteTableSprite.class;

		properties.add( Property.IMMOVABLE );
		properties.add( Property.INORGANIC );

		alignment = Alignment.NEUTRAL;
		state = PASSIVE;
	}

	@Override
	public void damage( int dmg, Object src ){
	}

	@Override
	public boolean add( Buff buff ){
		return false;
	}

	@Override
	public boolean reset(){
		return true;
	}

	@Override
	public boolean interact( Char c ){
		if (c != Dungeon.hero) return true;
		Game.runOnRenderThread( new Callback(){
			@Override
			public void call(){
				if (MasterworkCore.held( Dungeon.hero ) <= 0){
					GameScene.show( new WndQuest( RouletteTable.this, Messages.get( RouletteTable.class, "no_cores" ) ) );
				} else {
					GameScene.show( new WndRoulette( Dungeon.hero ) );
				}
			}
		} );
		return true;
	}
}
