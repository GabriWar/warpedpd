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

package xyz.gabriwar.warpedpixeldungeon.effects.skillfx;

import com.watabou.noosa.Game;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxCurves;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;

/**
 * Sanctuary's consecrated ground (docs/fx/skills.md, round 2): one seal of holy light lying on the
 * floor round the circle's middle, never a glyph a cell. Its painting (effects/sanctum.png, from
 * tools/assets/draw_sanctum.py) has a rim with a faint halo, a band of runes with a bead of light
 * between each two, an inner ring and a star of light (four long points on the axes; on the wide
 * seal four short ones on the diagonals and a small ring about the feet). Each cell of the field
 * draws its own 16 x 16 of it, so a wall in the circle has none of it and the seal is cut there.
 *
 * Its runes are the kit's RUNE_5 glyphs, RUNES[reach] of them on the band (RUNE_R px out), each
 * its own glyph for good: a rune never changes glyph, never moves, never turns, never scales. The
 * whole seal breathes (BREATH of its alpha over BREATH_PERIOD s) and a slow reading light goes
 * round the band (once every READ_PERIOD s), each rune brightening toward the ramp's pale as it
 * passes and dimming again: everything a smooth function of time, nothing stepped or toggled.
 * Freshly consecrated it opens over OPEN s and its runes are inscribed one after another round
 * the band from the top (INSCRIBE s in all, each fading in over RUNE_IN s); let go of, it fades
 * over FADE s. A seal of one reach is 9 or 25 quads and 8 or 12 runes. FxRandom only (none
 * drawn here), no allocation per frame.
 */
public final class Sanctum {

	/** The painting, its two seals (reach 1: 3 x 3 cells; reach 2: 5 x 5), their runes and the
	 *  radius of the rune band, px (draw_sanctum.py's SEALS). */
	public static final String TEXTURE = "effects/sanctum.png";
	public static final int[][] SEAL = { { 0, 0, 48, 48 }, { 48, 0, 80, 80 } };
	public static final int[] RUNES = { 8, 12 };
	public static final float[] RUNE_R = { 17.5f, 33.5f };
	/** Each rune's glyph (RUNE_5's hourglass, thorn, ring, ladder), read clockwise from the top. */
	static final int[][] WORDS = { { 0, 2, 1, 3, 2, 0, 3, 1 }, { 0, 2, 1, 3, 2, 0, 3, 1, 0, 3, 2, 1 } };

	/** The seal's light and its alpha, its breath (a share of its alpha) and how long one takes, s. */
	public static final int LIT = 0xF1D99C;
	public static final float ALPHA = 0.8f, BREATH = 0.1f, BREATH_PERIOD = 3.2f;
	/** The runes at rest (in RUNE_HOLY's body), and as the reading light passes (up to READ more,
	 *  toward its pale); the reading light's round, s, and how narrow its light is. */
	public static final float RUNE_A = 0.7f, READ = 0.3f, READ_PERIOD = 4.8f;
	public static final int READ_NARROW = 8;
	/** Opening (s), the runes' inscription (all of them, s, from INSCRIBE_AT s), each rune's
	 *  fade in (s), and the fade when let go of (s). */
	public static final float OPEN = 0.4f, INSCRIBE_AT = 0.1f, INSCRIBE = 0.5f, RUNE_IN = 0.3f, FADE = 0.4f;

	private static final Ramp RUNE = SkillPalette.RUNE_HOLY;

	/** Its seal's reach (1 or 2) and its middle cell. */
	public final int reach, middle;
	/** Its pieces: the seal's cut a cell (null for a cell it cannot hold), its runes. */
	final FxPic[] seal;
	final FxPic[] runes;
	private final float[] runeAngle;
	private final boolean fresh;

	/** The seal for a field's cells: its middle the origin when the field holds it, else the cell
	 *  nearest the cells' mean; its reach the farthest cell's (1 or 2). Any thread. */
	public Sanctum( int[] cells, int origin, boolean fresh ){
		this.fresh = fresh;
		int w = Dungeon.level == null ? 1 : Dungeon.level.width();
		middle = middle( cells, origin, w );
		int far = 0;
		for (int c : cells) far = Math.max( far, Math.max( Math.abs( c % w - middle % w ), Math.abs( c / w - middle / w ) ) );
		reach = Math.max( 1, Math.min( 2, far ) );
		int[] s = SEAL[reach - 1];
		seal = new FxPic[cells.length];
		for (int i = 0; i < cells.length; i++){
			int dx = cells[i] % w - middle % w, dy = cells[i] / w - middle / w;
			if (Math.abs( dx ) > reach || Math.abs( dy ) > reach) continue;
			FxPic p = new FxPic( TEXTURE );
			p.crop( s, (dx + reach) * 16, (dy + reach) * 16, 16, 16 );
			p.additive = true;
			p.lit( LIT );
			p.place( SkillVfx.cx( cells[i] ), SkillVfx.cy( cells[i] ) );
			p.am = 0;
			seal[i] = p;
		}
		int n = RUNES[reach - 1];
		runes = new FxPic[n];
		runeAngle = new float[n];
		float mx = SkillVfx.cx( middle ), my = SkillVfx.cy( middle ), r = RUNE_R[reach - 1];
		for (int k = 0; k < n; k++){
			runeAngle[k] = angle( k, n );
			FxPic p = new FxPic( SkillVfx.SHEET );
			p.show( FxFrames.RUNE_5[WORDS[reach - 1][k]] );
			p.additive = true;
			p.lit( RUNE.stop( 1 ) );
			p.place( mx + (float) Math.cos( runeAngle[k] ) * r, my + (float) Math.sin( runeAngle[k] ) * r );
			p.am = 0;
			p.visible = false;
			runes[k] = p;
		}
		//shown again (a stairway, a save): whole from its first frame
		if (!fresh) pose( SETTLED, Game.timeTotal, 1f, cells );
	}

