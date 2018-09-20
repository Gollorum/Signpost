package gollorum.signpost.util;

import java.util.HashMap;
import java.util.Map;

import gollorum.signpost.blocks.BigPostPost;
import gollorum.signpost.blocks.PostPost;
import gollorum.signpost.util.code.MinecraftIndependent;

@MinecraftIndependent
public abstract class BiomePaintEntry {
	
	protected Map<String, String> textures;

	public String getTexture(String key){
		return textures.get(key);
	}
	
	public static final BiomePaintEntry DEFAULT_PLAINS = new BiomePaintEntry(){
		{
			textures = new HashMap<String, String>();
			textures.put("PostSign", PostPost.PostType.OAK.texture.toString());
			textures.put("BigPostSign", BigPostPost.BigPostType.OAK.texture.toString());
			textures.put("PostPost", PostPost.PostType.OAK.resLocMain.toString());
		}
	};
	
	public static final BiomePaintEntry DEFAULT_DESERT = new BiomePaintEntry(){
		{
			textures = new HashMap<String, String>();
			textures.put("PostSign", "textures/blocks/sandstone_smooth.png");
			textures.put("BigPostSign", "textures/blocks/sandstone_smooth.png");
			textures.put("PostPost", "textures/blocks/cobblestone.png");
		}
	};
	
	public static final BiomePaintEntry DEFAULT_TAIGA = new BiomePaintEntry(){
		{
			textures = new HashMap<String, String>();
			textures.put("PostSign", PostPost.PostType.SPRUCE.texture.toString());
			textures.put("BigPostSign", BigPostPost.BigPostType.SPRUCE.texture.toString());
			textures.put("PostPost", PostPost.PostType.SPRUCE.resLocMain.toString());
		}
	};
	
	public static final BiomePaintEntry DEFAULT_SAVANNA = new BiomePaintEntry(){
		{
			textures = new HashMap<String, String>();
			textures.put("PostSign", PostPost.PostType.ACACIA.texture.toString());
			textures.put("BigPostSign", BigPostPost.BigPostType.ACACIA.texture.toString());
			textures.put("PostPost", PostPost.PostType.ACACIA.resLocMain.toString());
		}
	};
}
