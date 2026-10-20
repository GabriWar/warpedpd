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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.ItemGhost;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.HeavyBoomerang;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.JupitersWraith;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.RectF;

/**
 * A thrown or shot item's flight (ItemSprite in FLIGHT mode, set as MissileSprite.reset starts it):
 *
 * - an enchanted one: one streak of its family (STREAK_FLAME for embers, else STREAK_TAPER, its
 *   thick head on the item) at STREAK_ALPHA in its colour; one mote of its motif shed every SHED_PX
 *   flown (MotifFx, P2), MOST_SHEDS a flight, two looks taking turns, a droplet over water landing
 *   in it; an emissive family's passing light following the head. At most MOST_TRAILS full trails
 *   at once: later missiles draw the streak alone. A launcher's shot and a bow's arrow trail their
 *   launcher's magic (ItemLooks).
 * - any missile over a solid cell of its own path (a throw through a wall: Projecting's, an alpha
 *   wall throw's) draws at PHASED_ALPHA, its sheds turn to violet arcane motes, an upright ring
 *   ripples where it enters the wall and where it leaves (two frames, then gone over RIPPLE_FADE),
 *   and it melds in. Its path is the throw's cells (Ballistica, as the game flies it), not every
 *   cell its straight line of pixels clips: a throw past a wall's corner never phases.
 * - a boomerang (Boomerang, Heavy Boomerang, Jupiter's Wraith) leaves a dotted path, a mote every
 *   SHED_PX hanging DOT_HANG s (MOST_DOTS a leg), and GHOSTS copies of itself fading along it, out
 *   and back.
 *
 * Everything follows the item's own alpha (a boomerang's return fades in). It is the streak quad
 * itself, in the light layer, and steers itself from the missile each frame (render thread); it
 * ends with the flight (end, from MissileSprite.onComplete) or as soon as the missile is gone.
 * Pictures use FxRandom only; no game Random.
 */
public class EnchantTrail extends Image {

	/** Full trails at once; motes a flight; a boomerang's dots a leg and its ghosts. */
	public static final int MOST_TRAILS = 4, MOST_SHEDS = 8, MOST_DOTS = 6, GHOSTS = 4;
	/** Px flown between two sheds (and two dots). */
	public static final float SHED_PX = 12f;
	/** The streak's alpha, the passing light's level, a missile's alpha inside a wall. */
	public static final float STREAK_ALPHA = 0.5f, LIGHT_LEVEL = 0.35f, PHASED_ALPHA = 0.45f;
	/** A dot's hang, a ghost's alpha and life, a ripple's two frames and fade. */
	public static final float DOT_HANG = 0.4f, GHOST_ALPHA = 0.35f, GHOST_LIFE = 0.3f, RIPPLE_HOLD = 2 / 60f, RIPPLE_FADE = 0.2f;

	/** Where a meld's sound goes (SpatialSound in the game; a test's ear in a test). */
	static HeapLook.Ear ear = SpatialSound::play;

	//the full trails alive now (a slot whose trail is gone is free: a scene change ends none)
	private static final EnchantTrail[] fulls = new EnchantTrail[MOST_TRAILS];

	private ItemSprite owner;
	//the owner's flight it trails (a recycled missile's next flight is another's)
	private int flight;
	private boolean isFull, boomerang, streak;
	//the flight: where it began and ends (cells), how long it is, how far it has come
	private int fromCell, toCell, lastCell;
	private float length, flown, lastX, lastY;
	private float nextShed, nextDot, nextGhost;
	private int sheds, dots, ghosts;
	private boolean phasing;
	//the cells of its path between where it began and ends (a flight's; the array kept and grown)
	private int[] path = new int[16];
	private int pathLen;
	private FxLight passing;
	private int passingSetups;
	private boolean ended;
	private int[][] frames;
	private int frameShown = -1;
	private final RectF uv = new RectF();

