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
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.WhirlHitFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StaggerFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAura;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import com.watabou.utils.PathFinder;

public class Rampage extends ActiveSkill3 {

	{
		name = "Rampage";
		castText = "Rampage!";
		tier = 3;
		image = 19;
		mana = 5;
	}

	@Override
	public float damageModifier(){
		StanceAura.sync( Dungeon.hero );
		if (!active || Dungeon.hero.MP < getManaCost())
			return 1f;
		else {
			return 0.4f + 0.2f * level;
		}
	}

	@Override
	public boolean AoEDamage(){
		if (!active || Dungeon.hero.MP < getManaCost())
			return false;
		else {
			castTextYell();
			Dungeon.hero.MP -= getManaCost();
			sweep();
			if (level == Skill.MAX_LEVEL){
				bleedEveryoneAround();
			}
			return true;
		}
	}

	//a fully trained rampage leaves every swept enemy bleeding, which recoups the
	//sweep's damage penalty over time. the splash damage never runs through attackProc,
	//so onHitProc would only ever reach the primary target - the bleed is applied here
	private void bleedEveryoneAround(){
		Hero hero = Dungeon.hero;
		for (int n : PathFinder.NEIGHBOURS8){
			Char ch = Actor.findChar( hero.pos + n );
			if (ch != null && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()){
				Buff.affect( ch, Bleeding.class ).set( 2 );
			}
		}
	}

	//the ring around the hero: the blade goes round clockwise from the top-left, one cell a beat
	//after the last, its whistle climbing as it goes; every enemy in the ring is wounded
	private void sweep(){
		Hero hero = Dungeon.hero;
		int w = Dungeon.level.width();
		int[] clockwise = { -w - 1, -w, -w + 1, +1, w + 1, w, w - 1, -1 };
		int step = 0;
		for (int n : clockwise){
			final int c = hero.pos + n;
			if (c < 0 || c >= Dungeon.level.length() || Dungeon.level.solid[c]) continue;
			Char ch = Actor.findChar( c );
			if (ch != null && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()) Wound.hit( ch );
			final int beat = step++;
			StaggerFX.after( 0.035f * beat, () -> {
				if (!Dungeon.level.heroFOV[c]) return;
				WhirlHitFX.show( c );
				CellEmitter.get( c ).burst( Speck.factory( Speck.DUST ), 2 );
				if (beat % 2 == 1) SpatialSound.play( Assets.Sounds.HIT_SLASH, c, 0.5f, 0.9f + 0.06f * beat );
			} );
		}
		SpatialSound.play( Assets.Sounds.HIT_SLASH, hero, 1f, 0.8f );
		Camera.main.shake( 1, 0.15f );
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute(hero, action);
		if (action.equals(Skill.AC_ACTIVATE)){
			hero.heroSkills.deactivateOtherToggles( this );
			SpatialSound.play( Assets.Sounds.HIT_SLASH, hero, 0.8f, 1.3f );
			//the blade comes up glinting, and keeps glinting while the stance holds
			if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.STAR ), 5 );
		} else if (action.equals(Skill.AC_DEACTIVATE)){
			SpatialSound.play( Assets.Sounds.HIT_SLASH, hero, 0.4f, 0.7f );
		}
		StanceAura.sync( hero );
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 1 * level));
	}

	@Override
	protected boolean upgrade(){
		return true;
	}
}
