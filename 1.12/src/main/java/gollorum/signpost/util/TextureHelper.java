package gollorum.signpost.util;

import gollorum.signpost.blocks.BigPostPost;
import gollorum.signpost.blocks.PostPost;
import gollorum.signpost.blocks.SuperPostPost;
import gollorum.signpost.blocks.tiles.SuperPostPostTile;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.FMLClientHandler;

public class TextureHelper {

	private static TextureHelper INSTANCE;
	
	private TextureHelper(){}
	
	public static TextureHelper instance(){
		return INSTANCE==null? INSTANCE=new TextureHelper(): INSTANCE;
	}
	
	public ResourceLocation getHeldBlockTexture(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		try{
			ItemStack stack = player.inventory.getCurrentItem();
			ResourceLocation ret;

			Block block = Block.getBlockFromItem(stack.getItem());
			if(block instanceof PostPost){
				ret = ((PostPost)block).type.texture;
			}else if(block instanceof BigPostPost){
				ret = ((BigPostPost)block).type.texture;
			}else{
				IBlockState blockState = blockStateFromPlayer(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
				
				IBakedModel model = FMLClientHandler.instance().getClient().getRenderItem().getItemModelMesher().getItemModel(stack);
		
				Vec3d look = player.getLookVec();
				EnumFacing side = EnumFacing.getFacingFromVector((float)-look.x, (float)-look.y, (float)-look.z);
				
				BakedQuad quad = model.getQuads(blockState, side, 0).get(0);
				
				String textureName = quad.getSprite().getIconName();
				String resourceName = textureNameToResourceName(textureName);

				ret = new ResourceLocation(resourceName);
			}
			
			FMLClientHandler.instance().getClient().getResourceManager().getResource(ret);
			return ret;
		}catch(Exception e){return null;}
	}

	private IBlockState blockStateFromPlayer(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		try{
			ItemStack itemStack = player.inventory.getCurrentItem();
			Block block = Block.getBlockFromItem(itemStack.getItem());
			return block.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, itemStack.getMetadata(), player, hand);
		}catch(Exception e){
			return null;
		}
	}
	
	private String textureNameToResourceName(String textureName){
		if(textureName.equals("missingno") || !textureName.contains(":")){
			return null;
		}
		String[] split = textureName.split(":");
		if(split.length!=2){
			return null;
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

	public boolean setTexture(BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		EntityPlayer player = FMLClientHandler.instance().getClient().player;
		World world = FMLClientHandler.instance().getClient().world;
		ResourceLocation texture = getHeldBlockTexture(player, world, pos, hand, facing, hitX, hitY, hitZ);
		TileEntity tileEntity = world.getTileEntity(pos);
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
