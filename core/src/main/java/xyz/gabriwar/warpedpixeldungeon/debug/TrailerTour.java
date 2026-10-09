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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ChargedShot;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.MeteorCall;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.NinjaBomb;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Goo;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.net.SpectatorReceiver;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.SkyScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.TitleScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.ui.ActionIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.windows.WndHero;
import xyz.gabriwar.warpedpixeldungeon.windows.WndJournal;
import xyz.gabriwar.warpedpixeldungeon.windows.WndWorldMap;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.TimeUtils;
import com.watabou.glwrap.Texture;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.NoosaScript;
import com.watabou.noosa.NoosaScriptNoLighting;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * The store trailer: a scripted run through the game recorded frame by frame at a
 * fixed 30 fps into an mp4 (raw frames piped to ffmpeg). The scene is rendered
 * offscreen at the requested size exactly as ScreenshotTour does, and time is
 * stepped by a fixed 1/30 s per frame so the film is smooth whatever the encoder
 * costs. Captions are written beside the film as JSON (frame ranges) for
 * tools/trailer.py to burn in.
 *
 * Enabled by the desktop launcher via -Dwpd.trailer=FILE.mp4 -Dwpd.screenshots.size=WxH.
 */
public class TrailerTour {

	public static TrailerTour instance;

	public static final int FPS = 30;
	private static final float DT = 1f / FPS;
	private static final float TIMEOUT = 90f;

	public final String outFile;
	public final int width, height;

	private final ArrayList<Step> steps = new ArrayList<>();
	private int index = 0;
	private float waited = 0;
	private int frame = 0;
	private FrameBuffer fbo;
	private Process ffmpeg;
	private OutputStream pipe;
	private byte[] row;

	private final StringBuilder captions = new StringBuilder( "[\n" );
	private int captionStart = -1;
	private String captionTitle, captionDesc;

	private interface Step { boolean run(); }

	private static final int DAY_TURN = 250 + 300;
	private static final int NIGHT_TURN = DayNightCycle.FULL_CYCLE - 300;

