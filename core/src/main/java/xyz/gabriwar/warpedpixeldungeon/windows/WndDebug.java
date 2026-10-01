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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import com.watabou.noosa.Game;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleepiness;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.journal.Bestiary;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.journal.Document;
import xyz.gabriwar.warpedpixeldungeon.journal.Journal;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideProgress;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.CheckBox;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.OptionSlider;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTextInput;
import com.watabou.noosa.ui.Component;

public class WndDebug extends WndTabbed {

	private static final int WIDTH_P = 122;
	private static final int WIDTH_L = 200;
	private static final int GAP = 2;
	private static final int BTN_HEIGHT = 16;
	private static final int SLIDER_HEIGHT = 21;

	private static int lastTab = 0;

	public WndDebug() {
		super();

		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;

		// --- Build climate ScrollPane content first (for measurement) ---
		// ScrollPane must be added AFTER resize() — build content now, add later.
		ScrollPane climateScroll = new ScrollPane(new Component());
		Component climateContent = climateScroll.content();
		float fullClimateH = buildClimateContent(climateContent, width);
		climateContent.setSize(width, fullClimateH);
		int maxPaneH = (int)(PixelScene.uiCamera.height - chrome.marginVer() - 20);

		//every tab scrolls: the hero tab alone is taller than a phone screen, and the
		//window is never taller than the screen it is on. Each pane is created BEFORE
		//its tab: pointer listeners fire newest first, so a pane's controller made
		//after the buttons under it would take their presses
		ScrollPane itemsScroll = new ScrollPane(new Component());
		ItemsTab items = new ItemsTab();
		items.setSize(width, 0);
		fill(itemsScroll, items, width);

		ScrollPane mobsScroll = new ScrollPane(new Component());
		MobsTab mobs = new MobsTab();
		mobs.setSize(width, 0);
		fill(mobsScroll, mobs, width);

		ScrollPane travelScroll = new ScrollPane(new Component());
		TravelTab travel = new TravelTab();
		travel.setSize(width, 0);
		fill(travelScroll, travel, width);

		ScrollPane heroScroll = new ScrollPane(new Component());
		HeroTab hero = new HeroTab();
		hero.setSize(width, 0);
		fill(heroScroll, hero, width);

		ScrollPane perfScroll = new ScrollPane(new Component());
		PerfTab perf = new PerfTab();
		perf.setSize(width, 0);
		fill(perfScroll, perf, width);

		float height = fullClimateH;
		height = Math.max(height, perf.height());
		height = Math.max(height, items.height());
		height = Math.max(height, mobs.height());
		height = Math.max(height, travel.height());
		height = Math.max(height, hero.height());
		height = Math.min(height, maxPaneH);

		// --- resize() BEFORE adding ScrollPane (required for correct camera positioning) ---
		resize(width, (int) Math.ceil(height));

		// --- Climate Tab ---
		add(climateScroll);
		climateScroll.setRect(0, 0, width, height);

		add(new IconTab(Icons.get(Icons.SEED)) {
			@Override
			protected void select(boolean value) {
				super.select(value);
				climateScroll.visible = climateScroll.active = value;
				if (value) lastTab = 0;
			}
		});

		// --- Items Tab ---
		add(itemsScroll);
		itemsScroll.setRect(0, 0, width, height);

		add(new IconTab(Icons.get(Icons.BACKPACK)) {
			@Override
			protected void select(boolean value) {
				super.select(value);
				itemsScroll.visible = itemsScroll.active = value;
				if (value) lastTab = 1;
			}
		});

		// --- Mobs Tab ---
		add(mobsScroll);
		mobsScroll.setRect(0, 0, width, height);

		add(new IconTab(Icons.get(Icons.SKULL)) {
			@Override
			protected void select(boolean value) {
				super.select(value);
				mobsScroll.visible = mobsScroll.active = value;
				if (value) lastTab = 2;
			}
		});

		// --- Travel Tab ---
		add(travelScroll);
		travelScroll.setRect(0, 0, width, height);

		add(new IconTab(Icons.get(Icons.DEPTH)) {
			@Override
			protected void select(boolean value) {
				super.select(value);
				travelScroll.visible = travelScroll.active = value;
				if (value) lastTab = 3;
			}
		});

		// --- Hero Tab ---
		add(heroScroll);
		heroScroll.setRect(0, 0, width, height);

		add(new IconTab(Icons.get(Icons.TALENT)) {
			@Override
			protected void select(boolean value) {
				super.select(value);
				heroScroll.visible = heroScroll.active = value;
				if (value) lastTab = 4;
			}
		});

		// --- Perf Tab ---
		add(perfScroll);
		perfScroll.setRect(0, 0, width, height);

		add(new IconTab(Icons.get(Icons.WARNING)) {
			@Override
			protected void select(boolean value) {
				super.select(value);
				perfScroll.visible = perfScroll.active = value;
				if (value) lastTab = 5;
			}
		});

		layoutTabs();
		select(lastTab);
	}

	/** a tab into the pane made for it, so it scrolls when it is taller than the window */
	private void fill(ScrollPane pane, Component tab, float width) {
		pane.content().add(tab);
		pane.content().setSize(width, tab.height());
	}

	// =====================================================================
	// Climate / Temperature Tab — builds content into a Component for ScrollPane
	// =====================================================================

