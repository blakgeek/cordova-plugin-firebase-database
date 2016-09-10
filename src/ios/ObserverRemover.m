#import "ObserverRemover.h"
@import Firebase;

@implementation ObserverRemover
+(ObserverRemover*)observerRemoverWithQuery:(FIRDatabaseQuery *)query andHandle:(FIRDatabaseHandle)handle {
    ObserverRemover *observerRemover = [[ObserverRemover alloc] init];
    observerRemover.query = query;
    observerRemover.handle = handle;

    return observerRemover;
}

-(void) remove {
    [self.query removeObserverWithHandle:self.handle];
}
@end
