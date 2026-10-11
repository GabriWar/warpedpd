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

package xyz.gabriwar.warpedpixeldungeon.sprites;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.Nature;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfRegrowth;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.particles.Emitter;

import java.util.ArrayList;

/**
 * The lotus: it opens with its petals popping out and settling, stands in a pale breathing pool
 * of light, lets a petal drift off now and then, and the grass of its reach (GRASS_EMITTERS of
 * its cells at most, spread over it, each pouring a leaf every 1.5 s where the hero sees) rustles.
 */
public class LotusSprite extends MobSprite {

	/** Its grass: at most this many of its cells pour leaves, one every GRASS_EVERY s. */
	public static final int GRASS_EMITTERS = 24;
	public static final float GRASS_EVERY = 1.5f;
	/** Its opening petals; its pool's colour and level; a petal drifts off every 2-4 s. */
	public static final int OPEN_PETALS = 8, POOL_COLOR = 0xFFE8F0;
	public static final float POOL_LEVEL = 0.2f / FxLight.POOL_CAP, DRIFT_MIN = 2f, DRIFT_MAX = 4f;

	//a leaf of its grass, only where the hero sees it rise (none spent out of sight)
	private static final Emitter.Factory GRASS = new Emitter.Factory(){
		@Override
		public void emit( Emitter emitter, int index, float x, float y ){
			if (Fx.seen( x, y )) LeafParticle.LEVEL_SPECIFIC.emit( emitter, index, x, y );
		}
	};

	private ArrayList<Emitter> grassVfx;
	private FxLight pool;
	private float driftIn;

	public LotusSprite(){
		super();

		perspectiveRaise = 0f;

		texture( Assets.Sprites.LOTUS );

		TextureFilm frames = new TextureFilm( texture, 19, 16 );

		idle = new MovieClip.Animation( 1, true );
		idle.frames( frames, 0 );

		run = new MovieClip.Animation( 1, true );
		run.frames( frames, 0 );

		attack = new MovieClip.Animation( 1, false );
		attack.frames( frames, 0 );

		die = new MovieClip.Animation( 1, false );
		die.frames( frames, 0 );

		play( idle );
	}

	@Override
	public void link( Char ch ) {
		super.link( ch );

		renderShadow = false;

		if (grassVfx == null && ch instanceof WandOfRegrowth.Lotus){
			grassVfx = new ArrayList<>();
			final WandOfRegrowth.Lotus l = (WandOfRegrowth.Lotus) ch;
			//its own emitters, which draw nothing of the game's Random
			Fx.post( () -> open( l ) );
		}
	}

	//the grass of its reach (only the box round it is looked at), its pool, its petals
	private void open( WandOfRegrowth.Lotus l ){
		Level level = Dungeon.level;
		if (grassVfx == null || level == null || parent == null) return;
		int w = level.width(), r = l.range();
		int lx = l.pos % w, ly = l.pos / w;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int y = Math.max( 0, ly - r ); y <= Math.min( level.height() - 1, ly + r ); y++){
			for (int x = Math.max( 0, lx - r ); x <= Math.min( w - 1, lx + r ); x++){
				int c = x + y * w;
				if (!level.solid[c] && l.inRange( c )) cells.add( c );
			}
		}
		//spread over the reach: a fair sample of its cells
		for (int i = 0; i < cells.size() && i < GRASS_EMITTERS; i++){
			int j = i + FxRandom.Int( cells.size() - i );
			int c = cells.get( j );
			cells.set( j, cells.get( i ) );
			FxEmitter e = GameScene.fxEmitter();
			if (e == null) break;
			e.pos( (c % w) * DungeonTilemap.SIZE, (c / w) * DungeonTilemap.SIZE, DungeonTilemap.SIZE, DungeonTilemap.SIZE );
			e.pour( GRASS, GRASS_EVERY );
			grassVfx.add( e );
		}

		float cx = (lx + 0.5f) * DungeonTilemap.SIZE, cy = (ly + 0.5f) * DungeonTilemap.SIZE;
		pool = FxLight.pool( cx, cy + 4, 23, POOL_COLOR, POOL_LEVEL ).flicker( FxLight.BREATH ).persistent();
		Emitter air = Nature.air( null );
		if (air != null){
			for (int i = 0; i < OPEN_PETALS; i++){
				if (!FxBudget.keep( FxBudget.P2, cx, cy )) continue;
				float a = (float)(2 * Math.PI * i / OPEN_PETALS) + FxRandom.Float( -0.2f, 0.2f );
				float ground = cy + 4 + (float)Math.sin( a ) * 3;
				Nature.make( air, cx, cy + 2 ).look( FxFrames.PETAL_B ).paint( Nature.BLOSSOM.step( FxRandom.Float( 0f, 0.5f ) ) )
						.vel( (float)Math.cos( a ) * 24f, (float)Math.sin( a ) * 10f - 16f ).gravity( 90f ).drag( 2f )
						.ground( ground, FxParticle.SETTLE, 0 ).life( 0.9f ).alpha( 1f, 0, 0.4f ).priority( FxBudget.P2 );
			}
		}
		driftIn = FxRandom.Float( DRIFT_MIN, DRIFT_MAX );
	}

	/** How many of its cells pour leaves now. */
	public int grassEmitters(){
		return grassVfx == null ? 0 : grassVfx.size();
	}

	@Override
	public void place(int cell) {
		if (parent != null) parent.sendToBack(this);
		super.place(cell);
	}

	@Override
	public void turnTo(int from, int to) {
		//do nothing
	}

	@Override
	public void update() {
		visible = true;
		super.update();
		//now and then a petal lets go and drifts off
		if (grassVfx != null && (driftIn -= Game.elapsed) <= 0){
			driftIn = FxRandom.Float( DRIFT_MIN, DRIFT_MAX );
			Emitter air = Nature.air( null );
			float cx = x + width / 2f, cy = y + height / 2f;
			if (air != null && Fx.seen( cx, cy ) && FxBudget.keep( FxBudget.P3, cx, cy )){
				Nature.make( air, cx + FxRandom.Float( -4f, 4f ), cy ).look( FxFrames.PETAL_B )
						.paint( Nature.BLOSSOM.step( FxRandom.Float( 0f, 0.6f ) ) )
						.vel( FxRandom.Float( -6f, 6f ), -FxRandom.Float( 6f, 10f ) ).gravity( 10f )
						.sway( FxRandom.Float( 4f, 7f ), FxRandom.Float( 2f, 3f ) ).wind( 0.6f )
						.life( 2.4f ).alpha( 1f, 0.2f, 0.5f ).priority( FxBudget.P3 );
			}
		}
	}

	@Override
	public void die() {
		super.die();
		close();
	}

	@Override
	public void kill() {
		super.kill();
		close();
	}

	private void close(){
		if (grassVfx != null){
			for (Emitter e : grassVfx){
				e.on = false;
			}
			grassVfx = null;
		}
		if (pool != null){
			final FxLight p = pool;
			pool = null;
			Fx.post( () -> p.putOut( 0.4f ) );
		}
	}
}
