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

package xyz.gabriwar.warpedpixeldungeon.debug;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleepiness;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.journal.Bestiary;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.journal.Document;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideProgress;
import xyz.gabriwar.warpedpixeldungeon.journal.Journal;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIcon;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndDebugPicker;
import xyz.gabriwar.warpedpixeldungeon.windows.WndDebugReport;
import xyz.gabriwar.warpedpixeldungeon.windows.WndDebugScenes;
import xyz.gabriwar.warpedpixeldungeon.windows.WndDebugSkills;
import xyz.gabriwar.warpedpixeldungeon.windows.WndSupporterThanks;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTextInput;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.Tilemap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Everything the debug window holds, tab by tab and section by section, as plain data: each
 * control's label and icon, how it reads its state and what it does. WndDebug only lays these
 * out and keeps them in step with the game; a test can list them without a screen.
 */
public final class DebugMenu {

	private DebugMenu(){}

	public enum Kind {
		HEADER,   //a section title
		TEXT,     //a fixed note
		READOUT,  //live text
		ACTION,   //a button
		TOGGLE,   //on/off: the tick and a green label show ON, the same everywhere
		OVERRIDE, //a tick and a slider: holds a climate value at the slider's position while ticked
		VALUE     //a slider that sets an amount (gold, energy, skill points)
	}

	/** Every control that is on shows it the same way: this green, and a ticked box. */
	public static final int ON_COLOR = 0x44FF44;

	public static class Control {
		public final Kind kind;
		public final String label;
		public Supplier<Image> icon;
		//beside the next control even in a narrow window; on a wide one everything pairs up
		public boolean half;
		//asked before the action runs: the window goes first (it opens a list, a picker, the map)
		public BooleanSupplier closes = () -> false;
		//asked once, when the window is built
		public BooleanSupplier shown = () -> true;
		//asked live: a greyed-out control cannot be pressed
		public BooleanSupplier enabled = () -> true;
		public Runnable action;        //ACTION; for a TOGGLE, the flip
		public BooleanSupplier on;     //TOGGLE, OVERRIDE
		public Supplier<String> text;  //READOUT text; TOGGLE suffix; OVERRIDE and VALUE slider title
		public IntSupplier color;      //READOUT tint
		public int textSize = 6;       //TEXT, READOUT
		public int min, max;           //OVERRIDE, VALUE: the slider's range
		public String minLabel = "", maxLabel = "";
		public IntSupplier value;      //OVERRIDE, VALUE: where the slider stands
		public IntConsumer set;        //OVERRIDE: hold that position; VALUE: that position's amount
		public Runnable clear;         //OVERRIDE: back to the live value

		Control( Kind kind, String label ){
			this.kind = kind;
			this.label = label;
		}

		Control icon( Supplier<Image> icon ){ this.icon = icon; return this; }
		Control half(){ half = true; return this; }
		Control closing(){ closes = () -> true; return this; }
		Control shownIf( BooleanSupplier s ){ shown = s; return this; }
		Control enabledIf( BooleanSupplier s ){ enabled = s; return this; }
		Control suffix( Supplier<String> s ){ text = s; return this; }

		public boolean isOn(){
			return on != null && on.getAsBoolean();
		}

		/** An override's tick: unticking goes back to the live value, ticking holds the slider's. */
		public void tick( int sliderPos ){
			if (isOn()) clear.run();
			else set.accept( sliderPos );
		}

		/** A slider let go: an override holds that position (and its tick comes on with it). */
		public void slide( int sliderPos ){
			set.accept( Math.max( min, Math.min( max, sliderPos ) ) );
		}

		@Override
		public String toString(){
			return kind + " " + label;
		}
	}

	public static final class Tab {
		public final String title;
		public final Icons icon;
		public final List<Control> controls = new ArrayList<>();

		Tab( String title, Icons icon ){
			this.title = title;
			this.icon = icon;
		}

		Control add( Control c ){
			controls.add( c );
			return c;
		}

		Control header( String title, Supplier<Image> icon ){
			return add( new Control( Kind.HEADER, title ).icon( icon ) );
		}

		Control text( String text, int size ){
			Control c = add( new Control( Kind.TEXT, text ) );
			c.textSize = size;
			return c;
		}

		Control readout( String label, Supplier<String> text, IntSupplier color ){
			Control c = add( new Control( Kind.READOUT, label ) );
			c.text = text;
			c.color = color;
			return c;
		}

		Control action( String label, Runnable action ){
			Control c = add( new Control( Kind.ACTION, label ) );
			c.action = action;
			return c;
		}

		Control toggle( String label, BooleanSupplier on, Runnable flip ){
			Control c = add( new Control( Kind.TOGGLE, label ) );
			c.on = on;
			c.action = flip;
			return c;
		}
	}

	// ------------------------------------------------------------------ the tabs

