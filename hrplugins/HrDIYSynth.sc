HrDIYSynth : HadronPlugin
{
	var <synthInstance, sDef, codeView, wrapFunc, fadeNum, <fadeVal, oldSynth, <argBucket,
		<curNamesDefs, <cndIndexes, contSurf;
	
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
		curNamesDefs = [];
		cndIndexes = Dictionary.new;
		contSurf = DIYParamGui.new(this);
		
		StaticText(window, Rect(100, 370, 40, 20)).string_("Fade:");
		
		fadeNum = NumberBox(window, Rect(145, 370, 30, 20)).value_(0.1).action_({|num| fadeVal = num.value; });
		
		Button(window, Rect(360, 370, 80, 20)).states_([["S/H Controls"]])
			.action_
			({
				if(contSurf.win.visible == true, { contSurf.win.visible_(false); }, { contSurf.win.visible_(true); });
			});
		
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
			this.parseParams;
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
				] ++ curNamesDefs, target: group);
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
	
	parseParams
	{
		var ctlNamesDefs;
		var tInd;
		
		ctlNamesDefs = 
			sDef.allControlNames.reject
			({|item| 
				
				['inBus0', 'inBus1', 'outBus0', 'outBus1', 't_releaseTrig', 'relTime', 'initTime'].includes(item.name.asSymbol);
			}).collect
			({|item| 
				
				[item.name.asSymbol, item.defaultValue]; 
			});

		if(contSurf.doesRule,
		{
			ctlNamesDefs.do
			({|item, cnt|
				
				item = item[0];
				tInd = curNamesDefs.indexOf(item);
				if(tInd.notNil,
				{
					ctlNamesDefs[cnt][1] = curNamesDefs[tInd+1];
				});
			});
		});
		
		curNamesDefs = ctlNamesDefs.flat;
		
		cndIndexes = Dictionary.new;
		
		curNamesDefs.do
		({|item, cnt|
			
			if(item.species == Symbol,
			{
				cndIndexes.put(item, cnt);
			});
		});
		
		contSurf.buildArgs;
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
		contSurf.win.close;
	}
}

DIYParamGui
{
	var <win, <doesRule, <parentPlug, ctlItems, <scrView;
	
	*new
	{|argParentPlug|
		
		^super.new.init(argParentPlug);
	}
	
	init
	{|argParentPlug|
		
		parentPlug = argParentPlug;
		ctlItems = List.new;
		doesRule = false;

		win = Window("DIYCtl" + parentPlug.ident, 
			Rect
			(
				parentPlug.outerWindow.view.bounds.left + parentPlug.outerWindow.view.bounds.width + 5, 
				parentPlug.outerWindow.view.bounds.top + parentPlug.outerWindow.view.bounds.height - 65,
				250,
				300
			), false).userCanClose_(false);
			
		Button(win, Rect(10, 10, 230, 20))
			.states_
			([
				["Keep Settings on Re-Eval"],
				["Keep Settings on Re-Eval", Color.black, Color(0.5, 0.7, 0.5)]
			])
			.value_(0)
			.action_
			({|btn|
			
				doesRule = (btn.value == 1);
			});
		
		scrView = ScrollView(win, Rect(10, 30, 230, 260))
			.background_(Color.gray(0.3))
			.hasHorizontalScroller_(false);
		
		win.front;
		win.visible_(false);
	}
	
	buildArgs
	{
		var tempNew = parentPlug.curNamesDefs.clump(2).flop[0];
		var toRemove = List.new;
		var tempNames;
		
		if(doesRule,
		{
			tempNames = ctlItems.collect({|item| item.param; });
			ctlItems.do
			({|item, cnt|
				
				if(tempNew.includes(item.param).not, 
				{
					toRemove.add(cnt);
				});
			});
			
			toRemove = toRemove.reverse;
		},
		{
			tempNames  = List.new;
			toRemove = ({|c|c;} ! ctlItems.size).reverse;
		});
		
		{
			
			toRemove.do({|index| ctlItems[index].view.remove; ctlItems.removeAt(index); });
			
			tempNew.do
			({|item|
			
				if(tempNames.includes(item).not,
				{
					ctlItems.add(DIYPGitem.new(item, this));
				});
			});
			
			
			ctlItems.do
			({|item, cnt|
			
				item.view.bounds_(Rect(0, (cnt * 48) + 3, 230, 45));
				item.view.visible_(true);
			});
			
			win.refresh;
		}.defer;
	}
}

DIYPGitem
{
	var <view, <spec, <param, parent, slider, valNumBox, minNum, maxNum, curveMenu, expCurveNum;
	
	*new
	{|argParam, argParent|
		
		^super.new.init(argParam, argParent);
	}
	
	init
	{|argParam, argParent|
	
		var maxVal;
		
		param = argParam;
		parent = argParent;
		maxVal = parent.parentPlug.curNamesDefs[parent.parentPlug.curNamesDefs.indexOf(param) + 1];
		spec = [0, maxVal, \linear, 0, maxVal].asSpec;
		
		view = CompositeView(parent.scrView, Rect(0, 0, 230, 45)).background_(Color.gray(0.8)).visible_(false);
		StaticText(view, Rect(0, 0, 80, 20)).string_(param.asString);
		slider = HrSlider(view, Rect(80, 0, 100, 20))
			.value_(spec.unmap(maxVal))
			.action_
			({|sld|
			
				valNumBox.valueAction_(spec.map(sld.value));
			});
		
		valNumBox = NumberBox(view, Rect(180, 0, 50, 20))
			.value_(maxVal)
			.action_
			({|nmb|
			
				slider.valueAction_(spec.unmap(nmb.value));
				parent.parentPlug.synthInstance.set(param, nmb.value);
				parent.parentPlug.curNamesDefs[parent.parentPlug.cndIndexes.at(param) + 1] = nmb.value;
			});
		
		minNum = NumberBox(view, Rect(0, 25, 38, 20)).value_(spec.minval)
			.action_
			({|nmb|
				
				spec.minval = nmb.value;
				slider.valueAction_(spec.unmap(valNumBox.value));
			});
		maxNum = NumberBox(view, Rect(42, 25, 38, 20)).value_(spec.maxval)
			.action_
			({|nmb|
				
				spec.maxval = nmb.value;
				slider.valueAction_(spec.unmap(valNumBox.value));
			});
		
		curveMenu = PopUpMenu(view, Rect(80, 25, 100, 20))
			.items_(["Linear", "Exponential", "Sine", "Cosine", "Fader", "dB"])
			.action_
			({|mnu|
			
				spec = [spec.minval, spec.maxval, [\lin, expCurveNum.value, \sin, \cos, \amp, \db][mnu.value], 0, valNumBox.value].asSpec;
				if(mnu.value == 1,
				{
					expCurveNum.enabled_(true);
				},
				{
					expCurveNum.enabled_(false);
				});
				slider.valueAction_(spec.unmap(valNumBox.value));
			});
			
		expCurveNum = NumberBox(view, Rect(185, 25, 25, 20)).enabled_(false).value_(10)
			.action_
			({|nmb|
			
				spec = [spec.minval, spec.maxval, nmb.value, 0, valNumBox.value].asSpec;
				slider.valueAction_(spec.unmap(valNumBox.value));
			});
		
	}
}