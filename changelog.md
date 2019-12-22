### Open

### Release 0.31: Date 31/03/17
* Serverfarm is now fetched from jManagerServer after each restart.
* Removed LogWriter module. Added sl4j logging with rolling files. Converted the log files to csv.
* Updated connection details for Oracle.

### Release 0.30: Date 04/03/17
* Unified the various immutable strategy parameters under the StrategyParameters class to cut down on the number of Map<Integer, parameter> strategyIdToSomeParameter map being passed around.
* TradeExecutor now keeps track of the number of strategy/symbol trades per minute (of the hour), if that number is above a threshold, it triggers a zabbix alert and disables the trading for that strategy.
* ZabbixHandler now exists and runs along the main thread, outside of the loop that cleans up and reinitializes the various modules.
* Adjusted the zabbix discovery template on testing to have the critical frequency alert based on the sentinel value -1 sent by the program instead of a counter of events, to avoid issues with timing differences preventing notification
* The ServerFarm is now fetched by calling jManagerServer instead of fetching it directly from the DB.
* Performance improvement: Identity lp to symbol name mappings will no longer result in new Trade objects being created.
* Fixed a bug where the wrong character set was used when converting a comment string to and from a byte array, sometimes resulting in size validation exceptions.
* Randomized the order that strategies accumulate and respond to a message, for each message, to ensure regulatory compliance of fairness.
* When setting the buildMode to debug, the system will now target the oracledev db.

### Release 0.29: Date 07/01/17
* Implemented the single trade volume safeguard.
* Moved all the run parameters to the configuration file.

### Release 0.28: Date 27/11/17
* Fixed a bug where close by messages were being lost due to referencing 0 volume.

### Release 0.27: Date 26/11/17
* TODO: Alert safeguards description here

### Release 0.26: Date 19/11/17
* Currency conversions are initialized with a tick request on startup, this fixes a bug where there was a chance for the AntiCoverage exposures to become NaN permanently

### Release 0.25: Date 29/10/17
* Fixed a bug were the an exception in jManagerServer could cause the TradeExecutor not to de-accumulate the failed trade request.
* Fixed a bug were there was a chance that the anticoverage currency exposure values could become corrupted if a simulated price was accumulated before the proper rate was obtained.

### Release 0.24: Date 22/10/17
* Replaced the mechanism that used to lock the trade execution for a specific symbol, until the onTradeOpen message for the latest ordered trade reached ExposureCollector.
    The current mechanism accumulates each AntiCoverage trade immediately once the request is made (using a simulated open price based on a recent tick).
    Trades are marked with a specific comment. Pending trades are kept on a structure. 
    When the TradeOpen message loops back to the system, the exposure is adjusted to reflect the actual open price to maintain correctness in the CurrencyExposures. 
    If a trade request ever fails on the TradeExecutor it's exposure will be de-accumulated immediately.

### Release 0.23: Date 25/09/17 
* TradeExecutor now uses JManagerServerApiModule for the open order web service calls.
* strategyId is now a repeated run parameter. 
* ExposureSender
	Now maintains a separate connection(channel) per strategy.
	Added a clean up method that releases resources gracefully on exit.
* HeartbeatSender
	Now sends a message for each loaded strategy
* StrategyRiskGroupLoader
	Execution parameters are now loaded per strategy, adjusted the queries and types accordingly.
* TradeExecutor
	Now holds a mapping of strategyId to it's execution parameters and the messages that it gets from ExposureCollector contain a strategyId so it can lookup the target account.
* Added RiskGroupWeightInfo to factor away a Tuple in StrategyRiskGroupLoader.
* TradeExposureInfo now holds a List<TradeExposureOnStrategy> instead of a TradeExposure.
* MessageProcessor
	Trade messages now produce List<StrategyAffectingRiskGroup>, the risk group that they belong to for each strategy that they are visible to.
	On the same note, the MessageProcessors and the ExposureCollector communicate via TradeInfoWithRiskGroup objects that now contain 
		a List<TradeExposureOnStrategy>instead of a RiskGroupWithExpFactor.
* ExposureCollector
	Added a cleanup method that releases resources on exit.
	Now holds an ExposureBundle per strategy and updates all the affected ones for each message received
* Moved methods away from the main JFund class and into utility classes.
* Renamed TradeInfoWithRiskGroup -> TradeInfoWithStrategyExposures for clarity to better reflect the current members.
* Fetch the RiskGroups from the DB only once in StrategyRiskGroupLoader#loadRiskGroupsWithExposureFactor, not once per strategy if they are shared.
* MessageProcessor#processTradeUpdate will no longer send adjustments for 0 value forward.
* MessageProcessor#processAllOpenTrades will no longer send known ignored trades for reprocessing.

