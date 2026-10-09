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

package xyz.gabriwar.warpedpixeldungeon.windows;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.journal.Bestiary;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.OptionSlider;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollingListPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.PathFinder;
import com.watabou.utils.RectF;
import com.watabou.utils.Reflection;

import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class WndDebugPicker extends Window {

	private static final int HEADER_HEIGHT = 14;

	private static final Generator.Category[] WEAPON_TIERS = {
			Generator.Category.WEP_T1, Generator.Category.WEP_T2,
			Generator.Category.WEP_T3, Generator.Category.WEP_T4,
			Generator.Category.WEP_T5
	};
	private static final Generator.Category[] MISSILE_TIERS = {
			Generator.Category.MIS_T1, Generator.Category.MIS_T2,
			Generator.Category.MIS_T3, Generator.Category.MIS_T4,
			Generator.Category.MIS_T5
	};

	public static WndDebugPicker forItemCategory(Generator.Category cat) {
		return new WndDebugPicker(cat.name(), buildItemEntries(cat), true);
	}

	public static WndDebugPicker forAllItems() {
		return forItemClasses("ALL ITEMS", DebugItems.ALL);
	}

	public static WndDebugPicker forMobCategory(Bestiary bestiary) {
		return new WndDebugPicker(bestiary.name(), buildMobEntries(bestiary), true);
	}

	public static WndDebugPicker forAllMobs() {
		ArrayList<Entry> entries = new ArrayList<>();
		HashSet<Class<?>> seen = new HashSet<>();
		for (Bestiary b : Bestiary.values()) {
			for (Class<?> cls : b.entities()) {
				if (!Mob.class.isAssignableFrom(cls)) continue;
				if (seen.contains(cls)) continue;
				seen.add(cls);
				try {
					Mob sample = (Mob) Reflection.newInstance(cls);
					if (sample == null) continue;
					String name = sample.name();
					if (name == null || name.isEmpty()) name = cls.getSimpleName();
					Image icon = null;
					CharSprite sprite = sample.sprite();
					if (sprite != null) {
						sprite.idle();
						icon = new Image(sprite);
						if (icon.width() >= 17 || icon.height() >= 17) {
							RectF frame = icon.frame();
							float wS = frame.width() * (1f - 17f / icon.width());
							if (wS > 0) { frame.left += wS/2f; frame.right -= wS/2f; }
							float hS = frame.height() * (1f - 17f / icon.height());
							if (hS > 0) { frame.top += hS/2f; frame.bottom -= hS/2f; }
							icon.frame(frame);
						}
					}
					@SuppressWarnings("unchecked")
					final Class<? extends Mob> mobCls = (Class<? extends Mob>) cls;
					final String mobName = name;
					Entry entry = new Entry(name, icon, () -> selectCellForMob(mobCls, mobName));
					entry.searchText = name + " " + cls.getSimpleName();
					entries.add(entry);
				} catch (Exception ignored) { }
			}
		}
		Collections.sort(entries);
		return new WndDebugPicker("ALL MOBS (" + entries.size() + ")", entries, true);
	}

	@SuppressWarnings("unchecked")
	public static WndDebugPicker forWeatherBlobs() {
		ArrayList<Entry> entries = new ArrayList<>();
		Class<?>[] blobs = {
				xyz.gabriwar.warpedpixeldungeon.actors.blobs.StormCloud.class,
				xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blizzard.class,
				xyz.gabriwar.warpedpixeldungeon.actors.blobs.MistCloud.class,
				xyz.gabriwar.warpedpixeldungeon.actors.blobs.HeatHaze.class,
				xyz.gabriwar.warpedpixeldungeon.actors.blobs.FallenLeaves.class,
				xyz.gabriwar.warpedpixeldungeon.actors.blobs.Moonbeam.class,
				xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire.class,
				xyz.gabriwar.warpedpixeldungeon.actors.blobs.Freezing.class,
				xyz.gabriwar.warpedpixeldungeon.actors.blobs.Inferno.class,
				xyz.gabriwar.warpedpixeldungeon.actors.blobs.Electricity.class,
				xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas.class,
				xyz.gabriwar.warpedpixeldungeon.actors.blobs.ConfusionGas.class,
		};
		for (Class<?> cls : blobs) {
			String name = cls.getSimpleName();
			final Class<? extends Blob> blobCls = (Class<? extends Blob>) cls;
			entries.add(new Entry(name, null, () -> selectCellForBlob(blobCls, name)));
		}
		return new WndDebugPicker("WEATHER / BLOBS", entries);
	}

	public static WndDebugPicker forExoticPotions() {
		return forItemType("EXOTIC POTIONS", xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.ExoticPotion.class);
	}

	public static WndDebugPicker forExoticScrolls() {
		return forItemType("EXOTIC SCROLLS", xyz.gabriwar.warpedpixeldungeon.items.scrolls.exotic.ExoticScroll.class);
	}

	public static WndDebugPicker forSpells() {
		return forItemType("SPELLS", xyz.gabriwar.warpedpixeldungeon.items.spells.Spell.class);
	}

	private static WndDebugPicker forItemType(String title, Class<?> type) {
		ArrayList<Class<?>> classes = new ArrayList<>();
		for (Class<?> cls : DebugItems.ALL) if (type.isAssignableFrom(cls)) classes.add(cls);
		return forItemClasses(title, classes.toArray(new Class<?>[0]));
	}

	private static WndDebugPicker forItemClasses(String title, Class<?>[] classes) {
		ArrayList<Entry> entries = itemEntries(classes);
		return new WndDebugPicker(title, entries, true);
	}

	private static ArrayList<Entry> itemEntries(Class<?>[] classes) {
		ArrayList<Entry> entries = new ArrayList<>();
		for (Class<?> cls : classes) {
			String name = cls.getSimpleName();
			Image icon = null;
			try {
				Item sample = (Item) Reflection.newInstance(cls);
				if (sample != null) {
					// Do not identify preview objects: that changes global item knowledge.
					String translated = xyz.gabriwar.warpedpixeldungeon.messages.Messages.get(cls, "name");
					if (!translated.contains("NO TEXT FOUND") && !translated.isEmpty()) name = translated;
					icon = new ItemSprite(sample.image(), sample.glowing());
				}
			} catch (Exception ignored) {
				// Keep the entry available even when its preview needs live game state.
			}
			final String itemName = name;
			Entry entry = new Entry(name, icon, () -> selectCellForItem(cls, itemName));
			entry.searchText = name + " " + cls.getSimpleName();
			entries.add(entry);
		}
		Collections.sort(entries);
		return entries;
	}

	/** picks a weapon or armor from the pack, then a list of every enchantment or glyph
	 *  (common, uncommon, rare and the curses) to put on it, or strips it */
	public static void forEnchanting() {
		GameScene.selectItem(new WndBag.ItemSelector() {
			@Override
			public String textPrompt() { return "Choose a weapon or armor to enchant"; }

			@Override
			public boolean itemSelectable(Item item) {
				return item instanceof xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon
						|| item instanceof xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
			}

			@Override
			public void onSelect(Item item) {
				if (item == null) return;
				ArrayList<Entry> entries = new ArrayList<>();
				boolean weapon = item instanceof xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
				Class<?>[][] tables = weapon
						? new Class<?>[][]{ xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon.Enchantment.common,
								xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon.Enchantment.uncommon,
								xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon.Enchantment.rare,
								xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon.Enchantment.curses }
						: new Class<?>[][]{ xyz.gabriwar.warpedpixeldungeon.items.armor.Armor.Glyph.common,
								xyz.gabriwar.warpedpixeldungeon.items.armor.Armor.Glyph.uncommon,
								xyz.gabriwar.warpedpixeldungeon.items.armor.Armor.Glyph.rare,
								xyz.gabriwar.warpedpixeldungeon.items.armor.Armor.Glyph.curses };
				String[] tiers = { "common", "uncommon", "rare", "curse" };
				for (int t = 0; t < tables.length; t++) {
					for (Class<?> cls : tables[t]) {
						String name = xyz.gabriwar.warpedpixeldungeon.messages.Messages.get(cls, "name");
						if (name.contains("NO TEXT FOUND")) name = cls.getSimpleName();
						String label = name + " (" + tiers[t] + ")";
						Entry e = new Entry(label, null, () -> {
							Object ench = Reflection.newInstance(cls);
							if (weapon) ((xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon) item).enchant((xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon.Enchantment) ench);
							else ((xyz.gabriwar.warpedpixeldungeon.items.armor.Armor) item).inscribe((xyz.gabriwar.warpedpixeldungeon.items.armor.Armor.Glyph) ench);
							xyz.gabriwar.warpedpixeldungeon.effects.Enchanting.show(Dungeon.hero, item);
							Item.updateQuickslot();
						});
						e.searchText = label + " " + cls.getSimpleName();
						entries.add(e);
					}
				}
				entries.add(new Entry("- remove enchantment -", null, () -> {
					if (weapon) ((xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon) item).enchant(null);
					else ((xyz.gabriwar.warpedpixeldungeon.items.armor.Armor) item).inscribe(null);
					Item.updateQuickslot();
				}));
				GameScene.show(new WndDebugPicker((weapon ? "ENCHANT " : "INSCRIBE ") + item.name().toUpperCase(), entries, false));
			}
		});
	}

	// depth, branch, label: every place the fast travel list goes
	static final Object[][] TRAVEL_LEVELS = {
				// Main dungeon
				{1, 0, "Sewers 1"},
				{2, 0, "Sewers 2"},
				{3, 0, "Sewers 3"},
				{4, 0, "Sewers 4"},
				{5, 0, "Goo (Boss)"},
				{6, 0, "Prison 6"},
				{7, 0, "Prison 7"},
				{8, 0, "Prison 8"},
				{9, 0, "Prison 9"},
				{10, 0, "Tengu (Boss)"},
				{11, 0, "Caves 11"},
				{12, 0, "Caves 12"},
				{13, 0, "Caves 13"},
				{14, 0, "Caves 14"},
				{15, 0, "DM-300 (Boss)"},
				{16, 0, "City 16"},
				{17, 0, "City 17"},
				{18, 0, "City 18"},
				{19, 0, "City 19"},
				{20, 0, "Dwarf King (Boss)"},
				{21, 0, "Halls 21"},
				{22, 0, "Halls 22"},
				{23, 0, "Halls 23"},
				{24, 0, "Halls 24"},
				{25, 0, "Yog-Dzewa (Boss)"},
				{26, 0, "Last Level (Abyss)"},
				// Post-game / Sprouted
				{27, 0, "Field"},
				{28, 0, "Battle"},
				{29, 0, "Fishing"},
				{30, 0, "Sprouted Vault"},
				{31, 0, "Catacomb"},
				{32, 0, "Fortress"},
				{33, 0, "Chasm"},
				{35, 0, "Infest Boss"},
				{36, 0, "Tengu Den"},
				{37, 0, "Skeleton Boss"},
				{38, 0, "Crab Boss"},
				{40, 0, "Thief Boss"},
				{41, 0, "Thief Catch"},
				// Sokoban / Town
				{50, 0, "Safe Level"},
				{51, 0, "Sokoban Intro"},
				{52, 0, "Sokoban Castle"},
				{53, 0, "Sokoban Teleport"},
				{54, 0, "Sokoban Puzzles"},
				{55, 0, "Town"},
				// Mines / Post-town
				{56, 0, "Mine 56"},
				{57, 0, "Mine 57"},
				{58, 0, "Mine 58"},
				{59, 0, "Mine 59"},
				{60, 0, "Mine 60"},
				{61, 0, "Mine 61"},
				{62, 0, "Mine 62"},
				{63, 0, "Mine 63"},
				{64, 0, "Mine 64"},
				{65, 0, "Mines Boss"},
				{66, 0, "Sokoban Vault"},
				{67, 0, "Dragon Cave"},
				{85, 0, "Rooms Showcase (debug)"},
				{86, 0, "Warped Rooms (debug)"},
				{97, 0, "Overworld (debug)"},
				{99, 0, "Zot Boss"},
				// Branch 1
				{11, 1, "Mining 11 (branch 1)"},
				{12, 1, "Mining 12 (branch 1)"},
				{13, 1, "Mining 13 (branch 1)"},
				{14, 1, "Mining 14 (branch 1)"},
				{16, 1, "Vault 16 (branch 1)"},
				{17, 1, "Vault 17 (branch 1)"},
				{18, 1, "Vault 18 (branch 1)"},
				{19, 1, "Vault 19 (branch 1)"},
				// Branch 2 - Frozen region (Unleashed PD port), entered from Halls 22
				{21, 2, "Frozen 21 (branch 2)"},
				{22, 2, "Frozen 22 (branch 2)"},
				{23, 2, "Frozen 23 (branch 2)"},
				{24, 2, "Frozen 24 (branch 2)"},
				{25, 2, "Demon Lord (Frozen Boss, branch 2)"},
				// Branch 3/4 - Temple sub-region (Re-ARranged port), entered from Caves 14
				{14, 3, "Temple 14 (branch 3)"},
				{14, 4, "Temple Chasm 14 (branch 4)"},
				// Branch 5 - Spider Nest (Remixed reimplementation), loops 6->10->6
				{6,  5, "Spider Nest 6 (branch 5)"},
				{7,  5, "Spider Nest 7 (branch 5)"},
				{8,  5, "Spider Nest 8 (branch 5)"},
				{9,  5, "Spider Nest 9 (branch 5)"},
				{10, 5, "Spider Nest 10 (branch 5, loops)"},

				// Branch 6 - Remixed town building interiors, off the town square
				{1, 6, "Town: Church (branch 6)"},
				{2, 6, "Town: Cinema (branch 6)"},
				{3, 6, "Town: Library (branch 6)"},
				{4, 6, "Town: Shop (branch 6)"},
				{5, 6, "Town: Fortune Teller (branch 6)"},
				{6, 6, "Town: Inn (branch 6)"},
	};

	public static WndDebugPicker forTravel() {
		ArrayList<Entry> entries = new ArrayList<>();
		for (Object[] l : TRAVEL_LEVELS) {
			final int depth = (int) l[0];
			final int branch = (int) l[1];
			final String name = (String) l[2];
			entries.add(new Entry(name, null, () -> {
				try { Dungeon.saveAll(); } catch (Exception ignored) { }
				InterlevelScene.mode = InterlevelScene.Mode.RETURN;
				InterlevelScene.returnDepth = depth;
				InterlevelScene.returnBranch = branch;
				InterlevelScene.returnPos = -1;
				Game.switchScene(InterlevelScene.class);
			}));
		}
		return new WndDebugPicker("FAST TRAVEL", entries);
	}

	private WndDebugPicker(String title, ArrayList<Entry> entries) {
		this(title, entries, false);
	}

	private WndDebugPicker(String title, ArrayList<Entry> entries, boolean searchable) {
		super();
		//as wide as the debug window it opens from
		final int width = WndDebug.width(chrome.marginHor());
		final int listTop = HEADER_HEIGHT + (searchable ? 20 : 0);

		int maxH = (int)(Game.height / PixelScene.defaultZoom * 0.8f);
		int contentH = entries.size() * 18;
		int finalH = (int) Math.min(maxH, listTop + Math.max(18, contentH) + 4);

		resize(width, finalH);

		RenderedTextBlock header = PixelScene.renderTextBlock(title, 9);
		header.hardlight(Window.TITLE_COLOR);
		add(header);
		header.setPos((width - header.width()) / 2f, 0);

		ScrollingListPane list = new ScrollingListPane();
		add(list);
		list.setRect(0, listTop, width, finalH - listTop);
		if (searchable) {
			RedButton search = new RedButton("Search (" + entries.size() + ")", 6) {
				private String query = "";
				@Override protected void onClick() {
					GameScene.show(new WndTextInput("Search " + title.toLowerCase(java.util.Locale.ROOT),
							"Name or class name. Leave empty to show all.",
							query, 80, false, "Search", "Cancel") {
						@Override public void onSelect(boolean positive, String text) {
							if (!positive) return;
							query = text.trim();
							int count = populate(list, entries, query);
							text(query.isEmpty() ? "Search (" + count + ")" : "Search: " + query + " (" + count + ")");
						}
					});
				}
			};
			add(search);
			search.setRect(0, HEADER_HEIGHT, width, 18);
		}
		populate(list, entries, "");
	}

	private int populate(ScrollingListPane list, ArrayList<Entry> entries, String query) {
		list.clear();
		int count = 0;
		for (Entry e : entries) {
			if (!matches(e.searchText, query)) continue;
			count++;
			ScrollingListPane.ListItem item = new ScrollingListPane.ListItem(
					e.icon, null, e.name
			) {
				@Override
				public boolean onClick(float x, float y) {
					if (inside(x, y)) {
						hide();
						e.action.run();
						return true;
					}
					return false;
				}
			};
			list.addItem(item);
		}
		if (count == 0) list.addTitle("No matching items");
		list.scrollTo(0, 0);
		return count;
	}

	static boolean matches(String name, String query) {
		String normalized = name.toLowerCase(java.util.Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
		return normalized.contains(query.toLowerCase(java.util.Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", ""));
	}

	// --- Entry data ---

	private static class Entry implements Comparable<Entry> {
		String name;
		String searchText;
		Runnable action;
		Image icon;

		Entry(String name, Image icon, Runnable action) {
			this.name = name;
			this.searchText = name;
			this.icon = icon;
			this.action = action;
		}

		@Override
		public int compareTo(Entry o) {
			return name.compareToIgnoreCase(o.name);
		}
	}

	// --- Build item entries ---

	private static ArrayList<Entry> buildItemEntries(Generator.Category cat) {
		ArrayList<Class<?>> classes = new ArrayList<>();
		for (Class<?> cls : DebugItems.ALL) {
			if (cat.superClass.isAssignableFrom(cls)) classes.add(cls);
		}
		return itemEntries(classes.toArray(new Class<?>[0]));
	}

	// --- Build mob entries ---

	private static ArrayList<Entry> buildMobEntries(Bestiary bestiary) {
		ArrayList<Entry> entries = new ArrayList<>();

		for (Class<?> cls : bestiary.entities()) {
			if (!Mob.class.isAssignableFrom(cls)) continue;

			try {
				Mob sample = (Mob) Reflection.newInstance(cls);
				if (sample == null) continue;
				String name = sample.name();
				if (name == null || name.isEmpty()) name = cls.getSimpleName();

				Image icon = null;
				CharSprite sprite = sample.sprite();
				if (sprite != null) {
					sprite.idle();
					icon = new Image(sprite);
					// Clip large sprites to 17x17 like journal does
					if (icon.width() >= 17 || icon.height() >= 17) {
						RectF frame = icon.frame();
						float wShrink = frame.width() * (1f - 17f / icon.width());
						if (wShrink > 0) { frame.left += wShrink/2f; frame.right -= wShrink/2f; }
						float hShrink = frame.height() * (1f - 17f / icon.height());
						if (hShrink > 0) { frame.top += hShrink/2f; frame.bottom -= hShrink/2f; }
						icon.frame(frame);
					}
				}

				@SuppressWarnings("unchecked")
				final Class<? extends Mob> mobCls = (Class<? extends Mob>) cls;
				final String mobName = name;
				Entry entry = new Entry(name, icon, () -> selectCellForMob(mobCls, mobName));
				entry.searchText = name + " " + cls.getSimpleName();
				entries.add(entry);
			} catch (Exception ignored) { }
		}

		Collections.sort(entries);
		return entries;
	}

	// --- Quantity picker helper ---

	//the most the debug picker places at once: items can pile a hundred deep on a
	//cell; monsters each need a cell of their own around the tapped one
	private static final int MAX_ITEMS = 100;
	private static final int MAX_MOBS = 10;

	private static void askQuantity(String name, int max, IntConsumer callback) {
		final int[] qty = {1};
		WarpedPixelDungeon.scene().addToFront(new Window() {
			{
				//140, or less on a phone too narrow for it
				int w = (int)Math.min(140, PixelScene.uiCamera.width - chrome.marginHor() - 1);

				RenderedTextBlock title = PixelScene.renderTextBlock("", 7);
				title.hardlight(TITLE_COLOR);
				title.maxWidth(w);
				add(title);

				final OptionSlider[] slider = new OptionSlider[1];
				//the count shows in the title, and -/+ nudge it one at a time: a slider
				//a hundred values wide cannot land on an exact number by touch alone
				Runnable show = () -> {
					title.text("Quantity: " + name + "  x" + qty[0]);
					title.setPos((w - title.width()) / 2f, 2);
				};
				slider[0] = new OptionSlider("", "1", String.valueOf(max), 1, max) {
					@Override
					protected void onChange() {
						qty[0] = getSelectedValue();
						show.run();
					}
				};
				show.run();
				slider[0].setSelectedValue(1);
				add(slider[0]);
				slider[0].setRect(0, title.bottom() + 2, w, 21);

				RedButton less = new RedButton("-") {
					@Override
					protected void onClick() {
						qty[0] = Math.max(1, qty[0] - 1);
						slider[0].setSelectedValue(qty[0]);
						show.run();
					}
				};
				add(less);
				less.setRect(0, slider[0].bottom() + 2, w / 2f - 1, 14);
				RedButton more = new RedButton("+") {
					@Override
					protected void onClick() {
						qty[0] = Math.min(max, qty[0] + 1);
						slider[0].setSelectedValue(qty[0]);
						show.run();
					}
				};
				add(more);
				more.setRect(w / 2f + 1, slider[0].bottom() + 2, w / 2f - 1, 14);

				RedButton btnOk = new RedButton("Confirm") {
					@Override
					protected void onClick() {
						hide();
						callback.accept(qty[0]);
					}
				};
				add(btnOk);
				btnOk.setRect(0, less.bottom() + 2, w, 14);

				resize(w, (int) btnOk.bottom() + 2);
			}
		});
	}

	// --- Cell selection for placement ---

	private static void selectCellForItem(Class<?> itemCls, String name) {
		Item probe = (Item) Reflection.newInstance(itemCls);
		boolean typed = xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality.eligible(probe);
		askQuantity(name, MAX_ITEMS, count -> {
			if (typed) {
				askQuality(name, (rarity, type, masterwork) -> dropItems(name, count, cell -> {
					Item item = (Item) Reflection.newInstance(itemCls);
					if (item == null) return null;
					xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality.roll(item, rarity, type, masterwork);
					return item;
				}));
			} else {
				dropItems(name, count, cell -> (Item) Reflection.newInstance(itemCls));
			}
		});
	}

	private interface QualityChoice {
		void chosen(xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity rarity,
					xyz.gabriwar.warpedpixeldungeon.items.rarity.ItemType type, int masterwork);
	}

	//the extra step for weapons, armor, missiles and wands: rarity, type and masterwork,
	//each slider's leftmost value meaning "roll it like a real drop"
	private static void askQuality(String name, QualityChoice callback) {
		final int[] pick = {0, 0, 0};
		WarpedPixelDungeon.scene().addToFront(new Window() {
			{
				int w = 120;

				RenderedTextBlock title = PixelScene.renderTextBlock("Quality: " + name, 7);
				title.hardlight(TITLE_COLOR);
				title.maxWidth(w);
				add(title);
				title.setPos((w - title.width()) / 2f, 2);

				OptionSlider rarity = new OptionSlider("Rarity (random, common .. exotic)", "rnd", "exotic", 0, 5) {
					@Override
					protected void onChange() { pick[0] = getSelectedValue(); }
				};
				rarity.setSelectedValue(0);
				add(rarity);
				rarity.setRect(0, title.bottom() + 2, w, 24);

				OptionSlider type = new OptionSlider("Type (random, gamma, beta, alpha)", "rnd", "alpha", 0, 3) {
					@Override
					protected void onChange() { pick[1] = getSelectedValue(); }
				};
				type.setSelectedValue(0);
				add(type);
				type.setRect(0, rarity.bottom() + 2, w, 24);

				OptionSlider masterwork = new OptionSlider("Masterwork steps", "0", "10", 0, 10) {
					@Override
					protected void onChange() { pick[2] = getSelectedValue(); }
				};
				masterwork.setSelectedValue(0);
				add(masterwork);
				masterwork.setRect(0, type.bottom() + 2, w, 24);

				RedButton btnOk = new RedButton("Confirm") {
					@Override
					protected void onClick() {
						hide();
						callback.chosen(
								pick[0] == 0 ? null : xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity.values()[pick[0] - 1],
								pick[1] == 0 ? null : xyz.gabriwar.warpedpixeldungeon.items.rarity.ItemType.values()[pick[1] - 1],
								pick[2]);
					}
				};
				btnOk.setRect(0, masterwork.bottom() + 2, w, 16);
				add(btnOk);

				resize(w, (int) btnOk.bottom() + 2);
			}
		});
	}

	private interface ItemMaker {
		Item make(int cell);
	}

	private static void dropItems(String name, int count, ItemMaker maker) {
		GameScene.selectCell(new CellSelector.Listener() {
			@Override
			public void onSelect(Integer cell) {
				if (cell == null || Dungeon.level == null) return;
				for (int i = 0; i < count; i++) {
					Item item = maker.make(cell);
					if (item != null) {
						item.identify();
						Dungeon.level.drop(item, cell).sprite.drop();
					}
				}
			}

			@Override
			public String prompt() {
				return "Tap to place x" + count + ": " + name;
			}
		});
	}

	private static void selectCellForMob(Class<? extends Mob> mobCls, String name) {
		askQuantity(name, MAX_MOBS, count -> GameScene.selectCell(new CellSelector.Listener() {
			@Override
			public void onSelect(Integer cell) {
				if (cell == null || Dungeon.level == null) return;
				if (cell < 0 || cell >= Dungeon.level.length()) return;
				ArrayList<Integer> cells = spawnCells(cell, count, Dungeon.level.width(),
						Dungeon.level.passable, c -> Actor.findChar(c) != null);
				if (cells.size() < count) GLog.w("Room for only " + cells.size() + " of " + count + " there.");
				for (int c : cells) {
					Mob mob = Reflection.newInstance(mobCls);
					if (mob == null) return;
					mob.pos = c;
					mob.state = mob.debugSpawnState();
					GameScene.add(mob);
				}
			}

			@Override
			public String prompt() {
				return "Tap to spawn x" + count + ": " + name;
			}
		}));
	}

	/**
	 * Where spawned monsters stand: the tapped cell whatever its terrain (a bat over a chasm, as
	 * the window always allowed) unless someone stands there, then the nearest open, free cells
	 * reached from it without crossing a wall, one monster to a cell.
	 */
	static ArrayList<Integer> spawnCells(int cell, int count, int width, boolean[] passable, IntPredicate occupied) {
		ArrayList<Integer> out = new ArrayList<>();
		boolean[] seen = new boolean[passable.length];
		int[] around = { -width - 1, -width, -width + 1, -1, 1, width - 1, width, width + 1 };
		ArrayDeque<Integer> queue = new ArrayDeque<>();
		queue.add(cell);
		seen[cell] = true;
		while (!queue.isEmpty() && out.size() < count) {
			int c = queue.poll();
			if ((c == cell || passable[c]) && !occupied.test(c)) out.add(c);
			for (int o : around) {
				int n = c + o;
				//no wrapping from one row's end to the next row's start
				if (n < 0 || n >= passable.length || Math.abs(n % width - c % width) > 1) continue;
				if (seen[n] || !passable[n]) continue;
				seen[n] = true;
				queue.add(n);
			}
		}
		return out;
	}

	private static void selectCellForBlob(Class<? extends Blob> blobCls, String name) {
		GameScene.selectCell(new CellSelector.Listener() {
			@Override
			public void onSelect(Integer cell) {
				if (cell == null || Dungeon.level == null) return;
				// Seed blob at target + spread to neighbors
				GameScene.add(Blob.seed(cell, 10, blobCls));
				for (int offset : PathFinder.NEIGHBOURS8) {
					int adj = cell + offset;
					if (adj >= 0 && adj < Dungeon.level.length()
							&& !Dungeon.level.solid[adj]) {
						GameScene.add(Blob.seed(adj, 8, blobCls));
					}
				}
			}

			@Override
			public String prompt() {
				return "Tap to spawn: " + name;
			}
		});
	}
}
