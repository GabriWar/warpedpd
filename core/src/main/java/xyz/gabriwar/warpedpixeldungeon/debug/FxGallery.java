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
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gnoll;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxModule;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxModules;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The effects gallery: every area's effects shown one after another on a stage carved round the
 * hero (FxStage), so the effects can be judged where they will be seen - by day and by night.
 *
 *   fx-gallery          every area's page (FxModules order), each played twice: by day, then at
 *                       night (DayNightCycle.debugPhaseOverride), every exhibit logged as it plays
 *                       ("FX <page> / <exhibit>")
 *   fx-gallery-<key>    one area's page, by day and by night, over and over
 *   fx-busy             the busy fight the budget is measured by: 15 cells of fire, 20 of toxic gas,
 *                       two burning dummies, a fire bolt every 2 s, four blows a second, heavy rain
 *                       in a storm; FxBudget's tier and live counts logged every 2 s (for the
 *                       debug Profiler)
 *
 * plus every area's own scenes (FxModule.scenes). An area adds its exhibits in its own module
 * (FxModule.exhibits: page.add( name, period, stage -> ... )), so no two areas edit one file.
 * The phase override is let go when the scene is left, run again or another debug scene is run.
 */
public final class FxGallery {

	private FxGallery(){}

	/** One exhibit: it plays on the stage, on the render thread. */
	public interface Exhibit {
		void play( FxStage stage );
	}

	/** An area's page: its exhibits in order, each with how long it is given before the next. */
	public static final class Page {
		public final String key, title;
		final ArrayList<String> names = new ArrayList<>();
		final ArrayList<Float> periods = new ArrayList<>();
		final ArrayList<Exhibit> exhibits = new ArrayList<>();

		public Page( String key, String title ){
			this.key = key;
			this.title = title;
		}

		/** An exhibit, given `period` seconds before the next. */
		public Page add( String name, float period, Exhibit e ){
			names.add( name );
			periods.add( period );
			exhibits.add( e );
			return this;
		}

		public int size(){
			return exhibits.size();
		}

		public String name( int i ){
			return names.get( i );
		}

		public float period( int i ){
			return periods.get( i );
		}
	}

	/** An area's page, as its module fills it. */
	public static Page page( FxModule m ){
		Page p = new Page( m.key(), m.title() );
		m.exhibits( p );
		return p;
	}

	/** The gallery's scenes, every area's own after them. */
	public static final DebugScenes.Scene[] SCENES = scenes();

	private static DebugScenes.Scene[] scenes(){
		ArrayList<DebugScenes.Scene> out = new ArrayList<>();
		out.add( new Gallery( null ) );
		for (FxModule m : FxModules.ALL) out.add( new Gallery( m ) );
		out.add( new Busy() );
		for (FxModule m : FxModules.ALL) m.scenes( out );
		return out.toArray( new DebugScenes.Scene[0] );
	}

	// ------------------------------------------------------------------ the scenes

	//every page in turn, day then night, once; or one page over and over
	private static final class Gallery implements DebugScenes.Scene {
		final FxModule only;

		Gallery( FxModule only ){
			this.only = only;
		}

		@Override
		public String id(){
			return only == null ? "fx-gallery" : "fx-gallery-" + only.key();
		}

		@Override
		public String title(){
			return only == null ? "Effects: the whole gallery, by day and night" : "Effects: " + only.title();
		}

		@Override
		public void apply( Hero hero ){
			if (!DebugScenes.Room.ensure( this, hero )) return;
			FxStage stage = FxStage.build( hero );
			ArrayList<Page> pages = new ArrayList<>();
			if (only != null){
				pages.add( page( only ) );
			} else {
				for (FxModule m : FxModules.ALL) pages.add( page( m ) );
			}
			Runner.start( new Runner( stage, pages, only != null ) );
			GLog.i( "Scene: " + (only == null ? "every area's effects, a page by day and again at night"
					: only.title() + ", by day and at night, over and over") + "; each exhibit is logged as it plays" );
		}
	}

	//the busy fight: what a phone must still draw at its frame rate
	private static final class Busy implements DebugScenes.Scene {
		@Override
		public String id(){
			return "fx-busy";
		}

		@Override
		public String title(){
			return "Effects: the busy fight (budget and governor, for the Profiler)";
		}

		@Override
		public void apply( Hero hero ){
			if (!DebugScenes.Room.ensure( this, hero )) return;
			FxStage stage = FxStage.build( hero );
			Runner.start( new BusyRunner( stage ) );
			GLog.i( "Scene: 15 cells of fire, 20 of toxic gas, two burning dummies, a fire bolt every 2 s, four blows"
					+ " a second, heavy rain in a storm; the effects' budget logged every 2 s" );
		}
	}

	// ------------------------------------------------------------------ the runners

	//plays pages on the stage, frame by frame, on the render thread; let go with the scene
	static class Runner extends Gizmo {

		//the one running: a new one ends it
		private static Runner current;

		final FxStage stage;
		private final List<Page> pages;
		private final boolean loop;
		private int page, pass, item;
		private boolean started;
		private float wait;

		Runner( FxStage stage, List<Page> pages, boolean loop ){
			this.stage = stage;
			this.pages = pages;
			this.loop = loop;
		}

		static void start( Runner r ){
			if (current != null) current.stop();
			current = r;
			Game.scene().add( r );
		}

