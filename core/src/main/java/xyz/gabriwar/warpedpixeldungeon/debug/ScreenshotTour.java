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
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.plants.Cornwheat;
import xyz.gabriwar.warpedpixeldungeon.plants.Firebloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Icecap;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.plants.Rose;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import xyz.gabriwar.warpedpixeldungeon.plants.Tomatobush;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.net.NetHeroSprite;
import xyz.gabriwar.warpedpixeldungeon.net.SpectatorReceiver;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.HeroSelectScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.SkyScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.TitleScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.ui.ActionIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.GameLog;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import xyz.gabriwar.warpedpixeldungeon.windows.WndHero;
import xyz.gabriwar.warpedpixeldungeon.windows.WndWorldMap;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.NoosaScript;
import com.watabou.noosa.NoosaScriptNoLighting;
import com.watabou.utils.PathFinder;
import com.watabou.utils.DeviceCompat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Automated screenshot run: walks the title, the rogue's hero splash, a fresh game on
 * the overworld (the town by day and by night, fully revealed and zoomed in; then
 * as found, with the world map, with the inventory, with the skill tree), the sky by
 * night and by day, and a furnished safe spot, saving a PNG of each at a fixed
 * resolution, then quits. The scene is rendered into an
 * offscreen framebuffer of that resolution, so the window manager's idea of the
 * window size does not matter.
 *
 * Enabled by the desktop launcher via -Dwpd.screenshots=DIR -Dwpd.screenshots.size=WxH,
 * see tools/screenshots.sh.
 */
public class ScreenshotTour {

	public static ScreenshotTour instance;

	private static final float TIMEOUT = 60f;

	public final String outDir;
	public final int width, height;

	private final ArrayList<Step> steps = new ArrayList<>();
	private int index = 0;
	private float waited = 0;
	private FrameBuffer fbo;

	private interface Step { boolean run(); }

	//mid-day and deep night in every season (winter days are the shortest, 600 turns)
	private static final int DAY_TURN = 250 + 300;
	private static final int NIGHT_TURN = DayNightCycle.FULL_CYCLE - 300;

