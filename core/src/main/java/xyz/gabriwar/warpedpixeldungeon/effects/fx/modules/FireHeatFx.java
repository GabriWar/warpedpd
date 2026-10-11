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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.modules;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.CampFire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.FiendFire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Firewind;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ForgeCoals;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.HalomethaneFire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.HeatHaze;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.HellFire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Inferno;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.MagicFire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.NestCoals;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Elemental;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Warlock;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.StarStreak;
import xyz.gabriwar.warpedpixeldungeon.effects.WorldEventDecor;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxModule;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.BurstWatch;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.Embers;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.FireField;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.FireHits;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.FiredampFront;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.LightRef;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.PlaceLight;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BlastParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ElmoParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SacrificialParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SteamParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.HolyFlameParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Firebomb;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLiquidFlame;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfFireblast;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfFirebolt;
import xyz.gabriwar.warpedpixeldungeon.levels.CityLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.PrisonLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.QuickSlot;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.utils.PointF;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fire and heat (docs/fx/fire-heat.md): flames of every kind, blasts, embers, smoke, steam,
 * scorches, heat shimmer, and the light fire throws on the ground, the walls and the water. Its
 * code is effects/fx/fire; this wires it into the scene (a new scene's reset, ground burnt to
 * embers, something burning stepping into water) and fills its gallery page and its lab scene.
 */
public class FireHeatFx implements FxModule {

	@Override
	public String key(){
		return "fire-heat";
	}

	@Override
	public String title(){
		return "Fire and heat";
	}

	@Override
	public void sceneCreated( GameScene scene ){
		//the last scene took its fields, watches and lights with it
		FireField.sceneCreated();
		BurstWatch.reset();
		PlaceLight.reset();
		xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.Reflection.reset();
		LightRef.reset();
	}

	@Override
	public void terrainChanged( int cell, int oldTerrain, int newTerrain ){
		//ground burnt to embers: its scorch, specks and thin smoke
		if (newTerrain == Terrain.EMBERS && oldTerrain != Terrain.EMBERS) Embers.lay( cell );
	}

	// ------------------------------------------------------------------ the gallery

