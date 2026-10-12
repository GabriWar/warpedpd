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

package xyz.gabriwar.warpedpixeldungeon.homestead.animals;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Group;
import com.watabou.utils.PointF;

/**
 * The rope on a roped or tied animal (Leashed), drawn every frame from its neck to the hero's hand
 * or to its post: a run of pixel dots in two rope browns, sagging when slack (SAG pixels in the
 * middle when the two ends are close) and pulled straight as it tightens (TAUT pixels apart).
 * It follows the sprites wherever they are drawn, so a window moving under them moves it too;
 * hidden while either end is out of sight, gone with the tether.
 */
public class RopeLine extends Group {

	/** The two browns of the twist, the gap between dots, the slack rope's sag and the length at
	 *  which it is taut (pixels). */
	public static final int LIGHT = 0xFFC8A060, DARK = 0xFF8C6A3C;
	public static final float GAP = 2f, SAG = 4f, TAUT = 40f;
	//the most dots a rope is drawn with: the leash snaps past six cells
	private static final int MAX_DOTS = 64;

	private final Leashed tether;
	private final ColorBlock[] dots = new ColorBlock[MAX_DOTS];

	public RopeLine( Leashed tether ){
		this.tether = tether;
		for (int i = 0; i < MAX_DOTS; i++){
			dots[i] = new ColorBlock( 1, 1, i % 2 == 0 ? LIGHT : DARK );
			dots[i].visible = false;
			add( dots[i] );
		}
	}

	@Override
	public void update(){
		super.update();
		Char animal = tether.target;
		if (animal == null || animal.buff( Leashed.class ) != tether || tether.mode == Leashed.Mode.FREE
				|| animal.sprite == null || animal.sprite.parent == null){
			killAndErase();
			return;
		}
		PointF to = anchor();
		CharSprite s = animal.sprite;
		if (to == null || !s.visible){
			hideAll( 0 );
			return;
		}
		//from the beast's neck: a little over the middle of its frame
		float fx = s.x + s.width() / 2f, fy = s.y + s.height() * 0.45f;
		float dx = to.x - fx, dy = to.y - fy;
		float len = (float) Math.hypot( dx, dy );
		int n = Math.min( MAX_DOTS, Math.max( 2, Math.round( len / GAP ) ) );
		float sag = SAG * Math.max( 0f, 1f - len / TAUT );
		for (int i = 0; i < n; i++){
			float t = i / (float) (n - 1);
			ColorBlock d = dots[i];
			d.x = fx + dx * t;
			//a quadratic droop, deepest midway
			d.y = fy + dy * t + sag * 4f * t * (1f - t);
			d.visible = true;
		}
		hideAll( n );
	}

	private void hideAll( int from ){
		for (int i = from; i < MAX_DOTS; i++) dots[i].visible = false;
	}

	//where the rope's other end is: the hero's hand, the post's top; null while it cannot be seen
	private PointF anchor(){
		if (tether.mode == Leashed.Mode.LEASHED){
			Char hero = Dungeon.hero;
			if (hero == null || hero.sprite == null || !hero.sprite.visible) return null;
			return new PointF( hero.sprite.x + hero.sprite.width() / 2f, hero.sprite.y + hero.sprite.height() * 0.55f );
		}
		Level level = Dungeon.level;
		if (!(level instanceof OverworldLevel)) return null;
		int post = ((OverworldLevel) level).localCell( tether.postX, tether.postY );
		if (post == -1 || level.heroFOV == null || !level.heroFOV[post]) return null;
		PointF c = DungeonTilemap.tileCenterToWorld( post );
		//the post's ring stands a little above its cell's middle
		return new PointF( c.x, c.y - 3f );
	}
}
