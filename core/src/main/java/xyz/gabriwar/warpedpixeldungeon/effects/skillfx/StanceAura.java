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

import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.IronStance;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.KnockBack;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Rampage;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.RecklessFury;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SpiritArmor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Transcendence;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EquipmentSparkle;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * The quiet presence of a toggled stance: a slow trickle of particles off the hero for as long as
 * the stance is on. One buff per stance, bundled with the hero so it survives a reload, and it
 * lets go of itself the moment the stance it belongs to is off (however that happened).
 */
public class StanceAura extends Buff {

	public static final int KNOCKBACK = 0;
	public static final int RAMPAGE   = 1;
	public static final int IRON      = 2;
	public static final int FURY      = 3;
	public static final int SPIRIT    = 4;
	public static final int LIGHT     = 5;

	private static final int[] KINDS = { KNOCKBACK, RAMPAGE, IRON, FURY, SPIRIT, LIGHT };

	private static final int[] STEEL = { 0xCCDDEE, 0xFFFFFF, 0x99AABB };

	{
		type = buffType.POSITIVE;
	}

	private int kind = KNOCKBACK;
	private AuraEmitter emitter;

	/** brings the hero's auras in line with his stances: one for each that is on, none for the rest */
	public static void sync( Hero hero ){
		if (hero == null || hero.heroSkills == null) return;
		for (int kind : KINDS){
			Skill skill = hero.heroSkills.get( skillOf( kind ) );
			boolean on = skill != null && skill.active && skill.level > 0;
			StanceAura aura = null;
			for (StanceAura a : hero.buffs( StanceAura.class )){
				if (a.kind == kind){ aura = a; break; }
			}
			if (on && aura == null){
				aura = new StanceAura();
				aura.kind = kind;
				aura.attachTo( hero );
			} else if (!on && aura != null){
				aura.detach();
			}
		}
	}

	private static Class<? extends Skill> skillOf( int kind ){
		switch (kind){
			case RAMPAGE: return Rampage.class;
			case IRON:    return IronStance.class;
			case FURY:    return RecklessFury.class;
			case SPIRIT:  return SpiritArmor.class;
			case LIGHT:   return Transcendence.class;
			default:      return KnockBack.class;
		}
	}

	private static Emitter.Factory factory( int kind ){
		switch (kind){
			case RAMPAGE: return Speck.factory( Speck.STAR );
			case IRON:    return EquipmentSparkle.factory( STEEL );
			case FURY:    return Speck.factory( Speck.RED_LIGHT );
			case SPIRIT:  return Speck.factory( Speck.BLUE_LIGHT );
			case LIGHT:   return Speck.factory( Speck.LIGHT );
			default:      return Speck.factory( Speck.FORGE );
		}
	}

	//seconds between two particles: an aura, not a fountain
	private static float interval( int kind ){
		switch (kind){
			case IRON:    return 0.35f;
			case SPIRIT:  return 0.7f;
			case RAMPAGE: return 0.6f;
			default:      return 0.5f;
		}
	}

	@Override
	public boolean act(){
		if (!(target instanceof Hero)) { detach(); return true; }
		Hero hero = (Hero) target;
		Skill skill = hero.heroSkills == null ? null : hero.heroSkills.get( skillOf( kind ) );
		if (skill == null || !skill.active || skill.level <= 0){
			detach();
			return true;
		}
		spend( TICK );
		return true;
	}

	@Override
	public void fx( boolean on ){
		if (emitter != null){
			emitter.on = false;
			emitter = null;
		}
		if (!on || target.sprite == null || target.sprite.parent == null) return;
		emitter = new AuraEmitter( target.sprite );
		target.sprite.parent.add( emitter );
		emitter.pour( factory( kind ), interval( kind ) );
	}

	/** follows the sprite, hides with it, and takes itself off the scene once its last particle is gone */
	private static class AuraEmitter extends Emitter {

		private final CharSprite sprite;

		AuraEmitter( CharSprite sprite ){
			this.sprite = sprite;
			pos( sprite );
		}

		@Override
		public void update(){
			if (!sprite.exists) on = false;
			visible = sprite.visible;
			super.update();
		}

		@Override
		public void kill(){
			super.kill();
			if (parent != null) parent.erase( this );
		}
	}

	private static final String KIND = "kind";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( KIND, kind );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		kind = bundle.getInt( KIND );
	}
}
