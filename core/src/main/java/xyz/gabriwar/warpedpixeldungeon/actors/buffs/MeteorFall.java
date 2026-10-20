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

package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.ClosingRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;

/**
 * A meteor the mage has called: the marked tile glows for a turn, then the stone
 * comes down on it and the eight cells around it. Flame where it lands, a blast that
 * shakes the floor, and a burn on whatever it caught. It never hurts the mage or an
 * ally, and it never breaks the floor.
 *
 * Seen as a stone of fire, never a bomb: the mark gathers heat (embers lifting, a pool of
 * light breathing, a ring stepping in on the cell each beat: HeatMark), the stone falls as the
 * kit's METEOR (SkillFX.Fall) on the bomb's own draws and frame, and it strikes as earth and heat
 * (impact): cracks and a scorch, chips of stone thrown out, a ring of heat, embers, a shake, the
 * stone's crash; flame only where something truly burns.
 */
public class MeteorFall extends Buff {

	{
		type = buffType.NEUTRAL;
		announced = false;
	}

	private int cell = -1;
    private int floorDepth, floorBranch;
	private int level = 1;
	private boolean fallen = false;

	public void set( int cell, int level ){
		this.cell = cell; floorDepth=Dungeon.depth; floorBranch=Dungeon.branch;
		this.level = level;
		spend( TICK );
		//its mark is known only now (attached first, then set): the heat gathers on it from here
		if (target != null && target.sprite != null && mark == null) mark = HeatMark.at( cell );
	}

	@Override
	public boolean act() {
		if (floorDepth!=Dungeon.depth || floorBranch!=Dungeon.branch || cell < 0 || cell >= Dungeon.level.length() || !(target instanceof Hero)){
			detach();
			return true;
		}
		Hero hero = (Hero) target;
        // Resolve gameplay on the actor turn; the cosmetic explosion follows the
        // falling sprite and never owns actor scheduling.
        if (Dungeon.level.heroFOV[cell] && hero.sprite != null && hero.sprite.parent != null){
            SkillFX.rain(cell, new Bomb(), SkillFX.Fall.METEOR, 1, this::impactFX);
        }else{
            SpatialSound.play(Assets.Sounds.WALL_BREAK_1, cell);
        }

        for (int c : xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions.area(cell,1)){
            // No terrain conversion, bomb detonation or fire blob: doors and
            // destructible scenery survive the impact.
			Char ch = Actor.findChar( c );
			if (ch == null || ch == hero || ch.alignment == Char.Alignment.ALLY) continue;
			int dmg = Random.NormalIntRange( 8 + 4 * level, 16 + 8 * level );
			if (c == cell) dmg = Math.round( dmg * 1.5f );
			ch.damage( dmg, this );
			if (ch.isAlive()){
				Buff.affect( ch, Burning.class ).reignite( ch );
				if (level >= 3) Buff.affect( ch, Paralysis.class, 1f );
			}
		}
        if(level>=3) SkillField.place(hero,SkillField.CRACK,level,1,
                xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions.area(cell,1));
        fallen = true;
		detach();
		return true;
	}


    private void impactFX(){
        if(Dungeon.level==null||floorDepth!=Dungeon.depth||floorBranch!=Dungeon.branch
                ||cell<0||cell>=Dungeon.level.length()||!Dungeon.level.heroFOV[cell])return;
        //the stone strikes as earth and heat (never a bomb's blast): chips of it thrown out, and
        //flame only on a cell where something truly burns, embers on the rest (the chips' burst
        //keeps the blast's 30: each emit draws the game's Random twice for its spawn point)
        impact(cell);
        CellEmitter.center(cell).burst(SkillBursts.STONE_CHIP,30);
        for(int c:xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions.area(cell,1)){
            if(Dungeon.level.heroFOV[c])CellEmitter.get(c).burst(burning(c)?FlameParticle.FACTORY:SkillBursts.EMBER_MOTE,4);
        }
        SpatialSound.play(Assets.Sounds.WALL_BREAK_1, cell);
        SpatialSound.play(Assets.Sounds.HIT_CRUSH,cell,1f,.7f);
    }

    //something burning stands on the cell now
    private static boolean burning(int c){
        Char ch=Actor.findChar(c);
        return ch!=null&&ch.buff(Burning.class)!=null;
    }

	/** The meteor's heat: its ring, its pool, its mark's. */
	public static final int HEAT = 0xFF9030;
	/** The stone's chips thrown out, how many. */
	public static final int CHIPS = 8;

