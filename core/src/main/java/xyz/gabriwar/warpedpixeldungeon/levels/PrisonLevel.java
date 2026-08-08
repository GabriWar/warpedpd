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

import xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.spider.SpiderNestEntranceRoom;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MossySkeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Wandmaker;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.WindParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.PrisonPainter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.Room;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.AlarmTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.BurningTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.ChillingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.ConfusionTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.FlockTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GatewayTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GeyserTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GrippingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.OozeTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.PoisonDartTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.ShockingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.SummoningTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.TeleportationTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.ToxicTrap;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Halo;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class PrisonLevel extends RegularLevel {

	{
		color1 = 0x6a723d;
		color2 = 0x88924c;
	}

	public static final String[] PRISON_TRACK_LIST
			= new String[]{Assets.Music.PRISON_1, Assets.Music.PRISON_2, Assets.Music.PRISON_2,
			Assets.Music.PRISON_1, Assets.Music.PRISON_3, Assets.Music.PRISON_3};
	public static final float[] PRISON_TRACK_CHANCES = new float[]{1f, 1f, 0.5f, 0.25f, 1f, 0.5f};

	@Override
	public void playLevelMusic() {
		if (Wandmaker.Quest.active() || Statistics.amuletObtained){
			Music.INSTANCE.play(Assets.Music.PRISON_TENSE, true);
		} else {
			Music.INSTANCE.playTracks(PRISON_TRACK_LIST, PRISON_TRACK_CHANCES, false);
		}
		wandmakerQuestWasActive = Wandmaker.Quest.active();
	}

	//the spider nest hangs off this prison floor
	public static final int SPIDER_BRANCH_DEPTH = 6;

	@Override
	protected ArrayList<Room> initRooms() {
		ArrayList<Room> rooms = Wandmaker.Quest.spawnRoom(super.initRooms());

		//web-choked hole down into the spider nest (Remixed PD reimplementation)
		if (Dungeon.depth == SPIDER_BRANCH_DEPTH && Dungeon.branch == 0) {
			rooms.add(new SpiderNestEntranceRoom());
		}

		return rooms;
	}

	@Override
	protected void createMobs() {
		Wandmaker.Quest.spawnWandmaker(this, roomEntrance);

		//spawn a mossy skeleton alongside the wandmaker quest (from Sprouted, depth 9)
		if (Dungeon.depth == 9 && !Dungeon.LimitedDrops.PRISON_KEY.dropped()) {
			MossySkeleton skeleton = new MossySkeleton();
			do {
				skeleton.pos = randomRespawnCell( null );
			} while (skeleton.pos == -1);
			mobs.add( skeleton );
		}

		super.createMobs();
	}

	@Override
	protected int standardRooms(boolean forceMax) {
		int bonus = ((Dungeon.depth - 1) % 5) * 2;
		if (forceMax) return 8 + bonus;
		//7 to 8, average 7.5
		return 7+Random.chances(new float[]{1, 1}) + bonus;
	}
	
	@Override
	protected int specialRooms(boolean forceMax) {
		if (forceMax) return 3;
		//1 to 3, average 2.0
		return 1+Random.chances(new float[]{1, 3, 1});
	}
	
	@Override
	protected Painter painter() {
		return new PrisonPainter()
				.setWater(feeling == Feeling.WATER ? 0.90f : 0.30f, 4)
				.setGrass(feeling == Feeling.GRASS ? 0.80f : 0.20f, 3)
				.setTraps(nTraps(), trapClasses(), trapChances());
	}
	
	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_PRISON;
	}
	
	@Override
	public String waterTex() {
		return Assets.Environment.WATER_PRISON;
	}

	@Override
	public String frozenWaterTex() {
		return Assets.Environment.FROZEN_PRISON;
	}

	@Override
	protected Class<?>[] trapClasses() {
		return new Class[]{
				ChillingTrap.class, ShockingTrap.class, ToxicTrap.class, BurningTrap.class, PoisonDartTrap.class,
				AlarmTrap.class, OozeTrap.class, GrippingTrap.class,
				ConfusionTrap.class, FlockTrap.class, SummoningTrap.class, TeleportationTrap.class, GatewayTrap.class, GeyserTrap.class };
	}

	@Override
	protected float[] trapChances() {
		return new float[]{
				4, 4, 4, 4, 4,
				2, 2, 2,
				1, 1, 1, 1, 1, 1 };
	}

	@Override
	public void occupyCell(Char ch) {
		super.occupyCell(ch);
		if (ch == Dungeon.hero) {
			updateWandmakerQuestMusic();
		}
	}

	private Boolean wandmakerQuestWasActive = null;

	public void updateWandmakerQuestMusic(){
		if (wandmakerQuestWasActive == null) {
			wandmakerQuestWasActive = Wandmaker.Quest.active();
			return;
		}
		if (Wandmaker.Quest.active() != wandmakerQuestWasActive) {
			wandmakerQuestWasActive = Wandmaker.Quest.active();

			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					Music.INSTANCE.fadeOut(1f, new Callback() {
						@Override
						public void call() {
							if (Dungeon.level != null) {
								Dungeon.level.playLevelMusic();
							}
						}
					});
				}
			});
		}
	}

	@Override
	public String tileName( int tile ) {
		switch (tile) {
			case Terrain.WATER:
				return Messages.get(PrisonLevel.class, "water_name");
			case Terrain.REGION_DECO:
				return Messages.get(PrisonLevel.class, "region_deco_name");
			case Terrain.REGION_DECO_ALT:
				return Messages.get(PrisonLevel.class, "region_deco_alt_name");
			default:
				return super.tileName( tile );
		}
	}

	@Override
	public String tileDesc(int tile) {
		switch (tile) {
			case Terrain.EMPTY_DECO:
				return Messages.get(PrisonLevel.class, "empty_deco_desc");
			case Terrain.BOOKSHELF:
				return Messages.get(PrisonLevel.class, "bookshelf_desc");
			case Terrain.REGION_DECO:
				return Messages.get(PrisonLevel.class, "region_deco_desc");
			case Terrain.REGION_DECO_ALT:
				return Messages.get(PrisonLevel.class, "region_deco_alt_desc");
			default:
				return super.tileDesc( tile );
		}
	}
	
	@Override
	public Group addVisuals() {
		super.addVisuals();
		addPrisonVisuals(this, visuals);
		return visuals;
	}

	public static void addPrisonVisuals(Level level, Group group){
		for (int i=0; i < level.length(); i++) {
			if (level.map[i] == Terrain.WALL_DECO) {
				group.add( new Torch( i ) );
			}
			//alt deco is a chasm visual in the prison
			if (level.map[i] == Terrain.REGION_DECO_ALT) {
				group.add( new WindParticle.Wind( i ) );
			}
		}
	}
	
	public static class Torch extends Emitter {
		
		private int pos;
		
		public Torch( int pos ) {
			super();
			
			this.pos = pos;
			
			PointF p = DungeonTilemap.tileCenterToWorld( pos );
			pos( p.x - 1, p.y + 2, 2, 0 );
			
			pour( FlameParticle.FACTORY, 0.15f );
			
			add( new Halo( 12, 0xFFFFCC, 0.4f ).point( p.x, p.y + 1 ) );
		}
		
		@Override
		public void update() {
			if (visible = (pos < Dungeon.level.heroFOV.length && Dungeon.level.heroFOV[pos])) {
				super.update();
			}
		}
	}
}