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
import xyz.gabriwar.warpedpixeldungeon.items.ActiveMrDestructo;
import xyz.gabriwar.warpedpixeldungeon.items.InactiveMrDestructo;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.Mushroom;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.TinkererNPCSprite;
import xyz.gabriwar.warpedpixeldungeon.windows.WndQuest;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTinkerer2;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;

public class Tinkerer2 extends NPC {

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
		if (Shopkeeper.closedForNight()) {
			tell( Messages.get(this, "asleep") );
			return true;
		}

		Item item = Dungeon.hero.belongings.getItem( Mushroom.class );
		Item inmrd = Dungeon.hero.belongings.getItem( InactiveMrDestructo.class );
		Item acmrd = Dungeon.hero.belongings.getItem( ActiveMrDestructo.class );

		if (item != null && inmrd != null) {
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show( new WndTinkerer2( Tinkerer2.this, item, inmrd ) );
				}
			});
		} else if (item != null && acmrd != null) {
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show( new WndTinkerer2( Tinkerer2.this, item, acmrd ) );
				}
			});
		} else if (item != null) {
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show( new WndTinkerer2( Tinkerer2.this, item, null ) );
				}
			});
		} else {
			tell( Messages.get(this, "need_mushroom") );
		}

		return true;
	}

	private void tell( String text ) {
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.show( new WndQuest( Tinkerer2.this, text ) );
			}
		});
	}
}
