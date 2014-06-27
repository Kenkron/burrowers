package kenkron.burrowers;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.Direction;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**This AI represents a burrowing task for the mobs that 
 * just can't navigate to the player, but */
public class EntityAIBurrow extends EntityAIBase{

	/**The Class of the Entity that this AI targets.*/
	protected Class classTarget;
	/**Long memory means this creature will remain agressive
	 * even if it can't path to its target (eg. ZombiePigmen)*/
	protected boolean longMemory;
	/**Speed modifier for moving towards the target.
	 * If 1.0, the attacker will not speed up to attack.*/
	protected double speedTowardsTarget;
	/**The entity that is attacking.*/
	protected EntityCreature attacker;
	/**The world in which the attacker lives*/
	protected World world;
	/**The path through the world to the target.*/
	protected PathEntity pathToTarget;
	
	/**not sure about this.  Goes to ten if no path is found.*/
	protected int failedPathFindingPenalty;
	/**seems to go higher if it's harder to reach the target*/
	protected int obstructionLevel;
	/**the position of the feet of the attacker*/
	protected double x,y,z;
	
	/**(presumably) a timer for attacking.*/
	protected int digTick;
	
	/**the time it takes a zombie to dig*/
	public static final int DIG_TIME=20;
	
    public EntityAIBurrow(EntityCreature par1EntityCreature, Class par2Class, double speed, boolean persistant)
    {
        this(par1EntityCreature, speed, persistant);
        this.classTarget = par2Class;
    }

    public EntityAIBurrow(EntityCreature par1EntityCreature, double speed, boolean persistant)
    {
        this.attacker = par1EntityCreature;
        this.world = par1EntityCreature.worldObj;
        this.speedTowardsTarget = speed;
        this.longMemory = persistant;
        this.setMutexBits(3);
    }
	
	@Override
	public boolean shouldExecute() {
        EntityLivingBase target = this.attacker.getAttackTarget();

        if (target == null)
        {
            return false;
        }

        else if (!target.isEntityAlive())
        {
            return false;
        }
        else if (this.classTarget != null && !this.classTarget.isAssignableFrom(target.getClass()))
        {
            return false;
        }
        else
        {
            if (-- this.obstructionLevel <= 0)
            {
               this.pathToTarget = this.attacker.getNavigator().getPathToEntityLiving(target);
               this.obstructionLevel = 4 + this.attacker.getRNG().nextInt(7);
               return this.pathToTarget != null;
            }
            else
            {
                return true;
            }
        }
	}
	
