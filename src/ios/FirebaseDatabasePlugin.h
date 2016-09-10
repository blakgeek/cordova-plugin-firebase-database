#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
@import FirebaseDatabase;

@interface FirebaseDatabasePlugin : CDVPlugin
- (void)initialize:(CDVInvokedUrlCommand *)command;

- (void)once:(CDVInvokedUrlCommand *)command;
- (void)on:(CDVInvokedUrlCommand *)command;
- (void)off:(CDVInvokedUrlCommand *)command;
- (void)push:(CDVInvokedUrlCommand *)command;
- (void)set:(CDVInvokedUrlCommand *)command;
- (void)update:(CDVInvokedUrlCommand *)command;
- (void)remove:(CDVInvokedUrlCommand *)command;

@property(strong, nonatomic) NSString *eventCallbackId;
@property(strong, nonatomic) FIRDatabase *database;
@end
