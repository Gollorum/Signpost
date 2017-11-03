package gollorum.signpost.util;

import java.io.File;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ObfuscationReflectionHelper;
import gollorum.signpost.util.collections.Lurchsauna;
import net.minecraft.block.Block;
import net.minecraft.block.BlockTextureStealer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.FileStealer;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.ResourcePackRepository;

public class ResourceBrowser {

//	private static Map<String[], Lurchsauna<String>> saved = new Lurchpaerchensauna<String[], Lurchsauna<String>>();
    
	private static final String[] PNG = {".png"};
	
	public static Lurchsauna<String> getAllPNGs(Minecraft minecraft){
//		Lurchsauna<String> ret = saved.get(png);
//		if(ret==null){
//		Lurchsauna<String> ret = new Lurchsauna<String>();
		Lurchsauna<String> ret = getAllFiles(PNG, minecraft);
		for(Object object: Block.blockRegistry){
			if(object instanceof Block){
				Set<String> textureNames = BlockTextureStealer.INSTANCE.getTextureNames((Block) object);
				for(String textureName: textureNames){
					String add = fixBlockTextureName(textureName);
					ret.add(add);
				}
			}
		}
		return ret;
//		}else{
//			return ret;
//		}
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

//	public static Lurchsauna<String> getAllFiles(String[] postFixes, Minecraft minecraft){
//		Lurchsauna<String> ret = new Lurchsauna<String>();
//		Map mapTextureObjects = (Map) ObfuscationReflectionHelper.getPrivateValue(TextureManager.class, minecraft.getTextureManager(), 1);
//		for(Object object: mapTextureObjects.keySet()){
//			if(object instanceof ResourceLocation){
//				ret.add(object.toString());
//			}
//		}
//		return ret;
//	}
	
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
//				for(ModContainer container: Loader.instance().getModList()){
//					if(container.getSource().exists()){
//						files.add(container.getSource());
//					}
//				}
			}

//			System.out.println(Loader.instance().getConfigDir().getParentFile()+"\\\\versions\\\\"+Loader.MC_VERSION+"\\\\");
			files.add(new File(Loader.instance().getConfigDir().getParentFile()+"\\versions\\"+Loader.MC_VERSION+"\\"));
			
			for(File now: files){
				ret.addAll(handleFile(now, postFixes));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
//		saved.put(postFixes, ret);
		return ret;
	}
	
	private static Collection<? extends File> handleResourcePack(IResourcePack pack) throws Exception{
		if(pack instanceof AbstractResourcePack){
			HashSet<File> files = new HashSet<File>();
//			files.add((File) ObfuscationReflectionHelper.getPrivateValue(AbstractResourcePack.class, (AbstractResourcePack)pack, 1));
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
