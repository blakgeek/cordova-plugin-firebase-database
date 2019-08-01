#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
@import Firebase;

@interface FirebaseDatabasePlugin : CDVPlugin
- (void)initialize:(CDVInvokedUrlCommand *)command;

- (void)once:(CDVInvokedUrlCommand *)command;
- (void)on:(CDVInvokedUrlCommand *)command;
- (void)off:(CDVInvokedUrlCommand *)command;
- (void)push:(CDVInvokedUrlCommand *)command;
- (void)set:(CDVInvokedUrlCommand *)command;
- (void)update:(CDVInvokedUrlCommand *)command;
- (void)remove:(CDVInvokedUrlCommand *)command;
- (void)setOnline:(CDVInvokedUrlCommand *)command;
- (void)setLoggingEnabled:(CDVInvokedUrlCommand *)command;

@property(strong) NSString *eventCallbackId;
@property(strong) FIRDatabase *database;
@property(strong) NSMutableDictionary *observerRemovers;
@end
