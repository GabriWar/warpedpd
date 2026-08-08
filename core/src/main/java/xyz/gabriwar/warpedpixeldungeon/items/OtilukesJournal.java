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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Belongings;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ElmoParticle;
import xyz.gabriwar.warpedpixeldungeon.items.misc.Spectacles;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.journalpages.JournalPage;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

public class OtilukesJournal extends Item {

	public static final float TIME_TO_USE = 1;
	public static final int FULL_CHARGE = 1000;

	public static final String AC_RETURN = "RETURN";
	public static final String AC_ADD    = "ADD";
	public static final String AC_PORT   = "READ";

	public static final int NUM_ROOMS = 10;

	// Room number -> depth mapping:
	// 0 = Safe Room    -> depth 50
	// 1 = Sokoban 1    -> depth 51
	// 2 = Sokoban 2    -> depth 52
	// 3 = Sokoban 3    -> depth 53
	// 4 = Sokoban 4    -> depth 54
	// 5 = Town   -> depth 55
	// 6 = Vault        -> depth 66
	// 7 = Dragon Cave  -> depth 67

	public int returnDepth = -1;
	public int returnPos;

	public int charge = 0;
	public int journalLevel = 1;

	public boolean[] rooms  = new boolean[NUM_ROOMS];
	public boolean[] firsts = new boolean[NUM_ROOMS];

	{
		image = ItemSpriteSheet.OTILUKES_JOURNAL;
		unique = true;
	}

	public int checkReading() {
		int lvl = 1;
		if (Dungeon.hero.buff(Spectacles.MagicSight.class) != null) {
			lvl += 1;
		}
		return lvl;
	}

	public int reqCharges() {
		return Math.round(FULL_CHARGE / (journalLevel * checkReading()));
	}

	//spend the limited drop here rather than where the journal is placed, so that a shop copy
	//the hero never buys still leaves the boss drops available as a fallback
	@Override
	public boolean doPickUp(Hero hero, int pos) {
		if (super.doPickUp(hero, pos)) {
			Dungeon.LimitedDrops.JOURNAL.drop();
			return true;
		}
		return false;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);

		actions.add(AC_ADD);

		// RETURN is only available when inside journal dimensions (depths 50-54, 66, 67,
		// or the surface town) and we have a valid return depth stored
		if (returnDepth > 0
				&& Dungeon.depth >= 50
				&& (Dungeon.depth <= 55 || Dungeon.depth == 66 || Dungeon.depth == 67
					|| Dungeon.depth == xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel.DEPTH)
				&& !hero.petfollow) {
			actions.add(AC_RETURN);
		}

		// READ requires enough charges, not on a deep floor, and at least one page added
		if (charge >= reqCharges()
				&& Dungeon.depth < 26
				&& !hero.petfollow
				&& (journalLevel > 1 || rooms[0])) {
			actions.add(AC_PORT);
		}

		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {

		super.execute(hero, action);

		if (action.equals(AC_PORT)) {

			if (Dungeon.bossLevel()) {
				hero.spend(TIME_TO_USE);
				GLog.w(Messages.get(this, "preventing"));
				return;
			}

			// Build list of unlocked destination names and their room indices
			ArrayList<String> destNames = new ArrayList<>();
			final ArrayList<Integer> destRooms = new ArrayList<>();

			String[] roomKeys = {
				"room_safe", "room_sokoban1", "room_sokoban2", "room_sokoban3",
				"room_sokoban4", "room_dolyahaven", "room_vault", "room_dragoncave"
			};

			for (int i = 0; i < 8; i++) {
				if (rooms[i]) {
					destNames.add(Messages.get(this, roomKeys[i]));
					destRooms.add(i);
				}
			}

			if (destNames.isEmpty()) {
				GLog.w(Messages.get(this, "no_pages"));
				return;
			}

			GameScene.show(new WndOptions(
					new ItemSprite(this),
					Messages.get(this, "name"),
					Messages.get(this, "where"),
					destNames.toArray(new String[0])
			) {
				@Override
				protected void onSelect(int index) {
					if (index >= 0 && index < destRooms.size()) {
						int room = destRooms.get(index);
						portToRoom(room);
					}
				}
			});

		} else if (action.equals(AC_RETURN)) {

			hero.spend(TIME_TO_USE);

			updateQuickslot();

			Level.beforeTransition();
			InterlevelScene.mode = InterlevelScene.Mode.RETURN;
			InterlevelScene.returnDepth = returnDepth;
			InterlevelScene.returnPos = returnPos;
			Game.switchScene(InterlevelScene.class);
			returnDepth = -1;

		} else if (action.equals(AC_ADD)) {

			GameScene.selectItem(itemSelector);
		}
	}

