package gollorum.signpost.util;

import java.io.File;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import gollorum.signpost.util.collections.Lurchsauna;
import net.minecraft.block.Block;
import net.minecraft.block.BlockTextureStealer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.FileStealer;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class ResourceBrowser {
    
	private static final String[] PNG = {".png"};
	
	public static Lurchsauna<String> getAllPNGs(Minecraft minecraft){
		Lurchsauna<String> ret = getAllFiles(PNG, minecraft);
		for(Block block: Block.REGISTRY){
			Set<String> textureNames = BlockTextureStealer.INSTANCE.getTextureNames(block);
			for(String textureName: textureNames){
				String add = fixBlockTextureName(textureName);
				ret.add(add);
			}
		}
		return ret;
	}
	
	private static String fixBlockTextureName(String textureName) {
		String domain;
		String location;
		if(textureName.startsWith("MISSING_ICON_BLOCK")){
			return null;
		}
		if(textureName.contains(":")){
			String[] split = textureName.split(":");
			domain = split[0];
			location = split[1];
		}else{
			domain = "minecraft";
			location = textureName;
		}
		if(!location.startsWith("textures")){
			location = "textures/blocks/"+location;
		}
		if(!location.endsWith("png")){
			location = location+".png";
		}
		return domain+":"+location;
	}
	
	public static Lurchsauna<String> getAllFiles(String[] postFixes, Minecraft minecraft){
		Lurchsauna<String> ret = new Lurchsauna<String>();
		HashSet<File> files = new HashSet<File>();
		try {
			List<IResourcePack> resourcePackList = (List<IResourcePack>) ObfuscationReflectionHelper.getPrivateValue(FMLClientHandler.class, FMLClientHandler.instance(), 12);
			for(IResourcePack pack: resourcePackList){
				files.addAll(handleResourcePack(pack));
			}
			for(Object nw: FMLClientHandler.instance().getClient().getResourcePackRepository().getRepositoryEntries()){
				if(!(nw instanceof ResourcePackRepository.Entry)){
					continue;
				}
				ResourcePackRepository.Entry now = (ResourcePackRepository.Entry) nw;
				files.addAll(handleResourcePack(now.getResourcePack()));
			}

			files.add(new File(Loader.instance().getConfigDir().getParentFile()+"\\versions\\"+Loader.MC_VERSION+"\\"));
			
			for(File now: files){
				ret.addAll(handleFile(now, postFixes));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	private static Collection<? extends File> handleResourcePack(IResourcePack pack) throws Exception{
		if(pack instanceof AbstractResourcePack){
			HashSet<File> files = new HashSet<File>();
			files.add(FileStealer.INSTANCE.getAbstractFile((AbstractResourcePack) pack));
			return files;
		}
		return new HashSet<File>();
	}
	
	private static Collection<? extends String> handleFile(File file, String[] postFixes){
		Lurchsauna<String> ret = new Lurchsauna<String>();
		if(file.isDirectory()){
			for(File now: file.listFiles()){
				ret.addAll(handleFile(now, postFixes));
			}
		}else{
			if(endsWithIgnoreCase(file.getName(), ".jar") || endsWithIgnoreCase(file.getName(), ".zip")){
				try {
					ZipFile zipFile = new ZipFile(file);
					Enumeration<? extends ZipEntry> zipEnum = zipFile.entries();
					while(zipEnum.hasMoreElements()){
						ZipEntry zipEntry = zipEnum.nextElement();
						for(String nowPostFix: postFixes){
							if(endsWithIgnoreCase(zipEntry.getName(), nowPostFix)){
								ret.add(fixResource(zipEntry.getName()));
								break;
							}
						}
					}
					zipFile.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}else{
				for(String nowPostFix: postFixes){
					if(endsWithIgnoreCase(file.getName(), nowPostFix)){
						ret.add(fixResource(file.getPath()));
						break;
					}
				}
			}
		}
		return ret;
	}
	
	private static String fixResource(String path){
		path = path.replaceAll("\\\\", "/");
		if(path.contains("assets/")){
			int i = path.lastIndexOf("assets/");
			path = path.substring(i+"assets/".length());
			String[] split = path.split("/", 2);
			return split[0]+":"+split[1];
		}else{
			return null;
		}
	}
	
	private static boolean endsWithIgnoreCase(String str1, String str2){
		return str1.length()<str2.length() || str1.substring(str1.length()-str2.length()).equalsIgnoreCase(str2);
	}
}
