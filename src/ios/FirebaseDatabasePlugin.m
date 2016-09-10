#import "FirebaseDatabasePlugin.h"
#import "ObserverRemover.h"
@import Firebase;


@implementation FirebaseDatabasePlugin

- (void)initialize:(CDVInvokedUrlCommand *)command {

    if (![FIRApp defaultApp]) {
        [FIRApp configure];
    }
    self.database = [FIRDatabase database];
    self.observerRemovers = [NSMutableDictionary dictionary];
}

- (void)push:(CDVInvokedUrlCommand *)command {

    NSString *path = [command argumentAtIndex:0 withDefault:@"/" andClass:[NSString class]];
    id value = [command argumentAtIndex:1];
    FIRDatabaseReference *ref = [self.database referenceWithPath:path];

    [[ref childByAutoId] setValue:value withCompletionBlock:^(NSError *error, FIRDatabaseReference *ref) {
        CDVPluginResult *pluginResult;
        if (error) {

            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:@{
                    @"code" : @(error.code),
                    @"message" : error.description
            }];
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:[NSString stringWithFormat:@"%@/%@", path, [ref key]]];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)set:(CDVInvokedUrlCommand *)command {

    NSString *path = [command argumentAtIndex:0 withDefault:@"/" andClass:[NSString class]];
    id value = [command argumentAtIndex:1];
    FIRDatabaseReference *ref = [self.database referenceWithPath:path];

    [ref setValue:value withCompletionBlock:^(NSError *error, FIRDatabaseReference *ref) {
        CDVPluginResult *pluginResult;
        if (error) {

            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:@{
                    @"code" : @(error.code),
                    @"message" : error.description
            }];
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:path];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)update:(CDVInvokedUrlCommand *)command {

    NSString *path = [command argumentAtIndex:0 withDefault:@"/" andClass:[NSString class]];
    NSDictionary *values = [command argumentAtIndex:1 withDefault:@{} andClass:[NSDictionary class]];
    FIRDatabaseReference *ref = [self.database referenceWithPath:path];

    [ref updateChildValues:values withCompletionBlock:^(NSError *error, FIRDatabaseReference *ref) {
        CDVPluginResult *pluginResult;
        if (error) {

            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:@{
                    @"code" : @(error.code),
                    @"message" : error.description
            }];
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:path];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)remove:(CDVInvokedUrlCommand *)command {

    NSString *path = [command argumentAtIndex:0 withDefault:@"/" andClass:[NSString class]];
    FIRDatabaseReference *ref = [self.database referenceWithPath:path];

    [ref removeValueWithCompletionBlock:^(NSError *error, FIRDatabaseReference *ref) {
        CDVPluginResult *pluginResult;
        if (error) {

            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:@{
                    @"code" : @(error.code),
                    @"message" : error.description
            }];
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)once:(CDVInvokedUrlCommand *)command {

    NSString *path = [command argumentAtIndex:0 withDefault:@"/" andClass:[NSString class]];
    NSString *orderByType = [command argumentAtIndex:1 withDefault:nil andClass:[NSString class]];
    NSString *orderByPath = [command argumentAtIndex:2 withDefault:nil andClass:[NSString class]];
    NSDictionary *filters = [command argumentAtIndex:3 withDefault:@{} andClass:[NSDictionary class]];
    NSNumber *limitToFirst = [command argumentAtIndex:4 withDefault:nil andClass:[NSNumber class]];
    NSNumber *limitToLast = [command argumentAtIndex:5 withDefault:nil andClass:[NSNumber class]];
    FIRDataEventType type = [self stringToType:[command argumentAtIndex:6 withDefault:@"value" andClass:[NSString class]]];

    FIRDatabaseReference *ref = [self.database referenceWithPath:path];
    FIRDatabaseQuery *query = [self createRef:ref withOrderByType:orderByType andPath:orderByPath];
    FIRDatabaseQuery *filteredQuery = [self filterQuery:query withFilters:filters];
    FIRDatabaseQuery *limitedQuery = [self limitQuery:query toFirst:limitToFirst andLast:limitToLast];

    [limitedQuery observeSingleEventOfType:type withBlock:^(FIRDataSnapshot *_Nonnull snapshot) {

        [snapshot value];
        NSDictionary *result;
        CDVPluginResult *pluginResult = [self snapshotToResult:snapshot];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }                      withCancelBlock:^(NSError *_Nonnull error) {

        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:@{
                @"code" : @(error.code),
                @"message" : error.description
        }];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)on:(CDVInvokedUrlCommand *)command {

    NSString *path = [command argumentAtIndex:0 withDefault:@"/" andClass:[NSString class]];
    NSString *orderByType = [command argumentAtIndex:1 withDefault:nil andClass:[NSString class]];
    NSString *orderByPath = [command argumentAtIndex:2 withDefault:nil andClass:[NSString class]];
    NSDictionary *filters = [command argumentAtIndex:3 withDefault:@{} andClass:[NSDictionary class]];
    NSNumber *limitToFirst = [command argumentAtIndex:4 withDefault:nil andClass:[NSNumber class]];
    NSNumber *limitToLast = [command argumentAtIndex:5 withDefault:nil andClass:[NSNumber class]];
    FIRDataEventType type = [self stringToType:[command argumentAtIndex:6 withDefault:@"value" andClass:[NSString class]]];
    NSString *key = [command argumentAtIndex:7 withDefault:nil andClass:[NSString class]];


    FIRDatabaseReference *ref = [self.database.reference child:path];
    FIRDatabaseQuery *query = [self createRef:ref withOrderByType:orderByType andPath:orderByPath];
    FIRDatabaseQuery *filteredQuery = [self filterQuery:query withFilters:filters];
    FIRDatabaseQuery *limitedQuery = [self limitQuery:query toFirst:limitToFirst andLast:limitToLast];

    FIRDatabaseHandle handle = [limitedQuery observeEventType:type withBlock:^(FIRDataSnapshot *_Nonnull snapshot) {

        [snapshot value];
        NSDictionary *result;
        CDVPluginResult *pluginResult = [self snapshotToResult:snapshot];
        [pluginResult setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }              withCancelBlock:^(NSError *error) {

        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:@{
                @"code" : @(error.code),
                @"message" : error.description
        }];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];

    [self.observerRemovers setObject:[ObserverRemover observerRemoverWithQuery:query andHandle:handle] forKey:key];
    NSLog(@"listeners removers: %@", self.observerRemovers);
}

- (void)off:(CDVInvokedUrlCommand *)command {

    NSString *key = [command argumentAtIndex:0 withDefault:nil andClass:[NSString class]];
    ObserverRemover *remover = self.observerRemovers[key];
    [remover remove];
    [self.observerRemovers removeObjectForKey:key];
}

- (FIRDatabaseQuery *)limitQuery:query toFirst:(NSNumber *)limitToFirst andLast:(NSNumber *)limitToLast {

    FIRDatabaseQuery *result = query;
    if (limitToFirst) {
        result = [result queryLimitedToFirst:limitToFirst];
    }
    if (limitToLast) {
        result = [result queryLimitedToLast:limitToLast];
    }

    return result;
}

- (FIRDatabaseQuery *)filterQuery:query withFilters:filters {

    FIRDatabaseQuery *result = query;
    if (filters[@"equalTo"]) {
        result = [result queryEqualToValue:filters[@"equalTo"]];
    }

    if (filters[@"startAt"]) {
        result = [result queryStartingAtValue:filters[@"equalTo"]];
    }

    if (filters[@"endAt"]) {
        result = [result queryEndingAtValue:filters[@"equalTo"]];
    }

    return result;
}

- (FIRDatabaseQuery *)createRef:(FIRDatabaseReference *)ref withOrderByType:(NSString *)orderByType andPath:(NSString *)path {

    if ([orderByType isEqualToString:@"key"]) {
        return [ref queryOrderedByKey];
    } else if ([orderByType isEqualToString:@"child"]) {
        return [ref queryOrderedByChild:path];
    } else if ([orderByType isEqualToString:@"value"]) {
        return [ref queryOrderedByValue];
    } else if ([orderByType isEqualToString:@"priority"]) {
        return [ref queryOrderedByPriority];
    } else {
        return ref;
    }

}

- (CDVPluginResult *)snapshotToResult:(FIRDataSnapshot *)snapshot {

    id value = [snapshot value];

    if ([value isKindOfClass:[NSDictionary class]]) {
        return [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:value];
    } else if ([value isKindOfClass:[NSString class]]) {
        return [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:value];
    } else if ([value isKindOfClass:[NSNumber class]]) {
        return [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDouble:[value doubleValue]];
    } else if ([value isKindOfClass:[NSArray class]]) {
        return [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:value];
    } else {
        return [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }
}

- (FIRDataEventType)stringToType:(NSString *)type {

    if ([type isEqualToString:@"value"]) {

        return FIRDataEventTypeValue;
    } else if ([type isEqualToString:@"child_added"]) {

        return FIRDataEventTypeChildAdded;
    } else if ([type isEqualToString:@"child_removed"]) {

        return FIRDataEventTypeChildRemoved;
    } else if ([type isEqualToString:@"child_changed"]) {

        return FIRDataEventTypeChildChanged;
    } else if ([type isEqualToString:@"child_moved"]) {

        return FIRDataEventTypeChildMoved;
    }
}

@end