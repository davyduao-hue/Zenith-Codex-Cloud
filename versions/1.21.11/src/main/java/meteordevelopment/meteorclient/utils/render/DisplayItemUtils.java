package meteordevelopment.meteorclient.utils.render;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
public final class DisplayItemUtils { private DisplayItemUtils() {} public static ItemStack toStack(Item item) { return new ItemStack(item); } }
