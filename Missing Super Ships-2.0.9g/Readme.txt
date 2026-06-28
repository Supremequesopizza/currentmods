Weapon Platform Spawning:
Adjustable under /data/config/missp_super_config.json

You should only set enable to true or false, you are on your own if you change anything else.

If you want to play around with the other stuff the faction files still contain spawn rates and the default ship roles also exist
The mod defaults to deactivated recoverable and the idol in the roles. The last two are not configurable at this point.

The frequency and priority has been tested at 400 DP. This can be left deactivated to not spawn the supers it can be activated to make 
the supers spawn even at a later date and while the player already has the blueprints or supers of their own. The supers cant as of yet be deactivated
again and deactivating the supers also isnt a viable way to make the mod removeable.


Linked Weapons Targeting:
Adjustable under /data/config/missp_super_settings.json

The standard Keybinds are K to vent the targeted Module F5 to turn the module info screen a page to the left and F6 to turn a page to the right. 
If required crtl and alt can be activate/deactivated by using true and false.
Weapons aim and fire at the main hulls targets  if it is within the firing arc. Vent those modules that missbehave.

Known quirks:
-At higher flux the ships might deprioritize the player (or npc target) flux needs to be lowered to stop that from happening
-sometimes the module uses its shipsystem over and over can be stopped pressing x for cease fire. 
Should be completly fixed but if it still happends fix still applies
-sometimes the zero flux bonus wont apply even though it should. I managed to get out of that situation by pressing y twice. 
If hangars are present press y twice, or trice if that then puts the fighters into regroup.