	public TrailerTour( String outFile, int width, int height ){
		this.outFile = outFile;
		this.width = width;
		this.height = height;
		DeviceCompat.offscreenRender = true;

		act( () -> {
			WPDSettings.version( Game.versionCode );
			WPDSettings.updates( false );
			WPDSettings.news( false );
			WPDSettings.intro( false );
			Gdx.input.setCursorPosition( 0, Gdx.graphics.getHeight() - 1 );
			Game.switchScene( TitleScene.class );
		} );
		until( () -> Game.scene() instanceof TitleScene );

		// ---- 1. the town at dusk: the folk walk home as night falls ----
		newGame( HeroClass.ROGUE );
		act( () -> {
			//a little before nightfall, so the walk home happens on camera
			for (int t = 0; t < DayNightCycle.FULL_CYCLE; t++) {
				Dungeon.cycleTurn = t;
				if (DayNightCycle.phase() == DayNightCycle.Phase.DUSK && DayNightCycle.turnsUntilPhaseChange() <= 40) break;
			}
			ClimateManager.onHeroTurn();
			ClimateManager.debugCloudOverride = 0.1f;
			ClimateManager.debugPrecipOverride = 0f;
			Dungeon.debugNoFog = true;
			Game.switchScene( GameScene.class );
		} );
		until( () -> Game.scene() instanceof GameScene );
		delay( 1f );
		act( () -> {
			ScreenshotTour.closeWindows();
			ScreenshotTour.clearLog();
			Dungeon.observe();
			lookAtTown( 1.5f );
			caption( "A living world", "Townsfolk work by day and walk home at dusk" );
		} );
		//turns pass: every few frames the hero waits a turn, the town moves
		turns( 9f, 4 );

		// ---- 2. a village on the endless overworld, then the map ----
		act( () -> {
			caption( null, null );
			OverworldLevel ow = (OverworldLevel) Dungeon.level;
			int[] v = nearestVillage( ow.worldSeed() );
			OverworldLevel.arriveAt( v[0], v[1] );
			Dungeon.cycleTurn += 200; //well into the day
			ClimateManager.onHeroTurn();
			OverworldLevel.travelToSurface();
		} );
		until( () -> Game.scene() instanceof GameScene );
		delay( 1f );
		act( () -> {
			ScreenshotTour.closeWindows();
			ScreenshotTour.clearLog();
			Dungeon.observe();
			Camera.main.zoom( PixelScene.defaultZoom );
			caption( "An endless generated open world", "Villages, ruins, camps and standing stones to discover" );
		} );
		zoomTo( PixelScene.defaultZoom, Math.max( 1f, PixelScene.defaultZoom / 3f ), 4f );
		delay( 1f );
		act( () -> GameScene.show( new WndWorldMap( (OverworldLevel) Dungeon.level ) ) );
		delay( 3.5f );
		act( ScreenshotTour::closeWindows );

		// ---- 3. down the dungeon: five regions, whole floors ----
		act( () -> caption( "A BUNCH OF NEW CONTENT", "Five regions, dozens of floors, new rooms, mobs and bosses" ) );
		floor( 2 );
		floor( 7 );
		floor( 12 );
		floor( 17 );
		floor( 22 );

		// ---- 4. the safe spot, built tile by tile ----
		goTo( 50, 0 );
		delay( 0.5f );
		act( () -> {
			ScreenshotTour.closeWindows();
			ScreenshotTour.clearLog();
			Dungeon.observe();
			float zoom = (float) Math.floor( Math.min( width / (float) (ScreenshotTour.SAFE_PLAN[0].length() * DungeonTilemap.SIZE), height / (float) (ScreenshotTour.SAFE_PLAN.length * DungeonTilemap.SIZE) ) );
			Camera.main.zoom( Math.max( PixelScene.minZoom, zoom ) );
			int centre = ScreenshotTour.SAFE_X0 + ScreenshotTour.SAFE_PLAN[0].length()/2 + Dungeon.level.width() * (ScreenshotTour.SAFE_Y0 + ScreenshotTour.SAFE_PLAN.length/2);
			Camera.main.panTo( DungeonTilemap.tileCenterToWorld( centre ), 0 );
			Camera.main.snapTo( DungeonTilemap.tileCenterToWorld( centre ) );
			caption( "Build your own home", "A safe haven of your own: build it tile by tile, farm it, fill it with loot" );
			buildQueue.clear();
			for (int y = 0; y < ScreenshotTour.SAFE_PLAN.length; y++) {
				for (int x = 0; x < ScreenshotTour.SAFE_PLAN[y].length(); x++) {
					char c = ScreenshotTour.SAFE_PLAN[y].charAt( x );
					if (c != '.' && c != 'x') buildQueue.add( ScreenshotTour.SAFE_X0 + x + Dungeon.level.width() * (ScreenshotTour.SAFE_Y0 + y) );
				}
			}
		} );
		steps.add( () -> {
			//two tiles a frame: the house rises in a few seconds, the hero hammering away
			for (int i = 0; i < 2 && !buildQueue.isEmpty(); i++) {
				int cell = buildQueue.remove( 0 );
				char c = ScreenshotTour.SAFE_PLAN[cell / Dungeon.level.width() - ScreenshotTour.SAFE_Y0].charAt( cell % Dungeon.level.width() - ScreenshotTour.SAFE_X0 );
				Level.set( cell, ScreenshotTour.terrainFor( c ) );
				GameScene.updateMap( cell );
				if (frame % 12 == 0) Dungeon.hero.sprite.operate( cell );
			}
			return buildQueue.isEmpty();
		} );
		act( () -> {
			ScreenshotTour.furnishSafeSpot();
			Dungeon.hero.sprite.idle();
		} );
		delay( 2.5f );

		// ---- 5. the journal: the catalogue of things ----
		act( () -> {
			caption( "Hundreds of items", "New potions, scrolls, seeds, weapons, wands and artifacts" );
			for (Catalog c : Catalog.values()) for (Class<?> cls : c.items()) Catalog.setSeen( cls );
			WndJournal.last_index = 3;
			WndJournal.CatalogTab.currentItemIdx = 1;
			journal = new WndJournal();
			GameScene.show( journal );
		} );
		delay( 0.5f );
		scrollJournal( 6f );
		act( () -> {
			ScreenshotTour.closeWindows();
			caption( null, null );
		} );

		// ---- 6. skills: the tree, then three signature casts ----
		newGame( HeroClass.MAGE );
		goTo( 2, 0 );
		act( () -> {
			ScreenshotTour.closeWindows();
			ScreenshotTour.clearLog();
			arena( 5, 6 );
			Dungeon.hero.lvl = 15;
			Dungeon.hero.heroSkills.availableSkill = 6;
			int n = 0;
			for (Skill sk : Dungeon.hero.heroSkills.activeSkills) if (n++ < 3) sk.setLevel( 2 );
			WndHero.lastIdx = 1;
			GameScene.show( new WndHero() );
			caption( "Skill trees for every hero", "No level cap: keep growing, keep unlocking" );
		} );
		delay( 3.5f );
		act( ScreenshotTour::closeWindows );
		cast( MeteorCall.class, "Meteor Call" );
		turns( 5f, 15 ); //the stone falls on the next turn: the mage waits for it

		newGame( HeroClass.HUNTRESS );
		goTo( 2, 0 );
		act( () -> { ScreenshotTour.closeWindows(); ScreenshotTour.clearLog(); arena( 4, 6 ); } );
		cast( ChargedShot.class, "Charged Shot" );
		turns( 7f, 15 ); //she draws for several turns, then the lance goes

		newGame( HeroClass.ROGUE );
		goTo( 2, 0 );
		act( () -> { ScreenshotTour.closeWindows(); ScreenshotTour.clearLog(); arena( 3, 5 ); } );
		cast( NinjaBomb.class, "Ninja Bomb" );
		turns( 4f, 15 );

		// ---- 7. look at the sky ----
		act( () -> {
			caption( "Look up", "Living skies: real stars, moon phases, auroras and eclipses" );
			Dungeon.cycleTurn = NIGHT_TURN + 3 * DayNightCycle.FULL_CYCLE;
			ClimateManager.onHeroTurn();
			ClimateManager.debugCloudOverride = 0.15f;
			if (!ClimateManager.isAurora()) ClimateManager.debugToggleAurora();
			Game.switchScene( SkyScene.class );
		} );
		until( () -> Game.scene() instanceof SkyScene );
		delay( 5f );

		// ---- 8. three heroes against the Goo ----
		newGame( HeroClass.WARRIOR );
		goTo( 5, 0 );
		delay( 0.5f );
		act( () -> {
			ScreenshotTour.closeWindows();
			ScreenshotTour.clearLog();
			Goo goo = null;
			for (Mob m : Dungeon.level.mobs) if (m instanceof Goo) goo = (Goo) m;
			if (goo == null) { goo = new Goo(); goo.pos = Dungeon.hero.pos + 2; GameScene.add( goo ); }
			boss = goo;
			//everyone around the boss, awake and cornered
			int hp = freeCellNear( goo.pos );
			Dungeon.hero.pos = hp;
			Dungeon.hero.sprite.place( hp );
			Dungeon.hero.HT = Dungeon.hero.HP = 400;
			party.clear();
			party.add( ScreenshotTour.netHero( "Ana", HeroClass.MAGE ) );
			party.add( ScreenshotTour.netHero( "Leo", HeroClass.HUNTRESS ) );
			goo.beckon( hp );
			Dungeon.observe();
			Camera.main.zoom( PixelScene.defaultZoom );
			Camera.main.panTo( DungeonTilemap.tileCenterToWorld( goo.pos ), 0 );
			Camera.main.snapTo( DungeonTilemap.tileCenterToWorld( goo.pos ) );
			caption( "Together against the dark", "Free, open source, on Android and PC" );
		} );
		fight( 9f );
		act( () -> caption( null, null ) );
		delay( 0.5f );

		act( () -> Game.instance.finish() );
	}

