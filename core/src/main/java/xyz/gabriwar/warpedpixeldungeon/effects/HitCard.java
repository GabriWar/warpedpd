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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxOrbit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantState;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.RenderedText;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;

/**
 * One blow's card over the struck one (docs/fx/hit-card.md), beside FloatingText: a number row
 * - the left icon (the red X when any merged blow crit, else the blow's reason, else its damage
 * icon), the number summed over every damage the blow dealt this character, and "xN" from the
 * second merged blow - and over it a chip row of at most CHIPS icons (an enchantment's family, a
 * weapon type's silver mark, a line's gold one, a buff icon), the fourth slot "+N" when
 * there are more. It is born 3 px under its rest line (2 px over the sprite's top, 8 while a
 * mark circles its head), rises 10 px over 0.6 s and holds, follows the sprite while it lives,
 * and fades from 0.65 s after its last update (by 2 s after its birth whatever merges).
 *
 * Rapid blows by the same attacker on the same target within MERGE s update it in place (the
 * number sums, xN counts, chips are a union; a hop of the number row with a white flash of the
 * number for STAMP_FRAMES frames, at most once per 0.33 s;
 * a later crit stamps the X again, at most once per 0.33 s: for STAMP_FRAMES frames the X beside the
 * number and the number itself flash white, at their own size). Another
 * attacker's card pushes the older one up and starts its fade. A small card (another struck
 * character, or what flows back to the wielder) is a number in the small font and every chip it
 * carries, 0.7 s, rising 6 px. At most MOST cards live; past that the oldest fades at once.
 *
 * A blow with no number may still mark its card: a miss's dodge, block, parry or guard, or
 * immunity, drawn in the left slot with no number ("xN" counts the merged ones); a later merged
 * number moves that mark to the front of the chip row. Chips are text cells (7x8), buff icons in
 * a 9 px square, or a hero skill's own 16 px icon at half size in a 10 px square (SKILL_BASE + its
 * image), with nothing drawn behind them. A chip that comes after its blow closed (late) joins the struck one's live card, else
 * a small card of its own that later ones join too.
 *
 * Recycled from the scene's statuses (GameScene.Layer.STATUSES); its texts are laid out only when
 * they change, everything else is placed in update() with nothing allocated. Render thread.
 * Headless (no fonts) its words are left out and the rest runs the same.
 */
public class HitCard extends Component {

	/** Merging, rising, holding and fading, s and px. */
	public static final float MERGE = 0.4f, RISE = 10f, RISE_FOR = 0.6f, HOLD = 0.65f, FADE = 0.35f, LATEST = 2f,
			CHIPS_ONLY = 0.9f, SMALL_FOR = 0.7f, SMALL_RISE = 6f, BORN_BELOW = 3f, PUSHED_FADE = 0.25f, CAP_FADE = 0.15f;
	/** A chip landing; the merge hop; the X's stamp. */
	public static final float LAND_FOR = 0.05f, LAND_DROP = 3f, HOP_UP = 0.04f, HOP_SETTLE = 0.08f, HOP_PX = 2f,
			STAMP_GAP = 0.33f;
	/** The most cards at once; the chips a row shows; the X's stamp (its white flash), frames; how
	 *  far over the
	 *  sprite's top it rests (while a mark circles the head). */
	public static final int MOST = 12, CHIPS = 4, STAMP_FRAMES = 9, ABOVE = 2, ABOVE_MARK = 8;
	/** How white the X flashes on its stamp (Visual.lightness). */
	public static final float STAMP_LIGHT = 0.75f;
	/** "xN" and "+N": their colour. An echo's chip's alpha. */
	public static final int SMALL_COLOR = 0xD8D8D8;
	public static final float ECHO_ALPHA = 0.85f;
	/** A card on the struck one; a small card (a number or one chip); what flows back to a wielder. */
	public static final int MAIN = 0, SMALL = 1, GAIN = 2;
	/** A chip code from here on is a hero skill's icon: SKILL_BASE + Skill.image() (hero_skills.png,
	 *  16 px cells drawn at half size in a 10 px square). */
	public static final int SKILL_BASE = 1000;
	/** How long a late chip keeps the card it joins, s. */
	public static final float LATE_HOLD = 0.5f;
	/** A chip's kind: a text cell, a buff icon, a skill icon. */
	static final int TEXT_CHIP = 0, BUFF_CHIP = 1, SKILL_CHIP = 2;

