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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.nature;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Foliage;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Regrowth;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.WaterOfHealth;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.HallowedGround;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.HolyLance;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Wraith;
import xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfRegrowth;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.plants.Earthroot;
import xyz.gabriwar.warpedpixeldungeon.plants.Lavenderlantern;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.MissileSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.utils.PointF;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.List;

/**
 * The area's own debug scenes (debug menu > Scenes, or -Pscene=<id>), on a dungeon floor (from the
 * surface run fx-gallery once, or go down a floor: the surface's slices are painted over their
 * terrain):
 *
 *   fx-garden / fx-garden-night  high grass and grass, a Regrowth potion's pool, a Foliage patch,
 *                                Sungrass, Earthroot and Lavenderlantern, and the wands of regrowth
 *                                +3 and +10 in the pack, by day or at night
 *   fx-holy                      at night, by undead dummies: the holy spells' light played one
 *                                after another, over and over (the hero stays as he is)
 *   fx-heal                      a well of health, a potion of healing and a lifesteal sword in the
 *                                pack, a shielded ally and hardy dummies to hit; a heal and a shield
 *                                shown every few seconds
 */
public final class NatureScenes {

	private NatureScenes(){}

	public static void add( List<DebugScenes.Scene> out ){
		out.add( new Garden( false ) );
		out.add( new Garden( true ) );
		out.add( new Holy() );
		out.add( new Heal() );
	}

	// ------------------------------------------------------------------ the clearing

	static final int HALF_W = 7, HALF_H = 5;

	//a bare walled room round the hero (on a dungeon floor), false where there is none to make
	static boolean clearing( Hero hero ){
		Level level = Dungeon.level;
		if (level == null || level instanceof OverworldLevel){
			GLog.w( "Scene: go down to a dungeon floor first (or run fx-gallery once, which takes you there)" );
			return false;
		}
		Sequence.stop();
		int w = level.width(), hx = hero.pos % w, hy = hero.pos / w;
		for (int dy = -HALF_H - 1; dy <= HALF_H + 1; dy++){
			for (int dx = -HALF_W - 1; dx <= HALF_W + 1; dx++){
				int x = hx + dx, y = hy + dy;
				if (x <= 0 || y <= 0 || x >= w - 1 || y >= level.height() - 1) continue;
				int cell = x + y * w;
				boolean wall = Math.abs( dx ) > HALF_W || Math.abs( dy ) > HALF_H;
				Level.set( cell, wall ? Terrain.WALL : Terrain.EMPTY );
				level.plants.remove( cell );
				level.traps.remove( cell );
				if (level.heaps.get( cell ) != null) level.heaps.get( cell ).destroy();
				if (!wall && level.blobs != null){
					for (Blob b : level.blobs.values()) b.clear( cell );
				}
				for (Mob m : level.mobs.toArray( new Mob[0] )){
					if (m.pos == cell){
						if (m.sprite != null) m.sprite.killAndErase();
						m.destroy();
					}
				}
				level.visited[cell] = true;
			}
		}
		GameScene.updateMap();
		level.cleanWalls();
		GameScene.updateFog();
		return true;
	}

	static int at( Hero hero, int dx, int dy ){
		Level level = Dungeon.level;
		int w = level.width(), x = hero.pos % w + dx, y = hero.pos / w + dy;
		if (x <= 0 || y <= 0 || x >= w - 1 || y >= level.height() - 1) return -1;
		return x + y * w;
	}

	static void set( int cell, int terrain ){
		if (cell == -1 || Actor.findChar( cell ) instanceof Hero) return;
		Level.set( cell, terrain );
		Dungeon.level.visited[cell] = true;
		Dungeon.level.mapped[cell] = true;
	}