	public ScreenshotTour( String outDir, int width, int height ){
		this.outDir = outDir;
		this.width = width;
		this.height = height;
		DeviceCompat.offscreenRender = true;

		act( () -> {
			WPDSettings.version( Game.versionCode );
			WPDSettings.updates( false );
			WPDSettings.news( false );
			//the first-run tutorial hides the status pane and toolbar until the hero moves
			WPDSettings.intro( false );
			//the real pointer would otherwise hover a button and pop its tooltip
			Gdx.input.setCursorPosition( 0, Gdx.graphics.getHeight() - 1 );
			Game.switchScene( TitleScene.class );
		} );
		until( () -> Game.scene() instanceof TitleScene );
		delay( 2f );
		shot( "01_title" );

		//one hero splash stands for the six
		act( () -> {
			GamesInProgress.selectedClass = HeroClass.ROGUE;
			GamesInProgress.curSlot = 1;
			WarpedPixelDungeon.switchNoFade( HeroSelectScene.class );
		} );
		until( () -> Game.scene() instanceof HeroSelectScene );
		delay( 1.5f );
		shot( "02_hero" );

		//same as HeroSelectScene's start button
		act( () -> {
			GamesInProgress.selectedClass = HeroClass.WARRIOR;
			Dungeon.hero = null;
			Dungeon.daily = Dungeon.dailyReplay = false;
			Dungeon.initSeed();
			ActionIndicator.clearAction();
			InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
			Game.switchScene( InterlevelScene.class );
		} );
		until( () -> Game.scene() instanceof GameScene );
		delay( 3f );

		//the whole town, fully revealed, under a clear sky
		act( () -> {
			Dungeon.debugNoFog = true;
			ClimateManager.debugCloudOverride = 0f;
			ClimateManager.debugPrecipOverride = 0f;
		} );
		townShot( DAY_TURN, "03_town_day" );

		//two friends' heroes beside ours on the square, the way a shared run looks
		act( () -> {
			party.clear();
			party.add( netHero( "Ana", HeroClass.MAGE ) );
			party.add( netHero( "Leo", HeroClass.HUNTRESS ) );
			Camera.main.zoom( PixelScene.defaultZoom );
			Camera.main.panTo( Dungeon.hero.sprite.center(), 0 );
			Camera.main.snapTo( Dungeon.hero.sprite.center() );
		} );
		delay( 1f );
		act( ScreenshotTour::clearLog );
		shot( "03_multiplayer" );
		act( () -> {
			for (SpectatorReceiver.NetHeroMob m : party) {
				m.sprite.killAndErase();
				Dungeon.level.mobs.remove( m );
			}
			party.clear();
		} );

		townShot( NIGHT_TURN, "03_town_night" );

		//the overworld as the run found it
		act( () -> {
			Dungeon.debugNoFog = false;
			ClimateManager.debugCloudOverride = Float.NaN;
			ClimateManager.debugPrecipOverride = Float.NaN;
			Game.switchScene( GameScene.class );
		} );
		until( () -> Game.scene() instanceof GameScene );
		delay( 2f );
		act( ScreenshotTour::closeWindows );
		delay( 0.5f );
		act( ScreenshotTour::clearLog );
		shot( "04_overworld" );

		act( () -> GameScene.show( belowBanner( new WndWorldMap( (OverworldLevel) Dungeon.level ) ) ) );
		delay( 1f );
		shot( "05_overworld_map" );

		act( () -> {
			closeWindows();
			GameScene.show( belowBanner( new WndBag( Dungeon.hero.belongings.backpack ) ) );
		} );
		delay( 1f );
		shot( "06_inventory" );

		//the skill tree of a hero a dozen levels in
		act( () -> {
			closeWindows();
			Dungeon.hero.lvl = 12;
			Skill.availableSkill = 3;
			int bought = 0;
			for (Skill sk : Dungeon.hero.heroSkills.passiveASkills) if (bought++ < 2) sk.setLevel( 1 );
			bought = 0;
			for (Skill sk : Dungeon.hero.heroSkills.activeSkills)   if (bought++ < 2) sk.setLevel( 1 );
			bought = 0;
			for (Skill sk : Dungeon.hero.heroSkills.passiveBSkills) if (bought++ < 1) sk.setLevel( 1 );
			WndHero.lastIdx = 1;
			GameScene.show( belowBanner( new WndHero() ) );
		} );
		delay( 1f );
		shot( "07_skill_tree" );

		//clear skies: stars at night, sun by day
		act( () -> {
			closeWindows();
			ClimateManager.debugCloudOverride = 0f;
			ClimateManager.debugPrecipOverride = 0f;
		} );
		skyShot( NIGHT_TURN, "08_sky_night" );
		skyShot( DAY_TURN + DayNightCycle.FULL_CYCLE, "08_sky_day" ); //time only moves forward

		//the safe spot, furnished the way a player would with the builder's tool
		act( () -> {
			Dungeon.debugNoFog = true;
			InterlevelScene.mode = InterlevelScene.Mode.RETURN;
			InterlevelScene.returnDepth = 50;
			InterlevelScene.returnBranch = 0;
			InterlevelScene.returnPos = -1;
			Game.switchScene( InterlevelScene.class );
		} );
		until( () -> Game.scene() instanceof GameScene );
		delay( 2f );
		act( () -> {
			closeWindows();
			furnishSafeSpot();
			float zoom = (float) Math.floor( Math.min( width / (float) (SAFE_PLAN[0].length() * DungeonTilemap.SIZE), height / (float) (SAFE_PLAN.length * DungeonTilemap.SIZE) ) );
			Camera.main.zoom( Math.max( PixelScene.minZoom, zoom ) );
			int centre = SAFE_X0 + SAFE_PLAN[0].length()/2 + Dungeon.level.width() * (SAFE_Y0 + SAFE_PLAN.length/2);
			Camera.main.panTo( DungeonTilemap.tileCenterToWorld( centre ), 0 );
			Camera.main.snapTo( DungeonTilemap.tileCenterToWorld( centre ) );
		} );
		delay( 1f );
		act( ScreenshotTour::clearLog );
		shot( "09_safe_spot" );

		//the general store inside the town, keeper behind the counter
		act( () -> {
			InterlevelScene.mode = InterlevelScene.Mode.RETURN;
			InterlevelScene.returnDepth = 4;
			InterlevelScene.returnBranch = 6;
			InterlevelScene.returnPos = -1;
			Game.switchScene( InterlevelScene.class );
		} );
		until( () -> Game.scene() instanceof GameScene );
		delay( 2f );
		act( () -> {
			closeWindows();
			Level level = Dungeon.level;
			float zoom = (float) Math.floor( Math.min( width / (float) (level.width() * DungeonTilemap.SIZE), height / (float) (level.height() * DungeonTilemap.SIZE) ) );
			Camera.main.zoom( Math.max( PixelScene.minZoom, zoom ) );
			int centre = level.width()/2 + level.width() * (level.height()/2);
			Camera.main.panTo( DungeonTilemap.tileCenterToWorld( centre ), 0 );
			Camera.main.snapTo( DungeonTilemap.tileCenterToWorld( centre ) );
		} );
		delay( 1f );
		act( ScreenshotTour::clearLog );
		shot( "10_shop" );

		//the northern lights: the overworld only allows them over its far north,
		//anywhere else the climate's own word stands, so they are called from indoors
		act( () -> {
			Dungeon.cycleTurn = NIGHT_TURN + 2 * DayNightCycle.FULL_CYCLE;
			ClimateManager.onHeroTurn();
			if (!ClimateManager.isAurora()) ClimateManager.debugToggleAurora();
			Game.switchScene( SkyScene.class );
		} );
		until( () -> Game.scene() instanceof SkyScene );
		delay( 2f );
		shot( "11_sky_aurora" );

		act( () -> Game.instance.finish() );
	}

