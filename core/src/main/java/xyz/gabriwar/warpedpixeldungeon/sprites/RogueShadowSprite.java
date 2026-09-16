package xyz.gabriwar.warpedpixeldungeon.sprites;
import com.watabou.noosa.TextureFilm;
/** Native 12x15 translucent black skeleton animation, matching the existing mob scale. */
public class RogueShadowSprite extends MobSprite {
    public RogueShadowSprite(){
        texture("sprites/rogue_shadows.png");TextureFilm f=new TextureFilm(texture,12,15);
        idle=new Animation(6,true);idle.frames(f,0,0,0,0,1,2,3);
        run=new Animation(15,true);run.frames(f,4,5,6,7,8,9);
        attack=new Animation(15,false);attack.frames(f,14,15,16);
        die=new Animation(12,false);die.frames(f,10,11,12,13);
        play(idle);
    }
    @Override public int blood(){return 0xFF202126;}
}