	private float buildClimateContent(Component c, float width) {
		float pos = 0;
		CheckBox heatOverlay = new CheckBox("Heat overlay") {
			@Override protected void onClick() {
				super.onClick();
				xyz.gabriwar.warpedpixeldungeon.debug.HeatOverlay.enabled = checked();
			}
		};
		heatOverlay.checked(xyz.gabriwar.warpedpixeldungeon.debug.HeatOverlay.enabled);
		c.add(heatOverlay);
		heatOverlay.setRect(0, pos, width, BTN_HEIGHT);
		pos = heatOverlay.bottom() + GAP;
		RenderedTextBlock heatLegend = PixelScene.renderTextBlock(
				"Tile temperatures in C: blue = cold, orange/red = hot. Bars show freezing (blue) or melting (gold).", 5);
		heatLegend.maxWidth((int)width);
		c.add(heatLegend);
		heatLegend.setPos(0, pos);
		pos = heatLegend.bottom() + GAP;

		// --- Status readout ---
		float currentTemp = Dungeon.hero != null
				? TileTemperature.tileTemp(Dungeon.hero.pos)
				: ClimateManager.localTemp();
		String status = "Temp:" + fmt(currentTemp) + "C"
				+ "  Cloud:" + String.format("%.0f", ClimateManager.cloudCover() * 100) + "%"
				+ "  Wind:" + fmt(ClimateManager.localWindSpeed());
		RenderedTextBlock info = PixelScene.renderTextBlock(status, 5);
		info.hardlight(tempColor(currentTemp));
		c.add(info);
		info.setPos(0, pos);
		pos = info.bottom() + GAP;

		String status2 = DayNightCycle.phase().name()
				+ "  Moon:" + GameCalendar.moonPhase().name()
				+ "  Precip:" + String.format("%.0f", ClimateManager.localPrecipRate() * 100) + "%";
		RenderedTextBlock info2 = PixelScene.renderTextBlock(status2, 5);
		info2.hardlight(0xAAAACC);
		c.add(info2);
		info2.setPos(0, pos);
		pos = info2.bottom() + GAP;

		// --- Temperature override ---
		pos = addSliderOverride(c, width, "Temperature", "-40", "+60", -40, 60, pos,
				!Float.isNaN(ClimateManager.debugTempOverride),
				Float.isNaN(ClimateManager.debugTempOverride) ? Math.round(ClimateManager.localTemp()) : Math.round(ClimateManager.debugTempOverride),
				(enabled, val) -> ClimateManager.debugTempOverride = enabled ? val : Float.NaN);

		// --- Cloud cover override ---
		pos = addSliderOverride(c, width, "Clouds", "0%", "100%", 0, 10, pos,
				!Float.isNaN(ClimateManager.debugCloudOverride),
				Float.isNaN(ClimateManager.debugCloudOverride) ? Math.round(ClimateManager.cloudCover() * 10) : Math.round(ClimateManager.debugCloudOverride * 10),
				(enabled, val) -> ClimateManager.debugCloudOverride = enabled ? val / 10f : Float.NaN);

		// --- Wind override ---
		pos = addSliderOverride(c, width, "Wind", "0", "30", 0, 30, pos,
				!Float.isNaN(ClimateManager.debugWindOverride),
				Float.isNaN(ClimateManager.debugWindOverride) ? Math.round(ClimateManager.localWindSpeed()) : Math.round(ClimateManager.debugWindOverride),
				(enabled, val) -> ClimateManager.debugWindOverride = enabled ? val : Float.NaN);

		// --- Wind direction override (8 compass dirs: N/NE/E/SE/S/SW/W/NW = 0-7 → 0-315°) ---
		String[] compassNames = { "N", "NE", "E", "SE", "S", "SW", "W", "NW" };
		float curDir = Float.isNaN(ClimateManager.debugWindDirOverride)
				? ClimateManager.surfaceWindDir() : ClimateManager.debugWindDirOverride;
		int dirIdx = Math.round(curDir / 45f) % 8;
		pos = addSliderOverride(c, width, "Wind Dir: " + compassNames[dirIdx], "N", "NW", 0, 7, pos,
				!Float.isNaN(ClimateManager.debugWindDirOverride), dirIdx,
				(enabled, val) -> ClimateManager.debugWindDirOverride = enabled ? (val % 8) * 45f : Float.NaN);

		// --- Precipitation override ---
		pos = addSliderOverride(c, width, "Precip", "0%", "100%", 0, 10, pos,
				!Float.isNaN(ClimateManager.debugPrecipOverride),
				Float.isNaN(ClimateManager.debugPrecipOverride) ? Math.round(ClimateManager.localPrecipRate() * 10) : Math.round(ClimateManager.debugPrecipOverride * 10),
				(enabled, val) -> ClimateManager.debugPrecipOverride = enabled ? val / 10f : Float.NaN);

		// --- Precipitation type override ---
		PrecipType[] precipTypes = { PrecipType.RAIN, PrecipType.SNOW, PrecipType.HAIL, PrecipType.SLEET, PrecipType.BLIZZARD };
		PrecipType curPrecipType = ClimateManager.debugPrecipTypeOverride != null
				? ClimateManager.debugPrecipTypeOverride : ClimateManager.localPrecipType();
		int precipTypeIdx = 0;
		for (int i = 0; i < precipTypes.length; i++) {
			if (precipTypes[i] == curPrecipType) { precipTypeIdx = i; break; }
		}
		String precipTypeName = precipTypes[precipTypeIdx].name();
		pos = addSliderOverride(c, width, "Precip Type: " + precipTypeName, "Rain", "Blizzard", 0, 4, pos,
				ClimateManager.debugPrecipTypeOverride != null, precipTypeIdx,
				(enabled, val) -> ClimateManager.debugPrecipTypeOverride = enabled ? precipTypes[Math.max(0, Math.min(4, val))] : null);

		// --- Day Phase slider ---
		DayNightCycle.Phase[] phases = DayNightCycle.Phase.values();
		int phaseVal = DayNightCycle.debugPhaseOverride == null
				? DayNightCycle.phase().ordinal() : DayNightCycle.debugPhaseOverride.ordinal();
		String phaseName = phases[phaseVal].name();
		pos = addSliderOverride(c, width, "Day Phase: " + phaseName, "Dawn", "Night", 0, 3, pos,
				DayNightCycle.debugPhaseOverride != null, phaseVal,
				(enabled, val) -> DayNightCycle.debugPhaseOverride = enabled ? phases[Math.max(0, Math.min(3, val))] : null);

		// --- Moon Phase slider ---
		GameCalendar.MoonPhase[] moons = GameCalendar.MoonPhase.values();
		int moonVal = GameCalendar.debugMoonOverride == null
				? GameCalendar.moonPhase().ordinal() : GameCalendar.debugMoonOverride.ordinal();
		String moonName = moons[moonVal].name().replace("_", " ");
		pos = addSliderOverride(c, width, "Moon: " + moonName, "New", "Waning", 0, 7, pos,
				GameCalendar.debugMoonOverride != null, moonVal,
				(enabled, val) -> GameCalendar.debugMoonOverride = enabled ? moons[Math.max(0, Math.min(7, val))] : null);

		// --- Special Events ---
		RenderedTextBlock evtHeader = PixelScene.renderTextBlock("Special Events", 7);
		evtHeader.hardlight(0xCCCCCC);
		c.add(evtHeader);
		evtHeader.setPos(0, pos);
		pos = evtHeader.bottom() + GAP;

		float halfW = (width - GAP) / 2f;

		pos = addEventToggle(c, halfW, 0, pos, "Aurora",
				ClimateManager.isAurora(), () -> ClimateManager.debugToggleAurora());
		addEventToggle(c, halfW, halfW + GAP, pos - BTN_HEIGHT - GAP, "Rainbow",
				ClimateManager.isRainbow(), () -> ClimateManager.debugToggleRainbow());

		pos = addEventToggle(c, halfW, 0, pos, "Solar Eclipse",
				ClimateManager.isSolarEclipse(), () -> ClimateManager.debugToggleSolarEclipse());
		addEventToggle(c, halfW, halfW + GAP, pos - BTN_HEIGHT - GAP, "Lunar Eclipse",
				ClimateManager.isLunarEclipse(), () -> ClimateManager.debugToggleLunarEclipse());

		return pos;
	}