	//a cottage, a pond, a farm plot and a garden around the safe spot's entrance
	//(cell 23,15 of SafeLevel), the sort of base the builder's tool is for
	static final int SAFE_X0 = 4, SAFE_Y0 = 8;
	static final String[] SAFE_PLAN = {
		"##############################",
		"#............................#",
		"#..######wB####..~~~...\"\"\"'..#",
		"#..#rrrr..BB..#..~~~~..'\"\"\"..#",
		"#..#rrrr.....A#...~~...\"'\"...#",
		"#..+.....S....#..........===.#",
		"#..#...P......+..........===.#",
		"#..#..........#....x.....===.#",
		"#..#....E.....#..........===.#",
		"#..######/#####..........===.#",
		"#..........................s.#",
		"#..sss......W........s....\"..#",
		"#............................#",
		"##############################",
	};

	//tools/screenshot_banners.py paints a caption band over the top fifth of the
	//shot: windows slide down so their titles stay readable under it
	private Window belowBanner( Window w ){
		int band = (int) (Math.min( width, height ) * 0.22f / PixelScene.defaultZoom);
		w.offset( 0, band );
		w.boundOffsetWithMargin( 2 );
		return w;
	}

	private final ArrayList<SpectatorReceiver.NetHeroMob> party = new ArrayList<>();

	//a remote player's hero standing on a free cell next to ours
	static SpectatorReceiver.NetHeroMob netHero( String name, HeroClass cls ){
		SpectatorReceiver.NetHeroMob m = new SpectatorReceiver.NetHeroMob();
		m.heroClass = cls;
		m.ownerName = m.netName = name;
		m.HP = m.HT = 20;
		m.spriteClass = NetHeroSprite.class;
		m.pos = Dungeon.hero.pos;
		for (int d : PathFinder.NEIGHBOURS8) {
			int c = Dungeon.hero.pos + d;
			if (Dungeon.level.passable[c] && xyz.gabriwar.warpedpixeldungeon.actors.Actor.findChar( c ) == null) {
				m.pos = c;
				break;
			}
		}
		GameScene.add( m );
		return m;
	}

	static int terrainFor( char c ){
		switch (c) {
			case '#': return Terrain.WALL;
			case 'w': return Terrain.WALL_DECO;
			case ',': return Terrain.EMPTY_DECO;
			case '+': return Terrain.DOOR;
			case '/': return Terrain.OPEN_DOOR;
			case '~': return Terrain.WATER;
			case '"': return Terrain.GRASS;
			case '\'': return Terrain.HIGH_GRASS;
			case '=': return Terrain.FURROWED_GRASS;
			case 'r': return Terrain.WOOL_RUG;
			case 'B': return Terrain.BOOKSHELF;
			case 'S': return Terrain.STATUE;
			case 'A': return Terrain.ALCHEMY;
			case 'P': return Terrain.PEDESTAL;
			case 'W': return Terrain.WELL;
			case 's': return Terrain.SHRUB;
			case 'E': return Terrain.EMBERS;
			default:  return Terrain.EMPTY;
		}
	}

	static void furnishSafeSpot(){
		Level level = Dungeon.level;
		int w = level.width();
		ArrayList<Integer> furrows = new ArrayList<>();
		ArrayList<Integer> lawn = new ArrayList<>();
		int rug = -1, pedestal = -1;
		for (int y = 0; y < SAFE_PLAN.length; y++) {
			for (int x = 0; x < SAFE_PLAN[y].length(); x++) {
				char c = SAFE_PLAN[y].charAt( x );
				int cell = SAFE_X0 + x + w * (SAFE_Y0 + y);
				if (c == 'x') continue; //the entrance stays as it is
				Level.set( cell, terrainFor( c ) );
				if (c == '=') furrows.add( cell );
				if (c == '"') lawn.add( cell );
				if (c == 'r' && rug < 0) rug = cell;
				if (c == 'P') pedestal = cell;
			}
		}
		level.buildFlagMaps();
		level.cleanWalls();
		GameScene.updateMap();
		Dungeon.observe();

		Plant.Seed[] crops = { new Cornwheat.Seed(), new Tomatobush.Seed(), new Sungrass.Seed(), new Icecap.Seed() };
		for (int i = 0; i < furrows.size(); i++) {
			level.plant( crops[i % crops.length], furrows.get( i ) );
		}
		Plant.Seed[] flowers = { new Rose.Seed(), new Firebloom.Seed(), new Sungrass.Seed() };
		for (int i = 0; i < lawn.size(); i += 2) {
			level.plant( flowers[(i / 2) % flowers.length], lawn.get( i ) );
		}

		level.drop( Generator.random( Generator.Category.RING ), pedestal );
		level.drop( Generator.random( Generator.Category.WEAPON ), rug );
		level.drop( Generator.random( Generator.Category.ARMOR ), rug + 1 );
		level.drop( Generator.random( Generator.Category.POTION ), rug + w );
		level.drop( Generator.random( Generator.Category.SCROLL ), rug + w + 1 );
		level.drop( new Gold( 250 ), rug + 2 );
	}

