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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.windows.WndQuest;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

//shared base for the Remixed PD town folk ported as pure flavour: they stand in the
//town, are untouchable, and say a line when spoken to. No services or quests.
public abstract class FlavorNPC extends NPC {

	{
		properties.add( Property.IMMOVABLE );
		state = PASSIVE;
	}

	//how many "greetN" lines this NPC has in its strings (>=1)
	protected int lineCount() { return 1; }

	//most townsfolk are in bed at night; the few who keep a lamp lit override this
	protected boolean sleepsAtNight() { return true; }

	@Override
	public int defenseSkill( Char enemy ) {
		return INFINITE_EVASION;
	}

	@Override
	protected Char chooseEnemy() {
		return null;
	}

	@Override
	public void damage( int dmg, Object src ) {
	}

	@Override
	public boolean add( Buff buff ) {
		return false;
	}

	@Override
	public boolean interact( Char c ) {
		sprite.turnTo( pos, c.pos );
		if (c != Dungeon.hero) return true;

		final String text;
		if (sleepsAtNight() && Shopkeeper.closedForNight()) {
			text = Messages.get( this, "asleep" );
		} else {
			final String key = lineCount() > 1 ? "greet" + (1 + Random.Int( lineCount() )) : "greet";
			text = Messages.get( this, key );
		}
		Game.runOnRenderThread( new Callback() {
			@Override
			public void call() {
				GameScene.show( new WndQuest( FlavorNPC.this, text ) );
			}
		} );
		return true;
	}

	@Override
	public boolean reset() {
		return true;
	}
}