		@Override
		public void update(){
			float dt = Game.elapsed;
			stage.update( dt );
			if ((wait -= dt) > 0) return;
			step();
		}

		//the next exhibit: on through the page, then the page again at night, then the next page
		private void step(){
			if (pages.isEmpty()){
				stop();
				return;
			}
			if (!started){
				started = true;
				begin();
			} else if (++item >= pages.get( page ).size()){
				item = 0;
				if (++pass > 1){
					pass = 0;
					if (++page >= pages.size()){
						if (!loop){
							GLog.p( "FX gallery: done" );
							stop();
							return;
						}
						page = 0;
					}
				}
				begin();
			}
			Page p = pages.get( page );
			if (p.size() == 0){
				wait = 1f;
				return;
			}
			GLog.i( "FX " + p.title + " / " + p.name( item ) + (pass == 0 ? "" : " (night)") );
			p.exhibits.get( item ).play( stage );
			wait = p.period( item );
		}

		//a page's pass: the stage as built, by day or at night
		private void begin(){
			stage.reset();
			phase( pass == 0 ? DayNightCycle.Phase.DAY : DayNightCycle.Phase.NIGHT );
			GLog.p( "FX page: " + pages.get( page ).title + (pass == 0 ? ", by day" : ", at night") );
		}

		void stop(){
			if (current == this) current = null;
			phase( null );
			stage.reset();
			killAndErase();
		}

		@Override
		public void destroy(){
			super.destroy();
			if (current == this){
				current = null;
				//the scene is going: the next one works its own sky out
				DayNightCycle.debugPhaseOverride = null;
			}
		}
	}

	/** Whatever page is playing stopped, its time of day let go: another debug scene is run. */
	static void stopRunning(){
		if (Runner.current != null) Runner.current.stop();
	}

	//the time of day held (null lets it go), the sky's light worked out again at once
	static void phase( DayNightCycle.Phase p ){
		if (DayNightCycle.debugPhaseOverride == p) return;
		DayNightCycle.debugPhaseOverride = p;
		if (Dungeon.level != null && Dungeon.hero != null){
			ClimateManager.onHeroTurn();
			GameScene.updateDayNightTint();
		}
	}

	//the busy fight, kept going, the budget logged every 2 s
	static final class BusyRunner extends Runner {
		private float boltIn, hitIn, logIn;
		private FxStage.Puppet caster, fighter;

		BusyRunner( FxStage stage ){
			super( stage, new ArrayList<>(), true );
		}

		@Override
		public void update(){
			float dt = Game.elapsed;
			stage.update( dt );
			if (caster == null) set();
			if (caster == null || fighter == null) return;
			if ((boltIn -= dt) <= 0){
				boltIn = 2f;
				caster.zap( stage.wetRat.pos, () -> MagicMissile.boltFromChar( caster.sprite.parent, MagicMissile.FIRE,
						caster.sprite, stage.wetRat.pos, null ) );
			}
			if ((hitIn -= dt) <= 0){
				hitIn = 0.25f;
				fighter.attack( stage.dryRat.pos, () -> {
					if (stage.dryRat.sprite != null){
						stage.dryRat.sprite.bloodBurstA( fighter.sprite.center(), 6 );
						stage.dryRat.sprite.flash();
						Wound.hit( stage.dryRat );
					}
				} );
			}
			if ((logIn -= dt) <= 0){
				logIn = 2f;
				GLog.i( String.format( Locale.ENGLISH, "FX busy: tier %s, frame %.1f ms, particles %d, lights %d, decals %d, rings %d, droplets %d",
						new String[]{ "FULL", "REDUCED", "MINIMAL" }[FxBudget.tier()], FxBudget.frameTime() * 1000,
						FxBudget.live( FxBudget.PARTICLES ), FxBudget.live( FxBudget.LIGHTS ), FxBudget.live( FxBudget.DECALS ),
						FxBudget.live( FxBudget.RINGS ), FxBudget.live( FxBudget.DROPLETS ) ) );
			}
		}

		//the fight set up: at night, the fire and the gas, the burning dummies, the storm
		private void set(){
			phase( DayNightCycle.Phase.NIGHT );
			stage.reset();
			int[] fire = new int[15];
			int n = 0;
			for (int c : stage.grass()) fire[n++] = c;
			for (int c : stage.stone()) fire[n++] = c;
			fire[n++] = stage.cell( -3, -2 );
			fire[n++] = stage.cell( -2, -2 );
			fire[n] = stage.cell( -1, -2 );
			stage.seed( Fire.class, 6, fire );
			int[] gas = new int[20];
			n = 0;
			for (int dy = -4; dy <= -1 && n < gas.length; dy++){
				for (int dx = 1; dx <= 5 && n < gas.length; dx++) gas[n++] = stage.cell( dx, dy );
			}
			stage.seed( ToxicGas.class, 60, gas );
			if (stage.dryRat.sprite != null) stage.dryRat.sprite.add( CharSprite.State.BURNING );
			if (stage.skeleton.sprite != null) stage.skeleton.sprite.add( CharSprite.State.BURNING );
			stage.sky( PrecipType.RAIN, 0.6f, 15f, true );
			caster = stage.puppet( Gnoll.class, stage.cell( 1, 1 ) );
			fighter = stage.puppet( Gnoll.class, stage.cell( -1, 2 ) );
			boltIn = 0.5f;
		}

		@Override
		void stop(){
			super.stop();
			DebugScenes.clearWeather();
		}
	}
}
