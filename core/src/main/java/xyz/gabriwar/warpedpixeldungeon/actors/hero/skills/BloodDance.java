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


import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Adrenaline;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;

import java.util.ArrayList;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;

public class BloodDance extends Skill {

	{
		tag = "A5B";
		name = "Crimson Waltz";
		castText = "Stay close";
		image = 63;
		tier = 4;
		mana = 12;
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

			Buff.prolong( hero, Adrenaline.class, 4 + 3 * level );
			//at mastery the dance opens behind a blood-red guard
			if (level >= MAX_LEVEL) Buff.affect( hero, Barrier.class ).setShield( SkillInteractions.ofHealth( hero.HT, 0.03f * level ) );

			hero.MP -= getManaCost();
			castTextYell();
			Sample.INSTANCE.play( Assets.Sounds.HIT_STAB, 1f, 0.9f );
			Dungeon.hero.sprite.emitter().burst( Speck.factory( Speck.RED_LIGHT ), 6 );
			new Flare( 6, 20 ).color( 0xCC2222, true ).show( hero.sprite, 0.6f ).angularSpeed = 120;
			//the first turn of the waltz: a ring of red sweeping once around him, tile by tile
			int w = Dungeon.level.width();
			int[] around = { -w - 1, -w, -w + 1, 1, w + 1, w, w - 1, -1 };
			FxTimeline t = FxTimeline.start();
			for (int i = 0; i < around.length; i++){
				final int c = hero.pos + around[i];
				if (!SkillInteractions.valid( c ) || Dungeon.level.solid[c] || !Dungeon.level.heroFOV[c]) continue;
				t.at( 0.05f * i, () -> Splash.at( c, 0xCC1111, 3 ) );
			}
			t.at( 0.4f, () -> Sample.INSTANCE.play( Assets.Sounds.HIT_SLASH, 0.6f, 1.3f ) );
			Dungeon.hero.heroSkills.lastUsed = this;
			hero.spend( TIME_TO_USE );
			hero.busy();
			hero.sprite.operate( hero.pos );
		}
	}

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level > 0 && !ranged && enemy != null
				&& Dungeon.hero != null && Dungeon.hero.buff( Adrenaline.class ) != null){
			Buff.affect( enemy, Bleeding.class ).set( 1 + level );
			Splash.at( enemy.pos, 0xCC1111, 3 );
			Sample.INSTANCE.play( Assets.Sounds.HIT_STAB, 0.8f, 1.1f );
		}
		return damage;
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }
}