    /**
     * Execute a one shot task or start executing a continuous task
     */
    public void startExecuting()
    {
        this.attacker.getNavigator().setPath(this.pathToTarget, this.speedTowardsTarget);
        this.obstructionLevel = 0;
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    public boolean continueExecuting()
    {
        EntityLivingBase target = this.attacker.getAttackTarget();
        
        if (target == null){
        	return false;
        }
        
        if (!target.isEntityAlive()){
        	return false; 
        }
        
        return this.attacker.getNavigator().noPath() && 
        			this.attacker.isWithinHomeDistance(
        			MathHelper.floor_double(target.posX), 
        			MathHelper.floor_double(target.posY), 
        			MathHelper.floor_double(target.posZ));
    }

    /**
     * Resets the task
     */
    public void resetTask()
    {
        this.attacker.getNavigator().clearPathEntity();
    }
    
    
    /**
     * Updates the task
     */
    public void updateTask()
    {
        EntityLivingBase entitylivingbase = this.attacker.getAttackTarget();
        this.attacker.getLookHelper().setLookPositionWithEntity(entitylivingbase, 30.0F, 30.0F);
        double d0 = this.attacker.getDistanceSq(entitylivingbase.posX, entitylivingbase.boundingBox.minY, entitylivingbase.posZ);
        double d1 = (double)(this.attacker.width * 2.0F * this.attacker.width * 2.0F + entitylivingbase.width);
        --this.obstructionLevel;

        //if information is outdated/unititialized
        if (this.obstructionLevel <= 0 && 
        	(this.x == 0.0D && this.y == 0.0D && 
        	this.z == 0.0D || 
        	entitylivingbase.getDistanceSq(this.x, this.y, this.z) >= 1.0D || 
        	this.attacker.getRNG().nextFloat() < 0.05F))
        {
        	//initialize some information
            this.x = entitylivingbase.posX;
            this.y = entitylivingbase.boundingBox.minY;
            this.z = entitylivingbase.posZ;
            this.obstructionLevel = failedPathFindingPenalty + 4 + this.attacker.getRNG().nextInt(7);

            if (this.attacker.getNavigator().getPath() != null)
            {
                PathPoint finalPathPoint = this.attacker.getNavigator().getPath().getFinalPathPoint();
                if (finalPathPoint != null && entitylivingbase.getDistanceSq(finalPathPoint.xCoord, finalPathPoint.yCoord, finalPathPoint.zCoord) < 1)
                {
                    failedPathFindingPenalty = 0;
                }
                else
                {
                    failedPathFindingPenalty += 10;
                }
            }
            else
            {
                failedPathFindingPenalty += 10;
            }

            if (d0 > 1024.0D)
            {
                this.obstructionLevel += 10;
            }
            else if (d0 > 256.0D)
            {
                this.obstructionLevel += 5;
            }

            if (!this.attacker.getNavigator().tryMoveToEntityLiving(entitylivingbase, this.speedTowardsTarget))
            {
                this.obstructionLevel += 15;
            }
        }

        //this is the code for digging!
        this.digTick = Math.max(this.digTick - 1, 0);
        
        EntityLivingBase target = attacker.getAttackTarget();
        
        if (this.digTick <= 0 &&
        		target!=null &&
        		failedPathFindingPenalty>0)
        {
            this.digTick = 20;
            
            int x=(int)Math.floor(attacker.posX);            
            int y=(int)Math.floor(attacker.posY);            
            int z=(int)Math.floor(attacker.posZ);
            
            int monsterDirection = Direction.getMovementDirection(attacker.getLookVec().xCoord, attacker.getLookVec().zCoord);
            
            int xoff = Direction.offsetX[monsterDirection];
            int zoff = Direction.offsetZ[monsterDirection];
            
            Block above = world.getBlock(x,y+2,z);
            Block aboveForeward = world.getBlock(x+xoff,y+2,z+zoff);
            Block eyeLevel = world.getBlock(x+xoff,y+1,z+zoff);
            Block footLevel = world.getBlock(x+xoff,y,z+zoff);
            Block floorForward = world.getBlock(x+xoff, y-1, z+zoff);
            		
            int targety = (int)Math.floor(target.posY);
            //it's time to dig!
            
            //try the eye level block first
            if (!eyeLevel.isAir(world, x+xoff, y+1, z+zoff)){
            	breakBlock(world, eyeLevel, x+xoff, y+1, z+zoff);
            }
            //next the foot level
            else if (!(targety>y)&&
            		!(world.isAirBlock(x+xoff, y, z+zoff))){
            	breakBlock(world,footLevel,x+xoff,y,z+zoff);
            }
            //next above
            else if (targety>y&&
            		!(above.isAir(world, x, y+2, z))){
            	breakBlock(world, above, x, y+2, z);
            }
            //then the above forward
            else if (targety>y &&
            		!aboveForeward.isAir(world, x+xoff, y+2, z+zoff)){
            	breakBlock(world, aboveForeward, x+xoff, y+2, z+zoff);
            	
            }
            //then the below corner
            else if (targety>y &&
            		!floorForward.isAir(world, x+xoff, y-1, z+zoff)){
            	breakBlock(world, floorForward, x+xoff, y-1, z+zoff);
            }
        }
    }
    
    public void breakBlock(World w, Block b, int x,int y,int z){
    	ArrayList<ItemStack> drops=b.getDrops(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
    	b.dropBlockAsItem(w, x, y, z, world.getBlockMetadata(x, y, z), 0);
    	world.removeTileEntity(x, y, z);
    	world.setBlockToAir(x, y, z);
    }
}
