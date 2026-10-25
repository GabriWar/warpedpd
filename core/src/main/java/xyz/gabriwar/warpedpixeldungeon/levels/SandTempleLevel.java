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

package xyz.gabriwar.warpedpixeldungeon.levels;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ChampionEnemy;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ClayGolem;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FossilSkeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Ghoul;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Golem;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Monk;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mummy;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Scorpio;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Snake;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Spinner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Swarm;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.SandTemplePainter;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.AlarmTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.BurningTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.CursingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.FlashingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GrippingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.PitfallTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.PoisonDartTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.RockfallTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.SummoningTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.TeleportationTrap;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A floor of a buried temple's crypt (levels/Delves, a temple Entry): the dungeon's room graph cut
 * small - three to five rooms, a short crypt - in sandstone (tiles_sand_temple.png), sand drifted
 * against its walls and its walls carved (SandTemplePainter), and trapped more than a dungeon floor
 * of its depth: darts, grips, falling stone, fire, blinding flashes, curses, pits and the alarms
 * that wake its dead. Its own dead walk it, by the depth its floor takes: snakes, swarms and the
 * first mummies near the top of the world's strength, bats, skeletons, spiders and clay golems
 * further down, ghouls, temple monks, scorpions and stone golems deepest. The barrows' rules hold
 * (Delves: the scaling, the bans, the way out); its guardian and hoard are the temple's own.
 */
public class SandTempleLevel extends RegularLevel {

	{
		color1 = 0xd6b468;
		color2 = 0xa8844a;
	}

	@Override
	public xyz.gabriwar.warpedpixeldungeon.levels.ambience.Place ambience(){
		return xyz.gabriwar.warpedpixeldungeon.levels.ambience.Place.TEMPLE;
	}

	private static final String[] TRACKS = { Assets.Music.CAVES_1, Assets.Music.CAVES_2, Assets.Music.CAVES_3 };
	private static final float[] TRACK_CHANCES = { 1f, 1f, 0.5f };

	@Override
	public void playLevelMusic(){
		Music.INSTANCE.playTracks( TRACKS, TRACK_CHANCES, false );
	}

	@Override
	protected int standardRooms( boolean forceMax ){
		if (forceMax) return 5;
		//3 to 5, a short crypt
		return 3 + Random.chances( new float[]{ 1, 2, 1 } );
	}

	@Override
	protected int specialRooms( boolean forceMax ){
		if (forceMax) return 2;
		return 1 + Random.Int( 2 );
	}

	@Override
	protected int nTraps(){
		//a temple is trapped: half again what a dungeon floor of its depth holds, and two more
		return 2 + Math.round( super.nTraps() * 1.5f );
	}

	@Override
	protected Painter painter(){
		return new SandTemplePainter()
				.setWater( feeling == Feeling.WATER ? 0.85f : 0.15f, 4 )
				.setGrass( feeling == Feeling.GRASS ? 0.60f : 0.10f, 3 )
				.setTraps( nTraps(), trapClasses(), trapChances() );
	}

	@Override
	protected Class<?>[] trapClasses(){
		return new Class<?>[]{
				PoisonDartTrap.class, GrippingTrap.class, RockfallTrap.class, BurningTrap.class,
				FlashingTrap.class, AlarmTrap.class, CursingTrap.class, SummoningTrap.class,
				PitfallTrap.class, TeleportationTrap.class };
	}

	@Override
	protected float[] trapChances(){
		return new float[]{
				4, 3, 3, 3,
				2, 2, 1, 1,
				1, 1 };
	}

	@Override
	public String tilesTex(){
		return Assets.Environment.TILES_SAND_TEMPLE;
	}

	@Override
	public String waterTex(){
		return Assets.Environment.WATER_TEMPLE;
	}

	/** The temple's own dead for a floor of this depth (the barrows' depths: 1-24 without the bosses'). */
	public static ArrayList<Class<? extends Mob>> rotation( int depth ){
		int region = (Math.max( 1, depth ) - 1) / 5;
		switch (region){
			case 0:
				return new ArrayList<>( Arrays.asList( Snake.class, Snake.class, Snake.class, Mummy.class, Swarm.class ) );
			case 1:
				return new ArrayList<>( Arrays.asList( Mummy.class, Mummy.class, Skeleton.class, Skeleton.class,
						FossilSkeleton.class, Snake.class ) );
			case 2:
				return new ArrayList<>( Arrays.asList( Mummy.class, Mummy.class, Bat.class, Spinner.class, Spinner.class,
						ClayGolem.class ) );
			case 3:
				return new ArrayList<>( Arrays.asList( Mummy.class, Ghoul.class, Monk.class, Monk.class, Scorpio.class,
						Golem.class ) );
			default:
				return new ArrayList<>( Arrays.asList( Mummy.class, Scorpio.class, Scorpio.class, Golem.class, Monk.class,
						Ghoul.class ) );
		}
	}

	private ArrayList<Class<? extends Mob>> toSpawn = new ArrayList<>();

	@Override
	public Mob createMob(){
		if (toSpawn.isEmpty()){
			toSpawn = rotation( Dungeon.depth );
			Random.shuffle( toSpawn );
		}
		Mob m = Reflection.newInstance( toSpawn.remove( 0 ) );
		ChampionEnemy.rollForChampion( m );
		return m;
	}
}
