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
import xyz.gabriwar.warpedpixeldungeon.journal.Document;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.EnchantingScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.EnchantingStationSprite;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;

/**
 * The enchanting pedestal: an interactable station that can empower a weapon's
 * enchantments or weave a second one in, at the cost of alchemical energy.
 */
public class EnchantingStation extends NPC {

	{
		spriteClass = EnchantingStationSprite.class;

		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);

		alignment = Alignment.NEUTRAL;
		state = PASSIVE;
	}

	@Override
	public void damage( int dmg, Object src ) {
		//the pedestal is ancient stone, nothing chips it
	}

	@Override
	public boolean add( Buff buff ) {
		return false;
	}

	@Override
	public boolean reset() {
		return true;
	}

	@Override
	public boolean interact(Char c) {
		sprite.turnTo( pos, c.pos );
		if (c != Dungeon.hero){
			return true;
		}

		//point new players at the enchanting guide page, like the alchemy pot does
		Document.ADVENTURERS_GUIDE.findPage( Document.GUIDE_ENCHANTING );
		if (!Document.ADVENTURERS_GUIDE.isPageRead( Document.GUIDE_ENCHANTING )){
			GameScene.flashForDocument( Document.ADVENTURERS_GUIDE, Document.GUIDE_ENCHANTING );
		}

		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				Game.switchScene( EnchantingScene.class );
			}
		});

		return true;
	}
}
