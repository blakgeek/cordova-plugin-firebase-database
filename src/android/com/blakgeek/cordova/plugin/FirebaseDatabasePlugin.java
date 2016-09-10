package com.blakgeek.cordova.plugin;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class FirebaseDatabasePlugin extends CordovaPlugin {

    private CallbackContext eventContext;
    FirebaseDatabase database;
    Map<String, ListenerRemover> listeners = new HashMap<>();

    @Override
    protected void pluginInitialize() {

        database = FirebaseDatabase.getInstance();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        switch (action) {
            case "initialize":
                return initialize(args, callbackContext);
            case "once":
                return once(args, callbackContext);
            case "on":
                return on(args, callbackContext);
            case "off":
                return off(args, callbackContext);
            case "update":
                return update(args, callbackContext);
            case "set":
                return set(args, callbackContext);
            case "remove":
                return remove(args, callbackContext);
            case "push":
                return push(args, callbackContext);
            case "setOnline":
                return setOnline(args);
            case "setLoggingEnabled":
                return setLoggingEnabled(args);
            default:
                return false;
        }
    }

    private boolean setLoggingEnabled(JSONArray args) {

        if(args.optBoolean(0, false)) {
            database.setLogLevel(Logger.Level.DEBUG);
        } else {
            database.setLogLevel(Logger.Level.NONE);
        }
        return true;
    }

    private boolean setOnline(JSONArray args) {

        if(args.optBoolean(0, false)) {
            database.goOnline();
        } else {
            database.goOffline();
        }
        return true;
    }

    private Object toSettable(Object value) {

        Object result = value;
        if (value instanceof JSONObject) {
            Type type = new TypeToken<Map<String, Object>>() {
            }.getType();
            result = new Gson().fromJson(value.toString(), type);
        }

        return result;
    }

    private boolean push(JSONArray args, final CallbackContext callbackContext) {

        final String path = args.optString(0);
        Object value = toSettable(args.opt(1));

        database.getReference(path).push().setValue(value, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                if (databaseError != null) {
                    callbackContext.sendPluginResult(convertToPluginResult(databaseError, false));
                } else {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, path + '/' + databaseReference.getKey()));
                }
            }
        });
        return true;
    }

    private boolean remove(JSONArray args, final CallbackContext callbackContext) {

        final String path = args.optString(0);

        database.getReference(path).removeValue(new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                if (databaseError != null) {
                    callbackContext.sendPluginResult(convertToPluginResult(databaseError, false));
                } else {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, path));
                }
            }
        });
        return true;
    }

    private boolean set(JSONArray args, final CallbackContext callbackContext) {

        final String path = args.optString(0);
        Object value = toSettable(args.opt(1));

        database.getReference(path).setValue(value, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                if (databaseError != null) {
                    callbackContext.sendPluginResult(convertToPluginResult(databaseError, false));
                } else {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, path));
                }
            }
        });
        return true;
    }

    private boolean update(JSONArray args, final CallbackContext callbackContext) {

        final String path = args.optString(0);
        JSONObject rawValue = args.optJSONObject(1);
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> value = new Gson().fromJson(rawValue.toString(), type);

        database.getReference(path).updateChildren(value, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                if (databaseError != null) {
                    callbackContext.sendPluginResult(convertToPluginResult(databaseError, false));
                } else {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, path));
                }
            }
        });
        return true;
    }

    private boolean off(JSONArray args, CallbackContext callbackContext) {

        String id = args.optString(0);

        if (listeners.containsKey(id)) {

            listeners.remove(id).remove();
        }

        return true;
    }

    private boolean on(JSONArray args, final CallbackContext callbackContext) {

        final String type = args.optString(6, "value");
        final String id = args.optString(7);
        Query query = prepareQuery(args);

        if ("value".equals(type)) {
            ValueEventListener valueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    callbackContext.sendPluginResult(convertToPluginResult(dataSnapshot, true));
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                    PluginResult errorResult = convertToPluginResult(databaseError, false);
                    callbackContext.sendPluginResult(errorResult);
                }
            };
            query.addValueEventListener(valueEventListener);
            listeners.put(id, new ValueListenerRemover(query, valueEventListener));

        } else {
            ChildEventListener childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                    if ("child_added".equals(type)) {

                        callbackContext.sendPluginResult(convertToPluginResult(dataSnapshot, true));
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    if ("child_changed".equals(type)) {

                        callbackContext.sendPluginResult(convertToPluginResult(dataSnapshot, true));
                    }
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                    if ("child_removed".equals(type)) {

                        callbackContext.sendPluginResult(convertToPluginResult(dataSnapshot, true));
                    }
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    if ("child_moved".equals(type)) {

                        callbackContext.sendPluginResult(convertToPluginResult(dataSnapshot, true));
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                    PluginResult errorResult = convertToPluginResult(databaseError, false);
                    callbackContext.sendPluginResult(errorResult);
                }
            };
            query.addChildEventListener(childEventListener);
            listeners.put(id, new ChildListenerRemover(query, childEventListener));
        }
        return true;
    }

    private boolean once(JSONArray args, final CallbackContext callbackContext) {


        Query query = prepareQuery(args);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                callbackContext.sendPluginResult(convertToPluginResult(dataSnapshot));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

                PluginResult errorResult = convertToPluginResult(databaseError, false);
                callbackContext.sendPluginResult(errorResult);
            }
        });

        return true;
    }

    private Query prepareQuery(JSONArray args) {

        String path = args.optString(0, "/");
        String orderByType = args.optString(1);
        String orderByPath = args.optString(2, "/");
        JSONObject filters = args.optJSONObject(3);
        int start = args.optInt(4, -1);
        int end = args.optInt(5, -1);
        DatabaseReference ref = database.getReference(path);
        Query query = orderBy(ref, orderByType, orderByPath);
        Query filteredQuery = filter(query, filters);

        return limit(filteredQuery, start, end);
    }

    private Query limit(Query query, int first, int last) {

        Query limitedQuery = query;

        if (first > 0) {
            limitedQuery = limitedQuery.limitToFirst(first);
        }

        if (last > 0) {
            limitedQuery = limitedQuery.limitToLast(last);
        }

        return limitedQuery;
    }

    private Query filter(Query query, JSONObject filters) {

        Query filteredQuery = query;
        if (filters == null) return query;

        Object endAt = filters.opt("endAt");
        Object startAt = filters.opt("startAt");
        Object equalTo = filters.opt("equalTo");

        if (startAt != null) {
            if (startAt instanceof Number) {
                filteredQuery = filteredQuery.startAt((Double) startAt);
            } else if (startAt instanceof Boolean) {
                filteredQuery = filteredQuery.startAt((Boolean) startAt);
            } else {
                filteredQuery = filteredQuery.startAt(startAt.toString());
            }
        }

        if (endAt != null) {
            if (endAt instanceof Number) {
                filteredQuery = filteredQuery.endAt((Double) endAt);
            } else if (endAt instanceof Boolean) {
                filteredQuery = filteredQuery.endAt((Boolean) endAt);
            } else {
                filteredQuery = filteredQuery.endAt(endAt.toString());
            }
        }

        if (equalTo != null) {
            if (equalTo instanceof Number) {
                filteredQuery = filteredQuery.equalTo((Double) equalTo);
            } else if (equalTo instanceof Boolean) {
                filteredQuery = filteredQuery.equalTo((Boolean) equalTo);
            } else {
                filteredQuery = filteredQuery.equalTo(equalTo.toString());
            }
        }

        return filteredQuery;
    }

    private Query orderBy(DatabaseReference ref, String orderByType, String orderByPath) {

        switch (orderByType) {
            case "key":
                return ref.orderByKey();
            case "child":
                return ref.orderByChild(orderByPath);
            case "value":
                return ref.orderByValue();
            case "priority":
                return ref.orderByPriority();
            default:
                return ref;
        }
    }

    private PluginResult convertToPluginResult(DatabaseError error, boolean reusable) {

        JSONObject data = null;
        try {
            data.put("code", error.getCode());
            data.put("message", error.getMessage());
            data.put("details", error.getDetails());
        } catch (JSONException e) {

        }

        PluginResult result = new PluginResult(PluginResult.Status.ERROR, data);
        result.setKeepCallback(reusable);

        return result;
    }

    private PluginResult convertToPluginResult(PluginResult.Status status, Object value, boolean reusable) {

        JSONObject data = null;
        try {
            data = new JSONObject(new Gson().toJson(value));
        } catch (JSONException e) {
        }
        PluginResult result = new PluginResult(status, data);
        result.setKeepCallback(reusable);

        return result;
    }

    private PluginResult convertToPluginResult(DataSnapshot dataSnapshot) {

        return convertToPluginResult(PluginResult.Status.OK, dataSnapshot.getValue(), false);
    }

    private PluginResult convertToPluginResult(DataSnapshot dataSnapshot, boolean reusable) {

        return convertToPluginResult(PluginResult.Status.OK, dataSnapshot.getValue(), reusable);
    }

    private boolean initialize(JSONArray args, CallbackContext callbackContext) {

        database.setPersistenceEnabled(args.optBoolean(0, false));
        eventContext = callbackContext;
        return true;
    }

    private void raiseEvent(String type) {
        raiseEvent(type, null);
    }

    private void raiseEvent(String type, Object data) {

        if (eventContext != null) {

            JSONObject event = new JSONObject();
            try {
                event.put("type", type);
                event.put("data", data);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            PluginResult result = new PluginResult(PluginResult.Status.OK, event);
            result.setKeepCallback(true);
            eventContext.sendPluginResult(result);
        }
    }

    private interface ListenerRemover {

        public void remove();
    }

    private class ChildListenerRemover implements ListenerRemover {

        private Query query;
        private ChildEventListener listener;

        public ChildListenerRemover(Query query, ChildEventListener listener) {

            this.query = query;
            this.listener = listener;
        }

        @Override
        public void remove() {

            query.removeEventListener(listener);
        }
    }

    private class ValueListenerRemover implements ListenerRemover {

        private Query query;
        private ValueEventListener listener;

        public ValueListenerRemover(Query query, ValueEventListener listener) {

            this.query = query;
            this.listener = listener;
        }

        @Override
        public void remove() {

            query.removeEventListener(listener);
        }
    }
}