	private final ArrayList<Integer> buildQueue = new ArrayList<>();
	private final ArrayList<SpectatorReceiver.NetHeroMob> party = new ArrayList<>();
	private WndJournal journal;
	private Goo boss;

	// ------------------------------------------------------------ scene helpers

	private void newGame( HeroClass cls ){
		act( () -> {
			caption( null, null );
			ScreenshotTour.closeWindows();
			GamesInProgress.selectedClass = cls;
			GamesInProgress.curSlot = 1;
			Dungeon.hero = null;
			Dungeon.daily = Dungeon.dailyReplay = false;
			Dungeon.initSeed();
			ActionIndicator.clearAction();
			InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
			Game.switchScene( InterlevelScene.class );
		} );
		until( () -> Game.scene() instanceof GameScene );
		act( () -> {
			Dungeon.debugNoFog = true;
			ClimateManager.debugCloudOverride = 0.1f;
			ClimateManager.debugPrecipOverride = 0f;
		} );
		delay( 0.5f );
	}

	private void goTo( int depth, int branch ){
		act( () -> {
			ScreenshotTour.closeWindows();
			InterlevelScene.mode = InterlevelScene.Mode.RETURN;
			InterlevelScene.returnDepth = depth;
			InterlevelScene.returnBranch = branch;
			InterlevelScene.returnPos = -1;
			Game.switchScene( InterlevelScene.class );
		} );
		until( () -> Game.scene() instanceof GameScene );
		delay( 0.3f );
	}