	public EnchantTrail(){
		super( Assets.Effects.FX_SHEET );
	}

	/** Full trails alive now (tests). */
	public static int fullTrails(){
		int n = 0;
		for (EnchantTrail t : fulls) if (live( t )) n++;
		return n;
	}

	private static boolean live( EnchantTrail t ){
		return t != null && t.alive && t.isFull && t.parent != null;
	}

	//a full trail's place, if one is free
	private boolean claimFull(){
		for (int i = 0; i < fulls.length; i++){
			if (!live( fulls[i] )){
				fulls[i] = this;
				return true;
			}
		}
		return false;
	}

	/** A missile's flight from (fx, fy) to (tx, ty), world px, made (or recycled) in the light layer;
	 *  null off the scene. Render thread. */
	public static EnchantTrail attach( ItemSprite s, float fx, float fy, float tx, float ty ){
		EnchantTrail t = GameScene.recycle( GameScene.Layer.LIGHTS, EnchantTrail.class );
		if (t == null) return null;
		t.setup( s, fx, fy, tx, ty );
		return t;
	}

	private void setup( ItemSprite s, float fx, float fy, float tx, float ty ){
		owner = s;
		flight = s.flights();
		ended = false;
		Item item = s.item();
		boomerang = item instanceof HeavyBoomerang || item instanceof JupitersWraith;
		streak = s.looks().a != null;
		isFull = false;
		if (streak) isFull = claimFull();
		fromCell = Fx.cellAt( fx, fy + RAISE );
		toCell = Fx.cellAt( tx, ty + RAISE );
		path( fromCell, toCell );
		float dx = tx - fx, dy = ty - fy;
		length = (float)Math.sqrt( dx * dx + dy * dy );
		lastX = s.x + s.width() / 2f;
		lastY = s.y + s.height() / 2f;
		lastCell = Fx.cellAt( lastX, lastY + RAISE );
		flown = 0;
		nextShed = nextDot = SHED_PX;
		nextGhost = length / (GHOSTS + 1);
		sheds = dots = ghosts = 0;
		//a flight that starts inside a wall (thrown out of one) is not passing through it there
		phasing = false;
		passing = null;
		EnchantLook a = s.looks().a;
		frames = a != null && a.fam == Family.EMBER ? FxFrames.STREAK_FLAME : null;
		frameShown = -1;
		show( frames != null ? frames[0] : FxFrames.STREAK_TAPER );
		resetColor();
		am = 0;
		visible = false;
		revive();
	}

	//the missile's middle lies this far over its cell's (it flies at the raised tile centre)
	static final float RAISE = 6f;

	private void show( int[] f ){
		float tw = texture.width, th = texture.height;
		uv.set( f[0] / tw, f[1] / th, (f[0] + f[2]) / tw, (f[1] + f[3]) / th );
		frame( uv );
		//its thick head at the far end, on the item
		origin.set( f[2], f[3] / 2f );
	}

	/** The flight is over (the missile landed): everything let go at once. */
	public void end(){
		if (ended) return;
		ended = true;
		retire();
	}

	/** Whether it is a full trail (its motes and light), else the streak alone or none (tests). */
	public boolean isFull(){
		return isFull;
	}

	/** Motes shed, dots left, ghosts left this flight (tests). */
	public int sheds(){
		return sheds;
	}

	public int dots(){
		return dots;
	}

	public int ghosts(){
		return ghosts;
	}

	/** Whether it is passing through a wall now. */
	public boolean phasing(){
		return phasing;
	}

