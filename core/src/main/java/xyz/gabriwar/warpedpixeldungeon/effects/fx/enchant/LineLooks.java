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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drenched;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Light;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SpringSoak;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.RarityLine;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.SlayerFamily;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;

import java.lang.ref.WeakReference;

/**
 * The rarity lines' own looks (docs/fx/line-looks.md). A line that changes a blow (Slayer,
 * Executioner, Moonlit, Stormborn, Defiant; Steady when the wind was taking a shot's aim) shows only
 * its gold chip on the blow's card (chipOf, put there by Quality.edge, Steady's by Hero.attackProc)
 * and draws nothing. A line that acts on its wearer shows on a hero only
 * while it acts: warm grip and cooling with one moment as they start to act (at most once per
 * EDGE_GAP s, three quads and an afterglow), and every line acting with a sparse one-quad idle
 * (EVERY), at most one line mote alive a hero and none past WieldLook's ceiling; Fleet a puff of
 * dust behind the heel every FLEET_EVERY steps of a run on dry floor, Hushed a faint ring at the
 * feet stepping near a foe that has not noticed him, Surefoot a ring and a glint landing from a
 * fall. Nothing at MINIMAL; Fleet at FULL only.
 *
 * Which lines act is worked out once a hero turn from what Hero.checkWeatherEffects already has
 * (onWeather, actor thread, the host's: a co-op guest's client runs no actor loop, so a guest
 * sees these looks on the host's heroes only when he hosts), kept per hero here and drawn by the
 * render thread (step, from EnchantArea.afterDraw). Nothing is saved; nothing of either Random is
 * drawn deciding (the pictures' jitter is FxRandom's).
 */
public final class LineLooks {

	private LineLooks(){}

	/** The looks, by line. The first five are told acting by onWeather. */
	public static final int WARM_GRIP = 0, MOONLIT = 1, COOLING = 2, RAINPROOF = 3, WINDBREAK = 4,
			FLEET = 5, HUSHED = 6, SUREFOOT = 7;
	static final int ACTING = 5;

	/** Seconds between two idles of a line while it acts, WARM_GRIP..WINDBREAK. */
	public static final float[] EVERY = { 2f, 2f, 2.5f, 1.2f, 2.5f };
	/** A line's starting moment (warm grip's, cooling's) at most this often, s; its quads and how
	 *  long the longest of them lives. */
	public static final float EDGE_GAP = 30f, EDGE_LIFE = 0.6f;
	public static final int EDGE_QUADS = 3;
	/** Hushed's ring at most this often (s), and how near the foe it answers to (cells). */
	public static final float HUSH_GAP = 1.5f;
	public static final int HUSH_REACH = 4;
	/** Fleet's puff every FLEET_EVERY steps of a run: steps each from where the last ended, each
	 *  within RUN_GAP s of it. */
	public static final int FLEET_EVERY = 3;
	public static final float RUN_GAP = 0.5f;
	/** The colours of their own: warm grip's heat, moonlight, the hush, cooling's afterglow. */
	public static final int WARM = 0xFF9A48, MOON = 0xC8D8FF, HUSH = 0xA0A0C8, COOL = 0xBCE0FF;

	// ------------------------------------------------------------------ the chips

	/** A line's chip on the blow's card (a text-icon cell, drawn plain), -1 for a line with none:
	 *  Slayer its family's emblem, Executioner, Moonlit, Stormborn, Defiant and Steady their own. */
	public static int chipOf( RarityLine line, SlayerFamily family ){
		if (line == null) return -1;
		switch (line){
			case SLAYER:
				if (family == null) return -1;
				switch (family){
					case UNDEAD:     return FloatingText.S_UNDEAD;
					case DEMONS:     return FloatingText.S_DEMON;
					case CONSTRUCTS: return FloatingText.S_CONSTRUCT;
					case ELEMENTALS: return FloatingText.S_ELEMENTAL;
					case BEASTS:     return FloatingText.S_BEAST;
					case HUMANOIDS:  return FloatingText.S_HUMANOID;
					default:         return -1;
				}
			case EXECUTIONER: return FloatingText.EXECUTIONER;
			case MOONLIT:     return FloatingText.MOON;
			case STORMBORN:   return FloatingText.STORM;
			case DEFIANT:     return FloatingText.DEFIANT;
			case STEADY:      return FloatingText.STEADY;
			default:          return -1;
		}
	}