	//GameScene rebuilt at the given time of day, camera on the town square
	private void townShot( int cycleTurn, String name ){
		act( () -> {
			Dungeon.cycleTurn = cycleTurn;
			ClimateManager.onHeroTurn(); //light is only recomputed here
			Game.switchScene( GameScene.class );
		} );
		until( () -> Game.scene() instanceof GameScene );
		delay( 2f );
		act( () -> {
			closeWindows();
			Dungeon.observe();
			OverworldLevel level = (OverworldLevel) Dungeon.level;
			int square = level.townLocal( WorldStructures.TOWN_SIZE/2 * WorldStructures.TOWN_SIZE + WorldStructures.TOWN_SIZE/2 );
			//zoomed out until the whole town fits on the shorter side of the screen
			float zoom = (float) Math.floor( Math.min( width, height ) / (float) (WorldStructures.TOWN_SIZE * DungeonTilemap.SIZE) );
			Camera.main.zoom( Math.max( PixelScene.minZoom, zoom ) );
			Camera.main.panTo( DungeonTilemap.tileCenterToWorld( square ), 0 );
			Camera.main.snapTo( DungeonTilemap.tileCenterToWorld( square ) );
		} );
		delay( 0.5f );
		act( ScreenshotTour::clearLog );
		shot( name );
	}

	private void skyShot( int cycleTurn, String name ){
		act( () -> {
			Dungeon.cycleTurn = cycleTurn;
			ClimateManager.onHeroTurn();
			Game.switchScene( SkyScene.class );
		} );
		until( () -> Game.scene() instanceof SkyScene );
		delay( 2f );
		shot( name );
	}

	//called once per frame from WarpedPixelDungeon.update(), after the scene updated
	public void update(){
		if (index >= steps.size()) return;

		waited += Gdx.graphics.getDeltaTime();
		if (waited > TIMEOUT) {
			System.err.println( "screenshot tour: timed out at step " + index );
			System.exit( 2 );
		}

		if (steps.get( index ).run()) {
			index++;
			waited = 0;
		}
	}

	private void act( Runnable r ){
		steps.add( () -> { r.run(); return true; } );
	}

	private void until( Step condition ){
		steps.add( () -> condition.run() && !Game.switchingScene() );
	}

	private void delay( float seconds ){
		steps.add( () -> waited >= seconds );
	}

	private void shot( String name ){
		steps.add( () -> { capture( name ); return true; } );
	}

	//the log only redraws when text arrives, so an empty line follows the wipe
	static void clearLog(){
		GameLog.wipe();
		GLog.newLine();
	}

	static void closeWindows(){
		for (Gizmo g : Game.scene().membersView()) {
			if (g instanceof Window) ((Window) g).hide();
		}
	}

	private void capture( String name ){
		if (fbo == null) {
			fbo = new FrameBuffer( Pixmap.Format.RGBA8888, width, height, false );
			//gdx bound its own texture while building the fbo, noosa's cache is stale
			Texture.clear();
		}

		fbo.begin();
		NoosaScript.get().resetCamera();
		NoosaScriptNoLighting.get().resetCamera();
		Gdx.gl.glDisable( Gdx.gl.GL_SCISSOR_TEST );
		Gdx.gl.glClear( Gdx.gl.GL_COLOR_BUFFER_BIT );
		Game.scene().draw();
		Gdx.gl.glDisable( Gdx.gl.GL_SCISSOR_TEST );
		Pixmap pixmap = Pixmap.createFromFrameBuffer( 0, 0, width, height );
		fbo.end();

		//blending against the cleared fbo leaves partial alpha, the png must be opaque
		ByteBuffer pixels = pixmap.getPixels();
		for (int i = 3; i < pixels.limit(); i += 4) {
			pixels.put( i, (byte) 0xFF );
		}

		PixmapIO.writePNG( Gdx.files.absolute( outDir ).child( name + ".png" ), pixmap, -1, true );
		pixmap.dispose();
		System.out.println( "screenshot tour: " + name );
	}
}
