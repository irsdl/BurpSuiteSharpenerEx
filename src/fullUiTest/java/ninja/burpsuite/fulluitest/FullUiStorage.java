// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
package ninja.burpsuite.fulluitest;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/** Never reads or writes the user's Sharpener global/project preferences. */
final class FullUiStorage {
    final Map<String,String> global=new ConcurrentHashMap<>(), project=new ConcurrentHashMap<>();
    final List<String> errors=new CopyOnWriteArrayList<>(), output=new CopyOnWriteArrayList<>();
    final MontoyaApi api;
    FullUiStorage(MontoyaApi real) {
        var preferences=store(burp.api.montoya.persistence.Preferences.class,global);
        var data=store(PersistedObject.class,project);
        Persistence persistence=proxy(Persistence.class,(object,method,args)->switch(method.getName()) {
            case "preferences"->preferences; case "extensionData"->data; default->throw new UnsupportedOperationException(method.getName());
        });
        var logging=proxy(burp.api.montoya.logging.Logging.class,(object,method,args)->{
            if(method.getName().equals("logToError"))errors.add(String.valueOf(args[0]));
            if(method.getName().equals("logToOutput"))output.add(String.valueOf(args[0]));
            return invoke(method,real.logging(),args);
        });
        api=proxy(MontoyaApi.class,(object,method,args)->switch(method.getName()) {
            case "persistence"->persistence; case "logging"->logging; default->invoke(method,real,args);
        });
    }
    private static <T>T store(Class<T> type,Map<String,String> values) {
        return proxy(type,(object,method,args)->switch(method.getName()) {
            case "getString"->values.get((String)args[0]);
            case "setString"->{values.put((String)args[0],(String)args[1]);yield null;}
            case "deleteString"->{values.remove((String)args[0]);yield null;}
            case "stringKeys"->Set.copyOf(values.keySet());
            default->throw new UnsupportedOperationException("Full UI storage has no implementation for "+method.getName());
        });
    }
    static <T>T proxy(Class<T> type,InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(object,method,args)->{
            if(method.getDeclaringClass()==Object.class)return switch(method.getName()) {
                case "toString"->"FullUi("+type.getSimpleName()+")";case "hashCode"->System.identityHashCode(object);case "equals"->object==args[0];default->null;
            };
            return handler.invoke(object,method,args);
        }));
    }
    static Object invoke(Method method,Object object,Object[] args)throws Throwable {
        try{return method.invoke(object,args);}catch(InvocationTargetException wrapped){throw wrapped.getCause();}
    }
}
