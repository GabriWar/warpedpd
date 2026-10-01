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
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

public class DreadHowl extends Skill {

	{
		tag = "D4B";
		name = "Dread Howl";
		castText = "Turn on each other";
		image = 69;
		tier = 4;
		mana = 9;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && hero.MP >= getManaCost())
			actions.add(AC_CAST);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && hero.MP >= getManaCost()){

			//the howl applies no fear of its own; it turns fear that is already there into madness
			ArrayList<Char> terrified = new ArrayList<>();
			for (Char ch : Actor.chars()){
				if (!(ch instanceof Mob) || ch.alignment != Char.Alignment.ENEMY) continue;
				if (Dungeon.level.distance( hero.pos, ch.pos ) > 5) continue;
				if (!Dungeon.level.heroFOV[ch.pos]) continue;
				if (ch.buff( Terror.class ) == null) continue;
				terrified.add( ch );
			}

			//at mastery the howl makes its own fear in whoever is not afraid yet
			ArrayList<Char> unafraid = new ArrayList<>();
			if (level >= MAX_LEVEL) for (Char ch : Actor.chars()){
				if (!(ch instanceof Mob) || ch.alignment != Char.Alignment.ENEMY) continue;
				if (Dungeon.level.distance( hero.pos, ch.pos ) > 5) continue;
				if (!Dungeon.level.heroFOV[ch.pos]) continue;
				if (ch.buff( Terror.class ) != null || ch.buff( Amok.class ) != null) continue;
				unafraid.add( ch );
			}

			if (terrified.isEmpty() && unafraid.isEmpty()){
				GLog.w( Messages.get(this, "no_targets") );
				return;
			}

			for (Char ch : unafraid){
				Buff.affect( ch, Terror.class, 3 ).object = hero.id();
				CellEmitter.get( ch.pos ).burst( ShadowParticle.CURSE, 4 );
			}

			//the howl rolls out from the rogue ring by ring; each mind it reaches snaps as it passes
			FxTimeline t = FxTimeline.start();
			int w = Dungeon.level.width();
			for (int r = 1; r <= 5; r++){
				final int ring = r;
				t.at( 0.06f * r, () -> {
					for (int c : SkillInteractions.area( hero.pos, ring )){
						if (Dungeon.level.distance( hero.pos, c ) != ring || !Dungeon.level.heroFOV[c]) continue;
						if (((c % w) + (c / w) + ring) % 2 == 0) CellEmitter.get( c ).burst( ShadowParticle.MISSILE, 1 );
					}
				} );
			}
			t.at( 0.2f, () -> Sample.INSTANCE.play( Assets.Sounds.CHALLENGE, 0.6f, 0.7f ) );
			for (Char ch : terrified){
				Buff.detach( ch, Terror.class );
				Buff.prolong( ch, Amok.class, 3 + level );
				final Char mad = ch;
				t.at( 0.06f * Dungeon.level.distance( hero.pos, ch.pos ), () -> {
					if (mad.sprite == null || !mad.isAlive()) return;
					CellEmitter.get( mad.pos ).burst( Speck.factory( Speck.SCREAM ), 2 );
					CellEmitter.get( mad.pos ).burst( ShadowParticle.UP, 3 );
					mad.sprite.flash();
					mad.sprite.showStatus( CharSprite.NEGATIVE, "Mad" );
				} );
			}
			Camera.main.shake( 1, 0.3f );
			hero.sprite.emitter().burst( ShadowParticle.UP, 8 );

			hero.MP -= getManaCost();
			castTextYell();
			Sample.INSTANCE.play( Assets.Sounds.GHOST, 1f, 0.6f );
			Dungeon.hero.heroSkills.lastUsed = this;
			hero.spend( TIME_TO_USE );
			hero.busy();
			hero.sprite.operate( hero.pos );
		}
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }
}
