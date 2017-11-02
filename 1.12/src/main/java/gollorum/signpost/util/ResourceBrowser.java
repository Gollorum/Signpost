package gollorum.signpost.util;

import java.io.File;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import gollorum.signpost.Signpost;
import gollorum.signpost.util.collections.Lurchpaerchensauna;
import gollorum.signpost.util.collections.Lurchsauna;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.DefaultResourcePack;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.LegacyV2Adapter;
import net.minecraft.client.resources.ResourceIndex;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class ResourceBrowser {

	private static final Map<String[], Lurchsauna<String>> saved = new Lurchpaerchensauna<String[], Lurchsauna<String>>();
    
	private static final String[] png = {".png"};
	
	public static Lurchsauna<String> getAllPNGs(Minecraft minecraft){
		Lurchsauna<String> ret = saved.get(png);
		System.out.println("da will wer pngs");
		if(ret==null || ret.size()==0){
			System.out.println("Da haste neue");
			return getAllFiles(png, minecraft);
		}else{
			System.out.println("Da haste alte");
			return ret;
		}
	}
	
	public static Lurchsauna<String> reloadAllPNGs(Minecraft minecraft){
		System.out.println("da will wer pngs reloaden alla dick alla fett alla");
		return getAllFiles(png, minecraft);
	}
	
	public static Lurchsauna<String> getAllFiles(String[] postFixes, Minecraft minecraft){
		System.out.println("GET ALL FILES JETZT VERDAMMT");
		Lurchsauna<String> ret = new Lurchsauna<String>();
		Logger.getLogger(Signpost.MODID).log(Level.ALL, "Get Files?");
		new Exception("ICH HÄZTTE% MJETZ GERN E FILERS DUI SAZ DOIOIO!!!");
		HashSet<File> files = new HashSet<File>();
		try {
//			List<IResourcePack> resourcePackList = (List<IResourcePack>) ObfuscationReflectionHelper.getPrivateValue(FMLClientHandler.class, FMLClientHandler.instance(), 11);
//			for(IResourcePack pack: resourcePackList){
//				files.addAll(handleResourcePack(pack));
//			}
//			for(ResourcePackRepository.Entry now: FMLClientHandler.instance().getClient().getResourcePackRepository().getRepositoryEntries()){
//				files.addAll(handleResourcePack(now.getResourcePack()));
//			}
			if(FMLClientHandler.instance().getClient().getResourceManager() instanceof SimpleReloadableResourceManager){
				SimpleReloadableResourceManager simpleResourceManager = (SimpleReloadableResourceManager)FMLClientHandler.instance().getClient().getResourceManager();
				Map<String, FallbackResourceManager> domainResourceManagers = ObfuscationReflectionHelper.getPrivateValue(SimpleReloadableResourceManager.class, simpleResourceManager, 2);
				for(FallbackResourceManager fallbackResourceManager: domainResourceManagers.values()){
					List<IResourcePack> resourcePacks = ObfuscationReflectionHelper.getPrivateValue(FallbackResourceManager.class, fallbackResourceManager, 1);
					for(IResourcePack pack: resourcePacks){
						files.addAll(handleResourcePack(pack));
					}
				}
			}
			files.addAll(handleResourcePack(FMLClientHandler.instance().getClient().getResourcePackRepository().rprDefaultResourcePack));
			files.addAll(handleResourcePack(FMLClientHandler.instance().getClient().mcDefaultResourcePack));
//			for(String domain: FMLClientHandler.instance().getClient().getResourceManager().getResourceDomains()){
//				System.out.println(domain);
//				files.addAll(handleResourcePack(FMLClientHandler.instance().getResourcePackFor(domain)));
//			}
			for(File now: files){
				ret.addAll(handleFile(now, postFixes));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		saved.put(postFixes, ret);
//		for(String now: ret){
//			System.out.println(now);
//		}
		return ret;
	}
	
	private static Collection<? extends File> handleResourcePack(IResourcePack pack) throws Exception{
		if(pack==null){
			return new HashSet<File>();
		}
		System.out.println(pack.getClass()+": "+pack.getPackName());
		Class clas = pack.getClass();
		while(!clas.equals(Object.class)){
			System.out.println(clas = clas.getSuperclass());
		}
		if(pack instanceof AbstractResourcePack){
			try {
				HashSet<File> files = new HashSet<File>();
				files.add((File) ObfuscationReflectionHelper.getPrivateValue(AbstractResourcePack.class, (AbstractResourcePack)pack, 1));
				return files;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else if(pack instanceof LegacyV2Adapter){
			try {
				IResourcePack newPack = ObfuscationReflectionHelper.getPrivateValue(LegacyV2Adapter.class, (LegacyV2Adapter)pack, 0);
				return handleResourcePack(newPack);	
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else if(pack instanceof DefaultResourcePack){
			try {
				ResourceIndex resourceIndex = ObfuscationReflectionHelper.getPrivateValue(DefaultResourcePack.class, (DefaultResourcePack)pack, 1);
				Map<String, File> resourceMap = ObfuscationReflectionHelper.getPrivateValue(ResourceIndex.class, resourceIndex, 1);
				return resourceMap.values();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else{
			System.out.println(pack.getClass()+"KENNT ER NICHT");
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
				if(file.getName().equalsIgnoreCase("forge-1.12-14.21.1.2443.jar")){
					System.out.println("PRINT__________________________________________");
					printAllChildren(file);
					System.out.println("PRINT DONE__________________________________________");
				}
				System.out.println("HandleFile "+file.getName());
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
	
	private static void printAllChildren(File file){
		if(file.isDirectory()){
			System.out.println(file.getName());
			for(File now: file.listFiles()){
				printAllChildren(now);
			}
		}else{
			if(endsWithIgnoreCase(file.getName(), ".jar") || endsWithIgnoreCase(file.getName(), ".zip")){
				System.out.println("HandlePrinbtFile "+file.getName());
				try {
					ZipFile zipFile = new ZipFile(file);
					Enumeration<? extends ZipEntry> zipEnum = zipFile.entries();
					while(zipEnum.hasMoreElements()){
						ZipEntry zipEntry = zipEnum.nextElement();
						System.out.println(zipEntry.getName());
					}
					zipFile.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}else{
				System.out.println("NowPrintFile: "+file.getName());
				System.out.println(file.getPath());
				System.out.println(file.toURI());
				System.out.println();
			}
		}
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
