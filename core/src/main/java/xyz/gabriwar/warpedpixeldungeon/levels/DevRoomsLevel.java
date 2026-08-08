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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.Room;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.EmptyRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.StandardRoom;
import xyz.gabriwar.warpedpixeldungeon.tiles.DevSign;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * DEV TOOL: a showcase floor containing every room in the game (standard rooms
 * in each supported size category, specials, secrets, connections, quest, vault
 * and goo arena variants), laid out on an open plaza. Each room gets a statue
 * "sign" beside its door - tap it to see the room's name. Reached via the debug
 * menu's travel tab (depth 98). Rooms whose painters need real level context
 * are painted best-effort; failures are labeled on their sign.
 */
public class DevRoomsLevel extends Level {

	{
		color1 = 0x48763c;
		color2 = 0x59994a;
	}

	private static final Class<?>[] STANDARD = {
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.AquariumRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.BurnedRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.CaveRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.CavesFissureRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.CellBlockRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.ChasmBridgeRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.ChasmRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.CircleBasinRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.CirclePitRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.CircleWallRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.EmptyRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.FissureRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.GrassyGraveRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.HallwayRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.ImpShopRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.LibraryHallRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.LibraryRingRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.MinefieldRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.PillarsRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.PlantsRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.PlatformRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.RegionDecoBridgeRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.RegionDecoLineRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.RegionDecoPatchRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.RingRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.RitualRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.RuinsRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.SegmentedLibraryRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.SegmentedRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.SewerPipeRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.SkullsRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.StatueLineRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.StatuesRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.StripedRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.StudyRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.SuspiciousChestRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.WaterBridgeRoom.class
	};
	private static final Class<?>[] ENTRANCES = {
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.entrance.CaveEntranceRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.entrance.CavesFissureEntranceRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.entrance.CellBlockEntranceRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.entrance.ChasmBridgeEntranceRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.entrance.ChasmEntranceRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.entrance.CircleBasinEntranceRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.entrance.EntranceRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.entrance.HallwayEntranceRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.entrance.LibraryHallEntranceRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.entrance.LibraryRingEntranceRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.entrance.PillarsEntranceRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.entrance.RegionDecoBridgeEntranceRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.entrance.RegionDecoLineEntranceRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.entrance.RegionDecoPatchEntranceRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.entrance.RingEntranceRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.entrance.RitualEntranceRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.entrance.RuinsEntranceRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.entrance.StatueLineEntranceRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.entrance.StatuesEntranceRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.entrance.WaterBridgeEntranceRoom.class
	};
	private static final Class<?>[] EXITS = {
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.exit.CaveExitRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.exit.CavesFissureExitRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.exit.CellBlockExitRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.exit.ChasmBridgeExitRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.exit.ChasmExitRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.exit.CircleBasinExitRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.exit.CircleWallEntranceRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.exit.CircleWallExitRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.exit.ExitRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.exit.HallwayExitRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.exit.LibraryHallExitRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.exit.LibraryRingExitRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.exit.PillarsExitRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.exit.RegionDecoBridgeExitRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.exit.RegionDecoLineExitRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.exit.RegionDecoPatchExitRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.exit.RingExitRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.exit.RitualExitRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.exit.RuinsExitRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.exit.StatueLineExitRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.exit.StatuesExitRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.exit.WaterBridgeExitRoom.class
	};
	private static final Class<?>[] SPECIALS = {
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.AltarRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.ArmoryRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.CryptRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.CrystalChoiceRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.CrystalPathRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.CrystalVaultRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.DemonSpawnerRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.EnchantingLibraryRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.GardenRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.LaboratoryRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.LibraryRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.MagicWellRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.MagicalFireRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.PitRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.PoolRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.RunestoneRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.SacrificeRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.SentryRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.ShopRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.StatueRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.StorageRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.ToxicGasRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.TrapsRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.TreasuryRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.WeakFloorRoom.class
	};
	private static final Class<?>[] SECRETS = {
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.secret.RatKingRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.secret.SecretArtilleryRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.secret.SecretChestChasmRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.secret.SecretGardenRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.secret.SecretHoardRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.secret.SecretHoneypotRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.secret.SecretLaboratoryRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.secret.SecretLarderRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.secret.SecretLibraryRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.secret.SecretMazeRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.secret.SecretRunestoneRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.secret.SecretSummoningRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.secret.SecretWellRoom.class
	};
	private static final Class<?>[] CONNECTIONS = {
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.connection.BridgeRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.connection.MazeConnectionRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.connection.PerimeterRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.connection.RingBridgeRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.connection.RingTunnelRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.connection.TunnelRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.connection.WalkwayRoom.class
	};
	private static final Class<?>[] QUEST = {
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.AmbitiousImpRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.BlacksmithRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.MassGraveRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.MineEntrance.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.MineGiantRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.MineLargeRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.MineSecretRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.MineSmallRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.RitualSiteRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.RotGardenRoom.class
	};
	private static final Class<?>[] VAULT = {
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.vault.AlternatingTrapsRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.vault.VaultCircleRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.vault.VaultCrossRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.vault.VaultEnemyCenterRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.vault.VaultEntranceRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.vault.VaultFinalRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.vault.VaultLasersRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.vault.VaultLongRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.vault.VaultQuadrantsRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.vault.VaultRingRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.vault.VaultRingsRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.vault.VaultSimpleEnemyTreasureRoom.class
	};
	private static final Class<?>[] SEWER_BOSS = {
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.sewerboss.DiamondGooRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.sewerboss.SewerBossEntranceRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.sewerboss.SewerBossExitRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.sewerboss.ThickPillarsGooRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.sewerboss.ThinPillarsGooRoom.class,
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.sewerboss.WalledGooRoom.class
	};

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_SEWERS;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_SEWERS;
	}

	@Override
	protected boolean build() {

		ArrayList<Room> rooms = new ArrayList<>();
		ArrayList<String> labels = new ArrayList<>();

		//standard rooms appear once per size category they support
		for (Class<?> c : STANDARD) addStandard(rooms, labels, c, "");
		for (Class<?> c : ENTRANCES) addStandard(rooms, labels, c, "");
		for (Class<?> c : EXITS)     addStandard(rooms, labels, c, "");

		addAll(rooms, labels, SPECIALS,    "special: ");
		addAll(rooms, labels, SECRETS,     "secret: ");
		addAll(rooms, labels, CONNECTIONS, "connection: ");
		addAll(rooms, labels, QUEST,       "quest: ");
		addAll(rooms, labels, VAULT,       "vault: ");
		addAll(rooms, labels, SEWER_BOSS,  "goo arena: ");

		//flow layout across a fixed-width plaza
		final int MAXW = 160;
		int cx = 4, cy = 6, rowH = 0;
		for (Room r : rooms){
			int w = r.width() + 1;
			int h = r.height() + 1;
			if (cx + w + 4 > MAXW){
				cx = 4;
				cy += rowH + 5;
				rowH = 0;
			}
			r.shift( cx - r.left, cy - r.top );
			cx += w + 4;
			rowH = Math.max( rowH, h );
		}

		int H = cy + rowH + 6;
		setSize( MAXW, H );
		Painter.fill( this, 0, 0, MAXW, H, Terrain.EMPTY );
		//border walls
		Painter.fill( this, 0, 0, MAXW, 1, Terrain.WALL );
		Painter.fill( this, 0, H-1, MAXW, 1, Terrain.WALL );
		Painter.fill( this, 0, 0, 1, H, Terrain.WALL );
		Painter.fill( this, MAXW-1, 0, 1, H, Terrain.WALL );

		//entrance plaza, top-left
		int entrance = 2 + 2*width();
		Painter.set( this, entrance, Terrain.ENTRANCE );
		transitions.add( new LevelTransition( this, entrance, LevelTransition.Type.REGULAR_ENTRANCE ));

		for (int i = 0; i < rooms.size(); i++){
			Room r = rooms.get(i);

			//a door on the left wall, with a dummy neighbor so entrance() works.
			//deliberately OFF the vertical centerline: some painters (SentryRoom)
			//reroll center() until it differs from the door and would loop forever
			int doorY = Math.min( r.top + 2, r.bottom - 1 );
			Room.Door door = new Room.Door( r.left, doorY );
			EmptyRoom dummy = new EmptyRoom();
			dummy.set( r.left-3, door.y-1, r.left-1, door.y+1 );
			r.connected.put( dummy, door );

			boolean failed = false;
			try {
				//dev diagnostics: if generation hangs, the last printed room is the culprit
				System.out.println("[DevRooms] painting " + labels.get(i)
						+ " rect=(" + r.left + "," + r.top + " -> " + r.right + "," + r.bottom + ")"
						+ " size=" + (r.width()+1) + "x" + (r.height()+1));
				r.paint( this );
			} catch (Throwable t) {
				failed = true;
				//leave whatever was painted; the sign flags the failure
			}

			//force the door open and passable regardless of what the room set
			Painter.set( this, door.x, door.y, Terrain.DOOR );

			//the sign: a signpost outside the door corner, named after the room
			int signX = r.left - 1;
			int signY = door.y - 1;
			int signCell = signX + signY*width();
			map[signCell] = Terrain.EMPTY;

			DevSign sign = new DevSign();
			sign.label = labels.get(i) + (failed ? " (PAINT FAILED)" : "");
			sign.pos( signX, signY );
			customTiles.add( sign );
		}

		//kill any leftover doors leading nowhere and clean invalid cells
		buildFlagMaps();
		cleanWalls();

		//the whole showcase starts revealed on the map. visited (not just mapped)
		//so terrain renders everywhere instead of the flat teal 'mapped' fog
		java.util.Arrays.fill( mapped, true );
		java.util.Arrays.fill( visited, true );

		return true;
	}

	//the player always sees the entire floor here, and everything that lives on it
	//(painted sentries, shopkeepers, late demon-spawner spawns...) stays paralyzed
	@Override
	public void updateFieldOfView( xyz.gabriwar.warpedpixeldungeon.actors.Char c, boolean[] fieldOfView ) {
		super.updateFieldOfView( c, fieldOfView );
		if (c == xyz.gabriwar.warpedpixeldungeon.Dungeon.hero){
			java.util.Arrays.fill( fieldOfView, true );


			//only sweep once the game scene is built: buffing can have visual side
			//effects (paralysis reveals mimics -> particles) that NPE mid level-switch
			if (com.watabou.noosa.Game.scene() instanceof xyz.gabriwar.warpedpixeldungeon.scenes.GameScene){
				for (Mob m : mobs.toArray( new Mob[0] )){
					if (m.buff( xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis.class ) == null){
						xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff.affect( m,
								xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis.class, 1_000_000f );
					}
				}
			}
		}
	}

	private void addStandard(ArrayList<Room> rooms, ArrayList<String> labels, Class<?> c, String prefix){
		Room probe = (Room) Reflection.newInstance( c );
		if (probe == null) return;

		//a few rooms live in the standard package without extending StandardRoom
		//(e.g. ImpShopRoom) - they have no size categories, add them singly
		if (!(probe instanceof StandardRoom)){
			probe.setSize();
			rooms.add( probe );
			labels.add( prefix + c.getSimpleName() );
			return;
		}

		for (int ord = 0; ord < StandardRoom.SizeCategory.values().length; ord++){
			StandardRoom r = (StandardRoom) Reflection.newInstance( c );
			if (r == null) return;
			if (!r.setSizeCat( ord, ord )) continue;
			r.setSize();
			rooms.add( r );
			labels.add( prefix + c.getSimpleName() + " [" + r.sizeCat.name() + "]" );
		}
	}

	private void addAll(ArrayList<Room> rooms, ArrayList<String> labels, Class<?>[] classes, String prefix){
		for (Class<?> c : classes){
			Room r = (Room) Reflection.newInstance( c );
			if (r == null) continue;
			//clamp: bridge-style rooms can roll sizes wider than this level, and an
			//over-wide Painter.fill wraps across map rows (the flood-of-water bug)
			if (!r.setSizeWithLimit( 40, 40 )){
				r.setSize();
			}
			if (r.width() + 1 > 60 || r.height() + 1 > 60){
				//irreducibly huge - skip rather than corrupt the plaza
				continue;
			}
			rooms.add( r );
			labels.add( prefix + c.getSimpleName() );
		}
	}

	@Override
	protected void createMobs() {
		//only what the room painters themselves place
	}

	@Override
	public Mob createMob() {
		return null;
	}

	@Override
	public Actor addRespawner() {
		return null;
	}

	@Override
	protected void createItems() {
		//only what the room painters themselves drop
	}
}
