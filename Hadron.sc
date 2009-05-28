Hadron
{
	classvar <>plugins, <>loadDelay = 2;
	var win, <alivePlugs, <blackholeBus, <aliveMenu, <idPlugDict, <canvasObj,
	statusView, statusStString;
	
	
	*initClass
	{
		this.plugins = List.new;
		this.plugins = 
		[
			HrADC,
			HrDAC,
			HrFreeVerb,
			HrStereoMixer,
			HrStereoSplitter,
			HrSimpleModulator,
			HrDIYSynth		
		].asList;
	}
	
	*new
	{
		^super.new.init;
	}
	
	init
	{
		
		Server.default.waitForBoot
		{
			
			alivePlugs = List.new;
			idPlugDict = Dictionary.new;
			
			canvasObj = HadronCanvas.new(this);
			
			blackholeBus = Bus.audio(Server.default, 1);
			win = Window("Hadron", Rect(100, 100, 600, 70), resizable: false).userCanClose_(false);
			Button(win, Rect(10, 15, 85, 20))
			.states_
			([
				["New Inst.", Color.black, Color(0.5, 0.7, 0.5)], 
			])
			.action_
			({
				this.prShowNewInstDialog;
			});
			
			
			aliveMenu = PopUpMenu(win, Rect(100, 15, 200, 20))
			.action_({|menu| alivePlugs[menu.value].showWindow; });
			
			Button(win, Rect(310, 15, 50, 20))
			.states_([["Save"]])
			.action_({ this.prShowSave; });
			
			Button(win, Rect(370, 15, 50, 20))
			.states_([["Load"]])
			.action_({ this.prShowLoad; });
			
			Button(win, Rect(430, 15, 50, 20))
			.states_([["Exit"]])
			.action_
			({
				var tempWin; //can be modal but meh. does SwingOSC have it?
				tempWin = Window("Are you Sure?", Rect(400, 400, 190, 70), resizable: false);
				StaticText(tempWin, Rect(0, 10, 190, 20)).string_("Are you Sure?").align_(\center);
				Button(tempWin, Rect(10, 30, 80, 20)).states_([["Ok"]]).action_({ tempWin.close; this.graceExit; });
				Button(tempWin, Rect(100, 30, 80, 20)).states_([["Cancel"]]).action_({ tempWin.close; });
			
				tempWin.front;
			});
			
			Button(win, Rect(490, 15, 85, 20))
			.states_
			([
				["Show Canvas", Color.black, Color(0.5, 0.7, 0.5)], 
				["Hide Canvas", Color.white, Color(0.7, 0.5, 0.5)]
			])
			.action_
			({
				arg state;
				state.value.switch
				(
					1, { canvasObj.showWin; this.displayStatus("Right click on canvas to add plugins. Shift+click on a plugin to make connections")},
					0, { canvasObj.hideWin; this.displayStatus("READY.")}
				);
			});
			
			statusView = CompositeView(win, Rect(0, win.view.bounds.height - 20, win.view.bounds.width, 18)).background_(Color.gray(0.8));
			statusStString = StaticText(statusView, Rect(10, 2, win.view.bounds.width, 15)).string_("READY.");
			
			
			win.front;
		}
	}
	
	prShowSave
	{
		HadronStateSave(this).showSaveDialog;
	}
	
	prShowLoad
	{
		HadronStateLoad(this).showLoad;
	}
	
	prGiveUniqueId
	{
		var tempRand = 65536.rand;
		var tempPool = alivePlugs.collect({|item| item.uniqueID; });
		
		while({ tempPool.detectIndex({|item| item == tempRand; }).notNil },
		{
			tempRand = 65536.rand;
		});
		
		^tempRand;
	}
	
	prAddPlugin
	{|argPlug, argIdent, argUniqueID, extraArgs, argCanvasXY|
	
		var tempHolder = argPlug.new(this, argIdent, argUniqueID, extraArgs, argCanvasXY);
		alivePlugs.add(tempHolder);
		idPlugDict.put(tempHolder.uniqueID, tempHolder);
		this.prActiveMenuUpdate;
		alivePlugs.do(_.notifyPlugAdd(tempHolder));
	}
	
	prShowNewInstDialog
	{
		var tempWin = Window("Select Instrument", Rect(200, 200, 200, 130), resizable: false);
		var tempMenu = PopUpMenu(tempWin, Rect(10, 10, 180, 20))
		.items_(Hadron.plugins.collect({|item| item.asString; }));
		
		var tempIdent = TextField(tempWin, Rect(90, 40, 80, 20));
		var tempArgs = TextField(tempWin, Rect(90, 70, 80, 20));
		
		StaticText(tempWin, Rect(10, 40, 80, 20)).string_("Ident name:");
		StaticText(tempWin, Rect(10, 70, 80, 20)).string_("Extra Args:");
		
		Button(tempWin, Rect(10, 100, 80, 20))
		.states_([["Ok"]])
		.action_
		({
			this.prAddPlugin
			(
				tempMenu.items.at(tempMenu.value).interpret, 
				if(tempIdent.string == "", { "unnamed"; }, { tempIdent.string; }),
				nil,
				if(tempArgs.string == "", { nil; }, { tempArgs.string.split($ ); }),
				100@100
			);
			tempWin.close;
		});
		
		Button(tempWin, Rect(110, 100, 80, 20))
		.states_([["Cancel"]])
		.action_
		({
			tempWin.close;
		});
		
		tempWin.front;
	}
	
	prActiveMenuUpdate
	{
		aliveMenu.items = alivePlugs.collect({|item| item.class.asString + item.ident; });
	}
	
	displayStatus
	{|argString|
	
		statusStString.remove;
		statusStString = StaticText(statusView, Rect(10, 2, win.view.bounds.width, 15)).string_(argString);
	}
	
	graceExit
	{
		canvasObj.cWin.close;
		alivePlugs.size.do({ alivePlugs[0].selfDestruct; });
		win.close;
	}
}