	private float addSliderOverride(Component c, float width, String name, String minLabel, String maxLabel,
			int minVal, int maxVal, float pos, boolean active, int curVal,
			SliderCallback callback) {
		CheckBox chk = new CheckBox("Override " + name) {
			@Override protected void onClick() {
				super.onClick();
				callback.apply(checked(), 0);
			}
		};
		chk.checked(active);
		c.add(chk);
		chk.setRect(0, pos, width, BTN_HEIGHT);
		pos = chk.bottom() + GAP;

		OptionSlider slider = new OptionSlider(name, minLabel, maxLabel, minVal, maxVal) {
			@Override protected void onChange() {
				callback.apply(true, getSelectedValue());
			}
		};
		slider.setSelectedValue(Math.max(minVal, Math.min(maxVal, curVal)));
		c.add(slider);
		slider.setRect(0, pos, width, SLIDER_HEIGHT);
		pos = slider.bottom() + GAP;

		return pos;
	}

	// =====================================================================
	// Items Tab — category buttons that open a scrollable picker
	// =====================================================================
	private class ItemsTab extends Component {

		@Override
		protected void createChildren() { }

		@Override
		protected void layout() {
			float pos = y;

			// All items button
			RedButton allBtn = new RedButton("All Items") {
				@Override
				protected void onClick() {
					hide();
					GameScene.show(WndDebugPicker.forAllItems());
				}
			};
			allBtn.textColor(Window.TITLE_COLOR);
			add(allBtn);
			allBtn.setRect(x, pos, width, BTN_HEIGHT);
			pos = allBtn.bottom() + GAP;

			RenderedTextBlock header = PixelScene.renderTextBlock("By Category", 7);
			header.hardlight(0xCCCCCC);
			add(header);
			header.setPos(x, pos);
			pos = header.bottom() + GAP;

			float halfW = (width - GAP) / 2f;
			Object[][] cats = {
					{"Weapon",   Generator.Category.WEAPON},
					{"Armor",    Generator.Category.ARMOR},
					{"Wand",     Generator.Category.WAND},
					{"Ring",     Generator.Category.RING},
					{"Artifact", Generator.Category.ARTIFACT},
					{"Potion",   Generator.Category.POTION},
					{"Scroll",   Generator.Category.SCROLL},
					{"Seed",     Generator.Category.SEED},
					{"Stone",    Generator.Category.STONE},
					{"Food",     Generator.Category.FOOD},
					{"Missile",  Generator.Category.MISSILE},
					{"Trinket",  Generator.Category.TRINKET},
			};

			for (int i = 0; i < cats.length; i += 2) {
				final Generator.Category cat1 = (Generator.Category) cats[i][1];
				RedButton btn1 = new RedButton((String) cats[i][0]) {
					@Override
					protected void onClick() {
						hide();
						GameScene.show(WndDebugPicker.forItemCategory(cat1));
					}
				};
				add(btn1);
				btn1.setRect(x, pos, halfW, BTN_HEIGHT);

				if (i + 1 < cats.length) {
					final Generator.Category cat2 = (Generator.Category) cats[i + 1][1];
					RedButton btn2 = new RedButton((String) cats[i + 1][0]) {
						@Override
						protected void onClick() {
							hide();
							GameScene.show(WndDebugPicker.forItemCategory(cat2));
						}
					};
					add(btn2);
					btn2.setRect(x + halfW + GAP, pos, halfW, BTN_HEIGHT);
				}

				pos += BTN_HEIGHT + GAP;
			}

			RenderedTextBlock header2 = PixelScene.renderTextBlock("Exotic / Spells", 7);
			header2.hardlight(0xCCCCCC);
			add(header2);
			header2.setPos(x, pos);
			pos = header2.bottom() + GAP;

			RedButton exoticPotBtn = new RedButton("Exotic Potions") {
				@Override protected void onClick() {
					hide();
					GameScene.show(WndDebugPicker.forExoticPotions());
				}
			};
			add(exoticPotBtn);
			exoticPotBtn.setRect(x, pos, width, BTN_HEIGHT);
			pos = exoticPotBtn.bottom() + GAP;

			RedButton exoticScrollBtn = new RedButton("Exotic Scrolls") {
				@Override protected void onClick() {
					hide();
					GameScene.show(WndDebugPicker.forExoticScrolls());
				}
			};
			add(exoticScrollBtn);
			exoticScrollBtn.setRect(x, pos, width, BTN_HEIGHT);
			pos = exoticScrollBtn.bottom() + GAP;

			RedButton spellsBtn = new RedButton("Spells") {
				@Override protected void onClick() {
					hide();
					GameScene.show(WndDebugPicker.forSpells());
				}
			};
			add(spellsBtn);
			spellsBtn.setRect(x, pos, width, BTN_HEIGHT);
			pos = spellsBtn.bottom() + GAP;

			RedButton enchantBtn = new RedButton("Enchant weapon / armor") {
				@Override protected void onClick() {
					hide();
					WndDebugPicker.forEnchanting();
				}
			};
			add(enchantBtn);
			enchantBtn.setRect(x, pos, width, BTN_HEIGHT);
			pos = enchantBtn.bottom() + GAP;

			height = pos - y;
		}
	}

