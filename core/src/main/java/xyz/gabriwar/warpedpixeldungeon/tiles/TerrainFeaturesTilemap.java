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

package xyz.gabriwar.warpedpixeldungeon.tiles;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.levels.LastShopLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.Trap;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import com.watabou.noosa.Image;
import com.watabou.noosa.tweeners.ScaleTweener;
import com.watabou.utils.PointF;
import com.watabou.utils.RectF;
import com.watabou.utils.SparseArray;

public class TerrainFeaturesTilemap extends DungeonTilemap {

	private static TerrainFeaturesTilemap instance;

	private SparseArray<Plant> plants;
	private SparseArray<Trap> traps;

	static boolean usesSeparateDecoration(String texture) {
		return Assets.Environment.TILES_SEWERS.equals(texture)
				|| Assets.Environment.TILES_PRISON.equals(texture)
				|| Assets.Environment.TILES_CAVES.equals(texture)
				|| Assets.Environment.TILES_CAVES_CRYSTAL.equals(texture)
				|| Assets.Environment.TILES_CAVES_GNOLL.equals(texture)
				|| Assets.Environment.TILES_CITY.equals(texture)
				|| Assets.Environment.TILES_HALLS.equals(texture);
	}

	//the dressing row that belongs to a regional ground sheet, or -1 for any other sheet
	private static int regionalStage(String texture) {
		if (Assets.Environment.TILES_SEWERS.equals(texture))        return 0;
		if (Assets.Environment.TILES_PRISON.equals(texture))        return 1;
		if (Assets.Environment.TILES_CAVES.equals(texture)
				|| Assets.Environment.TILES_CAVES_CRYSTAL.equals(texture)
				|| Assets.Environment.TILES_CAVES_GNOLL.equals(texture)) return 2;
		if (Assets.Environment.TILES_CITY.equals(texture))          return 3;
		if (Assets.Environment.TILES_HALLS.equals(texture))         return 4;
		return -1;
	}

	public TerrainFeaturesTilemap(SparseArray<Plant> plants, SparseArray<Trap> traps) {
		super(Assets.Environment.TERRAIN_FEATURES);

		this.plants = plants;
		this.traps = traps;

		if (Dungeon.level != null) {
			map(Dungeon.level.map, Dungeon.level.width());
		}

		instance = this;
	}

