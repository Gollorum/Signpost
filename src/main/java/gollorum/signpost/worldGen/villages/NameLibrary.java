package gollorum.signpost.worldGen.villages;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.IOUtils;

import gollorum.signpost.Signpost;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.util.code.MinecraftIndependent;

@MinecraftIndependent
public class NameLibrary {
	
	private static NameLibrary INSTANCE;
	
	private static final int MAX_TRIES = 50;
	
	public static NameLibrary getInstance(){
		return INSTANCE;
	}

	private final List<String> first;
	private final List<String> second;
	private final List<String> third;
	
	public static void init(String configFolder){
		try{
			INSTANCE = new NameLibrary(configFolder);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private NameLibrary(String configFolder){
		assureFileExists(configFolder, "first");
		this.first = readNameParts(configFolder, "first");
		assureFileExists(configFolder, "second");
		this.second = readNameParts(configFolder, "second");
		assureFileExists(configFolder, "third");
		this.third = readNameParts(configFolder, "third");
	}
	
	private File getFile(String folder, String index) {
		return new File(folder, "villagenames"+index+".txt");
	}

	private void assureFileExists(String configFolder, String index) {
		try {
			if(!getFile(configFolder, index).exists()){
				copyFile(configFolder, index);
			}
		} catch (IOException e) {e.printStackTrace();}
	}

	private void copyFile(String configFolder, String index) throws IOException {
		InputStream in;
		in = Signpost.proxy.getResourceInputStream(Signpost.MODID+":worldgen/villagenames"+index+".txt");
		if(in == null){
			in = Signpost.proxy.getResourceInputStream("/assets/signpost/worldgen/villagenames"+index+".txt");
		}
		OutputStream out = new FileOutputStream(getFile(configFolder, index));
		IOUtils.copy(in, out);
		in.close();
		out.close();
	}

	private List<String> readNameParts(String configFolder, String index) {
		List<String> possibleNames = new LinkedList<String>();
		try{
			possibleNames.addAll(Files.readAllLines(getFile(configFolder, index).toPath()));
		}catch(IOException e){
			e.printStackTrace();
		}
		return possibleNames;
	}
	
	public boolean namesLeft(){
		for(int i=0; i<first.size(); i++) {
			for(int j=0; j<second.size(); j++) {
				for(int k=0; k<third.size(); k++) {
					if(!PostHandler.getNativeWaystones().nameTaken(getName(i, j, k))) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public String getName(Random random){
		for(int i=0; i<MAX_TRIES; i++) {
			String name = getName(random.nextInt(first.size()),
						random.nextInt(second.size()),
						random.nextInt(third.size()));
			if(!PostHandler.getNativeWaystones().nameTaken(name)){
				return name;
			}
		}
		return null;
	}
	
	private String getName(int i, int j, int k) {
		String a = first.get(i);
		String b = second.get(j);
		String c = third.get(k);
		if(a.endsWith(" ")) {
			b = b.substring(0, 1).toUpperCase() + b.substring(1);
		}
		if(b.endsWith(" ")) {
			c = c.substring(0, 1).toUpperCase() + c.substring(1);
		}
		return a+b+c;
	}
	
}
