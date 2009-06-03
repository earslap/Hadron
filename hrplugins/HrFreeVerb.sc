HrFreeVerb : HadronPlugin
{
	var synthInstance, mixSlider, roomSlider, dampSlider, levSlider;
	
	*new
	{|argParentApp, argIdent, argUniqueID, argExtraArgs, argCanvasXY|
		
		var numIns = 2;
		var numOuts = 2;
		var bounds = Rect((Window.screenBounds.width - 250).rand, rrand(100, Window.screenBounds.height), 250, 100);
		var name = "HrFreeVerb";
		^super.new(argParentApp, name, argIdent, argUniqueID, argExtraArgs, bounds, numIns, numOuts, argCanvasXY).init;
	}
	
	init
	{
		
		window.background_(Color.gray(0.7));
		helpString = "In 1/2 ar audio inputs, Out 1/2 are Dry+Wet outputs.";
		StaticText(window, Rect(10, 10, 50, 20)).string_("Mix:");
		StaticText(window, Rect(10, 30, 50, 20)).string_("Room:");
		StaticText(window, Rect(10, 50, 50, 20)).string_("Damp:");
		StaticText(window, Rect(10, 70, 50, 20)).string_("Level:");
		
		mixSlider = HrSlider(window, Rect(60, 10, 100, 20))
		.action_({|sld| synthInstance.set(\mix, sld.value); }).value_(0.5);
		
		roomSlider = HrSlider(window, Rect(60, 30, 100, 20))
		.action_({|sld| synthInstance.set(\room, sld.value); }).value_(0.5);
		
		dampSlider = HrSlider(window, Rect(60, 50, 100, 20))
		.action_({|sld| synthInstance.set(\damp, sld.value); }).value_(0.5);
		
		levSlider = HrSlider(window, Rect(60, 70, 100, 20))
		.action_({|sld| synthInstance.set(\lev, sld.value); }).value_(1);
		
		
		
		
		fork
		{
			SynthDef("hrFreeVerb"++uniqueID,
			{
				arg outBus0, outBus1, inBus0, inBus1, mix=0.5, room=0.5, damp=0.5, lev=1;
				
				var in0 = InFeedback.ar(inBus0);
				var in1 = InFeedback.ar(inBus1);
				
				var reverbed = FreeVerb2.ar(in0, in1, mix, room, damp) * lev;
				
				Out.ar(outBus0, reverbed[0]);
				Out.ar(outBus1, reverbed[1]);
				
			}).memStore;
			
			Server.default.sync;
			synthInstance = Synth("hrFreeVerb"++uniqueID, 
				[
					\outBus0, outBusses[0], 
					\outBus1, outBusses[1],
					\inBus0, inBusses[0], 
					\inBus1, inBusses[1]
				], group);
			
		};
		
		saveGets = 
		[
			{ mixSlider.value; },
			{ mixSlider.boundMidiArgs; },
			{ roomSlider.value; },
			{ roomSlider.boundMidiArgs; },
			{ dampSlider.value; },
			{ dampSlider.boundMidiArgs; },
			{ levSlider.value; },
			{ levSlider.boundMidiArgs; }
		];
		
		saveSets = 
		[
			{|argg| mixSlider.valueAction_(argg); },
			{|argg| mixSlider.boundMidiArgs_(argg); },
			{|argg| roomSlider.valueAction_(argg); },
			{|argg| roomSlider.boundMidiArgs_(argg); },
			{|argg| dampSlider.valueAction_(argg); },
			{|argg| dampSlider.boundMidiArgs_(argg); },
			{|argg| levSlider.valueAction_(argg); },
			{|argg| levSlider.boundMidiArgs_(argg); }
			
		];
		
		modulatables.put(\mix, {|argg| mixSlider.valueAction_(argg); });
		modulatables.put(\room, {|argg| roomSlider.valueAction_(argg); });
		modulatables.put(\damp, {|argg| dampSlider.valueAction_(argg); });
		modulatables.put(\level, {|argg| levSlider.valueAction_(argg); });
	}
	
	updateBusConnections
	{
		synthInstance.set
		(
			\outBus0, outBusses[0], 
			\outBus1, outBusses[1], 
			\inBus0, inBusses[0], 
			\inBus1, inBusses[1]);
	}
	
	cleanUp
	{
		synthInstance.free;
	}
}