	static final int MAX_CHIPS = 16;

	//the cards alive (render thread)
	private static final ArrayList<HitCard> live = new ArrayList<>();

	// ------------------------------------------------------------------ state

	CharSprite target;
	int attackerId, kind;
	boolean hasNumber, crit;
	int number, color, icon, count;
	int[] codes, tints;
	float[] lands;
	int chips;
	//its own clock: age, its last update, when its fade starts and how long it takes
	float age, updatedAt, fadeAt, fadeFor;
	//the last X stamp and merge hop (ages), the stamp's frames left, how far it was pushed up; when
	//the X shows (its crit's beat) and when a later merged crit stamps it again (ages, -1 none)
	float stampAt, hopAt, pushY, critAt, restampAt;
	int stampFrames;
	//a merge's white flash of the number (every merged hit shows landing), frames left
	int flashFrames;
	//its words need laying out again
	private boolean dirty;
	//its height as laid out
	private float tall;

	// ------------------------------------------------------------------ pictures

	private Image left;
	private Line num, cnt, plus;
	private Image[] chipIcons;
	//a buff's or a skill's chip: the square it stands in (9 or 10 px, its icon 1 px in, nothing
	//drawn behind it), 0 for a text cell
	private int[] box;
	private int[] chipKind;
	//a number-less card's mark in the left slot (a miss's verb, immunity), -1 none; a late chip's
	//own small card (later ones join it)
	int markIcon = -1;
	boolean late;

	private static TextureFilm buffFilm, skillFilm;

	@Override
	protected void createChildren(){
		codes = new int[MAX_CHIPS];
		tints = new int[MAX_CHIPS];
		lands = new float[MAX_CHIPS];
		chipIcons = new Image[CHIPS];
		box = new int[CHIPS];
		chipKind = new int[CHIPS];
		for (int i = 0; i < CHIPS; i++){
			chipIcons[i] = new Image( Assets.Effects.TEXT_ICONS );
			add( chipIcons[i] );
		}
		left = new Image( Assets.Effects.TEXT_ICONS );
		add( left );
		int z = zoom();
		num = new Line( 9 * z );
		cnt = new Line( 6 * z );
		plus = new Line( 6 * z );
		add( num );
		add( cnt );
		add( plus );
	}

	private static int zoom(){
		return Math.max( 1, PixelScene.defaultZoom );
	}

	// ------------------------------------------------------------------ making one

	//a card from the statuses, set up blank on a sprite; null off the scene
	private static HitCard make( CharSprite s, int kind ){
		HitCard c = GameScene.recycle( GameScene.Layer.STATUSES, HitCard.class );
		if (c == null) return null;
		c.revive();
		c.target = s;
		c.kind = kind;
		c.attackerId = -1;
		c.hasNumber = c.crit = false;
		c.number = 0;
		c.color = CharSprite.NEGATIVE;
		c.icon = -1;
		c.markIcon = -1;
		c.late = false;
		c.count = 1;
		c.chips = 0;
		c.age = c.updatedAt = 0f;
		c.fadeFor = FADE;
		c.fadeAt = kind == MAIN ? HOLD : SMALL_FOR - FADE;
		c.stampAt = c.hopAt = c.critAt = c.restampAt = -1f;
		c.stampFrames = c.flashFrames = 0;
		c.pushY = 0f;
		c.x = restX( s );
		c.y = restY( s );
		c.dirty = true;
		cap();
		live.add( c );
		return c;
	}

	//past MOST, the oldest not fading yet fades at once
	private static void cap(){
		int going = 0;
		HitCard oldest = null;
		for (int i = 0; i < live.size(); i++){
			HitCard c = live.get( i );
			if (c.age < c.fadeAt){
				going++;
				if (oldest == null || c.age > oldest.age) oldest = c;
			}
		}
		if (going >= MOST && oldest != null){
			oldest.fadeAt = oldest.age;
			oldest.fadeFor = CAP_FADE;
		}
	}