	// =====================================================================
	// Mobs Tab — bestiary category buttons that open a scrollable picker
	// =====================================================================
	private class MobsTab extends Component {

		@Override
		protected void createChildren() { }

		@Override
		protected void layout() {
			float pos = y;

			//debug scenes: a whole test situation around the hero in one tap (DebugScenes)
			RedButton scenes = new RedButton("Scenes...") {
				@Override
				protected void onClick() {
					hide();
					final xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes.Scene[] all
							= xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes.SCENES;
					String[] titles = new String[all.length];
					for (int i = 0; i < all.length; i++) titles[i] = all[i].title();
					GameScene.show(new WndOptions("Debug scenes",
							"Sets a situation up around the hero: the hero gets infinite health and the fog is lifted. "
							+ "Also from the command line: ./gradlew :desktop:debug -Pscene=<id>\n\nIds: "
							+ xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes.ids(), titles) {
						@Override
						protected void onSelect(int index) {
							xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes.run(all[index]);
						}
					});
				}
			};
			scenes.textColor(0xFFDD88);
			add(scenes);
			scenes.setRect(x, pos, width, BTN_HEIGHT);
			pos = scenes.bottom() + GAP;

			// All mobs button
			RedButton allMobs = new RedButton("All Mobs") {
				@Override
				protected void onClick() {
					hide();
					GameScene.show(WndDebugPicker.forAllMobs());
				}
			};
			allMobs.textColor(Window.TITLE_COLOR);
			add(allMobs);
			allMobs.setRect(x, pos, width, BTN_HEIGHT);
			pos = allMobs.bottom() + GAP;

			// Weather blobs button
			RedButton weatherBtn = new RedButton("Weather / Blobs") {
				@Override
				protected void onClick() {
					hide();
					GameScene.show(WndDebugPicker.forWeatherBlobs());
				}
			};
			weatherBtn.textColor(0x44AAFF);
			add(weatherBtn);
			weatherBtn.setRect(x, pos, width, BTN_HEIGHT);
			pos = weatherBtn.bottom() + GAP * 2;

			RenderedTextBlock mobHeader = PixelScene.renderTextBlock("Spawn Mobs", 7);
			mobHeader.hardlight(0xCCCCCC);
			add(mobHeader);
			mobHeader.setPos(x, pos);
			pos = mobHeader.bottom() + GAP;

			Object[][] cats = {
					{"Regional",  Bestiary.REGIONAL},
					{"Bosses",    Bestiary.BOSSES},
					{"Universal", Bestiary.UNIVERSAL},
					{"Rare",      Bestiary.RARE},
					{"Quest",     Bestiary.QUEST},
					{"Neutral",   Bestiary.NEUTRAL},
			};

			float halfW = (width - GAP) / 2f;

			for (int i = 0; i < cats.length; i += 2) {
				final Bestiary b1 = (Bestiary) cats[i][1];
				RedButton btn1 = new RedButton((String) cats[i][0]) {
					@Override
					protected void onClick() {
						hide();
						GameScene.show(WndDebugPicker.forMobCategory(b1));
					}
				};
				add(btn1);
				btn1.setRect(x, pos, halfW, BTN_HEIGHT);

				if (i + 1 < cats.length) {
					final Bestiary b2 = (Bestiary) cats[i + 1][1];
					RedButton btn2 = new RedButton((String) cats[i + 1][0]) {
						@Override
						protected void onClick() {
							hide();
							GameScene.show(WndDebugPicker.forMobCategory(b2));
						}
					};
					add(btn2);
					btn2.setRect(x + halfW + GAP, pos, halfW, BTN_HEIGHT);
				}

				pos += BTN_HEIGHT + GAP;
			}

			height = pos - y;
		}

	}

	// =====================================================================
	// Perf Tab: the profiler and the lag detector
	// =====================================================================

	private class PerfTab extends Component {

		private RedButton profBtn, lagToggle, jfrBtn;

		@Override
		protected void createChildren() { }

		private void showReport( String title, String report ){
			if (report == null){ GLog.w( "Nothing recorded yet." ); return; }
			GameScene.show( new WndTitledMessage( Icons.get( Icons.WARNING ), title, report ) );
		}

		private String profLabel(){
			return xyz.gabriwar.warpedpixeldungeon.debug.Profiler.running
					? String.format( java.util.Locale.ROOT, "Stop profiler (%.0fs)", xyz.gabriwar.warpedpixeldungeon.debug.Profiler.seconds() )
					: "Start profiler";
		}

		private String lagLabel(){
			return "Lag detector: " + (xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.enabled ? "ON" : "OFF");
		}

		private String chunkLabel(){
			return "Chunked tilemaps: " + (com.watabou.noosa.Tilemap.chunked ? "ON" : "OFF") + " (next level)";
		}

		private String jfrLabel(){
			return xyz.gabriwar.warpedpixeldungeon.debug.Profiler.jfrRunning() ? "Stop flight recording" : "Start flight recording (JFR)";
		}

		@Override
		public synchronized void update() {
			super.update();
			if (profBtn != null && xyz.gabriwar.warpedpixeldungeon.debug.Profiler.running) profBtn.text( profLabel() );
		}

