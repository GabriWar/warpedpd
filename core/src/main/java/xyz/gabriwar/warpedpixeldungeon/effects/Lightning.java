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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning.Afterglow;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning.Crackle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning.Lines;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning.StrikeGround;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning.StrikeLights;
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

/**
 * Every lightning of the game: one or more arcs (Arc) that strike, dip, strike again and fade in
 * DURATION s, its callback called on the frame it ends, as ever.
 *
 * A minor one (the default: a creature's zap, a shocking weapon's arc) cracks where it strikes in
 * the hero's sight (WeatherOverlay.crackOnly) but never flashes the screen nor shakes it; it
 * throws a small pool of light (POOL_MINOR) on the ground, the water answers it, and a zap
 * striking near another's lights that one's pool again rather than a second one. A major one
 * (major(): a wand's, a potion's, a spell's, a storm's) is what the sky answers: the flash
 * (FlashGate's game gap) and the crack (WeatherOverlay.struck) on the frame it strikes. The first
 * RICH_MAJORS majors at once also get their extras: a leader running down the channel before the
 * strike (LEADER s at LEADER_LIGHT), larger pools and pools along its joints, a scorch on dry
 * ground, a crackle where it lands, sparks again at the re-strike and an afterglow (Afterglow)
 * once it is gone.
 *
 * Its cores go white at the strike, a cooler white in the dip, white again at the re-strike and
 * cool to CORE_COOL over its last 30%, its halos from HALO_HOT to HALO_COOL. Its channel re-forms
 * as it re-strikes keeping its trunk (the joints within KEEP of the way), its fork, twigs and
 * middles thrown anew; its twigs go out at TWIGS_OFF.
 *
 * It is put together without the GPU (it may be on the actor thread) and its constructors draw
 * the game's Random as they always did; once it runs (render thread) every random number it
 * draws is the effects' own (FxRandom). visual() makes one that never draws the game's.
 *
 * A skill's own lightning is tinted (tint, STORM, ARCANE): its cores, halos, glows, pools and
 * afterglow in the skill's colours, never the Shocking enchantment's or a wand's blue-white. A
 * tint changes colours only: its draws, its shape and its time are the same.
 */
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

	/** A rich major's leader: its channel shown leg by leg from where it sets off to where it
	 *  strikes over LEADER s at LEADER_LIGHT, then the strike. */
	static final float LEADER = 0.03f, LEADER_LIGHT = 0.35f;
	/** Its twigs go out at TWIGS_OFF, and show TWIGS_BACK s more after a second re-form. */
	static final float TWIGS_OFF = 0.18f, TWIGS_BACK = 0.04f;
	/** A rich major sparks RESTRIKE_SPARKS more where it strikes at its re-strike. */
	static final float RESTRIKE = 0.12f;
	static final int RESTRIKE_SPARKS = 2;
	/** Its colours: its cores' at the strike, in the dip, at the end; its halos' at the strike and
	 *  at the end; it cools from COOL_FROM. */
	static final int CORE_HOT = 0xFFFFFF, CORE_DIP = 0xE8F0FF, CORE_COOL = 0x9CC0FF;
	static final int HALO_HOT = 0x7FA6FF, HALO_COOL = 0x6A80FF;
	static final float COOL_FROM = 0.7f * DURATION;
	/** How many majors at once get their extras. */
	static final int RICH_MAJORS = 3;
	/** Its light on the ground: a pool GROUND px under where it strikes, POOL_LEVEL of its halo,
	 *  POOL_STORM px wide for a storm's bolt, POOL_MAJOR for a major's arc, POOL_MINOR for a zap;
	 *  a rich major's arcs from JOINT_POOLS_FROM px a POOL_JOINT one at every second joint at
	 *  JOINT_LEVEL of its halo (JOINT_POOLS an arc at most); POOLS a lightning at most. */
	static final int POOL_COLOR = 0xA8C8FF, POOL_STORM = 47, POOL_MAJOR = 31, POOL_MINOR = 15, POOL_JOINT = 15;
	static final float POOL_LEVEL = 0.5f, JOINT_LEVEL = 0.12f, JOINT_POOLS_FROM = 48f;
	static final int JOINT_POOLS = 4, POOLS = 6, JOINT_POOLS_ALL = 8;
	/** How bright its crackle where it lands. */
	static final float CRACKLE_LEVEL = 0.9f;
	/** A skill's tints (tint(int[])): its cores at the strike and at the end, its halos at the
	 *  strike and at the end, its pools. STORM the game's own white-blue (Spark, Blink, Storm Call;
	 *  SkillPalette.LIGHTNING), ARCANE the arcane blue-violet (Arcane Echo, Sorcerer; SkillPalette.ARCANE). */
	public static final int[] STORM = { 0xFFFFFF, 0xE4F0FF, 0x7FA6FF, 0x6A90FF, 0xA8C8FF };
	public static final int[] ARCANE = { 0xFFFFFF, 0xE0D4FF, 0x9070F0, 0x5A38C0, 0xB8A0FF };
	/** A tint's glow where it strikes and where it sets off: its hot halo this far toward white. */
	static final float BLOOM_WHITE = 0.6f, SOURCE_WHITE = 0.45f;

	//the weather's layer of the game's scene, none off it. A test may hand its own
	static Supplier<WeatherOverlay> layer = GameScene::getWeatherOverlay;

	//rich majors alive now (render thread)
	private static int richAlive;

	private float life;
	//when it struck, s into its life: 0 but for a rich major (after its leader), whose stroke,
	//colours and run are timed from its strike so its peak is shown
	private float strikeT;

	private List<Arc> arcs;

	private Callback callback;

	//its first frame is past (begin); it has struck (strike)
	private boolean begun, struck;
	//one the sky answers: a flash, the crack, and its extras if rich
	private boolean major;
	//a storm's bolt (WeatherOverlay.strike): the overlay flashes, thunders and sparks for it
	private boolean stormsOwn;
	//a wand of lightning's zap (thunderous): where it cracks every time, -1 for any other
	private int thunderAt = -1;
	//one of the first RICH_MAJORS majors at once: its leader, pools along its joints, scorch,
	//re-strike sparks and afterglow
	private boolean rich;
	//drawn on the world's camera, on the game's scene: the ground answers it
	private boolean onWorld;
	//its channel forms anew a second time (REFORM_AGAIN)
	private final boolean again;
	//the weather's layer, which draws it over the day/night tint and flashes and sparks for it:
	//none off the game's scene
	private WeatherOverlay sky;
	//its stand-in there (WeatherOverlay.carry)
	private Gizmo above;
	//its pools of light, what share of its halo each shows, and where each lay when it took it: a
	//pool that made way for another light may be that light now, and is let go of (drives)
	private FxLight[] pools;
	private float[] poolShare, poolX, poolY;
	private int poolCount;
	//its colours: the storm's blue-white (the static ones) unless tinted
	private int coreHot = CORE_HOT, coreDip = CORE_DIP, coreCool = CORE_COOL, haloHot = HALO_HOT, haloCool = HALO_COOL,
			poolColor = POOL_COLOR;
	private boolean tinted;

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
		this( arcs, callback, false );
	}

	private Lightning( List<Arc> arcs, Callback callback, boolean visual ) {

		super();

		//a list of its own: a caller may clear its own for the next one while this one lasts
		this.arcs = new ArrayList<>( arcs );
		int legs = 0;
		for (Arc arc : this.arcs) {
			//one made render-side lays its halos and twigs from the effects' own numbers too
			if (visual) arc.fx = true;
			add( arc );
			legs += arc.legs;
			if (legs <= RICH_LEGS) arc.enrich();
		}

		this.callback = callback;

		life = DURATION;
		again = visual ? FxRandom.Int( 2 ) == 0 : Random.Int( 2 ) == 0;
	}

	/** A lightning that draws none of the game's randomness (its arcs' halos, twigs, shimmer and
	 *  re-forms from the effects' own numbers; arcs made with Arc.visual draw none either). */
	public static Lightning visual( List<Arc> arcs, Callback callback ){
		return new Lightning( arcs, callback, true );
	}

	/** One the sky answers: the flash and the crack where it strikes in sight, and (the first
	 *  RICH_MAJORS at once) its extras. Before it is first drawn. */
	public Lightning major() {
		major = true;
		return this;
	}

	public boolean isMajor(){
		return major;
	}

	/**
	 * In a skill's own colours: its cores white-hot `coreHot` at the strike and the re-strike, at
	 * `coreCool` in the dip and cooling to it at the end, its halos from `haloHot` to `haloCool`,
	 * its pools on the ground `pool`; its glows where it strikes and sets off its hot halo toward
	 * white, its afterglow its cool halo. Colours only: the same draws, shape and time. Before it
	 * is first drawn; any thread.
	 */
	public Lightning tint( int coreHot, int coreCool, int haloHot, int haloCool, int pool ){
		tinted = true;
		this.coreHot = coreHot;
		this.coreDip = coreCool;
		this.coreCool = coreCool;
		this.haloHot = haloHot;
		this.haloCool = haloCool;
		this.poolColor = pool;
		int bloom = Ramp.lerp( haloHot, 0xFFFFFF, BLOOM_WHITE ), source = Ramp.lerp( haloHot, 0xFFFFFF, SOURCE_WHITE );
		for (Arc arc : arcs) {
			arc.bloomColor = bloom;
			arc.sourceColor = source;
			arc.tinted = true;
			arc.tint( coreHot, haloHot );
		}
		return this;
	}

	/** The stroke's painted rim (effects.png's lightning frame: white in its middle, 4488FF at
	 *  0.6 on its edges), which a plain multiply (hardlight) cannot turn from blue. */
	static final float RIM_R = 0x44 / 255f, RIM_G = 0x88 / 255f, RIM_B = 0xFF / 255f;

	/**
	 * A stroke lit so its white middle shows `core` and its blue rim `rim`: per channel a multiply
	 * and an offset (noosa's colour, texel x m + a) through those two texels; a channel the rim
	 * already has at full keeps the core's multiply. No allocation.
	 */
	static void edgeTint( Image img, int core, int rim ){
		img.rm = edgeM( RIM_R, (core >> 16) & 0xFF, (rim >> 16) & 0xFF );
		img.gm = edgeM( RIM_G, (core >> 8) & 0xFF, (rim >> 8) & 0xFF );
		img.bm = edgeM( RIM_B, core & 0xFF, rim & 0xFF );
		img.ra = ((core >> 16) & 0xFF) / 255f - img.rm;
		img.ga = ((core >> 8) & 0xFF) / 255f - img.gm;
		img.ba = (core & 0xFF) / 255f - img.bm;
	}

	private static float edgeM( float texel, int core, int rim ){
		float c = core / 255f, e = rim / 255f;
		if (texel >= 0.999f) return c;
		return Math.max( 0f, Math.min( c, (c - e) / (1f - texel) ) );
	}

	/** tint( STORM ) or tint( ARCANE ): { coreHot, coreCool, haloHot, haloCool, pool }. */
	public Lightning tint( int[] t ){
		return tint( t[0], t[1], t[2], t[3], t[4] );
	}

	/** Whether a skill tinted it. */
	public boolean tinted(){
		return tinted;
	}

	/** A new scene: no rich majors alive. */
	public static synchronized void sceneCreated(){
		richAlive = 0;
	}

	private static synchronized boolean claimRich(){
		if (richAlive >= RICH_MAJORS) return false;
		richAlive++;
		return true;
	}

	private static synchronized void releaseRich(){
		if (richAlive > 0) richAlive--;
	}

	/** Rich majors alive now. */
	static synchronized int richAlive(){
		return richAlive;
	}

	@Override
	public void update() {
		if (!begun) {
			begun = true;
			begin();
		}
		float was = DURATION - life;
		life -= Game.elapsed;
		float t = DURATION - life;
		if (!struck && (!rich || t >= LEADER || life < 0)) {
			struck = true;
			if (rich) strikeT = t;
			strike();
		}

		if (life < 0) {

			if (rich) afterglow();
			killAndErase();
			//its images let go of their vertex buffers now, not when the scene changes
			destroy();
			if (callback != null) {
				callback.call();
			}

		} else if (!struck) {

			//the leader: its channel shown leg by leg, faint, toward where it will strike
			for (int i = 0; i < arcs.size(); i++) {
				Arc arc = arcs.get( i );
				arc.lead( t / LEADER );
			}
			super.update();

		} else {

			//from its strike: its last few hundredths of the fade go, the afterglow takes them on
			was -= strikeT;
			t -= strikeT;
			if (crosses( was, t, REFORM ) || (again && crosses( was, t, REFORM_AGAIN ))) {
				for (int i = 0; i < arcs.size(); i++) {
					Arc arc = arcs.get( i );
					arc.restrike();
				}
			}
			float b = flicker( t ), h = halo( t );
			int core = coreColor( t, coreHot, coreDip, coreCool ), halo = haloColor( t, haloHot, haloCool );
			boolean twigs = t < TWIGS_OFF || (again && t >= REFORM_AGAIN && t < REFORM_AGAIN + TWIGS_BACK);
			for (int i = 0; i < arcs.size(); i++) {
				Arc arc = arcs.get( i );
				arc.tint( core, halo );
				arc.twigsShown( twigs );
				if (arc.light( t, b, h )) lit( arc );
			}
			if (rich && sky != null && !stormsOwn && crosses( was, t, RESTRIKE )) {
				for (int i = 0; i < arcs.size(); i++) {
					Arc arc = arcs.get( i );
					if (arc.glows && arc.enriched()) sky.spark( arc.to.x, arc.to.y, RESTRIKE_SPARKS );
				}
			}
			for (int i = 0; i < poolCount; i++) {
				if (pools[i] == null) continue;
				if (drives( i )) pools[i].level( poolShare[i] * h );
				else pools[i] = null;
			}

			super.update();
		}
	}

	//whether its pool `i` is still its own to drive: the same light where it lay, not lit again by
	//another lightning since (StrikeLights.drives), nor gone to make way for another light and
	//perhaps made into that one (a fire's, an impact's)
	private boolean drives( int i ){
		FxLight p = pools[i];
		return p.alive && !p.isPersistent() && p.centerX() == poolX[i] && p.centerY() == poolY[i]
				&& StrikeLights.drives( this, p );
	}

	private static boolean crosses( float was, float now, float at ){
		return was < at && now >= at;
	}

	//an arc lit just now: sparks where it lands (a run's step as the run gets there), and a step
	//over the water lights up
	private void lit( Arc arc ){
		if (arc.sparks > 0 && sky != null && !stormsOwn) {
			sky.spark( arc.to.x, arc.to.y, arc.sparks );
		}
		if (onWorld && arc.faint && arc.sparks > 0) {
			StrikeGround.step( arc.to.x, arc.to.y );
		}
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
		if (rich) {
			rich = false;
			releaseRich();
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

	/** Its arcs come down out of the air onto where they land (a storm call's drop, charged
	 *  steam's bolt): each strikes there however short, and only its foot touches the ground (no
	 *  pools along its joints). Before it is first drawn. */
	public Lightning aloft() {
		for (Arc arc : arcs) {
			arc.aloft();
		}
		return this;
	}

	/** A storm's bolt and its arcs (WeatherOverlay.strike): the overlay flashes, thunders and sparks
	 *  for it, so it does none of the three itself; a major. Before it is first drawn. */
	Lightning stormsOwn() {
		stormsOwn = true;
		major = true;
		return this;
	}

	/** A wand of lightning's zap, striking `cell`: it cracks there with the storm's thunder every
	 *  time, however short and whatever cracked before (WeatherOverlay.thunder), and flashes where
	 *  the hero sees it under the usual gate. Before it is first drawn. */
	public Lightning thunderous( int cell ){
		thunderAt = cell;
		return this;
	}

	/** The same, on the overlay that drew it (wherever the game's scene has its own). */
	Lightning stormsOwn( WeatherOverlay overlay ) {
		sky = overlay;
		return stormsOwn();
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

	/** Its cores' colour `t` seconds in: white at the strike, CORE_DIP at the bottom of the dip
	 *  (0.08 s), white at the re-strike, cooling to CORE_COOL from COOL_FROM. */
	static int coreColor( float t ){
		return coreColor( t, CORE_HOT, CORE_DIP, CORE_COOL );
	}

	/** The same in a tint's colours. */
	static int coreColor( float t, int hot, int dip, int cool ){
		if (t >= COOL_FROM) return Ramp.lerp( hot, cool, Math.min( 1f, (t - COOL_FROM) / (DURATION - COOL_FROM) ) );
		float d = 1f - Math.abs( t - REFORM ) / 0.04f;
		return d > 0 ? Ramp.lerp( hot, dip, d ) : hot;
	}

	/** Its halos' colour `t` seconds in: HALO_HOT, cooling to HALO_COOL from COOL_FROM. */
	static int haloColor( float t ){
		return haloColor( t, HALO_HOT, HALO_COOL );
	}

	/** The same in a tint's colours. */
	static int haloColor( float t, int hot, int cool ){
		if (t < COOL_FROM) return hot;
		return Ramp.lerp( hot, cool, Math.min( 1f, (t - COOL_FROM) / (DURATION - COOL_FROM) ) );
	}

	//its first frame, on the render thread: from here its randomness is the effects' own; whether
	//it is drawn on the world's camera (one a window shows stays there, and the ground does not
	//answer it); one of the first RICH_MAJORS majors gets its extras; drawn over the night's tint
	private void begin(){
		for (Arc arc : arcs) {
			arc.fx = true;
		}
		if (sky == null) sky = layer.get();
		Camera cam = camera();
		boolean world = sky != null && (cam == null || cam == Camera.main);
		onWorld = world && StrikeLights.canLight();
		//extras only for one that strikes somewhere (a storm cloud's crackle in its cloud, a cross
		//through a cell or a hop strike nothing, and take no major's place; a drop out of the air
		//strikes however short)
		boolean strikes = false;
		for (Arc arc : arcs) strikes |= arc.strikes();
		if (major && world && strikes && claimRich()) rich = true;
		if (world && !stormsOwn) {
			above = sky.carry( this );
		}
	}

	//the strike (its first frame, or a rich major's after its leader), on the render thread (the
	//weather's sheet goes to the GPU as soon as it is made): the glows where its arcs strike and
	//set off; on the game's scene the sky's answer for the strike nearest the hero that he sees
	//(a major's flash and crack, a zap's crack), and the ground's
	private void strike() {
		glows();
		if (sky == null) return;
		if (stormsOwn) {
			sky.stormStruck( this );
		} else if (thunderAt != -1) {
			sky.wandThunder( thunderAt );
		} else {
			int cell = struckInSight( arcs, Dungeon.level, Dungeon.hero );
			if (cell != -1) {
				if (major) sky.struck( cell );
				else sky.crackOnly( cell );
			}
		}
		if (onWorld) world();
	}

	//the world's answer where its arcs strike: light on the ground, the crackle, the water's
	//answer or a scorch. Its pools are made as bright as it is now, so none is the dimmest light
	//that makes way for the next (its own next pool); those along its joints only while the lights
	//have room (an accent puts out no other light), and none for an arc out of the air, whose
	//joints are in the sky
	private void world(){
		pools = new FxLight[POOLS + JOINT_POOLS_ALL];
		poolShare = new float[pools.length];
		poolX = new float[pools.length];
		poolY = new float[pools.length];
		float h = halo( DURATION - life - strikeT );
		int mains = 0, joints = 0;
		for (Arc arc : arcs) {
			if (!arc.strikes()) continue;
			float x = arc.to.x, gy = StrikeGround.groundY( arc.to.x, arc.to.y );
			if (mains < POOLS) {
				FxLight p;
				float share = POOL_LEVEL;
				if (major) {
					p = StrikeLights.pool( this, x, gy, arc.storm ? POOL_STORM : POOL_MAJOR, poolColor, share * h, DURATION, 0.06f );
				} else {
					p = StrikeLights.minor( this, x, gy, POOL_MINOR, poolColor, share * h, DURATION, 0.06f );
					if (StrikeLights.relit( this )) share *= StrikeLights.AGAIN;
				}
				if (p != null) {
					keep( p, share );
					mains++;
				}
			}
			if (rich && arc.enriched() && !arc.fromAbove() && arc.length() >= JOINT_POOLS_FROM) {
				for (int j = 2, n = 0; j < arc.path.length - 1 && n < JOINT_POOLS && joints < JOINT_POOLS_ALL; j += 2, n++) {
					if (FxLight.live() >= FxBudget.lightCap()) break;
					PointF at = arc.path[j];
					FxLight p = StrikeLights.pool( this, at.x, at.y + StrikeGround.GROUND, POOL_JOINT, poolColor, JOINT_LEVEL * h, DURATION, 0.06f );
					if (p == null) break;
					keep( p, JOINT_LEVEL );
					joints++;
				}
			}
			if (arc.enriched() && Fx.seen( arc.to.x, arc.to.y )) {
				Crackle.at( sky, arc.to.x, arc.to.y, CRACKLE_LEVEL );
			}
			StrikeGround.struck( arc.to.x, arc.to.y, major, rich );
		}
		//its pools as bright as it is now
		for (int i = 0; i < poolCount; i++) {
			if (drives( i )) pools[i].level( poolShare[i] * h );
			else pools[i] = null;
		}
	}

	//a pool of its own to drive at `share` of its halo, where it lies now
	private void keep( FxLight p, float share ){
		pools[poolCount] = p;
		poolShare[poolCount] = share;
		poolX[poolCount] = p.centerX();
		poolY[poolCount++] = p.centerY();
	}

	//what it leaves on the eye: its striking arcs' cores, a moment longer (Afterglow)
	private void afterglow(){
		if (sky == null) return;
		Afterglow glow = null;
		for (Arc arc : arcs) {
			if (!arc.strikes() || !arc.enriched()) continue;
			if (glow == null) {
				glow = Afterglow.begin( sky );
				if (glow == null) return;
			}
			if (!arc.afterglow( glow )) break;
		}
		//a tinted one leaves its own cool halo on the eye
		if (tinted && glow != null) {
			for (Gizmo g : glow.membersView()) {
				if (g instanceof Image) edgeTint( (Image) g, haloCool, haloCool );
			}
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
			if (arc.glows && !arc.joined && !near( lit, arc.from )) {
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

	/** Whether the hero sees where any of its arcs sets off or lands. */
	public boolean seen(){
		for (Arc arc : arcs) {
			if (Fx.seen( arc.from.x, arc.from.y ) || Fx.seen( arc.to.x, arc.to.y )) return true;
		}
		return false;
	}

	/** Its arcs, as made. */
	List<Arc> arcs(){
		return arcs;
	}

	/**
	 * Of its arcs that strike (Arc.glows: 30 px and more, no cross through a cell; or one out of the
	 * air, Arc.aloft, however short), the cell where
	 * the one nearest the hero lands that he sees, -1 for none: where it brings the storm's flash
	 * and crack from.
	 */
	static int struckInSight( List<Arc> arcs, Level level, Hero hero ){
		if (level == null || hero == null || level.heroFOV == null) return -1;
		int w = level.width(), h = level.height(), best = -1;
		for (Arc arc : arcs) {
			if (!arc.glows && !arc.aloft) continue;
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
	//(three legs), with one to three twigs from 40 px, and a bloom (a 31 px glow of the light sheet,
	//47 px from 60 px) and sparks where it lands and an 11 px glow where it sets off. A step of a run
	//over the water (crawl) is thinner and fainter, and lights as the run gets there. It is put
	//together with no GPU call (it may be on the actor thread): its glows are made on its first frame.
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
		//as it re-strikes its joints within KEEP of the way hold (its trunk), the rest are thrown anew
		static final float KEEP = 0.6f;
		//its halo (lightning_halo.png) tinted at up to HALO_AM, overhanging each joint by OVERHANG px
		//so two legs' meet with no notch on a bend's outside
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
		//where it strikes, a glow of the light sheet (GLOW_WIDE from GLOW_WIDE_FROM of k) at BLOOM_AM,
		//and STRIKE_SPARKS sparks; where it sets off, a SOURCE glow at SOURCE_AM
		private static final int BLOOM_COLOR = 0xD8E4FF, SOURCE_COLOR = 0xC8D8FF, STRIKE_SPARKS = 3;
		private static final float BLOOM_AM = 0.9f, SOURCE_AM = 0.45f;
		static final int[] GLOW = FxFrames.Light.GLOW_31, GLOW_WIDE = FxFrames.Light.GLOW_47, SOURCE = FxFrames.Light.GLOW_11;
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
		//a storm's bolt out of the sky: it strikes, though the storm draws its glow
		boolean storm;
		//it comes down out of the air: it strikes where it lands however short, only its foot on
		//the ground
		boolean aloft;
		//it carries on from another arc (a thunderbolt's foot): no glow where it sets off
		boolean joined;
		//the share of a leader's run it takes: from leadFrom to leadTo of it
		float leadFrom = 0f, leadTo = 1f;
		//its randomness is the effects' own: made render-side, or running
		boolean fx;
		//a run over the water's faint step
		boolean faint;
		//its glows' colours where it strikes and where it sets off (a tint's own); in a skill's colours
		int bloomColor = BLOOM_COLOR, sourceColor = SOURCE_COLOR;
		boolean tinted;

		private final float k;
		private final boolean forked, fitted;
		private final int twigCount;
		private boolean lit;
		//its twigs are shown (they go out before it fades)
		private boolean twigsOn = true;
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
			this( from, to, joints( from, to ), wide( from, to ), FORK, PointF.distance( from, to ) >= GLOW_FROM, true, false );
		}

		//by the numbers given: the storm's bolt (WeatherOverlay), its shape the storm's (no twigs, its
		//fork as long as k makes it). Its fork is only rolled for, and its glows only made, where the
		//arc is long enough to have them
		Arc(PointF from, PointF to, int joints, float k, float forkChance, boolean glows){
			this( from, to, joints, k, forkChance, glows, false, false );
		}

		/** An arc sized by its length, made render-side: it draws none of the game's randomness. */
		public static Arc visual( PointF from, PointF to ){
			return new Arc( from, to, joints( from, to ), wide( from, to ), FORK, PointF.distance( from, to ) >= GLOW_FROM, true, true );
		}

		/** The storm's shape by the numbers given (WeatherOverlay, ThunderBolt), made render-side. */
		static Arc visual( PointF from, PointF to, int joints, float k, float forkChance, boolean glows ){
			return new Arc( from, to, joints, k, forkChance, glows, false, true );
		}

		private Arc(PointF from, PointF to, int joints, float k, float forkChance, boolean glows, boolean byLength, boolean fx){
			this.fx = fx;
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
			forked = joints >= 3 && forkChance > 0 && rf() < forkChance;
			twigCount = byLength && joints >= 3 && length >= TWIGS_FROM ? 1 + ri( 3 ) : 0;
			legs = joints + (forked ? 2 : 0);

			mids = new float[2 * legs];
			cores = new Image[2 * legs];
			for (int i = 0; i < cores.length; i++) {
				cores[i] = new Image(Effects.get(Effects.Type.LIGHTNING));
				cores[i].origin.set( 0, cores[i].height / 2 );
				if (i >= 2 * joints) cores[i].scale.y = FORK_CORE[(i - 2 * joints) / 2];
				//dark till its lightning lights it: one added between a frame's update and its draw
				//shows nothing that frame
				Lines.shade( cores[i], 0f );
				add( cores[i] );
			}

			form( null );
			layout();
		}

		//its random numbers: the game's while it is put together (as they always were), the
		//effects' own once it runs or when it was made render-side
		private float rf(){
			return fx ? FxRandom.Float() : Random.Float();
		}

		private float rf( float min, float max ){
			return fx ? FxRandom.Float( min, max ) : Random.Float( min, max );
		}

		private int ri( int n ){
			return fx ? FxRandom.Int( n ) : Random.Int( n );
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
		 * For a bolt coming straight down this is the storm's zigzag as it always was. Re-struck,
		 * the joints of `keep` within KEEP of the way hold.
		 */
		private PointF[] channel( PointF[] keep ){
			PointF[] p = new PointF[joints + 1];
			p[0] = from;
			float dx = to.x - from.x, dy = to.y - from.y;
			float len = (float)Math.sqrt( dx * dx + dy * dy );
			float ux = len > 0 ? dx / len : 0f, uy = len > 0 ? dy / len : 1f;
			for (int i = 1; i < joints; i++) {
				float t = i / (float)joints;
				if (keep != null && t <= KEEP) {
					p[i] = keep[i];
					continue;
				}
				float across = k * rf( -ACROSS, ACROSS ), along = k * rf( -ALONG, ALONG );
				p[i] = new PointF( from.x + dx * t + uy * across + ux * along,
						from.y + dy * t - ux * across + uy * along );
			}
			p[joints] = to;
			return p;
		}

		//a fork off the joint `at`: out to one side 10-18 px and on 14-24, then 6-14 and 10-20
		//more, by k, dying out short of where the arc strikes (the storm's)
		private PointF[] fork( PointF at, float k ){
			float dx = to.x - from.x, dy = to.y - from.y;
			float len = (float)Math.sqrt( dx * dx + dy * dy );
			float ux = dx / len, uy = dy / len;
			float side = ri( 2 ) == 0 ? -1 : 1;
			float out = k * side * rf( 10f, 18f ), on = k * rf( 14f, 24f );
			PointF mid = new PointF( at.x + uy * out + ux * on, at.y - ux * out + uy * on );
			out = k * side * rf( 6f, 14f );
			on = k * rf( 10f, 20f );
			PointF end = new PointF( mid.x + uy * out + ux * on, mid.y - ux * out + uy * on );
			return new PointF[]{ at, mid, end };
		}

		/** Its channel anew between the same two ends: its joints, its fork (if it has one) off the
		 *  joint a third of the way along, each leg's middle, its twigs. */
		void form(){
			form( null );
		}

		/** As its lightning re-strikes: the trunk holds (its joints within KEEP of the way), the
		 *  rest of its channel, its fork, twigs and middles are thrown anew. */
		void restrike(){
			form( path );
		}

		private void form( PointF[] keep ){
			path = channel( keep );
			if (forked) {
				PointF at = path[Math.max( 1, Math.round( joints / 3f ) )];
				//sized by its length, it runs on only as far as the room left before it strikes
				fork = fork( at, fitted ? Math.min( k, PointF.distance( at, to ) * FORK_ROOM / FORK_REACH ) : k );
			}
			for (int i = 0; i < mids.length; i++) {
				mids[i] = rf( -MID, MID );
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
				PointF at = path[1 + ri( joints - 1 )];
				float side = ri( 2 ) == 0 ? -1f : 1f;
				float a = way + side * (float)Math.toRadians( rf( 25f, 70f ) );
				float len = rf( TWIG_MIN, TWIG_MAX );
				PointF bend = new PointF( at.x + (float)Math.cos( a ) * len * 0.6f, at.y + (float)Math.sin( a ) * len * 0.6f );
				a -= side * (float)Math.toRadians( rf( 15f, 40f ) );
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
				halos[l].hardlight( HALO_HOT );
				Lines.shade( halos[l], 0f );
				add( halos[l] );
			}
			if (twigCount > 0) {
				twigs = new PointF[twigCount][];
				twigLines = new Image[2 * twigCount];
				for (int i = 0; i < twigLines.length; i++) {
					twigLines[i] = new Image(Effects.get(Effects.Type.LIGHTNING));
					twigLines[i].origin.set( 0, twigLines[i].height / 2 );
					twigLines[i].scale.y = TWIG_CORE;
					Lines.shade( twigLines[i], 0f );
					add( twigLines[i] );
				}
				twig();
			}
			layout();
		}

		/** Whether its lightning laid its halos and twigs (its first RICH_LEGS legs). */
		boolean enriched(){
			return halos != null;
		}

		/** Whether it strikes the ground where it lands: a bloom's arc, a storm's bolt, or one out
		 *  of the air (aloft). */
		boolean strikes(){
			return glows || storm || aloft;
		}

		/** Whether it comes down out of the sky, only its foot on the ground: a storm's bolt, or
		 *  one out of the air (aloft). */
		boolean fromAbove(){
			return storm || aloft;
		}

		/** Its length, px. */
		float length(){
			return PointF.distance( from, to );
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
			aloft = false;
			sparks = 0;
		}

		/** A storm's bolt out of the sky (WeatherOverlay): it strikes the ground, the storm drawing
		 *  its glow. */
		Arc storm(){
			storm = true;
			return this;
		}

		/** It comes down out of the air onto where it lands (Lightning.aloft). */
		Arc aloft(){
			aloft = true;
			return this;
		}

		/** It carries on from another arc: no glow where it sets off; its share of the leader's run
		 *  from `leadFrom` to `leadTo`. */
		Arc joined( float leadFrom, float leadTo ){
			joined = true;
			return lead( leadFrom, leadTo );
		}

		/** Its share of the leader's run, from `leadFrom` to `leadTo` of it. */
		Arc lead( float leadFrom, float leadTo ){
			this.leadFrom = leadFrom;
			this.leadTo = leadTo;
			return this;
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

		/**
		 * The leader, `p` (0..1) of its run done: its channel's legs shown from where it sets off,
		 * one by one, as far as its share of the run has come, at LEADER_LIGHT; its fork, twigs and
		 * a run's steps not yet.
		 */
		void lead( float p ){
			float share = leadTo > leadFrom ? (p - leadFrom) / (leadTo - leadFrom) : 1f;
			int shown = faint || share <= 0f ? 0 : (int)Math.ceil( Math.min( 1f, share ) * joints );
			for (int l = 0; l < legs; l++) {
				float a = l < joints && l < shown ? LEADER_LIGHT : 0f;
				Lines.shade( cores[2 * l], a );
				Lines.shade( cores[2 * l + 1], a );
				if (halos != null) Lines.shade( halos[l], a * HALO_AM );
			}
			if (twigLines != null) {
				for (Image t : twigLines) Lines.shade( t, 0f );
			}
		}

		private void shade( float b, float h ){
			for (int l = 0; l < legs; l++) {
				boolean channel = l < joints;
				float a = b * (channel ? (faint ? CRAWL_LIGHT : 1f) : FORK_LIGHT[l - joints]);
				Lines.shade( cores[2 * l], a );
				Lines.shade( cores[2 * l + 1], a );
				if (halos != null) {
					Lines.shade( halos[l], h * HALO_AM * (channel ? (faint ? CRAWL_HALO : 1f) : FORK_HALO[l - joints]) );
				}
			}
			if (twigLines != null) {
				for (Image t : twigLines) Lines.shade( t, twigsOn ? b * TWIG_LIGHT : 0f );
			}
			if (glow != null) {
				Lines.shade( glow, BLOOM_AM * b );
			}
			if (source != null) {
				Lines.shade( source, SOURCE_AM * b );
			}
		}

		/** Its cores' and twigs' colour, and its halos'. */
		void tint( int core, int halo ){
			if (tinted) {
				//a skill's colours: its stroke's painted blue rim in the halo's colour too (edgeTint)
				for (Image c : cores) edgeTint( c, core, halo );
				if (twigLines != null) {
					for (Image t : twigLines) edgeTint( t, core, halo );
				}
				if (halos != null) {
					for (Image hl : halos) hl.hardlight( halo );
				}
				return;
			}
			float r = ((core >> 16) & 0xFF) / 255f, g = ((core >> 8) & 0xFF) / 255f, bl = (core & 0xFF) / 255f;
			for (Image c : cores) c.hardlight( r, g, bl );
			if (twigLines != null) {
				for (Image t : twigLines) t.hardlight( r, g, bl );
			}
			if (halos != null) {
				for (Image hl : halos) hl.hardlight( halo );
			}
		}

		/** Its twigs shown or not (they go out before it fades). */
		void twigsShown( boolean shown ){
			twigsOn = shown;
		}

		/** Its cores copied into an afterglow; false once that is full. */
		boolean afterglow( Afterglow into ){
			for (int l = 0; l < joints; l++) {
				if (!into.line( cores[2 * l] ) || !into.line( cores[2 * l + 1] )) return false;
			}
			return true;
		}

		@Override
		public void update() {
			layout();
		}

		//on its first frame, on the render thread. A glow of the light sheet where it strikes, wider
		//from FULL px on
		void bloom(){
			glow = glowAt( to, k >= 1f ? GLOW_WIDE : GLOW, bloomColor );
		}

		//and a smaller one where it sets off
		void source(){
			source = glowAt( from, SOURCE, sourceColor );
		}

		//a painted glow of the light sheet at its own size, on whole pixels, its middle on the point
		private Image glowAt( PointF at, int[] f, int color ){
			Image g = new Image( Assets.Effects.FX_LIGHT );
			g.frame( g.texture.uvRect( f[0], f[1], f[0] + f[2], f[1] + f[3] ) );
			g.origin.set( f[2] / 2f, f[3] / 2f );
			g.hardlight( color );
			g.x = Math.round( at.x - f[2] / 2f );
			g.y = Math.round( at.y - f[3] / 2f );
			Lines.shade( g, 0f );
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
			float x2 = (start.x + end.x) / 2 + mids[2 * l] + rf( -JITTER, JITTER );
			float y2 = (start.y + end.y) / 2 + mids[2 * l + 1] + rf( -JITTER, JITTER );
			Lines.place( cores[2 * l], start.x, start.y, x2, y2 );
			Lines.place( cores[2 * l + 1], x2, y2, end.x, end.y );
			if (halos != null) {
				float dx = end.x - start.x, dy = end.y - start.y;
				float len = (float)Math.sqrt( dx * dx + dy * dy );
				float ux = len > 0.001f ? dx / len : 1f, uy = len > 0.001f ? dy / len : 0f;
				Lines.place( halos[l], start.x - ux * OVERHANG, start.y - uy * OVERHANG, end.x + ux * OVERHANG, end.y + uy * OVERHANG );
			}
			return l + 1;
		}

		private static void place( Image line, PointF start, PointF end ){
			Lines.place( line, start.x, start.y, end.x, end.y );
		}
	}
}