### Release 0.22: Date 27/08/17
* Starting the project in RELEASE mode will now result in it using the production jManagerServer to open trades, while starting it in DEBUG mode will use wildflydev.
* DEBUG strategies will now use accounts on Mirror8 instead of the jFund test MT4 server. The jFund test MT4 server (300002) will be decommissioned.

### Release 0.21: Date 18/07/17
* Added support for multiple LPs. Each tradingAccount is bound to a specific symbolMapping.
* Implemented a mechanism so that instances can run as windows services.
* Removed the sendExposuresToRabbitMQ run parameter and logic, as it has never been used / been useful.
* Added a new module 'ExposureSender' that sends exposures to RabbitMQ. This offloads ExposureCollector a bit, but more importantly is protects it from having to handle the RabbitMQ reconnection process.
* Added a new module 'LogWriter' that writes messages to logfiles.
* The MessageProcessors are now piped to the ExposureCollector using a BlockingDeque in order to support putting AntiCoverage messages on the front.
* Deprecated the -sendExposures run parameter as it was never really used by anyone.
* The Decision logs will now also contain the time that the TradeExecutor thread called jManagerServer.

### Release 0.20: Date 11/06/17
* Now correctly terminates the CurrencyConversionPump thread after each restart.

### Release 0.19: Date 02/04/17
* Adjusted the project to work with the new populateSymbolConversionFactors() and populateCurrencyConversionFactors() methods.
* Adjusted the notification list.

### Release Candidate 0.18:
* Added a notification level mechanism - restarts will now only send E-mails, not SMS notifications.

### Release 0.17: Date 17/03/17 
* Fixed a bug where sendExposuresToTheEther() catch block contained IOException and not Throwable, causing other exceptions to inadvertently escape.
* sendExposuresToTheEther() will now properly cleanUp and try to reconnect to RabbitMQ upon failure.
* Factored the decision logic away from ExposureCollector to the DecisionController interface.
* Class files containing classes that implement DecisionController can now be optionally loaded using the -dc run parameter, there is a DefaultController if nothing is given.
* Merge cleanups and various refactorings
* Updated the notification list.

### Release Candidate 0.16:
* Added a new component: HeartbeatSender that's controlled by a single thread executor and continuously sends the strategyId to a Zabbix trapper (different one for Debug and Release). To be expanded in the future.
* ExposuresCollector will now write in the strategy's DecisionLog every time it orders a trade to be executed. Decision logs are stored on: C:\XMProjects\jFund\DecisionLogs\{BuildMode}\

### Release Candidate 0.15:
* TradeExecutor will now immediately release the lock on a symbol upon execution failure, instead of trying the same execution multiple times.
* exposuresExceedIndicatorThreshold() now does a >= comparison to handle the edge case where both the N and the indicator are 0 gracefully.
* notifyAdmins() will now also send e-mails to dealers@xm.com and sms messages to Michalis, Aris, Chrysanthos and Neophytos.
* Changes to work with updated versions of internal libraries.

### Release Candidate 0.14:
* ExposureCollector now has an additional indicator formula check that must pass before comparing client and anticoverage exposures.
  This formula relies on raw exposures, in order to get them, the strategy and group weights are reversed.
  To preserve correctness, jFund will throw an exception and exit if the target strategy contains client risk groups with varying weights.
* The volume threshold logic was moved from ExposureCollector to MessageProcessor and it now applies to raw trade volumes, before any weight is applied to them. 
* The log files will be written in a different directory depending on the buildMode.
* jFund will exit with an exception if a strategy targeting a real server is used with a debug buildMode.
* When started with a debug buildMode jFund will contain the test MT4 server in it's connection pool.

### Release 0.13: Date 20/01/17
* Added shortened versions of the run parameters.
* Added a new repeatable run parameter -removeServer that allows the caller to exclude serverIds from the connection pool.
* The git / staging / release version will no longer connect to the test MT4 server 300002.

### Release 0.12: Date 17/01/17
* TradeExecutor now communicates with a dedicated instance of jManagerServer on the jFundVM.
* jFund now only uses manager 391 to connect to every server - Removed the location management logic.
* Fixed a bug where the exposure collector would lock a symbol by the requested name, and TradeExecutor would try to unlock it by the target (LP) name.
* The Mapping from symbolName to LpSymbolName in the DB now includes a serverId, 
* Refined the notificationUtils, added functionality to send e-mail notifications on critical exceptions. 
* jFund will now send an e-mail notification on every restart - this is rate capped at most 1 per minute to avoid flooding.
* (internal) Added dedicated SymbolCache and UserCache objects, now that the global references on jAnalystUtils are no longer valid.
* (internal) Replaced some mt4 specific objects with jTradingPlatform wrappers to work with the changes in jAnalystUtils.
* (internal) Type changes, various refactorings, moved logic away from the constructors of MessageProcessor, TradeExecutor and ExposuresCollector.
* (internal) Refactored StrategyRiskGroupLoader to bring it more up to our standards.