	// ------------------------------------------------------------------ per hero

	/** What a hero's lines are doing: written by onWeather and the steps, read by the looks. */
	static final class State {
		//held weakly: a hero gone with his game (a new game, a load, a guest's rejoin) is let go of
		final WeakReference<Hero> hero;
		//when it was last told anything: a new hero takes a gone hero's state, else the stalest
		float touched;
		//the lines acting now (bits by look), and whether the armour holds Fleet and Hushed
		volatile int acting;
		volatile boolean fleet, hushed;
		//how often each line began to act (onWeather), and how many of those the looks answered
		final int[] starts = new int[ACTING], seen = new int[ACTING];
		//its last starting moment and its next idle, by line; until when its line mote lives
		final float[] edgeAt = new float[ACTING], next = new float[ACTING];
		float moteUntil = -1f;
		//the steps: the run's length, where the last ended and when; the last hush
		int run, lastTo = -1;
		float stepAt = -1e9f, hushAt = -1e9f;

		State( Hero h ){
			hero = new WeakReference<>( h );
			java.util.Arrays.fill( edgeAt, -1e9f );
		}
	}

	//the heroes' states: the host's own and a co-op party's; a new one takes a free slot or a gone
	//hero's, else the stalest
	private static final State[] STATES = new State[8];

	static State state( Hero h, boolean make ){
		if (h == null) return null;
		for (State s : STATES) if (s != null && s.hero.get() == h) return s;
		if (!make) return null;
		synchronized (STATES){
			for (State s : STATES) if (s != null && s.hero.get() == h) return s;
			int slot = 0;
			for (int i = 0; i < STATES.length; i++){
				if (STATES[i] == null || STATES[i].hero.get() == null){
					slot = i;
					break;
				}
				if (STATES[i].touched < STATES[slot].touched) slot = i;
			}
			State s = new State( h );
			s.touched = Game.timeTotal;
			STATES[slot] = s;
			return s;
		}
	}

	/** A test's: no hero known. */
	static void reset(){
		synchronized (STATES){
			java.util.Arrays.fill( STATES, null );
		}
	}

	/**
	 * Once a hero turn, at the end of Hero.checkWeatherEffects (actor thread), with what it worked
	 * out: what he feels, whether rain falls on him, whether the wind reaches him and how hard it
	 * blows. Which of his lines act now: warm grip while it lifts him (the weapon in hand holds it
	 * and he feels no more than SpringSoak.COMFORT before the torch he carries: feelsLikeAt adds a
	 * flame's TORCH_WARMTH after the lift); moonlit at night; cooling while it cools him
	 * (he feels TileTemperature.COOLING_FLOOR or more); rainproof while it holds the rain off (rain
	 * on him, its count running, not drenched yet); windbreak while the wind chill would reach him.
	 */
	public static void onWeather( Hero hero, float feelsLike, boolean rainOn, boolean windHere, float wind ){
		if (hero == null || hero.belongings == null) return;
		Item w = hero.belongings.weapon(), a = hero.belongings.armor();
		int now = 0;
		if (Quality.value( w, RarityLine.WARM_GRIP ) > 0f && feelsLike - torchWarmth( hero ) <= SpringSoak.COMFORT) now |= 1 << WARM_GRIP;
		if (Quality.value( w, RarityLine.MOONLIT ) > 0f && DayNightCycle.phase() == DayNightCycle.Phase.NIGHT) now |= 1 << MOONLIT;
		if (Quality.value( a, RarityLine.COOLING ) > 0f && feelsLike >= TileTemperature.COOLING_FLOOR) now |= 1 << COOLING;
		if (Quality.value( a, RarityLine.RAINPROOF ) > 0f && rainOn && hero.rainTurns > 0
				&& hero.buff( Drenched.class ) == null) now |= 1 << RAINPROOF;
		if (Quality.value( a, RarityLine.WINDBREAK ) > 0f && windHere && wind > 3f
				&& Dungeon.level != null && TileTemperature.tileTemp( hero.pos ) < 10f) now |= 1 << WINDBREAK;
		boolean fleet = Quality.value( a, RarityLine.FLEET ) > 0f, hushed = Quality.value( a, RarityLine.HUSHED ) > 0f;
		State st = state( hero, now != 0 || fleet || hushed );
		if (st == null) return;
		int was = st.acting;
		for (int i = 0; i < ACTING; i++) if ((now & ~was & (1 << i)) != 0) st.starts[i]++;
		st.acting = now;
		st.fleet = fleet;
		st.hushed = hushed;
		st.touched = Game.timeTotal;
	}