	/**
	 * The stone striking the cell: the earth's impact a fifth bigger, a ring of heat spreading 13
	 * to 55 px, a large scorch and two cracks opening, CHIPS chips of stone thrown out all round
	 * (falling and bouncing, never dropping from above), six embers, a pool of heat and a shake.
	 * (no rising dust: it would cloud whoever was struck; the dust ring is the ground's answer).
	 * Kit only: nothing of the game's Random. Any thread.
	 */
	public static void impact( final int cell ){
		Fx.post( () -> {
			if (!SkillVfx.valid( cell ) || !SkillVfx.seen( cell )) return;
			float x = SkillVfx.cx( cell ), y = SkillVfx.cy( cell );
			//the strike: a white-hot core for its moment, the earth's impact, the ring of heat and the
			//shockwave of dust behind it
			FxLight.glow( x, y - 2, 31, 0xFFE0B0, 1f ).core().life( 0, 0.05f, 0.2f );
			FxRecipes.impact( x, y, Element.EARTH, 1.2f );
			Group floor = GameScene.layer( GameScene.Layer.FLOOR );
			FxRing.ground( floor, x, y + 4, 13, 55, 0.45f, HEAT, 0.8f, true );
			FxRing dust = FxRing.ground( floor, x, y + 4, 9, 41, 0.5f, Element.EARTH.ramp.stop( 1 ), 0.6f, false );
			if (dust != null) dust.delay( 0.05f );
			FxDecal.at( x, y + 4, FxFrames.SCORCH_L, FxRecipes.SCORCH_COLOR, 0.6f, 4f, 3f, FxRandom.chance( 0.5f ) );
			int crack = Element.EARTH.ramp.stop( 4 );
			FxDecal a = FxDecal.at( x - 5, y + 3, FxFrames.CRACK_17[0], crack, 0.8f, 2.5f, 1f, false );
			if (a != null) a.revealCrop( FxDecal.FROM_MIDDLE, 0.1f );
			FxDecal b = FxDecal.at( x + 5, y + 7, FxFrames.CRACK_17[1], crack, 0.8f, 2.5f, 1f, true );
			if (b != null) b.revealCrop( FxDecal.FROM_MIDDLE, 0.12f );
			for (int i = 0; i < CHIPS; i++){
				float ang = (float) Math.toRadians( i * 45 + FxRandom.Float( -15f, 15f ) );
				float speed = FxRandom.Float( 45f, 75f );
				FxParticle p = SkillVfx.matter( x, y + 2, FxBudget.P2 );
				if (p == null) continue;
				p.look( i % 3 == 0 ? FxFrames.PEBBLE_3V : i % 2 == 0 ? FxFrames.PEBBLE_3 : FxFrames.PEBBLE_2 )
						.paint( SkillBursts.STONE.step( FxRandom.Float() ) )
						.vel( (float) Math.cos( ang ) * speed, (float) Math.sin( ang ) * speed * 0.5f - FxRandom.Float( 35f, 55f ) )
						.gravity( 220f ).ground( y + 5 + (float) Math.sin( ang ) * 4f, FxParticle.RING, 0.3f ).landDry( FxParticle.BOUNCE )
						.strength( 0.2f ).spin( 0.08f ).life( 0.75f ).alpha( 1f, 0, 0.2f );
			}
			FxRecipes.embers( x, y, Element.FIRE, 8 );
			FxLight.pool( x, y + 4, 47, HEAT, 0.7f ).life( 0, 0.08f, 0.45f );
			Fx.shake( 3, 0.35f );
		} );
	}

	private HeatMark mark;

	@Override
	public void fx( boolean on ) {
		//the mark glows while the stone is on its way: embers lifting off it (never a red alarm),
		//and the heat gathering on it till it falls
		if (on && cell >= 0 && !fallen && Dungeon.level.heroFOV[cell]){
			CellEmitter.center( cell ).burst( SkillBursts.EMBER_MOTE, 4 );
		}
		//the heat only on the floor it was called on (the hero may have left it before it fell;
		//the burst above keeps master's draw as it was)
		boolean here = floorDepth == Dungeon.depth && floorBranch == Dungeon.branch;
		if (on && here && cell >= 0 && !fallen && mark == null){
			mark = HeatMark.at( cell );
		} else if (!on && mark != null){
			mark.release();
			mark = null;
		}
	}