	private void portToRoom(int room) {
		Hero hero = Dungeon.hero;

		returnDepth = Dungeon.depth;
		returnPos = hero.pos;
		hero.spend(TIME_TO_USE);

		Level.beforeTransition();

		InterlevelScene.mode = InterlevelScene.Mode.JOURNAL;
		InterlevelScene.returnDepth = Dungeon.depth;
		InterlevelScene.returnPos = Dungeon.hero.pos;
		InterlevelScene.journalpage = room;
		Game.switchScene(InterlevelScene.class);

		//forward the first-visit flag to the next newLevel() before clearing it,
		//so Sokoban/DragonCave levels generate their first-visit loot.
		Dungeon.nextLevelFirstVisit = firsts[room];
		firsts[room] = false;
		charge = 0;
	}

	@Override
	public int value() {
		return 300 * quantity;
	}

	public void reset() {
		returnDepth = -1;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public String desc() {
		String desc = Messages.get(this, "desc");

		if (journalLevel > 1) {
			if (charge < reqCharges()) {
				desc += "\n\n" + Messages.get(this, "charges", charge, reqCharges());
			} else {
				desc += "\n\n" + Messages.get(this, "fully_charged");
			}
		}

		return desc;
	}

	private final WndBag.ItemSelector itemSelector = new WndBag.ItemSelector() {

		@Override
		public String textPrompt() {
			return Messages.get(OtilukesJournal.class, "select_page");
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable(Item item) {
			return item instanceof JournalPage;
		}

		@Override
		public void onSelect(Item item) {
			if (item instanceof JournalPage) {
				Hero hero = Dungeon.hero;
				int room = ((JournalPage) item).room;

				hero.sprite.operate(hero.pos);
				hero.busy();
				hero.spend(2f);
				Sample.INSTANCE.play(Assets.Sounds.BURNING);
				hero.sprite.emitter().burst(ElmoParticle.FACTORY, 12);

				item.detach(hero.belongings.backpack);
				GLog.h(Messages.get(OtilukesJournal.class, "page_added"));
				journalLevel++;

				if (charge < (FULL_CHARGE - 500)) {
					charge = FULL_CHARGE;
				} else {
					charge += 500;
				}

				rooms[room] = true;
				firsts[room] = true;
			}
		}
	};

	private static final String DEPTH   = "returnDepth";
	private static final String POS     = "returnPos";
	private static final String ROOMS   = "rooms";
	private static final String FIRSTS  = "firsts";
	private static final String CHARGE  = "charge";
	private static final String LEVEL   = "journalLevel";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(DEPTH, returnDepth);
		bundle.put(ROOMS, rooms);
		bundle.put(CHARGE, charge);
		bundle.put(FIRSTS, firsts);
		bundle.put(LEVEL, journalLevel);
		if (returnDepth != -1) {
			bundle.put(POS, returnPos);
		}
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		returnDepth  = bundle.getInt(DEPTH);
		returnPos    = bundle.getInt(POS);
		charge       = bundle.getInt(CHARGE);
		journalLevel = bundle.getInt(LEVEL);
		rooms  = bundle.getBooleanArray(ROOMS);
		firsts = bundle.getBooleanArray(FIRSTS);
		// Safety: ensure arrays are the right size after deserialization
		if (rooms == null || rooms.length < NUM_ROOMS) {
			rooms = new boolean[NUM_ROOMS];
		}
		if (firsts == null || firsts.length < NUM_ROOMS) {
			firsts = new boolean[NUM_ROOMS];
		}
	}
}