	//a hardy dummy that does nothing
	static Mob dummy( Class<? extends Mob> kind, int cell, Char.Alignment side ){
		if (cell == -1 || Actor.findChar( cell ) != null) return null;
		Mob m = Reflection.newInstance( kind );
		if (m == null) return null;
		m.pos = cell;
		m.state = m.PASSIVE;
		if (side != null) m.alignment = side;
		GameScene.add( m );
		m.HP = m.HT = 9999;
		return m;
	}

	//the time of day held (null lets it go), the sky's light worked out again at once
	static void phase( DayNightCycle.Phase p ){
		DayNightCycle.debugPhaseOverride = p;
		if (Dungeon.level != null && Dungeon.hero != null){
			ClimateManager.onHeroTurn();
			GameScene.updateDayNightTint();
		}
	}

	static PointF mid( CharSprite s ){
		return new PointF( s.x + s.width() / 2f, s.y + s.height() / 2f );
	}

	// ------------------------------------------------------------------ a sequence of moments

	/** Steps played a few seconds apart on the render thread, over and over, until another of
	 *  these scenes starts. */
	static final class Sequence extends Gizmo {
		private static Sequence current;

		private final ArrayList<Runnable> steps = new ArrayList<>();
		private final float every;
		private float wait = 0.5f;
		private int next;

		Sequence( float every ){
			this.every = every;
		}

		Sequence step( Runnable r ){
			steps.add( r );
			return this;
		}

		void start(){
			stop();
			current = this;
			Game.scene().add( this );
		}

		static void stop(){
			if (current != null){
				current.killAndErase();
				current = null;
			}
		}

		@Override
		public void update(){
			super.update();
			if (steps.isEmpty() || Dungeon.hero == null || Dungeon.hero.sprite == null) return;
			if ((wait -= Game.elapsed) > 0) return;
			wait = every;
			steps.get( next ).run();
			next = (next + 1) % steps.size();
		}
	}

	// ------------------------------------------------------------------ fx-garden

	static final class Garden implements DebugScenes.Scene {
		final boolean night;

		Garden( boolean night ){
			this.night = night;
		}

		@Override
		public String id(){
			return night ? "fx-garden-night" : "fx-garden";
		}

		@Override
		public String title(){
			return "Effects: the garden (grass, growth, foliage, plants, wands of regrowth)" + (night ? ", at night" : "");
		}

		@Override
		public void apply( Hero hero ){
			if (!clearing( hero )) return;
			for (int dx = -5; dx <= -1; dx++) set( at( hero, dx, -3 ), Terrain.HIGH_GRASS );
			for (int dx = 1; dx <= 5; dx++) set( at( hero, dx, -3 ), Terrain.GRASS );
			set( at( hero, -6, 3 ), Terrain.WATER );
			set( at( hero, -5, 3 ), Terrain.WATER );
			GameScene.updateMap();
			Level level = Dungeon.level;
			for (int dy = 1; dy <= 3; dy++){
				for (int dx = -4; dx <= -2; dx++){
					int c = at( hero, dx, dy );
					if (c != -1) GameScene.add( Blob.seed( c, 40, Regrowth.class ) );
				}
			}
			for (int dy = 1; dy <= 2; dy++){
				for (int dx = 2; dx <= 3; dx++){
					int c = at( hero, dx, dy );
					if (c != -1) GameScene.add( Blob.seed( c, 20, Foliage.class ) );
				}
			}
			plant( level, new Sungrass.Seed(), at( hero, 5, -1 ) );
			plant( level, new Earthroot.Seed(), at( hero, 5, 1 ) );
			plant( level, new Lavenderlantern.Seed(), at( hero, 5, 3 ) );
			wand( hero, 3 );
			wand( hero, 10 );
			phase( night ? DayNightCycle.Phase.NIGHT : DayNightCycle.Phase.DAY );
			GLog.i( "Scene: walk the high grass (north), stand in the Regrowth (south-west) and the foliage (south-east),"
					+ " step on the plants (east), zap the wands of regrowth +3 and +10 (in your pack)"
					+ (night ? "; the night is held" : "; the day is held") );
		}

