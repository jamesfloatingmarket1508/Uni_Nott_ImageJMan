// Macro to auto open, save, close function for Francis' Marco(Modified 2/6/15 FB)
//
// Author: Xie He (hexie (at) outlook.com)
// Created 02/06/15 - Original version
//run("Size...", "width=512 height=512 depth=1000 interpolation=Bilinear");




// James wrote function auto-save for 10 days
showStatus("James wrote this on April");
var parentDir =  getDirectory("Choose parent dicretory");
listFolderRecursively(parentDir);

// James modified March 2022
  function listFolderRecursively(parentDir) {
      list = getFileList(parentDir);
      for (i=0; i<list.length; i++) {
          if (endsWith(list[i], "/"))
              listFolderRecursively(""+parentDir+list[i]);
          else{
            lastDir =  list[i];
            
            print("james" + parentDir +lastDir);
            doAutoSaveJob(parentDir);
            break;
          }
             
      }
  }

 function doAutoSaveJob(subDir) {

      var dir1 = subDir;
      setBatchMode(true);

      run("Image Sequence...", "open=dir1 sort");
      runMacro("Contractility.ijm");
      

     // runMacro("Minh.ijm");


      if ("Dialog"=="Average transient"); {
        close("Average transient");
        }
      if ("Dialog"=="Transient 30"); {
        close("transient 30");
        }
      if ("Dialog"=="Transient 29"); {
        close("transient 29");
        }
      if ("Dialog"=="Transient 28"); {
        close("transient 28");
        }
      if ("Dialog"=="Transient 27"); {
        close("transient 27");
        }
      if ("Dialog"=="Transient 26"); {
        close("transient 26");
        }
      if ("Dialog"=="Transient 25"); {
        close("transient 25");
        }
      if ("Dialog"=="Transient 24"); {
        close("transient 24");
        }
      if ("Dialog"=="Transient 23"); {
        close("transient 23");
        }
      if ("Dialog"=="Transient 22"); {
        close("transient 22");
        }
      if ("Dialog"=="Transient 21"); {
        close("transient 21");
        }
      if ("Dialog"=="Transient 20"); {
        close("transient 20");
        }
      if ("Dialog"=="Transient 19"); {
        close("transient 19");
        }
      if ("Dialog"=="Transient 18"); {
        close("transient 18");
        }
      if ("Dialog"=="Transient 17"); {
        close("transient 17");
        }
      if ("Dialog"=="Transient 16"); {
        close("transient 16");
        }
      if ("Dialog"=="Transient 15"); {
        close("transient 15");
        }
      if ("Dialog"=="Transient 14"); {
        close("transient 14");
        }
      if ("Dialog"=="Transient 13"); {
        close("transient 13");
        }
      if ("Dialog"=="Transient 12"); {
        close("transient 12");
        }
      if ("Dialog"=="Transient 11"); {
        close("transient 11");
        }
      if ("Dialog"=="Transient 10"); {
        close("transient 10");
        }
      if ("Dialog"=="Transient 9"); {
        close("transient 9");
        }
      if ("Dialog"=="Transient 8"); {
        close("transient 8");
        }
      if ("Dialog"=="Transient 7"); {
        close("transient 7");
        }
      if ("Dialog"=="Transient 6"); {
        close("transient 6");
        }
      if ("Dialog"=="Transient 5"); {
        close("transient 5");
        }
      if ("Dialog"=="Transient 4"); {
        close("transient 4");
        }
      if ("Dialog"=="Transient 3"); {
        close("transient 3");
        }
      if ("Dialog"=="Transient 2"); {
        close("transient 2");
        }
      if ("Dialog"=="Transient 1"); {
        close("transient 10");
        }
      if ("Dialog"=="Transient 1"); {
        close("transient 1");
        }
      if ("Dialog"=="Corrected signal"); {
        close("Corrected signal");
        }
      if ("Dialog"=="Signal"); {
        close("Signal");
        }
      if ("Dialog"=="1st derivative smoothed"); {
        close("1st derivative smoothed");
        }
      if ("Dialog"=="Running diff"); {
        close("Running diff");
        }
      if ("Dialog"=="Results"); {
        close("Results");
        }
      if ("Dialog"=="Global diff"); {
        // run("Fire"); 
        //run("AVI... ", "compression=JPEG frame=100");  
        // James wrote this
        // saveAs("AVI", dir1 + "Global diff" + ".avi");                                          
        close("Global diff");
        }
      if ("Dialog"=="Running diff"); {
        close("Running diff");
        }
      if ("Dialog"=="Seq"); {
       // run("AVI... ", "compression=JPEG frame=100");
       // James wrote this
      //saveAs("AVI", dir1 + "Seq" + ".avi"); 
        close("Seq");
        }
      if ("Dialog"=="Log"); {
        close("Log");
        }

      if("Window"=="Result"){
        selectWindow("Results");
        run("Close");
      }  

      if("Window"=="Log"){
        selectWindow("Log");
        run("Close");
      }

}  // end AutoSave job