	@Override
	public void update(){
		super.update();
		ItemSprite s = owner;
		if (s == null || ended || !s.alive || !s.exists || s.mode() != ItemSprite.Mode.FLIGHT || s.flights() != flight){
			retire();
			return;
		}
		float cx = s.x + s.width() / 2f, cy = s.y + s.height() / 2f;
		float dx = cx - lastX, dy = cy - lastY;
		float step = (float)Math.sqrt( dx * dx + dy * dy );
		float alpha = s.alpha();
		ItemLooks.Set looks = s.looks();
		EnchantLook a = looks.a, b = looks.b;

		//through a wall: drawn faint, rippling where it enters and leaves
		int cell = Fx.cellAt( cx, cy + RAISE );
		if (cell != lastCell){
			boolean solid = solid( cell ) && cell != toCell && cell != fromCell && onPath( cell );
			if (solid && !phasing){
				phasing = true;
				ripple( cx, cy );
				ear.play( Assets.Sounds.MELD, cell, 0.25f, 1.5f );
			} else if (!solid && phasing){
				phasing = false;
				ripple( cx, cy );
			}
			lastCell = cell;
		}
		s.phased( phasing ? PHASED_ALPHA : 1f );

		//the streak behind the head
		if (streak && step > 0.01f){
			if (frames != null){
				int f = (int)(Game.timeTotal * 12f) % frames.length;
				if (f != frameShown){
					frameShown = f;
					show( frames[f] );
				}
			}
			hardlight( phasing ? Element.ARCANE.light : s.backColor() );
			angle = (float)Math.toDegrees( Math.atan2( dy, dx ) );
			x = cx - origin.x;
			y = cy - origin.y;
			//a flame's short tongue drawn out long behind the blade; a taper as it is
			if (frames != null) scale.set( Math.min( 2.6f, 1.2f + step * 0.25f ), 1.5f );
			else scale.set( Math.min( 1.5f, 0.6f + step * 0.1f ), 1f );
			am = STREAK_ALPHA * alpha * (phasing ? PHASED_ALPHA : 1f);
			visible = am > 0.004f;
		} else if (!streak){
			visible = false;
		}

		flown += step;
		float vx = Game.elapsed > 0 ? dx / Game.elapsed : 0, vy = Game.elapsed > 0 ? dy / Game.elapsed : 0;

		//its motes, one each SHED_PX
		if (isFull && a != null && flown >= nextShed && sheds < MOST_SHEDS){
			nextShed = flown + SHED_PX;
			EnchantLook l = b != null && (sheds & 1) == 1 ? b : a;
			sheds++;
			if (phasing) arcane( cx, cy, vx, vy, alpha );
			else shed( l, cx, cy, vx, vy, alpha );
		}

		//an emissive one's passing light
		if (isFull && a != null && (a.emissive || (b != null && b.emissive)) && passing == null){
			EnchantLook e = a.emissive ? a : b;
			passing = FxLight.glow( cx, cy, 11, e.light, LIGHT_LEVEL );
			passing.follow( s, 0, 0 );
			passingSetups = passing.setups();
		}
		if (passing != null && passing.alive && passing.setups() == passingSetups){
			passing.level( LIGHT_LEVEL * alpha );
		}

		//a boomerang's dotted path and its ghosts
		if (boomerang){
			if (flown >= nextDot && dots < MOST_DOTS){
				nextDot = flown + SHED_PX;
				if (dot( cx, cy, a, alpha )) dots++;
			}
			if (ghosts < GHOSTS && flown >= nextGhost && length > 0){
				nextGhost += length / (GHOSTS + 1);
				int c = a != null ? ItemAura.lightColor( a ) : 0xE8F0FF;
				if (ItemGhost.of( s, c, GHOST_ALPHA * alpha, GHOST_LIFE, 0, 0 ) != null) ghosts++;
			}
		}

		lastX = cx;
		lastY = cy;
	}

	//a mote of the look shed where the item is; a droplet over water lands in it
	private void shed( EnchantLook l, float x, float y, float vx, float vy, float alpha ){
		FxParticle p = MotifFx.flight( l, x, y, vx, vy );
		if (p == null) return;
		if (alpha < 1f) p.alpha( 0.9f * Math.max( 0.2f, alpha ), 0f, 0.3f );
		//embers left behind a flying blade fall away from it
		if (l.fam == Family.EMBER) p.gravity( 40f );
		if (l.motif == Motif.DRIP || l.motif == Motif.SIP || l.motif == Motif.BUBBLE){
			WaterFX.drop( x, y + RAISE, 0.2f, MotifFx.matterColor( l ) );
		}
	}