		@Override
		protected void layout() {
			float pos = y;
			float halfW = (width - GAP) / 2f;

			RenderedTextBlock info = PixelScene.renderTextBlock(
					"The profiler samples the render and actor threads every 4 ms while it runs: play normally, then stop it and read where the time went. "
					+ "Frames, GL draw calls, heap, hot paths and slowest actors are in the same report.", 6 );
			info.maxWidth( (int) width );
			info.setPos( x, pos );
			add( info );
			pos = info.bottom() + GAP;

			profBtn = new RedButton( profLabel() ) {
				@Override protected void onClick() {
					if (xyz.gabriwar.warpedpixeldungeon.debug.Profiler.running){
						String report = xyz.gabriwar.warpedpixeldungeon.debug.Profiler.stop();
						text( profLabel() );
						showReport( "Profile", report );
					} else {
						xyz.gabriwar.warpedpixeldungeon.debug.Profiler.start();
						text( profLabel() );
						hide();
					}
				}
			};
			profBtn.textColor( Window.TITLE_COLOR );
			add( profBtn );
			profBtn.setRect( x, pos, width, BTN_HEIGHT );
			pos = profBtn.bottom() + GAP;

			RedButton profShow = new RedButton( "Show last profile" ) {
				@Override protected void onClick() { showReport( "Profile", xyz.gabriwar.warpedpixeldungeon.debug.Profiler.lastReport() ); }
			};
			add( profShow );
			profShow.setRect( x, pos, halfW, BTN_HEIGHT );
			RedButton profSave = new RedButton( "Save last profile" ) {
				@Override protected void onClick() {
					String r = xyz.gabriwar.warpedpixeldungeon.debug.Profiler.lastReport();
					if (r == null) GLog.w( "Nothing recorded yet." );
					else xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.save( "profile", r );
				}
			};
			add( profSave );
			profSave.setRect( x + halfW + GAP, pos, halfW, BTN_HEIGHT );
			pos = profSave.bottom() + GAP;

			if (xyz.gabriwar.warpedpixeldungeon.debug.Profiler.jfrAvailable()){
				jfrBtn = new RedButton( jfrLabel() ) {
					@Override protected void onClick() {
						if (xyz.gabriwar.warpedpixeldungeon.debug.Profiler.jfrRunning()){
							String path = xyz.gabriwar.warpedpixeldungeon.debug.Profiler.jfrStop();
							text( jfrLabel() );
							if (path != null) showReport( "Flight recording", "Saved to:\n" + path + "\n\nOpen it with JDK Mission Control (jmc) for method-level flame graphs, GC, allocation and lock profiles." );
						} else {
							xyz.gabriwar.warpedpixeldungeon.debug.Profiler.jfrStart();
							text( jfrLabel() );
							hide();
						}
					}
				};
				add( jfrBtn );
				jfrBtn.setRect( x, pos, width, BTN_HEIGHT );
				pos = jfrBtn.bottom() + GAP;
			}

			//the renderer's chunked tilemaps (culled, rebuilt per 16x16 block); the legacy
			//single-buffer path is kept for comparison and takes effect on the next level load
			RedButton chunkBtn = new RedButton( chunkLabel() ) {
				@Override protected void onClick() {
					com.watabou.noosa.Tilemap.chunked = !com.watabou.noosa.Tilemap.chunked;
					text( chunkLabel() );
					GLog.i( "Tilemap chunking " + (com.watabou.noosa.Tilemap.chunked ? "on" : "off") + " from the next level load." );
				}
			};
			add( chunkBtn );
			chunkBtn.setRect( x, pos, width, BTN_HEIGHT );
			pos = chunkBtn.bottom() + GAP;

			pos += GAP;
			lagToggle = new RedButton( lagLabel() ) {
				@Override protected void onClick() {
					xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.setEnabled( !xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.enabled );
					text( lagLabel() );
				}
			};
			add( lagToggle );
			lagToggle.setRect( x, pos, width, BTN_HEIGHT );
			pos = lagToggle.bottom() + GAP;

			RedButton lagNow = new RedButton( "Lag snapshot now" ) {
				@Override protected void onClick() {
					boolean was = xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.enabled;
					if (!was) xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.setEnabled( true );
					String report = xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.capture( "manual snapshot from the debug menu", false );
					if (!was) xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.setEnabled( false );
					showReport( "Lag snapshot", report );
				}
			};
			add( lagNow );
			lagNow.setRect( x, pos, halfW, BTN_HEIGHT );
			RedButton lagLast = new RedButton( "Save last snapshot" ) {
				@Override protected void onClick() {
					String last = xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.lastReport();
					if (last == null) GLog.w( "No lag snapshot captured yet." );
					else xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor.save( last );
				}
			};
			add( lagLast );
			lagLast.setRect( x + halfW + GAP, pos, halfW, BTN_HEIGHT );
			pos = lagLast.bottom() + GAP;

			height = pos - y;
		}
	}

	// =====================================================================
	// Hero Tab
	// =====================================================================
	private class HeroTab extends Component {

		@Override
		protected void createChildren() { }