	@Override
	public void exhibits( FxGallery.Page page ){
		page.add( "a 3x3 fire on the grass", 5f, s -> {
			for (int dy = 0; dy < 3; dy++){
				for (int dx = 0; dx < 3; dx++) s.seed( Fire.class, dx == 1 && dy == 1 ? 10 : 6, s.cell( -1 + dx, -4 + dy ) );
			}
			s.after( 4.5f, s::clearBlobs );
		} );
		page.add( "fire at the pool's edge, licks on the water", 4.5f, s -> {
			s.seed( Fire.class, 6, s.cell( 1, -1 ), s.cell( 1, 0 ), s.cell( 1, 1 ), s.cell( 2, 0 ) );
			s.after( 4f, s::clearBlobs );
		} );
		page.add( "a burning rat walks into the pool", 3f, s -> {
			FxStage.Puppet p = s.puppet( Rat.class, s.cell( 1, 0 ) );
			if (p == null) return;
			p.sprite.add( CharSprite.State.BURNING );
			s.after( 1f, () -> p.move( s.cell( 2, 0 ) ) );
			//its flames going out on the water: the burning look's douse (characters-combat)
			s.after( 1.3f, () -> p.sprite.remove( CharSprite.State.BURNING ) );
		} );
		page.add( "a bomb", 3f, s -> {
			s.burstCenter( s.stone()[0], BlastParticle.FACTORY, 30 );
			for (int n : s.stone()) s.burst( n, SmokeParticle.FACTORY, 4 );
		} );
		page.add( "a firebomb", 4f, s -> {
			int c = s.stone()[0];
			for (int n : s.stone()){
				s.seed( Fire.class, n == c ? 10 : 2, n );
				s.burst( n, FlameParticle.FACTORY, 5 );
			}
			s.after( 3.5f, s::clearBlobs );
		} );
		page.add( "DM-300's blast", 3.5f, s -> s.burstCenter( s.stone()[0], BlastParticle.FACTORY, 100 ) );
		page.add( "a fire bolt", 2.5f, s -> {
			FxStage.Puppet p = s.puppet( Warlock.class, s.cell( -1, 0 ) );
			if (p == null) return;
			int at = s.dryRat.pos;
			p.zap( at, () -> MagicMissile.boltFromChar( p.sprite.parent, MagicMissile.FIRE, p.sprite, at,
					() -> FireHits.firebolt( at ) ) );
		} );
		page.add( "a fireblast's cone, three charges", 3f, s -> {
			FxStage.Puppet p = s.puppet( Warlock.class, s.cell( -1, 0 ) );
			if (p == null) return;
			p.zap( s.poolMiddle(), () -> {
				for (int dy = -1; dy <= 1; dy++){
					final int at = s.cell( 5, dy );
					MagicMissile.boltFromChar( p.sprite.parent, MagicMissile.FIRE_CONE, p.sprite, at, () -> FireHits.heat( at, FireHits.CONE_EMBERS, 0 ) );
				}
			} );
		} );
		page.add( "eight fires: fire, hell, fiend, magic, halomethane, elmo, sacrificial, holy", 4.5f, s -> {
			s.seed( Fire.class, 8, s.cell( -4, 4 ) );
			s.seed( HellFire.class, 8, s.cell( -3, 4 ) );
			s.seed( FiendFire.class, 8, s.cell( -2, 4 ) );
			s.seed( MagicFire.class, 8, s.cell( -1, 4 ) );
			s.seed( HalomethaneFire.class, 8, s.cell( 0, 4 ) );
			s.pour( s.cell( 1, 4 ), ElmoParticle.FACTORY, 0.05f, 4f );
			s.pour( s.cell( 2, 4 ), SacrificialParticle.FACTORY, 0.04f, 4f );
			s.pour( s.cell( 3, 4 ), HolyFlameParticle.FACTORY, 0.05f, 4f );
			s.after( 4f, s::clearBlobs );
		} );
		page.add( "an inferno, a fire wind and a heat haze", 4.5f, s -> {
			s.seed( Inferno.class, 30, s.cell( -5, -1 ), s.cell( -4, -1 ), s.cell( -5, 0 ), s.cell( -4, 0 ) );
			s.seed( Firewind.class, 20, s.cell( -1, 2 ), s.cell( 0, 2 ) );
			s.seed( HeatHaze.class, 8, s.cell( 2, 3 ), s.cell( 3, 3 ) );
			s.after( 4f, s::clearBlobs );
		} );
		page.add( "coal beds: a forge, a camp fire, a nest", 4f, s -> {
			s.seed( ForgeCoals.class, 124, s.cell( -5, -1 ), s.cell( -4, -1 ) );
			s.seed( CampFire.class, 124, s.cell( -1, 2 ) );
			s.seed( NestCoals.class, 124, s.cell( 1, 2 ) );
			s.after( 3.5f, s::clearBlobs );
		} );
		page.add( "a prison torch, a city's green flame and a vent", 4.5f, s -> {
			int[] wall = s.wall();
			show( new PrisonLevel.Torch( wall[0] ), 4.4f );
			show( new CityLevel.GreenFlame( wall[1] ), 4.4f );
			show( new CityLevel.Smoke( wall[1] ), 4.4f );
		} );
		page.add( "a sacrifice", 3.5f, s -> {
			int c = s.cell( -2, -2 );
			s.pour( c, SacrificialParticle.FACTORY, 0.05f, 3f );
			s.after( 1f, () -> s.burst( c, SacrificialParticle.FACTORY, SacrificialParticle.SACRIFICE ) );
		} );
		page.add( "a star's crater, hot and cooling", 6f, s -> {
			if (!(Game.scene() instanceof GameScene)) return;
			final WorldEventDecor decor = new WorldEventDecor();
			final int scene = System.identityHashCode( Game.scene() );
			PointF at = FxStage.center( s.stone()[0] );
			final float x = at.x - 8, y = at.y - 8;
			decor.show( scene, crater( x, y, 3 ), Collections.emptyList() );
			s.after( 2f, () -> decor.show( scene, crater( x, y, 2 ), Collections.emptyList() ) );
			s.after( 3.5f, () -> decor.show( scene, crater( x, y, 1 ), Collections.emptyList() ) );
			s.after( 5f, () -> decor.show( scene, Collections.emptyList(), Collections.emptyList() ) );
		} );
		page.add( "firedamp: a flame licks at it, then it goes up", 4.5f, s -> {
			final int[] pocket = firedamp( s );
			FiredampFront.arming( pocket );
			s.after( 1.2f, () -> {
				FiredampFront.ignition( pocket );
				s.burstCenter( pocket[0], BlastParticle.FACTORY, 30 );
				for (int c : pocket){
					s.burst( c, BlastParticle.FACTORY, 3 );
					s.burst( c, SmokeParticle.FACTORY, 2 );
				}
			} );
		} );
		page.add( "a falling star", 3f, s -> {
			PointF to = FxStage.center( s.stone()[0] );
			StarStreak.fall( new PointF( to.x - 60, to.y - 140 ), to, 0.7f, true );
		} );
		page.add( "a quench, and a spring's steam", 4f, s -> {
			s.burst( s.cell( 2, -1 ), SteamParticle.FACTORY, SteamParticle.QUENCH );
			s.pour( s.poolMiddle(), SteamParticle.FACTORY, 0.45f, 3.5f );
			s.pour( s.cell( 5, 1 ), SteamParticle.FACTORY, 0.45f, 3.5f );
		} );
		page.add( "elementals: fire, newborn, frost, shock, chaos", 4f, s -> {
			Class<?>[] kinds = { Elemental.FireElemental.class, Elemental.NewbornFireElemental.class, Elemental.FrostElemental.class,
					Elemental.ShockElemental.class, Elemental.ChaosElemental.class };
			for (int i = 0; i < kinds.length; i++){
				@SuppressWarnings("unchecked")
				Class<? extends Mob> k = (Class<? extends Mob>) kinds[i];
				s.puppet( k, s.cell( -3 + i, 2 ) );
			}
		} );
	}

