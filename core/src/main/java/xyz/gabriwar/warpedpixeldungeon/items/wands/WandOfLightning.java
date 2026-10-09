/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.items.wands;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DwarfKing;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MagesStaff;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.utils.BArray;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

public class WandOfLightning extends DamageWand {

	{
		image = ItemSpriteSheet.WAND_LIGHTNING;
	}
	
	private ArrayList<Char> affected = new ArrayList<>();

	private ArrayList<Lightning.Arc> arcs = new ArrayList<>();

	//how many steps out the charge runs over the water (runOverWater): a storm bolt's (StormStrikes)
	static final int CRAWL = 3;
	//the most of its steps drawn: one start's whole 7x7 window (WeatherOverlay draws a frame's so)
	static final int DRAWN_STEPS = 48;

	public int min(int lvl){
		return 5+lvl;
	}

	public int max(int lvl){
		return 10+5*lvl;
	}
	
	@Override
	public void onZap(Ballistica bolt) {

		for (Char ch : affected.toArray(new Char[0])){
			if (ch != curUser && ch.alignment == curUser.alignment && ch.pos != bolt.collisionPos){
				affected.remove(ch);
			} else if (ch.buff(LightningCharge.class) != null){
				affected.remove(ch);
			}
		}

		//lightning deals less damage per-target, the more targets that are hit.
		float multiplier = 0.4f + (0.6f/affected.size());
		//if the main target is in water, all affected take full damage
		if (Dungeon.level.water[bolt.collisionPos]) multiplier = 1f;

		for (Char ch : affected){
			if (ch == Dungeon.hero) PixelScene.shake( 2, 0.3f );
			ch.sprite.centerEmitter().burst( SparkParticle.FACTORY, 3 );
			ch.sprite.flash();

			wandProc(ch, chargesPerCast());
			if (ch == curUser && ch.isAlive()) {
				ch.damage(Math.round(damageRoll() * multiplier * 0.5f), this);
				if (!curUser.isAlive()) {
					Badges.validateDeathFromFriendlyMagic();
					Dungeon.fail( this );
					GLog.n(Messages.get(this, "ondeath"));
				}
			} else {
				ch.damage(Math.round(damageRoll() * multiplier), this);
			}
		}
	}

	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {

		// lvl 0 - 25%
		// lvl 1 - 40%
		// lvl 2 - 50%
		float procChance = (buffedLvl()+1f)/(buffedLvl()+4f) * procChanceMultiplier(attacker);
		if (Random.Float() < procChance) {

			float powerMulti = Math.max(1f, procChance);

			FlavourBuff.prolong(attacker, LightningCharge.class, powerMulti*LightningCharge.DURATION);
			attacker.sprite.centerEmitter().burst( SparkParticle.FACTORY, 10 );
			attacker.sprite.flash();
			SpatialSound.play( Assets.Sounds.LIGHTNING, attacker );

		}
	}

	public static class LightningCharge extends FlavourBuff {

		{
			type = buffType.POSITIVE;
		}

		public static float DURATION = 10f;

		@Override
		public int icon() {
			return BuffIndicator.IMBUE;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(1, 1, 0);
		}
	}

	private void arc( Char ch ) {

		int dist = Dungeon.level.water[ch.pos] ? 2 : 1;

		if (curUser.buff(LightningCharge.class) != null){
			dist++;
		}

		ArrayList<Char> hitThisArc = new ArrayList<>();
		PathFinder.buildDistanceMap( ch.pos, BArray.not( Dungeon.level.solid, null ), dist );
		for (int i = 0; i < PathFinder.distance.length; i++) {
			if (PathFinder.distance[i] < Integer.MAX_VALUE){
				Char n = Actor.findChar( i );
				if (n == Dungeon.hero && PathFinder.distance[i] > 1)
					//the hero is only zapped if they are adjacent
					continue;
				else if (n != null && !affected.contains( n )) {
					hitThisArc.add(n);
				}
			}
		}
		
		affected.addAll(hitThisArc);
		for (Char hit : hitThisArc){
			arcs.add(new Lightning.Arc(ch.sprite.center(), hit.sprite.center()));
			arc(hit);
		}
	}