		@Override
		protected void layout() {
			float pos = y;

			if (Dungeon.hero == null) {
				height = 0;
				return;
			}

			Hero hero = Dungeon.hero;

			RenderedTextBlock header = PixelScene.renderTextBlock("Hero Stats", 7);
			header.hardlight(0xCCCCCC);
			add(header);
			header.setPos(x, pos);
			pos = header.bottom() + GAP;

			Sleepiness tired = hero.buff(Sleepiness.class);
			int sleepLvl = tired != null ? (int)tired.level() : 0;

			RenderedTextBlock stats = PixelScene.renderTextBlock(
					"HP: " + hero.HP + "/" + hero.HT
					+ "  STR: " + hero.STR()
					+ "  Lvl: " + hero.lvl
					+ "  Exp: " + hero.exp
					+ "\nSleepiness: " + sleepLvl + "/" + (int)Sleepiness.COMATOSE
					+ "  (drowsy@" + (int)Sleepiness.DROWSY + ")", 6);
			stats.hardlight(0xAAFFAA);
			add(stats);
			stats.setPos(x, pos);
			pos = stats.bottom() + GAP;

			//debug builds mark every guide page read at startup, which is exactly what
			//hides the guidebook, its trail and the searching page: back to first-run
			pos = addHeroBtn("Reset Tutorial", pos, () -> {
				WPDSettings.intro(true);
				for (String page : Document.ADVENTURERS_GUIDE.pageNames()) {
					Document.ADVENTURERS_GUIDE.deletePage(page);
				}
				//to disk now: a restart would otherwise bring the read pages back
				Journal.saveGlobal();
				GLog.p("Tutorial reset: the guidebook returns in the next new game.");
			});
			//online play asks the relay for a supporter receipt. This is the relay's own
			//test secret, typed in here rather than shipped: an empty box is the shipped
			//state, and the relay refuses anything that is not the token it was started
			//with. Rotating it there revokes every device at once
			pos = addHeroBtn("Relay Dev Token", pos, () -> GameScene.show(new WndTextInput(
					"Relay dev token",
					"Stands in for a supporter receipt, and only on a relay started with the "
							+ "same secret. Leave empty to go back to using the store receipt.",
					WPDSettings.relayDevToken(), 128, false, "Save", "Cancel") {
				@Override
				public void onSelect(boolean positive, String text) {
					if (!positive) return;
					WPDSettings.relayDevToken(text.trim());
					GLog.p(text.trim().isEmpty() ? "Relay dev token cleared." : "Relay dev token set.");
				}
			}));
			pos = addHeroBtn("Supporter Thanks T1", pos, () -> GameScene.show(new WndSupporterThanks(1, "Tier 1")));
			pos = addHeroBtn("Supporter Thanks T2", pos, () -> GameScene.show(new WndSupporterThanks(2, "Tier 2")));
			pos = addHeroBtn("Supporter Thanks T3", pos, () -> GameScene.show(new WndSupporterThanks(3, "Tier 3")));
			//the real purchase path: the thank-you is written down, shown, and then the
			//scene is rebuilt the way the store sheet closing rebuilds it (a resize resets
			//the scene). The window must come straight back on the rebuilt scene
			pos = addHeroBtn("Thanks T3 + scene reset", pos, () -> {
				WndSupporterThanks.request(3, "Tier 3 (test)");
				Game.scene().add(new com.watabou.noosa.tweeners.Delayer(0.6f) {
					@Override
					protected void onComplete() {
						super.onComplete();
						Game.resetScene();
					}
				});
			});
			hero.migrateDebugGodmode();
			pos = addHeroBtn("Full Heal", pos, () -> hero.HP = hero.HT);
			pos = addHeroRefreshBtn("Infinite health: " + (hero.debugInfiniteHealth ? "ON" : "OFF"), pos, () -> {
				hero.debugInfiniteHealth = !hero.debugInfiniteHealth;
				if (hero.debugInfiniteHealth) hero.HP = hero.HT;
			});
			pos = addHeroBtn("+5 Strength", pos, () -> hero.STR += 5);
			pos = addHeroBtn("+5 Levels", pos, () -> {
				for (int i = 0; i < 5; i++) {
					hero.earnExp(hero.maxExp(), WndDebug.class);
				}
			});
			pos = addHeroBtn("Satisfy Hunger", pos, () -> {
				Hunger hunger = hero.buff(Hunger.class);
				if (hunger != null) hunger.satisfy(Hunger.STARVING);
			});

			//sleepiness controls — rebuild the window so the readout updates
			pos = addHeroRefreshBtn("Max Sleepiness", pos, () -> {
				Sleepiness s = Buff.affect(hero, Sleepiness.class);
				s.affectSleep(Sleepiness.COMATOSE);
			});
			pos = addHeroRefreshBtn("Make Drowsy", pos, () -> {
				Sleepiness s = Buff.affect(hero, Sleepiness.class);
				s.wake(Sleepiness.COMATOSE);       // reset to 0...
				s.affectSleep(Sleepiness.DROWSY);  // ...then up to the drowsy threshold
			});
			pos = addHeroRefreshBtn("Clear Sleepiness", pos, () -> {
				Sleepiness s = Buff.affect(hero, Sleepiness.class);
				s.wake(Sleepiness.COMATOSE);
			});

			pos = addValueSlider("Gold", () -> Dungeon.gold, v -> Dungeon.gold = v, pos);
			pos = addValueSlider("Alchemy energy", () -> Dungeon.energy, v -> Dungeon.energy = v, pos);
			pos = addValueSlider("Skill points",
					() -> xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill.availableSkill,
					v -> xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill.availableSkill = v, pos);
			pos = addHeroRefreshBtn("Max skill tree", pos, () -> {
				xyz.gabriwar.warpedpixeldungeon.debug.SkillDebug.maxTree(hero);
				GLog.p("Current skill tree fully upgraded; all paths unlocked.");
			});
			pos = addHeroRefreshBtn(hero.debugAllSkillPaths ? "All skill paths: ON" : "Unlock all skill paths", pos, () -> {
				hero.debugAllSkillPaths = !hero.debugAllSkillPaths;
				GLog.p(hero.debugAllSkillPaths ? "All skill paths unlocked, including exclusive choices."
						: "Normal skill path restrictions restored.");
			});

			pos = addHeroBtn("Reveal Level", pos, () -> {
				for (int i = 0; i < Dungeon.level.length(); i++){
					Dungeon.level.visited[i] = Dungeon.level.mapped[i] = true;
				}
				GameScene.updateFog();
				Dungeon.observe();
			});

			boolean noFog = Dungeon.debugNoFog;
			RedButton fogBtn = new RedButton(noFog ? "No Fog of War (ON)" : "No Fog of War") {
				@Override
				protected void onClick() {
					Dungeon.debugNoFog = !Dungeon.debugNoFog;
					Dungeon.observe();
					hide();
					GameScene.show(new WndDebug());
				}
			};
			if (noFog) fogBtn.textColor(0x44FF44);
			add(fogBtn);
			fogBtn.setRect(x, pos, width, BTN_HEIGHT);
			pos = fogBtn.bottom() + GAP;

			boolean invisOn = Dungeon.debugInvisible;
			RedButton invisBtn = new RedButton(invisOn ? "Invisibility (ON)" : "Invisibility (infinite)") {
				@Override
				protected void onClick() {
					Dungeon.debugInvisible = !Dungeon.debugInvisible;
					if (Dungeon.debugInvisible) {
						Buff.affect(hero, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility.class, 1_000_000_000f);
					} else {
						xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility.dispel(hero);
					}
					hide();
					GameScene.show(new WndDebug());
				}
			};
			if (invisOn) invisBtn.textColor(0x44FF44);
			add(invisBtn);
			invisBtn.setRect(x, pos, width, BTN_HEIGHT);
			pos = invisBtn.bottom() + GAP;

			boolean ohkOn = Dungeon.debugOneHitKill;
			RedButton ohkBtn = new RedButton(ohkOn ? "One Hit Kill (ON)" : "One Hit Kill") {
				@Override
				protected void onClick() {
					Dungeon.debugOneHitKill = !Dungeon.debugOneHitKill;
					hide();
					GameScene.show(new WndDebug());
				}
			};
			if (ohkOn) ohkBtn.textColor(0x44FF44);
			add(ohkBtn);
			ohkBtn.setRect(x, pos, width, BTN_HEIGHT);
			pos = ohkBtn.bottom() + GAP;

			boolean manaOn = Dungeon.debugInfiniteMana;
			RedButton manaBtn = new RedButton(manaOn ? "Infinite Mana (ON)" : "Infinite Mana") {
				@Override
				protected void onClick() {
					Dungeon.debugInfiniteMana = !Dungeon.debugInfiniteMana;
					if (Dungeon.debugInfiniteMana) hero.MP = hero.MT;
					hide();
					GameScene.show(new WndDebug());
				}
			};
			if (manaOn) manaBtn.textColor(0x44FF44);
			add(manaBtn);
			manaBtn.setRect(x, pos, width, BTN_HEIGHT);
			pos = manaBtn.bottom() + GAP;

			pos = addHeroBtnPair("Identify All", () -> {
				for (Item item : hero.belongings) {
					item.identify();
				}
			}, "Fill Mob Drops", () -> {
				for (Bestiary b : Bestiary.values()) {
					for (Class<?> cls : b.entities()) {
						for (int slot = 0; slot < 8; slot++) {
							Bestiary.setLootSlotSeen(cls, slot);
						}
					}
				}
			}, pos);

			pos = addHeroBtnPair("Fill Journal", () -> {
				for (Catalog cat : Catalog.values()) {
					for (Class<?> cls : cat.items()) {
						Catalog.setSeen(cls);
					}
				}
				for (Bestiary b : Bestiary.values()) {
					for (Class<?> cls : b.entities()) {
						Bestiary.setSeen(cls);
					}
				}
				for (Document doc : Document.values()) {
					for (String page : doc.pageNames()) {
						doc.findPage(page);
						doc.readPage(page);
					}
				}
				// Journal.saveNeeded set internally by Catalog/Bestiary/Document
			}, "Clear Journal", () -> {
				for (Document doc : Document.values()) {
					for (String page : doc.pageNames()) {
						doc.deletePage(page);
					}
				}
				// Journal.saveNeeded set internally by Catalog/Bestiary/Document
			}, pos);

			pos = addHeroBtnPair("Fill Guide", () -> {
				for (String key : GuideGraph.allKeys()) {
					GuideProgress.findPage(key);
				}
			}, "Clear Guide", () -> GuideProgress.clear(), pos);

			height = pos - y;
		}

