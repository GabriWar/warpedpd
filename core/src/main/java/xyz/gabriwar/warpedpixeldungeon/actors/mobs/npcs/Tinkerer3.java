/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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
import xyz.gabriwar.warpedpixeldungeon.items.Waterskin;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.Mushroom;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.TinkererNPCSprite;
import xyz.gabriwar.warpedpixeldungeon.windows.WndQuest;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTinkerer3;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;

public class Tinkerer3 extends NPC {

	{
		spriteClass = TinkererNPCSprite.class;

		properties.add( Property.IMMOVABLE );
	}

	@Override
	protected boolean act() {
		throwItems();
		return super.act();
	}

	@Override
	public int defenseSkill( Char enemy ) {
		return INFINITE_EVASION;
	}

	@Override
	public void damage( int dmg, Object src ) {
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
	public boolean interact( Char c ) {

		sprite.turnTo( pos, c.pos );

		if (xyz.gabriwar.warpedpixeldungeon.net.NetDialogs.handleNetHero(c, description())) return true;
		if (c != Dungeon.hero) {
			return true;
		}

		Item item = Dungeon.hero.belongings.getItem( Mushroom.class );
		Item vial = Dungeon.hero.belongings.getItem( Waterskin.class );

		if (item != null && vial != null) {
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show( new WndTinkerer3( Tinkerer3.this, item ) );
				}
			});
		} else if (item == null && vial != null) {
			tell( Messages.get(this, "need_mushroom") );
		} else {
			tell( Messages.get(this, "greeting") );
		}

		return true;
	}

	private void tell( String text ) {
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.show( new WndQuest( Tinkerer3.this, text ) );
			}
		});
	}
}