	//a crater posted as the world's events post it, its heart at (x, y)
	private static List<WorldEventDecor.Crater> crater( float x, float y, int heat ){
		ArrayList<WorldEventDecor.Crater> out = new ArrayList<>();
		out.add( new WorldEventDecor.Crater( 0xF1AE, x, y, heat ) );
		return out;
	}

	//a pocket of firedamp over the stone and round it, its first cell where the flame licks
	private static int[] firedamp( FxStage s ){
		ArrayList<Integer> cells = new ArrayList<>();
		for (int dy = -2; dy <= 1; dy++){
			for (int dx = -7; dx <= -3; dx++){
				int c = s.cell( dx, dy );
				if (c != -1 && !Dungeon.level.solid[c]) cells.add( c );
			}
		}
		int[] out = new int[cells.size()];
		for (int i = 0; i < out.length; i++) out[i] = cells.get( i );
		return out;
	}

	//one of a level's visuals shown among the effects for a while, then taken off (with or
	//without the gallery's timers)
	private static void show( final Gizmo visual, final float seconds ){
		Group effects = GameScene.layer( GameScene.Layer.EFFECTS );
		if (effects == null) return;
		effects.add( visual );
		effects.add( new Gizmo(){
			float left = seconds;

			@Override
			public void update(){
				if ((left -= Game.elapsed) > 0) return;
				visual.killAndErase();
				visual.destroy();
				killAndErase();
			}
		} );
	}

	// ------------------------------------------------------------------ the lab

	@Override
	public void scenes( List<DebugScenes.Scene> out ){
		out.add( new FireLab( false ) );
		out.add( new FireLab( true ) );
	}

	/**
	 * fx-fire-lab (and fx-fire-lab-night): the blank room with a strip of grass to burn to the
	 * north, a pool to the east, a bed of forge coals to the west, three hardy rats to burn (one in
	 * the pool) and the fire to do it with - wands of firebolt and fireblast +3, bombs, firebombs
	 * and liquid flame, quickslotted - by day or held at night.
	 */
	static final class FireLab implements DebugScenes.Scene {

