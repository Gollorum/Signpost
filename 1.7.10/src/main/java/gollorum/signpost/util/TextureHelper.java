package gollorum.signpost.util;

import cpw.mods.fml.client.FMLClientHandler;
import gollorum.signpost.blocks.SuperPostPost;
import gollorum.signpost.blocks.tiles.SuperPostPostTile;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class TextureHelper {

	private static TextureHelper INSTANCE;
	
	private TextureHelper(){}
	
	public static TextureHelper instance(){
		return INSTANCE==null? INSTANCE=new TextureHelper(): INSTANCE;
	}
	
	public ResourceLocation getHeldBlockTexture(EntityPlayer player, World worldIn, int x, int y, int z){
		try{
			Vec3 look = player.getLookVec();
			int side = getFacingFromVector((float)-look.xCoord, (float)-look.yCoord, (float)-look.zCoord);
			
			Block block = Block.getBlockFromItem(player.getHeldItem().getItem());
			String textureName = block.getBlockTextureFromSide(side).getIconName();
			String resourceName = textureNameToResourceName(textureName);

			ResourceLocation ret = new ResourceLocation(resourceName);
			
			FMLClientHandler.instance().getClient().getResourceManager().getResource(ret);
			return ret;
		}catch(Exception e){return null;}
	}
	
    public static int getFacingFromVector(float x, float y, float z){
        EnumFacing enumfacing = EnumFacing.NORTH;
        float f = Float.MIN_VALUE;

        for (EnumFacing enumfacing1 : EnumFacing.values())
        {
            float f1 = x * (float)enumfacing1.getFrontOffsetX() + y * (float)enumfacing1.getFrontOffsetY() + z * (float)enumfacing1.getFrontOffsetZ();

            if (f1 > f)
            {
                f = f1;
                enumfacing = enumfacing1;
            }
        }

        return enumfacing.ordinal();
    }
	
	private String textureNameToResourceName(String textureName){
		if(textureName.equals("missingno")){
			return null;
		}
		String[] split;
		if(textureName.contains(":")){
			split = textureName.split(":");
			if(split.length!=2){
				return null;
			}
		}else{
			split = new String[2];
			split[0] = "minecraft";
			split[1] = textureName;
		}
		if(!split[1].startsWith("textures/blocks/")){
			if(!split[1].startsWith("blocks/")){
				split[1] = "blocks/"+split[1];
			}
			split[1] = "textures/"+split[1];
		}
		if(!endsWithIgnoreCase(split[1], ".png")){
			split[1] += ".png";
		}
		return split[0]+":"+split[1];
	}

	public boolean setTexture(int x, int y, int z){
		EntityPlayer player = FMLClientHandler.instance().getClient().thePlayer;
		World world = FMLClientHandler.instance().getClient().theWorld;
		ResourceLocation texture = getHeldBlockTexture(player, world, x, y, z);
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if(texture==null ||! (tileEntity instanceof SuperPostPostTile)){
			return false;
		}else{
			SuperPostPostTile superTile = (SuperPostPostTile) tileEntity;
			superTile.getPaintObject().setTexture(texture);
			superTile.setPaintObject(null);
			superTile.setAwaitingPaint(false);
			((SuperPostPost)superTile.getBlockType()).sendPostBasesToServer(superTile);
			return true;
		}
	}
	
	private static boolean endsWithIgnoreCase(String str1, String str2){
		return str1.length()<str2.length() || str1.substring(str1.length()-str2.length()).equalsIgnoreCase(str2);
	}
}