	/**
	 * After its arcs the charge runs out over the water: from the struck cell if it is water, and
	 * from everyone hit standing in water and not flying, eight ways and breadth first over the
	 * water, up to CRAWL steps from where it set off (the 7x7 window round it: never a whole lake),
	 * each cell once. Whoever stands in that water and does not fly is hit too, the caster as well
	 * (for half, as ever: onZap), though an ally off the struck cell is still spared there; the run
	 * goes on from none it reaches. Its steps are drawn as a crackle over the water lit as it gets
	 * there, nearest first and DRAWN_STEPS at most, in the zap's own lightning.
	 */
	private void runOverWater( int cell ) {
		Level level = Dungeon.level;
		HashMap<Integer, Integer> steps = new HashMap<>();
		ArrayDeque<Integer> run = new ArrayDeque<>();
		if (level.water[cell]){
			steps.put( cell, 0 );
			run.add( cell );
		}
		for (Char ch : affected){
			if (level.water[ch.pos] && !ch.flying && !steps.containsKey( ch.pos )){
				steps.put( ch.pos, 0 );
				run.add( ch.pos );
			}
		}
		int w = level.width(), h = level.height(), drawn = 0;
		while (!run.isEmpty()){
			int c = run.poll();
			int s = steps.get( c );
			if (s == CRAWL) continue;
			int x = c % w, y = c / w;
			for (int dy = -1; dy <= 1; dy++){
				for (int dx = -1; dx <= 1; dx++){
					int nx = x + dx, ny = y + dy;
					if ((dx == 0 && dy == 0) || nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
					int n = nx + ny * w;
					if (!level.water[n] || steps.containsKey( n )) continue;
					steps.put( n, s + 1 );
					run.add( n );
					Char in = Actor.findChar( n );
					if (in != null && !in.flying && !affected.contains( in )){
						affected.add( in );
					}
					if (drawn++ < DRAWN_STEPS){
						arcs.add( new Lightning.Arc( c, n ).crawl( s + 1 ) );
					}
				}
			}
		}
	}

	@Override
	public void fx(Ballistica bolt, Callback callback) {

		affected.clear();
		arcs.clear();

		int cell = bolt.collisionPos;

		Char ch = Actor.findChar( cell );
		if (ch != null) {
			if (ch instanceof DwarfKing){
				Statistics.qualifiedForBossChallengeBadge = false;
			}

			affected.add( ch );
			arcs.add( new Lightning.Arc(curUser.sprite.center(), ch.sprite.center()));
			arc(ch);
		} else {
			arcs.add( new Lightning.Arc(curUser.sprite.center(), DungeonTilemap.raisedTileCenterToWorld(bolt.collisionPos)));
			CellEmitter.center( cell ).burst( SparkParticle.FACTORY, 3 );
		}

		runOverWater( cell );

		//don't want to wait for the effect before processing damage.
		curUser.sprite.parent.addToFront( new Lightning( arcs, null ).thunderous( cell ) );
		SpatialSound.play( Assets.Sounds.LIGHTNING, curUser );
		//and where it strikes it goes off like a storm's bolt: the thunder cracks there with its first
		//frame (Lightning.thunderous, at full), and a blast, at 0.6 level with the crack (its file is
		//4 dB the louder) and a little high so it snaps rather than booms
		SpatialSound.play( Assets.Sounds.BLAST, cell, 0.6f, 1.1f );
		callback.call();
	}

	@Override
	public void staffFx(MagesStaff.StaffParticle particle) {
		particle.color(0xFFFFFF);
		particle.am = 0.6f;
		particle.setLifespan(0.6f);
		particle.acc.set(0, +10);
		particle.speed.polar(-Random.Float(3.1415926f), 6f);
		particle.setSize(0f, 1.5f);
		particle.sizeJitter = 1f;
		particle.shuffleXY(1f);
		float dst = Random.Float(1f);
		particle.x -= dst;
		particle.y += dst;
	}
	
}
