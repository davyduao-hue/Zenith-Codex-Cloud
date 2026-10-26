package meteordevelopment.meteorclient.zenith;

import me.pindour.catppuccin.api.text.RichText;
import me.pindour.catppuccin.gui.themes.catppuccin.CatppuccinWidget;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import static meteordevelopment.meteorclient.MeteorClient.mc;

/** Zenith's pill switches and individual glass rows, using the native Meteor module contract. */
public final class WZenithModule extends WPressable implements CatppuccinWidget {
    private final Module module;
    private final RichText title;
    private double active,hover;
    private double pulse=1;
    public WZenithModule(Module module,String title) {
        this.module=module;this.title=RichText.of(title);tooltip=module.description;
        active=module.isActive()?1:0;
    }
    @Override protected void onCalculateSize() {
        width=theme().textWidth(title)+theme.scale(62);
        height=theme.textHeight()+theme.scale(14);
    }
    @Override protected void onPressed(int button) {
        if(button==0){module.toggle();pulse=0;}
        else if(button==1)mc.setScreen(theme.moduleScreen(module));
    }
    @Override protected void onRender(GuiRenderer renderer,double mouseX,double mouseY,double delta) {
        ZenithGuiTheme zenith=(ZenithGuiTheme)theme;
        boolean reduced=zenith.reducedMotion.get();
        active=reduced?(module.isActive()?1:0):UiMotion.approach(active,module.isActive()?1:0,16*zenith.motionSpeed.get(),delta);
        hover=reduced?(mouseOver?1:0):UiMotion.approach(hover,mouseOver?1:0,18*zenith.motionSpeed.get(),delta);
        pulse=Math.min(1,pulse+Math.max(0,Math.min(.1,delta))*zenith.motionSpeed.get()/ .5);
        double pad=theme.scale(5),inset=theme.scale(3);
        Color accent=theme().accentColor();
        roundedRect().pos(x+inset,y+inset).size(width-inset*2,height-inset*2).radius(theme.scale(7))
            .color(new Color(accent.r,accent.g,accent.b,(int)(8+active*35+hover*20)))
            .outline(new Color(accent.r,accent.g,accent.b,(int)(12+active*45+hover*32)),.7f).render();
        if(!reduced&&zenith.clickPulses.get()&&pulse<1) {
            double spread=(1-Math.pow(1-pulse,3))*(width-inset*2);
            roundedRect().pos(x+(width-spread)/2,y+height-theme.scale(3)).size(spread,theme.scale(1.5)).radius(1)
                .color(new Color(accent.r,accent.g,accent.b,(int)(180*(1-pulse)))).render();
        }
        double sw=theme.scale(25),sh=theme.scale(13),sx=x+width-sw-pad*2,sy=y+(height-sh)/2;
        roundedRect().pos(sx,sy).size(sw,sh).radius(sh/2)
            .color(ZenithDraw.mix(theme().surface1Color(),accent,active)).render();
        double knob=sh-theme.scale(4);
        roundedRect().pos(sx+theme.scale(2)+(sw-sh)*active,sy+theme.scale(2)).size(knob,knob).radius(knob/2)
            .color(ZenithDraw.mix(theme().overlay1Color(),new Color(255,247,255),active)).render();
        renderer().text(title,x+pad*2+hover*theme.scale(2),y+(height-theme.textHeight())/2,
            ZenithDraw.mix(theme().textColor(),new Color(242,218,255),active));
    }
}
