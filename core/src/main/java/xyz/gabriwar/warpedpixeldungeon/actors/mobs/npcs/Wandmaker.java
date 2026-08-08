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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AscensionChallenge;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.items.AdamantWand;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Elemental;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RotHeart;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.quest.CeremonialCandle;
import xyz.gabriwar.warpedpixeldungeon.items.quest.CorpseDust;
import xyz.gabriwar.warpedpixeldungeon.items.quest.Embers;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.journal.Notes;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.RegularLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.Room;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.MassGraveRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.RitualSiteRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.RotGardenRoom;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Rotberry;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.WandmakerSprite;
import xyz.gabriwar.warpedpixeldungeon.windows.WndQuest;
import xyz.gabriwar.warpedpixeldungeon.windows.WndWandmaker;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class Wandmaker extends NPC {

	{
		spriteClass = WandmakerSprite.class;

		properties.add(Property.IMMOVABLE);
	}

	@Override
	public Notes.Landmark landmark() {
		return Notes.Landmark.WANDMAKER;
	}

	@Override
	protected boolean act() {
		if (Dungeon.hero.buff(AscensionChallenge.class) != null){
			die(null);
			return true;
		}
		return super.act();
	}
	
	@Override
	public int defenseSkill( Char enemy ) {
		return INFINITE_EVASION;
	}

	@Override
	public void damage( int dmg, Object src ) {
		//do nothing
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

		if (!(c instanceof Hero)) return true;
		Hero h = (Hero) c;
		Quest.HeroProgress hp = Quest.progressFor( h.id() );

		if (hp != null && hp.given) {
			if (hp.completed) return true;
			if (Quest.claimable( h )) {
				showReward( h );
			} else {
				showReminder( h );
			}
		} else {
			Quest.give( h );
			showIntro( h );
		}

		return true;
	}

	private void showReward( final Hero h ) {
		if (h.isRemote) {
			Quest.sendReward( h );
		} else {
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show( new WndWandmaker( Wandmaker.this, h ) );
				}
			});
		}
	}

	private void showReminder( final Hero h ) {
		final String msg;
		switch (Quest.type){
			case 1: default: msg = Messages.get(this, "reminder_dust",  Messages.titleCase(h.name())); break;
			case 2:          msg = Messages.get(this, "reminder_ember", Messages.titleCase(h.name())); break;
			case 3:          msg = Messages.get(this, "reminder_berry", Messages.titleCase(h.name())); break;
		}
		if (h.isRemote) {
			QuestSupport.sendInfo( this, h, msg );
		} else {
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show(new WndQuest(Wandmaker.this, msg));
				}
			});
		}
	}

	private void showIntro( final Hero h ) {
		String msg1 = "";
		switch (h.heroClass){
			case WARRIOR:  msg1 += Messages.get(this, "intro_warrior"); break;
			case ROGUE:    msg1 += Messages.get(this, "intro_rogue");   break;
			case MAGE:     msg1 += Messages.get(this, "intro_mage", Messages.titleCase(h.name())); break;
			case HUNTRESS: msg1 += Messages.get(this, "intro_huntress"); break;
			case DUELIST:  msg1 += Messages.get(this, "intro_duelist");  break;
			case CLERIC:   msg1 += Messages.get(this, "intro_cleric");   break;
		}
		msg1 += Messages.get(this, "intro_1");

		String msg2 = "";
		switch (Quest.type){
			case 1: msg2 += Messages.get(this, "intro_dust");  break;
			case 2: msg2 += Messages.get(this, "intro_ember"); break;
			case 3: msg2 += Messages.get(this, "intro_berry"); break;
		}
		msg2 += Messages.get(this, "intro_2");

		final String msg1Final = msg1;
		final String msg2Final = msg2;

		if (h.isRemote) {
			// Two chained panels collapse to one combined info dialog for the client.
			QuestSupport.sendInfo( this, h, msg1Final + "\n\n" + msg2Final );
		} else {
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show(new WndQuest(Wandmaker.this, msg1Final){
						@Override
						public void hide() {
							super.hide();
							GameScene.show(new WndQuest(Wandmaker.this, msg2Final));
						}
					});
				}
			});
		}
	}
	
	public static class Quest {

		private static int type;
		// 1 = corpse dust quest
		// 2 = elemental embers quest
		// 3 = rotberry quest

		private static boolean spawned;

		// Shared objective: set true when the FIRST hero hands in the quest item. After
		// that, every hero who accepted the quest can claim their own wand pair without
		// needing the (unique) world item.
		private static boolean objectiveDone;

		// Per-hero reward + acceptance. Each hero rolls their own pair of wands.
		public static class HeroProgress implements PerHeroProgress {
			public int heroId;
			public boolean given;
			public boolean completed;
			public Wand wand1;
			public Wand wand2;

			@Override public int heroId() { return heroId; }

			@Override
			public void storeInBundle( Bundle b ) {
				b.put( HERO_ID, heroId );
				b.put( GIVEN, given );
				b.put( COMPLETED, completed );
				if (wand1 != null) b.put( WAND1, wand1 );
				if (wand2 != null) b.put( WAND2, wand2 );
			}

			@Override
			public void restoreFromBundle( Bundle b ) {
				heroId    = b.getInt( HERO_ID );
				given     = b.getBoolean( GIVEN );
				completed = b.getBoolean( COMPLETED );
				if (b.contains( WAND1 )) wand1 = (Wand) b.get( WAND1 );
				if (b.contains( WAND2 )) wand2 = (Wand) b.get( WAND2 );
			}
		}

		private static final PerHeroStore<HeroProgress> progress = new PerHeroStore<>();

		public static void reset() {
			spawned = false;
			type = 0;
			objectiveDone = false;
			progress.clear();
		}

		public static HeroProgress progressFor( int heroId ) { return progress.get( heroId ); }
		public static int type() { return type; }

		private static final String NODE		= "wandmaker";

		private static final String SPAWNED		= "spawned";
		private static final String TYPE		= "type";
		private static final String OBJDONE		= "objective_done";
		private static final String PROGRESS	= "progress";
		private static final String HERO_ID		= "hero_id";
		private static final String GIVEN		= "given";
		private static final String COMPLETED	= "completed";
		private static final String WAND1		= "wand1";
		private static final String WAND2		= "wand2";

		private static final String RITUALPOS	= "ritualpos";

		public static void storeInBundle( Bundle bundle ) {

			Bundle node = new Bundle();

			node.put( SPAWNED, spawned );

			if (spawned) {
				node.put( TYPE, type );
				node.put( OBJDONE, objectiveDone );
				progress.store( node, PROGRESS );

				if (type == 2){
					node.put( RITUALPOS, CeremonialCandle.ritualPos );
				}
			}

			bundle.put( NODE, node );
		}

		public static void restoreFromBundle( Bundle bundle ) {

			Bundle node = bundle.getBundle( NODE );

			progress.clear();

			if (!node.isNull() && (spawned = node.getBoolean( SPAWNED ))) {

				type = node.getInt(TYPE);
				objectiveDone = node.getBoolean( OBJDONE );

				progress.restore( node, PROGRESS );

				if (type == 2){
					CeremonialCandle.ritualPos = node.getInt( RITUALPOS );
				}

			} else {
				reset();
			}
		}

		private static boolean questRoomSpawned;

		public static void spawnWandmaker( Level level, Room room ) {
			if (questRoomSpawned) {

				questRoomSpawned = false;

				Wandmaker npc = new Wandmaker();
				boolean validPos;
				//Do not spawn wandmaker on the entrance, in front of a door, or on bad terrain.
				int tries = 0;
				int dist = 2;
				do {
					validPos = true;
					if (tries > 30 && dist > 0){
						tries = 0;
						dist--;
					}
					npc.pos = level.pointToCell(room.random(dist));
					if (npc.pos == level.entrance() || level.solid[npc.pos]){
						validPos = false;
					}
					for (int i : PathFinder.NEIGHBOURS4){
						if (level.map[npc.pos+i] == Terrain.DOOR){
							validPos = false;
						}
					}
					if (level.traps.get(npc.pos) != null
							|| !level.passable[npc.pos]
							|| level.map[npc.pos] == Terrain.EMPTY_SP){
						validPos = false;
					}
					tries++;
				} while (!validPos);
				level.mobs.add( npc );

				spawned = true;
			}
		}

		/** Register the quest for a hero and roll their personal pair of wands. */
		public static void give( Hero h ) {
			HeroProgress hp = new HeroProgress();
			hp.heroId = h.id();

			hp.wand1 = (Wand) Generator.random(Generator.Category.WAND);
			hp.wand1.cursed = false;
			hp.wand1.upgrade();

			hp.wand2 = (Wand) Generator.random(Generator.Category.WAND);
			ArrayList<Item> toUndo = new ArrayList<>();
			while (hp.wand2.getClass() == hp.wand1.getClass()) {
				toUndo.add(hp.wand2);
				hp.wand2 = (Wand) Generator.random(Generator.Category.WAND);
			}
			for (Item i : toUndo){
				Generator.undoDrop(i);
			}
			hp.wand2.cursed = false;
			hp.wand2.upgrade();

			hp.given = true;
			progress.put( hp );
		}

		// The hero's copy of the (unique) quest item, looked up in THEIR belongings.
		private static Item questItemIn( Hero h ) {
			switch (type) {
				case 1: default: return h.belongings.getItem(CorpseDust.class);
				case 2:          return h.belongings.getItem(Embers.class);
				case 3:          return h.belongings.getItem(Rotberry.Seed.class);
			}
		}

		// A hero can claim once the objective is globally done, or if they personally
		// hold the quest item (which becomes the one hand-in that finishes the objective).
		public static boolean claimable( Hero h ) {
			return objectiveDone || questItemIn( h ) != null;
		}

		/** Grant the chosen wand to a hero. The first claim consumes the quest item and
		 *  marks the shared objective done; later heroes claim without an item. */
		public static void claimReward( Hero h, boolean chooseWand1 ) {
			HeroProgress hp = progress.get( h.id() );
			if (hp == null || !hp.given || hp.completed) return;

			Item item = questItemIn( h );
			if (!objectiveDone && item == null) return; // need the item for the first hand-in

			if (!objectiveDone && item != null) {
				item.detach( h.belongings.backpack );
				objectiveDone = true;
				if (type == 1) Statistics.questScores[1] += 2000;
			}

			Wand reward = chooseWand1 ? hp.wand1 : hp.wand2;
			hp.completed = true;
			hp.wand1 = null;
			hp.wand2 = null;
			if (reward != null) {
				reward.identify(false);
				if (reward.doPickUp( h )) {
					xyz.gabriwar.warpedpixeldungeon.net.NetManager.heroLog( h,
							Messages.capitalize(Messages.get(h, "you_now_have", reward.name())) );
				} else {
					Wandmaker wm = findWandmaker();
					Dungeon.level.drop( reward, wm != null ? wm.pos : h.pos ).sprite.drop();
				}
			}

			//Sprouted parity: a bonus AdamantWand reforge reagent — always for the
			//wand class, or for any hero who reached the prison without killing
			//anything (sewerKills snapshot at depth 6 == total kills). Revives the
			//otherwise-dead Blacksmith2 wand-reforge feature.
			if (h.heroClass == HeroClass.MAGE || Statistics.sewerKills == Statistics.enemiesSlain) {
				AdamantWand aw = new AdamantWand();
				if (aw.doPickUp( h )) {
					xyz.gabriwar.warpedpixeldungeon.net.NetManager.heroLog( h,
							Messages.capitalize(Messages.get(h, "you_now_have", aw.name())) );
				} else {
					Wandmaker wm = findWandmaker();
					Dungeon.level.drop( aw, wm != null ? wm.pos : h.pos ).sprite.drop();
				}
			}

			maybeDespawnWandmaker();
		}

		private static Wandmaker findWandmaker() {
			if (Dungeon.level == null) return null;
			for (Mob m : Dungeon.level.mobs) {
				if (m instanceof Wandmaker) return (Wandmaker) m;
			}
			return null;
		}

		// Despawn only once every present hero has claimed their reward.
		private static void maybeDespawnWandmaker() {
			if (!QuestSupport.allPresentDone( progress, hp -> hp.completed )) return;
			final Wandmaker wm = findWandmaker();
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					if (wm != null) {
						wm.yell( Messages.get(Wandmaker.class, "farewell",
								Dungeon.hero != null ? Messages.titleCase(Dungeon.hero.name()) : "") );
						wm.destroy();
						if (wm.sprite != null) wm.sprite.die();
					}
					Notes.remove( Notes.Landmark.WANDMAKER );
				}
			});
		}

		// Route the wand-pick window to a remote hero's client (KIND_WANDMAKER_REWARD).
		static void sendReward( Hero h ) {
			HeroProgress hp = progress.get( h.id() );
			if (hp == null) return;
			try {
				org.json.JSONObject p = new org.json.JSONObject();
				p.put("type", type);
				if (hp.wand1 != null) p.put("wand1", QuestSupport.itemDisplay(hp.wand1));
				if (hp.wand2 != null) p.put("wand2", QuestSupport.itemDisplay(hp.wand2));
				xyz.gabriwar.warpedpixeldungeon.net.NetDialogs.request(
						h, xyz.gabriwar.warpedpixeldungeon.net.NetDialogs.KIND_WANDMAKER_REWARD, p, true );
			} catch (Exception ignored) {}
		}

		public static ArrayList<Room> spawnRoom( ArrayList<Room> rooms) {
			questRoomSpawned = false;
			if (!spawned && (type != 0 || (Dungeon.depth > 6 && Random.Int( 10 - Dungeon.depth ) == 0))) {

				// decide between 1,2, or 3 for quest type.
				if (type == 0) type = Random.Int(3)+1;

				switch (type){
					case 1: default:
						rooms.add(new MassGraveRoom());
						break;
					case 2:
						rooms.add(new RitualSiteRoom());
						break;
					case 3:
						rooms.add(new RotGardenRoom());
						break;
				}

				questRoomSpawned = true;

			}
			return rooms;
		}

		//quest is active if:
		public static boolean active(){
			//it is not completed
			if (objectiveDone
					|| !(Dungeon.level instanceof RegularLevel) || Dungeon.hero == null){
				return false;
			}

			//and...
			if (type == 1){
				//hero is in the mass grave room
				if (((RegularLevel) Dungeon.level).room(Dungeon.hero.pos) instanceof MassGraveRoom) {
					return true;
				}

				//or if they are corpse dust cursed
				for (Buff b : Dungeon.hero.buffs()) {
					if (b instanceof CorpseDust.DustGhostSpawner) {
						return true;
					}
				}

				return false;
			} else if (type == 2){
				//hero has summoned the newborn elemental
				for (Mob m : Dungeon.level.mobs) {
					if (m instanceof Elemental.NewbornFireElemental) {
						return true;
					}
				}

				//or hero is in the ritual room and all 4 candles are with them
				if (((RegularLevel) Dungeon.level).room(Dungeon.hero.pos) instanceof RitualSiteRoom) {
					int candles = 0;
					if (Dungeon.hero.belongings.getItem(CeremonialCandle.class) != null){
						candles += Dungeon.hero.belongings.getItem(CeremonialCandle.class).quantity();
					}

					if (candles >= 4){
						return true;
					}

					for (Heap h : Dungeon.level.heaps.valueList()){
						if (((RegularLevel) Dungeon.level).room(h.pos) instanceof RitualSiteRoom){
							for (Item i : h.items){
								if (i instanceof CeremonialCandle){
									candles += i.quantity();
								}
							}
						}
					}

					if (candles >= 4){
						return true;
					}

				}

				return false;
			} else {
				//hero is in the rot garden room and the rot heart is alive
				if (((RegularLevel) Dungeon.level).room(Dungeon.hero.pos) instanceof RotGardenRoom) {
					for (Mob m : Dungeon.level.mobs) {
						if (m instanceof RotHeart) {
							return true;
						}
					}
				}

				return false;
			}
		}
	}
}
