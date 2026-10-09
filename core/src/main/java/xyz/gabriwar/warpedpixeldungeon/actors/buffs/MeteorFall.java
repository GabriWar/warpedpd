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

import com.watabou.noosa.Camera;
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
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BlastParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;

/**
 * A meteor the mage has called: the marked tile glows for a turn, then the stone
 * comes down on it and the eight cells around it. Flame where it lands, a blast that
 * shakes the floor, and a burn on whatever it caught. It never hurts the mage or an
 * ally, and it never breaks the floor.
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
            SkillFX.rain(cell, new Bomb(), 1, this::impactFX);
        }else{
            SpatialSound.play(Assets.Sounds.BLAST, cell);
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
        CellEmitter.center(cell).burst(BlastParticle.FACTORY,30);
        for(int c:xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions.area(cell,1)){
            if(Dungeon.level.heroFOV[c])CellEmitter.get(c).burst(FlameParticle.FACTORY,4);
        }
        if(Camera.main!=null)Camera.main.shake(4,.6f);
        SpatialSound.play(Assets.Sounds.BLAST, cell);
        SpatialSound.play(Assets.Sounds.ROCKS,cell,1f,.8f);
    }

	@Override
	public void fx( boolean on ) {
		//the mark glows while the stone is on its way
		if (on && cell >= 0 && !fallen && Dungeon.level.heroFOV[cell]){
			CellEmitter.center( cell ).burst( Speck.factory( Speck.RED_LIGHT ), 4 );
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
