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

import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

public class Litany extends SubSkill1 {

	//each cast is a verse that mends you and the allies within this many tiles
	private static final int VERSE_RANGE = 2;
	//at max rank every third verse also blesses everyone it touches
	private static final float VERSE_BLESS = 3f;

	{
		name = "Litany";
		image = 162;
		tier = 1;
	}

	private int verses = 0;

	//a passive: nothing to switch on, so it stays out of the quick panel
	@Override
	public boolean toggleable(){ return false; }

	@Override
	public java.util.ArrayList<String> actions( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero ){
		return new java.util.ArrayList<>();
	}

	@Override
	protected boolean upgrade(){ return true; }

	/** 2 / 4 / 6 HP */
	private int verseHeal( Char ch ){
		return SkillInteractions.ofHealth( ch.HT, 0.02f * level );
	}

	//every skill you cast (not a switch) speaks a verse
	@Override
	public void onSkillCast( Skill skill ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || !hero.isAlive()) return;
		verses++;
		boolean bless = level >= MAX_LEVEL && verses % 3 == 0;
		if (hero.sprite != null)
			hero.sprite.emitter().burst( Speck.factory( Speck.NOTE ), 2 );
		mend( hero, hero, bless );
		for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (m.alignment == Char.Alignment.ALLY && m.isAlive() && Dungeon.level.distance( hero.pos, m.pos ) <= VERSE_RANGE)
				mend( hero, m, bless );
		}
		Sample.INSTANCE.play( Assets.Sounds.CHARMS, 0.6f, 1.3f );
	}

	private void mend( Hero hero, Char ch, boolean bless ){
		int heal = Math.min( verseHeal( ch ), ch.HT - ch.HP );
		ch.HP += heal;
		if (ch == hero) AsceticVow.tithe( hero, heal );
		if (bless)
			Buff.prolong( ch, Bless.class, VERSE_BLESS );
		if (ch.sprite == null) return;
		ch.sprite.emitter().burst( Speck.factory( Speck.HEALING ), 3 );
		if (heal > 0) ch.sprite.showStatus( CharSprite.POSITIVE, "+" + heal + "HP" );
		if (ch != hero && hero.sprite != null && hero.sprite.parent != null)
			hero.sprite.parent.add( new Beam.HealthRay( hero.sprite.center(), ch.sprite.center() ) );
	}
}