	/**
	 * The heat gathering on a meteor's mark while the stone is on its way: a pool of its light on
	 * the cell, breathing and swelling, and every BEAT s a ring of it stepping in from 23 to 9 px
	 * (ClosingRing's steps, on a cell). Made without the GPU (any thread), put on the floor's layer
	 * by Fx.post, let go of with release (Fx.dispose; its pool put out). One ring quad and one pool.
	 */
	public static final class HeatMark extends Group {

		public static final float BEAT = 0.5f, CLOSE = 0.42f, POOL = 0.45f, SWELL = 0.3f, SWELL_FOR = 1f;
		/** Embers lifting off the mark each beat. */
		public static final int EMBERS = 2;
		public static final int RING_FROM = 23, RING_TO = 9, POOL_W = 31;

		private final int cell;
		private final FxPic ring;
		private FxLight pool;
		private int poolSetups;
		private float age;
		private int shown = -1, beats = -1;
		private volatile boolean gone;

		private HeatMark( int cell ){
			this.cell = cell;
			ring = new FxPic( SkillVfx.SHEET );
			ring.additive = true;
			ring.lit( HEAT );
			ring.visible = false;
			add( ring );
		}

		/** The heat on a cell, shown till released. Any thread. */
		public static HeatMark at( int cell ){
			final HeatMark m = new HeatMark( cell );
			Fx.post( () -> {
				Group floor = GameScene.layer( GameScene.Layer.FLOOR );
				if (m.gone || floor == null) return;
				floor.add( m );
			} );
			return m;
		}

		/** Let go of (Fx.dispose, the pool put out), whether or not it was shown yet. Any thread. */
		public void release(){
			gone = true;
			Fx.dispose( this );
		}

		@Override
		public void update(){
			super.update();
			age += Game.elapsed;
			boolean seen = SkillVfx.seen( cell );
			float x = SkillVfx.cx( cell ), y = SkillVfx.cy( cell ) + 4;
			//the pool: made on its first seen frame, swelling as the stone comes
			if (seen && (pool == null || !pool.alive || pool.setups() != poolSetups)){
				pool = FxLight.pool( x, y, POOL_W, HEAT, POOL ).flicker( FxLight.BREATH );
				poolSetups = pool.setups();
			} else if (!seen && pool != null){
				putOut();
			}
			if (pool != null) pool.level( POOL + SWELL * Math.min( 1f, age / SWELL_FOR ) );
			//each beat a couple of embers lift off it, the heat rising
			int beat = (int) (age / BEAT);
			if (beat != beats){
				beats = beat;
				if (seen) for (int i = 0; i < EMBERS; i++){
					FxParticle e = SkillVfx.glow( x + FxRandom.Float( -6f, 6f ), y + FxRandom.Float( -2f, 2f ), FxBudget.P2 );
					if (e != null) e.look( FxRandom.chance( 0.5f ) ? FxFrames.EMBER_2 : FxFrames.EMBER_1 )
							.paint( SkillBursts.FIRE.at( FxRandom.Float( 0.1f, 0.6f ) ) ).vel( FxRandom.Float( -3f, 3f ), FxRandom.Float( -16f, -10f ) )
							.sway( 2f, 4f ).life( 0.6f ).alpha( 1f, 0.05f, 0.3f );
				}
			}
			//the ring: stepped in from RING_FROM to RING_TO over CLOSE s of every BEAT
			float p = (age % BEAT) / CLOSE;
			boolean on = seen && p < 1f;
			ring.visible = on;
			if (!on) return;
			int f = ClosingRing.frameAt( FxFrames.ringIndex( RING_FROM ), FxFrames.ringIndex( RING_TO ), p );
			if (f != shown){
				shown = f;
				ring.show( FxFrames.RINGS[f] );
			}
			ring.place( x, y );
			ring.am = 0.35f + 0.5f * p;
		}

		private void putOut(){
			if (pool != null && pool.alive && pool.setups() == poolSetups) pool.putOut( 0.2f );
			pool = null;
		}

		@Override
		public void kill(){
			putOut();
			super.kill();
		}

		@Override
		public void destroy(){
			putOut();
			super.destroy();
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.NONE;
	}

	private static final String CELL = "cell", LEVEL = "level";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put("floor_depth",floorDepth);bundle.put("floor_branch",floorBranch);
		bundle.put( CELL, cell );
		bundle.put( LEVEL, level );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		floorDepth=bundle.contains("floor_depth")?bundle.getInt("floor_depth"):Dungeon.depth;floorBranch=bundle.contains("floor_branch")?bundle.getInt("floor_branch"):Dungeon.branch;
		cell = bundle.getInt( CELL );
		level = bundle.getInt( LEVEL );
	}
}
