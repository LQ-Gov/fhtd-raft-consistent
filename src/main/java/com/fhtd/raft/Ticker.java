package com.fhtd.raft;

import org.apache.commons.lang3.RandomUtils;

import java.util.*;

/**
 * @author liuqi19
 * @version Ticker2, 2019/9/4 11:40 下午 liuqi19
 **/
public class Ticker {

    private Timer timer = new Timer();

    private long period;


    private List<Tick> ticks = new LinkedList<>();

    private boolean running;


    public Ticker(long period) {
        this.period = period;
    }


    public synchronized Ticker start() {
        if(!isRunning()) {
            timer.schedule(new Task(), 0, period);
            this.running = true;
        }
        return this;
    }

    public synchronized void stop(){
        if(isRunning()) {
            timer.cancel();
            this.running = false;
        }
    }

    public boolean isRunning(){
        return running;

    }


    public void connect(Tick tick){
        ticks.add(tick);

    }


    private  class Task extends TimerTask {

        @Override
        public synchronized void run() {

            Iterator<Tick> it = ticks.iterator();

            while (it.hasNext()){
                Tick tick = it.next();
                if(tick.isClosed) it.remove();
                else tick.run();
            }

        }
    }

    public static class Tick{

        private int lease;

        private int randomizedLease;

        private volatile long elapsed;

        private boolean resting = true;

        private boolean isClosed = false;

        private Runnable runnable;

        public Tick(int lease,Runnable runnable){
            this.lease = lease;
            this.runnable = runnable;
        }


        public synchronized void run(){
            if (++elapsed >= (lease + randomizedLease)) {
                resting = false;

                runnable.run();

                elapsed = 0;

                resting = true;
            }

        }


        public int halfLease() {
            return lease / 2;
        }


        public int randomLease() {
            return RandomUtils.nextInt(0, this.lease);
        }

        public void reset(boolean randomInc) {

            reset(randomInc ? randomLease() : 0);

        }


        public void reset(int inc) {

            randomizedLease = inc;
            elapsed = 0;
        }


        public void reset() {
            reset(0);
        }

        public boolean resting() {
            return resting;
        }

        public long remain() {
            return (lease + randomizedLease) - elapsed;
        }


        public void close(){
            this.isClosed=true;
        }

    }
}
