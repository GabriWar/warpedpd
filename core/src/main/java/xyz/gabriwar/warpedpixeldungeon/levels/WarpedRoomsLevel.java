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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.items.Egg;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfFrost;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLiquidFlame;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfToxicGas;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfWater;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.MasterworkCore;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.TypeShifter;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfFireblast;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfFrost;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.Room;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.EmptyRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.ThermalSpringRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.WayfarersCampRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.BellowsRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.BlackMarketRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.BreakersBenchRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.CounterweightVaultRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.ElementalLockRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.FrozenCacheRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.HothouseRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.IncubatorNestRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.PedigreeHallRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.RivalGalleryRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.SquirrelsHoardRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.TemperingForgeRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.WarpedRooms;
import xyz.gabriwar.warpedpixeldungeon.plants.Firebloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Icecap;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import xyz.gabriwar.warpedpixeldungeon.plants.Waterweed;
import xyz.gabriwar.warpedpixeldungeon.tiles.DevSign;

import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * DEV TOOL: the floor behind the "Warped rooms" debug scenes (debug/DebugScenes). Every
 * room Warped adds, painted once each on an open plaza and left live - nothing on it is
 * paralysed, every mechanism runs - with the rooms that read the calendar painted once
 * per season side by side, and a row of supplies by the entrance: the seeds, potions and
 * wands that answer the rooms' physics, cores and shifters, an egg, and spare gear in
 * threes of each rarity for the trade-up contract, the bench and the pressure plates.
 *
 * It sits at depth 86 only so the scenes have somewhere to send the hero. To the climate
 * it is a shallow sewer floor (climateDepth), so the seasons reach it and its water can
 * freeze; the rooms' own creatures scale with the deepest real floor (WarpedRooms.threat).
 * Doors alternate between the left and the top wall, so both orientations of every
 * door-relative layout (RoomFrame) get looked at.
 */
public class WarpedRoomsLevel extends Level {

	{
		color1 = 0x48763c;
		color2 = 0x59994a;
	}

	/** set by the debug scene before it travels: paint the twelve Warped rooms and nothing
	 *  else - no standard rooms, and one of each of the two that read the calendar rather
	 *  than one per season. The supplies stay, since the rooms are unusable without them */
	public static boolean warpedOnly = false;

	private static final int MAXW = 84;
	//the supplies' two rows, and where the first room row starts under them
	private static final int SUPPLY_X = 6, SUPPLY_Y = 2, ROOMS_Y = 9;

	@Override
	public String tilesTex(){
		return Assets.Environment.TILES_SEWERS;
	}

	@Override
	public String waterTex(){
		return Assets.Environment.WATER_SEWERS;
	}

	@Override
	public String frozenWaterTex(){
		return Assets.Environment.FROZEN_SEWERS;
	}

	//a sewer floor's climate: nearly all of the surface's season reaches it
	@Override
	public int climateDepth(){
		return 3;
	}

	//making a floor charges the run: Level.create spends the drop budgets a depth is
	//"due" (strength potions, upgrade scrolls - and at depth 86 every one of them looks
	//overdue), and the rooms draw their prizes from the run's item decks, artifacts
	//included. A dev floor that is rebuilt on every tap must not eat the real run's loot:
	//both are put back exactly as they were
	@Override
	public void create(){
		Dungeon.LimitedDrops[] drops = Dungeon.LimitedDrops.values();
		int[] counts = new int[drops.length];
		for (int i = 0; i < drops.length; i++) counts[i] = drops[i].count;
		Bundle decks = new Bundle();
		Generator.storeInBundle( decks );

		super.create();

		Generator.restoreFromBundle( decks );
		for (int i = 0; i < drops.length; i++) drops[i].count = counts[i];
	}