		static final int HARDY = 9999;
		final boolean night;

		FireLab( boolean night ){
			this.night = night;
		}

		@Override
		public String id(){
			return night ? "fx-fire-lab-night" : "fx-fire-lab";
		}

		@Override
		public String title(){
			return "Effects: the fire lab" + (night ? ", at night" : "");
		}

		@Override
		public void apply( Hero hero ){
			if (Dungeon.level instanceof OverworldLevel){
				//the blank room sends the hero to the sewers first; the lab is run from there
				GLog.w( "Scene: the fire lab is set up in the dungeon: run it again once in the sewers." );
				DebugScenes.byId( "blank-room" ).apply( hero );
				return;
			}
			DebugScenes.byId( "blank-room" ).apply( hero );
			Level level = Dungeon.level;
			int w = level.width(), hx = hero.pos % w, hy = hero.pos / w;
			for (int dx = -3; dx <= 3; dx++) set( level, hx + dx, hy - 3, dx < 0 ? Terrain.HIGH_GRASS : Terrain.GRASS );
			for (int dy = -1; dy <= 1; dy++) for (int dx = 2; dx <= 6; dx++) set( level, hx + dx, hy + dy, Terrain.WATER );
			GameScene.updateMap();
			level.cleanWalls();
			GameScene.updateFog();
			int forge = cell( level, hx - 5, hy );
			if (forge != -1) GameScene.add( xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob.seed( forge, 1, ForgeCoals.class ) );
			int forge2 = cell( level, hx - 5, hy + 1 );
			if (forge2 != -1) GameScene.add( xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob.seed( forge2, 1, ForgeCoals.class ) );

			hardy( cell( level, hx + 1, hy - 3 ) );
			hardy( cell( level, hx + 4, hy ) );
			hardy( cell( level, hx - 2, hy + 3 ) );

			give( hero, new WandOfFirebolt() );
			give( hero, new WandOfFireblast() );
			give( hero, new Bomb().quantity( 3 ) );
			give( hero, new Firebomb().quantity( 3 ) );
			give( hero, new PotionOfLiquidFlame().quantity( 2 ) );
			Item.updateQuickslot();

			DayNightCycle.debugPhaseOverride = night ? DayNightCycle.Phase.NIGHT : null;
			ClimateManager.onHeroTurn();
			GameScene.updateDayNightTint();
			GLog.i( "Scene: burn the grass, zap the rats with the wands (fireblast's cone at three charges), throw the bombs"
					+ " and the liquid flame by the pool, and light the forge's cold coals with fire" + (night ? ", at night." : "."
					+ " fx-fire-lab-night is the same at night.") );
		}

		private static int cell( Level level, int x, int y ){
			if (x <= 0 || y <= 0 || x >= level.width() - 1 || y >= level.height() - 1) return -1;
			return x + y * level.width();
		}

		private static void set( Level level, int x, int y, int terrain ){
			int c = cell( level, x, y );
			if (c == -1 || c == Dungeon.hero.pos) return;
			Level.set( c, terrain );
			level.visited[c] = true;
			level.mapped[c] = true;
		}

		private static void hardy( int c ){
			if (c == -1 || xyz.gabriwar.warpedpixeldungeon.actors.Actor.findChar( c ) != null) return;
			Mob m = Reflection.newInstance( Rat.class );
			if (m == null) return;
			m.pos = c;
			m.HP = m.HT = HARDY;
			m.state = m.PASSIVE;
			GameScene.add( m );
		}

		private static void give( Hero hero, Item item ){
			item.identify();
			if (item instanceof Wand){
				item.upgrade( 3 );
				((Wand) item).curCharges = ((Wand) item).maxCharges;
			}
			if (!item.collect( hero.belongings.backpack )) return;
			for (int s = 0; s < QuickSlot.SIZE; s++){
				if (Dungeon.quickslot.getItem( s ) == null){
					Dungeon.quickslot.setSlot( s, item );
					break;
				}
			}
		}
	}
}
