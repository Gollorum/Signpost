package net.minecraft.client.resources;

import java.io.File;

public class FileStealer {
	
	public static final FileStealer INSTANCE = new FileStealer();
	
	private FileStealer(){}
	
	public File getAbstractFile(AbstractResourcePack pack){
		return pack.resourcePackFile;
	}

}