	@Override
	protected boolean build(){
		ArrayList<Room> rooms = new ArrayList<>();
		ArrayList<String> labels = new ArrayList<>();

		add( rooms, labels, new BlackMarketRoom(),        "Black Market (dealer is there only at NIGHT)" );
		add( rooms, labels, new RivalGalleryRoom(),       "Rival's Gallery (reads your equipped weapon)" );
		add( rooms, labels, new TemperingForgeRoom(),     "Tempering Forge" );
		add( rooms, labels, new BreakersBenchRoom(),      "Breaker's Bench" );
		add( rooms, labels, new PedigreeHallRoom(),       "Pedigree Hall (reads your weapon/armor)" );
		add( rooms, labels, new ElementalLockRoom(),      "Elemental Lock Vault" );
		add( rooms, labels, new CounterweightVaultRoom(), "Counterweight Vault" );
		add( rooms, labels, new BellowsRoom(),            "The Bellows" );
		add( rooms, labels, new HothouseRoom(),           "Hothouse" );
		add( rooms, labels, new IncubatorNestRoom(),      "Incubator Nest" );
		if (!warpedOnly){
			add( rooms, labels, new ThermalSpringRoom(),  "Thermal Spring (standard room)" );
			add( rooms, labels, new WayfarersCampRoom(),  "Wayfarer's Camp (standard room)" );
		}

		add( rooms, labels, new FrozenCacheRoom(),        "Frozen Cache (today's season)" );
		if (!warpedOnly){
			for (GameCalendar.Season season : new GameCalendar.Season[]{
					GameCalendar.Season.WINTER, GameCalendar.Season.SUMMER }){
				FrozenCacheRoom cache = new FrozenCacheRoom();
				cache.seasonOverride = season;
				add( rooms, labels, cache, "Frozen Cache (" + season.name() + ")" );
			}
		}
		if (warpedOnly){
			add( rooms, labels, new SquirrelsHoardRoom(), "Squirrel's Hoard (today's season)" );
		} else {
			for (GameCalendar.Season season : GameCalendar.Season.values()){
				SquirrelsHoardRoom hoard = new SquirrelsHoardRoom();
				hoard.seasonOverride = season;
				add( rooms, labels, hoard, "Squirrel's Hoard (" + season.name() + ")" );
			}
		}

		//flow layout across the plaza, under the supplies
		int cx = 5, cy = ROOMS_Y, rowH = 0;
		for (Room r : rooms){
			int w = r.width(), h = r.height();
			if (cx + w + 5 > MAXW){
				cx = 5;
				cy += rowH + 6;
				rowH = 0;
			}
			r.shift( cx - r.left, cy - r.top );
			cx += w + 5;
			rowH = Math.max( rowH, h );
		}

		int H = cy + rowH + 6;
		setSize( MAXW, H );
		Painter.fill( this, 0, 0, MAXW, H, Terrain.EMPTY );
		Painter.fill( this, 0, 0, MAXW, 1, Terrain.WALL );
		Painter.fill( this, 0, H - 1, MAXW, 1, Terrain.WALL );
		Painter.fill( this, 0, 0, 1, H, Terrain.WALL );
		Painter.fill( this, MAXW - 1, 0, 1, H, Terrain.WALL );

		int entrance = 2 + 2 * width();
		Painter.set( this, entrance, Terrain.ENTRANCE );
		transitions.add( new LevelTransition( this, entrance, LevelTransition.Type.REGULAR_ENTRANCE ) );

		for (int i = 0; i < rooms.size(); i++){
			Room r = rooms.get( i );

			//the door: left wall on even rooms, top wall on odd ones, always off the
			//centre line (SentryRoom-style painters reroll a centre that equals the door)
			boolean leftDoor = i % 2 == 0;
			Room.Door door = leftDoor
					? new Room.Door( r.left, Math.min( r.top + 2, r.bottom - 1 ) )
					: new Room.Door( Math.min( r.left + 2, r.right - 1 ), r.top );
			EmptyRoom dummy = new EmptyRoom();
			if (leftDoor) dummy.set( r.left - 3, door.y - 1, r.left - 1, door.y + 1 );
			else          dummy.set( door.x - 1, r.top - 3, door.x + 1, r.top - 1 );
			r.connected.put( dummy, door );

			boolean failed = false;
			try {
				System.out.println( "[WarpedRooms] painting " + labels.get( i ) );
				r.paint( this );
			} catch (Throwable t){
				failed = true;
				t.printStackTrace();
			}

			//a plain open door whatever the room asked for: hidden and locked ones too
			Painter.set( this, door.x, door.y, Terrain.DOOR );

			int signX = leftDoor ? r.left - 1 : door.x - 1;
			int signY = leftDoor ? door.y - 1 : r.top - 1;
			map[signX + signY * width()] = Terrain.EMPTY;
			sign( signX, signY, labels.get( i ) + (failed ? " (PAINT FAILED)" : "") );
		}

		supplies();

		buildFlagMaps();
		cleanWalls();

		java.util.Arrays.fill( mapped, true );
		java.util.Arrays.fill( visited, true );
		return true;
	}