	protected int getTileVisual(int pos, int tile, boolean flat){
		if (traps.get(pos) != null){
			Trap trap = traps.get(pos);
			if (!trap.visible)
				return -1;
			else
				return (trap.active ? trap.color : Trap.BLACK) + (trap.shape * 16);
		}

		if (plants.get(pos) != null){
			return plants.get(pos).image + 7*16;
		}

		//Only the upstream sheets were split into base art + decoration in v4.0.
		//Custom sheets still contain the complete object (including its overhang).
		//Adding a depth-based overlay paints prison bars over Spider Nest minecarts.
		if (!usesSeparateDecoration(Dungeon.level.tilesTex())
				&& (tile == Terrain.BARRICADE || tile == Terrain.ALCHEMY
				|| tile == Terrain.STATUE || tile == Terrain.STATUE_SP
				|| tile == Terrain.REGION_DECO || tile == Terrain.REGION_DECO_ALT
				|| tile == Terrain.MINE_CRYSTAL || tile == Terrain.MINE_BOULDER)) {
			return -1;
		}

		int stage = (Dungeon.depth-1)/5;
		if (Dungeon.depth == 21 && Dungeon.level instanceof LastShopLevel) stage--;
		stage = Math.min(stage, 4);
		//a floor drawn on one of the five regional sheets gets that region's dressing
		//whatever its depth says: the dev floors sit at 85 and 86 on sewer tiles, and by
		//depth alone they were dressed for the demon halls (a halls urn for a sewer barrel)
		int regional = regionalStage(Dungeon.level.tilesTex());
		if (regional != -1) stage = regional;
		//the overworld sits at depth 97, which lands on the demon-halls stage
		//and paints every grass tile with red dressing - it wants sewers green
		if (Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) stage = 0;
		//the overworld's trees are drawn by its dressing layers (variety +
		//walk-behind canopy) - the generic single-tile art would double up
		if ((tile == Terrain.TREE_PINE || tile == Terrain.TREE_OAK)
				&& Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel){
			return -1;
		}
		if (tile == Terrain.SHRUB){
			//the bush is an overlay with a transparent background - the ground
			//tile underneath (grass, via directVisuals) shows through cleanly
			return 15;
		} else if (tile == Terrain.TREE_PINE){
			return 31;
		} else if (tile == Terrain.TREE_OAK){
			return 47;
		} else if (tile == Terrain.BOULDER){
			//a standing rock in the world is drawn whole by the overworld dressing
			if (Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel
					&& ((xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level).tallRockAt( pos )){
				return -1;
			}
			return 63;
		} else if (tile == Terrain.FLOWER_PATCH){
			return 79;
		} else if (tile == Terrain.MUSHROOM_PATCH){
			return 95;
		} else if (tile == Terrain.HIGH_GRASS){
			if (DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.RAISED_HIGH_GRASS, pos) == DungeonTileSheet.RAISED_HIGH_GRASS_ALT){
				return 192 + 16*stage + 1;
			} else {
				return 192 + 16*stage;
			}
		} else if (tile == Terrain.FURROWED_GRASS
				&& !DungeonTileSheet.tilledSoil( pos )){
			//tilled soil and a village's field are clean farm ground - no grass decoration on top
			if (DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.RAISED_FURROWED_GRASS, pos) == DungeonTileSheet.RAISED_FURROWED_ALT){
				return 194 + 16*stage + 1;
			} else {
				return 194 + 16*stage;
			}
		} else if (tile == Terrain.GRASS) {
			if (DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.GRASS, pos) == DungeonTileSheet.GRASS_ALT){
				return 196 + 16*stage + 1;
			} else {
				return 196 + 16*stage;
			}
		} else if (tile == Terrain.BARRICADE) {
			return 198 + 16*stage;

		} else if (tile == Terrain.ALCHEMY) {
			return 199 + 16*stage;

		} else if (tile == Terrain.STATUE || tile == Terrain.STATUE_SP) {
			return 200 + 16*stage;

		} else if (tile == Terrain.REGION_DECO) {
			return 201 + 16 * stage;

		} else if (tile == Terrain.REGION_DECO_ALT) {
			return 202 + 16 * stage;

		} else if (tile == Terrain.EMBERS) {
			if (DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.EMBERS, pos) == DungeonTileSheet.EMBERS_ALT){
				return 272 + 1;
			} else {
				return 272;
			}
		} else if (tile == Terrain.MINE_CRYSTAL){
			int vis = DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.RAISED_MINE_CRYSTAL_BLUE_1, pos);
			if (vis == DungeonTileSheet.RAISED_MINE_CRYSTAL_RED_2){
				return 274 + 5;
			} else if (vis == DungeonTileSheet.RAISED_MINE_CRYSTAL_RED_1){
				return 274 + 4;
			} else if (vis == DungeonTileSheet.RAISED_MINE_CRYSTAL_GREEN_2){
				return 274 + 3;
			} else if (vis == DungeonTileSheet.RAISED_MINE_CRYSTAL_GREEN_1){
				return 274 + 2;
			} else if (vis == DungeonTileSheet.RAISED_MINE_CRYSTAL_BLUE_2){
				return 274 + 1;
			} else {
				return 274 + 0;
			}
		} else if (tile == Terrain.MINE_BOULDER){
			int vis = DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.RAISED_MINE_BOULDER, pos);
			if (vis == DungeonTileSheet.RAISED_MINE_BOULDER_ALT_2){
				return 280 + 2;
			} else if (vis == DungeonTileSheet.RAISED_MINE_BOULDER_ALT){
				return 280 + 1;
			} else {
				return 280;
			}
		}

		return -1;
	}

	public static Image getTrapVisual( Trap trap ){
		if (instance == null) instance = new TerrainFeaturesTilemap(null, null);

		RectF uv = instance.tileset.get((trap.active ? trap.color : Trap.BLACK) + (trap.shape * 16));
		if (uv == null) return null;

		Image img = new Image( instance.texture );
		img.frame(uv);
		return img;
	}

	public static Image getPlantVisual( Plant plant ){
		if (instance == null) instance = new TerrainFeaturesTilemap(null, null);

		RectF uv = instance.tileset.get(plant.image + 7*16);
		if (uv == null) return null;

		Image img = new Image( instance.texture );
		img.frame(uv);
		return img;
	}

	public static Image tile(int pos, int tile ) {
		RectF uv = instance.tileset.get( instance.getTileVisual( pos, tile, true ) );
		if (uv == null) return null;
		
		Image img = new Image( instance.texture );
		img.frame(uv);
		return img;
	}

	public void growPlant( final int pos ){
		final Image plant = tile( pos, map[pos] );
		if (plant == null) return;
		
		plant.origin.set( 8, 12 );
		plant.scale.set( 0 );
		plant.point( DungeonTilemap.tileToWorld( pos ) );

		parent.add( plant );

		parent.add( new ScaleTweener( plant, new PointF(1, 1), 0.2f ) {
			protected void onComplete() {
				plant.killAndErase();
				killAndErase();
				updateMapCell(pos);
			}
		} );
	}

}
