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

import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.StanceOrbit;

/**
 * The quiet presence of a held stance: the stance's orbit (StanceOrbit: two trailing motes of its
 * colour circling the hero, breathing, flaring with each blow, a glint at his feet) and now and
 * then one accent particle, poured at the stance's own rate. Nothing allocated per frame; let go
 * of when the stance is lowered or the sprite goes.
 */
public class StanceAuraFX extends Group {

	private final Char owner;
	private final StanceOrbit orbit;
	private final Emitter accent;

	public StanceAuraFX( Char owner, int color, Emitter.Factory accentFactory, float accentInterval ){
		this.owner = owner;
		add( orbit = new StanceOrbit( owner, color ) );
		if (accentFactory != null && owner.sprite != null){
			accent = new Emitter();
			accent.pos( owner.sprite );
			accent.autoKill = false;
			add( accent );
			accent.pour( accentFactory, accentInterval );
		} else {
			accent = null;
		}
	}

	@Override
	public void update(){
		super.update();
		if (owner.sprite == null || !owner.sprite.exists){ killAndErase(); return; }
		visible = owner.sprite.visible;
		if (accent != null) accent.on = visible;
	}

	public void stop(){
		if (accent != null) accent.on = false;
		orbit.killAndErase();
		killAndErase();
	}
}