	/**
	 * The blow's card on `s` by attacker `attackerId`: merged into the live one when the same
	 * attacker updated it within MERGE s and it is not fading yet, else a new one (pushing an older
	 * card of another attacker up and starting its fade). `number` < 0: none. `chips` its codes
	 * (Blow.chip's), tints and land delays (s from now), the first n of them. `crit`: a crit of the
	 * blow, its X showing `critIn` s from now (its picture's beat). Render thread; null off the scene.
	 */
	public static HitCard show( CharSprite s, int attackerId, int number, int color, int icon, boolean crit, float critIn,
			int[] chipCodes, int[] chipTints, float[] chipLands, int n ){
		if (s == null) return null;
		EnchantState st = EnchantState.of( s );
		HitCard c = st.card;
		boolean merge = c != null && c.alive && c.target == s && c.kind == MAIN && c.attackerId == attackerId
				&& c.age - c.updatedAt <= MERGE && c.age < c.fadeAt;
		boolean pushed = false;
		float wasTall = merge ? c.tall : 0f;
		if (merge){
			c.count++;
			c.updatedAt = c.age;
			c.fadeAt = Math.max( c.age, Math.min( c.age + HOLD, LATEST ) );
			if (c.age - c.hopAt >= STAMP_GAP || c.hopAt < 0){
				c.hopAt = c.age;
				c.flashFrames = STAMP_FRAMES;
			}
		} else {
			if (c != null && c.alive && c.target == s && c.kind == MAIN){
				//another attacker's (or a spent) card: fading soon, pushed up out of the way once the
				//new one is laid out
				c.fadeAt = Math.min( c.fadeAt, c.age + PUSHED_FADE );
				pushed = true;
			}
			c = make( s, MAIN );
			if (c == null) return null;
			c.attackerId = attackerId;
			st.card = c;
		}
		if (number >= 0){
			if (!c.hasNumber){
				c.hasNumber = true;
				c.color = color;
				c.icon = icon;
				//the mark it bore (a miss, immunity) leads the chip row now: never lost
				if (c.markIcon >= 0){
					c.addChipFirst( c.markIcon, c.age );
					c.markIcon = -1;
				}
			}
			c.number += number;
		} else if (icon >= 0){
			//no number but a mark: the left slot's while it has neither, else a chip
			if (!c.hasNumber && c.markIcon < 0) c.markIcon = icon;
			else if (icon != c.markIcon) c.addChip( icon, 0, c.age );
		}
		if (crit){
			float at = c.age + Math.max( 0f, critIn );
			if (!c.crit){
				c.crit = true;
				c.critAt = at;
			} else {
				c.restampAt = at;
			}
		}
		for (int i = 0; i < n; i++) c.addChip( chipCodes[i], chipTints[i], c.age + chipLands[i] );
		if (!c.hasNumber && !merge) c.fadeAt = Math.max( 0f, CHIPS_ONLY - FADE );
		c.dirty = true;
		c.relayout();
		c.stampX();
		//the older cards on it clear the new one's whole height (a taller card grown by a merge
		//pushes them on by what it grew)
		float up = pushed ? c.tall + 2f : (merge ? c.tall - wasTall : 0f);
		if (up > 0f) pushOthers( s, c, up );
		return c;
	}

	//every other live card on the struck one moved up by `up` px, keeping their order
	private static void pushOthers( CharSprite s, HitCard c, float up ){
		for (int i = 0; i < live.size(); i++){
			HitCard o = live.get( i );
			if (o != c && o.alive && o.target == s && o.kind == MAIN) o.pushY -= up;
		}
	}

	/** Another struck character's small card: its number in the small font (none: -1) and one chip
	 *  (none: -1). Render thread; null off the scene. */
	public static HitCard small( CharSprite s, int number, int color, int chipCode, int chipTint ){
		return chipCode == -1 ? small( s, number, color, NO_CHIPS, NO_CHIPS, 0 )
				: small( s, number, color, new int[]{ chipCode }, new int[]{ chipTint }, 1 );
	}

