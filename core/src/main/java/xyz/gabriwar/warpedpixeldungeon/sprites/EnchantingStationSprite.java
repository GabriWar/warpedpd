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

package xyz.gabriwar.warpedpixeldungeon.sprites;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ElmoParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The enchanting pedestal. Borrows the dwarven pedestal tile art, and while
 * visible it pulls slow streams of assorted particles out of the surrounding
 * bookshelves - glyphs flowing from the library into the stone.
 */
public class EnchantingStationSprite extends MobSprite {

	//sparkles plus red, green and blue motes.
	//also used by EnchantingScene for its ambient particles
	public static final Emitter.Factory[] GLYPH_FX = new Emitter.Factory[]{
			SparkParticle.STATIC,
			Speck.factory( Speck.LIGHT ),
			FlameParticle.FACTORY,
			ElmoParticle.FACTORY,
			FlowParticle.FACTORY
	};

	private static final int STREAMS      = 3;
	private static final float STREAM_TIME = 1.2f;

	public EnchantingStationSprite() {
		super();

		texture( Assets.Environment.TILES_CITY );

		TextureFilm film = new TextureFilm( texture, 16, 16 );

		idle = new Animation( 1, true );
		idle.frames( film, DungeonTileSheet.PEDESTAL );

		run = idle.clone();
		attack = idle.clone();
		die = idle.clone();

		play( idle );
	}

	private class GlyphStream {
		PointF from;
		Emitter.Factory fx;
		float t;
		float emitCooldown;
	}

	private ArrayList<GlyphStream> streams;
	private float rescanCooldown = 0;
	private ArrayList<PointF> sources;

	@Override
	public void update() {
		super.update();

		if (!visible || ch == null || paused) return;

		//re-scan periodically so bookshelves built next to the pedestal after it
		//was placed start feeding it live
		rescanCooldown -= Game.elapsed;
		if (sources == null || rescanCooldown <= 0){
			rescanCooldown = 2f;
			findSources();
		}
		if (sources.isEmpty()) return;

		if (streams == null){
			streams = new ArrayList<>();
			for (int i = 0; i < STREAMS; i++){
				GlyphStream s = newStream();
				//stagger the initial streams so they don't pulse in sync
				s.t = i / (float)STREAMS;
				streams.add(s);
			}
		}

		PointF target = center();
		for (GlyphStream s : streams){
			s.t += Game.elapsed / STREAM_TIME;
			if (s.t >= 1f){
				GlyphStream fresh = newStream();
				s.from = fresh.from;
				s.fx = fresh.fx;
				s.t = 0;
				continue;
			}
			s.emitCooldown -= Game.elapsed;
			if (s.emitCooldown <= 0){
				s.emitCooldown = 0.08f;
				float x = s.from.x + (target.x - s.from.x) * s.t;
				float y = s.from.y + (target.y - s.from.y) * s.t;
				Emitter e = GameScene.emitter();
				if (e != null){
					e.pos( x, y );
					e.burst( s.fx, 1 );
				}
			}
		}
	}

	private GlyphStream newStream(){
		GlyphStream s = new GlyphStream();
		s.from = Random.element( sources );
		s.fx = Random.element( GLYPH_FX );
		s.t = 0;
		s.emitCooldown = 0;
		return s;
	}

	//bookshelves within a short radius feed the pedestal
	private void findSources(){
		sources = new ArrayList<>();
		if (Dungeon.level == null || ch == null) return;
		int w = Dungeon.level.width();
		int cx = ch.pos % w;
		int cy = ch.pos / w;
		for (int y = Math.max(0, cy-6); y <= Math.min(Dungeon.level.height()-1, cy+6); y++){
			for (int x = Math.max(0, cx-6); x <= Math.min(w-1, cx+6); x++){
				int cell = x + y*w;
				int t = Dungeon.level.map[cell];
				if (t == Terrain.BOOKSHELF || t == Terrain.EMPTY_BOOKSHELF){
					sources.add( DungeonTilemap.tileCenterToWorld( cell ) );
				}
			}
		}
	}

	@Override
	public void die() {
		super.die();
		streams = null;
	}
}
