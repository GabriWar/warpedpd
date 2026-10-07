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


package xyz.gabriwar.warpedpixeldungeon.levels.overworld;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.Visual;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * The cracks on thin ice that has been stepped on (HazardWatch), drawn on the ice of a world
 * slice, under everyone standing there: environment/ice_cracks.png (tools/ice_cracks_gen.py),
 * the column by which sides of the cell are not ice (the crack keeps off the ground feathered
 * over them), the row by stage (cracked, giving way) and a variant per world cell. A cell giving
 * way has its breaks run on into the ice round it (rows 4-5), where the hero standing on it
 * does not hide them.
 *
 * Render thread. OverworldLevel.addVisuals puts it among the level's visuals, which a rebase
 * does not slide: it sees the slide through a sentinel kept in the effects group, which does
 * slide (the OverworldCritters.Field way), and lays the cracks again in the new frame; a guest's
 * mirror, re-labelled without a slide, compares the window's origin. Nothing is laid while the
 * window and the scene disagree (a host's rebase between its actor and render halves).
 */
final class IceCracks extends Group {

	private final OverworldLevel level;
	private final Visual sentinel = new Visual( 0, 0, 0, 0 );
	//the world cell drawn at the scene's cell (0,0)
	private int visX, visY;
	private int version = -1;
	private boolean stale = true;
	private final HashMap<Long, Image> laid = new HashMap<>();
	//a giving cell's breaks on the ice above, right, below and left of it
	private final HashMap<Long, Image[]> spills = new HashMap<>();
	private TextureFilm film;

	IceCracks( OverworldLevel level ){
		this.level = level;
		visX = level.worldX();
		visY = level.worldY();
		sentinel.visible = false;
		sentinel.active = false;
	}

	@Override
	public void update(){
		super.update();
		if (Dungeon.level != level) return;
		if (sentinel.parent == null) GameScene.effect( sentinel );
		if (sentinel.x != 0f || sentinel.y != 0f){
			visX -= Math.round( sentinel.x / DungeonTilemap.SIZE );
			visY -= Math.round( sentinel.y / DungeonTilemap.SIZE );
			sentinel.x = sentinel.y = 0f;
			stale = true;
		}
		if (NetManager.isNetClient() && (level.worldX() != visX || level.worldY() != visY)){
			visX = level.worldX();
			visY = level.worldY();
			stale = true;
		}
		HazardWatch.Cracks cracks = level.hazards().cracks();
		if (!stale && cracks.version == version) return;
		//the window has moved and the scene not yet: wait for the slide
		if (level.worldX() != visX || level.worldY() != visY) return;
		lay( cracks );
		version = cracks.version;
		stale = false;
	}

	private void lay( HazardWatch.Cracks cracks ){
		if (film == null) film = new TextureFilm( Assets.Environment.ICE_CRACKS, DungeonTilemap.SIZE, DungeonTilemap.SIZE );
		int w = level.width(), h = level.height();
		int[] map = level.map;
		HashSet<Long> keep = new HashSet<>();
		for (int i = 0; i < cracks.keys.length; i++){
			long k = cracks.keys[i];
			int wx = (int)(k & 0xFFFFFFFFL), wy = (int)(k >> 32);
			int x = wx - visX, y = wy - visY;
			if (x <= 0 || y <= 0 || x >= w - 1 || y >= h - 1) continue;
			int c = x + y * w;
			if (map[c] != Terrain.FROZEN_WATER) continue;
			int sides = (map[c - w] != Terrain.FROZEN_WATER ? 1 : 0) | (map[c + 1] != Terrain.FROZEN_WATER ? 2 : 0)
					| (map[c + w] != Terrain.FROZEN_WATER ? 4 : 0) | (map[c - 1] != Terrain.FROZEN_WATER ? 8 : 0);
			int variant = (int) Math.floorMod( WindowGenerator.dressHash( level.worldSeed(), wx, wy, 0x1CEC4AL ), 2L );
			int row = (cracks.stages[i] >= 2 ? 2 : 0) + variant;
			Image img = laid.get( k );
			if (img == null){
				img = new Image( Assets.Environment.ICE_CRACKS );
				laid.put( k, img );
				add( img );
			}
			img.frame( film.get( sides + 16 * row ) );
			img.point( DungeonTilemap.tileToWorld( c ) );
			keep.add( k );
			Image[] sp = spills.get( k );
			if (cracks.stages[i] >= 2){
				if (sp == null){
					sp = new Image[4];
					spills.put( k, sp );
				}
				int[] around = { c - w, c + 1, c + w, c - 1 };
				for (int d = 0; d < 4; d++){
					if (map[around[d]] != Terrain.FROZEN_WATER){
						if (sp[d] != null){
							sp[d].killAndErase();
							sp[d] = null;
						}
						continue;
					}
					if (sp[d] == null){
						sp[d] = new Image( Assets.Environment.ICE_CRACKS );
						add( sp[d] );
					}
					sp[d].frame( film.get( d + 16 * (4 + variant) ) );
					sp[d].point( DungeonTilemap.tileToWorld( around[d] ) );
				}
			} else if (sp != null){
				for (Image im : sp) if (im != null) im.killAndErase();
				spills.remove( k );
			}
		}
		Iterator<Map.Entry<Long, Image>> it = laid.entrySet().iterator();
		while (it.hasNext()){
			Map.Entry<Long, Image> e = it.next();
			if (keep.contains( e.getKey() )) continue;
			e.getValue().killAndErase();
			it.remove();
			Image[] sp = spills.remove( e.getKey() );
			if (sp != null) for (Image im : sp) if (im != null) im.killAndErase();
		}
	}
}
