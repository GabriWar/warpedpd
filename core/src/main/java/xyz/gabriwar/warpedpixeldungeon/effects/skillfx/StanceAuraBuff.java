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

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;

/**
 * Carries a stance's ambient aura across save/load: it is a bundled buff, so it comes back with
 * the hero and re-attaches its visual when the sprite links. It lets go on its own the turn its
 * stance is found lowered (another stance raised over it, a level lost, the skill switched off).
 */
public abstract class StanceAuraBuff extends Buff {

	{
		type = buffType.POSITIVE;
		announced = false;
	}

	private StanceAuraFX aura;

	/** the stance this aura belongs to */
	protected abstract Class<? extends Skill> stance();

	/** the visual, built fresh each time the sprite links */
	protected abstract StanceAuraFX build();

	private boolean raised(){
		if (!(target instanceof Hero) || !target.isAlive()) return false;
		Hero hero = (Hero) target;
		if (hero.heroSkills == null) return false;
		Skill s = hero.heroSkills.get( stance() );
		return s != null && s.active && s.level > 0;
	}

	@Override
	public boolean act(){
		if (!raised()){
			detach();
			return true;
		}
		spend( TICK );
		return true;
	}

	@Override
	public void fx( boolean on ){
		if (aura != null){
			aura.stop();
			aura = null;
		}
		if (on && target.sprite != null && target.sprite.parent != null){
			aura = build();
			target.sprite.parent.add( aura );
		}
	}

	@Override
	public int icon(){ return BuffIndicator.NONE; }

	/** raise or lower the aura buff for a hero, whichever the stance's flag says */
	public static void sync( Hero hero, Class<? extends StanceAuraBuff> cls, boolean on ){
		if (hero == null) return;
		if (on) Buff.affect( hero, cls );
		else Buff.detach( hero, cls );
	}
}
