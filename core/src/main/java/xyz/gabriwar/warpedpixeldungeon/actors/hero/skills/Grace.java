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
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.BlowPresenter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.MoteFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBolt;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Callback;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMagic;

public class Grace extends PassiveSkillA2 {

	//at max rank a mote that finds the pool already full heals instead
	private static final float OVERFLOW_HEAL = 0.03f;

	{
		name = "Grace";
		image = 105;
		tier = 2;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//every enemy you slay sends a mote of light home: 1 mana per level
	@Override
	public void onKill( Mob mob, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive())
			return;

		int maxMana = hero.MT + RingOfMagic.manaBonus( hero );
		boolean overflow = hero.MP >= maxMana;
		if (overflow && (level < MAX_LEVEL || hero.HP >= hero.HT))
			return;

		//a mote of holy light flies home from the slain (MagicMissile.boltFromChar's line and its one
		//draw, never a wand's bolt); mana gained, it lands in a bloom of mana on the cleric (a heal
		//past a full pool shows the heal's own crosses instead)
		if (mob.sprite != null && hero.sprite != null && hero.sprite.parent != null){
			final CharSprite hs = hero.sprite;
			Callback landed = overflow ? null : () -> manaBloom( hs );
			Char home = Actor.findChar( hero.pos );
			if (home != null) SkillBolt.fly( hero.sprite.parent, mob.sprite.center(), home.sprite.destinationCenter(), SkillBolt.HOLY, landed );
			else SkillBolt.fly( hero.sprite.parent, mob.sprite, hero.pos, SkillBolt.HOLY, landed );
		}

		if (overflow){
			hero.heal( SkillInteractions.ofHealth( hero.HT, OVERFLOW_HEAL ) );
		} else {
			int gain = Math.min( level, maxMana - hero.MP );
			hero.MP += gain;
			if (hero.sprite != null){
				hero.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 2 + gain );
				hero.sprite.showStatusWithIcon( BlowPresenter.MANA_COLOR, Integer.toString( gain ), xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.MANA );
			}
		}
		SpatialSound.play( Assets.Sounds.CHARMS, hero, 0.5f, 1.5f );
	}

	/** The bloom's ring (px wide, from and to), its life (s) and alpha; the motes gathered in (how
	 *  many, from how far, px, over how long, s). */
	public static final int BLOOM_FROM = 9, BLOOM_TO = 13, BLOOM_MOTES = 4;
	public static final float BLOOM_LIFE = 0.4f, BLOOM_ALPHA = 0.8f, BLOOM_REACH = 12f, BLOOM_GATHER = 0.3f;

	/** The mote of light home as mana: a small ring of mana blue spreading at the cleric's feet and
	 *  BLOOM_MOTES motes of mana drawn into him (one ring, at most BLOOM_MOTES particles, no light).
	 *  Any thread; FxRandom only. */
	public static void manaBloom( final CharSprite s ){
		if (s == null) return;
		MoteFX.gather( s, BLOOM_MOTES, SkillPalette.MANA, BLOOM_REACH, BLOOM_GATHER );
		Fx.post( () -> {
			if (s.parent == null || !s.visible) return;
			FxRing.ground( GameScene.layer( GameScene.Layer.FLOOR ), SkillVfx.midX( s ), s.y + s.height() - 3,
					BLOOM_FROM, BLOOM_TO, BLOOM_LIFE, SkillPalette.MANA_LIGHT, BLOOM_ALPHA, true );
		} );
	}
}
