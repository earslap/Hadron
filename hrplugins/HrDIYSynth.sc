HrDIYSynth : HadronPlugin
{
	var <synthInstance, sDef, codeView, wrapFunc, fadeNum, <fadeVal, oldSynth, <argBucket;
	
	*new
	{|argParentApp, argIdent, argUniqueID, argExtraArgs, argCanvasXY|
		
		var numIns = 2;
		var numOuts = 2;
		var bounds = Rect((Window.screenBounds.width - 450).rand, (Window.screenBounds.height - 400).rand, 450, 400);
		var name = "HrDIYSynth";
		^super.new(argParentApp, name, argIdent, argUniqueID, argExtraArgs, bounds, numIns, numOuts, argCanvasXY).init;
	}
	
	init
	{
		window.background_(Color.new255(160, 220, 89));
		helpString = "In1/In2 audio inputs, given as args to function. You must return 2 channels of audio inside function.";
		fadeVal = 0.1;
		argBucket = Dictionary.new;
		
		StaticText(window, Rect(100, 370, 40, 20)).string_("Fade:");
		
		fadeNum = NumberBox(window, Rect(145, 370, 30, 20)).value_(0.1).action_({|num| fadeVal = num.value; });
		
		Button(window, Rect(10, 370, 80, 20)).states_([["Evaluate"]])
		.action_
		({
			this.redefineSynth;
		});
		
		{
			codeView = TextView(window, Rect(10, 10, 430, 350))
			.string_("{\n\targ input;\n\tinput;\n}")
			.usesTabToFocusNextView_(false)
			.enterInterpretsSelection_(false)
			.editable_(true)
			.keyDownAction_
			({|...args| 
				
				if((args[2] == 262401) and: { args[3] == 3 }, { this.redefineSynth; }); //control + c evaluates
				if((args[2] == 262401) and: { args[3] == 22 }, { this.dumpArgs; }); //control + v dumps args
				
				if(args[3]==13 or:(args[3]==32) or: (args[3]==3) or: (args[3]==46),
				{ 
					args[0].syntaxColorize;
				});
			}); 
			
			if(GUI.id == \cocoa, { codeView.font_(CocoaDocument.defaultFont); });
			
			if(GUI.id == \swing, { SwingOSC.default.sync; });
			
			this.redefineSynth;
		}.fork(AppClock);
		
		saveGets =
			[
				{ codeView.string.replace("\n", 30.asAscii); },
				{ fadeVal; }
			];
			
		saveSets =
			[
				{|argg| codeView.string_(argg.replace(30.asAscii.asString, "\n")); },
				{|argg| { fadeNum.valueAction_(argg); }.defer; }
			]
	
	}
	
	redefineSynth
	{
		
		{
			
			codeView.background_(Color.rand);
			codeView.syntaxColorize;
			0.1.wait;
			codeView.background_(Color.white);
		}.fork(AppClock);
		
		argBucket = Dictionary.new;
		
		sDef = 
		SynthDef("hrDIYSynth"++uniqueID,
		{
			arg inBus0, inBus1, outBus0, outBus1, t_releaseTrig, relTime = 0, initTime = 0;
			var inputs = [InFeedback.ar(inBus0), InFeedback.ar(inBus1)];
			
			var initEnv = EnvGen.ar(Env([1, 2], [initTime], \exponential), 1) - 1;
			var releaseEnv = EnvGen.ar(Env([2, 1], [relTime], \exponential), t_releaseTrig, doneAction: 2) - 1;
			var sound = SynthDef.wrap(codeView.string.interpret, [0], [inputs]);
						
			Out.ar(outBus0, sound[0] * releaseEnv * initEnv);
			Out.ar(outBus1, sound[1] * releaseEnv * initEnv);
		});
		
		fork
		{
					
			sDef.memStore;
			
			Server.default.sync;
			
			//oldSynth = synthInstance;
			synthInstance.set(\relTime, fadeVal);
			synthInstance.set(\t_releaseTrig, 1);
			
			synthInstance = 
			Synth("hrDIYSynth"++uniqueID, 
				[
					\inBus0, inBusses[0], 
					\inBus1, inBusses[1],
					\outBus0, outBusses[0],
					\outBus1, outBusses[1],
					\initTime, fadeVal
				], target: group);
		};
	}
	
	set
	{|...args|
		
		if(args.size.odd,
		{
			parentApp.displayStatus("HrDIYSynth.set needs even number of arguments", -1);
		},
		{
			synthInstance.set(*args);
			args.clump(2).do
			({|item|
				
				argBucket.put(item[0], item[1]);
			});
		});
		
		^synthInstance;
	}
	
	dumpArgs
	{
		"Current args for %:".format(uniqueID).postln;
		argBucket.keys.do
		({|key|
		
			"%: %".format(key, argBucket.at(key)).postln;
		});
		"--".postln;
	}
	
	wakeFromLoad
	{
		{
			if(GUI.id == \swing, { SwingOSC.default.sync; });
			this.redefineSynth;
		}.fork(AppClock);
	}
	
	updateBusConnections
	{
		synthInstance.set(\inBus1, inBusses[0], \inBus2, inBusses[1], \outBus0, outBusses[0], \outBus1, outBusses[1]);
	}
	
	cleanUp
	{
		synthInstance.free;
	}
}