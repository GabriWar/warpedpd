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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Lit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.Arc;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.ArcFlash;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

//the glyph of knowing: twenty dots gathering into a question mark (its twenty draws of the
//game's Random in the constructor, as ever), on a soft glow of its source's colour (a well's
//water, the default blue; a holy one's gold given by its caller), twinkling each in its own
//phase, a star snapping on as the glyph completes, then dissolving into motes rising off it.
//Drawn over the night's tint; let go of when done.
public class Identification extends Group implements Lit {

	private static int[] DOTS = {
		-1, -3,
		 0, -3,
		+1, -3,
		-1, -2,
		+1, -2,
		+1, -1,
		 0,  0,
		+1,  0,
		 0, +1,
		 0, +3
	};

	/** Its colours: the default blue, a well's water, the holy. */
	public static final int BLUE = 0x4488CC, WATER = 0x60D0D0, HOLY = 0xFFE9A8;
	/** When the glyph is whole (a star snaps on), when it dissolves; its glow's alpha; its motes. */
	public static final float WHOLE_AT = 1.0f, DISSOLVE_AT = 1.4f, GLOW = 0.3f;
	public static final int MOTES = 4;
	/** Its pictures at most: the dots, the glow, the star (the motes come as the dots go). */
	public static final int DRAWS = 20 + 1 + 1 + 2;

	private final PointF p;
	private int color = -1;
	private float age;
	private boolean started, whole, dissolved;
	private Gizmo carrier;

	public Identification( PointF p ) {
		this.p = p;
		for (int i=0; i < DOTS.length; i += 2) {
			add( new Speck( p.x, p.y, DOTS[i], DOTS[i+1] ) );
			add( new Speck( p.x, p.y, DOTS[i], DOTS[i+1] ) );
		}
	}

	/** In a colour of its source's (HOLY, WATER, BLUE). */
	public Identification( PointF p, int color ) {
		this( p );
		this.color = color;
	}

	//its colour: given, else a well's water where it rises over a well, else blue
	private int color(){
		if (color != -1) return color;
		int cell = Fx.cellAt( p.x, p.y );
		if (cell >= 0 && Dungeon.level != null && Dungeon.level.map[cell] == Terrain.WELL) return WATER;
		return BLUE;
	}

	@Override
	public void update() {
		if (!started) start();
		super.update();
		age += Game.elapsed;
		if (!whole && age >= WHOLE_AT){
			whole = true;
			ArcFlash s = ArcFlash.light( p.x, p.y, FxFrames.Light.STAR4_9_A, 0xFFFFFF, 0.9f, 0.25f );
			if (s != null) s.swap( FxFrames.Light.STAR4_9_B, 0.08f ).fade( 0, 0.15f );
		}
		if (!dissolved && age >= DISSOLVE_AT){
			dissolved = true;
			for (int i = 0; i < MOTES; i++){
				FxParticle m = Arc.light( p.x + FxRandom.Float( -3f, 3f ), p.y + FxRandom.Float( -6f, 6f ), FxBudget.P2 );
				if (m == null) break;
				m.look( FxFrames.MOTE_1 ).paint( color() ).vel( FxRandom.Float( -2f, 2f ), -FxRandom.Float( 6f, 10f ) )
						.drag( 0.5f ).life( FxRandom.Float( 0.5f, 0.8f ) ).alpha( 1f, 0.05f, 0.3f ).snap();
			}
		}
		if (countLiving() == 0) {
			if (carrier != null) carrier.killAndErase();
			Fx.dispose( this );
		}
	}

	//its first frame (render thread): over the tint, its dots in its colour, its glow
	private void start(){
		started = true;
		int c = color();
		for (int i = 0; i < length; i++){
			Gizmo g = members.get( i );
			if (g instanceof Speck) ((Speck) g).paint( c );
		}
		carrier = GameScene.carryLight( this );
		FxLight l = Arc.glow( p.x, p.y, 23, c, GLOW );
		if (l != null) l.life( 0.3f, 0.9f, 0.6f );
	}

	@Override
	public void draw() {
		if (carrier != null && carrier.exists && carrier.parent != null) return;
		drawLit();
	}

	@Override
	public void drawLit() {
		Blending.setLightMode();
		super.draw();
		Blending.setNormalMode();
	}

	public static class Speck extends PixelParticle {

		private static final int COLOR = 0x4488CC;
		private static final int SIZE = 3;

		private float phase = -1;

		public Speck( float x0, float y0, int mx, int my ) {

			super();
			color( COLOR );

			float x1 = x0 + mx * SIZE;
			float y1 = y0 + my * SIZE;

			PointF p = new PointF().polar( Random.Float( 2 * PointF.PI ), 8 );
			x0 += p.x;
			y0 += p.y;

			float dx = x1 - x0;
			float dy = y1 - y0;

			x = x0;
			y = y0;
			speed.set( dx, dy );
			acc.set( -dx / 4, -dy / 4 );

			left = lifespan = 2f;
		}

		void paint( int c ){
			color( c );
		}

		@Override
		public void update() {
			super.update();
			if (phase < 0) phase = FxRandom.angle();

			am = 1 - Math.abs( left / lifespan - 0.5f ) * 2;
			am *= am;
			//a whole-pixel dot, 1 to 3, twinkling in its own phase
			size( Math.max( 1, Math.round( am * SIZE ) ) );
			am *= 0.8f + 0.2f * (float)Math.sin( phase + (lifespan - left) * 14f );
		}

		@Override
		public void draw() {
			//slow as it settles into the glyph: on whole pixels
			if (speed.x * speed.x + speed.y * speed.y < 144f) {
				float ox = x, oy = y;
				x = Math.round( x );
				y = Math.round( y );
				super.draw();
				x = ox;
				y = oy;
			} else {
				super.draw();
			}
		}
	}
}
