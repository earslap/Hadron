HadronStateSave
{
	var parentApp;
	
	*new
	{|argParentApp|
		
		^super.new.init(argParentApp);
	}
	
	init
	{|argParentApp|
	
		parentApp = argParentApp;
	}
	
	showSaveDialog
	{
		var saveWin, pField, nField, okBut;
		
		saveWin = Window("Path and Name", Rect(300, 300, 220, 100), resizable: false);
		StaticText(saveWin, Rect(10, 10, 50, 20)).string_("Path:");
		StaticText(saveWin, Rect(10, 40, 50, 20)).string_("Name:");
		
		pField = TextField(saveWin, Rect(60, 10, 150, 20)).string_("~/Desktop/");
		nField = TextField(saveWin, Rect(60, 40, 150, 20));
		
		okBut = Button(saveWin, Rect(10, 70, 80, 20))
		.states_([["Ok"]])
		.action_({ this.saveState(pField.string, nField.string); saveWin.close; });
		
		saveWin.front;
	}
	
	saveState
	{|argPath, argName|
		var outFile;
		
		outFile = File(argPath.standardizePath ++ argName, "w");
		outFile.write("?Hadron 1\n");
		outFile.write("?StartPlugs\n");
		
		parentApp.alivePlugs.do
		({|item|
			outFile.write
			(
				item.class.asString
				++ 31.asAscii 
				++ item.ident 
				++ 31.asAscii 
				++ item.uniqueID.asString 
				++ 31.asAscii
				++ item.extraArgs.asString 
				++ 31.asAscii
				++ (item.boundCanvasItem.objView.bounds.left@item.boundCanvasItem.objView.bounds.top).asString
				++ 31.asAscii
				++ item.outerWindow.bounds.asString
				++ 31.asAscii
				++ item.oldWinBounds.asString
				++ 31.asAscii
				++ item.isHidden.asString
				++"\n"
			);
			
			
		});
		
		
		outFile.write("?EndPlugs\n");
		outFile.write("?StartConnections\n");
		
		parentApp.alivePlugs.do
		({|item|
		
			var tempIn, tempOut;
			tempIn = item.inConnections.deepCopy;
			tempOut = item.outConnections.deepCopy;
			
			tempIn.do
			({|inItem, count|
				
				if(inItem[0] != nil,
				{
					tempIn[count][0] = tempIn[count][0].uniqueID;
				});
			});
			
			tempOut.do
			({|outItem, count|
				
				if(outItem[0] != nil,
				{
					tempOut[count][0] = tempOut[count][0].uniqueID;
				});
			});
			
			outFile.write
			(
				item.uniqueID.asString
				++ 31.asAscii
				++ tempIn.asString
				++ 31.asAscii
				++ tempOut.asString
				++ "\n"
			);			
		});
		
		outFile.write("?EndConnections\n");
		
		outFile.write("?StartPlugParams\n");
		
		parentApp.alivePlugs.do
		({|item|
			
			outFile.write(item.uniqueID.asString);
			
			item.giveSaveValues.do
			({|sValue|
				
				outFile.write(31.asAscii);
				outFile.write(sValue.asCompileString);
			});
			outFile.write("\n");
		});
		
		outFile.write("?EndPlugParams\n");
		outFile.write("?EndSave\n");
		
		outFile.close;
	}
	
}