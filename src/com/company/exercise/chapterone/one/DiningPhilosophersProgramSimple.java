package com.company.exercise.chapterone.one;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class DiningPhilosophersProgramSimple {

    private static final int EAT_TIME = 100;
    private static final int PHILOSOPHERS_COUNT = 5;
    private static final int CHOPSTICK_COUNT = 5;
    private volatile static boolean anyoneStarved = false;

    public static void main(String[] args) throws InterruptedException {
        Table table = new Table();
        Collection<Thread> philosophers = new ArrayList<>();

        for (int i = 0; i < PHILOSOPHERS_COUNT; i++) {
            philosophers.add(new Thread(new Philosopher(i + 1, table)));
        }

        try {
            for (Thread philosopher : philosophers)
                philosopher.start();

            for (Thread philosopher : philosophers)
                philosopher.join();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static class Philosopher implements Runnable {
        private int id;
        private Table table;
        private PlateChopsticks plateChopsticks;
        private Chair chair;

        public Philosopher(int id, Table table) {
            this.id = id;
            this.table = table;
        }

        @Override
        public void run() {
            int thinkingCycle = 0;
            int starvationCounter = 1000;
            boolean hasEaten = true;

            try {
                while (thinkingCycle < 50) {
                    if (anyoneStarved)
                        throw new InterruptedException("Someone starved");

                    if (starvationCounter <= 0) {
                        anyoneStarved = true;
                        throw new RuntimeException(id + " starved.");
                    }

                    if (hasEaten) {
                        System.out.println(id + " is thinking in " + thinkingCycle++ + ". cycle");
                        think();
                    }

                    hasEaten = eat();

                    if (!hasEaten) {
                        Thread.sleep(100);
                        starvationCounter--;
                    }
                }

                System.out.println(id + " has finished all cycles");

            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }

        private void think() {
            int thinkingTime = ThreadLocalRandom.current().nextInt(1, 11) * 100;

            System.out.println(id + " is thinking " + thinkingTime + " miliseconds.");

            try {
                Thread.sleep(thinkingTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private boolean eat() throws InterruptedException {
            System.out.println(id + " is going to eat");

            // if already sitting on chair, don't check another chair
            if (chair == null) {
                chair = table.getFreeChair();

                // There are no chairs, try again
                if (chair == null) {
                    return false;
                }
            }

            System.out.println(id + " took chair " + chair.id);

            plateChopsticks = table.getPlateChopsticks(chair.id);

            if (!canEat())
                return false;

            System.out.println(String.format("%d is EATING with on chair %d with left %d and right %d",
                    id,
                    chair.id,
                    plateChopsticks.left.id,
                    plateChopsticks.right.id
            ));

            try {
                Thread.sleep(EAT_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println(String.format("%d is FINISHED eating with on chair %d with left %d and right %d",
                    id,
                    chair.id,
                    plateChopsticks.left.id,
                    plateChopsticks.right.id
            ));

            finishEating();

            return true;
        }

        private boolean canEat() {
            return plateChopsticks != null && plateChopsticks.left != null && plateChopsticks.right != null;
        }

        private void finishEating() {
            table.finishPlate(plateChopsticks, chair);
            plateChopsticks = null;
        }
    }

    static class Table {
        private final Object lock = new Object();
        private Collection<Chair> chairs = new ArrayList<>(5);
        private volatile Collection<Chopstick> chopsticks = new ArrayList<>();

        public Table() {
            for (int i = 0; i < CHOPSTICK_COUNT; i++) {
                chairs.add(new Chair(i + 1));
                chopsticks.add(new Chopstick(i + 1));
            }
        }

        public synchronized Chair getFreeChair() {
            return chairs.stream()
                    .filter(c -> !c.isTaken)
                    .peek(c -> c.setTaken(true))
                    .findFirst()
                    .orElse(null);
        }

        public synchronized void finishPlate(PlateChopsticks plateChopsticks, Chair chair) {
            chair.setTaken(false);
            plateChopsticks.leaveLeft();
            plateChopsticks.leaveRight();
        }

        private PlateChopsticks getPlateChopsticks(int plateNumber) {
            int left = plateNumber == 1 ? chopsticks.size() : plateNumber - 1;
            int right = plateNumber;

            PlateChopsticks plateChopsticks = new PlateChopsticks();

            synchronized (lock) {
                chopsticks.stream()
                        .filter(c -> !c.isTaken())
                        .filter(c -> c.id == left)
                        .findFirst()
                        .ifPresent(plateChopsticks::takeLeft);

                chopsticks.stream()
                        .filter(c -> !c.isTaken())
                        .filter(c -> c.getId() == right)
                        .findFirst()
                        .ifPresent(plateChopsticks::takeRight);

                if (plateChopsticks.left == null || plateChopsticks.right == null) {
                    plateChopsticks.leaveBoth();
                }
            }

            return plateChopsticks;
        }
    }

    static class PlateChopsticks {
        private Chopstick left;
        private Chopstick right;

        public PlateChopsticks() {
        }

        public void takeLeft(Chopstick left) {
            this.left = left;
            this.left.setTaken(true);
        }

        public void leaveLeft() {
            this.left.setTaken(false);
            this.left = null;
        }

        public void takeRight(Chopstick right) {
            this.right = right;
            this.right.setTaken(true);
        }

        public void leaveRight() {
            this.right.setTaken(false);
            this.right = null;
        }

        public void leaveBoth() {
            Optional.ofNullable(this.left)
                    .ifPresent(c -> this.leaveLeft());
            Optional.ofNullable(this.right)
                    .ifPresent(c -> this.leaveRight());
        }
    }

    static class Chair {
        private int id;

        private boolean isTaken;

        public Chair(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public boolean isTaken() {
            return isTaken;
        }

        public void setTaken(boolean taken) {
            isTaken = taken;
        }
    }

    static class Chopstick {
        private int id;
        private boolean isTaken;

        public Chopstick(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public boolean isTaken() {
            return isTaken;
        }

        public void setTaken(boolean taken) {
            isTaken = taken;
        }
    }
}