	public static List<Tab> tabs(){
		ArrayList<Tab> tabs = new ArrayList<>();
		tabs.add( climate() );
		tabs.add( items() );
		tabs.add( mobs() );
		tabs.add( travel() );
		tabs.add( hero() );
		tabs.add( perf() );
		tabs.add( journal() );
		return tabs;
	}

	// ------------------------------------------------------------------ climate

	static final String[] COMPASS = { "N", "NE", "E", "SE", "S", "SW", "W", "NW" };
	static final PrecipType[] PRECIP_TYPES = { PrecipType.RAIN, PrecipType.SNOW, PrecipType.HAIL, PrecipType.SLEET, PrecipType.BLIZZARD };

	private static Tab climate(){
		Tab t = new Tab( "Climate", Icons.SEED );

		t.header( "Here and now", icon( Icons.COMPASS ) );
		t.readout( "Temperature, clouds and wind", () -> {
			float temp = heroTemp();
			return "Temp " + fmt( temp ) + "C  Cloud " + pct( ClimateManager.cloudCover() )
					+ "  Wind " + fmt( ClimateManager.localWindSpeed() );
		}, () -> tempColor( heroTemp() ) ).textSize = 5;
		t.readout( "Day phase, moon and precipitation", () -> DayNightCycle.phase().name()
				+ "  Moon " + pretty( GameCalendar.moonPhase().name() )
				+ "  Precip " + pct( ClimateManager.localPrecipRate() ), () -> 0xAAAACC ).textSize = 5;

		t.header( "Map overlay", small( BuffIndicator.FIRE ) );
		t.toggle( "Heat overlay", () -> HeatOverlay.enabled, () -> HeatOverlay.enabled = !HeatOverlay.enabled );
		t.text( "Tile temperatures in C: blue = cold, orange/red = hot. Bars show freezing (blue) or melting (gold).", 5 );

		t.header( "Weather overrides", small( BuffIndicator.WINDSWEPT ) );
		override( t, "Temperature", -40, 60, "-40", "+60",
				() -> !Float.isNaN( ClimateManager.debugTempOverride ),
				() -> Math.round( ClimateManager.localTemp() ),
				() -> "Temperature: " + (Float.isNaN( ClimateManager.debugTempOverride )
						? fmt( ClimateManager.localTemp() ) : Integer.toString( Math.round( ClimateManager.debugTempOverride ) )) + "C",
				v -> ClimateManager.debugTempOverride = v,
				() -> ClimateManager.debugTempOverride = Float.NaN );
		override( t, "Clouds", 0, 10, "0%", "100%",
				() -> !Float.isNaN( ClimateManager.debugCloudOverride ),
				() -> Math.round( ClimateManager.cloudCover() * 10 ),
				() -> "Clouds: " + pct( ClimateManager.cloudCover() ),
				v -> ClimateManager.debugCloudOverride = v / 10f,
				() -> ClimateManager.debugCloudOverride = Float.NaN );
		override( t, "Wind", 0, 30, "0", "30",
				() -> !Float.isNaN( ClimateManager.debugWindOverride ),
				() -> Math.round( ClimateManager.localWindSpeed() ),
				() -> "Wind: " + fmt( ClimateManager.localWindSpeed() ),
				v -> ClimateManager.debugWindOverride = v,
				() -> ClimateManager.debugWindOverride = Float.NaN );
		override( t, "Wind direction", 0, 7, "N", "NW",
				() -> !Float.isNaN( ClimateManager.debugWindDirOverride ),
				() -> compassIndex( ClimateManager.surfaceWindDir() ),
				() -> "Wind dir: " + COMPASS[compassIndex( ClimateManager.surfaceWindDir() )],
				v -> ClimateManager.debugWindDirOverride = (v % 8) * 45f,
				() -> ClimateManager.debugWindDirOverride = Float.NaN );
		override( t, "Precipitation", 0, 10, "0%", "100%",
				() -> !Float.isNaN( ClimateManager.debugPrecipOverride ),
				() -> Math.round( ClimateManager.localPrecipRate() * 10 ),
				() -> "Precip: " + pct( ClimateManager.localPrecipRate() ),
				v -> ClimateManager.debugPrecipOverride = v / 10f,
				() -> ClimateManager.debugPrecipOverride = Float.NaN );
		override( t, "Precipitation type", 0, PRECIP_TYPES.length - 1, "Rain", "Blizzard",
				() -> ClimateManager.debugPrecipTypeOverride != null,
				() -> precipIndex( ClimateManager.localPrecipType() ),
				() -> "Precip type: " + pretty( ClimateManager.localPrecipType().name() ),
				v -> ClimateManager.debugPrecipTypeOverride = PRECIP_TYPES[v],
				() -> ClimateManager.debugPrecipTypeOverride = null );
		t.toggle( "Force storm", () -> ClimateManager.debugForceStorm,
				() -> ClimateManager.debugForceStorm = !ClimateManager.debugForceStorm )
				.icon( buff( BuffIndicator.VERTIGO ) );

		t.header( "Time overrides", icon( Icons.SLEEP ) );
		final DayNightCycle.Phase[] phases = DayNightCycle.Phase.values();
		override( t, "Day phase", 0, phases.length - 1, "Dawn", "Night",
				() -> DayNightCycle.debugPhaseOverride != null,
				() -> DayNightCycle.phase().ordinal(),
				() -> "Day phase: " + pretty( DayNightCycle.phase().name() ),
				v -> DayNightCycle.debugPhaseOverride = phases[v],
				() -> DayNightCycle.debugPhaseOverride = null );
		final GameCalendar.MoonPhase[] moons = GameCalendar.MoonPhase.values();
		override( t, "Moon", 0, moons.length - 1, "New", "Waning",
				() -> GameCalendar.debugMoonOverride != null,
				() -> GameCalendar.moonPhase().ordinal(),
				() -> "Moon: " + pretty( GameCalendar.moonPhase().name() ),
				v -> GameCalendar.debugMoonOverride = moons[v],
				() -> GameCalendar.debugMoonOverride = null );

		//each one starts the event for its usual length (aurora 500 turns, rainbow 200, solar
		//eclipse 150, lunar eclipse 300) or ends it; an event this level's sky does not show
		//(an overworld aurora outside the far north, say) is still on, and says so
		t.header( "Special events", icon( Icons.ALERT ) );
		t.toggle( "Aurora", ClimateManager::isAuroraActive, ClimateManager::debugToggleAurora )
				.suffix( () -> ClimateManager.isAuroraActive() && !ClimateManager.isAurora() ? " (hidden)" : "" );
		t.toggle( "Rainbow", ClimateManager::isRainbowActive, ClimateManager::debugToggleRainbow )
				.suffix( () -> ClimateManager.isRainbowActive() && !ClimateManager.isRainbow() ? " (hidden)" : "" );
		t.toggle( "Solar eclipse", ClimateManager::isSolarEclipse, ClimateManager::debugToggleSolarEclipse );
		t.toggle( "Lunar eclipse", ClimateManager::isLunarEclipse, ClimateManager::debugToggleLunarEclipse );
		return t;
	}

