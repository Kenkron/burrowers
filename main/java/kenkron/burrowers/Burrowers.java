package kenkron.burrowers;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraftforge.event.entity.living.ZombieEvent;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.registry.EntityRegistry;

@Mod(modid = Burrowers.MODID, version = Burrowers.VERSION)
public class Burrowers
{
    public static final String MODID = "Burrowers";
    public static final String VERSION = "1.0";
    
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
    	int burrowerID = getUniqueEntityID();
		EntityList.addMapping(BurrowingZombie.class, "Burrower", burrowerID, 0x0F7F0F, 0x3F7F3F);
//		EntityRegistry.registerGlobalEntityID(
//		BurrowingZombie.class, "Burrower", burrowerID);
EntityRegistry.addSpawn(
		BurrowingZombie.class, 100, 10, 10, 
		EnumCreatureType.monster);
    }
    
	protected static int startEntityID=0; 
	public static int getUniqueEntityID(){
		do {
			startEntityID++;
		}while (EntityList.getStringFromID(startEntityID) != null);
		return startEntityID;
	}
}