	//inside a wall: a violet arcane mote
	private void arcane( float x, float y, float vx, float vy, float alpha ){
		FxEmitter e = Fx.glowAir();
		if (e == null || !FxBudget.keep( FxBudget.P2, x, y )) return;
		FxParticle p = ((FxParticle) e.recycle( FxParticle.class )).reset( x, y ).light( true ).priority( FxBudget.P2 );
		p.look( FxFrames.MOTE_2 ).ramp( Element.ARCANE.ramp, true ).vel( vx * 0.08f + FxRandom.Float( -3f, 3f ), vy * 0.08f - 3f )
				.life( 0.5f ).alpha( 0.9f * Math.max( 0.2f, alpha ), 0f, 0.3f );
	}

	//a boomerang's dot, hanging where it passed
	private boolean dot( float x, float y, EnchantLook l, float alpha ){
		FxEmitter e = Fx.glowAir();
		if (e == null || !FxBudget.keep( FxBudget.P2, x, y )) return false;
		FxParticle p = ((FxParticle) e.recycle( FxParticle.class )).reset( x, y ).light( true ).priority( FxBudget.P2 );
		p.look( FxFrames.MOTE_1 ).paint( l != null ? ItemAura.lightColor( l ) : 0xE8F0FF ).snap()
				.life( DOT_HANG ).alpha( 0.8f * Math.max( 0.25f, alpha ), 0f, DOT_HANG * 0.6f );
		return true;
	}

	//an upright ring at a wall's face (a mirage's sign: space folding)
	private static void ripple( float x, float y ){
		EnchantFx.Flash f = EnchantFx.Flash.make( false, true, RIPPLE, 0f, x, y, Element.ARCANE.light, 0.8f,
				RIPPLE_HOLD + RIPPLE_FADE );
		if (f != null) f.envelope( 0f, RIPPLE_HOLD ).turn( 90f );
	}

	private static final int[][] RIPPLE = { FxFrames.RING_9 };

	//the throw's cells from one to the other, the ends left out (once a flight, as it starts)
	private void path( int from, int to ){
		pathLen = 0;
		Level l = Dungeon.level;
		if (l == null || from < 0 || to < 0 || from == to || !l.insideMap( from ) || !l.insideMap( to )) return;
		Ballistica b = new Ballistica( from, to, Ballistica.STOP_TARGET );
		for (int i = 1; i < b.dist; i++){
			if (pathLen == path.length) path = java.util.Arrays.copyOf( path, path.length * 2 );
			path[pathLen++] = b.path.get( i );
		}
	}

	private boolean onPath( int cell ){
		for (int i = 0; i < pathLen; i++) if (path[i] == cell) return true;
		return false;
	}

	private static boolean solid( int cell ){
		Level l = Dungeon.level;
		return l != null && l.solid != null && cell >= 0 && cell < l.solid.length && l.solid[cell];
	}

	private void retire(){
		if (isFull){
			isFull = false;
			for (int i = 0; i < fulls.length; i++) if (fulls[i] == this) fulls[i] = null;
		}
		if (passing != null && passing.alive && passing.setups() == passingSetups) passing.putOut( 0.15f );
		passing = null;
		if (owner != null && owner.flights() == flight) owner.phased( 1f );
		owner = null;
		kill();
	}

	@Override
	public void draw(){
		Blending.setLightMode();
		super.draw();
		Blending.setNormalMode();
	}

	/** A test's: no trail counted as alive. */
	public static void reset(){
		for (int i = 0; i < fulls.length; i++) fulls[i] = null;
	}
}
