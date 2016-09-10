#import <Firebase.h>

@class FIRDatabaseQuery;

@interface ObserverRemover : NSObject
+ (ObserverRemover *)observerRemoverWithQuery:(FIRDatabaseQuery *)query andHandle:(FIRDatabaseHandle)handle;

-(void) remove;
    @property FIRDatabaseHandle handle;
    @property (strong, nonatomic) FIRDatabaseQuery *query;
@end