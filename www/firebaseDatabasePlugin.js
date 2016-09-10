var exec = require('cordova/exec');
var listenerCtr = 1;
function FirebaseDatabase(persist) {

    this.isOnline = false;
    this.isPersistenceEnabled = persist;
    exec(dispatchEvent, null, 'FirebaseDatabasePlugin', 'initialize', [persist]);

    function dispatchEvent(event) {

        window.dispatchEvent(new CustomEvent(event.type, {detail: event.data}));
    }
}

FirebaseDatabase.prototype.ref = function (path) {
    return new DbRef(path);
};

Object.defineProperties(FirebaseDatabase.prototype, {

    online: {
        set: function (value) {
            this.isOnline = !!value;
            exec(dispatchEvent, null, 'FirebaseDatabasePlugin', 'setOnline', [this.isOnline]);
        },
        get: function () {
            return this.isOnline;
        }
    },
    loggingEnabled: {
        set: function (value) {
            this.isLoggingEnabled = !!value;
            exec(dispatchEvent, null, 'FirebaseDatabasePlugin', 'setLoggingEnabled', [this.isLoggingEnabled]);
        },
        get: function () {
            return this.isLoggingEnabled;
        }
    },
    persistenceEnabled: {
        get: function () {
            return this.isPersistenceEnabled;
        }
    }
});

function DbQuery() {}

DbQuery.prototype = {

    orderByKey: function () {

        this.orderByType = 'key';
    },

    orderByChild: function (path) {

        this.orderByType = 'child';
        this.orderByPath = path;
    },

    orderByValue: function () {

        this.orderByType = 'value';
    },

    orderByPriority: function () {

        this.orderByType = 'priority';
    },

    startAt: function (val) {

        this.filters.startAt = val;
    },

    endAt: function (val) {

        this.filters.endAt = val;
    },

    equalTo: function (val) {

        this.filters.equalTo = val;
    },

    limitToFirst: function (val) {

        this.first = val;
    },

    limitToLast: function (val) {

        this.last = val;
    },

    on: function (type, fn, context) {

        var id = fn.$listenerId || ++listenerCtr;
        var resolved = false;
        fn.$listenerId = id;

        return new Promise(function (resolve, reject) {
            exec(function (data) {
                var snapshot = new DbSnapshot(data);
                fn(snapshot);
                if (!resolved) {
                    resolve(fn);
                    resolved = true;
                }
            }, reject, 'FirebaseDatabasePlugin', 'on', [
                this.path,
                this.orderByType,
                this.orderByPath,
                this.filters,
                this.first,
                this.last,
                type,
                type + ':' + id
            ]);
        }.bind(this));
    },

    once: function (fn, context) {

        return new Promise(function (resolve, reject) {
            exec(function (data) {
                var snapshot = new DbSnapshot(data);
                resolve(snapshot);
                if (typeof fn === 'function') fn(snapshot);
            }, reject, 'FirebaseDatabasePlugin', 'once', [
                this.path,
                this.orderByType,
                this.orderByPath,
                this.filters,
                this.first,
                this.last
            ]);
        }.bind(this));
    },

    off: function (type, fn) {

        if (fn.$listenerId) {
            exec(null, null, 'FirebaseDatabasePlugin', 'off', [type + ':' + fn.$listenerId])
        }
    }
};

function DbRef(path) {

    this.path = path.replace(/\/$/, '');
}

DbRef.prototype = Object.create(DbQuery.prototype, {

    key: {
        get: function () {

            return this.path.split('/').slice(-1)[0];
        }
    },
    parent: {
        get: parent = function () {
            return this.path.split('/').slice(0, -1).join('/');
        }
    },
    ref: {
        get: function () {
            return this;
        }
    }
});

DbRef.prototype.push = function (value) {

    return new Promise(function (resolve, reject) {
        exec(function (path) {
            resolve(new DbRef(path));
        }, reject, 'FirebaseDatabasePlugin', 'push', [
            this.path,
            value
        ]);
    }.bind(this));
};

DbRef.prototype.set = function (value) {

    return new Promise(function (resolve, reject) {
        exec(function (path) {
            resolve(new DbRef(path));
        }, reject, 'FirebaseDatabasePlugin', 'set', [
            this.path,
            value
        ]);
    }.bind(this));
};

DbRef.prototype.update = function (children) {

    return new Promise(function (resolve, reject) {
        exec(function (path) {
            resolve(new DbRef(path));
        }, reject, 'FirebaseDatabasePlugin', 'update', [
            this.path,
            children
        ]);
    }.bind(this));
};

DbRef.prototype.remove = function () {

    return new Promise(function (resolve, reject) {
        exec(resolve, reject, 'FirebaseDatabasePlugin', 'remove', [
            this.path
        ]);
    }.bind(this));
};

DbRef.prototype.child = function (path) {

    return new DbRef(this.path.split('/').concat(path.split('/')).join('/'));
};

function DbSnapshot(value) {

    this.value = value;
}

DbSnapshot.prototype = {

    child: function (path) {

        return new DbSnapshot(findChild(String(path).split('/'), this.value));
    },

    hasChild: function (path) {

        var child = findChild(String(path).split('/'), this.value);
        return child !== null && child !== undefined;
    }
};

Object.defineProperties(DbSnapshot.prototype, {

    val: {
        get: function () {
            return this.value;
        }
    },
    exists: {
        get: function () {
            return !!this.value;
        }
    },

    hasChildren: {
        get: function () {
            return typeof this.value === 'object' && !!Object.keys(this.value).length;
        }
    },

    numChildren: {
        get: function () {
            return typeof this.value === 'object' ? Object.keys(this.value).length : 0;
        }
    }
});

function findChild(path, obj) {

    var key = path[0];
    if (!obj) {
        return null;
    } else if (path.length > 1) {
        return findChild(path.slice(1), key.trim() ? obj[key] : obj);
    } else {
        return obj[key];
    }
}


if (typeof module !== undefined && module.exports) {

    module.exports = FirebaseDatabase;
}