	private static void override( Tab t, String label, int min, int max, String minLabel, String maxLabel,
	                              BooleanSupplier on, IntSupplier value, Supplier<String> title,
	                              IntConsumer set, Runnable clear ){
		Control c = t.add( new Control( Kind.OVERRIDE, label ) );
		c.min = min;
		c.max = max;
		c.minLabel = minLabel;
		c.maxLabel = maxLabel;
		c.on = on;
		c.value = value;
		c.text = title;
		c.set = set;
		c.clear = clear;
	}

	private static float heroTemp(){
		return Dungeon.hero != null && Dungeon.level != null
				? TileTemperature.tileTemp( Dungeon.hero.pos )
				: ClimateManager.localTemp();
	}

	static int compassIndex( float degrees ){
		return ((Math.round( degrees / 45f ) % 8) + 8) % 8;
	}

	//no precipitation reads as the first type, with the title saying "none"
	static int precipIndex( PrecipType type ){
		for (int i = 0; i < PRECIP_TYPES.length; i++) if (PRECIP_TYPES[i] == type) return i;
		return 0;
	}

	// ------------------------------------------------------------------ items

	private static Tab items(){
		Tab t = new Tab( "Items", Icons.BACKPACK );

		t.action( "All items", () -> GameScene.show( WndDebugPicker.forAllItems() ) )
				.closing().icon( icon( Icons.BACKPACK_LRG ) );

		t.header( "By category", icon( Icons.BACKPACK ) );
		category( t, "Weapon",   Generator.Category.WEAPON,   ItemSpriteSheet.WEAPON_HOLDER );
		category( t, "Armor",    Generator.Category.ARMOR,    ItemSpriteSheet.ARMOR_HOLDER );
		category( t, "Wand",     Generator.Category.WAND,     ItemSpriteSheet.WAND_HOLDER );
		category( t, "Ring",     Generator.Category.RING,     ItemSpriteSheet.RING_HOLDER );
		category( t, "Artifact", Generator.Category.ARTIFACT, ItemSpriteSheet.ARTIFACT_HOLDER );
		category( t, "Potion",   Generator.Category.POTION,   ItemSpriteSheet.POTION_HOLDER );
		category( t, "Scroll",   Generator.Category.SCROLL,   ItemSpriteSheet.SCROLL_HOLDER );
		category( t, "Seed",     Generator.Category.SEED,     ItemSpriteSheet.SEED_HOLDER );
		category( t, "Stone",    Generator.Category.STONE,    ItemSpriteSheet.STONE_HOLDER );
		category( t, "Food",     Generator.Category.FOOD,     ItemSpriteSheet.FOOD_HOLDER );
		category( t, "Missile",  Generator.Category.MISSILE,  ItemSpriteSheet.MISSILE_HOLDER );
		category( t, "Trinket",  Generator.Category.TRINKET,  ItemSpriteSheet.TRINKET_HOLDER );

		t.header( "Exotic and spells", small( BuffIndicator.RECHARGING ) );
		t.action( "Exotic potions", () -> GameScene.show( WndDebugPicker.forExoticPotions() ) )
				.closing().icon( item( ItemSpriteSheet.ELIXIR_HOLDER ) );
		t.action( "Exotic scrolls", () -> GameScene.show( WndDebugPicker.forExoticScrolls() ) )
				.closing().icon( icon( Icons.SCROLL_COLOR ) );
		t.action( "Spells", () -> GameScene.show( WndDebugPicker.forSpells() ) )
				.closing().icon( item( ItemSpriteSheet.SPELL_HOLDER ) );

		t.header( "What you carry", small( BuffIndicator.IMBUE ) );
		t.action( "Enchant weapon / armor", WndDebugPicker::forEnchanting )
				.closing().icon( icon( Icons.ALCHEMY ) );
		t.action( "Identify all", withHero( h -> {
			for (Item item : h.belongings) item.identify();
		} ) ).enabledIf( () -> Dungeon.hero != null ).icon( icon( Icons.MAGNIFY ) );
		return t;
	}

