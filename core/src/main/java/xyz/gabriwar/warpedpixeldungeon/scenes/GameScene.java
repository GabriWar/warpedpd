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

package xyz.gabriwar.warpedpixeldungeon.scenes;

import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.BloodMoonBuff;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Chrome;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.Rankings;
import xyz.gabriwar.warpedpixeldungeon.WPDAction;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AscensionChallenge;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ChampionEnemy;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DemonSpawner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Ghoul;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mimic;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Snake;
import xyz.gabriwar.warpedpixeldungeon.effects.BannerSprites;
import xyz.gabriwar.warpedpixeldungeon.effects.AuroraOverlay;
import xyz.gabriwar.warpedpixeldungeon.effects.RainbowOverlay;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherOverlay;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.EmoIcon;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.Ripple;
import xyz.gabriwar.warpedpixeldungeon.effects.SpellSprite;
import xyz.gabriwar.warpedpixeldungeon.items.Ankh;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Honeypot;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.DriedRose;
import xyz.gabriwar.warpedpixeldungeon.items.journal.Guidebook;
import xyz.gabriwar.warpedpixeldungeon.items.potions.Potion;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.InventoryScroll;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.DimensionalSundial;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.TrinketCatalyst;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.journal.Bestiary;
import xyz.gabriwar.warpedpixeldungeon.journal.Document;
import xyz.gabriwar.warpedpixeldungeon.journal.Journal;
import xyz.gabriwar.warpedpixeldungeon.journal.Notes;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.RegularLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.Room;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.secret.SecretRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.Trap;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.DiscardedItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.HeroSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.CustomTilemap;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTerrainTilemap;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonWallsTilemap;
import xyz.gabriwar.warpedpixeldungeon.tiles.FogOfWar;
import xyz.gabriwar.warpedpixeldungeon.tiles.GridTileMap;
import xyz.gabriwar.warpedpixeldungeon.tiles.RaisedTerrainTilemap;
import xyz.gabriwar.warpedpixeldungeon.tiles.TerrainFeaturesTilemap;
import xyz.gabriwar.warpedpixeldungeon.tiles.WallBlockingTilemap;
import xyz.gabriwar.warpedpixeldungeon.ui.ActionIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.AttackIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.CrouchIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.SundialIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.Banner;
import xyz.gabriwar.warpedpixeldungeon.ui.BossHealthBar;
import xyz.gabriwar.warpedpixeldungeon.ui.CharHealthIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.GameLog;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.InventoryPane;
import xyz.gabriwar.warpedpixeldungeon.ui.LootIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.MenuPane;
import xyz.gabriwar.warpedpixeldungeon.ui.QuickSlotButton;
import xyz.gabriwar.warpedpixeldungeon.ui.ResumeIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.RightClickMenu;
import xyz.gabriwar.warpedpixeldungeon.ui.StatusPane;
import xyz.gabriwar.warpedpixeldungeon.ui.StyledButton;
import xyz.gabriwar.warpedpixeldungeon.ui.Tag;
import xyz.gabriwar.warpedpixeldungeon.ui.TargetHealthIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.Toast;
import xyz.gabriwar.warpedpixeldungeon.ui.Toolbar;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import xyz.gabriwar.warpedpixeldungeon.windows.WndGame;
import xyz.gabriwar.warpedpixeldungeon.windows.WndHero;
import xyz.gabriwar.warpedpixeldungeon.windows.WndInfoCell;
import xyz.gabriwar.warpedpixeldungeon.windows.WndInfoItem;
import xyz.gabriwar.warpedpixeldungeon.windows.WndInfoMob;
import xyz.gabriwar.warpedpixeldungeon.windows.WndInfoPlant;
import xyz.gabriwar.warpedpixeldungeon.windows.WndInfoTrap;
import xyz.gabriwar.warpedpixeldungeon.windows.WndKeyBindings;
import xyz.gabriwar.warpedpixeldungeon.windows.WndMessage;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import xyz.gabriwar.warpedpixeldungeon.windows.WndResurrect;
import xyz.gabriwar.warpedpixeldungeon.windows.WndUpgrade;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Blending;
import com.watabou.input.ControllerHandler;
import com.watabou.input.KeyBindings;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.NoosaScript;
import com.watabou.noosa.NoosaScriptNoLighting;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.SkinnedBlock;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.tweeners.Tweener;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;
import com.watabou.utils.PlatformSupport;
import com.watabou.utils.Point;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.RectF;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

public class GameScene extends PixelScene {

	static GameScene scene;

	private SkinnedBlock water;
	private DungeonTerrainTilemap tiles;
	private GridTileMap visualGrid;
	private TerrainFeaturesTilemap terrainFeatures;
	private RaisedTerrainTilemap raisedTerrain;
	private DungeonWallsTilemap walls;
	private WallBlockingTilemap wallBlocking;
	private FogOfWar fog;
	private ColorBlock dayNightOverlay;
	// Smooth tint transition state
	private static float curTintR = 0, curTintG = 0, curTintB = 0, curTintA = 0;
	private static float tgtTintR = 0, tgtTintG = 0, tgtTintB = 0, tgtTintA = 0;
	private static final float TINT_LERP_SPEED = 1.5f; // full transition in ~0.7s
	private HeroSprite hero;

	private MenuPane menu;
	private xyz.gabriwar.warpedpixeldungeon.ui.IconButton mapButton;
	private StatusPane status;

	private BossHealthBar boss;

	private GameLog log;

	private static CellSelector cellSelector;
	
	private Group terrain;
	private Group customTiles;
	private Group levelVisuals;
	private Group levelWallVisuals;
	private Group customWalls;
	private Group ripples;
	private Group plants;
	private Group traps;
	private Group heaps;
	private Group mobs;
	private Group floorEmitters;
	private Group emitters;
	private Group effects;
	private Group gases;
	private Group spells;
	private Group statuses;
	private Group emoicons;
	private Group overFogEffects;
	private Group healthIndicators;
	private WeatherOverlay weatherOverlay;
	private AuroraOverlay auroraOverlay;
	private RainbowOverlay rainbowOverlay;

	private InventoryPane inventory;
	private float invBottom; // stored insets.bottom for inventory repositioning
	private static int invState = 0; // 0=visible(normal), 1=hidden, 2=expanded

	private Toolbar toolbar;
	private Toast prompt;

	private SundialIndicator sundialIndicator;
	private AttackIndicator attack;
	private LootIndicator loot;
	private ActionIndicator action;
	private ResumeIndicator resume;
	private xyz.gabriwar.warpedpixeldungeon.ui.WaypointIndicator waypointTag;
	private xyz.gabriwar.warpedpixeldungeon.ui.SkillPointsIndicator skillTag;
	private CrouchIndicator crouch;

	{
		inGameScene = true;
	}

	@Override
	public void create() {
		
		if (Dungeon.hero == null || Dungeon.level == null){
			WarpedPixelDungeon.switchNoFade(TitleScene.class);
			return;
		}

		Dungeon.level.playLevelMusic();

		WPDSettings.lastClass(Dungeon.hero.heroClass.ordinal());
		
		super.create();
		Camera.main.zoom( GameMath.gate(minZoom, defaultZoom + WPDSettings.zoom(), maxZoom));
		Camera.main.edgeScroll.set(1);

		switch (WPDSettings.cameraFollow()) {
			case 4: default:    Camera.main.setFollowDeadzone(0);      break;
			case 3:             Camera.main.setFollowDeadzone(0.2f);   break;
			case 2:             Camera.main.setFollowDeadzone(0.5f);   break;
			case 1:             Camera.main.setFollowDeadzone(0.9f);   break;
		}

		RectF insets = getCommonInsets();
		//we want to check if large is the same as blocking here
		float largeInsetTop = Game.platform.getSafeInsets(PlatformSupport.INSET_LRG).scale(1f/defaultZoom).top;

		scene = this;

		terrain = new Group();
		add( terrain );

		water = new SkinnedBlock(
			Dungeon.level.width() * DungeonTilemap.SIZE,
			Dungeon.level.height() * DungeonTilemap.SIZE,
			Dungeon.level.waterTex() ){

			@Override
			protected NoosaScript script() {
				return NoosaScriptNoLighting.get();
			}

			@Override
			public void draw() {
				//water has no alpha component, this improves performance
				Blending.disable();
				super.draw();
				Blending.enable();
			}
		};
		water.autoAdjust = true;
		terrain.add( water );

		ripples = new Group();
		terrain.add( ripples );

		DungeonTileSheet.setupVariance(Dungeon.level.map.length, Dungeon.seedCurDepth());
		
		tiles = new DungeonTerrainTilemap();
		terrain.add( tiles );

		customTiles = new Group();
		terrain.add(customTiles);

		for( CustomTilemap visual : Dungeon.level.customTiles){
			addCustomTile(visual);
		}

		visualGrid = new GridTileMap();
		terrain.add( visualGrid );

		terrainFeatures = new TerrainFeaturesTilemap(Dungeon.level.plants, Dungeon.level.traps);
		terrain.add(terrainFeatures);
		
		levelVisuals = Dungeon.level.addVisuals();
		add(levelVisuals);

		floorEmitters = new Group();
		add(floorEmitters);

		heaps = new Group();
		add( heaps );
		
		for ( Heap heap : Dungeon.level.heaps.valueList() ) {
			addHeapSprite( heap );
		}

		emitters = new Group();
		effects = new Group();
		healthIndicators = new Group();
		emoicons = new Group();
		overFogEffects = new Group();
		
		mobs = new Group();
		add( mobs );

		hero = new HeroSprite();
		hero.place( Dungeon.hero.pos );
		hero.updateArmor();
		mobs.add( hero );
		
		for (Mob mob : Dungeon.level.mobs) {
			addMobSprite( mob );
		}

		// Net MP host: re-sprite any remote players. They live in NetManager.netHeroes,
		// not Dungeon.level.mobs, so the loop above misses them on scene rebuild
		// (level transition, return-to-menu, etc.) Without this their sprite is null
		// after rebuild and Hero.act() NPEs in getCloser → sprite.move().
		if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isHost()) {
			for (xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero nh
					: xyz.gabriwar.warpedpixeldungeon.net.NetManager.getNetHeroes()) {
				addNetHero(nh);
			}
		}

		raisedTerrain = new RaisedTerrainTilemap();
		add( raisedTerrain );

		walls = new DungeonWallsTilemap();
		add(walls);

		customWalls = new Group();
		add(customWalls);

		for( CustomTilemap visual : Dungeon.level.customWalls){
			addCustomWall(visual);
		}

		levelWallVisuals = Dungeon.level.addWallVisuals();
		add( levelWallVisuals );

		wallBlocking = new WallBlockingTilemap();
		add (wallBlocking);

		add( emitters );
		add( effects );

		gases = new Group();
		add( gases );

		for (Blob blob : Dungeon.level.blobs.values()) {
			blob.emitter = null;
			addBlobSprite( blob );
		}