	//a whole floor on screen, then a slow push in
	private void floor( int depth ){
		goTo( depth, 0 );
		act( () -> {
			ScreenshotTour.closeWindows();
			ScreenshotTour.clearLog();
			Dungeon.observe();
			Level level = Dungeon.level;
			int centre = level.width()/2 + level.width() * (level.height()/2);
			Camera.main.panTo( DungeonTilemap.tileCenterToWorld( centre ), 0 );
			Camera.main.snapTo( DungeonTilemap.tileCenterToWorld( centre ) );
		} );
		steps.add( new Step() {
			float from, to;
			boolean started = false;
			@Override public boolean run(){
				if (!started) {
					started = true;
					Level level = Dungeon.level;
					from = Math.max( 0.5f, Math.min( width / (float) (level.width() * DungeonTilemap.SIZE), height / (float) (level.height() * DungeonTilemap.SIZE) ) );
					to = from * 1.25f;
				}
				float p = Math.min( 1f, waited / 3f );
				Camera.main.zoom( from + (to - from) * p );
				return p >= 1f;
			}
		} );
	}

	private void zoomTo( float from, float to, float seconds ){
		steps.add( () -> {
			float p = Math.min( 1f, waited / seconds );
			Camera.main.zoom( from + (to - from) * p );
			Camera.main.panTo( Dungeon.hero.sprite.center(), 0 );
			Camera.main.snapTo( Dungeon.hero.sprite.center() );
			return p >= 1f;
		} );
	}

	private void lookAtTown( float zoom ){
		OverworldLevel level = (OverworldLevel) Dungeon.level;
		int square = level.townLocal( WorldStructures.TOWN_SIZE/2 * WorldStructures.TOWN_SIZE + WorldStructures.TOWN_SIZE/2 );
		Camera.main.zoom( Math.max( PixelScene.minZoom, (float) Math.floor( Math.min( width, height ) / (float) (WorldStructures.TOWN_SIZE * DungeonTilemap.SIZE) ) * zoom ) );
		Camera.main.panTo( DungeonTilemap.tileCenterToWorld( square ), 0 );
		Camera.main.snapTo( DungeonTilemap.tileCenterToWorld( square ) );
	}

	//the hero waits a turn every few frames, so the world's own clock runs on camera
	private void turns( float seconds, int everyFrames ){
		steps.add( () -> {
			if (frame % everyFrames == 0 && Dungeon.hero.ready) Dungeon.hero.rest( false );
			return waited >= seconds;
		} );
	}