	private static void category( Tab t, String label, Generator.Category cat, int sprite ){
		t.action( label, () -> GameScene.show( WndDebugPicker.forItemCategory( cat ) ) )
				.closing().half().icon( item( sprite ) );
	}

	// ------------------------------------------------------------------ mobs

	private static Tab mobs(){
		Tab t = new Tab( "Mobs", Icons.SKULL );

		t.action( "All mobs", () -> GameScene.show( WndDebugPicker.forAllMobs() ) )
				.closing().icon( item( ItemSpriteSheet.MOB_HOLDER ) );
		t.action( "Weather / blobs", () -> GameScene.show( WndDebugPicker.forWeatherBlobs() ) )
				.closing().icon( buff( BuffIndicator.FROST ) );

		t.header( "Spawn by bestiary", icon( Icons.SKULL ) );
		bestiary( t, "Regional",  Bestiary.REGIONAL );
		bestiary( t, "Bosses",    Bestiary.BOSSES );
		bestiary( t, "Universal", Bestiary.UNIVERSAL );
		bestiary( t, "Rare",      Bestiary.RARE );
		bestiary( t, "Quest",     Bestiary.QUEST );
		bestiary( t, "Neutral",   Bestiary.NEUTRAL );
		return t;
	}

	private static void bestiary( Tab t, String label, Bestiary b ){
		t.action( label, () -> GameScene.show( WndDebugPicker.forMobCategory( b ) ) ).closing().half();
	}

	// ------------------------------------------------------------------ travel

	private static Tab travel(){
		Tab t = new Tab( "Travel", Icons.DEPTH );

		t.readout( "Where the hero is", () -> {
			String s = "Here: depth " + Dungeon.depth + ", branch " + Dungeon.branch;
			if (Dungeon.level instanceof OverworldLevel) s += ", slice " + ((OverworldLevel) Dungeon.level).altitude();
			return s;
		}, () -> READOUT_COLOR );

		t.header( "Go somewhere", small( BuffIndicator.HASTE ) );
		t.action( "Scenes...", () -> GameScene.show( new WndDebugScenes() ) )
				.closing().icon( icon( Icons.ENTER ) );
		t.action( "Teleport to a tile", DebugMenu::teleport )
				.closing().enabledIf( () -> Dungeon.hero != null && Dungeon.level != null ).icon( icon( Icons.TARGET ) );
		t.action( "All levels", () -> GameScene.show( WndDebugPicker.forTravel() ) )
				.closing().icon( icon( Icons.STAIRS ) );

		t.header( "Debug levels", small( BuffIndicator.FORESIGHT ) );
		t.text( "The rooms showcase holds every room, each with its sign; the overworld is the infinite world (a work in progress).", 5 );
		t.action( "Rooms showcase", () -> goTo( DEV_ROOMS_DEPTH ) )
				.closing().icon( icon( Icons.STAIRS_SECRETS ) );
		t.action( "Overworld", () -> goTo( OVERWORLD_DEPTH ) )
				.closing().icon( icon( Icons.GRASS ) );

		//one slice up or down from here, on the same world cell; only on the overworld
		t.header( "World slices", small( BuffIndicator.LEVITATION ) );
		t.action( "Slice up (+1)", () -> slice( 1 ) ).closing().half().enabledIf( () -> sliceExists( 1 ) );
		t.action( "Slice down (-1)", () -> slice( -1 ) ).closing().half().enabledIf( () -> sliceExists( -1 ) );
		return t;
	}

	public static final int DEV_ROOMS_DEPTH = 85;
	public static final int OVERWORLD_DEPTH = 97;

