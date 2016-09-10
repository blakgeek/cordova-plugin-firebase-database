var exec = require('cordova/exec');
var listenerCtr = 1;
function FirebaseDatabase() {

    exec(dispatchEvent, null, 'FirebaseDatabasePlugin', 'initialize', []);

    this.ref = function (path) {

        return new DbRef(path);
    };

    function dispatchEvent(event) {

        window.dispatchEvent(new CustomEvent(event.type, {detail: event.data}));
    }
}

function DbQuery() {

}

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
        fn.$listenerId = id;
        exec(fn, null, 'FirebaseDatabasePlugin', 'on', [
            this.path,
            this.orderByType,
            this.orderByPath,
            this.filters,
            this.first,
            this.last,
            type,
            type + ':' + id
        ]);
    },

    once: function (fn, context) {

        return new Promise(function (resolve, reject) {
            exec(resolve, reject, 'FirebaseDatabasePlugin', 'once', [
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

DbRef.prototype = Object.create(DbQuery.prototype);

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

DbRef.prototype.key = function () {

    return this.path.split('/').slice(-1);
};

DbRef.prototype.parent = function () {
    return this.path.split('/').slice(0, -1).join('/');
};

DbRef.prototype.child = function (path) {

    return new DbRef(this.path.split('/').concat(path.split('/')).join('/'));
};

DbRef.prototype.ref = function () {
    return this;
};


if (typeof module !== undefined && module.exports) {

    module.exports = FirebaseDatabase;
}