	private void scrollJournal( float seconds ){
		steps.add( () -> {
			//everything just marked seen unlocks badges, whose windows would sit on the page
			for (com.watabou.noosa.Gizmo g : Game.scene().membersView()) {
				if (g instanceof Window && g != journal) ((Window) g).hide();
			}
			ScreenshotTour.clearLog();
			try {
				Object tab = field( journal, "catalogTab" );
				ScrollPane grid = (ScrollPane) field( tab, "grid" );
				float max = grid.content().height() - grid.height();
				grid.scrollTo( 0, max * Math.min( 1f, waited / seconds ) );
			} catch (Exception e) {
				Game.reportException( e );
				return true;
			}
			return waited >= seconds;
		} );
	}

	//a few sleeping rats in front of the hero, in view, for the spell to land on
	private int target = -1;
	private void arena( int minDist, int maxDist ){
		Hero hero = Dungeon.hero;
		hero.MT = hero.MP = 200;
		hero.HT = hero.HP = 400;
		target = -1;
		int placed = 0;
		for (int d = minDist; d <= maxDist && placed < 3; d++) {
			for (int dir : PathFinder.NEIGHBOURS8) {
				int c = hero.pos + dir * d;
				if (c < 0 || c >= Dungeon.level.length() || !Dungeon.level.passable[c] || Actor.findChar( c ) != null) continue;
				if (!Dungeon.level.heroFOV[c]) continue;
				Rat r = new Rat();
				r.pos = c;
				r.state = r.SLEEPING; //asleep: they stay put for the spell, and a sleeper is a sure hit
				GameScene.add( r );
				if (target < 0) target = c;
				if (++placed >= 3) break;
			}
		}
		if (target < 0) target = freeCellNear( hero.pos );
		Camera.main.zoom( PixelScene.defaultZoom );
		Camera.main.panTo( hero.sprite.center(), 0 );
		Camera.main.snapTo( hero.sprite.center() );
	}

	private void cast( Class<? extends Skill> cls, String name ){
		act( () -> {
			Skill sk = Dungeon.hero.heroSkills.get( cls );
			sk.setLevel( 2 );
			caption( name, Dungeon.hero.heroClass.title() + ": one of dozens of skills to learn" );
			sk.execute( Dungeon.hero, Skill.AC_CAST );
		} );
		steps.add( () -> {
			if (!Dungeon.hero.ready) return false;
			try {
				CellSelector cs = (CellSelector) field( null, "cellSelector", GameScene.class );
				cs.select( target, PointerEvent.LEFT );
			} catch (Exception e) {
				Game.reportException( e );
			}
			return true;
		} );
	}

	//the three of them hammer at the boss, and it hits back
	private void fight( float seconds ){
		steps.add( () -> {
			if (boss != null && boss.isAlive()) {
				if (frame % 15 == 0 && Dungeon.hero.ready) {
					if (Dungeon.hero.handle( boss.pos )) Dungeon.hero.next();
				}
				for (int i = 0; i < party.size(); i++) {
					if ((frame + i * 10) % 30 == 0) party.get( i ).sprite.attack( boss.pos );
				}
			}
			return waited >= seconds;
		} );
	}