	//already there: the level is made again from scratch
	private static void goTo( int depth ){
		if (Dungeon.depth == depth){
			InterlevelScene.mode = InterlevelScene.Mode.RESET;
		} else {
			InterlevelScene.mode = InterlevelScene.Mode.RETURN;
			InterlevelScene.returnDepth = depth;
			InterlevelScene.returnBranch = 0;
			InterlevelScene.returnPos = -1;
		}
		Game.switchScene( InterlevelScene.class );
	}

	private static boolean sliceExists( int step ){
		return Dungeon.level instanceof OverworldLevel && Dungeon.hero != null
				&& WorldLayers.exists( ((OverworldLevel) Dungeon.level).altitude() + step );
	}

	private static void slice( int step ){
		if (!sliceExists( step )){
			GLog.w( "No world slice there." );
			return;
		}
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		ow.travelToSlice( Dungeon.hero, ow.altitude() + step );
	}

	private static void teleport(){
		GameScene.selectCell( new CellSelector.Listener(){
			@Override
			public void onSelect( Integer cell ){
				if (cell == null || Dungeon.level == null || Dungeon.hero == null) return;
				Hero hero = Dungeon.hero;
				if (cell < 0 || cell >= Dungeon.level.length()
						|| !(Dungeon.level.passable[cell] || (hero.flying && Dungeon.level.avoid[cell]))
						|| (Actor.findChar( cell ) != null && Actor.findChar( cell ) != hero)){
					GLog.w( "Choose an open, unoccupied tile." );
					return;
				}
				if (cell == hero.pos) return;
				hero.interrupt();
				Buff.detach( hero, Roots.class );
				//debug travel deliberately bypasses teleport pathfinding restrictions
				ScrollOfTeleportation.appear( hero, cell );
				Dungeon.level.occupyCell( hero );
				Dungeon.observe();
				GameScene.updateFog();
			}

			@Override
			public String prompt(){
				return "Click or tap a tile to teleport.";
			}
		} );
	}

	// ------------------------------------------------------------------ hero

	public static final int READOUT_COLOR = 0xAACCFF;

	//the amounts a value slider steps through: every decade and its half, up to a million
	public static final int[] VALUE_STEPS = { 0, 10, 50, 100, 500, 1000, 5000, 10000, 50000, 100000, 1000000 };

	/** The highest step at or under an amount: where a value slider stands for it. */
	public static int stepOf( int amount ){
		int step = 0;
		for (int i = 0; i < VALUE_STEPS.length; i++) if (VALUE_STEPS[i] <= amount) step = i;
		return step;
	}

