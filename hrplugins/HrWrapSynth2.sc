HrWrapSynth2 : HadronPlugin
{
	var <defName, synthBusArgs;
	
	
	*initClass
	{
		this.addHadronPlugin;
	}
	
	*new
	{|argParentApp, argIdent, argUniqueID, argExtraArgs, argCanvasXY|
			
		var numIns, numOuts, bounds, name = "HrWrapSynth2", numControls;
		
		if(argExtraArgs.size == 0, 
		{ 
			argParentApp.displayStatus("This plugin requires an argument. See HrWrapSynth2 help.", -1);
			this.halt; 
		});
		
		if(SynthDescLib.global.synthDescs.at(argExtraArgs[0].asSymbol) == nil,
		{
			argParentApp.displayStatus("SynthDef"+argExtraArgs[0]+"not found in global SynthDescLib. See HrWrapSynth2 help.", -1);
			this.halt; 
		});
		
		numControls = SynthDescLib.global.synthDescs.at(argExtraArgs[0].asSymbol).controlNames.reject
			({|item| 
				
				(item.find("inBus", true, 0) == 0).or(item.find("outBus", true, 0) == 0)
			}).size;

		
		numIns = SynthDescLib.global.synthDescs.at(argExtraArgs[0].asSymbol).controlNames.select({|item| item.find("inBus", true, 0) == 0; }).size;
		numOuts = SynthDescLib.global.synthDescs.at(argExtraArgs[0].asSymbol).controlNames.select({|item| item.find("outBus", true, 0) == 0; }).size;
		
		bounds = Rect(400, 400, 350, 30);
		
		^super.new(argParentApp, name, argIdent, argUniqueID, argExtraArgs, bounds, numIns, numOuts, argCanvasXY).init(argExtraArgs[0].asSymbol);
	}
	
	init
	{|argDefName|
		
		helpString = "This plugin reads a SynthDef from SynthDescLib.default and integrates it with the Hadron system.";
		
		defName = argDefName;
				
		synthBusArgs = 
		{
			inBusses.collect({|item, cnt| [("inBus"++cnt).asSymbol, inBusses[cnt]] }).flatten ++
			outBusses.collect({|item, cnt| [("outBus"++cnt).asSymbol, outBusses[cnt]] }).flatten;
		};
	}
	
	updateBusConnections
	{
		group.set(*synthBusArgs.value);
	}
	
	cleanUp
	{
	}
	
	playSynth
	{|...args|
		
		^Synth(defName, args ++ synthBusArgs.value, target: group);
	}
}