	//the warmth of the flame he carries, as TileTemperature.feelsLikeAt adds it after the warm lines
	private static float torchWarmth( Hero hero ){
		Light light = hero.buff( Light.class );
		return light != null && light.flame ? TileTemperature.TORCH_WARMTH : 0f;
	}

	/** Whether a line acts on the hero now (as onWeather last found; tests and the gallery). */
	public static boolean acting( Hero hero, int look ){
		State st = state( hero, false );
		return st != null && look < ACTING && (st.acting & (1 << look)) != 0;
	}

	/** Once a frame for a drawn sprite (EnchantArea.afterDraw, render thread): a hero's lines'
	 *  starting moments and idles. */
	public static void step( CharSprite s ){
		if (!(s.ch instanceof Hero)) return;
		State st = state( (Hero) s.ch, false );
		if (st == null) return;
		if (FxBudget.tier() == FxBudget.MINIMAL || !s.visible || s.parent == null){
			//what began meanwhile is let go of, not played late
			for (int i = 0; i < ACTING; i++) st.seen[i] = st.starts[i];
			return;
		}
		float now = Game.timeTotal;
		int acting = st.acting;

		EnchantState es = EnchantState.get( s );
		//one line look alive at a time: a moment's three quads, or one idle's mote
		boolean busy = now < st.moteUntil && st.moteUntil - now <= 2f;

		//warm grip and cooling starting to act: their moment, at most once per EDGE_GAP; one that
		//finds no room under WieldLook's ceiling (or another line look alive) waits for it
		for (int i = 0; i < ACTING; i++){
			int k = st.starts[i];
			if (k == st.seen[i]) continue;
			if ((i != WARM_GRIP && i != COOLING) || (acting & (1 << i)) == 0){
				st.seen[i] = k;
				continue;
			}
			float last = st.edgeAt[i];
			if (now >= last && now - last < EDGE_GAP){
				st.seen[i] = k;
				continue;
			}
			if (busy || (es != null && WieldLook.live( es, now ) + EDGE_QUADS > WieldLook.PER_SPRITE)) continue;
			st.seen[i] = k;
			st.edgeAt[i] = now;
			if (i == WARM_GRIP) warmEdge( s );
			else coolEdge( s );
			st.next[i] = now + EVERY[i];
			held( st, es, now + EDGE_LIFE, EDGE_QUADS );
			return;
		}

		//one line mote alive at a time, and none past what WieldLook allows a sprite
		if (busy) return;
		if (es != null && WieldLook.live( es, now ) >= WieldLook.PER_SPRITE) return;
		//the line whose idle has waited longest
		int pick = -1;
		for (int i = 0; i < ACTING; i++){
			if ((acting & (1 << i)) == 0 || !due( st, i, now )) continue;
			if (pick < 0 || st.next[i] < st.next[pick]) pick = i;
		}
		if (pick < 0) return;
		st.next[pick] = now + EVERY[pick];
		float life = idle( s, pick );
		if (life > 0f) held( st, es, now + life, 1 );
	}

	//a line look alive until `until`, its quads counted against WieldLook's ceiling meanwhile
	private static void held( State st, EnchantState es, float until, int quads ){
		st.moteUntil = until;
		if (es != null){
			es.lineUntil = until;
			es.lineQuads = quads;
		}
	}

	//whether a line's idle is due (its next time passed; one set further off than its pace is a
	//clock gone back, and due too)
	private static boolean due( State st, int i, float now ){
		float next = st.next[i];
		return now >= next || next - now > EVERY[i];
	}

