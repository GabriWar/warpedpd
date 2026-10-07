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
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.PlateArmor;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.Artifact;
import xyz.gabriwar.warpedpixeldungeon.items.quest.DwarfToken;
import xyz.gabriwar.warpedpixeldungeon.items.rings.Ring;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
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
import java.util.Collection;

public class Imp extends NPC {

	{
		spriteClass = ImpSprite.class;

		properties.add(Property.IMMOVABLE);
	}

	private boolean seenBefore = false;

	@Override
	public Notes.Landmark landmark() {
		return Quest.isCompleted() ? null : Notes.Landmark.IMP;
	}

	@Override
	protected boolean act() {
		if (Dungeon.hero.buff(AscensionChallenge.class) != null){
			die(null);
			return true;
		}

		//extra logic in case imp is holding the quest reward
		if (Quest.isCompleted() && Quest.reward != null){
			Dungeon.level.drop(Quest.reward, pos);
			throwItems();
			Quest.reward = null;
		}

		if (!Quest.oldQuest && Quest.isCompleted() && Quest.score > 2000
				&& fieldOfView != null && !fieldOfView[Dungeon.hero.pos]){
			flee();
		} else if (!Quest.given(Dungeon.hero) && Dungeon.level.visited[pos]) {
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
		final Hero h = (Hero) c;

		//pre v4.0.0 logic: the token hunt, tracked per hero so every player on
		//the floor can accept it, hand in their own tokens and claim their own ring
		if (Quest.oldQuest) {
			Quest.HeroProgress hp = Quest.progressFor( h.id() );

			if (hp != null && hp.given) {
				if (hp.claims >= Quest.MAX_CLAIMS) return true;
				DwarfToken tokens = h.belongings.getItem( DwarfToken.class );
				if (tokens != null && tokens.quantity() >= Quest.tokensRequired()) {
					showReward( h );
				} else {
					tell( h, Quest.alternative ?
							Messages.get(this, "old_monks_2", Messages.titleCase(h.name()))
							: Messages.get(this, "old_golems_2", Messages.titleCase(h.name())) );
				}
			} else {
				Quest.give( h );
				tell( h, Messages.get(this, "old_intro") + "\n\n" + (Quest.alternative ?
						Messages.get(this, "old_monks_1", Messages.titleCase(h.name()))
						: Messages.get(this, "old_golems_1", Messages.titleCase(h.name()))) );
			}
		} else {
			if (!Quest.given()){
				if (h.isRemote) {
					Quest.given = true;
					Quest.completed = false;
					QuestSupport.sendInfo( this, h, Messages.get(Imp.this, "quest_intro_1")
							+ "\n\n" + Messages.get(Imp.this, "quest_intro_2") );
				} else {
					Game.runOnRenderThread(new Callback() {
						@Override
						public void call() {
							GameScene.show(new WndQuest(Imp.this, Messages.get(Imp.this, "quest_intro_1")) {
								@Override
								public void hide() {
									super.hide();

									Quest.given = true;
									Quest.completed = false;

									tell(h, Messages.get(Imp.this, "quest_intro_2"));
								}
							});
						}
					});
				}
			} else if (!Quest.isCompleted()) {
				tell(h, Messages.get(Imp.this, "quest_in_progress"));
			} else {
				if (Quest.score <= 2000){
					tell(h, Messages.get(Imp.this, "quest_completed_bad"));
				} else if (Quest.score < 4000){
					tell(h, Messages.get(Imp.this, "quest_completed_good"));
				} else {
					tell(h, Messages.get(Imp.this, "quest_completed_great"));
				}
			}
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

		private static boolean spawned;

		//variables exclusive to old, pre-4.0.0 Imp quest
		private static boolean oldQuest = false;
		private static boolean alternative; //true= golems, false = monks

		// Old quest progress is per hero: each hero accepts the quest, collects their own
		// DwarfToken stack (tokens are shared floor loot, but each hero picks into their
		// own backpack), and claims their own rolled ring, up to MAX_CLAIMS rings each.
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

		private static final PerHeroStore<HeroProgress> progress = new PerHeroStore<>();

		//variables shared by both quests
		private static boolean given;
		private static boolean completed;
		public static Item reward; //just used to hold the reward if her's inventory is full in new version

		//variables exclusive to new quest
		public static ArrayList<Item> rewardOptions = new ArrayList<>();
		public static int hazardFreebies; //player gets two free hits from hazards before they start penalizing score
		public static boolean mirrorUsed = false;
		private static int score; //Not the score used in rankings! This score has no penalty applied

		public static void reset() {
			spawned = false;
			given = false;
			completed = false;

			reward = null;
			hazardFreebies = 2;
			mirrorUsed = false;
			score = 0;
			progress.clear();
		}

		public static boolean alternative() { return alternative; }
		public static HeroProgress progressFor( int heroId ) { return progress.get( heroId ); }

		// Tokens needed to claim: monks(alternative) ask for 5, golems ask for 4.
		public static int tokensRequired() { return alternative ? 5 : 4; }

		private static final String NODE        = "demon";

		private static final String SPAWNED     = "spawned";

		private static final String OLD_QUEST   = "old_quest";
		private static final String ALTERNATIVE = "alternative";
		private static final String REWARD      = "reward";
		private static final String PROGRESS    = "progress";
		private static final String HERO_ID     = "hero_id";
		private static final String CLAIMS      = "claims";

		private static final String GIVEN       = "given";
		private static final String COMPLETED   = "completed";

		private static final String HAZRD_FREEBIES = "hazard_freebies";
		private static final String SCORE       = "score";
		private static final String REWARD_OPTIONS = "reward_options";
		private static final String MIRROR_USED = "mirror_used";


		public static void storeInBundle( Bundle bundle ) {

			Bundle node = new Bundle();

			node.put( SPAWNED, spawned );

			if (spawned) {
				node.put( OLD_QUEST, oldQuest );
				node.put( ALTERNATIVE, alternative );
				progress.store( node, PROGRESS );

				node.put( GIVEN, given );
				node.put( COMPLETED, completed );
				node.put( REWARD, reward );

				node.put( HAZRD_FREEBIES, hazardFreebies );
				node.put( SCORE, score );
				node.put( REWARD_OPTIONS, rewardOptions );
				node.put( MIRROR_USED, mirrorUsed );
			}

			bundle.put( NODE, node );
		}

		public static void restoreFromBundle( Bundle bundle ) {

			Bundle node = bundle.getBundle( NODE );

			progress.clear();

			if (!node.isNull() && (spawned = node.getBoolean( SPAWNED ))) {

				if (node.contains( OLD_QUEST )){
					oldQuest = node.getBoolean( OLD_QUEST );
				} else {
					oldQuest = true;
				}
				if (oldQuest){
					alternative	= node.getBoolean( ALTERNATIVE );
					// Guarded for pre-per-hero saves (no "progress" array).
					progress.restore( node, PROGRESS );
					score = 0;
					rewardOptions.clear();
					mirrorUsed = false;
				} else {
					alternative = false;
					hazardFreebies = node.getInt( HAZRD_FREEBIES );
					mirrorUsed = node.getBoolean( MIRROR_USED );
					score = node.getInt( SCORE );
					rewardOptions = new ArrayList<>((Collection<Item>) (Collection<?>) node.getCollection( REWARD_OPTIONS ));
				}

				reward = (Item)node.get( REWARD );

				given = node.getBoolean( GIVEN );
				completed = node.getBoolean( COMPLETED );
			}
		}

		public static ArrayList<Room> spawn( ArrayList<Room> rooms ) {
			if (!spawned && Dungeon.depth > 16 && Random.Int( 20 - Dungeon.depth ) == 0) {

				rooms.add(new AmbitiousImpRoom());
				spawned = true;

				oldQuest = false;
				reward = null;
				score = 0;

				given = false;
				mirrorUsed = false;
				progress.clear();

				rewardOptions.clear();
				Item artif = Generator.randomArtifact();
				//generate a ring instead
				if (artif != null){
					((Artifact)artif.identify(false)).transferUpgrade(5);
				} else {
					artif = Generator.random(Generator.Category.RING);
					//we delay the ID on rings until the boss is defeated
					artif.level(Random.IntRange(2, 4));
				}
				rewardOptions.add(artif);

				Item ring;
				do {
					ring = Generator.random(Generator.Category.RING);
				} while (ring.getClass() == artif.getClass()); //rare cases of the same kind of ring twice
				//we delay the ID on rings until the boss is defeated
				ring.level(Random.IntRange(2, 4));
				rewardOptions.add(ring);

				if (Random.Int(2) == 0) {
					rewardOptions.add(((Weapon)Generator.random(Generator.Category.WEP_T5)).enchant().identify(false).level(Random.IntRange(2, 4)));
					rewardOptions.add(((Weapon)Generator.random(Generator.Category.MIS_T4)).enchant().identify(false).level(Random.IntRange(3, 5)));
				} else {
					rewardOptions.add(((Weapon)Generator.random(Generator.Category.MIS_T5)).enchant().identify(false).level(Random.IntRange(2, 4)));
					rewardOptions.add(((Weapon)Generator.random(Generator.Category.WEP_T4)).enchant().identify(false).level(Random.IntRange(3, 5)));
				}
				rewardOptions.add(new PlateArmor().inscribe().identify(false).level(Random.IntRange(2, 4)));
				Wand w = (Wand) Generator.random(Generator.Category.WAND);
				w.identify(false).level(Random.IntRange(2, 4));
				w.curCharges = w.maxCharges;
				rewardOptions.add(w);

				for (Item i : rewardOptions){
					i.cursed = false;
				}
			}

			return rooms;
		}

		public static boolean given(){
			return given;
		}

		/** Old quest: has this specific hero accepted it. New quest: the single shared flag. */
		public static boolean given( Hero h ) {
			if (!oldQuest) return given;
			HeroProgress hp = progress.get( h.id() );
			return hp != null && hp.given;
		}

		public static boolean isOld(){
			return oldQuest;
		}

		/** Old quest: register it for a hero and roll their first ring reward. */
		public static void give( Hero h ) {
			HeroProgress hp = new HeroProgress();
			hp.heroId = h.id();
			hp.reward = rollRing();
			hp.given = true;
			progress.put( hp );
			given = true;
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

		// Any hero who can still claim — gates whether kills drop tokens.
		private static boolean anyActive() {
			for (HeroProgress hp : progress.values()) {
				if (hp.given && hp.claims < MAX_CLAIMS) return true;
			}
			return false;
		}

		public static void oldProcess( Mob mob ) {
			// Tokens are shared floor loot — drop one at the corpse for whoever collects it,
			// as long as at least one hero is on the matching quest.
			if (spawned && oldQuest && anyActive() && Dungeon.depth != 20) {
				if ((alternative && mob instanceof Monk) ||
					(!alternative && mob instanceof Golem)) {

					Dungeon.level.drop( new DwarfToken(), mob.pos ).sprite.drop();
				}
			}
		}

		/** Old quest: hand in tokens and grant the ring to a hero. Runs on the actor thread
		 *  (remote via NetDialogs.resolve) or render thread (local WndImp). */
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

			oldComplete();
			maybeDespawnImp();
		}

		/** Old quest: a ring has been claimed, the quest counts as done for the world. */
		public static void oldComplete() {
			completed = true;

			Statistics.questScores[3] = 4000;
		}

		public static void complete( int score ){
			completed = true;

			Imp.Quest.score = score;
			Statistics.questScores[3] += score;
			Notes.remove( Notes.Landmark.IMP );
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

		// Route the old-quest reward confirm to a remote hero's client (KIND_IMP_REWARD).
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

		public static boolean isCompleted() {
			return spawned && completed;
		}

		public static boolean earnedShop() {
			return completed && (oldQuest || score > 2000);
		}
	}
}
