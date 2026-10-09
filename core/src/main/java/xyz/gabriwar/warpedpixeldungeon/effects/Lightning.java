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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class Lightning extends Group {

	private static final float DURATION = 0.3f;

	//its brightness through its life, as (time, brightness) keys joined by straight runs: the
	//strike, a dip as its channel forms anew, the re-strike with the flash's second pulse
	//(LightningFlash: 0.14 s in), and a fade with a last waver, as a real stroke's return strokes
	//flicker down the same sky
	static final float[] FLICKER = { 0f, 1f, 0.04f, 0.85f, 0.08f, 0.25f, 0.12f, 0.8f, 0.14f, 1f,
			0.18f, 0.65f, 0.2f, 0.4f, 0.22f, 0.5f, DURATION, 0f };
	//when its channel forms anew: at the dip, and half the time once more at the last waver
	static final float REFORM = 0.08f, REFORM_AGAIN = 0.2f;
	//its halos and twigs go on its first RICH_LEGS legs only: one with an arc to every char on the
	//level (a ball lightning potion) costs about what it always did
	static final int RICH_LEGS = 64;
	//a glow already within this many px of a place is that place's (glows)
	private static final float GLOW_NEAR = 3f;

	//the weather's layer of the game's scene, none off it. A test may hand its own
	static Supplier<WeatherOverlay> layer = GameScene::getWeatherOverlay;

	private float life;

	private List<Arc> arcs;

	private Callback callback;

	//its first frame is past: its glows are made, its flash and thunder seen to
	private boolean struck;
	//a storm's bolt (WeatherOverlay): the overlay flashes, thunders and sparks for it
	private boolean stormsOwn;
	//a wand of lightning's zap (thunderous): where it cracks every time, -1 for any other
	private int thunderAt = -1;
	//its channel forms anew a second time (REFORM_AGAIN)
	private final boolean again;
	//the weather's layer, which draws it over the day/night tint and flashes and sparks for it:
	//none off the game's scene
	private WeatherOverlay sky;
	//its stand-in there (WeatherOverlay.carry)
	private Gizmo above;

	public Lightning(int from, int to, Callback callback){
		this(Arrays.asList(new Arc(from, to)), callback);
	}

	public Lightning(PointF from, int to, Callback callback){
		this(Arrays.asList(new Arc(from, to)), callback);
	}

	public Lightning(int from, PointF to, Callback callback){
		this(Arrays.asList(new Arc(from, to)), callback);
	}

	public Lightning(PointF from, PointF to, Callback callback){
		this(Arrays.asList(new Arc(from, to)), callback);
	}

	public Lightning( List<Arc> arcs, Callback callback ) {

		super();

		//a list of its own: a caller may clear its own for the next one while this one lasts
		this.arcs = new ArrayList<>( arcs );
		int legs = 0;
		for (Arc arc : this.arcs) {
			add( arc );
			legs += arc.legs;
			if (legs <= RICH_LEGS) arc.enrich();
		}

		this.callback = callback;

		life = DURATION;
		again = Random.Int( 2 ) == 0;
	}

	private static final double A = 180 / Math.PI;

	@Override
	public void update() {
		if (!struck) {
			struck = true;
			strike();
		}
		float was = DURATION - life;
		if ((life -= Game.elapsed) < 0) {

			killAndErase();
			//its images let go of their vertex buffers now, not when the scene changes
			destroy();
			if (callback != null) {
				callback.call();
			}

		} else {

			float t = DURATION - life;
			if (crosses( was, t, REFORM ) || (again && crosses( was, t, REFORM_AGAIN ))) {
				for (Arc arc : arcs) {
					arc.form();
				}
			}
			float b = flicker( t ), h = halo( t );
			for (Arc arc : arcs) {
				//sparks where it lands as it lights: a run's step as the run gets there
				if (arc.light( t, b, h ) && arc.sparks > 0 && sky != null && !stormsOwn) {
					sky.spark( arc.to.x, arc.to.y, arc.sparks );
				}
			}

			super.update();
		}
	}

	private static boolean crosses( float was, float now, float at ){
		return was < at && now >= at;
	}

	@Override
	public void draw() {
		//its stand-in draws it on the weather's layer instead (strike), while it stands
		if (above == null || !above.exists) drawLit();
	}

	//drawn as light is: added to what is under it
	void drawLit() {
		Blending.setLightMode();
		super.draw();
		Blending.setNormalMode();
	}

	@Override
	public synchronized void destroy() {
		super.destroy();
		if (above != null) {
			above.killAndErase();
			above = null;
		}
	}

	/** Its arcs end in no glow: a cross through a cell, which strikes nothing at either end, nor
	 *  flashes, nor thunders. Before it is first drawn. */
	public Lightning noGlow() {
		for (Arc arc : arcs) {
			arc.noGlow();
		}
		return this;
	}

	/** A storm's bolt and its arcs (WeatherOverlay.strike): the overlay flashes, thunders and sparks
	 *  for it, so it does none of the three itself. Before it is first drawn. */
	Lightning stormsOwn() {
		stormsOwn = true;
		return this;
	}

	/** A wand of lightning's zap, striking `cell`: it cracks there with the storm's thunder every
	 *  time, however short and whatever cracked before (WeatherOverlay.thunder), and flashes where
	 *  the hero sees it under the usual gate. Before it is first drawn. */
	public Lightning thunderous( int cell ){
		thunderAt = cell;
		return this;
	}

	/** Its brightness `t` seconds into its life (FLICKER), none past its end. */
	static float flicker( float t ){
		if (t <= 0f) return FLICKER[1];
		for (int i = 2; i < FLICKER.length; i += 2) {
			if (t <= FLICKER[i]) {
				float t0 = FLICKER[i - 2], b0 = FLICKER[i - 1];
				return b0 + (FLICKER[i + 1] - b0) * (t - t0) / (FLICKER[i] - t0);
			}
		}
		return 0f;
	}

	/** Its halos' brightness `t` seconds in: steadier than its stroke, the flicker eased halfway to a
	 *  plain fade, so its light lingers through the dip. */
	static float halo( float t ){
		return 0.5f * flicker( t ) + 0.5f * Math.max( 0f, 1f - t / DURATION );
	}

	//its first frame, on the render thread (the weather's sheet goes to the GPU as soon as it is
	//made): the glows where its arcs strike and set off; on the game's scene, the storm's flash and
	//crack for the strike nearest the hero that he sees, and its stand-in over the night's tint
	private void strike() {
		glows();
		sky = layer.get();
		if (sky == null || stormsOwn) return;
		if (thunderAt != -1) {
			sky.thunder( thunderAt );
		} else {
			int cell = struckInSight( arcs, Dungeon.level, Dungeon.hero );
			if (cell != -1) {
				sky.struck( cell );
			}
		}
		//drawn over the flash it brought. Only one in the world: one a window shows stays in it
		Camera cam = camera();
		if (cam == null || cam == Camera.main) {
			above = sky.carry( this );
		}
	}

	//a bloom where each arc strikes and a small glow where it sets off, one to a place: a chain's
	//arcs meet where one strikes and the next sets off, and every arc a caster throws shares his
	private void glows() {
		ArrayList<PointF> lit = new ArrayList<>();
		for (Arc arc : arcs) {
			if (arc.glows && !near( lit, arc.to )) {
				arc.bloom();
				lit.add( arc.to );
			}
		}
		for (Arc arc : arcs) {
			if (arc.glows && !near( lit, arc.from )) {
				arc.source();
				lit.add( arc.from );
			}
		}
	}

	private static boolean near( List<PointF> lit, PointF p ){
		for (PointF q : lit) {
			if (Math.abs( q.x - p.x ) <= GLOW_NEAR && Math.abs( q.y - p.y ) <= GLOW_NEAR) return true;
		}
		return false;
	}

	/**
	 * Of its arcs that strike (Arc.glows: 30 px and more, no cross through a cell), the cell where
	 * the one nearest the hero lands that he sees, -1 for none: where it brings the storm's flash
	 * and crack from.
	 */
	static int struckInSight( List<Arc> arcs, Level level, Hero hero ){
		if (level == null || hero == null || level.heroFOV == null) return -1;
		int w = level.width(), h = level.height(), best = -1;
		for (Arc arc : arcs) {
			if (!arc.glows) continue;
			int x = (int)Math.floor( arc.to.x / DungeonTilemap.SIZE ), y = (int)Math.floor( arc.to.y / DungeonTilemap.SIZE );
			if (x < 0 || y < 0 || x >= w || y >= h) continue;
			int cell = x + y * w;
			if (cell >= level.heroFOV.length || !level.heroFOV[cell]) continue;
			if (best == -1 || level.distance( hero.pos, cell ) < level.distance( hero.pos, best )) {
				best = cell;
			}
		}
		return best;
	}

	//A lightning object is meant to be loaded up with arcs.
	//these act as a means of easily expressing lighting between two points.
	//
	//Every arc has the storm's look (WeatherOverlay.strike), sized by its length, and is drawn as
	//light: a bright core (two halves to each leg meeting at a middle thrown up to 3 px as it forms,
	//which shimmers a px a frame) laid on a soft blue-white halo along each leg. One under 30 px (a
	//cell, a diagonal, a cell's hop between two sprites' middles) is a single leg, as it always was;
	//a longer one strikes: a zigzag with a joint every 16 px (8 legs at most) thrown wider the longer
	//it is (the storm bolt's own from 60 px, half of it at least), forked 7 times in 10 from 40 px
	//(three legs), with one to three twigs from 40 px, and a bloom and sparks where it lands and a
	//small glow where it sets off. A step of a run over the water (crawl) is thinner and fainter, and
	//lights as the run gets there. It is put together with no GPU call (it may be on the actor
	//thread): its glows are made on its first frame.
	public static class Arc extends Group {

		//the storm's channel: its joints thrown up to this far across its line and along it, by k
		private static final float ACROSS = 12f, ALONG = 4f;
		//an arc sized by its length: a joint every EVERY px of it, MOST legs at most; thrown k = its
		//length over FULL, LEAST of it at least (the storm bolt's own from FULL); forked FORK times in
		//ten from three legs (40 px), the fork running on no further than FORK_ROOM of the way left
		//to where it strikes (FORK_REACH: the most a fork runs on, by k); one to three twigs from
		//TWIGS_FROM px
		private static final float EVERY = 16f, FULL = 60f, LEAST = 0.5f, FORK = 0.7f;
		private static final float FORK_ROOM = 0.6f, FORK_REACH = 44f, TWIGS_FROM = 40f;
		private static final int MOST = 8;
		//an arc this long strikes: it zigzags, blooms and sparks where it lands and glows where it
		//sets off. A hop of a cell between two sprites' middles stays under it unless one sprite
		//stands 19 px taller than the other
		static final float GLOW_FROM = 30f;
		//each leg's middle, thrown up to MID px each way as it forms and up to JITTER more each
		//frame: never past the 4 px the old arc's middle flickered
		static final float MID = 3f, JITTER = 1f;
		//its halo (lightning_halo.png) tinted HALO_COLOR at up to HALO_AM, overhanging each joint by
		//OVERHANG px so two legs' meet with no notch on a bend's outside
		private static final int HALO_COLOR = 0x7FA6FF;
		private static final float HALO_AM = 0.55f, OVERHANG = 3f;
		//its fork's two legs, thinner and fainter than its channel, core and halo, by these shares
		private static final float[] FORK_CORE = { 0.75f, 0.6f }, FORK_LIGHT = { 0.9f, 0.75f };
		private static final float[] FORK_HALO = { 0.7f, 0.5f }, FORK_HALO_WIDE = { 0.75f, 0.6f };
		//a run over the water's step (crawl): a thin, faint crackle
		private static final float CRAWL_CORE = 0.6f, CRAWL_LIGHT = 0.8f, CRAWL_HALO = 0.4f, CRAWL_HALO_WIDE = 0.7f;
		//its twigs: 5-12 px off a joint, 25-70 degrees off its way and bent back 15-40 two thirds
		//out, a thin line (TWIG_CORE of the core's width) at TWIG_LIGHT
		static final float TWIG_MIN = 5f, TWIG_MAX = 12f;
		private static final float TWIG_CORE = 0.5f, TWIG_LIGHT = 0.7f;
		//where it strikes, the storm's StrikeGlow (as wide as the storm's, 30 px, from FULL px) at
		//BLOOM_AM, and STRIKE_SPARKS sparks; where it sets off, a glow SOURCE_SCALE times the weather's
		//5 px one at SOURCE_AM
		private static final int BLOOM_COLOR = 0xD8E4FF, SOURCE_COLOR = 0xC8D8FF, STRIKE_SPARKS = 3;
		private static final float BLOOM_AM = 0.9f, SOURCE_SCALE = 2.2f, SOURCE_AM = 0.45f;
		//a run over the water's step lights STEP_LAG s after the strike for each step out, with a
		//flash of its own that dies away over PULSE s
		static final float STEP_LAG = 0.025f, PULSE = 0.08f;

		//where it sets off and where it strikes
		final PointF from, to;
		//its joints from where it starts to where it strikes; a fork's three points, or none. Formed
		//anew as its lightning re-strikes (form)
		PointF[] path;
		PointF[] fork;
		//each twig's three points: off its joint, its bend, its tip
		PointF[][] twigs;
		//the bloom where it strikes and the glow where it sets off, made on its first frame
		Image glow, source;
		//it strikes: it blooms and sparks where it lands and glows where it sets off
		boolean glows;
		//the sparks where it lands, as it lights
		int sparks;
		//when it lights, after the strike (a run over the water's step)
		float lag;
		//its legs: its channel's (joints), then its fork's two
		final int joints, legs;

		private final float k;
		private final boolean forked, fitted;
		private final int twigCount;
		private boolean faint, lit;
		//each leg's two halves; each leg's halo and each twig's two lines, none till its lightning
		//lays them (enrich)
		private final Image[] cores;
		private Image[] halos, twigLines;
		//each leg's middle as it was thrown, x and y
		private final float[] mids;

		public Arc(int from, int to){
			this( DungeonTilemap.tileCenterToWorld(from),
					DungeonTilemap.tileCenterToWorld(to));
		}

		public Arc(PointF from, int to){
			this( from, DungeonTilemap.tileCenterToWorld(to));
		}

		public Arc(int from, PointF to){
			this( DungeonTilemap.tileCenterToWorld(from), to);
		}

		public Arc(PointF from, PointF to){
			this( from, to, joints( from, to ), wide( from, to ), FORK, PointF.distance( from, to ) >= GLOW_FROM, true );
		}

		//by the numbers given: the storm's bolt (WeatherOverlay), its shape the storm's (no twigs, its
		//fork as long as k makes it). Its fork is only rolled for, and its glows only made, where the
		//arc is long enough to have them
		Arc(PointF from, PointF to, int joints, float k, float forkChance, boolean glows){
			this( from, to, joints, k, forkChance, glows, false );
		}

		private Arc(PointF from, PointF to, int joints, float k, float forkChance, boolean glows, boolean byLength){
			float length = PointF.distance( from, to );
			if (length < 1f) {
				joints = 1;
				glows = false;
			}
			this.from = from;
			this.to = to;
			this.joints = joints;
			this.k = k;
			this.glows = glows;
			sparks = glows ? STRIKE_SPARKS : 0;
			fitted = byLength;
			forked = joints >= 3 && forkChance > 0 && Random.Float() < forkChance;
			twigCount = byLength && joints >= 3 && length >= TWIGS_FROM ? 1 + Random.Int( 3 ) : 0;
			legs = joints + (forked ? 2 : 0);

			mids = new float[2 * legs];
			cores = new Image[2 * legs];
			for (int i = 0; i < cores.length; i++) {
				cores[i] = new Image(Effects.get(Effects.Type.LIGHTNING));
				cores[i].origin.set( 0, cores[i].height / 2 );
				if (i >= 2 * joints) cores[i].scale.y = FORK_CORE[(i - 2 * joints) / 2];
				add( cores[i] );
			}

			form();
			layout();
		}

		//its joints: one under GLOW_FROM px, else one every EVERY px of it, two at least and MOST at most
		private static int joints( PointF from, PointF to ){
			float length = PointF.distance( from, to );
			return length < GLOW_FROM ? 1 : Math.max( 2, Math.min( MOST, Math.round( length / EVERY ) ) );
		}

		//how wide its zigzag is thrown: the storm's from FULL, LEAST of it at least
		private static float wide( PointF from, PointF to ){
			return Math.max( LEAST, Math.min( 1f, PointF.distance( from, to ) / FULL ) );
		}

		/**
		 * A channel from `from` to `to` in `joints` legs: each joint where it would lie evenly along
		 * the line, thrown up to ACROSS × k across it and ALONG × k along it, the last exactly `to`.
		 * For a bolt coming straight down this is the storm's zigzag as it always was.
		 */
		static PointF[] channel( PointF from, PointF to, int joints, float k ){
			PointF[] p = new PointF[joints + 1];
			p[0] = from;
			float dx = to.x - from.x, dy = to.y - from.y;
			float len = (float)Math.sqrt( dx * dx + dy * dy );
			float ux = len > 0 ? dx / len : 0f, uy = len > 0 ? dy / len : 1f;
			for (int i = 1; i < joints; i++) {
				float t = i / (float)joints;
				float across = k * Random.Float( -ACROSS, ACROSS ), along = k * Random.Float( -ALONG, ALONG );
				p[i] = new PointF( from.x + dx * t + uy * across + ux * along,
						from.y + dy * t - ux * across + uy * along );
			}
			p[joints] = to;
			return p;
		}

		//a fork off the joint `at`: out to one side 10-18 px and on 14-24, then 6-14 and 10-20
		//more, by k, dying out short of where the arc strikes (the storm's)
		private static PointF[] fork( PointF at, PointF from, PointF to, float k ){
			float dx = to.x - from.x, dy = to.y - from.y;
			float len = (float)Math.sqrt( dx * dx + dy * dy );
			float ux = dx / len, uy = dy / len;
			float side = Random.Int( 2 ) == 0 ? -1 : 1;
			float out = k * side * Random.Float( 10f, 18f ), on = k * Random.Float( 14f, 24f );
			PointF mid = new PointF( at.x + uy * out + ux * on, at.y - ux * out + uy * on );
			out = k * side * Random.Float( 6f, 14f );
			on = k * Random.Float( 10f, 20f );
			PointF end = new PointF( mid.x + uy * out + ux * on, mid.y - ux * out + uy * on );
			return new PointF[]{ at, mid, end };
		}

		/** Its channel anew between the same two ends, as its lightning re-strikes: its joints, its
		 *  fork (if it has one) off the joint a third of the way along, each leg's middle, its twigs. */
		void form(){
			path = channel( from, to, joints, k );
			if (forked) {
				PointF at = path[Math.max( 1, Math.round( joints / 3f ) )];
				//sized by its length, it runs on only as far as the room left before it strikes
				fork = fork( at, from, to, fitted ? Math.min( k, PointF.distance( at, to ) * FORK_ROOM / FORK_REACH ) : k );
			}
			for (int i = 0; i < mids.length; i++) {
				mids[i] = Random.Float( -MID, MID );
			}
			if (twigs != null) {
				twig();
			}
		}

		//its twigs anew: each off a joint between its ends, 25-70 degrees off its way to either side
		//and bent back toward it 15-40 degrees two thirds out, 5-12 px in all
		private void twig(){
			float way = (float)Math.atan2( to.y - from.y, to.x - from.x );
			for (int i = 0; i < twigs.length; i++) {
				PointF at = path[1 + Random.Int( joints - 1 )];
				float side = Random.Int( 2 ) == 0 ? -1f : 1f;
				float a = way + side * (float)Math.toRadians( Random.Float( 25f, 70f ) );
				float len = Random.Float( TWIG_MIN, TWIG_MAX );
				PointF bend = new PointF( at.x + (float)Math.cos( a ) * len * 0.6f, at.y + (float)Math.sin( a ) * len * 0.6f );
				a -= side * (float)Math.toRadians( Random.Float( 15f, 40f ) );
				twigs[i] = new PointF[]{ at, bend,
						new PointF( bend.x + (float)Math.cos( a ) * len * 0.4f, bend.y + (float)Math.sin( a ) * len * 0.4f ) };
			}
		}

		//its halo along each leg and its twigs: its lightning lays them on its first RICH_LEGS legs
		void enrich(){
			if (halos != null) return;
			halos = new Image[legs];
			for (int l = 0; l < legs; l++) {
				halos[l] = new Image( Assets.Effects.LIGHTNING_HALO );
				halos[l].origin.set( 0, halos[l].height / 2 );
				halos[l].scale.y = l < joints ? (faint ? CRAWL_HALO_WIDE : 1f) : FORK_HALO_WIDE[l - joints];
				halos[l].hardlight( HALO_COLOR );
				add( halos[l] );
			}
			if (twigCount > 0) {
				twigs = new PointF[twigCount][];
				twigLines = new Image[2 * twigCount];
				for (int i = 0; i < twigLines.length; i++) {
					twigLines[i] = new Image(Effects.get(Effects.Type.LIGHTNING));
					twigLines[i].origin.set( 0, twigLines[i].height / 2 );
					twigLines[i].scale.y = TWIG_CORE;
					add( twigLines[i] );
				}
				twig();
			}
			layout();
		}

		/**
		 * A step of a run over the water (the wand of lightning's, a storm bolt's), `step` steps out
		 * from where it set off: thinner and fainter, with no glow at either end, lit as the run gets
		 * there (STEP_LAG s a step out; 0 lights with the strike) with a flash of its own, and a spark
		 * where it lands. Before it is first drawn.
		 */
		public Arc crawl( int step ){
			faint = true;
			glows = false;
			sparks = 1;
			lag = Math.max( 0, step ) * STEP_LAG;
			for (int i = 0; i < 2 * joints; i++) {
				cores[i].scale.y = CRAWL_CORE;
			}
			return this;
		}

		void noGlow(){
			glows = false;
			sparks = 0;
		}

		/** Its brightness as a whole: its strokes' and its glows' at this, its halos' as bright. */
		public void alpha(float alpha) {
			shade( alpha, alpha );
		}

		/**
		 * Its brightness `t` seconds into its lightning's life: `b` its strokes' and `h` its halos'
		 * (flicker, halo); none before its lag, and a flash of its own as it lights (pulse). Whether
		 * it lit just now.
		 */
		boolean light( float t, float b, float h ){
			if (t < lag) {
				shade( 0f, 0f );
				return false;
			}
			if (lag > 0f) {
				float p = pulse( t - lag );
				b = Math.max( b, p );
				h = Math.max( h, p );
			}
			shade( b, h );
			boolean first = !lit;
			lit = true;
			return first;
		}

		/** A run's step's own flash, `u` seconds after it lit: dying away over PULSE s. */
		static float pulse( float u ){
			return Math.max( 0f, 1f - u / PULSE );
		}

		private void shade( float b, float h ){
			for (int l = 0; l < legs; l++) {
				boolean channel = l < joints;
				cores[2 * l].am = cores[2 * l + 1].am = b * (channel ? (faint ? CRAWL_LIGHT : 1f) : FORK_LIGHT[l - joints]);
				if (halos != null) {
					halos[l].am = h * HALO_AM * (channel ? (faint ? CRAWL_HALO : 1f) : FORK_HALO[l - joints]);
				}
			}
			if (twigLines != null) {
				for (Image t : twigLines) t.am = b * TWIG_LIGHT;
			}
			if (glow != null) {
				glow.am = BLOOM_AM * b;
			}
			if (source != null) {
				source.am = SOURCE_AM * b;
			}
		}

		@Override
		public void update() {
			layout();
		}

		//on its first frame, on the render thread: the weather's sheet goes to the GPU as soon as it
		//is made. The storm's StrikeGlow where it strikes, as wide as the arc is long
		void bloom(){
			glow = glowAt( to, 6f * Math.max( 0.4f, k ), BLOOM_COLOR );
		}

		//and a smaller one where it sets off
		void source(){
			source = glowAt( from, SOURCE_SCALE, SOURCE_COLOR );
		}

		private Image glowAt( PointF at, float scale, int color ){
			int[] f = WeatherSprites.GLOW_5;
			Image g = new Image( WeatherSprites.get() );
			g.frame( f[0], f[1], f[2], f[3] );
			g.origin.set( f[2] / 2f, f[3] / 2f );
			g.scale.set( scale );
			g.hardlight( color );
			g.x = at.x - f[2] / 2f;
			g.y = at.y - f[3] / 2f;
			g.am = 0f;
			add( g );
			return g;
		}

		//every leg anew each frame: its middle shimmers (the flicker), its halo lies along it
		private void layout(){
			int l = 0;
			for (int i = 0; i + 1 < path.length; i++) {
				l = leg( l, path[i], path[i + 1] );
			}
			if (fork != null) {
				l = leg( l, fork[0], fork[1] );
				leg( l, fork[1], fork[2] );
			}
			if (twigLines != null) {
				for (int i = 0; i < twigs.length; i++) {
					place( twigLines[2 * i], twigs[i][0], twigs[i][1] );
					place( twigLines[2 * i + 1], twigs[i][1], twigs[i][2] );
				}
			}
		}

		//a leg: two halves meeting at its middle, thrown as it formed and shimmering up to JITTER px
		//each frame, on its halo; the next leg's index
		private int leg( int l, PointF start, PointF end ){
			float x2 = (start.x + end.x) / 2 + mids[2 * l] + Random.Float( -JITTER, JITTER );
			float y2 = (start.y + end.y) / 2 + mids[2 * l + 1] + Random.Float( -JITTER, JITTER );
			place( cores[2 * l], start.x, start.y, x2, y2 );
			place( cores[2 * l + 1], x2, y2, end.x, end.y );
			if (halos != null) {
				float dx = end.x - start.x, dy = end.y - start.y;
				float len = (float)Math.sqrt( dx * dx + dy * dy );
				float ux = len > 0.001f ? dx / len : 1f, uy = len > 0.001f ? dy / len : 0f;
				place( halos[l], start.x - ux * OVERHANG, start.y - uy * OVERHANG, end.x + ux * OVERHANG, end.y + uy * OVERHANG );
			}
			return l + 1;
		}

		private static void place( Image line, PointF start, PointF end ){
			place( line, start.x, start.y, end.x, end.y );
		}

		//a line image from (x, y) to (toX, toY): its middle row (its origin) laid along the line,
		//turned and stretched about it
		private static void place( Image line, float x, float y, float toX, float toY ){
			float dx = toX - x;
			float dy = toY - y;
			line.angle = (float)(Math.atan2( dy, dx ) * A);
			line.scale.x = (float)Math.sqrt( dx * dx + dy * dy ) / line.width;
			line.x = x - line.origin.x;
			line.y = y - line.origin.y;
		}
	}
}