	private static final int[] NO_CHIPS = {};

	/** Another struck character's small card: its number in the small font (none: -1) and every
	 *  chip it carries, the first n of codes (Blow.chip's) and tints. No merge. Render thread; null
	 *  off the scene. */
	public static HitCard small( CharSprite s, int number, int color, int[] chipCodes, int[] chipTints, int n ){
		if (s == null || (number < 0 && n <= 0)) return null;
		HitCard c = make( s, SMALL );
		if (c == null) return null;
		if (number >= 0){
			c.hasNumber = true;
			c.number = number;
			c.color = color;
		}
		for (int i = 0; i < n; i++) c.addChip( chipCodes[i], chipTints[i], 0f );
		c.relayout();
		return c;
	}

	/**
	 * A chip that comes after its blow closed (or with no blow: a buff taking hold, a skill's shout,
	 * a miss of a zap): it lands now on the struck one's live card, MAIN and not fading, keeping it
	 * LATE_HOLD s more; else on a late small card still standing on it; else on a new late small
	 * card. Render thread; null off the scene.
	 */
	public static HitCard late( CharSprite s, int code, int tint ){
		return late( s, new int[]{ code }, new int[]{ tint }, 1 );
	}

	/** The same for several chips at once, the first n (a miss's verb and its reason). */
	public static HitCard late( CharSprite s, int[] chipCodes, int[] chipTints, int n ){
		if (s == null || n <= 0) return null;
		EnchantState st = EnchantState.of( s );
		HitCard c = st.card;
		if (c == null || !c.alive || c.target != s || c.kind != MAIN || c.age >= c.fadeAt){
			c = null;
			for (int i = 0; i < live.size() && c == null; i++){
				HitCard o = live.get( i );
				if (o.late && o.alive && o.target == s && o.kind == SMALL && o.age < o.fadeAt) c = o;
			}
		}
		if (c == null){
			c = small( s, -1, CharSprite.NEGATIVE, chipCodes, chipTints, n );
			if (c != null) c.late = true;
			return c;
		}
		float wasTall = c.tall;
		for (int i = 0; i < n; i++) c.addChip( chipCodes[i], chipTints[i], c.age );
		c.fadeAt = Math.max( c.fadeAt, Math.min( c.age + LATE_HOLD, LATEST ) );
		c.relayout();
		if (c.kind == MAIN && c.tall > wasTall) pushOthers( s, c, c.tall - wasTall );
		return c;
	}

	/** What flows back to a wielder: "[icon] N" on him, summed with the hop into his live card of
	 *  the same icon within MERGE s; a second kind of gain stacks over the first. The icon is a chip
	 *  code: a text cell, a buff icon (a counter: fury, stone plates) or a skill's icon. Render
	 *  thread; null off the scene. */
	public static HitCard gain( CharSprite s, int icon, int amount, int color ){
		return gain( s, icon, amount, color, true );
	}

	/** A count over s where a word with a number floated ("Fury 3", "2 plates left", a chain's
	 *  "x3"): "[icon] N" drawn as a gain is, the live one of the same icon showing the new count
	 *  (not summed). Render thread; null off the scene. */
	public static HitCard counter( CharSprite s, int icon, int value, int color ){
		return gain( s, icon, value, color, false );
	}

