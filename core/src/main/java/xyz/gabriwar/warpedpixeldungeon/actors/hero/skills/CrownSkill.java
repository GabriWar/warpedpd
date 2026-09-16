package xyz.gabriwar.warpedpixeldungeon.actors.hero.skills;

import com.watabou.noosa.Image;
import java.util.ArrayList;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.ArmorAbility;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClassArmor;
import xyz.gabriwar.warpedpixeldungeon.ui.HeroIcon;

/** Quickslot adapter: casting still goes through the equipped armor's normal checks. */
public class CrownSkill extends Skill {
	public static final String TAG = "crown_ability";
	public final ArmorAbility ability;

	public CrownSkill(ArmorAbility ability){
		this.ability = ability;
		tag = TAG;
		level = 1;
	}

	@Override public String name(){ return ability.name(); }
	@Override public String info(){ return ability.desc(); }
	@Override public Image quickslotIcon(){ return new HeroIcon(ability); }
	@Override public boolean requestUpgrade(){ return false; }
	@Override public String quickslotStatus(){
		Hero hero = Dungeon.hero;
		return hero != null && hero.belongings.armor() instanceof ClassArmor
				? ((ClassArmor)hero.belongings.armor()).status() : "";
	}
	@Override public ArrayList<String> actions(Hero hero){
		ArrayList<String> actions = new ArrayList<>();
		if (hero.armorAbility == ability && hero.belongings.armor() instanceof ClassArmor
				&& ((ClassArmor)hero.belongings.armor()).charge >= ability.chargeUse(hero)){
			actions.add(AC_CAST);
		}
		return actions;
	}
	@Override public void execute(Hero hero, String action){
		if (AC_CAST.equals(action) && hero.armorAbility == ability
				&& hero.belongings.armor() instanceof ClassArmor){
			ClassArmor armor = (ClassArmor)hero.belongings.armor();
			armor.execute(hero, armor.defaultAction());
		}
	}
}
