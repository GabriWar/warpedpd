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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AscensionChallenge;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Golem;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Monk;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.quest.DwarfToken;
import xyz.gabriwar.warpedpixeldungeon.items.rings.Ring;
import xyz.gabriwar.warpedpixeldungeon.journal.Notes;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.Room;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.AmbitiousImpRoom;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ImpSprite;
import xyz.gabriwar.warpedpixeldungeon.windows.WndImp;
import xyz.gabriwar.warpedpixeldungeon.windows.WndQuest;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class Imp extends NPC {

	{
		spriteClass = ImpSprite.class;

		properties.add(Property.IMMOVABLE);
	}
	
	private boolean seenBefore = false;

	@Override
	public Notes.Landmark landmark() {
		return Notes.Landmark.IMP;
	}

	@Override
	protected boolean act() {
		if (Dungeon.hero.buff(AscensionChallenge.class) != null){
			die(null);
			return true;
		}
		if (!Quest.given(Dungeon.hero) && Dungeon.level.visited[pos]) {
			if (!seenBefore && Dungeon.level.heroFOV[pos]) {
				yell(Messages.get(this, "hey", Messages.titleCase(Dungeon.hero.name())));
				seenBefore = true;
			}
		} else {
			seenBefore = false;
		}
		
		return super.act();
	}
	
	@Override
	public int defenseSkill( Char enemy ) {
		return INFINITE_EVASION;
	}

	@Override
	public void damage( int dmg, Object src ) {
		//do nothing
	}

	@Override
	public boolean add( Buff buff ) {
		return false;
	}
	
	@Override
	public boolean reset() {
		return true;
	}
	
	@Override
	public boolean interact(Char c) {

		sprite.turnTo( pos, c.pos );

		if (!(c instanceof Hero)) return true;
		Hero h = (Hero) c;
		Quest.HeroProgress hp = Quest.progressFor( h.id() );

		if (hp != null && hp.given) {
			if (hp.claims >= Quest.MAX_CLAIMS) return true;
			DwarfToken tokens = h.belongings.getItem( DwarfToken.class );
			if (tokens != null && tokens.quantity() >= Quest.tokensRequired()) {
				showReward( h );
			} else {
				tell( h, Quest.alternative ?
						Messages.get(this, "monks_2", Messages.titleCase(h.name()))
						: Messages.get(this, "golems_2", Messages.titleCase(h.name())) );
			}
		} else {
			Quest.give( h );
			tell( h, Messages.get(this, "intro") + "\n\n" + (Quest.alternative ?
					Messages.get(this, "monks_1", Messages.titleCase(h.name()))
					: Messages.get(this, "golems_1", Messages.titleCase(h.name()))) );
		}

		return true;
	}

	private void showReward( final Hero h ) {
		if (h.isRemote) {
			Quest.sendReward( h );
		} else {
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show( new WndImp( Imp.this, h ) );
				}
			});
		}
	}

	private void tell( final Hero h, final String text ) {
		if (h.isRemote) {
			QuestSupport.sendInfo( this, h, text );
		} else {
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show( new WndQuest( Imp.this, text ));
				}
			});
		}
	}
	
	public void flee() {
		
		yell( Messages.get(this, "cya", Messages.titleCase(Dungeon.hero.name())) );
		
		destroy();
		sprite.die();
	}

	public static class Quest {

		// Per-hero progress: each hero accepts the quest, collects their own DwarfToken
		// stack (tokens are shared floor loot, but each hero picks into their own backpack),
		// and claims their own rolled ring. alternative/spawned describe the single world
		// placement and stay global.
		// Each hero may claim up to MAX_CLAIMS rings (costing tokens each time).
		public static final int MAX_CLAIMS = 2;

		public static class HeroProgress implements PerHeroProgress {
			public int heroId;
			public boolean given;
			public int claims;       // rings taken so far (0..MAX_CLAIMS)
			public Ring reward;      // the next ring on offer (pre-rolled), null once maxed

			@Override public int heroId() { return heroId; }

			@Override
			public void storeInBundle( Bundle b ) {
				b.put( HERO_ID, heroId );
				b.put( GIVEN, given );
				b.put( CLAIMS, claims );
				if (reward != null) b.put( REWARD, reward );
			}

			@Override
			public void restoreFromBundle( Bundle b ) {
				heroId = b.getInt( HERO_ID );
				given  = b.getBoolean( GIVEN );
				claims = b.getInt( CLAIMS );
				if (b.contains( REWARD )) reward = (Ring) b.get( REWARD );
			}
		}

		private static boolean alternative;
		private static boolean spawned;

		private static final PerHeroStore<HeroProgress> progress = new PerHeroStore<>();

		public static void reset() {
			spawned = false;
			progress.clear();
		}

		public static boolean alternative() { return alternative; }
		public static HeroProgress progressFor( int heroId ) { return progress.get( heroId ); }

		// Tokens needed to claim: monks(alternative) ask for 5, golems ask for 4.
		public static int tokensRequired() { return alternative ? 5 : 4; }

		private static final String NODE		= "demon";

		private static final String ALTERNATIVE	= "alternative";
		private static final String SPAWNED		= "spawned";
		private static final String PROGRESS	= "progress";
		private static final String HERO_ID		= "hero_id";
		private static final String GIVEN		= "given";
		private static final String CLAIMS		= "claims";
		private static final String REWARD		= "reward";

		public static void storeInBundle( Bundle bundle ) {

			Bundle node = new Bundle();

			node.put( SPAWNED, spawned );

			if (spawned) {
				node.put( ALTERNATIVE, alternative );
				progress.store( node, PROGRESS );
			}

			bundle.put( NODE, node );
		}

		public static void restoreFromBundle( Bundle bundle ) {

			Bundle node = bundle.getBundle( NODE );

			progress.clear();

			if (!node.isNull() && (spawned = node.getBoolean( SPAWNED ))) {
				alternative = node.getBoolean( ALTERNATIVE );
				// Guarded for pre-per-hero saves (no "progress" array).
				progress.restore( node, PROGRESS );
			}
		}

		public static ArrayList<Room> spawn( ArrayList<Room> rooms ) {
			if (!spawned && Dungeon.depth > 16 && Random.Int( 20 - Dungeon.depth ) == 0) {

				rooms.add(new AmbitiousImpRoom());
				spawned = true;

				//always assigns monks on floor 17, golems on floor 19, and 50/50 between either on 18
				switch (Dungeon.depth){
					case 17: default:
						alternative = true;
						break;
					case 18:
						alternative = Random.Int(2) == 0;
						break;
					case 19:
						alternative = false;
						break;
				}
			}

			return rooms;
		}

		/** Register the quest for a hero and roll their first ring reward. */
		public static void give( Hero h ) {
			HeroProgress hp = new HeroProgress();
			hp.heroId = h.id();
			hp.reward = rollRing();
			hp.given = true;
			progress.put( hp );
		}

		private static Ring rollRing() {
			Ring reward;
			do {
				reward = (Ring) Generator.random( Generator.Category.RING );
			} while (reward.cursed);
			reward.upgrade( 2 );
			reward.cursed = true;
			return reward;
		}

		public static boolean given( Hero h ) {
			HeroProgress hp = progress.get( h.id() );
			return hp != null && hp.given;
		}

		// Any hero who can still claim — gates whether kills drop tokens.
		private static boolean anyActive() {
			for (HeroProgress hp : progress.values()) {
				if (hp.given && hp.claims < MAX_CLAIMS) return true;
			}
			return false;
		}

		public static void process( Mob mob ) {
			// Tokens are shared floor loot — drop one at the corpse for whoever collects it,
			// as long as at least one hero is on the matching quest.
			if (spawned && anyActive() && Dungeon.depth != 20) {
				if ((alternative && mob instanceof Monk) ||
					(!alternative && mob instanceof Golem)) {

					Dungeon.level.drop( new DwarfToken(), mob.pos ).sprite.drop();
				}
			}
		}

		/** Hand in tokens and grant the ring to a hero. Runs on the actor thread (remote
		 *  via NetDialogs.resolve) or render thread (local WndImp). */
		public static void claimReward( Hero hero ) {
			HeroProgress hp = progress.get( hero.id() );
			if (hp == null || !hp.given || hp.claims >= MAX_CLAIMS) return;

			int required = tokensRequired();
			DwarfToken tokens = hero.belongings.getItem( DwarfToken.class );
			if (tokens == null || tokens.quantity() < required) return;

			// Consume only the required tokens so a leftover surplus counts toward a 2nd ring.
			for (int i = 0; i < required && tokens.quantity() > 0; i++) {
				tokens.detach( hero.belongings.backpack );
			}

			Ring reward = hp.reward;
			hp.claims++;
			// Pre-roll the next offer, or clear it once this hero has maxed out.
			hp.reward = hp.claims < MAX_CLAIMS ? rollRing() : null;

			if (reward != null) {
				reward.identify(false);
				if (reward.doPickUp( hero )) {
					xyz.gabriwar.warpedpixeldungeon.net.NetManager.heroLog( hero,
							Messages.capitalize(Messages.get(hero, "you_now_have", reward.name())) );
				} else {
					Imp imp = findImp();
					Dungeon.level.drop( reward, imp != null ? imp.pos : hero.pos ).sprite.drop();
				}
			}

			Statistics.questScores[3] = 4000;
			maybeDespawnImp();
		}

		private static Imp findImp() {
			if (Dungeon.level == null) return null;
			for (Mob m : Dungeon.level.mobs) {
				if (m instanceof Imp) return (Imp) m;
			}
			return null;
		}

		// SP parity + MP correctness: the imp only flees (and its landmark clears) once
		// every present hero has finished — so a player who hasn't talked to it yet keeps
		// their chance.
		private static void maybeDespawnImp() {
			if (!QuestSupport.allPresentDone( progress, hp -> hp.claims >= MAX_CLAIMS )) return;
			final Imp imp = findImp();
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					if (imp != null) imp.flee();
					Notes.remove( Notes.Landmark.IMP );
				}
			});
		}

		// Route the reward confirm to a remote hero's client (KIND_IMP_REWARD).
		static void sendReward( Hero h ) {
			HeroProgress hp = progress.get( h.id() );
			if (hp == null) return;
			try {
				org.json.JSONObject p = new org.json.JSONObject();
				p.put("text", Messages.get(WndImp.class, "message"));
				if (hp.reward != null) p.put("ring", QuestSupport.itemDisplay(hp.reward));
				xyz.gabriwar.warpedpixeldungeon.net.NetDialogs.request(
						h, xyz.gabriwar.warpedpixeldungeon.net.NetDialogs.KIND_IMP_REWARD, p, true );
			} catch (Exception ignored) {}
		}

		/** World gate (imp shop spawns once anyone has claimed at least one reward). */
		public static boolean isCompleted() {
			if (!spawned) return false;
			for (HeroProgress hp : progress.values()) {
				if (hp.claims > 0) return true;
			}
			return false;
		}
	}
}