	private static HitCard gain( CharSprite s, int icon, int amount, int color, boolean sum ){
		if (s == null || amount <= 0) return null;
		EnchantState st = EnchantState.of( s );
		for (int i = 0; i < st.gains.length; i++){
			HitCard c = st.gains[i];
			if (c != null && c.alive && c.target == s && c.kind == GAIN && c.icon == icon
					&& c.age - c.updatedAt <= MERGE && c.age < c.fadeAt){
				c.number = sum ? c.number + amount : amount;
				c.updatedAt = c.age;
				c.fadeAt = Math.max( c.age, Math.min( c.age + SMALL_FOR - FADE, LATEST ) );
				if (c.age - c.hopAt >= STAMP_GAP || c.hopAt < 0) c.hopAt = c.age;
				c.dirty = true;
				c.relayout();
				return c;
			}
		}
		HitCard c = make( s, GAIN );
		if (c == null) return null;
		c.hasNumber = true;
		c.number = amount;
		c.color = color;
		c.icon = icon;
		//over the live gains on him, as plain texts stack; in the first free slot (the first one's
		//when all three are taken)
		int slot = -1;
		for (int i = 0; i < st.gains.length; i++){
			HitCard o = st.gains[i];
			if (o != null && o.alive && o.target == s && o.kind == GAIN){
				c.pushY = Math.min( c.pushY, o.pushY - o.tall - 2f );
			} else if (slot < 0){
				slot = i;
			}
		}
		st.gains[Math.max( 0, slot )] = c;
		c.relayout();
		return c;
	}

	private void addChip( int code, int tint, float landAt ){
		for (int i = 0; i < chips; i++) if (codes[i] == code) return;
		if (chips >= MAX_CHIPS) return;
		codes[chips] = code;
		tints[chips] = tint;
		lands[chips] = landAt;
		chips++;
		dirty = true;
	}

	//a chip at the front of the row (a mark that gave its slot to a number), plain
	private void addChipFirst( int code, float landAt ){
		for (int i = 0; i < chips; i++) if (codes[i] == code) return;
		int last = Math.min( chips, MAX_CHIPS - 1 );
		for (int i = last; i > 0; i--){
			codes[i] = codes[i - 1];
			tints[i] = tints[i - 1];
			lands[i] = lands[i - 1];
		}
		codes[0] = code;
		tints[0] = 0;
		lands[0] = landAt;
		chips = last + 1;
		dirty = true;
	}

	// ------------------------------------------------------------------ reading it

	/** Where a plain text over `s` starts while its card lives: the card's top (world y), or NaN
	 *  when it has none. Render thread. */
	public static float textTop( CharSprite s ){
		EnchantState st = s == null ? null : EnchantState.get( s );
		HitCard c = st != null ? st.card : null;
		if (c == null || !c.alive || c.target != s) return Float.NaN;
		return c.cardTop();
	}

	/** The cards alive now. */
	public static int alive(){
		return live.size();
	}

	/** A new scene: none of the last one's cards count. */
	public static void reset(){
		live.clear();
	}

	public int number(){
		return hasNumber ? number : -1;
	}

	public int count(){
		return count;
	}

	public boolean crit(){
		return crit;
	}

	/** The left icon: a number-less card's mark (a miss's verb, immunity); else the red X once a
	 *  merged blow's crit showed, else the icon it was given. */
	public int leftIcon(){
		if (!hasNumber && markIcon >= 0) return markIcon;
		return xShown() ? FloatingText.CRIT : icon;
	}

	/** Whether this is a late chip's own small card. */
	public boolean isLate(){
		return late;
	}

	/** A shown chip's square (its width and height), 0 for a text cell's (none). Render thread. */
	public float chipFrame( int i ){
		return i < shownChips() && chipKind[i] != TEXT_CHIP ? box[i] : 0f;
	}

	/** A shown chip's place on the row (its left x) and its drawn scale. */
	public float chipX( int i ){
		return chipIcons[i].x;
	}

	public float chipScale( int i ){
		return chipIcons[i].scale.x;
	}

	/** Whether the left slot draws now (a number's icon, the X, or a mark). */
	public boolean leftShown(){
		return left.visible;
	}

	//whether its X shows now
	private boolean xShown(){
		return crit && age >= critAt;
	}

	//the X's first showing, or a later crit's, stamped (for STAMP_FRAMES frames) when its time
	//has come, at most once per STAMP_GAP s
	private void stampX(){
		boolean first = crit && stampAt < 0 && age >= critAt;
		boolean again = restampAt >= 0 && age >= restampAt;
		if (again) restampAt = -1f;
		if (first || (again && age - stampAt >= STAMP_GAP)){
			stampAt = age;
			stampFrames = STAMP_FRAMES;
			if (left != null && hasNumber && kind != SMALL){
				left.frame( FloatingText.iconFilm.get( FloatingText.CRIT ) );
				left.visible = true;
			}
		}
	}