	private static Tab hero(){
		Tab t = new Tab( "Hero", Icons.TALENT );
		if (Dungeon.hero == null){
			t.text( "No hero yet: start or load a run first.", 6 );
			return t;
		}

		t.header( "Hero stats", small( BuffIndicator.HEALING ) );
		t.readout( "Hero stats", DebugMenu::heroStats, () -> READOUT_COLOR );

		t.header( "Cheats", small( BuffIndicator.BERSERK ) );
		t.toggle( "Infinite health", () -> Dungeon.hero != null && Dungeon.hero.debugInfiniteHealth, withHero( h -> {
			h.debugInfiniteHealth = !h.debugInfiniteHealth;
			if (h.debugInfiniteHealth) h.HP = h.HT;
		} ) ).icon( buff( BuffIndicator.ANKH ) );
		t.toggle( "Invisibility (infinite)", DebugMenu::invisibleOn, DebugMenu::flipInvisible )
				.icon( buff( BuffIndicator.INVISIBLE ) );
		t.toggle( "One hit kill", () -> Dungeon.debugOneHitKill,
				() -> Dungeon.debugOneHitKill = !Dungeon.debugOneHitKill ).icon( buff( BuffIndicator.MARK ) );
		t.toggle( "Infinite mana", () -> Dungeon.debugInfiniteMana, withHero( h -> {
			Dungeon.debugInfiniteMana = !Dungeon.debugInfiniteMana;
			if (Dungeon.debugInfiniteMana) h.MP = h.MT;
		} ) ).icon( buff( BuffIndicator.RECHARGING ) );

		t.header( "Map", small( BuffIndicator.MIND_VISION ) );
		t.toggle( "No fog of war", () -> Dungeon.debugNoFog, DebugMenu::flipNoFog )
				.icon( buff( BuffIndicator.MIND_VISION ) );
		t.action( "Reveal level", DebugMenu::revealLevel ).icon( icon( Icons.MAGNIFY ) );

		t.header( "Body", small( BuffIndicator.HEART ) );
		t.action( "Full heal", withHero( h -> h.HP = h.HT ) ).half();
		t.action( "+5 strength", withHero( h -> h.STR += 5 ) ).half();
		t.action( "+5 levels", withHero( h -> {
			for (int i = 0; i < 5; i++) h.earnExp( h.maxExp(), DebugMenu.class );
		} ) ).half();
		t.action( "Satisfy hunger", withHero( h -> {
			Hunger hunger = h.buff( Hunger.class );
			if (hunger != null) hunger.satisfy( Hunger.STARVING );
		} ) ).half();

		t.header( "Sleep", small( BuffIndicator.DROWSY ) );
		t.action( "Max sleepiness", withHero( h -> Buff.affect( h, Sleepiness.class ).affectSleep( Sleepiness.COMATOSE ) ) );
		t.action( "Make drowsy", withHero( h -> {
			Sleepiness s = Buff.affect( h, Sleepiness.class );
			s.wake( Sleepiness.COMATOSE );      //down to nothing...
			s.affectSleep( Sleepiness.DROWSY ); //...then up to the drowsy line
		} ) ).half();
		t.action( "Clear sleepiness", withHero( h -> Buff.affect( h, Sleepiness.class ).wake( Sleepiness.COMATOSE ) ) ).half();

		t.header( "Resources", icon( Icons.COIN_SML ) );
		value( t, "Gold", () -> Dungeon.gold, v -> Dungeon.gold = v );
		value( t, "Alchemy energy", () -> Dungeon.energy, v -> Dungeon.energy = v );
		value( t, "Skill points", () -> Dungeon.hero == null ? 0 : Dungeon.hero.heroSkills.availableSkill, v -> {
			if (Dungeon.hero != null) Dungeon.hero.heroSkills.availableSkill = v;
		} );

		t.header( "Skills", small( BuffIndicator.UPGRADE ) );
		t.action( "Max skill tree", withHero( h -> {
			SkillDebug.maxTree( h );
			GLog.p( "Current skill tree fully upgraded; all paths unlocked." );
		} ) );
		t.toggle( "All skill paths", () -> Dungeon.hero != null && Dungeon.hero.debugAllSkillPaths, withHero( h -> {
			h.debugAllSkillPaths = !h.debugAllSkillPaths;
			GLog.p( h.debugAllSkillPaths ? "All skill paths unlocked, including exclusive choices."
					: "Normal skill path restrictions restored." );
		} ) );
		t.action( "Skills of any class...", () -> GameScene.show( new WndDebugSkills() ) )
				.closing().icon( icon( Icons.TALENT ) );
		//on: every other class's and calling's skill at its top level; off: the hero's own skills only
		t.toggle( "All skills, all classes", () -> Dungeon.hero != null && SkillDebug.allClassesOn( Dungeon.hero ), withHero( h -> {
			boolean on = !SkillDebug.allClassesOn( h );
			SkillDebug.allClasses( h, on );
			GLog.p( on ? "Every skill of every class learned, at its top level."
					: "Back to the hero's own skills: the other classes' are gone." );
		} ) );
		return t;
	}

	private static void value( Tab t, String label, IntSupplier get, IntConsumer set ){
		Control c = t.add( new Control( Kind.VALUE, label ) );
		c.min = 0;
		c.max = VALUE_STEPS.length - 1;
		c.minLabel = "0";
		c.maxLabel = "1M";
		c.value = () -> stepOf( get.getAsInt() );
		c.text = () -> label + ": " + get.getAsInt();
		c.set = pos -> set.accept( VALUE_STEPS[Math.max( 0, Math.min( VALUE_STEPS.length - 1, pos ) )] );
	}

	static String heroStats(){
		Hero h = Dungeon.hero;
		if (h == null) return "No hero.";
		Sleepiness tired = h.buff( Sleepiness.class );
		int sleep = tired != null ? (int)tired.level() : 0;
		Hunger hunger = h.buff( Hunger.class );
		return "HP " + h.HP + "/" + h.HT + "  MP " + h.MP + "/" + h.MT + "  STR " + h.STR()
				+ "\nLvl " + h.lvl + "  Exp " + h.exp + "/" + h.maxExp()
				+ (hunger != null ? "  Hunger " + hunger.hunger() + "/" + (int)Hunger.STARVING : "")
				+ "\nSleepiness " + sleep + "/" + (int)Sleepiness.COMATOSE + " (drowsy at " + (int)Sleepiness.DROWSY + ")";
	}

	//the state is the buff itself (a billion turns, never dispelled): it outlives a reload, and
	//a new hero starts without it
	static boolean invisibleOn(){
		Hero h = Dungeon.hero;
		return h != null && Invisibility.isDebugEndless( h );
	}

	static void flipInvisible(){
		Hero h = Dungeon.hero;
		if (h == null) return;
		if (invisibleOn()){
			Buff.detach( h, Invisibility.class );
			Invisibility.dispel( h );
		} else {
			Buff.affect( h, Invisibility.class, Invisibility.DEBUG_DURATION );
		}
	}

	//what the hero had seen before the fog was lifted, so lifting it can be undone. Kept with the
	//array it was copied from: the overworld swaps its array whenever its window moves, and a
	//copy from another window would put the fog back in the wrong places
	private static boolean[] fogArray;
	private static boolean[] fogVisited;

