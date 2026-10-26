package meteordevelopment.meteorclient.zenith.mixin;

import meteordevelopment.meteorclient.zenith.ZenithMenuScreen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
public abstract class ZenithLoadingMixin extends Screen {
    protected ZenithLoadingMixin(Component title) { super(title); }
    @Inject(method="render",at=@At("TAIL"))
    private void zenith$loading(GuiGraphics g,int mx,int my,float delta,CallbackInfo ci) {
        double time=System.nanoTime()/1e9;
        ZenithMenuScreen.background(g,width,height,time);
        ZenithMenuScreen.floatingStar(g,width/2,height/2-18,25,time);
        ZenithMenuScreen.centered(g,"Entering your world",width/2,height/2+35,0xffd6c4ec,1);
        ZenithMenuScreen.centered(g,"ZENITH CLIENT 2.0",width/2,height/2+53,0xff9f7fbd,.65f);
    }
}