	/** The chips in order, as many as chipCount(). */
	public int chip( int i ){
		return codes[i];
	}

	public int chipCount(){
		return chips;
	}

	/** A chip's tint (0: drawn plain), and how far it has landed now (0 not yet, 1 in place). */
	public int chipTint( int i ){
		return tints[i];
	}

	public float chipLanded( int i ){
		float land = age - lands[i];
		return land < 0f ? 0f : Math.min( 1f, land / LAND_FOR );
	}

	/** Its number's colour. */
	public int color(){
		return color;
	}

	/** Its number row's bottom as drawn (world y, whole pixels; the hop apart). */
	public float baseY(){
		return Math.round( y + rise() + pushY );
	}

	/** How many of its chips the row shows (the rest folded into "+N"). */
	public int shownChips(){
		return chips <= CHIPS ? chips : CHIPS - 1;
	}

	public int kind(){
		return kind;
	}

	public float age(){
		return age;
	}

	/** When its fade starts, s of its age. */
	public float fadeAt(){
		return fadeAt;
	}

	/** Its alpha now. */
	public float alpha(){
		return age < fadeAt ? 1f : Math.max( 0f, 1f - (age - fadeAt) / fadeFor );
	}

	/** The number row's lift by the merge hop now, px (0 or up). */
	public float hop(){
		if (hopAt < 0) return 0f;
		float t = age - hopAt;
		if (t < 0f) return 0f;
		if (t < HOP_UP) return -HOP_PX * t / HOP_UP;
		if (t < HOP_UP + HOP_SETTLE) return -HOP_PX * (1f - (t - HOP_UP) / HOP_SETTLE);
		return 0f;
	}

	/** Whether the X draws at 2x this frame (its stamp). */
	public boolean stamping(){
		return stampFrames > 0;
	}

	/** Whether the number flashes white this frame for a merged hit landing (not the X's stamp). */
	public boolean flashing(){
		return flashFrames > 0;
	}

	/** Its rise now from its rest line, px (positive down): born BORN_BELOW under it. */
	public float rise(){
		if (kind == MAIN){
			float u = Math.min( 1f, age / RISE_FOR );
			return BORN_BELOW - RISE * (1f - (1f - u) * (1f - u));
		}
		float u = Math.min( 1f, age / SMALL_FOR );
		return BORN_BELOW - SMALL_RISE * (1f - (1f - u) * (1f - u));
	}

	/** Its top as drawn (world y). */
	public float cardTop(){
		return y + rise() + pushY - tall;
	}

	/** The sprite it is on. */
	public CharSprite target(){
		return target;
	}

	// ------------------------------------------------------------------ where it rests

	private static float restX( CharSprite s ){
		return s.x + s.width() / 2f;
	}

	private static float restY( CharSprite s ){
		boolean mark = s.stunStars() || FxOrbit.get( s, FxOrbit.Slot.MARK ) != null;
		return s.y - (mark ? ABOVE_MARK : ABOVE);
	}

	// ------------------------------------------------------------------ a frame

	@Override
	public void update(){
		super.update();
		age += Game.elapsed;
		if (age >= fadeAt + fadeFor){
			kill();
			return;
		}
		//following its sprite while it lives (a dead or departed one's card stays put)
		if (target != null && target.parent != null && target.visible && target.ch != null && target.ch.isAlive()){
			x += (restX( target ) - x) * 0.5f;
			y += (restY( target ) - y) * 0.5f;
		}
		if (stampFrames > 0) stampFrames--;
		if (flashFrames > 0) flashFrames--;
		stampX();
		place();
	}

	@Override
	protected void layout(){
		//moved as a whole (GameScene.shiftWorldVisuals): every picture placed again
		if (num != null) place();
	}

