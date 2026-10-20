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

package xyz.gabriwar.warpedpixeldungeon.effects.fx;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.RectF;

/**
 * An item's afterimage: a copy of an item's frame (an ItemSprite's as it shows now, or an item
 * image by its id) left where it was, in a colour, as light, sliding (dx, dy) px over its life
 * while it fades from its alpha to nothing - a boomerang's trail, a mirage's sliding double, a
 * round flicked back into the hand. Afterimage.of takes a character's sprite; this is the same for
 * an item.
 *
 * At most MOST live at once, and never more than FxBudget.afterimageCap() together with the
 * characters' afterimages (Afterimage.live()): past either, this one takes the place of its own
 * oldest, and with none of its own to replace there is no room (null). Pooled in the light layer,
 * added. Render thread; null off the game's scene.
 */
public class ItemGhost extends Image {

	/** The most item ghosts at once. */
	public static final int MOST = 8;

	private static final ItemGhost[] live = new ItemGhost[MOST];
	private static int count;
	private static long serials;

	private float peak, life, age, x0, y0, dx, dy;
	private long serial;
	private final RectF uv = new RectF();

	public ItemGhost(){
		super( Assets.Sprites.ITEMS );
	}

	/** A new scene: none of the last one's. */
	public static synchronized void reset(){
		for (int i = 0; i < count; i++) live[i] = null;
		count = 0;
	}

	public static synchronized int live(){
		return count;
	}

	/** The sprite's frame as it shows now, in `color` at `alpha`, sliding (dx, dy) over `life` s. */
	public static ItemGhost of( ItemSprite s, int color, float alpha, float life, float dx, float dy ){
		if (s == null || s.texture == null || s.frame() == null) return null;
		if (!room()) return null;
		ItemGhost g = GameScene.recycle( GameScene.Layer.LIGHTS, ItemGhost.class );
		if (g == null) return null;
		RectF f = s.frame();
		g.texture = s.texture;
		g.uv.set( f.left, f.top, f.right, f.bottom );
		g.frame( g.uv );
		g.flipHorizontal = s.flipHorizontal;
		g.flipVertical = s.flipVertical;
		g.scale.set( s.scale.x, s.scale.y );
		g.origin.set( s.origin.x, s.origin.y );
		g.angle = s.angle;
		g.setup( s.x, s.y, color, alpha, life, dx, dy );
		enter( g );
		return g;
	}

	/** An item image (ItemSpriteSheet) with its middle at (x, y), turned `angle` degrees, in
	 *  `color` at `alpha`, sliding (dx, dy) over `life` s. */
	public static ItemGhost of( int image, float x, float y, float angle, int color, float alpha, float life, float dx, float dy ){
		if (!room()) return null;
		ItemGhost g = GameScene.recycle( GameScene.Layer.LIGHTS, ItemGhost.class );
		if (g == null) return null;
		RectF f = ItemSpriteSheet.film.get( image );
		if (f == null){
			g.kill();
			return null;
		}
		g.texture( Assets.Sprites.ITEMS );
		g.uv.set( f.left, f.top, f.right, f.bottom );
		g.frame( g.uv );
		g.flipHorizontal = g.flipVertical = false;
		g.scale.set( 1 );
		g.origin.set( g.width / 2f, g.height / 2f );
		g.angle = angle;
		g.setup( x - g.width / 2f, y - g.height / 2f, color, alpha, life, dx, dy );
		enter( g );
		return g;
	}

	private void setup( float x, float y, int color, float alpha, float life, float dx, float dy ){
		x0 = x;
		y0 = y;
		this.x = Math.round( x );
		this.y = Math.round( y );
		this.dx = dx;
		this.dy = dy;
		resetColor();
		hardlight( ((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f );
		peak = alpha;
		am = alpha;
		this.life = Math.max( 0.01f, life );
		age = 0;
		visible = true;
		revive();
	}

	@Override
	public void update(){
		super.update();
		if (Emitter.freezeEmitters && Game.timeTotal > 1) return;
		age += Game.elapsed;
		if (age >= life){
			kill();
			return;
		}
		float p = age / life;
		//whole pixels: it slides a pixel at a time
		x = Math.round( x0 + dx * p );
		y = Math.round( y0 + dy * p );
		am = peak * (1f - p);
	}

	@Override
	public void draw(){
		Blending.setLightMode();
		super.draw();
		Blending.setNormalMode();
	}

	@Override
	public void kill(){
		super.kill();
		leave( this );
	}

	//room for one more: under MOST and, with the characters' afterimages, under the shared cap;
	//else its own oldest goes (none of its own: no room). The afterimages are counted first,
	//outside this one's lock (theirs reads this count: Afterimage.of)
	private static boolean room(){
		int others = Afterimage.live();
		synchronized (ItemGhost.class){
			if (count < MOST && count + others < FxBudget.afterimageCap()) return true;
			ItemGhost oldest = null;
			for (int i = 0; i < count; i++) if (oldest == null || live[i].serial < oldest.serial) oldest = live[i];
			if (oldest == null) return false;
			oldest.kill();
			return true;
		}
	}

	private static synchronized void enter( ItemGhost g ){
		g.serial = ++serials;
		for (int i = 0; i < count; i++) if (live[i] == g) return;
		if (count < live.length) live[count++] = g;
	}

	private static synchronized void leave( ItemGhost g ){
		for (int i = 0; i < count; i++){
			if (live[i] == g){
				live[i] = live[--count];
				live[count] = null;
				return;
			}
		}
	}
}
