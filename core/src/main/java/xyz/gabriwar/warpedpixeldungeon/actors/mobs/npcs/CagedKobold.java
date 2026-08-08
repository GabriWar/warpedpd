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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.CandleOfMindVision;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IceKey;
import xyz.gabriwar.warpedpixeldungeon.journal.Notes;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CagedKoboldSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndQuest;
import com.watabou.utils.Bundle;

//ported from Remixed PD's Ice Caves: a kobold locked in a cage that trades its
//candle for the ice key carried by the branch's boss.
public class CagedKobold extends NPC {

	{
		spriteClass = CagedKoboldSprite.class;

		properties.add( Property.IMMOVABLE );
		properties.add( Property.MINIBOSS ); //immune to charm/dread the way other quest NPCs are

		state = PASSIVE;
	}

	@Override
	public boolean interact( Char c ) {
		sprite.turnTo( pos, c.pos );

		if (!(c instanceof Hero)) {
			return true;
		}
		Hero hero = (Hero) c;

		if (Quest.completed) {
			GLog.i( Messages.get( this, "thanks" ) );
			return true;
		}

		if (Quest.given) {
			IceKey key = hero.belongings.getItem( IceKey.class );
			if (key != null) {
				key.detach( hero.belongings.backpack );

				CandleOfMindVision candle = new CandleOfMindVision();
				if (!candle.collect()) {
					Dungeon.level.drop( candle, hero.pos ).sprite.drop();
				}

				Quest.complete();
				GameScene.show( new WndQuest( this, Messages.get( this, "end" ) ) );
				CellEmitter.get( pos ).start( Speck.factory( Speck.LIGHT ), 0.2f, 3 );

				destroy();
				sprite.die();
			} else {
				GLog.i( Messages.get( this, "reminder" ) );
			}
		} else {
			GameScene.show( new WndQuest( this, Messages.get( this, "intro" ) ) );
			Quest.given = true;
			Notes.add( Notes.Landmark.CAGED_KOBOLD );
		}

		return true;
	}

	@Override
	public boolean reset() {
		return true;
	}

	public static class Quest {

		private static boolean spawned;
		private static boolean given;
		private static boolean completed;

		public static void reset() {
			spawned = false;
			given = false;
			completed = false;
		}

		private static final String NODE      = "cagedkobold";
		private static final String SPAWNED   = "spawned";
		private static final String GIVEN     = "given";
		private static final String COMPLETED = "completed";

		public static void storeInBundle( Bundle bundle ) {
			Bundle node = new Bundle();
			node.put( SPAWNED, spawned );
			node.put( GIVEN, given );
			node.put( COMPLETED, completed );
			bundle.put( NODE, node );
		}

		public static void restoreFromBundle( Bundle bundle ) {
			Bundle node = bundle.getBundle( NODE );
			if (node != null && !node.isNull()) {
				spawned   = node.getBoolean( SPAWNED );
				given     = node.getBoolean( GIVEN );
				completed = node.getBoolean( COMPLETED );
			} else {
				reset();
			}
		}

		public static boolean spawned() {
			return spawned;
		}

		public static void complete() {
			completed = true;
			Notes.remove( Notes.Landmark.CAGED_KOBOLD );
		}

		//one cage per run, placed on the first frozen floor
		public static void spawn( xyz.gabriwar.warpedpixeldungeon.levels.Level level, int cell ) {
			if (spawned) return;
			CagedKobold npc = new CagedKobold();
			npc.pos = cell;
			level.mobs.add( npc );
			spawned = true;
		}
	}
}