	//the words laid out again (only when they change)
	private void relayout(){
		if (!dirty) return;
		dirty = false;
		boolean words = Game.platform != null;
		num.visible = cnt.visible = plus.visible = false;
		if (words && hasNumber){
			//a small card's number is in the small font, with no icon
			Line l = kind == SMALL ? cnt : num;
			l.say( Integer.toString( number ), color );
			l.visible = true;
		}
		if (words && count > 1 && kind == MAIN){
			cnt.say( "x" + count, SMALL_COLOR );
			cnt.visible = true;
		}
		if (words && chips > CHIPS){
			plus.say( "+" + (chips - (CHIPS - 1)), SMALL_COLOR );
			plus.visible = true;
		}
		int shown = shownChips();
		int rowH = 0;
		for (int i = 0; i < CHIPS; i++){
			boolean on = i < shown;
			chipIcons[i].visible = on;
			box[i] = 0;
			if (!on) continue;
			int code = codes[i];
			chipKind[i] = code >= SKILL_BASE ? SKILL_CHIP : code < 0 ? BUFF_CHIP : TEXT_CHIP;
			picture( chipIcons[i], code );
			if (chipKind[i] != TEXT_CHIP){
				box[i] = chipKind[i] == SKILL_CHIP ? 10 : 9;
			}
			chipIcons[i].resetColor();
			if (tints[i] != 0) chipIcons[i].hardlight( tints[i] );
			rowH = Math.max( rowH, chipKind[i] == SKILL_CHIP ? 10 : chipKind[i] == BUFF_CHIP ? 9 : FloatingText.ICON_HEIGHT );
		}
		left.visible = kind != SMALL && (hasNumber ? xShown() || icon != -1 : markIcon >= 0);
		if (left.visible) picture( left, leftIcon() );
		boolean row = numberRow();
		tall = (row ? FloatingText.ICON_HEIGHT : 0) + (shown > 0 ? (row ? 1 : 0) + rowH : 0);
		place();
	}

	//whether it has a number row: a number, or a mark in the left slot
	private boolean numberRow(){
		return hasNumber || (kind != SMALL && markIcon >= 0);
	}

	/** A picture of a chip code on an image: a text cell (7x8), a buff icon (7x7), a hero skill's
	 *  icon (16 px at half size); nothing for a code past its sheet. */
	public static void picture( Image img, int code ){
		img.scale.set( 1f );
		com.watabou.utils.RectF f;
		if (code >= SKILL_BASE){
			img.texture( Assets.Sprites.HERO_SKILLS );
			if (skillFilm == null) skillFilm = new TextureFilm( img.texture, 16, 16 );
			f = skillFilm.get( code - SKILL_BASE );
			img.scale.set( 0.5f );
		} else if (code < 0){
			img.texture( Assets.Interfaces.BUFFS_SMALL );
			if (buffFilm == null) buffFilm = new TextureFilm( img.texture, 7, 7 );
			f = buffFilm.get( -code - 1 );
		} else {
			img.texture( Assets.Effects.TEXT_ICONS );
			f = FloatingText.iconFilm.get( code );
		}
		if (f != null) img.frame( f );
		else img.frame( 0, 0, 0, 0 );
	}

	//how far the next chip starts after one of a kind, px: its picture and a 1 px gap
	private static float pitch( int chipKind ){
		return chipKind == SKILL_CHIP ? 11f : chipKind == BUFF_CHIP ? 10f : 8f;
	}

