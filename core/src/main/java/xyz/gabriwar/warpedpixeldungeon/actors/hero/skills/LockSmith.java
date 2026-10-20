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


import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Haste;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.ClosingRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.Random;

public class LockSmith extends PassiveSkillA3 {

	private static final int READ_RANGE = 3;

	{
		name = "Lock Smith";
		tier = 3;
		image = 51;
	}

	/** The steel of a mechanism read: its glints, the ring found traps show (px, from and to, its life). */
	public static final int STEEL = 0xB9DEEE, FOUND_FROM = 5, FOUND_TO = 17;
	public static final float FOUND_ALPHA = 0.5f, FOUND_LIFE = 0.45f;

	//a hidden trap read: a small ring of steel light spreading on its cell. Any thread
	public static void found( final int cell ){
		Fx.post( () -> {
			if (!SkillVfx.valid( cell ) || !SkillVfx.seen( cell )) return;
			FxRing.ground( GameScene.layer( GameScene.Layer.FLOOR ), SkillVfx.cx( cell ), SkillVfx.cy( cell ) + 2,
					FOUND_FROM, FOUND_TO, FOUND_LIFE, STEEL, FOUND_ALPHA, true );
		} );
	}

	@Override
	public boolean disableTrap(){
		if (Random.Int(100) >= 33 * level) return false;
		castTextYell();
		//the mechanism is seen and heard giving up: steel glints, a ring of steel light closing on
		//his feet like a lock clicking shut (never a trap firing)
		Hero hero = Dungeon.hero;
		if (hero != null && hero.sprite != null){
			CellEmitter.center( hero.pos ).burst( SkillBursts.STEEL_GLINT, 6 );
			ClosingRing.on( hero.sprite, 23, 5, 0.3f, STEEL );
			PixelFlare.show( SkillVfx.cx( hero.pos ), hero.sprite.y + hero.sprite.height() - 3, STEEL, PixelFlare.S, 0.3f );
		}
		SpatialSound.play( Assets.Sounds.UNLOCK, hero, 1f, 1.1f );
		if (level >= Skill.MAX_LEVEL && hero != null) readMechanism( hero );
		return true;
	}

	//at mastery the jammed plate tells how the whole floor was rigged: hidden traps nearby show themselves
	private void readMechanism( Hero hero ){
		boolean found = false;
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (Dungeon.level.distance( hero.pos, i ) > READ_RANGE) continue;
			xyz.gabriwar.warpedpixeldungeon.levels.traps.Trap trap = Dungeon.level.traps.get( i );
			if (trap == null || trap.visible) continue;
			Dungeon.level.discover( i );
			xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMagicMapping.discover( i );
			CellEmitter.get( i ).burst( SkillBursts.STEEL_GLINT, 5 );
			found( i );
			found = true;
		}
		if (found){
			if (hero.sprite != null) hero.sprite.showStatus( xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite.NEUTRAL, Messages.get( this, "found" ) );
			SpatialSound.play( Assets.Sounds.SECRET, hero, 0.8f, 1.2f );
		}
	}

	@Override
	protected boolean upgrade(){
		return true;
	}
}