		dayNightOverlay = new ColorBlock(
				Dungeon.level.width() * DungeonTilemap.SIZE,
				Dungeon.level.height() * DungeonTilemap.SIZE,
				0xFFFFFFFF );
		add( dayNightOverlay );
		updateDayNightTint();
		// Snap tint immediately on scene creation (no lerp from black)
		curTintR = tgtTintR;
		curTintG = tgtTintG;
		curTintB = tgtTintB;
		curTintA = tgtTintA;
		dayNightOverlay.hardlight(curTintR, curTintG, curTintB);
		dayNightOverlay.alpha(curTintA);

		weatherOverlay = new WeatherOverlay();
		weatherOverlay.setup();
		add( weatherOverlay );

		auroraOverlay = new AuroraOverlay();
		add( auroraOverlay );

		rainbowOverlay = new RainbowOverlay();
		add( rainbowOverlay );

		if (!xyz.gabriwar.warpedpixeldungeon.net.NetManager.isNetClient()) {
			//the overworld IS the surface: depth 0 to the climate, whatever
			//its slot number - at its real depth the weather would stay at
			//the bottom of the dungeon (no rain, ash in the air)
			ClimateManager.onLevelChange(
					Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel
							? 0 : Dungeon.depth );
			updateWeather();
		} else {
			// Apply weather state received from host
			xyz.gabriwar.warpedpixeldungeon.net.SpectatorReceiver.applyPendingWeather();
		}

		fog = new FogOfWar( Dungeon.level.width(), Dungeon.level.height() );
		add( fog );