	/** A sprite stepped (EnchantArea.stepped; the actor thread for the host's heroes, whose mobs it
	 *  reads): Fleet's puff on every FLEET_EVERY-th step of a run on dry floor, Hushed's ring on a
	 *  step near a sleeping or wandering foe he sees, at most once per HUSH_GAP s. */
	public static void stepped( final CharSprite s, final int from, final int to ){
		if (!(s.ch instanceof Hero)) return;
		Hero h = (Hero) s.ch;
		State st = state( h, false );
		if (st == null || (!st.fleet && !st.hushed)) return;
		float now = Game.timeTotal;
		boolean onward = from == st.lastTo && now >= st.stepAt && now - st.stepAt <= RUN_GAP;
		st.run = onward ? st.run + 1 : 1;
		st.lastTo = to;
		st.stepAt = now;
		final boolean puff = st.fleet && st.run % FLEET_EVERY == 0 && dry( from );
		final boolean hush = st.hushed && (now < st.hushAt || now - st.hushAt >= HUSH_GAP) && unaware( h, to );
		if (hush) st.hushAt = now;
		if (!puff && !hush) return;
		final int dir = direction( from, to, s );
		Fx.post( () -> {
			if (s.parent == null || !s.visible) return;
			if (puff) fleetPuff( s, dir );
			if (hush) hushRing( s );
		} );
	}

	/** The hero landed from a fall with a Surefoot armour (Chasm.heroLand, any thread): a ring
	 *  spreading at his feet and a glint, "landed well". The landing draws no dust of its own. */
	public static void landed( Hero hero, float sure ){
		if (hero == null || sure <= 0f || !hero.isAlive() || hero.sprite == null) return;
		final CharSprite s = hero.sprite;
		Fx.post( () -> {
			if (s.parent != null && s.visible) landing( s );
		} );
	}

	//dry floor: no water, no grass (water has its wake, grass its trample)
	private static boolean dry( int cell ){
		Level l = Dungeon.level;
		if (l == null || cell < 0 || cell >= l.length()) return false;
		if (l.water != null && l.water[cell]) return false;
		int t = l.map[cell];
		return t != Terrain.GRASS && t != Terrain.HIGH_GRASS && t != Terrain.FURROWED_GRASS;
	}

	//a foe within HUSH_REACH of the cell, in the hero's view, asleep or wandering (not hunting him)
	private static boolean unaware( Hero h, int cell ){
		Level l = Dungeon.level;
		if (l == null || l.mobs == null || h.fieldOfView == null || cell < 0 || cell >= l.length()) return false;
		for (Mob m : l.mobs){
			if (m.alignment != Char.Alignment.ENEMY || m.pos < 0 || m.pos >= h.fieldOfView.length || !h.fieldOfView[m.pos]) continue;
			if (m.state != m.SLEEPING && m.state != m.WANDERING) continue;
			if (l.distance( cell, m.pos ) <= HUSH_REACH) return true;
		}
		return false;
	}

	//+1 stepping east, -1 west; the way it faces otherwise
	private static int direction( int from, int to, CharSprite s ){
		Level l = Dungeon.level;
		if (l != null && l.width() > 0){
			int dx = to % l.width() - from % l.width();
			if (dx != 0) return dx > 0 ? 1 : -1;
		}
		return s.flipHorizontal ? -1 : 1;
	}

	// ------------------------------------------------------------------ the gallery's

	/** A look restaged on any sprite (the gallery, the previews; render thread): a line's starting
	 *  moment (`edge`, warm grip and cooling) or one idle; Fleet's puff, Hushed's ring, Surefoot's
	 *  landing. */
	public static void stage( CharSprite s, int look, boolean edge ){
		if (s == null) return;
		switch (look){
			case WARM_GRIP:
				if (edge) warmEdge( s ); else idle( s, look );
				break;
			case COOLING:
				if (edge) coolEdge( s ); else idle( s, look );
				break;
			case MOONLIT: case RAINPROOF: case WINDBREAK:
				idle( s, look );
				break;
			case FLEET:
				fleetPuff( s, s.flipHorizontal ? -1 : 1 );
				break;
			case HUSHED:
				hushRing( s );
				break;
			case SUREFOOT:
				landing( s );
				break;
			default:
				break;
		}
	}

	// ------------------------------------------------------------------ the pictures

	/** A part of a look: moonlight twinkling, a bead rolling then dropping; else a plain particle.
	 *  Public, made by the emitter's recycling. */
	public static final class Part extends FxParticle {

		static final int PLAIN = 0, TWINKLE = 1, BEAD = 2;
		/** A bead rolls this long before it drops off. */
		static final float ROLL = 0.4f;

		int kind;
		float side;

		public Part(){
			super();
		}