		private static void plant( Level level, xyz.gabriwar.warpedpixeldungeon.plants.Plant.Seed seed, int cell ){
			if (cell != -1) level.plant( seed, cell );
		}

		private static void wand( Hero hero, int level ){
			Wand w = (Wand) new WandOfRegrowth().upgrade( level );
			w.identify();
			w.curCharges = w.maxCharges;
			w.collect( hero.belongings.backpack );
		}
	}

	// ------------------------------------------------------------------ fx-holy

	static final class Holy implements DebugScenes.Scene {
		@Override
		public String id(){
			return "fx-holy";
		}

		@Override
		public String title(){
			return "Effects: holy light on undead dummies, at night";
		}

		@Override
		public void apply( final Hero hero ){
			if (!clearing( hero )) return;
			phase( DayNightCycle.Phase.NIGHT );
			final Mob skel = dummy( Skeleton.class, at( hero, 3, 0 ), null );
			final Mob wraith = dummy( Wraith.class, at( hero, 3, -2 ), null );
			final Mob ally = dummy( Rat.class, at( hero, -2, 1 ), Char.Alignment.ALLY );
			final int[] ground = { at( hero, -3, -2 ), at( hero, -2, -2 ), at( hero, -1, -2 ), at( hero, -3, -3 ), at( hero, -2, -3 ) };
			new Sequence( 2.5f )
					.step( () -> {
						GLog.i( "FX holy: bless (a star where the flare was)" );
						HolyFx.star( hero.sprite, HolyFx.BLESS, false, 8 );
						hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, "15", FloatingText.SHIELDING );
					} )
					.step( () -> {
						if (skel == null || skel.sprite == null) return;
						GLog.i( "FX holy: smite" );
						skel.sprite.burst( 0xFFFFFFFF, 10 );
						HolyFx.impact( skel.sprite );
					} )
					.step( () -> {
						if (skel == null || skel.sprite == null) return;
						GLog.i( "FX holy: sunray" );
						hero.sprite.parent.add( new Beam.SunRay( mid( hero.sprite ), mid( skel.sprite ) ) );
						HolyFx.sunMotes( skel.sprite );
					} )
					.step( () -> {
						if (wraith == null || wraith.sprite == null) return;
						GLog.i( "FX holy: guiding light and its mark" );
						MagicMissile.boltFromChar( hero.sprite.parent, MagicMissile.LIGHT_MISSILE, hero.sprite, wraith.pos,
								() -> HolyFx.mark( wraith.sprite, true ) );
					} )
					.step( () -> {
						if (wraith != null && wraith.sprite != null) HolyFx.mark( wraith.sprite, false );
						if (skel == null || skel.sprite == null) return;
						GLog.i( "FX holy: holy lance" );
						MissileSprite lance = (MissileSprite) hero.sprite.parent.recycle( MissileSprite.class );
						HolyFx.lanceGlow( lance );
						lance.reset( hero.sprite, skel.sprite, new HolyLance.HolyLanceVFX(), () -> {
							skel.sprite.burst( 0xFFFFFFFF, 10 );
							HolyFx.impact( skel.sprite );
						} );
					} )
					.step( () -> {
						GLog.i( "FX holy: the avatar's halo" );
						final HolyFx.Halo h = HolyFx.halo( hero.sprite, 2 );
						int[] edge = new int[16];
						int n = 0;
						for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++)
							if (Math.max( Math.abs( dx ), Math.abs( dy ) ) == 2) edge[n++] = at( hero, dx, dy );
						HolyFx.sparkles( edge, n );
						hero.sprite.parent.add( new Delay( 2.3f, h::out ) );
					} )
					.step( () -> {
						GLog.i( "FX holy: divine wrath's ground" );
						for (int c : ground) if (c != -1) CellEmitter.get( c ).start( BlobFx.WRATH, 0.3f, 7 );
					} )
					.step( () -> {
						GLog.i( "FX holy: hallowed ground" );
						for (int c : ground) if (c != -1) GameScene.add( Blob.seed( c, 6, HallowedGround.HallowedTerrain.class ) );
					} )
					.step( () -> {
						GLog.i( "FX holy: radiance (a bloom, the soft flash gated)" );
						GameScene.flash( 0x80FFF4D0 );
						HolyFx.radiance( hero.sprite );
					} )
					.step( () -> {
						if (skel == null || skel.sprite == null) return;
						GLog.i( "FX holy: holy water on the undead" );
						int[] cells = { skel.pos, at( hero, 2, 0 ), at( hero, 4, 0 ), at( hero, 3, 1 ), at( hero, 3, -1 ) };
						HolyFx.holyWater( skel.pos, cells, cells.length, new int[]{ skel.pos }, 1 );
					} )
					.step( () -> {
						if (ally == null || ally.sprite == null) return;
						GLog.i( "FX holy: cleanse, a shield of light, a glint over a form" );
						HolyFx.star( ally.sprite, HolyFx.CLEANSE, false, 0 );
						HolyFx.wisps( ally.sprite, 6 );
						HolyFx.bubble( hero.sprite, 0xFFE9A8, 0.3f, 0.6f, 4 );
						HolyFx.glintSweep( hero.sprite );
					} )
					.step( () -> {
						if (wraith == null || wraith.sprite == null) return;
						GLog.i( "FX holy: a spirit's light rising" );
						wraith.sprite.emitter().start( HolyFx.SPIRIT_WISP, 0.3f, 3 );
					} )
					.start();
			GLog.i( "Scene: the holy spells' light, one every 2.5 s, over and over (the log names each)" );
		}
	}

	//runs a thing once, a while from now
	static final class Delay extends Gizmo {
		private float left;
		private final Runnable r;

		Delay( float seconds, Runnable r ){
			left = seconds;
			this.r = r;
		}

		@Override
		public void update(){
			super.update();
			if ((left -= Game.elapsed) <= 0){
				killAndErase();
				r.run();
			}
		}
	}

	// ------------------------------------------------------------------ fx-heal

	static final class Heal implements DebugScenes.Scene {
		@Override
		public String id(){
			return "fx-heal";
		}

		@Override
		public String title(){
			return "Effects: healing (a well of health, a potion, a lifesteal sword, a shielded ally)";
		}

		@Override
		public void apply( final Hero hero ){
			if (!clearing( hero )) return;
			int well = at( hero, 2, -2 );
			set( well, Terrain.WELL );
			GameScene.updateMap();
			if (well != -1) GameScene.add( Blob.seed( well, 1, WaterOfHealth.class ) );
			new PotionOfHealing().identify().collect( hero.belongings.backpack );
			Item sword = new Sword().enchant( new Vampiric() );
			sword.identify().collect( hero.belongings.backpack );
			final Mob ally = dummy( Rat.class, at( hero, -2, 0 ), Char.Alignment.ALLY );
			if (ally != null) Buff.affect( ally, Barrier.class ).setShield( 20 );
			final Mob dry = dummy( Rat.class, at( hero, 0, 2 ), null );
			dummy( Rat.class, at( hero, 1, 2 ), null );
			new Sequence( 3f )
					.step( () -> {
						if (ally != null && ally.sprite != null) ally.sprite.showStatusWithIcon( CharSprite.POSITIVE, "20", FloatingText.SHIELDING );
					} )
					.step( () -> {
						if (dry != null && dry.sprite != null) dry.sprite.showStatusWithIcon( CharSprite.POSITIVE, "12", FloatingText.HEALING );
					} )
					.start();
			GLog.i( "Scene: drink from the well (north-east), quaff the potion, hit the rats with the lifesteal sword;"
					+ " the ally's shield and a heal are shown every 3 s" );
		}
	}
}
