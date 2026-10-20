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


import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.utils.Random;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RuneFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.items.wands.DamageWand;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;

public class Sorcerer extends PassiveSkillB2 {

	//its damage is already a share of a blow, a hit or a health pool, so it grows with the hero on its own
	@Override
	public boolean weaponScaled(){ return true; }


	{
		name = "Sorcerer";
		image = 34;
		tier = 2;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	/** Wand's zapper calls this once the bolt has landed: a damaging zap can overload the enemy it hit */
	public static void onZap( Wand wand, Char target, int cell ){
		Hero hero = Dungeon.hero;
		if (!(wand instanceof DamageWand) || hero == null || hero.heroSkills == null) return;
		Sorcerer skill = hero.heroSkills.get( Sorcerer.class );
		if (skill == null || skill.level <= 0) return;
		if (target == null || target == hero || !target.isAlive() || target.alignment != Char.Alignment.ENEMY
				|| target.buff( Overload.class ) != null) return;
		//20/35/50%
		if (Random.Int( 100 ) >= 5 + 15 * skill.level) return;

		DamageWand dw = (DamageWand) wand;
		int dmg = Math.max( 1, Hero.heroDamageIntRange( dw.min(), dw.max() ) / 2 );
		Overload overload = xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff.affect( target, Overload.class );
		overload.set( dmg );
		if (target.sprite != null){
			overloaded( target.sprite );
			overload.runes = charging( target.sprite );
			Skill.chipOn( Sorcerer.class, target );
			xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow.buffIcon( target, target.buff( Overload.class ) );
		}
		SpatialSound.play( Assets.Sounds.LIGHTNING, target, 0.5f, 1.4f );
	}

	/** Magic caught in an enemy: arcane sparks off its middle and an arcane bloom gathering in it
	 *  (whatever wand cast it: the overload is the skill's, not the wand's element). Any thread
	 *  (one burst of the sprite's emitter, as the sparks'). */
	public static void overloaded( CharSprite s ){
		s.centerEmitter().burst( SkillBursts.ARCANE_SPARK, 8 );
		FxRecipes.castBloom( SkillVfx.midX( s ), SkillVfx.midY( s ), Element.ARCANE );
	}

	/** The overload's runes: Enchanting's ring of RuneFX.RING_N arcane runes round the feet, spinning
	 *  up to SPIN_TO rad/s over SPIN_FOR s and held at most CHARGE s (the magic caught, winding up);
	 *  at the discharge thrown out at RELEASE px/s, fading over RELEASE_FOR s. */
	public static final float SPIN_TO = 8f, SPIN_FOR = 0.3f, CHARGE = 1.5f, RELEASE = 40f, RELEASE_FOR = 0.25f;
	/** Below +3 an overloaded body dying first takes the magic with it: its ring fades where it
	 *  is over FIZZLE s. */
	public static final float FIZZLE = 0.2f;

	/** The ring winding up round an overloaded body (followed). Any thread. */
	public static RuneFX.Runes charging( CharSprite s ){
		return RuneFX.ring( s, SkillPalette.RUNE_ARCANE, CHARGE ).spin( SPIN_TO, SPIN_FOR );
	}

	/** The discharge at `cell`: the held ring's runes thrown out; its ring gone by then (a long wait,
	 *  a save), a ring at full spin shown for its fade-in (RuneFX.RING_IN) and thrown out. Any thread. */
	public static void discharged( RuneFX.Runes held, final int cell ){
		if (held != null && held.shown() && held.alive){
			held.release( RELEASE, RELEASE_FOR );
		} else Fx.post( () -> {
			if (!SkillVfx.seen( cell )) return;
			final RuneFX.Runes ring = RuneFX.ring( SkillVfx.cx( cell ), SkillVfx.raisedY( cell ) + 6, RuneFX.RING_N,
					SkillPalette.RUNE_ARCANE, RuneFX.RING_RX, RuneFX.RING_RY, SPIN_TO, RuneFX.RING_IN + RELEASE_FOR );
			FxTimeline.start().at( RuneFX.RING_IN, () -> ring.release( RELEASE, RELEASE_FOR ) );
		} );
	}

	//+3: an overloaded enemy that dies before it discharges bursts at once, twice as wide
	@Override
	public void onEnemyDeath( Mob mob, Object cause ){
		if (mob == null) return;
		Overload o = mob.buff( Overload.class );
		if (o == null) return;
		if (level < MAX_LEVEL){
			//nothing bursts: the ring winding up round it fades where it is
			if (o.runes != null) o.runes.lift( 0f, FIZZLE );
			o.runes = null;
			return;
		}
		final int pos = mob.pos;
		final int dmg = o.damage;
		o.detach();
		SkillInteractions.defer( () -> Overload.discharge( pos, 2, dmg, o ) );
	}

	/** crackling magic caught in an enemy: a turn later it discharges into it and every enemy beside it */
	public static class Overload extends xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff implements SkillInteractions.HeroDamageSource {

		{
			type = buffType.NEGATIVE;
		}

		int damage = 0;
		private boolean armed = false;
		//its runes winding up (a view only, never saved)
		RuneFX.Runes runes;

		void set( int damage ){
			this.damage = damage;
		}

		@Override
		public boolean rangedSource(){ return true; }

		@Override
		public boolean act(){
			if (!armed){
				armed = true;
				if (target.sprite != null) target.sprite.centerEmitter().burst( SkillBursts.ARCANE_SPARK, 5 );
				spend( TICK );
				return true;
			}
			discharge( target.pos, 1, damage, this );
			detach();
			return true;
		}

		static void discharge( int center, int radius, int damage, Object source ){
			Hero hero = Dungeon.hero;
			if (source instanceof Overload){
				discharged( ((Overload) source).runes, center );
				((Overload) source).runes = null;
			}
			for (int c : SkillInteractions.area( center, radius )){
				Char ch = xyz.gabriwar.warpedpixeldungeon.actors.Actor.findChar( c );
				if (ch == null || ch == hero || ch.alignment != Char.Alignment.ENEMY || !ch.isAlive()) continue;
				if (c != center && hero != null && hero.sprite != null && hero.sprite.parent != null){
					//the overload's arcs in arcane violet (never a wand of lightning's blue-white)
					hero.sprite.parent.add( new Lightning( center, c, null ).tint( Lightning.ARCANE ) );
				}
				if (ch.sprite != null){
					ch.sprite.centerEmitter().burst( SkillBursts.ARCANE_SPARK, 6 );
					ch.sprite.flash();
				}
				ch.damage( damage, source );
			}
			SpatialSound.play( Assets.Sounds.LIGHTNING, center, 0.8f, 1.1f );
		}

		@Override
		public int icon(){ return xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator.RECHARGING; }

		@Override
		public void tintIcon( com.watabou.noosa.Image icon ){ icon.hardlight( 1f, 0.7f, 0.2f ); }

		private static final String DAMAGE = "damage";
		private static final String ARMED = "armed";

		@Override
		public void storeInBundle( com.watabou.utils.Bundle bundle ){
			super.storeInBundle( bundle );
			bundle.put( DAMAGE, damage );
			bundle.put( ARMED, armed );
		}

		@Override
		public void restoreFromBundle( com.watabou.utils.Bundle bundle ){
			super.restoreFromBundle( bundle );
			damage = bundle.getInt( DAMAGE );
			armed = bundle.getBoolean( ARMED );
		}
	}
}
