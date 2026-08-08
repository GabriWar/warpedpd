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

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.items.PuddingCup;
import xyz.gabriwar.warpedpixeldungeon.items.TownReturnBeacon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.OtilukeNPCSprite;
import xyz.gabriwar.warpedpixeldungeon.windows.WndQuest;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

public class OtilukeNPC extends NPC {

	{
		spriteClass = OtilukeNPCSprite.class;

		properties.add( Property.IMMOVABLE );
	}

	protected static final float SPAWN_DELAY = 2f;

	private boolean first = true;

	private static final String FIRST = "first";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( FIRST, first );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		first = bundle.getBoolean( FIRST );
	}

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

	public static OtilukeNPC spawnAt( int pos ) {
		if (Dungeon.level.passable[pos] && Actor.findChar( pos ) == null) {

			OtilukeNPC w = new OtilukeNPC();
			w.pos = pos;
			GameScene.add( w, SPAWN_DELAY );

			return w;

		} else {
			return null;
		}
	}

	@Override
	public boolean interact( Char c ) {

		sprite.turnTo( pos, c.pos );

		if (xyz.gabriwar.warpedpixeldungeon.net.NetDialogs.handleNetHero(c, description())) return true;
		if (c != Dungeon.hero) {
			return true;
		}

		TownReturnBeacon beacon = Dungeon.hero.belongings.getItem( TownReturnBeacon.class );

		if (Badges.checkOtilukeRescued()) {
			// Already rescued — occasional pudding cup reward (5% chance)
			if (Random.Int(100) < 5) {
				tell( Messages.get(this, "rescued_pudding") );
				Dungeon.level.drop( new PuddingCup(), Dungeon.hero.pos ).sprite.drop();
			} else {
				tell( Messages.get(this, "rescued") );
			}
		} else if (first && beacon == null) {
			// First rescue, no beacon — give one
			Badges.validateOtilukeRescued();
			first = false;
			tell( Messages.get(this, "first_rescue_beacon") );
			Dungeon.level.drop( new TownReturnBeacon(), Dungeon.hero.pos ).sprite.drop();
		} else {
			// First rescue with beacon, or subsequent visits
			Badges.validateOtilukeRescued();
			tell( Messages.get(this, "first_rescue") );
		}

		return true;
	}

	private void tell( String text ) {
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.show( new WndQuest( OtilukeNPC.this, text ) );
			}
		});
	}

	@Override
	public boolean reset() {
		return true;
	}
}
