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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.StormStrikes;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Electricity;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DM100;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gnoll;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Pylon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Fairy;
import xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.ThunderBolt;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxModule;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning.Afterglow;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning.Crackle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning.ElectricityFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning.StrikeLights;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfLightning;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.QuickSlot;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.PointF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Lightning (the lightning area: docs/fx/lightning.md): every bolt, arc and spark of electricity,
 * the storm's and the game's, how far they flash, and how the world answers them.
 *
 * Its gallery page sets a creature's zaps (minor: no flash) beside a wand's (major: its leader,
 * flash, scorch and afterglow), the chain over the pool, a storm's bolt, a thunderbolt, an
 * electrified pond, a shocking weapon's proc and the four sparks; its scene, lightning-minor-major,
 * a room with DM-100s, a pet fairy, an active pylon, a pool and a wand of lightning +3 in hand.
 */
public class LightningFx implements FxModule {

	@Override
	public String key(){
		return "lightning";
	}

	@Override
	public String title(){
		return "Lightning";
	}

	@Override
	public void sceneCreated( GameScene scene ){
		Lightning.sceneCreated();
		Crackle.reset();
		Afterglow.reset();
		StrikeLights.reset();
		ElectricityFx.sceneCreated();
	}

	//a lightning drawn on the stage between two points, as a wand's would be
	private static void zap( FxStage.Puppet p, PointF to, boolean major ){
		Lightning l = Lightning.visual( Arrays.asList( Lightning.Arc.visual( p.sprite.center(), to ) ), null );
		p.sprite.parent.addToFront( major ? l.major() : l );
	}

	@Override
	public void exhibits( FxGallery.Page page ){
		page.add( "a DM-100's zaps (no flash), then a wand's (leader, flash, scorch)", 3.6f, s -> {
			FxStage.Puppet dm = s.puppet( DM100.class, s.cell( -1, 1 ) );
			for (int i = 0; i < 3; i++){
				s.after( 0.2f + 0.65f * i, () -> dm.zap( s.dryRat.pos, () -> {
					if (s.dryRat.sprite != null) zap( dm, s.dryRat.sprite.center(), false );
				} ) );
			}
			FxStage.Puppet mage = s.puppet( Gnoll.class, s.cell( -4, -1 ) );
			s.after( 2.3f, () -> mage.zap( s.stone()[0], () -> zap( mage, FxStage.center( s.stone()[3] ), true ) ) );
		} );
		page.add( "a wand of lightning into the pool and on (washes, rings, fish)", 2.8f, s -> {
			FxStage.Puppet p = s.puppet( Gnoll.class, s.cell( 0, 0 ) );
			p.zap( s.wetRat.pos, () -> {
				if (s.wetRat.sprite == null || s.bat.sprite == null) return;
				ArrayList<Lightning.Arc> arcs = new ArrayList<>();
				arcs.add( Lightning.Arc.visual( p.sprite.center(), s.wetRat.sprite.center() ) );
				arcs.add( Lightning.Arc.visual( s.wetRat.sprite.center(), s.bat.sprite.center() ) );
				p.sprite.parent.addToFront( Lightning.visual( arcs, null ).major() );
			} );
		} );
		page.add( "its charge running over the water", 2.8f, s -> {
			FxStage.Puppet p = s.puppet( Gnoll.class, s.cell( 1, -1 ) );
			p.zap( s.poolMiddle(), () -> {
				ArrayList<Lightning.Arc> arcs = new ArrayList<>();
				PointF at = FxStage.center( s.poolMiddle() );
				arcs.add( Lightning.Arc.visual( p.sprite.center(), at ) );
				int[] run = { s.cell( 5, 0 ), s.cell( 6, 1 ), s.cell( 3, 1 ), s.cell( 2, 0 ) };
				PointF from = at;
				for (int i = 0; i < run.length; i++){
					PointF to = FxStage.center( run[i] );
					arcs.add( Lightning.Arc.visual( from, to ).crawl( i + 1 ) );
					from = to;
				}
				p.sprite.parent.addToFront( Lightning.visual( arcs, null ).major() );
			} );
		} );
		page.add( "a storm's bolt (leader, re-strike, pool, scorch)", 3f, s -> StormStrikes.show( Dungeon.level, s.cell( 5, 1 ), false ) );
		page.add( "a storm's bolt into the pool", 3f, s -> StormStrikes.show( Dungeon.level, s.cell( 3, 0 ), false ) );
		page.add( "a thunderbolt on the skeleton", 2.8f, s -> ThunderBolt.thunderEffect( s.skeleton.sprite ) );
		page.add( "an electrified pond", 4f, s -> s.seed( Electricity.class, 8, s.pool() ) );
		page.add( "a shocking weapon's proc on the rat", 2.5f, s -> {
			FxStage.Puppet p = s.puppet( Gnoll.class, s.cell( -3, 2 ) );
			p.attack( s.dryRat.pos, () -> {
				if (s.dryRat.sprite == null || s.skeleton.sprite == null) return;
				s.dryRat.sprite.centerEmitter().burst( SparkParticle.FACTORY, 3 );
				p.sprite.parent.addToFront( Lightning.visual( Arrays.asList(
						Lightning.Arc.visual( s.dryRat.sprite.center(), s.skeleton.sprite.center() ) ), null ) );
				Crackle.on( s.dryRat.sprite );
				Crackle.on( s.skeleton.sprite );
				Crackle.weapon( p.sprite );
			} );
		} );
		page.add( "sparks: struck, static, electric, metal", 2.5f, s -> {
			s.burstCenter( s.cell( -3, -1 ), SparkParticle.FACTORY, 8 );
			s.burstCenter( s.cell( -1, -1 ), SparkParticle.STATIC, 8 );
			s.burstCenter( s.cell( 1, -1 ), SparkParticle.ELECTRIC, 8 );
			s.burstCenter( s.cell( 3, -1 ), SparkParticle.METAL, 8 );
		} );
	}