	private int freeCellNear( int pos ){
		for (int d : PathFinder.NEIGHBOURS8) {
			int c = pos + d;
			if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.passable[c] && Actor.findChar( c ) == null) return c;
		}
		return pos;
	}

	//the closest village sector to the town, spiralling out
	private static int[] nearestVillage( long seed ){
		for (int r = 1; r < 8; r++) {
			for (int sy = -r; sy <= r; sy++) {
				for (int sx = -r; sx <= r; sx++) {
					if (Math.max( Math.abs( sx ), Math.abs( sy ) ) != r) continue;
					if (WorldStructures.siteType( seed, sx, sy ) == WorldStructures.Site.VILLAGE) {
						return new int[]{ WorldStructures.siteX( seed, sx, sy ), WorldStructures.siteY( seed, sx, sy ) };
					}
				}
			}
		}
		return new int[]{ 0, 0 };
	}

	private static Object field( Object on, String name ) throws Exception {
		return field( on, name, on.getClass() );
	}

	private static Object field( Object on, String name, Class<?> cls ) throws Exception {
		Field f = cls.getDeclaredField( name );
		f.setAccessible( true );
		return f.get( on );
	}

	// ------------------------------------------------------------ the step machine

	private void act( Runnable r ){
		steps.add( () -> { r.run(); return true; } );
	}

	private void until( Step condition ){
		steps.add( () -> condition.run() && !Game.switchingScene() );
	}

	private void delay( float seconds ){
		steps.add( () -> waited >= seconds );
	}

	private void caption( String title, String desc ){
		if (captionStart >= 0 && captionTitle != null) {
			captions.append( String.format( java.util.Locale.ROOT,
					"  {\"start\": %d, \"end\": %d, \"title\": %s, \"desc\": %s},\n",
					captionStart, frame, json( captionTitle ), json( captionDesc ) ) );
		}
		captionStart = frame;
		captionTitle = title;
		captionDesc = desc;
	}

	private static String json( String s ){
		return "\"" + s.replace( "\\", "\\\\" ).replace( "\"", "\\\"" ) + "\"";
	}

	// ------------------------------------------------------------ per frame

	//replaces Game.update() for the recording: a fixed step instead of wall time
	public void frame(){
		Game.elapsed = Game.timeScale * DT;
		Game.timeTotal += Game.elapsed;
		Game.realTime = TimeUtils.millis();

		Game.inputHandler.processAllEvents();
		Music.INSTANCE.update();
		Sample.INSTANCE.update();
		Game.scene().update();
		Camera.updateAll();

		if (index < steps.size()) {
			waited += DT;
			if (waited > TIMEOUT) {
				System.err.println( "trailer: timed out at step " + index );
				finish();
				System.exit( 2 );
			}
			if (steps.get( index ).run()) {
				index++;
				waited = 0;
			}
			//the tour unlocks badges left and right (items seen, floors reached):
			//their banners are not part of the film
			for (com.watabou.noosa.Gizmo g : Game.scene().membersView()) {
				if (g instanceof xyz.gabriwar.warpedpixeldungeon.effects.BadgeBanner) g.killAndErase();
			}
			xyz.gabriwar.warpedpixeldungeon.effects.BadgeBanner.showing.clear();
			//the loading screens are cut: every change of place is a hard cut
			if (!(Game.scene() instanceof InterlevelScene)) {
				capture();
				frame++;
			}
		}
	}

	private void capture(){
		try {
			if (fbo == null) {
				fbo = new FrameBuffer( Pixmap.Format.RGBA8888, width, height, false );
				Texture.clear();
				ffmpeg = new ProcessBuilder( "ffmpeg", "-y", "-loglevel", "error",
						"-f", "rawvideo", "-pix_fmt", "rgba", "-s", width + "x" + height, "-r", String.valueOf( FPS ), "-i", "-",
						"-vf", "vflip", "-c:v", "libx264", "-preset", "fast", "-crf", "17", "-pix_fmt", "yuv420p", outFile )
						.redirectError( ProcessBuilder.Redirect.INHERIT ).start();
				pipe = new java.io.BufferedOutputStream( ffmpeg.getOutputStream(), 1 << 22 );
				row = new byte[width * height * 4];
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

			ByteBuffer pixels = pixmap.getPixels();
			pixels.position( 0 );
			pixels.get( row );
			pixmap.dispose();
			pipe.write( row );
		} catch (IOException e) {
			Game.reportException( e );
		}
	}

	//closes the film and the captions; called by the game on exit
	public void finish(){
		caption( null, null );
		try {
			if (pipe != null) {
				pipe.close();
				ffmpeg.waitFor();
			}
			String text = captions.toString();
			if (text.endsWith( ",\n" )) text = text.substring( 0, text.length() - 2 ) + "\n";
			Gdx.files.absolute( outFile + ".captions.json" ).writeString( text + "]\n", false );
			System.out.println( "trailer: " + frame + " frames, " + (frame / FPS) + "s -> " + outFile );
		} catch (Exception e) {
			Game.reportException( e );
		}
		instance = null;
	}
}
