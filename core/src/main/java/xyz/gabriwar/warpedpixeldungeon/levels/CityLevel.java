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

package xyz.gabriwar.warpedpixeldungeon.levels;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.Flames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.PlaceLight;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AscensionChallenge;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.LostInventory;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GoldThief;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Imp;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ElmoParticle;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClassArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClothArmor;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.Artifact;
import xyz.gabriwar.warpedpixeldungeon.items.quest.EscapeCrystal;
import xyz.gabriwar.warpedpixeldungeon.items.rings.Ring;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.CityPainter;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.Room;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.BlazingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.CorrosionTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.CursingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.DisarmingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.DisintegrationTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.DistortionTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.FlashingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.FrostTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GatewayTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GeyserTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GuardianTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.PitfallTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.RockfallTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.StormTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.SummoningTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.WarpingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.WeakeningTrap;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import xyz.gabriwar.warpedpixeldungeon.sprites.ImpSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class CityLevel extends RegularLevel {

	@Override
	public xyz.gabriwar.warpedpixeldungeon.levels.ambience.Place ambience() {
		return xyz.gabriwar.warpedpixeldungeon.levels.ambience.Place.CITY;
	}


	{
		color1 = 0x4b6636;
		color2 = 0xf2f2f2;
	}

	public static final String[] CITY_TRACK_LIST
			= new String[]{Assets.Music.CITY_1, Assets.Music.CITY_2, Assets.Music.CITY_2,
			Assets.Music.CITY_1, Assets.Music.CITY_3, Assets.Music.CITY_3};
	public static final float[] CITY_TRACK_CHANCES = new float[]{1f, 1f, 0.5f, 0.25f, 1f, 0.5f};

	@Override
	public void playLevelMusic() {
		if (Statistics.amuletObtained){
			Music.INSTANCE.play(Assets.Music.CITY_TENSE, true);
		} else {
			Music.INSTANCE.playTracks(CITY_TRACK_LIST, CITY_TRACK_CHANCES, false);
		}
	}

	@Override
	protected int standardRooms(boolean forceMax) {
		int bonus = ((Dungeon.depth - 1) % 5) * 2;
		if (forceMax) return 10 + bonus;
		//8 to 10, average 9
		return 8+Random.chances(new float[]{1, 3, 1}) + bonus;
	}

	@Override
	protected int specialRooms(boolean forceMax) {
		if (forceMax) return 3;
		//2 to 3, average 2.33
		return 2 + Random.chances(new float[]{2, 1});
	}

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_CITY;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_CITY;
	}

	@Override
	public String frozenWaterTex() {
		return Assets.Environment.FROZEN_CITY;
	}

	@Override
	protected Painter painter() {
		return new CityPainter()
				.setWater(feeling == Feeling.WATER ? 0.90f : 0.30f, 4)
				.setGrass(feeling == Feeling.GRASS ? 0.80f : 0.20f, 3)
				.setTraps(nTraps(), trapClasses(), trapChances());
	}

	@Override
	protected Class<?>[] trapClasses() {
		return new Class[]{
				FrostTrap.class, StormTrap.class, CorrosionTrap.class, BlazingTrap.class, DisintegrationTrap.class,
				RockfallTrap.class, FlashingTrap.class, GuardianTrap.class, WeakeningTrap.class,
				DisarmingTrap.class, SummoningTrap.class, WarpingTrap.class, CursingTrap.class, PitfallTrap.class, DistortionTrap.class, GatewayTrap.class, GeyserTrap.class };
	}

	@Override
	protected float[] trapChances() {
		return new float[]{
				4, 4, 4, 4, 4,
				2, 2, 2, 2,
				1, 1, 1, 1, 1, 1, 1, 1 };
	}

	@Override
	public boolean activateTransition(Hero hero, LevelTransition transition) {
		if (transition.type == LevelTransition.Type.BRANCH_EXIT) {

			if ( Imp.Quest.isOld() || Imp.Quest.isCompleted() || !Imp.Quest.given()
					|| hero.buff(AscensionChallenge.class) != null
					|| hero.buff(LostInventory.class) != null){
				return false;
			}

			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show( new WndOptions( new ImpSprite(),
							Messages.titleCase(Messages.get(Imp.class, "name")),
							Messages.get(Imp.class, "enter_text"),
							Messages.get(Imp.class, "enter_yes"),
							Messages.get(Imp.class, "enter_no")){
						@Override
						protected void onSelect(int index) {
							if (index == 0){

								Dungeon.hero.live(); //clears all non-persist buffs, resets hunger/regen
								hero.HP = hero.HT; //full heal

								EscapeCrystal crystal = hero.belongings.getItem(EscapeCrystal.class);
								if (crystal == null) {
									crystal = new EscapeCrystal();
								} else {
									crystal.detachAll(Dungeon.hero.belongings.backpack);
								}
								if (crystal.storedItems == null){
									crystal.storeHeroBelongings(Dungeon.hero);
								}
								crystal.collect();
								hero.belongings.armor = new ClothArmor();
								hero.belongings.armor.identify();
								hero.updateHT( false );
								CityLevel.super.activateTransition(hero, transition);
							}
						}
					} );
				}
			});
			return false;

		} else {
			return super.activateTransition(hero, transition);
		}
	}

	@Override
	protected void createMobs() {
		//spawn a gold thief alongside the imp quest (from Sprouted, depth 19)
		if (Dungeon.depth == 19 && !Delves.inDelve() && !Dungeon.LimitedDrops.CITY_KEY.dropped()) {
			GoldThief thief = new GoldThief();
			do {
				thief.pos = randomRespawnCell( null );
			} while (thief.pos == -1);
			mobs.add( thief );
		}

		super.createMobs();
	}

	@Override
	protected ArrayList<Room> initRooms() {
		//the quests are the main dungeon's: a barrow cut like its floors does not host them
		return Delves.inDelve() ? super.initRooms() : Imp.Quest.spawn(super.initRooms());
	}
	
	@Override
	public String tileName( int tile ) {
		switch (tile) {
			case Terrain.WATER:
				return Messages.get(CityLevel.class, "water_name");
			case Terrain.HIGH_GRASS:
				return Messages.get(CityLevel.class, "high_grass_name");
			case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT:
				return Messages.get(CityLevel.class, "region_deco_name");
			default:
				return super.tileName( tile );
		}
	}
	
	@Override
	public String tileDesc(int tile) {
		switch (tile) {
			case Terrain.ENTRANCE:
			case Terrain.ENTRANCE_SP:
				return Messages.get(CityLevel.class, "entrance_desc");
			case Terrain.EXIT:
				return Messages.get(CityLevel.class, "exit_desc");
			case Terrain.WALL_DECO:
			case Terrain.EMPTY_DECO:
				return Messages.get(CityLevel.class, "deco_desc");
			case Terrain.STATUE:
			case Terrain.STATUE_SP:
				return Messages.get(CityLevel.class, "statue_desc");
			case Terrain.BOOKSHELF:
				return Messages.get(CityLevel.class, "bookshelf_desc");
			case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT:
				return Messages.get(CityLevel.class, "region_deco_desc");
			default:
				return super.tileDesc( tile );
		}
	}
	
	@Override
	public Group addVisuals() {
		super.addVisuals();
		addCityVisuals( this, visuals );
		return visuals;
	}

	public static void addCityVisuals( Level level, Group group ) {
		for (int i=0; i < level.length(); i++) {
			if (level.map[i] == Terrain.WALL_DECO) {
				group.add( new Smoke( i ) );
			}
		}
	}

	@Override
	public Group addWallVisuals() {
		super.addWallVisuals();
		addCityWallVisuals( this, wallVisuals );
		return wallVisuals;
	}

	public static void addCityWallVisuals( Level level, Group group ) {
		for (int i=0; i < level.length(); i++) {
			if (level.map[i] == Terrain.REGION_DECO || level.map[i] == Terrain.REGION_DECO_ALT) {
				group.add( new GreenFlame( i ) );
			}
		}
	}

	/**
	 * A green flame on the city's walls: Saint Elmo's tongues (70 small to 30 middling) every
	 * 0.1 s and a pale green glow breathing round them (effects/fx/fire/PlaceLight).
	 */
	public static class GreenFlame extends Emitter {

		private static final int LIGHT = 0x70FF98;

		private int pos;
		private final PlaceLight light;

		public static final Emitter.Factory factory = new Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				if (!emitter.visible || !FxBudget.keep( FxBudget.P1, x, y )) return;
				Flames.tongue( Flames.make( Flames.host( emitter, GreenFlameParticle.class ), GreenFlameParticle.class, x, y ),
						Element.ELMO, FxRandom.chance( 0.7f ) ? Flames.S : Flames.M ).source( x, y );
			}
			@Override
			public boolean lightMode() {
				return true;
			}
		};

		public GreenFlame( int pos ) {
			super();

			this.pos = pos;

			PointF p = DungeonTilemap.raisedTileCenterToWorld( pos );
			pos( p.x - 2, p.y - 5, 4, 4 );

			pour( factory, 0.1f );

			light = new PlaceLight( pos, p.x, p.y - 4, 15, 0, 0, 0, LIGHT, 0.35f / FxLight.GLOW_CAP, FxLight.BREATH );
		}

		@Override
		public void update() {
			//its wall broken (a pick, a bomb), its fire goes with it
			if (Dungeon.level.map[pos] != Terrain.REGION_DECO && Dungeon.level.map[pos] != Terrain.REGION_DECO_ALT){
				killAndErase();
				//its images let go of their vertex buffers now, not when the scene changes
				destroy();
				return;
			}

			if (visible = (pos < Dungeon.level.heroFOV.length && Dungeon.level.heroFOV[pos])) {
				super.update();
				light.update();
			}
		}

	}

	/** The green flames' tongues (the kit's flame in Saint Elmo's ramp). */
	public static class GreenFlameParticle extends ElmoParticle {
	}

	/**
	 * A vent in the city's walls breathing out warm grey fumes: a puff every 0.5 s, pale as it
	 * leaves (9A9690) and darkening as it thins (6A6662), never more than 0.35 opaque, rising
	 * straight up for 3 s (a city has no wind), and a dull red glow breathing at its mouth.
	 */
	public static class Smoke extends Emitter {

		private static final int LIGHT = 0xFF7040;
		private static final Ramp FUMES = new Ramp( 0x9A9690, 0x827E78, 0x6A6662 );
		//a puff's sizes, one frame each
		private static final int[][][] PUFF_M = { { FxFrames.PUFF_M[0] }, { FxFrames.PUFF_M[1] } };

		private int pos;
		private final PlaceLight light;

		public static final Emitter.Factory factory = new Factory() {

			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				if (!emitter.visible || !FxBudget.keep( FxBudget.P2, x, y )) return;
				int v = FxRandom.Int( 2 );
				SmokeParticle p = (SmokeParticle)emitter.recycle( SmokeParticle.class );
				p.reset( x, y );
				p.look( FxFrames.PUFF_S[v] ).then( 0.45f, PUFF_M[v] ).ramp( FUMES, false ).smooth().life( 3f )
						.alpha( 0.35f, 0.6f, 1.4f ).vel( FxRandom.Float( -1f, 1f ), -FxRandom.Float( 4f, 7f ) ).drag( 0.1f )
						.turbulence( FxRandom.Float( 1f, 2f ), FxRandom.Float( 1.5f, 3f ) ).priority( FxBudget.P2 );
			}
		};

		public Smoke( int pos ) {
			super();

			this.pos = pos;

			PointF p = DungeonTilemap.tileCenterToWorld( pos );
			pos( p.x - 6, p.y - 4, 12, 12 );

			pour( factory, 0.5f );

			light = new PlaceLight( pos, p.x, p.y + 2, 7, p.x, p.y + 6, 15, LIGHT, 0.2f / FxLight.POOL_CAP, FxLight.BREATH );
		}

		@Override
		public void update() {
			//its wall broken (a pick, a bomb), its smoke goes with it
			if (Dungeon.level.map[pos] != Terrain.WALL_DECO){
				killAndErase();
				//its images let go of their vertex buffers now, not when the scene changes
				destroy();
				return;
			}

			if (visible = (pos < Dungeon.level.heroFOV.length && Dungeon.level.heroFOV[pos])) {
				super.update();
				light.update();
			}
		}
	}

	/** A vent's puff of fumes (the kit's painted puffs). */
	public static final class SmokeParticle extends FxParticle {
	}
}