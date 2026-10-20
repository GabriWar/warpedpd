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
 * Skill system ported from Skillful Pixel Dungeon by bilboldev (Moussa)
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

package xyz.gabriwar.warpedpixeldungeon.actors.hero.skills;

import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;

/**
 * Warrior: stone plates cover the hero. Each one takes a single blow, up to a cap, and
 * shatters, spraying shards into an adjacent attacker. Broken plates regrow one at a time.
 * Fully trained, a blow a plate stops completely leaves the attacker stunned.
 */
public class StoneSkin extends Skill {

	{
		tag = "CA";
		name = "Stone Skin";
		image = 22;
		tier = 4;
	}

	//each plate stops up to this share of the hero's max health from one blow
	private static final float PLATE = 0.10f;
	private static final int SHARD_DAMAGE = 2;
	private static final float STUN = 2f;
	private static final float REGROW = 10f;

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public int incomingDamageReduction( int damage, Object source ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || damage <= 0) return 0;
		Plates plates = hero.buff( Plates.class );
		if (plates != null && plates.broken >= level) return 0;
		if (plates == null) plates = Buff.affect( hero, Plates.class );
		plates.breakOne();

		int stopped = Math.min( damage, SkillInteractions.ofHealth( hero.HT, PLATE ) );
		if (hero.sprite != null){
			//a plate breaks off him: chips thrown out and up, a crack where he stands (never rocks falling)
			hero.sprite.emitter().burst( SkillBursts.STONE_CHIP, 6 );
			crack( hero.sprite );
			//the plates left as a count, the skill alone when none is
			if (level - plates.broken > 0) counterOn( hero, level - plates.broken, CharSprite.NEUTRAL );
			else chipOn( hero );
		}
		SpatialSound.play( Assets.Sounds.ROCKS, hero, 0.7f, 1.3f );

		if (source instanceof Char && source != hero){
			Char attacker = (Char) source;
			if (attacker.isAlive() && Dungeon.level.adjacent( hero.pos, attacker.pos )){
				CellEmitter.center( attacker.pos ).burst( SkillBursts.STONE_CHIP, 5 );
				attacker.damage( SHARD_DAMAGE, this );
				//+3: a blow the plate stopped whole leaves the attacker reeling
				if (level >= MAX_LEVEL && stopped >= damage && attacker.isAlive()
						&& !attacker.properties().contains( Char.Property.BOSS )){
					Buff.prolong( attacker, Paralysis.class, STUN );
				}
			}
		}
		return stopped;
	}

	/** How long a regrowing plate's pebbles take to gather into him, s; how many; from how far, px. */
	public static final float GATHER = 0.25f;
	public static final int GATHER_PEBBLES = 4;
	public static final float GATHER_FROM = 11f;

	//a plate breaking off him: a crack opening at his boots and a small ring of dust (its thud).
	//Any thread
	public static void crack( final CharSprite s ){
		Fx.post( () -> {
			if (s.parent == null || !s.visible) return;
			float x = SkillVfx.midX( s ), y = s.y + s.height() + 1;
			if (!Fx.seen( x, y )) return;
			FxDecal d = FxDecal.at( x, y, FxFrames.CRACK_11[FxRandom.Int( 2 )], Element.EARTH.ramp.stop( 4 ), 0.7f, 1f, 0.8f, FxRandom.chance( 0.5f ) );
			if (d != null) d.revealCrop( FxDecal.FROM_MIDDLE, 0.08f );
			FxRing.ground( GameScene.layer( GameScene.Layer.FLOOR ), x, y - 2, 9, 17, 0.25f, Element.EARTH.ramp.stop( 1 ), 0.5f, false );
		} );
	}

	//a plate grows back: GATHER_PEBBLES pebbles of stone drawn in from round him into his body over
	//GATHER s (matter, P2). Any thread
	public static void regrow( final CharSprite s ){
		Fx.post( () -> {
			if (s.parent == null || !s.visible) return;
			float cx = SkillVfx.midX( s ), cy = SkillVfx.midY( s ) + 2;
			if (!Fx.seen( cx, cy )) return;
			float a0 = FxRandom.angle();
			for (int i = 0; i < GATHER_PEBBLES; i++){
				float a = a0 + (float) (2 * Math.PI * i / GATHER_PEBBLES);
				float dx = (float) Math.cos( a ) * GATHER_FROM, dy = (float) Math.sin( a ) * GATHER_FROM * 0.6f;
				FxParticle p = SkillVfx.matter( cx + dx, cy + dy, FxBudget.P2 );
				if (p != null) p.look( i % 2 == 0 ? FxFrames.PEBBLE_3 : FxFrames.PEBBLE_2 ).paint( SkillBursts.STONE.step( FxRandom.Float() ) )
						.vel( -dx / GATHER, -dy / GATHER ).life( GATHER ).alpha( 1f, 0.05f, 0.05f ).source( cx, cy );
			}
		} );
	}

	/** exists only while some plates are broken; regrows them one every REGROW turns */
	public static class Plates extends Buff {

		{
			type = buffType.POSITIVE;
		}

		int broken = 0;
		private float regrow = 0f;

		void breakOne(){
			if (broken == 0) regrow = REGROW;
			broken++;
		}

		private int intact(){
			return Math.max( 0, CurrentSkills.skillLevel( StoneSkin.class ) - broken );
		}

		@Override
		public boolean act(){
			regrow -= TICK;
			if (regrow <= 0){
				broken--;
				regrow = REGROW;
				if (target.sprite != null){
					target.sprite.emitter().burst( SkillBursts.GRIT, 3 );
					regrow( target.sprite );
				}
				SpatialSound.play( Assets.Sounds.ROCKS_LIGHT, target, 0.6f, 1.4f );
			}
			spend( TICK );
			if (broken <= 0) detach();
			return true;
		}

		@Override
		public int icon(){ return BuffIndicator.ARMOR; }

		@Override
		public void tintIcon( Image icon ){ icon.hardlight( 0.7f, 0.6f, 0.5f ); }

		@Override
		public float iconFadePercent(){ return Math.max( 0, (REGROW - regrow) / REGROW ); }

		@Override
		public String iconTextDisplay(){ return Integer.toString( intact() ); }

		@Override
		public String desc(){
			return Messages.get( this, "desc", intact(), dispTurns( regrow ) );
		}

		private static final String BROKEN = "broken";
		private static final String REGROW_LEFT = "regrow";

		@Override
		public void storeInBundle( Bundle bundle ){
			super.storeInBundle( bundle );
			bundle.put( BROKEN, broken );
			bundle.put( REGROW_LEFT, regrow );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ){
			super.restoreFromBundle( bundle );
			broken = bundle.getInt( BROKEN );
			regrow = bundle.getFloat( REGROW_LEFT );
		}
	}
}