		// PLAYER mode bootstraps its own FoV (host's FoV was discarded because the
		// client occupies a different cell). Normal flow runs observe() inside
		// Dungeon.switchLevel(), but net clients skip switchLevel entirely — without
		// this call the player sees a black map until they take their first step.
		if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isPlayer()
				&& Dungeon.hero != null && Dungeon.level != null) {
			Dungeon.observe();
		}

		spells = new Group();
		add( spells );

		add(overFogEffects);
		
		statuses = new Group();
		add( statuses );
		
		add( healthIndicators );
		//always appears ontop of other health indicators
		add( new TargetHealthIndicator() );
		
		add( emoicons );
		
		add( cellSelector = new CellSelector( tiles ) );
		cellSelector.enabled = !xyz.gabriwar.warpedpixeldungeon.net.NetManager.isNetClient();

		int uiSize = WPDSettings.interfaceSize();

		//display cutouts can obstruct various UI elements, so we need to adjust for that sometimes
		float heroPaneExtraWidth = insets.left;
		float menuBarMaxLeft = uiCamera.width-insets.right-MenuPane.WIDTH;
		int hpBarMaxWidth = 50; //default max width
		float[] buffBarRowLimits = new float[9];
		float[] buffBarRowAdjusts = new float[9];

		if (largeInsetTop == 0 && insets.top > 0){
				//smaller non-notch cutouts are of varying size and may obstruct various UI elements
				// some are small hole punches, some are huge dynamic islands
				RectF cutout = Game.platform.getDisplayCutout().scale(1f / defaultZoom);
				//if the cutout is positioned to obstruct the hero portrait in the status pane
				if (cutout.top < 30
						&& cutout.left < 20
						&& cutout.right > 12) {
					heroPaneExtraWidth = Math.max(heroPaneExtraWidth, cutout.right-12);
					//make sure we have space to actually move it though
					heroPaneExtraWidth = Math.min(heroPaneExtraWidth, uiCamera.width - PixelScene.MIN_WIDTH_P);
				}
				//if the cutout is positioned to obstruct the menu bar
				else if (cutout.top < 20
						&& cutout.left < menuBarMaxLeft + MenuPane.WIDTH
						&& cutout.right > menuBarMaxLeft) {
					menuBarMaxLeft = Math.min(menuBarMaxLeft, cutout.left - MenuPane.WIDTH);
					//make sure we have space to actually move it though
					menuBarMaxLeft = Math.max(menuBarMaxLeft, PixelScene.MIN_WIDTH_P-MenuPane.WIDTH);
				}
				//if the cutout is positioned to obstruct the HP bar
				else if (cutout.left < 78
						&& cutout.top < 4
						&& cutout.right > 32) {
					//subtract starting position, but add a bit back due to end of bar
					hpBarMaxWidth = Math.round(cutout.left - 32 + 4);
					hpBarMaxWidth = Math.max(hpBarMaxWidth, 21); //cannot go below 21 (30 effective)
				}
				//if the cutout is positioned to obstruct the buff bar
				if (cutout.left < 84
						&& cutout.top < 10
						&& cutout.right > 32
						&& cutout.bottom > 11) {
					int i = 1;
					int rowTop = 11;
					//in most cases this just obstructs one row, but dynamic island can block more =S
					while (cutout.bottom > rowTop){
						if (i == 1 || cutout.bottom > rowTop+2 ) { //always shorten first row
							//subtract starting position, add a bit back to allow slight overlap
							buffBarRowLimits[i] = cutout.left - 32 + 3;
						} else {
							//if row is only slightly cut off, lower it instead of limiting width
							buffBarRowAdjusts[i] = cutout.bottom - rowTop + 1;
							rowTop += buffBarRowAdjusts[i];
						}
						i++;
						rowTop += 8;
					}
				}
		}

		float screentop = largeInsetTop;
		if (screentop == 0 && uiSize == 0){
			screentop--; //on mobile UI, if we render in fullscreen, clip the top 1px;
		}

		menu = new MenuPane();
		menu.camera = uiCamera;
		menu.setPos( menuBarMaxLeft, screentop);
		add(menu);

		//the map button sits under the menu pane in the top-right corner, where a map
		//belongs and where nothing else is competing for the corner. Only on the
		//overworld: a dungeon floor has its own map and does not need this one.
		if (Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) {
			mapButton = new xyz.gabriwar.warpedpixeldungeon.ui.IconButton( Icons.get( Icons.MAGNIFY ) ) {
				@Override
				protected void onClick() {
					if (Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) {
						GameScene.show( new xyz.gabriwar.warpedpixeldungeon.windows.WndWorldMap(
								(xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level ) );
					}
				}

				@Override
				protected String hoverText() {
					return Messages.titleCase( Messages.get(
							xyz.gabriwar.warpedpixeldungeon.windows.WndWorldMap.class, "title" ) );
				}
			};
			mapButton.camera = uiCamera;
			//the sundial panel owns the strip directly under the menu pane while it
			//is shown: the button sits just left of it, and slides into the corner
			//whenever the panel is hidden
			final float mapRight = uiCamera.width - insets.right;
			final float mapY = menu.bottom() + 2;
			mapButton.setRect( mapRight - SundialIndicator.WIDTH - 20, mapY, 18, 18 );
			final xyz.gabriwar.warpedpixeldungeon.ui.IconButton mb = mapButton;
			mapButton.active = true;
			add( new com.watabou.noosa.Gizmo(){
				private boolean lastShown = WPDSettings.sundial();
				@Override
				public void update(){
					boolean shown = WPDSettings.sundial();
					if (shown != lastShown){
						lastShown = shown;
						mb.setRect( shown ? mapRight - SundialIndicator.WIDTH - 20 : mapRight - 20,
								mapY, 18, 18 );
					}
				}
			} );
			if (!WPDSettings.sundial()){
				mapButton.setRect( mapRight - 20, mapY, 18, 18 );
			}
			add( mapButton );
		}

		float extraRight = uiCamera.width - (menuBarMaxLeft + MenuPane.WIDTH);
		if (extraRight > 0){
			SkinnedBlock bar = new SkinnedBlock(extraRight, 20, TextureCache.createSolid(0x88000000));
			bar.x = uiCamera.width - extraRight;
			bar.camera = uiCamera;
			add(bar);

			PointerArea blocker = new PointerArea(uiCamera.width - extraRight, 0, extraRight, 20);
			blocker.camera = uiCamera;
			add(blocker);
		}

		status = new StatusPane( WPDSettings.interfaceSize() > 0 );
		status.camera = uiCamera;
		StatusPane.heroPaneExtraWidth = heroPaneExtraWidth;
		StatusPane.hpBarMaxWidth = hpBarMaxWidth;
		StatusPane.buffBarRowMaxWidths = buffBarRowLimits;
		StatusPane.buffBarRowAdjusts = buffBarRowAdjusts;
		status.setRect(insets.left, uiSize > 0 ? uiCamera.height-39-insets.bottom : screentop, uiCamera.width - insets.left - insets.right, 0 );
		add(status);

		//multiplayer party pane — pinned to the top-center of the safe area. It
		//self-centers on every rebuild (its width isn't known until then).
		if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isActive()) {
			xyz.gabriwar.warpedpixeldungeon.net.ui.NetTurnIndicator turnInd =
					new xyz.gabriwar.warpedpixeldungeon.net.ui.NetTurnIndicator();
			turnInd.camera = uiCamera;
			float netCenterX = insets.left + (uiCamera.width - insets.left - insets.right) / 2f;
			float netTop = Math.max(insets.top, screentop) + 2;
			turnInd.anchorTopCenter(netCenterX, netTop);
			add(turnInd);
		}

		if (uiSize < 2 && largeInsetTop != 0) {
			SkinnedBlock bar = new SkinnedBlock(uiCamera.width, largeInsetTop, TextureCache.createSolid(0x88000000));
			bar.camera = uiCamera;
			add(bar);

			PointerArea blocker = new PointerArea(0, 0, uiCamera.width, largeInsetTop);
			blocker.camera = uiCamera;
			add(blocker);
		}

		boss = new BossHealthBar();
		boss.camera = uiCamera;
		boss.setPos( (uiCamera.width - boss.width())/2, screentop + (landscape() ? 7 : 26));
		if (buffBarRowLimits[2] != 0){
			//if we potentially have a 3rd buff bar row, lower by 7px
			boss.setPos(boss.left(), boss.top() + 7);
		} else if (buffBarRowAdjusts[2] != 0){
			//
			boss.setPos(boss.left(), boss.top() + buffBarRowAdjusts[2]);
		}
		add(boss);

		resume = new ResumeIndicator();
		resume.camera = uiCamera;
		add( resume );

		waypointTag = new xyz.gabriwar.warpedpixeldungeon.ui.WaypointIndicator();
		waypointTag.camera = uiCamera;
		add( waypointTag );

		skillTag = new xyz.gabriwar.warpedpixeldungeon.ui.SkillPointsIndicator();
		skillTag.camera = uiCamera;
		add( skillTag );

		action = new ActionIndicator();
		action.camera = uiCamera;
		add( action );

		loot = new LootIndicator();
		loot.camera = uiCamera;
		add( loot );

		sundialIndicator = new SundialIndicator();
		sundialIndicator.camera = uiCamera;
		//top-right, unfolding under the menu pane (whose sundial cell toggles it), clear
		//of the toolbar/tag/log traffic at the bottom. It slides below the boss health
		//bar when one is up and would overlap.
		SundialIndicator.anchor(
				uiCamera.width - insets.right,
				menu,
				boss.left() + boss.width(),
				boss.bottom() + 2 );
		add( sundialIndicator );

		attack = new AttackIndicator();
		attack.camera = uiCamera;
		add( attack );

		crouch = new CrouchIndicator();
		crouch.camera = uiCamera;
		add( crouch );

		log = new GameLog();
		log.camera = uiCamera;
		log.newLine();
		add( log );

		if (uiSize > 0){
			bringToFront(status);
		}

		toolbar = new Toolbar();
		toolbar.camera = uiCamera;
		add( toolbar );

		if (uiSize == 2) {
			invBottom = insets.bottom;
			inventory = new InventoryPane();
			inventory.camera = uiCamera;
			inventory.setPos(uiCamera.width - inventory.width() - insets.right, uiCamera.height - inventory.height() - insets.bottom);
			add(inventory);

			toolbar.setRect( insets.left, uiCamera.height - toolbar.height() - inventory.height() - insets.bottom, uiCamera.width - insets.right, toolbar.height() );
		} else {
			toolbar.setRect( insets.left, uiCamera.height - toolbar.height() - insets.bottom, uiCamera.width - insets.right, toolbar.height() );
		}

		if (insets.bottom > 0){
			SkinnedBlock bar = new SkinnedBlock(uiCamera.width, insets.bottom, TextureCache.createSolid(0x88000000));
			bar.camera = uiCamera;
			bar.y = uiCamera.height - insets.bottom;
			add(bar);

			PointerArea blocker = new PointerArea(0, uiCamera.height - insets.bottom, uiCamera.width, insets.bottom);
			blocker.camera = uiCamera;
			add(blocker);
		}

		layoutTags();

		if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isNetClient()) {
			// Remote clients skip all game-logic init (talents, badges, quests, etc.)
			Camera.main.snapTo(hero.center().x, hero.center().y);
			fog.updateFog();
			if (wallBlocking != null) wallBlocking.updateMap();
			afterObserve();
			xyz.gabriwar.warpedpixeldungeon.net.SpectatorReceiver.applyPendingBoss(
					xyz.gabriwar.warpedpixeldungeon.net.NetVisuals.getSpectatorMobs());
			fadeIn();
			return;
		}

		switch (InterlevelScene.mode) {
			case RESURRECT:
				Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
				ScrollOfTeleportation.appearVFX( Dungeon.hero );
				SpellSprite.show(Dungeon.hero, SpellSprite.ANKH);
				new Flare( 5, 16 ).color( 0xFFFF00, true ).show( hero, 4f ) ;
				break;
			case RETURN:
				if (Dungeon.level.pit[Dungeon.hero.pos] && !Dungeon.hero.flying){
					//delay this so falling into the chasm processes properly
					WarpedPixelDungeon.runOnRenderThread(new Callback() {
						@Override
						public void call() {
							ScrollOfTeleportation.appearVFX(Dungeon.hero);
						}
					});
				} else {
					ScrollOfTeleportation.appearVFX(Dungeon.hero);
				}
				break;
			case DESCEND:
			case FALL:
				if (Dungeon.hero.isAlive()) {
					Badges.validateNoKilling();
				}
				break;
		}

		ArrayList<Item> dropped = Dungeon.droppedItems.get( Dungeon.depth );
		if (dropped != null) {
			for (Item item : dropped) {
				int pos = Dungeon.level.randomRespawnCell( null );
				if (pos == -1) pos = Dungeon.level.entrance();
				if (item instanceof Potion) {
					((Potion) item).shatter(pos);
				} else if (item instanceof Plant.Seed && !Dungeon.isChallenged(Challenges.NO_HERBALISM)) {
					Dungeon.level.plant((Plant.Seed) item, pos);
				} else if (item instanceof Honeypot) {
					Dungeon.level.drop(((Honeypot) item).shatter(null, pos), pos);
				} else {
					Dungeon.level.drop(item, pos);
				}
			}
			Dungeon.droppedItems.remove( Dungeon.depth );
		}

		Dungeon.hero.next();

		switch (InterlevelScene.mode){
			case FALL: case DESCEND: case CONTINUE:
				Camera.main.snapTo(hero.center().x, hero.center().y - DungeonTilemap.SIZE * (defaultZoom/Camera.main.zoom));
				break;
			case ASCEND:
				Camera.main.snapTo(hero.center().x, hero.center().y + DungeonTilemap.SIZE * (defaultZoom/Camera.main.zoom));
				break;
			default:
				Camera.main.snapTo(hero.center().x, hero.center().y);
		}
		Camera.main.panTo(hero.center(), 2.5f);

		if (InterlevelScene.mode != InterlevelScene.Mode.NONE) {
			if (Dungeon.depth == Statistics.deepestFloor
					&& (InterlevelScene.mode == InterlevelScene.Mode.DESCEND || InterlevelScene.mode == InterlevelScene.Mode.FALL)) {
				GLog.h(Messages.get(this, "descend"), Dungeon.depth);
				Sample.INSTANCE.play(Assets.Sounds.DESCEND);

				if (Dungeon.depth == 1) {
					GLog.i(GameCalendar.dateString());
					String phaseMsg = Messages.get(DayNightCycle.class, DayNightCycle.phase().name().toLowerCase());
					if (phaseMsg != null && !phaseMsg.startsWith("!!!")) {
						GLog.i(phaseMsg);
					}
				}
				
				for (Char ch : Actor.chars()){
					if (ch instanceof DriedRose.GhostHero){
						((DriedRose.GhostHero) ch).sayAppeared();
					}
				}

				int spawnersAbove = Statistics.spawnersAlive;
				if (spawnersAbove > 0 && Dungeon.depth <= 25) {
					for (Mob m : Dungeon.level.mobs) {
						if (m instanceof DemonSpawner && ((DemonSpawner) m).spawnRecorded) {
							spawnersAbove--;
						}
					}

					if (spawnersAbove > 0) {
						if (Dungeon.bossLevel()) {
							GLog.n(Messages.get(this, "spawner_warn_final"));
						} else {
							GLog.n(Messages.get(this, "spawner_warn"));
						}
					}
				}
				
			} else if (InterlevelScene.mode == InterlevelScene.Mode.RESET) {
				GLog.h(Messages.get(this, "warp"));
			} else if (InterlevelScene.mode == InterlevelScene.Mode.RESURRECT) {
				GLog.h(Messages.get(this, "resurrect"), Dungeon.depth);
			} else {
				GLog.h(Messages.get(this, "return"), Dungeon.depth);
			}

			//lunar flavour on arriving at a dungeon floor: the moon reaches down here
			if (Dungeon.branch == 0 && Dungeon.depth >= 1 && Dungeon.depth <= 26) {
				if (ClimateManager.isLunarEclipse() || Dungeon.hero.buff(BloodMoonBuff.class) != null) {
					GLog.w(Messages.get(this, "blood_moon"));
				} else if (GameCalendar.isFullMoon()) {
					GLog.w(Messages.get(this, "full_moon"));
				} else if (GameCalendar.isNewMoon()) {
					GLog.w(Messages.get(this, "new_moon"));
				}
			}

			if (Dungeon.hero.hasTalent(Talent.ROGUES_FORESIGHT)
					&& Dungeon.level instanceof RegularLevel && Dungeon.branch == 0){
				int reqSecrets = Dungeon.level.feeling == Level.Feeling.SECRETS ? 2 : 1;
				for (Room r : ((RegularLevel) Dungeon.level).rooms()){
					if (r instanceof SecretRoom) reqSecrets--;
				}

				//75%/100% chance, use level's seed so that we get the same result for the same level
				//offset seed slightly to avoid output patterns
				Random.pushGenerator(Dungeon.seedCurDepth()+1);
					if (reqSecrets <= 0 && Random.Int(4) < 2+Dungeon.hero.pointsInTalent(Talent.ROGUES_FORESIGHT)){
						GLog.p(Messages.get(this, "secret_hint"));
					}
				Random.popGenerator();
			}

			boolean unspentTalents = false;
			for (int i = 1; i <= Dungeon.hero.talents.size(); i++){
				if (Dungeon.hero.talentPointsAvailable(i) > 0){
					unspentTalents = true;
					break;
				}
			}
			if (unspentTalents){
				GLog.newLine();
				GLog.w( Messages.get(Dungeon.hero, "unspent") );
				StatusPane.talentBlink = 10f;
				WndHero.lastIdx = 1;
			}

			switch (Dungeon.level.feeling) {
				case CHASM:
					GLog.w(Dungeon.level.feeling.desc());
					Notes.add(Notes.Landmark.CHASM_FLOOR);
					break;
				case WATER:
					GLog.w(Dungeon.level.feeling.desc());
					Notes.add(Notes.Landmark.WATER_FLOOR);
					break;
				case GRASS:
					GLog.w(Dungeon.level.feeling.desc());
					Notes.add(Notes.Landmark.GRASS_FLOOR);
					break;
				case DARK:
					GLog.w(Dungeon.level.feeling.desc());
					Notes.add(Notes.Landmark.DARK_FLOOR);
					break;
				case LARGE:
					GLog.w(Dungeon.level.feeling.desc());
					Notes.add(Notes.Landmark.LARGE_FLOOR);
					break;
				case TRAPS:
					GLog.w(Dungeon.level.feeling.desc());
					Notes.add(Notes.Landmark.TRAPS_FLOOR);
					break;
				case SECRETS:
					GLog.w(Dungeon.level.feeling.desc());
					Notes.add(Notes.Landmark.SECRETS_FLOOR);
					break;
			}

			for (Mob mob : Dungeon.level.mobs) {
				if (!mob.buffs(ChampionEnemy.class).isEmpty()) {
					GLog.w(Messages.get(ChampionEnemy.class, "warn"));
				}
			}

			if (Dungeon.hero.buff(AscensionChallenge.class) != null){
				Dungeon.hero.buff(AscensionChallenge.class).saySwitch();
			}

			DimensionalSundial.sundialWarned = true;
			if (DimensionalSundial.spawnMultiplierAtCurrentTime() > 1){
				GLog.w(Messages.get(DimensionalSundial.class, "warning"));
			} else {
				DimensionalSundial.sundialWarned = false;
			}

			InterlevelScene.mode = InterlevelScene.Mode.NONE;

			
		}

		//Tutorial
		if (WPDSettings.intro()){

			if (Document.ADVENTURERS_GUIDE.isPageFound(Document.GUIDE_INTRO)){
				GameScene.flashForDocument(Document.ADVENTURERS_GUIDE, Document.GUIDE_INTRO);
			} else if (ControllerHandler.isControllerConnected()) {
				GameLog.wipe();
				GLog.p(Messages.get(GameScene.class, "tutorial_move_controller"));
			} else if (WPDSettings.interfaceSize() == 0) {
				GameLog.wipe();
				GLog.p(Messages.get(GameScene.class, "tutorial_move_mobile"));
			} else {
				GameLog.wipe();
				GLog.p(Messages.get(GameScene.class, "tutorial_move_desktop"));
			}
			toolbar.visible = toolbar.active = false;
			status.visible = status.active = false;
			if (inventory != null) inventory.visible = inventory.active = false;
		}

		if (!WPDSettings.intro() &&
				Rankings.INSTANCE.totalNumber > 0 &&
				!Document.ADVENTURERS_GUIDE.isPageRead(Document.GUIDE_DIEING)){
			GameScene.flashForDocument(Document.ADVENTURERS_GUIDE, Document.GUIDE_DIEING);
		}

		TrinketCatalyst cata = Dungeon.hero.belongings.getItem(TrinketCatalyst.class);
		if (cata != null && cata.hasRolledTrinkets()){
			addToFront(new TrinketCatalyst.WndTrinket(cata));
		}

		updateItemDisplays = true; // ensure HUD reflects loaded inventory (bags bypass collect())
		restoreInvState();
		fadeIn();

		//re-show WndResurrect if needed
		if (!Dungeon.hero.isAlive()){
			//check if hero has an unblessed ankh
			Ankh ankh = null;
			for (Ankh i : Dungeon.hero.belongings.getAllItems(Ankh.class)){
				if (!i.isBlessed()){
					ankh = i;
				}
			}
			if (ankh != null && GamesInProgress.gameExists(GamesInProgress.curSlot)) {
				add(new WndResurrect(ankh));
			} else {
				gameOver();
			}
		}

	}
	
	public void destroy() {
		
		//tell the actor thread to finish, then wait for it to complete any actions it may be doing.
		if (!waitForActorThread( 4500, true )){
			Throwable t = new Throwable();
			t.setStackTrace(actorThread.getStackTrace());
			throw new RuntimeException("timeout waiting for actor thread! ", t);
		}

		Emitter.freezeEmitters = false;
		
		scene = null;
		Badges.saveGlobal();
		Journal.saveGlobal();
		
		super.destroy();
	}
	
	public static void endActorThread(){
		if (actorThread != null && actorThread.isAlive()){
			Actor.keepActorThreadAlive = false;
			actorThread.interrupt();
		}
	}

	public boolean waitForActorThread(int msToWait, boolean interrupt){
		if (actorThread == null || !actorThread.isAlive()) {
			return true;
		}
		synchronized (actorThread) {
			if (interrupt) actorThread.interrupt();
			try {
				actorThread.wait(msToWait);
			} catch (InterruptedException e) {
				WarpedPixelDungeon.reportException(e);
			}
			return !Actor.processing();
		}
	}
	
	@Override
	public synchronized void onPause() {
		if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isNetClient()) return;
		try {
			if (!Dungeon.hero.ready) waitForActorThread(500, false);
			Dungeon.saveAll();
			Badges.saveGlobal();
			Journal.saveGlobal();
		} catch (IOException e) {
			WarpedPixelDungeon.reportException(e);
		}
	}

	private static Thread actorThread;
	
	//sometimes UI changes can be prompted by the actor thread.
	// We queue any removed element destruction, rather than destroying them in the actor thread.
	private ArrayList<Gizmo> toDestroy = new ArrayList<>();

	//the actor thread processes at a maximum of 60 times a second
	//this caps the speed of resting for higher refresh rate displays
	private float notifyDelay = 1/60f;

	public static boolean updateItemDisplays = false;

	public static boolean tagDisappeared = false;
	public static boolean updateTags = false;

	private static float waterOfs = 0;

	@Override
	public synchronized void update() {
		frameId++;
		lastOffset = null;

		if (updateItemDisplays){
			updateItemDisplays = false;
			QuickSlotButton.refresh();
			InventoryPane.refresh();
			repositionInv();
			if (ActionIndicator.action instanceof MeleeWeapon.Charger) {
				//Champion weapon swap uses items, needs refreshing whenever item displays are updated
				ActionIndicator.refresh();
			}
		}

		if (Dungeon.hero == null || scene == null) {
			return;
		}

		super.update();

		if (!xyz.gabriwar.warpedpixeldungeon.net.NetManager.isNetClient()) {
			updateDayNightTint();
			updateWeather();
		}
		lerpDayNightTint(Game.elapsed);

		if (notifyDelay > 0) notifyDelay -= Game.elapsed;

		if (!Emitter.freezeEmitters) {
			waterOfs -= 5 * Game.elapsed;
			water.offsetTo( 0, waterOfs );
			waterOfs = water.offsetY(); //re-assign to account for auto adjust
		}

		if (!xyz.gabriwar.warpedpixeldungeon.net.NetManager.isNetClient()
				&& !Actor.processing() && Dungeon.hero.isAlive()) {
			if (actorThread == null || !actorThread.isAlive()) {
				
				actorThread = new Thread() {
					@Override
					public void run() {
						Actor.process();
					}
				};

				//if cpu cores are limited, game should prefer drawing the current frame
				if (Runtime.getRuntime().availableProcessors() == 1) {
					actorThread.setPriority(Thread.NORM_PRIORITY - 1);
				}
				actorThread.setName("WPD Actor Thread");
				Thread.currentThread().setName("WPD Render Thread");
				Actor.keepActorThreadAlive = true;
				actorThread.start();
			} else if (notifyDelay <= 0f) {
				notifyDelay += 1/60f;
				synchronized (actorThread) {
					actorThread.notify();
				}
			}
		}

		if (Dungeon.hero.ready && Dungeon.hero.paralysed == 0) {
			log.newLine();
		}

		if (updateTags){
			tagAttack = attack.active;
			tagLoot = loot.visible;
			tagAction = action.visible;
			tagResume = resume.visible;
			tagWaypoint = waypointTag.visible;
			tagSkills = skillTag.visible;
			tagCrouch = crouch.visible;

			layoutTags();

		} else if (tagAttack != attack.active ||
				tagLoot != loot.visible ||
				tagAction != action.visible ||
				tagResume != resume.visible ||
				tagWaypoint != waypointTag.visible ||
				tagSkills != skillTag.visible ||
				tagCrouch != crouch.visible) {

			boolean tagAppearing = (attack.active && !tagAttack) ||
									(loot.visible && !tagLoot) ||
									(action.visible && !tagAction) ||
									(resume.visible && !tagResume) ||
									(waypointTag.visible && !tagWaypoint) ||
									(skillTag.visible && !tagSkills) ||
									(crouch.visible && !tagCrouch);

			tagAttack = attack.active;
			tagLoot = loot.visible;
			tagAction = action.visible;
			tagResume = resume.visible;
			tagWaypoint = waypointTag.visible;
			tagSkills = skillTag.visible;
			tagCrouch = crouch.visible;

			//if a new tag appears, re-layout tags immediately
			//otherwise, wait until the hero acts, so as to not suddenly change their position
			if (tagAppearing)   layoutTags();
			else                tagDisappeared = true;

		}

		// Net players: gate the cursor on THIS client's turn, not Dungeon.hero.ready
		// (the host's hero mirror). Runs every frame, so it would otherwise re-disable
		// input after any window closes.
		cellSelector.enable(xyz.gabriwar.warpedpixeldungeon.net.NetManager.isPlayer()
				? xyz.gabriwar.warpedpixeldungeon.net.NetManager.isMyTurn()
				: Dungeon.hero.ready);

		if (!toDestroy.isEmpty()) {
			for (Gizmo g : toDestroy) {
				g.destroy();
			}
			toDestroy.clear();
		}
	}

	private static Point lastOffset = null;

	@Override
	public synchronized Gizmo erase (Gizmo g) {
		Gizmo result = super.erase(g);
		if (result instanceof Window){
			lastOffset = ((Window) result).getOffset();
		}
		return result;
	}

	private boolean tagAttack    = false;
	private boolean tagLoot      = false;
	private boolean tagAction    = false;
	private boolean tagResume    = false;
	private boolean tagWaypoint  = false;
	private boolean tagSkills    = false;
	private boolean tagCrouch    = false;

	public static void layoutTags() {

		updateTags = false;

		if (scene == null) return;

		//move the camera center up a bit if we're on full UI and it is taking up lots of space
		if (scene.inventory != null && scene.inventory.visible
				&& (uiCamera.width < 460 && uiCamera.height < 300)){
			Camera.main.setCenterOffset(0, Math.min(300-uiCamera.height, 460-uiCamera.width) / Camera.main.zoom);
		} else {
			Camera.main.setCenterOffset(0, 0);
		}
		//Camera.main.panTo(Dungeon.hero.sprite.center(), 5f);

		//adjust spacing for elements based on display cutouts
		// We use ALL here as some elements can be a fair but up the side of the screen
		RectF insets = Game.platform.getSafeInsets( PlatformSupport.INSET_ALL );
		insets = insets.scale(1f / uiCamera.zoom);

		boolean tagsOnLeft = WPDSettings.flipTags();
		float tagWidth = Tag.SIZE + (tagsOnLeft ? insets.left : insets.right);
		float tagLeft = tagsOnLeft ? 0 : uiCamera.width - tagWidth;

		float y = WPDSettings.interfaceSize() == 0 ? scene.toolbar.top()-2 : scene.status.top()-2;
		if (scene.tagCrouch) {
			y -= Tag.SIZE;
		}
		if (WPDSettings.interfaceSize() == 0){
			if (tagsOnLeft) {
				scene.log.setRect(tagWidth, y, uiCamera.width - tagWidth - insets.right, 0);
			} else {
				scene.log.setRect(insets.left, y, uiCamera.width - tagWidth - insets.left, 0);
			}
		} else {
			if (tagsOnLeft) {
				scene.log.setRect(tagWidth, y, 160 - tagWidth, 0);
			} else {
				scene.log.setRect(insets.left, y, 160 - insets.left, 0);
			}
		}

		float pos = scene.toolbar.top();
		if (tagsOnLeft && WPDSettings.interfaceSize() > 0){
			pos = scene.status.top();
		}

		//Crouch is always anchored to the left edge, independent of the other tags. Laid out
		//first so that a flipped tag stack starts above it instead of on top of it - and in
		//full UI it anchors to the status pane, which spans the whole width down there.
		if (scene.tagCrouch) {
			float crouchTagWidth = Tag.SIZE + insets.left;
			float crouchTop = WPDSettings.interfaceSize() == 0 ? scene.toolbar.top() : scene.status.top();
			scene.crouch.setRect( 0, crouchTop - Tag.SIZE, crouchTagWidth, Tag.SIZE );
			//left-edge tag: flip so the rounded corner faces inward and the flat side
			//bleeds off the screen edge (unflipped looks cut off, only rounded on the left)
			scene.crouch.flip( true );
			if (tagsOnLeft) {
				pos = scene.crouch.top();
			}
		}

		if (scene.tagAttack){
			scene.attack.setRect( tagLeft, pos - Tag.SIZE, tagWidth, Tag.SIZE );
			scene.attack.flip(tagsOnLeft);
			pos = scene.attack.top();
		}

		if (scene.tagLoot) {
			scene.loot.setRect( tagLeft, pos - Tag.SIZE, tagWidth, Tag.SIZE );
			scene.loot.flip(tagsOnLeft);
			pos = scene.loot.top();
		}

		if (scene.tagAction) {
			scene.action.setRect( tagLeft, pos - Tag.SIZE, tagWidth, Tag.SIZE );
			scene.action.flip(tagsOnLeft);
			pos = scene.action.top();
		}

		if (scene.tagResume) {
			scene.resume.setRect( tagLeft, pos - Tag.SIZE, tagWidth, Tag.SIZE );
			scene.resume.flip(tagsOnLeft);
			pos = scene.resume.top();
		}

		if (scene.tagWaypoint) {
			scene.waypointTag.setRect( tagLeft, pos - Tag.SIZE, tagWidth, Tag.SIZE );
			scene.waypointTag.flip(tagsOnLeft);
			pos = scene.waypointTag.top();
		}

		if (scene.tagSkills) {
			scene.skillTag.setRect( tagLeft, pos - Tag.SIZE, tagWidth, Tag.SIZE );
			scene.skillTag.flip(tagsOnLeft);
			pos = scene.skillTag.top();
		}

	}
	
	@Override
	protected void onBackPressed() {
		if (!cancel()) {
			add( new WndGame() );
		}
	}

	public void addCustomTile( CustomTilemap visual){
		customTiles.add( visual.create() );
	}

	public void addCustomWall( CustomTilemap visual){
		customWalls.add( visual.create() );
	}

	private void addHeapSprite( Heap heap ) {
		ItemSprite sprite = heap.sprite = (ItemSprite)heaps.recycle( ItemSprite.class );
		sprite.revive();
		sprite.link( heap );
		heaps.add( sprite );
	}
	
	private void addDiscardedSprite( Heap heap ) {
		heap.sprite = (DiscardedItemSprite)heaps.recycle( DiscardedItemSprite.class );
		heap.sprite.revive();
		heap.sprite.link( heap );
		heaps.add( heap.sprite );
	}
	
	private void addBlobSprite( final Blob gas ) {
		if (gas.emitter == null) {
			gases.add( new BlobEmitter( gas ) );
		}
	}
	
	private synchronized void addMobSprite( Mob mob ) {
		CharSprite sprite = mob.sprite();
		sprite.visible = Dungeon.level.heroFOV[mob.pos];
		mobs.add( sprite );
		sprite.link( mob );
		sortMobSprites();
		if (mob instanceof xyz.gabriwar.warpedpixeldungeon.net.SpectatorReceiver.SpectatorMob) {
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-CLI] addMobSprite for " + mob.getClass().getSimpleName()
					+ " pos=" + mob.pos
					+ " spriteClass=" + (sprite == null ? "null" : sprite.getClass().getSimpleName())
					+ " visible=" + (sprite == null ? "n/a" : sprite.visible)
					+ " fovAtPos=" + Dungeon.level.heroFOV[mob.pos]);
		}
	}

	//ensures that mob sprites are drawn from top to bottom, in case of overlap
	public static void sortMobSprites(){
		if (scene != null){
			synchronized (scene) {
				scene.mobs.sort(new Comparator() {
					@Override
					public int compare(Object a, Object b) {
						//elements that aren't visual go to the end of the list
						if (a instanceof Visual && b instanceof Visual) {
							return (int) Math.signum((((Visual) a).y + ((Visual) a).height())
									- (((Visual) b).y + ((Visual) b).height()));
						} else if (a instanceof Visual){
							return -1;
						} else if (b instanceof Visual){
							return 1;
						} else {
							return 0;
						}
					}
				});
			}
		}
	}
	
	private synchronized void prompt( String text ) {
		
		if (prompt != null) {
			prompt.killAndErase();
			toDestroy.add(prompt);
			prompt = null;
		}
		
		if (text != null) {
			prompt = new Toast( text ) {
				@Override
				protected void onClose() {
					cancel();
				}
			};
			prompt.camera = uiCamera;
			prompt.setPos( (uiCamera.width - prompt.width()) / 2, uiCamera.height - 60 );

			if (inventory != null && inventory.visible && prompt.right() > inventory.left() - 10){
				prompt.setPos(inventory.left() - prompt.width() - 10, prompt.top());
			}

			add( prompt );
		}
	}
	
	private void showBanner( Banner banner ) {
		banner.camera = uiCamera;

		float offset = Camera.main.centerOffset.y;
		banner.x = align( uiCamera, (uiCamera.width - banner.width) / 2 );
		banner.y = align( uiCamera, (uiCamera.height - banner.height) / 2 - 32 - offset );

		addToFront( banner );
	}
	
	// -------------------------------------------------------
	
	public static void add( Blob gas ) {
		Actor.add( gas );
		if (scene != null) {
			scene.addBlobSprite( gas );
		}
	}
	
	public static void add( Heap heap ) {
		if (scene != null) {
			//heaps that aren't added as part of levelgen don't count for exploration bonus
			heap.autoExplored = true;
			scene.addHeapSprite( heap );
		}
	}
	
	public static void discard( Heap heap ) {
		if (scene != null) {
			scene.addDiscardedSprite( heap );
		}
	}
	
	public static void add( Mob mob ) {
		add( mob, 0);
	}

	public static void addSprite( Mob mob ) {
		scene.addMobSprite( mob );
	}
	
	public static void add( Mob mob, float delay ) {
		Dungeon.level.mobs.add( mob );
		//mobs added on partial turns wait until next full turn to act
		delay = (float)Math.ceil(Actor.now() + delay) - Actor.now();
		if (scene != null) {
			scene.addMobSprite(mob);
			Actor.addDelayed(mob, delay);
			mob.spendToWhole();
		}
	}
	
	public static void add( EmoIcon icon ) {
		scene.emoicons.add( icon );
	}
	
	public static void add( CharHealthIndicator indicator ){
		if (scene != null) scene.healthIndicators.add(indicator);
	}
	
	public static void add( CustomTilemap t, boolean wall ){
		if (scene == null) return;
		if (wall){
			scene.addCustomWall(t);
		} else {
			scene.addCustomTile(t);
		}
	}
	
	public static void effect( Visual effect ) {
		if (scene != null) scene.effects.add( effect );
	}

	public static void effectOverFog( Visual effect ) {
		scene.overFogEffects.add( effect );
	}
	
	public static Ripple ripple( int pos ) {
		if (scene != null) {
			Ripple ripple = (Ripple) scene.ripples.recycle(Ripple.class);
			ripple.reset(pos);
			return ripple;
		} else {
			return null;
		}
	}
	
	public static synchronized SpellSprite spellSprite() {
		return (SpellSprite)scene.spells.recycle( SpellSprite.class );
	}
	
	public static synchronized Emitter emitter() {
		if (scene != null) {
			Emitter emitter = (Emitter)scene.emitters.recycle( Emitter.class );
			emitter.revive();
			return emitter;
		} else {
			return null;
		}
	}

	public static synchronized Emitter floorEmitter() {
		if (scene != null) {
			Emitter emitter = (Emitter)scene.floorEmitters.recycle( Emitter.class );
			emitter.revive();
			return emitter;
		} else {
			return null;
		}
	}
	
	public static FloatingText status() {
		return scene != null ? (FloatingText)scene.statuses.recycle( FloatingText.class ) : null;
	}
	
	public static void pickUp( Item item, int pos ) {
		if (scene != null) scene.toolbar.pickup( item, pos );
	}

	public static void pickUpJournal( Item item, int pos ) {
		if (scene != null) scene.menu.pickup( item, pos );
	}

	public static void flashForDocument( Document doc, String page ){
		if (scene != null) {
			if (doc == Document.ADVENTURERS_GUIDE){
				if (!page.equals(Document.GUIDE_INTRO)) {
					if (WPDSettings.interfaceSize() == 0) {
						GLog.p(Messages.get(Guidebook.class, "hint_mobile"));
					} else {
						GLog.p(Messages.get(Guidebook.class, "hint_desktop", KeyBindings.getKeyName(KeyBindings.getFirstKeyForAction(WPDAction.JOURNAL, ControllerHandler.isControllerConnected()))));
					}
				}
				Dungeon.hero.sprite.showStatus(CharSprite.POSITIVE, Messages.get(Guidebook.class, "hint_status"));
			}
			scene.menu.flashForPage( doc, page );
		}
	}

	public static void endIntro(){
		if (scene != null){
			WPDSettings.intro(false);
			scene.add(new Tweener(scene, 2f){
				@Override
				protected void updateValues(float progress) {
					if (progress <= 0.5f) {
						scene.status.alpha(2*progress);
						scene.status.visible = scene.status.active = true;
						scene.toolbar.visible = scene.toolbar.active = false;
						if (scene.inventory != null) scene.inventory.visible = scene.inventory.active = false;
					} else {
						scene.status.alpha(1f);
						scene.status.visible = scene.status.active = true;
						scene.toolbar.alpha((progress - 0.5f)*2);
						scene.toolbar.visible = scene.toolbar.active = true;
						if (scene.inventory != null){
							scene.inventory.visible = scene.inventory.active = true;
							scene.inventory.alpha((progress - 0.5f)*2);
						}
					}
				}
			});
			GameLog.wipe();
			if (WPDSettings.interfaceSize() == 0){
				GLog.p(Messages.get(GameScene.class, "tutorial_ui_mobile"));
			} else {
				GLog.p(Messages.get(GameScene.class, "tutorial_ui_desktop",
						KeyBindings.getKeyName(KeyBindings.getFirstKeyForAction(WPDAction.HERO_INFO, ControllerHandler.isControllerConnected())),
						KeyBindings.getKeyName(KeyBindings.getFirstKeyForAction(WPDAction.INVENTORY, ControllerHandler.isControllerConnected()))));
			}

			//clear hidden doors, it's floor 1 so there are only the entrance ones
			for (int i = 0; i < Dungeon.level.length(); i++){
				if (Dungeon.level.map[i] == Terrain.SECRET_DOOR){
					Dungeon.level.discover(i);
					discoverTile(i, Terrain.SECRET_DOOR);
				}
			}
		}
	}
	
	public static void updateKeyDisplay(){
		if (scene != null && scene.menu != null) scene.menu.updateKeys();
	}

	public static void showlevelUpStars(){
		if (scene != null && scene.status != null) scene.status.showStarParticles();
	}

	public static void updateAvatar(){
		if (scene != null && scene.status != null) scene.status.updateAvatar();
	}

	public static void resetMap() {
		if (scene != null) {
			scene.tiles.map(Dungeon.level.map, Dungeon.level.width() );
			scene.visualGrid.map(Dungeon.level.map, Dungeon.level.width() );
			scene.terrainFeatures.map(Dungeon.level.map, Dungeon.level.width() );
			scene.raisedTerrain.map(Dungeon.level.map, Dungeon.level.width() );
			scene.walls.map(Dungeon.level.map, Dungeon.level.width() );
		}
		updateFog();
	}

	//updates the whole map
	/**
	 * Slides every TRANSIENT world-space visual by a pixel delta: live
	 * particles, emitters and their spawn rects, floating damage numbers,
	 * in-flight spell effects, ripples. Data-anchored visuals (tilemaps,
	 * char/heap sprites) are repositioned from data by the caller. Together
	 * with an equal camera-scroll shift this makes a sliding-window rebase
	 * render pixel-identically - the stream becomes invisible.
	 */
	//frame counter for diagnosing cross-thread tearing
	public static long frameId = 0;

	public static void shiftWorldVisuals( float sx, float sy ){
		if (scene == null) return;
		com.watabou.noosa.Group[] groups = {
				scene.ripples, scene.floorEmitters, scene.emitters, scene.effects,
				scene.gases, scene.spells, scene.statuses, scene.emoicons,
				scene.overFogEffects };
		for (com.watabou.noosa.Group g : groups){
			if (g != null) shiftRec( g, sx, sy );
		}
		//note: the water plane is NOT compensated - its texture is a uniform
		//repeating pattern and an offset here proved worse than the (sub-tile)
		//jump it tried to hide
	}

	private static void shiftRec( com.watabou.noosa.Gizmo g, float sx, float sy ){
		if (g == null) return;
		if (g instanceof com.watabou.noosa.ui.Component){
			//components move their own children on setPos - no recursion
			com.watabou.noosa.ui.Component c = (com.watabou.noosa.ui.Component) g;
			c.setPos( c.left() + sx, c.top() + sy );
			return;
		}
		if (g instanceof com.watabou.noosa.particles.Emitter){
			com.watabou.noosa.particles.Emitter e = (com.watabou.noosa.particles.Emitter) g;
			e.x += sx;
			e.y += sy;
		} else if (g instanceof com.watabou.noosa.Visual){
			((com.watabou.noosa.Visual) g).x += sx;
			((com.watabou.noosa.Visual) g).y += sy;
		}
		if (g instanceof com.watabou.noosa.Group){
			for (com.watabou.noosa.Gizmo child
					: ((com.watabou.noosa.Group) g).membersView()){
				shiftRec( child, sx, sy );
			}
		}
	}

	/**
	 * Sliding-window rebase fast path: shifts the cached content of every
	 * tilemap and the fog by the window delta, recomputing only the exposed
	 * strips - the full updateMap() re-ran the whole tile-visual pipeline
	 * (9216 cells x 6 maps) plus a full fog rebuild in a single frame, which
	 * was the visible stutter at every chunk crossing.
	 */
	public static void shiftMapContent( int dcx, int dcy ){
		if (scene == null) return;
		xyz.gabriwar.warpedpixeldungeon.tiles.DungeonWallsTilemap.shiftSkipCells(
				dcx, dcy, Dungeon.level.width(), Dungeon.level.height() );
		scene.tiles.shiftAndUpdate( dcx, dcy );
		scene.visualGrid.shiftAndUpdate( dcx, dcy );
		scene.terrainFeatures.shiftAndUpdate( dcx, dcy );
		scene.raisedTerrain.shiftAndUpdate( dcx, dcy );
		scene.walls.shiftAndUpdate( dcx, dcy );
		scene.fog.shiftContent( dcx, dcy );
		scene.wallBlocking.updateMap();
		updateDayNightTint();
	}

	public static void updateMap() {
		if (scene != null) {
			scene.tiles.updateMap();
			scene.visualGrid.updateMap();
			scene.terrainFeatures.updateMap();
			scene.raisedTerrain.updateMap();
			scene.walls.updateMap();
			updateFog();
		}
	}
	
	public static void updateMap( int cell ) {
		if (scene != null) {
			scene.tiles.updateMapCell( cell );
			scene.visualGrid.updateMapCell( cell );
			scene.terrainFeatures.updateMapCell( cell );
			scene.raisedTerrain.updateMapCell( cell );
			scene.walls.updateMapCell( cell );
			//update adjacent cells too
			updateFog( cell, 1 );
		}
	}

	public static void plantSeed( int cell ) {
		if (scene != null) {
			scene.terrainFeatures.growPlant( cell );
		}
	}

	public static void discoverTile( int pos, int oldValue ) {
		if (scene != null) {
			scene.tiles.discover( pos, oldValue );
		}
	}
	
	public static void show( Window wnd ) {
		if (scene != null) {
			cancel();

			//If a window is already present (or was just present)
			// then inherit the offset it had
			if (scene.inventory != null && scene.inventory.visible){
				Point offsetToInherit = null;
				for (Gizmo g : scene.members){
					if (g instanceof Window) offsetToInherit = ((Window) g).getOffset();
				}
				if (lastOffset != null) {
					offsetToInherit = lastOffset;
				}
				if (offsetToInherit != null && !offsetToInherit.isZero()) {
					wnd.offset(offsetToInherit);
					wnd.boundOffsetWithMargin(3);
				}
			}

			scene.addToFront(wnd);
		}
	}

	public static boolean showingWindow(){
		if (scene == null) return false;

		for (Gizmo g : scene.members){
			if (g instanceof Window) return true;
		}

		return false;
	}

	public static boolean interfaceBlockingHero(){
		if (scene == null) return false;

		if (showingWindow()) return true;

		if (scene.inventory != null && scene.inventory.isSelecting()){
			return true;
		}

		return false;
	}

	public static void toggleInvPane(){
		if (scene != null && scene.inventory != null){
			invState = (invState + 1) % 3;
			applyInvState();
			layoutTags();
		}
	}

	private static void applyInvState(){
		if (scene == null || scene.inventory == null) return;
		switch (invState) {
			case 1: // hidden
				scene.inventory.setExpanded(false);
				scene.inventory.visible = scene.inventory.active = false;
				scene.toolbar.setPos(scene.toolbar.left(), uiCamera.height - scene.toolbar.height() - scene.invBottom);
				break;
			case 2: // expanded (fuller backpack)
				scene.inventory.setExpanded(true);
				scene.inventory.visible = scene.inventory.active = true;
				scene.inventory.setPos(scene.inventory.left(), uiCamera.height - scene.inventory.height() - scene.invBottom);
				scene.toolbar.setPos(scene.toolbar.left(), scene.inventory.top() - scene.toolbar.height());
				break;
			default: // 0: normal visible
				scene.inventory.setExpanded(false);
				scene.inventory.visible = scene.inventory.active = true;
				scene.inventory.setPos(scene.inventory.left(), uiCamera.height - scene.inventory.height() - scene.invBottom);
				scene.toolbar.setPos(scene.toolbar.left(), scene.inventory.top() - scene.toolbar.height());
				break;
		}
	}

	private static void restoreInvState(){
		applyInvState();
	}

	private static boolean repositioning = false;
	public static void repositionInv(){
		if (repositioning) return;
		repositioning = true;
		applyInvState();
		repositioning = false;
	}

	public static void centerNextWndOnInvPane(){
		if (scene != null && scene.inventory != null && scene.inventory.visible){
			lastOffset = new Point((int)scene.inventory.centerX() - uiCamera.width/2,
					(int)scene.inventory.centerY() - uiCamera.height/2);
		}
	}

	public static void updateFog(){
		if (scene != null) {
			scene.fog.updateFog();
			scene.wallBlocking.updateMap();
			updateDayNightTint();
		}
	}

	public static void updateDayNightTint(){
		if (scene != null && scene.dayNightOverlay != null) {
			float[] tint = DayNightCycle.phaseTintSmooth();
			float brightness = DayNightCycle.brightnessMult();

			float tintA = tint[0];
			float tintR = tint[1];
			float tintG = tint[2];
			float tintB = tint[3];

			// When brightness < 1, add darkness. Blend dark overlay with color tint.
			if (brightness < 1f) {
				float darkA = 1f - brightness;
				float totalA = Math.min(1f, darkA + tintA);
				if (totalA > 0) {
					float w = tintA / totalA;
					tintR *= w;
					tintG *= w;
					tintB *= w;
				}
				tintA = totalA;
			}

			// Apply directly — values are already smoothly interpolated
			tgtTintR = tintR;
			tgtTintG = tintG;
			tgtTintB = tintB;
			tgtTintA = tintA;
		}
	}

	private void lerpDayNightTint(float elapsed) {
		if (dayNightOverlay == null) return;

		float speed = TINT_LERP_SPEED * elapsed;
		curTintR += (tgtTintR - curTintR) * speed;
		curTintG += (tgtTintG - curTintG) * speed;
		curTintB += (tgtTintB - curTintB) * speed;
		curTintA += (tgtTintA - curTintA) * speed;

		// Snap when very close to avoid endless drifting
		if (Math.abs(curTintA - tgtTintA) < 0.005f) {
			curTintR = tgtTintR;
			curTintG = tgtTintG;
			curTintB = tgtTintB;
			curTintA = tgtTintA;
		}

		dayNightOverlay.hardlight(curTintR, curTintG, curTintB);
		dayNightOverlay.alpha(curTintA);
	}

	/**
	 * Reads current climate state and drives the WeatherOverlay.
	 * Called every frame — the overlay's own guards prevent redundant emitter work.
	 */
	private static void updateWeather() {
		if (scene == null || scene.weatherOverlay == null) return;

		// The place's own fog (the overworld's swamps at dawn) feeds the climate first
		ClimateManager.setLocalFog(
				Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel
						&& ((xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level).localFog() );

		// Precipitation
		scene.weatherOverlay.setPrecipitation(
				ClimateManager.localPrecipType(),
				ClimateManager.localPrecipRate()
		);

		// Ambient particles
		ClimateManager.WeatherOverlayAmbient cm = ClimateManager.ambientType();
		WeatherOverlay.AmbientType ambient;
		switch (cm) {
			case FIREFLIES:     ambient = WeatherOverlay.AmbientType.FIREFLIES;     break;
			case AUTUMN_LEAVES: ambient = WeatherOverlay.AmbientType.AUTUMN_LEAVES; break;
			case SPRING_PETALS: ambient = WeatherOverlay.AmbientType.SPRING_PETALS; break;
			case MIST:          ambient = WeatherOverlay.AmbientType.MIST;          break;
			case DUST:          ambient = WeatherOverlay.AmbientType.DUST;          break;
			case ASH:           ambient = WeatherOverlay.AmbientType.ASH;           break;
			case STEAM:         ambient = WeatherOverlay.AmbientType.STEAM;         break;
			case CORONA:        ambient = WeatherOverlay.AmbientType.CORONA;        break;
			case DRIP:          ambient = WeatherOverlay.AmbientType.DRIP;          break;
			case AURORA:        // handled by AuroraOverlay below
			case RAINBOW:       // handled by RainbowOverlay below
			default:            ambient = WeatherOverlay.AmbientType.NONE;           break;
		}
		scene.weatherOverlay.setAmbient(ambient);

		// Aurora overlay — show/hide based on climate state
		if (scene.auroraOverlay != null) {
			if (cm == ClimateManager.WeatherOverlayAmbient.AURORA) {
				scene.auroraOverlay.show();
			} else {
				scene.auroraOverlay.hide();
			}
		}

		// Rainbow overlay — show/hide based on climate state
		if (scene.rainbowOverlay != null) {
			if (cm == ClimateManager.WeatherOverlayAmbient.RAINBOW) {
				scene.rainbowOverlay.show();
			} else {
				scene.rainbowOverlay.hide();
			}
		}
	}

	// --- Network spectator helpers ---

	public static void refreshNetHeaps() {
		if (scene == null) return;
		scene.heaps.clear();
		for (Heap heap : Dungeon.level.heaps.valueList()) {
			scene.addHeapSprite(heap);
		}
	}

	/** Add a network player hero sprite to the scene (host side) */
	public static void addNetHero( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero ) {
		if (scene == null) return;
		// Create a HeroSprite — but we need to temporarily set Dungeon.hero
		// so HeroSprite constructor can read the class. Instead, use a MobSprite approach.
		xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite sprite =
				new xyz.gabriwar.warpedpixeldungeon.sprites.HeroSprite(hero);
		sprite.visible = true;
		scene.mobs.add(sprite);
		sprite.place(hero.pos);
	}

	/** Enable cell selector for remote player input */
	public static void enablePlayerInput() {
		if (scene == null || cellSelector == null) return;
		cellSelector.enabled = true;
		cellSelector.listener = playerCellListener;
	}

	/** Cell listener for remote players — sends action to host instead of local processing */
	private static final CellSelector.Listener playerCellListener = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-CLI] playerCellListener.onSelect cell=" + cell);
			if (cell != null && cell >= 0) {
				// Client tapped the shopkeeper portal gate → open its UI locally (state is
				// mirrored from the host); pay/travel get routed back as actions. Host
				// validates adjacency, so tapping from afar just shows a no-op window.
				xyz.gabriwar.warpedpixeldungeon.Portals.Record pr =
						xyz.gabriwar.warpedpixeldungeon.net.NetManager.isPlayer()
								? xyz.gabriwar.warpedpixeldungeon.Portals.get(Dungeon.depth) : null;
				if (pr != null && pr.cell == cell) {
					xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.PortalGate gate =
							new xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.PortalGate();
					gate.state = pr.state;
					xyz.gabriwar.warpedpixeldungeon.net.NetManager.sendPlayerActionTyped("portal_open");
					GameScene.show(new xyz.gabriwar.warpedpixeldungeon.windows.WndPortal(gate));
					cellSelector.enabled = false;
					return;
				}
				xyz.gabriwar.warpedpixeldungeon.net.NetManager.sendPlayerAction(cell);
			}
			cellSelector.enabled = false;
		}
		@Override
		public void onRightClick(Integer cell) {
			// Allow examine but don't send action
			if (cell != null) {
				examineCell(cell);
			}
		}
		@Override
		public String prompt() { return "Your turn — tap where to act"; }
	};

	public static void addNetBlobSprite( Blob gas ) {
		// scene.gases is initialized in create(); during a scene transition (e.g.,
		// initial full-state arrival just before switchScene) the static `scene`
		// can still point at the previous instance whose Group fields were torn
		// down. Guard against the NPE — applyPendingBlobs() will resync once the
		// new GameScene is up.
		if (scene != null && scene.gases != null && gas.emitter == null) {
			scene.gases.add( new xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter( gas ) );
		}
	}

	public static WeatherOverlay getWeatherOverlay() {
		return scene != null ? scene.weatherOverlay : null;
	}

	public static void showAurora() {
		if (scene != null && scene.auroraOverlay != null) scene.auroraOverlay.show();
	}

	public static void hideAurora() {
		if (scene != null && scene.auroraOverlay != null) scene.auroraOverlay.hide();
	}

	public static void showRainbow() {
		if (scene != null && scene.rainbowOverlay != null) scene.rainbowOverlay.show();
	}

	public static void hideRainbow() {
		if (scene != null && scene.rainbowOverlay != null) scene.rainbowOverlay.hide();
	}

	public static void setDayNightTint(float[] tint, float brightness) {
		if (scene != null && scene.dayNightOverlay != null) {
			float tintA = tint[0];
			float tintR = tint[1];
			float tintG = tint[2];
			float tintB = tint[3];

			if (brightness < 1f) {
				float darkA = 1f - brightness;
				float totalA = Math.min(1f, darkA + tintA);
				if (totalA > 0) {
					float w2 = tintA / totalA;
					tintR *= w2;
					tintG *= w2;
					tintB *= w2;
				}
				tintA = totalA;
			}

			tgtTintR = tintR;
			tgtTintG = tintG;
			tgtTintB = tintB;
			tgtTintA = tintA;
		}
	}

	public static void updateFog(int x, int y, int w, int h){
		if (scene != null) {
			scene.fog.updateFogArea(x, y, w, h);
			scene.wallBlocking.updateArea(x, y, w, h);
		}
	}
	
	public static void updateFog( int cell, int radius ){
		if (scene != null) {
			scene.fog.updateFog( cell, radius );
			scene.wallBlocking.updateArea( cell, radius );
		}
	}
	
	public static void afterObserve() {
		if (scene != null) {
			for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
				if (mob.sprite != null) {
					if (mob instanceof Mimic && mob.state == mob.PASSIVE && ((Mimic) mob).stealthy() && Dungeon.level.visited[mob.pos]){
						//mimics stay visible in fog of war after being first seen
						mob.sprite.visible = true;
					} else {
						mob.sprite.visible = Dungeon.level.heroFOV[mob.pos];
					}
				}
				if (mob instanceof Ghoul){
					for (Ghoul.GhoulLifeLink link : mob.buffs(Ghoul.GhoulLifeLink.class)){
						link.updateVisibility();
					}
				}
			}
		}
	}

	public static void flash( int color ) {
		flash( color, true);
	}

	public static void flash( int color, boolean lightmode ) {
		if (scene != null) {
			//don't want to do this on the actor thread
			WarpedPixelDungeon.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					//greater than 0 to account for negative values (which have the first bit set to 1)
					if (scene != null) {
						if (color > 0 && color < 0x01000000) {
							scene.fadeIn(0xFF000000 | color, lightmode);
						} else {
							scene.fadeIn(color, lightmode);
						}
					}
				}
			});
		}
	}

	public static void gameOver() {
		if (scene == null) return;

		Banner gameOver = new Banner( BannerSprites.get( BannerSprites.Type.GAME_OVER ) );
		gameOver.show( 0x000000, 2f );
		scene.showBanner( gameOver );

		StyledButton restart = new StyledButton(Chrome.Type.GREY_BUTTON_TR, Messages.get(StartScene.class, "new"), 9){
			@Override
			protected void onClick() {
				GamesInProgress.selectedClass = Dungeon.hero.heroClass;
				GamesInProgress.curSlot = GamesInProgress.firstEmpty();
				WarpedPixelDungeon.switchScene(HeroSelectScene.class);
			}

			@Override
			public void update() {
				alpha((float)Math.pow(gameOver.am, 2));
				super.update();
			}
		};
		restart.icon(Icons.get(Icons.ENTER));
		restart.alpha(0);
		restart.camera = uiCamera;
		float offset = Camera.main.centerOffset.y;
		restart.setSize(Math.max(80, restart.reqWidth()), 20);
		restart.setPos(
				align(uiCamera, (restart.camera.width - restart.width()) / 2),
				align(uiCamera, (restart.camera.height - restart.height()) / 2 + 8 - offset)
		);
		scene.add(restart);

		StyledButton menu = new StyledButton(Chrome.Type.GREY_BUTTON_TR, Messages.get(WndKeyBindings.class, "menu"), 9){
			@Override
			protected void onClick() {
				GameScene.show(new WndGame());
			}

			@Override
			public void update() {
				alpha((float)Math.pow(gameOver.am, 2));
				super.update();
			}
		};
		menu.icon(Icons.get(Icons.PREFS));
		menu.alpha(0);
		menu.camera = uiCamera;
		menu.setSize(Math.max(80, menu.reqWidth()), 20);
		menu.setPos(
				align(uiCamera, (menu.camera.width - menu.width()) / 2),
				restart.bottom() + 2
		);
		scene.add(menu);
	}
	
	public static void bossSlain() {
		if (Dungeon.hero.isAlive()) {
			Banner bossSlain = new Banner( BannerSprites.get( BannerSprites.Type.BOSS_SLAIN ) );
			bossSlain.show( 0xFFFFFF, 0.3f, 5f );
			scene.showBanner( bossSlain );
			
			Sample.INSTANCE.play( Assets.Sounds.BOSS );
		}
	}
	
	public static void handleCell( int cell ) {
		cellSelector.select( cell, PointerEvent.LEFT );
	}
	
	public static void selectCell( CellSelector.Listener listener ) {
		if (cellSelector.listener != null && cellSelector.listener != defaultCellListener){
			cellSelector.listener.onSelect(null);
		}
		// Net players drive the idle cursor through playerCellListener (sends taps to the
		// host) and gate on their OWN turn — here Dungeon.hero is the host's hero mirror,
		// so using its .ready would wrongly disable the client after every window closes.
		if (listener == defaultCellListener && xyz.gabriwar.warpedpixeldungeon.net.NetManager.isPlayer()) {
			listener = playerCellListener;
			cellSelector.listener = listener;
			cellSelector.enabled = xyz.gabriwar.warpedpixeldungeon.net.NetManager.isMyTurn();
		} else {
			cellSelector.listener = listener;
			cellSelector.enabled = Dungeon.hero.ready;
		}
		if (scene != null) {
			scene.prompt(listener.prompt());
		}
	}
	
	public static boolean cancelCellSelector() {
		if (cellSelector.listener != null && cellSelector.listener != defaultCellListener) {
			cellSelector.resetKeyHold();
			cellSelector.cancel();
			return true;
		} else {
			return false;
		}
	}
	
	public static WndBag selectItem( WndBag.ItemSelector listener ) {
		cancel();

		if (scene != null) {
			//TODO can the inventory pane work in these cases? bad to fallback to mobile window
			if (scene.inventory != null && scene.inventory.visible && !showingWindow()){
				scene.inventory.setSelector(listener);
				return null;
			} else {
				WndBag wnd = WndBag.getBag( listener );
				show(wnd);
				return wnd;
			}
		}

		return null;
	}

	//logic for preserving inventory selection windows on scene reset (e.g. via auto-rotate)
	private static WndBag.ItemSelector savedSelector;

	@Override
	public synchronized void saveWindows() {
		if (members == null) return;

		super.saveWindows();
		if (scene != null && scene.inventory != null && scene.inventory.getSelector() != null){
			savedSelector = scene.inventory.getSelector();
		} else {
			for (Gizmo g : members.toArray(new Gizmo[0])){
				if (g instanceof WndBag){
					savedSelector = ((WndBag) g).getSelector();
				//also keeps selector active over inventory scroll cancel and upgrade window
				} else if (g instanceof InventoryScroll.WndConfirmCancel){
					savedSelector = ((InventoryScroll.WndConfirmCancel) g).getItemSelector();
				} else if (g instanceof WndUpgrade){
					savedSelector = ((WndUpgrade) g).getItemSelector();
				}
			}
		}
	}

	@Override
	public synchronized void restoreWindows() {
		super.restoreWindows();
		if (savedSelector != null){
			if (scene != null && scene.inventory != null){
				scene.inventory.setSelector(savedSelector);
			} else {
				addToFront(new WndBag(Dungeon.hero.belongings.backpack, savedSelector));
			}
			savedSelector = null;
		}
	}

	public static boolean cancel() {
		cellSelector.resetKeyHold();
		if (Dungeon.hero != null && (Dungeon.hero.curAction != null || Dungeon.hero.resting)) {
			
			Dungeon.hero.curAction = null;
			Dungeon.hero.resting = false;
			return true;
			
		} else {
			
			return cancelCellSelector();
			
		}
	}
	
	public static void ready() {
		selectCell( defaultCellListener );
		QuickSlotButton.cancel();
		InventoryPane.cancelTargeting();
		if (scene != null && scene.toolbar != null) scene.toolbar.examining = false;
		if (tagDisappeared) {
			tagDisappeared = false;
			updateTags = true;
		}
	}
	
	public static void checkKeyHold(){
		cellSelector.processKeyHold();
	}
	
	public static void resetKeyHold(){
		cellSelector.resetKeyHold();
	}

	public static void examineCell( Integer cell ) {
		if (cell == null
				|| cell < 0
				|| cell > Dungeon.level.length()
				|| (!Dungeon.level.visited[cell] && !Dungeon.level.mapped[cell])) {
			return;
		}

		ArrayList<Object> objects = getObjectsAtCell(cell);

		if (objects.isEmpty()) {
			GameScene.show(new WndInfoCell(cell));
		} else if (objects.size() == 1){
			examineObject(objects.get(0));
		} else {
			String[] names = getObjectNames(objects).toArray(new String[0]);

			GameScene.show(new WndOptions(Icons.get(Icons.INFO),
					Messages.get(GameScene.class, "choose_examine"),
					Messages.get(GameScene.class, "multiple_examine"),
					names){
				@Override
				protected void onSelect(int index) {
					examineObject(objects.get(index));
				}
			});

		}
	}

	private static ArrayList<Object> getObjectsAtCell( int cell ){
		ArrayList<Object> objects = new ArrayList<>();

		if (cell == Dungeon.hero.pos) {
			objects.add(Dungeon.hero);

		} else if (Dungeon.level.heroFOV[cell]) {
			// Net MP: cell may hold another player's Hero (host side) or a NetHeroMob (client side).
			// findChar returns Char; unchecked Mob cast crashes on Hero — guard the cast.
			xyz.gabriwar.warpedpixeldungeon.actors.Char ch = Actor.findChar(cell);
			if (ch instanceof Mob)        objects.add((Mob) ch);
			else if (ch instanceof Hero)  objects.add(ch);
		}

		Heap heap = Dungeon.level.heaps.get(cell);
		if (heap != null && heap.seen) objects.add(heap);

		Plant plant = Dungeon.level.plants.get( cell );
		if (plant != null) objects.add(plant);

		Trap trap = Dungeon.level.traps.get( cell );
		if (trap != null && trap.visible) objects.add(trap);

		return objects;
	}

	private static ArrayList<String> getObjectNames( ArrayList<Object> objects ){
		ArrayList<String> names = new ArrayList<>();
		for (Object obj : objects){
			if (obj instanceof Hero)        names.add(((Hero) obj).className().toUpperCase(Locale.ENGLISH));
			else if (obj instanceof Mob)    names.add(Messages.titleCase( ((Mob)obj).name() ));
			else if (obj instanceof Heap)   names.add(Messages.titleCase( ((Heap)obj).title() ));
			else if (obj instanceof Plant)  names.add(Messages.titleCase( ((Plant) obj).name() ));
			else if (obj instanceof Trap)   names.add(Messages.titleCase( ((Trap) obj).name() ));
		}
		return names;
	}

	public static void examineObject(Object o){
		if (o == Dungeon.hero){
			GameScene.show( new WndHero() );
		} else if ( o instanceof Hero ){
			// Net MP host side: another player's actual Hero.
			// No WndHero (assumes Dungeon.hero) — show a basic info popup instead.
			Hero h = (Hero) o;
			String title = Messages.titleCase(h.className());
			String body = "HP " + h.HP + "/" + h.HT
					+ "\nLvl " + h.lvl
					+ "\nSTR " + h.STR;
			GameScene.show(new xyz.gabriwar.warpedpixeldungeon.windows.WndTitledMessage(
					Icons.get(Icons.INFO), title, body));
		} else if ( o instanceof xyz.gabriwar.warpedpixeldungeon.net.SpectatorReceiver.NetHeroMob ){
			// Net MP client side: a remote hero proxy (other player or the host).
			xyz.gabriwar.warpedpixeldungeon.net.SpectatorReceiver.NetHeroMob nhm =
					(xyz.gabriwar.warpedpixeldungeon.net.SpectatorReceiver.NetHeroMob) o;
			String title = nhm.netName.isEmpty()
					? Messages.titleCase(nhm.heroClass.title())
					: nhm.netName;
			String body = "Class: " + Messages.titleCase(nhm.heroClass.title())
					+ "\nHP " + nhm.HP + "/" + nhm.HT;
			GameScene.show(new xyz.gabriwar.warpedpixeldungeon.windows.WndTitledMessage(
					Icons.get(Icons.INFO), title, body));
		} else if ( o instanceof Mob && ((Mob) o).isActive() ){
			GameScene.show(new WndInfoMob((Mob) o));
			if (o instanceof Snake && !Document.ADVENTURERS_GUIDE.isPageRead(Document.GUIDE_SURPRISE_ATKS)){
				GameScene.flashForDocument(Document.ADVENTURERS_GUIDE, Document.GUIDE_SURPRISE_ATKS);
			}
		} else if ( o instanceof Heap && !((Heap) o).isEmpty() ){
			GameScene.show(new WndInfoItem((Heap)o));
		} else if ( o instanceof Plant ){
			GameScene.show( new WndInfoPlant((Plant) o) );
			//plants can be harmful to trample, so let the player ID just by examine
			Bestiary.setSeen(o.getClass());
		} else if ( o instanceof Trap ){
			GameScene.show( new WndInfoTrap((Trap) o));
			//traps are often harmful to trigger, so let the player ID just by examine
			Bestiary.setSeen(o.getClass());
		} else {
			GameScene.show( new WndMessage( Messages.get(GameScene.class, "dont_know") ) ) ;
		}
	}

	
	private static final CellSelector.Listener defaultCellListener = new CellSelector.Listener() {
		@Override
		public void onSelect( Integer cell ) {
			if (Dungeon.hero.handle( cell )) {
				Dungeon.hero.next();
			}
		}

		@Override
		public void onRightClick(Integer cell) {
			if (cell == null
					|| cell < 0
					|| cell > Dungeon.level.length()
					|| (!Dungeon.level.visited[cell] && !Dungeon.level.mapped[cell])) {
				return;
			}

			ArrayList<Object> objects = getObjectsAtCell(cell);
			ArrayList<String> textLines = getObjectNames(objects);

			//determine title and image
			String title = null;
			Image image = null;
			if (objects.isEmpty()) {
				title = WndInfoCell.cellName(cell);
				image = WndInfoCell.cellImage(cell);
			} else if (objects.size() > 1){
				title = Messages.get(GameScene.class, "multiple");
				image = Icons.get(Icons.INFO);
			} else if (objects.get(0) instanceof Hero) {
				title = textLines.remove(0);
				image = HeroSprite.avatar((Hero) objects.get(0));
			} else if (objects.get(0) instanceof Mob) {
				title = textLines.remove(0);
				image = ((Mob) objects.get(0)).sprite();
			} else if (objects.get(0) instanceof Heap) {
				title = textLines.remove(0);
				image = new ItemSprite((Heap) objects.get(0));
			} else if (objects.get(0) instanceof Plant) {
				title = textLines.remove(0);
				image = TerrainFeaturesTilemap.tile(cell, Dungeon.level.map[cell]);
			} else if (objects.get(0) instanceof Trap) {
				title = textLines.remove(0);
				image = TerrainFeaturesTilemap.tile(cell, Dungeon.level.map[cell]);
			}

			//determine first text line
			if (objects.isEmpty()) {
				textLines.add(0, Messages.get(GameScene.class, "go_here"));
			} else if (objects.get(0) instanceof Hero) {
				textLines.add(0, Messages.get(GameScene.class, "go_here"));
			} else if (objects.get(0) instanceof Mob) {
				if (((Mob) objects.get(0)).alignment != Char.Alignment.ENEMY) {
					textLines.add(0, Messages.get(GameScene.class, "interact"));
				} else {
					textLines.add(0, Messages.get(GameScene.class, "attack"));
				}
			} else if (objects.get(0) instanceof Heap) {
				switch (((Heap) objects.get(0)).type) {
					case HEAP:
						textLines.add(0, Messages.get(GameScene.class, "pick_up"));
						break;
					case FOR_SALE:
						textLines.add(0, Messages.get(GameScene.class, "purchase"));
						break;
					default:
						textLines.add(0, Messages.get(GameScene.class, "interact"));
						break;
				}
			} else if (objects.get(0) instanceof Plant) {
				textLines.add(0, Messages.get(GameScene.class, "trample"));
			} else if (objects.get(0) instanceof Trap) {
				textLines.add(0, Messages.get(GameScene.class, "interact"));
			}

			//final text formatting
			if (objects.size() > 1){
				textLines.add(0, "_" + textLines.remove(0) + ":_ " + textLines.get(0));
				for (int i = 1; i < textLines.size(); i++){
					textLines.add(i, "_" + Messages.get(GameScene.class, "examine") + ":_ " + textLines.remove(i));
				}
			} else {
				textLines.add(0, "_" + textLines.remove(0) + "_");
				textLines.add(1, "_" + Messages.get(GameScene.class, "examine") + "_");
			}

			RightClickMenu menu = new RightClickMenu(image,
					title,
					textLines.toArray(new String[0])){
				@Override
				public void onSelect(int index) {
					if (index == 0){
						handleCell(cell);
					} else {
						if (objects.size() == 0){
							GameScene.show(new WndInfoCell(cell));
						} else {
							examineObject(objects.get(index-1));
						}
					}
				}
			};
			scene.addToFront(menu);
			menu.camera = PixelScene.uiCamera;
			PointF mousePos = PointerEvent.currentHoverPos();
			mousePos = menu.camera.screenToCamera((int)mousePos.x, (int)mousePos.y);
			menu.setPos(mousePos.x-3, mousePos.y-3);

		}

		@Override
		public String prompt() {
			return null;
		}
	};
}