	private void add( ArrayList<Room> rooms, ArrayList<String> labels, Room room, String label ){
		room.setSize();
		rooms.add( room );
		labels.add( label );
	}

	private void sign( int x, int y, String label ){
		DevSign sign = new DevSign();
		sign.label = label;
		sign.pos( x, y );
		customTiles.add( sign );
	}

	// ---------------------------------------------------------------- supplies

	private int supplyIndex = 0;

	//one kind of thing per cell, along two rows by the entrance
	private void lay( Item item ){
		int col = supplyIndex / 2, row = supplyIndex % 2;
		supplyIndex++;
		int x = SUPPLY_X + col * 2, y = SUPPLY_Y + row * 2;
		if (x >= MAXW - 2) return;
		drop( item, x + y * width() );
	}

	private void supplies(){
		supplyIndex = 0;
		sign( SUPPLY_X - 2, SUPPLY_Y, "Supplies: seeds, potions and wands for the rooms' physics; cores, shifters, "
				+ "an egg; spare gear in threes per rarity for the trade-up contract, the bench and the plates" );

		lay( new Firebloom.Seed().quantity( 8 ) );
		lay( new Icecap.Seed().quantity( 8 ) );
		lay( new Waterweed.Seed().quantity( 5 ) );
		lay( new Sungrass.Seed().quantity( 3 ) );
		lay( Generator.random( Generator.Category.SEED ) );
		lay( Generator.random( Generator.Category.SEED ) );

		lay( new PotionOfLiquidFlame().quantity( 5 ).identify() );
		lay( new PotionOfFrost().quantity( 5 ).identify() );
		lay( new PotionOfToxicGas().quantity( 3 ).identify() );
		lay( new PotionOfWater().quantity( 3 ).identify() );

		lay( new WandOfFireblast().upgrade( 2 ).identify() );
		lay( new WandOfFrost().upgrade( 2 ).identify() );

		lay( new MasterworkCore().quantity( 12 ) );
		lay( new TypeShifter().quantity( 3 ) );
		lay( new Egg() );
		lay( new Food().quantity( 3 ) );

		//spare gear, three of a kind per rarity: contracts, the bench, the wheel, the plates
		int tier = WarpedRooms.threat() / 5;
		for (Rarity rarity : new Rarity[]{ Rarity.UNCOMMON, Rarity.RARE, Rarity.LEGENDARY }){
			for (int i = 0; i < 3; i++){
				Item weapon = Generator.randomWeapon( tier, true );
				weapon.cursed = false;
				lay( WarpedRooms.forceQuality( weapon, rarity, null ).identify() );
			}
		}
		for (int i = 0; i < 3; i++){
			Item armor = Generator.randomArmor( tier );
			armor.cursed = false;
			lay( WarpedRooms.forceQuality( armor, Rarity.UNCOMMON, null ).identify() );
		}
	}

	// ---------------------------------------------------------------- nothing of its own

	@Override
	protected void createMobs(){
		//only what the rooms themselves place
	}

	@Override
	public Mob createMob(){
		return null;
	}

	@Override
	public Actor addRespawner(){
		return null;
	}

	@Override
	protected void createItems(){
		//only what the rooms and the supply rows drop
	}
}
