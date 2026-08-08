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

import com.watabou.utils.Callback;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.frozen.FrozenEntranceRoom;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.items.Torch;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.HallsPainter;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.Room;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.DemonSpawnerRoom;
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
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GrimTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GuardianTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.PitfallTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.RockfallTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.StormTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.SummoningTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.WarpingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.WeakeningTrap;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class HallsLevel extends RegularLevel {

	{
		
		viewDistance = Math.min( 26 - Dungeon.depth, viewDistance );
		
		color1 = 0x801500;
		color2 = 0xa68521;
	}

	public static final String[] HALLS_TRACK_LIST
			= new String[]{Assets.Music.HALLS_1, Assets.Music.HALLS_2, Assets.Music.HALLS_2,
			Assets.Music.HALLS_1, Assets.Music.HALLS_3, Assets.Music.HALLS_3};
	public static final float[] HALLS_TRACK_CHANCES = new float[]{1f, 1f, 0.5f, 0.25f, 1f, 0.5f};


	@Override
	public void playLevelMusic() {
		if (Statistics.amuletObtained){
			Music.INSTANCE.play(Assets.Music.HALLS_TENSE, true);
		} else {
			Music.INSTANCE.playTracks(HALLS_TRACK_LIST, HALLS_TRACK_CHANCES, false);
		}
	}

	//the frozen branch hangs off this Halls floor
	public static final int FROZEN_BRANCH_DEPTH = 22;

	@Override
	protected ArrayList<Room> initRooms() {
		ArrayList<Room> rooms = super.initRooms();

		rooms.add(new DemonSpawnerRoom());

		//mouth of the frozen branch, on a single Halls floor (Unleashed PD port)
		if (Dungeon.depth == FROZEN_BRANCH_DEPTH && Dungeon.branch == 0) {
			rooms.add(new FrozenEntranceRoom());
		}

		return rooms;
	}


	@Override
	protected int standardRooms(boolean forceMax) {
		int bonus = ((Dungeon.depth - 1) % 5) * 2;
		if (forceMax) return 11 + bonus;
		//10 to 11, average 10.33
		return 10+Random.chances(new float[]{2, 1}) + bonus;
	}
	
	@Override
	protected int specialRooms(boolean forceMax) {
		if (forceMax) return 3;
		//2 to 3, average 2.5
		return 2 + Random.chances(new float[]{1, 1});
	}
	
	@Override
	protected Painter painter() {
		return new HallsPainter()
				.setWater(feeling == Feeling.WATER ? 0.70f : 0.15f, 6)
				.setGrass(feeling == Feeling.GRASS ? 0.65f : 0.10f, 3)
				.setTraps(nTraps(), trapClasses(), trapChances());
	}
	
	@Override
	public void create() {
		addItemToSpawn( new Torch() );
		addItemToSpawn( new Torch() );
		super.create();
	}
	
	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_HALLS;
	}
	
	@Override
	public String waterTex() {
		return Assets.Environment.WATER_HALLS;
	}
	
	@Override
	protected Class<?>[] trapClasses() {
		return new Class[]{
				FrostTrap.class, StormTrap.class, CorrosionTrap.class, BlazingTrap.class, DisintegrationTrap.class,
				RockfallTrap.class, FlashingTrap.class, GuardianTrap.class, WeakeningTrap.class,
				DisarmingTrap.class, SummoningTrap.class, WarpingTrap.class, CursingTrap.class, GrimTrap.class, PitfallTrap.class, DistortionTrap.class, GatewayTrap.class, GeyserTrap.class };
	}

	@Override
	protected float[] trapChances() {
		return new float[]{
				4, 4, 4, 4, 4,
				2, 2, 2, 2,
				1, 1, 1, 1, 1, 1, 1, 1, 1 };
	}
	
	@Override
	public String tileName( int tile ) {
		switch (tile) {
			case Terrain.WATER:
				return Messages.get(HallsLevel.class, "water_name");
			case Terrain.GRASS:
				return Messages.get(HallsLevel.class, "grass_name");
			case Terrain.HIGH_GRASS:
				return Messages.get(HallsLevel.class, "high_grass_name");
			case Terrain.STATUE:
			case Terrain.STATUE_SP:
				return Messages.get(HallsLevel.class, "statue_name");
			case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT:
				return Messages.get(HallsLevel.class, "region_deco_name");
			default:
				return super.tileName( tile );
		}
	}
	
	@Override
	public String tileDesc(int tile) {
		switch (tile) {
			case Terrain.WATER:
				return Messages.get(HallsLevel.class, "water_desc");
			case Terrain.STATUE:
			case Terrain.STATUE_SP:
				return Messages.get(HallsLevel.class, "statue_desc");
			case Terrain.BOOKSHELF:
				return Messages.get(HallsLevel.class, "bookshelf_desc");
			case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT:
				return Messages.get(HallsLevel.class, "region_deco_desc");
			default:
				return super.tileDesc( tile );
		}
	}
	
	@Override
	public Group addVisuals() {
		super.addVisuals();
		addHallsVisuals( this, visuals );
		return visuals;
	}
	
	public static void addHallsVisuals( Level level, Group group ) {
		for (int i=0; i < level.length(); i++) {
			if (level.map[i] == Terrain.WATER) {
				group.add( new Stream( i ) );
			}
		}
	}
	
	private static class Stream extends Group {
		
		private int pos;
		
		private float delay;
		
		public Stream( int pos ) {
			super();
			
			this.pos = pos;
			
			delay = Random.Float( 2 );
		}
		
		@Override
		public void update() {

			if (!Dungeon.level.water[pos]){
				killAndErase();
				return;
			}
			
			if (visible = (pos < Dungeon.level.heroFOV.length && Dungeon.level.heroFOV[pos])) {
				
				super.update();
				
				if ((delay -= Game.elapsed) <= 0) {
					
					delay = Random.Float( 2 );
					
					PointF p = DungeonTilemap.tileToWorld( pos );
					((FireParticle)recycle( FireParticle.class )).reset(
						p.x + Random.Float( DungeonTilemap.SIZE ),
						p.y + Random.Float( DungeonTilemap.SIZE ) );
				}
			}
		}
		
		@Override
		public void draw() {
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}
	
	public static class FireParticle extends PixelParticle.Shrinking {
		
		public FireParticle() {
			super();
			
			color( 0xEE7722 );
			lifespan = 1f;
			
			acc.set( 0, +80 );
		}
		
		public void reset( float x, float y ) {
			revive();
			
			this.x = x;
			this.y = y;
			
			left = lifespan;
			
			speed.set( 0, -40 );
			size = 4;
		}
		
		@Override
		public void update() {
			super.update();
			float p = left / lifespan;
			am = p > 0.8f ? (1 - p) * 5 : 1;
		}
	}

	//Optional danger branch: confirm before entering so nobody falls in by accident.
	@Override
	public boolean activateTransition(Hero hero, LevelTransition transition) {
		if (transition.type == LevelTransition.Type.BRANCH_EXIT
				&& transition.destBranch == FrozenEntranceRoom.FROZEN_BRANCH) {
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show( new WndOptions(
							new ItemSprite( ItemSpriteSheet.SHARD ),
							Messages.get(HallsLevel.class, "branch_title"),
							Messages.get(HallsLevel.class, "branch_prompt"),
							Messages.get(HallsLevel.class, "branch_yes"),
							Messages.get(HallsLevel.class, "branch_no")){
						@Override
						protected void onSelect(int index) {
							if (index == 0) {
								HallsLevel.super.activateTransition(hero, transition);
							}
						}
					} );
				}
			});
			return false;
		}
		return super.activateTransition(hero, transition);
	}
}
