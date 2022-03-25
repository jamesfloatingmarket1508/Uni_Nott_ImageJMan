

macro "Minh" {

	dirname = getDirectory("image");
	dataname = getTitle();
	
    print( "dirname is " + dirname);

    print("date name" + dataname);

    pathFileTxt = "/Users/votrannhatminh/Documents/Nottingham/StemCell/300_photo_phantich"+ "all_mean_row" +".txt";

    
    File.append("Lan n" , pathFileTxt);
	 		

}