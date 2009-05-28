HadronConManager
{
	var areArgsGood;
	
	*new
	{|argSourcePlug, argTargetPlug|
	
		^super.new.init(argSourcePlug, argTargetPlug);
	}
	
	init
	{|argSourcePlug, argTargetPlug|
	
		var win = Window("Comma separated \"in,out\" pairs:", Rect(400, 400, 300, 50));
		var field = TextField(win, Rect(10, 20, 280, 20))
		.action_
		({|fld|
			
			var pairs; 
			
			if(fld.value.size != 0,
			{			
				pairs = fld.value.replace(" ", "").split($,).collect(_.asInteger).clump(2);
				//check for bounds
				pairs.do
				({|item|
					//no need to subtract values for .size, they are already +1
					if(item[1] > argTargetPlug.inBusses.size or: { item[1] < 0 }, 
					{
						areArgsGood = false;
					});
					
					if(item[0] > argSourcePlug.outBusses.size or: { item[0] < 1 },
					{
						areArgsGood = false;
					});
					
				});
				
				if(areArgsGood,
				{
					pairs.do
					({|item|
					
						if(item[1] == 0,
						{
							argSourcePlug.setOutBus(nil, nil, item[0] - 1); //setOutBus knows how to handle this.
						},
						{
							argSourcePlug.setOutBus(argTargetPlug, item[1] - 1, item[0] - 1);
						});
						
					});
					win.close;
					argSourcePlug.parentApp.alivePlugs.do({|plug| if(plug.conWindow.isClosed.not, { plug.refreshConWindow; }) });
					argSourcePlug.parentApp.displayStatus("Right click on canvas to add plugins. Shift+click on a plugin to make connections");
				},
				{
					argSourcePlug.parentApp.displayStatus("One of the indexes are out of bounds...");
					areArgsGood = true;
				});
			});
			
		})
		.focus(true);
		
		areArgsGood = true; //ahh... good intentions...
		
		argSourcePlug.parentApp.displayStatus("Enter input output pairs, comma separated. Ex: 1,1,2,2");
		win.front;		
	}
}