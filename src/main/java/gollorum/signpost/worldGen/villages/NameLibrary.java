package gollorum.signpost.worldGen.villages;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.IOUtils;

import gollorum.signpost.management.PostHandler;
import gollorum.signpost.util.BaseInfo;

public class NameLibrary {
	
	private static NameLibrary INSTANCE;
	
	public static NameLibrary getInstance(){
		return INSTANCE;
	}
	
	private final List<String> possibleNames;
	
	public static void init(File file){
		try{
			INSTANCE = new NameLibrary(file);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private NameLibrary(File file){
		assureFileExists(file);
		this.possibleNames = readPossibleNames(file);
	}

	private void assureFileExists(File file) {
		try {
			if(!file.exists()){
				copyFile(file);
			}
		} catch (IOException e) {e.printStackTrace();}
	}

	private void copyFile(File file) throws IOException {
		InputStream in = getClass().getResourceAsStream("villageNames.txt");
		OutputStream out = new FileOutputStream(file);
		IOUtils.copy(in, out);
		in.close();
		out.close();
	}

	public List<String> readPossibleNames(File file) {
		List<String> possibleNames = new LinkedList<String>();
		try{
			possibleNames.addAll(Files.readAllLines(file.toPath()));
		}catch(IOException e){
			e.printStackTrace();
		}
		return possibleNames;
	}
	
	public boolean namesLeft(){
		return !getPossibleNames().isEmpty();
	}
	
	public String getName(){
		if(namesLeft()){
			List<String> possibles = getPossibleNames();
			return possibles.get(new Random().nextInt(possibles.size()));
		}else{
			return "null";
		}
	}
	
	private List<String> getPossibleNames(){
		LinkedList<String> possibles = new LinkedList<String>();
		possibles.addAll(possibleNames);
		for(BaseInfo now: PostHandler.getNativeWaystones()){
			possibles.remove(now.getName());
		}
		return possibles;
	}
}
