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


import com.watabou.noosa.Camera;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.ShieldHalo;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

import java.util.ArrayList;
import java.util.List;

public class Sanctuary extends PassiveSkillA3 {

	private static final int CIRCLE_TURNS = 3;
	private static final int STILL_TURNS = 3;
	private static final int CIRCLE_MANA = 3;

	@Override
	public boolean toggleable(){ return true; }

	{
		name = "Sanctuary";
		tier = 3;
		image = 107;
		level = 0;
	}

	@Override
	protected boolean upgrade(){ return true; }

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (!active && level > 0)
			actions.add(AC_ACTIVATE);
		else if (level > 0)
			actions.add(AC_DEACTIVATE);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_ACTIVATE)){
			active = true;
			xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff.affect( hero, Watch.class );
			xyz.gabriwar.warpedpixeldungeon.effects.SkillCastFX.play(this,hero);
			//the ward is raised: a halo settles over the cleric
			if (hero.sprite != null){
				ShieldHalo halo = new ShieldHalo( hero.sprite );
				hero.sprite.parent.add( halo );
				halo.putOut();
				hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 6 );
			}
			Sample.INSTANCE.play( Assets.Sounds.CHARMS, 1f, 1.1f );
		} else if (action.equals(Skill.AC_DEACTIVATE)){
			active = false;
			xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff.detach( hero, Watch.class );
			Sample.INSTANCE.play( Assets.Sounds.DEGRADE, 0.6f, 1.4f );
			//at mastery lowering the ward sets off every circle it has drawn
			if (level >= MAX_LEVEL) release( hero );
		}
	}

	private static int reach( int rank ){
		return rank >= 2 ? 2 : 1;
	}

	private static boolean circleUp( Hero hero ){
		for (SkillField f : hero.buffs( SkillField.class )){
			if (f.kind == SkillField.SANCTUARY && f.sameFloor()) return true;
		}
		return false;
	}

	//holding your ground consecrates a circle: it mends allies inside, gathers light in enemies inside,
	//and that light bursts when the circle fades
	private void consecrate( Hero hero ){
		List<Integer> cells = SkillInteractions.area( hero.pos, reach( level ) );
		SkillField.place( hero, SkillField.SANCTUARY, level, CIRCLE_TURNS, cells );
		for (int c : cells) SkillInteractions.flare( c, 0xFFF1A1 );
		if (hero.sprite != null){
			new Flare( 8, 32 ).color( 0xFFEE88, true ).show( hero.sprite, 0.8f );
			hero.sprite.showStatus( CharSprite.POSITIVE, Messages.get( this, "circle" ) );
		}
		Sample.INSTANCE.play( Assets.Sounds.CHARMS, 1f, 0.9f );
	}

	private void release( Hero hero ){
		boolean any = false;
		for (SkillField f : hero.buffs( SkillField.class ).toArray( new SkillField[0] )){
			if (f.kind != SkillField.SANCTUARY || !f.sameFloor()) continue;
			any = true;
			SkillInteractions.detonateRadiance( f );
			ArrayList<Char> inside = new ArrayList<>();
			for (int c : f.cells){
				Char ch = Actor.findChar( c );
				if (ch != null && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()) inside.add( ch );
				SkillInteractions.flare( c, 0xFFF1A1 );
			}
			for (Char ch : inside) SkillInteractions.push( ch, f.origin, 2, 0 );
			f.detach();
		}
		if (any){
			Sample.INSTANCE.play( Assets.Sounds.BLAST, 0.9f, 1.2f );
			Camera.main.shake( 2, 0.3f );
		}
	}

	/** counts the turns the cleric holds still while the ward is up. Saves from before this rework
	 *  carry SANCTUARY_CHARGE, which nothing reads any more */
	public static class Watch extends xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff {

		private int still = 0;

		@Override
		public boolean act(){
			if (!(target instanceof Hero)){
				detach();
				return true;
			}
			Hero hero = (Hero) target;
			Sanctuary skill = hero.heroSkills == null ? null : hero.heroSkills.get( Sanctuary.class );
			if (skill == null || !skill.active || skill.level <= 0){
				detach();
				return true;
			}
			if (SkillInteractions.heroMovedLastTurn()) still = 0;
			else still++;
			if (still >= STILL_TURNS && hero.MP >= CIRCLE_MANA && !circleUp( hero )){
				still = 0;
				hero.MP -= CIRCLE_MANA;
				skill.consecrate( hero );
			}
			spend( TICK );
			return true;
		}

		@Override
		public int icon(){ return xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator.NONE; }

		//the ward held: two pale motes round the cleric and a slow shaft of light now and then.
		//Rebuilt whenever the sprite links, so it comes back after a save or a stairway
		private xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAuraFX aura;

		@Override
		public void fx( boolean on ){
			if (aura != null){
				aura.stop();
				aura = null;
			}
			if (on && target.sprite != null && target.sprite.parent != null){
				aura = new xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAuraFX( target, 0xFFF1A1, 0,
						xyz.gabriwar.warpedpixeldungeon.effects.particles.ShaftParticle.FACTORY, 1.6f );
				target.sprite.parent.add( aura );
			}
		}

		@Override
		public void storeInBundle( Bundle bundle ){
			super.storeInBundle( bundle );
			bundle.put( "still", still );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ){
			super.restoreFromBundle( bundle );
			still = bundle.getInt( "still" );
		}
	}
}