		private float addHeroBtn(String label, float pos, Runnable action) {
			RedButton btn = new RedButton(label) {
				@Override
				protected void onClick() {
					action.run();
				}
			};
			add(btn);
			btn.setRect(x, pos, width, BTN_HEIGHT);
			return btn.bottom() + GAP;
		}

		//two half-width buttons on one row, to keep the tab short
		//the amounts a value slider steps through: every decade and its half, up to a million
		private final int[] VALUE_STEPS = { 0, 10, 50, 100, 500, 1000, 5000, 10000, 50000, 100000, 1000000 };

		//a slider that SETS a value (gold, energy) to one of VALUE_STEPS; its title shows the amount
		private float addValueSlider(String name, java.util.function.IntSupplier get,
				java.util.function.IntConsumer set, float pos) {
			int now = get.getAsInt();
			int start = 0;
			for (int i = 0; i < VALUE_STEPS.length; i++) if (VALUE_STEPS[i] <= now) start = i;
			final OptionSlider[] slider = new OptionSlider[1];
			slider[0] = new OptionSlider(name + ": " + now, "0", "1M", 0, VALUE_STEPS.length - 1) {
				@Override
				protected void onChange() {
					int v = VALUE_STEPS[getSelectedValue()];
					set.accept(v);
					slider[0].setTitle(name + ": " + v);
				}
			};
			slider[0].setSelectedValue(start);
			add(slider[0]);
			slider[0].setRect(x, pos, width, 21);
			return slider[0].bottom() + GAP;
		}

		private float addHeroBtnPair(String labelA, Runnable actionA, String labelB, Runnable actionB, float pos) {
			float halfW = (width - GAP) / 2f;
			RedButton btnA = new RedButton(labelA) {
				@Override
				protected void onClick() {
					actionA.run();
				}
			};
			add(btnA);
			btnA.setRect(x, pos, halfW, BTN_HEIGHT);

			RedButton btnB = new RedButton(labelB) {
				@Override
				protected void onClick() {
					actionB.run();
				}
			};
			add(btnB);
			btnB.setRect(x + halfW + GAP, pos, halfW, BTN_HEIGHT);

			return btnA.bottom() + GAP;
		}