	@Override
	public void scenes( List<DebugScenes.Scene> out ){
		out.add( new MinorMajor() );
	}

	/**
	 * lightning-minor-major: around the hero (on a dungeon floor) a patch of floor with a pool
	 * east of him, two DM-100s to the west (their zaps: minor, no flash), an active pylon to the
	 * north (minor), a pet fairy if he has no pet (minor), and a wand of lightning +3 in hand,
	 * charged and quickslotted (major: leader, flash, scorch, afterglow).
	 */
	static final class MinorMajor implements DebugScenes.Scene {
		static final int HALF_W = 6, HALF_H = 4;

		@Override public String id(){ return "lightning-minor-major"; }
		@Override public String title(){ return "Lightning: minor zaps and major strikes"; }

		@Override
		public void apply( Hero hero ){
			Level level = Dungeon.level;
			if (level instanceof OverworldLevel){
				GLog.w( "Scene: run it on a dungeon floor (blank-room first)" );
				return;
			}
			for (int dy = -HALF_H; dy <= HALF_H; dy++){
				for (int dx = -HALF_W; dx <= HALF_W; dx++){
					int c = off( hero, dx, dy );
					if (c == -1 || c == hero.pos || Actor.findChar( c ) != null) continue;
					boolean pool = dx >= 2 && dx <= 5 && Math.abs( dy ) <= 1;
					Level.set( c, pool ? Terrain.WATER : Terrain.EMPTY );
					level.visited[c] = true;
				}
			}
			GameScene.updateMap();
			level.cleanWalls();

			spawn( DM100.class, off( hero, -4, -2 ) );
			spawn( DM100.class, off( hero, -4, 2 ) );
			Mob pylon = spawn( Pylon.class, off( hero, 0, -3 ) );
			if (pylon instanceof Pylon) ((Pylon) pylon).activate();
			int fairyAt = off( hero, -1, 1 );
			if (!hero.haspet && fairyAt != -1 && Actor.findChar( fairyAt ) == null){
				Fairy f = new Fairy();
				f.spawn( 1 );
				f.HP = f.HT;
				f.pos = fairyAt;
				f.state = f.HUNTING;
				GameScene.add( f );
				hero.haspet = true;
				f.syncToHero();
			}

			WandOfLightning wand = null;
			for (WandOfLightning w : hero.belongings.getAllItems( WandOfLightning.class )){
				if (w.level() >= 3) wand = w;
			}
			if (wand == null){
				wand = new WandOfLightning();
				wand.upgrade( 3 );
				wand.identify();
				wand.collect( hero.belongings.backpack );
			}
			wand.curCharges = wand.maxCharges;
			if (!Dungeon.quickslot.contains( wand )){
				for (int s = 0; s < QuickSlot.SIZE; s++){
					if (Dungeon.quickslot.getItem( s ) == null){
						Dungeon.quickslot.setSlot( s, wand );
						break;
					}
				}
			}
			Item.updateQuickslot();
			GLog.i( "Scene: the DM-100s, the pylon and the fairy zap: minor, a crack and a small pool of light, never"
					+ " a flash. Zap a DM-100 with the wand (+3): a major strike, its leader, the flash (2 s apart at"
					+ " most), a scorch and an afterglow. Zap into the pool: the water lights up, rings and the charge"
					+ " crawls over it, then the electrified water flickers." );
		}

		private static int off( Hero hero, int dx, int dy ){
			int w = Dungeon.level.width(), x = hero.pos % w + dx, y = hero.pos / w + dy;
			if (x <= 0 || y <= 0 || x >= w - 1 || y >= Dungeon.level.height() - 1) return -1;
			return x + y * w;
		}

		private static Mob spawn( Class<? extends Mob> kind, int cell ){
			if (cell == -1 || !Dungeon.level.passable[cell] || Actor.findChar( cell ) != null) return null;
			Mob mob = com.watabou.utils.Reflection.newInstance( kind );
			if (mob == null) return null;
			mob.pos = cell;
			mob.state = mob.WANDERING;
			GameScene.add( mob );
			return mob;
		}
	}
}