	static void flipNoFog(){
		Level level = Dungeon.level;
		if (!Dungeon.debugNoFog){
			fogArray = level != null ? level.visited : null;
			fogVisited = level != null ? level.visited.clone() : null;
			Dungeon.debugNoFog = true;
		} else {
			Dungeon.debugNoFog = false;
			if (level != null && fogArray != null && level.visited == fogArray){
				System.arraycopy( fogVisited, 0, level.visited, 0, fogVisited.length );
			}
			fogArray = null;
			fogVisited = null;
		}
		if (level != null){
			Dungeon.observe();
			GameScene.updateFog();
		}
	}

	private static void revealLevel(){
		Level level = Dungeon.level;
		if (level == null) return;
		for (int i = 0; i < level.length(); i++){
			level.visited[i] = level.mapped[i] = true;
		}
		//revealed under no-fog: turning no-fog off keeps it revealed
		if (fogArray == level.visited) Arrays.fill( fogVisited, true );
		GameScene.updateFog();
		Dungeon.observe();
	}

	// ------------------------------------------------------------------ performance

	private static Tab perf(){
		Tab t = new Tab( "Performance", Icons.WARNING );

		t.header( "Profiler", small( BuffIndicator.TIME ) );
		t.text( "The profiler samples the render and actor threads every 4 ms while it runs: play normally, then stop it and read where the time went. "
				+ "Frames, GL draw calls, heap, hot paths and slowest actors are in the same report.", 6 );
		//turning it on closes the window so the game can be played; turning it off shows the report
		Control prof = t.toggle( "Profiler", () -> Profiler.running, () -> {
			if (Profiler.running) showReport( "Profile", Profiler.stop() );
			else Profiler.start();
		} ).icon( icon( Icons.STATS ) )
				.suffix( () -> Profiler.running ? String.format( Locale.ROOT, " (%.0fs)", Profiler.seconds() ) : "" );
		prof.closes = () -> !Profiler.running;
		t.action( "Show last profile", () -> showReport( "Profile", Profiler.lastReport() ) ).half();
		t.action( "Save last profile", () -> {
			String r = Profiler.lastReport();
			if (r == null) GLog.w( "Nothing recorded yet." );
			else LagMonitor.save( "profile", r );
		} ).half();
		Control jfr = t.toggle( "Flight recording (JFR)", Profiler::jfrRunning, () -> {
			if (Profiler.jfrRunning()){
				String path = Profiler.jfrStop();
				if (path != null) showReport( "Flight recording", "Saved to:\n" + path
						+ "\n\nOpen it with JDK Mission Control (jmc) for method-level flame graphs, GC, allocation and lock profiles." );
			} else {
				Profiler.jfrStart();
			}
		} ).shownIf( Profiler::jfrAvailable );
		jfr.closes = () -> !Profiler.jfrRunning();

		t.header( "Lag detector", small( BuffIndicator.PARALYSIS ) );
		t.toggle( "Lag detector", () -> LagMonitor.enabled, () -> {
			LagMonitor.setEnabled( !LagMonitor.enabled );
			Profiler.keepLagDetector();
		} )
				.icon( icon( Icons.ALERT ) );
		t.action( "Lag snapshot now", () -> {
			boolean was = LagMonitor.enabled;
			if (!was) LagMonitor.setEnabled( true );
			String report = LagMonitor.capture( "manual snapshot from the debug menu", false );
			if (!was) LagMonitor.setEnabled( false );
			showReport( "Lag snapshot", report );
		} ).half();
		t.action( "Save last snapshot", () -> {
			String last = LagMonitor.lastReport();
			if (last == null) GLog.w( "No lag snapshot captured yet." );
			else LagMonitor.save( last );
		} ).half();

		//the renderer's chunked tilemaps (culled, rebuilt per 16x16 block); the legacy
		//single-buffer path is kept for comparison and takes effect on the next level load
		t.header( "Renderer", small( BuffIndicator.LIGHT ) );
		t.toggle( "Chunked tilemaps (next level)", () -> Tilemap.chunked, () -> {
			Tilemap.chunked = !Tilemap.chunked;
			GLog.i( "Tilemap chunking " + (Tilemap.chunked ? "on" : "off") + " from the next level load." );
		} );
		return t;
	}

	private static void showReport( String title, String report ){
		if (report == null){
			GLog.w( "Nothing recorded yet." );
			return;
		}
		GameScene.show( new WndDebugReport( title, report ) );
	}

	// ------------------------------------------------------------------ journal and account