### Release 0.11: Date 20/10/16
* Strategy logs will now always be saved inside C:\XMProjects\jFund\StrategyLogs. The directory will be created if it doesn't exist on the host system.
* jFund will now connect to all Real MT4 servers and the jFund MT4 server.
* Fixed some issues with the way exposures from custom mapped LP symbols were being calculated.
* Trade request WS Calls now have maxConnectionEstablish and maxResponseWait timeout values.
* Changes that result from internal library changes since the last release.

### Release 0.10b: Date 26/07/16
* Fixed a bug where tradeUpdate messages would send adjustment trades for the full exposure of ignored trades and mark them as relevant.

### Release 0.10: Date 22/07/16
* Now handles micro and zero symbols correctly. Setting a threshold for a parent symbol will automatically make it's children relevant.
* Changed the jManagerServer host target from the dev wildflydev to a dedicated one.
* Order requests now use the quantity based system.
* Will now log every relevant trade that has passed through the system, and mark the setup and trading stages.
* Restored the tradeUpdate log entries removed in 0.08b.
* Fixed a bug where trades with 0 exposure effect would be logged.
* Removed a redundant log column.

### Release 0.09: Date 28/06/16
* This release replaces the trade open time based recovery mechanism, with one that flags trade messages as relevant or not on MessageProcessor.
* This resolves the previous known design limitation that caused trades arriving right after an AC trade request is issued and before it was executed, to not generate exposures if a DC happened at a specific time.
* Fixed a bug where the exposures after a DC recovery would differ from the starting.
* 'Full' logging option now has an additional column (the third one) with a trade's opening time.
* 'Full' logging option will now delete the log, leave a note and reconstruct it upon reconnect from a DC, to avoid duplication of data.

### Release 0.08c: Date 28/06/16
* Fixed a bug that caused threads from the past state to survive alongside the new state after a restart.

### Release 0.08b: Date 23/06/16
* 'Full' logging option will no longer show update trades.
* Temporarily deactivated the email sending mechanism due to a network permissions issue.

### Release 0.08: Date 17/06/16
* A strategy has an additional option specifying the threshold under which trades won't have an impact on exposures. This is added in jFundStrategyLoader using -tradeVolumeThreshold
* Only trades that happened after the latest AntiCoverage trade will generate exposures in case of a disconnect.
* Full logging option now displays normalized trade volumes.
* Fixed a bug that caused closing trades of irrelevant open trades to impact jFund's state if -includePastState was false.
* -includePastState has a different effect now. Both options have initial open trades being kept in state,
  both options won't let old trades be 'covered' a second time. The difference lies in whether to let initial   trades have an impact on exposures.

### Release 0.07b: Date: 10/06/16
* Fixed a bug where AntiCoverage trades generated exposures based on the appetite.

### Release 0.07: Date: 27/05/16
* Internal changes in dependencies, Db connection is now supplied by a separate module.

### Release 0.05b: Date: 16/03/2016
* Bug fix in mt4jc that caused some ticks to be skipped if a client was not subscribed to all symbols.

### Release 0.05: Date: 03/03/2016
* MessageProcessor will now correctly handle TradeUpdate messages (volume changes, price changes, tradeType changes etc) by only sending adjusting trades forward,
  instead of sending virtual closeTrade() and openTrade() messages forward for each update.
* `-logExposures` now points to an enum with options: full(that logs each trade and it's impact), accumulation(that logs the internal state per unit of time), none(that doesn't log exposures).
   Default: accumulation.
* ExposureCollector will no longer send trades for a symbol if there is a trade 'under execution' for that symbol in TradeExecutor. This replaces the internal synchronization mechanism of the later.
* Improvements to the disconnect monitor.

### Release 0.04: Date: 12/02/2016
* Network conditions will no longer cause duplicate trades/large counterbalance trades to appear.
* Logfiles now take less space and are 'per strategy, per day', so they are more easily readable and manageable.
* Now correctly calculates and sends conversion factors / quotes alongside exposure information for jFundAnalyst.
* Implemented a cleaner way to monitor for mt4 server disconnect events, that's less CPU intensive.

### Release 0.03: Date: 08/02/2016
* New optional run parameter `-sendExposures` that controls whether jFund will send exposures to RabbitMQ for jFundAnalyst to display. Default: true.
* New optional run parameter `-logExposures` that controls whether jFund will log it's internal state to a txt file. Default: false.
* The old Map<String, Double> objects for keeping exposures are now replaced by the ExposureBundle object that we calculate anyway.
* Fixed a bug where the TradeExecutor assumed the OpenOrderCommand WebService result was always.
* Various readability changes.