	//every picture placed from the card's state (no allocation)
	private void place(){
		float a = alpha();
		float baseY = baseY();
		float cx = x;
		//the number row
		float rowY = baseY - FloatingText.ICON_HEIGHT + Math.round( hop() );
		float iconW = left.visible ? left.width() + 1f : 0f;
		float numW = num.visible ? num.width() : 0f;
		float cntW = cnt.visible ? cnt.width() + (num.visible ? 2f : 0f) : 0f;
		float rowX = Math.round( cx - (iconW + numW + cntW) / 2f );
		if (left.visible){
			//the X's stamp: a white flash at its own size
			left.x = rowX;
			left.y = rowY;
			left.resetColor();
			if (stampFrames > 0) left.lightness( STAMP_LIGHT );
			left.alpha( a );
		}
		if (num.visible){
			num.tint( stampFrames > 0 || flashFrames > 0 ? 0xFFFFFF : color );
			num.moveTo( rowX + iconW, rowY + 1f );
			num.alphaAll( a );
		}
		if (cnt.visible){
			cnt.moveTo( rowX + iconW + numW + (numW > 0f ? 2f : 0f), rowY + 2f );
			cnt.alphaAll( a );
		}
		//the chip row, 1 px over the number row (or alone)
		int shown = shownChips();
		if (shown == 0 && !plus.visible) return;
		float chipBase = numberRow() ? rowY - 1f : baseY;
		float rowW = 0f;
		for (int i = 0; i < shown; i++) rowW += pitch( chipKind[i] );
		if (plus.visible) rowW += plus.width() + 1f;
		float px = Math.round( cx - rowW / 2f );
		for (int i = 0; i < shown; i++){
			float la = chipLanded( i );
			float drop = Math.round( -LAND_DROP * (1f - la) );
			Image ic = chipIcons[i];
			float chipA = a * la * (codes[i] >= FloatingText.hollow( FloatingText.F_EMBER )
					&& codes[i] <= FloatingText.hollow( FloatingText.F_HEX ) ? ECHO_ALPHA : 1f);
			if (chipKind[i] != TEXT_CHIP){
				//a buff's chip in its 9 px square, a skill's in 10; its icon 1 px in, no frame behind it
				float fs = box[i];
				ic.x = px + 1f;
				ic.y = chipBase - fs + 1f + drop;
				px += pitch( chipKind[i] );
			} else {
				ic.x = px;
				ic.y = chipBase - FloatingText.ICON_HEIGHT + drop;
				px += 8f;
			}
			ic.visible = la > 0f;
			ic.alpha( chipA );
		}
		if (plus.visible){
			plus.moveTo( px, chipBase - 6f );
			plus.alphaAll( a );
		}
	}

	@Override
	public void kill(){
		super.kill();
		live.remove( this );
		target = null;
	}

	//Group.kill killed every picture of it with it, and Gizmo.revive brings back the card alone:
	//a recycled card's icon, number and chips would not exist (Group.draw and update skip them)
	@Override
	public void revive(){
		super.revive();
		reviveAll( this );
	}

	private static void reviveAll( Group g ){
		for (Gizmo m : g.membersView()){
			if (m == null) continue;
			m.revive();
			if (m instanceof Group) reviveAll( (Group) m );
		}
	}

	//whether its pictures exist (Group.draw skips one that does not): the left icon, the number's
	//line, a chip's icon
	boolean leftExists(){
		return left.exists;
	}

	boolean numberExists(){
		return num.exists;
	}

	boolean chipExists( int i ){
		return chipIcons[i].exists;
	}

	@Override
	public void destroy(){
		live.remove( this );
		super.destroy();
	}

	// ------------------------------------------------------------------ a line of words

	//a block of words laid out once, then moved, tinted and faded by hand (no layout, no allocation)
	private static final class Line extends RenderedTextBlock {

		Line( int size ){
			super( size );
			setHightlighting( false );
			zoom( 1f / HitCard.zoom() );
		}

		void say( String text, int color ){
			text( text );
			hardlight( color );
			setPos( 0, 0 );
		}

		void moveTo( float nx, float ny ){
			Camera c = Camera.main;
			if (c != null){
				nx = PixelScene.align( c, nx );
				ny = PixelScene.align( c, ny );
			}
			float dx = nx - x, dy = ny - y;
			if (dx == 0f && dy == 0f) return;
			x = nx;
			y = ny;
			for (int i = 0; i < words.size(); i++){
				RenderedText w = words.get( i );
				w.x += dx;
				w.y += dy;
			}
		}

		void tint( int c ){
			for (int i = 0; i < words.size(); i++){
				RenderedText w = words.get( i );
				w.hardlight( c );
			}
		}

		void alphaAll( float a ){
			for (int i = 0; i < words.size(); i++) words.get( i ).alpha( a );
		}
	}
}
