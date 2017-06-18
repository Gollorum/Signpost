package gollorum.signpost.util;

import java.io.File;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import gollorum.signpost.util.collections.Lurchpaerchensauna;
import gollorum.signpost.util.collections.Lurchsauna;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.LegacyV2Adapter;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class ResourceBrowser {

	private static Map<String[], Lurchsauna<String>> saved = new Lurchpaerchensauna<String[], Lurchsauna<String>>();
    
	private static final String[] png = {".png"};
	
	public static Lurchsauna<String> getAllPNGs(Minecraft minecraft){
		Lurchsauna<String> ret = saved.get(png);
		if(ret==null){
			return getAllFiles(png, minecraft);
		}else{
			return ret;
		}
	}
	
	public static Lurchsauna<String> getAllFiles(String[] postFixes, Minecraft minecraft){
		Lurchsauna<String> ret = new Lurchsauna<String>();
		HashSet<File> files = new HashSet<File>();
		try {
			List<IResourcePack> resourcePackList = (List<IResourcePack>) ObfuscationReflectionHelper.getPrivateValue(FMLClientHandler.class, FMLClientHandler.instance(), 11);
			for(IResourcePack pack: resourcePackList){
				files.addAll(handleResourcePack(pack));
			}
			for(File now: files){
				ret.addAll(handleFile(now, postFixes));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		saved.put(postFixes, ret);
		return ret;
	}
	
	private static Collection<? extends File> handleResourcePack(IResourcePack pack) throws Exception{
		if(pack instanceof AbstractResourcePack){
			HashSet<File> files = new HashSet<File>();
			files.add((File) ObfuscationReflectionHelper.getPrivateValue(AbstractResourcePack.class, (AbstractResourcePack)pack, 1));
			return files;
		}else if(pack instanceof LegacyV2Adapter){
			return handleResourcePack((IResourcePack) ObfuscationReflectionHelper.getPrivateValue(LegacyV2Adapter.class, (LegacyV2Adapter)(pack), 0));
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
			if(file.getName().endsWith(".jar") || file.getName().endsWith(".zip")){
				try {
					ZipFile zipFile = new ZipFile(file);
					Enumeration<? extends ZipEntry> zipEnum = zipFile.entries();
					while(zipEnum.hasMoreElements()){
						ZipEntry zipEntry = zipEnum.nextElement();
						for(String nowPostFix: postFixes){
							if(zipEntry.getName().endsWith(nowPostFix)){
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
					if(file.getName().endsWith(nowPostFix)){
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
	
}