		@Override
		public void update(){
			if (kind == BEAD && age < ROLL && age + Game.elapsed >= ROLL){
				//off the outline: sideways and falling
				vx = side * 14f;
				vy = 0f;
				gravity( 160f );
			}
			super.update();
			if (kind == TWINKLE && alive) am *= 0.8f + 0.2f * (float)Math.cos( 2 * Math.PI * 2f * age );
		}
	}

	private static Part part( boolean light, float x, float y, int pri, int kind ){
		FxEmitter e = light ? Fx.glowAir() : Fx.air();
		if (e == null || !FxBudget.keep( pri, x, y )) return null;
		Part p = (Part) e.recycle( Part.class );
		p.reset( x, y ).light( light ).priority( pri );
		p.kind = kind;
		p.side = 0f;
		return p;
	}

	//the weapon's side at the hand, and his middle
	private static float handX( CharSprite s ){
		return s.x + s.width() / 2f + (s.flipHorizontal ? -EnchantFx.WEAPON_SIDE : EnchantFx.WEAPON_SIDE);
	}

	private static float midY( CharSprite s ){
		return s.y + s.height() / 2f;
	}

	private static float feetY( CharSprite s ){
		return s.y + s.height() - 1f;
	}

	//warm grip starting to warm him: three embers rising 6 px from the hand, and the heat's afterglow
	static void warmEdge( CharSprite s ){
		float x = handX( s ), y = midY( s ) + 1f;
		for (int k = 0; k < 3; k++){
			Part p = part( true, x + (k - 1) * 1.5f, y, FxBudget.P2, Part.PLAIN );
			if (p == null) break;
			p.look( FxFrames.EMBER_1 ).ramp( Element.FIRE.ember, true ).vel( FxRandom.Float( -2f, 2f ), -12f - 1.5f * k )
					.life( 0.5f ).alpha( 0.9f, 0.05f, 0.25f );
		}
		s.afterglow( WARM, 0.2f );
	}

	//cooling starting to cool him: three motes falling off the shoulders, and a cool afterglow
	static void coolEdge( CharSprite s ){
		float cx = s.x + s.width() / 2f, y = s.y + s.height() * 0.35f;
		for (int k = 0; k < 3; k++){
			float side = k == 0 ? -4f : k == 1 ? 4f : FxRandom.Float( -2f, 2f );
			Part p = part( true, cx + side, y, FxBudget.P2, Part.PLAIN );
			if (p == null) break;
			p.look( FxFrames.MOTE_1 ).paint( Element.FROST.light ).vel( side * 0.25f, 9f + FxRandom.Float( 2f ) )
					.life( 0.6f ).alpha( 0.9f, 0.05f, 0.3f );
		}
		s.afterglow( COOL, 0.2f );
	}