	/** An age by which a fresh seal is open and inscribed, s. */
	static final float SETTLED = INSCRIBE_AT + INSCRIBE + RUNE_IN;

	/** The k-th of n runes' angle (radians, 0 right, PI/2 down): the first just clockwise of the
	 *  top, a bead of light between each two. */
	public static float angle( int k, int n ){
		return (float) (-Math.PI / 2 + Math.PI / n + 2 * Math.PI * k / n);
	}

	//the origin when the field holds it, else the cell nearest the mean of its cells
	static int middle( int[] cells, int origin, int w ){
		for (int c : cells) if (c == origin) return origin;
		if (cells.length == 0) return origin;
		float mx = 0, my = 0;
		for (int c : cells){
			mx += (c % w) / (float) cells.length;
			my += (c / w) / (float) cells.length;
		}
		int best = cells[0];
		float d = Float.MAX_VALUE;
		for (int c : cells){
			float e = (c % w - mx) * (c % w - mx) + (c / w - my) * (c / w - my);
			if (e < d){
				d = e;
				best = c;
			}
		}
		return best;
	}

	// ------------------------------------------------------------------ its light, by time

	/** The breath: 1 +- BREATH, a sine of the scene's clock. */
	public static float breath( float time ){
		return 1f + BREATH * (float) Math.sin( 2 * Math.PI * time / BREATH_PERIOD );
	}

	/** The seal's alpha `age` s after it was made (fresh: opening), at the scene's `time`. */
	public static float sealAlpha( float age, float time, boolean fresh ){
		float open = fresh ? FxCurves.smooth( Math.min( 1f, Math.max( 0f, age / OPEN ) ) ) : 1f;
		return ALPHA * open * breath( time );
	}

	/** How lit the k-th of n runes is by the reading light (0 to 1): the light at the angle the
	 *  clock has carried it round to, falling off as cos^READ_NARROW of the way between. */
	public static float read( int k, int n, float time ){
		double way = angle( k, n ) - (-Math.PI / 2 + 2 * Math.PI * time / READ_PERIOD);
		float c = (float) Math.cos( way );
		return c <= 0 ? 0f : (float) Math.pow( c, READ_NARROW );
	}

	/** How far the k-th of n runes is inscribed `age` s after a fresh seal was made (0 to 1). */
	public static float inscribed( int k, int n, float age, boolean fresh ){
		if (!fresh) return 1f;
		float at = INSCRIBE_AT + INSCRIBE * k / n;
		return FxCurves.smooth( Math.min( 1f, Math.max( 0f, (age - at) / RUNE_IN ) ) );
	}

	/** The k-th rune's alpha. */
	public static float runeAlpha( int k, int n, float age, float time, boolean fresh ){
		return inscribed( k, n, age, fresh ) * (RUNE_A * breath( time ) + READ * read( k, n, time ));
	}

	/** The k-th rune's colour: RUNE_HOLY's body toward its pale as it is read or inscribed. */
	public static int runeColor( int k, int n, float age, float time, boolean fresh ){
		float pale = Math.max( read( k, n, time ), 1f - inscribed( k, n, age, fresh ) );
		return Ramp.lerp( RUNE.stop( 1 ), RUNE.stop( 0 ), pale );
	}

	// ------------------------------------------------------------------ posing

	/** Poses it for a frame (render thread): `age` s since it was made, the scene's `time`, `fade`
	 *  its share left (1 but while it fades), `cells` the field's (a rune shows where its cell is
	 *  one of them and in sight; a cut where its cell is in sight). */
	public void pose( float age, float time, float fade, int[] cells ){
		float a = Math.min( 1f, sealAlpha( age, time, fresh ) * fade );
		for (FxPic p : seal){
			if (p == null) continue;
			p.takeSlide();
			p.place( p.midX(), p.midY() );
			p.am = a;
			p.visible = a > 0.004f && SkillVfx.seen( Fx.cellAt( p.midX(), p.midY() ) );
		}
		int n = runes.length;
		for (int k = 0; k < n; k++){
			FxPic p = runes[k];
			p.takeSlide();
			p.place( p.midX(), p.midY() );
			int cell = Fx.cellAt( p.midX(), p.midY() );
			float ra = Math.min( 1f, runeAlpha( k, n, age, time, fresh ) * fade );
			p.am = ra;
			p.lit( runeColor( k, n, age, time, fresh ) );
			p.visible = ra > 0.004f && holds( cells, cell ) && SkillVfx.seen( cell );
		}
	}

	private static boolean holds( int[] cells, int cell ){
		for (int c : cells) if (c == cell) return true;
		return false;
	}

	/** The seal's cut for the field's i-th cell (null past its reach). */
	public FxPic cut( int i ){
		return seal[i];
	}

	/** Its runes, round the band clockwise from the top. */
	public FxPic[] runes(){
		return runes;
	}

	/** Whether it was made fresh (opening and inscribed) rather than shown again. */
	public boolean fresh(){
		return fresh;
	}
}
