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

import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.temple.TempleEntranceRoom;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.items.Mushroom;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Piranha;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith;
import xyz.gabriwar.warpedpixeldungeon.items.quest.Pickaxe;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.CavesPainter;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.Room;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.BurningTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.ConfusionTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.CorrosionTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.FrostTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GatewayTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GeyserTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GrippingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GuardianTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.PitfallTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.PoisonDartTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.RockfallTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.StormTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.SummoningTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.WarpingTrap;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.BlacksmithSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTitledMessage;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class CavesLevel extends RegularLevel {

	{
		color1 = 0x534f3e;
		color2 = 0xb9d661;
	}

	public static final String[] CAVES_TRACK_LIST
			= new String[]{Assets.Music.CAVES_1, Assets.Music.CAVES_2, Assets.Music.CAVES_2,
			Assets.Music.CAVES_1, Assets.Music.CAVES_3, Assets.Music.CAVES_3};
	public static final float[] CAVES_TRACK_CHANCES = new float[]{1f, 1f, 0.5f, 0.25f, 1f, 0.5f};

	@Override
	public void playLevelMusic() {
		if (Statistics.amuletObtained){
			Music.INSTANCE.play(Assets.Music.CAVES_TENSE, true);
		} else {
			Music.INSTANCE.playTracks(CAVES_TRACK_LIST, CAVES_TRACK_CHANCES, false);
		}
	}

	@Override
	protected void createItems() {
		//third mushroom source (Sprouted): one per run in the caves, so all three
		//Tinkerer quests (Sewer/Town/Fortress) can actually be completed.
		if (Dungeon.depth == 11) {
			addItemToSpawn(new Mushroom());
		}
		super.createItems();
	}

	@Override
	protected void createMobs() {
		//spawn a piranha alongside the blacksmith quest (guarantees CavesKey access)
		if (Dungeon.depth == 14 && !Dungeon.LimitedDrops.CAVES_KEY.dropped()) {
			Piranha piranha = new Piranha();
			do {
				piranha.pos = randomRespawnCell( null );
			} while (piranha.pos == -1);
			mobs.add( piranha );
		}

		super.createMobs();
	}

	@Override
	protected ArrayList<Room> initRooms() {
		ArrayList<Room> rooms = Blacksmith.Quest.spawn(super.initRooms());

		//mouth of the Temple gauntlet, on the last caves floor (Re-ARranged port)
		if (Dungeon.depth == TEMPLE_BRANCH_DEPTH && Dungeon.branch == 0) {
			rooms.add(new TempleEntranceRoom());
		}

		return rooms;
	}

	//the Temple branch hangs off this caves floor
	public static final int TEMPLE_BRANCH_DEPTH = 14;

	@Override
	protected int standardRooms(boolean forceMax) {
		int bonus = ((Dungeon.depth - 1) % 5) * 2;
		if (forceMax) return 9 + bonus;
		//8 to 9, average 8.333
		return 8+Random.chances(new float[]{2, 1}) + bonus;
	}
	
	@Override
	protected int specialRooms(boolean forceMax) {
		if (forceMax) return 3;
		//2 to 3, average 2.2
		return 2+Random.chances(new float[]{4, 1});
	}
	
	@Override
	protected Painter painter() {
		return new CavesPainter()
				.setWater(feeling == Feeling.WATER ? 0.85f : 0.30f, 6)
				.setGrass(feeling == Feeling.GRASS ? 0.65f : 0.15f, 3)
				.setTraps(nTraps(), trapClasses(), trapChances());
	}
	
	@Override
	public boolean activateTransition(Hero hero, LevelTransition transition) {
		//Temple gauntlet: confirm before entering so nobody falls in by accident
		if (transition.type == LevelTransition.Type.BRANCH_EXIT
				&& transition.destBranch == TempleEntranceRoom.TEMPLE_BRANCH) {
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show( new WndOptions(
							new ItemSprite( ItemSpriteSheet.ANKH ),
							Messages.get(CavesLevel.class, "temple_title"),
							Messages.get(CavesLevel.class, "temple_prompt"),
							Messages.get(CavesLevel.class, "temple_yes"),
							Messages.get(CavesLevel.class, "temple_no")){
						@Override
						protected void onSelect(int index) {
							if (index == 0) {
								CavesLevel.super.activateTransition(hero, transition);
							}
						}
					} );
				}
			});
			return false;
		}

		if (transition.type == LevelTransition.Type.BRANCH_EXIT
				&& (!Blacksmith.Quest.given(hero) || Blacksmith.Quest.completedBy(hero) || !Blacksmith.Quest.started())) {

			Blacksmith smith = null;
			for (Char c : Actor.chars()){
				if (c instanceof Blacksmith){
					smith = (Blacksmith) c;
					break;
				}
			}

			if (smith == null || !Blacksmith.Quest.given(hero) || Blacksmith.Quest.completedBy(hero)) {
				GLog.w(Messages.get(Blacksmith.class, "entrance_blocked"));
			} else {
				final Pickaxe pick = hero.belongings.getItem(Pickaxe.class);
				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						if (pick == null){
							GameScene.show( new WndTitledMessage(new BlacksmithSprite(),
									Messages.titleCase(Messages.get(Blacksmith.class, "name")),
									Messages.get(Blacksmith.class, "lost_pick"))
							);
						} else {
							GameScene.show( new WndOptions( new BlacksmithSprite(),
									Messages.titleCase(Messages.get(Blacksmith.class, "name")),
									Messages.get(Blacksmith.class, "quest_start_prompt"),
									Messages.get(Blacksmith.class, "enter_yes"),
									Messages.get(Blacksmith.class, "enter_no")){
								@Override
								protected void onSelect(int index) {
									if (index == 0){
										Blacksmith.Quest.start();
										CavesLevel.super.activateTransition(hero, transition);
									}
								}
							} );
						}

					}
				});
			}
			return false;

		} else {
			return super.activateTransition(hero, transition);
		}
	}

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_CAVES;
	}
	
	@Override
	public String waterTex() {
		return Assets.Environment.WATER_CAVES;
	}

	@Override
	public String frozenWaterTex() {
		return Assets.Environment.FROZEN_CAVES;
	}

	@Override
	protected Class<?>[] trapClasses() {
		return new Class[]{
				BurningTrap.class, PoisonDartTrap.class, FrostTrap.class, StormTrap.class, CorrosionTrap.class,
				GrippingTrap.class, RockfallTrap.class,  GuardianTrap.class,
				ConfusionTrap.class, SummoningTrap.class, WarpingTrap.class, PitfallTrap.class, GatewayTrap.class, GeyserTrap.class };
	}

	@Override
	protected float[] trapChances() {
		return new float[]{
				4, 4, 4, 4, 4,
				2, 2, 2,
				1, 1, 1, 1, 1, 1 };
	}
	
	@Override
	public String tileName( int tile ) {
		switch (tile) {
			case Terrain.GRASS:
				return Messages.get(CavesLevel.class, "grass_name");
			case Terrain.HIGH_GRASS:
				return Messages.get(CavesLevel.class, "high_grass_name");
			case Terrain.WATER:
				return Messages.get(CavesLevel.class, "water_name");
			case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT:
				return Messages.get(CavesLevel.class, "region_deco_name");
			default:
				return super.tileName( tile );
		}
	}
	
	@Override
	public String tileDesc( int tile ) {
		switch (tile) {
			case Terrain.ENTRANCE:
			case Terrain.ENTRANCE_SP:
				return Messages.get(CavesLevel.class, "entrance_desc");
			case Terrain.EXIT:
				return Messages.get(CavesLevel.class, "exit_desc");
			case Terrain.HIGH_GRASS:
				return Messages.get(CavesLevel.class, "high_grass_desc");
			case Terrain.WALL_DECO:
				return Messages.get(CavesLevel.class, "wall_deco_desc");
			case Terrain.BOOKSHELF:
				return Messages.get(CavesLevel.class, "bookshelf_desc");
			case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT:
				return Messages.get(CavesLevel.class, "region_deco_desc");
			default:
				return super.tileDesc( tile );
		}
	}
	
	@Override
	public Group addVisuals() {
		super.addVisuals();
		addCavesVisuals( this, visuals );
		return visuals;
	}

	public static void addCavesVisuals( Level level, Group group ) {
		addCavesVisuals(level, group, false);
	}
	
	public static void addCavesVisuals( Level level, Group group, boolean overHang ) {
		for (int i=0; i < level.length(); i++) {
			if (level.map[i] == Terrain.WALL_DECO) {
				group.add( new Vein( i, overHang ) );
			}
		}
	}
	
	private static class Vein extends Group {
		
		private int pos;

		private boolean includeOverhang;
		
		private float delay;

		public Vein( int pos ) {
			this(pos, false);
		}

		public Vein( int pos, boolean includeOverhang ) {
			super();
			
			this.pos = pos;
			this.includeOverhang = includeOverhang;
			
			delay = Random.Float( 2 );
		}
		
		@Override
		public void update() {
			
			if (visible = (pos < Dungeon.level.heroFOV.length && Dungeon.level.heroFOV[pos])) {
				
				super.update();

				if ((delay -= Game.elapsed) <= 0) {

					//pickaxe can remove the ore, should remove the sparkling too.
					if (Dungeon.level.map[pos] != Terrain.WALL_DECO){
						kill();
						return;
					}
					
					delay = Random.Float();

					PointF p = DungeonTilemap.tileToWorld( pos );
					if (includeOverhang && !DungeonTileSheet.wallStitcheable(Dungeon.level.map[pos-Dungeon.level.width()])){
						//also sparkles in the bottom 1/2 of the upper tile. Increases particle frequency by 50% accordingly.
						delay *= 0.67f;
						p.y -= DungeonTilemap.SIZE/2f;
						((Sparkle)recycle( Sparkle.class )).reset(
								p.x + Random.Float( DungeonTilemap.SIZE ),
								p.y + Random.Float( DungeonTilemap.SIZE*1.5f ) );
					} else {
						((Sparkle)recycle( Sparkle.class )).reset(
								p.x + Random.Float( DungeonTilemap.SIZE ),
								p.y + Random.Float( DungeonTilemap.SIZE ) );
					}
				}
			}
		}
	}
	
	public static final class Sparkle extends PixelParticle {
		
		public void reset( float x, float y ) {
			revive();
			
			this.x = x;
			this.y = y;
			
			left = lifespan = 0.5f;
		}
		
		@Override
		public void update() {
			super.update();
			
			float p = left / lifespan;
			size( (am = p < 0.5f ? p * 2 : (1 - p) * 2) * 2 );
		}
	}
}