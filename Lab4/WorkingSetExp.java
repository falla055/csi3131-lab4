// File: WorkingSetExp.java
// Description: Simulation of memory management system using Working Set algorithm

import cern.jet.random.engine.*;

class WorkingSetExp
{
    public static void main(String[] args)
    {
        double startTime = 0.0, endTime = 10000;
        Seeds sds;
        MemManage mmng;

        RandomSeedGenerator rsg = new RandomSeedGenerator();
        sds = new Seeds(
            rsg.nextSeed(), rsg.nextSeed(),
            rsg.nextSeed(), rsg.nextSeed(),
            rsg.nextSeed(), rsg.nextSeed(),
            rsg.nextSeed(), rsg.nextSeed(),
            rsg.nextSeed(), rsg.nextSeed(),
            rsg.nextSeed(), rsg.nextSeed(),
            rsg.nextSeed(), rsg.nextSeed()
        );

        System.out.println("Running simulation with Working Set");
        mmng = new MemManage(PagingAlgorithm.WORKING_SET, startTime, endTime, sds);
        mmng.runSimulation();
        mmng.computeOutput();
        System.out.println("Number of faults: " + mmng.phiTimeBtwFaults.number);
        System.out.println("Number memory accesses (no faults): " + mmng.numMemAccesses);
        System.out.println("Number of faults per 1000 references: " + mmng.numPer1000);
    }
}