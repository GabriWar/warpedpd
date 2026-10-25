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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WetlandSites;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Visual;
import com.watabou.noosa.particles.Emitter;

import java.util.ArrayList;

/**
 * The sea over a beach's tide pools at high water (levels/overworld/WetlandSites): the waves break
 * over the rocks, white foam rolling on the pools and spray thrown up off them where the hero sees
 * them - dense, white, short-lived - and nothing at low water, when the pools lie still with their
 * shells in them. A tick every TICK seconds puts a whitecap or two and a few droplets on random pool
 * cells of every beach in view, from the effects' own random numbers into their loose holder
 * (Fx.air), budgeted (FxBudget) like every other particle. Render thread only; nothing is saved, so
 * a co-op guest's screen makes its own from its own window and the world's clock.
 *
 * The window cells it reads are the scene's: a sentinel in the effects group shows a window slide,
 * and nothing is laid while the window and the scene disagree.
 */
public class TideSurf extends Gizmo {

	private static final float TICK = 0.12f;
	//of the pool cells of a beach in view, how many break each tick
	private static final int BREAKS = 2;
	/** The foam's white, the spray's. */
	public static final int FOAM = 0xF2F8FA, SPRAY = 0xE4F2F8;

	private final OverworldLevel level;
	private final Visual sentinel = new Visual( 0, 0, 0, 0 );
	private int visX, visY;
	private float tickIn = TICK;

	public TideSurf( OverworldLevel level ){
		this.level = level;
		visX = level.worldX();
		visY = level.worldY();
		sentinel.visible = false;
		sentinel.active = false;
	}

	//a whitecap rolling on the water: one of the kit's foam crests, white, drifting with the swell
	private static final Emitter.Factory CREST = new Emitter.Factory(){
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			if (!FxBudget.keep( FxBudget.P2, x, y )) return;
			((FxParticle) e.recycle( FxParticle.class )).reset( x, y )
					.look( FxFrames.WHITECAP[FxRandom.Int( FxFrames.WHITECAP.length )] ).paint( FOAM )
					.life( FxRandom.Float( 0.5f, 0.9f ) ).alpha( 0.85f, 0.1f, 0.35f )
					.vel( FxRandom.Float( -4f, 4f ), -FxRandom.Float( 0.5f, 2f ) ).snap().priority( FxBudget.P2 );
		}
	};

	@Override
	public void update(){
		super.update();
		if (Dungeon.level != level || level.altitude() != 0 || Dungeon.hero == null) return;
		if (sentinel.parent == null) GameScene.effect( sentinel );
		if (sentinel.x != 0f || sentinel.y != 0f){
			visX -= Math.round( sentinel.x / DungeonTilemap.SIZE );
			visY -= Math.round( sentinel.y / DungeonTilemap.SIZE );
			sentinel.x = sentinel.y = 0f;
		}
		if (NetManager.isNetClient()){
			visX = level.worldX();
			visY = level.worldY();
		}
		if (Emitter.freezeEmitters || (tickIn -= Game.elapsed) > 0) return;
		tickIn = TICK;
		if (level.worldX() != visX || level.worldY() != visY) return;
		if (WetlandSites.tideOut( WorldClock.turn() )) return;
		Camera cam = Camera.main;
		FxEmitter air = Fx.air();
		if (cam == null || air == null) return;
		final int S = DungeonTilemap.SIZE;
		int wx0 = visX + (int)(cam.scroll.x / S) - 1, wy0 = visY + (int)(cam.scroll.y / S) - 1;
		int wx1 = visX + (int)((cam.scroll.x + cam.width) / S) + 1, wy1 = visY + (int)((cam.scroll.y + cam.height) / S) + 1;
		for (WetlandSites.Place p : WetlandSites.placesIn( level.worldSeed(), wx0, wy0, wx1, wy1 )){
			if (p.kind != xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures.Site.TIDE_POOLS) continue;
			ArrayList<Integer> seen = new ArrayList<>();
			int[] pools = p.pools();
			for (int i = 0; i < pools.length; i += 2){
				int c = level.localCell( pools[i], pools[i + 1] );
				if (c == -1 || level.map[c] != Terrain.WATER || level.heroFOV == null || !level.heroFOV[c]) continue;
				seen.add( c );
			}
			for (int b = 0; b < BREAKS && !seen.isEmpty(); b++){
				int c = seen.get( FxRandom.Int( seen.size() ) );
				float x = (c % level.width()) * S + FxRandom.Float( 2f, 14f ), y = (c / level.width()) * S + FxRandom.Float( 4f, 13f );
				CREST.emit( air, 0, x, y );
				if (FxRandom.chance( 0.6f )){
					int n = FxRandom.IntRange( 1, 3 );
					for (int k = 0; k < n; k++) FxFactories.droplet( SPRAY ).emit( air, 0, x, y );
				}
			}
		}
	}

	@Override
	public void destroy(){
		super.destroy();
		sentinel.killAndErase();
	}
}
