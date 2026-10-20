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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RuneFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillRay;
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
		//a verse: runes of light rising off the cleric (never a lullaby's notes), and the spoken
		//prayer's ring of gold runes rising from his feet
		if (hero.sprite != null){
			hero.sprite.emitter().burst( SkillBursts.VERSE, 2 );
			spoken( hero.sprite );
		}
		mend( hero, hero, bless );
		for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (m.alignment == Char.Alignment.ALLY && m.isAlive() && Dungeon.level.distance( hero.pos, m.pos ) <= VERSE_RANGE)
				mend( hero, m, bless );
		}
		SpatialSound.play( Assets.Sounds.CHARMS, hero, 0.6f, 1.3f );
	}

	private void mend( Hero hero, Char ch, boolean bless ){
		int heal = Math.min( verseHeal( ch ), ch.HT - ch.HP );
		ch.HP += heal;
		if (ch == hero) AsceticVow.tithe( hero, heal );
		if (bless)
			Buff.prolong( ch, Bless.class, VERSE_BLESS );
		if (ch.sprite == null) return;
		ch.sprite.emitter().burst( Speck.factory( Speck.HEALING ), 3 );
		if (heal > 0) ch.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( heal ), xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.HEALING );
		//the mend flows to an ally as Mass Heal's does, landing in the heal's red crosses: only to
		//one it mends or blesses, never a heal's picture on an ally already whole
		if ((heal > 0 || bless) && ch != hero && hero.sprite != null && hero.sprite.parent != null)
			SkillRay.show( hero.sprite, ch.sprite, SkillRay.HEAL, MassHeal.FLOW );
	}

	/** The verse spoken: RISE_N gold runes on a RISE_RX x RISE_RY ring rising from the feet at
	 *  RISE_SPEED px/s for RISE_LIFE s (RuneFX.rise, RUNE_HOLY; one Runes gizmo). Any thread. */
	public static void spoken( CharSprite s ){
		RuneFX.rise( s, RISE_N, SkillPalette.RUNE_HOLY, RISE_RX, RISE_RY, RISE_SPEED, RISE_LIFE );
	}

	public static final int RISE_N = 6;
	public static final float RISE_RX = 9f, RISE_RY = 3.5f, RISE_SPEED = 18f, RISE_LIFE = 0.4f;
}