		//like addHeroBtn but reopens the window afterwards so live readouts refresh
		private float addHeroRefreshBtn(String label, float pos, Runnable action) {
			RedButton btn = new RedButton(label) {
				@Override
				protected void onClick() {
					action.run();
					hide();
					GameScene.show(new WndDebug());
				}
			};
			add(btn);
			btn.setRect(x, pos, width, BTN_HEIGHT);
			return btn.bottom() + GAP;
		}
	}

	// =====================================================================
	// Travel Tab — teleport within or between levels
	// =====================================================================
	private class TravelTab extends Component {

		@Override
		protected void createChildren() { }

		@Override
		protected void layout() {
			float pos = y;

			RenderedTextBlock header = PixelScene.renderTextBlock(
					"Current: depth " + Dungeon.depth + " branch " + Dungeon.branch, 6);
			header.hardlight(0xAAFFAA);
			add(header);
			header.setPos(x, pos);
			pos = header.bottom() + GAP;

			RedButton teleport = new RedButton("Teleport to Cursor") {
				@Override
				protected void onClick() {
					WndDebug.this.hide();
					GameScene.selectCell(new CellSelector.Listener() {
						@Override
						public void onSelect(Integer cell) {
							if (cell == null || Dungeon.level == null || Dungeon.hero == null) return;
							Hero hero = Dungeon.hero;
							if (cell < 0 || cell >= Dungeon.level.length()
									|| !(Dungeon.level.passable[cell] || (hero.flying && Dungeon.level.avoid[cell]))
									|| (Actor.findChar(cell) != null && Actor.findChar(cell) != hero)) {
								GLog.w("Choose an open, unoccupied tile.");
								return;
							}
							if (cell == hero.pos) return;
							hero.interrupt();
							Buff.detach(hero, Roots.class);
							// Debug travel deliberately bypasses teleport pathfinding restrictions.
							ScrollOfTeleportation.appear(hero, cell);
							Dungeon.level.occupyCell(hero);
							Dungeon.observe();
							GameScene.updateFog();
						}

						@Override
						public String prompt() {
							return "Click or tap a tile to teleport.";
						}
					});
				}
			};
			add(teleport);
			teleport.setRect(x, pos, width, BTN_HEIGHT);
			pos = teleport.bottom() + GAP;

			RedButton allLevels = new RedButton("All Levels") {
				@Override
				protected void onClick() {
					hide();
					GameScene.show(WndDebugPicker.forTravel());
				}
			};
			allLevels.textColor(Window.TITLE_COLOR);
			add(allLevels);
			allLevels.setRect(x, pos, width, BTN_HEIGHT);
			pos = allLevels.bottom() + GAP;

			RedButton roomsShowcase = new RedButton("Rooms Showcase (all rooms + signs)") {
				@Override
				protected void onClick() {
					hide();
					if (Dungeon.depth == 85){
						//already here: regenerate the showcase from scratch
						InterlevelScene.mode = InterlevelScene.Mode.RESET;
					} else {
						InterlevelScene.mode = InterlevelScene.Mode.RETURN;
						InterlevelScene.returnDepth = 85;
						InterlevelScene.returnBranch = 0;
						InterlevelScene.returnPos = -1;
					}
					Game.switchScene(InterlevelScene.class);
				}
			};
			roomsShowcase.textColor(Window.TITLE_COLOR);
			add(roomsShowcase);
			roomsShowcase.setRect(x, pos, width, BTN_HEIGHT);
			pos = roomsShowcase.bottom() + GAP;

			RedButton overworld = new RedButton("Overworld (infinite world, WIP)") {
				@Override
				protected void onClick() {
					hide();
					if (Dungeon.depth == 97){
						InterlevelScene.mode = InterlevelScene.Mode.RESET;
					} else {
						InterlevelScene.mode = InterlevelScene.Mode.RETURN;
						InterlevelScene.returnDepth = 97;
						InterlevelScene.returnBranch = 0;
						InterlevelScene.returnPos = -1;
					}
					Game.switchScene(InterlevelScene.class);
				}
			};
			overworld.textColor(Window.TITLE_COLOR);
			add(overworld);
			overworld.setRect(x, pos, width, BTN_HEIGHT);
			pos = overworld.bottom() + GAP;
			//the world's slices: one up or one down from here, on the same world cell
			for (final int step : new int[]{ 1, -1 }){
				RedButton slice = new RedButton(step > 0 ? "World slice up (+1)" : "World slice down (-1)") {
					@Override
					protected void onClick() {
						hide();
						if (!(Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel)) return;
						xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel ow
								= (xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level;
						int dest = ow.altitude() + step;
						if (!xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers.exists(dest)) return;
						ow.travelToSlice(Dungeon.hero, dest);
					}
				};
				slice.textColor(Window.TITLE_COLOR);
				add(slice);
				slice.setRect(x, pos, width, BTN_HEIGHT);
				pos = slice.bottom() + GAP;
			}
			height = pos - y;
		}
	}

	private float addEventToggle(Component c, float w, float xOff, float pos,
	                             String label, boolean active, Runnable toggle) {
		RedButton btn = new RedButton(active ? label + " (ON)" : label) {
			@Override
			protected void onClick() {
				toggle.run();
				hide();
				GameScene.show(new WndDebug());
			}
		};
		if (active) btn.textColor(0x44FF44);
		c.add(btn);
		btn.setRect(xOff, pos, w, BTN_HEIGHT);
		return btn.bottom() + GAP;
	}

	private interface SliderCallback {
		void apply(boolean enabled, int value);
	}

	// =====================================================================
	// Utility
	// =====================================================================

	private static String fmt(float v) {
		return String.format("%.1f", v);
	}

	private static int tempColor(float temp) {
		if (temp <= -10f) return 0x4488FF;
		if (temp <= 0f)   return 0x88BBFF;
		if (temp <= 15f)  return 0xCCCCCC;
		if (temp <= 30f)  return 0xFFCC44;
		return 0xFF4422;
	}
}