	private static Tab journal(){
		Tab t = new Tab( "Journal and account", Icons.CATALOG );

		t.header( "Journal", icon( Icons.INVESTIGATE ) );
		t.action( "Fill journal", () -> {
			for (Catalog cat : Catalog.values()){
				for (Class<?> cls : cat.items()) Catalog.setSeen( cls );
			}
			for (Bestiary b : Bestiary.values()){
				for (Class<?> cls : b.entities()) Bestiary.setSeen( cls );
			}
			for (Document doc : Document.values()){
				for (String page : doc.pageNames()){
					doc.findPage( page );
					doc.readPage( page );
				}
			}
			//Journal.saveNeeded is set by Catalog, Bestiary and Document themselves
		} ).half();
		t.action( "Clear journal", () -> {
			for (Document doc : Document.values()){
				for (String page : doc.pageNames()) doc.deletePage( page );
			}
		} ).half();
		t.action( "Fill mob drops", () -> {
			for (Bestiary b : Bestiary.values()){
				for (Class<?> cls : b.entities()){
					for (int slot = 0; slot < 8; slot++) Bestiary.setLootSlotSeen( cls, slot );
				}
			}
		} );

		t.header( "Guide", icon( Icons.LOST ) );
		t.action( "Fill guide", () -> {
			for (String key : GuideGraph.allKeys()) GuideProgress.findPage( key );
		} ).half();
		t.action( "Clear guide", GuideProgress::clear ).half();
		//debug builds mark every guide page read at startup, which is exactly what hides the
		//guidebook, its trail and the searching page: back to first-run
		t.action( "Reset tutorial", () -> {
			WPDSettings.intro( true );
			for (String page : Document.ADVENTURERS_GUIDE.pageNames()){
				Document.ADVENTURERS_GUIDE.deletePage( page );
			}
			//to disk now: a restart would otherwise bring the read pages back
			Journal.saveGlobal();
			GLog.p( "Tutorial reset: the guidebook returns in the next new game." );
		} ).icon( icon( Icons.REPEAT ) );

		//online play asks the relay for a supporter receipt. This is the relay's own test
		//secret, typed in here rather than shipped: an empty box is the shipped state, and the
		//relay refuses anything that is not the token it was started with. Rotating it there
		//revokes every device at once
		t.header( "Account", small( BuffIndicator.ARMOR ) );
		t.action( "Relay dev token", () -> GameScene.show( new WndTextInput(
				"Relay dev token",
				"Stands in for a supporter receipt, and only on a relay started with the "
						+ "same secret. Leave empty to go back to using the store receipt.",
				WPDSettings.relayDevToken(), 128, false, "Save", "Cancel" ){
			@Override
			public void onSelect( boolean positive, String text ){
				if (!positive) return;
				WPDSettings.relayDevToken( text.trim() );
				GLog.p( text.trim().isEmpty() ? "Relay dev token cleared." : "Relay dev token set." );
			}
		} ) ).icon( icon( Icons.DATA ) );

		t.header( "Supporter thanks", small( BuffIndicator.BLESS ) );
		t.action( "Thanks T1", () -> GameScene.show( new WndSupporterThanks( 1, "Tier 1" ) ) ).half();
		t.action( "Thanks T2", () -> GameScene.show( new WndSupporterThanks( 2, "Tier 2" ) ) ).half();
		t.action( "Thanks T3", () -> GameScene.show( new WndSupporterThanks( 3, "Tier 3" ) ) ).half();
		//the real purchase path: the thank-you is written down, shown, and then the scene is
		//rebuilt the way the store sheet closing rebuilds it (a resize resets the scene). The
		//window must come straight back on the rebuilt scene
		t.action( "T3 + scene reset", () -> {
			WndSupporterThanks.request( 3, "Tier 3 (test)" );
			Game.scene().add( new com.watabou.noosa.tweeners.Delayer( 0.6f ){
				@Override
				protected void onComplete(){
					super.onComplete();
					Game.resetScene();
				}
			} );
		} ).half();
		return t;
	}

	// ------------------------------------------------------------------ helpers

	private static Runnable withHero( Consumer<Hero> action ){
		return () -> {
			Hero h = Dungeon.hero;
			if (h != null) action.accept( h );
		};
	}

	private static Supplier<Image> icon( Icons icon ){
		return () -> Icons.get( icon );
	}

	private static Supplier<Image> item( int sprite ){
		return () -> new ItemSprite( sprite );
	}

	private static Supplier<Image> buff( int index ){
		return () -> new BuffIcon( index, true );
	}

	//a section header's icon: the small buff icons, all one height (7)
	private static Supplier<Image> small( int index ){
		return () -> new BuffIcon( index, false );
	}

	/** One decimal, the same on every device (a pt-BR phone would print "12,3"). */
	static String fmt( float v ){
		return String.format( Locale.ROOT, "%.1f", v );
	}

	static String pct( float fraction ){
		return Math.round( fraction * 100 ) + "%";
	}

	//NEW_MOON -> new moon
	static String pretty( String enumName ){
		return enumName.toLowerCase( Locale.ROOT ).replace( '_', ' ' );
	}

	static int tempColor( float temp ){
		if (temp <= -10f) return 0x4488FF;
		if (temp <= 0f)   return 0x88BBFF;
		if (temp <= 15f)  return 0xCCCCCC;
		if (temp <= 30f)  return 0xFFCC44;
		return 0xFF4422;
	}
}
