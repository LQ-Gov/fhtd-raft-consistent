package com.fhtd.raft.config;

import java.util.Properties;

public class Conf<T> {

    private String key;

    private T value;

    private final T defaultValue;

    public Conf(String key,T defaultValue){
        this.key = key;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public final static <T> Conf<T> create(String key,T defaultValue){
        return new Conf<>(key,defaultValue);

    }

    public void init(Properties properties) {
        if (properties != null) this.value = (T) properties.getOrDefault(key, defaultValue);
    }

    public String key(){return key;}

    public T value(){return value;}
}