	//one idle of a line acting; its life (0: none drawn)
	static float idle( CharSprite s, int look ){
		float cx = s.x + s.width() / 2f;
		Part p;
		switch (look){
			case WARM_GRIP:
				//a faint ember off the weapon side, wavering a pixel either way as it rises
				p = part( true, handX( s ) + FxRandom.Float( -1f, 1f ), midY( s ), FxBudget.P3, Part.PLAIN );
				if (p == null) return 0f;
				p.look( FxFrames.EMBER_1 ).paint( Element.FIRE.light ).vel( 0f, -4f ).sway( 6.3f, 6.3f )
						.life( 1f ).alpha( 0.35f, 0.15f, 0.4f );
				return 1f;
			case MOONLIT:
				//moonlight drifting down off the weapon side, twinkling
				p = part( true, handX( s ), midY( s ) - 1f, FxBudget.P3, Part.TWINKLE );
				if (p == null) return 0f;
				p.look( FxFrames.MOTE_1 ).paint( MOON ).vel( 0f, 3f ).life( 1.2f ).alpha( 1f, 0.15f, 0.4f );
				return 1.2f;
			case COOLING:
				//a breath of cool air sinking off the armour, the mirror of heatstroke's rising steam
				p = part( false, cx, midY( s ) + 2f, FxBudget.P3, Part.PLAIN );
				if (p == null) return 0f;
				p.look( FxFrames.PUFF_S, 3f ).paint( Element.STEAM.ramp.stop( 0 ) ).smooth().vel( 0f, 4f )
						.life( 1.2f ).alpha( 0.2f, 0.3f, 0.5f );
				return 1.2f;
			case RAINPROOF: {
				//a bead on a shoulder rolling down the outline and dropping off sideways
				float side = FxRandom.chance( 0.5f ) ? -1f : 1f;
				p = part( false, cx + side * 4f, s.y + s.height() * 0.3f, FxBudget.P3, Part.BEAD );
				if (p == null) return 0f;
				p.side = side;
				p.look( FxFrames.DROP_2 ).paint( Element.FROST.light ).vel( 0f, 10f ).life( Part.ROLL + 0.5f ).alpha( 1f, 0.1f, 0.2f );
				return Part.ROLL + 0.5f;
			}
			case WINDBREAK: {
				//a wisp of the wind stood on end at his windward side, drawn in a little and rising
				//along him as the wind parts round him: born 2.5 px further out than WIND_OFF, the
				//drag stopping it within 2 px (8 px/s at 4/s; the half pixel is its whole-pixel
				//placing), so its near side keeps WIND_OFF px off his outline, never through him and
				//never reaching across into the next cell's occupant
				float side = windward( s );
				float off = s.width() / 2f + WISP_HALF + WIND_OFF + 2.5f;
				p = part( false, cx + side * off, midY( s ) + FxRandom.Float( -1f, 2f ), FxBudget.P3, Part.PLAIN );
				if (p == null) return 0f;
				p.look( FxFrames.EDGE_WISP, 6f ).paint( Element.STEAM.ramp.stop( 1 ) ).smooth().vel( -side * 8f, -6f )
						.drag( 4f ).gravity( -24f ).life( 0.5f ).alpha( 0.25f, 0.1f, 0.25f );
				p.angle = 90f;
				return 0.5f;
			}
			default:
				return 0f;
		}
	}

	/** Half the wind wisp's width stood on end (EDGE_WISP is 10 x 3 px), and how far its near side
	 *  keeps off him. */
	static final float WISP_HALF = 1.5f, WIND_OFF = 3f;

	//the side the wind comes from (-1 his left): against Fx's screen wind, else the way he faces
	private static float windward( CharSprite s ){
		float w = Fx.windX();
		if (w > 0f) return -1f;
		if (w < 0f) return 1f;
		return s.flipHorizontal ? 1f : -1f;
	}

	//Fleet: a little sandy dust kicked up behind the heel, still (never the Swiftness glyph's streak)
	static void fleetPuff( CharSprite s, int dir ){
		if (FxBudget.tier() != FxBudget.FULL) return;
		Part p = part( false, s.x + s.width() / 2f - dir * 3f, feetY( s ), FxBudget.P3, Part.PLAIN );
		if (p == null) return;
		p.look( FxFrames.PUFF_S, 4f ).paint( Element.EARTH.smoke.stop( 0 ) ).smooth().vel( -dir * 6f, -3f ).drag( 4f )
				.life( 0.5f ).alpha( 0.3f, 0.05f, 0.3f );
	}

	//Hushed: a faint hush ring at the feet
	static void hushRing( CharSprite s ){
		if (FxBudget.tier() == FxBudget.MINIMAL) return;
		Part p = part( false, s.x + s.width() / 2f, feetY( s ), FxBudget.P3, Part.PLAIN );
		if (p == null) return;
		p.look( FxFrames.RING_9 ).paint( HUSH ).smooth().life( 0.3f ).alpha( 0.2f, 0.05f, 0.15f );
	}

	static final int[][] LANDING_RING = { FxFrames.RING_9, FxFrames.RING_13 };

	//Surefoot: a ring spreading at the feet and a glint rising off them
	static void landing( CharSprite s ){
		if (FxBudget.tier() == FxBudget.MINIMAL) return;
		float cx = s.x + s.width() / 2f, y = feetY( s );
		Part r = part( false, cx, y, FxBudget.P2, Part.PLAIN );
		if (r != null) r.look( LANDING_RING, 0f ).paint( Element.EARTH.light ).smooth().life( 0.4f ).alpha( 0.3f, 0f, 0.2f );
		Part g = part( true, cx, y - 3f, FxBudget.P2, Part.PLAIN );
		if (g != null) g.look( FxFrames.PLUS_3 ).paint( Element.EARTH.light ).vel( 0f, -6f ).life( 0.35f ).alpha( 0.8f, 0.05f, 0.2f );
	}
}
